
#ifndef SHADERCOMBINER_H
#define SHADERCOMBINER_H

#define PRINT_SHADER
#define UNIFORM_CHECK

#define SC_FOGENABLED           0x1
#define SC_ALPHAENABLED         0x2
#define SC_ALPHAGREATER         0x4
#define SC_2CYCLE               0x8

#define SC_POSITION             1
#define SC_COLOR                2
#define SC_TEXCOORD0            3
#define SC_TEXCOORD1            4

#ifdef UNIFORM_CHECK
#define SC_SetUniform1i(A, B) \
    if (scProgramCurrent->uniforms.A.val != B) \
        {scProgramCurrent->uniforms.A.val = B; glUniform1i(scProgramCurrent->uniforms.A.loc, B);}

#define SC_SetUniform1f(A, B) \
    if (scProgramCurrent->uniforms.A.val != B) \
    {scProgramCurrent->uniforms.A.val = B; glUniform1f(scProgramCurrent->uniforms.A.loc, B);}

#define SC_SetUniform4fv(A, B) \
    if ((scProgramCurrent->uniforms.A.val[0] != (B)[0]) || (scProgramCurrent->uniforms.A.val[1] != (B)[1]) || \
        (scProgramCurrent->uniforms.A.val[2] != (B)[2]) || (scProgramCurrent->uniforms.A.val[3] != (B)[3]))   \
        {memcpy(scProgramCurrent->uniforms.A.val, B, 16); glUniform4fv(scProgramCurrent->uniforms.A.loc, 1, B);}

#define SC_SetUniform2f(A, B, C) \
    if ((scProgramCurrent->uniforms.A.val[0] != B) || (scProgramCurrent->uniforms.A.val[1] != C)) \
        {scProgramCurrent->uniforms.A.val[0] = B; scProgramCurrent->uniforms.A.val[1] = C; glUniform2f(scProgramCurrent->uniforms.A.loc, B, C);}

#define SC_ForceUniform1i(A, B) \
        {scProgramCurrent->uniforms.A.val = B; glUniform1i(scProgramCurrent->uniforms.A.loc, B);}

#define SC_ForceUniform1f(A, B) \
        {scProgramCurrent->uniforms.A.val = B; glUniform1f(scProgramCurrent->uniforms.A.loc, B);}

#define SC_ForceUniform4fv(A, B) \
        {memcpy(scProgramCurrent->uniforms.A.val, B, 16); glUniform4fv(scProgramCurrent->uniforms.A.loc, 1, B);}

#define SC_ForceUniform2f(A, B, C) \
        {scProgramCurrent->uniforms.A.val[0] = B; scProgramCurrent->uniforms.A.val[1] = C; glUniform2f(scProgramCurrent->uniforms.A.loc, B, C);}

#else
#define SC_SetUniform1i(A, B)       glUniform1i(scProgramCurrent->uniforms.A.loc, B)
#define SC_SetUniform1f(A, B)       glUniform1f(scProgramCurrent->uniforms.A.loc, B)
#define SC_SetUniform4fv(A, B)      glUniform4fv(scProgramCurrent->uniforms.A.loc, 1, B)
#define SC_SetUniform2f(A, B, C)    glUniform2f(scProgramCurrent->uniforms.A.loc, B, C)
#define SC_ForceUniform1i(A, B)     glUniform1i(scProgramCurrent->uniforms.A.loc, B)
#define SC_ForceUniform1f(A, B)     glUniform1f(scProgramCurrent->uniforms.A.loc, B)
#define SC_ForceUniform4fv(A, B)    glUniform4fv(scProgramCurrent->uniforms.A.loc, 1, B)
#define SC_ForceUniform2f(A, B, C)  glUniform2f(scProgramCurrent->uniforms.A.loc, B, C)
#endif

/* Color combiner constants: */
#define G_CCMUX_COMBINED        0
#define G_CCMUX_TEXEL0          1
#define G_CCMUX_TEXEL1          2
#define G_CCMUX_PRIMITIVE       3
#define G_CCMUX_SHADE           4
#define G_CCMUX_ENVIRONMENT     5
#define G_CCMUX_CENTER          6
#define G_CCMUX_SCALE           6
#define G_CCMUX_COMBINED_ALPHA  7
#define G_CCMUX_TEXEL0_ALPHA    8
#define G_CCMUX_TEXEL1_ALPHA    9
#define G_CCMUX_PRIMITIVE_ALPHA 10
#define G_CCMUX_SHADE_ALPHA     11
#define G_CCMUX_ENV_ALPHA       12
#define G_CCMUX_LOD_FRACTION    13
#define G_CCMUX_PRIM_LOD_FRAC   14
#define G_CCMUX_NOISE           7
#define G_CCMUX_K4              7
#define G_CCMUX_K5              15
#define G_CCMUX_1               6
#define G_CCMUX_0               31

/* Alpha combiner constants: */
#define G_ACMUX_COMBINED        0
#define G_ACMUX_TEXEL0          1
#define G_ACMUX_TEXEL1          2
#define G_ACMUX_PRIMITIVE       3
#define G_ACMUX_SHADE           4
#define G_ACMUX_ENVIRONMENT     5
#define G_ACMUX_LOD_FRACTION    0
#define G_ACMUX_PRIM_LOD_FRAC   6
#define G_ACMUX_1               6
#define G_ACMUX_0               7

#define EncodeCombineMode( a0, b0, c0, d0, Aa0, Ab0, Ac0, Ad0,  \
        a1, b1, c1, d1, Aa1, Ab1, Ac1, Ad1 ) \
        (u64)(((u64)(_SHIFTL( G_CCMUX_##a0, 20, 4 ) | _SHIFTL( G_CCMUX_##c0, 15, 5 ) | \
        _SHIFTL( G_ACMUX_##Aa0, 12, 3 ) | _SHIFTL( G_ACMUX_##Ac0, 9, 3 ) | \
        _SHIFTL( G_CCMUX_##a1, 5, 4 ) | _SHIFTL( G_CCMUX_##c1, 0, 5 )) << 32) | \
        (u64)(_SHIFTL( G_CCMUX_##b0, 28, 4 ) | _SHIFTL( G_CCMUX_##d0, 15, 3 ) | \
        _SHIFTL( G_ACMUX_##Ab0, 12, 3 ) | _SHIFTL( G_ACMUX_##Ad0, 9, 3 ) | \
        _SHIFTL( G_CCMUX_##b1, 24, 4 ) | _SHIFTL( G_ACMUX_##Aa1, 21, 3 ) | \
        _SHIFTL( G_ACMUX_##Ac1, 18, 3 ) | _SHIFTL( G_CCMUX_##d1, 6, 3 ) | \
        _SHIFTL( G_ACMUX_##Ab1, 3, 3 ) | _SHIFTL( G_ACMUX_##Ad1, 0, 3 )))

#define G_CC_PRIMITIVE              0, 0, 0, PRIMITIVE, 0, 0, 0, PRIMITIVE
#define G_CC_SHADE                  0, 0, 0, SHADE, 0, 0, 0, SHADE
#define G_CC_MODULATEI              TEXEL0, 0, SHADE, 0, 0, 0, 0, SHADE
#define G_CC_MODULATEIA             TEXEL0, 0, SHADE, 0, TEXEL0, 0, SHADE, 0
#define G_CC_MODULATEIDECALA        TEXEL0, 0, SHADE, 0, 0, 0, 0, TEXEL0
#define G_CC_MODULATERGB            G_CC_MODULATEI
#define G_CC_MODULATERGBA           G_CC_MODULATEIA
#define G_CC_MODULATERGBDECALA      G_CC_MODULATEIDECALA
#define G_CC_MODULATEI_PRIM         TEXEL0, 0, PRIMITIVE, 0, 0, 0, 0, PRIMITIVE
#define G_CC_MODULATEIA_PRIM        TEXEL0, 0, PRIMITIVE, 0, TEXEL0, 0, PRIMITIVE, 0
#define G_CC_MODULATEIDECALA_PRIM   TEXEL0, 0, PRIMITIVE, 0, 0, 0, 0, TEXEL0
#define G_CC_MODULATERGB_PRIM       G_CC_MODULATEI_PRIM
#define G_CC_MODULATERGBA_PRIM      G_CC_MODULATEIA_PRIM
#define G_CC_MODULATERGBDECALA_PRIM G_CC_MODULATEIDECALA_PRIM
#define G_CC_DECALRGB               0, 0, 0, TEXEL0, 0, 0, 0, SHADE
#define G_CC_DECALRGBA              0, 0, 0, TEXEL0, 0, 0, 0, TEXEL0
#define G_CC_BLENDI                 ENVIRONMENT, SHADE, TEXEL0, SHADE, 0, 0, 0, SHADE
#define G_CC_BLENDIA                ENVIRONMENT, SHADE, TEXEL0, SHADE, TEXEL0, 0, SHADE, 0
#define G_CC_BLENDIDECALA           ENVIRONMENT, SHADE, TEXEL0, SHADE, 0, 0, 0, TEXEL0
#define G_CC_BLENDRGBA              TEXEL0, SHADE, TEXEL0_ALPHA, SHADE, 0, 0, 0, SHADE
#define G_CC_BLENDRGBDECALA         TEXEL0, SHADE, TEXEL0_ALPHA, SHADE, 0, 0, 0, TEXEL0
#define G_CC_ADDRGB                 1, 0, TEXEL0, SHADE, 0, 0, 0, SHADE
#define G_CC_ADDRGBDECALA           1, 0, TEXEL0, SHADE, 0, 0, 0, TEXEL0
#define G_CC_REFLECTRGB             ENVIRONMENT, 0, TEXEL0, SHADE, 0, 0, 0, SHADE
#define G_CC_REFLECTRGBDECALA       ENVIRONMENT, 0, TEXEL0, SHADE, 0, 0, 0, TEXEL0
#define G_CC_HILITERGB              PRIMITIVE, SHADE, TEXEL0, SHADE, 0, 0, 0, SHADE
#define G_CC_HILITERGBA             PRIMITIVE, SHADE, TEXEL0, SHADE, PRIMITIVE, SHADE, TEXEL0, SHADE
#define G_CC_HILITERGBDECALA        PRIMITIVE, SHADE, TEXEL0, SHADE, 0, 0, 0, TEXEL0
#define G_CC_SHADEDECALA            0, 0, 0, SHADE, 0, 0, 0, TEXEL0
#define G_CC_BLENDPE                PRIMITIVE, ENVIRONMENT, TEXEL0, ENVIRONMENT, TEXEL0, 0, SHADE, 0
#define G_CC_BLENDPEDECALA          PRIMITIVE, ENVIRONMENT, TEXEL0, ENVIRONMENT, 0, 0, 0, TEXEL0
#define _G_CC_BLENDPE               ENVIRONMENT, PRIMITIVE, TEXEL0, PRIMITIVE, TEXEL0, 0, SHADE, 0
#define _G_CC_BLENDPEDECALA         ENVIRONMENT, PRIMITIVE, TEXEL0, PRIMITIVE, 0, 0, 0, TEXEL0
#define _G_CC_TWOCOLORTEX           PRIMITIVE, SHADE, TEXEL0, SHADE, 0, 0, 0, SHADE
#define _G_CC_SPARSEST              PRIMITIVE, TEXEL0, LOD_FRACTION, TEXEL0, PRIMITIVE, TEXEL0, LOD_FRACTION, TEXEL0
#define G_CC_TEMPLERP               TEXEL1, TEXEL0, PRIM_LOD_FRAC, TEXEL0, TEXEL1, TEXEL0, PRIM_LOD_FRAC, TEXEL0
#define G_CC_TRILERP                TEXEL1, TEXEL0, LOD_FRACTION, TEXEL0, TEXEL1, TEXEL0, LOD_FRACTION, TEXEL0
#define G_CC_INTERFERENCE           TEXEL0, 0, TEXEL1, 0, TEXEL0, 0, TEXEL1, 0
#define G_CC_1CYUV2RGB              TEXEL0, K4, K5, TEXEL0, 0, 0, 0, SHADE
#define G_CC_YUV2RGB                TEXEL1, K4, K5, TEXEL1, 0, 0, 0, 0
#define G_CC_PASS2                  0, 0, 0, COMBINED, 0, 0, 0, COMBINED
#define G_CC_MODULATEI2             COMBINED, 0, SHADE, 0, 0, 0, 0, SHADE
#define G_CC_MODULATEIA2            COMBINED, 0, SHADE, 0, COMBINED, 0, SHADE, 0
#define G_CC_MODULATERGB2           G_CC_MODULATEI2
#define G_CC_MODULATERGBA2          G_CC_MODULATEIA2
#define G_CC_MODULATEI_PRIM2        COMBINED, 0, PRIMITIVE, 0, 0, 0, 0, PRIMITIVE
#define G_CC_MODULATEIA_PRIM2       COMBINED, 0, PRIMITIVE, 0, COMBINED, 0, PRIMITIVE, 0
#define G_CC_MODULATERGB_PRIM2      G_CC_MODULATEI_PRIM2
#define G_CC_MODULATERGBA_PRIM2     G_CC_MODULATEIA_PRIM2
#define G_CC_DECALRGB2              0, 0, 0, COMBINED, 0, 0, 0, SHADE
#define G_CC_BLENDI2                ENVIRONMENT, SHADE, COMBINED, SHADE, 0, 0, 0, SHADE
#define G_CC_BLENDIA2               ENVIRONMENT, SHADE, COMBINED, SHADE, COMBINED, 0, SHADE, 0
#define G_CC_CHROMA_KEY2            TEXEL0, CENTER, SCALE, 0, 0, 0, 0, 0
#define G_CC_HILITERGB2             ENVIRONMENT, COMBINED, TEXEL0, COMBINED, 0, 0, 0, SHADE
#define G_CC_HILITERGBA2            ENVIRONMENT, COMBINED, TEXEL0, COMBINED, ENVIRONMENT, COMBINED, TEXEL0, COMBINED
#define G_CC_HILITERGBDECALA2       ENVIRONMENT, COMBINED, TEXEL0, COMBINED, 0, 0, 0, TEXEL0
#define G_CC_HILITERGBPASSA2        ENVIRONMENT, COMBINED, TEXEL0, COMBINED, 0, 0, 0, COMBINED

// Internal generalized combiner inputs
#define COMBINED        0
#define TEXEL0          1
#define TEXEL1          2
#define PRIMITIVE       3
#define SHADE           4
#define ENVIRONMENT     5
#define CENTER          6
#define SCALE           7
#define COMBINED_ALPHA  8
#define TEXEL0_ALPHA    9
#define TEXEL1_ALPHA    10
#define PRIMITIVE_ALPHA 11
#define SHADE_ALPHA     12
#define ENV_ALPHA       13
#define LOD_FRACTION    14
#define PRIM_LOD_FRAC   15
#define NOISE           16
#define K4              17
#define K5              18
#define ONE             19
#define ZERO            20
#define UNKNOWN         21


struct UniformLocation
{
    struct {GLint loc; int val;} uTex0, uTex1, uNoise;
    struct {GLint loc; int val;} uEnableFog;
    struct {GLint loc; float val;} uFogMultiplier, uFogOffset, uAlphaRef, uPrimLODFrac, uRenderState, uK4, uK5;
    struct {GLint loc; float val[4];} uEnvColor, uPrimColor, uFogColor;
    struct {GLint loc; float val[2];}  uTexScale, uTexOffset[2], uCacheShiftScale[2],
        uCacheScale[2], uCacheOffset[2];
};

struct ShaderProgram
{
    GLint       program;
    GLint       fragment;
    GLint       vertex;
    int         usesT0;       //uses texcoord0 attrib
    int         usesT1;       //uses texcoord1 attrib
    int         usesCol;      //uses color attrib
    int         usesNoise;    //requires noise texture

    UniformLocation uniforms;
    gDPCombine      combine;
    u32             flags;
    ShaderProgram   *left, *right;
    u32             lastUsed;
};


//dmux flags:
#define SC_IGNORE_RGB0      (1<<0)
#define SC_IGNORE_ALPHA0    (1<<1)
#define SC_IGNORE_RGB1      (1<<2)
#define SC_IGNORE_ALPHA1    (1<<3)

class DecodedMux
{
    public:
        DecodedMux(u64 mux, bool cycle2);

        void hack();
        bool find(int index, int src);
        bool swap(int cycle, int src0, int src1);
        bool replace(int cycle, int src, int dest);

        gDPCombine combine;
        int decode[4][4];
        int flags;
};

extern int CCEncodeA[];
extern int CCEncodeB[];
extern int CCEncodeC[];
extern int CCEncodeD[];
extern int ACEncodeA[];
extern int ACEncodeB[];
extern int ACEncodeC[];
extern int ACEncodeD[];

extern ShaderProgram    *scProgramRoot;
extern ShaderProgram    *scProgramCurrent;
extern int              scProgramChanged;
extern int              scProgramCount;

extern void ShaderCombiner_Init();
extern void ShaderCombiner_Destroy();
extern void ShaderCombiner_DeleteProgram(ShaderProgram *prog);
extern void ShaderCombiner_Set(u64 mux, int flags=-1);
extern ShaderProgram *ShaderCombiner_Compile(DecodedMux *dmux, int flags);

#endif

