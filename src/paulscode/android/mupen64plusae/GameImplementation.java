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

import java.io.File;

import paulscode.android.mupen64plusae.input.PeripheralController;
import paulscode.android.mupen64plusae.input.TouchscreenController;
import paulscode.android.mupen64plusae.input.map.VisibleTouchMap;
import paulscode.android.mupen64plusae.input.provider.AxisProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider.ImeFormula;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.Prompt.OnFileListener;
import paulscode.android.mupen64plusae.util.Prompt.OnTextListener;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
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
    private VisibleTouchMap mTouchscreenMap;
    private TouchscreenView mTouchscreenView;
    @SuppressWarnings( "unused" )
    private TouchscreenController mTouchscreenController;
    private KeyProvider mKeyProvider;
    private AxisProvider mAxisProvider;
    private PeripheralController mPeripheralController1;
    private PeripheralController mPeripheralController2;
    private PeripheralController mPeripheralController3;
    private PeripheralController mPeripheralController4;
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
        if( Globals.IS_HONEYCOMB )
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
        if( Globals.IS_HONEYCOMB)
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
        FileUtil.loadNativeLibName( "SDL" );
        FileUtil.loadNativeLibName( "core" );
        FileUtil.loadNativeLibName( "front-end" );
        if( Globals.userPrefs.isVideoEnabled )
            FileUtil.loadNativeLib( Globals.userPrefs.videoPlugin );
        if( Globals.userPrefs.isAudioEnabled )
            FileUtil.loadNativeLib( Globals.userPrefs.audioPlugin );
        if( Globals.userPrefs.isInputEnabled )
            FileUtil.loadNativeLib( Globals.userPrefs.inputPlugin );
        if( Globals.userPrefs.isRspEnabled )
            FileUtil.loadNativeLib( Globals.userPrefs.rspPlugin );
        
        // Initialize user interface devices
        initTouchscreen();
        initPeripherals();
        Vibrator vibrator = (Vibrator) mActivity.getSystemService( Context.VIBRATOR_SERVICE );
        
        // Override the key provider, to add some extra functionality
        mSdlSurface.setOnKeyListener( this );
        
        // Synchronize the interface to the emulator core
        CoreInterface.startup( mActivity, mSdlSurface, vibrator );
        
        // Notify user that the game activity has started
        Notifier.showToast( mActivity, R.string.toast_appStarted );
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
                saveStateToFile();
                break;
            case R.id.ingameLoad:
                loadStateFromFile();
                break;
            case R.id.ingameMenu:
                // Save game state and launch MenuActivity
                saveSession();
                Notifier.clear();
                CoreInterface.shutdown();
                Intent intent = new Intent( mActivity, MenuActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                mActivity.startActivity( intent );
                // mActivity.finish();
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
        if( keyCode == KeyEvent.KEYCODE_BACK && Globals.IS_HONEYCOMB )
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
                        || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE ) )
            return false;
        
        // Let the PeripheralControllers' provider handle everything else
        else if( mKeyProvider != null )
            return mKeyProvider.onKey( view, keyCode, event );
        
        // Let Android handle whatever remains
        else
            return false;
    }
    
    private void initTouchscreen()
    {
        if( Globals.IS_ECLAIR
                && ( Globals.userPrefs.isTouchscreenEnabled || Globals.userPrefs.isFrameRateEnabled ) )
        {
            // The touch map and view are needed to display frame rate and/or controls
            mTouchscreenMap = new VisibleTouchMap( mActivity.getResources(),
                    Globals.userPrefs.isFrameRateEnabled, Globals.paths.fontsDir );
            mTouchscreenMap.load( Globals.userPrefs.touchscreenLayoutFolder );
            mTouchscreenView.initialize( mTouchscreenMap );
            mSdlSurface.initialize( mTouchscreenMap );
            
            // The touch controller is needed to handle touch events
            if( Globals.userPrefs.isInputEnabled && Globals.userPrefs.isTouchscreenEnabled )
            {
                mTouchscreenController = new TouchscreenController( mTouchscreenMap, mSdlSurface, Globals.userPrefs.isOctagonalJoystick );
            }
        }
    }
    
    private void initPeripherals()
    {
        if( Globals.userPrefs.isInputEnabled )
        {
            // Create the input providers shared among all peripheral controllers
            mKeyProvider = new KeyProvider( mSdlSurface, ImeFormula.DEFAULT );
            if( Globals.IS_HONEYCOMB_MR1 )
                mAxisProvider = new AxisProvider( mSdlSurface );
            else
                mAxisProvider = null;
            
            // Create the peripheral controllers for players 1-4
            if( Globals.userPrefs.inputMap1.isEnabled() )
            {
                mPeripheralController1 = new PeripheralController( Globals.userPrefs.inputMap1,
                        mKeyProvider, mAxisProvider );
                mPeripheralController1.setPlayerNumber( 1 );
            }
            if( Globals.userPrefs.inputMap2.isEnabled() )
            {
                mPeripheralController2 = new PeripheralController( Globals.userPrefs.inputMap2,
                        mKeyProvider, mAxisProvider );
                mPeripheralController2.setPlayerNumber( 2 );
            }
            if( Globals.userPrefs.inputMap3.isEnabled() )
            {
                mPeripheralController3 = new PeripheralController( Globals.userPrefs.inputMap3,
                        mKeyProvider, mAxisProvider );
                mPeripheralController3.setPlayerNumber( 3 );
            }
            if( Globals.userPrefs.inputMap4.isEnabled() )
            {
                mPeripheralController4 = new PeripheralController( Globals.userPrefs.inputMap4,
                        mKeyProvider, mAxisProvider );
                mPeripheralController4.setPlayerNumber( 4 );
            }
        }
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
            Notifier.showToast( mActivity, R.string.toast_savegameSlot, mSlot );
        if( mSlotMenuItem != null )
            mSlotMenuItem.setTitle( mActivity.getString( R.string.ingameSlot_title, mSlot ) );
    }
    
    private void saveSession()
    {
        if( !Globals.userPrefs.isAutoSaveEnabled )
            return;
        
        // Pop up a toast message
        if( mActivity != null )
            Notifier.showToast( mActivity, R.string.toast_savingGame );
        
        // Call the native method to save the emulator state
        NativeMethods.fileSaveEmulator( Globals.userPrefs.selectedGameAutoSavefile );
        mSdlSurface.waitForResume();
    }
    
    private void loadStateFromFile()
    {
        Prompt.promptFile( mActivity, mActivity.getText( R.string.ingameLoad_title ), null,
                new File( Globals.userPrefs.gameSaveDir ), new OnFileListener()
                {
                    @Override
                    public void onFile( File file )
                    {
                        NativeMethods.fileLoadEmulator( file.getAbsolutePath() );
                    }
                } );
    }
    
    private void saveStateToFile()
    {
        CharSequence title = mActivity.getText( R.string.ingameSave_title );
        CharSequence hint = mActivity.getText( R.string.gameImplementation_saveHint );
        Prompt.promptText( mActivity, title, null, hint, new OnTextListener()
        {
            @Override
            public void onText( CharSequence text )
            {
                saveStateToFile( text.toString() );
            }
        } );
    }
    
    private void saveStateToFile( final String filename )
    {
        final File file = new File( Globals.userPrefs.gameSaveDir + "/" + filename );
        if( file.exists() )
        {
            String title = mActivity.getString( R.string._confirmation );
            String message = mActivity
                    .getString( R.string.gameImplementation_confirmFile, filename );
            Prompt.promptConfirm( mActivity, title, message, new OnClickListener()
            {
                @Override
                public void onClick( DialogInterface dialog, int which )
                {
                    if( which == DialogInterface.BUTTON_POSITIVE )
                    {
                        Log.i( "GameImplementation", "Overwriting file " + filename );
                        NativeMethods.fileSaveEmulator( file.getAbsolutePath() );
                    }
                }
            } );
        }
        else
        {
            Log.i( "GameImplementation", "Saving file " + filename );
            NativeMethods.fileSaveEmulator( file.getAbsolutePath() );
        }
    }
    
    @TargetApi( 11 )
    private void toggleActionBar( View rootView )
    {
        // Only applies to Honeycomb devices
        if( !Globals.IS_HONEYCOMB )
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
