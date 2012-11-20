package paulscode.android.mupen64plusae;

import java.io.File; 
import java.io.IOException;
import java.io.InputStream;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.Toast;

/**
 * The GameActivityCommon class provides access to interfaces that are common to both
 * GameActivity and GameActivityXperiaPlay
 */
public class GameActivityCommon
{
    // Menu items:
    public static final int MAIN_MENU_ITEM = Menu.FIRST;
    public static final int SLOT_MENU_ITEM = MAIN_MENU_ITEM + 1;
    public static final int SAVE_MENU_ITEM = SLOT_MENU_ITEM + 1;
    public static final int LOAD_MENU_ITEM = SAVE_MENU_ITEM + 1;
    public static final int CLOSE_MENU_ITEM = LOAD_MENU_ITEM + 1;

    // Used by Main thread to call back a title change:
    public static final int COMMAND_CHANGE_TITLE = 1;

    // Emulator states:
    public static final int EMULATOR_STATE_UNKNOWN = 0;
    public static final int EMULATOR_STATE_STOPPED = 1;
    public static final int EMULATOR_STATE_RUNNING = 2;
    public static final int EMULATOR_STATE_PAUSED = 3;

    // Statusbar notifications:
    public static NotificationManager notificationManager = null;

    // Activity handle to either gameActivity or gameActivityXperiaPlay:
    public static Activity mSingleton = null;
    // Only one of these two should ever be instantiated:
    public static GameActivity gameActivity = null;
    public static GameActivityXperiaPlay gameActivityXperiaPlay = null;

    // FPS:
    private static int frameCount = -1;
    private static int fpsRate = 15;
    private static long lastFPSCheck = 0;

    // Audio:
    private static Thread mAudioThread = null;
    private static AudioTrack mAudioTrack = null;

    // Vibrator:
    private static long[] vibratePattern = { 0, 500, 0 };
    public static Vibrator mVibrate = null;

    // Rendering surface:
    public static SDLSurface mSurface = null;

    // Virtual gamepad
    public static GamePad mGamePad = null;
    public static GamePad.GamePadListing mGamePadListing = null;
    public static int whichPad = 0;
    public static boolean[] previousKeyStates = new boolean[ GamePad.MAX_BUTTONS ];

    // Color mode:
    public static boolean rgba8888 = false;

    // Path name to a temporary file if it exists (used to temporarily decompress a zipped ROM):
    public static String tmpFile = null;
    // Becomes true once the ROM data has been successfully read:
    public static boolean finishedReading = false;

    // User chose to resume their last game:
    public static boolean resumeLastSession = false;

    // True if there is no Input plug-in attached (for blocking input-related JNI calls to native):
    public static boolean noInputPlugin = false;

    // Toast Messages:
    private static Toast toast = null;
    private static Runnable toastMessager = null;



    /** BEGIN Native interface methods **/

    // Start things up on the native side:
    public static native void nativeInit();
    // Shut things down on the native side:
    public static native void nativeQuit();
    // Start the audio thread:
    public static native void nativeRunAudioThread();

    // Surface dimensions changed:
    public static native void onNativeResize( int x, int y, int format );

    // Accelerometer sensor changed:
    public static native void onNativeAccel( float x, float y, float z );

    // Native functions for reading ROM header info:
    public static native String nativeGetHeaderName( String filename );
    public static native String nativeGetHeaderCRC( String filename );

    // Input events:
    public static native void onNativeKeyDown( int keycode );     // Android keycodes
    public static native void onNativeKeyUp( int keycode );
    public static native void onNativeSDLKeyDown( int keycode );  // SDL scancodes (TODO: merge)
    public static native void onNativeSDLKeyUp( int keycode );
    public static native void onNativeTouch( int action, float x, float y, float p );

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

    /** END Native interface methods **/



    static
    {
        GameActivityCommon.loadNativeLibName( "SDL" );
        GameActivityCommon.loadNativeLibName( "core" );  // TODO: let the user choose which core to load
        GameActivityCommon.loadNativeLibName( "front-end" );
    }

    /**
     * Simulates key events for the SDLButtons on the virtual gamepad 
     * @param SDLButtonPressed Whether each button is pressed or not.
     * @param SDLButtonCodes Key code for each button.
     * @param SDLButtonCount Number of buttons.
     */
    public static void updateSDLButtonStates( boolean[] SDLButtonPressed, int[] SDLButtonCodes, int SDLButtonCount )
    {
        if( mSurface == null )
            return;
        
        for( int x = 0; x < SDLButtonCount; x++ )
        {
            if( SDLButtonPressed[x] != previousKeyStates[x] )
            {
                previousKeyStates[x] = SDLButtonPressed[x];
                if( SDLButtonPressed[x] )
                    mSurface.onSDLKey( SDLButtonCodes[x], KeyEvent.ACTION_DOWN );
                else
                    mSurface.onSDLKey( SDLButtonCodes[x], KeyEvent.ACTION_UP );
            }
        }
    }

    /**
     * Pops up a temporary message on the device 
     * @param message Message to display.
     */
    public static void showToast( String message )
    {
        if( mSingleton == null )
            return;  // Activity hasn't been created yet
        if( toast != null )
            toast.setText( message );  // Toast exists, just change the text
        else
        { // Message short in duration, and at the bottom of the screen:
            toast = Toast.makeText( mSingleton, message, Toast.LENGTH_SHORT );
            toast.setGravity( Gravity.BOTTOM, 0, 0 );
        }
        // Toast messages must be run on the UiThread, which looks ugly as hell, but works:
        if( toastMessager == null )  // Create it if it wasn't already
            toastMessager = new Runnable()
                            {
                                public void run()
                                {  // just show the toast:
                                    if( toast != null )
                                        toast.show();
                                }
                            };
        mSingleton.runOnUiThread( toastMessager );
    }

    public static boolean getScreenStretch()
    {
        return Globals.screen_stretch;
    }
    
    public static boolean getAutoFrameSkip()
    {
        return Globals.auto_frameskip;
    }
    
    public static int getMaxFrameSkip()
    {
        return Globals.max_frameskip;
    }
    

    // Audio
    private static Object buf;
    
    public static Object audioInit( int sampleRate, boolean is16Bit, boolean isStereo, int desiredFrames )
    {
        int channelConfig = isStereo ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
        int audioFormat = is16Bit ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
        int frameSize = (isStereo ? 2 : 1) * (is16Bit ? 2 : 1);
        
        // Let the user pick a larger buffer if they really want -- but ye
        // gods they probably shouldn't, the minimums are horrifyingly high
        // latency already
        desiredFrames = Math.max(desiredFrames, (AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat) + frameSize - 1) / frameSize);
        
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                channelConfig, audioFormat, desiredFrames * frameSize, AudioTrack.MODE_STREAM);
        
        audioStartThread();
        
        if (is16Bit) {
            buf = new short[desiredFrames * (isStereo ? 2 : 1)];
        } else {
            buf = new byte[desiredFrames * (isStereo ? 2 : 1)]; 
        }
        return buf;
    }
    
    public static void audioStartThread()
    {
        mAudioThread = new Thread(new Runnable() {
            public void run() {
                try
                {
                    mAudioTrack.play();
                    nativeRunAudioThread();
                }
                catch( IllegalStateException ise )
                {
                    Log.e( "GameActivityCommon", "audioStartThread IllegalStateException", ise );
                    if( mSingleton != null )
                        showToast( mSingleton.getString( R.string.illegal_audio_state ) );
                    else  // Static context, can't get the string in the correct locale, so just use English:
                        showToast( "Audio track illegal state.  Please report at paulscode.com" );
                }
            }
        });
        
        // I'd take REALTIME if I could get it!
        mAudioThread.setPriority( Thread.MAX_PRIORITY );
        mAudioThread.start();
    }
    
    public static void audioWriteShortBuffer( short[] buffer )
    {
        for (int i = 0; i < buffer.length; ) {
            int result = mAudioTrack.write(buffer, i, buffer.length - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch(InterruptedException e) {
                    // Nom nom
                }
            } else {
                Log.w( "GameActivityCommon", "SDL audio: error return from write(short)" );
                return;
            }
        }
    }
    
    public static void audioWriteByteBuffer( byte[] buffer )
    {
        for (int i = 0; i < buffer.length; ) {
            int result = mAudioTrack.write(buffer, i, buffer.length - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch(InterruptedException e) {
                    // Nom nom
                }
            } else {
                Log.w( "GameActivityCommon", "SDL audio: error return from write(short)" );
                return;
            }
        }
    }

    public static void audioQuit()
    {
        if (mAudioThread != null) {
            try {
                mAudioThread.join();
            } catch(Exception e) {
                Log.v( "GameActivityCommon", "Problem stopping audio thread: " + e );
            }
            mAudioThread = null;

            // Log.v("SDL", "Finished waiting for audio thread");
        }

        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack = null;
        }
    }
    
    public static void vibrate( boolean active )
    {
        if( mVibrate == null )
            return;
        if( active )
            mVibrate.vibrate( vibratePattern, 0 );
        else
            mVibrate.cancel();
    }

    public static boolean createGLContext(int majorVersion, int minorVersion)
    {
        return mSurface.initEGL(majorVersion, minorVersion);
    }
    
    public static boolean useRGBA8888()
    {
        return rgba8888;
    }

    public static void flipBuffers()
    {
        mSurface.flipEGL();
        if( frameCount < 0 )
        {
            frameCount = 0;
            lastFPSCheck = System.currentTimeMillis();
        }
        frameCount++;
        if( (mGamePad != null && frameCount >= mGamePad.fpsRate) ||
            (mGamePad == null && frameCount >= fpsRate) )
        {
            long currentTime = System.currentTimeMillis();
            float fFPS = ( (float) frameCount / (float) (currentTime - lastFPSCheck) ) * 1000.0f;
            if( mGamePad != null )
                mGamePad.updateFPS( (int) fFPS );
            frameCount = 0;
            lastFPSCheck = currentTime;
        }
    }

    public static void setActivityTitle( String title )
    {
        // Called from SDLMain() thread and can't directly affect the view
        if( Globals.isXperiaPlay )
            gameActivityXperiaPlay.sendCommand( COMMAND_CHANGE_TITLE, title );
        else
            gameActivity.sendCommand( COMMAND_CHANGE_TITLE, title );
    }

    public static Object getROMPath()
    {
        finishedReading = false;
        if( Globals.chosenROM == null || Globals.chosenROM.length() < 1 )
        {
            finishedReading = true;
            //return (Object) (Globals.DataDir + "/roms/mupen64plus.v64");
            System.exit( 0 );
        }
        else if( Globals.chosenROM.substring( Globals.chosenROM.length() - 3, Globals.chosenROM.length() ).equalsIgnoreCase( "zip" ) )
        {
            // Create the tmp folder if it doesn't exist:
            File tmpFolder = new File( Globals.DataDir + "/tmp" );
            tmpFolder.mkdir();
            // Clear the folder if anything is in there:
            String[] children = tmpFolder.list();
            for( String child : children )
            {
                Utility.deleteFolder( new File( tmpFolder, child ) );
            }
            tmpFile = Utility.unzipFirstROM( new File( Globals.chosenROM ), Globals.DataDir + "/tmp" );
            if( tmpFile == null )
            {
                Log.v( "GameActivityCommon", "Unable to play zipped ROM: '" + Globals.chosenROM + "'" );

                notificationManager.cancel( Globals.NOTIFICATION_ID );

                if( Globals.errorMessage != null )
                {
                    MenuActivity.error_log.put( "OPEN_ROM", "fail_crash", Globals.errorMessage );
                    MenuActivity.error_log.save();
                }
                Intent intent = new Intent( mSingleton, MenuActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                mSingleton.startActivity( intent );
                finishedReading = true;
                System.exit( 0 );
            }
            else
            {
                finishedReading = true;
                return (Object) tmpFile;
            }
        }
        else
        {
            finishedReading = true;
            return (Object) Globals.chosenROM;
        }
        finishedReading = true;
        return (Object) Globals.chosenROM;
    }

    public static Object getDataDir()
    {
        return (Object) Globals.DataDir;
    }
    
    public static int getHardwareType()
    {
        return Globals.hardwareType;
    }

    public static Object getExtraArgs()
    {
        if( Globals.extraArgs == null )
            return (Object) ".";
        return (Object) Globals.extraArgs;
    }

    public static void saveSession()
    {
        if( tmpFile != null )
        {
            try
            {
                new File( tmpFile ).delete();
            }
            catch( Exception e )
            {}
        }
        if( !Globals.auto_save )
            return;
        showToast( mSingleton.getString( R.string.saving_game ) );
        fileSaveEmulator( "Mupen64PlusAE_LastSession.sav" );
        try{Thread.sleep( 500 );}catch(InterruptedException e){}  // wait a bit
        int c = 0;
        int state = stateEmulator();
        while( state == EMULATOR_STATE_PAUSED && c < 120 )
        {  // it should be paused while saving the session.
            try{Thread.sleep( 500 );}catch(InterruptedException e){}
            state = stateEmulator();
            c++;
        }
        mSurface.buffFlipped = false;
        c = 0;
        while( !mSurface.buffFlipped )
        { // wait for the game to have resumed, as indicated
          // by a call to flip the EGL buffers.
            try{Thread.sleep( 20 );}catch(InterruptedException e){}
            c++;
        }
        try{ Thread.sleep( 40 ); }catch(InterruptedException e){}  // Just to be sure..
    }

    public static void readCpuInfo()
    {
        Log.v( "GameActivityCommon", "CPU info available from file /proc/cpuinfo:" );
        ProcessBuilder cmd;
        try
        {
            String[] args = { "/system/bin/cat", "/proc/cpuinfo" };
            cmd = new ProcessBuilder( args );
            java.lang.Process process = cmd.start();
            InputStream in = process.getInputStream();
            byte[] re = new byte[1024];
            String line;
            String[] lines;
            String[] splitLine;
            String processor = null;
            String features = null;
            String hardware = null;

            if( in.read( re ) != -1 )
            {
                line = new String( re );
                Log.v( "GameActivityCommon", line );
                lines = line.split( "\\r\\n|\\n|\\r" );
                if( lines != null )
                {
                    for( int x = 0; x < lines.length; x++ )
                    {
                        splitLine = lines[x].split( ":" );
                        if( splitLine != null && splitLine.length == 2 )
                        {
                            if( processor == null && splitLine[0].trim().toLowerCase().equals( "processor" ) )
                                processor = splitLine[1].trim().toLowerCase();
                            else if( features == null && splitLine[0].trim().toLowerCase().equals( "features" ) )
                                features = splitLine[1].trim().toLowerCase();
                            else if( hardware == null && splitLine[0].trim().toLowerCase().equals( "hardware" ) )
                                hardware = splitLine[1].trim().toLowerCase();
                        }
                    }
                }
            }
            if( hardware != null && (hardware.contains( "mapphone" ) ||
                                     hardware.contains( "tuna" )     ||
                                     hardware.contains( "smdkv" )    ||
                                     hardware.contains( "herring" )  ||
                                     hardware.contains( "aries" )) )
                Globals.hardwareType = Globals.HARDWARE_TYPE_OMAP;
            else if( hardware != null && (hardware.contains( "liberty" )  ||
                                          hardware.contains( "gt-s5830" ) ||
                                          hardware.contains( "zeus" )) )
                Globals.hardwareType = Globals.HARDWARE_TYPE_QUALCOMM;
            else if( hardware != null && hardware.contains( "imap" ))
                Globals.hardwareType = Globals.HARDWARE_TYPE_IMAP;
            else if( ( hardware != null && (hardware.contains( "tegra 2" )  ||
                                            hardware.contains( "grouper" )  ||
                                            hardware.contains( "meson-m1" ) ||
                                            hardware.contains( "smdkc" )) ) ||
                     ( features != null && features.contains( "vfpv3d16" )) )
                Globals.hardwareType = Globals.HARDWARE_TYPE_TEGRA2;
            in.close();
        }
        catch( IOException ioe )
        {
            ioe.printStackTrace();
        }
    }

    /**
     * Loads the native .so file specified
     * @param filepath Full path to a native .so file (may optionally be in quotes).
     */
    public static void loadNativeLib( String filepath )
    {
        String filename = null;
        if( filepath != null && filepath.length() > 0 )
        {
            filename = filepath.replace( "\"", "" );
            if( filename.equalsIgnoreCase( "dummy" ) )
                return;

            Log.v( "GameActivityCommon", "Loading native library '" + filename + "'" );
            try
            {
                System.load( filename );
            }
            catch( UnsatisfiedLinkError e )
            {
                Log.e( "GameActivityCommon", "Unable to load native library '" + filename + "'" );
            }
        }
    }

    /**
     * Loads the specified native library name (without "lib" and ".so")
     * @param filepath Full path to a native .so file (may optionally be in quotes).
     */
    public static void loadNativeLibName( String libname )
    {
        Log.v( "GameActivityCommon", "Loading native library '" + libname + "'" );
        try
        {
            System.loadLibrary( libname );
        }
        catch( UnsatisfiedLinkError e )
        {
            Log.e( "GameActivityCommon", "Unable to load native library '" + libname + "'" );
        }
    }
}
