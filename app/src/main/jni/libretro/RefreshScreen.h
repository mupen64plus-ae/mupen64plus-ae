#ifndef RefreshScreen_H
#define RefreshScreen_H

#include <GLES3/gl3.h>

extern void RefreshScreenInit(void);
extern void RefreshScreenDestroy(void);
extern void RefreshScreen(const void *data, unsigned width, unsigned height, size_t pitch);

#endif