#version 100
precision highp float;

uniform sampler2D Texture;
varying vec2 TexCoordTest;
uniform vec2 TextureSize;

void main()
{
    float total = floor(TexCoordTest.x*TextureSize.x/100.0) + floor(TexCoordTest.y*TextureSize.y/100.0);
    bool isEven = mod(total,2.0)==0.0;
    vec4 col1 = vec4(0.0,0.0,0.0,1.0);
    vec4 col2 = vec4(1.0,1.0,1.0,1.0);
    gl_FragColor = (isEven)? col1:col2;
}