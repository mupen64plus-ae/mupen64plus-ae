package paulscode.android.mupen64plusae;

import java.io.File; 

import android.content.res.Configuration;
import android.app.*;
import android.content.*;
import android.view.*;
import android.os.*;
import android.support.v4.app.*;
import android.util.Log;


/**
 * GameActivity
 */
public class GameActivityXperiaPlay extends NativeActivity
{
    public static int saveSlot = 0;

    private static boolean touchPadPointers[] = new boolean[256];
    private static int touchPadPointerX[] = new int[256];
    private static int touchPadPointerY[] = new int[256];

    private static boolean touchScreenPointers[] = new boolean[256];
    private static int touchScreenPointerX[] = new int[256];
    private static int touchScreenPointerY[] = new int[256];

    public static TouchPad mTouchPad = null;
    public static TouchPad.TouchPadListing mTouchPadListing = null;
    public static int whichTouchPad = 0;

    // Setup
    protected void onCreate( Bundle savedInstanceState )
    {
        // paulscode, place an icon into the status bar:
        if( GameActivityCommon.notificationManager == null )
            GameActivityCommon.notificationManager = (NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE );
        int statusIcon = R.drawable.status;
        CharSequence text = "Mupen64Plus AE is running";
        CharSequence contentTitle = "Mupen64Plus AE";
        CharSequence contentText = "Mupen64Plus AE";
        long when = System.currentTimeMillis();
        Context context = getApplicationContext();

        GameActivityCommon.mVibrate = (Vibrator) getSystemService( Context.VIBRATOR_SERVICE );

        Intent intent = new Intent( this, MenuActivity.class );
        intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
        PendingIntent contentIntent = PendingIntent.getActivity( this, 0, intent, 0 );
        
        NotificationCompat.Builder notification = new NotificationCompat.Builder( context )
        .setSmallIcon( statusIcon )
        .setAutoCancel( true )
        .setTicker( text )
        .setWhen( when )
        .setContentTitle( contentTitle )
        .setContentText( contentText )
        .setContentIntent( contentIntent );

        GameActivityCommon.notificationManager.notify( Globals.NOTIFICATION_ID, notification.getNotification() );

        // paulscode, load the native libraries:
        GameActivityCommon.loadNativeLibName( "xperia-touchpad" );
        GameActivityCommon.loadNativeLib( MenuActivity.mupen64plus_cfg.get( "UI-Console", "VideoPlugin" ) );
        GameActivityCommon.loadNativeLib( MenuActivity.mupen64plus_cfg.get( "UI-Console", "AudioPlugin" ) );
        GameActivityCommon.loadNativeLib( MenuActivity.mupen64plus_cfg.get( "UI-Console", "InputPlugin" ) );
        GameActivityCommon.loadNativeLib( MenuActivity.mupen64plus_cfg.get( "UI-Console", "RspPlugin" ) );

        /// paulscode, fix potential crash when input plug-in is disabled
        String inp = MenuActivity.mupen64plus_cfg.get( "UI-Console", "InputPlugin" );
        if( inp != null )
        {
            inp = inp.replace( "\"", "" );
            if( inp.equalsIgnoreCase( "dummy" ) )
                GameActivityCommon.noInputPlugin = true;
        }
        ///

        // paulscode, gather's information about the device, and chooses a hardware profile (used to customize settings)
        GameActivityCommon.readCpuInfo();
        int x;
        // paulscode, Xperia Play native input linkage
        for( x = 0; x < 256; x++ )
        {
            touchPadPointers[x] = false;
            touchPadPointerX[x] = -1;
            touchPadPointerY[x] = -1;
            touchScreenPointers[x] = false;
            touchScreenPointerX[x] = -1;
            touchScreenPointerY[x] = -1;
        }
        // paulscode, clears the virtual gamepad key states
        for( x = 0; x < 30; x++ )
        {
            GameActivityCommon.previousKeyStates[x] = false;
        }

        Globals.checkLocale( this );

        // Fullscreen mode
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
            getWindow().requestFeature( Window.FEATURE_ACTION_BAR_OVERLAY );
        else
            requestWindowFeature( Window.FEATURE_NO_TITLE );

        getWindow().setFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN,
                              WindowManager.LayoutParams.FLAG_FULLSCREEN );
        if( Globals.InhibitSuspend )
            getWindow().setFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                                  WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );

        super.onCreate( savedInstanceState );

        // So we can call stuff from static callbacks
        GameActivityCommon.mSingleton = (Activity) this;
        GameActivityCommon.gameActivityXperiaPlay = this;

        getWindow().takeSurface( null );
        getWindow().setContentView( R.layout.main );
        RegisterThis();

        GameActivityCommon.mSurface = (SDLSurface) findViewById( R.id.my_surface );

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
        {  // SDK version at least HONEYCOMB, so there should be software buttons on this device:
            View mView = GameActivityCommon.mSurface.getRootView();
            if( mView == null )
                Log.e( "GameActivityXperiaPlay", "getRootView() returned null in method onCreate" );
            else
                mView.setSystemUiVisibility( View.SYSTEM_UI_FLAG_LOW_PROFILE );
            getActionBar().hide();
        }
        GameActivityCommon.mGamePad = (GamePad) findViewById( R.id.my_gamepad );
        GameActivityCommon.mGamePad.setResources( getResources() );
        GameActivityCommon.mGamePadListing = new GamePad.GamePadListing( Globals.DataDir + "/skins/gamepads/gamepad_list.ini" );

        // Make sure the gamepad preferences are loaded;
        String val = MenuActivity.gui_cfg.get( "GAME_PAD", "analog_octagon" );
        if( val != null )
            MenuSkinsGamepadActivity.analogAsOctagon = ( val.equals( "1" ) ? true : false );
        val = MenuActivity.gui_cfg.get( "GAME_PAD", "show_fps" );
        if( val != null )
            MenuSkinsGamepadActivity.showFPS = ( val.equals( "1" ) ? true : false );
        val = MenuActivity.gui_cfg.get( "GAME_PAD", "enabled" );
        if( val != null )
            MenuSkinsGamepadActivity.enabled = ( val.equals( "1" ) ? true : false );
        MenuSkinsGamepadActivity.chosenGamepad = MenuActivity.gui_cfg.get( "GAME_PAD", "which_pad" );
        val = MenuActivity.gui_cfg.get( "VIDEO_PLUGIN", "rgba8888" );
        if( val != null )
            GameActivityCommon.rgba8888 = ( val.equals( "1" ) ? true : false );
         
        // Look up any special codes for the analog controls
        if( Globals.analog_100_64 )
        {
            for( int p = 0; p < 4; p++ )
            {
                val = MenuActivity.mupen64plus_cfg.get( "Input-SDL-Control" + (p+1), "plugged" );
                if( val == null )
                {
                    if( new File( Globals.StorageDir ).exists() )
                    {
                        MenuActivity.mupen64plus_cfg = new Config( Globals.DataDir + "/mupen64plus.cfg" );
                        val = MenuActivity.mupen64plus_cfg.get( "Input-SDL-Control" + (p+1), "plugged" );
                    }
                    else
                    {
                        Log.e( "GameActivityXperiaPlay", "No access to storage, probably in USB Mass Storage mode" );
                        GameActivityCommon.showToast( getString( R.string.app_data_inaccessible ) );
                    }
                }
                if( val != null && val.equals( "True" ) )
                {
                    val = MenuActivity.mupen64plus_cfg.get( "Input-SDL-Control" + (p+1), "X Axis" );
                    if( val != null )
                    {
                        x = val.indexOf( "(" );
                        int y = val.indexOf( ")" );
                        if( x >= 0 && y >= 0 && y > x )
                        {
                            val = val.substring( x + 1, y ).trim();
                            x = val.indexOf( "," );
                            if( x >= 0 )
                            {
                                Globals.ctrlr[p][0] = Utility.toInt( val.substring( x + 1, val.length() ), 0 );
                                Globals.ctrlr[p][1] = Utility.toInt( val.substring( 0, x ), 0 );
                            }
                        }
                        val = MenuActivity.mupen64plus_cfg.get( "Input-SDL-Control" + (p+1), "Y Axis" );
                        x = val.indexOf( "(" );
                        y = val.indexOf( ")" );
                        if( x >= 0 && y >= 0 && y > x )
                        {
                            val = val.substring( x + 1, y ).trim();
                            x = val.indexOf( "," );
                            if( x >= 0 )
                            {
                                Globals.ctrlr[p][2] = Utility.toInt( val.substring( x + 1, val.length() ), 0 );
                                Globals.ctrlr[p][3] = Utility.toInt( val.substring( 0, x ), 0 );
                            }
                        }
                    }
                }
            }
        }

        if( !MenuSkinsGamepadActivity.enabled )
            GameActivityCommon.mGamePad.loadPad( null );
        else if( MenuSkinsGamepadActivity.chosenGamepad != null && MenuSkinsGamepadActivity.chosenGamepad.length() > 0 )
            GameActivityCommon.mGamePad.loadPad( MenuSkinsGamepadActivity.chosenGamepad );
        else if( GameActivityCommon.mGamePadListing.numPads > 0 )
            GameActivityCommon.mGamePad.loadPad( GameActivityCommon.mGamePadListing.padNames[0] );
        else
        {
            GameActivityCommon.mGamePad.loadPad( null );
            Log.v( "GameActivityXperiaPlay", "No gamepad skins found" );
        }

        // paulscode, Xperia Play touchpad skins/layouts
        mTouchPad = new TouchPad( this, getResources() );
        mTouchPadListing = new TouchPad.TouchPadListing( Globals.DataDir + "/skins/touchpads/touchpad_list.ini" );

        // Make sure the touchpad preferences are loaded;
        val = MenuActivity.gui_cfg.get( "TOUCH_PAD", "analog_octagon" );
        if( val != null )
            MenuSkinsTouchpadActivity.analogAsOctagon = ( val.equals( "1" ) ? true : false );
        val = MenuActivity.gui_cfg.get( "TOUCH_PAD", "enabled" );
        if( val != null )
            MenuSkinsTouchpadActivity.enabled = ( val.equals( "1" ) ? true : false );
        MenuSkinsTouchpadActivity.chosenTouchpad = MenuActivity.gui_cfg.get( "TOUCH_PAD", "which_pad" );

        if( !MenuSkinsTouchpadActivity.enabled )
            mTouchPad.loadPad( null );
        else if( MenuSkinsTouchpadActivity.chosenTouchpad != null && MenuSkinsTouchpadActivity.chosenTouchpad.length() > 0 )
            mTouchPad.loadPad( MenuSkinsTouchpadActivity.chosenTouchpad );
        else if( mTouchPadListing.numPads > 0 )
            mTouchPad.loadPad( mTouchPadListing.padNames[0] );
        else
        {
            mTouchPad.loadPad( null );
            Log.v( "GameActivityXperiaPlay", "No touchpad skins found" );
        }
            
        GameActivityCommon.showToast( getString( R.string.mupen64plus_started ) );
        
    }

    // paulscode, add the menu options:
    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        saveSlot = 0;
        menu.add( 0, GameActivityCommon.MAIN_MENU_ITEM, 0, getString( R.string.ingame_menu ) );
        menu.add( 0, GameActivityCommon.SLOT_MENU_ITEM, 0, getString( R.string.ingame_inc_slot ) + " (0)" );
        menu.add( 0, GameActivityCommon.SAVE_MENU_ITEM, 0, getString( R.string.ingame_save ) );
        menu.add( 0, GameActivityCommon.LOAD_MENU_ITEM, 0, getString( R.string.ingame_load ) );
        menu.add( 0, GameActivityCommon.CLOSE_MENU_ITEM, 0, getString( R.string.ingame_close ) );
        return super.onCreateOptionsMenu( menu );
    }

    // paulscode, add the menu options:
    @Override
    public boolean onPrepareOptionsMenu( Menu menu )
    {
        menu.clear();
        menu.add( 0, GameActivityCommon.MAIN_MENU_ITEM, 0, getString( R.string.ingame_menu ) );
        menu.add( 0, GameActivityCommon.SLOT_MENU_ITEM, 0, getString( R.string.ingame_inc_slot ) + " (" + saveSlot + ")" );
        menu.add( 0, GameActivityCommon.SAVE_MENU_ITEM, 0, getString( R.string.ingame_save ) );
        menu.add( 0, GameActivityCommon.LOAD_MENU_ITEM, 0, getString( R.string.ingame_load ) );
        menu.add( 0, GameActivityCommon.CLOSE_MENU_ITEM, 0, getString( R.string.ingame_close ) );
        return super.onCreateOptionsMenu( menu );
    }

    // paulscode, add the menu options:
    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch( item.getItemId() )
        {
            case GameActivityCommon.MAIN_MENU_ITEM:
                GameActivityCommon.saveSession();  // Workaround, allows us to force-close later
//
                GameActivityCommon.notificationManager.cancel( Globals.NOTIFICATION_ID );
                Intent intent = new Intent( this, MenuActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                GameActivityCommon.mSingleton = null;
                GameActivityCommon.gameActivityXperiaPlay = null;

//                this.finish();  // This causes menu to crash, why??
                System.exit( 0 );  // Workaround, force-close (what about SDL thread?)
                break;
            case GameActivityCommon.SLOT_MENU_ITEM:
                saveSlot++;
                if( saveSlot > 9 )
                    saveSlot = 0;
                GameActivityCommon.stateSetSlotEmulator( saveSlot );
                GameActivityCommon.showToast( getString( R.string.savegame_slot ) + " " + saveSlot );
                break;
            case GameActivityCommon.SAVE_MENU_ITEM:
                GameActivityCommon.stateSaveEmulator();
                break;
            case GameActivityCommon.LOAD_MENU_ITEM:
                GameActivityCommon.stateLoadEmulator();
                break;
            case GameActivityCommon.CLOSE_MENU_ITEM:
//                notificationManager.cancel( Globals.NOTIFICATION_ID );
//                this.finish();  // This doesn't save; closes to quickly maybe?
                GameActivityCommon.saveSession();  // Workaround, wait for fileSaveEmulator to finish first
                GameActivityCommon.notificationManager.cancel( Globals.NOTIFICATION_ID );
                GameActivityCommon.mSingleton = null;
                GameActivityCommon.gameActivityXperiaPlay = null;
//                this.finish();  // Gles2rice doesn't stop, why??
                System.exit( 0 );  // Workaround, force-close (what about SDL thread?)
//
                break;
        }
        return super.onOptionsItemSelected( item );
    }

    @Override
    public void onConfigurationChanged( Configuration newConfig )  // This executes when the device configuration changes
    { 
        super.onConfigurationChanged( newConfig );
    }

    @Override
    public void onUserLeaveHint()  // This executes when Home is pressed (can't detect it in onKey).
    { 
//
//        fileSaveEmulator( "Mupen64PlusAE_LastSession.sav" );  // immediate resume causes problems!
        GameActivityCommon.saveSession();  // Workaround, allows us to force-close later
//
        super.onUserLeaveHint();  // weird bug if chosing "Close" from menu.  Force-close here?
        
        GameActivityCommon.mSingleton = null;
        GameActivityCommon.gameActivityXperiaPlay = null;
        System.exit( 0 );  // Workaround, force-close (what about SDL thread?)

        /* How to go home using an intent:
                Intent intent = new Intent( Intent.ACTION_MAIN );
                intent.addCategory( Intent.CATEGORY_HOME );
                startActivity( intent );
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
        if( GameActivityCommon.mSurface != null || GameActivityCommon.mGamePad != null )
        {
            getWindow().setContentView( R.layout.main );

            GameActivityCommon.mSurface = (SDLSurface) findViewById( R.id.my_surface );

            GameActivityCommon.mGamePad = (GamePad) findViewById( R.id.my_gamepad );
            GameActivityCommon.mGamePad.setResources( getResources() );
            GameActivityCommon.mGamePadListing = new GamePad.GamePadListing( Globals.DataDir + "/skins/gamepads/gamepad_list.ini" );
            if( !MenuSkinsGamepadActivity.enabled )
                GameActivityCommon.mGamePad.loadPad( null );
            else if( MenuSkinsGamepadActivity.chosenGamepad != null && MenuSkinsGamepadActivity.chosenGamepad.length() > 0 )
                GameActivityCommon.mGamePad.loadPad( MenuSkinsGamepadActivity.chosenGamepad );
            else if( GameActivityCommon.mGamePadListing.numPads > 0 )
                GameActivityCommon.mGamePad.loadPad( GameActivityCommon.mGamePadListing.padNames[0] );

        }
    }
    
    @Override
    protected void onDestroy() 
    {
        super.onDestroy();
    }
    
    // Handler for the messages
    Handler commandHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.arg1 == GameActivityCommon.COMMAND_CHANGE_TITLE) {
                setTitle((String)msg.obj);
            }
        }
    };

    // Send a message from the SDLMain thread
    void sendCommand(int command, Object data) {
        Message msg = commandHandler.obtainMessage();
        msg.arg1 = command;
        msg.obj = data;
        commandHandler.sendMessage(msg);
    }

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
        if( GameActivityCommon.mSurface != null )
            GameActivityCommon.mSurface.onTouchScreen( touchScreenPointers, touchScreenPointerX, touchScreenPointerY, 64 );
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
        touchPadPointerY[pointer_id] = TouchPad.PAD_HEIGHT - y;  // the Xperia Play's touchpad y-axis is flipped for some reason
    }
    public void touchPadEndEvent()
    {
        if( GameActivityCommon.mSurface != null )
        {
            GameActivityCommon.mSurface.onTouchPad( touchPadPointers, touchPadPointerX, touchPadPointerY, 64 );
        }
    }

    public boolean onNativeKey( int action, int keycode )
    {
        if( GameActivityCommon.mSurface == null )
            return false;
        return GameActivityCommon.mSurface.onKey( keycode, action );
    }
}
