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
import paulscode.android.mupen64plusae.input.map.VisibleTouchMap;
import paulscode.android.mupen64plusae.input.provider.AxisProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider.ImeFormula;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;

//@formatter:off
/**
 * (start)
 *    |
 * onCreate <-- (killed) <-----\
 *    |                        |
 * onStart <-- onRestart <--\  |
 *    |                     |  |
 * onResume <------------\  |  |
 *    |                  |  |  |
 * [*onSurfaceCreated*]  |  |  |
 *    |                  |  |  |
 * [*onSurfaceChanged*]  |  |  |
 *    |                  |  |  |
 * (running)             |  |  |   
 *    |                  |  |  |
 * onPause --------------/  |  |
 *    |                     |  |
 * [*onSurfaceDestroyed*]   |  |
 *    |                     |  |
 * onStop ------------------/--/
 *    |
 * onDestroy
 *    |
 * (end)
 * 
 * 
 * [*doesn't always occur*]
 * 
 * 
 */
//@formatter:on

public class GameLifecycleHandler implements View.OnKeyListener, GameSurface.CoreLifecycleListener
{
    // Activity and views
    private Activity mActivity;
    private GameSurface mSurface;
    private GameOverlay mOverlay;
    
    // Internal flags
    boolean mCoreRunning = false;
    
    // Input helpers
    private VisibleTouchMap mTouchscreenMap;
    private KeyProvider mKeyProvider;
    private AxisProvider mAxisProvider;
    
    // Controllers
    @SuppressWarnings( "unused" )
    private TouchscreenController mTouchscreenController;
    private PeripheralController mPeripheralController1;
    private PeripheralController mPeripheralController2;
    private PeripheralController mPeripheralController3;
    private PeripheralController mPeripheralController4;
    
    public GameLifecycleHandler( Activity activity )
    {
        mActivity = activity;
    }
    
    @TargetApi( 11 )
    public void onCreate( Bundle savedInstanceState )
    {
        // Notify user that the game activity has started
        Notifier.showToast( mActivity, R.string.toast_appStarted );

        // Lay out content and initialize stuff
        
        // For Honeycomb, let the action bar overlay the rendered view (rather than squeezing it)
        // For earlier APIs, remove the title bar to yield more space
        Window window = mActivity.getWindow();
        if( Globals.IS_HONEYCOMB )
            window.requestFeature( Window.FEATURE_ACTION_BAR_OVERLAY );
        else
            window.requestFeature( Window.FEATURE_NO_TITLE );
        
        // Enable full-screen mode
        window.setFlags( LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN );
        
        // Keep screen on under certain conditions
        window.setFlags( LayoutParams.FLAG_KEEP_SCREEN_ON, LayoutParams.FLAG_KEEP_SCREEN_ON );
        
        // Get the views
        mActivity.setContentView( R.layout.game_activity );
        mSurface = (GameSurface) mActivity.findViewById( R.id.gameSurface );
        mOverlay = (GameOverlay) mActivity.findViewById( R.id.gameOverlay );
        
        // Hide the action bar introduced in higher Android versions
        if( Globals.IS_HONEYCOMB )
        {
            // SDK version at least HONEYCOMB, so there should be software buttons on this device:
            View view = mSurface.getRootView();
            if( view != null )
                view.setSystemUiVisibility( View.SYSTEM_UI_FLAG_LOW_PROFILE );
            mActivity.getActionBar().hide();
        }
        
        // Load native libraries
        FileUtil.loadNativeLibName( "SDL" );
        FileUtil.loadNativeLibName( "core" );
        FileUtil.loadNativeLibName( "front-end" );
        FileUtil.loadNativeLib( Globals.userPrefs.videoPlugin );
        FileUtil.loadNativeLib( Globals.userPrefs.audioPlugin );
        FileUtil.loadNativeLib( Globals.userPrefs.inputPlugin );
        FileUtil.loadNativeLib( Globals.userPrefs.rspPlugin );
        
        // Initialize user interface devices
        initTouchscreen();
        initPeripherals();
        Vibrator vibrator = (Vibrator) mActivity.getSystemService( Context.VIBRATOR_SERVICE );
        
        // Override the peripheral controllers' key provider, to add some extra functionality
        mSurface.setOnKeyListener( this );
        
        // Start listening to game surface events
        mSurface.setListeners( this, mOverlay, mTouchscreenMap.getFpsRecalcPeriod() );
        
        // Refresh the objects and data files interfacing to the emulator core
        CoreInterface.refresh( mActivity, mSurface, vibrator );
    }
    
    public void onResume()
    {
        if( mCoreRunning )
        {
            Notifier.showToast( mActivity, R.string.toast_loadingSession );
            NativeMethods.fileLoadEmulator( Globals.userPrefs.selectedGameAutoSavefile );
            NativeMethods.resumeEmulator();
        }
    }
    
    public void onPause()
    {
        if( mCoreRunning )
        {
            NativeMethods.pauseEmulator();
            Notifier.showToast( mActivity, R.string.toast_savingSession );
            NativeMethods.fileSaveEmulator( Globals.userPrefs.selectedGameAutoSavefile );
        }
    }
    
    @Override
    public void onCoreStartup()
    {
        mCoreRunning = true;
        Notifier.showToast( mActivity, R.string.toast_loadingSession );
        NativeMethods.fileLoadEmulator( Globals.userPrefs.selectedGameAutoSavefile );
        NativeMethods.resumeEmulator();
    }

    @Override
    public void onCoreShutdown()
    {
        mCoreRunning = false;
    }

    @Override
    public boolean onKey( View view, int keyCode, KeyEvent event )
    {
        Log.i( "GameLifecycleHandler", "onKey " + keyCode + ": " );
        
        boolean keyDown = event.getAction() == KeyEvent.ACTION_DOWN;
        
        if( keyCode == KeyEvent.KEYCODE_BACK )
        {
            // Absorb all back key presses, and toggle the ActionBar if applicable
            if( keyDown )
                toggleActionBar( view.getRootView() );
            return true;
        }
        
        // Let Android handle the menu key if available
        else if( keyCode == KeyEvent.KEYCODE_MENU )
        {
            return false;
        }
        
        // Let Android handle the volume keys if not used for control
        else if( !Globals.userPrefs.isVolKeysEnabled
                && ( keyCode == KeyEvent.KEYCODE_VOLUME_UP
                        || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE ) )
            return false;
        
        // Let the PeripheralControllers handle everything else
        else if( mKeyProvider != null )
            return mKeyProvider.onKey( view, keyCode, event );
        
        // Let Android handle everything else if no PeripheralControllers
        else
            return false;
    }
    
    private void initTouchscreen()
    {
        if( Globals.IS_ECLAIR
                && ( Globals.userPrefs.isTouchscreenEnabled || Globals.userPrefs.isFrameRateEnabled ) )
        {
            // The touch map and overlay are needed to display frame rate and/or controls
            mTouchscreenMap = new VisibleTouchMap( mActivity.getResources(),
                    Globals.userPrefs.isFrameRateEnabled, Globals.paths.fontsDir );
            mTouchscreenMap.load( Globals.userPrefs.touchscreenLayout );
            mOverlay.initialize( mTouchscreenMap );
            
            // The touch controller is needed to handle touch events
            if( Globals.userPrefs.isInputEnabled && Globals.userPrefs.isTouchscreenEnabled )
            {
                mTouchscreenController = new TouchscreenController( mTouchscreenMap, mSurface, mOverlay,
                        Globals.userPrefs.isOctagonalJoystick );
            }
        }
    }
    
    private void initPeripherals()
    {
        if( Globals.userPrefs.isInputEnabled && Globals.userPrefs.isPeripheralEnabled )
        {
            // Create the input providers shared among all peripheral controllers
            mKeyProvider = new KeyProvider( mSurface, ImeFormula.DEFAULT );
            if( Globals.IS_HONEYCOMB_MR1 )
                mAxisProvider = new AxisProvider( mSurface );
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
            
            // Make the home buttons almost invisible again
            if( rootView != null )
                rootView.setSystemUiVisibility( View.SYSTEM_UI_FLAG_LOW_PROFILE );
        }
        else
        {
            actionBar.show();
        }
    }
}
