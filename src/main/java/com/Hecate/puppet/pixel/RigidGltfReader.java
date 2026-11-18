package com.Hecate.puppet.pixel;

import com.google.gson.*;
import com.jme3.math.*;
import com.jme3.scene.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/** Reads only source triangles and object transforms; no materials, animation frames or skin deformation. */
final class RigidGltfReader {
    private final JsonObject json;
    private final List<ByteBuffer> buffers=new ArrayList<>();
    private RigidGltfReader(Path path)throws IOException {
        if(Files.size(path)>100L*1024*1024)throw new IOException("模型文件超过 100 MB，请先简化并拆分");
        byte[] bytes=Files.readAllBytes(path),binary=null;String text;
        if(path.toString().toLowerCase(Locale.ROOT).endsWith(".glb")){
            ByteBuffer data=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            if(data.remaining()<12||data.getInt()!=0x46546c67||data.getInt()!=2||data.getInt()!=bytes.length)throw new IOException("无效的 GLB 2.0 文件");
            text=null;
            while(data.remaining()>=8){int length=data.getInt(),type=data.getInt();if(length<0||length>data.remaining())throw new IOException("GLB 数据段无效");byte[] chunk=new byte[length];data.get(chunk);if(type==0x4e4f534a)text=new String(chunk,java.nio.charset.StandardCharsets.UTF_8).trim();if(type==0x004e4942)binary=chunk;}
            if(text==null)throw new IOException("GLB 缺少场景数据");
        }else text=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
        json=new JsonParser().parse(text).getAsJsonObject();
        if(!json.getAsJsonObject("asset").get("version").getAsString().startsWith("2"))throw new IOException("需要 glTF 2.0");
        for(JsonElement element:json.getAsJsonArray("buffers")){
            JsonObject buffer=element.getAsJsonObject();byte[] content;
            if(!buffer.has("uri")){if(binary==null)throw new IOException("模型缺少网格二进制数据");content=binary;}
            else {String uri=buffer.get("uri").getAsString();if(uri.startsWith("data:"))content=Base64.getDecoder().decode(uri.substring(uri.indexOf(',')+1));else{Path source=path.toAbsolutePath().getParent().resolve(java.net.URLDecoder.decode(uri,java.nio.charset.StandardCharsets.UTF_8));if(Files.size(source)>100L*1024*1024)throw new IOException("网格数据超过 100 MB");content=Files.readAllBytes(source);}}
            if(content.length<buffer.get("byteLength").getAsInt()||content.length>100L*1024*1024)throw new IOException("网格数据长度无效");buffers.add(ByteBuffer.wrap(content).order(ByteOrder.LITTLE_ENDIAN));
        }
    }
    static Node read(Path path)throws IOException {
        try{return new RigidGltfReader(path).scene();}catch(IllegalArgumentException|IndexOutOfBoundsException|NullPointerException e){throw new IOException("无法读取刚性网格："+e.getMessage(),e);}
    }
    private Node scene()throws IOException {
        JsonArray nodes=json.getAsJsonArray("nodes");if(nodes==null||nodes.size()>4096)throw new IOException("模型节点无效或超过 4096 个");
        Node root=new Node("BlenderScene");JsonArray scenes=json.getAsJsonArray("scenes");int chosen=number(json,"scene",0);
        if(scenes==null)throw new IOException("模型缺少场景");JsonArray roots=scenes.get(chosen).getAsJsonObject().getAsJsonArray("nodes");
        for(JsonElement index:roots)root.attachChild(node(index.getAsInt(),new HashSet<>()));return root;
    }
    private Node node(int index,Set<Integer> path)throws IOException {
        if(!path.add(index))throw new IOException("模型节点形成循环");if(path.size()>128)throw new IOException("模型层级超过 128 层");
        JsonObject data=json.getAsJsonArray("nodes").get(index).getAsJsonObject();Node node=new Node(data.has("name")?data.get("name").getAsString():"Part_"+index);
        if(data.has("skin"))throw new IOException("部件含蒙皮。请把上臂、前臂等拆成独立刚性物体后导出");
        if(data.has("matrix")){JsonArray values=data.getAsJsonArray("matrix");Matrix4f matrix=new Matrix4f();for(int col=0;col<4;col++)for(int row=0;row<4;row++)matrix.set(row,col,values.get(col*4+row).getAsFloat());Transform transform=new Transform();transform.fromTransformMatrix(matrix);node.setLocalTransform(transform);}
        else {if(data.has("translation"))node.setLocalTranslation(vector(data.getAsJsonArray("translation")));if(data.has("scale"))node.setLocalScale(vector(data.getAsJsonArray("scale")));if(data.has("rotation")){JsonArray q=data.getAsJsonArray("rotation");node.setLocalRotation(new Quaternion(q.get(0).getAsFloat(),q.get(1).getAsFloat(),q.get(2).getAsFloat(),q.get(3).getAsFloat()).normalizeLocal());}}
        if(data.has("mesh")){
            JsonArray primitives=json.getAsJsonArray("meshes").get(data.get("mesh").getAsInt()).getAsJsonObject().getAsJsonArray("primitives");int primitive=0;
            for(JsonElement entry:primitives){JsonObject p=entry.getAsJsonObject();if(number(p,"mode",4)!=4)throw new IOException("仅支持三角形网格");if(p.has("extensions")&&p.getAsJsonObject("extensions").has("KHR_draco_mesh_compression"))throw new IOException("请在 Blender 导出时关闭 Draco 网格压缩");
                int accessor=p.getAsJsonObject("attributes").get("POSITION").getAsInt();float[] positions=positions(accessor);int[] indices;
                if(p.has("indices"))indices=indices(p.get("indices").getAsInt());else{indices=new int[positions.length/3];for(int i=0;i<indices.length;i++)indices[i]=i;}
                Mesh mesh=new Mesh();mesh.setBuffer(VertexBuffer.Type.Position,3,positions);mesh.setBuffer(VertexBuffer.Type.Index,3,indices);mesh.updateBound();node.attachChild(new Geometry(node.getName()+"_"+(primitive++),mesh));
            }
        }
        if(data.has("children"))for(JsonElement child:data.getAsJsonArray("children"))node.attachChild(node(child.getAsInt(),path));path.remove(index);return node;
    }
    private record Access(ByteBuffer buffer,int offset,int stride,int count,int component,String type) {}
    private Access access(int index,int components)throws IOException {
        JsonObject a=json.getAsJsonArray("accessors").get(index).getAsJsonObject();if(a.has("sparse")||!a.has("bufferView"))throw new IOException("暂不支持稀疏网格数据，请重新导出普通网格");
        JsonObject view=json.getAsJsonArray("bufferViews").get(a.get("bufferView").getAsInt()).getAsJsonObject();int component=a.get("componentType").getAsInt();int bytes=component==5126||component==5125?4:component==5123?2:component==5121?1:0;
        int count=a.get("count").getAsInt(),stride=number(view,"byteStride",components*bytes);if(bytes==0||count<1||count>200000||stride<components*bytes)throw new IOException("网格数据超出范围");
        int viewStart=number(view,"byteOffset",0),viewLength=view.get("byteLength").getAsInt(),localStart=number(a,"byteOffset",0);ByteBuffer buffer=buffers.get(view.get("buffer").getAsInt());long end=(long)localStart+(long)(count-1)*stride+components*bytes;
        if(viewStart<0||viewLength<0||localStart<0||end>viewLength||(long)viewStart+viewLength>buffer.limit())throw new IOException("网格数据越界");
        return new Access(buffer,viewStart+localStart,stride,count,component,a.get("type").getAsString());
    }
    private float[] positions(int index)throws IOException {Access a=access(index,3);if(a.component!=5126||!a.type.equals("VEC3"))throw new IOException("位置必须为普通浮点 VEC3");float[] result=new float[a.count*3];for(int i=0;i<a.count;i++)for(int c=0;c<3;c++)result[i*3+c]=a.buffer.getFloat(a.offset+i*a.stride+c*4);return result;}
    private int[] indices(int index)throws IOException {Access a=access(index,1);if(!a.type.equals("SCALAR")||a.component==5126)throw new IOException("索引必须为整数");int[] result=new int[a.count];for(int i=0;i<a.count;i++){int pos=a.offset+i*a.stride;result[i]=a.component==5121?a.buffer.get(pos)&255:a.component==5123?a.buffer.getShort(pos)&65535:a.buffer.getInt(pos);}return result;}
    private static int number(JsonObject object,String key,int fallback){return object.has(key)?object.get(key).getAsInt():fallback;}
    private static Vector3f vector(JsonArray values){return new Vector3f(values.get(0).getAsFloat(),values.get(1).getAsFloat(),values.get(2).getAsFloat());}
}
