uniform vec4 m_Bounds;
in vec3 inPosition;
void main(){gl_Position=vec4(mix(m_Bounds.xy,m_Bounds.zw,inPosition.xy),0.0,1.0);}
