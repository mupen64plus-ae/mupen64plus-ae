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


import android.util.Log;
import android.view.Surface;

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
        System.loadLibrary( "ae-vidext" );
    }

    static boolean mLibrariesLoaded = false;


    
    // TODO: Add javadoc

    static void loadLibrariesIfNotLoaded(String libPath, int androidSDK )
    {
        if(!mLibrariesLoaded)
        {
            mLibrariesLoaded = true;
            loadLibraries( libPath, androidSDK );
        }
    }

    static void unloadLibrariesIfLoaded()
    {
        if(mLibrariesLoaded)
        {
            unloadLibraries();
            mLibrariesLoaded = false;
        }
    }
    
    static native void loadLibraries( String libPath, int androidSDK );
    
    static native void unloadLibraries();
    
    static native int emuStart( String userDataPath, String userCachePath, Object[] args );
    
    static native void emuStop();

    static native void emuShutdown();
    
    static native void emuResume();
    
    static native void emuPause();
    
    static native void emuAdvanceFrame();
    
    static native void emuSetSpeed( int percent );
    
    static native void emuSetFramelimiter( boolean enabled );
    
    static native void emuSetSlot( int slotID );
    
    static native void emuLoadSlot();
    
    static native void emuSaveSlot();
    
    static native void emuLoadFile( String filename );
    
    static native void emuSaveFile( String filename );
    
    static native void emuScreenshot();
    
    static native void emuGameShark( boolean pressed );

    static native void emuDestroySurface();

    static native void FPSEnabled(int recalc);
    
    static native int emuGetState();
    
    static native int emuGetSpeed();
    
    static native boolean emuGetFramelimiter();
    
    static native int emuGetSlot();

    static native int emuReset();

    static native void setNativeWindow(Surface surface);
}
