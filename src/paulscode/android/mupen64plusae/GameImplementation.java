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
 * Authors: paulscode, lioncash, littleguy77
 */
package paulscode.android.mupen64plusae;

import paulscode.android.mupen64plusae.input.PeripheralController;
import paulscode.android.mupen64plusae.input.TouchscreenController;
import paulscode.android.mupen64plusae.input.transform.KeyTransform.ImeFormula;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Utility;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;

public class GameImplementation implements View.OnKeyListener
{
    // Used by Main thread to call back a title change:
    public static final int COMMAND_CHANGE_TITLE = 1;
    
    // Emulator states
    public static final int EMULATOR_STATE_UNKNOWN = 0;
    public static final int EMULATOR_STATE_STOPPED = 1;
    public static final int EMULATOR_STATE_RUNNING = 2;
    public static final int EMULATOR_STATE_PAUSED = 3;
    
    // App state
    public static boolean finishedReading = false;
    
    // Private static objects used by public static methods
    private static GameActivity sGameActivity = null;
    private static GameActivityXperiaPlay sGameActivityXperiaPlay = null;
    private static Vibrator sVibrator = null;
    private static final long[] VIBRATE_PATTERN = { 0, 500, 0 };
    
    // Internals
    private Activity mActivity;
    @SuppressWarnings( "unused" )
    private TouchscreenController mTouchscreenController;
    private PeripheralController mPeripheralController;
    private MenuItem mSlotMenuItem;
    private int mSlot = 0;
    private static final int NUM_SLOTS = 10;
    
    public GameImplementation( Activity activity )
    {
        mActivity = activity;
        if( activity instanceof GameActivity )
            sGameActivity = ( GameActivity ) activity;
        if( activity instanceof GameActivityXperiaPlay )
            sGameActivityXperiaPlay = ( GameActivityXperiaPlay ) activity;
    }
    
    public static Object getRomPath()
    {
        if( sGameActivity != null )
            return Globals.paths.getROMPath( Globals.userPrefs, sGameActivity );
        else if( sGameActivityXperiaPlay != null )
            return Globals.paths.getROMPath( Globals.userPrefs, sGameActivityXperiaPlay );
        else
            return null;
    }
    
    public static void runOnUiThread( Runnable action )
    {
        
        if( sGameActivity != null )
            sGameActivity.runOnUiThread( action );
        else if( sGameActivityXperiaPlay != null )
            sGameActivityXperiaPlay.runOnUiThread( action );
    }
    
    public static void showToast( String message )
    {
        if( sGameActivity != null )
            Notifier.showToast( message, sGameActivity );
        else if( sGameActivityXperiaPlay != null )
            Notifier.showToast( message, sGameActivityXperiaPlay );
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
    
    public void onCreate( Bundle savedInstanceState )
    {
        // Lay out content and initialize stuff
        configureLayout();
        notifyPendingIntent();
        loadNativeLibraries();
        
        // Initialize controllers
        SDLSurface surface = (SDLSurface) mActivity.findViewById( R.id.sdlSurface );
        mTouchscreenController = new TouchscreenController( surface );
        mPeripheralController = new PeripheralController( surface,
                Globals.userPrefs.gamepadMap1, ImeFormula.DEFAULT );
        sVibrator = (Vibrator) mActivity.getSystemService( Context.VIBRATOR_SERVICE );
        
        // Override the peripheral controller key listener, to add some functionality
        surface.setOnKeyListener( this );
        
        // Notify that game activity has started
        Notifier.showToast( mActivity.getString( R.string.mupen64plus_started ), mActivity );
    }
    
    public void onCreateOptionsMenu( Menu menu )
    {
        // Inflate the in-game menu, get the 'Slot X' menu item
        mActivity.getMenuInflater().inflate( R.menu.game_activity, menu );
        mSlotMenuItem = menu.findItem( R.id.ingameSlot );
        setSlot( 0 );
    }
    
    public void onOptionsItemSelected( MenuItem item )
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
                saveSession();
                Notifier.clear();
                sGameActivity = null;
                sGameActivityXperiaPlay = null;
                Intent intent = new Intent( mActivity, MenuActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                mActivity.startActivity( intent );
                mActivity.finish();
                break;
            default:
                break;
        }
    }
    
    public void onUserLeaveHint()
    {
        // This executes when Home is pressed (can't detect it in onKey).
        saveSession();
    }

    @Override
    public boolean onKey( View v, int keyCode, KeyEvent event )
    {
        // Let Android handle all keys if user disables input plugin
        if( !Globals.userPrefs.isInputEnabled )
        {
            return false;
        }
        
        // Let Android handle the menu key
        else if( keyCode == KeyEvent.KEYCODE_MENU )
        {
            return false;
        }
        
        // Toggle the ActionBar for HoneyComb+ when back is pressed
        else if( keyCode == KeyEvent.KEYCODE_BACK
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
        {
            if( event.getAction() == KeyEvent.ACTION_DOWN )
                mActivity.runOnUiThread( new ToggleActionBar( v.getRootView() ) );
            return true;
        }
        
        // Let Android handle the volume keys if not used for control
        else if( !Globals.userPrefs.isVolKeysEnabled
                && ( keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE ) )
        {
            return false;
        }
        
        // Send everything else to the peripheral controller
        else
        {
            return mPeripheralController.getTransform().onKey( v, keyCode, event );
        }
    }

    @TargetApi( 11 )
    private void configureLayout()
    {
        Window window = mActivity.getWindow();
        
        // Configure full-screen mode
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
            window.requestFeature( Window.FEATURE_ACTION_BAR_OVERLAY );
        else
            mActivity.requestWindowFeature( Window.FEATURE_NO_TITLE );
        window.setFlags( LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN );
        
        // Keep screen on under certain conditions
        if( Globals.INHIBIT_SUSPEND )
            window.setFlags( LayoutParams.FLAG_KEEP_SCREEN_ON, LayoutParams.FLAG_KEEP_SCREEN_ON );
                
        mActivity.setContentView( R.layout.game_activity );
        Globals.sdlSurface = (SDLSurface) mActivity.findViewById( R.id.sdlSurface );
        Globals.touchscreenView = (TouchscreenView) mActivity.findViewById( R.id.touchscreenView );
        Globals.touchscreenView.setResources( mActivity.getResources() );
        Globals.touchscreenView.loadPad();
        
        // Hide the action bar introduced in higher Android versions
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
        {
            // SDK version at least HONEYCOMB, so there should be software buttons on this device:
            View view = Globals.sdlSurface.getRootView();
            if( view != null )
                view.setSystemUiVisibility( View.SYSTEM_UI_FLAG_LOW_PROFILE );
            mActivity.getActionBar().hide();
        }
    }

    private void notifyPendingIntent()
    {
        Intent intent = new Intent( mActivity, MenuActivity.class );
        intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
        PendingIntent contentIntent = PendingIntent.getActivity( mActivity, 0, intent, 0 );
        
        String appName = mActivity.getString( R.string.app_name );
        CharSequence text = mActivity.getString( R.string.gameActivity_ticker, appName );
        CharSequence contentTitle = appName;
        CharSequence contentText = appName;
        long when = System.currentTimeMillis();
        Context context = mActivity.getApplicationContext();
        
        NotificationCompat.Builder notification = new NotificationCompat.Builder( context )
                .setSmallIcon( R.drawable.status ).setAutoCancel( true ).setTicker( text )
                .setWhen( when ).setContentTitle( contentTitle ).setContentText( contentText )
                .setContentIntent( contentIntent );
        
        Notifier.notify( notification.build() );
    }

    private void loadNativeLibraries()
    {
        Utility.loadNativeLibName( "SDL" );
        Utility.loadNativeLibName( "core" ); // TODO: Let the user choose which core to load
        Utility.loadNativeLibName( "front-end" );
        if( Globals.userPrefs.isVideoEnabled )
            Utility.loadNativeLib( Globals.userPrefs.videoPlugin );
        if( Globals.userPrefs.isAudioEnabled )
            Utility.loadNativeLib( Globals.userPrefs.audioPlugin );
        if( Globals.userPrefs.isInputEnabled )
            Utility.loadNativeLib( Globals.userPrefs.inputPlugin );
        if( Globals.userPrefs.isRspEnabled )
            Utility.loadNativeLib( Globals.userPrefs.rspPlugin );
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
        Notifier.showToast( mActivity.getString( R.string.savegame_slot, mSlot ), mActivity );
        if( mSlotMenuItem != null )
            mSlotMenuItem.setTitle( mActivity.getString( R.string.ingameSlot_title, mSlot ) );
    }
    
    private void saveSession()
    {
        if( !Globals.userPrefs.isAutoSaveEnabled )
            return;
        
        // Pop up a toast message
        if( mActivity != null )
            Notifier.showToast( mActivity.getString( R.string.saving_game ), mActivity );
        
        // Call the native method to save the emulator state
        NativeMethods.fileSaveEmulator( "Mupen64PlusAE_LastSession.sav" );
        Globals.sdlSurface.buffFlipped = false;
        
        // Wait for the game to resume by monitoring emulator state and the EGL buffer flip
        do
        {
            Utility.safeSleep( 500 );
        }
        while( !Globals.sdlSurface.buffFlipped
                && NativeMethods.stateEmulator() == EMULATOR_STATE_PAUSED );
    }

    private class ToggleActionBar implements Runnable
    {
        View mRootView;
        public ToggleActionBar( View rootView )
        {
            mRootView = rootView;
        }
        
        @Override
        @TargetApi( 11 )
        public void run()
        {
            ActionBar actionBar = mActivity.getActionBar();
            if( mActivity.getActionBar().isShowing() )
            {
                if( mRootView != null )
                    mRootView.setSystemUiVisibility( View.SYSTEM_UI_FLAG_LOW_PROFILE );
                actionBar.hide();
            }
            else
            {
                actionBar.show();
            }
        }        
    }    
}
