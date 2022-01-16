#version 100

#if defined(VERTEX)

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

#elif defined(FRAGMENT)
precision mediump float;
varying vec2 vTexPosition;

uniform mediump vec2 TextureSize;
uniform sampler2D Texture;

void main() {
  float limit = 0.8;
  float factor = 0.33;
  float finalFactor = 0.33;

  vec2 fragCoord = vTexPosition * TextureSize;
  vec2 inverseVP = vec2(1.0) / TextureSize;
  vec2 rgbT = (fragCoord + vec2(0.0, -1.0)) * inverseVP;
  vec2 rgbB = (fragCoord + vec2(0.0, 1.0)) * inverseVP;
  vec2 rgbM = vTexPosition;

  vec3 col = vec3(0.0);
  vec3 colT = texture2D(Texture, rgbT).xyz;
  vec3 colM = texture2D(Texture, rgbM).xyz;
  vec3 colB = texture2D(Texture, rgbB).xyz;

  if(abs(colM.r - colT.r) < limit && abs(colM.g - colT.g) < limit && abs(colM.b - colT.b) < limit)
    col += colT * factor;
  else
    col += colM * factor;

  if(abs(colM.r - colB.r) < limit && abs(colM.g - colB.g) < limit && abs(colM.b - colB.b) < limit)
    col += colB * factor;
  else
    col += colM * factor;

  col += colM * finalFactor;

  gl_FragColor = vec4(col.r,col.g,col.b,1.0);
}

#endif