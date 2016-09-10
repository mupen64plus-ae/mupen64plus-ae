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
package paulscode.android.mupen64plusae.jni;

public class NativeConstants
{
    // @formatter:off    
    public static final int EMULATOR_STATE_UNKNOWN      = 0;
    public static final int EMULATOR_STATE_STOPPED      = 1;
    public static final int EMULATOR_STATE_RUNNING      = 2;
    public static final int EMULATOR_STATE_PAUSED       = 3;
    
    public static final int M64CORE_EMU_STATE           = 1;
    public static final int M64CORE_VIDEO_MODE          = 2;
    public static final int M64CORE_SAVESTATE_SLOT      = 3;
    public static final int M64CORE_SPEED_FACTOR        = 4;
    public static final int M64CORE_SPEED_LIMITER       = 5;
    public static final int M64CORE_VIDEO_SIZE          = 6;
    public static final int M64CORE_AUDIO_VOLUME        = 7;
    public static final int M64CORE_AUDIO_MUTE          = 8;
    public static final int M64CORE_INPUT_GAMESHARK     = 9;
    public static final int M64CORE_STATE_LOADCOMPLETE  = 10;
    public static final int M64CORE_STATE_SAVECOMPLETE  = 11;
    
    public static final int PAK_TYPE_NONE               = 1;
    public static final int PAK_TYPE_MEMORY             = 2;
    public static final int PAK_TYPE_RUMBLE             = 5;
    // @formatter:on
}
