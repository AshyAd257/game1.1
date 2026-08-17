#import "Common/ShaderLib/GLSLCompat.glsllib"

// 从顶点着色器传入
varying vec2 texCoord;

// 纹理
uniform sampler2D m_IntensityMap;
uniform sampler2D m_ColorGradient;

// 参数
uniform float m_SmokeThreshold;
uniform float m_Exposure;
uniform float m_Time;
uniform float m_NoiseFreq;
uniform float m_NoiseSpeed;
uniform float m_NoiseAmp;
uniform vec4 m_DarkGray;
uniform vec4 m_LightGray;

/**
 * 简单噪声函数（基于 sin 的伪随机）
 * 返回 0-1 范围的值
 */
float noise(vec2 uv) {
    return fract(sin(dot(uv, vec2(12.9898, 78.233))) * 43758.5453);
}

/**
 * 平滑噪声（双线性插值）
 */
float smoothNoise(vec2 uv) {
    vec2 i = floor(uv);
    vec2 f = fract(uv);

    // 四个角的噪声值
    float a = noise(i);
    float b = noise(i + vec2(1.0, 0.0));
    float c = noise(i + vec2(0.0, 1.0));
    float d = noise(i + vec2(1.0, 1.0));

    // 平滑插值
    vec2 u = f * f * (3.0 - 2.0 * f); // smoothstep
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

/**
 * 分形噪声（多层叠加）
 */
float fractalNoise(vec2 uv) {
    float value = 0.0;
    float amplitude = 0.5;

    for (int i = 0; i < 3; i++) {
        value += smoothNoise(uv) * amplitude;
        uv *= 2.0;
        amplitude *= 0.5;
    }

    return value;
}

void main() {
    vec2 uv = texCoord;

    // 步骤1: 添加噪声扰动（制造流动效果）
    float n = fractalNoise(uv * m_NoiseFreq + m_Time * m_NoiseSpeed);
    vec2 distortion = vec2(n - 0.5) * m_NoiseAmp;
    uv = uv + distortion;

    // 步骤2: 采样强度图
    float h = texture2D(m_IntensityMap, uv).r;

    // 应用曝光
    h = clamp(h * m_Exposure, 0.0, 1.0);

    // 步骤3: 根据强度值选择颜色
    vec3 color;

    if (h < m_SmokeThreshold) {
        // 烟雾区域：灰色渐变
        float t = h / m_SmokeThreshold;
        color = mix(m_DarkGray.rgb, m_LightGray.rgb, t);
    } else {
        // 火焰区域：从颜色条纹图取色
        // 将强度映射到 0-1 范围
        float gradientPos = (h - m_SmokeThreshold) / (1.0 - m_SmokeThreshold);
        color = texture2D(m_ColorGradient, vec2(gradientPos, 0.5)).rgb;
    }

    // 步骤4: 输出颜色，alpha 通道用强度值
    float alpha = h; // 强度越高越不透明
    gl_FragColor = vec4(color, alpha);
}
