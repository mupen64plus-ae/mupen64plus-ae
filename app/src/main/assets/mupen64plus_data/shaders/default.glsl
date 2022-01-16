#version 100

#if defined(VERTEX)

attribute vec4 VertexCoord;
attribute vec4 TexCoord;
varying vec2 vTexPosition;

void main() {
  gl_Position = VertexCoord;
  vTexPosition = TexCoord.xy;
}

#elif defined(FRAGMENT)

precision mediump float;
uniform sampler2D Texture;
varying vec2 vTexPosition;

void main() {
  gl_FragColor = texture2D(Texture, vTexPosition);
}

#endif