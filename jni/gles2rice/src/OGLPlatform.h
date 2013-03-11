#include <SDL_config.h>

#if SDL_VIDEO_OPENGL
#include <SDL_opengl.h>
#define GLSL_VERSION "120"

#elif SDL_VIDEO_OPENGL_ES2
#include <SDL_opengles2.h>
#define GLSL_VERSION "100"

#endif
