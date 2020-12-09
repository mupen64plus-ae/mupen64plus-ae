uniform mediump vec2 OutputSize;
uniform mediump vec2 TextureSize;
uniform mediump vec2 InputSize;
attribute vec4 VertexCoord;
attribute vec4 TexCoord;
precision highp float;

varying vec2 vTexPosition;


void main(void) {
  gl_Position = VertexCoord;
  vTexPosition = TexCoord.xy;
}