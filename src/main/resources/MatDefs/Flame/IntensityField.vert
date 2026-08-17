#import "Common/ShaderLib/GLSLCompat.glsllib"

// 输入顶点属性
attribute vec3 inPosition;
attribute vec2 inTexCoord;

// 输出到片段着色器
varying vec2 texCoord;

void main() {
    // 直接传递纹理坐标
    texCoord = inTexCoord;

    // 将顶点位置转换到裁剪空间（全屏四边形）
    gl_Position = vec4(inPosition, 1.0);
}
