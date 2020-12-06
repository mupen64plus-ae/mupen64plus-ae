/*
    CRT-interlaced

    Copyright (C) 2010-2012 cgwg, Themaister and DOLLS

    This program is free software; you can redistribute it and/or modify it
    under the terms of the GNU General Public License as published by the Free
    Software Foundation; either version 2 of the License, or (at your option)
    any later version.

    (cgwg gave their consent to have the original version of this shader
    distributed under the GPL in this message:

        http://board.byuu.org/viewtopic.php?p=26075#p26075

        "Feel free to distribute my shaders under the GPL. After all, the
        barrel distortion code was taken from the Curvature shader, which is
        under the GPL."
    )
	This shader variant is pre-configured with screen curvature
*/
/*
#pragma parameter CRTgamma "CRTGeom Target Gamma" 2.4 0.1 5.0 0.1
#pragma parameter monitorgamma "CRTGeom Monitor Gamma" 2.2 0.1 5.0 0.1
#pragma parameter d "CRTGeom Distance" 1.6 0.1 3.0 0.1
#pragma parameter CURVATURE "CRTGeom Curvature Toggle" 1.0 0.0 1.0 1.0
#pragma parameter R "CRTGeom Curvature Radius" 2.0 0.1 10.0 0.1
#pragma parameter cornersize "CRTGeom Corner Size" 0.03 0.001 1.0 0.005
#pragma parameter cornersmooth "CRTGeom Corner Smoothness" 1000.0 80.0 2000.0 100.0
#pragma parameter x_tilt "CRTGeom Horizontal Tilt" 0.0 -0.5 0.5 0.05
#pragma parameter y_tilt "CRTGeom Vertical Tilt" 0.0 -0.5 0.5 0.05
#pragma parameter overscan_x "CRTGeom Horiz. Overscan %" 100.0 -125.0 125.0 1.0
#pragma parameter overscan_y "CRTGeom Vert. Overscan %" 100.0 -125.0 125.0 1.0
#pragma parameter DOTMASK "CRTGeom Dot Mask Toggle" 0.3 0.0 0.3 0.3
#pragma parameter SHARPER "CRTGeom Sharpness" 1.0 1.0 3.0 1.0
#pragma parameter scanline_weight "CRTGeom Scanline Weight" 0.3 0.1 0.5 0.05
#pragma parameter lum "CRTGeom Luminance" 0.0 0.0 1.0 0.01
#pragma parameter interlace_detect "CRTGeom Interlacing Simulation" 1.0 0.0 1.0 1.0
*/

#define CRTgamma 2.4
#define monitorgamma 2.2
#define d 1.6
#define CURVATURE 1.0
#define R 2.0
#define cornersize 0.03
#define cornersmooth 1000.0
#define x_tilt 0.0
#define y_tilt 0.0
#define overscan_x 100.0
#define overscan_y 100.0
#define DOTMASK 0.3
#define SHARPER 1.0
#define scanline_weight 0.3
#define lum 0.0
#define interlace_detect 1.0

attribute vec4 VertexCoord;
attribute vec4 COLOR;
attribute vec4 TexCoord;
varying vec4 TEX0;

uniform mediump vec2 OutputSize;
uniform mediump vec2 TextureSize;
uniform mediump vec2 InputSize;

varying vec2 overscan;
varying vec2 aspect;
varying vec3 stretch;
varying vec2 sinangle;
varying vec2 cosangle;
varying vec2 one;
varying float mod_factor;
varying vec2 ilfac;

float intersect(vec2 xy)
{
    float A = dot(xy,xy)+d*d;
    float B = 2.0*(R*(dot(xy,sinangle)-d*cosangle.x*cosangle.y)-d*d);
    float C = d*d + 2.0*R*d*cosangle.x*cosangle.y;
    return (-B-sqrt(B*B-4.0*A*C))/(2.0*A);
}

vec2 bkwtrans(vec2 xy)
{
    float c = intersect(xy);
    vec2 point = vec2(c)*xy;
    point -= vec2(-R)*sinangle;
    point /= vec2(R);
    vec2 tang = sinangle/cosangle;
    vec2 poc = point/cosangle;
    float A = dot(tang,tang)+1.0;
    float B = -2.0*dot(poc,tang);
    float C = dot(poc,poc)-1.0;
    float a = (-B+sqrt(B*B-4.0*A*C))/(2.0*A);
    vec2 uv = (point-a*sinangle)/cosangle;
    float r = R*acos(a);
    return uv*r/sin(r/R);
}

vec2 fwtrans(vec2 uv)
{
    float r = sqrt(dot(uv,uv));
    r =  max(abs(r), 1e-5);
    uv *= sin(r/R)/r;
    float x = 1.0-cos(r/R);
    float D = d/R + x*cosangle.x*cosangle.y+dot(uv,sinangle);
    return d*(uv*cosangle-x*sinangle)/D;
}

vec3 maxscale()
{
    vec2 c = bkwtrans(-R * sinangle / (1.0 + R/d*cosangle.x*cosangle.y));
    vec2 a = vec2(0.5,0.5)*aspect;
    vec2 lo = vec2(fwtrans(vec2(-a.x,c.y)).x, fwtrans(vec2(c.x,-a.y)).y)/aspect;
    vec2 hi = vec2(fwtrans(vec2(+a.x,c.y)).x, fwtrans(vec2(c.x,+a.y)).y)/aspect;
    return vec3((hi+lo)*aspect*0.5,max(hi.x-lo.x,hi.y-lo.y));
}

void main()
{
    // START of parameters

    // gamma of simulated CRT
    //	CRTgamma = 1.8;
    // gamma of display monitor (typically 2.2 is correct)
    //	monitorgamma = 2.2;
    // overscan (e.g. 1.02 for 2% overscan)
    overscan = vec2(1.00,1.00);
    // aspect ratio
    aspect = vec2(1.0, 0.75);
    // lengths are measured in units of (approximately) the width
    // of the monitor simulated distance from viewer to monitor
    //	d = 2.0;
    // radius of curvature
    //	R = 1.5;
    // tilt angle in radians
    // (behavior might be a bit wrong if both components are
    // nonzero)
    const vec2 angle = vec2(0.0,0.0);
    // size of curved corners
    //	cornersize = 0.03;
    // border smoothness parameter
    // decrease if borders are too aliased
    //	cornersmooth = 1000.0;

    // END of parameters

    vec2 _otexCoord;
    gl_Position = VertexCoord;
    _otexCoord = TexCoord.xy;
    TEX0.xy = TexCoord.xy;

    // Precalculate a bunch of useful values we'll need in the fragment
    // shader.
    sinangle = sin(vec2(x_tilt, y_tilt)) + vec2(0.001);//sin(vec2(max(abs(x_tilt), 1e-3), max(abs(y_tilt), 1e-3)));
    cosangle = cos(vec2(x_tilt, y_tilt)) + vec2(0.001);//cos(vec2(max(abs(x_tilt), 1e-3), max(abs(y_tilt), 1e-3)));
    stretch = maxscale();

    ilfac = vec2(1.0,clamp(floor(InputSize.y/200.0), 1.0, 2.0));

    // The size of one texel, in texture-coordinates.
    vec2 sharpTextureSize = vec2(SHARPER * TextureSize.x, TextureSize.y);
    one = ilfac / sharpTextureSize;

    // Resulting X pixel-coordinate of the pixel we're drawing.
    mod_factor = TexCoord.x * TextureSize.x * OutputSize.x / InputSize.x;
}
