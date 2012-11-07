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
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae;

import java.io.File;

import paulscode.android.mupen64plusae.input.transform.InputMap;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.Paths;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.ErrorLogger;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.SafeMethods;
import paulscode.android.mupen64plusae.util.Utility;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;

/**
 * A class that consolidates all interactions with the emulator core.
 * <p/>
 * It uses a simple startup/shutdown semantic to ensure all objects are properly synchronized before
 * the core launches. This is much cleaner and safer than using public static fields (i.e. globals),
 * since client code need not know how and when to update each global object.
 * 
 * @see NativeMethods
 */
public class CoreInterface
{
    // TODO: Eliminate or encapsulate
    // This is used by getRomPath() and queried in class SDLSurface
    public static boolean finishedReading = false;
    
    // Public constants
    public static final int EMULATOR_STATE_UNKNOWN = 0;
    public static final int EMULATOR_STATE_STOPPED = 1;
    public static final int EMULATOR_STATE_RUNNING = 2;
    public static final int EMULATOR_STATE_PAUSED = 3;
    
    // Private constants
    private static final long[] VIBRATE_PATTERN = { 0, 500, 0 };
    private static int COMMAND_CHANGE_TITLE = 1;
    
    // Internals
    private static Activity sActivity = null;
    private static SDLSurface sSurface;
    private static Vibrator sVibrator = null;
    private static Thread sAudioThread = null;
    private static AudioTrack sAudioTrack = null;
    private static Object sAudioBuffer;
    private static String sExtraArgs = ".";
    
    public static void startup( Activity activity, SDLSurface surface, Vibrator vibrator )
    {
        sActivity = activity;
        sSurface = surface;
        sVibrator = vibrator;
        syncConfigFiles( Globals.userPrefs, Globals.paths );
    }
    
    public static void shutdown()
    {
        // Actually, this might be a bad idea if other threads are still using these
        // sActivity = null;
        // sSurface = null;
        // sVibrator = null;
        // sAudioThread = null;
        // sAudioTrack = null;
        // sAudioBuffer = null;
    }
    
    public static String getExtraArgs()
    {
        return sExtraArgs;
    }
    
    public static void setExtraArgs( String extraArgs )
    {
        sExtraArgs = extraArgs;
    }
    
    public static boolean initEGL( int majorVersion, int minorVersion )
    {
        return sSurface.initEGL( majorVersion, minorVersion );
    }
    
    public static void flipEGL()
    {
        sSurface.flipEGL();
    }
    
    public static Object getRomPath()
    {
        String selectedGame = Globals.userPrefs.selectedGame;
        boolean isSelectedGameNull = selectedGame == null || !( new File( selectedGame ) ).exists();
        boolean isSelectedGameZipped = !isSelectedGameNull
                && selectedGame.length() > 3
                && selectedGame.substring( selectedGame.length() - 3, selectedGame.length() )
                        .equalsIgnoreCase( "zip" );
        
        if( sActivity == null )
            return null;
        
        finishedReading = false;
        if( isSelectedGameNull )
        {
            finishedReading = true;
            SafeMethods.exit( "Invalid ROM", sActivity, 2000 );
        }
        else if( isSelectedGameZipped )
        {
            // Create the temp folder if it doesn't exist:
            String tmpFolderName = Globals.paths.dataDir + "/tmp";
            File tmpFolder = new File( tmpFolderName );
            tmpFolder.mkdir();
            
            // Clear the folder if anything is in there:
            String[] children = tmpFolder.list();
            for( String child : children )
            {
                FileUtil.deleteFolder( new File( tmpFolder, child ) );
            }
            
            // Unzip the ROM
            Paths.tmpFile = Utility.unzipFirstROM( new File( selectedGame ), tmpFolderName );
            if( Paths.tmpFile == null )
            {
                Log.v( "CoreInterface", "Cannot play zipped ROM: '" + selectedGame + "'" );
                
                Notifier.clear();
                
                if( ErrorLogger.hasError() )
                    ErrorLogger.putLastError( "OPEN_ROM", "fail_crash" );
                
                finishedReading = true;
                
                // Kick back out to the main menu
                sActivity.finish();
            }
            else
            {
                finishedReading = true;
                return (Object) Paths.tmpFile;
            }
        }
        finishedReading = true;
        return (Object) selectedGame;
    }
    
    public static void runOnUiThread( Runnable action )
    {
        if( sActivity != null )
            sActivity.runOnUiThread( action );
    }
    
    public static void setActivityTitle( String title )
    {
        sendCommand( COMMAND_CHANGE_TITLE, title );
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
    
    public static Object audioInit( int sampleRate, boolean is16Bit, boolean isStereo,
            int desiredFrames )
    {
        int channelConfig = isStereo
                ? AudioFormat.CHANNEL_OUT_STEREO
                : AudioFormat.CHANNEL_OUT_MONO;
        int audioFormat = is16Bit
                ? AudioFormat.ENCODING_PCM_16BIT
                : AudioFormat.ENCODING_PCM_8BIT;
        int frameSize = ( isStereo
                ? 2
                : 1 ) * ( is16Bit
                ? 2
                : 1 );
        
        // Let the user pick a larger buffer if they really want -- but ye
        // gods they probably shouldn't, the minimums are horrifyingly high
        // latency already
        desiredFrames = Math
                .max( desiredFrames,
                        ( AudioTrack.getMinBufferSize( sampleRate, channelConfig, audioFormat )
                                + frameSize - 1 )
                                / frameSize );
        
        sAudioTrack = new AudioTrack( AudioManager.STREAM_MUSIC, sampleRate, channelConfig,
                audioFormat, desiredFrames * frameSize, AudioTrack.MODE_STREAM );
        
        audioStartThread();
        
        if( is16Bit )
        {
            sAudioBuffer = new short[desiredFrames * ( isStereo
                    ? 2
                    : 1 )];
        }
        else
        {
            sAudioBuffer = new byte[desiredFrames * ( isStereo
                    ? 2
                    : 1 )];
        }
        return sAudioBuffer;
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
                try
                {
                    Thread.sleep( 1 );
                }
                catch( InterruptedException e )
                {
                    // Nom nom
                }
            }
            else
            {
                Log.w( "CoreInterface", "SDL Audio: Error returned from write(short)" );
                return;
            }
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
                try
                {
                    Thread.sleep( 1 );
                }
                catch( InterruptedException e )
                {
                    // Nom nom
                }
            }
            else
            {
                Log.w( "CoreInterface", "SDL Audio: Error returned from write(byte)" );
                return;
            }
        }
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
            
            // Log.v("CoreInterface", "Finished waiting for audio thread");
        }
        
        if( sAudioTrack != null )
        {
            sAudioTrack.stop();
            sAudioTrack = null;
        }
    }
    
    private static void audioStartThread()
    {
        sAudioThread = new Thread( new Runnable()
        {
            public void run()
            {
                try
                {
                    sAudioTrack.play();
                    NativeMethods.runAudioThread();
                }
                catch( IllegalStateException ise )
                {
                    Log.e( "CoreInterface", "audioStartThread IllegalStateException", ise );
                }
            }
        }, "Audio Thread" );
        
        // I'd take REALTIME if I could get it!
        sAudioThread.setPriority( Thread.MAX_PRIORITY );
        sAudioThread.start();
    }
    
    /**
     * Populates the core configuration files with the user preferences.
     */
    private static void syncConfigFiles( UserPrefs user, Paths paths )
    {
        //@formatter:off
        
        // Core and GLES2RICE config file
        ConfigFile mupen64plus_cfg = new ConfigFile( paths.mupen64plus_cfg );
        mupen64plus_cfg.put( "Core", "Version", "1.00" );
        mupen64plus_cfg.put( "Core", "OnScreenDisplay", "True" ); // TODO: Should this be false?
        mupen64plus_cfg.put( "Core", "R4300Emulator", "2" );
        mupen64plus_cfg.put( "Core", "NoCompiledJump", "False" );
        mupen64plus_cfg.put( "Core", "DisableExtraMem", "False" );
        mupen64plus_cfg.put( "Core", "AutoStateSlotIncrement", "False" );
        mupen64plus_cfg.put( "Core", "EnableDebugger", "False" );
        mupen64plus_cfg.put( "Core", "CurrentStateSlot", "0" );
        mupen64plus_cfg.put( "Core", "ScreenshotPath", "\"\"" );
        mupen64plus_cfg.put( "Core", "SaveStatePath", "\"\"" ); // TODO: Consider using user preference
        mupen64plus_cfg.put( "Core", "SharedDataPath", "\"\"" );

        mupen64plus_cfg.put( "CoreEvents", "Version", "1.00" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Stop", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Fullscreen", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Save State", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Load State", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Increment Slot", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Reset", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Speed Down", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Speed Up", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Screenshot", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Pause", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Mute", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Increase Volume", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Decrease Volume", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Fast Forward", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Frame Advance", "0" );
        mupen64plus_cfg.put( "CoreEvents", "Kbd Mapping Gameshark", "0" );

        mupen64plus_cfg.put( "Audio-SDL", "Version", "1.00" );
        mupen64plus_cfg.put( "UI-Console", "Version", "1.00" );
        mupen64plus_cfg.put( "UI-Console", "PluginDir", '"' + paths.libsDir + '"' );
        mupen64plus_cfg.put( "UI-Console", "VideoPlugin", '"' + user.videoPlugin + '"' );
        mupen64plus_cfg.put( "UI-Console", "AudioPlugin", '"' + user.audioPlugin + '"' );
        mupen64plus_cfg.put( "UI-Console", "InputPlugin", '"' + user.inputPlugin + '"' );
        mupen64plus_cfg.put( "UI-Console", "RspPlugin", '"' + user.rspPlugin + '"' );

        mupen64plus_cfg.put( "Video-General", "Version", "1.00" );
        mupen64plus_cfg.put( "Video-Rice", "Version", "1.00" );
        mupen64plus_cfg.put( "Video-Rice", "SkipFrame", booleanToString( user.isGles2RiceAutoFrameskipEnabled ) );
        mupen64plus_cfg.put( "Video-Rice", "FastTextureLoading", booleanToString( user.isGles2RiceFastTextureLoadingEnabled ) );
        mupen64plus_cfg.put( "Video-Rice", "FastTextureCRC", booleanToString( user.isGles2RiceFastTextureCrcEnabled ) );
        mupen64plus_cfg.put( "Video-Rice", "LoadHiResTextures", booleanToString( user.isGles2RiceHiResTexturesEnabled ) );
        
        syncConfigFileInputs( mupen64plus_cfg, user.inputMap1, 1);
        syncConfigFileInputs( mupen64plus_cfg, user.inputMap2, 2);
        syncConfigFileInputs( mupen64plus_cfg, user.inputMap3, 3);
        syncConfigFileInputs( mupen64plus_cfg, user.inputMap4, 4);

        mupen64plus_cfg.save();
        
        // GLES2N64 config file
        ConfigFile gles2n64_conf = new ConfigFile( paths.gles2n64_conf );
        gles2n64_conf.put( "[<sectionless!>]", "enable fog", booleanToString( user.isGles2N64FogEnabled ) );
        gles2n64_conf.put( "[<sectionless!>]", "enable alpha test", booleanToString( user.isGles2N64AlphaTestEnabled ) );
        gles2n64_conf.put( "[<sectionless!>]", "force screen clear", booleanToString( user.isGles2N64ScreenClearEnabled ) );
        gles2n64_conf.put( "[<sectionless!>]", "hack z", booleanToString( !user.isGles2N64DepthTestEnabled ) );
        gles2n64_conf.save();        
        //@formatter:on
    }
    
    private static void syncConfigFileInputs( ConfigFile mupen64plus_cfg, InputMap map,
            int playerNumber )
    {
        String sectionTitle = "Input-SDL-Control" + playerNumber;
        
        mupen64plus_cfg.put( sectionTitle, "Version", "1.00" );
        mupen64plus_cfg.put( sectionTitle, "plugged", map.isEnabled() ? "True" : "False" );
        mupen64plus_cfg.put( sectionTitle, "plugin", "2" );
        mupen64plus_cfg.put( sectionTitle, "device", "-2" );
        mupen64plus_cfg.put( sectionTitle, "mouse", "False" );
        mupen64plus_cfg.put( sectionTitle, "DPad R", "key(0)" );
        mupen64plus_cfg.put( sectionTitle, "DPad L", "key(0)" );
        mupen64plus_cfg.put( sectionTitle, "DPad D", "key(0)" );
        mupen64plus_cfg.put( sectionTitle, "DPad U", "key(0)" );
        mupen64plus_cfg.put( sectionTitle, "Start", "key(0)" );
        mupen64plus_cfg.put( sectionTitle, "Z Trig", "key(0)" );
        mupen64plus_cfg.put( sectionTitle, "B Button", "key(0)" );
        mupen64plus_cfg.put( sectionTitle, "A Button", "key(0)" );
        mupen64plus_cfg.put( sectionTitle, "C Button R", "key(0)" );
        mupen64plus_cfg.put( sectionTitle, "C Button L", "key(0)" );
        mupen64plus_cfg.put( sectionTitle, "C Button D", "key(0)" );
        mupen64plus_cfg.put( sectionTitle, "C Button U", "key(0)" );
        mupen64plus_cfg.put( sectionTitle, "R Trig", "key(0)" );
        mupen64plus_cfg.put( sectionTitle, "L Trig", "key(0)" );
        mupen64plus_cfg.put( sectionTitle, "Mempak switch", "key(0)" );
        mupen64plus_cfg.put( sectionTitle, "Rumblepak switch", "key(0)" );
        mupen64plus_cfg.put( sectionTitle, "X Axis", "key(0,0)" );
        mupen64plus_cfg.put( sectionTitle, "Y Axis", "key(0,0)" );
    }
    
    private static String booleanToString( boolean b )
    {
        return b ? "1" : "0";
    }
    
    private static Handler commandHandler = new Handler()
    {
        public void handleMessage( Message msg )
        {
            if( msg.arg1 == COMMAND_CHANGE_TITLE )
            {
                sActivity.setTitle( (String) msg.obj );
            }
        }
    };
    
    private static void sendCommand( int command, Object data )
    {
        Message msg = commandHandler.obtainMessage();
        msg.arg1 = command;
        msg.obj = data;
        commandHandler.sendMessage( msg );
    }
}
