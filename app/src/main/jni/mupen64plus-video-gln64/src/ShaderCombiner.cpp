
#include <stdlib.h>
#include "OpenGL.h"
#include "ShaderCombiner.h"
#include "Common.h"
#include "Textures.h"
#include "Config.h"


//(sa - sb) * m + a
static const u32 saRGBExpanded[] =
{
    COMBINED,           TEXEL0,             TEXEL1,             PRIMITIVE,
    SHADE,              ENVIRONMENT,        ONE,                NOISE,
    ZERO,               ZERO,               ZERO,               ZERO,
    ZERO,               ZERO,               ZERO,               ZERO
};

static const u32 sbRGBExpanded[] =
{
    COMBINED,           TEXEL0,             TEXEL1,             PRIMITIVE,
    SHADE,              ENVIRONMENT,        CENTER,             K4,
    ZERO,               ZERO,               ZERO,               ZERO,
    ZERO,               ZERO,               ZERO,               ZERO
};

static const u32 mRGBExpanded[] =
{
    COMBINED,           TEXEL0,             TEXEL1,             PRIMITIVE,
    SHADE,              ENVIRONMENT,        SCALE,              COMBINED_ALPHA,
    TEXEL0_ALPHA,       TEXEL1_ALPHA,       PRIMITIVE_ALPHA,    SHADE_ALPHA,
    ENV_ALPHA,          LOD_FRACTION,       PRIM_LOD_FRAC,      K5,
    ZERO,               ZERO,               ZERO,               ZERO,
    ZERO,               ZERO,               ZERO,               ZERO,
    ZERO,               ZERO,               ZERO,               ZERO,
    ZERO,               ZERO,               ZERO,               ZERO
};

static const u32 aRGBExpanded[] =
{
    COMBINED,           TEXEL0,             TEXEL1,             PRIMITIVE,
    SHADE,              ENVIRONMENT,        ONE,                ZERO
};

static const u32 saAExpanded[] =
{
    COMBINED,           TEXEL0_ALPHA,       TEXEL1_ALPHA,       PRIMITIVE_ALPHA,
    SHADE_ALPHA,        ENV_ALPHA,          ONE,                ZERO
};

static const u32 sbAExpanded[] =
{
    COMBINED,           TEXEL0_ALPHA,       TEXEL1_ALPHA,       PRIMITIVE_ALPHA,
    SHADE_ALPHA,        ENV_ALPHA,          ONE,                ZERO
};

static const u32 mAExpanded[] =
{
    LOD_FRACTION,       TEXEL0_ALPHA,       TEXEL1_ALPHA,       PRIMITIVE_ALPHA,
    SHADE_ALPHA,        ENV_ALPHA,          PRIM_LOD_FRAC,      ZERO,
};

static const u32 aAExpanded[] =
{
    COMBINED,           TEXEL0_ALPHA,       TEXEL1_ALPHA,       PRIMITIVE_ALPHA,
    SHADE_ALPHA,        ENV_ALPHA,          ONE,                ZERO
};

int CCEncodeA[] = {0, 1, 2, 3, 4, 5, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 7, 15, 15, 6, 15 };
int CCEncodeB[] = {0, 1, 2, 3, 4, 5, 6, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 7, 15, 15, 15 };
int CCEncodeC[] = {0, 1, 2, 3, 4, 5, 31, 6, 7, 8, 9, 10, 11, 12, 13, 14, 31, 31, 15, 31, 31};
int CCEncodeD[] = {0, 1, 2, 3, 4, 5, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 6, 15};
int ACEncodeA[] = {7, 7, 7, 7, 7, 7, 7, 7, 0, 1, 2, 3, 4, 5, 7, 7, 7, 7, 7, 6, 7};
int ACEncodeB[] = {7, 7, 7, 7, 7, 7, 7, 7, 0, 1, 2, 3, 4, 5, 7, 7, 7, 7, 7, 6, 7};
int ACEncodeC[] = {7, 7, 7, 7, 7, 7, 7, 7, 0, 1, 2, 3, 4, 5, 7, 6, 7, 7, 7, 7, 7};
int ACEncodeD[] = {7, 7, 7, 7, 7, 7, 7, 7, 0, 1, 2, 3, 4, 5, 7, 7, 7, 7, 7, 6, 7};

ShaderProgram *scProgramRoot = NULL;
ShaderProgram *scProgramCurrent = NULL;
int scProgramChanged = 0;
int scProgramCount = 0;

GLint _vertex_shader = 0;

const char *_frag_header = "                                \n"\
"uniform sampler2D uTex0;                                   \n"\
"uniform sampler2D uTex1;                                   \n"\
"uniform sampler2D uNoise;                                  \n"\
"uniform lowp vec4 uEnvColor;                               \n"\
"uniform lowp vec4 uPrimColor;                              \n"\
"uniform lowp vec4 uFogColor;                               \n"\
"uniform highp float uAlphaRef;                             \n"\
"uniform lowp float uPrimLODFrac;                           \n"\
"uniform lowp float uK4;                                    \n"\
"uniform lowp float uK5;                                    \n"\
"                                                           \n"\
"varying lowp float vFactor;                                \n"\
"varying lowp vec4 vShadeColor;                             \n"\
"varying mediump vec2 vTexCoord0;                           \n"\
"varying mediump vec2 vTexCoord1;                           \n"\
"                                                           \n"\
"void main()                                                \n"\
"{                                                          \n"\
"lowp vec4 lFragColor;                                      \n";


const char *_vert = "                                       \n"\
"attribute highp vec4 	aPosition;                          \n"\
"attribute lowp vec4 	aColor;                             \n"\
"attribute highp vec2   aTexCoord0;                         \n"\
"attribute highp vec2   aTexCoord1;                         \n"\
"                                                           \n"\
"uniform bool		    uEnableFog;                         \n"\
"uniform float			uFogMultiplier, uFogOffset;         \n"\
"uniform float 			uRenderState;                       \n"\
"                                                           \n"\
"uniform mediump vec2 	uTexScale;                          \n"\
"uniform mediump vec2 	uTexOffset[2];                      \n"\
"uniform mediump vec2 	uCacheShiftScale[2];                \n"\
"uniform mediump vec2 	uCacheScale[2];                     \n"\
"uniform mediump vec2 	uCacheOffset[2];                    \n"\
"                                                           \n"\
"varying lowp float     vFactor;                            \n"\
"varying lowp vec4 		vShadeColor;                        \n"\
"varying mediump vec2 	vTexCoord0;                         \n"\
"varying mediump vec2 	vTexCoord1;                         \n"\
"                                                           \n"\
"void main()                                                \n"\
"{                                                          \n"\
"gl_Position = aPosition;                                   \n"\
"vShadeColor = aColor;                                      \n"\
"                                                           \n"\
"if (uRenderState == 1.0)                                   \n"\
"{                                                          \n"\
"vTexCoord0 = (aTexCoord0 * (uTexScale[0] *                 \n"\
"           uCacheShiftScale[0]) + (uCacheOffset[0] -       \n"\
"           uTexOffset[0])) * uCacheScale[0];               \n"\
"vTexCoord1 = (aTexCoord0 * (uTexScale[1] *                 \n"\
"           uCacheShiftScale[1]) + (uCacheOffset[1] -       \n"\
"           uTexOffset[1])) * uCacheScale[1];               \n"\
"}                                                          \n"\
"else                                                       \n"\
"{                                                          \n"\
"vTexCoord0 = aTexCoord0;                                   \n"\
"vTexCoord1 = aTexCoord1;                                   \n"\
"}                                                          \n"\
"                                                           \n";

const char * _vertfog = "                                   \n"\
"if (uEnableFog)                                            \n"\
"{                                                          \n"\
"vFactor = max(-1.0, aPosition.z / aPosition.w)             \n"\
"   * uFogMultiplier + uFogOffset;                          \n"\
"vFactor = clamp(vFactor, 0.0, 1.0);                        \n"\
"}                                                          \n";

const char * _vertzhack = "                                 \n"\
"if (uRenderState == 1.0)                                   \n"\
"{                                                          \n"\
"gl_Position.z = (gl_Position.z + gl_Position.w*9.0) * 0.1; \n"\
"}                                                          \n";


const char * _color_param_str(int param)
{
    switch(param)
    {
        case COMBINED:          return "lFragColor.rgb";
        case TEXEL0:            return "lTex0.rgb";
        case TEXEL1:            return "lTex1.rgb";
        case PRIMITIVE:         return "uPrimColor.rgb";
        case SHADE:             return "vShadeColor.rgb";
        case ENVIRONMENT:       return "uEnvColor.rgb";
        case CENTER:            return "vec3(0.0)";
        case SCALE:             return "vec3(0.0)";
        case COMBINED_ALPHA:    return "vec3(lFragColor.a)";
        case TEXEL0_ALPHA:      return "vec3(lTex0.a)";
        case TEXEL1_ALPHA:      return "vec3(lTex1.a)";
        case PRIMITIVE_ALPHA:   return "vec3(uPrimColor.a)";
        case SHADE_ALPHA:       return "vec3(vShadeColor.a)";
        case ENV_ALPHA:         return "vec3(uEnvColor.a)";
        case LOD_FRACTION:      return "vec3(0.0)";
        case PRIM_LOD_FRAC:     return "vec3(uPrimLODFrac)";
        case NOISE:             return "lNoise.rgb";
        case K4:                return "vec3(uK4)";
        case K5:                return "vec3(uK5)";
        case ONE:               return "vec3(1.0)";
        case ZERO:              return "vec3(0.0)";
        default:
            return "vec3(0.0)";
    }
}

const char * _alpha_param_str(int param)
{
    switch(param)
    {
        case COMBINED:          return "lFragColor.a";
        case TEXEL0:            return "lTex0.a";
        case TEXEL1:            return "lTex1.a";
        case PRIMITIVE:         return "uPrimColor.a";
        case SHADE:             return "vShadeColor.a";
        case ENVIRONMENT:       return "uEnvColor.a";
        case CENTER:            return "0.0";
        case SCALE:             return "0.0";
        case COMBINED_ALPHA:    return "lFragColor.a";
        case TEXEL0_ALPHA:      return "lTex0.a";
        case TEXEL1_ALPHA:      return "lTex1.a";
        case PRIMITIVE_ALPHA:   return "uPrimColor.a";
        case SHADE_ALPHA:       return "vShadeColor.a";
        case ENV_ALPHA:         return "uEnvColor.a";
        case LOD_FRACTION:      return "0.0";
        case PRIM_LOD_FRAC:     return "uPrimLODFrac";
        case NOISE:             return "lNoise.a";
        case K4:                return "uK4";
        case K5:                return "uK5";
        case ONE:               return "1.0";
        case ZERO:              return "0.0";
        default:
            return "0.0";
    }
}

DecodedMux::DecodedMux(u64 mux, bool cycle2)
{
    combine.mux = mux;
    flags = 0;

    //set to ZERO.
    for(int i=0;i<4;i++)
        for(int j=0; j< 4; j++)
            decode[i][j] = ZERO;

    //rgb cycle 0
    decode[0][0] = saRGBExpanded[combine.saRGB0];
    decode[0][1] = sbRGBExpanded[combine.sbRGB0];
    decode[0][2] = mRGBExpanded[combine.mRGB0];
    decode[0][3] = aRGBExpanded[combine.aRGB0];
    decode[1][0] = saAExpanded[combine.saA0];
    decode[1][1] = sbAExpanded[combine.sbA0];
    decode[1][2] = mAExpanded[combine.mA0];
    decode[1][3] = aAExpanded[combine.aA0];
    if (cycle2)
    {
        //rgb cycle 1
        decode[2][0] = saRGBExpanded[combine.saRGB1];
        decode[2][1] = sbRGBExpanded[combine.sbRGB1];
        decode[2][2] = mRGBExpanded[combine.mRGB1];
        decode[2][3] = aRGBExpanded[combine.aRGB1];
        decode[3][0] = saAExpanded[combine.saA1];
        decode[3][1] = sbAExpanded[combine.sbA1];
        decode[3][2] = mAExpanded[combine.mA1];
        decode[3][3] = aAExpanded[combine.aA1];

        //texel 0/1 are swapped in 2nd cycle.
        swap(1, TEXEL0, TEXEL1);
        swap(1, TEXEL0_ALPHA, TEXEL1_ALPHA);
    }

    //simplifying mux:
    if (replace(G_CYC_1CYCLE, LOD_FRACTION, ZERO) || replace(G_CYC_2CYCLE, LOD_FRACTION, ZERO))
        LOG(LOG_VERBOSE, "SC Replacing LOD_FRACTION with ZERO\n");
#if 1
    if (replace(G_CYC_1CYCLE, K4, ZERO) || replace(G_CYC_2CYCLE, K4, ZERO))
        LOG(LOG_VERBOSE, "SC Replacing K4 with ZERO\n");

    if (replace(G_CYC_1CYCLE, K5, ZERO) || replace(G_CYC_2CYCLE, K5, ZERO))
        LOG(LOG_VERBOSE, "SC Replacing K5 with ZERO\n");
#endif

    if (replace(G_CYC_1CYCLE, CENTER, ZERO) || replace(G_CYC_2CYCLE, CENTER, ZERO))
        LOG(LOG_VERBOSE, "SC Replacing CENTER with ZERO\n");

    if (replace(G_CYC_1CYCLE, SCALE, ZERO) || replace(G_CYC_2CYCLE, SCALE, ZERO))
        LOG(LOG_VERBOSE, "SC Replacing SCALE with ZERO\n");

    //Combiner has initial value of zero in cycle 0
    if (replace(G_CYC_1CYCLE, COMBINED, ZERO))
        LOG(LOG_VERBOSE, "SC Setting CYCLE1 COMBINED to ZERO\n");

    if (replace(G_CYC_1CYCLE, COMBINED_ALPHA, ZERO))
        LOG(LOG_VERBOSE, "SC Setting CYCLE1 COMBINED_ALPHA to ZERO\n");

    if (!config.enableNoise)
    {
        if (replace(G_CYC_1CYCLE, NOISE, ZERO))
            LOG(LOG_VERBOSE, "SC Setting CYCLE1 NOISE to ZERO\n");

        if (replace(G_CYC_2CYCLE, NOISE, ZERO))
            LOG(LOG_VERBOSE, "SC Setting CYCLE2 NOISE to ZERO\n");

    }

    //mutiplying by zero: (A-B)*0 + C = C
    for(int i=0 ; i<4; i++)
    {
        if (decode[i][2] == ZERO)
        {
            decode[i][0] = ZERO;
            decode[i][1] = ZERO;
        }
    }

    //(A1-B1)*C1 + D1
    //(A2-B2)*C2 + D2
    //1. ((A1-B1)*C1 + D1 - B2)*C2 + D2 = A1*C1*C2 - B1*C1*C2 + D1*C2 - B2*C2 + D2
    //2. (A2 - (A1-B1)*C1 - D1)*C2 + D2 = A2*C2 - A1*C1*C2 + B1*C1*C2 - D1*C2 + D2
    //3. (A2 - B2)*((A1-B1)*C1 + D1) + D2 = A2*A1*C1 - A2*B1*C1 + A2*D1 - B2*A1*C1 + B2*B1*C1 - B2*D1 + D2
    //4. (A2-B2)*C2 + (A1-B1)*C1 + D1 = A2*C2 - B2*C2 + A1*C1 - B1*C1 + D1

    if (cycle2)
    {

        if (!find(2, COMBINED))
            flags |= SC_IGNORE_RGB0;

        if (!(find(2, COMBINED_ALPHA) || find(3, COMBINED_ALPHA) || find(3, COMBINED)))
            flags |= SC_IGNORE_ALPHA0;

        if (decode[2][0] == ZERO && decode[2][1] == ZERO && decode[2][2] == ZERO && decode[2][3] == COMBINED)
        {
            flags |= SC_IGNORE_RGB1;
        }

        if (decode[3][0] == ZERO && decode[3][1] == ZERO && decode[3][2] == ZERO &&
            (decode[3][3] == COMBINED_ALPHA || decode[3][3] == COMBINED))
        {
            flags |= SC_IGNORE_ALPHA1;
        }

    }
}

bool DecodedMux::find(int index, int src)
{
    for(int j=0;j<4;j++)
    {
        if (decode[index][j] == src) return true;
    }
    return false;
}

bool DecodedMux::replace(int cycle, int src, int dest)
{
    int r = false;
    for(int i=0;i<2;i++)
    {
        int ii = (cycle == 0) ? i : (2+i);
        for(int j=0;j<4;j++)
        {
            if (decode[ii][j] == src) {decode[ii][j] = dest; r=true;}
        }
    }
    return r;
}

bool DecodedMux::swap(int cycle, int src0, int src1)
{
    int r = false;
    for(int i=0;i<2;i++)
    {
        int ii = (cycle == 0) ? i : (2+i);
        for(int j=0;j<4;j++)
        {
            if (decode[ii][j] == src0) {decode[ii][j] = src1; r=true;}
            else if (decode[ii][j] == src1) {decode[ii][j] = src0; r=true;}
        }
    }
    return r;
}

void DecodedMux::hack()
{
    if (config.hackZelda)
    {
        if(combine.mux == 0xfffd923800ffadffLL)
        {
            replace(G_CYC_1CYCLE, TEXEL1, TEXEL0);
            replace(G_CYC_2CYCLE, TEXEL1, TEXEL0);
        }
        else if (combine.mux == 0xff5bfff800121603LL)
        {
            replace(G_CYC_1CYCLE, TEXEL1, ZERO);
            replace(G_CYC_2CYCLE, TEXEL1, ZERO);
        }
    }

}


int _program_compare(ShaderProgram *prog, DecodedMux *dmux, u32 flags)
{
    if (prog)
        return ((prog->combine.mux == dmux->combine.mux) && (prog->flags == flags));
    else
        return 1;
}

void _glcompiler_error(GLint shader)
{
    int len, i;
    char* log;

    glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &len);
    log = (char*) malloc(len + 1);
    glGetShaderInfoLog(shader, len, &i, log);
    log[len] = 0;
    LOG(LOG_ERROR, "COMPILE ERROR: %s \n", log);
    free(log);
}

void _gllinker_error(GLint program)
{
    int len, i;
    char* log;

    glGetProgramiv(program, GL_INFO_LOG_LENGTH, &len);
    log = (char*) malloc(len + 1);
    glGetProgramInfoLog(program, len, &i, log);
    log[len] = 0;
    LOG(LOG_ERROR, "LINK ERROR: %s \n", log);
    free(log);
};

void _locate_attributes(ShaderProgram *p)
{
    glBindAttribLocation(p->program, SC_POSITION,   "aPosition");
    glBindAttribLocation(p->program, SC_COLOR,      "aColor");
    glBindAttribLocation(p->program, SC_TEXCOORD0,  "aTexCoord0");
    glBindAttribLocation(p->program, SC_TEXCOORD1,  "aTexCoord1");
};

#define LocateUniform(A) \
    p->uniforms.A.loc = glGetUniformLocation(p->program, #A);

void _locate_uniforms(ShaderProgram *p)
{
    LocateUniform(uTex0);
    LocateUniform(uTex1);
    LocateUniform(uNoise);
    LocateUniform(uEnvColor);
    LocateUniform(uPrimColor);
    LocateUniform(uPrimLODFrac);
    LocateUniform(uK4);
    LocateUniform(uK5);
    LocateUniform(uFogColor);
    LocateUniform(uEnableFog);
    LocateUniform(uRenderState);
    LocateUniform(uFogMultiplier);
    LocateUniform(uFogOffset);
    LocateUniform(uAlphaRef);
    LocateUniform(uTexScale);
    LocateUniform(uTexOffset[0]);
    LocateUniform(uTexOffset[1]);
    LocateUniform(uCacheShiftScale[0]);
    LocateUniform(uCacheShiftScale[1]);
    LocateUniform(uCacheScale[0]);
    LocateUniform(uCacheScale[1]);
    LocateUniform(uCacheOffset[0]);
    LocateUniform(uCacheOffset[1]);
}

void _force_uniforms()
{
    SC_ForceUniform1i(uTex0, 0);
    SC_ForceUniform1i(uTex1, 1);
    SC_ForceUniform1i(uNoise, 2);
    SC_ForceUniform4fv(uEnvColor, &gDP.envColor.r);
    SC_ForceUniform4fv(uPrimColor, &gDP.primColor.r);
    SC_ForceUniform1f(uPrimLODFrac, gDP.primColor.l);
    SC_ForceUniform1f(uK4, gDP.convert.k4);
    SC_ForceUniform1f(uK5, gDP.convert.k5);
    SC_ForceUniform4fv(uFogColor, &gDP.fogColor.r);
    SC_ForceUniform1i(uEnableFog, ((config.enableFog==1) && (gSP.geometryMode & G_FOG)));
    SC_ForceUniform1f(uRenderState, OGL.renderState);
    SC_ForceUniform1f(uFogMultiplier, (float) gSP.fog.multiplier / 255.0f);
    SC_ForceUniform1f(uFogOffset, (float) gSP.fog.offset / 255.0f);
    SC_ForceUniform1f(uAlphaRef, (gDP.otherMode.cvgXAlpha) ? 0.5 : gDP.blendColor.a);
    SC_ForceUniform2f(uTexScale, gSP.texture.scales, gSP.texture.scalet);

    if (gSP.textureTile[0]){
        SC_ForceUniform2f(uTexOffset[0], gSP.textureTile[0]->fuls, gSP.textureTile[0]->fult);
    } else {
        SC_ForceUniform2f(uTexOffset[0], 0.0f, 0.0f);
    }

    if (gSP.textureTile[1])
    {
        SC_ForceUniform2f(uTexOffset[1], gSP.textureTile[1]->fuls, gSP.textureTile[1]->fult);
    }
    else
    {
        SC_ForceUniform2f(uTexOffset[1], 0.0f, 0.0f);
    }

    if (cache.current[0])
    {
        SC_ForceUniform2f(uCacheShiftScale[0], cache.current[0]->shiftScaleS, cache.current[0]->shiftScaleT);
        SC_ForceUniform2f(uCacheScale[0], cache.current[0]->scaleS, cache.current[0]->scaleT);
        SC_ForceUniform2f(uCacheOffset[0], cache.current[0]->offsetS, cache.current[0]->offsetT);
    }
    else
    {
        SC_ForceUniform2f(uCacheShiftScale[0], 1.0f, 1.0f);
        SC_ForceUniform2f(uCacheScale[0], 1.0f, 1.0f);
        SC_ForceUniform2f(uCacheOffset[0], 0.0f, 0.0f);
    }

    if (cache.current[1])
    {
        SC_ForceUniform2f(uCacheShiftScale[1], cache.current[1]->shiftScaleS, cache.current[1]->shiftScaleT);
        SC_ForceUniform2f(uCacheScale[1], cache.current[1]->scaleS, cache.current[1]->scaleT);
        SC_ForceUniform2f(uCacheOffset[1], cache.current[1]->offsetS, cache.current[1]->offsetT);
    }
    else
    {
        SC_ForceUniform2f(uCacheShiftScale[1], 1.0f, 1.0f);
        SC_ForceUniform2f(uCacheScale[1], 1.0f, 1.0f);
        SC_ForceUniform2f(uCacheOffset[1], 0.0f, 0.0f);
    }
}

void _update_uniforms()
{
    SC_SetUniform4fv(uEnvColor, &gDP.envColor.r);
    SC_SetUniform4fv(uPrimColor, &gDP.primColor.r);
    SC_SetUniform1f(uPrimLODFrac, gDP.primColor.l);
    SC_SetUniform4fv(uFogColor, &gDP.fogColor.r);
    SC_SetUniform1i(uEnableFog, (config.enableFog && (gSP.geometryMode & G_FOG)));
    SC_SetUniform1f(uRenderState, OGL.renderState);
    SC_SetUniform1f(uFogMultiplier, (float) gSP.fog.multiplier / 255.0f);
    SC_SetUniform1f(uFogOffset, (float) gSP.fog.offset / 255.0f);
    SC_SetUniform1f(uAlphaRef, (gDP.otherMode.cvgXAlpha) ? 0.5 : gDP.blendColor.a);
    SC_SetUniform1f(uK4, gDP.convert.k4);
    SC_SetUniform1f(uK5, gDP.convert.k5);

    //for some reason i must force these...
    SC_ForceUniform2f(uTexScale, gSP.texture.scales, gSP.texture.scalet);
    if (scProgramCurrent->usesT0)
    {
        if (gSP.textureTile[0])
        {
            SC_ForceUniform2f(uTexOffset[0], gSP.textureTile[0]->fuls, gSP.textureTile[0]->fult);
        }
        if (cache.current[0])
        {
            SC_ForceUniform2f(uCacheShiftScale[0], cache.current[0]->shiftScaleS, cache.current[0]->shiftScaleT);
            SC_ForceUniform2f(uCacheScale[0], cache.current[0]->scaleS, cache.current[0]->scaleT);
            SC_ForceUniform2f(uCacheOffset[0], cache.current[0]->offsetS, cache.current[0]->offsetT);
        }
    }

    if (scProgramCurrent->usesT1)
    {
        if (gSP.textureTile[1])
        {
            SC_ForceUniform2f(uTexOffset[1], gSP.textureTile[1]->fuls, gSP.textureTile[1]->fult);
        }
        if (cache.current[1])
        {
            SC_ForceUniform2f(uCacheShiftScale[1], cache.current[1]->shiftScaleS, cache.current[1]->shiftScaleT);
            SC_ForceUniform2f(uCacheScale[1], cache.current[1]->scaleS, cache.current[1]->scaleT);
            SC_ForceUniform2f(uCacheOffset[1], cache.current[1]->offsetS, cache.current[1]->offsetT);
        }
    }
};

void ShaderCombiner_Init()
{
    //compile vertex shader:
    GLint success;
    const char *src[1];
    char buff[4096];
    char *str = buff;

    str += sprintf(str, "%s", _vert);
    if (config.enableFog)
    {
        str += sprintf(str, "%s", _vertfog);
    }
    if (config.zHack)
    {
        str += sprintf(str, "%s", _vertzhack);
    }

    str += sprintf(str, "}\n\n");

#ifdef PRINT_SHADER
    LOG(LOG_VERBOSE, "=============================================================\n");
    LOG(LOG_VERBOSE, "Vertex Shader:\n");
    LOG(LOG_VERBOSE, "=============================================================\n");
    LOG(LOG_VERBOSE, "%s", buff);
    LOG(LOG_VERBOSE, "=============================================================\n");
#endif

    src[0] = buff;
    _vertex_shader = glCreateShader(GL_VERTEX_SHADER);
    glShaderSource(_vertex_shader, 1, (const char**) src, NULL);
    glCompileShader(_vertex_shader);
    glGetShaderiv(_vertex_shader, GL_COMPILE_STATUS, &success);
    if (!success)
    {
        _glcompiler_error(_vertex_shader);
    }
};

void ShaderCombiner_DeletePrograms(ShaderProgram *prog)
{
    if (prog)
    {
        ShaderCombiner_DeletePrograms(prog->left);
        ShaderCombiner_DeletePrograms(prog->right);
        glDeleteProgram(prog->program);
        //glDeleteShader(prog->fragment);
        free(prog);
        scProgramCount--;
    }
}

void ShaderCombiner_Destroy()
{
    ShaderCombiner_DeletePrograms(scProgramRoot);
    glDeleteShader(_vertex_shader);
    scProgramCount = scProgramChanged = 0;
    scProgramRoot = scProgramCurrent = NULL;
}

void ShaderCombiner_Set(u64 mux, int flags)
{
    //banjo tooie hack
    if ((gDP.otherMode.cycleType == G_CYC_1CYCLE) && (mux == 0x00ffe7ffffcf9fcfLL))
    {
        mux = EncodeCombineMode( 0, 0, 0, 0, TEXEL0, 0, PRIMITIVE, 0,
                                 0, 0, 0, 0, TEXEL0, 0, PRIMITIVE, 0 );
    }

    //determine flags
    if (flags == -1)
    {
        flags = 0;
        if ((config.enableFog) && (gSP.geometryMode & G_FOG))
            flags |= SC_FOGENABLED;

        if (config.enableAlphaTest)
        {
            if ((gDP.otherMode.alphaCompare == G_AC_THRESHOLD) && !(gDP.otherMode.alphaCvgSel)){
                flags |= SC_ALPHAENABLED;
                if (gDP.blendColor.a > 0.0f) flags |= SC_ALPHAGREATER;
            } else if (gDP.otherMode.cvgXAlpha){
                flags |= SC_ALPHAENABLED;
                flags |= SC_ALPHAGREATER;
            }
        }

        if (gDP.otherMode.cycleType == G_CYC_2CYCLE)
            flags |= SC_2CYCLE;
    }


    DecodedMux dmux(mux, flags&SC_2CYCLE);
    dmux.hack();

    //if already bound:
    if (scProgramCurrent)
    {
        if (_program_compare(scProgramCurrent, &dmux, flags))
        {
            scProgramChanged = 0;
            return;
        }
    }

    //traverse binary tree for cached programs
    scProgramChanged = 1;
    ShaderProgram *root = scProgramRoot;
    ShaderProgram *prog = root;
    while(!_program_compare(prog, &dmux, flags))
    {
        root = prog;
        if (prog->combine.mux < dmux.combine.mux)
            prog = prog->right;
        else
            prog = prog->left;
    }

    //build new program
    if (!prog)
    {
        scProgramCount++;
        prog = ShaderCombiner_Compile(&dmux, flags);
        if (!root)
            scProgramRoot = prog;
        else if (root->combine.mux < dmux.combine.mux)
            root->right = prog;
        else
            root->left = prog;

    }

    prog->lastUsed = OGL.frame_dl;
    scProgramCurrent = prog;
    glUseProgram(prog->program);
    _force_uniforms();
}

ShaderProgram *ShaderCombiner_Compile(DecodedMux *dmux, int flags)
{
    GLint success;
    char frag[4096];
    char *buffer = frag;
    ShaderProgram *prog = (ShaderProgram*) malloc(sizeof(ShaderProgram));

    prog->left = prog->right = NULL;
    prog->usesT0 = prog->usesT1 = prog->usesCol = prog->usesNoise = 0;
    prog->combine = dmux->combine;
    prog->flags = flags;
    prog->vertex = _vertex_shader;

    for(int i=0; i < ((flags & SC_2CYCLE) ? 4 : 2); i++)
    {
        //make sure were not ignoring cycle:
        if ((dmux->flags&(1<<i)) == 0)
        {
            for(int j=0;j<4;j++)
            {
                prog->usesT0 |= (dmux->decode[i][j] == TEXEL0 || dmux->decode[i][j] == TEXEL0_ALPHA);
                prog->usesT1 |= (dmux->decode[i][j] == TEXEL1 || dmux->decode[i][j] == TEXEL1_ALPHA);
                prog->usesCol |= (dmux->decode[i][j] == SHADE || dmux->decode[i][j] == SHADE_ALPHA);
                prog->usesNoise |= (dmux->decode[i][j] == NOISE);
            }
        }
    }

    buffer += sprintf(buffer, "%s", _frag_header);
    if (prog->usesT0)
        buffer += sprintf(buffer, "lowp vec4 lTex0 = texture2D(uTex0, vTexCoord0); \n");
    if (prog->usesT1)
        buffer += sprintf(buffer, "lowp vec4 lTex1 = texture2D(uTex1, vTexCoord1); \n");
    if (prog->usesNoise)
        buffer += sprintf(buffer, "lowp vec4 lNoise = texture2D(uNoise, (1.0 / 1024.0) * gl_FragCoord.st); \n");

    for(int i = 0; i < ((flags & SC_2CYCLE) ? 2 : 1); i++)
    {
        if ((dmux->flags&(1<<(i*2))) == 0)
        {
            buffer += sprintf(buffer, "lFragColor.rgb = (%s - %s) * %s + %s; \n",
                _color_param_str(dmux->decode[i*2][0]),
                _color_param_str(dmux->decode[i*2][1]),
                _color_param_str(dmux->decode[i*2][2]),
                _color_param_str(dmux->decode[i*2][3])
                );
        }

        if ((dmux->flags&(1<<(i*2+1))) == 0)
        {
            buffer += sprintf(buffer, "lFragColor.a = (%s - %s) * %s + %s; \n",
                _alpha_param_str(dmux->decode[i*2+1][0]),
                _alpha_param_str(dmux->decode[i*2+1][1]),
                _alpha_param_str(dmux->decode[i*2+1][2]),
                _alpha_param_str(dmux->decode[i*2+1][3])
                );
        }
        buffer += sprintf(buffer, "gl_FragColor = lFragColor; \n");
    };

    //fog
    if (flags&SC_FOGENABLED)
    {
        buffer += sprintf(buffer, "gl_FragColor = mix(gl_FragColor, uFogColor, vFactor); \n");
    }

    //alpha function
    if (flags&SC_ALPHAENABLED)
    {
        if (flags&SC_ALPHAGREATER)
            buffer += sprintf(buffer, "if (gl_FragColor.a < uAlphaRef) %s;\n", config.hackAlpha ? "gl_FragColor.a = 0" : "discard");
        else
            buffer += sprintf(buffer, "if (gl_FragColor.a <= uAlphaRef) %s;\n", config.hackAlpha ? "gl_FragColor.a = 0" : "discard");
    }
    buffer += sprintf(buffer, "} \n\n");
    *buffer = 0;

#ifdef PRINT_SHADER
    LOG(LOG_VERBOSE, "=============================================================\n");
    LOG(LOG_VERBOSE, "Combine=0x%llx flags=0x%x dmux flags=0x%x\n", prog->combine.mux, flags, dmux->flags);
    LOG(LOG_VERBOSE, "Num=%i \t usesT0=%i usesT1=%i usesCol=%i usesNoise=%i\n", scProgramCount, prog->usesT0, prog->usesT1, prog->usesCol, prog->usesNoise);
    LOG(LOG_VERBOSE, "=============================================================\n");
    LOG(LOG_VERBOSE, "%s", frag);
    LOG(LOG_VERBOSE, "=============================================================\n");
#endif

    prog->program = glCreateProgram();

    //Compile:
    char *src[1];
    src[0] = frag;
    GLint len[1];
    len[0] = min(4096, strlen(frag));
    prog->fragment = glCreateShader(GL_FRAGMENT_SHADER);

    glShaderSource(prog->fragment, 1, (const char**) src, len);
    glCompileShader(prog->fragment);


    glGetShaderiv(prog->fragment, GL_COMPILE_STATUS, &success);
    if (!success)
    {
        _glcompiler_error(prog->fragment);
    }

    //link
    _locate_attributes(prog);
    glAttachShader(prog->program, prog->fragment);
    glAttachShader(prog->program, prog->vertex);
    glLinkProgram(prog->program);
    glGetProgramiv(prog->program, GL_LINK_STATUS, &success);
    if (!success)
    {
        _gllinker_error(prog->program);
    }

    //remove fragment shader:
    glDeleteShader(prog->fragment);

    _locate_uniforms(prog);
    return prog;
}

