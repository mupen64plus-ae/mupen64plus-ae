#version 100

attribute vec4 VertexCoord;
attribute vec4 TexCoord;
varying vec2 vTexPosition;

void main() {
  gl_Position = VertexCoord;
  vTexPosition = TexCoord.xy;
}