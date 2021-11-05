#include "Glide64/Gfx_1.3.h"
#include <cmath>
#include <arm_neon.h>
#include "Glide64/3dmath.h"

void DotProductMax7FullNeon( float v0[3], float v1[7][3], float lights[7][3], float _vtx[3])
{
    // load v1
    float32x4x3_t _v10 = vld3q_f32(v1[0]);               // load 4x3 mtx interleaved
    float32x2x3_t _v11 = vld3_f32(v1[4]);                // load 2x3 mtx interleaved
    float32x2x3_t _v12 = vld3_dup_f32(v1[6]);            // load 1x3 mtx interleaved
    for(int i = 0; i< 3; i++){
        _v12.val[i][1]=0.0;
    }

    // load lights
    float32x4x3_t _lights0 = vld3q_f32(lights[0]);       // load 4x3 mtx interleaved
    float32x2x3_t _lights1 = vld3_f32(lights[4]);        // load 2x3 mtx interleaved
    float32x2x3_t _lights2 = vld3_dup_f32(lights[6]);    // load 1x3 mtx interleaved

    float32x4_t product0;
    float32x2_t product1;
    float32x2_t product2;
    float32x4_t max = vmovq_n_f32(0.0);
    float32x2_t max1 = vmov_n_f32(0.0);

    // calc product
    product0 = vmulq_n_f32(_v10.val[0],v0[0]);
    product1 = vmul_n_f32(_v11.val[0],v0[0]);
    product2 = vmul_n_f32(_v12.val[0],v0[0]);
    product0 = vmlaq_n_f32(product0, _v10.val[1],v0[1]);
    product1 = vmla_n_f32(product1, _v11.val[1],v0[1]);
    product2 = vmla_n_f32(product2, _v12.val[1],v0[1]);
    product0 = vmlaq_n_f32(product0, _v10.val[2],v0[2]);
    product1 = vmla_n_f32(product1, _v11.val[2],v0[2]);
    product2 = vmla_n_f32(product2, _v12.val[2],v0[2]);

    product0 = vmaxq_f32(product0, max);
    product1 = vmax_f32(product1, max1);
    product2 = vmax_f32(product2, max1);

    // multiply product with lights
    _lights0.val[0] = vmulq_f32(_lights0.val[0],product0);
    _lights1.val[0] = vmul_f32(_lights1.val[0],product1);
    _lights1.val[0] = vmla_f32(_lights1.val[0],_lights2.val[0],product2);
    _lights0.val[1] = vmulq_f32(_lights0.val[1],product0);
    _lights1.val[1] = vmul_f32(_lights1.val[1],product1);
    _lights1.val[1] = vmla_f32(_lights1.val[1],_lights2.val[1],product2);
    _lights0.val[2] = vmulq_f32(_lights0.val[2],product0);
    _lights1.val[2] = vmul_f32(_lights1.val[2],product1);
    _lights1.val[2] = vmla_f32(_lights1.val[2],_lights2.val[2],product2);

    // add x, y and z values
    float32x2_t d00 = vadd_f32(vget_high_f32(_lights0.val[0]),vget_low_f32(_lights0.val[0]));
    float32x2_t d10 = vadd_f32(vget_high_f32(_lights0.val[1]),vget_low_f32(_lights0.val[1]));
    float32x2_t d20 = vadd_f32(vget_high_f32(_lights0.val[2]),vget_low_f32(_lights0.val[2]));
    d00 = vadd_f32(d00,_lights1.val[0]);
    d10 = vadd_f32(d10,_lights1.val[1]);
    d20 = vadd_f32(d20,_lights1.val[2]);
    d00 = vpadd_f32(d00,d00);
    d10 = vpadd_f32(d10,d10);
    d20 = vpadd_f32(d20,d20);

    _vtx[0] += d00[0];
    _vtx[1] += d10[0];
    _vtx[2] += d20[0];
}

void DotProductMax4FullNeon( float v0[3], float v1[4][3], float lights[4][3], float vtx[3])
{
    float32x4x3_t _v1 = vld3q_f32(v1[0]);               // load 4x3 mtx interleaved
    float32x4x3_t _lights = vld3q_f32(lights[0]);       // load 4x3 mtx interleaved

    float32x4_t product;
    float32x4_t max = vmovq_n_f32(0.0);

    product = vmulq_n_f32(_v1.val[0],v0[0]);
    product = vmlaq_n_f32(product, _v1.val[1],v0[1]);
    product = vmlaq_n_f32(product, _v1.val[2],v0[2]);

    product = vmaxq_f32(product, max);

    _lights.val[0] = vmulq_f32(_lights.val[0],product);
    _lights.val[1] = vmulq_f32(_lights.val[1],product);
    _lights.val[2] = vmulq_f32(_lights.val[2],product);

    float32x2_t d00 = vadd_f32(vget_high_f32(_lights.val[0]),vget_low_f32(_lights.val[0]));
    float32x2_t d10 = vadd_f32(vget_high_f32(_lights.val[1]),vget_low_f32(_lights.val[1]));
    float32x2_t d20 = vadd_f32(vget_high_f32(_lights.val[2]),vget_low_f32(_lights.val[2]));
    d00 = vpadd_f32(d00,d00);
    d10 = vpadd_f32(d10,d10);
    d20 = vpadd_f32(d20,d20);

    vtx[0] += d00[0];
    vtx[1] += d10[0];
    vtx[2] += d20[0];
}

void calc_light (VERTEX *v)
{
    float color[3] = {rdp.light[rdp.num_lights].r, rdp.light[rdp.num_lights].g, rdp.light[rdp.num_lights].b};

	int count = rdp.num_lights - 1;

	// This doesn't work due to different data structure
	/*
	while (count >= 6) {
		DotProductMax7FullNeon(rdp.light_vector[rdp.num_lights - count - 1],
						 (float (*)[3])v->vec,
						 (float (*)[3])&(rdp.light[rdp.num_lights - count - 1]).r,
						 color);
		count -= 7;
	}

	while (count >= 3) {
		DotProductMax4FullNeon(rdp.light_vector[rdp.num_lights - count - 1],
						 (float (*)[3])v->vec,
						 (float (*)[3])&(rdp.light[rdp.num_lights - count - 1]).r,
						 color);
		count -= 4;
	}
    */

    while (count >= 0)
    {
        float intensity = DotProduct (rdp.light_vector[rdp.num_lights - count - 1], v->vec);

        if (intensity > 0.0f)
        {
            color[0] += rdp.light[rdp.num_lights - count - 1].r * intensity;
            color[1] += rdp.light[rdp.num_lights - count - 1].g * intensity;
            color[2] += rdp.light[rdp.num_lights - count - 1].b * intensity;
        }

        --count;
    }

    if (color[0] > 1.0f) color[0] = 1.0f;
    if (color[1] > 1.0f) color[1] = 1.0f;
    if (color[2] > 1.0f) color[2] = 1.0f;

    v->r = (wxUint8)(color[0]*255.0f);
    v->g = (wxUint8)(color[1]*255.0f);
    v->b = (wxUint8)(color[2]*255.0f);
}

void TransformVectorNormalize(float* vec, float* dst, float mtx[4][4])
{
	// Load mtx
	float32x4x4_t _mtx;
	_mtx.val[0] = vld1q_f32(mtx[0]);
	_mtx.val[1] = vld1q_f32(mtx[1]);
	_mtx.val[2] = vld1q_f32(mtx[2]);
	_mtx.val[3] = vld1q_f32(mtx[3]);

	// Multiply and add
	float32x4_t product;
	product = vmulq_n_f32(_mtx.val[0], vec[0]);
	product = vmlaq_n_f32(product, _mtx.val[1], vec[1]);
	product = vmlaq_n_f32(product, _mtx.val[2], vec[2]);

	// Normalize
	float32x2_t product0 = {product[0],product[1]};
	float32x2_t product1 = {product[2],product[3]};
	float32x2_t temp;

	temp = vmul_f32(product0, product0);
	temp = vpadd_f32(temp, temp);
	temp = vmla_f32(temp, product1, product1);       // temp[0] is important

	float32x2_t recpSqrtEst;
	float32x2_t recp;
	float32x2_t prod;

	recpSqrtEst = vrsqrte_f32(temp);
	prod =        vmul_f32(recpSqrtEst,temp);
	recp =        vrsqrts_f32(prod,recpSqrtEst);
	recpSqrtEst = vmul_f32(recpSqrtEst,recp);
	prod =        vmul_f32(recpSqrtEst,temp);
	recp =        vrsqrts_f32(prod,recpSqrtEst);
	recpSqrtEst = vmul_f32(recpSqrtEst,recp);

	product = vmulq_n_f32(product, recpSqrtEst[0]);

	// Store mtx
	dst[0] = product[0];
	dst[1] = product[1];
	dst[2] = product[2];
}

void NormalizeVector_NEON(float* v)
{
    // Load vector
    float32x4_t product = {v[0], v[1], v[2], 0.0};

    // Normalize
    float32x2_t product0 = {product[0],product[1]};
    float32x2_t product1 = {product[2],product[3]};
    float32x2_t temp;

    temp = vmul_f32(product0, product0);
    temp = vpadd_f32(temp, temp);
    temp = vmla_f32(temp, product1, product1);       // temp[0] is important

    float32x2_t recpSqrtEst;
    float32x2_t recp;
    float32x2_t prod;

    recpSqrtEst = vrsqrte_f32(temp);
    prod =        vmul_f32(recpSqrtEst,temp);
    recp =        vrsqrts_f32(prod,recpSqrtEst);
    recpSqrtEst = vmul_f32(recpSqrtEst,recp);
    prod =        vmul_f32(recpSqrtEst,temp);
    recp =        vrsqrts_f32(prod,recpSqrtEst);
    recpSqrtEst = vmul_f32(recpSqrtEst,recp);

    product = vmulq_n_f32(product, recpSqrtEst[0]);

    // Store vector
    v[0] = product[0];
    v[1] = product[1];
    v[2] = product[2];
}

void calc_linear (VERTEX *v)
{
    if (settings.force_calc_sphere)
    {
        calc_sphere(v);
        return;
    }
    DECLAREALIGN16VAR(vec[3]);

	TransformVectorNormalize(v->vec, vec, rdp.model);

    float x, y;
    if (!rdp.use_lookat)
    {
        x = vec[0];
        y = vec[1];
    }
    else
    {
        x = DotProduct (rdp.lookat[0], vec);
        y = DotProduct (rdp.lookat[1], vec);
    }

    if (x > 1.0f)
        x = 1.0f;
    else if (x < -1.0f)
        x = -1.0f;
    if (y > 1.0f)
        y = 1.0f;
    else if (y < -1.0f)
        y = -1.0f;

    if (rdp.cur_cache[0])
    {
        // scale >> 6 is size to map to
        v->ou = (acosf(x)/3.141592654f) * (rdp.tiles[rdp.cur_tile].org_s_scale >> 6);
        v->ov = (acosf(y)/3.141592654f) * (rdp.tiles[rdp.cur_tile].org_t_scale >> 6);
    }
    v->uv_scaled = 1;
#ifdef EXTREME_LOGGING
    FRDP ("calc linear u: %f, v: %f\n", v->ou, v->ov);
#endif
}

void calc_sphere (VERTEX *v)
{
//  LRDP("calc_sphere\n");
    DECLAREALIGN16VAR(vec[3]);
    int s_scale, t_scale;
    if (settings.hacks&hack_Chopper)
    {
        s_scale = min(rdp.tiles[rdp.cur_tile].org_s_scale >> 6, rdp.tiles[rdp.cur_tile].lr_s);
        t_scale = min(rdp.tiles[rdp.cur_tile].org_t_scale >> 6, rdp.tiles[rdp.cur_tile].lr_t);
    }
    else
    {
        s_scale = rdp.tiles[rdp.cur_tile].org_s_scale >> 6;
        t_scale = rdp.tiles[rdp.cur_tile].org_t_scale >> 6;
    }

	TransformVectorNormalize(v->vec, vec, rdp.model);
    float x, y;
    if (!rdp.use_lookat)
    {
        x = vec[0];
        y = vec[1];
    }
    else
    {
        x = DotProduct (rdp.lookat[0], vec);
        y = DotProduct (rdp.lookat[1], vec);
    }
    v->ou = (x * 0.5f + 0.5f) * s_scale;
    v->ov = (y * 0.5f + 0.5f) * t_scale;
    v->uv_scaled = 1;
#ifdef EXTREME_LOGGING
    FRDP ("calc sphere u: %f, v: %f\n", v->ou, v->ov);
#endif
}

float DotProductC(float *v1, float *v2)
{
    float result;
    result = v1[0]*v2[0] + v1[1]*v2[1] + v1[2]*v2[2];
    return(result);
}

void TransformVector_NEON(float *src, float *dst, float mat[4][4])
{
    // Load vtx
    float32x4_t _vtx = vld1q_f32(src);

    // Load mtx
    float32x4_t _mtx0 = vld1q_f32(mat[0]);
    float32x4_t _mtx1 = vld1q_f32(mat[1]);
    float32x4_t _mtx2 = vld1q_f32(mat[2]);
    float32x4_t _mtx3 = vld1q_f32(mat[3]);

    // Multiply and add
    _mtx0 = vmlaq_n_f32(_mtx3, _mtx0, _vtx[0]);    // _mtx0 = _mtx3 + _mtx0 * _vtx[0]
    _mtx0 = vmlaq_n_f32(_mtx0, _mtx1, _vtx[1]);    // _mtx0 = _mtx0 + _mtx1 * _vtx[1]
    _mtx0 = vmlaq_n_f32(_mtx0, _mtx2, _vtx[2]);    // _mtx0 = _mtx0 + _mtx2 * _vtx[2]

    // Store vtx
    vst1q_f32(dst, _mtx0);
}

void InverseTransformVector_NEON(float *src, float *dst, float mat[4][4])
{
    float32x4x4_t _mtx = vld4q_f32(mat[0]);                         // load 4x4 mtx interleaved

    _mtx.val[0] = vmulq_n_f32(_mtx.val[0], src[0]);                 // mtx[0][0]=mtx[0][0]*_vtx[0]
    // mtx[0][1]=mtx[0][1]*_vtx[0]
    // mtx[0][2]=mtx[0][2]*_vtx[0]
    _mtx.val[0] = vmlaq_n_f32(_mtx.val[0], _mtx.val[1], src[1]);    // mtx[0][0]+=mtx[1][0]*_vtx[1]
    // mtx[0][1]+=mtx[1][1]*_vtx[1]
    // mtx[0][2]+=mtx[1][2]*_vtx[1]
    _mtx.val[0] = vmlaq_n_f32(_mtx.val[0], _mtx.val[2], src[2]);    // mtx[0][0]+=mtx[2][0]*_vtx[2]
    // mtx[0][1]+=mtx[2][1]*_vtx[2]
    // mtx[0][2]+=mtx[2][2]*_vtx[2]

    dst[0] = _mtx.val[0][0];                                              // store vec[0]
    dst[1] = _mtx.val[0][1];                                              // store vec[1]
    dst[2] = _mtx.val[0][2];                                              // store vec[2]
}

void MultMatrix_NEON( float m1[4][4], float m0[4][4], float dest[4][4])
{
	// Load m0
	float32x4x4_t _m0;
	_m0.val[0] = vld1q_f32(m0[0]);
	_m0.val[1] = vld1q_f32(m0[1]);
	_m0.val[2] = vld1q_f32(m0[2]);
	_m0.val[3] = vld1q_f32(m0[3]);

	// Load m1
	float32x4x4_t _m1;
	_m1.val[0] = vld1q_f32(m1[0]);
	_m1.val[1] = vld1q_f32(m1[1]);
	_m1.val[2] = vld1q_f32(m1[2]);
	_m1.val[3] = vld1q_f32(m1[3]);

	float32x4x4_t _dest;

	_dest.val[0] = vmulq_n_f32(_m0.val[0], _m1.val[0][0]);
	_dest.val[1] = vmulq_n_f32(_m0.val[0], _m1.val[1][0]);
	_dest.val[2] = vmulq_n_f32(_m0.val[0], _m1.val[2][0]);
	_dest.val[3] = vmulq_n_f32(_m0.val[0], _m1.val[3][0]);
	_dest.val[0] = vmlaq_n_f32(_dest.val[0], _m0.val[1], _m1.val[0][1]);
	_dest.val[1] = vmlaq_n_f32(_dest.val[1], _m0.val[1], _m1.val[1][1]);
	_dest.val[2] = vmlaq_n_f32(_dest.val[2], _m0.val[1], _m1.val[2][1]);
	_dest.val[3] = vmlaq_n_f32(_dest.val[3], _m0.val[1], _m1.val[3][1]);
	_dest.val[0] = vmlaq_n_f32(_dest.val[0], _m0.val[2], _m1.val[0][2]);
	_dest.val[1] = vmlaq_n_f32(_dest.val[1], _m0.val[2], _m1.val[1][2]);
	_dest.val[2] = vmlaq_n_f32(_dest.val[2], _m0.val[2], _m1.val[2][2]);
	_dest.val[3] = vmlaq_n_f32(_dest.val[3], _m0.val[2], _m1.val[3][2]);
	_dest.val[0] = vmlaq_n_f32(_dest.val[0], _m0.val[3], _m1.val[0][3]);
	_dest.val[1] = vmlaq_n_f32(_dest.val[1], _m0.val[3], _m1.val[1][3]);
	_dest.val[2] = vmlaq_n_f32(_dest.val[2], _m0.val[3], _m1.val[2][3]);
	_dest.val[3] = vmlaq_n_f32(_dest.val[3], _m0.val[3], _m1.val[3][3]);

	vst1q_f32(dest[0], _dest.val[0]);
	vst1q_f32(dest[1], _dest.val[1]);
	vst1q_f32(dest[2], _dest.val[2]);
	vst1q_f32(dest[3], _dest.val[3]);
}

void MulMatricesC(float m1[4][4],float m2[4][4],float r[4][4])
{
    float row[4][4];
    unsigned int i, j;

    for (i = 0; i < 4; i++)
        for (j = 0; j < 4; j++)
            row[i][j] = m2[i][j];
    for (i = 0; i < 4; i++)
    {
        // auto-vectorizable algorithm
        // vectorized loop style, such that compilers can
        // easily create optimized SSE instructions.
        float leftrow[4];
        float summand[4][4];

        for (j = 0; j < 4; j++)
            leftrow[j] = m1[i][j];

        for (j = 0; j < 4; j++)
            summand[0][j] = leftrow[0] * row[0][j];
        for (j = 0; j < 4; j++)
            summand[1][j] = leftrow[1] * row[1][j];
        for (j = 0; j < 4; j++)
            summand[2][j] = leftrow[2] * row[2][j];
        for (j = 0; j < 4; j++)
            summand[3][j] = leftrow[3] * row[3][j];

        for (j = 0; j < 4; j++)
            r[i][j] =
                    summand[0][j]
                    + summand[1][j]
                    + summand[2][j]
                    + summand[3][j]
                    ;
    }
}

// 2008.03.29 H.Morii - added SSE 3DNOW! 3x3 1x3 matrix multiplication
//                      and 3DNOW! 4x4 4x4 matrix multiplication
// 2011-01-03 Balrog - removed because is in NASM format and not 64-bit compatible
// This will need fixing.
MULMATRIX MulMatrices = MultMatrix_NEON;
TRANSFORMVECTOR TransformVector = TransformVector_NEON;
TRANSFORMVECTOR InverseTransformVector = InverseTransformVector_NEON;
DOTPRODUCT DotProduct = DotProductC;
NORMALIZEVECTOR NormalizeVector = NormalizeVector_NEON;


void math_init()
{

}
