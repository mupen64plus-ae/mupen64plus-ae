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
 * Authors: Paul Lamb
 */
package paulscode.android.mupen64plusae;

import java.io.File;
import java.util.Locale;

import paulscode.android.mupen64plusae.util.ErrorLogger;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.SafeMethods;
import paulscode.android.mupen64plusae.util.Utility;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * The portion of the core interface that directly bridges Java and C code. Any function names
 * changed here should also be changed in the corresponding C code, and vice versa.
 * 
 * @see CoreInterface
 */
public class CoreInterfaceNative extends CoreInterface
{
    // TODO: These should all have javadoc comments. 
    // It would better document calls going in/out of native code.
    
    // ************************************************************************
    // ************************************************************************
    // Java <-> C communication (input-android)
    // jni/input-android/plugin.c
    // ************************************************************************
    // ************************************************************************
    
    public static native void jniInitInput();
    
    public static native void setControllerState( int controllerNum, boolean[] buttons, int axisX, int axisY );   
    
    public static native void setControllerConfig( int controllerNum, boolean plugged, int pakType );   
    
    public static void rumble( int controllerNum, boolean active )
    {
        if( sVibrators[controllerNum] == null )
            return;
        
        if( active )
            sVibrators[controllerNum].vibrate( VIBRATE_TIMEOUT );
        else
            sVibrators[controllerNum].cancel();
    }    
    
    // ************************************************************************
    // ************************************************************************
    // Java <-> C communication (ae-bridge)
    // ************************************************************************
    // ************************************************************************
    
    //-------------------------------------------------------------------------
    // Call-outs made TO native code
    // jni/ae-bridge/ae_bridge_main.cpp
    //-------------------------------------------------------------------------
    
    public static native void sdlInit();
    
    //-------------------------------------------------------------------------
    // Call-outs made TO native code
    // jni/ae-bridge/ae_exports.cpp
    //-------------------------------------------------------------------------
    
    public static native void sdlOnResize( int x, int y, int format );
    
    public static native void sdlQuit();

    public static native void sdlRunAudioThread();

    public static native boolean sdlVersionAtLeast( int major, int minor, int patch );

    public static native void emuGameShark( boolean pressed );
    
    public static native void emuPause();
    
    public static native void emuResume();
    
    public static native void emuReset();
    
    public static native void emuStop();
    
    public static native void emuAdvanceFrame();
    
    public static native void emuSetSpeed( int percent );
    
    public static native void emuSetSlot( int slotID );
    
    public static native void emuLoadSlot();
    
    public static native void emuSaveSlot();
    
    public static native void emuLoadFile( String filename );
    
    public static native void emuSaveFile( String filename );
    
    public static native int emuGetState();
    
    public static native String getHeaderName( String filename );

    public static native String getHeaderCRC( String filename );
    
    //-------------------------------------------------------------------------
    // Call-ins made FROM native code
    // jni/ae-bridge/ae_imports.cpp
    //-------------------------------------------------------------------------

    public static void stateCallback( int paramChanged, int newValue )
    {
        synchronized( sStateCallbackLock )
        {
            if( sStateCallbackListener != null )
                sStateCallbackListener.onStateCallback( paramChanged, newValue );
            }
        }
    
    public static int getHardwareType()
    {
        int autoDetected = sAppData.hardwareInfo.hardwareType;
        int overridden = sUserPrefs.videoHardwareType;
        return overridden < 0 ? autoDetected : overridden;
    }
    
    public static Object getDataDir()
    {
        return sAppData.dataDir;
    }
    
    public static Object getROMPath()
    {
        String selectedGame = sUserPrefs.selectedGame;
        boolean isSelectedGameNull = selectedGame == null || !( new File( selectedGame ) ).exists();
        boolean isSelectedGameZipped = !isSelectedGameNull && selectedGame.length() >= 5
                && selectedGame.toLowerCase( Locale.US ).endsWith( ".zip" );
        
        if( sActivity == null )
            return null;
        
        if( isSelectedGameNull )
        {
            SafeMethods.exit( "Invalid ROM", sActivity, 2000 );
        }
        else if( isSelectedGameZipped )
        {
            // Create the temp folder if it doesn't exist:
            String tmpFolderName = sAppData.dataDir + "/tmp";
            File tmpFolder = new File( tmpFolderName );
            tmpFolder.mkdir();
            
            // Clear the folder if anything is in there:
            String[] children = tmpFolder.list();
            for( String child : children )
            {
                FileUtil.deleteFolder( new File( tmpFolder, child ) );
            }
            
            // Unzip the ROM
            String selectedGameUnzipped = Utility.unzipFirstROM( new File( selectedGame ), tmpFolderName );
            if( selectedGameUnzipped == null )
            {
                Log.v( "CoreInterface", "Cannot play zipped ROM: '" + selectedGame + "'" );
                
                Notifier.clear();
                
                if( ErrorLogger.hasError() )
                    ErrorLogger.putLastError( "OPEN_ROM", "fail_crash" );
                
                // Kick back out to the main menu
                sActivity.finish();
            }
            else
            {
                return selectedGameUnzipped;
            }
        }
        return selectedGame;
    }
    
    public static Object getExtraArgs()
    {
        String extraArgs = sUserPrefs.isFramelimiterEnabled ? "" : "--nospeedlimit ";
        if( sCheatOptions != null )
            extraArgs += sCheatOptions;
        return extraArgs.trim();
    }
    
    public static boolean getAutoFrameSkip()
    {
        return sUserPrefs.isGles2N64AutoFrameskipEnabled;
    }
    
    public static int getMaxFrameSkip()
    {
        return sUserPrefs.gles2N64MaxFrameskip;
    }
    
    public static int getScreenPosition()
    {
        return sUserPrefs.videoPosition;
    }
    
    public static boolean getScreenStretch()
    {
        return sUserPrefs.isStretched;
    }
    
    public static boolean useRGBA8888()
    {
        return sUserPrefs.isRgba8888;
    }
    
    // ************************************************************************
    // ************************************************************************
    // Java <-> C communication (SDL)
    // ************************************************************************
    // ************************************************************************
    
    //-------------------------------------------------------------------------
    // Call-ins made FROM native code
    // jni/SDL/src/core/android/SDL_android.cpp
    //-------------------------------------------------------------------------
    
    public static boolean createGLContext( int majorVersion, int minorVersion )
    {
        // SDL 1.3
        return sSurface.createGLContext( majorVersion, minorVersion );
    }
    
    public static boolean createGLContext( int majorVersion, int minorVersion, int[] configSpec )
    {
        // SDL 2.0
        return sSurface.createGLContext( majorVersion, minorVersion, configSpec );
    }
    
    public static void flipBuffers()
    {
        sSurface.flipBuffers();
        
        // Update frame rate info
        if( sFpsRecalcPeriod > 0 && sFpsListener != null )
        {
            sFrameCount++;
            if( sFrameCount >= sFpsRecalcPeriod )
            {
                long currentTime = System.currentTimeMillis();
                float fFPS = ( (float) sFrameCount / (float) ( currentTime - sLastFpsTime ) ) * 1000.0f;
                sFpsListener.onFpsChanged( Math.round( fFPS ) );
                sFrameCount = 0;
                sLastFpsTime = currentTime;
            }
        }
    }
    
    public static void audioInit(int sampleRate, boolean is16Bit, boolean isStereo, int desiredFrames) {
        int channelConfig = isStereo ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
        int audioFormat = is16Bit ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
        int frameSize = (isStereo ? 2 : 1) * (is16Bit ? 2 : 1);
        
        Log.v("SDL", "SDL audio: wanted " + (isStereo ? "stereo" : "mono") + " " + (is16Bit ? "16-bit" : "8-bit") + " " + ((float)sampleRate / 1000f) + "kHz, " + desiredFrames + " frames buffer");
        
        // Let the user pick a larger buffer if they really want -- but ye
        // gods they probably shouldn't, the minimums are horrifyingly high
        // latency already
        desiredFrames = Math.max(desiredFrames, (AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat) + frameSize - 1) / frameSize);
        
        sAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                channelConfig, audioFormat, desiredFrames * frameSize, AudioTrack.MODE_STREAM);
        
        audioStartThread();
        
        Log.v("SDL", "SDL audio: got " + ((sAudioTrack.getChannelCount() >= 2) ? "stereo" : "mono") + " " + ((sAudioTrack.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT) ? "16-bit" : "8-bit") + " " + ((float)sAudioTrack.getSampleRate() / 1000f) + "kHz, " + desiredFrames + " frames buffer");
    }
    
    public static void audioWriteShortBuffer(short[] buffer) {
        for (int i = 0; i < buffer.length; ) {
            int result = sAudioTrack.write(buffer, i, buffer.length - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch(InterruptedException e) {
                    // Nom nom
                }
            } else {
                Log.w("SDL", "SDL audio: error return from write(short)");
                return;
            }
        }
    }
    
    public static void audioWriteByteBuffer(byte[] buffer) {
        for (int i = 0; i < buffer.length; ) {
            int result = sAudioTrack.write(buffer, i, buffer.length - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch(InterruptedException e) {
                    // Nom nom
                }
            } else {
                Log.w("SDL", "SDL audio: error return from write(short)");
                return;
            }
        }
    }

    public static void audioQuit() {
        if (sAudioThread != null) {
            try {
                sAudioThread.join();
            } catch(InterruptedException e) {
                Log.v("SDL", "Problem stopping audio thread: " + e);
            }
            sAudioThread = null;

            //Log.v("SDL", "Finished waiting for audio thread");
        }

        if (sAudioTrack != null) {
            sAudioTrack.stop();
            sAudioTrack = null;
        }
    }
    
    public static void audioStartThread() {
        sAudioThread = new Thread(new Runnable() {
            public void run() {
                sAudioTrack.play();
                sdlRunAudioThread();
        }
        });
    
        // I'd take REALTIME if I could get it!
        sAudioThread.setPriority(Thread.MAX_PRIORITY);
        sAudioThread.start();
    }
    
    public static void runOnUiThread( Runnable action )
    {
        if( sActivity != null )
            sActivity.runOnUiThread( action );
    }
    
    public static void setActivityTitle( String title )
    {
        // No-op interface to guarantee compatibility with all SDL versions
        // TODO: Probably not necessary since SDL 2.0 checks whether this function exists before calling it...
    }
}
