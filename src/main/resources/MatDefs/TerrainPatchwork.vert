#import "Common/ShaderLib/GLSLCompat.glsllib"

// 顶点属性
attribute vec3 inPosition;
attribute vec3 inNormal;
attribute vec2 inTexCoord;

// 输出到片段着色器
varying vec3 vWorldPos;      // 世界坐标（用于无缝纹理）
varying vec3 vNormal;        // 法线
varying vec2 vTexCoord;      // 纹理坐标

// 系统矩阵
uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldMatrix;
uniform mat3 g_NormalMatrix;

void main() {
    // 计算世界坐标位置
    vec4 worldPos = g_WorldMatrix * vec4(inPosition, 1.0);
    vWorldPos = worldPos.xyz;

    // 变换法线到世界空间
    vNormal = normalize(g_NormalMatrix * inNormal);

    // 传递纹理坐标
    vTexCoord = inTexCoord;

    // 计算最终顶点位置
    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);
}
