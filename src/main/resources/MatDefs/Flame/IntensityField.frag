#import "Common/ShaderLib/GLSLCompat.glsllib"

// 从顶点着色器传入
varying vec2 texCoord;

// 粒子参数
uniform vec2 m_CirclePosition;  // 圆心位置（屏幕空间 0-1）
uniform float m_CircleRadius;    // 半径（像素或归一化单位）
uniform float m_CircleIntensity; // 强度 0-1
uniform float m_CircleSoftness;  // 软边系数（2-3）

void main() {
    // 计算当前像素到圆心的距离
    vec2 pixelPos = texCoord;
    float dist = distance(pixelPos, m_CirclePosition);

    // 归一化距离（相对于半径）
    float normalizedDist = dist / m_CircleRadius;

    // 软边衰减函数：falloff = (1 - d)^softness
    float falloff = max(0.0, 1.0 - normalizedDist);
    falloff = pow(falloff, m_CircleSoftness);

    // 计算最终强度值
    float intensity = falloff * m_CircleIntensity;

    // 输出灰度值到红色通道（单通道纹理）
    gl_FragColor = vec4(intensity, 0.0, 0.0, 1.0);
}
