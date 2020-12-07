uniform mediump vec2 OutputSize;
uniform mediump vec2 TextureSize;
uniform mediump vec2 InputSize;
attribute vec4 VertexCoord;
attribute vec4 TexCoord;

precision mediump float;

//texcoords computed in vertex step
//to avoid dependent texture reads
varying vec2 v_rgbT;
varying vec2 v_rgbB;
varying vec2 v_rgbM;

void main(void) {
  gl_Position = VertexCoord;

  //compute the texture coords and send them to varyingn
  vec2 inverseVertexCoord = VertexCoord.xy;
  inverseVertexCoord.y = -inverseVertexCoord.y;
  vec2 vUv = (inverseVertexCoord.xy + 1.0) * 0.5;
  vec2 fragCoord = vUv * TextureSize;
  vec2 inverseVP = vec2(1.0) / TextureSize;
  v_rgbT = (fragCoord + vec2(0.0, -1.0)) * inverseVP;
  v_rgbB = (fragCoord + vec2(0.0, 1.0)) * inverseVP;
  v_rgbM = vec2(fragCoord * inverseVP);
}