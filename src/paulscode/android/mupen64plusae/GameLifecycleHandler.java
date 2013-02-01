/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2013 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
 * not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: paulscode, lioncash, littleguy77
 */
package paulscode.android.mupen64plusae;

import java.util.ArrayList;

import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.input.PeripheralController;
import paulscode.android.mupen64plusae.input.TouchController;
import paulscode.android.mupen64plusae.input.map.TouchMap;
import paulscode.android.mupen64plusae.input.map.VisibleTouchMap;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae.input.provider.AxisProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider.ImeFormula;
import paulscode.android.mupen64plusae.input.provider.NativeInputSource;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.Demultiplexer;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.InputDevice;
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
    
    // Input resources
    private final ArrayList<AbstractController> mControllers;
    private VisibleTouchMap mTouchscreenMap;
    private KeyProvider mKeyProvider;
    
    // Internal flags
    private boolean mCoreRunning = false;
    private final boolean mIsXperiaPlay;
    
    // App data and user preferences
    private AppData mAppData;
    private UserPrefs mUserPrefs;
    
    public GameLifecycleHandler( Activity activity )
    {
        mActivity = activity;
        mControllers = new ArrayList<AbstractController>();
        mIsXperiaPlay = !( activity instanceof GameActivity );
    }
    
    @TargetApi( 11 )
    public void onCreateBegin( Bundle savedInstanceState )
    {
        // Get app data and user preferences
        mAppData = new AppData( mActivity );
        mUserPrefs = new UserPrefs( mActivity );
        mUserPrefs.enforceLocale( mActivity );
        
        // Load native libraries
        if( mIsXperiaPlay )
            FileUtil.loadNativeLibName( "xperia-touchpad" );
        FileUtil.loadNativeLibName( "SDL" );
        FileUtil.loadNativeLibName( "core" );
        FileUtil.loadNativeLibName( "front-end" );
        FileUtil.loadNativeLib( mUserPrefs.videoPlugin.path );
        FileUtil.loadNativeLib( mUserPrefs.audioPlugin.path );
        FileUtil.loadNativeLib( mUserPrefs.inputPlugin.path );
        FileUtil.loadNativeLib( mUserPrefs.rspPlugin.path );
        
        // For Honeycomb, let the action bar overlay the rendered view (rather than squeezing it)
        // For earlier APIs, remove the title bar to yield more space
        Window window = mActivity.getWindow();
        if( AppData.IS_HONEYCOMB && !mUserPrefs.isOuyaMode )
            window.requestFeature( Window.FEATURE_ACTION_BAR_OVERLAY );
        else
            window.requestFeature( Window.FEATURE_NO_TITLE );
        
        // Enable full-screen mode
        window.setFlags( LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN );
        
        // Keep screen from going to sleep
        window.setFlags( LayoutParams.FLAG_KEEP_SCREEN_ON, LayoutParams.FLAG_KEEP_SCREEN_ON );
        
        // Set the screen orientation
        mActivity.setRequestedOrientation( mUserPrefs.videoOrientation );
    }
    
    @TargetApi( 11 )
    public void onCreateEnd( Bundle savedInstanceState )
    {
        // Take control of the GameSurface if necessary
        if( mIsXperiaPlay )
            mActivity.getWindow().takeSurface( null );
        
        // Lay out content and get the views
        mActivity.setContentView( R.layout.game_activity );
        mSurface = (GameSurface) mActivity.findViewById( R.id.gameSurface );
        mOverlay = (GameOverlay) mActivity.findViewById( R.id.gameOverlay );
        
        // Hide the action bar introduced in higher Android versions
        if( AppData.IS_HONEYCOMB && !mUserPrefs.isOuyaMode )
        {
            // SDK version at least HONEYCOMB, so there should be software buttons on this device:
            View view = mSurface.getRootView();
            if( view != null )
                view.setSystemUiVisibility( View.SYSTEM_UI_FLAG_LOW_PROFILE );
            mActivity.getActionBar().hide();
        }
        
        // Initialize the screen elements
        if( mUserPrefs.isTouchscreenEnabled || mUserPrefs.isFpsEnabled )
        {
            // The touch map and overlay are needed to display frame rate and/or controls
            mTouchscreenMap = new VisibleTouchMap( mActivity.getResources(),
                    mUserPrefs.isFpsEnabled, mAppData.fontsDir, mUserPrefs.touchscreenTransparency);
            mTouchscreenMap.load( mUserPrefs.touchscreenLayout );
            mOverlay.initialize( mTouchscreenMap, mUserPrefs.touchscreenRefresh,
                    mUserPrefs.isFpsEnabled, !mUserPrefs.isTouchscreenHidden );
        }
        
        // Initialize user interface devices
        View inputSource = mIsXperiaPlay ? new NativeInputSource( mActivity ) : mSurface;
        if( mUserPrefs.inputPlugin.enabled )
            initControllers( inputSource );
        Vibrator vibrator = (Vibrator) mActivity.getSystemService( Context.VIBRATOR_SERVICE );
        
        // Override the peripheral controllers' key provider, to add some extra functionality
        inputSource.setOnKeyListener( this );
        
        // Start listening to game surface events
        mSurface.init( this, mOverlay, mUserPrefs.videoFpsRefresh, mUserPrefs.isRgba8888 );
        
        // Refresh the objects and data files interfacing to the emulator core
        CoreInterface.refresh( mActivity, mSurface, vibrator );
    }
    
    public void onResume()
    {
        if( mCoreRunning )
        {
            Notifier.showToast( mActivity, R.string.toast_loadingSession );
            NativeMethods.fileLoadEmulator( mUserPrefs.selectedGameAutoSavefile );
            NativeMethods.resumeEmulator();
        }
    }
    
    public void onPause()
    {
        if( mCoreRunning )
        {
            NativeMethods.pauseEmulator();
            Notifier.showToast( mActivity, R.string.toast_savingSession );
            NativeMethods.fileSaveEmulator( mUserPrefs.selectedGameAutoSavefile );
        }
    }
    
    @Override
    public void onCoreStartup()
    {
        mCoreRunning = true;
        Notifier.showToast( mActivity, R.string.toast_loadingSession );
        
        if( !CoreInterface.isRestarting() )
            NativeMethods.fileLoadEmulator( mUserPrefs.selectedGameAutoSavefile );
        
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
        boolean keyDown = event.getAction() == KeyEvent.ACTION_DOWN;
        
        // For devices with an action bar, absorb all back key presses
        // and toggle the action bar
        if( keyCode == KeyEvent.KEYCODE_BACK && AppData.IS_HONEYCOMB && !mUserPrefs.isOuyaMode )
        {
            if( keyDown )
                toggleActionBar( view.getRootView() );
            return true;
        }
        
        // Let the PeripheralControllers and Android handle everything else
        else
        {
            // If PeripheralControllers exist and handle the event,
            // they return true. Else they return false, signaling
            // Android to handle the event (menu button, vol keys).
            if( mKeyProvider != null )
                return mKeyProvider.onKey( view, keyCode, event );
            
            return false;
        }
    }
    
    private void initControllers( View inputSource )
    {
        // Create the touchpad controls, if applicable
        TouchController touchpadController = null;
        Vibrator vibrator = (Vibrator) mActivity.getSystemService( Context.VIBRATOR_SERVICE );
        
        if( mIsXperiaPlay )
        {
            // Create the map for the touchpad
            TouchMap touchpadMap = new TouchMap( mActivity.getResources() );
            touchpadMap.load( mUserPrefs.touchpadLayout );
            touchpadMap.resize( NativeInputSource.PAD_WIDTH, NativeInputSource.PAD_HEIGHT );
            
            // Create the touchpad controller
            touchpadController = new TouchController( touchpadMap, inputSource, null,
                    mUserPrefs.isOctagonalJoystick, vibrator);
            mControllers.add( touchpadController );
            
            // Filter by source identifier
            touchpadController.setSourceFilter( InputDevice.SOURCE_TOUCHPAD );
        }
        
        // Create the touchscreen controls
        if( mUserPrefs.isTouchscreenEnabled )
        {
            // Create the touchscreen controller
            TouchController touchscreenController = new TouchController( mTouchscreenMap,
                    inputSource, mOverlay, mUserPrefs.isOctagonalJoystick, vibrator );
            mControllers.add( touchscreenController );
            
            // If using touchpad & touchscreen together...
            if( touchpadController != null )
            {
                // filter by source identifier...
                touchscreenController.setSourceFilter( InputDevice.SOURCE_TOUCHSCREEN );
                
                // and demux the input source to both touch listeners
                Demultiplexer.OnTouchListener demux = new Demultiplexer.OnTouchListener();
                demux.addListener( touchpadController );
                demux.addListener( touchscreenController );
                inputSource.setOnTouchListener( demux );
            }
        }
        
        // Create the input providers shared among all peripheral controllers
        mKeyProvider = new KeyProvider( inputSource, ImeFormula.DEFAULT,
                mUserPrefs.unmappableKeyCodes );
        AbstractProvider axisProvider = AppData.IS_HONEYCOMB_MR1
                ? new AxisProvider( inputSource )
                : null;
        
        // Create the peripheral controls to handle key/stick presses
        if( mUserPrefs.isInputEnabled1 )
        {
            mControllers.add( new PeripheralController( 1, mUserPrefs.playerMap,
                    mUserPrefs.inputMap1, mKeyProvider, axisProvider ) );
        }
        if( mUserPrefs.isInputEnabled2 )
        {
            mControllers.add( new PeripheralController( 2, mUserPrefs.playerMap,
                    mUserPrefs.inputMap2, mKeyProvider, axisProvider ) );
        }
        if( mUserPrefs.isInputEnabled3 )
        {
            mControllers.add( new PeripheralController( 3, mUserPrefs.playerMap,
                    mUserPrefs.inputMap3, mKeyProvider, axisProvider ) );
        }
        if( mUserPrefs.isInputEnabled4 )
        {
            mControllers.add( new PeripheralController( 4, mUserPrefs.playerMap,
                    mUserPrefs.inputMap4, mKeyProvider, axisProvider ) );
        }
    }
    
    @TargetApi( 11 )
    private void toggleActionBar( View rootView )
    {
        // Only applies to Honeycomb devices
        if( !AppData.IS_HONEYCOMB )
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
