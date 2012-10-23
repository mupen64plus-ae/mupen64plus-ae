package paulscode.android.mupen64plusae;

import paulscode.android.mupen64plusae.NativeMethods.Helpers;
import paulscode.android.mupen64plusae.input.XperiaPlayController;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Utility;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;

// TODO Need to subclass NativeActivity for Xperia Play... may need a different refactoring.
// @TargetApi( 9 )
public class GameActivity extends Activity
{
    public static XperiaPlayController mTouchPad = null;
    public static XperiaPlayController.TouchPadListing mTouchPadListing = null;
    
    // Used by Main thread to call back a title change:
    public static final int COMMAND_CHANGE_TITLE = 1;
    
    // Emulator states
    public static final int EMULATOR_STATE_UNKNOWN = 0;
    public static final int EMULATOR_STATE_STOPPED = 1;
    public static final int EMULATOR_STATE_RUNNING = 2;
    public static final int EMULATOR_STATE_PAUSED = 3;
    
    // App state
    public static boolean finishedReading = false;

    // Internals
    private Vibrator mVibrator = null;
    private static long[] vibratePattern = { 0, 500, 0 };
    private int mSlot = 0;
    private static final int NUM_SLOTS = 10;
    
    static
    {
        Helpers.loadNativeLibName( "SDL" );
        Helpers.loadNativeLibName( "core" ); // TODO: Let the user choose which core to load
        Helpers.loadNativeLibName( "front-end" );
    }
    
    private static void saveSession( GameActivity activity )
    {
        if( !Globals.userPrefs.autoSaveEnabled )
            return;
        
        // Pop up a toast message
        if( activity != null )
            Notifier.showToast( activity.getString( R.string.saving_game ), activity );
        
        // Call the native method to save the emulator state
        NativeMethods.fileSaveEmulator( "Mupen64PlusAE_LastSession.sav" );
        Globals.surfaceInstance.buffFlipped = false;
        
        // Wait for the game to resume by monitoring emulator state and the EGL buffer flip
        do
        {
            Utility.safeWait( 500 );
        }
        while( !Globals.surfaceInstance.buffFlipped && NativeMethods.stateEmulator() == EMULATOR_STATE_PAUSED );
    }
    
    @TargetApi( 9 )
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        Globals.gameInstance = this;
        
        // Lay out content
        configureLayout();
        
        // Initialize stuff
        notifyPendingIntent();
        loadNativeLibraries();
        initializeControllers();
        
        Notifier.showToast( getString( R.string.mupen64plus_started ), this );
    }
    
    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        getMenuInflater().inflate( R.menu.ingame, menu );
        return super.onCreateOptionsMenu( menu );
    }
    
    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch( item.getItemId() )
        {
            case R.id.ingameSlot:
                // incrementSlot();
                break;
            case R.id.slot0:
                setSlot( 0 );
                break;
            case R.id.slot1:
                setSlot( 1 );
                break;
            case R.id.slot2:
                setSlot( 2 );
                break;
            case R.id.ingameSave:
                NativeMethods.stateSaveEmulator();
                break;
            case R.id.ingameLoad:
                NativeMethods.stateLoadEmulator();
                break;
            case R.id.ingameMenu:
                saveSession( this ); // Workaround, allows us to force-close later
                Notifier.clear();
                Intent intent = new Intent( this, MenuActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                Globals.gameInstance = null;
                
                // this.finish(); // This causes menu to crash, why??
                System.exit( 0 ); // Workaround, force-close (what about SDL thread?)
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected( item );
    }
    
    @Override
    public void onUserLeaveHint()
    {
        // This executes when Home is pressed (can't detect it in onKey).
        //
        // fileSaveEmulator( "Mupen64PlusAE_LastSession.sav" ); // immediate resume causes problems!
        saveSession( this ); // Workaround, allows us to force-close later
        //
        super.onUserLeaveHint(); // weird bug if choosing "Close" from menu. Force-close here?
        
        Globals.gameInstance = null;
        System.exit( 0 ); // Workaround, force-close (what about SDL thread?)
        
        /*
         * How to go home using an intent: Intent intent = new Intent( Intent.ACTION_MAIN );
         * intent.addCategory( Intent.CATEGORY_HOME ); startActivity( intent );
         */
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        if( Globals.surfaceInstance != null || Globals.touchscreenInstance != null )
        {
            // Replace the surface and touchscreen objects
            Globals.surfaceInstance = (SDLSurface) findViewById( R.id.sdlSurface );
            Globals.touchscreenInstance = (TouchscreenView) findViewById( R.id.touchscreenController );
            Globals.touchscreenInstance.setResources( getResources() );
            Globals.touchscreenInstance.loadPad();
        }
    }
    
    public void vibrate( boolean active )
    {
        if( mVibrator == null )
            return;
        if( active )
            mVibrator.vibrate( vibratePattern, 0 );
        else
            mVibrator.cancel();
    }
    
    private void notifyPendingIntent()
    {
        Intent intent = new Intent( this, MenuActivity.class );
        intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
        PendingIntent contentIntent = PendingIntent.getActivity( this, 0, intent, 0 );
        
        String appName = getString( R.string.app_name );
        CharSequence text = appName + " is running"; // TODO: localize text
        CharSequence contentTitle = appName;
        CharSequence contentText = appName;
        long when = System.currentTimeMillis();
        Context context = getApplicationContext();
        
        NotificationCompat.Builder notification = new NotificationCompat.Builder( context )
                .setSmallIcon( R.drawable.status ).setAutoCancel( true ).setTicker( text )
                .setWhen( when ).setContentTitle( contentTitle ).setContentText( contentText )
                .setContentIntent( contentIntent );
        
        Notifier.notify( notification.build() );
    }
    
    private void initializeControllers()
    {
        if( Globals.userPrefs.isXperiaEnabled )
        {
            mTouchPadListing = new XperiaPlayController.TouchPadListing( Globals.path.touchpad_ini );
            mTouchPad = new XperiaPlayController( this, getResources() );
            mTouchPad.loadPad();
        }
        
        mVibrator = (Vibrator) getSystemService( Context.VIBRATOR_SERVICE );
    }

    private void loadNativeLibraries()
    {
        if( Globals.userPrefs.isXperiaEnabled )
        {
            Helpers.loadNativeLibName( "xperia-touchpad" );
        }
        Helpers.loadNativeLib( Globals.mupen64plus_cfg.get( "UI-Console", "VideoPlugin" ) );
        Helpers.loadNativeLib( Globals.mupen64plus_cfg.get( "UI-Console", "AudioPlugin" ) );
        Helpers.loadNativeLib( Globals.mupen64plus_cfg.get( "UI-Console", "InputPlugin" ) );
        Helpers.loadNativeLib( Globals.mupen64plus_cfg.get( "UI-Console", "RspPlugin" ) );
    }
    
    @TargetApi( 11 )
    private void configureLayout()
    {
        Window window = getWindow();
        
        // Configure full-screen mode
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
            window.requestFeature( Window.FEATURE_ACTION_BAR_OVERLAY );
        else
            requestWindowFeature( Window.FEATURE_NO_TITLE );
        window.setFlags( LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN );
        
        // Keep screen on under certain conditions
        if( Globals.INHIBIT_SUSPEND )
            window.setFlags( LayoutParams.FLAG_KEEP_SCREEN_ON, LayoutParams.FLAG_KEEP_SCREEN_ON );
        
        if( Globals.userPrefs.isXperiaEnabled )
        {
            window.takeSurface( null );
            NativeMethods.RegisterThis();
        }
        
        setContentView( R.layout.game );
        Globals.surfaceInstance = (SDLSurface) findViewById( R.id.sdlSurface );
        Globals.touchscreenInstance = (TouchscreenView) findViewById( R.id.touchscreenController );
        Globals.touchscreenInstance.setResources( getResources() );
        Globals.touchscreenInstance.loadPad();
        
        // Hide the action bar introduced in higher Android versions
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
        {
            // SDK version at least HONEYCOMB, so there should be software buttons on this device:
            View view = Globals.surfaceInstance.getRootView();
            if( view == null )
                Log.e( "GameActivity", "getRootView() returned null in method onCreate" );
            else
                view.setSystemUiVisibility( View.SYSTEM_UI_FLAG_LOW_PROFILE );
            getActionBar().hide();
        }
    }
    
    private void setSlot( int value )
    {
        mSlot = value % NUM_SLOTS;
        NativeMethods.stateSetSlotEmulator( mSlot );
        Notifier.showToast( getString( R.string.savegame_slot ) + " " + mSlot, this );
    }
    
    @SuppressWarnings( "unused" )
    private void incrementSlot()
    {
        setSlot( mSlot + 1 );
    }
    
    @SuppressWarnings( "unused" )
    private void decrementSlot()
    {
        setSlot( mSlot + NUM_SLOTS - 1 );
    }
    
    // Handler for the messages
    Handler commandHandler = new Handler()
    {
        public void handleMessage( Message msg )
        {
            if( msg.arg1 == COMMAND_CHANGE_TITLE )
            {
                setTitle( (String) msg.obj );
            }
        }
    };
    
    // Send a message from the SDLMain thread
    void sendCommand( int command, Object data )
    {
        Message msg = commandHandler.obtainMessage();
        msg.arg1 = command;
        msg.obj = data;
        commandHandler.sendMessage( msg );
    }
}
