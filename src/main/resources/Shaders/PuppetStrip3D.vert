#import "Common/ShaderLib/GLSLCompat.glsllib"
uniform mat4 g_ViewProjectionMatrix;
#ifdef SURFACE_DEPTH
uniform vec4 m_ScreenBounds;
#endif
in vec3 inPosition;
in vec2 inTexCoord;
out vec2 uv;
void main(){
    uv=inTexCoord;
#ifdef SURFACE_DEPTH
    gl_Position=vec4(mix(m_ScreenBounds.xy,m_ScreenBounds.zw,inTexCoord),0.0,1.0);
#else
    gl_Position=g_ViewProjectionMatrix*vec4(inPosition,1.0);
#endif
}
