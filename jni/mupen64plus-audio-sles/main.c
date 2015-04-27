/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus-audio-sles - main.c                                   *
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

#ifdef USE_SRC
#include <samplerate.h>
#endif
#ifdef USE_SPEEX
#include <speex/speex_resampler.h>
#endif

#define M64P_PLUGIN_PROTOTYPES 1
#include "m64p_common.h"
#include "m64p_config.h"
#include "m64p_plugin.h"
#include "m64p_types.h"
#include "main.h"
#include "osal_dynamiclib.h"

typedef struct threadLock_
{
  pthread_mutex_t mutex;
  pthread_cond_t  cond;
  volatile unsigned char value;
} threadLock;

// TODO: do dynamic allocation on ring buffer depending on output buffer size (typically x8)
// TODO: make output buffer size + output buffer number configurable
#define RING_BUFFER_SIZE 	32768
#define OUTPUT_BUFFER_SIZE 	4096
#define OUTPUT_BUFFER_NBR	2
#define DEFAULT_FREQUENCY 	33600

/* local variables */
static void (*l_DebugCallback)(void *, int, const char *) = NULL;
static void *l_DebugCallContext = NULL;
static int l_PluginInit = 0;

static m64p_handle l_ConfigAudio;

enum resampler_type {
	RESAMPLER_TRIVIAL,
#ifdef USE_SRC
	RESAMPLER_SRC,
#endif
#ifdef USE_SPEEX
	RESAMPLER_SPEEX,
#endif
};

/* Read header for type definition */
static AUDIO_INFO AudioInfo;
/* Ring buffer where audio is loaded after it has been extracted from n64's memory */
static unsigned char ringBuffer[RING_BUFFER_SIZE];
/* Size of the ring buffer */
static int ringBufferSize = RING_BUFFER_SIZE;
/* Position of the next output buffer to enqueue, inside ring buffer */
static int ringBufferNextQueuePos = 0;
/* Current Position inside ring buffer */
static int ringBufferCurrentPos = 0;
/* Size of output buffers */
static int outputBufferBytes = OUTPUT_BUFFER_SIZE;
/* Number of bytes in the ring buffer that haven't been enqueued yet */
static int unqueuedBytes = 0;
/* Audio frequency, this is usually obtained from the game, but for compatibility we set default value */
static int GameFreq = DEFAULT_FREQUENCY;
/* SpeedFactor is used to increase/decrease game playback speed */
static unsigned int speed_factor = 100;
/* If this is true then left and right channels are swapped */
static int SwapChannels = 0;
/* Resample type */
static enum resampler_type Resample = RESAMPLER_TRIVIAL;
/* Resampler specific quality */
static int ResampleQuality = 3;
/* Output Audio frequency */
static int OutputFreq;

/* Prototype of local functions */
static void InitializeAudio(int freq);
static void ReadConfig(void);

static int critical_failure = 0;

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

/* This callback handler is called every time a buffer finishes playing */
void queueCallback(SLAndroidSimpleBufferQueueItf caller, void *context)
{
    threadLock *plock = (threadLock *) context;
    
    pthread_mutex_lock(&(plock->mutex));
    
    if(plock->value == 0)
        pthread_cond_signal(&(plock->cond));

    if(plock->value < OUTPUT_BUFFER_NBR)
        plock->value++;

    pthread_mutex_unlock(&(plock->mutex));
}

static void openSLDestroyEngine(void)
{
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
    
    // TODO: check if mutex + cond exist before destroying
    pthread_cond_signal(&(lock.cond));
    pthread_mutex_unlock(&(lock.mutex));
    pthread_cond_destroy(&(lock.cond));
    pthread_mutex_destroy(&(lock.mutex));
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

    if (!ConfigOpenSection || !ConfigDeleteSection || !ConfigSetParameter || !ConfigGetParameter ||
        !ConfigSetDefaultInt || !ConfigSetDefaultFloat || !ConfigSetDefaultBool || !ConfigSetDefaultString ||
        !ConfigGetParamInt   || !ConfigGetParamFloat   || !ConfigGetParamBool   || !ConfigGetParamString)
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
    ConfigSetDefaultFloat(	l_ConfigAudio, "Version"			,CONFIG_PARAM_VERSION	,"Mupen64Plus OpenSLES Audio Plugin config parameter version number");
    ConfigSetDefaultInt(	l_ConfigAudio, "DEFAULT_FREQUENCY"	,DEFAULT_FREQUENCY		,"Frequency which is used if rom doesn't want to change it");
    ConfigSetDefaultBool(	l_ConfigAudio, "SWAP_CHANNELS"		,0						,"Swaps left and right channels");
    ConfigSetDefaultString(	l_ConfigAudio, "RESAMPLE"			,"trivial"				,"Audio resampling algorithm. src-sinc-best-quality, src-sinc-medium-quality, src-sinc-fastest, src-zero-order-hold, src-linear, speex-fixed-{10-0}, trivial");

    if (bSaveConfig && ConfigAPIVersion >= 0x020100)
        ConfigSaveSection("Audio-OpenSLES");

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

EXPORT void CALL AiLenChanged(void)
{
    if (critical_failure == 1)
        return;

    if (!l_PluginInit)
        return;

    int newsamplerate = OutputFreq * 100 / speed_factor;
    int oldsamplerate = GameFreq;
    
    unsigned int LenReg = *AudioInfo.AI_LEN_REG;
    unsigned char * p = AudioInfo.RDRAM + (*AudioInfo.AI_DRAM_ADDR_REG & 0xFFFFFF);
    
    // TODO: resample
    // if (newsamplerate > oldsamplerate)
    // else if (newsamplerate < oldsamplerate)
    // else

    unqueuedBytes += LenReg;
    
    if (unqueuedBytes < ringBufferSize)
    {
        unsigned int i;
        unsigned int j = 0;
    
        for ( i = 0 ; i < LenReg ; i += 4 )
        {
            if ((ringBufferCurrentPos+j) >= ringBufferSize)
                ringBufferCurrentPos = j = 0;
    
            if(SwapChannels == 0)
            {
                /* Left channel */
                ringBuffer[ ringBufferCurrentPos + j ] = p[ i + 2 ];
                ringBuffer[ ringBufferCurrentPos + j + 1 ] = p[ i + 3 ];

                /* Right channel */
                ringBuffer[ ringBufferCurrentPos + j + 2 ] = p[ i ];
                ringBuffer[ ringBufferCurrentPos + j + 3 ] = p[ i + 1 ];
            } 
            else 
            {
                /* Left channel */
                ringBuffer[ ringBufferCurrentPos + j ] = p[ i ];
                ringBuffer[ ringBufferCurrentPos + j + 1 ] = p[ i + 1 ];

                /* Right channel */
                ringBuffer[ ringBufferCurrentPos + j + 2 ] = p[ i + 2];
                ringBuffer[ ringBufferCurrentPos + j + 3 ] = p[ i + 3 ];
            }
            
            j += 4;
        }
        ringBufferCurrentPos += j;
    }
    else
        DebugMessage(M64MSG_WARNING, "AiLenChanged(): Audio ring buffer overflow.");
    
    if (unqueuedBytes >= outputBufferBytes)
    {
        int toEnqueue = unqueuedBytes / outputBufferBytes;
    
        while (toEnqueue)
        {
            pthread_mutex_lock(&(lock.mutex));
    
            // Wait for the next callback if no more output buffers available
            while (lock.value == 0)
                pthread_cond_wait(&(lock.cond), &(lock.mutex));
    
            lock.value--;

            pthread_mutex_unlock(&(lock.mutex));

            (*bufferQueue)->Enqueue(bufferQueue, (ringBuffer+ringBufferNextQueuePos), outputBufferBytes);
            ringBufferNextQueuePos += outputBufferBytes;
    
            if (ringBufferNextQueuePos >= ringBufferSize)
                ringBufferNextQueuePos = 0;
    
            unqueuedBytes -= outputBufferBytes;
            toEnqueue--;
        }
    }
    else
    {
        //DebugMessage(M64MSG_WARNING, "AiLenChanged(): Audio buffer underflow.");
    }
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
static float *_src = NULL;
static unsigned int _src_len = 0;
static float *_dest = NULL;
static unsigned int _dest_len = 0;
static int error;
static SRC_STATE *src_state;
static SRC_DATA src_data;
#endif
#ifdef USE_SPEEX
SpeexResamplerState* spx_state = NULL;
static int error;
#endif

static int resample(unsigned char *input, int input_avail, int oldsamplerate, unsigned char *output, int output_needed, int newsamplerate)
{
    int *psrc = (int*)input;
    int *pdest = (int*)output;
    int i = 0, j = 0;

#ifdef USE_SPEEX
    spx_uint32_t in_len, out_len;
    if(Resample == RESAMPLER_SPEEX)
    {
        if(spx_state == NULL)
        {
            spx_state = speex_resampler_init(2, oldsamplerate, newsamplerate, ResampleQuality,  &error);
            if(spx_state == NULL)
            {
                memset(output, 0, output_needed);
                return 0;
            }
        }
        speex_resampler_set_rate(spx_state, oldsamplerate, newsamplerate);
        in_len = input_avail / 4;
        out_len = output_needed / 4;

        if ((error = speex_resampler_process_interleaved_int(spx_state, (const spx_int16_t *)input, &in_len, (spx_int16_t *)output, &out_len)))
        {
            memset(output, 0, output_needed);
            return input_avail;  // number of bytes consumed
        }
        return in_len * 4;
    }
#endif
#ifdef USE_SRC
    if(Resample == RESAMPLER_SRC)
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
            src_state = src_new (ResampleQuality, 2, &error);
            if(src_state == NULL)
            {
                memset(output, 0, output_needed);
                return 0;
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
    // RESAMPLE == TRIVIAL
    if (newsamplerate >= oldsamplerate)
    {
        int sldf = oldsamplerate;
        int const2 = 2*sldf;
        int dldf = newsamplerate;
        int const1 = const2 - 2*dldf;
        int criteria = const2 - dldf;
        for (i = 0; i < output_needed/4; i++)
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
    // newsamplerate < oldsamplerate, this only happens when speed_factor > 1
    for (i = 0; i < output_needed/4; i++)
    {
        j = i * oldsamplerate / newsamplerate;
        pdest[i] = psrc[j];
    }
    return j * 4; //number of bytes consumed
}

EXPORT int CALL RomOpen(void)
{
    if (!l_PluginInit)
        return 0;

    ReadConfig();
    InitializeAudio(GameFreq);
    return 1;
}

static void InitializeAudio(int freq)
{
    SLuint32 sample_rate;

    /* Sometimes a bad frequency is requested so ignore it */
    if (freq < 4000)
        return;

    if (critical_failure)
        return;

    /* This is important for the sync */
    GameFreq = freq;

    // TODO: Needs validation
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
    
    DebugMessage(M64MSG_INFO, "Game frequency: %iHz.", GameFreq);
    DebugMessage(M64MSG_INFO, "Requested output frequency: %iHz.", OutputFreq);

    /* Engine object */
    SLresult result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    if(result != SL_RESULT_SUCCESS) goto failure;
    
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    if(result != SL_RESULT_SUCCESS) goto failure;
    
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
    if(result != SL_RESULT_SUCCESS) goto failure;
    
    /* Output mix object */
    result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 0, NULL, NULL);
    if(result != SL_RESULT_SUCCESS) goto failure;
    
    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    if(result != SL_RESULT_SUCCESS) goto failure;
    
    SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, OUTPUT_BUFFER_NBR};
    
    SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM,2, sample_rate,
                   SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
                   (SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT), SL_BYTEORDER_LITTLEENDIAN};
    
    SLDataSource audioSrc = {&loc_bufq, &format_pcm};
    
    /* Configure audio sink */
    SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};
    SLDataSink audioSnk = {&loc_outmix, NULL};
    
    /* Create audio player */
    const SLInterfaceID ids1[] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
    const SLboolean req1[] = {SL_BOOLEAN_TRUE};
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &(playerObject), &audioSrc, &audioSnk, 1, ids1, req1);
    if(result != SL_RESULT_SUCCESS) goto failure;
    
    /* Realize the player */
    result = (*playerObject)->Realize(playerObject, SL_BOOLEAN_FALSE);
    if(result != SL_RESULT_SUCCESS) goto failure;
    
    /* Get the play interface */
    result = (*playerObject)->GetInterface(playerObject, SL_IID_PLAY, &(playerPlay));
    if(result != SL_RESULT_SUCCESS) goto failure;
    
    /* Get the buffer queue interface */
    result = (*playerObject)->GetInterface(playerObject, SL_IID_ANDROIDSIMPLEBUFFERQUEUE, &(bufferQueue));
    if(result != SL_RESULT_SUCCESS) goto failure;
    
    /* Create thread Locks to ensure synchronization between callback and processing code */
    if (pthread_mutex_init(&(lock.mutex), (pthread_mutexattr_t*) NULL) != 0)
        goto failure;
    
    if (pthread_cond_init(&(lock.cond), (pthread_condattr_t*) NULL) != 0)
    {
        pthread_mutex_destroy(&(lock.mutex));
        goto failure;
    }
    
    /* Number of output buffers */
    lock.value = OUTPUT_BUFFER_NBR;
    
    pthread_cond_signal(&(lock.cond));
    
    /* register callback on the buffer queue */
    result = (*bufferQueue)->RegisterCallback(bufferQueue, queueCallback, &lock);
    if(result != SL_RESULT_SUCCESS) goto failure;
    
    /* set the player's state to playing */
    result = (*playerPlay)->SetPlayState(playerPlay, SL_PLAYSTATE_PLAYING);
    
    return;

failure:
    DebugMessage(M64MSG_ERROR, "Couldn't open OpenSLES audio");
    openSLDestroyEngine();
    critical_failure = 1;
    return;
}

EXPORT void CALL RomClosed( void )
{
    if (!l_PluginInit)
        return;

    if (critical_failure == 1)
       return;

    DebugMessage(M64MSG_VERBOSE, "Cleaning up OpenSLES sound plugin...");
    openSLDestroyEngine();
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

static void ReadConfig(void)
{
    const char *resampler_id;

    /* read the configuration values into our static variables */
    GameFreq = ConfigGetParamInt(l_ConfigAudio, "DEFAULT_FREQUENCY");
    SwapChannels = ConfigGetParamBool(l_ConfigAudio, "SWAP_CHANNELS");
    resampler_id = ConfigGetParamString(l_ConfigAudio, "RESAMPLE");

    if (!resampler_id) {
        Resample = RESAMPLER_TRIVIAL;
    DebugMessage(M64MSG_WARNING, "Could not find RESAMPLE configuration; use trivial resampler");
    return;
    }
    if (strcmp(resampler_id, "trivial") == 0) {
        Resample = RESAMPLER_TRIVIAL;
        return;
    }
#ifdef USE_SPEEX
    if (strncmp(resampler_id, "speex-fixed-", strlen("speex-fixed-")) == 0) {
        int i;
        static const char *speex_quality[] = {
            "speex-fixed-0",
            "speex-fixed-1",
            "speex-fixed-2",
            "speex-fixed-3",
            "speex-fixed-4",
            "speex-fixed-5",
            "speex-fixed-6",
            "speex-fixed-7",
            "speex-fixed-8",
            "speex-fixed-9",
            "speex-fixed-10",
        };
        Resample = RESAMPLER_SPEEX;
        for (i = 0; i < sizeof(speex_quality) / sizeof(*speex_quality); i++) {
            if (strcmp(speex_quality[i], resampler_id) == 0) {
                ResampleQuality = i;
                return;
            }
        }
        DebugMessage(M64MSG_WARNING, "Unknown RESAMPLE configuration %s; use speex-fixed-4 resampler", resampler_id);
        ResampleQuality = 4;
        return;
    }
#endif
#ifdef USE_SRC
    if (strncmp(resampler_id, "src-", strlen("src-")) == 0) {
        Resample = RESAMPLER_SRC;
        if (strcmp(resampler_id, "src-sinc-best-quality") == 0) {
            ResampleQuality = SRC_SINC_BEST_QUALITY;
            return;
        }
        if (strcmp(resampler_id, "src-sinc-medium-quality") == 0) {
            ResampleQuality = SRC_SINC_MEDIUM_QUALITY;
            return;
        }
        if (strcmp(resampler_id, "src-sinc-fastest") == 0) {
            ResampleQuality = SRC_SINC_FASTEST;
            return;
        }
        if (strcmp(resampler_id, "src-zero-order-hold") == 0) {
            ResampleQuality = SRC_ZERO_ORDER_HOLD;
            return;
        }
        if (strcmp(resampler_id, "src-linear") == 0) {
            ResampleQuality = SRC_LINEAR;
            return;
        }
        DebugMessage(M64MSG_WARNING, "Unknown RESAMPLE configuration %s; use src-sinc-medium-quality resampler", resampler_id);
        ResampleQuality = SRC_SINC_MEDIUM_QUALITY;
        return;
    }
#endif
    DebugMessage(M64MSG_WARNING, "Unknown RESAMPLE configuration %s; use trivial resampler", resampler_id);
    Resample = RESAMPLER_TRIVIAL;
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


