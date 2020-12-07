// Ported from https://raw.githubusercontent.com/gizmo98/common-shaders/master/blurs/blur9x9.glsl

attribute vec4 VertexCoord;
attribute vec4 TexCoord;
varying vec4 TEX0;
varying vec4 TEX1;

uniform mediump vec2 OutputSize;
uniform mediump vec2 TextureSize;
uniform mediump vec2 InputSize;
void main()
{
    vec2 _dxdy_scale;
    _dxdy_scale = InputSize/OutputSize;
    gl_Position = VertexCoord;
    TEX0.xy = TexCoord.xy;
    TEX1.xy = _dxdy_scale/TextureSize;
}
