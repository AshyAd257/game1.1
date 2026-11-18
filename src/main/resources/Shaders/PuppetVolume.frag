#import "Common/ShaderLib/GLSLCompat.glsllib"
uniform vec4 m_Color;
uniform sampler2D m_ColorMap;
uniform sampler2D m_NormalMap;
uniform mat3 m_PartRotation;
uniform vec3 m_LightDirection;
uniform float m_LightStrength;
varying vec2 texCoord;
void main() {
    vec4 color = m_Color;
    #ifdef HAS_COLOR
        color *= texture2D(m_ColorMap, texCoord);
    #endif
    if (color.a < 0.1) discard;
    #ifdef USE_LIGHTING
        vec3 normal = vec3(0.0, 0.0, 1.0);
        #ifdef HAS_NORMAL
            vec3 sampled = texture2D(m_NormalMap, texCoord).rgb * 2.0 - 1.0;
            if (dot(sampled, sampled) > 0.00001) normal = normalize(sampled);
        #endif
        normal = normalize(m_PartRotation * normal);
        float diffuse = max(0.0, dot(normal, normalize(m_LightDirection)));
        color.rgb *= clamp(0.35 + diffuse * m_LightStrength, 0.0, 2.0);
    #endif
    gl_FragColor = color;
}
