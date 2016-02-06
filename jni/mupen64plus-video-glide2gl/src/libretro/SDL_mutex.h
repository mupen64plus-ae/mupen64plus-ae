#ifndef __FAKE_SDL_MUTEX_H__
#define __FAKE_SDL_MUTEX_H__

typedef int SDL_sem;
#define SDL_CreateSemaphore(...) ((int*)1)
#define SDL_SemTryWait(...) 0
#define SDL_SemPost(...)

#endif
