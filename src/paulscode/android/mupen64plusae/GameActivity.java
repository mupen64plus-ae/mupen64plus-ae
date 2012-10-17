package paulscode.android.mupen64plusae;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import paulscode.android.mupen64plusae.persistent.Config;
import paulscode.android.mupen64plusae.persistent.Settings;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NativeActivity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * GameActivity
 */
@TargetApi( 9 )
public class GameActivity extends NativeActivity
{
    public static class GameState
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
    
        public static Vibrator mVibrate = null;
        
        // Activity handle to either gameActivity or gameActivityXperiaPlay:
        public static Activity mSingleton = null;
        
        public static GameActivity gameActivity = null;
        
        // Rendering surface:
        public static SDLSurface mSurface = null;
        
        // Virtual gamepad
        public static GamePad mGamePad = null;
        public static GamePad.GamePadListing mGamePadListing = null;
        public static int whichPad = 0;
        public static boolean[] previousKeyStates = new boolean[GamePad.MAX_BUTTONS];
        
        // Becomes true once the ROM data has been successfully read:
        public static boolean finishedReading = false;
        
        // User chose to resume their last game:
        public static boolean resumeLastSession = false;
        
        // True if there is no Input plug-in attached (for blocking input-related JNI calls to
        // native):
        public static boolean noInputPlugin = false;
        public static boolean inhibitSuspend = true;
        public static final int NOTIFICATION_ID = 10001;
    }

    public static int saveSlot = 0;
    
    // Setup
    @TargetApi( 11 )
    protected void onCreate( Bundle savedInstanceState )
    {
        // paulscode, place an icon into the status bar:
        if( GameState.notificationManager == null )
            GameState.notificationManager = (NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE );
        int statusIcon = R.drawable.status;
        CharSequence text = "Mupen64Plus AE is running";
        CharSequence contentTitle = "Mupen64Plus AE";
        CharSequence contentText = "Mupen64Plus AE";
        long when = System.currentTimeMillis();
        Context context = getApplicationContext();
        
        GameState.mVibrate = (Vibrator) getSystemService( Context.VIBRATOR_SERVICE );
        
        Intent intent = new Intent( this, MenuActivity.class );
        intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
        PendingIntent contentIntent = PendingIntent.getActivity( this, 0, intent, 0 );
        
        NotificationCompat.Builder notification = new NotificationCompat.Builder( context )
                .setSmallIcon( statusIcon ).setAutoCancel( true ).setTicker( text ).setWhen( when )
                .setContentTitle( contentTitle ).setContentText( contentText )
                .setContentIntent( contentIntent );
        
        GameState.notificationManager.notify( GameState.NOTIFICATION_ID, notification.build() );
        
        // paulscode, load the native libraries:
        if( Settings.user.xperiaEnabled )
        {
            loadNativeLibName( "xperia-touchpad" );
        }
        loadNativeLib( Settings.mupen64plus_cfg.get( "UI-Console", "VideoPlugin" ) );
        loadNativeLib( Settings.mupen64plus_cfg.get( "UI-Console", "AudioPlugin" ) );
        loadNativeLib( Settings.mupen64plus_cfg.get( "UI-Console", "InputPlugin" ) );
        loadNativeLib( Settings.mupen64plus_cfg.get( "UI-Console", "RspPlugin" ) );
        
        // paulscode, fix potential crash when input plug-in is disabled
        String inp = Settings.mupen64plus_cfg.get( "UI-Console", "InputPlugin" );
        if( inp != null )
        {
            inp = inp.replace( "\"", "" );
            if( inp.equalsIgnoreCase( "dummy" ) )
                GameState.noInputPlugin = true;
        }
        
        // paulscode, gathers information about the device, and chooses a hardware profile (used to
        // customize settings)
        readCpuInfo();
        
        // paulscode, Xperia Play native input linkage
        if( Settings.user.xperiaEnabled )
        {
            for( int x = 0; x < 256; x++ )
            {
                touchPadPointers[x] = false;
                touchPadPointerX[x] = -1;
                touchPadPointerY[x] = -1;
                touchScreenPointers[x] = false;
                touchScreenPointerX[x] = -1;
                touchScreenPointerY[x] = -1;
            }
        }
        
        // paulscode, clears the virtual gamepad key states
        for( int x = 0; x < 30; x++ )
        {
            GameState.previousKeyStates[x] = false;
        }
        
        super.onCreate( savedInstanceState );
        
        // Fullscreen mode
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
            getWindow().requestFeature( Window.FEATURE_ACTION_BAR_OVERLAY );
        else
            requestWindowFeature( Window.FEATURE_NO_TITLE );
        
        getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN );
        if( GameState.inhibitSuspend )
            getWindow().setFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );
        
        // So we can call stuff from static callbacks
        GameState.mSingleton = (Activity) this;
        GameState.gameActivity = this;
        
        if( Settings.user.xperiaEnabled )
            getWindow().takeSurface( null );
        getWindow().setContentView( R.layout.main );
        if( Settings.user.xperiaEnabled )
            RegisterThis();
        
        GameState.mSurface = (SDLSurface) findViewById( R.id.my_surface );
        
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
        {
            // SDK version at least HONEYCOMB, so there should be software buttons on this device:
            View mView = GameState.mSurface.getRootView();
            if( mView == null )
                Log.e( "GameActivityXperiaPlay", "getRootView() returned null in method onCreate" );
            else
                mView.setSystemUiVisibility( View.SYSTEM_UI_FLAG_LOW_PROFILE );
            getActionBar().hide();
        }
        GameState.mGamePad = (GamePad) findViewById( R.id.my_gamepad );
        GameState.mGamePad.setResources( getResources() );
        GameState.mGamePadListing = new GamePad.GamePadListing( Settings.path.dataDir
                + "/skins/gamepads/gamepad_list.ini" );
        
        String val;
        
        // Look up any special codes for the analog controls
        for( int p = 0; p < 4; p++ )
        {
            val = Settings.mupen64plus_cfg.get( "Input-SDL-Control" + ( p + 1 ), "plugged" );
            if( val == null )
            {
                if( new File( Settings.path.storageDir ).exists() )
                {
                    Settings.mupen64plus_cfg = new Config( Settings.path.mupen64plus_cfg );
                    val = Settings.mupen64plus_cfg.get( "Input-SDL-Control" + ( p + 1 ), "plugged" );
                }
                else
                {
                    Log.e( "GameActivityXperiaPlay",
                            "No access to storage, probably in USB Mass Storage mode" );
                    showToast( getString( R.string.app_data_inaccessible ) );
                }
            }
            
            if( val != null && val.equals( "True" ) )
            {
                val = Settings.mupen64plus_cfg.get( "Input-SDL-Control" + ( p + 1 ), "X Axis" );
                if( val != null )
                {
                    int x = val.indexOf( "(" );
                    int y = val.indexOf( ")" );
                    if( x >= 0 && y >= 0 && y > x )
                    {
                        val = val.substring( x + 1, y ).trim();
                        x = val.indexOf( "," );
                        if( x >= 0 )
                        {
                            Settings.ctrlr[p][0] = Utility.toInt(
                                    val.substring( x + 1, val.length() ), 0 );
                            Settings.ctrlr[p][1] = Utility.toInt( val.substring( 0, x ), 0 );
                        }
                    }
                    val = Settings.mupen64plus_cfg.get( "Input-SDL-Control" + ( p + 1 ), "Y Axis" );
                    x = val.indexOf( "(" );
                    y = val.indexOf( ")" );
                    if( x >= 0 && y >= 0 && y > x )
                    {
                        val = val.substring( x + 1, y ).trim();
                        x = val.indexOf( "," );
                        if( x >= 0 )
                        {
                            Settings.ctrlr[p][2] = Utility.toInt(
                                    val.substring( x + 1, val.length() ), 0 );
                            Settings.ctrlr[p][3] = Utility.toInt( val.substring( 0, x ), 0 );
                        }
                    }
                }
            }
        }
        
        GameState.mGamePad.loadPad();
        
        // paulscode, Xperia Play touchpad skins/layouts
        if( Settings.user.xperiaEnabled )
        {
            mTouchPad = new TouchPad( this, getResources() );
            mTouchPadListing = new TouchPad.TouchPadListing( Settings.path.touchpad_ini );
            mTouchPad.loadPad();
        }
        
        showToast( getString( R.string.mupen64plus_started ) );
        
    }
    
    // paulscode, add the menu options:
    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        // TODO: I think we can do this in an XML resource...
        saveSlot = 0;
        menu.add( 0, GameState.MAIN_MENU_ITEM, 0, getString( R.string.ingame_menu ) );
        menu.add( 0, GameState.SLOT_MENU_ITEM, 0, getString( R.string.ingame_inc_slot ) + " (0)" );
        menu.add( 0, GameState.SAVE_MENU_ITEM, 0, getString( R.string.ingame_save ) );
        menu.add( 0, GameState.LOAD_MENU_ITEM, 0, getString( R.string.ingame_load ) );
        return super.onCreateOptionsMenu( menu );
    }
    
    // paulscode, add the menu options:
    @Override
    public boolean onPrepareOptionsMenu( Menu menu )
    {
        // TODO: I think we can do this in an XML resource...
        menu.clear();
        menu.add( 0, GameState.MAIN_MENU_ITEM, 0, getString( R.string.ingame_menu ) );
        menu.add( 0, GameState.SLOT_MENU_ITEM, 0, getString( R.string.ingame_inc_slot ) + " ("
                + saveSlot + ")" );
        menu.add( 0, GameState.SAVE_MENU_ITEM, 0, getString( R.string.ingame_save ) );
        menu.add( 0, GameState.LOAD_MENU_ITEM, 0, getString( R.string.ingame_load ) );
        return super.onCreateOptionsMenu( menu );
    }
    
    // paulscode, add the menu options:
    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch( item.getItemId() )
        {
            case GameState.MAIN_MENU_ITEM:
                saveSession(); // Workaround, allows us to force-close later
                //
                GameState.notificationManager.cancel( GameState.NOTIFICATION_ID );
                Intent intent = new Intent( this, MenuActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                GameState.mSingleton = null;
                GameState.gameActivity = null;
                
                // this.finish(); // This causes menu to crash, why??
                System.exit( 0 ); // Workaround, force-close (what about SDL thread?)
                break;
            case GameState.SLOT_MENU_ITEM:
                saveSlot++;
                if( saveSlot > 9 )
                    saveSlot = 0;
                NativeMethods.stateSetSlotEmulator( saveSlot );
                showToast( getString( R.string.savegame_slot ) + " " + saveSlot );
                break;
            case GameState.SAVE_MENU_ITEM:
                NativeMethods.stateSaveEmulator();
                break;
            case GameState.LOAD_MENU_ITEM:
                NativeMethods.stateLoadEmulator();
                break;
            case GameState.CLOSE_MENU_ITEM:
                // notificationManager.cancel( Globals.NOTIFICATION_ID );
                // this.finish(); // This doesn't save; closes to quickly maybe?
                saveSession(); // Workaround, wait for fileSaveEmulator to finish
                                                  // first
                GameState.notificationManager.cancel( GameState.NOTIFICATION_ID );
                GameState.mSingleton = null;
                GameState.gameActivity = null;
                // this.finish(); // Gles2rice doesn't stop, why??
                System.exit( 0 ); // Workaround, force-close (what about SDL thread?)
                //
                break;
        }
        return super.onOptionsItemSelected( item );
    }
    
    @Override
    public void onConfigurationChanged( Configuration newConfig ) // This executes when the device
                                                                  // configuration changes
    {
        super.onConfigurationChanged( newConfig );
    }
    
    @Override
    public void onUserLeaveHint() // This executes when Home is pressed (can't detect it in onKey).
    {
        //
        // fileSaveEmulator( "Mupen64PlusAE_LastSession.sav" ); // immediate resume causes problems!
        saveSession(); // Workaround, allows us to force-close later
        //
        super.onUserLeaveHint(); // weird bug if chosing "Close" from menu. Force-close here?
        
        GameState.mSingleton = null;
        GameState.gameActivity = null;
        System.exit( 0 ); // Workaround, force-close (what about SDL thread?)
        
        /*
         * How to go home using an intent: Intent intent = new Intent( Intent.ACTION_MAIN );
         * intent.addCategory( Intent.CATEGORY_HOME ); startActivity( intent );
         */
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        if( GameState.mSurface != null || GameState.mGamePad != null )
        {
            getWindow().setContentView( R.layout.main );
            
            GameState.mSurface = (SDLSurface) findViewById( R.id.my_surface );
            
            GameState.mGamePad = (GamePad) findViewById( R.id.my_gamepad );
            GameState.mGamePad.setResources( getResources() );
            GameState.mGamePadListing = new GamePad.GamePadListing( Settings.path.gamepad_ini );
            GameState.mGamePad.loadPad();
        }
    }
    
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }
    
    // Handler for the messages
    Handler commandHandler = new Handler()
    {
        public void handleMessage( Message msg )
        {
            if( msg.arg1 == GameState.COMMAND_CHANGE_TITLE )
            {
                setTitle( (String) msg.obj );
            }
        }
    };
    
    // Send a message from the SDLMain thread
    private void sendCommand( int command, Object data )
    {
        Message msg = commandHandler.obtainMessage();
        msg.arg1 = command;
        msg.obj = data;
        commandHandler.sendMessage( msg );
    }
    
    private static boolean[] touchPadPointers = new boolean[256];
    private static int[] touchPadPointerX = new int[256];
    private static int[] touchPadPointerY = new int[256];
    
    private static boolean[] touchScreenPointers = new boolean[256];
    private static int[] touchScreenPointerX = new int[256];
    private static int[] touchScreenPointerY = new int[256];
    
    public static TouchPad mTouchPad = null;
    public static TouchPad.TouchPadListing mTouchPadListing = null;
    public static int whichTouchPad = 0;
    
    native int RegisterThis();
    
    public void touchScreenBeginEvent()
    {
    }
    
    public void touchScreenPointerDown( int pointer_id )
    {
        touchScreenPointers[pointer_id] = true;
    }
    
    public void touchScreenPointerUp( int pointer_id )
    {
        touchScreenPointers[pointer_id] = false;
        touchScreenPointerX[pointer_id] = -1;
        touchScreenPointerY[pointer_id] = -1;
    }
    
    public void touchScreenPointerPosition( int pointer_id, int x, int y )
    {
        touchScreenPointers[pointer_id] = true;
        touchScreenPointerX[pointer_id] = x;
        touchScreenPointerY[pointer_id] = y;
    }
    
    public void touchScreenEndEvent()
    {
        if( GameState.mSurface != null )
            GameState.mSurface.onTouchScreen( touchScreenPointers, touchScreenPointerX,
                    touchScreenPointerY, 64 );
    }
    
    public void touchPadBeginEvent()
    {
    }
    
    public void touchPadPointerDown( int pointer_id )
    {
        touchPadPointers[pointer_id] = true;
    }
    
    public void touchPadPointerUp( int pointer_id )
    {
        touchPadPointers[pointer_id] = false;
        touchPadPointerX[pointer_id] = -1;
        touchPadPointerY[pointer_id] = -1;
    }
    
    public void touchPadPointerPosition( int pointer_id, int x, int y )
    {
        touchPadPointers[pointer_id] = true;
        touchPadPointerX[pointer_id] = x;
        touchPadPointerY[pointer_id] = TouchPad.PAD_HEIGHT - y; // the Xperia Play's touchpad y-axis
                                                                // is flipped for some reason
    }
    
    public void touchPadEndEvent()
    {
        if( GameState.mSurface != null )
        {
            GameState.mSurface
                    .onTouchPad( touchPadPointers, touchPadPointerX, touchPadPointerY, 64 );
        }
    }
    
    public boolean onNativeKey( int action, int keycode )
    {
        if( GameState.mSurface == null )
            return false;
        return GameState.mSurface.onKey( keycode, action );
    }

    /**
     * Simulates key events for the SDLButtons on the virtual gamepad 
     * @param SDLButtonPressed Whether each button is pressed or not.
     * @param SDLButtonCodes Key code for each button.
     * @param SDLButtonCount Number of buttons.
     */
    public static void updateSDLButtonStates( boolean[] SDLButtonPressed, int[] SDLButtonCodes, int SDLButtonCount )
    {
        if( GameState.mSurface == null )
            return;
        
        for( int x = 0; x < SDLButtonCount; x++ )
        {
            if( SDLButtonPressed[x] != GameState.previousKeyStates[x] )
            {
                GameState.previousKeyStates[x] = SDLButtonPressed[x];
                if( SDLButtonPressed[x] )
                    GameState.mSurface.onSDLKey( SDLButtonCodes[x], KeyEvent.ACTION_DOWN );
                else
                    GameState.mSurface.onSDLKey( SDLButtonCodes[x], KeyEvent.ACTION_UP );
            }
        }
    }
    
    // FPS:
    private static int frameCount = -1;
    private static int fpsRate = 15;
    private static long lastFPSCheck = 0;

    // Audio:
    private static Thread mAudioThread = null;
    private static AudioTrack mAudioTrack = null;

    // Vibrator:
    private static long[] vibratePattern = { 0, 500, 0 };
    
    // Toast Messages:
    private static Toast toast = null;
    private static Runnable toastMessager = null;


    // Path name to a temporary file if it exists (used to temporarily decompress a zipped ROM):
    private static String tmpFile;






    static
    {
        loadNativeLibName( "SDL" );
        loadNativeLibName( "core" );  // TODO: Let the user choose which core to load
        loadNativeLibName( "front-end" );
    }

    /**
     * Pops up a temporary message on the device 
     * @param message Message to display.
     */
    public static void showToast( String message )
    {
        if( GameState.mSingleton == null )
            return;  // Activity hasn't been created yet
        if( toast != null )
            toast.setText( message );  // Toast exists, just change the text
        else
        { // Message short in duration, and at the bottom of the screen:
            toast = Toast.makeText( GameState.mSingleton, message, Toast.LENGTH_SHORT );
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
        GameState.mSingleton.runOnUiThread( toastMessager );
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
                    NativeMethods.runAudioThread();
                }
                catch( IllegalStateException ise )
                {
                    Log.e( "GameActivityCommon", "audioStartThread IllegalStateException", ise );
                    if( GameState.mSingleton != null )
                        showToast( GameState.mSingleton.getString( R.string.illegal_audio_state ) );
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
        if( GameState.mVibrate == null )
            return;
        if( active )
            GameState.mVibrate.vibrate( vibratePattern, 0 );
        else
            GameState.mVibrate.cancel();
    }

    public static boolean createGLContext(int majorVersion, int minorVersion)
    {
        return GameState.mSurface.initEGL(majorVersion, minorVersion);
    }
    
    public static void flipBuffers()
    {
        GameState.mSurface.flipEGL();
        if( frameCount < 0 )
        {
            frameCount = 0;
            lastFPSCheck = System.currentTimeMillis();
        }
        frameCount++;
        if( (GameState.mGamePad != null && frameCount >= GameState.mGamePad.fpsRate) ||
            (GameState.mGamePad == null && frameCount >= fpsRate) )
        {
            long currentTime = System.currentTimeMillis();
            float fFPS = ( (float) frameCount / (float) (currentTime - lastFPSCheck) ) * 1000.0f;
            if( GameState.mGamePad != null )
                GameState.mGamePad.updateFPS( (int) fFPS );
            frameCount = 0;
            lastFPSCheck = currentTime;
        }
    }

    public static void setActivityTitle( String title )
    {
        // Called from SDLMain() thread and can't directly affect the view
        GameState.gameActivity.sendCommand( GameState.COMMAND_CHANGE_TITLE, title );
    }

    public static Object getROMPath()
    {
        GameState.finishedReading = false;
        if( Settings.user.isLastGameNull )
        {
            GameState.finishedReading = true;
            //return (Object) (Globals.DataDir + "/roms/mupen64plus.v64");
            System.exit( 0 );
        }
        else if( Settings.user.isLastGameZipped )
        {
            // Create the tmp folder if it doesn't exist:
            String tmpFolderName = Settings.path.dataDir + "/tmp";
            File tmpFolder = new File( tmpFolderName );
            tmpFolder.mkdir();
            // Clear the folder if anything is in there:
            String[] children = tmpFolder.list();
            for( String child : children )
            {
                Utility.deleteFolder( new File( tmpFolder, child ) );
            }
            tmpFile = Utility.unzipFirstROM( new File( Settings.user.lastGame ), tmpFolderName );
            if( tmpFile == null )
            {
                Log.v( "GameActivityCommon", "Unable to play zipped ROM: '" + Settings.user.lastGame + "'" );

                GameState.notificationManager.cancel( GameState.NOTIFICATION_ID );

                if( ErrorLogger.hasError() )
                    ErrorLogger.putLastError( "OPEN_ROM", "fail_crash" );

                Intent intent = new Intent( GameState.mSingleton, MenuActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                GameState.mSingleton.startActivity( intent );
                GameState.finishedReading = true;
                System.exit( 0 );
            }
            else
            {
                GameState.finishedReading = true;
                return (Object) tmpFile;
            }
        }
        GameState.finishedReading = true;
        return (Object) Settings.user.lastGame;
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
        if( !Settings.user.autoSaveEnabled )
            return;
        showToast( GameState.mSingleton.getString( R.string.saving_game ) );
        NativeMethods.fileSaveEmulator( "Mupen64PlusAE_LastSession.sav" );
        try{Thread.sleep( 500 );}catch(InterruptedException e){}  // wait a bit
        int c = 0;
        int state = NativeMethods.stateEmulator();
        while( state == GameState.EMULATOR_STATE_PAUSED && c < 120 )
        {  // it should be paused while saving the session.
            try{Thread.sleep( 500 );}catch(InterruptedException e){}
            state = NativeMethods.stateEmulator();
            c++;
        }
        GameState.mSurface.buffFlipped = false;
        c = 0;
        while( !GameState.mSurface.buffFlipped )
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
        String hardware = null;
        String features = null;
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
            in.close();
        }
        catch( IOException ioe )
        {
            ioe.printStackTrace();
        }
        Settings.device.setHardwareType( hardware, features );
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
