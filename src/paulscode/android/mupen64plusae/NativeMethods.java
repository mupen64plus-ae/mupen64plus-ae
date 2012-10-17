/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2012 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with Mupen64PlusAE. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: paulscode
 */
package paulscode.android.mupen64plusae;

public class NativeMethods
{
    // TODO: Change function calls in C++

    // Start things up on the native side:
    public static native void init();
    // Shut things down on the native side:
    public static native void quit();
    // Start the audio thread:
    public static native void runAudioThread();

    // Surface dimensions changed:
    public static native void onResize( int x, int y, int format );

    // Accelerometer sensor changed:
    public static native void onAccel( float x, float y, float z );

    // Native functions for reading ROM header info:
    public static native String getHeaderName( String filename );
    public static native String getHeaderCRC( String filename );

    // Input events:
    public static native void onKeyDown( int keycode );     // Android keycodes
    public static native void onKeyUp( int keycode );
    public static native void onSDLKeyDown( int keycode );  // SDL scancodes TODO: Merge
    public static native void onSDLKeyUp( int keycode );
    public static native void onTouch( int action, float x, float y, float p );

    /* From the N64 func ref: The 3D Stick data is of type signed char and in the range between
       80 and -80. (32768 / 409 = ~80.1) */
    // Sends virtual gamepad states to the input plug-in:
    public static native void updateVirtualGamePadStates( int controllerNum, boolean[] buttons, int axisX, int axisY );

    // Core functions:   
    public static native void pauseEmulator();   // Pause if running
    public static native void resumeEmulator();  // Resume if paused
    public static native void stopEmulator();    // Shut down
    public static native void stateSetSlotEmulator( int slotID ); // Change the save slot
    public static native void stateSaveEmulator();  // Save to current slot
    public static native void stateLoadEmulator();  // Load from current slot
    public static native void fileSaveEmulator( String filename ); // Save to specific file
    public static native void fileLoadEmulator( String filename ); // Load from specific file
    public static native int stateEmulator();  // Current state the emulator is in
}
