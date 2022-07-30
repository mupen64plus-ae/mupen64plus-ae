/*
    Scanlines Sine Absolute Value
    An ultra light scanline shader
    by RiskyJumps
	license: public domain
*/
#version 100

/*
#pragma parameter amp          "Amplitude"      1.2500  0.000 2.000 0.05
#pragma parameter phase        "Phase"          0.5000  0.000 2.000 0.05
#pragma parameter lines_black  "Lines Blacks"   0.0000  0.000 1.000 0.05
#pragma parameter lines_white  "Lines Whites"   1.0000  0.000 2.000 0.05
*/

#define offset           0.000000
#define freq             0.500000
#define pi               3.141592654
#define amp              1.250000
#define phase            0.500000
#define lines_black      0.100000
#define lines_white      1.000000

#if defined(VERTEX)

precision highp float;

attribute vec4 VertexCoord;
attribute vec4 TexCoord;
uniform vec2 OutputSize;
varying vec4 TEX0;
varying float angle;

void main()
{
    gl_Position = VertexCoord;
    TEX0.xy = TexCoord.xy;

    float omega = 2.0 * pi * freq;              // Angular frequency
    angle = TEX0.y * omega * (480.0/2.8) + phase;
}

#elif defined(FRAGMENT)

precision highp float;

uniform sampler2D Texture;
varying vec4 TEX0;
varying float angle;

void main()
{
    vec3 color = texture2D(Texture, TEX0.xy).xyz;
    float grid;

    float lines;

    lines = sin(angle);
    lines *= amp;
    lines += offset;
    lines = abs(lines);
    lines *= lines_white - lines_black;
    lines += lines_black;
    color *= lines;

    gl_FragColor = vec4(color.xyz, 1.0);
}

#endif