#version 100

#if defined(VERTEX)

attribute vec4 VertexCoord;
attribute vec4 TexCoord;
varying vec2 TexCoordTest;

precision highp float;

void main()
{
    gl_Position = vec4( VertexCoord.xy, 0.0, 1.0 );
    gl_Position = sign( gl_Position );
    TexCoordTest = (vec2( gl_Position.x, gl_Position.y ) + vec2( 1.0 ) ) / vec2( 2.0 );
}

#elif defined(FRAGMENT)

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

#endif