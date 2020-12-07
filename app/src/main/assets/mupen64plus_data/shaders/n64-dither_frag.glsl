precision mediump float;
varying vec2 v_rgbT;
varying vec2 v_rgbB;
varying vec2 v_rgbM;

uniform mediump vec2 TextureSize;
uniform sampler2D Texture;

void main() {
  float limit = 0.2;
  vec3 col = vec3(0.0);
  vec3 colT = texture2D(Texture, v_rgbT).xyz;
  vec3 colM = texture2D(Texture, v_rgbM).xyz;
  vec3 colB = texture2D(Texture, v_rgbB).xyz;
  if(abs(colM.r - colT.r) < limit && abs(colM.g - colT.g) < limit && abs(colM.b - colT.b) < limit)
    col += colT * 0.25;
  else
    col += colM * 0.25;

  if(abs(colM.r - colB.r) < limit && abs(colM.g - colB.g) < limit && abs(colM.b - colB.b) < limit)
    col += colB * 0.25;
  else
    col += colM * 0.25;

  col += colM * 0.5;

  gl_FragColor = vec4(col.r,col.g,col.b,1.0);
}
