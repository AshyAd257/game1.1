import com.jme3.scene.shape.Quad;
import com.jme3.scene.VertexBuffer;

public class test_uv {
    public static void main(String[] args) {
        Quad quad = new Quad(2, 2);
        

        
        // 获取Position缓冲区
        VertexBuffer posBuffer = quad.getBuffer(VertexBuffer.Type.Position);
        if (posBuffer != null) {
            float[] positions = new float[12];
            posBuffer.getData().position(0);
            for (int i = 0; i < 12; i++) {
                positions[i] = ((java.nio.FloatBuffer)posBuffer.getData()).get(i);
            }
            System.out.println("Position buffer:");
            for (int i = 0; i < 4; i++) {
                System.out.println("  Vertex " + i + ": (" + positions[i*3] + ", " + positions[i*3+1] + ", " + positions[i*3+2] + ")");
            }
        }
        
        // 获取TexCoord缓冲区
        VertexBuffer texBuffer = quad.getBuffer(VertexBuffer.Type.TexCoord);
        if (texBuffer != null) {
            float[] texCoords = new float[8];
            texBuffer.getData().position(0);
            for (int i = 0; i < 8; i++) {
                texCoords[i] = ((java.nio.FloatBuffer)texBuffer.getData()).get(i);
            }
            System.out.println("\nTexCoord buffer:");
            for (int i = 0; i < 4; i++) {
                System.out.println("  Vertex " + i + ": (U=" + texCoords[i*2] + ", V=" + texCoords[i*2+1] + ")");
            }
        }
        
        // 获取Index缓冲区
        VertexBuffer indexBuffer = quad.getBuffer(VertexBuffer.Type.Index);
        if (indexBuffer != null) {
            System.out.println("\nIndex buffer:");
            indexBuffer.getData().position(0);
            int count = indexBuffer.getNumElements();
            for (int i = 0; i < count; i += 3) {
                short i0 = ((java.nio.ShortBuffer)indexBuffer.getData()).get(i);
                short i1 = ((java.nio.ShortBuffer)indexBuffer.getData()).get(i+1);
                short i2 = ((java.nio.ShortBuffer)indexBuffer.getData()).get(i+2);
                System.out.println("  Triangle " + (i/3) + ": [" + i0 + ", " + i1 + ", " + i2 + "]");
            }
        }
    }
}
