/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus-sdl-audio - main.c                                        *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
 *   Copyright (C) 2007-2009 Richard Goedeken                              *
 *   Copyright (C) 2007-2008 Ebenblues                                     *
 *   Copyright (C) 2003 JttL                                               *
 *   Copyright (C) 2002 Hactarux                                           *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.          *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>

#include <SDL.h>
#include <SDL_audio.h>

#ifdef USE_SRC
#include <samplerate.h>
#endif

#include "m64p_types.h"
#include "m64p_plugin.h"
#include "m64p_config.h"

#include "main.h"
#include "volume.h"
#include "osal_dynamiclib.h"

/* Size of primary buffer in bytes. This is the buffer where audio is loaded
after it's extracted from n64's memory. */
#define PRIMARY_BUFFER_SIZE 65536

/* If buffer load goes under LOW_BUFFER_LOAD_LEVEL then game is speeded up to
fill the buffer. If buffer load exeeds HIGH_BUFFER_LOAD_LEVEL then some
extra slowdown is added to prevent buffer overflow (which is not supposed
to happen in any circumstanses if syncronization is working but because
computer's clock is such inaccurate (10ms) that might happen. I'm planning
to add support for Real Time Clock for greater accuracy but we will see.

The plugin tries to keep the buffer's load always between these values.
So if you change only PRIMARY_BUFFER_SIZE, nothing changes. You have to
adjust these values instead. You propably want to play with
LOW_BUFFER_LOAD_LEVEL if you get dropouts. */
#define LOW_BUFFER_LOAD_LEVEL 16384
#define HIGH_BUFFER_LOAD_LEVEL 32768

/* Size of secondary buffer. This is actually SDL's hardware buffer. This is
amount of samples, so final bufffer size is four times this. */
#define SECONDARY_BUFFER_SIZE 4096

/* This sets default frequency what is used if rom doesn't want to change it.
Popably only game that needs this is Zelda: Ocarina Of Time Master Quest 
*NOTICE* We should try to find out why Demos' frequencies are always wrong
They tend to rely on a default frequency, apparently, never the same one ;)*/
#define DEFAULT_FREQUENCY 33600

/* Name of config file */
#define CONFIG_FILE "jttl_audio.conf"

/* volume mixer types */
#define VOLUME_TYPE_SDL     1
#define VOLUME_TYPE_OSS     2

/* local variables */
static void (*l_DebugCallback)(void *, int, const char *) = NULL;
static void *l_DebugCallContext = NULL;
static int l_PluginInit = 0;

/* Read header for type definition */
static AUDIO_INFO AudioInfo;
/* The hardware specifications we are using */
static SDL_AudioSpec *hardware_spec;
/* Pointer to the primary audio buffer */
static unsigned char *buffer = NULL;
/* Pointer to the mixing buffer for voume control*/
static unsigned char *mixBuffer = NULL;
/* Position in buffer array where next audio chunk should be placed */
static unsigned int buffer_pos = 0;
/* Audio frequency, this is usually obtained from the game, but for compatibility we set default value */
static int GameFreq = DEFAULT_FREQUENCY;
/* This is for syncronization, it's ticks saved just before AiLenChanged() returns. */
static unsigned int last_ticks = 0;
/* SpeedFactor is used to increase/decrease game playback speed */
static unsigned int speed_factor = 100;
// AI_LEN_REG at previous round */
static unsigned int prev_len_reg = 0;
// If this is true then left and right channels are swapped */
static int SwapChannels = 0;
// Size of Primary audio buffer
static unsigned int PrimaryBufferSize = PRIMARY_BUFFER_SIZE;
// Size of Secondary audio buffer
static unsigned int SecondaryBufferSize = SECONDARY_BUFFER_SIZE;
// Lowest buffer load before we need to speed things up
static unsigned int LowBufferLoadLevel = LOW_BUFFER_LOAD_LEVEL;
// Highest buffer load before we need to slow things down
static unsigned int HighBufferLoadLevel = HIGH_BUFFER_LOAD_LEVEL;
// Resample or not
static unsigned char Resample = 1;
// volume to scale the audio by, range of 0..100
static int VolPercent = 80;
// how much percent to increment/decrement volume by
static int VolDelta = 5;
// the actual volume passed into SDL, range of 0..SDL_MIX_MAXVOLUME
static int VolSDL = SDL_MIX_MAXVOLUME;
// stores the previous volume when it is muted
static int VolMutedSave = -1;
//which type of volume control to use
static int VolumeControlType = VOLUME_TYPE_OSS;

static int OutputFreq;
static char configdir[PATH_MAX] = {0};

/* definitions of pointers to Core config functions */
ptr_ConfigListSections     ConfigListSections = NULL;
ptr_ConfigOpenSection      ConfigOpenSection = NULL;
ptr_ConfigListParameters   ConfigListParameters = NULL;
ptr_ConfigSaveFile         ConfigSaveFile = NULL;
ptr_ConfigSetParameter     ConfigSetParameter = NULL;
ptr_ConfigGetParameter     ConfigGetParameter = NULL;
ptr_ConfigGetParameterHelp ConfigGetParameterHelp = NULL;
ptr_ConfigSetDefaultInt    ConfigSetDefaultInt = NULL;
ptr_ConfigSetDefaultFloat  ConfigSetDefaultFloat = NULL;
ptr_ConfigSetDefaultBool   ConfigSetDefaultBool = NULL;
ptr_ConfigSetDefaultString ConfigSetDefaultString = NULL;
ptr_ConfigGetParamInt      ConfigGetParamInt = NULL;
ptr_ConfigGetParamFloat    ConfigGetParamFloat = NULL;
ptr_ConfigGetParamBool     ConfigGetParamBool = NULL;
ptr_ConfigGetParamString   ConfigGetParamString = NULL;

/* Global functions */
void DebugMessage(int level, const char *message, ...)
{
  char msgbuf[1024];
  va_list args;

  if (l_DebugCallback == NULL)
      return;

  va_start(args, message);
  vsprintf(msgbuf, message, args);

  (*l_DebugCallback)(l_DebugCallContext, level, msgbuf);

  va_end(args);
}

/* Mupen64Plus plugin functions */
EXPORT m64p_error CALL PluginStartup(m64p_dynlib_handle CoreLibHandle, void *Context,
                                   void (*DebugCallback)(void *, int, const char *))
{
    if (l_PluginInit)
        return M64ERR_ALREADY_INIT;

    /* first thing is to set the callback function for debug info */
    l_DebugCallback = DebugCallback;
    l_DebugCallContext = Context;

    /* Get the core config function pointers from the library handle */
    ConfigOpenSection = (ptr_ConfigOpenSection) osal_dynlib_getproc(CoreLibHandle, "ConfigOpenSection");
    ConfigSetParameter = (ptr_ConfigSetParameter) osal_dynlib_getproc(CoreLibHandle, "ConfigSetParameter");
    ConfigGetParameter = (ptr_ConfigGetParameter) osal_dynlib_getproc(CoreLibHandle, "ConfigGetParameter");
    ConfigSetDefaultInt = (ptr_ConfigSetDefaultInt) osal_dynlib_getproc(CoreLibHandle, "ConfigSetDefaultInt");
    ConfigSetDefaultFloat = (ptr_ConfigSetDefaultFloat) osal_dynlib_getproc(CoreLibHandle, "ConfigSetDefaultFloat");
    ConfigSetDefaultBool = (ptr_ConfigSetDefaultBool) osal_dynlib_getproc(CoreLibHandle, "ConfigSetDefaultBool");
    ConfigSetDefaultString = (ptr_ConfigSetDefaultString) osal_dynlib_getproc(CoreLibHandle, "ConfigSetDefaultString");
    ConfigGetParamInt = (ptr_ConfigGetParamInt) osal_dynlib_getproc(CoreLibHandle, "ConfigGetParamInt");
    ConfigGetParamFloat = (ptr_ConfigGetParamFloat) osal_dynlib_getproc(CoreLibHandle, "ConfigGetParamFloat");
    ConfigGetParamBool = (ptr_ConfigGetParamBool) osal_dynlib_getproc(CoreLibHandle, "ConfigGetParamBool");
    ConfigGetParamString = (ptr_ConfigGetParamString) osal_dynlib_getproc(CoreLibHandle, "ConfigGetParamString");

    if (!ConfigOpenSection || !ConfigSetParameter || !ConfigGetParameter ||
        !ConfigSetDefaultInt || !ConfigSetDefaultFloat || !ConfigSetDefaultBool || !ConfigSetDefaultString ||
        !ConfigGetParamInt   || !ConfigGetParamFloat   || !ConfigGetParamBool   || !ConfigGetParamString)
        return M64ERR_INCOMPATIBLE;

    l_PluginInit = 1;
    return M64ERR_SUCCESS;
}

EXPORT m64p_error CALL PluginShutdown(void)
{
    if (!l_PluginInit)
        return M64ERR_NOT_INIT;

    /* reset some local variables */
    l_DebugCallback = NULL;
    l_DebugCallContext = NULL;

    /* make sure our buffer is freed */
    if (mixBuffer != NULL)
    {
        free(mixBuffer);
        mixBuffer = NULL;
    }

    l_PluginInit = 0;
    return M64ERR_SUCCESS;
}

EXPORT m64p_error CALL PluginGetVersion(m64p_plugin_type *PluginType, int *PluginVersion, int *APIVersion, const char **PluginNamePtr, int *Capabilities)
{
    /* set version info */
    if (PluginType != NULL)
        *PluginType = M64PLUGIN_AUDIO;

    if (PluginVersion != NULL)
        *PluginVersion = 0x20000;

    if (APIVersion != NULL)
        *APIVersion = PLUGIN_API_VERSION;
    
    if (PluginNamePtr != NULL)
        *PluginNamePtr = "Mupen64Plus SDL Audio Plugin";

    if (Capabilities != NULL)
    {
        *Capabilities = 0;
    }
                    
    return M64ERR_SUCCESS;
}

/* ----------- Audio Functions ------------- */
void InitializeAudio(int freq);
void ReadConfig();
void InitializeSDL();

int critical_failure = 0;

EXPORT void CALL AiDacrateChanged( int SystemType )
{
    int f = GameFreq;

    if (!l_PluginInit)
        return;

    switch (SystemType)
    {
        case SYSTEM_NTSC:
            f = 48681812 / (*AudioInfo.AI_DACRATE_REG + 1);
            break;
        case SYSTEM_PAL:
            f = 49656530 / (*AudioInfo.AI_DACRATE_REG + 1);
            break;
        case SYSTEM_MPAL:
            f = 48628316 / (*AudioInfo.AI_DACRATE_REG + 1);
            break;
    }
    InitializeAudio(f);
}


EXPORT void CALL AiLenChanged( void )
{
    if (critical_failure == 1)
        return;
    if (!l_PluginInit)
        return;

    unsigned int LenReg = *AudioInfo.AI_LEN_REG;
    unsigned char *p = (unsigned char*)(AudioInfo.RDRAM + (*AudioInfo.AI_DRAM_ADDR_REG & 0xFFFFFF));

    DebugMessage(M64MSG_VERBOSE, "AiLenChanged(): New audio chunk, %i bytes", LenReg);

    if(buffer_pos + LenReg  < PrimaryBufferSize)
    {
        register unsigned int i;

        SDL_LockAudio();
        for ( i = 0 ; i < LenReg ; i += 4 )
        {

            if(SwapChannels == 0)
            {
                // Left channel
                buffer[ buffer_pos + i ] = p[ i + 2 ];
                buffer[ buffer_pos + i + 1 ] = p[ i + 3 ];

                // Right channel
                buffer[ buffer_pos + i + 2 ] = p[ i ];
                buffer[ buffer_pos + i + 3 ] = p[ i + 1 ];
            } else {
                // Left channel
                buffer[ buffer_pos + i ] = p[ i ];
                buffer[ buffer_pos + i + 1 ] = p[ i + 1 ];

                // Right channel
                buffer[ buffer_pos + i + 2 ] = p[ i + 2];
                buffer[ buffer_pos + i + 3 ] = p[ i + 3 ];
            }
        }
        buffer_pos += i;
        SDL_UnlockAudio();
    }
    else
    {
        DebugMessage(M64MSG_VERBOSE, "AiLenChanged(): Audio buffer overflow.");
    }

    // Time that should be sleeped to keep game in sync.
    int wait_time = 0;

    // And then syncronization */

    /* If buffer is running slow we speed up the game a bit. Actually we skip the syncronization. */
    if (buffer_pos < LowBufferLoadLevel)
    {
        wait_time = -1;
        if(buffer_pos < SecondaryBufferSize*4)
          SDL_PauseAudio(1);
    }
    else
        SDL_PauseAudio(0);

    if (wait_time != -1)
    {
        /* Adjust the game frequency by the playback speed factor for the purposes of timing */
        int InputFreq = GameFreq * speed_factor / 100;

        /* If for some reason game is runnin extremely fast and there is risk buffer is going to
           overflow, we slow down the game a bit to keep sound smooth. The overspeed is caused
           by inaccuracy in machines clock. */
        if (buffer_pos > HighBufferLoadLevel)
        {
            int overflow = (buffer_pos - HIGH_BUFFER_LOAD_LEVEL) / 4; /* in samples */
            wait_time += overflow * 1000 / InputFreq;                 /* in milliseconds */
        }

        /* calculate how many milliseconds should have elapsed since the last audio chunk was added */
        int prev_samples = prev_len_reg / 4;
        int expected_ticks = prev_samples * 1000 / InputFreq;  /* in milliseconds */

        /* now determine if we are ahead of schedule, and if so, wait */
        int cur_ticks = SDL_GetTicks();
        if (last_ticks + expected_ticks > cur_ticks)
        {
            wait_time += (last_ticks + expected_ticks) - cur_ticks;
            DebugMessage(M64MSG_VERBOSE, "AiLenChanged(): wait_time: %i, Buffer: %i/%i", wait_time, buffer_pos, PrimaryBufferSize);
            SDL_Delay(wait_time);
        }
    }

    last_ticks = SDL_GetTicks();
    prev_len_reg = LenReg;
}

EXPORT int CALL InitiateAudio( AUDIO_INFO Audio_Info )
{
    if (!l_PluginInit)
        return 0;

    AudioInfo = Audio_Info;
    return 1;
}

static int underrun_count = 0;

#ifdef USE_SRC
float *_src = 0;
unsigned int _src_len = 0;
float *_dest = 0;
unsigned int _dest_len = 0;
int error;
SRC_STATE *src_state;
SRC_DATA src_data;
#endif

static int resample(unsigned char *input, int input_avail, int oldsamplerate, unsigned char *output, int output_needed, int newsamplerate)
{

#ifdef USE_SRC
    if(Resample == 2)
    {
        // the high quality resampler needs more input than the samplerate ratio would indicate to work properly
        if (input_avail > output_needed * 3 / 2)
            input_avail = output_needed * 3 / 2; // just to avoid too much short-float-short conversion time
        if (_src_len < input_avail*2 && input_avail > 0)
        {
            if(_src) free(_src);
            _src_len = input_avail*2;
            _src = malloc(_src_len);
        }
        if (_dest_len < output_needed*2 && output_needed > 0)
        {
            if(_dest) free(_dest);
            _dest_len = output_needed*2;
            _dest = malloc(_dest_len);
        }
        memset(_src,0,_src_len);
        memset(_dest,0,_dest_len);
        if(src_state == NULL)
        {
            src_state = src_new (SRC_SINC_BEST_QUALITY, 2, &error);
            if(src_state == NULL)
            {
                memset(output, 0, output_needed);
                return;
            }
        }
        src_short_to_float_array ((short *) input, _src, input_avail/2);
        src_data.end_of_input = 0;
        src_data.data_in = _src;
        src_data.input_frames = input_avail/4;
        src_data.src_ratio = (float) newsamplerate / oldsamplerate;
        src_data.data_out = _dest;
        src_data.output_frames = output_needed/4;
        if ((error = src_process (src_state, &src_data)))
        {
            memset(output, 0, output_needed);
            return input_avail;  // number of bytes consumed
        }
        src_float_to_short_array (_dest, (short *) output, output_needed/2);
        return src_data.input_frames_used * 4;
    }
#endif
    // RESAMPLE == 1
    int *psrc = (int*)input;
    int *pdest = (int*)output;
    int i;
    int j=0;
    int sldf = oldsamplerate;
    int const2 = 2*sldf;
    int dldf = newsamplerate;
    int const1 = const2 - 2*dldf;
    int criteria = const2 - dldf;
    for(i = 0; i < output_needed/4; i++)
    {
        pdest[i] = psrc[j];
        if(criteria >= 0)
        {
            ++j;
            criteria += const1;
        }
        else criteria += const2;
    }
    return j * 4; //number of bytes consumed
}

void my_audio_callback(void *userdata, unsigned char *stream, int len)
{
    if (!l_PluginInit)
        return;

    int newsamplerate = OutputFreq * 100 / speed_factor;
    int oldsamplerate = GameFreq;

    if (buffer_pos > (len * oldsamplerate) / newsamplerate)
    {
        int input_used;
        if (VolumeControlType == VOLUME_TYPE_SDL)
        {
            input_used = resample(buffer, buffer_pos, oldsamplerate, mixBuffer, len, newsamplerate);
            SDL_MixAudio(stream, mixBuffer, len, VolSDL);
        }
        else
        {
            input_used = resample(buffer, buffer_pos, oldsamplerate, stream, len, newsamplerate);
        }
        memmove(buffer, &buffer[input_used], buffer_pos - input_used);
        buffer_pos -= input_used;
    }
    else
    {
        underrun_count++;
        DebugMessage(M64MSG_VERBOSE, "Audio buffer underrun (%i).",underrun_count);
        memset(stream , 0, len);
        buffer_pos = 0;
    }
}
EXPORT void CALL RomOpen()
{
    if (!l_PluginInit)
        return;

    ReadConfig();
    InitializeAudio(GameFreq);
}

void InitializeSDL()
{
    ReadConfig();
    DebugMessage(M64MSG_INFO, "Initializing SDL audio subsystem...");

    DebugMessage(M64MSG_VERBOSE, "Primary buffer: %i bytes.", PrimaryBufferSize);
    DebugMessage(M64MSG_VERBOSE, "Secondary buffer: %i bytes.", SecondaryBufferSize * 4);
    DebugMessage(M64MSG_VERBOSE, "Low buffer level: %i bytes.", LowBufferLoadLevel);
    DebugMessage(M64MSG_VERBOSE, "High buffer level: %i bytes.", HighBufferLoadLevel);

    if(SDL_Init(SDL_INIT_AUDIO | SDL_INIT_TIMER) < 0)
    {
        DebugMessage(M64MSG_ERROR, "Failed to initialize SDL audio subsystem; forcing exit.\n");
        critical_failure = 1;
        return;
    }
    critical_failure = 0;

}
void InitializeAudio(int freq)
{
    if(SDL_WasInit(SDL_INIT_AUDIO|SDL_INIT_TIMER) == (SDL_INIT_AUDIO|SDL_INIT_TIMER) ) 
    {
        DebugMessage(M64MSG_WARNING, "Audio and timer allready initialized.");
    }
    else 
    {
        DebugMessage(M64MSG_VERBOSE, "Audio and timer not yet initialized. Initializing...");
        InitializeSDL();
    }
    if (critical_failure == 1)
        return;
    GameFreq = freq; // This is important for the sync
    if(hardware_spec != NULL) free(hardware_spec);
    SDL_PauseAudio(1);
    SDL_CloseAudio();

    // Prototype of our callback function
    void my_audio_callback(void *userdata, unsigned char *stream, int len);

    // Open the audio device
    SDL_AudioSpec *desired, *obtained;
    
    // Allocate a desired SDL_AudioSpec
    desired = malloc(sizeof(SDL_AudioSpec));
    
    // Allocate space for the obtained SDL_AudioSpec
    obtained = malloc(sizeof(SDL_AudioSpec));
    
    // 22050Hz - FM Radio quality
    //desired->freq=freq;
    
    if(freq < 11025) OutputFreq = 11025;
    else if(freq < 22050) OutputFreq = 22050;
    else OutputFreq = 44100;
    
    desired->freq = OutputFreq;
    
    DebugMessage(M64MSG_VERBOSE, "Requesting frequency: %iHz.", desired->freq);
    /* 16-bit signed audio */
    desired->format=AUDIO_S16SYS;
    DebugMessage(M64MSG_VERBOSE, "Requesting format: %i.", desired->format);
    /* Stereo */
    desired->channels=2;
    /* Large audio buffer reduces risk of dropouts but increases response time */
    desired->samples=SecondaryBufferSize;

    /* Our callback function */
    desired->callback=my_audio_callback;
    desired->userdata=NULL;

    if(buffer == NULL)
    {
        DebugMessage(M64MSG_VERBOSE, "Allocating memory for audio buffer: %i bytes.", PrimaryBufferSize);
        buffer = (unsigned char*) malloc(PrimaryBufferSize);
    }

    if (mixBuffer == NULL)
    {
        //this should be the size of the SDL audio buffer
        mixBuffer = (unsigned char*) malloc(SecondaryBufferSize * 4);
    }

    memset(buffer, 0, PrimaryBufferSize);

    /* Open the audio device */
    if ( SDL_OpenAudio(desired, obtained) < 0 )
    {
        DebugMessage(M64MSG_ERROR, "Couldn't open audio: %s", SDL_GetError());
        critical_failure = 1;
        return;
    }
    /* desired spec is no longer needed */

    if(desired->format != obtained->format)
    {
        DebugMessage(M64MSG_WARNING, "Obtained audio format differs from requested.");
    }
    if(desired->freq != obtained->freq)
    {
        DebugMessage(M64MSG_WARNING, "Obtained frequency differs from requested.");
    }
    free(desired);
    hardware_spec=obtained;

    DebugMessage(M64MSG_VERBOSE, "Frequency: %i", hardware_spec->freq);
    DebugMessage(M64MSG_VERBOSE, "Format: %i", hardware_spec->format);
    DebugMessage(M64MSG_VERBOSE, "Channels: %i", hardware_spec->channels);
    DebugMessage(M64MSG_VERBOSE, "Silence: %i", hardware_spec->silence);
    DebugMessage(M64MSG_VERBOSE, "Samples: %i", hardware_spec->samples);
    DebugMessage(M64MSG_VERBOSE, "Size: %i", hardware_spec->size);

    SDL_PauseAudio(0);
    
    /* set playback volume */
    if (VolumeControlType == VOLUME_TYPE_SDL)
    {
        VolSDL = SDL_MIX_MAXVOLUME * VolPercent / 100;
    }
    else
    {
        VolPercent = volGet();
    }

}
EXPORT void CALL RomClosed( void )
{
    if (!l_PluginInit)
        return;
   if (critical_failure == 1)
       return;
    DebugMessage(M64MSG_VERBOSE, "Cleaning up SDL sound plugin...");
    
    // Shut down SDL Audio output
    SDL_PauseAudio(1);
    SDL_CloseAudio();

    // Delete the buffer, as we are done producing sound
    if (buffer != NULL)
    {
        free(buffer);
        buffer = NULL;
    }
    if (mixBuffer != NULL)
    {
        free(mixBuffer);
        mixBuffer = NULL;
    }

    // Delete the hardware spec struct
    if(hardware_spec != NULL) free(hardware_spec);
    hardware_spec = NULL;
    buffer = NULL;

    // Shutdown the respective subsystems
    if(SDL_WasInit(SDL_INIT_AUDIO) != 0) SDL_QuitSubSystem(SDL_INIT_AUDIO);
    if(SDL_WasInit(SDL_INIT_TIMER) != 0) SDL_QuitSubSystem(SDL_INIT_TIMER);
}

EXPORT void CALL ProcessAList()
{
}

EXPORT void CALL SetSpeedFactor(int percentage)
{
    if (!l_PluginInit)
        return;
    if (percentage >= 10 && percentage <= 300)
        speed_factor = percentage;
}

void SaveConfig()
{
    FILE *config_file;
    char path[PATH_MAX];

    if(strlen(configdir) > 0) strncpy(path, configdir, PATH_MAX);

    // Ensure that there's a trailing '/' 
    if(path[strlen(path)-1] != '/') strncat(path, "/", PATH_MAX - strlen(path));

    strncat(path, CONFIG_FILE, PATH_MAX - strlen(path));
    if ((config_file = fopen(path, "w")) == NULL)
    {
        DebugMessage(M64MSG_ERROR, "Cannot open config file for saving.\n");
        return;
    }
    fprintf(config_file, "# SDL sound plugin's config-file\n\n"
                         "# This sets default frequency which is used if rom doesn't want to change it.\n"
                         "# Probably only game that needs this is Zelda: Ocarina Of Time Master Quest\n");
    fprintf(config_file, "DEFAULT_FREQUENCY %d\n\n", GameFreq);

    fprintf(config_file, "# Swaps left and right channels ( 0 = no, 1 = yes )\n");
    fprintf(config_file, "SWAP_CHANNELS %d\n\n", SwapChannels);

    fprintf(config_file, "# Size of primary buffer in bytes. This is the buffer where audio is loaded\n"
                         "# after it's extracted from n64's memory.\n");
    fprintf(config_file, "PRIMARY_BUFFER_SIZE %d\n\n", PrimaryBufferSize);

    fprintf(config_file, "# If buffer load goes under LOW_BUFFER_LOAD_LEVEL then game is speeded up to\n"
                         "# fill the buffer. If buffer load exeeds HIGH_BUFFER_LOAD_LEVEL then some\n"
                         "# extra slowdown is added to prevent buffer overflow (which is not supposed\n"
                         "# to happen in any circumstanses if syncronization is working but because\n"
                         "# computer's clock is such inaccurate (10ms) that might happen. I'm planning\n"
                         "# to add support for Real Time Clock for greater accuracy but we will see.\n\n"
                         "# The plugin tries to keep the buffer's load always between these values.\n"
                         "# So if you change only PRIMARY_BUFFER_SIZE, nothing changes. You have to\n"
                         "# adjust these values instead. You probably want to play with\n"
                         "# LOW_BUFFER_LOAD_LEVEL if you get dropouts.\n\n");
    fprintf(config_file, "LOW_BUFFER_LOAD_LEVEL %d\n", LowBufferLoadLevel);
    fprintf(config_file, "HIGH_BUFFER_LOAD_LEVEL %d\n\n", HighBufferLoadLevel);

    fprintf(config_file, "# Size of secondary buffer. This is actually SDL's hardware buffer. This is\n"
                         "# amount of samples, so final buffer size is four times this.\n\n");
    fprintf(config_file, "SECONDARY_BUFFER_SIZE %d\n\n", SecondaryBufferSize);

    fprintf(config_file, "# Enable Linear Resampling.\n"
                         "# Possible values:\n"
                         "#  1. Unfiltered resampling (very fast, okay quality)\n"
                         "#  2. SINC resampling (Best Quality, requires libsamplerate)\n");
    fprintf(config_file, "RESAMPLE %d\n\n", Resample);

    fprintf(config_file, "# Select volume control type\n"
                         "# Possible values:\n"
                         "#  1. Use internal SDL volume control.  Changing the volume will only affect\n"
                         "#     the volume of mupen64plus and works independently of the hardware mixer.\n"
                         "#  2. Use the OSS mixer.  This directly controls the OSS mixer, adjusting the\n"
                         "#     master volume for PC\n");
    fprintf(config_file, "VOLUME_CONTROL_TYPE %d\n\n", VolumeControlType);

    fprintf(config_file, "# Default Volume (0-100%%)\n"
                         "# Only used if you set VOLUME_CONTROL_TYPE to 1.  Otherwise the default volume\n"
                         "# is the volume that the harware mixer is set to when mupen64plus loads.\n");
    fprintf(config_file, "VOLUME_DEFAULT %d\n\n", VolPercent);

    fprintf(config_file, "# Volume increment/decrement\n"
                         "# Set the percentage change each time the volume is increased or decreased.\n");
    fprintf(config_file, "VOLUME_ADJUST %d\n\n", VolDelta);

    fclose(config_file);
}

void ReadConfig()
{
    FILE * config_file;
    char line[256];
    char param[128];
    char *value;
    char path[PATH_MAX];

    if(strlen(configdir) > 0) strncpy(path, configdir, PATH_MAX);

    // Ensure that there's a trailing '/' 
    if(path[strlen(path)-1] != '/') strncat(path, "/", PATH_MAX - strlen(path));

    strncat(path, CONFIG_FILE, PATH_MAX - strlen(path));

    if ((config_file = fopen(path, "r")) == NULL)
    {
        DebugMessage(M64MSG_ERROR, "Cannot open config file.");
        return;
    }

    while(!feof(config_file))
    {
        fgets(line, 256, config_file);
        if((line[0] != '#') && (strlen(line) > 1))
        {
            value = strchr(line, ' ');
            if (value[strlen(value)-1] == '\n') value[strlen(value)-1] = '\0';

            strncpy(param, line, (strlen(line) - strlen(value)));
            param[(strlen(line) - strlen(value))] = '\0';
            if(strcasecmp(param, "DEFAULT_FREQUENCY") == 0) GameFreq = atoi(value);
            if(strcasecmp(param, "SWAP_CHANNELS") == 0) SwapChannels = atoi(value);
            if(strcasecmp(param, "PRIMARY_BUFFER_SIZE") == 0) PrimaryBufferSize = atoi(value);
            if(strcasecmp(param, "SECONDARY_BUFFER_SIZE") == 0) SecondaryBufferSize = atoi(value);
            if(strcasecmp(param, "LOW_BUFFER_LOAD_LEVEL") == 0) LowBufferLoadLevel = atoi(value);
            if(strcasecmp(param, "HIGH_BUFFER_LOAD_LEVEL") == 0) HighBufferLoadLevel = atoi(value);
            if(strcasecmp(param, "RESAMPLE") == 0) Resample = atoi(value);
            if(strcasecmp(param, "VOLUME_CONTROL_TYPE") == 0) VolumeControlType = atoi(value);
            if(strcasecmp(param, "VOLUME_ADJUST") == 0) VolDelta = atoi(value);
            if(strcasecmp(param, "VOLUME_DEFAULT") == 0) VolPercent = atoi(value);
        }
    }
    fclose(config_file);
}

EXPORT void CALL VolumeMute(void)
{
    if (!l_PluginInit)
        return;

    if (VolMutedSave > -1)
    {
        //unmute
        VolPercent = VolMutedSave;
        VolMutedSave = -1;
        if (VolumeControlType == VOLUME_TYPE_SDL)
        {
            VolSDL = SDL_MIX_MAXVOLUME * VolPercent / 100;
        }
        else
        {
            //OSS mixer volume
            volSet(VolPercent);
        }
    } 
    else
    {
        //mute
        VolMutedSave = VolPercent;
        VolPercent = 0;
        if (VolumeControlType == VOLUME_TYPE_SDL)
        {
            VolSDL = 0;
        }
        else
        {
            //OSS mixer volume
            volSet(0);
        }
    }
}

EXPORT void CALL VolumeUp(void)
{
    if (!l_PluginInit)
        return;

    //if muted, unmute first
    if (VolMutedSave > -1)
        VolumeMute();

    // reload volume if we're using OSS
    if (VolumeControlType == VOLUME_TYPE_OSS)
    {
        VolPercent = volGet();
    }

    // adjust volume variable
    VolPercent += VolDelta;
    if (VolPercent > 100)
        VolPercent = 100;

    if (VolumeControlType == VOLUME_TYPE_SDL) 
    {
        VolSDL = SDL_MIX_MAXVOLUME * VolPercent / 100;
    }
    else
    {
        //OSS mixer volume
        volSet(VolPercent);
    }
}

EXPORT void CALL VolumeDown(void)
{
    if (!l_PluginInit)
        return;

    //if muted, unmute first
    if (VolMutedSave > -1)
        VolumeMute();

    // reload volume if we're using OSS
    if (VolumeControlType == VOLUME_TYPE_OSS)
    {
        VolPercent = volGet();
    }

    // adjust volume variable
    VolPercent -= VolDelta;
    if (VolPercent < 0)
        VolPercent = 0;

    if (VolumeControlType == VOLUME_TYPE_SDL)
    {
        VolSDL = SDL_MIX_MAXVOLUME * VolPercent / 100;
    }
    else
    {
        //OSS mixer volume
        volSet(VolPercent);
    }
}

EXPORT int CALL VolumeGetLevel(void)
{
    return VolPercent;
}

EXPORT void CALL VolumeSetLevel(int level)
{
    if (!l_PluginInit)
        return;

    // adjust volume 
    VolPercent = level;
    if (VolPercent < 0)
        VolPercent = 0;
    else if (VolPercent > 100)
        VolPercent = 100;

    if (VolumeControlType == VOLUME_TYPE_SDL)
    {
        VolSDL = SDL_MIX_MAXVOLUME * VolPercent / 100;
    }
    else
    {
        //OSS mixer volume
        volSet(VolPercent);
    }
}

static char VolumeString[32];

EXPORT const char * CALL VolumeGetString(void)
{
    if (VolMutedSave > -1)
    {
        strcpy(VolumeString, "Mute");
    }
    else
    {
        sprintf(VolumeString, "%i%%", VolPercent);
    }

    return VolumeString;
}

