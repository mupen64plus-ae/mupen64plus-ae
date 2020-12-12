/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus-audio-android - main.c                                       *
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

#include <cstdarg>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <thread>
#include <cerrno>
#include <cmath>

#define M64P_PLUGIN_PROTOTYPES 1

#include "m64p_common.h"
#include "m64p_config.h"
#include "m64p_plugin.h"
#include "m64p_types.h"
#include "m64p_frontend.h"
#include "plugin.h"
#include "osal_dynamiclib.h"
#include <jni.h>
#include "AudioHandler.h"

/* number of bytes per sample */
#define N64_SAMPLE_BYTES 4

/* local variables */
static void (*l_DebugCallback)(void *, int, const char *) = nullptr;

static void *l_DebugCallContext = nullptr;
static int l_PluginInit = 0;
static m64p_handle l_ConfigAudio;

// This sets default frequency what is used if rom doesn't want to change it.
// Probably only game that needs this is Zelda: Ocarina Of Time Master Quest
// *NOTICE* We should try to find out why Demos' frequencies are always wrong
// They tend to rely on a default frequency, apparently, never the same one ;)
static const int defaultFrequency = 33600;

/* Read header for type definition */
static AUDIO_INFO AudioInfo;
/* Audio frequency, this is usually obtained from the game, but for compatibility we set default value */
static int GameFreq = defaultFrequency;
/* SpeedFactor is used to increase/decrease game playback speed */
static unsigned int speed_factor = 100;

/* Definitions of pointers to Core config functions */
ptr_ConfigOpenSection ConfigOpenSection = nullptr;
ptr_ConfigDeleteSection ConfigDeleteSection = nullptr;
ptr_ConfigSaveSection ConfigSaveSection = nullptr;
ptr_ConfigSetParameter ConfigSetParameter = nullptr;
ptr_ConfigGetParameter ConfigGetParameter = nullptr;
ptr_ConfigGetParameterHelp ConfigGetParameterHelp = nullptr;
ptr_ConfigSetDefaultInt ConfigSetDefaultInt = nullptr;
ptr_ConfigSetDefaultFloat ConfigSetDefaultFloat = nullptr;
ptr_ConfigSetDefaultBool ConfigSetDefaultBool = nullptr;
ptr_ConfigSetDefaultString ConfigSetDefaultString = nullptr;
ptr_ConfigGetParamInt ConfigGetParamInt = nullptr;
ptr_ConfigGetParamFloat ConfigGetParamFloat = nullptr;
ptr_ConfigGetParamBool ConfigGetParamBool = nullptr;
ptr_ConfigGetParamString ConfigGetParamString = nullptr;
ptr_CoreDoCommand CoreDoCommand = nullptr;

/* Global functions */
static void DebugMessage(int level, const char *message, ...) {
    char msgbuf[1024];
    va_list args;

    if (l_DebugCallback == nullptr)
        return;

    va_start(args, message);
    vsprintf(msgbuf, message, args);

    (*l_DebugCallback)(l_DebugCallContext, level, msgbuf);

    va_end(args);
}

static void ReadConfig() {
    /* read the configuration values into our static variables */
    int swapChannels = ConfigGetParamBool(l_ConfigAudio, "SWAP_CHANNELS");
    int hardwareBufferSize = ConfigGetParamInt(l_ConfigAudio, "HARDWARE_BUFFER_SIZE");
    int audioBuffersMs = ConfigGetParamInt(l_ConfigAudio, "AUDIO_BUFFER_SIZE_MS");
    int volume = ConfigGetParamInt(l_ConfigAudio, "VOLUME");
    int samplingRateSelection = ConfigGetParamInt(l_ConfigAudio, "SAMPLING_RATE");
    int samplingType = ConfigGetParamInt(l_ConfigAudio, "SAMPLING_TYPE");
    int timeStretchEnabled = ConfigGetParamBool(l_ConfigAudio, "TIME_STRETCH_ENABLED");
    int forceSles = ConfigGetParamBool(l_ConfigAudio, "FORCE_SLES");

    AudioHandler::get().setSwapChannels(swapChannels);
    AudioHandler::get().setHardwareBufferSize(hardwareBufferSize);
    AudioHandler::get().setTargetPrimingBuffersMs(audioBuffersMs);
    AudioHandler::get().setSamplingRateSelection(samplingRateSelection);
    AudioHandler::get().setSamplingType(samplingType);
    AudioHandler::get().setTimeStretchEnabled(timeStretchEnabled);
    AudioHandler::get().setVolume(volume);
    AudioHandler::get().forceSles(forceSles);
}

/* Mupen64Plus plugin functions */
EXPORT m64p_error CALL PluginStartup(m64p_dynlib_handle CoreLibHandle, void *Context,
                                     void (*DebugCallback)(void *, int, const char *)) {
    ptr_CoreGetAPIVersions CoreAPIVersionFunc;

    int ConfigAPIVersion, DebugAPIVersion, VidextAPIVersion, bSaveConfig;
    float fConfigParamsVersion = 0.0f;

    if (l_PluginInit)
        return M64ERR_ALREADY_INIT;

    /* first thing is to set the callback function for debug info */
    l_DebugCallback = DebugCallback;
    l_DebugCallContext = Context;

    AudioHandler::get().setLoggingFunction(Context, DebugCallback);

    /* attach and call the CoreGetAPIVersions function, check Config API version for compatibility */
    CoreAPIVersionFunc = (ptr_CoreGetAPIVersions) osal_dynlib_getproc(CoreLibHandle,
                                                                      "CoreGetAPIVersions");
    if (CoreAPIVersionFunc == nullptr) {
        DebugMessage(M64MSG_ERROR, "Core emulator broken; no CoreAPIVersionFunc() function found.");
        return M64ERR_INCOMPATIBLE;
    }

    (*CoreAPIVersionFunc)(&ConfigAPIVersion, &DebugAPIVersion, &VidextAPIVersion, nullptr);
    if ((ConfigAPIVersion & 0xffff0000) != (CONFIG_API_VERSION & 0xffff0000)) {
        DebugMessage(M64MSG_ERROR,
                     "Emulator core Config API (v%i.%i.%i) incompatible with plugin (v%i.%i.%i)",
                     VERSION_PRINTF_SPLIT(ConfigAPIVersion),
                     VERSION_PRINTF_SPLIT(CONFIG_API_VERSION));
        return M64ERR_INCOMPATIBLE;
    }

    /* Get the core config function pointers from the library handle */
    ConfigOpenSection = (ptr_ConfigOpenSection) osal_dynlib_getproc(CoreLibHandle,
                                                                    "ConfigOpenSection");
    ConfigDeleteSection = (ptr_ConfigDeleteSection) osal_dynlib_getproc(CoreLibHandle,
                                                                        "ConfigDeleteSection");
    ConfigSaveSection = (ptr_ConfigSaveSection) osal_dynlib_getproc(CoreLibHandle,
                                                                    "ConfigSaveSection");
    ConfigSetParameter = (ptr_ConfigSetParameter) osal_dynlib_getproc(CoreLibHandle,
                                                                      "ConfigSetParameter");
    ConfigGetParameter = (ptr_ConfigGetParameter) osal_dynlib_getproc(CoreLibHandle,
                                                                      "ConfigGetParameter");
    ConfigSetDefaultInt = (ptr_ConfigSetDefaultInt) osal_dynlib_getproc(CoreLibHandle,
                                                                        "ConfigSetDefaultInt");
    ConfigSetDefaultFloat = (ptr_ConfigSetDefaultFloat) osal_dynlib_getproc(CoreLibHandle,
                                                                            "ConfigSetDefaultFloat");
    ConfigSetDefaultBool = (ptr_ConfigSetDefaultBool) osal_dynlib_getproc(CoreLibHandle,
                                                                          "ConfigSetDefaultBool");
    ConfigSetDefaultString = (ptr_ConfigSetDefaultString) osal_dynlib_getproc(CoreLibHandle,
                                                                              "ConfigSetDefaultString");
    ConfigGetParamInt = (ptr_ConfigGetParamInt) osal_dynlib_getproc(CoreLibHandle,
                                                                    "ConfigGetParamInt");
    ConfigGetParamFloat = (ptr_ConfigGetParamFloat) osal_dynlib_getproc(CoreLibHandle,
                                                                        "ConfigGetParamFloat");
    ConfigGetParamBool = (ptr_ConfigGetParamBool) osal_dynlib_getproc(CoreLibHandle,
                                                                      "ConfigGetParamBool");
    ConfigGetParamString = (ptr_ConfigGetParamString) osal_dynlib_getproc(CoreLibHandle,
                                                                          "ConfigGetParamString");
    CoreDoCommand = (ptr_CoreDoCommand) osal_dynlib_getproc(CoreLibHandle, "CoreDoCommand");

    if (!ConfigOpenSection || !ConfigDeleteSection || !ConfigSetParameter || !ConfigGetParameter ||
        !ConfigSetDefaultInt || !ConfigSetDefaultFloat || !ConfigSetDefaultBool ||
        !ConfigSetDefaultString ||
        !ConfigGetParamInt || !ConfigGetParamFloat || !ConfigGetParamBool ||
        !ConfigGetParamString ||
        !CoreDoCommand)
        return M64ERR_INCOMPATIBLE;

    /* ConfigSaveSection was added in Config API v2.1.0 */
    if (ConfigAPIVersion >= 0x020100 && !ConfigSaveSection)
        return M64ERR_INCOMPATIBLE;

    /* get a configuration section handle */
    if (ConfigOpenSection("Audio-Android", &l_ConfigAudio) != M64ERR_SUCCESS) {
        DebugMessage(M64MSG_ERROR, "Couldn't open config section 'Audio-Android'");
        return M64ERR_INPUT_NOT_FOUND;
    }

    /* check the section version number */
    bSaveConfig = 0;
    if (ConfigGetParameter(l_ConfigAudio, "Version", M64TYPE_FLOAT, &fConfigParamsVersion,
                           sizeof(float)) != M64ERR_SUCCESS) {
        DebugMessage(M64MSG_WARNING,
                     "No version number in 'Audio-Android' config section. Setting defaults.");
        ConfigDeleteSection("Audio-Android");
        ConfigOpenSection("Audio-Android", &l_ConfigAudio);
        bSaveConfig = 1;
    } else if (((int) fConfigParamsVersion) != ((int) CONFIG_PARAM_VERSION)) {
        DebugMessage(M64MSG_WARNING,
                     "Incompatible version %.2f in 'Audio-Android' config section: current is %.2f. Setting defaults.",
                     fConfigParamsVersion, (float) CONFIG_PARAM_VERSION);
        ConfigDeleteSection("Audio-Android");
        ConfigOpenSection("Audio-Android", &l_ConfigAudio);
        bSaveConfig = 1;
    } else if ((CONFIG_PARAM_VERSION - fConfigParamsVersion) >= 0.0001f) {
        /* handle upgrades */
        float fVersion = CONFIG_PARAM_VERSION;
        ConfigSetParameter(l_ConfigAudio, "Version", M64TYPE_FLOAT, &fVersion);
        DebugMessage(M64MSG_INFO,
                     "Updating parameter set version in 'Audio-Android' config section to %.2f",
                     fVersion);
        bSaveConfig = 1;
    }

    /* set the default values for this plugin */
    ConfigSetDefaultFloat(l_ConfigAudio, "Version", CONFIG_PARAM_VERSION,
                          "Mupen64Plus SDL Audio Plugin config parameter version number");
    ConfigSetDefaultBool(l_ConfigAudio, "SWAP_CHANNELS", 0, "Swaps left and right channels");
    ConfigSetDefaultInt(l_ConfigAudio, "HARDWARE_BUFFER_SIZE", AudioHandler::defaultHardwareBufferSize,
                        "Size of  hardware buffer in samples");
    ConfigSetDefaultInt(l_ConfigAudio, "AUDIO_BUFFER_SIZE_MS", AudioHandler::maxBufferSizeMs,
                        "Audio buffer size in milliseconds");
    ConfigSetDefaultInt(l_ConfigAudio, "VOLUME", 100, "Desired volume");
    ConfigSetDefaultInt(l_ConfigAudio, "SAMPLING_RATE", 0,
                        "Sampling rate in hz, (0=game original");
    ConfigSetDefaultInt(l_ConfigAudio, "SAMPLING_TYPE", 0,
                        "Sampling type when not time streteching, (0=trivial, 1=soundtouch");
    ConfigSetDefaultBool(l_ConfigAudio, "TIME_STRETCH_ENABLED", 1,
                         "Enable audio time stretching to prevent crackling");
    ConfigSetDefaultBool(l_ConfigAudio, "FORCE_SLES", 0, "Force SLES audio (0=auto,1=force)");

    if (bSaveConfig && ConfigAPIVersion >= 0x020100)
        ConfigSaveSection("Audio-Android");

    l_PluginInit = 1;

    return M64ERR_SUCCESS;
}

EXPORT m64p_error CALL PluginShutdown(void)
{
    if (!l_PluginInit)
        return M64ERR_NOT_INIT;

    AudioHandler::get().closeAudio();

    /* reset some local variables */
    l_DebugCallback = nullptr;
    l_DebugCallContext = nullptr;
    l_PluginInit = 0;

    return M64ERR_SUCCESS;
}

EXPORT m64p_error CALL
PluginGetVersion(m64p_plugin_type *PluginType, int *PluginVersion, int *APIVersion,
                 const char **PluginNamePtr, int *Capabilities) {
    /* set version info */
    if (PluginType != nullptr)
        *PluginType = M64PLUGIN_AUDIO;

    if (PluginVersion != nullptr)
        *PluginVersion = OPENSLES_AUDIO_PLUGIN_VERSION;

    if (APIVersion != nullptr)
        *APIVersion = AUDIO_PLUGIN_API_VERSION;

    if (PluginNamePtr != nullptr)
        *PluginNamePtr = "Mupen64Plus Android Audio Plugin";

    if (Capabilities != nullptr) {
        *Capabilities = 0;
    }

    return M64ERR_SUCCESS;
}

/* ----------- Audio Functions ------------- */
EXPORT void CALL AiDacrateChanged(int SystemType) {
    int freq;

    if (!l_PluginInit)
        return;

    switch (SystemType) {
        case SYSTEM_NTSC:
            freq = 48681812 / (*AudioInfo.AI_DACRATE_REG + 1);
            break;
        case SYSTEM_PAL:
            freq = 49656530 / (*AudioInfo.AI_DACRATE_REG + 1);
            break;
        case SYSTEM_MPAL:
            freq = 48628316 / (*AudioInfo.AI_DACRATE_REG + 1);
            break;
        default:
            freq = defaultFrequency;
            break;
    }

    ReadConfig();
    GameFreq = freq;
    AudioHandler::get().initializeAudio(freq);
}

bool isSpeedLimiterEnabled() {
    int e = 1;
    CoreDoCommand(M64CMD_CORE_STATE_QUERY, M64CORE_SPEED_LIMITER, &e);
    return static_cast<bool>(e);
}

EXPORT void CALL AiLenChanged(void) {
    static const double minSleepNeededForReset = -5.0;
    static const double minSleepNeeded = -0.1;
    static const double maxSleepNeeded = 0.5;
    static bool hasBeenReset = false;
    static unsigned long totalElapsedSamples = 0;
    static std::chrono::time_point<std::chrono::steady_clock, std::chrono::duration<double>> gameStartTime;
    static int lastSpeedFactor = 100;
    static bool lastSpeedLimiterEnabledState = false;
    static bool busyWait = false;
    static int busyWaitEnableCount = 0;
    static int busyWaitDisableCount = 0;
    static const int busyWaitCheck = 30;

    if (!l_PluginInit)
        return;

    bool limiterEnabled = isSpeedLimiterEnabled();

    auto currentTime = std::chrono::steady_clock::now();

    //if this is the first time or we are resuming from pause
    if (gameStartTime.time_since_epoch().count() == 0 || !hasBeenReset ||
        lastSpeedFactor != speed_factor || lastSpeedLimiterEnabledState != limiterEnabled) {
        lastSpeedLimiterEnabledState = limiterEnabled;
        gameStartTime = currentTime;
        totalElapsedSamples = 0;
        hasBeenReset = true;
        totalElapsedSamples = 0;
    }

    lastSpeedFactor = speed_factor;

    unsigned int LenReg = *AudioInfo.AI_LEN_REG;
    unsigned char *inputAudio = AudioInfo.RDRAM + (*AudioInfo.AI_DRAM_ADDR_REG & 0xFFFFFF);

    // Push data to the audio handler
    std::chrono::duration<double> timeSinceStart = currentTime - gameStartTime;
	AudioHandler::get().pushData(reinterpret_cast<int16_t*>(inputAudio), LenReg/N64_SAMPLE_BYTES, timeSinceStart);

    //Calculate total ellapsed game time
    totalElapsedSamples += LenReg / N64_SAMPLE_BYTES;
    double speedFactor = static_cast<double>(speed_factor) / 100.0;
    double totalElapsedGameTime = ((double) totalElapsedSamples) / (double) GameFreq / speedFactor;

    //Slow the game down if sync game to audio is enabled
    if (!limiterEnabled) {
        double sleepNeeded = totalElapsedGameTime - timeSinceStart.count();

        if (sleepNeeded < minSleepNeededForReset || sleepNeeded > (maxSleepNeeded / speedFactor)) {
            hasBeenReset = false;
        }

        //We don't want to let the game get too far ahead, otherwise we may have a sudden burst of speed
        if (sleepNeeded < minSleepNeeded) {
            gameStartTime -= std::chrono::duration<double>(minSleepNeeded);
        }

        //Enable busywait mode if we have X callbacks of negative sleep. Don't disable busywait
        //until we have X positive callbacks
        if (sleepNeeded <= 0.0) {
            ++busyWaitEnableCount;
        } else {
            busyWaitEnableCount = 0;
        }

        if (busyWaitEnableCount == busyWaitCheck) {
            busyWait = true;
            busyWaitEnableCount = 0;
            busyWaitDisableCount = 0;
        }

        if (busyWait) {
            if (sleepNeeded > 0) {
                ++busyWaitDisableCount;
            }

            if (busyWaitDisableCount == busyWaitCheck) {
                busyWait = false;
            }
        }

        //Useful logging
        //DebugMessage(M64MSG_ERROR, "Real=%f, Game=%f, sleep=%f, start=%f, speed=%d, sleep_before_factor=%f",
        //             timeSinceStart.count(), totalElapsedGameTime, sleepNeeded, gameStartTime, speed_factor, sleepNeeded*speedFactor);
        if (sleepNeeded > 0.0 && sleepNeeded < (maxSleepNeeded / speedFactor)) {
            auto endTime = currentTime + std::chrono::duration<double>(sleepNeeded);

            if (busyWait) {
                while (std::chrono::steady_clock::now() < endTime);
            }
            else {
                std::this_thread::sleep_until(endTime);
            }
        }
    }
}

EXPORT int CALL InitiateAudio(AUDIO_INFO Audio_Info) {
    if (!l_PluginInit)
        return 0;

    AudioInfo = Audio_Info;
    return 1;
}

EXPORT int CALL RomOpen(void) {
    if (!l_PluginInit)
        return 0;

    ReadConfig();
    AudioHandler::get().initializeAudio(GameFreq);

    return 1;
}

EXPORT void CALL RomClosed(void) {
    if (!l_PluginInit)
        return;

    DebugMessage(M64MSG_VERBOSE, "Cleaning up Android sound plugin...");

    AudioHandler::get().closeAudio();
}

EXPORT void CALL ProcessAList(void) {
}

EXPORT void CALL SetSpeedFactor(int percentage) {
    if (!l_PluginInit)
        return;
    if (percentage >= 10 && percentage <= 300) {
        speed_factor = percentage;
        AudioHandler::get().setSpeedFactor(speed_factor);
    }
}

EXPORT void CALL VolumeMute(void) {
}

EXPORT void CALL VolumeUp(void) {
}

EXPORT void CALL VolumeDown(void) {
    DebugMessage(M64MSG_ERROR, "VOLUME DOWN");
}

EXPORT int CALL VolumeGetLevel(void) {
    return 100;
}

EXPORT void CALL VolumeSetLevel(int level) {
}

EXPORT const char *CALL VolumeGetString(void) {
    return "100%";
}

extern "C" EXPORT void pauseEmulator(void) {
	AudioHandler::get().pausePlayback();
}

extern "C" EXPORT void CALL resumeEmulator(void) {
	AudioHandler::get().resumePlayback();
}

extern "C" EXPORT void CALL setVolume(int volume) {
	AudioHandler::get().setVolume(volume);
}
