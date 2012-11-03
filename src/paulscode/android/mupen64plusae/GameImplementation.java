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
import paulscode.android.mupen64plusae.input.transform.TouchMap;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Utility;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;

public class GameImplementation implements View.OnKeyListener
{
    // Internals
    private Activity mActivity;
    private SDLSurface mSdlSurface;
    private TouchMap mTouchscreenMap;
    private TouchscreenView mTouchscreenView;
    @SuppressWarnings( "unused" )
    private TouchscreenController mTouchscreenController;
    private PeripheralController mPeripheralController1;
    private MenuItem mSlotMenuItem;
    private int mSlot = 0;
    private static final int NUM_SLOTS = 10;
    
    public GameImplementation( Activity activity )
    {
        mActivity = activity;
    }
    
    @TargetApi( 11 )
    public void onCreate( Bundle savedInstanceState )
    {
        // Lay out content and initialize stuff
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
        
        // Get the view objects
        mActivity.setContentView( R.layout.game_activity );
        mSdlSurface = (SDLSurface) mActivity.findViewById( R.id.sdlSurface );
        mTouchscreenView = (TouchscreenView) mActivity.findViewById( R.id.touchscreenView );
        
        // Hide the action bar introduced in higher Android versions
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
        {
            // SDK version at least HONEYCOMB, so there should be software buttons on this device:
            View view = mSdlSurface.getRootView();
            if( view != null )
                view.setSystemUiVisibility( View.SYSTEM_UI_FLAG_LOW_PROFILE );
            mActivity.getActionBar().hide();
        }
        
        // TODO: I removed the status notification... Do we really need it?
        
        // Load native libraries
        // TODO: Let the user choose which core to load
        Utility.loadNativeLibName( "SDL" );
        Utility.loadNativeLibName( "core" );
        Utility.loadNativeLibName( "front-end" );
        if( Globals.userPrefs.isVideoEnabled )
            Utility.loadNativeLib( Globals.userPrefs.videoPlugin );
        if( Globals.userPrefs.isAudioEnabled )
            Utility.loadNativeLib( Globals.userPrefs.audioPlugin );
        if( Globals.userPrefs.isInputEnabled )
            Utility.loadNativeLib( Globals.userPrefs.inputPlugin );
        if( Globals.userPrefs.isRspEnabled )
            Utility.loadNativeLib( Globals.userPrefs.rspPlugin );
        
        // Initialize user interface devices
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR
                && ( Globals.userPrefs.isTouchscreenEnabled || Globals.userPrefs.isFrameRateEnabled ) )
        {
            // The touch map and view are needed to display frame rate and/or controls
            mTouchscreenMap = new TouchMap();
            mTouchscreenMap.setResources( mActivity.getResources() );
            mTouchscreenMap.load( Globals.userPrefs.touchscreenLayoutFolder );
            mTouchscreenView.initialize( mTouchscreenMap );
            mSdlSurface.initialize( mTouchscreenMap );
            
            // The touch controller is needed to handle touch events
            if( Globals.userPrefs.isTouchscreenEnabled )
            {
                mTouchscreenController = new TouchscreenController( mTouchscreenMap, mSdlSurface );
            }
        }
        if( Globals.userPrefs.isInputEnabled )
        {
            // Create the peripheral controllers
            mPeripheralController1 = new PeripheralController( mSdlSurface, Globals.userPrefs.inputMap1, ImeFormula.DEFAULT );
        }
        Vibrator vibrator = (Vibrator) mActivity.getSystemService( Context.VIBRATOR_SERVICE );
        
        // Override the peripheral controller key listener, to add some extra functionality
        mSdlSurface.setOnKeyListener( this );
        
        // Synchronize the interface to the emulator core
        CoreInterface.startup( mActivity, mSdlSurface, vibrator );
        
        // Notify user that the game activity has started
        Notifier.showToast( mActivity.getString( R.string.mupen64plus_started ), mActivity );
    }
    
    public void onCreateOptionsMenu( Menu menu )
    {
        // Inflate the in-game menu, record the 'Slot X' menu object for later
        mActivity.getMenuInflater().inflate( R.menu.game_activity, menu );
        mSlotMenuItem = menu.findItem( R.id.ingameSlot );
        setSlot( 0, false );
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
                CoreInterface.shutdown();
                Intent intent = new Intent( mActivity, MenuActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                mActivity.startActivity( intent );
                //mActivity.finish();
                break;
            default:
                break;
        }
    }
    
    public void onUserLeaveHint()
    {
        // This executes when Home is pressed (can't detect it in onKey).
        // TODO Why does this cause app crash?
        saveSession();
    }

    @TargetApi( 11 )
    @Override
    public boolean onKey( View view, int keyCode, KeyEvent event )
    {
        // Toggle the ActionBar for HoneyComb+ when back is pressed
        if( keyCode == KeyEvent.KEYCODE_BACK
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
        {
            if( event.getAction() == KeyEvent.ACTION_DOWN )
                toggleActionBar( view.getRootView() );
            return true;
        }
        
        // Let Android handle the menu key
        else if( keyCode == KeyEvent.KEYCODE_MENU )
            return false;
        
        // Let Android handle the volume keys if not used for control
        else if( !Globals.userPrefs.isVolKeysEnabled
                && ( keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE ) )
            return false;
        
        // Send everything else to the peripheral controller if it exists
        else if( mPeripheralController1 != null )
            return mPeripheralController1.onKeyUnderride( view, keyCode, event );
        
        // Let Android handle whatever remains
        else
            return false;
    }

    private void setSlot( int value, MenuItem item )
    {
        setSlot( value );
        item.setChecked( true );
    }
    
    private void setSlot( int value )
    {
        setSlot( value, true );
    }
    
    private void setSlot( int value, boolean notify )
    {
        mSlot = value % NUM_SLOTS;
        NativeMethods.stateSetSlotEmulator( mSlot );
        if( notify )
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
        NativeMethods.fileSaveEmulator( Globals.userPrefs.selectedGameAutoSavefile );
        mSdlSurface.waitForResume();
    }
    
    @TargetApi( 11 )
    private void toggleActionBar( View rootView )
    {
        // Only applies to Honeycomb devices
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB )
            return;
        
        // Toggle the action bar
        ActionBar actionBar = mActivity.getActionBar();
        if( actionBar.isShowing() )
        {
            actionBar.hide();
            // Make the home buttons almost invisible
            if( rootView != null )
                rootView.setSystemUiVisibility( View.SYSTEM_UI_FLAG_LOW_PROFILE );
        }
        else
        {
            actionBar.show();
        }        
    }
}
