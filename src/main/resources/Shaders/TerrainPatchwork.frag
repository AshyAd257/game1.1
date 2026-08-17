#import "Common/ShaderLib/GLSLCompat.glsllib"

// 从顶点着色器传入
varying vec3 vWorldPos;
flat varying vec3 vNormal;  // flat禁止插值，每个三角形使用同一法线
varying vec2 vTexCoord;

// 泥土纹理
#ifdef HAS_DIRT_TEXTURES
    uniform sampler2D m_Dirt1Texture;
    uniform sampler2D m_Dirt2Texture;
    uniform sampler2D m_Dirt3Texture;

    uniform sampler2D m_Grs1Texture;
    uniform sampler2D m_Grs2Texture;
    uniform sampler2D m_Grs3Texture;
    uniform sampler2D m_Grs4Texture;
    uniform sampler2D m_Grs5Texture;
    uniform sampler2D m_Grs6Texture;
    uniform sampler2D m_Grs7Texture;
    uniform sampler2D m_Grs8Texture;
    uniform sampler2D m_Grs9Texture;
    uniform sampler2D m_Grs10Texture;
#endif

// 其他纹理
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
    // 使用世界坐标XZ平面作为纹理坐标（实现无缝重复）
    vec2 worldUV = vWorldPos.xz;

    // 计算当前方格的ID（每个1x1单位是一个方格）
    vec2 gridCell = floor(worldUV);

    // 基于方格ID生成随机哈希值
    float cellHash = hash(gridCell);

    // 基础颜色（每个方格随机变化）
    vec3 colorVariation = vec3(
        cellHash * 0.3,           // 红色通道变化
        0.5 + cellHash * 0.3,     // 绿色通道变化
        0.3 + cellHash * 0.2      // 蓝色通道变化
    );

    vec4 baseColor = vec4(colorVariation, 1.0);

    // 如果有纹理，则覆盖颜色
    #ifdef HAS_DIRT_TEXTURES
    if (m_MaterialType == 0) {
        // 根据Y坐标决定使用dirt还是grs纹理
        // Y > 0 (地面以上): 使用grs纹理 (10张)
        // Y <= 0 (地下): 使用dirt纹理 (3张)

        if (vWorldPos.y > 0.0) {
            // 顶层：从10张grs纹理中随机选择
            int texIndex = int(cellHash * 10.0);

            if (texIndex == 0) {
                baseColor = texture2D(m_Grs1Texture, worldUV);
            } else if (texIndex == 1) {
                baseColor = texture2D(m_Grs2Texture, worldUV);
            } else if (texIndex == 2) {
                baseColor = texture2D(m_Grs3Texture, worldUV);
            } else if (texIndex == 3) {
                baseColor = texture2D(m_Grs4Texture, worldUV);
            } else if (texIndex == 4) {
                baseColor = texture2D(m_Grs5Texture, worldUV);
            } else if (texIndex == 5) {
                baseColor = texture2D(m_Grs6Texture, worldUV);
            } else if (texIndex == 6) {
                baseColor = texture2D(m_Grs7Texture, worldUV);
            } else if (texIndex == 7) {
                baseColor = texture2D(m_Grs8Texture, worldUV);
            } else if (texIndex == 8) {
                baseColor = texture2D(m_Grs9Texture, worldUV);
            } else {
                baseColor = texture2D(m_Grs10Texture, worldUV);
            }
        } else {
            // 底层：从3张dirt纹理中随机选择
            int texIndex = int(cellHash * 3.0);

            if (texIndex == 0) {
                baseColor = texture2D(m_Dirt1Texture, worldUV);
            } else if (texIndex == 1) {
                baseColor = texture2D(m_Dirt2Texture, worldUV);
            } else {
                baseColor = texture2D(m_Dirt3Texture, worldUV);
            }
        }

        // 添加轻微的亮度变化（不改变色相）
        float variation = (cellHash - 0.5) * 0.1;
        baseColor.rgb *= (1.0 + variation);
    }
    #endif

    #ifdef HAS_SAND_TEXTURE
    if (m_MaterialType == 1) {
        baseColor = texture2D(m_SandTexture, worldUV);
        float variation = (cellHash - 0.5) * 0.15;
        baseColor.rgb *= (1.0 + variation);
    }
    #endif

    #ifdef HAS_WATER_TEXTURE
    if (m_MaterialType == 2) {
        vec2 flowUV = worldUV + vec2(m_Time * m_WaterFlowSpeed, 0.0);
        baseColor = texture2D(m_WaterTexture, flowUV);
    }
    #endif

    // 重新计算几何法线（flat shading）
    vec3 dPosX = dFdx(vWorldPos);
    vec3 dPosY = dFdy(vWorldPos);
    vec3 geometricNormal = normalize(cross(dPosX, dPosY));

    // 光照
    vec3 lightDir = normalize(vec3(0.5, 1.0, 0.3));
    float diffuse = max(dot(geometricNormal, lightDir), 0.3);
    baseColor.rgb *= diffuse;

    // 确保不透明
    baseColor.a = 1.0;

    gl_FragColor = baseColor;
}
