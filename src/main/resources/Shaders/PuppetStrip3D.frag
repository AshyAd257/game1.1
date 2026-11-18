#import "Common/ShaderLib/GLSLCompat.glsllib"
uniform sampler2D m_ColorMap;
uniform sampler2D m_NormalMap;
uniform vec2 m_TexelSize;
uniform mat3 m_NormalRotation;
uniform bool m_UseLighting;
uniform bool m_Selected;
uniform vec3 m_LightDirection;
uniform float m_LightStrength;
#ifndef COVER_COUNT
#define COVER_COUNT 0
#endif
uniform vec4 m_CoverViewport;
uniform mat3 m_ArtScreenToUV;
uniform bool m_UseArtProjection;
#if COVER_COUNT > 0
uniform sampler2D m_CoverMap0;
uniform mat3 m_CoverScreenToUV0;
uniform vec3 m_CoverDepth0;
#endif
#if COVER_COUNT > 1
uniform sampler2D m_CoverMap1;
uniform mat3 m_CoverScreenToUV1;
uniform vec3 m_CoverDepth1;
#endif
#if COVER_COUNT > 2
uniform sampler2D m_CoverMap2;
uniform mat3 m_CoverScreenToUV2;
uniform vec3 m_CoverDepth2;
#endif
#if COVER_COUNT > 3
uniform sampler2D m_CoverMap3;
uniform mat3 m_CoverScreenToUV3;
uniform vec3 m_CoverDepth3;
#endif
#if COVER_COUNT > 4
uniform sampler2D m_CoverMap4;
uniform mat3 m_CoverScreenToUV4;
uniform vec3 m_CoverDepth4;
#endif
#if COVER_COUNT > 5
uniform sampler2D m_CoverMap5;
uniform mat3 m_CoverScreenToUV5;
uniform vec3 m_CoverDepth5;
#endif
#if COVER_COUNT > 6
uniform sampler2D m_CoverMap6;
uniform mat3 m_CoverScreenToUV6;
uniform vec3 m_CoverDepth6;
#endif
#if COVER_COUNT > 7
uniform sampler2D m_CoverMap7;
uniform mat3 m_CoverScreenToUV7;
uniform vec3 m_CoverDepth7;
#endif
in vec2 uv;
out vec4 outColor;
// Remove only inner texels actually covered by a visible outer texel. The outer
// card keeps its ordinary depth test against every unrelated part and the world.
vec2 pixelUV(sampler2D map,vec2 st){
    vec2 size=vec2(textureSize(map,0));
    return (floor(st*size+vec2(0.0001))+vec2(0.5))/size;
}
bool covered(sampler2D map,mat3 screenToUV,vec3 depth,vec3 screen){
    vec3 q=screenToUV*screen;
    if(q.z<=0.0)return false;
    vec2 st=q.xy/q.z;float z=dot(depth,screen);
    if(z < -1.0 || z > 1.0 || any(lessThan(st,vec2(0.0))) || any(greaterThanEqual(st,vec2(1.0))))return false;
    // Cover tests diverge within a pixel quad; explicit LOD avoids undefined implicit derivatives.
    return textureLod(map,pixelUV(map,st),0.0).a>=0.5;
}
#import "Shaders/PuppetSurfaceDepth.glsllib"
void main(){
#ifdef SURFACE_DEPTH
    renderSurface();return;
#else
    gl_FragDepth=gl_FragCoord.z;
    vec3 screen=vec3((gl_FragCoord.xy-m_CoverViewport.xy)/max(m_CoverViewport.zw,vec2(1.0))*2.0-1.0,1.0);
#if COVER_COUNT > 0
    if(covered(m_CoverMap0,m_CoverScreenToUV0,m_CoverDepth0,screen))discard;
#endif
#if COVER_COUNT > 1
    if(covered(m_CoverMap1,m_CoverScreenToUV1,m_CoverDepth1,screen))discard;
#endif
#if COVER_COUNT > 2
    if(covered(m_CoverMap2,m_CoverScreenToUV2,m_CoverDepth2,screen))discard;
#endif
#if COVER_COUNT > 3
    if(covered(m_CoverMap3,m_CoverScreenToUV3,m_CoverDepth3,screen))discard;
#endif
#if COVER_COUNT > 4
    if(covered(m_CoverMap4,m_CoverScreenToUV4,m_CoverDepth4,screen))discard;
#endif
#if COVER_COUNT > 5
    if(covered(m_CoverMap5,m_CoverScreenToUV5,m_CoverDepth5,screen))discard;
#endif
#if COVER_COUNT > 6
    if(covered(m_CoverMap6,m_CoverScreenToUV6,m_CoverDepth6,screen))discard;
#endif
#if COVER_COUNT > 7
    if(covered(m_CoverMap7,m_CoverScreenToUV7,m_CoverDepth7,screen))discard;
#endif

    vec3 cardUV=m_ArtScreenToUV*screen;
    vec2 texelUV=pixelUV(m_ColorMap,m_UseArtProjection?cardUV.xy/cardUV.z:uv);
    vec4 color=texture(m_ColorMap,texelUV);
    if(color.a<0.5){
        if(m_Selected){
            float neighbor=max(max(texture(m_ColorMap,texelUV+vec2(m_TexelSize.x,0.0)).a,texture(m_ColorMap,texelUV-vec2(m_TexelSize.x,0.0)).a),max(texture(m_ColorMap,texelUV+vec2(0.0,m_TexelSize.y)).a,texture(m_ColorMap,texelUV-vec2(0.0,m_TexelSize.y)).a));
            if(neighbor>=0.5){outColor=vec4(0.35,0.8,1.0,1.0);return;}
        }
        discard;
    }
    if(m_UseLighting){vec3 n=texture(m_NormalMap,texelUV).rgb*2.0-1.0;n=normalize(m_NormalRotation*n);color.rgb*=0.35+max(dot(n,normalize(m_LightDirection)),0.0)*m_LightStrength;}
    outColor=vec4(color.rgb,1.0);
#endif
}
