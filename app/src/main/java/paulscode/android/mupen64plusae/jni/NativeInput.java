/*
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
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.jni;


import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * Calls made between the native input-android library and Java. Any function names changed here
 * should also be changed in the corresponding C code, and vice versa.
 * 
 * @see mupen64plus-input-android/plugin.c
 * @see CoreInterface
 */
public class NativeInput
{
    /** Maximum duration for vibration if no further vibration commands are issued. */
    private static final long VIBRATE_TIMEOUT = 1000;
    
    static
    {
        System.loadLibrary( "mupen64plus-input-android" );
    }

    private static final Vibrator[] sVibrators = new Vibrator[4];
    
    /**
     * Initialize input-android plugin.
     */
    static native void init();
    
    /**
     * Set the button/axis state of a controller.
     * 
     * @param controllerNum Controller index, in the range [0,3].
     * @param buttons The pressed state of the buttons.
     * @param axisX The analog value of the x-axis, in the range [-80,80].
     * @param axisY The analog value of the y-axis, in the range [-80,80].
     * @param isDigital True if input cones frim a keyboard
     */
    static native void setState( int controllerNum, boolean[] buttons, double axisX, double axisY, boolean isDigital );
    
    /**
     * Set the plugged state and pak type of a controller.
     * 
     * @param controllerNum Controller index, in the range [0,3].
     * @param plugged Whether the controller is plugged in.
     * @param pakType The type of controller pak used.
     */
    static native void setConfig( int controllerNum, boolean plugged, int pakType );
    
    /**
     * This method should only be called by native code.
     * @see mupen64plus-input-android/plugin.c
     */
    @Deprecated
    static void rumble( int controllerNum, boolean active )
    {
        if( sVibrators[controllerNum] == null )
            return;
        
        if( active ) {
            sVibrators[controllerNum].vibrate(VIBRATE_TIMEOUT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                sVibrators[controllerNum].vibrate(VibrationEffect.createOneShot(VIBRATE_TIMEOUT, 100));
            } else {
                sVibrators[controllerNum].vibrate(VIBRATE_TIMEOUT);
            }
        }else
            sVibrators[controllerNum].cancel();
    }

    static void registerVibrator( int player, Vibrator vibrator )
    {
        boolean hasVibrator = vibrator.hasVibrator();

        if( hasVibrator && player > 0 && player < 5 )
        {
            sVibrators[player - 1] = vibrator;
        }
    }
}
