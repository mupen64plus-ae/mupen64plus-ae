precision mediump float;
uniform sampler2D Texture;
varying vec2 vTexPosition;

void main() {
  gl_FragColor = texture2D(Texture, vTexPosition);
}