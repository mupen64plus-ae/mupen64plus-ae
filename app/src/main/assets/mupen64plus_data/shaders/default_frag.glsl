#extension GL_OES_EGL_image_external : require
precision mediump float;
uniform samplerExternalOES Texture;
varying vec2 vTexPosition;

void main() {
  gl_FragColor = texture2D(Texture, vTexPosition);
}