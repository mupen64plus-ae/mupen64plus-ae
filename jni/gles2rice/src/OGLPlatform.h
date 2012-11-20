#ifdef GLES_2
#include "GLES2/gl2.h"
#include "GLES2/gl2ext.h"
#define GLSL_VERSION "100"
#else
#include <GL\glew.h>
#define GLSL_VERSION "120"
#endif
