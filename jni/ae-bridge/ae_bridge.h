/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 *
 * Copyright (C) 2013 Paul Lamb
 *
 * This file is part of Mupen64PlusAE.
 *
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * Authors: Paul Lamb, littleguy77
 */

#ifndef AE_BRIDGE_H
#define AE_BRIDGE_H

#include <jni.h>
#include <android/log.h>

// Generally we should use the core's Debug API, but these can be used in a pinch
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "ae-bridge", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,   "ae-bridge", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,    "ae-bridge", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,    "ae-bridge", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,   "ae-bridge", __VA_ARGS__)
#ifndef printf
#define printf(...) LOGV(__VA_ARGS__)
#endif

#ifdef __cplusplus
extern "C" {
#endif

/*******************************************************************************
 Constants (must match definitions in Java)
 *******************************************************************************/

// arrays.xml, AppData.HardwareInfo
#define HARDWARE_TYPE_UNKNOWN       0
#define HARDWARE_TYPE_OMAP          1
#define HARDWARE_TYPE_OMAP_2        2
#define HARDWARE_TYPE_QUALCOMM      3
#define HARDWARE_TYPE_IMAP          4
#define HARDWARE_TYPE_TEGRA         5
#define HARDWARE_TYPE_CUSTOM        999

/*******************************************************************************
 Imported Java methods (to be called from C)
 *******************************************************************************/

// Initialization
extern void         SDL_Android_Init(JNIEnv* env, jclass cls);
extern void         SDL_Android_Init_Extras(JNIEnv* env, jclass cls);

// Native utilities
extern void         Android_JNI_State_Callback(int paramChanged, int newValue);

// Native utilities - TODO: Move to configuration file if possible
extern int          Android_JNI_GetHardwareType();
extern int          Android_JNI_UseRGBA8888();
extern void         Android_JNI_GetPolygonOffset(const int hardwareType, const int bias, float* f1, float* f2);

// SDL functions - TODO: Remove (plugins shouldn't be calling these directly)
extern void         Android_JNI_SwapWindow();

// Global flags - TODO: Remove when ASDP bug fixed
extern int do_Start;

#ifdef __cplusplus
}
#endif

#endif // AE_BRIDGE_H
