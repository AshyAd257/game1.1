uniform sampler2D m_ColorMap;
uniform mat3 m_ArtScreenToUV;
uniform vec4 m_Viewport;
uniform bool m_UseEnclosingSelection;
uniform sampler2D m_EnclosingSelection;
uniform mat3 m_EnclosingProjection;
out vec4 outColor;
void main(){
    vec3 screen=vec3((gl_FragCoord.xy-m_Viewport.xy)/m_Viewport.zw*2.0-1.0,1.0);
    if(m_UseEnclosingSelection){
        vec3 selection=m_EnclosingProjection*screen;
        if(textureLod(m_EnclosingSelection,selection.xy/selection.z,0.0).a<.5)discard;
    }
    vec3 q=m_ArtScreenToUV*screen;vec2 st=q.xy/q.z;
    if(any(lessThan(st,vec2(0.0)))||any(greaterThanEqual(st,vec2(1.0))))discard;
    vec2 size=vec2(textureSize(m_ColorMap,0));st=(floor(st*size+vec2(.0001))+vec2(.5))/size;
    vec4 color=textureLod(m_ColorMap,st,0.0);if(color.a<.5)discard;
    outColor=vec4(color.rgb,1.0);
}
