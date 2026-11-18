package com.Hecate.puppet.editor;

import com.Hecate.puppet.editor.core.EditorBone;
import com.Hecate.puppet.volume.VolumeDefinition;
import com.jme3.font.*;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.scene.shape.Quad;
import java.util.*;
import java.util.function.Supplier;

/** In-window inspector for the new mode. All model mutation stays in PuppetEditorApp commands. */
public final class VolumeInspectorPanel {
    private final PuppetEditorApp app;private final BitmapFont font;private final TTFontLoader ttf;
    private final int x,y,width,height;private final Node root=new Node("VolumeInspector");
    private final java.util.List<Button> buttons=new ArrayList<>();
    private final Map<Button,Supplier<String>> values=new LinkedHashMap<>();private final Map<Button,String> previous=new HashMap<>();
    private EditorBone bone;private boolean hasVolume;private boolean strip3D;private boolean surfaceDepth;private boolean baked;private int tab;private int rowY;
    public VolumeInspectorPanel(PuppetEditorApp app,BitmapFont font,TTFontLoader ttf,int x,int y,int width,int height) {
        this.app=app;this.font=font;this.ttf=ttf;this.x=x;this.y=y;this.width=width;this.height=height;rebuild();
    }
    public Node getRootNode(){return root;}
    public void showTool(int mode){if(mode==2)return;tab=mode==1?0:1;rebuild();}
    public void update(float tpf) {
        EditorBone selected=app.getVolumeSelectedBone();boolean present=selected!=null&&selected.getVolumeDefinition()!=null;
        boolean enabled=present&&selected.getVolumeDefinition().isStrip3DEnabled();
        boolean surface=present&&selected.getVolumeDefinition().isSurfaceDepthEnabled();boolean captured=present&&selected.getVolumeDefinition().isBakedPixelsEnabled();
        if(bone!=selected||hasVolume!=present||strip3D!=enabled||surfaceDepth!=surface||baked!=captured){bone=selected;hasVolume=present;strip3D=enabled;surfaceDepth=surface;baked=captured;rebuild();}
        for(var entry:values.entrySet()){String text=entry.getValue().get();if(!text.equals(previous.get(entry.getKey()))){entry.getKey().setText(text);previous.put(entry.getKey(),text);}}
        buttons.forEach(b->b.update(tpf));
    }
    public boolean handleMouseClick(int mx,int my) {
        if(mx<x||mx>x+width||my<y||my>y+height)return false;
        for(Button b:new ArrayList<>(buttons))if(b.handleMouseClick(mx,my))return true;
        return true;
    }
    private void rebuild() {
        root.detachAllChildren();buttons.clear();values.clear();previous.clear();
        Geometry bg=new Geometry("VolumeInspectorBackground",new Quad(width,height));Material m=new Material(app.getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color",new ColorRGBA(.12f,.14f,.18f,1));bg.setMaterial(m);bg.setLocalTranslation(x,y,-1);root.attachChild(bg);
        label("部件控制",x+12,y+height-26);
        String[] names={"实体","姿态","图像",baked?"颜色":"光照"};for(int i=0;i<4;i++){final int index=i;Button b=button(names[i],x+10+i*(width-20)/4,y+height-65,(width-26)/4,30,()->{tab=index;rebuild();});b.setActive(tab==i);}
        rowY=y+height-104;
        if(bone==null){field(()->"先导入 Blender 模型…",app::requestBlenderModel);label("0.04 米 = 1 格 · 照片四舍五入",x+12,rowY);return;}
        label(bone.getName().length()>25?bone.getName().substring(0,22)+"…":bone.getName(),x+12,rowY);rowY-=42;
        if(tab==1){
            for(String key:new String[]{"px","py","pz","rx","ry","rz","sx","sy","sz"})field(()->poseText(key),()->app.editVolumePose(key));
            label("局部骨骼姿态 · 参与关键帧",x+12,rowY);return;
        }
        if(bone.getVolumeDefinition()==null||bone.getVolumeDefinition().isSkeletonOnly()){field(()->"导入 Blender 刚性部件…",app::requestBlenderModel);return;}
        if(tab==3&&definition().isBakedPixelsEnabled()){
            field(()->"皮肤／头发／衣服调色板…",app::editPixelPalette);
            field(()->"修改部件颜色…",app::editPixelTint);
            label("换颜色只改显示，不重新拍照",x+12,rowY);return;
        }
        if(tab==3){
            field(()->"简易光照："+(definition().isLightingEnabled()?"开":"关"),()->app.volumeImageAction("lighting"));
            for(String key:new String[]{"yaw","elevation","strength"})field(()->volumeText(key),()->app.editVolumeNumber(key));
            field(()->"导入／修改三层法线",app::editStrip3DTextures);return;
        }
        if(tab==0){
            if(definition().getImportedMesh()==null){String[] shapes={"圆柱","锥台","圆锥","方块"};
            for(int i=0;i<4;i++){final int index=i;button(shapes[i],x+10+i*(width-20)/4,rowY,(width-26)/4,30,()->app.setVolumeShape(VolumeDefinition.Shape.values()[index]));}rowY-=40;}else{label("0.04 米／格 · 左键拖动预览",x+12,rowY);rowY-=38;}
            field(()->"修改长宽高…",app::editVolumeDimensions);
            for(String key:definition().getImportedMesh()==null?new String[]{"width","height","depth","ratio","density"}:new String[]{"width","height","depth"})field(()->volumeText(key),()->app.editVolumeNumber(key));
            if(!definition().isBakedPixelsEnabled()){field(()->"遮挡："+(definition().isSurfaceDepthEnabled()?"实体表面":"平面兼容"),()->app.volumeImageAction("surfaceDepth"));
            if(definition().isSurfaceDepthEnabled())field(()->"轮廓："+(definition().isSurfaceContourSnapped()?"贴图整格":"精细"),()->app.volumeImageAction("surfaceContour"));
            if(!definition().isSurfaceDepthEnabled())field(()->"包覆目标："+(definition().getCoverTarget()==null?"无":definition().getCoverTarget()),app::editVolumeCoverTarget);
            }else{field(()->"拍照角度与贴图起点…",app::editPixelCaptureSettings);field(()->"重新生成此部件",app::regeneratePixelSilhouettes);}
            label(definition().isBakedPixelsEnabled()?"固定剪影与深度 · 动画只取图":definition().isSurfaceDepthEnabled()?"实体决定遮挡 · 选框决定透明":"外层有色像素遮住指定内层",x+12,rowY);rowY-=38;
            field(()->"导入贴图 → 图像面板",()->{tab=2;rebuild();});
        }else if(definition().isBakedPixelsEnabled()){
            int[] angles={90,45,0,-45,-90};String[] views={"顶端","上斜","平视","下斜","底端"};
            for(int i=0;i<angles.length;i++){final int angle=angles[i];button(views[i],x+10+i*(width-20)/5,rowY,(width-26)/5,30,()->app.setPixelCameraElevation(angle));}rowY-=38;
            field(app::getPixelTextureSize,app::editStrip3DTextures);
            field(app::getPixelTextureStep,app::editStrip3DTextures);
            field(()->"导入整部件贴图…",app::importPixelWholeColor);
            for(var surface:com.Hecate.puppet.volume.Strip3DDefinition.Surface.values()){
                int index=surface.ordinal();String name=new String[]{"上表面","侧面","下表面"}[index];button("材质"+name,x+10+index*(width-20)/3,rowY,(width-26)/3,30,()->app.importStrip3DColor(surface));}rowY-=38;
            field(()->"手改某个角度的剪影…",app::editBakedSilhouette);
            field(()->"编辑条带选框…",app::editSelectionShape);
            field(()->"条带上下移动／行重要度…",app::editStrip3DTextures);
            field(()->"像素描边："+(definition().getStrip3D().isOutlineEnabled()?"开":"关"),()->app.volumeImageAction("outline"));
            field(()->"修改部件颜色…",app::editPixelTint);
            field(()->"拍照角度与贴图起点…",app::editPixelCaptureSettings);
            field(()->"重新生成此部件",app::regeneratePixelSilhouettes);
            field(()->"导出整人 20 方向检查图…",app::exportPixelAssemblyCheck);
            field(app::getPixelCaptureStatus,()->app.volumeImageAction("status"));
            field(()->app.getVolumeImageError()==null?"状态：正常":"状态：请检查",()->app.volumeImageAction("status"));
        }else if(definition().isStrip3DEnabled()){
            field(()->"三维条带：开",()->app.volumeImageAction("strip3d"));
            for(var surface:com.Hecate.puppet.volume.Strip3DDefinition.Surface.values()){
                int index=surface.ordinal();String name=new String[]{"上层","中层","下层"}[index];button("导入"+name+"贴图",x+10+index*(width-20)/3,rowY,(width-26)/3,30,()->app.importStrip3DColor(surface));}rowY-=38;
            field(()->"纵向："+(definition().getStrip3D().getVertical()==com.Hecate.puppet.volume.Strip3DDefinition.Sampling.SLIDE?"滑动":"透视挑行"),()->app.volumeImageAction("vertical"));
            field(()->"横向："+(definition().getStrip3D().getHorizontal()==com.Hecate.puppet.volume.Strip3DDefinition.Sampling.SLIDE?"滑动":"透视挑列"),()->app.volumeImageAction("horizontal"));
            field(()->"选框宽度："+definition().getStrip3D().getSampleWidth()+" px",app::editSelectionWidth);
            field(()->"像素描边："+(definition().getStrip3D().isOutlineEnabled()?"开":"关"),()->app.volumeImageAction("outline"));
            field(()->"绘制选框形状…",app::editSelectionShape);
            field(()->"编辑行重要度（1–3）…",app::editRowImportance);
            field(()->"贴图／法线／取样设置…",app::editStrip3DTextures);
            Button refresh=button("刷新三层贴图",x+10,rowY,(width-26)/2,30,()->app.volumeImageAction("refresh"));
            Supplier<String> status=()->app.getVolumeImageError()==null?"贴图状态：正常":"贴图异常（查看）";
            Button statusButton=button(status.get(),x+16+(width-26)/2,rowY,(width-26)/2,30,()->app.volumeImageAction("status"));
            values.put(statusButton,status);rowY-=38;
            if(rowY>y+40){label("1 正面 · 3 侧面 · 7 顶面",x+12,rowY);rowY-=24;label("小键盘 5 切换正交／透视",x+12,rowY);}
        }else{
            field(()->"三维条带：关（点击启用）",()->app.volumeImageAction("strip3d"));
            field(()->"导入条带颜色图",()->app.volumeImageAction("strip"));
            field(()->"打开整数像素选框",()->app.volumeImageAction("crop"));
            field(()->definition().getNormalTexturePath()==null?"导入法线图":"更换法线图",()->app.volumeImageAction("normal"));
            field(()->"刷新法线图",()->app.volumeImageAction("refresh"));
            field(()->"清除法线图",()->app.volumeImageAction("clearNormal"));
            field(()->"简易光照："+(definition().isLightingEnabled()?"开":"关"),()->app.volumeImageAction("lighting"));
            for(String key:new String[]{"yaw","elevation","strength"})field(()->volumeText(key),()->app.editVolumeNumber(key));
            if(rowY>y+16)label("法线 RGB = 部件局部 XYZ",x+12,rowY);
        }
    }
    private VolumeDefinition definition(){VolumeDefinition d=bone.getVolumeDefinition();return d==null?new VolumeDefinition():d;}
    private String volumeText(String key){VolumeDefinition d=definition();switch(key){
        case "width":return "宽度："+d.getWidthPx()+" px";case "height":return "高度："+d.getHeightPx()+" px";case "depth":return "长度／深度："+d.getDepthPx()+" px";
        case "ratio":return "顶部比例："+format(d.getTopRatio());case "density":return "每单位像素："+format(d.getPixelsPerUnit());
        case "yaw":return "灯光水平角："+format(d.getLightYaw());case "elevation":return "灯光仰角："+format(d.getLightElevation());default:return "灯光强度："+format(d.getLightStrength());}}
    private String poseText(String key){int i="xyz".indexOf(key.charAt(1));float v;String name;
        if(key.charAt(0)=='p'){v=bone.getLocalPosition().get(i);name="位置";}else if(key.charAt(0)=='s'){v=bone.getLocalScale().get(i);name="缩放";}else{v=bone.getLocalRotation().toAngles(null)[i]*FastMath.RAD_TO_DEG;name="旋转";}
        return name+" "+Character.toUpperCase(key.charAt(1))+"："+format(v);
    }
    private static String format(float value){return String.format(Locale.ROOT,"%.2f",value);}
    private void field(Supplier<String> text,Runnable action){Button b=button(text.get(),x+10,rowY,width-20,30,action);values.put(b,text);rowY-=38;}
    private Button button(String text,int bx,int by,int w,int h,Runnable action){Button b=ttf!=null?new Button(app,ttf,text,bx,by,w,h):new Button(app,font,text,bx,by,w,h);b.setClickListener(action::run);buttons.add(b);root.attachChild(b.getRootNode());return b;}
    private void label(String text,int lx,int ly){if(ttf!=null){Node n=ttf.createText(text,ColorRGBA.White);n.setLocalTranslation(lx,ly,1);root.attachChild(n);}else{BitmapText n=new BitmapText(font);n.setText(text);n.setSize(16);n.setLocalTranslation(lx,ly,1);root.attachChild(n);}}
}
