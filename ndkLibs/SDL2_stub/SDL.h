#ifndef __SDL2_H__
#define __SDL2_H__

#include <pthread.h>
#include <semaphore.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <errno.h>

/* Printable format: "%d.%d.%d", MAJOR, MINOR, PATCHLEVEL
*/
#define SDL_MAJOR_VERSION   1
#define SDL_MINOR_VERSION   5
#define SDL_PATCHLEVEL      0

/**
 *  \brief Macro to determine SDL version program was compiled against.
 *
 *  This macro fills in a SDL_version structure with the version of the
 *  library you compiled against. This is determined by what header the
 *  compiler uses. Note that if you dynamically linked the library, you might
 *  have a slightly newer or older version at runtime. That version can be
 *  determined with SDL_GetVersion(), which, unlike SDL_VERSION(),
 *  is not a macro.
 *
 *  \param x A pointer to a SDL_version struct to initialize.
 *
 *  \sa SDL_version
 *  \sa SDL_GetVersion
 */
#define SDL_VERSION(x)                          \
{                                   \
    (x)->major = SDL_MAJOR_VERSION;                 \
    (x)->minor = SDL_MINOR_VERSION;                 \
    (x)->patch = SDL_PATCHLEVEL;                    \
}

/**
 *  This macro turns the version numbers into a numeric value:
 *  \verbatim
    (1,2,3) -> (1203)
    \endverbatim
 *
 *  This assumes that there will never be more than 100 patchlevels.
 */
#define SDL_VERSIONNUM(X, Y, Z)                     \
    ((X)*1000 + (Y)*100 + (Z))

/**
 *  This is the version number macro for the current SDL version.
 */
#define SDL_COMPILEDVERSION \
    SDL_VERSIONNUM(SDL_MAJOR_VERSION, SDL_MINOR_VERSION, SDL_PATCHLEVEL)

/**
 *  This macro will evaluate to true if compiled with SDL at least X.Y.Z.
 */
#define SDL_VERSION_ATLEAST(X, Y, Z) \
    (SDL_COMPILEDVERSION >= SDL_VERSIONNUM(X, Y, Z))

#define SDL_INIT_TIMER          0x00000001u
#define SDL_INIT_AUDIO          0x00000010u
#define SDL_INIT_VIDEO          0x00000020u  /**< SDL_INIT_VIDEO implies SDL_INIT_EVENTS */
#define SDL_INIT_JOYSTICK       0x00000200u  /**< SDL_INIT_JOYSTICK implies SDL_INIT_EVENTS */
#define SDL_INIT_HAPTIC         0x00001000u
#define SDL_INIT_GAMECONTROLLER 0x00002000u  /**< SDL_INIT_GAMECONTROLLER implies SDL_INIT_JOYSTICK */
#define SDL_INIT_EVENTS         0x00004000u
#define SDL_INIT_SENSOR         0x00008000u
#define SDL_INIT_NOPARACHUTE    0x00100000u  /**< compatibility; this flag is ignored. */
#define SDL_INIT_EVERYTHING ( \
                SDL_INIT_TIMER | SDL_INIT_AUDIO | SDL_INIT_VIDEO | SDL_INIT_EVENTS | \
                SDL_INIT_JOYSTICK | SDL_INIT_HAPTIC | SDL_INIT_GAMECONTROLLER | SDL_INIT_SENSOR \
            )


#define SDL_VIDEO_OPENGL_ES2 1

#define SDL_QUERY   -1
#define SDL_IGNORE   0
#define SDL_DISABLE  0
#define SDL_ENABLE   1



#define SDL_ANYFORMAT       0x00100000
#define SDL_HWPALETTE       0x00200000
#define SDL_FULLSCREEN      0x00800000
#define SDL_RESIZABLE       0x01000000
#define SDL_NOFRAME         0x02000000
#define SDL_OPENGL          0x04000000
#define SDL_HWSURFACE       0x08000001  /**< \note Not used */
#define SDL_SWSURFACE       0

#define SDL_BUTTON_WHEELUP      4
#define SDL_BUTTON_WHEELDOWN    5

#define Uint32 unsigned int
#define Sint32 int
#define Uint8 unsigned char
#define Sint16 short
#define Uint16 unsigned short
#define Sint64 long long
#define SDL_JoystickID Sint32
#define SDL_TouchID Sint64
#define SDL_FingerID Sint64
#define SDL_GestureID Sint64
#define SDL_Keycode Sint32

#define SDLCALL

typedef enum
{
    SDL_FALSE = 0,
    SDL_TRUE = 1
} SDL_bool;

/**
 *  \brief OpenGL configuration attributes
 */
typedef enum
{
    SDL_GL_RED_SIZE,
    SDL_GL_GREEN_SIZE,
    SDL_GL_BLUE_SIZE,
    SDL_GL_ALPHA_SIZE,
    SDL_GL_BUFFER_SIZE,
    SDL_GL_DOUBLEBUFFER,
    SDL_GL_DEPTH_SIZE,
    SDL_GL_STENCIL_SIZE,
    SDL_GL_ACCUM_RED_SIZE,
    SDL_GL_ACCUM_GREEN_SIZE,
    SDL_GL_ACCUM_BLUE_SIZE,
    SDL_GL_ACCUM_ALPHA_SIZE,
    SDL_GL_STEREO,
    SDL_GL_MULTISAMPLEBUFFERS,
    SDL_GL_MULTISAMPLESAMPLES,
    SDL_GL_ACCELERATED_VISUAL,
    SDL_GL_RETAINED_BACKING,
    SDL_GL_CONTEXT_MAJOR_VERSION,
    SDL_GL_CONTEXT_MINOR_VERSION,
    SDL_GL_CONTEXT_EGL,
    SDL_GL_CONTEXT_FLAGS,
    SDL_GL_CONTEXT_PROFILE_MASK,
    SDL_GL_SHARE_WITH_CURRENT_CONTEXT,
    SDL_GL_FRAMEBUFFER_SRGB_CAPABLE,
    SDL_GL_CONTEXT_RELEASE_BEHAVIOR,
    SDL_GL_CONTEXT_RESET_NOTIFICATION,
    SDL_GL_CONTEXT_NO_ERROR
} SDL_GLattr;

typedef enum
{
    SDL_GL_CONTEXT_PROFILE_CORE           = 0x0001,
    SDL_GL_CONTEXT_PROFILE_COMPATIBILITY  = 0x0002,
    SDL_GL_CONTEXT_PROFILE_ES             = 0x0004 /**< GLX_CONTEXT_ES2_PROFILE_BIT_EXT */
} SDL_GLprofile;

typedef enum
{
    SDL_GL_CONTEXT_DEBUG_FLAG              = 0x0001,
    SDL_GL_CONTEXT_FORWARD_COMPATIBLE_FLAG = 0x0002,
    SDL_GL_CONTEXT_ROBUST_ACCESS_FLAG      = 0x0004,
    SDL_GL_CONTEXT_RESET_ISOLATION_FLAG    = 0x0008
} SDL_GLcontextFlag;

typedef enum
{
    SDL_GL_CONTEXT_RELEASE_BEHAVIOR_NONE   = 0x0000,
    SDL_GL_CONTEXT_RELEASE_BEHAVIOR_FLUSH  = 0x0001
} SDL_GLcontextReleaseFlag;

typedef enum
{
    SDL_GL_CONTEXT_RESET_NO_NOTIFICATION = 0x0000,
    SDL_GL_CONTEXT_RESET_LOSE_CONTEXT    = 0x0001
} SDL_GLContextResetNotification;

typedef enum
{
    SDL_WINDOWEVENT_NONE,           /**< Never used */
    SDL_WINDOWEVENT_SHOWN,          /**< Window has been shown */
    SDL_WINDOWEVENT_HIDDEN,         /**< Window has been hidden */
    SDL_WINDOWEVENT_EXPOSED,        /**< Window has been exposed and should be
                                         redrawn */
    SDL_WINDOWEVENT_MOVED,          /**< Window has been moved to data1, data2
                                     */
    SDL_WINDOWEVENT_RESIZED,        /**< Window has been resized to data1xdata2 */
    SDL_WINDOWEVENT_SIZE_CHANGED,   /**< The window size has changed, either as
                                         a result of an API call or through the
                                         system or user changing the window size. */
    SDL_WINDOWEVENT_MINIMIZED,      /**< Window has been minimized */
    SDL_WINDOWEVENT_MAXIMIZED,      /**< Window has been maximized */
    SDL_WINDOWEVENT_RESTORED,       /**< Window has been restored to normal size
                                         and position */
    SDL_WINDOWEVENT_ENTER,          /**< Window has gained mouse focus */
    SDL_WINDOWEVENT_LEAVE,          /**< Window has lost mouse focus */
    SDL_WINDOWEVENT_FOCUS_GAINED,   /**< Window has gained keyboard focus */
    SDL_WINDOWEVENT_FOCUS_LOST,     /**< Window has lost keyboard focus */
    SDL_WINDOWEVENT_CLOSE,          /**< The window manager requests that the window be closed */
    SDL_WINDOWEVENT_TAKE_FOCUS,     /**< Window is being offered a focus (should SetWindowInputFocus() on itself or a subwindow, or ignore) */
    SDL_WINDOWEVENT_HIT_TEST        /**< Window had a hit test that wasn't SDL_HITTEST_NORMAL. */
} SDL_WindowEventID;

typedef struct SDL_Color
{
    unsigned char r;
    unsigned char g;
    unsigned char b;
    unsigned char a;
} SDL_Color;

typedef struct SDL_Palette
{
    int ncolors;
    SDL_Color *colors;
    unsigned int version;
    int refcount;
} SDL_Palette;

struct SDL_mutex
{
    pthread_mutex_t id;
};
typedef struct SDL_mutex SDL_mutex;

struct SDL_semaphore
{
    sem_t sem;
};
typedef struct SDL_semaphore SDL_sem;

typedef struct SDL_PixelFormat
{
    unsigned int format;
    SDL_Palette *palette;
    unsigned char BitsPerPixel;
    unsigned char BytesPerPixel;
    unsigned char padding[2];
    unsigned int Rmask;
    unsigned int Gmask;
    unsigned int Bmask;
    unsigned int Amask;
    unsigned char Rloss;
    unsigned char Gloss;
    unsigned char Bloss;
    unsigned char Aloss;
    unsigned char Rshift;
    unsigned char Gshift;
    unsigned char Bshift;
    unsigned char Ashift;
    int refcount;
    struct SDL_PixelFormat *next;
} SDL_PixelFormat;

typedef struct SDL_Rect
{
    int x, y;
    int w, h;
} SDL_Rect;

typedef struct SDL_Surface
{
    unsigned int flags;               /**< Read-only */
    SDL_PixelFormat *format;    /**< Read-only */
    int w, h;                   /**< Read-only */
    int pitch;                  /**< Read-only */
    void *pixels;               /**< Read-write */

    /** Application data associated with the surface */
    void *userdata;             /**< Read-write */

    /** information needed for surfaces requiring locks */
    int locked;                 /**< Read-only */
    void *lock_data;            /**< Read-only */

    /** clipping information */
    SDL_Rect clip_rect;         /**< Read-only */

    /** info for fast blit mapping to other surfaces */
    struct SDL_BlitMap *map;    /**< Private */

    /** Reference count -- used when freeing surface */
    int refcount;               /**< Read-mostly */
} SDL_Surface;

typedef struct
{
    unsigned int format;              /**< pixel format */
    int w;                      /**< width, in screen coordinates */
    int h;                      /**< height, in screen coordinates */
    int refresh_rate;           /**< refresh rate (or zero for unspecified) */
    void *driverdata;           /**< driver-specific data, initialize to 0 */
} SDL_DisplayMode;

typedef struct SDL_VideoInfo
{
    Uint32 hw_available:1;
    Uint32 wm_available:1;
    Uint32 UnusedBits1:6;
    Uint32 UnusedBits2:1;
    Uint32 blit_hw:1;
    Uint32 blit_hw_CC:1;
    Uint32 blit_hw_A:1;
    Uint32 blit_sw:1;
    Uint32 blit_sw_CC:1;
    Uint32 blit_sw_A:1;
    Uint32 blit_fill:1;
    Uint32 UnusedBits3:16;
    Uint32 video_mem;

    SDL_PixelFormat *vfmt;

    int current_w;
    int current_h;
} SDL_VideoInfo;

#include "SDL_events.h"

inline void SDL_Quit(void)
{

}

inline int SDL_SetHint(const char *name, const char *value)
{
    return 1;
}

inline int SDL_InitSubSystem(unsigned int flags)
{
    return 0;
}

inline const char* SDL_GetError(void)
{
    return 0;
}

inline void SDL_QuitSubSystem(unsigned int flags)
{

}

inline unsigned int SDL_WasInit(unsigned int flags)
{
    return 1;
}

inline void SDL_FreeSurface(SDL_Surface * surface)
{

}

inline void SDL_GL_DeleteContext(void* context)
{

}

inline void SDL_DestroyWindow(void * window)
{

}

inline char* SDL_getenv(const char *name)
{
    return 0;
}

inline int SDL_atoi(const char *str)
{
    return 0;
}

inline int SDL_GetDesktopDisplayMode(int displayIndex, SDL_DisplayMode * mode)
{
    return 0;
}

inline SDL_PixelFormat * SDL_AllocFormat(unsigned int pixel_format)
{
    return 0;
}

inline int SDL_ShowCursor(int toggle)
{
    return 0;
}

static const SDL_VideoInfo * SDL_GetVideoInfo(void)
{
    static SDL_VideoInfo info;
    return &info;
}

inline SDL_Rect ** SDL_ListModes(const SDL_PixelFormat * format, Uint32 flags)
{
    SDL_Rect **modes;
    return modes;
}

inline SDL_Surface * SDL_SetVideoMode(int width, int height, int bpp, Uint32 flags)
{
    /* We're finally done! */
    return NULL;
}

inline void SDL_WM_SetCaption(const char *title, const char *icon)
{
}

inline int SDL_WM_ToggleFullScreen(SDL_Surface * surface)
{
    return 1;
}

inline void * SDL_GL_GetProcAddress(const char *proc)
{
    return 0;
}


inline int SDL_GL_SetAttribute(SDL_GLattr attr, int value)
{
    return 0;
}


inline int SDL_GL_GetAttribute(SDL_GLattr attr, int *value)
{
    return 0;
}

inline void SDL_GL_SwapBuffers(void)
{

}

inline int SDL_NumJoysticks(void)
{
    return 0;
}

inline int SDL_JoystickOpened(int joystick)
{
    return 0;
}

inline void SDL_JoystickOpen(int joystick)
{

}

inline void SDL_EnableKeyRepeat(int param1, int param2)
{

}

// For the following actually create an implementation
inline SDL_mutex * SDL_CreateMutex(void)
{
    SDL_mutex *mutex;
    pthread_mutexattr_t attr;

    /* Allocate the structure */
    mutex = (SDL_mutex *) calloc(1, sizeof(*mutex));

    pthread_mutexattr_init(&attr);
    if (pthread_mutex_init(&mutex->id, &attr) != 0) {
        free(mutex);
        mutex = NULL;
    }

    return (mutex);
}

inline void SDL_DestroyMutex(SDL_mutex * mutex)
{
    if (mutex) {
        pthread_mutex_destroy(&mutex->id);
        free(mutex);
    }
}

inline int SDL_LockMutex(SDL_mutex * mutex)
{
    if (mutex == NULL) {
        return 1;
    }

    if (pthread_mutex_lock(&mutex->id) != 0) {
        return 1;
    }

    return 0;
}

inline int SDL_TryLockMutex(SDL_mutex * mutex)
{
    int retval;
    int result;

    if (mutex == NULL) {
        return 1;
    }

    retval = 0;

    result = pthread_mutex_trylock(&mutex->id);
    if (result != 0) {
        if (result == 16) {
            retval = 1;
        } else {
            retval = 1;
        }
    }

    return retval;
}

inline int SDL_UnlockMutex(SDL_mutex * mutex)
{
    if (mutex == NULL) {
        return 1;
    }

    if (pthread_mutex_unlock(&mutex->id) != 0) {
        return 1;
    }

    return 0;
}

static struct timespec sdl_start_ts;
static struct timeval sdl_start_tv;
static SDL_bool sdl_ticks_started = SDL_FALSE;

static void SDL_TicksInit(void)
{
    if (sdl_ticks_started) {
        return;
    }
    sdl_ticks_started = SDL_TRUE;

    /* Set first ticks value */
    gettimeofday(&sdl_start_tv, NULL);
}

static Uint32 SDL_GetTicks(void)
{
    Uint32 ticks;
    if (!sdl_ticks_started) {
        SDL_TicksInit();
    }

    struct timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    ticks = (now.tv_sec - sdl_start_ts.tv_sec) * 1000 + (now.tv_nsec -
                                             sdl_start_ts.tv_nsec) / 1000000;

    return (ticks);
}

inline void SDL_Delay(Uint32 ms)
{
    int was_error;

    struct timespec elapsed, tv;

    /* Set the timeout interval */
    elapsed.tv_sec = ms / 1000;
    elapsed.tv_nsec = (ms % 1000) * 1000000;

    do {
        errno = 0;
        tv.tv_sec = elapsed.tv_sec;
        tv.tv_nsec = elapsed.tv_nsec;
        was_error = nanosleep(&tv, &elapsed);
    } while (was_error && (errno == EINTR));
}

/* Create a semaphore, initialized with value */
inline SDL_sem * SDL_CreateSemaphore(Uint32 initial_value)
{
    SDL_sem *sem = (SDL_sem *) malloc(sizeof(SDL_sem));

    if (sem_init(&sem->sem, 0, initial_value) < 0) {
        free(sem);
        sem = NULL;
    }
    return sem;
}

inline void SDL_DestroySemaphore(SDL_sem * sem)
{
    if (sem) {
        sem_destroy(&sem->sem);
        free(sem);
    }
}

inline int SDL_SemTryWait(SDL_sem * sem)
{
    int retval;

    if (!sem) {
        return 1;
    }
    retval = ETIMEDOUT;
    if (sem_trywait(&sem->sem) == 0) {
        retval = 0;
    }
    return retval;
}

inline int SDL_SemWait(SDL_sem * sem)
{
    int retval;

    if (!sem) {
        return 1;
    }

    do {
        retval = sem_wait(&sem->sem);
    } while (retval < 0 && errno == EINTR);

    if (retval < 0) {
        retval = 1;
    }
    return retval;
}

#define SDL_MUTEX_MAXWAIT   (~(Uint32)0)
#define SDL_MUTEX_TIMEDOUT  1
#define SDL_TICKS_PASSED(A, B)  ((Sint32)((B) - (A)) <= 0)

static int SDL_SemWaitTimeout(SDL_sem * sem, Uint32 timeout)
{
    int retval;
    Uint32 end;

    if (!sem) {
        return 1;
    }

    /* Try the easy cases first */
    if (timeout == 0) {
        return SDL_SemTryWait(sem);
    }
    if (timeout == SDL_MUTEX_MAXWAIT) {
        return SDL_SemWait(sem);
    }

    end = SDL_GetTicks() + timeout;
    while ((retval = SDL_SemTryWait(sem)) == SDL_MUTEX_TIMEDOUT) {
        if (SDL_TICKS_PASSED(SDL_GetTicks(), end)) {
            break;
        }
        SDL_Delay(1);
    }

    return retval;
}

inline Uint32 SDL_SemValue(SDL_sem * sem)
{
    int ret = 0;
    if (sem) {
        sem_getvalue(&sem->sem, &ret);
        if (ret < 0) {
            ret = 0;
        }
    }
    return (Uint32) ret;
}

inline int SDL_SemPost(SDL_sem * sem)
{
    int retval;

    if (!sem) {
        return 1;
    }

    retval = sem_post(&sem->sem);
    return retval;
}

#endif