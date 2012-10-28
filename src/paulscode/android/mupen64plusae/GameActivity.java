package paulscode.android.mupen64plusae;

import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.input.PeripheralController;
import paulscode.android.mupen64plusae.input.TouchscreenController;
import paulscode.android.mupen64plusae.input.XperiaPlayController;
import paulscode.android.mupen64plusae.input.transform.KeyTransform.ImeFormula;
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
import android.util.AttributeSet;
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
    private MenuItem mSlotMenuItem;
    
    static
    {
        Utility.loadNativeLibName( "SDL" );
        Utility.loadNativeLibName( "core" ); // TODO: Let the user choose which core to load
        Utility.loadNativeLibName( "front-end" );
    }
    
    private void loadNativeLibraries()
    {
        if( Globals.userPrefs.isXperiaEnabled )
        {
            Utility.loadNativeLibName( "xperia-touchpad" );
        }
        Utility.loadNativeLib( Globals.userPrefs.videoPlugin );
        Utility.loadNativeLib( Globals.userPrefs.audioPlugin );
        Utility.loadNativeLib( Globals.userPrefs.inputPlugin );
        Utility.loadNativeLib( Globals.userPrefs.rspPlugin );
    }
    
    private static void saveSession( GameActivity activity )
    {
        if( !Globals.userPrefs.isAutoSaveEnabled )
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
            Utility.safeSleep( 500 );
        }
        while( !Globals.surfaceInstance.buffFlipped
                && NativeMethods.stateEmulator() == EMULATOR_STATE_PAUSED );
    }
    
    @Override
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
        
        // New controller system
        SDLSurface surface = (SDLSurface) findViewById( R.id.sdlSurface );
        PeripheralController controller1 = new PeripheralController( surface,
                Globals.userPrefs.gamepadMap1, ImeFormula.DEFAULT );
        
        Notifier.showToast( getString( R.string.mupen64plus_started ), this );
    }
    
    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        getMenuInflater().inflate( R.menu.game_activity, menu );
        mSlotMenuItem = menu.findItem( R.id.ingameSlot );
        setSlot( 0 );
        return super.onCreateOptionsMenu( menu );
    }
    
    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch( item.getItemId() )
        {
            case R.id.slot0:
                setSlot( 0, item );
                break;
            case R.id.slot1:
                setSlot( 1, item );
                break;
            case R.id.slot2:
                setSlot( 2, item );
                break;
            case R.id.slot3:
                setSlot( 3, item );
                break;
            case R.id.slot4:
                setSlot( 4, item );
                break;
            case R.id.slot5:
                setSlot( 5, item );
                break;
            case R.id.slot6:
                setSlot( 6, item );
                break;
            case R.id.slot7:
                setSlot( 7, item );
                break;
            case R.id.slot8:
                setSlot( 8, item );
                break;
            case R.id.slot9:
                setSlot( 9, item );
                break;
            case R.id.ingameQuicksave:
                NativeMethods.stateSaveEmulator();
                break;
            case R.id.ingameQuickload:
                NativeMethods.stateLoadEmulator();
                break;
            case R.id.ingameSave:
                // TODO: Implement dialog for save filename
                // NativeMethods.fileSaveEmulator( filename );
                break;
            case R.id.ingameLoad:
                // TODO: Implement dialog for load filename
                // NativeMethods.fileLoadEmulator( filename );
                break;
            case R.id.ingameMenu:
                // Save game state and launch MenuActivity
                saveSession( this );
                Notifier.clear();
                Globals.gameInstance = null;
                Intent intent = new Intent( this, MenuActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected( item );
    }
    
    private void setSlot( int value, MenuItem item )
    {
        setSlot( value );
        item.setChecked( true );
    }
    
    private void setSlot( int value )
    {
        mSlot = value % NUM_SLOTS;
        NativeMethods.stateSetSlotEmulator( mSlot );
        Notifier.showToast( getString( R.string.savegame_slot, mSlot ), this );
        if( mSlotMenuItem != null )
            mSlotMenuItem.setTitle( getString( R.string.ingameSlot_title, mSlot ) );
    }
    
    @Override
    public void onUserLeaveHint()
    {
        // This executes when Home is pressed (can't detect it in onKey).
        saveSession( this ); // Workaround, allows us to force-close later
        super.onUserLeaveHint(); // weird bug if choosing "Close" from menu. Force-close here?
        // Globals.gameInstance = null;
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
        CharSequence text = getString( R.string.gameActivity_ticker, appName );
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
            mTouchPadListing = new XperiaPlayController.TouchPadListing(
                    Globals.paths.xperiaPlayLayouts_ini );
            mTouchPad = new XperiaPlayController( this, getResources() );
            mTouchPad.loadPad();
        }
        
        mVibrator = (Vibrator) getSystemService( Context.VIBRATOR_SERVICE );
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
        
        setContentView( R.layout.game_activity );
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
    
    // Send a message from the SDLMain thread
    void sendCommand( int command, Object data )
    {
        CommandHandler commandHandler = new CommandHandler( this );
        Message msg = commandHandler.obtainMessage();
        msg.arg1 = command;
        msg.obj = data;
        commandHandler.sendMessage( msg );
    }
    
    // Handler for the messages
    static class CommandHandler extends Handler
    {
        GameActivity mActivity;
        
        public CommandHandler( GameActivity activity )
        {
            mActivity = activity;
        }
        
        public void handleMessage( Message msg )
        {
            if( msg.arg1 == COMMAND_CHANGE_TITLE )
            {
                mActivity.setTitle( (String) msg.obj );
            }
        }
    };
}
