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
