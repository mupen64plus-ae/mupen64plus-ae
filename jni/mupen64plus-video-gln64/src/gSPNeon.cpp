#include "gSP.h"
#include "OpenGL.h"

#ifdef __VEC4_OPT
static void gSPTransformVertex4NEON(u32 v, float mtx[4][4])
{
    float *ptr = &OGL.triangles.vertices[v].x;

#if 0
    volatile int tmp0, tmp1;
	asm volatile (
    "vld1.32 		{d0, d1}, [%1, :128]		  	\n\t"	//q0 = {x,y,z,w}
    "add 		    %1, %1, %4   		  	\n\t"	//q0 = {x,y,z,w}
    "vld1.32 		{d18, d19}, [%0, :128]!		\n\t"	//q9 = m
    "vld1.32 		{d2, d3}, [%1, :128]	    	\n\t"	//q1 = {x,y,z,w}
    "add 		    %1, %1, %4	    	  	\n\t"	//q0 = {x,y,z,w}
    "vld1.32 		{d20, d21}, [%0, :128]!       \n\t"	//q10 = m
    "vld1.32 		{d4, d5}, [%1, :128]	        \n\t"	//q2 = {x,y,z,w}
    "add 		    %1, %1, %4		      	\n\t"	//q0 = {x,y,z,w}
    "vld1.32 		{d22, d23}, [%0, :128]!       \n\t"	//q11 = m
    "vld1.32 		{d6, d7}, [%1, :128]	        \n\t"	//q3 = {x,y,z,w}
    "vld1.32 		{d24, d25}, [%0, :128]        \n\t"	//q12 = m
    "sub 		    %1, %1, %6   		  	\n\t"	//q0 = {x,y,z,w}

    "vmov.f32 		q13, q12  			\n\t"	//q13 = q12
    "vmov.f32 		q14, q12   			\n\t"	//q14 = q12
    "vmov.f32 		q15, q12    			\n\t"	//q15 = q12

    "vmla.f32 		q12, q9, d0[0]			\n\t"	//q12 = q9*d0[0]
    "vmla.f32 		q13, q9, d2[0]			\n\t"	//q13 = q9*d0[0]
    "vmla.f32 		q14, q9, d4[0]			\n\t"	//q14 = q9*d0[0]
    "vmla.f32 		q15, q9, d6[0]			\n\t"	//q15 = q9*d0[0]
    "vmla.f32 		q12, q10, d0[1]			\n\t"	//q12 = q10*d0[1]
    "vmla.f32 		q13, q10, d2[1]			\n\t"	//q13 = q10*d0[1]
    "vmla.f32 		q14, q10, d4[1]			\n\t"	//q14 = q10*d0[1]
    "vmla.f32 		q15, q10, d6[1]			\n\t"	//q15 = q10*d0[1]
    "vmla.f32 		q12, q11, d1[0]			\n\t"	//q12 = q11*d1[0]
    "vmla.f32 		q13, q11, d3[0]			\n\t"	//q13 = q11*d1[0]
    "vmla.f32 		q14, q11, d5[0]			\n\t"	//q14 = q11*d1[0]
    "vmla.f32 		q15, q11, d7[0]			\n\t"	//q15 = q11*d1[0]

    "add 		    %0, %1, %4 		      	\n\t"	//q0 = {x,y,z,w}
    "add 		    %2, %1, %5 		      	\n\t"	//q0 = {x,y,z,w}
    "add 		    %3, %1, %6 	    	  	\n\t"	//q0 = {x,y,z,w}
    "vst1.32 		{d24, d25}, [%1, :128] 		\n\t"	//q12
    "vst1.32 		{d26, d27}, [%0, :128] 	    \n\t"	//q13
    "vst1.32 		{d28, d29}, [%2, :128] 	    \n\t"	//q14
    "vst1.32 		{d30, d31}, [%3, :128]     	\n\t"	//q15
	: "+&r"(mtx), "+&r"(ptr), "+r"(tmp0), "+r"(tmp1)
	: "I"(sizeof(SPVertex)),"I"(2 * sizeof(SPVertex)), "I"(3 * sizeof(SPVertex))
    : "d0", "d1", "d2", "d3", "d4", "d5", "d6", "d7",
      "d18","d19", "d20", "d21", "d22", "d23", "d24",
      "d25", "d26", "d27", "d28", "d29", "d30", "d31", "memory"
	);
#else
	asm volatile (
	"vld1.32 		{d0, d1}, [%1]		  	\n\t"	//q0 = {x,y,z,w}
	"add 		    %1, %1, %2   		  	\n\t"	//q0 = {x,y,z,w}
	"vld1.32 		{d2, d3}, [%1]	    	\n\t"	//q1 = {x,y,z,w}
	"add 		    %1, %1, %2 	    	  	\n\t"	//q0 = {x,y,z,w}
	"vld1.32 		{d4, d5}, [%1]	        \n\t"	//q2 = {x,y,z,w}
	"add 		    %1, %1, %2 		      	\n\t"	//q0 = {x,y,z,w}
	"vld1.32 		{d6, d7}, [%1]	        \n\t"	//q3 = {x,y,z,w}
    "sub 		    %1, %1, %3   		  	\n\t"	//q0 = {x,y,z,w}

	"vld1.32 		{d18, d19}, [%0]!		\n\t"	//q9 = m
	"vld1.32 		{d20, d21}, [%0]!       \n\t"	//q10 = m
	"vld1.32 		{d22, d23}, [%0]!       \n\t"	//q11 = m
	"vld1.32 		{d24, d25}, [%0]        \n\t"	//q12 = m

	"vmov.f32 		q13, q12    			\n\t"	//q13 = q12
	"vmov.f32 		q14, q12    			\n\t"	//q14 = q12
	"vmov.f32 		q15, q12    			\n\t"	//q15 = q12

	"vmla.f32 		q12, q9, d0[0]			\n\t"	//q12 = q9*d0[0]
	"vmla.f32 		q13, q9, d2[0]			\n\t"	//q13 = q9*d0[0]
	"vmla.f32 		q14, q9, d4[0]			\n\t"	//q14 = q9*d0[0]
	"vmla.f32 		q15, q9, d6[0]			\n\t"	//q15 = q9*d0[0]
	"vmla.f32 		q12, q10, d0[1]			\n\t"	//q12 = q10*d0[1]
	"vmla.f32 		q13, q10, d2[1]			\n\t"	//q13 = q10*d0[1]
	"vmla.f32 		q14, q10, d4[1]			\n\t"	//q14 = q10*d0[1]
	"vmla.f32 		q15, q10, d6[1]			\n\t"	//q15 = q10*d0[1]
	"vmla.f32 		q12, q11, d1[0]			\n\t"	//q12 = q11*d1[0]
	"vmla.f32 		q13, q11, d3[0]			\n\t"	//q13 = q11*d1[0]
	"vmla.f32 		q14, q11, d5[0]			\n\t"	//q14 = q11*d1[0]
	"vmla.f32 		q15, q11, d7[0]			\n\t"	//q15 = q11*d1[0]

	"vst1.32 		{d24, d25}, [%1] 		\n\t"	//q12
	"add 		    %1, %1, %2 		      	\n\t"	//q0 = {x,y,z,w}
	"vst1.32 		{d26, d27}, [%1] 	    \n\t"	//q13
	"add 		    %1, %1, %2 	    	  	\n\t"	//q0 = {x,y,z,w}
	"vst1.32 		{d28, d29}, [%1] 	    \n\t"	//q14
	"add 		    %1, %1, %2   		  	\n\t"	//q0 = {x,y,z,w}
	"vst1.32 		{d30, d31}, [%1]     	\n\t"	//q15

	: "+&r"(mtx), "+&r"(ptr)
	: "I"(sizeof(SPVertex)), "I"(3 * sizeof(SPVertex))
    : "d0", "d1", "d2", "d3", "d4", "d5", "d6", "d7",
      "d18","d19", "d20", "d21", "d22", "d23", "d24",
      "d25", "d26", "d27", "d28", "d29", "d30", "d31", "memory"
	);
#endif
}

//4x Transform normal and normalize
static void gSPTransformNormal4NEON(u32 v, float mtx[4][4])
{
    void *ptr = (void*)&OGL.triangles.vertices[v].nx;
	asm volatile (
    "vld1.32 		{d0, d1}, [%1]		  	\n\t"	//q0 = {x,y,z,w}
	"add 		    %1, %1, %2  		  	\n\t"	//q0 = {x,y,z,w}
	"vld1.32 		{d2, d3}, [%1]	    	\n\t"	//q1 = {x,y,z,w}
	"add 		    %1, %1, %2  		  	\n\t"	//q0 = {x,y,z,w}
	"vld1.32 		{d4, d5}, [%1]	        \n\t"	//q2 = {x,y,z,w}
	"add 		    %1, %1, %2  		  	\n\t"	//q0 = {x,y,z,w}
	"vld1.32 		{d6, d7}, [%1]	        \n\t"	//q3 = {x,y,z,w}
    "sub 		    %1, %1, %3  		  	\n\t"	//q0 = {x,y,z,w}

	"vld1.32 		{d18, d19}, [%0]!		\n\t"	//q9 = m
	"vld1.32 		{d20, d21}, [%0]!	    \n\t"	//q10 = m+16
	"vld1.32 		{d22, d23}, [%0]    	\n\t"	//q11 = m+32

	"vmul.f32 		q12, q9, d0[0]			\n\t"	//q12 = q9*d0[0]
	"vmul.f32 		q13, q9, d2[0]			\n\t"	//q13 = q9*d2[0]
    "vmul.f32 		q14, q9, d4[0]			\n\t"	//q14 = q9*d4[0]
    "vmul.f32 		q15, q9, d6[0]			\n\t"	//q15 = q9*d6[0]

    "vmla.f32 		q12, q10, d0[1]			\n\t"	//q12 += q10*q0[1]
    "vmla.f32 		q13, q10, d2[1]			\n\t"	//q13 += q10*q2[1]
    "vmla.f32 		q14, q10, d4[1]			\n\t"	//q14 += q10*q4[1]
    "vmla.f32 		q15, q10, d6[1]			\n\t"	//q15 += q10*q6[1]

	"vmla.f32 		q12, q11, d1[0]			\n\t"	//q12 += q11*d1[0]
	"vmla.f32 		q13, q11, d3[0]			\n\t"	//q13 += q11*d3[0]
	"vmla.f32 		q14, q11, d5[0]			\n\t"	//q14 += q11*d5[0]
	"vmla.f32 		q15, q11, d7[0]			\n\t"	//q15 += q11*d7[0]

    "vmul.f32 		q0, q12, q12			\n\t"	//q0 = q12*q12
    "vmul.f32 		q1, q13, q13			\n\t"	//q1 = q13*q13
    "vmul.f32 		q2, q14, q14			\n\t"	//q2 = q14*q14
    "vmul.f32 		q3, q15, q15			\n\t"	//q3 = q15*q15

    "vpadd.f32 		d0, d0  				\n\t"	//d0[0] = d0[0] + d0[1]
    "vpadd.f32 		d2, d2  				\n\t"	//d2[0] = d2[0] + d2[1]
    "vpadd.f32 		d4, d4  				\n\t"	//d4[0] = d4[0] + d4[1]
    "vpadd.f32 		d6, d6  				\n\t"	//d6[0] = d6[0] + d6[1]

    "vmov.f32    	s1, s2  				\n\t"	//d0[1] = d1[0]
    "vmov.f32 	    s5, s6  				\n\t"	//d2[1] = d3[0]
    "vmov.f32 	    s9, s10  				\n\t"	//d4[1] = d5[0]
    "vmov.f32    	s13, s14  				\n\t"	//d6[1] = d7[0]

    "vpadd.f32 		d0, d0, d2  			\n\t"	//d0 = {d0[0] + d0[1], d2[0] + d2[1]}
    "vpadd.f32 		d1, d4, d6  			\n\t"	//d1 = {d4[0] + d4[1], d6[0] + d6[1]}

	"vmov.f32 		q1, q0					\n\t"	//q1 = q0
	"vrsqrte.f32 	q0, q0					\n\t"	//q0 = ~ 1.0 / sqrt(q0)
	"vmul.f32 		q2, q0, q1				\n\t"	//q2 = q0 * q1
	"vrsqrts.f32 	q3, q2, q0				\n\t"	//q3 = (3 - q0 * q2) / 2
	"vmul.f32 		q0, q0, q3				\n\t"	//q0 = q0 * q3
	"vmul.f32 		q2, q0, q1				\n\t"	//q2 = q0 * q1
	"vrsqrts.f32 	q3, q2, q0				\n\t"	//q3 = (3 - q0 * q2) / 2
	"vmul.f32 		q0, q0, q3				\n\t"	//q0 = q0 * q3

	"vmul.f32 		q3, q15, d1[1]			\n\t"	//q3 = q15*d1[1]
	"vmul.f32 		q2, q14, d1[0]			\n\t"	//q2 = q14*d1[0]
	"vmul.f32 		q1, q13, d0[1]			\n\t"	//q1 = q13*d0[1]
	"vmul.f32 		q0, q12, d0[0]			\n\t"	//q0 = q12*d0[0]

	"vst1.32 		{d0, d1}, [%1]  	    \n\t"	//d0={nx,ny,nz,pad}
	"add 		    %1, %1, %2   		  	\n\t"	//q0 = {x,y,z,w}
	"vst1.32 		{d2, d3}, [%1]  	    \n\t"	//d2={nx,ny,nz,pad}
	"add 		    %1, %1, %2  		  	\n\t"	//q0 = {x,y,z,w}
	"vst1.32 		{d4, d5}, [%1]  	    \n\t"	//d4={nx,ny,nz,pad}
	"add 		    %1, %1, %2  		  	\n\t"	//q0 = {x,y,z,w}
    "vst1.32 		{d6, d7}, [%1]        	\n\t"	//d6={nx,ny,nz,pad}

    : "+&r"(mtx), "+&r"(ptr)
    : "I"(sizeof(SPVertex)), "I"(3 * sizeof(SPVertex))
    : "d0", "d1", "d2", "d3", "d4", "d5", "d6", "d7",
      "d16","d17", "d18","d19", "d20", "d21", "d22",
      "d23", "d24", "d25", "d26", "d27", "d28", "d29",
      "d30", "d31", "memory"
	);
}

static void gSPLightVertex4NEON(u32 v)
{
    volatile float result[16];

 	volatile int i = gSP.numLights;
    volatile int tmp = 0;
    volatile void *ptr0 = &(gSP.lights[0].r);
    volatile void *ptr1 = &(OGL.triangles.vertices[v].nx);
    volatile void *ptr2 = result;
	volatile void *ptr3 = gSP.matrix.modelView[gSP.matrix.modelViewi];
	asm volatile (
    "vld1.32 		{d0, d1}, [%1]		  	\n\t"	//q0 = {x,y,z,w}
	"add 		    %1, %1, %2 		      	\n\t"	//q0 = {x,y,z,w}
	"vld1.32 		{d2, d3}, [%1]	    	\n\t"	//q1 = {x,y,z,w}
	"add 		    %1, %1, %2 	    	  	\n\t"	//q0 = {x,y,z,w}
	"vld1.32 		{d4, d5}, [%1]	        \n\t"	//q2 = {x,y,z,w}
	"add 		    %1, %1, %2   		  	\n\t"	//q0 = {x,y,z,w}
	"vld1.32 		{d6, d7}, [%1]	        \n\t"	//q3 = {x,y,z,w}
    "sub 		    %1, %1, %3   		  	\n\t"	//q0 = {x,y,z,w}

	"vld1.32 		{d18, d19}, [%0]!		\n\t"	//q9 = m
	"vld1.32 		{d20, d21}, [%0]!	    \n\t"	//q10 = m+16
	"vld1.32 		{d22, d23}, [%0]    	\n\t"	//q11 = m+32

	"vmul.f32 		q12, q9, d0[0]			\n\t"	//q12 = q9*d0[0]
	"vmul.f32 		q13, q9, d2[0]			\n\t"	//q13 = q9*d2[0]
    "vmul.f32 		q14, q9, d4[0]			\n\t"	//q14 = q9*d4[0]
    "vmul.f32 		q15, q9, d6[0]			\n\t"	//q15 = q9*d6[0]

    "vmla.f32 		q12, q10, d0[1]			\n\t"	//q12 += q10*q0[1]
    "vmla.f32 		q13, q10, d2[1]			\n\t"	//q13 += q10*q2[1]
    "vmla.f32 		q14, q10, d4[1]			\n\t"	//q14 += q10*q4[1]
    "vmla.f32 		q15, q10, d6[1]			\n\t"	//q15 += q10*q6[1]

	"vmla.f32 		q12, q11, d1[0]			\n\t"	//q12 += q11*d1[0]
	"vmla.f32 		q13, q11, d3[0]			\n\t"	//q13 += q11*d3[0]
	"vmla.f32 		q14, q11, d5[0]			\n\t"	//q14 += q11*d5[0]
	"vmla.f32 		q15, q11, d7[0]			\n\t"	//q15 += q11*d7[0]

    "vmul.f32 		q0, q12, q12			\n\t"	//q0 = q12*q12
    "vmul.f32 		q1, q13, q13			\n\t"	//q1 = q13*q13
    "vmul.f32 		q2, q14, q14			\n\t"	//q2 = q14*q14
    "vmul.f32 		q3, q15, q15			\n\t"	//q3 = q15*q15

    "vpadd.f32 		d0, d0  				\n\t"	//d0[0] = d0[0] + d0[1]
    "vpadd.f32 		d2, d2  				\n\t"	//d2[0] = d2[0] + d2[1]
    "vpadd.f32 		d4, d4  				\n\t"	//d4[0] = d4[0] + d4[1]
    "vpadd.f32 		d6, d6  				\n\t"	//d6[0] = d6[0] + d6[1]

    "vmov.f32    	s1, s2  				\n\t"	//d0[1] = d1[0]
    "vmov.f32 	    s5, s6  				\n\t"	//d2[1] = d3[0]
    "vmov.f32 	    s9, s10  				\n\t"	//d4[1] = d5[0]
    "vmov.f32    	s13, s14  				\n\t"	//d6[1] = d7[0]

    "vpadd.f32 		d0, d0, d2  			\n\t"	//d0 = {d0[0] + d0[1], d2[0] + d2[1]}
    "vpadd.f32 		d1, d4, d6  			\n\t"	//d1 = {d4[0] + d4[1], d6[0] + d6[1]}

	"vmov.f32 		q1, q0					\n\t"	//q1 = q0
	"vrsqrte.f32 	q0, q0					\n\t"	//q0 = ~ 1.0 / sqrt(q0)
	"vmul.f32 		q2, q0, q1				\n\t"	//q2 = q0 * q1
	"vrsqrts.f32 	q3, q2, q0				\n\t"	//q3 = (3 - q0 * q2) / 2
	"vmul.f32 		q0, q0, q3				\n\t"	//q0 = q0 * q3
	"vmul.f32 		q2, q0, q1				\n\t"	//q2 = q0 * q1
	"vrsqrts.f32 	q3, q2, q0				\n\t"	//q3 = (3 - q0 * q2) / 2
	"vmul.f32 		q0, q0, q3				\n\t"	//q0 = q0 * q3

	"vmul.f32 		q3, q15, d1[1]			\n\t"	//q3 = q15*d1[1]
	"vmul.f32 		q2, q14, d1[0]			\n\t"	//q2 = q14*d1[0]
	"vmul.f32 		q1, q13, d0[1]			\n\t"	//q1 = q13*d0[1]
	"vmul.f32 		q0, q12, d0[0]			\n\t"	//q0 = q12*d0[0]

	"vst1.32 		{d0, d1}, [%1]  	    \n\t"	//d0={nx,ny,nz,pad}
	"add 		    %1, %1, %2 		  	    \n\t"	//q0 = {x,y,z,w}
	"vst1.32 		{d2, d3}, [%1]  	    \n\t"	//d2={nx,ny,nz,pad}
	"add 		    %1, %1, %2 		  	    \n\t"	//q0 = {x,y,z,w}
	"vst1.32 		{d4, d5}, [%1]  	    \n\t"	//d4={nx,ny,nz,pad}
	"add 		    %1, %1, %2 		  	    \n\t"	//q0 = {x,y,z,w}
    "vst1.32 		{d6, d7}, [%1]        	\n\t"	//d6={nx,ny,nz,pad}

    : "+&r"(ptr3), "+&r"(ptr1)
    : "I"(sizeof(SPVertex)), "I"(3 * sizeof(SPVertex))
    : "d0", "d1", "d2", "d3", "d4", "d5", "d6", "d7",
      "d16","d17", "d18","d19", "d20", "d21", "d22",
      "d23", "d24", "d25", "d26", "d27", "d28", "d29",
      "d30", "d31", "memory"
	);
    asm volatile (

    "mov    		%0, %5        			\n\t"	//r0=sizeof(light)
    "mla    		%0, %1, %0, %2 			\n\t"	//r0=r1*r0+r2

    "vmov.f32 		q8, q0  			    \n\t"	//q8=q0
    "vmov.f32 		q9, q1  			    \n\t"	//q9=q1
    "vmov.f32 		q10, q2  			    \n\t"	//q10=q2
    "vmov.f32 		q11, q3  			    \n\t"	//q11=q3

    "vld1.32 		{d0}, [%0]			    \n\t"	//d0={r,g}
    "flds   		s2, [%0, #8]			\n\t"	//d1[0]={b}
    "vmov.f32 		q1, q0  			    \n\t"	//q1=q0
    "vmov.f32 		q2, q0  			    \n\t"	//q2=q0
    "vmov.f32 		q3, q0  			    \n\t"	//q3=q0

    "vmov.f32 		q15, #0.0     			\n\t"	//q15=0
    "vdup.f32 		q15, d30[0]     		\n\t"	//q15=d30[0]

    "cmp     		%1, #0       			\n\t"	//
    "beq     		2f             			\n\t"	//(r1==0) goto 2

    "1:                          			\n\t"	//
    "vld1.32 		{d8}, [%2]!	        	\n\t"	//d8={r,g}
    "flds    		s18, [%2]   	    	\n\t"	//q9[0]={b}
    "add    		%2, %2, #4   	    	\n\t"	//q9[0]={b}
    "vld1.32 		{d10}, [%2]!    		\n\t"	//d10={x,y}
    "flds    		s22, [%2]   	    	\n\t"	//d11[0]={z}
    "add    		%2, %2, #4   	    	\n\t"	//q9[0]={b}

    "vmov.f32 		q13, q5  	       		\n\t"	//q13 = q5
    "vmov.f32 		q12, q4  	       		\n\t"	//q12 = q4

    "vmul.f32 		q4, q8, q13	       		\n\t"	//q4 = q8*q13
    "vmul.f32 		q5, q9, q13	       		\n\t"	//q5 = q9*q13
    "vmul.f32 		q6, q10, q13	        \n\t"	//q6 = q10*q13
    "vmul.f32 		q7, q11, q13	       	\n\t"	//q7 = q11*q13

    "vpadd.f32 		d8, d8  				\n\t"	//d8[0] = d8[0] + d8[1]
    "vpadd.f32 		d10, d10  				\n\t"	//d10[0] = d10[0] + d10[1]
    "vpadd.f32 		d12, d12  				\n\t"	//d12[0] = d12[0] + d12[1]
    "vpadd.f32 		d14, d14  				\n\t"	//d14[0] = d14[0] + d14[1]

    "vmov.f32    	s17, s18  				\n\t"	//d8[1] = d9[0]
    "vmov.f32    	s21, s22  				\n\t"	//d10[1] = d11[0]
    "vmov.f32    	s25, s26  				\n\t"	//d12[1] = d13[0]
    "vmov.f32    	s29, s30  				\n\t"	//d14[1] = d15[0]

    "vpadd.f32 		d8, d8, d10  			\n\t"	//d8 = {d8[0] + d8[1], d10[0] + d10[1]}
    "vpadd.f32 		d9, d12, d14  			\n\t"	//d9 = {d12[0] + d12[1], d14[0] + d14[1]}

    "vmax.f32 		q4, q4, q15  			\n\t"	//q4=max(q4, 0)

    "vmla.f32 		q0, q12, d8[0]  		\n\t"	//q0 +=
    "vmla.f32 		q1, q12, d8[1]  		\n\t"	//d1 = {d4[0] + d4[1], d6[0] + d6[1]}
    "vmla.f32 		q2, q12, d9[0]  		\n\t"	//d1 = {d4[0] + d4[1], d6[0] + d6[1]}
    "vmla.f32 		q3, q12, d9[1]  		\n\t"	//d1 = {d4[0] + d4[1], d6[0] + d6[1]}

    "subs     		%1, %1, #1       		\n\t"	//r1=r1 - 1
    "bne     		1b                 		\n\t"	//(r1!=0) goto 1

    "2:                          			\n\t"	//

    "vmov.f32        q4, #1.0	        	\n\t"	//
    "vmin.f32 		q0, q0, q4  	        \n\t"	//
    "vmin.f32 		q1, q1, q4  	        \n\t"	//
    "vmin.f32 		q2, q2, q4  	        \n\t"	//
    "vmin.f32 		q3, q3, q4  	        \n\t"	//
    "vst1.32 		{d0, d1}, [%4]!	        \n\t"	//
    "vst1.32 		{d2, d3}, [%4]! 	    \n\t"	//
    "vst1.32 		{d4, d5}, [%4]!	        \n\t"	//
    "vst1.32 		{d6, d7}, [%4]     	    \n\t"	//

    : "+&r"(tmp), "+&r"(i), "+&r"(ptr0), "+&r"(ptr1), "+&r"(ptr2)
    : "I"(sizeof(SPLight))
    : "d0", "d1", "d2", "d3", "d4", "d5", "d6", "d7",
      "d8", "d9", "d10", "d11", "d12", "d13", "d14", "d15",
      "d16","d17", "d18","d19", "d20", "d21", "d22", "d23",
      "d24", "d25", "d26", "d27", "d28", "d29", "d30", "d31",
      "memory", "cc"
    );
    OGL.triangles.vertices[v].r = result[0];
    OGL.triangles.vertices[v].g = result[1];
    OGL.triangles.vertices[v].b = result[2];
    OGL.triangles.vertices[v+1].r = result[4];
    OGL.triangles.vertices[v+1].g = result[5];
    OGL.triangles.vertices[v+1].b = result[6];
    OGL.triangles.vertices[v+2].r = result[8];
    OGL.triangles.vertices[v+2].g = result[9];
    OGL.triangles.vertices[v+2].b = result[10];
    OGL.triangles.vertices[v+3].r = result[12];
    OGL.triangles.vertices[v+3].g = result[13];
    OGL.triangles.vertices[v+3].b = result[14];
}

static void gSPBillboardVertex4NEON(u32 v)
{
    int i = 0;

#ifdef __TRIBUFFER_OPT
    i = OGL.triangles.indexmap[0];
#endif

    void *ptr0 = (void*)&OGL.triangles.vertices[v].x;
    void *ptr1 = (void*)&OGL.triangles.vertices[i].x;
    asm volatile (

    "vld1.32 		{d0, d1}, [%0]		  	\n\t"	//q0 = {x,y,z,w}
	"add 		    %0, %0, %2 		  	    \n\t"	//q0 = {x,y,z,w}
	"vld1.32 		{d2, d3}, [%0]	    	\n\t"	//q1 = {x,y,z,w}
	"add 		    %0, %0, %2 		      	\n\t"	//q0 = {x,y,z,w}
	"vld1.32 		{d4, d5}, [%0]	        \n\t"	//q2 = {x,y,z,w}
	"add 		    %0, %0, %2 	    	  	\n\t"	//q0 = {x,y,z,w}
	"vld1.32 		{d6, d7}, [%0]	        \n\t"	//q3 = {x,y,z,w}
    "sub 		    %0, %0, %3   		  	\n\t"	//q0 = {x,y,z,w}

    "vld1.32 		{d16, d17}, [%1]		\n\t"	//q2={x1,y1,z1,w1}
    "vadd.f32 		q0, q0, q8 			    \n\t"	//q1=q1+q1
    "vadd.f32 		q1, q1, q8 			    \n\t"	//q1=q1+q1
    "vadd.f32 		q2, q2, q8 			    \n\t"	//q1=q1+q1
    "vadd.f32 		q3, q3, q8 			    \n\t"	//q1=q1+q1
    "vst1.32 		{d0, d1}, [%0] 		    \n\t"	//
    "add 		    %0, %0, %2  		  	\n\t"	//q0 = {x,y,z,w}
	"vst1.32 		{d2, d3}, [%0]          \n\t"	//
    "add 		    %0, %0, %2 		  	    \n\t"	//q0 = {x,y,z,w}
	"vst1.32 		{d4, d5}, [%0]          \n\t"	//
    "add 		    %0, %0, %2 		  	    \n\t"	//q0 = {x,y,z,w}
	"vst1.32 		{d6, d7}, [%0]          \n\t"	//
    : "+&r"(ptr0), "+&r"(ptr1)
    : "I"(sizeof(SPVertex)), "I"(3 * sizeof(SPVertex))
    : "d0", "d1", "d2", "d3", "d4", "d5", "d6", "d7",
      "d16", "d17", "memory"
    );
}
#endif

static void gSPTransformVertexNEON(float vtx[4], float mtx[4][4])
{
//optimised using cycle analyser
#if 0
    volatile int tmp0, tmp1;
	asm volatile (
	"vld1.32 		{d0, d1}, [%3, :128]		\n\t"	//q0 = *v
	"add 		    %1, %0, #16		  	    \n\t"	//r1=r0+16
	"vld1.32 		{d18, d19}, [%0, :128]	\n\t"	//q9 = m
	"add 		    %2, %0, #32		  	    \n\t"	//r2=r0+32
	"vld1.32 		{d20, d21}, [%1, :128]    \n\t"	//q10 = m+4
	"add 		    %0, %0, #48		  	    \n\t"	//r0=r0+48
	"vld1.32 		{d22, d23}, [%2, :128]   	\n\t"	//q11 = m+8
	"vld1.32 		{d24, d25}, [%0, :128]  	\n\t"	//q12 = m+12

    "vmla.f32 		q12, q9, d0[0]          \n\t"	//q12 = q12 + q9*Q0[0]
    "vmul.f32 		q13, q10, d0[1]         \n\t"	//q13 = Q10*Q0[1]
    "vmul.f32 		q14, q11, d1[0]         \n\t"	//q14 = Q11*Q0[2]
    "vadd.f32 		q12, q12, q13           \n\t"	//q12 = q12 + q14
    "vadd.f32 		q12, q12, q14           \n\t"	//Q12 = q12 + q15

	"vst1.32 		{d24, d25}, [%3, :128]	\n\t"	//*v = q12

	: "+r"(mtx), "+r"(tmp0), "+r"(tmp1) : "r"(vtx)
    : "d0", "d1", "d18","d19","d20","d21","d22","d23","d24","d25",
	"d26", "d27", "memory"
	);

#else
	asm volatile (
	"vld1.32 		{d0, d1}, [%1]		  	\n\t"	//d8 = {x,y}
	"vld1.32 		{d18, d19}, [%0]!		\n\t"	//Q1 = m
	"vld1.32 		{d20, d21}, [%0]!   	\n\t"	//Q2 = m+4
	"vld1.32 		{d22, d23}, [%0]!   	\n\t"	//Q3 = m+8
	"vld1.32 		{d24, d25}, [%0]    	\n\t"	//Q4 = m+12

	"vmul.f32 		q13, q9, d0[0]			\n\t"	//Q5 = Q1*Q0[0]
	"vmla.f32 		q13, q10, d0[1]			\n\t"	//Q5 += Q1*Q0[1]
	"vmla.f32 		q13, q11, d1[0]			\n\t"	//Q5 += Q2*Q0[2]
	"vadd.f32 		q13, q13, q12			\n\t"	//Q5 += Q3*Q0[3]
	"vst1.32 		{d26, d27}, [%1] 		\n\t"	//Q4 = m+12

	: "+r"(mtx) : "r"(vtx)
    : "d0", "d1", "d18","d19","d20","d21","d22","d23","d24","d25",
	"d26", "d27", "memory"
	);
#endif
}

static void gSPLightVertexNEON(u32 v)
{
    volatile float result[4];

    volatile int tmp = 0;
    volatile int i = gSP.numLights;
    volatile void *ptr0 = &gSP.lights[0].r;
    volatile void *ptr1 = &OGL.triangles.vertices[v].nx;
    volatile void *ptr2 = result;;
    volatile void *ptr3 = gSP.matrix.modelView[gSP.matrix.modelViewi];

	asm volatile (
	"vld1.32 		{d0, d1}, [%1]  		\n\t"	//Q0 = v
	"vld1.32 		{d18, d19}, [%0]!		\n\t"	//Q1 = m
	"vld1.32 		{d20, d21}, [%0]!	    \n\t"	//Q2 = m+4
	"vld1.32 		{d22, d23}, [%0]	    \n\t"	//Q3 = m+8

	"vmul.f32 		q2, q9, d0[0]			\n\t"	//q2 = q9*Q0[0]
	"vmla.f32 		q2, q10, d0[1]			\n\t"	//Q5 += Q1*Q0[1]
	"vmla.f32 		q2, q11, d1[0]			\n\t"	//Q5 += Q2*Q0[2]

    "vmul.f32 		d0, d4, d4				\n\t"	//d0 = d0*d0
	"vpadd.f32 		d0, d0, d0				\n\t"	//d0 = d[0] + d[1]
    "vmla.f32 		d0, d5, d5				\n\t"	//d0 = d0 + d5*d5

	"vmov.f32 		d1, d0					\n\t"	//d1 = d0
	"vrsqrte.f32 	d0, d0					\n\t"	//d0 = ~ 1.0 / sqrt(d0)
	"vmul.f32 		d2, d0, d1				\n\t"	//d2 = d0 * d1
	"vrsqrts.f32 	d3, d2, d0				\n\t"	//d3 = (3 - d0 * d2) / 2
	"vmul.f32 		d0, d0, d3				\n\t"	//d0 = d0 * d3
	"vmul.f32 		d2, d0, d1				\n\t"	//d2 = d0 * d1
	"vrsqrts.f32 	d3, d2, d0				\n\t"	//d3 = (3 - d0 * d3) / 2
	"vmul.f32 		d0, d0, d3				\n\t"	//d0 = d0 * d4

	"vmul.f32 		q1, q2, d0[0]			\n\t"	//q1 = d2*d4

	"vst1.32 		{d2, d3}, [%1]  	    \n\t"	//d0={nx,ny,nz,pad}

	: "+&r"(ptr3): "r"(ptr1)
    : "d0","d1","d2","d3","d18","d19","d20","d21","d22", "d23", "memory"
	);

    asm volatile (
    "mov    		%0, #24        			\n\t"	//r0=24
    "mla    		%0, %1, %0, %2 			\n\t"	//r0=r1*r0+r2

    "vld1.32 		{d0}, [%0]!	    		\n\t"	//d0={r,g}
    "flds   		s2, [%0]	        	\n\t"	//d1[0]={b}
    "cmp            %0, #0     		        \n\t"	//
    "beq            2f       		        \n\t"	//(r1==0) goto 2

    "1:                        		        \n\t"	//
    "vld1.32 		{d4}, [%2]!	        	\n\t"	//d4={r,g}
    "flds    		s10, [%2]	        	\n\t"	//q5[0]={b}
    "add 		    %2, %2, #4 		        \n\t"	//r2+=4
    "vld1.32 		{d6}, [%2]!	    		\n\t"	//d6={x,y}
    "flds    		s14, [%2]	        	\n\t"	//d7[0]={z}
    "add 		    %2, %2, #4 		        \n\t"	//r2+=4
    "vmul.f32 		d6, d2, d6 			    \n\t"	//d6=d2*d6
    "vpadd.f32 		d6, d6   			    \n\t"	//d6=d6[0]+d6[1]
    "vmla.f32 		d6, d3, d7 			    \n\t"	//d6=d6+d3*d7
    "vmov.f32 		d7, #0.0     			\n\t"	//d7=0
    "vmax.f32 		d6, d6, d7   		    \n\t"	//d6=max(d6, d7)
    "vmla.f32 		q0, q2, d6[0] 		    \n\t"	//q0=q0+q2*d6[0]
    "sub 		    %1, %1, #1 		        \n\t"	//r0=r0-1
    "cmp 		    %1, #0   		        \n\t"	//r0=r0-1
    "bgt 		    1b 		                \n\t"	//(r1!=0) ? goto 1
    "b  		    2f 		                \n\t"	//(r1!=0) ? goto 1
    "2:                        		        \n\t"	//
    "vmov.f32        q1, #1.0	        	\n\t"	//
    "vmin.f32        q0, q0, q1	        	\n\t"	//
    "vst1.32        {d0, d1}, [%3]	    	\n\t"	//

    : "+&r"(tmp), "+&r"(i), "+&r"(ptr0), "+&r"(ptr2)
    :: "d0", "d1", "d2", "d3", "d4", "d5", "d6", "d7",
      "d16", "memory", "cc"
    );
    OGL.triangles.vertices[v].r = result[0];
    OGL.triangles.vertices[v].g = result[1];
    OGL.triangles.vertices[v].b = result[2];
}

static void gSPBillboardVertexNEON(u32 v, u32 i)
{
    asm volatile (
    "vld1.32 		{d2, d3}, [%0]			\n\t"	//q1={x0,y0, z0, w0}
    "vld1.32 		{d4, d5}, [%1]			\n\t"	//q2={x1,y1, z1, w1}
    "vadd.f32 		q1, q1, q2 			    \n\t"	//q1=q1+q1
    "vst1.32 		{d2, d3}, [%0] 		    \n\t"	//
    :: "r"(&OGL.triangles.vertices[v].x), "r"(&OGL.triangles.vertices[i].x)
    : "d2", "d3", "d4", "d5", "memory"
    );
}

void gSPInitNeon()
{
#ifdef __VEC4_OPT
    gSPTransformVertex4 = gSPTransformVertex4NEON;
    gSPTransformNormal4 = gSPTransformNormal4NEON;
    gSPLightVertex4 = gSPLightVertex4NEON;
    gSPBillboardVertex4 = gSPBillboardVertex4NEON;
#endif
    gSPTransformVertex = gSPTransformVertexNEON;
    gSPLightVertex = gSPLightVertexNEON;
    gSPBillboardVertex = gSPBillboardVertexNEON;
}
