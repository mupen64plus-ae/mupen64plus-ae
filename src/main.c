/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus - main.c                                                  *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
 *   Copyright (C) 2007-2008 Richard42 Ebenblues                           *
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
#include <SDL_thread.h>

#ifdef USE_SRC
#include <samplerate.h>
#endif

#include "../main/winlnxdefs.h"

#include "Audio_1.2.h"
#include "gui.h"
#include "volume.h"
#include "main.h"

#include "../main/version.h"
#include "../main/translate.h"

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

/*--------------- VARIABLE DEFINITIONS ----------------*/

/* Read header for type definition */
static AUDIO_INFO AudioInfo;
/* The hardware specifications we are using */
static SDL_AudioSpec *hardware_spec;
/* Pointer to the primary audio buffer */
static Uint8 *buffer = NULL;
/* Pointer to the mixing buffer for voume control*/
static Uint8 *mixBuffer = NULL;
/* Position in buffer array where next audio chunk should be placed */
static unsigned int buffer_pos = 0;
/* Audio frequency, this is usually obtained from the game, but for compatibility we set default value */
int GameFreq = DEFAULT_FREQUENCY;
/* This is for syncronization, it's ticks saved just before AiLenChanged() returns. */
static Uint32 last_ticks = 0;
/* SpeedFactor is used to increase/decrease game playback speed */
static Uint32 speed_factor = 100;
// AI_LEN_REG at previous round */
static DWORD prev_len_reg = 0;
// If this is true then left and right channels are swapped */
BOOL SwapChannels = FALSE;
// Size of Primary audio buffer
Uint32 PrimaryBufferSize = PRIMARY_BUFFER_SIZE;
// Size of Secondary audio buffer
Uint32 SecondaryBufferSize = SECONDARY_BUFFER_SIZE;
// Lowest buffer load before we need to speed things up
Uint32 LowBufferLoadLevel = LOW_BUFFER_LOAD_LEVEL;
// Highest buffer load before we need to slow things down
Uint32 HighBufferLoadLevel = HIGH_BUFFER_LOAD_LEVEL;
// Resample or not
Uint8 Resample = 1;
// volume to scale the audio by, range of 0..100
int VolPercent = 80;
// how much percent to increment/decrement volume by
int VolDelta = 5;
// the actual volume passed into SDL, range of 0..SDL_MIX_MAXVOLUME
static int VolSDL = SDL_MIX_MAXVOLUME;
// stores the previous volume when it is muted
static int VolMutedSave = -1;
//which type of volume control to use
int VolumeControlType = VOLUME_TYPE_OSS;

static int OutputFreq;
static char configdir[PATH_MAX] = {0};

/* ----------- FUNCTIONS ------------- */
/* This function closes the audio device and reinitializes it with requested frequency */
void InitializeAudio(int freq);
void ReadConfig();
void InitializeSDL();

int critical_failure = 0;

EXPORT void CALL AiDacrateChanged( int SystemType )
{
    int f = GameFreq;
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
    DWORD LenReg = *AudioInfo.AI_LEN_REG;
    Uint8 *p = (Uint8*)(AudioInfo.RDRAM + (*AudioInfo.AI_DRAM_ADDR_REG & 0xFFFFFF));
#ifdef DEBUG
    printf("[JttL's SDL Audio plugin] Debug: New audio chunk, %i bytes\n", LenReg);
#endif
    if(buffer_pos + LenReg  < PrimaryBufferSize)
    {
        register unsigned int i;

        SDL_LockAudio();
        for ( i = 0 ; i < LenReg ; i += 4 )
        {

            if(SwapChannels == FALSE)
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
#ifdef DEBUG
    else
    {
        fprintf(stderr, "[JttL's SDL Audio plugin] Debug: Audio buffer overflow.\n");
    }
#endif

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
#ifdef DEBUG
            printf("[JttL's SDL Audio plugin] Debug: wait_time: %i, Buffer: %i/%i\n", wait_time, buffer_pos, PrimaryBufferSize);
#endif
            SDL_Delay(wait_time);
        }
    }

    last_ticks = SDL_GetTicks();
    prev_len_reg = LenReg;
}

EXPORT DWORD CALL AiReadLength( void )
{
    return 0;
}

EXPORT void CALL CloseDLL( void )
{
    if (mixBuffer != NULL)
    {
        free(mixBuffer);
        mixBuffer = NULL;
    }

}

EXPORT void CALL DllTest ( HWND hParent )
{
    // Defining flags for tests
    BOOL init_audio = FALSE;
    BOOL init_timer = FALSE;
    BOOL open_audio_device = FALSE;
    BOOL format_match = FALSE;
    BOOL freq_match = FALSE;

    // Storage for SDL_Errors.
    char *sdl_error[3];

    // Clear the pointers (Should not be truly necessary unless something horrible goes wrong)
    memset(sdl_error, 0, sizeof(char*[3]));

    // Print out inital message
    printf("[JttL's SDL Audio plugin] Starting Audio Test.\n");

    // Make Sure SDL Audio is disabled so we can restart fresh
    SDL_PauseAudio(1);
    SDL_CloseAudio();

    // Quit the subsystems before attempting to reinitalize them, if either are initalized already
    if(SDL_WasInit(SDL_INIT_AUDIO) != 0) SDL_QuitSubSystem(SDL_INIT_AUDIO);
    if(SDL_WasInit(SDL_INIT_TIMER) != 0) SDL_QuitSubSystem(SDL_INIT_TIMER);

    // Attempt to initialize SDL Audio
    if(SDL_Init(SDL_INIT_AUDIO) < 0 )
    {
        sdl_error[0] = SDL_GetError();
        printf("[JttL's SDL Audio plugin] Error: Couldn't initialize audio subsystem: %s\n", sdl_error[0]);
        init_audio = FALSE;
    }
    else
    {
        printf("[JttL's SDL Audio plugin] Audio subsystem initialized.\n");
        init_audio = TRUE;
    }

    // Attempt to initialize SDL Timer
    if(SDL_InitSubSystem(SDL_INIT_TIMER) < 0 )
    {
        sdl_error[1] = SDL_GetError();
        printf("[JttL's SDL Audio plugin] Error: Couldn't initialize timer subsystem: %s\n", sdl_error[1]);
        init_timer = FALSE;
    }
    else
    {
        printf("[JttL's SDL Audio plugin] Timer subsystem initialized.\n");
        init_timer = TRUE;
    }

    // Close the audio device
    SDL_PauseAudio(1);
    SDL_CloseAudio();

    // Prototype of our callback function
    void my_audio_callback(void *userdata, Uint8 *stream, int len);

    // Open the audio device
    SDL_AudioSpec *desired, *obtained;

    // Allocate a desired SDL_AudioSpec
    desired = malloc(sizeof(SDL_AudioSpec));

    // Allocate space for the obtained SDL_AudioSpec
    obtained = malloc(sizeof(SDL_AudioSpec));

    // 22050Hz - FM Radio quality
    desired->freq=GameFreq;

    // Print out message for frequency
    printf("[JttL's SDL Audio plugin] Requesting frequency: %iHz.\n", desired->freq);

    // 16-bit signed audio
    desired->format=AUDIO_S16SYS;

    // Print out message for format
    printf("[JttL's SDL Audio plugin] Requesting format: %i.\n", desired->format);

    // Enable two hardware channels (for Stereo output)
    desired->channels=2;

    // Large audio buffer reduces risk of dropouts but increases response time
    desired->samples=SecondaryBufferSize;

    // Our callback function
    desired->callback=my_audio_callback;
    desired->userdata=NULL;

    // Open the audio device
    if ( SDL_OpenAudio(desired, obtained) < 0 )
    {
        sdl_error[2] = SDL_GetError();
        fprintf(stderr, "[JttL's SDL Audio plugin] Error: Couldn't open audio device: %s\n", sdl_error[2]);
        open_audio_device = FALSE;
    }
    else 
    {
        open_audio_device = TRUE;
    }

    // Check to see if we have the audio format we requested.
    if(desired->format != obtained->format)
    {
        fprintf(stderr, "[JttL's SDL Audio plugin] Error: Obtained audio format differs from requested.\n");
        format_match = FALSE;
    }
    else
    {
        format_match = TRUE;
    }

    // Check to see if we have the frequency we requested.
    if(desired->freq != obtained->freq)
    {
        fprintf(stderr, "[JttL's SDL Audio plugin] Error: Obtained frequency differs from requested.\n");
        freq_match = FALSE;
    }
    else 
    {
        freq_match = TRUE;
    }

    // Free no longer needed objects used for testing the specifications.
    free(desired);
    free(obtained);

    // Uninitialize SDL audio, as it is no longer needed.
    SDL_PauseAudio(1);
    SDL_CloseAudio();

    // Quit the Audio and Timer subsystems if they are enabled. (They should be, unless something went horribly wrong.)
    if(SDL_WasInit(SDL_INIT_AUDIO) != 0) SDL_QuitSubSystem(SDL_INIT_AUDIO);
    if(SDL_WasInit(SDL_INIT_TIMER) != 0) SDL_QuitSubSystem(SDL_INIT_TIMER);

    char tMsg[1024];

    if((init_audio == TRUE) && ( init_timer == TRUE ) && ( open_audio_device == TRUE ) && (format_match == TRUE) && (freq_match == TRUE)) 
    {
        sprintf(tMsg,"[JttL's SDL Audio plugin] Audio test successful.");
        critical_failure = 0;
    }
    else 
    {
        sprintf(tMsg,"[JttL's SDL Audio plugin] Test Results\n--\n");
        if(init_audio != TRUE)
        {
            sprintf(tMsg, "%sError initalizing SDL Audio:\n - %s\n", tMsg,sdl_error[0]);
        }
        if(init_timer != TRUE)
        {
            sprintf(tMsg, "%sError initalizing SDL Timer:\n - %s\n", tMsg,sdl_error[1]);
        }
        if(open_audio_device != TRUE)
        {
            sprintf(tMsg, "%sError opening audio device:\n - %s\n", tMsg,sdl_error[2]);
        }
        if(format_match != TRUE)
        {
            sprintf(tMsg, "%sUnable to get the requested output audio format.\n", tMsg);
        }
        if(freq_match != TRUE)
        {
            sprintf(tMsg, "%sUnable to get the requested output frequency.\n", tMsg);
        }
        critical_failure = 1;
    }

    display_test(tMsg);
}

EXPORT void CALL GetDllInfo( PLUGIN_INFO * PluginInfo )
{
    PluginInfo->Version = 0x0101;
    PluginInfo->Type    = PLUGIN_TYPE_AUDIO;
    sprintf(PluginInfo->Name,"JttL's SDL Audio %s", PLUGIN_VERSION);
    PluginInfo->NormalMemory  = TRUE;
    PluginInfo->MemoryBswaped = TRUE;
}

EXPORT BOOL CALL InitiateAudio( AUDIO_INFO Audio_Info )
{
    AudioInfo = Audio_Info;
    return TRUE;
}
#ifdef DEBUG
static int underrun_count = 0;
#endif

#ifdef USE_SRC
float *_src = 0;
unsigned int _src_len = 0;
float *_dest = 0;
unsigned int _dest_len = 0;
int error;
SRC_STATE *src_state;
SRC_DATA src_data;
#endif

static int resample(Uint8 *input, int input_avail, int oldsamplerate, Uint8 *output, int output_needed, int newsamplerate)
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

void my_audio_callback(void *userdata, Uint8 *stream, int len)
{
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
#ifdef DEBUG
        underrun_count++;
        fprintf(stderr, "[JttL's SDL Audio plugin] Debug: Audio buffer underrun (%i).\n",underrun_count);
#endif
        memset(stream , 0, len);
        buffer_pos = 0;
    }
}
EXPORT void CALL RomOpen()
{
    /* This function is for compatibility with Mupen64. */
    ReadConfig();
    InitializeAudio( GameFreq );
}
void InitializeSDL()
{
    ReadConfig();
#ifdef DEBUG
    printf("[JttL's SDL Audio plugin] Sound plugin started in debug mode.\n");
#endif
    printf("[JttL's SDL Audio plugin] version %s initalizing.\n", MUPEN_VERSION);
    printf("[JttL's SDL Audio plugin] Initializing SDL audio subsystem...\n");
#ifdef DEBUG
    printf("[JttL's SDL Audio plugin] Debug: Primary buffer: %i bytes.\n", PrimaryBufferSize);
    printf("[JttL's SDL Audio plugin] Debug: Secondary buffer: %i bytes.\n", SecondaryBufferSize * 4);
    printf("[JttL's SDL Audio plugin] Debug: Low buffer level: %i bytes.\n", LowBufferLoadLevel);
    printf("[JttL's SDL Audio plugin] Debug: High buffer level: %i bytes.\n", HighBufferLoadLevel);
#endif
    if(SDL_Init(SDL_INIT_AUDIO | SDL_INIT_TIMER) < 0)
    {
        fprintf(stderr, "[JttL's SDL Audio plugin] Error: Failed to initialize SDL audio subsystem.\n[JttL's SDL Audio plugin] Error: Forcing exit.\n");
        critical_failure = 1;
        return;
    }
    critical_failure = 0;

}
void InitializeAudio(int freq)
{
    if(SDL_WasInit(SDL_INIT_AUDIO|SDL_INIT_TIMER) == (SDL_INIT_AUDIO|SDL_INIT_TIMER) ) 
    {
#ifdef DEBUG
        printf("[JttL's SDL Audio plugin] Debug: Audio and timer allready initialized.\n");
#endif
    }
    else 
    {
#ifdef DEBUG
        printf("[JttL's SDL Audio plugin] Debug: Audio and timer not yet initialized. Initializing...\n");
#endif
        InitializeSDL();
    }
    if (critical_failure == 1)
        return;
    GameFreq = freq; // This is important for the sync
    if(hardware_spec != NULL) free(hardware_spec);
    SDL_PauseAudio(1);
    SDL_CloseAudio();

    // Prototype of our callback function
    void my_audio_callback(void *userdata, Uint8 *stream, int len);

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
    
#ifdef DEBUG
    printf("[JttL's SDL Audio plugin] Debug: Requesting frequency: %iHz.\n", desired->freq);
#endif
    /* 16-bit signed audio */
    desired->format=AUDIO_S16SYS;
#ifdef DEBUG
    printf("[JttL's SDL Audio plugin] Debug: Requesting format: %i.\n", desired->format);
#endif
    /* Stereo */
    desired->channels=2;
    /* Large audio buffer reduces risk of dropouts but increases response time */
    desired->samples=SecondaryBufferSize;

    /* Our callback function */
    desired->callback=my_audio_callback;
    desired->userdata=NULL;

    if(buffer == NULL)
    {
        printf("[JttL's SDL Audio plugin] Allocating memory for audio buffer: %i bytes.\n", (int) (PrimaryBufferSize*sizeof(Uint8)));
        buffer = (Uint8*) malloc(PrimaryBufferSize);
    }

    if (mixBuffer == NULL)
    {
        //this should be the size of the SDL audio buffer
        mixBuffer = (Uint8*) malloc(SecondaryBufferSize * 4);
    }

    memset(buffer, 0, PrimaryBufferSize * sizeof(Uint8));

    /* Open the audio device */
    if ( SDL_OpenAudio(desired, obtained) < 0 )
    {
        fprintf(stderr, "[JttL's SDL Audio plugin] Error: Couldn't open audio: %s\n", SDL_GetError());
        critical_failure = 1;
        return;
    }
    /* desired spec is no longer needed */

    if(desired->format != obtained->format)
    {
        fprintf(stderr, "[JttL's SDL Audio plugin] Error: Obtained audio format differs from requested.\n");
    }
    if(desired->freq != obtained->freq)
    {
        fprintf(stderr, "[JttL's SDL Audio plugin] Error: Obtained frequency differs from requested.\n");
    }
    free(desired);
    hardware_spec=obtained;

#ifdef DEBUG
    printf("[JttL's SDL Audio plugin] Debug: Frequency: %i\n", hardware_spec->freq);
    printf("[JttL's SDL Audio plugin] Debug: Format: %i\n", hardware_spec->format);
    printf("[JttL's SDL Audio plugin] Debug: Channels: %i\n", hardware_spec->channels);
    printf("[JttL's SDL Audio plugin] Debug: Silence: %i\n", hardware_spec->silence);
    printf("[JttL's SDL Audio plugin] Debug: Samples: %i\n", hardware_spec->samples);
    printf("[JttL's SDL Audio plugin] Debug: Size: %i\n", hardware_spec->size);
#endif
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
   if (critical_failure == 1)
       return;
    printf("[JttL's SDL Audio plugin] Cleaning up SDL sound plugin...\n");
    
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

EXPORT void CALL SetConfigDir(char* configDir)
{
    strncpy(configdir, configDir, PATH_MAX);
}

EXPORT void CALL ProcessAList()
{
}

EXPORT void CALL SetSpeedFactor(int percentage)
{
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
        fprintf(stderr, "[JttL's SDL Audio plugin] Error: Cannot open config file for saving.\n");
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
        fprintf(stderr, "[JttL's SDL Audio plugin] Error: Cannot open config file.\n");
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
#ifdef DEBUG
            printf("[JttL's SDL Audio plugin] Debug: Parameter \"%s\", value: \"%i\"\n", &param, atoi(&value[1]));
#endif
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

EXPORT void CALL VolumeUp(void)
{
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


EXPORT void CALL VolumeMute(void)
{
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

