uniform sampler2D m_DepthAtlas, m_EditAtlas, m_TextureRows, m_Palette, m_Color0, m_Color1, m_Color2;
uniform vec4 m_Viewport, m_ViewChoice, m_Source0, m_Source1, m_Source2, m_Tint;
uniform vec3 m_ViewRight0, m_ViewRight1, m_ViewUp0, m_ViewUp1, m_ViewToward0, m_ViewToward1;
uniform vec4 m_ViewPlane0, m_ViewPlane1, m_ViewPerspective0, m_ViewPerspective1;
uniform vec2 m_ViewSampleCentre0, m_ViewSampleCentre1;
uniform mat3 m_ScreenToUV0, m_ScreenToUV1;
uniform mat4 m_LocalToClip;
uniform vec2 m_TextureOrigin, m_AtlasTiles;
uniform float m_AtlasSize, m_YawViews, m_Radius, m_PixelStep, m_Priority, m_PaletteCount;
uniform bool m_Selected, m_Outline, m_HasEditedAtlas;
out vec4 fragColor;
vec4 sampleCell(vec2 cell, out vec4 depthData, out bool upper) {
    if(any(lessThan(cell,vec2(0.0)))||any(greaterThanEqual(cell,vec2(m_AtlasSize)))){depthData=vec4(0.0);upper=false;return vec4(0.0);}
    float row=m_AtlasSize-1.0-cell.y;
    upper=row<m_ViewChoice.w;
    float view=(upper?m_ViewChoice.z:m_ViewChoice.y)*m_YawViews+m_ViewChoice.x;
    vec2 tile=vec2(mod(view,m_AtlasTiles.x),floor(view/m_AtlasTiles.x));
    vec2 atlasUV=(tile+(cell+0.5)/m_AtlasSize)/m_AtlasTiles;
    depthData=m_HasEditedAtlas?texture(m_EditAtlas,atlasUV):texture(m_DepthAtlas,atlasUV);
    if(depthData.a<0.1)return vec4(0.0);
    float layer=floor(depthData.a*255.0/64.0+0.01)-1.0;
    vec4 source=layer<0.5?m_Source0:layer<1.5?m_Source1:m_Source2;
    vec2 texel=vec2(cell.x,row)-(upper?m_ViewSampleCentre1:m_ViewSampleCentre0)+m_TextureOrigin+floor(vec2(source.x/8.0,source.y/2.0))+source.zw+vec2(floor(m_ViewChoice.x*source.x/m_YawViews),0.0);
    vec4 rowData=texture(m_TextureRows,vec2((row+0.5)/m_AtlasSize,(layer+0.5)/3.0));
    if(rowData.a<0.5)return vec4(0.0);
    int column=int(floor(texel.x+0.5)),period=int(source.x);
    if(column<0)column+=((-column)/period+1)*period;
    texel.x=float(column%period);texel.y=dot(floor(rowData.rg*255.0+0.5),vec2(256.0,1.0));
    vec2 uv=vec2((texel.x+0.5)/source.x,1.0-(texel.y+0.5)/source.y);
    return layer<0.5?texture(m_Color0,uv):layer<1.5?texture(m_Color1,uv):texture(m_Color2,uv);
}
void main(){
    vec2 screen=(floor((gl_FragCoord.xy-m_Viewport.xy)/m_PixelStep)+0.5)*m_PixelStep;
    vec2 ndc=screen/m_Viewport.zw*2.0-1.0;
    vec3 h0=m_ScreenToUV0*vec3(ndc,1.0),h1=m_ScreenToUV1*vec3(ndc,1.0);
    vec2 c0=floor(h0.xy/h0.z*m_AtlasSize+0.0001),c1=floor(h1.xy/h1.z*m_AtlasSize+0.0001);
    bool lowerValid=m_AtlasSize-1.0-c0.y>=m_ViewChoice.w;
    bool upperValid=m_AtlasSize-1.0-c1.y<m_ViewChoice.w;
    if(!lowerValid&&!upperValid)discard;
    vec2 cell=lowerValid?c0:c1;
    vec4 depthData;bool upper;vec4 color=sampleCell(cell,depthData,upper);
    bool edge=false;
    if(color.a<0.5){
        if(!m_Outline&&!m_Selected)discard;
        vec2 offsets[4]=vec2[4](vec2(-1,0),vec2(1,0),vec2(0,-1),vec2(0,1));
        for(int i=0;i<4;i++){vec4 neighbour;bool nextUpper;vec4 next=sampleCell(cell+offsets[i],neighbour,nextUpper);if(next.a>=0.5){cell+=offsets[i];depthData=neighbour;upper=nextUpper;edge=true;break;}}
        if(!edge)discard;
        color=m_Selected?vec4(0.1,0.8,1.0,1.0):vec4(0.08,0.07,0.1,1.0);
    }
    float depth=(dot(floor(depthData.rgb*255.0+0.5),vec3(65536.0,256.0,1.0))/16777215.0*2.0-1.0)*m_Radius;
    vec3 r=upper?m_ViewRight1:m_ViewRight0,u=upper?m_ViewUp1:m_ViewUp0,t=upper?m_ViewToward1:m_ViewToward0;
    vec2 pos=((cell+0.5)/m_AtlasSize-0.5)*2.0*m_Radius;
    vec4 planeData=upper?m_ViewPlane1:m_ViewPlane0;pos=pos*planeData.xy+planeData.zw;
    vec4 perspective=upper?m_ViewPerspective1:m_ViewPerspective0;
    pos=perspective.xy+(pos-perspective.xy)*(1.0-(depth-perspective.z)*perspective.w);
    vec4 clip=m_LocalToClip*vec4(r*pos.x+u*pos.y+t*depth,1.0);
    if(clip.w<=0.0)discard;
    float z=clip.z/clip.w*0.5+0.5;
    if(z<0.0||z>1.0)discard;
    // A tiny, deterministic depth bias resolves joint ties without whole-part sorting.
    gl_FragDepth=clamp(z-m_Priority*0.0000001,0.0,1.0);
    if(!edge)for(int i=0;i<16;i++){if(float(i)>=m_PaletteCount)break;vec3 original=texture(m_Palette,vec2((float(i)+0.5)/16.0,0.25)).rgb;if(all(lessThan(abs(color.rgb-original),vec3(0.0001)))){color.rgb=texture(m_Palette,vec2((float(i)+0.5)/16.0,0.75)).rgb;break;}}
    fragColor=vec4(edge?color.rgb:color.rgb*m_Tint.rgb,1.0);
}

