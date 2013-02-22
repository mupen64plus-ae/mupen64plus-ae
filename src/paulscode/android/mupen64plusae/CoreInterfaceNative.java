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

import paulscode.android.mupen64plusae.util.ErrorLogger;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.SafeMethods;
import paulscode.android.mupen64plusae.util.Utility;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * The portion of the core interface that directly bridges Java and C code. Any function names
 * changed here should also be changed in the corresponding C code, and vice versa.
 * 
 * @see CoreInterface
 */
public class CoreInterfaceNative extends CoreInterface
{
    // *************************************************
    // *************************************************
    // *************************************************
    // Call-outs made TO the native code
    // See jni/front-end/src/main.c
    //     jni/input-sdl/src/plugin.c
    //     jni/SDL/src/core/android/SDL_android.cpp
    //     jni/SDL/src/main/android/SDL_android_main.cpp
    // *************************************************
    // *************************************************
    // *************************************************
    
    // TODO: These should all have javadoc comments. 
    // It would better document calls going in/out of native code.
    
    public static native void init();
    
    public static native void fileLoadEmulator( String filename );
    
    public static native void fileSaveEmulator( String filename );
    
    public static native String getHeaderCRC( String filename );
    
    public static native String getHeaderName( String filename );
    
    public static native void onAccel( float x, float y, float z );
    
    public static native void onResize( int x, int y, int format );
    
    public static native void pauseEmulator();
    
    public static native void quit();
    
    public static native void resetEmulator();
    
    public static native void resumeEmulator();
    
    public static native void runAudioThread();
    
    public static native int stateEmulator();
    
    public static native void stateLoadEmulator();
    
    public static native void stateSaveEmulator();
    
    public static native void stateSetSlotEmulator( int slotID );
    
    public static native void stopEmulator();
    
    public static native void updateVirtualGamePadStates( int controllerNum, boolean[] buttons,
            int axisX, int axisY );

    public static native void stateSetSpeed( int percent );
    
    // ********************************************
    // ********************************************
    // ********************************************
    // Call-ins made FROM the native code
    // See jni/SDL/src/core/android/SDL_android.cpp
    // ********************************************
    // ********************************************
    // ********************************************
    
    public static boolean createGLContext( int majorVersion, int minorVersion )
    {
        return sSurface.createGLContext( majorVersion, minorVersion );
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
    
    public static boolean getAutoFrameSkip()
    {
        return sUserPrefs.isGles2N64AutoFrameskipEnabled;
    }
    
    public static int getMaxFrameSkip()
    {
        return sUserPrefs.gles2N64MaxFrameskip;
    }
    
    public static boolean getScreenStretch()
    {
        return sUserPrefs.isStretched;
    }
    
    public static int getScreenPosition()
    {
        return sUserPrefs.videoPosition;
    }
    
    public static boolean useRGBA8888()
    {
        return sUserPrefs.isRgba8888;
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
        boolean isSelectedGameZipped = !isSelectedGameNull && selectedGame.length() > 3
                && selectedGame.substring( selectedGame.length() - 3, selectedGame.length() ).equalsIgnoreCase( "zip" );
        
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
    
    /**
     * Constructs any extra parameters to pass to the front-end, based on user preferences
     * 
     * @return Object handle to String containing space-separated parameters.
     */
    public static Object getExtraArgs()
    {
        String extraArgs = sUserPrefs.isFramelimiterEnabled ? "" : "--nospeedlimit ";
        if( sCheatOptions != null )
            extraArgs += sCheatOptions;
        return extraArgs.trim();
    }
    
    public static Object audioInit( int sampleRate, boolean is16Bit, boolean isStereo, int desiredFrames )
    {
        // Be sure audio is stopped so that we can restart it
        audioQuit();
        
        // Audio configuration
        int channelConfig = isStereo ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
        int audioFormat = is16Bit ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
        int frameSize = ( isStereo ? 2 : 1 ) * ( is16Bit ? 2 : 1 );
        
        // Let the user pick a larger buffer if they really want -- but ye gods they probably
        // shouldn't, the minimums are horrifyingly high latency already
        int minBufSize = AudioTrack.getMinBufferSize( sampleRate, channelConfig, audioFormat );
        int defaultFrames = ( minBufSize + frameSize - 1 ) / frameSize;
        desiredFrames = Math.max( desiredFrames, defaultFrames );
        
        sAudioTrack = new AudioTrack( AudioManager.STREAM_MUSIC, sampleRate, channelConfig, audioFormat, desiredFrames
                * frameSize, AudioTrack.MODE_STREAM );
        
        // if( sAudioThread == null )
        assert ( sAudioThread == null );
        {
            sAudioThread = new Thread( new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        sAudioTrack.play();
                        runAudioThread();
                    }
                    catch( IllegalStateException ise )
                    {
                        Log.e( "CoreInterface", "audioStartThread IllegalStateException", ise );
                    }
                }
            }, "Audio Thread" );
            
            sAudioThread.setPriority( Thread.MAX_PRIORITY );
            sAudioThread.start();
        }
        
        int bufSize = desiredFrames * ( isStereo ? 2 : 1 );
        sAudioBuffer = is16Bit ? new short[bufSize] : new byte[bufSize];
        return sAudioBuffer;
    }
    
    public static void audioQuit()
    {
        if( sAudioThread != null )
        {
            try
            {
                sAudioThread.join();
            }
            catch( Exception e )
            {
                Log.v( "CoreInterface", "Problem stopping audio thread: " + e );
            }
            sAudioThread = null;
        }
        
        if( sAudioTrack != null )
        {
            sAudioTrack.stop();
            sAudioTrack = null;
        }
    }
    
    public static void audioWriteByteBuffer( byte[] buffer )
    {
        for( int i = 0; i < buffer.length; )
        {
            int result = sAudioTrack.write( buffer, i, buffer.length - i );
            if( result > 0 )
            {
                i += result;
            }
            else if( result == 0 )
            {
                SafeMethods.sleep( 1 );
            }
            else
            {
                Log.w( "CoreInterface", "SDL Audio: Error returned from write(byte[])" );
                return;
            }
        }
    }
    
    public static void audioWriteShortBuffer( short[] buffer )
    {
        for( int i = 0; i < buffer.length; )
        {
            int result = sAudioTrack.write( buffer, i, buffer.length - i );
            if( result > 0 )
            {
                i += result;
            }
            else if( result == 0 )
            {
                SafeMethods.sleep( 1 );
            }
            else
            {
                Log.w( "CoreInterface", "SDL Audio: Error returned from write(short[])" );
                return;
            }
        }
    }
    
    public static void stateCallback( int paramChanged, int newValue )
    {
        synchronized( sStateCallbackLock )
        {
            if( sStateCallbackListener != null )
                sStateCallbackListener.onStateCallback( paramChanged, newValue );
        }
    }
    
    public static void showToast( String message )
    {
        if( sActivity != null )
            Notifier.showToast( sActivity, message );
    }
    
    public static void vibrate( boolean active )
    {
        if( sVibrator == null )
            return;
        if( active )
            sVibrator.vibrate( VIBRATE_PATTERN, 0 );
        else
            sVibrator.cancel();
    }
    
    public static void runOnUiThread( Runnable action )
    {
        if( sActivity != null )
            sActivity.runOnUiThread( action );
    }
    
    public static void setActivityTitle( String title )
    {
        Handler commandHandler = new Handler()
        {
            @Override
            public void handleMessage( Message msg )
            {
                if( msg.arg1 == COMMAND_CHANGE_TITLE )
                {
                    sActivity.setTitle( (CharSequence) msg.obj );
                }
            }
        };
        
        Message msg = commandHandler.obtainMessage();
        msg.arg1 = COMMAND_CHANGE_TITLE;
        msg.obj = title;
        commandHandler.sendMessage( msg );
    }
}
