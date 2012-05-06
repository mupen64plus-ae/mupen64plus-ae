#ifndef _3DMATH_H
#define _3DMATH_H

#include <string.h>

extern void (*MultMatrix)(float m0[4][4], float m1[4][4], float dest[4][4]);
extern void (*TransformVectorNormalize)(float vec[3], float mtx[4][4]);
extern void (*Normalize)(float v[3]);
extern float (*DotProduct)(float v0[3], float v1[3]);

inline void CopyMatrix( float m0[4][4], float m1[4][4] )
{
    memcpy( m0, m1, 16 * sizeof( float ) );
}

inline void MultMatrix2( float m0[4][4], float m1[4][4] )
{
    float dst[4][4];
    MultMatrix(m0, m1, dst);
    memcpy( m0, dst, sizeof(float) * 16 );
}

inline void Transpose3x3Matrix( float mtx[4][4] )
{
    float tmp;

    tmp = mtx[0][1];
    mtx[0][1] = mtx[1][0];
    mtx[1][0] = tmp;

    tmp = mtx[0][2];
    mtx[0][2] = mtx[2][0];
    mtx[2][0] = tmp;

    tmp = mtx[1][2];
    mtx[1][2] = mtx[2][1];
    mtx[2][1] = tmp;
}

#ifdef __NEON_OPT
void MathInitNeon();
#endif

#endif

