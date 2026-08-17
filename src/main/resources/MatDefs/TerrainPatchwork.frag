#import "Common/ShaderLib/GLSLCompat.glsllib"

// 从顶点着色器传入
varying vec3 vWorldPos;
varying vec3 vNormal;
varying vec2 vTexCoord;

// 纹理
#ifdef HAS_DIRT_TEXTURE
    uniform sampler2D m_DirtTexture;
#endif
#ifdef HAS_SAND_TEXTURE
    uniform sampler2D m_SandTexture;
#endif
#ifdef HAS_WATER_TEXTURE
    uniform sampler2D m_WaterTexture;
#endif

// 参数
uniform int m_MaterialType;
uniform float m_PatchScale;
uniform float m_DecorationDensity;
uniform float m_DecorationScale;
uniform float m_Time;
uniform float m_WaterFlowSpeed;

// ========== 噪声函数 ==========

// 2D哈希函数
float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

// 2D Perlin噪声
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f); // 平滑插值

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// 分形布朗运动 (多层噪声)
float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;

    for (int i = 0; i < 3; i++) {
        value += amplitude * noise(p * frequency);
        amplitude *= 0.5;
        frequency *= 2.0;
    }

    return value;
}

// Voronoi噪声（用于拼块边界）
float voronoi(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);

    float minDist = 1.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 neighbor = vec2(float(x), float(y));
            vec2 point = hash(i + neighbor) * vec2(1.0, 1.0) + neighbor;
            float dist = length(point - f);
            minDist = min(minDist, dist);
        }
    }

    return minDist;
}

// ========== 装饰图案 ==========

// 生成点状装饰
float decorationDots(vec2 p) {
    vec2 gridPos = fract(p * m_DecorationScale);
    vec2 cellId = floor(p * m_DecorationScale);

    // 使用哈希决定是否在此格子放置点
    if (hash(cellId) > m_DecorationDensity) {
        return 0.0;
    }

    // 在格子中心放置小点
    vec2 center = vec2(0.5, 0.5);
    float dist = length(gridPos - center);
    return smoothstep(0.15, 0.05, dist);
}

// 生成短线装饰
float decorationLines(vec2 p) {
    vec2 cellId = floor(p * m_DecorationScale);
    vec2 cellPos = fract(p * m_DecorationScale);

    // 使用哈希决定是否在此格子放置线
    float h = hash(cellId);
    if (h > m_DecorationDensity * 0.5) {
        return 0.0;
    }

    // 随机方向的短线
    float angle = h * 6.28318; // 0-2π
    vec2 lineDir = vec2(cos(angle), sin(angle));
    vec2 center = vec2(0.5, 0.5);
    vec2 toCenter = cellPos - center;

    // 计算到线段的距离
    float alongLine = dot(toCenter, lineDir);
    float perpDist = abs(dot(toCenter, vec2(-lineDir.y, lineDir.x)));

    if (abs(alongLine) < 0.2 && perpDist < 0.02) {
        return 1.0;
    }

    return 0.0;
}

// ========== 主函数 ==========

void main() {
    // 使用世界坐标XZ平面作为纹理坐标（实现无缝）
    vec2 worldUV = vWorldPos.xz;

    // 基础颜色 - 改为淡绿色系
    vec4 baseColor = vec4(1.0, 1.0, 1.0, 1.0);

    // 根据材质类型选择颜色（暂时不使用纹理，使用程序化颜色）
    if (m_MaterialType == 0) {
        // DIRT - 淡绿色草地
        baseColor = vec4(0.65, 0.85, 0.75, 1.0); // 薄荷绿
    } else if (m_MaterialType == 1) {
        // SAND - 淡黄绿色
        baseColor = vec4(0.75, 0.88, 0.70, 1.0); // 浅黄绿
    } else if (m_MaterialType == 2) {
        // WATER - 淡蓝绿色
        baseColor = vec4(0.60, 0.82, 0.80, 0.8); // 淡青色半透明
    }

    // 拼布效果：使用Voronoi噪声创建块状感（增强对比）
    float patchNoise = voronoi(worldUV / m_PatchScale);
    float patchBrightness = 0.75 + patchNoise * 0.5; // 0.75-1.25（增强对比）

    // 叠加细节噪声（模拟草地纹理）
    float detailNoise = fbm(worldUV * 4.0);
    patchBrightness += (detailNoise - 0.5) * 0.15;

    // 应用拼布亮度变化
    baseColor.rgb *= patchBrightness;

    // 添加装饰图案（点和短线）
    float dots = decorationDots(worldUV);
    float lines = decorationLines(worldUV);
    float decoration = max(dots, lines);

    // 装饰颜色（亮色 - 白色、粉色、淡黄色随机）
    float decorationHue = hash(floor(worldUV * m_DecorationScale));
    vec3 decorationColor;
    if (decorationHue < 0.33) {
        decorationColor = vec3(0.95, 0.95, 0.98); // 白色
    } else if (decorationHue < 0.66) {
        decorationColor = vec3(0.95, 0.75, 0.85); // 粉色
    } else {
        decorationColor = vec3(0.95, 0.92, 0.75); // 淡黄色
    }
    baseColor.rgb = mix(baseColor.rgb, decorationColor, decoration * 0.8);

    // 柔和的光照（降低对比度）
    vec3 lightDir = normalize(vec3(0.5, 1.0, 0.3));
    float diffuse = max(dot(vNormal, lightDir), 0.6); // 高环境光，柔和阴影
    baseColor.rgb *= diffuse;

    // 淡色块：整体提亮（增强淡色效果）
    baseColor.rgb = mix(baseColor.rgb, vec3(1.0), 0.25);

    gl_FragColor = baseColor;
}
