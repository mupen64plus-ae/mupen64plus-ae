/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus-audio-sles - main.c                                       *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
 *   Copyright (C) 2015 Gilles Siberlin                                    *
 *   Copyright (C) 2007-2009 Richard Goedeken                              *
 *   Copyright (C) 2007-2008 Ebenblues                                     *
 *   Copyright (C) 2003 JttL                                               *
 *   Copyright (C) 2002 Hacktarux                                          *
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

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <errno.h>
#include <math.h>
#include <SoundTouch.h>

#define M64P_PLUGIN_PROTOTYPES 1
#include "m64p_common.h"
#include "m64p_config.h"
#include "m64p_plugin.h"
#include "m64p_types.h"
#include "m64p_frontend.h"
#include "main.h"
#include "osal_dynamiclib.h"
#include "threadqueue.h"
#include <jni.h>

#include <SLES/OpenSLES_Android.h>

typedef struct threadLock_
{
  volatile int value;
  volatile int limit;
} threadLock;

/* Default start-time size of primary buffer (in equivalent output samples).
   This is the buffer where audio is loaded after it's extracted from n64's memory. */
#define PRIMARY_BUFFER_SIZE 16384

/* Size of a single secondary buffer, in output samples. This is the requested size of OpenSLES's
   hardware buffer, this should be a power of two. */
#define SECONDARY_BUFFER_SIZE 256

/* This sets default frequency what is used if rom doesn't want to change it.
   Probably only game that needs this is Zelda: Ocarina Of Time Master Quest
   *NOTICE* We should try to find out why Demos' frequencies are always wrong
   They tend to rely on a default frequency, apparently, never the same one ;) */
#define DEFAULT_FREQUENCY 33600

/* This is the requested number of OpenSLES's hardware buffers */
#define SECONDARY_BUFFER_NBR 100

/* number of bytes per sample */
#define N64_SAMPLE_BYTES 4

#ifdef FP_ENABLED
#define SLES_SAMPLE_BYTES 8
#else
#define SLES_SAMPLE_BYTES 4
#endif

/* local variables */
static void (*l_DebugCallback)(void *, int, const char *) = NULL;
static void *l_DebugCallContext = NULL;
static int l_PluginInit = 0;
static m64p_handle l_ConfigAudio;

/* Read header for type definition */
static AUDIO_INFO AudioInfo;
/* Pointer to the primary audio buffer */
static unsigned char *primaryBuffer = NULL;
/* Size of the primary buffer */
static int primaryBufferBytes = 0;
/* Size of the primary audio buffer in equivalent output samples */
static unsigned int PrimaryBufferSize = PRIMARY_BUFFER_SIZE;
/* Pointer to secondary buffers */
static unsigned char ** secondaryBuffers = NULL;
/* Size of a single secondary buffer */
static int secondaryBufferBytes = 0;
/* Size of a single secondary audio buffer in output samples */
static unsigned int SecondaryBufferSize = SECONDARY_BUFFER_SIZE;
/* Index of the next secondary buffer available */
static int secondaryBufferIndex = 0;
/* Number of secondary buffers */
static unsigned int SecondaryBufferNbr = SECONDARY_BUFFER_NBR;
/* Audio frequency, this is usually obtained from the game, but for compatibility we set default value */
static int GameFreq = DEFAULT_FREQUENCY;
/* SpeedFactor is used to increase/decrease game playback speed */
static unsigned int speed_factor = 100;
/* If this is true then left and right channels are swapped */
static int SwapChannels = 0;
/* Number of secondary buffers to target */
static int TargetSecondaryBuffers = 20;
/* Selected samplin rate */
static int SamplingRateSelection = 0;
/* Output Audio frequency */
static int OutputFreq;
/* Indicate that the audio plugin failed to initialize, so the emulator can keep running without sound */
static int critical_failure = 0;

typedef struct queueData_
{
   unsigned char* data;
   unsigned int lenght;
} queueData;

void processAudio(const unsigned char* buffer, unsigned int length);
static void* audioConsumer(void*);
static pthread_t audioConsumerThread;
static struct threadqueue audioConsumerQueue;

static volatile bool shutdown = true;

using namespace soundtouch;
static SoundTouch soundTouch;

/* Thread Lock */
threadLock lock;

/* Engine interfaces */
SLObjectItf engineObject = NULL;
SLEngineItf engineEngine = NULL;

/* Output mix interfaces */
SLObjectItf outputMixObject = NULL;

/* Player interfaces */
SLObjectItf playerObject = NULL;
SLPlayItf playerPlay = NULL;

/* Buffer queue interfaces */
SLAndroidSimpleBufferQueueItf bufferQueue = NULL;

/* Definitions of pointers to Core config functions */
ptr_ConfigOpenSection      ConfigOpenSection = NULL;
ptr_ConfigDeleteSection    ConfigDeleteSection = NULL;
ptr_ConfigSaveSection      ConfigSaveSection = NULL;
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
ptr_CoreDoCommand          CoreDoCommand = NULL;

/* Global functions */
static void DebugMessage(int level, const char *message, ...)
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

void queueCallback(SLAndroidSimpleBufferQueueItf caller, void *context);

static void CloseAudio(void)
{
    if(!shutdown)
    {
       shutdown = true;
       pthread_join(audioConsumerThread,NULL);

       thread_queue_cleanup(&audioConsumerQueue, 1);
    }

    int i = 0;
    
    secondaryBufferIndex = 0;
    
    /* Delete Primary buffer */
    if (primaryBuffer != NULL)
    {
        primaryBufferBytes = 0;
        free(primaryBuffer);
        primaryBuffer = NULL;
    }

    /* Delete Secondary buffers */
    if (secondaryBuffers != NULL)
    {
        for(i=0;i<SecondaryBufferNbr;i++)
        {
            if (secondaryBuffers[i] != NULL)
            {
                free(secondaryBuffers[i]);
                secondaryBuffers[i] = NULL;
            }
        }
        secondaryBufferBytes = 0;
        free(secondaryBuffers);
        secondaryBuffers = NULL;
    }

    /* Destroy buffer queue audio player object, and invalidate all associated interfaces */
    if (playerObject != NULL)
    {
        SLuint32 state = SL_PLAYSTATE_PLAYING;
        (*playerPlay)->SetPlayState(playerPlay, SL_PLAYSTATE_STOPPED);
    
        while(state != SL_PLAYSTATE_STOPPED)
            (*playerPlay)->GetPlayState(playerPlay, &state);
    
        (*playerObject)->Destroy(playerObject);
        playerObject = NULL;
        playerPlay = NULL;
        bufferQueue = NULL;
    }
    
    /* Destroy output mix object, and invalidate all associated interfaces */
    if (outputMixObject != NULL)
    {
        (*outputMixObject)->Destroy(outputMixObject);
        outputMixObject = NULL;
    }
    
    /* Destroy engine object, and invalidate all associated interfaces */
    if (engineObject != NULL)
    {
        (*engineObject)->Destroy(engineObject);
        engineObject = NULL;
        engineEngine = NULL;
    }
}

static int CreatePrimaryBuffer(void)
{
    unsigned int primaryBytes = (unsigned int) (PrimaryBufferSize * N64_SAMPLE_BYTES);

    DebugMessage(M64MSG_VERBOSE, "Allocating memory for primary audio buffer: %i bytes.", primaryBytes);

    primaryBuffer = (unsigned char*) malloc(primaryBytes);

    if (primaryBuffer == NULL)
        return 0;

    memset(primaryBuffer, 0, primaryBytes);
    primaryBufferBytes = primaryBytes;

    return 1;
}

static int CreateSecondaryBuffers(void)
{
    int i = 0;
    int status = 1;
    unsigned int secondaryBytes = (unsigned int) (SecondaryBufferSize * SLES_SAMPLE_BYTES);

    DebugMessage(M64MSG_VERBOSE, "Allocating memory for %d secondary audio buffers: %i bytes.", SecondaryBufferNbr, secondaryBytes);

    /* Allocate number of secondary buffers */
    secondaryBuffers = (unsigned char**) malloc(sizeof(char*) * SecondaryBufferNbr);

    if (secondaryBuffers == NULL)
        return 0;

    /* Allocate size of each secondary buffers */
    for(i=0;i<SecondaryBufferNbr;i++)
    {
        secondaryBuffers[i] = (unsigned char*) malloc(secondaryBytes);

        if (secondaryBuffers[i] == NULL)
        {
            status = 0;
            break;
        }

        memset(secondaryBuffers[i], 0, secondaryBytes);
    }

    secondaryBufferBytes = secondaryBytes;

    return status;
}

void OnInitFailure(void)
{
   DebugMessage(M64MSG_ERROR, "Couldn't open OpenSLES audio");
   CloseAudio();
   critical_failure = 1;
}

static void InitializeAudio(int freq)
{

   /* reload these because they gets re-assigned from data below, and InitializeAudio can be called more than once */
   PrimaryBufferSize = ConfigGetParamInt(l_ConfigAudio, "PRIMARY_BUFFER_SIZE");
   SecondaryBufferSize = ConfigGetParamInt(l_ConfigAudio, "SECONDARY_BUFFER_SIZE");
   TargetSecondaryBuffers = ConfigGetParamInt(l_ConfigAudio, "SECONDARY_BUFFER_NBR");
   SamplingRateSelection = ConfigGetParamInt(l_ConfigAudio, "SAMPLING_RATE");

    SLuint32 sample_rate;

    /* Sometimes a bad frequency is requested so ignore it */
    if (freq < 4000)
        return;

    if (critical_failure)
        return;

    /* This is important for the sync */
    GameFreq = freq;

    if(SamplingRateSelection == 0)
    {
       if((freq/1000) <= 11)
       {
           OutputFreq = 11025;
           sample_rate = SL_SAMPLINGRATE_11_025;
       }
       else if((freq/1000) <= 22)
       {
           OutputFreq = 22050;
           sample_rate = SL_SAMPLINGRATE_22_05;
       }
       else if((freq/1000) <= 32)
       {
           OutputFreq = 32000;
           sample_rate = SL_SAMPLINGRATE_32;
       }
       else
       {
           OutputFreq = 44100;
           sample_rate = SL_SAMPLINGRATE_44_1;
       }
    }
    else
    {
       switch(SamplingRateSelection)
       {
          case 16:
             OutputFreq = 16000;
             sample_rate = SL_SAMPLINGRATE_16;
             break;
          case 24:
             OutputFreq = 24000;
             sample_rate = SL_SAMPLINGRATE_24;
             break;
          case 32:
             OutputFreq = 32000;
             sample_rate = SL_SAMPLINGRATE_32;
             break;
          case 441:
             OutputFreq = 44100;
             sample_rate = SL_SAMPLINGRATE_44_1;
             break;
          case 48:
             OutputFreq = 48000;
             sample_rate = SL_SAMPLINGRATE_48;
             break;
       }
    }

    DebugMessage(M64MSG_VERBOSE, "Requesting frequency: %iHz.", OutputFreq);
    
    double bufferMultiplier = (double)OutputFreq/DEFAULT_FREQUENCY;
    SecondaryBufferSize = bufferMultiplier*(double)SecondaryBufferSize;

    /* Close everything because InitializeAudio can be called more than once */
    CloseAudio();

    /* Create primary buffer */
    if(!CreatePrimaryBuffer())
    {
       OnInitFailure();
       return;
    }

    /* Create secondary buffers */
    if(!CreateSecondaryBuffers())
    {
       OnInitFailure();
       return;
    }

    lock.value = lock.limit = SecondaryBufferNbr;

    /* Engine object */
    SLresult result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    if(result != SL_RESULT_SUCCESS)
    {
       OnInitFailure();
       return;
    }

    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    if(result != SL_RESULT_SUCCESS)
    {
       OnInitFailure();
       return;
    }

    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
    if(result != SL_RESULT_SUCCESS)
    {
       OnInitFailure();
       return;
    }

    /* Output mix object */
    result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 0, NULL, NULL);
    if(result != SL_RESULT_SUCCESS)
    {
       OnInitFailure();
       return;
    }

    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    if(result != SL_RESULT_SUCCESS)
    {
       OnInitFailure();
       return;
    }

    SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, SecondaryBufferNbr};

#ifdef FP_ENABLED

    SLAndroidDataFormat_PCM_EX format_pcm = {SL_ANDROID_DATAFORMAT_PCM_EX, 2, sample_rate,
                   32, 32, SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,
                   SL_BYTEORDER_LITTLEENDIAN, SL_ANDROID_PCM_REPRESENTATION_FLOAT};
#else
    SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM,2, sample_rate,
                   SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
                   (SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT), SL_BYTEORDER_LITTLEENDIAN};
#endif

    SLDataSource audioSrc = {&loc_bufq, &format_pcm};

    /* Configure audio sink */
    SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};
    SLDataSink audioSnk = {&loc_outmix, NULL};

    /* Create audio player */
    const SLInterfaceID ids1[] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
    const SLboolean req1[] = {SL_BOOLEAN_TRUE};
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &(playerObject), &audioSrc, &audioSnk, 1, ids1, req1);
    if(result != SL_RESULT_SUCCESS)
    {
       OnInitFailure();
       return;
    }

    /* Realize the player */
    result = (*playerObject)->Realize(playerObject, SL_BOOLEAN_FALSE);
    if(result != SL_RESULT_SUCCESS)
    {
       OnInitFailure();
       return;
    }

    /* Get the play interface */
    result = (*playerObject)->GetInterface(playerObject, SL_IID_PLAY, &(playerPlay));
    if(result != SL_RESULT_SUCCESS)
    {
       OnInitFailure();
       return;
    }

    /* Get the buffer queue interface */
    result = (*playerObject)->GetInterface(playerObject, SL_IID_ANDROIDSIMPLEBUFFERQUEUE, &(bufferQueue));
    if(result != SL_RESULT_SUCCESS)
    {
       OnInitFailure();
       return;
    }

    /* register callback on the buffer queue */
    result = (*bufferQueue)->RegisterCallback(bufferQueue, queueCallback, &lock);
    if(result != SL_RESULT_SUCCESS)
    {
       OnInitFailure();
       return;
    }

    /* set the player's state to playing */
    result = (*playerPlay)->SetPlayState(playerPlay, SL_PLAYSTATE_PLAYING);
    if(result != SL_RESULT_SUCCESS)
    {
       OnInitFailure();
       return;
    }

    thread_queue_init(&audioConsumerQueue);
    shutdown = false;
    pthread_create( &audioConsumerThread, NULL, audioConsumer, NULL);

    return;
}

static void ReadConfig(void)
{
    /* read the configuration values into our static variables */
    GameFreq = ConfigGetParamInt(l_ConfigAudio, "DEFAULT_FREQUENCY");
    SwapChannels = ConfigGetParamBool(l_ConfigAudio, "SWAP_CHANNELS");
    PrimaryBufferSize = ConfigGetParamInt(l_ConfigAudio, "PRIMARY_BUFFER_SIZE");
    SecondaryBufferSize = ConfigGetParamInt(l_ConfigAudio, "SECONDARY_BUFFER_SIZE");
    TargetSecondaryBuffers = ConfigGetParamInt(l_ConfigAudio, "SECONDARY_BUFFER_NBR");
    SamplingRateSelection = ConfigGetParamInt(l_ConfigAudio, "SAMPLING_RATE");
}

/* Mupen64Plus plugin functions */
EXPORT m64p_error CALL PluginStartup(m64p_dynlib_handle CoreLibHandle, void *Context,
                                   void (*DebugCallback)(void *, int, const char *))
{
    ptr_CoreGetAPIVersions CoreAPIVersionFunc;
    
    int ConfigAPIVersion, DebugAPIVersion, VidextAPIVersion, bSaveConfig;
    float fConfigParamsVersion = 0.0f;
    
    if (l_PluginInit)
        return M64ERR_ALREADY_INIT;

    /* first thing is to set the callback function for debug info */
    l_DebugCallback = DebugCallback;
    l_DebugCallContext = Context;

    /* attach and call the CoreGetAPIVersions function, check Config API version for compatibility */
    CoreAPIVersionFunc = (ptr_CoreGetAPIVersions) osal_dynlib_getproc(CoreLibHandle, "CoreGetAPIVersions");
    if (CoreAPIVersionFunc == NULL)
    {
        DebugMessage(M64MSG_ERROR, "Core emulator broken; no CoreAPIVersionFunc() function found.");
        return M64ERR_INCOMPATIBLE;
    }
    
    (*CoreAPIVersionFunc)(&ConfigAPIVersion, &DebugAPIVersion, &VidextAPIVersion, NULL);
    if ((ConfigAPIVersion & 0xffff0000) != (CONFIG_API_VERSION & 0xffff0000))
    {
        DebugMessage(M64MSG_ERROR, "Emulator core Config API (v%i.%i.%i) incompatible with plugin (v%i.%i.%i)",
                VERSION_PRINTF_SPLIT(ConfigAPIVersion), VERSION_PRINTF_SPLIT(CONFIG_API_VERSION));
        return M64ERR_INCOMPATIBLE;
    }

    /* Get the core config function pointers from the library handle */
    ConfigOpenSection = (ptr_ConfigOpenSection) osal_dynlib_getproc(CoreLibHandle, "ConfigOpenSection");
    ConfigDeleteSection = (ptr_ConfigDeleteSection) osal_dynlib_getproc(CoreLibHandle, "ConfigDeleteSection");
    ConfigSaveSection = (ptr_ConfigSaveSection) osal_dynlib_getproc(CoreLibHandle, "ConfigSaveSection");
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
    CoreDoCommand =  (ptr_CoreDoCommand) osal_dynlib_getproc(CoreLibHandle, "CoreDoCommand");

    if (!ConfigOpenSection || !ConfigDeleteSection || !ConfigSetParameter || !ConfigGetParameter ||
        !ConfigSetDefaultInt || !ConfigSetDefaultFloat || !ConfigSetDefaultBool || !ConfigSetDefaultString ||
        !ConfigGetParamInt   || !ConfigGetParamFloat   || !ConfigGetParamBool   || !ConfigGetParamString ||
        !CoreDoCommand)
        return M64ERR_INCOMPATIBLE;

    /* ConfigSaveSection was added in Config API v2.1.0 */
    if (ConfigAPIVersion >= 0x020100 && !ConfigSaveSection)
        return M64ERR_INCOMPATIBLE;

    /* get a configuration section handle */
    if (ConfigOpenSection("Audio-OpenSLES", &l_ConfigAudio) != M64ERR_SUCCESS)
    {
        DebugMessage(M64MSG_ERROR, "Couldn't open config section 'Audio-OpenSLES'");
        return M64ERR_INPUT_NOT_FOUND;
    }

    /* check the section version number */
    bSaveConfig = 0;
    if (ConfigGetParameter(l_ConfigAudio, "Version", M64TYPE_FLOAT, &fConfigParamsVersion, sizeof(float)) != M64ERR_SUCCESS)
    {
        DebugMessage(M64MSG_WARNING, "No version number in 'Audio-OpenSLES' config section. Setting defaults.");
        ConfigDeleteSection("Audio-OpenSLES");
        ConfigOpenSection("Audio-OpenSLES", &l_ConfigAudio);
        bSaveConfig = 1;
    }
    else if (((int) fConfigParamsVersion) != ((int) CONFIG_PARAM_VERSION))
    {
        DebugMessage(M64MSG_WARNING, "Incompatible version %.2f in 'Audio-OpenSLES' config section: current is %.2f. Setting defaults.", fConfigParamsVersion, (float) CONFIG_PARAM_VERSION);
        ConfigDeleteSection("Audio-OpenSLES");
        ConfigOpenSection("Audio-OpenSLES", &l_ConfigAudio);
        bSaveConfig = 1;
    }
    else if ((CONFIG_PARAM_VERSION - fConfigParamsVersion) >= 0.0001f)
    {
        /* handle upgrades */
        float fVersion = CONFIG_PARAM_VERSION;
        ConfigSetParameter(l_ConfigAudio, "Version", M64TYPE_FLOAT, &fVersion);
        DebugMessage(M64MSG_INFO, "Updating parameter set version in 'Audio-OpenSLES' config section to %.2f", fVersion);
        bSaveConfig = 1;
    }

    /* set the default values for this plugin */
    ConfigSetDefaultFloat(l_ConfigAudio, "Version",             CONFIG_PARAM_VERSION,  "Mupen64Plus SDL Audio Plugin config parameter version number");
    ConfigSetDefaultInt(l_ConfigAudio, "DEFAULT_FREQUENCY",     DEFAULT_FREQUENCY,     "Frequency which is used if rom doesn't want to change it");
    ConfigSetDefaultBool(l_ConfigAudio, "SWAP_CHANNELS",        0,                     "Swaps left and right channels");
    ConfigSetDefaultInt(l_ConfigAudio, "PRIMARY_BUFFER_SIZE",   PRIMARY_BUFFER_SIZE,   "Size of primary buffer in output samples. This is where audio is loaded after it's extracted from n64's memory.");
    ConfigSetDefaultInt(l_ConfigAudio, "SECONDARY_BUFFER_SIZE", SECONDARY_BUFFER_SIZE, "Size of secondary buffer in output samples. This is OpenSLES's hardware buffer.");
    ConfigSetDefaultInt(l_ConfigAudio, "SECONDARY_BUFFER_NBR" , SECONDARY_BUFFER_NBR,  "Number of secondary buffers.");
    ConfigSetDefaultInt(l_ConfigAudio, "SAMPLING_RATE" ,        0,                     "Sampling rate, (0=game original, 16, 24, 32, 441, 48");

    if (bSaveConfig && ConfigAPIVersion >= 0x020100)
        ConfigSaveSection("Audio-OpenSLES");

    l_PluginInit = 1;

    return M64ERR_SUCCESS;
}

EXPORT m64p_error CALL PluginShutdown(void)
{
    if (!l_PluginInit)
        return M64ERR_NOT_INIT;

    CloseAudio();

    /* reset some local variables */
    l_DebugCallback = NULL;
    l_DebugCallContext = NULL;
    l_PluginInit = 0;

    return M64ERR_SUCCESS;
}

EXPORT m64p_error CALL PluginGetVersion(m64p_plugin_type *PluginType, int *PluginVersion, int *APIVersion, const char **PluginNamePtr, int *Capabilities)
{
    /* set version info */
    if (PluginType != NULL)
        *PluginType = M64PLUGIN_AUDIO;

    if (PluginVersion != NULL)
        *PluginVersion = OPENSLES_AUDIO_PLUGIN_VERSION;

    if (APIVersion != NULL)
        *APIVersion = AUDIO_PLUGIN_API_VERSION;
    
    if (PluginNamePtr != NULL)
        *PluginNamePtr = "Mupen64Plus OpenSLES Audio Plugin";

    if (Capabilities != NULL)
    {
        *Capabilities = 0;
    }
                    
    return M64ERR_SUCCESS;
}

/* ----------- Audio Functions ------------- */
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

bool isSpeedLimiterEnabled(void)
{
   int e = 1;
   CoreDoCommand(M64CMD_CORE_STATE_QUERY, M64CORE_SPEED_LIMITER, &e);
   return  e;
}

EXPORT void CALL AiLenChanged(void)
{
    static const bool sleepPerfFixEnabled = false;
    static const double minSleepNeeded = -0.05;
    static const double maxSleepNeeded = 0.05;
    static bool resetOnce = false;
    static unsigned long totalElapsedSamples = 0;
    static double gameStartTime = 0;
    static int lastSpeedFactor = 100;

    if (critical_failure == 1)
        return;

    if (!l_PluginInit)
        return;

    bool limiterEnabled = isSpeedLimiterEnabled();
    
    timespec time;
    clock_gettime(CLOCK_REALTIME, &time);
    double timeDouble = static_cast<double>(time.tv_sec) +
          static_cast<double>(time.tv_nsec)/1.0e9;

    //if this is the first time or we are resuming from pause
    if(gameStartTime == 0 || !resetOnce || lastSpeedFactor != speed_factor)
    {
       gameStartTime = timeDouble;
       totalElapsedSamples = 0;
       resetOnce = true;
       totalElapsedSamples = 0;
    }

    lastSpeedFactor = speed_factor;

    unsigned int LenReg = *AudioInfo.AI_LEN_REG;
    unsigned char * p = AudioInfo.RDRAM + (*AudioInfo.AI_DRAM_ADDR_REG & 0xFFFFFF);
    
    queueData* theQueueData = (queueData*)malloc(sizeof(queueData));
    theQueueData->data = (unsigned char*)malloc(LenReg);
    theQueueData->lenght = LenReg;

    memcpy(theQueueData->data, p, LenReg);

    thread_queue_add(&audioConsumerQueue, theQueueData, 0);

    //Calculate total ellapsed game time
    totalElapsedSamples += LenReg/N64_SAMPLE_BYTES;
    double speedFactor = static_cast<double>(speed_factor)/100.0;
    double totalElapsedGameTime = ((double)totalElapsedSamples)/(double)GameFreq/speedFactor;

    //Slow the game down if sync game to audio is enabled
    if(!limiterEnabled)
    {
       double totalRealTimeElapsed = timeDouble - gameStartTime;
       double sleepNeeded = totalElapsedGameTime - totalRealTimeElapsed;

       if(sleepNeeded < minSleepNeeded || sleepNeeded > maxSleepNeeded)
       {
          resetOnce = false;
       }

       //Useful logging
       //DebugMessage(M64MSG_ERROR, "Real=%f, Game=%f, sleep=%f, start=%f, time=%f, speed=%d, sleep_before_factor=%f",
       //             totalRealTimeElapsed, totalElapsedGameTime, sleepNeeded, gameStartTime, timeDouble, speed_factor, sleepNeeded*speedFactor);

       if(sleepNeeded > 0.0 && sleepNeeded < maxSleepNeeded)
       {
          if(sleepPerfFixEnabled)
          {
             double endTime = timeDouble + sleepNeeded;

             timespec time;
             clock_gettime(CLOCK_REALTIME, &time);
             double currTime = static_cast<double>(time.tv_sec) +
                   static_cast<double>(time.tv_nsec)/1.0e9;
             while(currTime < endTime)
             {
                clock_gettime(CLOCK_REALTIME, &time);
                currTime = static_cast<double>(time.tv_sec) +
                      static_cast<double>(time.tv_nsec)/1.0e9;
             }
          }
          else
          {
             timespec sleepTime;
             sleepTime.tv_sec = static_cast<time_t>(sleepNeeded);
             sleepTime.tv_nsec = (sleepNeeded - sleepTime.tv_sec)*1e9;
             nanosleep(&sleepTime, NULL );
          }
       }
    }
}

double TimeDiff(struct timespec* currTime, struct timespec* prevTime)
{
   return ((double)currTime->tv_sec+((double)currTime->tv_nsec)/1.0e9) -
         ((double)prevTime->tv_sec+((double)prevTime->tv_nsec)/1.0e9);
}

float GetAverageTime( float* feedTimes, int numTimes)
{
   float sum = 0;
   for(int index = 0; index < numTimes; ++index)
   {
      sum += feedTimes[index];
   }

   return sum/(float)numTimes;
}


void* audioConsumer(void* param)
{
   /*
   static int sequenceLenMS = 63;
   static int seekWindowMS = 16;
   static int overlapMS = 7;*/

   soundTouch.setSampleRate(GameFreq);
   soundTouch.setChannels(2);
   soundTouch.setSetting( SETTING_USE_QUICKSEEK, 1 );
   soundTouch.setSetting( SETTING_USE_AA_FILTER, 1 );
   //soundTouch.setSetting( SETTING_SEQUENCE_MS, sequenceLenMS );
   //soundTouch.setSetting( SETTING_SEEKWINDOW_MS, seekWindowMS );
   //soundTouch.setSetting( SETTING_OVERLAP_MS, overlapMS );

   soundTouch.setRate((double)GameFreq/(double)OutputFreq);
   
   double bufferMultiplier = (double)OutputFreq/DEFAULT_FREQUENCY;

   int prevQueueSize = thread_queue_length(&audioConsumerQueue);
   int currQueueSize = prevQueueSize;
   int maxQueueSize = TargetSecondaryBuffers + 30.0*bufferMultiplier;
   int minQueueSize = (double)TargetSecondaryBuffers*bufferMultiplier;
   bool drainQueue = false;

   //Sound queue ran dry, device is running slow
   int ranDry = 0;

   //adjustment used when a device running too slow
   double slowAdjustment = 1.0;
   double currAdjustment = 1.0;

   //how quickly to return to original speed
   const double returnSpeed = 0.10;
   const double minSlowValue = 0.2;
   const double maxSlowValue = 3.0;
   //Adjust tempo in x% increments so it's more steady
   int increments = 4;
   const double catchUpOffset = increments*2/100.0;
   queueData* currQueueData = NULL;
   struct timespec currTime;
   struct timespec prevTime;

   //How long to wait for some data
   struct timespec waitTime;
   waitTime.tv_sec = 1;
   waitTime.tv_nsec = 0;

   //use the smallest of the two
   const int maxWindowSize = 10;
   int feedTimeWindowSize = fmin(TargetSecondaryBuffers, maxWindowSize);
   int feedTimeIndex = 0;
   bool feedTimesSet = false;
   float timePerBuffer = 1.0*SecondaryBufferSize/GameFreq;
   float feedTimes[feedTimeWindowSize];
   float gameTimes[feedTimeWindowSize];
   float averageGameTime = 0.01666;
   float averageFeedTime = 0.01666;

   while(!shutdown)
   {
      int slesQueueLength = lock.limit - lock.value;

      ranDry = slesQueueLength < minQueueSize;

      struct threadmsg msg;

      clock_gettime(CLOCK_REALTIME, &prevTime);
      int result = thread_queue_get(&audioConsumerQueue, &waitTime, &msg);

      if( result != ETIMEDOUT )
      {
         int threadQueueLength = thread_queue_length(&audioConsumerQueue);

         currQueueData = (queueData*)msg.data;
         int dataLength = currQueueData->lenght;

         float temp =  averageGameTime/averageFeedTime;

         //Game is running too fast speed up audio
         if((slesQueueLength > maxQueueSize || drainQueue) && !ranDry)
         {
            drainQueue = true;
            currAdjustment = temp + catchUpOffset;
         }
         //Device can't keep up with the game or we have too much in the queue after slowing it down
         else if(ranDry)
         {
            drainQueue = false;
            currAdjustment = temp - catchUpOffset/2.0;
         }
         else if(!ranDry && slesQueueLength < maxQueueSize)
         {
            currAdjustment = temp;
         }

         //Allow the tempo to slow quickly with no minimum value change, but restore original tempo more slowly.
         if( currAdjustment > minSlowValue && currAdjustment < maxSlowValue)
         {
            if(fabs(currAdjustment - 1.0) < fabs(slowAdjustment - 1.0))
            {
               if(currAdjustment - slowAdjustment > returnSpeed)
               {
                  slowAdjustment += returnSpeed;
               }
               else
               {
                  slowAdjustment = currAdjustment;
               }
            }
            else
            {
               slowAdjustment = currAdjustment;
            }

            //Adjust tempo in x% increments so it's more steady
            int temp2 = ((int)(slowAdjustment*100))/increments;
            temp2 *= increments;
            slowAdjustment = ((double)temp2)/100;

            soundTouch.setTempo(slowAdjustment);
         }

         processAudio(currQueueData->data, dataLength);

         free(currQueueData->data);
         free(currQueueData);

         //Useful logging
         //if(slesQueueLength == 0)
         //{
         //   DebugMessage(M64MSG_ERROR, "sles_length=%d, thread_length=%d, dry=%d, slow_adj=%f, curr_adj=%f, temp=%f, feed_time=%f, game_time=%f",
         //      slesQueueLength, threadQueueLength, ranDry, slowAdjustment, currAdjustment, temp, averageFeedTime, averageGameTime);
         //}

         //Calculate rates
         clock_gettime(CLOCK_REALTIME, &currTime);

         //Figure out how much to slow down by
         float timeDiff = TimeDiff(&currTime, &prevTime);

         //sometimes this ends up as less than 0, not sure how
         if(timeDiff > 0)
         {
            feedTimes[feedTimeIndex] = timeDiff;
         }

         averageFeedTime = GetAverageTime(feedTimes, feedTimesSet ? feedTimeWindowSize : (feedTimeIndex+1));

         gameTimes[feedTimeIndex] = (float)dataLength/(float)N64_SAMPLE_BYTES/(float)GameFreq;
         averageGameTime = GetAverageTime(gameTimes, feedTimesSet ? feedTimeWindowSize : (feedTimeIndex+1));

         ++feedTimeIndex;
         if(feedTimeIndex == feedTimeWindowSize)
         {
            feedTimeIndex = 0;
            feedTimesSet = true;
         }
      }
   }

   return 0;
}

/* This callback handler is called every time a buffer finishes playing */
void queueCallback(SLAndroidSimpleBufferQueueItf caller, void *context)
{
    threadLock *plock = (threadLock *) context;

    plock->value++;
}

void processAudio(const unsigned char* buffer, unsigned int length)
{
   if (length < primaryBufferBytes)
   {
       unsigned int i;

       for ( i = 0 ; i < length ; i += 4 )
       {
           if(SwapChannels == 0)
           {
               /* Left channel */
              primaryBuffer[ i ] = buffer[ i + 2 ];
              primaryBuffer[ i + 1 ] = buffer[ i + 3 ];

               /* Right channel */
              primaryBuffer[ i + 2 ] = buffer[ i ];
              primaryBuffer[ i + 3 ] = buffer[ i + 1 ];
           }
           else
           {
               /* Left channel */
              primaryBuffer[ i ] = buffer[ i ];
              primaryBuffer[ i + 1 ] = buffer[ i + 1 ];

               /* Right channel */
              primaryBuffer[ i + 2 ] = buffer[ i + 2 ];
              primaryBuffer[ i + 3 ] = buffer[ i + 3 ];
           }
       }
   }
   else
       DebugMessage(M64MSG_WARNING, "processAudio(): Audio primary buffer overflow.");

#ifdef FP_ENABLED
   int numSamples = length/sizeof(short);
   short* primaryBufferShort = (short*)primaryBuffer;
   float primaryBufferFloat[numSamples];

   for(int index = 0; index < numSamples; ++index)
   {
      primaryBufferFloat[index] = static_cast<float>(primaryBufferShort[index])/32767.0;
   }

   soundTouch.putSamples((SAMPLETYPE*)primaryBufferFloat, length/N64_SAMPLE_BYTES);

#else
   soundTouch.putSamples((SAMPLETYPE*)primaryBuffer, length/N64_SAMPLE_BYTES);
#endif

   int outSamples = 0;

   do
   {
      outSamples = soundTouch.receiveSamples((SAMPLETYPE*)secondaryBuffers[secondaryBufferIndex], SecondaryBufferSize);

      if(outSamples != 0 && lock.value > 0)
      {
         SLresult result = (*bufferQueue)->Enqueue(bufferQueue, secondaryBuffers[secondaryBufferIndex],
            outSamples*SLES_SAMPLE_BYTES);

         --lock.value;

         secondaryBufferIndex++;

         if(secondaryBufferIndex > (SecondaryBufferNbr-1))
            secondaryBufferIndex = 0;
      }
   }
   while (outSamples != 0);
}

EXPORT int CALL InitiateAudio( AUDIO_INFO Audio_Info )
{
    if (!l_PluginInit)
        return 0;

    AudioInfo = Audio_Info;
    return 1;
}

EXPORT int CALL RomOpen(void)
{
    if (!l_PluginInit)
        return 0;

    ReadConfig();
    InitializeAudio(GameFreq);

    return 1;
}

EXPORT void CALL RomClosed( void )
{
    if (!l_PluginInit)
        return;

    if (critical_failure == 1)
       return;

    DebugMessage(M64MSG_VERBOSE, "Cleaning up OpenSLES sound plugin...");

    CloseAudio();
}

EXPORT void CALL ProcessAList(void)
{
}

EXPORT void CALL SetSpeedFactor(int percentage)
{
    if (!l_PluginInit)
        return;
    if (percentage >= 10 && percentage <= 300)
        speed_factor = percentage;
}

EXPORT void CALL VolumeMute(void)
{
}

EXPORT void CALL VolumeUp(void)
{
}

EXPORT void CALL VolumeDown(void)
{
}

EXPORT int CALL VolumeGetLevel(void)
{
    return 100;
}

EXPORT void CALL VolumeSetLevel(int level)
{
}

EXPORT const char * CALL VolumeGetString(void)
{
    return "100%";
}

