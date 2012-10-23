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

import paulscode.android.mupen64plusae.util.Audio;
import paulscode.android.mupen64plusae.util.Notifier;
import android.util.Log;

public class NativeMethods
{
    
    public static class Helpers
    {
        static
        {
            loadNativeLibName( "SDL" );
            loadNativeLibName( "core" ); // TODO: let the user choose which core to load
            loadNativeLibName( "front-end" );
        }
        
        /**
         * Loads the specified native library name (without "lib" and ".so")
         * 
         * @param filepath
         *            Full path to a native .so file (may optionally be in quotes).
         */
        public static void loadNativeLibName( String libname )
        {
            Log.v( "GameActivity", "Loading native library '" + libname + "'" );
            try
            {
                System.loadLibrary( libname );
            }
            catch( UnsatisfiedLinkError e )
            {
                Log.e( "GameActivity", "Unable to load native library '" + libname + "'" );
            }
        }
        
        /**
         * Loads the native .so file specified
         * 
         * @param filepath
         *            Full path to a native .so file (may optionally be in quotes).
         */
        public static void loadNativeLib( String filepath )
        {
            String filename = null;
            if( filepath != null && filepath.length() > 0 )
            {
                filename = filepath.replace( "\"", "" );
                if( filename.equalsIgnoreCase( "dummy" ) )
                    return;
                
                Log.v( "GameActivity", "Loading native library '" + filename + "'" );
                try
                {
                    System.load( filename );
                }
                catch( UnsatisfiedLinkError e )
                {
                    Log.e( "GameActivity", "Unable to load native library '" + filename + "'" );
                }
            }
        }
    }
    
    // *************************************************
    // *************************************************
    // *************************************************
    // Call-outs made TO the native code
    // See jni/front-end/src/main.c
    // jni/input-sdl/src/plugin.c
    // jni/SDL/src/core/android/SDL_android.cpp
    // jni/SDL/src/main/android/SDL_android_main.cpp
    // *************************************************
    // *************************************************
    // *************************************************
    
    public static native void fileLoadEmulator( String filename );
    
    public static native void fileSaveEmulator( String filename );
    
    public static native void init();
    
    public static native String getHeaderCRC( String filename );
    
    public static native String getHeaderName( String filename );
    
    public static native void onAccel( float x, float y, float z );
    
    public static native void onKeyDown( int keycode );
    
    public static native void onKeyUp( int keycode );
    
    public static native void onResize( int x, int y, int format );
    
    public static native void onSDLKeyDown( int keycode );
    
    public static native void onSDLKeyUp( int keycode );
    
    public static native void onTouch( int action, float x, float y, float p );
    
    public static native void pauseEmulator();
    
    public static native void quit();
    
    public static native void resumeEmulator();
    
    public static native void runAudioThread();
    
    public static native int stateEmulator();
    
    public static native void stateLoadEmulator();
    
    public static native void stateSaveEmulator();
    
    public static native void stateSetSlotEmulator( int slotID );
    
    public static native void stopEmulator();
    
    public static native void updateVirtualGamePadStates( int controllerNum, boolean[] buttons,
            int axisX, int axisY );
    
    public static native int RegisterThis();
    
    // ********************************************
    // ********************************************
    // ********************************************
    // Call-ins made FROM the native code
    // See jni/SDL/src/core/android/SDL_android.cpp
    // ********************************************
    // ********************************************
    // ********************************************
    
    public static Object audioInit( int sampleRate, boolean is16Bit, boolean isStereo,
            int desiredFrames )
    {
        return Audio.init( sampleRate, is16Bit, isStereo, desiredFrames );
    }
    
    public static void audioQuit()
    {
        Audio.quit();
    }
    
    public static void audioWriteByteBuffer( byte[] buffer )
    {
        Audio.writeByteBuffer( buffer );
    }
    
    public static void audioWriteShortBuffer( short[] buffer )
    {
        Audio.writeShortBuffer( buffer );
    }
    
    public static boolean createGLContext( int majorVersion, int minorVersion )
    {
        return SDLSurface.createGLContext( majorVersion, minorVersion );
    }
    
    public static void flipBuffers()
    {
        SDLSurface.flipBuffers();
    }
    
    public static boolean getAutoFrameSkip()
    {
        return Globals.userPrefs.isAutoFrameskipEnabled;
    }
    
    public static Object getDataDir()
    {
        return (Object) Globals.path.dataDir;
    }
    
    public static Object getExtraArgs()
    {
        if( Globals.extraArgs == null )
            return (Object) ".";
        return (Object) Globals.extraArgs;
    }
    
    public static int getHardwareType()
    {
        return Globals.appData.getHardwareType();
    }
    
    public static int getMaxFrameSkip()
    {
        return Globals.userPrefs.videoMaxFrameskip;
    }
    
    public static Object getROMPath()
    {
        return Globals.path.getROMPath();
    }
    
    public static boolean getScreenStretch()
    {
        return Globals.userPrefs.videoStretch;
    }
    
    public static void setActivityTitle( String title )
    {
        SDLSurface.setActivityTitle( title );
    }
    
    public static void showToast( String message )
    {
        if( Globals.gameInstance != null )
            Notifier.showToast( message, Globals.gameInstance );
    }
    
    public static boolean useRGBA8888()
    {
        return Globals.userPrefs.videoRGBA8888;
    }
    
    public static void vibrate( boolean active )
    {
        if( Globals.gameInstance != null )
            Globals.gameInstance.vibrate( active );
    }
}
