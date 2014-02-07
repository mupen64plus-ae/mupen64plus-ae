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

import paulscode.android.mupen64plusae.CoreInterface;

/**
 * Call-outs made from Java to the native ae-exports library. Any function names changed here should
 * also be changed in the corresponding C code, and vice versa.
 * 
 * @see jni/ae-bridge/ae_exports.cpp
 * @see CoreInterface
 */
public class NativeExports
{
    static
    {
        System.loadLibrary( "ae-exports" );
    }
    
    // TODO: Add javadoc
    
    public static native void loadLibraries( String libPath );
    
    public static native void unloadLibraries();
    
    public static native void emuStart( String userDataPath, String userCachePath, Object[] args );
    
    public static native void emuStop();
    
    public static native void emuResume();
    
    public static native void emuPause();
    
    public static native void emuAdvanceFrame();
    
    public static native void emuSetSpeed( int percent );
    
    public static native void emuSetFramelimiter( boolean enabled );
    
    public static native void emuSetSlot( int slotID );
    
    public static native void emuLoadSlot();
    
    public static native void emuSaveSlot();
    
    public static native void emuLoadFile( String filename );
    
    public static native void emuSaveFile( String filename );
    
    public static native void emuScreenshot();
    
    public static native void emuGameShark( boolean pressed );
    
    public static native int emuGetState();
    
    public static native int emuGetSpeed();
    
    public static native boolean emuGetFramelimiter();
    
    public static native int emuGetSlot();
}
