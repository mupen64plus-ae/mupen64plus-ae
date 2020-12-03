/*
    Scanlines Sine Absolute Value
    An ultra light scanline shader
    by RiskyJumps
	license: public domain
*/

#define freq             0.500000
#define pi               3.141592654

#define amp              1.250000
#define phase            0.500000

precision highp float;

attribute vec4 VertexCoord;
attribute vec4 TexCoord;
uniform vec2 TextureSize;
varying vec4 TEX0;
varying float angle;

void main()
{
    gl_Position = VertexCoord;
    TEX0.xy = TexCoord.xy;

    float omega = 2.0 * pi * freq;              // Angular frequency
    angle = TEX0.y * omega * (TextureSize.y/2.8) + phase;
}

