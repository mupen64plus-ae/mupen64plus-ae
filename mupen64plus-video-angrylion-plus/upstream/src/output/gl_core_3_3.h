#ifndef GLES

#ifndef POINTER_C_GENERATED_HEADER_OPENGL_H
#define POINTER_C_GENERATED_HEADER_OPENGL_H

#if defined(__glew_h__) || defined(__GLEW_H__)
#error Attempt to include auto-generated header after including glew.h
#endif
#if defined(__gl_h_) || defined(__GL_H__)
#error Attempt to include auto-generated header after including gl.h
#endif
#if defined(__glext_h_) || defined(__GLEXT_H_)
#error Attempt to include auto-generated header after including glext.h
#endif
#if defined(__gltypes_h_)
#error Attempt to include auto-generated header after gltypes.h
#endif
#if defined(__gl_ATI_h_)
#error Attempt to include auto-generated header after including glATI.h
#endif

#define __glew_h__
#define __GLEW_H__
#define __gl_h_
#define __GL_H__
#define __glext_h_
#define __GLEXT_H_
#define __gltypes_h_
#define __gl_ATI_h_

#ifndef APIENTRY
	#if defined(__MINGW32__)
		#ifndef WIN32_LEAN_AND_MEAN
			#define WIN32_LEAN_AND_MEAN 1
		#endif
		#ifndef NOMINMAX
			#define NOMINMAX
		#endif
		#include <windows.h>
	#elif (_MSC_VER >= 800) || defined(_STDCALL_SUPPORTED) || defined(__BORLANDC__)
		#ifndef WIN32_LEAN_AND_MEAN
			#define WIN32_LEAN_AND_MEAN 1
		#endif
		#ifndef NOMINMAX
			#define NOMINMAX
		#endif
		#include <windows.h>
	#else
		#define APIENTRY
	#endif
#endif /*APIENTRY*/

#ifndef CODEGEN_FUNCPTR
	#define CODEGEN_REMOVE_FUNCPTR
	#if defined(_WIN32)
		#define CODEGEN_FUNCPTR APIENTRY
	#else
		#define CODEGEN_FUNCPTR
	#endif
#endif /*CODEGEN_FUNCPTR*/

#ifndef GLAPI
	#define GLAPI extern
#endif


#ifndef GL_LOAD_GEN_BASIC_OPENGL_TYPEDEFS
#define GL_LOAD_GEN_BASIC_OPENGL_TYPEDEFS


#endif /*GL_LOAD_GEN_BASIC_OPENGL_TYPEDEFS*/


#include <stddef.h>
#ifndef GLEXT_64_TYPES_DEFINED
/* This code block is duplicated in glxext.h, so must be protected */
#define GLEXT_64_TYPES_DEFINED
/* Define int32_t, int64_t, and uint64_t types for UST/MSC */
/* (as used in the GL_EXT_timer_query extension). */
#if defined(__STDC_VERSION__) && __STDC_VERSION__ >= 199901L
#include <inttypes.h>
#elif defined(__sun__) || defined(__digital__)
#include <inttypes.h>
#if defined(__STDC__)
#if defined(__arch64__) || defined(_LP64)
typedef long int int64_t;
typedef unsigned long int uint64_t;
#else
typedef long long int int64_t;
typedef unsigned long long int uint64_t;
#endif /* __arch64__ */
#endif /* __STDC__ */
#elif defined( __VMS ) || defined(__sgi)
#include <inttypes.h>
#elif defined(__SCO__) || defined(__USLC__)
#include <stdint.h>
#elif defined(__UNIXOS2__) || defined(__SOL64__)
typedef long int int32_t;
typedef long long int int64_t;
typedef unsigned long long int uint64_t;
#elif defined(_WIN32) && defined(__GNUC__)
#include <stdint.h>
#elif defined(_WIN32)
typedef __int32 int32_t;
typedef __int64 int64_t;
typedef unsigned __int64 uint64_t;
#else
/* Fallback if nothing above works */
#include <inttypes.h>
#endif
#endif
	typedef unsigned int GLenum;
	typedef unsigned char GLboolean;
	typedef unsigned int GLbitfield;
	typedef void GLvoid;
	typedef signed char GLbyte;
	typedef short GLshort;
	typedef int GLint;
	typedef unsigned char GLubyte;
	typedef unsigned short GLushort;
	typedef unsigned int GLuint;
	typedef int GLsizei;
	typedef float GLfloat;
	typedef float GLclampf;
	typedef double GLdouble;
	typedef double GLclampd;
	typedef char GLchar;
	typedef char GLcharARB;
	#ifdef __APPLE__
typedef void *GLhandleARB;
#else
typedef unsigned int GLhandleARB;
#endif
		typedef unsigned short GLhalfARB;
		typedef unsigned short GLhalf;
		typedef GLint GLfixed;
		typedef ptrdiff_t GLintptr;
		typedef ptrdiff_t GLsizeiptr;
		typedef int64_t GLint64;
		typedef uint64_t GLuint64;
		typedef ptrdiff_t GLintptrARB;
		typedef ptrdiff_t GLsizeiptrARB;
		typedef int64_t GLint64EXT;
		typedef uint64_t GLuint64EXT;
		typedef struct __GLsync *GLsync;
		struct _cl_context;
		struct _cl_event;
		typedef void (APIENTRY *GLDEBUGPROC)(GLenum source,GLenum type,GLuint id,GLenum severity,GLsizei length,const GLchar *message,const void *userParam);
		typedef void (APIENTRY *GLDEBUGPROCARB)(GLenum source,GLenum type,GLuint id,GLenum severity,GLsizei length,const GLchar *message,const void *userParam);
		typedef void (APIENTRY *GLDEBUGPROCAMD)(GLuint id,GLenum category,GLenum severity,GLsizei length,const GLchar *message,void *userParam);
		typedef unsigned short GLhalfNV;
		typedef GLintptr GLvdpauSurfaceNV;

#ifdef __cplusplus
extern "C" {
#endif /*__cplusplus*/

#define GL_ALPHA 0x1906
#define GL_ALWAYS 0x0207
#define GL_AND 0x1501
#define GL_AND_INVERTED 0x1504
#define GL_AND_REVERSE 0x1502
#define GL_BACK 0x0405
#define GL_BACK_LEFT 0x0402
#define GL_BACK_RIGHT 0x0403
#define GL_BLEND 0x0BE2
#define GL_BLEND_DST 0x0BE0
#define GL_BLEND_SRC 0x0BE1
#define GL_BLUE 0x1905
#define GL_BYTE 0x1400
#define GL_CCW 0x0901
#define GL_CLEAR 0x1500
#define GL_COLOR 0x1800
#define GL_COLOR_BUFFER_BIT 0x00004000
#define GL_COLOR_CLEAR_VALUE 0x0C22
#define GL_COLOR_LOGIC_OP 0x0BF2
#define GL_COLOR_WRITEMASK 0x0C23
#define GL_COPY 0x1503
#define GL_COPY_INVERTED 0x150C
#define GL_CULL_FACE 0x0B44
#define GL_CULL_FACE_MODE 0x0B45
#define GL_CW 0x0900
#define GL_DECR 0x1E03
#define GL_DEPTH 0x1801
#define GL_DEPTH_BUFFER_BIT 0x00000100
#define GL_DEPTH_CLEAR_VALUE 0x0B73
#define GL_DEPTH_COMPONENT 0x1902
#define GL_DEPTH_FUNC 0x0B74
#define GL_DEPTH_RANGE 0x0B70
#define GL_DEPTH_TEST 0x0B71
#define GL_DEPTH_WRITEMASK 0x0B72
#define GL_DITHER 0x0BD0
#define GL_DONT_CARE 0x1100
#define GL_DOUBLE 0x140A
#define GL_DOUBLEBUFFER 0x0C32
#define GL_DRAW_BUFFER 0x0C01
#define GL_DST_ALPHA 0x0304
#define GL_DST_COLOR 0x0306
#define GL_EQUAL 0x0202
#define GL_EQUIV 0x1509
#define GL_EXTENSIONS 0x1F03
#define GL_FALSE 0
#define GL_FASTEST 0x1101
#define GL_FILL 0x1B02
#define GL_FLOAT 0x1406
#define GL_FRONT 0x0404
#define GL_FRONT_AND_BACK 0x0408
#define GL_FRONT_FACE 0x0B46
#define GL_FRONT_LEFT 0x0400
#define GL_FRONT_RIGHT 0x0401
#define GL_GEQUAL 0x0206
#define GL_GREATER 0x0204
#define GL_GREEN 0x1904
#define GL_INCR 0x1E02
#define GL_INT 0x1404
#define GL_INVALID_ENUM 0x0500
#define GL_INVALID_OPERATION 0x0502
#define GL_INVALID_VALUE 0x0501
#define GL_INVERT 0x150A
#define GL_KEEP 0x1E00
#define GL_LEFT 0x0406
#define GL_LEQUAL 0x0203
#define GL_LESS 0x0201
#define GL_LINE 0x1B01
#define GL_LINEAR 0x2601
#define GL_LINEAR_MIPMAP_LINEAR 0x2703
#define GL_LINEAR_MIPMAP_NEAREST 0x2701
#define GL_LINES 0x0001
#define GL_LINE_LOOP 0x0002
#define GL_LINE_SMOOTH 0x0B20
#define GL_LINE_SMOOTH_HINT 0x0C52
#define GL_LINE_STRIP 0x0003
#define GL_LINE_WIDTH 0x0B21
#define GL_LINE_WIDTH_GRANULARITY 0x0B23
#define GL_LINE_WIDTH_RANGE 0x0B22
#define GL_LOGIC_OP_MODE 0x0BF0
#define GL_MAX_TEXTURE_SIZE 0x0D33
#define GL_MAX_VIEWPORT_DIMS 0x0D3A
#define GL_NAND 0x150E
#define GL_NEAREST 0x2600
#define GL_NEAREST_MIPMAP_LINEAR 0x2702
#define GL_NEAREST_MIPMAP_NEAREST 0x2700
#define GL_NEVER 0x0200
#define GL_NICEST 0x1102
#define GL_NONE 0
#define GL_NOOP 0x1505
#define GL_NOR 0x1508
#define GL_NOTEQUAL 0x0205
#define GL_NO_ERROR 0
#define GL_ONE 1
#define GL_ONE_MINUS_DST_ALPHA 0x0305
#define GL_ONE_MINUS_DST_COLOR 0x0307
#define GL_ONE_MINUS_SRC_ALPHA 0x0303
#define GL_ONE_MINUS_SRC_COLOR 0x0301
#define GL_OR 0x1507
#define GL_OR_INVERTED 0x150D
#define GL_OR_REVERSE 0x150B
#define GL_OUT_OF_MEMORY 0x0505
#define GL_PACK_ALIGNMENT 0x0D05
#define GL_PACK_LSB_FIRST 0x0D01
#define GL_PACK_ROW_LENGTH 0x0D02
#define GL_PACK_SKIP_PIXELS 0x0D04
#define GL_PACK_SKIP_ROWS 0x0D03
#define GL_PACK_SWAP_BYTES 0x0D00
#define GL_POINT 0x1B00
#define GL_POINTS 0x0000
#define GL_POINT_SIZE 0x0B11
#define GL_POINT_SIZE_GRANULARITY 0x0B13
#define GL_POINT_SIZE_RANGE 0x0B12
#define GL_POLYGON_MODE 0x0B40
#define GL_POLYGON_OFFSET_FACTOR 0x8038
#define GL_POLYGON_OFFSET_FILL 0x8037
#define GL_POLYGON_OFFSET_LINE 0x2A02
#define GL_POLYGON_OFFSET_POINT 0x2A01
#define GL_POLYGON_OFFSET_UNITS 0x2A00
#define GL_POLYGON_SMOOTH 0x0B41
#define GL_POLYGON_SMOOTH_HINT 0x0C53
#define GL_PROXY_TEXTURE_1D 0x8063
#define GL_PROXY_TEXTURE_2D 0x8064
#define GL_R3_G3_B2 0x2A10
#define GL_READ_BUFFER 0x0C02
#define GL_RED 0x1903
#define GL_RENDERER 0x1F01
#define GL_REPEAT 0x2901
#define GL_REPLACE 0x1E01
#define GL_RGB 0x1907
#define GL_RGB10 0x8052
#define GL_RGB10_A2 0x8059
#define GL_RGB12 0x8053
#define GL_RGB16 0x8054
#define GL_RGB4 0x804F
#define GL_RGB5 0x8050
#define GL_RGB5_A1 0x8057
#define GL_RGB8 0x8051
#define GL_RGBA 0x1908
#define GL_RGBA12 0x805A
#define GL_RGBA16 0x805B
#define GL_RGBA2 0x8055
#define GL_RGBA4 0x8056
#define GL_RGBA8 0x8058
#define GL_RIGHT 0x0407
#define GL_SCISSOR_BOX 0x0C10
#define GL_SCISSOR_TEST 0x0C11
#define GL_SET 0x150F
#define GL_SHORT 0x1402
#define GL_SRC_ALPHA 0x0302
#define GL_SRC_ALPHA_SATURATE 0x0308
#define GL_SRC_COLOR 0x0300
#define GL_STENCIL 0x1802
#define GL_STENCIL_BUFFER_BIT 0x00000400
#define GL_STENCIL_CLEAR_VALUE 0x0B91
#define GL_STENCIL_FAIL 0x0B94
#define GL_STENCIL_FUNC 0x0B92
#define GL_STENCIL_INDEX 0x1901
#define GL_STENCIL_PASS_DEPTH_FAIL 0x0B95
#define GL_STENCIL_PASS_DEPTH_PASS 0x0B96
#define GL_STENCIL_REF 0x0B97
#define GL_STENCIL_TEST 0x0B90
#define GL_STENCIL_VALUE_MASK 0x0B93
#define GL_STENCIL_WRITEMASK 0x0B98
#define GL_STEREO 0x0C33
#define GL_SUBPIXEL_BITS 0x0D50
#define GL_TEXTURE 0x1702
#define GL_TEXTURE_1D 0x0DE0
#define GL_TEXTURE_2D 0x0DE1
#define GL_TEXTURE_ALPHA_SIZE 0x805F
#define GL_TEXTURE_BINDING_1D 0x8068
#define GL_TEXTURE_BINDING_2D 0x8069
#define GL_TEXTURE_BLUE_SIZE 0x805E
#define GL_TEXTURE_BORDER_COLOR 0x1004
#define GL_TEXTURE_GREEN_SIZE 0x805D
#define GL_TEXTURE_HEIGHT 0x1001
#define GL_TEXTURE_INTERNAL_FORMAT 0x1003
#define GL_TEXTURE_MAG_FILTER 0x2800
#define GL_TEXTURE_MIN_FILTER 0x2801
#define GL_TEXTURE_RED_SIZE 0x805C
#define GL_TEXTURE_WIDTH 0x1000
#define GL_TEXTURE_WRAP_S 0x2802
#define GL_TEXTURE_WRAP_T 0x2803
#define GL_TRIANGLES 0x0004
#define GL_TRIANGLE_FAN 0x0006
#define GL_TRIANGLE_STRIP 0x0005
#define GL_TRUE 1
#define GL_UNPACK_ALIGNMENT 0x0CF5
#define GL_UNPACK_LSB_FIRST 0x0CF1
#define GL_UNPACK_ROW_LENGTH 0x0CF2
#define GL_UNPACK_SKIP_PIXELS 0x0CF4
#define GL_UNPACK_SKIP_ROWS 0x0CF3
#define GL_UNPACK_SWAP_BYTES 0x0CF0
#define GL_UNSIGNED_BYTE 0x1401
#define GL_UNSIGNED_INT 0x1405
#define GL_UNSIGNED_SHORT 0x1403
#define GL_VENDOR 0x1F00
#define GL_VERSION 0x1F02
#define GL_VIEWPORT 0x0BA2
#define GL_XOR 0x1506
#define GL_ZERO 0

#define GL_ALIASED_LINE_WIDTH_RANGE 0x846E
#define GL_BGR 0x80E0
#define GL_BGRA 0x80E1
#define GL_CLAMP_TO_EDGE 0x812F
#define GL_MAX_3D_TEXTURE_SIZE 0x8073
#define GL_MAX_ELEMENTS_INDICES 0x80E9
#define GL_MAX_ELEMENTS_VERTICES 0x80E8
#define GL_PACK_IMAGE_HEIGHT 0x806C
#define GL_PACK_SKIP_IMAGES 0x806B
#define GL_PROXY_TEXTURE_3D 0x8070
#define GL_SMOOTH_LINE_WIDTH_GRANULARITY 0x0B23
#define GL_SMOOTH_LINE_WIDTH_RANGE 0x0B22
#define GL_SMOOTH_POINT_SIZE_GRANULARITY 0x0B13
#define GL_SMOOTH_POINT_SIZE_RANGE 0x0B12
#define GL_TEXTURE_3D 0x806F
#define GL_TEXTURE_BASE_LEVEL 0x813C
#define GL_TEXTURE_BINDING_3D 0x806A
#define GL_TEXTURE_DEPTH 0x8071
#define GL_TEXTURE_MAX_LEVEL 0x813D
#define GL_TEXTURE_MAX_LOD 0x813B
#define GL_TEXTURE_MIN_LOD 0x813A
#define GL_TEXTURE_WRAP_R 0x8072
#define GL_UNPACK_IMAGE_HEIGHT 0x806E
#define GL_UNPACK_SKIP_IMAGES 0x806D
#define GL_UNSIGNED_BYTE_2_3_3_REV 0x8362
#define GL_UNSIGNED_BYTE_3_3_2 0x8032
#define GL_UNSIGNED_INT_10_10_10_2 0x8036
#define GL_UNSIGNED_INT_2_10_10_10_REV 0x8368
#define GL_UNSIGNED_INT_8_8_8_8 0x8035
#define GL_UNSIGNED_INT_8_8_8_8_REV 0x8367
#define GL_UNSIGNED_SHORT_1_5_5_5_REV 0x8366
#define GL_UNSIGNED_SHORT_4_4_4_4 0x8033
#define GL_UNSIGNED_SHORT_4_4_4_4_REV 0x8365
#define GL_UNSIGNED_SHORT_5_5_5_1 0x8034
#define GL_UNSIGNED_SHORT_5_6_5 0x8363
#define GL_UNSIGNED_SHORT_5_6_5_REV 0x8364

#define GL_ACTIVE_TEXTURE 0x84E0
#define GL_CLAMP_TO_BORDER 0x812D
#define GL_COMPRESSED_RGB 0x84ED
#define GL_COMPRESSED_RGBA 0x84EE
#define GL_COMPRESSED_TEXTURE_FORMATS 0x86A3
#define GL_MAX_CUBE_MAP_TEXTURE_SIZE 0x851C
#define GL_MULTISAMPLE 0x809D
#define GL_NUM_COMPRESSED_TEXTURE_FORMATS 0x86A2
#define GL_PROXY_TEXTURE_CUBE_MAP 0x851B
#define GL_SAMPLES 0x80A9
#define GL_SAMPLE_ALPHA_TO_COVERAGE 0x809E
#define GL_SAMPLE_ALPHA_TO_ONE 0x809F
#define GL_SAMPLE_BUFFERS 0x80A8
#define GL_SAMPLE_COVERAGE 0x80A0
#define GL_SAMPLE_COVERAGE_INVERT 0x80AB
#define GL_SAMPLE_COVERAGE_VALUE 0x80AA
#define GL_TEXTURE0 0x84C0
#define GL_TEXTURE1 0x84C1
#define GL_TEXTURE10 0x84CA
#define GL_TEXTURE11 0x84CB
#define GL_TEXTURE12 0x84CC
#define GL_TEXTURE13 0x84CD
#define GL_TEXTURE14 0x84CE
#define GL_TEXTURE15 0x84CF
#define GL_TEXTURE16 0x84D0
#define GL_TEXTURE17 0x84D1
#define GL_TEXTURE18 0x84D2
#define GL_TEXTURE19 0x84D3
#define GL_TEXTURE2 0x84C2
#define GL_TEXTURE20 0x84D4
#define GL_TEXTURE21 0x84D5
#define GL_TEXTURE22 0x84D6
#define GL_TEXTURE23 0x84D7
#define GL_TEXTURE24 0x84D8
#define GL_TEXTURE25 0x84D9
#define GL_TEXTURE26 0x84DA
#define GL_TEXTURE27 0x84DB
#define GL_TEXTURE28 0x84DC
#define GL_TEXTURE29 0x84DD
#define GL_TEXTURE3 0x84C3
#define GL_TEXTURE30 0x84DE
#define GL_TEXTURE31 0x84DF
#define GL_TEXTURE4 0x84C4
#define GL_TEXTURE5 0x84C5
#define GL_TEXTURE6 0x84C6
#define GL_TEXTURE7 0x84C7
#define GL_TEXTURE8 0x84C8
#define GL_TEXTURE9 0x84C9
#define GL_TEXTURE_BINDING_CUBE_MAP 0x8514
#define GL_TEXTURE_COMPRESSED 0x86A1
#define GL_TEXTURE_COMPRESSED_IMAGE_SIZE 0x86A0
#define GL_TEXTURE_COMPRESSION_HINT 0x84EF
#define GL_TEXTURE_CUBE_MAP 0x8513
#define GL_TEXTURE_CUBE_MAP_NEGATIVE_X 0x8516
#define GL_TEXTURE_CUBE_MAP_NEGATIVE_Y 0x8518
#define GL_TEXTURE_CUBE_MAP_NEGATIVE_Z 0x851A
#define GL_TEXTURE_CUBE_MAP_POSITIVE_X 0x8515
#define GL_TEXTURE_CUBE_MAP_POSITIVE_Y 0x8517
#define GL_TEXTURE_CUBE_MAP_POSITIVE_Z 0x8519

#define GL_BLEND_COLOR 0x8005
#define GL_BLEND_DST_ALPHA 0x80CA
#define GL_BLEND_DST_RGB 0x80C8
#define GL_BLEND_SRC_ALPHA 0x80CB
#define GL_BLEND_SRC_RGB 0x80C9
#define GL_CONSTANT_ALPHA 0x8003
#define GL_CONSTANT_COLOR 0x8001
#define GL_DECR_WRAP 0x8508
#define GL_DEPTH_COMPONENT16 0x81A5
#define GL_DEPTH_COMPONENT24 0x81A6
#define GL_DEPTH_COMPONENT32 0x81A7
#define GL_FUNC_ADD 0x8006
#define GL_FUNC_REVERSE_SUBTRACT 0x800B
#define GL_FUNC_SUBTRACT 0x800A
#define GL_INCR_WRAP 0x8507
#define GL_MAX 0x8008
#define GL_MAX_TEXTURE_LOD_BIAS 0x84FD
#define GL_MIN 0x8007
#define GL_MIRRORED_REPEAT 0x8370
#define GL_ONE_MINUS_CONSTANT_ALPHA 0x8004
#define GL_ONE_MINUS_CONSTANT_COLOR 0x8002
#define GL_POINT_FADE_THRESHOLD_SIZE 0x8128
#define GL_TEXTURE_COMPARE_FUNC 0x884D
#define GL_TEXTURE_COMPARE_MODE 0x884C
#define GL_TEXTURE_DEPTH_SIZE 0x884A
#define GL_TEXTURE_LOD_BIAS 0x8501

#define GL_ARRAY_BUFFER 0x8892
#define GL_ARRAY_BUFFER_BINDING 0x8894
#define GL_BUFFER_ACCESS 0x88BB
#define GL_BUFFER_MAPPED 0x88BC
#define GL_BUFFER_MAP_POINTER 0x88BD
#define GL_BUFFER_SIZE 0x8764
#define GL_BUFFER_USAGE 0x8765
#define GL_CURRENT_QUERY 0x8865
#define GL_DYNAMIC_COPY 0x88EA
#define GL_DYNAMIC_DRAW 0x88E8
#define GL_DYNAMIC_READ 0x88E9
#define GL_ELEMENT_ARRAY_BUFFER 0x8893
#define GL_ELEMENT_ARRAY_BUFFER_BINDING 0x8895
#define GL_QUERY_COUNTER_BITS 0x8864
#define GL_QUERY_RESULT 0x8866
#define GL_QUERY_RESULT_AVAILABLE 0x8867
#define GL_READ_ONLY 0x88B8
#define GL_READ_WRITE 0x88BA
#define GL_SAMPLES_PASSED 0x8914
#define GL_SRC1_ALPHA 0x8589
#define GL_STATIC_COPY 0x88E6
#define GL_STATIC_DRAW 0x88E4
#define GL_STATIC_READ 0x88E5
#define GL_STREAM_COPY 0x88E2
#define GL_STREAM_DRAW 0x88E0
#define GL_STREAM_READ 0x88E1
#define GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING 0x889F
#define GL_WRITE_ONLY 0x88B9

#define GL_ACTIVE_ATTRIBUTES 0x8B89
#define GL_ACTIVE_ATTRIBUTE_MAX_LENGTH 0x8B8A
#define GL_ACTIVE_UNIFORMS 0x8B86
#define GL_ACTIVE_UNIFORM_MAX_LENGTH 0x8B87
#define GL_ATTACHED_SHADERS 0x8B85
#define GL_BLEND_EQUATION_ALPHA 0x883D
#define GL_BLEND_EQUATION_RGB 0x8009
#define GL_BOOL 0x8B56
#define GL_BOOL_VEC2 0x8B57
#define GL_BOOL_VEC3 0x8B58
#define GL_BOOL_VEC4 0x8B59
#define GL_COMPILE_STATUS 0x8B81
#define GL_CURRENT_PROGRAM 0x8B8D
#define GL_CURRENT_VERTEX_ATTRIB 0x8626
#define GL_DELETE_STATUS 0x8B80
#define GL_DRAW_BUFFER0 0x8825
#define GL_DRAW_BUFFER1 0x8826
#define GL_DRAW_BUFFER10 0x882F
#define GL_DRAW_BUFFER11 0x8830
#define GL_DRAW_BUFFER12 0x8831
#define GL_DRAW_BUFFER13 0x8832
#define GL_DRAW_BUFFER14 0x8833
#define GL_DRAW_BUFFER15 0x8834
#define GL_DRAW_BUFFER2 0x8827
#define GL_DRAW_BUFFER3 0x8828
#define GL_DRAW_BUFFER4 0x8829
#define GL_DRAW_BUFFER5 0x882A
#define GL_DRAW_BUFFER6 0x882B
#define GL_DRAW_BUFFER7 0x882C
#define GL_DRAW_BUFFER8 0x882D
#define GL_DRAW_BUFFER9 0x882E
#define GL_FLOAT_MAT2 0x8B5A
#define GL_FLOAT_MAT3 0x8B5B
#define GL_FLOAT_MAT4 0x8B5C
#define GL_FLOAT_VEC2 0x8B50
#define GL_FLOAT_VEC3 0x8B51
#define GL_FLOAT_VEC4 0x8B52
#define GL_FRAGMENT_SHADER 0x8B30
#define GL_FRAGMENT_SHADER_DERIVATIVE_HINT 0x8B8B
#define GL_INFO_LOG_LENGTH 0x8B84
#define GL_INT_VEC2 0x8B53
#define GL_INT_VEC3 0x8B54
#define GL_INT_VEC4 0x8B55
#define GL_LINK_STATUS 0x8B82
#define GL_LOWER_LEFT 0x8CA1
#define GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS 0x8B4D
#define GL_MAX_DRAW_BUFFERS 0x8824
#define GL_MAX_FRAGMENT_UNIFORM_COMPONENTS 0x8B49
#define GL_MAX_TEXTURE_IMAGE_UNITS 0x8872
#define GL_MAX_VARYING_FLOATS 0x8B4B
#define GL_MAX_VERTEX_ATTRIBS 0x8869
#define GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS 0x8B4C
#define GL_MAX_VERTEX_UNIFORM_COMPONENTS 0x8B4A
#define GL_POINT_SPRITE_COORD_ORIGIN 0x8CA0
#define GL_SAMPLER_1D 0x8B5D
#define GL_SAMPLER_1D_SHADOW 0x8B61
#define GL_SAMPLER_2D 0x8B5E
#define GL_SAMPLER_2D_SHADOW 0x8B62
#define GL_SAMPLER_3D 0x8B5F
#define GL_SAMPLER_CUBE 0x8B60
#define GL_SHADER_SOURCE_LENGTH 0x8B88
#define GL_SHADER_TYPE 0x8B4F
#define GL_SHADING_LANGUAGE_VERSION 0x8B8C
#define GL_STENCIL_BACK_FAIL 0x8801
#define GL_STENCIL_BACK_FUNC 0x8800
#define GL_STENCIL_BACK_PASS_DEPTH_FAIL 0x8802
#define GL_STENCIL_BACK_PASS_DEPTH_PASS 0x8803
#define GL_STENCIL_BACK_REF 0x8CA3
#define GL_STENCIL_BACK_VALUE_MASK 0x8CA4
#define GL_STENCIL_BACK_WRITEMASK 0x8CA5
#define GL_UPPER_LEFT 0x8CA2
#define GL_VALIDATE_STATUS 0x8B83
#define GL_VERTEX_ATTRIB_ARRAY_ENABLED 0x8622
#define GL_VERTEX_ATTRIB_ARRAY_NORMALIZED 0x886A
#define GL_VERTEX_ATTRIB_ARRAY_POINTER 0x8645
#define GL_VERTEX_ATTRIB_ARRAY_SIZE 0x8623
#define GL_VERTEX_ATTRIB_ARRAY_STRIDE 0x8624
#define GL_VERTEX_ATTRIB_ARRAY_TYPE 0x8625
#define GL_VERTEX_PROGRAM_POINT_SIZE 0x8642
#define GL_VERTEX_SHADER 0x8B31

#define GL_COMPRESSED_SRGB 0x8C48
#define GL_COMPRESSED_SRGB_ALPHA 0x8C49
#define GL_FLOAT_MAT2x3 0x8B65
#define GL_FLOAT_MAT2x4 0x8B66
#define GL_FLOAT_MAT3x2 0x8B67
#define GL_FLOAT_MAT3x4 0x8B68
#define GL_FLOAT_MAT4x2 0x8B69
#define GL_FLOAT_MAT4x3 0x8B6A
#define GL_PIXEL_PACK_BUFFER 0x88EB
#define GL_PIXEL_PACK_BUFFER_BINDING 0x88ED
#define GL_PIXEL_UNPACK_BUFFER 0x88EC
#define GL_PIXEL_UNPACK_BUFFER_BINDING 0x88EF
#define GL_SRGB 0x8C40
#define GL_SRGB8 0x8C41
#define GL_SRGB8_ALPHA8 0x8C43
#define GL_SRGB_ALPHA 0x8C42

#define GL_BGRA_INTEGER 0x8D9B
#define GL_BGR_INTEGER 0x8D9A
#define GL_BLUE_INTEGER 0x8D96
#define GL_BUFFER_ACCESS_FLAGS 0x911F
#define GL_BUFFER_MAP_LENGTH 0x9120
#define GL_BUFFER_MAP_OFFSET 0x9121
#define GL_CLAMP_READ_COLOR 0x891C
#define GL_CLIP_DISTANCE0 0x3000
#define GL_CLIP_DISTANCE1 0x3001
#define GL_CLIP_DISTANCE2 0x3002
#define GL_CLIP_DISTANCE3 0x3003
#define GL_CLIP_DISTANCE4 0x3004
#define GL_CLIP_DISTANCE5 0x3005
#define GL_CLIP_DISTANCE6 0x3006
#define GL_CLIP_DISTANCE7 0x3007
#define GL_COLOR_ATTACHMENT0 0x8CE0
#define GL_COLOR_ATTACHMENT1 0x8CE1
#define GL_COLOR_ATTACHMENT10 0x8CEA
#define GL_COLOR_ATTACHMENT11 0x8CEB
#define GL_COLOR_ATTACHMENT12 0x8CEC
#define GL_COLOR_ATTACHMENT13 0x8CED
#define GL_COLOR_ATTACHMENT14 0x8CEE
#define GL_COLOR_ATTACHMENT15 0x8CEF
#define GL_COLOR_ATTACHMENT16 0x8CF0
#define GL_COLOR_ATTACHMENT17 0x8CF1
#define GL_COLOR_ATTACHMENT18 0x8CF2
#define GL_COLOR_ATTACHMENT19 0x8CF3
#define GL_COLOR_ATTACHMENT2 0x8CE2
#define GL_COLOR_ATTACHMENT20 0x8CF4
#define GL_COLOR_ATTACHMENT21 0x8CF5
#define GL_COLOR_ATTACHMENT22 0x8CF6
#define GL_COLOR_ATTACHMENT23 0x8CF7
#define GL_COLOR_ATTACHMENT24 0x8CF8
#define GL_COLOR_ATTACHMENT25 0x8CF9
#define GL_COLOR_ATTACHMENT26 0x8CFA
#define GL_COLOR_ATTACHMENT27 0x8CFB
#define GL_COLOR_ATTACHMENT28 0x8CFC
#define GL_COLOR_ATTACHMENT29 0x8CFD
#define GL_COLOR_ATTACHMENT3 0x8CE3
#define GL_COLOR_ATTACHMENT30 0x8CFE
#define GL_COLOR_ATTACHMENT31 0x8CFF
#define GL_COLOR_ATTACHMENT4 0x8CE4
#define GL_COLOR_ATTACHMENT5 0x8CE5
#define GL_COLOR_ATTACHMENT6 0x8CE6
#define GL_COLOR_ATTACHMENT7 0x8CE7
#define GL_COLOR_ATTACHMENT8 0x8CE8
#define GL_COLOR_ATTACHMENT9 0x8CE9
#define GL_COMPARE_REF_TO_TEXTURE 0x884E
#define GL_COMPRESSED_RED 0x8225
#define GL_COMPRESSED_RED_RGTC1 0x8DBB
#define GL_COMPRESSED_RG 0x8226
#define GL_COMPRESSED_RG_RGTC2 0x8DBD
#define GL_COMPRESSED_SIGNED_RED_RGTC1 0x8DBC
#define GL_COMPRESSED_SIGNED_RG_RGTC2 0x8DBE
#define GL_CONTEXT_FLAGS 0x821E
#define GL_CONTEXT_FLAG_FORWARD_COMPATIBLE_BIT 0x00000001
#define GL_DEPTH24_STENCIL8 0x88F0
#define GL_DEPTH32F_STENCIL8 0x8CAD
#define GL_DEPTH_ATTACHMENT 0x8D00
#define GL_DEPTH_COMPONENT32F 0x8CAC
#define GL_DEPTH_STENCIL 0x84F9
#define GL_DEPTH_STENCIL_ATTACHMENT 0x821A
#define GL_DRAW_FRAMEBUFFER 0x8CA9
#define GL_DRAW_FRAMEBUFFER_BINDING 0x8CA6
#define GL_FIXED_ONLY 0x891D
#define GL_FLOAT_32_UNSIGNED_INT_24_8_REV 0x8DAD
#define GL_FRAMEBUFFER 0x8D40
#define GL_FRAMEBUFFER_ATTACHMENT_ALPHA_SIZE 0x8215
#define GL_FRAMEBUFFER_ATTACHMENT_BLUE_SIZE 0x8214
#define GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING 0x8210
#define GL_FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE 0x8211
#define GL_FRAMEBUFFER_ATTACHMENT_DEPTH_SIZE 0x8216
#define GL_FRAMEBUFFER_ATTACHMENT_GREEN_SIZE 0x8213
#define GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME 0x8CD1
#define GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE 0x8CD0
#define GL_FRAMEBUFFER_ATTACHMENT_RED_SIZE 0x8212
#define GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE 0x8217
#define GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE 0x8CD3
#define GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LAYER 0x8CD4
#define GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL 0x8CD2
#define GL_FRAMEBUFFER_BINDING 0x8CA6
#define GL_FRAMEBUFFER_COMPLETE 0x8CD5
#define GL_FRAMEBUFFER_DEFAULT 0x8218
#define GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT 0x8CD6
#define GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER 0x8CDB
#define GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT 0x8CD7
#define GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE 0x8D56
#define GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER 0x8CDC
#define GL_FRAMEBUFFER_SRGB 0x8DB9
#define GL_FRAMEBUFFER_UNDEFINED 0x8219
#define GL_FRAMEBUFFER_UNSUPPORTED 0x8CDD
#define GL_GREEN_INTEGER 0x8D95
#define GL_HALF_FLOAT 0x140B
#define GL_INTERLEAVED_ATTRIBS 0x8C8C
#define GL_INT_SAMPLER_1D 0x8DC9
#define GL_INT_SAMPLER_1D_ARRAY 0x8DCE
#define GL_INT_SAMPLER_2D 0x8DCA
#define GL_INT_SAMPLER_2D_ARRAY 0x8DCF
#define GL_INT_SAMPLER_3D 0x8DCB
#define GL_INT_SAMPLER_CUBE 0x8DCC
#define GL_INVALID_FRAMEBUFFER_OPERATION 0x0506
#define GL_MAJOR_VERSION 0x821B
#define GL_MAP_FLUSH_EXPLICIT_BIT 0x0010
#define GL_MAP_INVALIDATE_BUFFER_BIT 0x0008
#define GL_MAP_INVALIDATE_RANGE_BIT 0x0004
#define GL_MAP_READ_BIT 0x0001
#define GL_MAP_UNSYNCHRONIZED_BIT 0x0020
#define GL_MAP_WRITE_BIT 0x0002
#define GL_MAX_ARRAY_TEXTURE_LAYERS 0x88FF
#define GL_MAX_CLIP_DISTANCES 0x0D32
#define GL_MAX_COLOR_ATTACHMENTS 0x8CDF
#define GL_MAX_PROGRAM_TEXEL_OFFSET 0x8905
#define GL_MAX_RENDERBUFFER_SIZE 0x84E8
#define GL_MAX_SAMPLES 0x8D57
#define GL_MAX_TRANSFORM_FEEDBACK_INTERLEAVED_COMPONENTS 0x8C8A
#define GL_MAX_TRANSFORM_FEEDBACK_SEPARATE_ATTRIBS 0x8C8B
#define GL_MAX_TRANSFORM_FEEDBACK_SEPARATE_COMPONENTS 0x8C80
#define GL_MAX_VARYING_COMPONENTS 0x8B4B
#define GL_MINOR_VERSION 0x821C
#define GL_MIN_PROGRAM_TEXEL_OFFSET 0x8904
#define GL_NUM_EXTENSIONS 0x821D
#define GL_PRIMITIVES_GENERATED 0x8C87
#define GL_PROXY_TEXTURE_1D_ARRAY 0x8C19
#define GL_PROXY_TEXTURE_2D_ARRAY 0x8C1B
#define GL_QUERY_BY_REGION_NO_WAIT 0x8E16
#define GL_QUERY_BY_REGION_WAIT 0x8E15
#define GL_QUERY_NO_WAIT 0x8E14
#define GL_QUERY_WAIT 0x8E13
#define GL_R11F_G11F_B10F 0x8C3A
#define GL_R16 0x822A
#define GL_R16F 0x822D
#define GL_R16I 0x8233
#define GL_R16UI 0x8234
#define GL_R32F 0x822E
#define GL_R32I 0x8235
#define GL_R32UI 0x8236
#define GL_R8 0x8229
#define GL_R8I 0x8231
#define GL_R8UI 0x8232
#define GL_RASTERIZER_DISCARD 0x8C89
#define GL_READ_FRAMEBUFFER 0x8CA8
#define GL_READ_FRAMEBUFFER_BINDING 0x8CAA
#define GL_RED_INTEGER 0x8D94
#define GL_RENDERBUFFER 0x8D41
#define GL_RENDERBUFFER_ALPHA_SIZE 0x8D53
#define GL_RENDERBUFFER_BINDING 0x8CA7
#define GL_RENDERBUFFER_BLUE_SIZE 0x8D52
#define GL_RENDERBUFFER_DEPTH_SIZE 0x8D54
#define GL_RENDERBUFFER_GREEN_SIZE 0x8D51
#define GL_RENDERBUFFER_HEIGHT 0x8D43
#define GL_RENDERBUFFER_INTERNAL_FORMAT 0x8D44
#define GL_RENDERBUFFER_RED_SIZE 0x8D50
#define GL_RENDERBUFFER_SAMPLES 0x8CAB
#define GL_RENDERBUFFER_STENCIL_SIZE 0x8D55
#define GL_RENDERBUFFER_WIDTH 0x8D42
#define GL_RG 0x8227
#define GL_RG16 0x822C
#define GL_RG16F 0x822F
#define GL_RG16I 0x8239
#define GL_RG16UI 0x823A
#define GL_RG32F 0x8230
#define GL_RG32I 0x823B
#define GL_RG32UI 0x823C
#define GL_RG8 0x822B
#define GL_RG8I 0x8237
#define GL_RG8UI 0x8238
#define GL_RGB16F 0x881B
#define GL_RGB16I 0x8D89
#define GL_RGB16UI 0x8D77
#define GL_RGB32F 0x8815
#define GL_RGB32I 0x8D83
#define GL_RGB32UI 0x8D71
#define GL_RGB8I 0x8D8F
#define GL_RGB8UI 0x8D7D
#define GL_RGB9_E5 0x8C3D
#define GL_RGBA16F 0x881A
#define GL_RGBA16I 0x8D88
#define GL_RGBA16UI 0x8D76
#define GL_RGBA32F 0x8814
#define GL_RGBA32I 0x8D82
#define GL_RGBA32UI 0x8D70
#define GL_RGBA8I 0x8D8E
#define GL_RGBA8UI 0x8D7C
#define GL_RGBA_INTEGER 0x8D99
#define GL_RGB_INTEGER 0x8D98
#define GL_RG_INTEGER 0x8228
#define GL_SAMPLER_1D_ARRAY 0x8DC0
#define GL_SAMPLER_1D_ARRAY_SHADOW 0x8DC3
#define GL_SAMPLER_2D_ARRAY 0x8DC1
#define GL_SAMPLER_2D_ARRAY_SHADOW 0x8DC4
#define GL_SAMPLER_CUBE_SHADOW 0x8DC5
#define GL_SEPARATE_ATTRIBS 0x8C8D
#define GL_STENCIL_ATTACHMENT 0x8D20
#define GL_STENCIL_INDEX1 0x8D46
#define GL_STENCIL_INDEX16 0x8D49
#define GL_STENCIL_INDEX4 0x8D47
#define GL_STENCIL_INDEX8 0x8D48
#define GL_TEXTURE_1D_ARRAY 0x8C18
#define GL_TEXTURE_2D_ARRAY 0x8C1A
#define GL_TEXTURE_ALPHA_TYPE 0x8C13
#define GL_TEXTURE_BINDING_1D_ARRAY 0x8C1C
#define GL_TEXTURE_BINDING_2D_ARRAY 0x8C1D
#define GL_TEXTURE_BLUE_TYPE 0x8C12
#define GL_TEXTURE_DEPTH_TYPE 0x8C16
#define GL_TEXTURE_GREEN_TYPE 0x8C11
#define GL_TEXTURE_RED_TYPE 0x8C10
#define GL_TEXTURE_SHARED_SIZE 0x8C3F
#define GL_TEXTURE_STENCIL_SIZE 0x88F1
#define GL_TRANSFORM_FEEDBACK_BUFFER 0x8C8E
#define GL_TRANSFORM_FEEDBACK_BUFFER_BINDING 0x8C8F
#define GL_TRANSFORM_FEEDBACK_BUFFER_MODE 0x8C7F
#define GL_TRANSFORM_FEEDBACK_BUFFER_SIZE 0x8C85
#define GL_TRANSFORM_FEEDBACK_BUFFER_START 0x8C84
#define GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN 0x8C88
#define GL_TRANSFORM_FEEDBACK_VARYINGS 0x8C83
#define GL_TRANSFORM_FEEDBACK_VARYING_MAX_LENGTH 0x8C76
#define GL_UNSIGNED_INT_10F_11F_11F_REV 0x8C3B
#define GL_UNSIGNED_INT_24_8 0x84FA
#define GL_UNSIGNED_INT_5_9_9_9_REV 0x8C3E
#define GL_UNSIGNED_INT_SAMPLER_1D 0x8DD1
#define GL_UNSIGNED_INT_SAMPLER_1D_ARRAY 0x8DD6
#define GL_UNSIGNED_INT_SAMPLER_2D 0x8DD2
#define GL_UNSIGNED_INT_SAMPLER_2D_ARRAY 0x8DD7
#define GL_UNSIGNED_INT_SAMPLER_3D 0x8DD3
#define GL_UNSIGNED_INT_SAMPLER_CUBE 0x8DD4
#define GL_UNSIGNED_INT_VEC2 0x8DC6
#define GL_UNSIGNED_INT_VEC3 0x8DC7
#define GL_UNSIGNED_INT_VEC4 0x8DC8
#define GL_UNSIGNED_NORMALIZED 0x8C17
#define GL_VERTEX_ARRAY_BINDING 0x85B5
#define GL_VERTEX_ATTRIB_ARRAY_INTEGER 0x88FD

#define GL_ACTIVE_UNIFORM_BLOCKS 0x8A36
#define GL_ACTIVE_UNIFORM_BLOCK_MAX_NAME_LENGTH 0x8A35
#define GL_COPY_READ_BUFFER 0x8F36
#define GL_COPY_WRITE_BUFFER 0x8F37
#define GL_INT_SAMPLER_2D_RECT 0x8DCD
#define GL_INT_SAMPLER_BUFFER 0x8DD0
#define GL_INVALID_INDEX 0xFFFFFFFF
#define GL_MAX_COMBINED_FRAGMENT_UNIFORM_COMPONENTS 0x8A33
#define GL_MAX_COMBINED_GEOMETRY_UNIFORM_COMPONENTS 0x8A32
#define GL_MAX_COMBINED_UNIFORM_BLOCKS 0x8A2E
#define GL_MAX_COMBINED_VERTEX_UNIFORM_COMPONENTS 0x8A31
#define GL_MAX_FRAGMENT_UNIFORM_BLOCKS 0x8A2D
#define GL_MAX_GEOMETRY_UNIFORM_BLOCKS 0x8A2C
#define GL_MAX_RECTANGLE_TEXTURE_SIZE 0x84F8
#define GL_MAX_TEXTURE_BUFFER_SIZE 0x8C2B
#define GL_MAX_UNIFORM_BLOCK_SIZE 0x8A30
#define GL_MAX_UNIFORM_BUFFER_BINDINGS 0x8A2F
#define GL_MAX_VERTEX_UNIFORM_BLOCKS 0x8A2B
#define GL_PRIMITIVE_RESTART 0x8F9D
#define GL_PRIMITIVE_RESTART_INDEX 0x8F9E
#define GL_PROXY_TEXTURE_RECTANGLE 0x84F7
#define GL_R16_SNORM 0x8F98
#define GL_R8_SNORM 0x8F94
#define GL_RG16_SNORM 0x8F99
#define GL_RG8_SNORM 0x8F95
#define GL_RGB16_SNORM 0x8F9A
#define GL_RGB8_SNORM 0x8F96
#define GL_RGBA16_SNORM 0x8F9B
#define GL_RGBA8_SNORM 0x8F97
#define GL_SAMPLER_2D_RECT 0x8B63
#define GL_SAMPLER_2D_RECT_SHADOW 0x8B64
#define GL_SAMPLER_BUFFER 0x8DC2
#define GL_SIGNED_NORMALIZED 0x8F9C
#define GL_TEXTURE_BINDING_BUFFER 0x8C2C
#define GL_TEXTURE_BINDING_RECTANGLE 0x84F6
#define GL_TEXTURE_BUFFER 0x8C2A
#define GL_TEXTURE_BUFFER_DATA_STORE_BINDING 0x8C2D
#define GL_TEXTURE_RECTANGLE 0x84F5
#define GL_UNIFORM_ARRAY_STRIDE 0x8A3C
#define GL_UNIFORM_BLOCK_ACTIVE_UNIFORMS 0x8A42
#define GL_UNIFORM_BLOCK_ACTIVE_UNIFORM_INDICES 0x8A43
#define GL_UNIFORM_BLOCK_BINDING 0x8A3F
#define GL_UNIFORM_BLOCK_DATA_SIZE 0x8A40
#define GL_UNIFORM_BLOCK_INDEX 0x8A3A
#define GL_UNIFORM_BLOCK_NAME_LENGTH 0x8A41
#define GL_UNIFORM_BLOCK_REFERENCED_BY_FRAGMENT_SHADER 0x8A46
#define GL_UNIFORM_BLOCK_REFERENCED_BY_GEOMETRY_SHADER 0x8A45
#define GL_UNIFORM_BLOCK_REFERENCED_BY_VERTEX_SHADER 0x8A44
#define GL_UNIFORM_BUFFER 0x8A11
#define GL_UNIFORM_BUFFER_BINDING 0x8A28
#define GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT 0x8A34
#define GL_UNIFORM_BUFFER_SIZE 0x8A2A
#define GL_UNIFORM_BUFFER_START 0x8A29
#define GL_UNIFORM_IS_ROW_MAJOR 0x8A3E
#define GL_UNIFORM_MATRIX_STRIDE 0x8A3D
#define GL_UNIFORM_NAME_LENGTH 0x8A39
#define GL_UNIFORM_OFFSET 0x8A3B
#define GL_UNIFORM_SIZE 0x8A38
#define GL_UNIFORM_TYPE 0x8A37
#define GL_UNSIGNED_INT_SAMPLER_2D_RECT 0x8DD5
#define GL_UNSIGNED_INT_SAMPLER_BUFFER 0x8DD8

#define GL_ALREADY_SIGNALED 0x911A
#define GL_CONDITION_SATISFIED 0x911C
#define GL_CONTEXT_COMPATIBILITY_PROFILE_BIT 0x00000002
#define GL_CONTEXT_CORE_PROFILE_BIT 0x00000001
#define GL_CONTEXT_PROFILE_MASK 0x9126
#define GL_DEPTH_CLAMP 0x864F
#define GL_FIRST_VERTEX_CONVENTION 0x8E4D
#define GL_FRAMEBUFFER_ATTACHMENT_LAYERED 0x8DA7
#define GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS 0x8DA8
#define GL_GEOMETRY_INPUT_TYPE 0x8917
#define GL_GEOMETRY_OUTPUT_TYPE 0x8918
#define GL_GEOMETRY_SHADER 0x8DD9
#define GL_GEOMETRY_VERTICES_OUT 0x8916
#define GL_INT_SAMPLER_2D_MULTISAMPLE 0x9109
#define GL_INT_SAMPLER_2D_MULTISAMPLE_ARRAY 0x910C
#define GL_LAST_VERTEX_CONVENTION 0x8E4E
#define GL_LINES_ADJACENCY 0x000A
#define GL_LINE_STRIP_ADJACENCY 0x000B
#define GL_MAX_COLOR_TEXTURE_SAMPLES 0x910E
#define GL_MAX_DEPTH_TEXTURE_SAMPLES 0x910F
#define GL_MAX_FRAGMENT_INPUT_COMPONENTS 0x9125
#define GL_MAX_GEOMETRY_INPUT_COMPONENTS 0x9123
#define GL_MAX_GEOMETRY_OUTPUT_COMPONENTS 0x9124
#define GL_MAX_GEOMETRY_OUTPUT_VERTICES 0x8DE0
#define GL_MAX_GEOMETRY_TEXTURE_IMAGE_UNITS 0x8C29
#define GL_MAX_GEOMETRY_TOTAL_OUTPUT_COMPONENTS 0x8DE1
#define GL_MAX_GEOMETRY_UNIFORM_COMPONENTS 0x8DDF
#define GL_MAX_INTEGER_SAMPLES 0x9110
#define GL_MAX_SAMPLE_MASK_WORDS 0x8E59
#define GL_MAX_SERVER_WAIT_TIMEOUT 0x9111
#define GL_MAX_VERTEX_OUTPUT_COMPONENTS 0x9122
#define GL_OBJECT_TYPE 0x9112
#define GL_PROGRAM_POINT_SIZE 0x8642
#define GL_PROVOKING_VERTEX 0x8E4F
#define GL_PROXY_TEXTURE_2D_MULTISAMPLE 0x9101
#define GL_PROXY_TEXTURE_2D_MULTISAMPLE_ARRAY 0x9103
#define GL_QUADS_FOLLOW_PROVOKING_VERTEX_CONVENTION 0x8E4C
#define GL_SAMPLER_2D_MULTISAMPLE 0x9108
#define GL_SAMPLER_2D_MULTISAMPLE_ARRAY 0x910B
#define GL_SAMPLE_MASK 0x8E51
#define GL_SAMPLE_MASK_VALUE 0x8E52
#define GL_SAMPLE_POSITION 0x8E50
#define GL_SIGNALED 0x9119
#define GL_SYNC_CONDITION 0x9113
#define GL_SYNC_FENCE 0x9116
#define GL_SYNC_FLAGS 0x9115
#define GL_SYNC_FLUSH_COMMANDS_BIT 0x00000001
#define GL_SYNC_GPU_COMMANDS_COMPLETE 0x9117
#define GL_SYNC_STATUS 0x9114
#define GL_TEXTURE_2D_MULTISAMPLE 0x9100
#define GL_TEXTURE_2D_MULTISAMPLE_ARRAY 0x9102
#define GL_TEXTURE_BINDING_2D_MULTISAMPLE 0x9104
#define GL_TEXTURE_BINDING_2D_MULTISAMPLE_ARRAY 0x9105
#define GL_TEXTURE_CUBE_MAP_SEAMLESS 0x884F
#define GL_TEXTURE_FIXED_SAMPLE_LOCATIONS 0x9107
#define GL_TEXTURE_SAMPLES 0x9106
#define GL_TIMEOUT_EXPIRED 0x911B
#define GL_TIMEOUT_IGNORED 0xFFFFFFFFFFFFFFFF
#define GL_TRIANGLES_ADJACENCY 0x000C
#define GL_TRIANGLE_STRIP_ADJACENCY 0x000D
#define GL_UNSIGNALED 0x9118
#define GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE 0x910A
#define GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE_ARRAY 0x910D
#define GL_WAIT_FAILED 0x911D

#define GL_ANY_SAMPLES_PASSED 0x8C2F
#define GL_INT_2_10_10_10_REV 0x8D9F
#define GL_MAX_DUAL_SOURCE_DRAW_BUFFERS 0x88FC
#define GL_ONE_MINUS_SRC1_ALPHA 0x88FB
#define GL_ONE_MINUS_SRC1_COLOR 0x88FA
#define GL_RGB10_A2UI 0x906F
#define GL_SAMPLER_BINDING 0x8919
#define GL_SRC1_COLOR 0x88F9
#define GL_TEXTURE_SWIZZLE_A 0x8E45
#define GL_TEXTURE_SWIZZLE_B 0x8E44
#define GL_TEXTURE_SWIZZLE_G 0x8E43
#define GL_TEXTURE_SWIZZLE_R 0x8E42
#define GL_TEXTURE_SWIZZLE_RGBA 0x8E46
#define GL_TIMESTAMP 0x8E28
#define GL_TIME_ELAPSED 0x88BF
#define GL_VERTEX_ATTRIB_ARRAY_DIVISOR 0x88FE

extern void (CODEGEN_FUNCPTR *_ptrc_glBlendFunc)(GLenum sfactor, GLenum dfactor);
#define glBlendFunc _ptrc_glBlendFunc
extern void (CODEGEN_FUNCPTR *_ptrc_glClear)(GLbitfield mask);
#define glClear _ptrc_glClear
extern void (CODEGEN_FUNCPTR *_ptrc_glClearColor)(GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha);
#define glClearColor _ptrc_glClearColor
extern void (CODEGEN_FUNCPTR *_ptrc_glClearDepth)(GLdouble depth);
#define glClearDepth _ptrc_glClearDepth
extern void (CODEGEN_FUNCPTR *_ptrc_glClearStencil)(GLint s);
#define glClearStencil _ptrc_glClearStencil
extern void (CODEGEN_FUNCPTR *_ptrc_glColorMask)(GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha);
#define glColorMask _ptrc_glColorMask
extern void (CODEGEN_FUNCPTR *_ptrc_glCullFace)(GLenum mode);
#define glCullFace _ptrc_glCullFace
extern void (CODEGEN_FUNCPTR *_ptrc_glDepthFunc)(GLenum func);
#define glDepthFunc _ptrc_glDepthFunc
extern void (CODEGEN_FUNCPTR *_ptrc_glDepthMask)(GLboolean flag);
#define glDepthMask _ptrc_glDepthMask
extern void (CODEGEN_FUNCPTR *_ptrc_glDepthRange)(GLdouble ren_near, GLdouble ren_far);
#define glDepthRange _ptrc_glDepthRange
extern void (CODEGEN_FUNCPTR *_ptrc_glDisable)(GLenum cap);
#define glDisable _ptrc_glDisable
extern void (CODEGEN_FUNCPTR *_ptrc_glDrawBuffer)(GLenum buf);
#define glDrawBuffer _ptrc_glDrawBuffer
extern void (CODEGEN_FUNCPTR *_ptrc_glEnable)(GLenum cap);
#define glEnable _ptrc_glEnable
extern void (CODEGEN_FUNCPTR *_ptrc_glFinish)(void);
#define glFinish _ptrc_glFinish
extern void (CODEGEN_FUNCPTR *_ptrc_glFlush)(void);
#define glFlush _ptrc_glFlush
extern void (CODEGEN_FUNCPTR *_ptrc_glFrontFace)(GLenum mode);
#define glFrontFace _ptrc_glFrontFace
extern void (CODEGEN_FUNCPTR *_ptrc_glGetBooleanv)(GLenum pname, GLboolean * data);
#define glGetBooleanv _ptrc_glGetBooleanv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetDoublev)(GLenum pname, GLdouble * data);
#define glGetDoublev _ptrc_glGetDoublev
extern GLenum (CODEGEN_FUNCPTR *_ptrc_glGetError)(void);
#define glGetError _ptrc_glGetError
extern void (CODEGEN_FUNCPTR *_ptrc_glGetFloatv)(GLenum pname, GLfloat * data);
#define glGetFloatv _ptrc_glGetFloatv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetIntegerv)(GLenum pname, GLint * data);
#define glGetIntegerv _ptrc_glGetIntegerv
extern const GLubyte * (CODEGEN_FUNCPTR *_ptrc_glGetString)(GLenum name);
#define glGetString _ptrc_glGetString
extern void (CODEGEN_FUNCPTR *_ptrc_glGetTexImage)(GLenum target, GLint level, GLenum format, GLenum type, void * pixels);
#define glGetTexImage _ptrc_glGetTexImage
extern void (CODEGEN_FUNCPTR *_ptrc_glGetTexLevelParameterfv)(GLenum target, GLint level, GLenum pname, GLfloat * params);
#define glGetTexLevelParameterfv _ptrc_glGetTexLevelParameterfv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetTexLevelParameteriv)(GLenum target, GLint level, GLenum pname, GLint * params);
#define glGetTexLevelParameteriv _ptrc_glGetTexLevelParameteriv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetTexParameterfv)(GLenum target, GLenum pname, GLfloat * params);
#define glGetTexParameterfv _ptrc_glGetTexParameterfv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetTexParameteriv)(GLenum target, GLenum pname, GLint * params);
#define glGetTexParameteriv _ptrc_glGetTexParameteriv
extern void (CODEGEN_FUNCPTR *_ptrc_glHint)(GLenum target, GLenum mode);
#define glHint _ptrc_glHint
extern GLboolean (CODEGEN_FUNCPTR *_ptrc_glIsEnabled)(GLenum cap);
#define glIsEnabled _ptrc_glIsEnabled
extern void (CODEGEN_FUNCPTR *_ptrc_glLineWidth)(GLfloat width);
#define glLineWidth _ptrc_glLineWidth
extern void (CODEGEN_FUNCPTR *_ptrc_glLogicOp)(GLenum opcode);
#define glLogicOp _ptrc_glLogicOp
extern void (CODEGEN_FUNCPTR *_ptrc_glPixelStoref)(GLenum pname, GLfloat param);
#define glPixelStoref _ptrc_glPixelStoref
extern void (CODEGEN_FUNCPTR *_ptrc_glPixelStorei)(GLenum pname, GLint param);
#define glPixelStorei _ptrc_glPixelStorei
extern void (CODEGEN_FUNCPTR *_ptrc_glPointSize)(GLfloat size);
#define glPointSize _ptrc_glPointSize
extern void (CODEGEN_FUNCPTR *_ptrc_glPolygonMode)(GLenum face, GLenum mode);
#define glPolygonMode _ptrc_glPolygonMode
extern void (CODEGEN_FUNCPTR *_ptrc_glReadBuffer)(GLenum src);
#define glReadBuffer _ptrc_glReadBuffer
extern void (CODEGEN_FUNCPTR *_ptrc_glReadPixels)(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, void * pixels);
#define glReadPixels _ptrc_glReadPixels
extern void (CODEGEN_FUNCPTR *_ptrc_glScissor)(GLint x, GLint y, GLsizei width, GLsizei height);
#define glScissor _ptrc_glScissor
extern void (CODEGEN_FUNCPTR *_ptrc_glStencilFunc)(GLenum func, GLint ref, GLuint mask);
#define glStencilFunc _ptrc_glStencilFunc
extern void (CODEGEN_FUNCPTR *_ptrc_glStencilMask)(GLuint mask);
#define glStencilMask _ptrc_glStencilMask
extern void (CODEGEN_FUNCPTR *_ptrc_glStencilOp)(GLenum fail, GLenum zfail, GLenum zpass);
#define glStencilOp _ptrc_glStencilOp
extern void (CODEGEN_FUNCPTR *_ptrc_glTexImage1D)(GLenum target, GLint level, GLint internalformat, GLsizei width, GLint border, GLenum format, GLenum type, const void * pixels);
#define glTexImage1D _ptrc_glTexImage1D
extern void (CODEGEN_FUNCPTR *_ptrc_glTexImage2D)(GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const void * pixels);
#define glTexImage2D _ptrc_glTexImage2D
extern void (CODEGEN_FUNCPTR *_ptrc_glTexParameterf)(GLenum target, GLenum pname, GLfloat param);
#define glTexParameterf _ptrc_glTexParameterf
extern void (CODEGEN_FUNCPTR *_ptrc_glTexParameterfv)(GLenum target, GLenum pname, const GLfloat * params);
#define glTexParameterfv _ptrc_glTexParameterfv
extern void (CODEGEN_FUNCPTR *_ptrc_glTexParameteri)(GLenum target, GLenum pname, GLint param);
#define glTexParameteri _ptrc_glTexParameteri
extern void (CODEGEN_FUNCPTR *_ptrc_glTexParameteriv)(GLenum target, GLenum pname, const GLint * params);
#define glTexParameteriv _ptrc_glTexParameteriv
extern void (CODEGEN_FUNCPTR *_ptrc_glViewport)(GLint x, GLint y, GLsizei width, GLsizei height);
#define glViewport _ptrc_glViewport

extern void (CODEGEN_FUNCPTR *_ptrc_glBindTexture)(GLenum target, GLuint texture);
#define glBindTexture _ptrc_glBindTexture
extern void (CODEGEN_FUNCPTR *_ptrc_glCopyTexImage1D)(GLenum target, GLint level, GLenum internalformat, GLint x, GLint y, GLsizei width, GLint border);
#define glCopyTexImage1D _ptrc_glCopyTexImage1D
extern void (CODEGEN_FUNCPTR *_ptrc_glCopyTexImage2D)(GLenum target, GLint level, GLenum internalformat, GLint x, GLint y, GLsizei width, GLsizei height, GLint border);
#define glCopyTexImage2D _ptrc_glCopyTexImage2D
extern void (CODEGEN_FUNCPTR *_ptrc_glCopyTexSubImage1D)(GLenum target, GLint level, GLint xoffset, GLint x, GLint y, GLsizei width);
#define glCopyTexSubImage1D _ptrc_glCopyTexSubImage1D
extern void (CODEGEN_FUNCPTR *_ptrc_glCopyTexSubImage2D)(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint x, GLint y, GLsizei width, GLsizei height);
#define glCopyTexSubImage2D _ptrc_glCopyTexSubImage2D
extern void (CODEGEN_FUNCPTR *_ptrc_glDeleteTextures)(GLsizei n, const GLuint * textures);
#define glDeleteTextures _ptrc_glDeleteTextures
extern void (CODEGEN_FUNCPTR *_ptrc_glDrawArrays)(GLenum mode, GLint first, GLsizei count);
#define glDrawArrays _ptrc_glDrawArrays
extern void (CODEGEN_FUNCPTR *_ptrc_glDrawElements)(GLenum mode, GLsizei count, GLenum type, const void * indices);
#define glDrawElements _ptrc_glDrawElements
extern void (CODEGEN_FUNCPTR *_ptrc_glGenTextures)(GLsizei n, GLuint * textures);
#define glGenTextures _ptrc_glGenTextures
extern GLboolean (CODEGEN_FUNCPTR *_ptrc_glIsTexture)(GLuint texture);
#define glIsTexture _ptrc_glIsTexture
extern void (CODEGEN_FUNCPTR *_ptrc_glPolygonOffset)(GLfloat factor, GLfloat units);
#define glPolygonOffset _ptrc_glPolygonOffset
extern void (CODEGEN_FUNCPTR *_ptrc_glTexSubImage1D)(GLenum target, GLint level, GLint xoffset, GLsizei width, GLenum format, GLenum type, const void * pixels);
#define glTexSubImage1D _ptrc_glTexSubImage1D
extern void (CODEGEN_FUNCPTR *_ptrc_glTexSubImage2D)(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const void * pixels);
#define glTexSubImage2D _ptrc_glTexSubImage2D

extern void (CODEGEN_FUNCPTR *_ptrc_glCopyTexSubImage3D)(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLint x, GLint y, GLsizei width, GLsizei height);
#define glCopyTexSubImage3D _ptrc_glCopyTexSubImage3D
extern void (CODEGEN_FUNCPTR *_ptrc_glDrawRangeElements)(GLenum mode, GLuint start, GLuint end, GLsizei count, GLenum type, const void * indices);
#define glDrawRangeElements _ptrc_glDrawRangeElements
extern void (CODEGEN_FUNCPTR *_ptrc_glTexImage3D)(GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLsizei depth, GLint border, GLenum format, GLenum type, const void * pixels);
#define glTexImage3D _ptrc_glTexImage3D
extern void (CODEGEN_FUNCPTR *_ptrc_glTexSubImage3D)(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const void * pixels);
#define glTexSubImage3D _ptrc_glTexSubImage3D

extern void (CODEGEN_FUNCPTR *_ptrc_glActiveTexture)(GLenum texture);
#define glActiveTexture _ptrc_glActiveTexture
extern void (CODEGEN_FUNCPTR *_ptrc_glCompressedTexImage1D)(GLenum target, GLint level, GLenum internalformat, GLsizei width, GLint border, GLsizei imageSize, const void * data);
#define glCompressedTexImage1D _ptrc_glCompressedTexImage1D
extern void (CODEGEN_FUNCPTR *_ptrc_glCompressedTexImage2D)(GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLint border, GLsizei imageSize, const void * data);
#define glCompressedTexImage2D _ptrc_glCompressedTexImage2D
extern void (CODEGEN_FUNCPTR *_ptrc_glCompressedTexImage3D)(GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLsizei depth, GLint border, GLsizei imageSize, const void * data);
#define glCompressedTexImage3D _ptrc_glCompressedTexImage3D
extern void (CODEGEN_FUNCPTR *_ptrc_glCompressedTexSubImage1D)(GLenum target, GLint level, GLint xoffset, GLsizei width, GLenum format, GLsizei imageSize, const void * data);
#define glCompressedTexSubImage1D _ptrc_glCompressedTexSubImage1D
extern void (CODEGEN_FUNCPTR *_ptrc_glCompressedTexSubImage2D)(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLsizei imageSize, const void * data);
#define glCompressedTexSubImage2D _ptrc_glCompressedTexSubImage2D
extern void (CODEGEN_FUNCPTR *_ptrc_glCompressedTexSubImage3D)(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLsizei imageSize, const void * data);
#define glCompressedTexSubImage3D _ptrc_glCompressedTexSubImage3D
extern void (CODEGEN_FUNCPTR *_ptrc_glGetCompressedTexImage)(GLenum target, GLint level, void * img);
#define glGetCompressedTexImage _ptrc_glGetCompressedTexImage
extern void (CODEGEN_FUNCPTR *_ptrc_glSampleCoverage)(GLfloat value, GLboolean invert);
#define glSampleCoverage _ptrc_glSampleCoverage

extern void (CODEGEN_FUNCPTR *_ptrc_glBlendColor)(GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha);
#define glBlendColor _ptrc_glBlendColor
extern void (CODEGEN_FUNCPTR *_ptrc_glBlendEquation)(GLenum mode);
#define glBlendEquation _ptrc_glBlendEquation
extern void (CODEGEN_FUNCPTR *_ptrc_glBlendFuncSeparate)(GLenum sfactorRGB, GLenum dfactorRGB, GLenum sfactorAlpha, GLenum dfactorAlpha);
#define glBlendFuncSeparate _ptrc_glBlendFuncSeparate
extern void (CODEGEN_FUNCPTR *_ptrc_glMultiDrawArrays)(GLenum mode, const GLint * first, const GLsizei * count, GLsizei drawcount);
#define glMultiDrawArrays _ptrc_glMultiDrawArrays
extern void (CODEGEN_FUNCPTR *_ptrc_glMultiDrawElements)(GLenum mode, const GLsizei * count, GLenum type, const void *const* indices, GLsizei drawcount);
#define glMultiDrawElements _ptrc_glMultiDrawElements
extern void (CODEGEN_FUNCPTR *_ptrc_glPointParameterf)(GLenum pname, GLfloat param);
#define glPointParameterf _ptrc_glPointParameterf
extern void (CODEGEN_FUNCPTR *_ptrc_glPointParameterfv)(GLenum pname, const GLfloat * params);
#define glPointParameterfv _ptrc_glPointParameterfv
extern void (CODEGEN_FUNCPTR *_ptrc_glPointParameteri)(GLenum pname, GLint param);
#define glPointParameteri _ptrc_glPointParameteri
extern void (CODEGEN_FUNCPTR *_ptrc_glPointParameteriv)(GLenum pname, const GLint * params);
#define glPointParameteriv _ptrc_glPointParameteriv

extern void (CODEGEN_FUNCPTR *_ptrc_glBeginQuery)(GLenum target, GLuint id);
#define glBeginQuery _ptrc_glBeginQuery
extern void (CODEGEN_FUNCPTR *_ptrc_glBindBuffer)(GLenum target, GLuint buffer);
#define glBindBuffer _ptrc_glBindBuffer
extern void (CODEGEN_FUNCPTR *_ptrc_glBufferData)(GLenum target, GLsizeiptr size, const void * data, GLenum usage);
#define glBufferData _ptrc_glBufferData
extern void (CODEGEN_FUNCPTR *_ptrc_glBufferSubData)(GLenum target, GLintptr offset, GLsizeiptr size, const void * data);
#define glBufferSubData _ptrc_glBufferSubData
extern void (CODEGEN_FUNCPTR *_ptrc_glDeleteBuffers)(GLsizei n, const GLuint * buffers);
#define glDeleteBuffers _ptrc_glDeleteBuffers
extern void (CODEGEN_FUNCPTR *_ptrc_glDeleteQueries)(GLsizei n, const GLuint * ids);
#define glDeleteQueries _ptrc_glDeleteQueries
extern void (CODEGEN_FUNCPTR *_ptrc_glEndQuery)(GLenum target);
#define glEndQuery _ptrc_glEndQuery
extern void (CODEGEN_FUNCPTR *_ptrc_glGenBuffers)(GLsizei n, GLuint * buffers);
#define glGenBuffers _ptrc_glGenBuffers
extern void (CODEGEN_FUNCPTR *_ptrc_glGenQueries)(GLsizei n, GLuint * ids);
#define glGenQueries _ptrc_glGenQueries
extern void (CODEGEN_FUNCPTR *_ptrc_glGetBufferParameteriv)(GLenum target, GLenum pname, GLint * params);
#define glGetBufferParameteriv _ptrc_glGetBufferParameteriv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetBufferPointerv)(GLenum target, GLenum pname, void ** params);
#define glGetBufferPointerv _ptrc_glGetBufferPointerv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetBufferSubData)(GLenum target, GLintptr offset, GLsizeiptr size, void * data);
#define glGetBufferSubData _ptrc_glGetBufferSubData
extern void (CODEGEN_FUNCPTR *_ptrc_glGetQueryObjectiv)(GLuint id, GLenum pname, GLint * params);
#define glGetQueryObjectiv _ptrc_glGetQueryObjectiv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetQueryObjectuiv)(GLuint id, GLenum pname, GLuint * params);
#define glGetQueryObjectuiv _ptrc_glGetQueryObjectuiv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetQueryiv)(GLenum target, GLenum pname, GLint * params);
#define glGetQueryiv _ptrc_glGetQueryiv
extern GLboolean (CODEGEN_FUNCPTR *_ptrc_glIsBuffer)(GLuint buffer);
#define glIsBuffer _ptrc_glIsBuffer
extern GLboolean (CODEGEN_FUNCPTR *_ptrc_glIsQuery)(GLuint id);
#define glIsQuery _ptrc_glIsQuery
extern void * (CODEGEN_FUNCPTR *_ptrc_glMapBuffer)(GLenum target, GLenum access);
#define glMapBuffer _ptrc_glMapBuffer
extern GLboolean (CODEGEN_FUNCPTR *_ptrc_glUnmapBuffer)(GLenum target);
#define glUnmapBuffer _ptrc_glUnmapBuffer

extern void (CODEGEN_FUNCPTR *_ptrc_glAttachShader)(GLuint program, GLuint shader);
#define glAttachShader _ptrc_glAttachShader
extern void (CODEGEN_FUNCPTR *_ptrc_glBindAttribLocation)(GLuint program, GLuint index, const GLchar * name);
#define glBindAttribLocation _ptrc_glBindAttribLocation
extern void (CODEGEN_FUNCPTR *_ptrc_glBlendEquationSeparate)(GLenum modeRGB, GLenum modeAlpha);
#define glBlendEquationSeparate _ptrc_glBlendEquationSeparate
extern void (CODEGEN_FUNCPTR *_ptrc_glCompileShader)(GLuint shader);
#define glCompileShader _ptrc_glCompileShader
extern GLuint (CODEGEN_FUNCPTR *_ptrc_glCreateProgram)(void);
#define glCreateProgram _ptrc_glCreateProgram
extern GLuint (CODEGEN_FUNCPTR *_ptrc_glCreateShader)(GLenum type);
#define glCreateShader _ptrc_glCreateShader
extern void (CODEGEN_FUNCPTR *_ptrc_glDeleteProgram)(GLuint program);
#define glDeleteProgram _ptrc_glDeleteProgram
extern void (CODEGEN_FUNCPTR *_ptrc_glDeleteShader)(GLuint shader);
#define glDeleteShader _ptrc_glDeleteShader
extern void (CODEGEN_FUNCPTR *_ptrc_glDetachShader)(GLuint program, GLuint shader);
#define glDetachShader _ptrc_glDetachShader
extern void (CODEGEN_FUNCPTR *_ptrc_glDisableVertexAttribArray)(GLuint index);
#define glDisableVertexAttribArray _ptrc_glDisableVertexAttribArray
extern void (CODEGEN_FUNCPTR *_ptrc_glDrawBuffers)(GLsizei n, const GLenum * bufs);
#define glDrawBuffers _ptrc_glDrawBuffers
extern void (CODEGEN_FUNCPTR *_ptrc_glEnableVertexAttribArray)(GLuint index);
#define glEnableVertexAttribArray _ptrc_glEnableVertexAttribArray
extern void (CODEGEN_FUNCPTR *_ptrc_glGetActiveAttrib)(GLuint program, GLuint index, GLsizei bufSize, GLsizei * length, GLint * size, GLenum * type, GLchar * name);
#define glGetActiveAttrib _ptrc_glGetActiveAttrib
extern void (CODEGEN_FUNCPTR *_ptrc_glGetActiveUniform)(GLuint program, GLuint index, GLsizei bufSize, GLsizei * length, GLint * size, GLenum * type, GLchar * name);
#define glGetActiveUniform _ptrc_glGetActiveUniform
extern void (CODEGEN_FUNCPTR *_ptrc_glGetAttachedShaders)(GLuint program, GLsizei maxCount, GLsizei * count, GLuint * shaders);
#define glGetAttachedShaders _ptrc_glGetAttachedShaders
extern GLint (CODEGEN_FUNCPTR *_ptrc_glGetAttribLocation)(GLuint program, const GLchar * name);
#define glGetAttribLocation _ptrc_glGetAttribLocation
extern void (CODEGEN_FUNCPTR *_ptrc_glGetProgramInfoLog)(GLuint program, GLsizei bufSize, GLsizei * length, GLchar * infoLog);
#define glGetProgramInfoLog _ptrc_glGetProgramInfoLog
extern void (CODEGEN_FUNCPTR *_ptrc_glGetProgramiv)(GLuint program, GLenum pname, GLint * params);
#define glGetProgramiv _ptrc_glGetProgramiv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetShaderInfoLog)(GLuint shader, GLsizei bufSize, GLsizei * length, GLchar * infoLog);
#define glGetShaderInfoLog _ptrc_glGetShaderInfoLog
extern void (CODEGEN_FUNCPTR *_ptrc_glGetShaderSource)(GLuint shader, GLsizei bufSize, GLsizei * length, GLchar * source);
#define glGetShaderSource _ptrc_glGetShaderSource
extern void (CODEGEN_FUNCPTR *_ptrc_glGetShaderiv)(GLuint shader, GLenum pname, GLint * params);
#define glGetShaderiv _ptrc_glGetShaderiv
extern GLint (CODEGEN_FUNCPTR *_ptrc_glGetUniformLocation)(GLuint program, const GLchar * name);
#define glGetUniformLocation _ptrc_glGetUniformLocation
extern void (CODEGEN_FUNCPTR *_ptrc_glGetUniformfv)(GLuint program, GLint location, GLfloat * params);
#define glGetUniformfv _ptrc_glGetUniformfv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetUniformiv)(GLuint program, GLint location, GLint * params);
#define glGetUniformiv _ptrc_glGetUniformiv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetVertexAttribPointerv)(GLuint index, GLenum pname, void ** pointer);
#define glGetVertexAttribPointerv _ptrc_glGetVertexAttribPointerv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetVertexAttribdv)(GLuint index, GLenum pname, GLdouble * params);
#define glGetVertexAttribdv _ptrc_glGetVertexAttribdv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetVertexAttribfv)(GLuint index, GLenum pname, GLfloat * params);
#define glGetVertexAttribfv _ptrc_glGetVertexAttribfv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetVertexAttribiv)(GLuint index, GLenum pname, GLint * params);
#define glGetVertexAttribiv _ptrc_glGetVertexAttribiv
extern GLboolean (CODEGEN_FUNCPTR *_ptrc_glIsProgram)(GLuint program);
#define glIsProgram _ptrc_glIsProgram
extern GLboolean (CODEGEN_FUNCPTR *_ptrc_glIsShader)(GLuint shader);
#define glIsShader _ptrc_glIsShader
extern void (CODEGEN_FUNCPTR *_ptrc_glLinkProgram)(GLuint program);
#define glLinkProgram _ptrc_glLinkProgram
extern void (CODEGEN_FUNCPTR *_ptrc_glShaderSource)(GLuint shader, GLsizei count, const GLchar *const* string, const GLint * length);
#define glShaderSource _ptrc_glShaderSource
extern void (CODEGEN_FUNCPTR *_ptrc_glStencilFuncSeparate)(GLenum face, GLenum func, GLint ref, GLuint mask);
#define glStencilFuncSeparate _ptrc_glStencilFuncSeparate
extern void (CODEGEN_FUNCPTR *_ptrc_glStencilMaskSeparate)(GLenum face, GLuint mask);
#define glStencilMaskSeparate _ptrc_glStencilMaskSeparate
extern void (CODEGEN_FUNCPTR *_ptrc_glStencilOpSeparate)(GLenum face, GLenum sfail, GLenum dpfail, GLenum dppass);
#define glStencilOpSeparate _ptrc_glStencilOpSeparate
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform1f)(GLint location, GLfloat v0);
#define glUniform1f _ptrc_glUniform1f
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform1fv)(GLint location, GLsizei count, const GLfloat * value);
#define glUniform1fv _ptrc_glUniform1fv
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform1i)(GLint location, GLint v0);
#define glUniform1i _ptrc_glUniform1i
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform1iv)(GLint location, GLsizei count, const GLint * value);
#define glUniform1iv _ptrc_glUniform1iv
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform2f)(GLint location, GLfloat v0, GLfloat v1);
#define glUniform2f _ptrc_glUniform2f
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform2fv)(GLint location, GLsizei count, const GLfloat * value);
#define glUniform2fv _ptrc_glUniform2fv
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform2i)(GLint location, GLint v0, GLint v1);
#define glUniform2i _ptrc_glUniform2i
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform2iv)(GLint location, GLsizei count, const GLint * value);
#define glUniform2iv _ptrc_glUniform2iv
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform3f)(GLint location, GLfloat v0, GLfloat v1, GLfloat v2);
#define glUniform3f _ptrc_glUniform3f
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform3fv)(GLint location, GLsizei count, const GLfloat * value);
#define glUniform3fv _ptrc_glUniform3fv
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform3i)(GLint location, GLint v0, GLint v1, GLint v2);
#define glUniform3i _ptrc_glUniform3i
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform3iv)(GLint location, GLsizei count, const GLint * value);
#define glUniform3iv _ptrc_glUniform3iv
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform4f)(GLint location, GLfloat v0, GLfloat v1, GLfloat v2, GLfloat v3);
#define glUniform4f _ptrc_glUniform4f
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform4fv)(GLint location, GLsizei count, const GLfloat * value);
#define glUniform4fv _ptrc_glUniform4fv
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform4i)(GLint location, GLint v0, GLint v1, GLint v2, GLint v3);
#define glUniform4i _ptrc_glUniform4i
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform4iv)(GLint location, GLsizei count, const GLint * value);
#define glUniform4iv _ptrc_glUniform4iv
extern void (CODEGEN_FUNCPTR *_ptrc_glUniformMatrix2fv)(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value);
#define glUniformMatrix2fv _ptrc_glUniformMatrix2fv
extern void (CODEGEN_FUNCPTR *_ptrc_glUniformMatrix3fv)(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value);
#define glUniformMatrix3fv _ptrc_glUniformMatrix3fv
extern void (CODEGEN_FUNCPTR *_ptrc_glUniformMatrix4fv)(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value);
#define glUniformMatrix4fv _ptrc_glUniformMatrix4fv
extern void (CODEGEN_FUNCPTR *_ptrc_glUseProgram)(GLuint program);
#define glUseProgram _ptrc_glUseProgram
extern void (CODEGEN_FUNCPTR *_ptrc_glValidateProgram)(GLuint program);
#define glValidateProgram _ptrc_glValidateProgram
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib1d)(GLuint index, GLdouble x);
#define glVertexAttrib1d _ptrc_glVertexAttrib1d
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib1dv)(GLuint index, const GLdouble * v);
#define glVertexAttrib1dv _ptrc_glVertexAttrib1dv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib1f)(GLuint index, GLfloat x);
#define glVertexAttrib1f _ptrc_glVertexAttrib1f
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib1fv)(GLuint index, const GLfloat * v);
#define glVertexAttrib1fv _ptrc_glVertexAttrib1fv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib1s)(GLuint index, GLshort x);
#define glVertexAttrib1s _ptrc_glVertexAttrib1s
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib1sv)(GLuint index, const GLshort * v);
#define glVertexAttrib1sv _ptrc_glVertexAttrib1sv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib2d)(GLuint index, GLdouble x, GLdouble y);
#define glVertexAttrib2d _ptrc_glVertexAttrib2d
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib2dv)(GLuint index, const GLdouble * v);
#define glVertexAttrib2dv _ptrc_glVertexAttrib2dv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib2f)(GLuint index, GLfloat x, GLfloat y);
#define glVertexAttrib2f _ptrc_glVertexAttrib2f
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib2fv)(GLuint index, const GLfloat * v);
#define glVertexAttrib2fv _ptrc_glVertexAttrib2fv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib2s)(GLuint index, GLshort x, GLshort y);
#define glVertexAttrib2s _ptrc_glVertexAttrib2s
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib2sv)(GLuint index, const GLshort * v);
#define glVertexAttrib2sv _ptrc_glVertexAttrib2sv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib3d)(GLuint index, GLdouble x, GLdouble y, GLdouble z);
#define glVertexAttrib3d _ptrc_glVertexAttrib3d
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib3dv)(GLuint index, const GLdouble * v);
#define glVertexAttrib3dv _ptrc_glVertexAttrib3dv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib3f)(GLuint index, GLfloat x, GLfloat y, GLfloat z);
#define glVertexAttrib3f _ptrc_glVertexAttrib3f
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib3fv)(GLuint index, const GLfloat * v);
#define glVertexAttrib3fv _ptrc_glVertexAttrib3fv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib3s)(GLuint index, GLshort x, GLshort y, GLshort z);
#define glVertexAttrib3s _ptrc_glVertexAttrib3s
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib3sv)(GLuint index, const GLshort * v);
#define glVertexAttrib3sv _ptrc_glVertexAttrib3sv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib4Nbv)(GLuint index, const GLbyte * v);
#define glVertexAttrib4Nbv _ptrc_glVertexAttrib4Nbv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib4Niv)(GLuint index, const GLint * v);
#define glVertexAttrib4Niv _ptrc_glVertexAttrib4Niv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib4Nsv)(GLuint index, const GLshort * v);
#define glVertexAttrib4Nsv _ptrc_glVertexAttrib4Nsv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib4Nub)(GLuint index, GLubyte x, GLubyte y, GLubyte z, GLubyte w);
#define glVertexAttrib4Nub _ptrc_glVertexAttrib4Nub
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib4Nubv)(GLuint index, const GLubyte * v);
#define glVertexAttrib4Nubv _ptrc_glVertexAttrib4Nubv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib4Nuiv)(GLuint index, const GLuint * v);
#define glVertexAttrib4Nuiv _ptrc_glVertexAttrib4Nuiv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib4Nusv)(GLuint index, const GLushort * v);
#define glVertexAttrib4Nusv _ptrc_glVertexAttrib4Nusv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib4bv)(GLuint index, const GLbyte * v);
#define glVertexAttrib4bv _ptrc_glVertexAttrib4bv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib4d)(GLuint index, GLdouble x, GLdouble y, GLdouble z, GLdouble w);
#define glVertexAttrib4d _ptrc_glVertexAttrib4d
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib4dv)(GLuint index, const GLdouble * v);
#define glVertexAttrib4dv _ptrc_glVertexAttrib4dv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib4f)(GLuint index, GLfloat x, GLfloat y, GLfloat z, GLfloat w);
#define glVertexAttrib4f _ptrc_glVertexAttrib4f
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib4fv)(GLuint index, const GLfloat * v);
#define glVertexAttrib4fv _ptrc_glVertexAttrib4fv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib4iv)(GLuint index, const GLint * v);
#define glVertexAttrib4iv _ptrc_glVertexAttrib4iv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib4s)(GLuint index, GLshort x, GLshort y, GLshort z, GLshort w);
#define glVertexAttrib4s _ptrc_glVertexAttrib4s
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib4sv)(GLuint index, const GLshort * v);
#define glVertexAttrib4sv _ptrc_glVertexAttrib4sv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib4ubv)(GLuint index, const GLubyte * v);
#define glVertexAttrib4ubv _ptrc_glVertexAttrib4ubv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib4uiv)(GLuint index, const GLuint * v);
#define glVertexAttrib4uiv _ptrc_glVertexAttrib4uiv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttrib4usv)(GLuint index, const GLushort * v);
#define glVertexAttrib4usv _ptrc_glVertexAttrib4usv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribPointer)(GLuint index, GLint size, GLenum type, GLboolean normalized, GLsizei stride, const void * pointer);
#define glVertexAttribPointer _ptrc_glVertexAttribPointer

extern void (CODEGEN_FUNCPTR *_ptrc_glUniformMatrix2x3fv)(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value);
#define glUniformMatrix2x3fv _ptrc_glUniformMatrix2x3fv
extern void (CODEGEN_FUNCPTR *_ptrc_glUniformMatrix2x4fv)(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value);
#define glUniformMatrix2x4fv _ptrc_glUniformMatrix2x4fv
extern void (CODEGEN_FUNCPTR *_ptrc_glUniformMatrix3x2fv)(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value);
#define glUniformMatrix3x2fv _ptrc_glUniformMatrix3x2fv
extern void (CODEGEN_FUNCPTR *_ptrc_glUniformMatrix3x4fv)(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value);
#define glUniformMatrix3x4fv _ptrc_glUniformMatrix3x4fv
extern void (CODEGEN_FUNCPTR *_ptrc_glUniformMatrix4x2fv)(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value);
#define glUniformMatrix4x2fv _ptrc_glUniformMatrix4x2fv
extern void (CODEGEN_FUNCPTR *_ptrc_glUniformMatrix4x3fv)(GLint location, GLsizei count, GLboolean transpose, const GLfloat * value);
#define glUniformMatrix4x3fv _ptrc_glUniformMatrix4x3fv

extern void (CODEGEN_FUNCPTR *_ptrc_glBeginConditionalRender)(GLuint id, GLenum mode);
#define glBeginConditionalRender _ptrc_glBeginConditionalRender
extern void (CODEGEN_FUNCPTR *_ptrc_glBeginTransformFeedback)(GLenum primitiveMode);
#define glBeginTransformFeedback _ptrc_glBeginTransformFeedback
extern void (CODEGEN_FUNCPTR *_ptrc_glBindBufferBase)(GLenum target, GLuint index, GLuint buffer);
#define glBindBufferBase _ptrc_glBindBufferBase
extern void (CODEGEN_FUNCPTR *_ptrc_glBindBufferRange)(GLenum target, GLuint index, GLuint buffer, GLintptr offset, GLsizeiptr size);
#define glBindBufferRange _ptrc_glBindBufferRange
extern void (CODEGEN_FUNCPTR *_ptrc_glBindFragDataLocation)(GLuint program, GLuint color, const GLchar * name);
#define glBindFragDataLocation _ptrc_glBindFragDataLocation
extern void (CODEGEN_FUNCPTR *_ptrc_glBindFramebuffer)(GLenum target, GLuint framebuffer);
#define glBindFramebuffer _ptrc_glBindFramebuffer
extern void (CODEGEN_FUNCPTR *_ptrc_glBindRenderbuffer)(GLenum target, GLuint renderbuffer);
#define glBindRenderbuffer _ptrc_glBindRenderbuffer
extern void (CODEGEN_FUNCPTR *_ptrc_glBindVertexArray)(GLuint ren_array);
#define glBindVertexArray _ptrc_glBindVertexArray
extern void (CODEGEN_FUNCPTR *_ptrc_glBlitFramebuffer)(GLint srcX0, GLint srcY0, GLint srcX1, GLint srcY1, GLint dstX0, GLint dstY0, GLint dstX1, GLint dstY1, GLbitfield mask, GLenum filter);
#define glBlitFramebuffer _ptrc_glBlitFramebuffer
extern GLenum (CODEGEN_FUNCPTR *_ptrc_glCheckFramebufferStatus)(GLenum target);
#define glCheckFramebufferStatus _ptrc_glCheckFramebufferStatus
extern void (CODEGEN_FUNCPTR *_ptrc_glClampColor)(GLenum target, GLenum clamp);
#define glClampColor _ptrc_glClampColor
extern void (CODEGEN_FUNCPTR *_ptrc_glClearBufferfi)(GLenum buffer, GLint drawbuffer, GLfloat depth, GLint stencil);
#define glClearBufferfi _ptrc_glClearBufferfi
extern void (CODEGEN_FUNCPTR *_ptrc_glClearBufferfv)(GLenum buffer, GLint drawbuffer, const GLfloat * value);
#define glClearBufferfv _ptrc_glClearBufferfv
extern void (CODEGEN_FUNCPTR *_ptrc_glClearBufferiv)(GLenum buffer, GLint drawbuffer, const GLint * value);
#define glClearBufferiv _ptrc_glClearBufferiv
extern void (CODEGEN_FUNCPTR *_ptrc_glClearBufferuiv)(GLenum buffer, GLint drawbuffer, const GLuint * value);
#define glClearBufferuiv _ptrc_glClearBufferuiv
extern void (CODEGEN_FUNCPTR *_ptrc_glColorMaski)(GLuint index, GLboolean r, GLboolean g, GLboolean b, GLboolean a);
#define glColorMaski _ptrc_glColorMaski
extern void (CODEGEN_FUNCPTR *_ptrc_glDeleteFramebuffers)(GLsizei n, const GLuint * framebuffers);
#define glDeleteFramebuffers _ptrc_glDeleteFramebuffers
extern void (CODEGEN_FUNCPTR *_ptrc_glDeleteRenderbuffers)(GLsizei n, const GLuint * renderbuffers);
#define glDeleteRenderbuffers _ptrc_glDeleteRenderbuffers
extern void (CODEGEN_FUNCPTR *_ptrc_glDeleteVertexArrays)(GLsizei n, const GLuint * arrays);
#define glDeleteVertexArrays _ptrc_glDeleteVertexArrays
extern void (CODEGEN_FUNCPTR *_ptrc_glDisablei)(GLenum target, GLuint index);
#define glDisablei _ptrc_glDisablei
extern void (CODEGEN_FUNCPTR *_ptrc_glEnablei)(GLenum target, GLuint index);
#define glEnablei _ptrc_glEnablei
extern void (CODEGEN_FUNCPTR *_ptrc_glEndConditionalRender)(void);
#define glEndConditionalRender _ptrc_glEndConditionalRender
extern void (CODEGEN_FUNCPTR *_ptrc_glEndTransformFeedback)(void);
#define glEndTransformFeedback _ptrc_glEndTransformFeedback
extern void (CODEGEN_FUNCPTR *_ptrc_glFlushMappedBufferRange)(GLenum target, GLintptr offset, GLsizeiptr length);
#define glFlushMappedBufferRange _ptrc_glFlushMappedBufferRange
extern void (CODEGEN_FUNCPTR *_ptrc_glFramebufferRenderbuffer)(GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer);
#define glFramebufferRenderbuffer _ptrc_glFramebufferRenderbuffer
extern void (CODEGEN_FUNCPTR *_ptrc_glFramebufferTexture1D)(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level);
#define glFramebufferTexture1D _ptrc_glFramebufferTexture1D
extern void (CODEGEN_FUNCPTR *_ptrc_glFramebufferTexture2D)(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level);
#define glFramebufferTexture2D _ptrc_glFramebufferTexture2D
extern void (CODEGEN_FUNCPTR *_ptrc_glFramebufferTexture3D)(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level, GLint zoffset);
#define glFramebufferTexture3D _ptrc_glFramebufferTexture3D
extern void (CODEGEN_FUNCPTR *_ptrc_glFramebufferTextureLayer)(GLenum target, GLenum attachment, GLuint texture, GLint level, GLint layer);
#define glFramebufferTextureLayer _ptrc_glFramebufferTextureLayer
extern void (CODEGEN_FUNCPTR *_ptrc_glGenFramebuffers)(GLsizei n, GLuint * framebuffers);
#define glGenFramebuffers _ptrc_glGenFramebuffers
extern void (CODEGEN_FUNCPTR *_ptrc_glGenRenderbuffers)(GLsizei n, GLuint * renderbuffers);
#define glGenRenderbuffers _ptrc_glGenRenderbuffers
extern void (CODEGEN_FUNCPTR *_ptrc_glGenVertexArrays)(GLsizei n, GLuint * arrays);
#define glGenVertexArrays _ptrc_glGenVertexArrays
extern void (CODEGEN_FUNCPTR *_ptrc_glGenerateMipmap)(GLenum target);
#define glGenerateMipmap _ptrc_glGenerateMipmap
extern void (CODEGEN_FUNCPTR *_ptrc_glGetBooleani_v)(GLenum target, GLuint index, GLboolean * data);
#define glGetBooleani_v _ptrc_glGetBooleani_v
extern GLint (CODEGEN_FUNCPTR *_ptrc_glGetFragDataLocation)(GLuint program, const GLchar * name);
#define glGetFragDataLocation _ptrc_glGetFragDataLocation
extern void (CODEGEN_FUNCPTR *_ptrc_glGetFramebufferAttachmentParameteriv)(GLenum target, GLenum attachment, GLenum pname, GLint * params);
#define glGetFramebufferAttachmentParameteriv _ptrc_glGetFramebufferAttachmentParameteriv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetIntegeri_v)(GLenum target, GLuint index, GLint * data);
#define glGetIntegeri_v _ptrc_glGetIntegeri_v
extern void (CODEGEN_FUNCPTR *_ptrc_glGetRenderbufferParameteriv)(GLenum target, GLenum pname, GLint * params);
#define glGetRenderbufferParameteriv _ptrc_glGetRenderbufferParameteriv
extern const GLubyte * (CODEGEN_FUNCPTR *_ptrc_glGetStringi)(GLenum name, GLuint index);
#define glGetStringi _ptrc_glGetStringi
extern void (CODEGEN_FUNCPTR *_ptrc_glGetTexParameterIiv)(GLenum target, GLenum pname, GLint * params);
#define glGetTexParameterIiv _ptrc_glGetTexParameterIiv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetTexParameterIuiv)(GLenum target, GLenum pname, GLuint * params);
#define glGetTexParameterIuiv _ptrc_glGetTexParameterIuiv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetTransformFeedbackVarying)(GLuint program, GLuint index, GLsizei bufSize, GLsizei * length, GLsizei * size, GLenum * type, GLchar * name);
#define glGetTransformFeedbackVarying _ptrc_glGetTransformFeedbackVarying
extern void (CODEGEN_FUNCPTR *_ptrc_glGetUniformuiv)(GLuint program, GLint location, GLuint * params);
#define glGetUniformuiv _ptrc_glGetUniformuiv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetVertexAttribIiv)(GLuint index, GLenum pname, GLint * params);
#define glGetVertexAttribIiv _ptrc_glGetVertexAttribIiv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetVertexAttribIuiv)(GLuint index, GLenum pname, GLuint * params);
#define glGetVertexAttribIuiv _ptrc_glGetVertexAttribIuiv
extern GLboolean (CODEGEN_FUNCPTR *_ptrc_glIsEnabledi)(GLenum target, GLuint index);
#define glIsEnabledi _ptrc_glIsEnabledi
extern GLboolean (CODEGEN_FUNCPTR *_ptrc_glIsFramebuffer)(GLuint framebuffer);
#define glIsFramebuffer _ptrc_glIsFramebuffer
extern GLboolean (CODEGEN_FUNCPTR *_ptrc_glIsRenderbuffer)(GLuint renderbuffer);
#define glIsRenderbuffer _ptrc_glIsRenderbuffer
extern GLboolean (CODEGEN_FUNCPTR *_ptrc_glIsVertexArray)(GLuint ren_array);
#define glIsVertexArray _ptrc_glIsVertexArray
extern void * (CODEGEN_FUNCPTR *_ptrc_glMapBufferRange)(GLenum target, GLintptr offset, GLsizeiptr length, GLbitfield access);
#define glMapBufferRange _ptrc_glMapBufferRange
extern void (CODEGEN_FUNCPTR *_ptrc_glRenderbufferStorage)(GLenum target, GLenum internalformat, GLsizei width, GLsizei height);
#define glRenderbufferStorage _ptrc_glRenderbufferStorage
extern void (CODEGEN_FUNCPTR *_ptrc_glRenderbufferStorageMultisample)(GLenum target, GLsizei samples, GLenum internalformat, GLsizei width, GLsizei height);
#define glRenderbufferStorageMultisample _ptrc_glRenderbufferStorageMultisample
extern void (CODEGEN_FUNCPTR *_ptrc_glTexParameterIiv)(GLenum target, GLenum pname, const GLint * params);
#define glTexParameterIiv _ptrc_glTexParameterIiv
extern void (CODEGEN_FUNCPTR *_ptrc_glTexParameterIuiv)(GLenum target, GLenum pname, const GLuint * params);
#define glTexParameterIuiv _ptrc_glTexParameterIuiv
extern void (CODEGEN_FUNCPTR *_ptrc_glTransformFeedbackVaryings)(GLuint program, GLsizei count, const GLchar *const* varyings, GLenum bufferMode);
#define glTransformFeedbackVaryings _ptrc_glTransformFeedbackVaryings
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform1ui)(GLint location, GLuint v0);
#define glUniform1ui _ptrc_glUniform1ui
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform1uiv)(GLint location, GLsizei count, const GLuint * value);
#define glUniform1uiv _ptrc_glUniform1uiv
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform2ui)(GLint location, GLuint v0, GLuint v1);
#define glUniform2ui _ptrc_glUniform2ui
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform2uiv)(GLint location, GLsizei count, const GLuint * value);
#define glUniform2uiv _ptrc_glUniform2uiv
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform3ui)(GLint location, GLuint v0, GLuint v1, GLuint v2);
#define glUniform3ui _ptrc_glUniform3ui
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform3uiv)(GLint location, GLsizei count, const GLuint * value);
#define glUniform3uiv _ptrc_glUniform3uiv
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform4ui)(GLint location, GLuint v0, GLuint v1, GLuint v2, GLuint v3);
#define glUniform4ui _ptrc_glUniform4ui
extern void (CODEGEN_FUNCPTR *_ptrc_glUniform4uiv)(GLint location, GLsizei count, const GLuint * value);
#define glUniform4uiv _ptrc_glUniform4uiv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribI1i)(GLuint index, GLint x);
#define glVertexAttribI1i _ptrc_glVertexAttribI1i
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribI1iv)(GLuint index, const GLint * v);
#define glVertexAttribI1iv _ptrc_glVertexAttribI1iv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribI1ui)(GLuint index, GLuint x);
#define glVertexAttribI1ui _ptrc_glVertexAttribI1ui
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribI1uiv)(GLuint index, const GLuint * v);
#define glVertexAttribI1uiv _ptrc_glVertexAttribI1uiv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribI2i)(GLuint index, GLint x, GLint y);
#define glVertexAttribI2i _ptrc_glVertexAttribI2i
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribI2iv)(GLuint index, const GLint * v);
#define glVertexAttribI2iv _ptrc_glVertexAttribI2iv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribI2ui)(GLuint index, GLuint x, GLuint y);
#define glVertexAttribI2ui _ptrc_glVertexAttribI2ui
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribI2uiv)(GLuint index, const GLuint * v);
#define glVertexAttribI2uiv _ptrc_glVertexAttribI2uiv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribI3i)(GLuint index, GLint x, GLint y, GLint z);
#define glVertexAttribI3i _ptrc_glVertexAttribI3i
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribI3iv)(GLuint index, const GLint * v);
#define glVertexAttribI3iv _ptrc_glVertexAttribI3iv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribI3ui)(GLuint index, GLuint x, GLuint y, GLuint z);
#define glVertexAttribI3ui _ptrc_glVertexAttribI3ui
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribI3uiv)(GLuint index, const GLuint * v);
#define glVertexAttribI3uiv _ptrc_glVertexAttribI3uiv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribI4bv)(GLuint index, const GLbyte * v);
#define glVertexAttribI4bv _ptrc_glVertexAttribI4bv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribI4i)(GLuint index, GLint x, GLint y, GLint z, GLint w);
#define glVertexAttribI4i _ptrc_glVertexAttribI4i
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribI4iv)(GLuint index, const GLint * v);
#define glVertexAttribI4iv _ptrc_glVertexAttribI4iv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribI4sv)(GLuint index, const GLshort * v);
#define glVertexAttribI4sv _ptrc_glVertexAttribI4sv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribI4ubv)(GLuint index, const GLubyte * v);
#define glVertexAttribI4ubv _ptrc_glVertexAttribI4ubv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribI4ui)(GLuint index, GLuint x, GLuint y, GLuint z, GLuint w);
#define glVertexAttribI4ui _ptrc_glVertexAttribI4ui
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribI4uiv)(GLuint index, const GLuint * v);
#define glVertexAttribI4uiv _ptrc_glVertexAttribI4uiv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribI4usv)(GLuint index, const GLushort * v);
#define glVertexAttribI4usv _ptrc_glVertexAttribI4usv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribIPointer)(GLuint index, GLint size, GLenum type, GLsizei stride, const void * pointer);
#define glVertexAttribIPointer _ptrc_glVertexAttribIPointer

extern void (CODEGEN_FUNCPTR *_ptrc_glCopyBufferSubData)(GLenum readTarget, GLenum writeTarget, GLintptr readOffset, GLintptr writeOffset, GLsizeiptr size);
#define glCopyBufferSubData _ptrc_glCopyBufferSubData
extern void (CODEGEN_FUNCPTR *_ptrc_glDrawArraysInstanced)(GLenum mode, GLint first, GLsizei count, GLsizei instancecount);
#define glDrawArraysInstanced _ptrc_glDrawArraysInstanced
extern void (CODEGEN_FUNCPTR *_ptrc_glDrawElementsInstanced)(GLenum mode, GLsizei count, GLenum type, const void * indices, GLsizei instancecount);
#define glDrawElementsInstanced _ptrc_glDrawElementsInstanced
extern void (CODEGEN_FUNCPTR *_ptrc_glGetActiveUniformBlockName)(GLuint program, GLuint uniformBlockIndex, GLsizei bufSize, GLsizei * length, GLchar * uniformBlockName);
#define glGetActiveUniformBlockName _ptrc_glGetActiveUniformBlockName
extern void (CODEGEN_FUNCPTR *_ptrc_glGetActiveUniformBlockiv)(GLuint program, GLuint uniformBlockIndex, GLenum pname, GLint * params);
#define glGetActiveUniformBlockiv _ptrc_glGetActiveUniformBlockiv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetActiveUniformName)(GLuint program, GLuint uniformIndex, GLsizei bufSize, GLsizei * length, GLchar * uniformName);
#define glGetActiveUniformName _ptrc_glGetActiveUniformName
extern void (CODEGEN_FUNCPTR *_ptrc_glGetActiveUniformsiv)(GLuint program, GLsizei uniformCount, const GLuint * uniformIndices, GLenum pname, GLint * params);
#define glGetActiveUniformsiv _ptrc_glGetActiveUniformsiv
extern GLuint (CODEGEN_FUNCPTR *_ptrc_glGetUniformBlockIndex)(GLuint program, const GLchar * uniformBlockName);
#define glGetUniformBlockIndex _ptrc_glGetUniformBlockIndex
extern void (CODEGEN_FUNCPTR *_ptrc_glGetUniformIndices)(GLuint program, GLsizei uniformCount, const GLchar *const* uniformNames, GLuint * uniformIndices);
#define glGetUniformIndices _ptrc_glGetUniformIndices
extern void (CODEGEN_FUNCPTR *_ptrc_glPrimitiveRestartIndex)(GLuint index);
#define glPrimitiveRestartIndex _ptrc_glPrimitiveRestartIndex
extern void (CODEGEN_FUNCPTR *_ptrc_glTexBuffer)(GLenum target, GLenum internalformat, GLuint buffer);
#define glTexBuffer _ptrc_glTexBuffer
extern void (CODEGEN_FUNCPTR *_ptrc_glUniformBlockBinding)(GLuint program, GLuint uniformBlockIndex, GLuint uniformBlockBinding);
#define glUniformBlockBinding _ptrc_glUniformBlockBinding

extern GLenum (CODEGEN_FUNCPTR *_ptrc_glClientWaitSync)(GLsync sync, GLbitfield flags, GLuint64 timeout);
#define glClientWaitSync _ptrc_glClientWaitSync
extern void (CODEGEN_FUNCPTR *_ptrc_glDeleteSync)(GLsync sync);
#define glDeleteSync _ptrc_glDeleteSync
extern void (CODEGEN_FUNCPTR *_ptrc_glDrawElementsBaseVertex)(GLenum mode, GLsizei count, GLenum type, const void * indices, GLint basevertex);
#define glDrawElementsBaseVertex _ptrc_glDrawElementsBaseVertex
extern void (CODEGEN_FUNCPTR *_ptrc_glDrawElementsInstancedBaseVertex)(GLenum mode, GLsizei count, GLenum type, const void * indices, GLsizei instancecount, GLint basevertex);
#define glDrawElementsInstancedBaseVertex _ptrc_glDrawElementsInstancedBaseVertex
extern void (CODEGEN_FUNCPTR *_ptrc_glDrawRangeElementsBaseVertex)(GLenum mode, GLuint start, GLuint end, GLsizei count, GLenum type, const void * indices, GLint basevertex);
#define glDrawRangeElementsBaseVertex _ptrc_glDrawRangeElementsBaseVertex
extern GLsync (CODEGEN_FUNCPTR *_ptrc_glFenceSync)(GLenum condition, GLbitfield flags);
#define glFenceSync _ptrc_glFenceSync
extern void (CODEGEN_FUNCPTR *_ptrc_glFramebufferTexture)(GLenum target, GLenum attachment, GLuint texture, GLint level);
#define glFramebufferTexture _ptrc_glFramebufferTexture
extern void (CODEGEN_FUNCPTR *_ptrc_glGetBufferParameteri64v)(GLenum target, GLenum pname, GLint64 * params);
#define glGetBufferParameteri64v _ptrc_glGetBufferParameteri64v
extern void (CODEGEN_FUNCPTR *_ptrc_glGetInteger64i_v)(GLenum target, GLuint index, GLint64 * data);
#define glGetInteger64i_v _ptrc_glGetInteger64i_v
extern void (CODEGEN_FUNCPTR *_ptrc_glGetInteger64v)(GLenum pname, GLint64 * data);
#define glGetInteger64v _ptrc_glGetInteger64v
extern void (CODEGEN_FUNCPTR *_ptrc_glGetMultisamplefv)(GLenum pname, GLuint index, GLfloat * val);
#define glGetMultisamplefv _ptrc_glGetMultisamplefv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetSynciv)(GLsync sync, GLenum pname, GLsizei bufSize, GLsizei * length, GLint * values);
#define glGetSynciv _ptrc_glGetSynciv
extern GLboolean (CODEGEN_FUNCPTR *_ptrc_glIsSync)(GLsync sync);
#define glIsSync _ptrc_glIsSync
extern void (CODEGEN_FUNCPTR *_ptrc_glMultiDrawElementsBaseVertex)(GLenum mode, const GLsizei * count, GLenum type, const void *const* indices, GLsizei drawcount, const GLint * basevertex);
#define glMultiDrawElementsBaseVertex _ptrc_glMultiDrawElementsBaseVertex
extern void (CODEGEN_FUNCPTR *_ptrc_glProvokingVertex)(GLenum mode);
#define glProvokingVertex _ptrc_glProvokingVertex
extern void (CODEGEN_FUNCPTR *_ptrc_glSampleMaski)(GLuint maskNumber, GLbitfield mask);
#define glSampleMaski _ptrc_glSampleMaski
extern void (CODEGEN_FUNCPTR *_ptrc_glTexImage2DMultisample)(GLenum target, GLsizei samples, GLenum internalformat, GLsizei width, GLsizei height, GLboolean fixedsamplelocations);
#define glTexImage2DMultisample _ptrc_glTexImage2DMultisample
extern void (CODEGEN_FUNCPTR *_ptrc_glTexImage3DMultisample)(GLenum target, GLsizei samples, GLenum internalformat, GLsizei width, GLsizei height, GLsizei depth, GLboolean fixedsamplelocations);
#define glTexImage3DMultisample _ptrc_glTexImage3DMultisample
extern void (CODEGEN_FUNCPTR *_ptrc_glWaitSync)(GLsync sync, GLbitfield flags, GLuint64 timeout);
#define glWaitSync _ptrc_glWaitSync

extern void (CODEGEN_FUNCPTR *_ptrc_glBindFragDataLocationIndexed)(GLuint program, GLuint colorNumber, GLuint index, const GLchar * name);
#define glBindFragDataLocationIndexed _ptrc_glBindFragDataLocationIndexed
extern void (CODEGEN_FUNCPTR *_ptrc_glBindSampler)(GLuint unit, GLuint sampler);
#define glBindSampler _ptrc_glBindSampler
extern void (CODEGEN_FUNCPTR *_ptrc_glDeleteSamplers)(GLsizei count, const GLuint * samplers);
#define glDeleteSamplers _ptrc_glDeleteSamplers
extern void (CODEGEN_FUNCPTR *_ptrc_glGenSamplers)(GLsizei count, GLuint * samplers);
#define glGenSamplers _ptrc_glGenSamplers
extern GLint (CODEGEN_FUNCPTR *_ptrc_glGetFragDataIndex)(GLuint program, const GLchar * name);
#define glGetFragDataIndex _ptrc_glGetFragDataIndex
extern void (CODEGEN_FUNCPTR *_ptrc_glGetQueryObjecti64v)(GLuint id, GLenum pname, GLint64 * params);
#define glGetQueryObjecti64v _ptrc_glGetQueryObjecti64v
extern void (CODEGEN_FUNCPTR *_ptrc_glGetQueryObjectui64v)(GLuint id, GLenum pname, GLuint64 * params);
#define glGetQueryObjectui64v _ptrc_glGetQueryObjectui64v
extern void (CODEGEN_FUNCPTR *_ptrc_glGetSamplerParameterIiv)(GLuint sampler, GLenum pname, GLint * params);
#define glGetSamplerParameterIiv _ptrc_glGetSamplerParameterIiv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetSamplerParameterIuiv)(GLuint sampler, GLenum pname, GLuint * params);
#define glGetSamplerParameterIuiv _ptrc_glGetSamplerParameterIuiv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetSamplerParameterfv)(GLuint sampler, GLenum pname, GLfloat * params);
#define glGetSamplerParameterfv _ptrc_glGetSamplerParameterfv
extern void (CODEGEN_FUNCPTR *_ptrc_glGetSamplerParameteriv)(GLuint sampler, GLenum pname, GLint * params);
#define glGetSamplerParameteriv _ptrc_glGetSamplerParameteriv
extern GLboolean (CODEGEN_FUNCPTR *_ptrc_glIsSampler)(GLuint sampler);
#define glIsSampler _ptrc_glIsSampler
extern void (CODEGEN_FUNCPTR *_ptrc_glQueryCounter)(GLuint id, GLenum target);
#define glQueryCounter _ptrc_glQueryCounter
extern void (CODEGEN_FUNCPTR *_ptrc_glSamplerParameterIiv)(GLuint sampler, GLenum pname, const GLint * param);
#define glSamplerParameterIiv _ptrc_glSamplerParameterIiv
extern void (CODEGEN_FUNCPTR *_ptrc_glSamplerParameterIuiv)(GLuint sampler, GLenum pname, const GLuint * param);
#define glSamplerParameterIuiv _ptrc_glSamplerParameterIuiv
extern void (CODEGEN_FUNCPTR *_ptrc_glSamplerParameterf)(GLuint sampler, GLenum pname, GLfloat param);
#define glSamplerParameterf _ptrc_glSamplerParameterf
extern void (CODEGEN_FUNCPTR *_ptrc_glSamplerParameterfv)(GLuint sampler, GLenum pname, const GLfloat * param);
#define glSamplerParameterfv _ptrc_glSamplerParameterfv
extern void (CODEGEN_FUNCPTR *_ptrc_glSamplerParameteri)(GLuint sampler, GLenum pname, GLint param);
#define glSamplerParameteri _ptrc_glSamplerParameteri
extern void (CODEGEN_FUNCPTR *_ptrc_glSamplerParameteriv)(GLuint sampler, GLenum pname, const GLint * param);
#define glSamplerParameteriv _ptrc_glSamplerParameteriv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribDivisor)(GLuint index, GLuint divisor);
#define glVertexAttribDivisor _ptrc_glVertexAttribDivisor
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribP1ui)(GLuint index, GLenum type, GLboolean normalized, GLuint value);
#define glVertexAttribP1ui _ptrc_glVertexAttribP1ui
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribP1uiv)(GLuint index, GLenum type, GLboolean normalized, const GLuint * value);
#define glVertexAttribP1uiv _ptrc_glVertexAttribP1uiv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribP2ui)(GLuint index, GLenum type, GLboolean normalized, GLuint value);
#define glVertexAttribP2ui _ptrc_glVertexAttribP2ui
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribP2uiv)(GLuint index, GLenum type, GLboolean normalized, const GLuint * value);
#define glVertexAttribP2uiv _ptrc_glVertexAttribP2uiv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribP3ui)(GLuint index, GLenum type, GLboolean normalized, GLuint value);
#define glVertexAttribP3ui _ptrc_glVertexAttribP3ui
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribP3uiv)(GLuint index, GLenum type, GLboolean normalized, const GLuint * value);
#define glVertexAttribP3uiv _ptrc_glVertexAttribP3uiv
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribP4ui)(GLuint index, GLenum type, GLboolean normalized, GLuint value);
#define glVertexAttribP4ui _ptrc_glVertexAttribP4ui
extern void (CODEGEN_FUNCPTR *_ptrc_glVertexAttribP4uiv)(GLuint index, GLenum type, GLboolean normalized, const GLuint * value);
#define glVertexAttribP4uiv _ptrc_glVertexAttribP4uiv

enum ogl_LoadStatus
{
	ogl_LOAD_FAILED = 0,
	ogl_LOAD_SUCCEEDED = 1,
};

int ogl_LoadFunctions();

int ogl_GetMinorVersion(void);
int ogl_GetMajorVersion(void);
int ogl_IsVersionGEQ(int majorVersion, int minorVersion);

#ifdef __cplusplus
}
#endif /*__cplusplus*/

#endif /*POINTER_C_GENERATED_HEADER_OPENGL_H*/

#endif // GLES
