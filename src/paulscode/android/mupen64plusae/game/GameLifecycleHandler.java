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
package paulscode.android.mupen64plusae.game;

import java.io.File;
import java.util.ArrayList;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.DrawerDrawable;
import paulscode.android.mupen64plusae.GameSidebar;
import paulscode.android.mupen64plusae.dialog.Popups;
import paulscode.android.mupen64plusae.hack.MogaHack;
import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.input.PeripheralController;
import paulscode.android.mupen64plusae.input.TouchController;
import paulscode.android.mupen64plusae.input.map.TouchMap;
import paulscode.android.mupen64plusae.input.map.VisibleTouchMap;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae.input.provider.AxisProvider;
import paulscode.android.mupen64plusae.input.provider.Demultiplexer;
import paulscode.android.mupen64plusae.input.provider.KeyProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider.ImeFormula;
import paulscode.android.mupen64plusae.input.provider.MogaProvider;
import paulscode.android.mupen64plusae.jni.CoreInterface;
import paulscode.android.mupen64plusae.jni.NativeConstants;
import paulscode.android.mupen64plusae.jni.NativeExports;
import paulscode.android.mupen64plusae.jni.NativeInput;
import paulscode.android.mupen64plusae.jni.NativeXperiaTouchpad;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.profile.ControllerProfile;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.RomDatabase;
import paulscode.android.mupen64plusae.util.RomHeader;
import paulscode.android.mupen64plusae.util.RomDatabase.RomDetail;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.view.GravityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.bda.controller.Controller;

//@formatter:off
/**
* (start)
*    |
* onCreate <-- (killed) <---------\
*    |                            |
* onStart  <-- onRestart <-----\  |
*    |                         |  |
* onResume <----------------\  |  |
*    |                      |  |  |
* [*onSurfaceCreated*]      |  |  |
*    |                      |  |  |
* [*onSurfaceChanged*]      |  |  |
*    |                      |  |  |
* [*onWindowFocusChanged*]  |  |  |
*    |                      |  |  |
* (running)                 |  |  |
*    |                      |  |  |
* [*onWindowFocusChanged*]  |  |  |
*    |                      |  |  |
* onPause ------------------/  |  |
*    |                         |  |
* [*onSurfaceDestroyed*]       |  |
*    |                         |  |
* onStop ----------------------/--/
*    |
* onDestroy
*    |
* (end)
* 
* 
* [*non-deterministic sequence*]
* 
* 
*/
//@formatter:on

public class GameLifecycleHandler implements View.OnKeyListener, SurfaceHolder.Callback
{
    // Activity and views
    private Activity mActivity;
    private GameSurface mSurface;
    private GameOverlay mOverlay;
    private GameDrawerLayout mDrawerLayout;
    private GameSidebar mGameSidebar;
    
    // Input resources
    private final ArrayList<AbstractController> mControllers;
    private VisibleTouchMap mTouchscreenMap;
    private KeyProvider mKeyProvider;
    private Controller mMogaController;
    
    // Internal flags
    private final boolean mIsXperiaPlay;
    
    // Intent data
    private final String mRomPath;
    private final String mRomMd5;
    private final String mRomCrc;
    private final String mRomHeaderName;
    private final byte mRomCountryCode;
    private String mCheatArgs;
    private final boolean mDoRestart;
    private final String mArtPath;
    private final String mRomGoodName;
    
    // Lifecycle state tracking
    private boolean mIsFocused = false;     // true if the window is focused
    private boolean mIsResumed = false;     // true if the activity is resumed
    private boolean mIsSurface = false;     // true if the surface is available
    
    // App data and user preferences
    private GlobalPrefs mGlobalPrefs;
    private GamePrefs mGamePrefs;
    private GameAutoSaveManager mAutoSaveManager;
    private boolean mShowSlot = false;
    private int mShowPak = -1;
    
    public GameLifecycleHandler( Activity activity )
    {
        mActivity = activity;
        mControllers = new ArrayList<AbstractController>();
        mIsXperiaPlay = !( activity instanceof GameActivity );
        mMogaController = Controller.getInstance( mActivity );
        
        // Get the intent data
        Bundle extras = mActivity.getIntent().getExtras();
        if( extras == null )
            throw new Error( "ROM path and MD5 must be passed via the extras bundle when starting GameActivity" );
        mRomPath = extras.getString( ActivityHelper.Keys.ROM_PATH );
        mRomMd5 = extras.getString( ActivityHelper.Keys.ROM_MD5 );
        mRomCrc = extras.getString( ActivityHelper.Keys.ROM_CRC );
        mRomHeaderName = extras.getString( ActivityHelper.Keys.ROM_HEADER_NAME );
        mRomCountryCode = extras.getByte( ActivityHelper.Keys.ROM_COUNTRY_CODE );
        mArtPath = extras.getString( ActivityHelper.Keys.ROM_ART_PATH );
        mRomGoodName = extras.getString( ActivityHelper.Keys.ROM_GOOD_NAME );
        mDoRestart = extras.getBoolean( ActivityHelper.Keys.DO_RESTART, false );
        if( TextUtils.isEmpty( mRomPath ) || TextUtils.isEmpty( mRomMd5 ) )
            throw new Error( "ROM path and MD5 must be passed via the extras bundle when starting GameActivity" );
    }
    
    @TargetApi( 11 )
    public void onCreateBegin( Bundle savedInstanceState )
    {
        Log.i( "GameLifecycleHandler", "onCreate" );
        
        // Initialize MOGA controller API
        // TODO: Remove hack after MOGA SDK is fixed
        // mMogaController.init();
        MogaHack.init( mMogaController, mActivity );
        
        // Get app data and user preferences
        mGlobalPrefs = new GlobalPrefs( mActivity );

        mGamePrefs = new GamePrefs( mActivity, mRomMd5, mRomCrc, mRomHeaderName, RomHeader.countryCodeToSymbol(mRomCountryCode) );
        mCheatArgs =  mGamePrefs.getCheatArgs();
        
        mAutoSaveManager = new GameAutoSaveManager(mGamePrefs);

        mGlobalPrefs.enforceLocale( mActivity );

        Window window = mActivity.getWindow();
        
        // Enable full-screen mode
        window.setFlags( LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN );
        
        // Keep screen from going to sleep
        window.setFlags( LayoutParams.FLAG_KEEP_SCREEN_ON, LayoutParams.FLAG_KEEP_SCREEN_ON );
        
        // Set the screen orientation
        mActivity.setRequestedOrientation( mGlobalPrefs.displayOrientation );
        
        // If the orientation changes, the screensize info changes, so we must refresh dependencies
        mGlobalPrefs = new GlobalPrefs( mActivity );
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
        mOverlay = (GameOverlay) mActivity.findViewById(R.id.gameOverlay);
        mDrawerLayout = (GameDrawerLayout) mActivity.findViewById(R.id.drawerLayout);
        mGameSidebar = (GameSidebar) mActivity.findViewById(R.id.gameSidebar);

        // Don't darken the game screen when the drawer is open
        mDrawerLayout.setScrimColor(0x0);

        // Make the background solid black
        mSurface.getRootView().setBackgroundColor(0xFF000000);

        if (!TextUtils.isEmpty(mArtPath) && new File(mArtPath).exists())
            mGameSidebar.setImage(new BitmapDrawable(mActivity.getResources(), mArtPath));

        mGameSidebar.setTitle(mRomGoodName);
        updateSidebar();

        // Initialize the objects and data files interfacing to the emulator core
        CoreInterface.initialize( mActivity, mSurface, mGamePrefs, mRomPath, mRomMd5, mCheatArgs, mDoRestart );

        
        // Listen to game surface events (created, changed, destroyed)
        mSurface.getHolder().addCallback( this );
        
        // Update the GameSurface size
        mSurface.getHolder().setFixedSize( mGlobalPrefs.videoRenderWidth, mGlobalPrefs.videoRenderHeight );
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mSurface.getLayoutParams();
        params.width = mGlobalPrefs.videoSurfaceWidth;
        params.height = mGlobalPrefs.videoSurfaceHeight;
        params.gravity = mGlobalPrefs.displayPosition | Gravity.CENTER_HORIZONTAL;
        mSurface.setLayoutParams( params );
        
        // Initialize the screen elements
        if( mGamePrefs.isTouchscreenEnabled || mGlobalPrefs.isFpsEnabled )
        {
            // The touch map and overlay are needed to display frame rate and/or controls
            mTouchscreenMap = new VisibleTouchMap( mActivity.getResources() );
            mTouchscreenMap.load( mGlobalPrefs.touchscreenSkin, mGamePrefs.touchscreenProfile,
                    mGlobalPrefs.isTouchscreenAnimated, mGlobalPrefs.isFpsEnabled,
                    mGlobalPrefs.touchscreenScale, mGlobalPrefs.touchscreenTransparency );
            mOverlay.initialize( mTouchscreenMap, !mGamePrefs.isTouchscreenHidden,
                    mGlobalPrefs.isFpsEnabled, mGlobalPrefs.isTouchscreenAnimated );
        }
        
        // Initialize user interface devices
        View inputSource = mIsXperiaPlay ? new NativeXperiaTouchpad(mActivity) : mOverlay;
        initControllers(inputSource);

        // Override the peripheral controllers' key provider, to add some extra
        // functionality
        inputSource.setOnKeyListener(this);

        if (savedInstanceState == null)
        {
            // Show the drawer at the start and have it hide itself
            // automatically
            mDrawerLayout.openDrawer(GravityCompat.START);
            mDrawerLayout.postDelayed(new Runnable()
            {
                public void run()
                {
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                }
            }, 1000);
        }
    }

    @TargetApi(11)
    protected void updateSidebar()
    {
        mGameSidebar.clear();

        // Save & Exit
        mGameSidebar.addRow(R.drawable.ic_undo, mActivity.getString(R.string.menuItem_exit), null,
            new GameSidebar.Action()
            {
                @Override
                public void onAction()
                {
                    mActivity.finish();
                }
            });

        // Speed
        mGameSidebar.addRow(R.drawable.ic_clock,
            mActivity.getString(R.string.menuItem_toggleSpeed, NativeExports.emuGetSpeed()), null,
            new GameSidebar.Action()
            {
                @Override
                public void onAction()
                {
                    CoreInterface.toggleSpeed();
                    updateSidebar();
                }
            });

        mGameSidebar.addRow(0x0, mActivity.getString(R.string.menuItem_setSpeed), null, new GameSidebar.Action()
        {
            @Override
            public void onAction()
            {
                CoreInterface.setCustomSpeedFromPrompt();
            }
        });

        mGameSidebar.addRow(R.drawable.ic_picture, mActivity.getString(R.string.menuItem_screenshot), null,
            new GameSidebar.Action()
            {
                @Override
                public void onAction()
                {
                    CoreInterface.screenshot();
                }
            });

        // Set slot
        mGameSidebar.addRow(R.drawable.ic_storage,
            mActivity.getString(R.string.menuItem_setSlot, NativeExports.emuGetSlot()), null, new GameSidebar.Action()
            {
                @Override
                public void onAction()
                {
                    mShowSlot = !mShowSlot;
                    updateSidebar();
                }
            }, (mShowSlot) ? R.drawable.ic_arrow_u : R.drawable.ic_arrow_d, 0);

        if (mShowSlot)
        {
            // Expand the Slot selection panel by adding the custom view to the
            // sidebar
            for (int slot = 0; slot <= 9; slot++)
            {
                int icon = R.drawable.ic_box;
                if (slot == NativeExports.emuGetSlot())
                    icon = R.drawable.ic_check;

                final int finalSlot = slot;
                mGameSidebar.addRow(0x0, mActivity.getString(R.string.menuItem_setSlot, slot), null,
                    new GameSidebar.Action()
                    {
                        @Override
                        public void onAction()
                        {
                            CoreInterface.setSlot(finalSlot);
                            mShowSlot = false;
                            updateSidebar();
                        }
                    }, icon, 1);
            }
        }

        // Slot load
        mGameSidebar.addRow(R.drawable.ic_folder, mActivity.getString(R.string.menuItem_slotLoad), null,
            new GameSidebar.Action()
            {
                @Override
                public void onAction()
                {
                    CoreInterface.loadSlot();
                }
            });

        // Slot save
        mGameSidebar.addRow(R.drawable.ic_save, mActivity.getString(R.string.menuItem_slotSave), null,
            new GameSidebar.Action()
            {
                @Override
                public void onAction()
                {
                    CoreInterface.saveSlot();
                }
            });

        mGameSidebar.addRow(0x0, mActivity.getString(R.string.menuItem_fileLoad), null, new GameSidebar.Action()
        {
            @Override
            public void onAction()
            {
                CoreInterface.loadFileFromPrompt();
            }
        });

        mGameSidebar.addRow(0x0, mActivity.getString(R.string.menuItem_fileSave), null, new GameSidebar.Action()
        {
            @Override
            public void onAction()
            {
                CoreInterface.saveFileFromPrompt();
            }
        });
        
        mGameSidebar.addRow(0x0, mActivity.getString(R.string.menuItem_fileLoadAutoSave), null, new GameSidebar.Action()
        {
            @Override
            public void onAction()
            {
                CoreInterface.loadAutoSaveFromPrompt();
            }
        });

        int resId = NativeExports.emuGetFramelimiter() ? R.string.menuItem_disableFramelimiter
            : R.string.menuItem_enableFramelimiter;

        mGameSidebar.addRow(0x0, mActivity.getString(resId), null, new GameSidebar.Action()
        {
            @Override
            public void onAction()
            {
                CoreInterface.toggleFramelimiter();
                updateSidebar();
            }
        });

        mGameSidebar.addRow(0x0, mActivity.getString(R.string.menuItem_paks), null, new GameSidebar.Action()
        {
            @Override
            public void onAction()
            {
                mShowPak = (mShowPak == -1) ? 0 : -1;
                updateSidebar();
            }
        }, (mShowPak >= 0) ? R.drawable.ic_arrow_u : R.drawable.ic_arrow_d, 0);

        if (mShowPak >= 0)
        {
            boolean[] plugged = { mGamePrefs.isPlugged1, mGamePrefs.isPlugged2, mGamePrefs.isPlugged3,
                mGamePrefs.isPlugged4 };

            final String[][] pakTypes = {
                { mActivity.getString(R.string.menuItem_pak1_empty), mActivity.getString(R.string.menuItem_pak1_mem),
                    mActivity.getString(R.string.menuItem_pak1_rumble) },
                { mActivity.getString(R.string.menuItem_pak2_empty), mActivity.getString(R.string.menuItem_pak2_mem),
                    mActivity.getString(R.string.menuItem_pak2_rumble) },
                { mActivity.getString(R.string.menuItem_pak3_empty), mActivity.getString(R.string.menuItem_pak3_mem),
                    mActivity.getString(R.string.menuItem_pak3_rumble) },
                { mActivity.getString(R.string.menuItem_pak4_empty), mActivity.getString(R.string.menuItem_pak4_mem),
                    mActivity.getString(R.string.menuItem_pak4_rumble) } };

            final int[] pakMap = { NativeConstants.PAK_TYPE_NONE, NativeConstants.PAK_TYPE_MEMORY,
                NativeConstants.PAK_TYPE_RUMBLE };

            // Hide Rumble Pak menu item if not available
            Vibrator vibrator = (Vibrator) mActivity.getSystemService(Context.VIBRATOR_SERVICE);
            boolean hasPhoneVibrator = AppData.IS_HONEYCOMB ? vibrator.hasVibrator() : vibrator != null;

            for (int player = 1; player <= 4; player++)
            {
                if (!plugged[player - 1])
                    continue;

                int pakIndex;
                switch (mGlobalPrefs.getPakType(player))
                {
                default:
                case NativeConstants.PAK_TYPE_NONE:
                    pakIndex = 0;
                    break;
                case NativeConstants.PAK_TYPE_MEMORY:
                    pakIndex = 1;
                    break;
                case NativeConstants.PAK_TYPE_RUMBLE:
                    pakIndex = 2;
                    break;
                }

                final String[] pakString = pakTypes[player - 1][pakIndex].split(": ");
                final int finalPlayer = player;

                mGameSidebar.addRow(0x0, pakString[0], pakString[pakString.length - 1], new GameSidebar.Action()
                {
                    @Override
                    public void onAction()
                    {
                        mShowPak = (mShowPak == finalPlayer) ? 0 : finalPlayer;
                        updateSidebar();
                    }
                }, (mShowPak == player) ? R.drawable.ic_arrow_u : R.drawable.ic_arrow_d, 1);

                if (mShowPak != player)
                    continue;

                boolean permitRumble = AppData.IS_JELLY_BEAN || (player == 1 && hasPhoneVibrator);

                // Show the options for this player
                for (int pak = 0; pak < pakTypes[0].length; pak++)
                {
                    if (!permitRumble && pak == 2)
                        continue;

                    int icon = R.drawable.ic_box;
                    if (pakIndex == pak)
                        icon = R.drawable.ic_check;

                    final int finalPak = pak;
                    final String[] pakName = pakTypes[player - 1][pak].split(": ");

                    mGameSidebar.addRow(0x0, pakName[pakName.length - 1], null, new GameSidebar.Action()
                    {
                        @Override
                        public void onAction()
                        {
                            // Set the new Pak type for this player
                            int newPakIndex = pakMap[finalPak];
                            mGlobalPrefs.putPakType(finalPlayer, newPakIndex);
                            NativeInput.setConfig(finalPlayer - 1, true, newPakIndex);

                            // Send a toast message
                            Notifier.showToast(mActivity, pakTypes[finalPlayer - 1][finalPak]);

                            // Collapse this section in the sidebar
                            mShowPak = 0;
                            updateSidebar();
                        }
                    }, icon, 2);
                }
            }
        }

        mGameSidebar.addRow(0x0, mActivity.getString(R.string.menuItem_setIme), null, new GameSidebar.Action()
        {
            @Override
            public void onAction()
            {
                InputMethodManager imeManager = (InputMethodManager) mActivity
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imeManager != null)
                    imeManager.showInputMethodPicker();
            }
        });
    }

    public void onStart()
    {
        Log.i( "GameLifecycleHandler", "onStart" );
    }
    
    public void onResume()
    {
        Log.i("GameLifecycleHandler", "onResume");
        mIsResumed = true;
        tryRunning();

        // Set the sidebar opacity
        mGameSidebar.setBackgroundDrawable(new DrawerDrawable(
            mGlobalPrefs.displayActionBarTransparency));

        mMogaController.onResume();
    }
    
    @Override
    public void surfaceCreated( SurfaceHolder holder )
    {
        Log.i( "GameLifecycleHandler", "surfaceCreated" );
    }
    
    @Override
    public void surfaceChanged( SurfaceHolder holder, int format, int width, int height )
    {
        Log.i( "GameLifecycleHandler", "surfaceChanged" );
        mIsSurface = true;
        tryRunning();
    }
    
    public void onWindowFocusChanged( boolean hasFocus )
    {
        // Only try to run; don't try to pause. User may just be touching the in-game menu.
        Log.i( "GameLifecycleHandler", "onWindowFocusChanged: " + hasFocus );
        mIsFocused = hasFocus;
        if( hasFocus )
            hideSystemBars();
        tryRunning();
    }
    
    public void onPause()
    {
        Log.i( "GameLifecycleHandler", "onPause" );
        mIsResumed = false;
        tryPausing();
        
        mMogaController.onPause();
    }
    
    @Override
    public void surfaceDestroyed( SurfaceHolder holder )
    {
        Log.i( "GameLifecycleHandler", "surfaceDestroyed" );
        mIsSurface = false;
        tryStopping();
    }
    
    public void onStop()
    {
        Log.i( "GameLifecycleHandler", "onStop" );
    }
    
    public void onDestroy()
    {
        Log.i( "GameLifecycleHandler", "onDestroy" );
        mMogaController.exit();
    }

    @Override
    public boolean onKey( View view, int keyCode, KeyEvent event )
    {
        boolean keyDown = event.getAction() == KeyEvent.ACTION_DOWN;
        
        if( keyDown && keyCode == KeyEvent.KEYCODE_MENU )
        {
            if( mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
                mDrawerLayout.closeDrawer( GravityCompat.START );
            else
                mDrawerLayout.openDrawer( GravityCompat.START );
            return true;
        }
        else if( keyDown && keyCode == KeyEvent.KEYCODE_BACK )
        {
            if( mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
                mDrawerLayout.closeDrawer( GravityCompat.START );
            else
                mActivity.finish();
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
    
    @SuppressLint( "InlinedApi" )
    private void initControllers( View inputSource )
    {
        // By default, send Player 1 rumbles through phone vibrator
        Vibrator vibrator = (Vibrator) mActivity.getSystemService( Context.VIBRATOR_SERVICE );
        CoreInterface.registerVibrator( 1, vibrator );
        
        // Create the touchpad controls, if applicable
        TouchController touchpadController = null;
        if( mIsXperiaPlay )
        {
            // Create the map for the touchpad
            TouchMap touchpadMap = new TouchMap( mActivity.getResources() );
            touchpadMap.load( mGlobalPrefs.touchpadSkin, mGlobalPrefs.touchpadProfile, false );
            touchpadMap.resize( NativeXperiaTouchpad.PAD_WIDTH, NativeXperiaTouchpad.PAD_HEIGHT );
            
            // Create the touchpad controller
            touchpadController = new TouchController( touchpadMap, inputSource, null, vibrator,
                    TouchController.AUTOHOLD_METHOD_DISABLED, mGlobalPrefs.isTouchpadFeedbackEnabled,
                    null );
            mControllers.add( touchpadController );
            
            // Filter by source identifier
            touchpadController.setSourceFilter( InputDevice.SOURCE_TOUCHPAD );
        }
        
        // Create the touchscreen controls
        if( mGamePrefs.isTouchscreenEnabled )
        {
            // Create the touchscreen controller
            TouchController touchscreenController = new TouchController( mTouchscreenMap,
                    inputSource, mOverlay, vibrator, mGlobalPrefs.touchscreenAutoHold,
                    mGlobalPrefs.isTouchscreenFeedbackEnabled, mGamePrefs.touchscreenAutoHoldables );
            mControllers.add( touchscreenController );
            
            mDrawerLayout.setTouchMap( mTouchscreenMap );
            
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
        
        //Check for controller configuration
        boolean needs1 = false;
        boolean needs2 = false;
        boolean needs3 = false;
        boolean needs4 = false;
        AppData appData = new AppData(mActivity);

        // Popup the multi-player dialog if necessary and abort if any players are unassigned
        RomDatabase romDatabase = new RomDatabase( appData.mupen64plus_ini );
        RomDetail romDetail = romDatabase.lookupByMd5WithFallback( mRomMd5, new File( mRomPath ), mRomCrc );
        if( romDetail.players > 1 && mGamePrefs.playerMap.isEnabled()
                && mGlobalPrefs.getPlayerMapReminder() )
        {
            mGamePrefs.playerMap.removeUnavailableMappings();
            needs1 = mGamePrefs.isControllerEnabled1 && !mGamePrefs.playerMap.isMapped( 1 );
            needs2 = mGamePrefs.isControllerEnabled2 && !mGamePrefs.playerMap.isMapped( 2 );
            needs3 = mGamePrefs.isControllerEnabled3 && !mGamePrefs.playerMap.isMapped( 3 )
                    && romDetail.players > 2;
            needs4 = mGamePrefs.isControllerEnabled4 && !mGamePrefs.playerMap.isMapped( 4 )
                    && romDetail.players > 3;
            
            if( needs1 || needs2 || needs3 || needs4 )
            {
// TODO FIXME
//                  @SuppressWarnings( "deprecation" )
//                  PlayerMapPreference pref = (PlayerMapPreference) findPreference( "playerMap" );
//                  pref.show();
//                  return;
                Popups.showNeedsPlayerMap( mActivity );
            }
        }
        
        // Create the input providers shared among all peripheral controllers
        mKeyProvider = new KeyProvider( inputSource, ImeFormula.DEFAULT,
                mGlobalPrefs.unmappableKeyCodes );
        MogaProvider mogaProvider = new MogaProvider( mMogaController );
        AbstractProvider axisProvider = AppData.IS_HONEYCOMB_MR1
                ? new AxisProvider( inputSource )
                : null;
        
        // Create the peripheral controls to handle key/stick presses
        if( mGamePrefs.isControllerEnabled1 && !needs1)
        {
            ControllerProfile p = mGamePrefs.controllerProfile1;
            mControllers.add( new PeripheralController( 1, mGamePrefs.playerMap, p.getMap(), p.getDeadzone(),
                    p.getSensitivity(), mKeyProvider, axisProvider, mogaProvider ) );
        }
        if( mGamePrefs.isControllerEnabled2 && !needs2)
        {
            ControllerProfile p = mGamePrefs.controllerProfile2;
            mControllers.add( new PeripheralController( 2, mGamePrefs.playerMap, p.getMap(), p.getDeadzone(),
                    p.getSensitivity(), mKeyProvider, axisProvider, mogaProvider ) );
        }
        if( mGamePrefs.isControllerEnabled3 && !needs3)
        {
            ControllerProfile p = mGamePrefs.controllerProfile3;
            mControllers.add( new PeripheralController( 3, mGamePrefs.playerMap, p.getMap(), p.getDeadzone(),
                    p.getSensitivity(), mKeyProvider, axisProvider, mogaProvider ) );
        }
        if( mGamePrefs.isControllerEnabled4 && !needs4)
        {
            ControllerProfile p = mGamePrefs.controllerProfile4;
            mControllers.add( new PeripheralController( 4, mGamePrefs.playerMap, p.getMap(), p.getDeadzone(),
                    p.getSensitivity(), mKeyProvider, axisProvider, mogaProvider ) );
        }
    }
    
    @SuppressLint( "InlinedApi" )
    @TargetApi( 11 )
    private void hideSystemBars()
    {
        // Only applies to Honeycomb devices
        if( !AppData.IS_HONEYCOMB )
            return;
        
        View view = mSurface.getRootView();
        if( view != null )
        {
            if( AppData.IS_KITKAT && mGlobalPrefs.isImmersiveModeEnabled )
                view.setSystemUiVisibility( View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN );
            else
                view.setSystemUiVisibility( View.SYSTEM_UI_FLAG_LOW_PROFILE ); // == STATUS_BAR_HIDDEN for Honeycomb
        }
    }
    
    private boolean isSafeToRender()
    {
        return mIsFocused && mIsResumed && mIsSurface;
    }
    
    private void tryRunning()
    {        
        int state = NativeExports.emuGetState();
        if( isSafeToRender() && ( state != NativeConstants.EMULATOR_STATE_RUNNING ))
        {
            String latestSave = mAutoSaveManager.getLatestAutoSave();
            switch( state )
            {
                case NativeConstants.EMULATOR_STATE_UNKNOWN:
                    CoreInterface.startupEmulator(latestSave);
                    break;
                case NativeConstants.EMULATOR_STATE_PAUSED:
                    CoreInterface.resumeEmulator();
                    break;
                default:
                    break;
            }
        }
    }
    
    private void tryPausing()
    {
        if( NativeExports.emuGetState() != NativeConstants.EMULATOR_STATE_PAUSED )
        {
            String saveFileName = mAutoSaveManager.getAutoSaveFileName();
            
            CoreInterface.pauseEmulator( true, saveFileName );
            
            mAutoSaveManager.clearOldest();
        }
    }
    
    private void tryStopping()
    {
        if( NativeExports.emuGetState() != NativeConstants.EMULATOR_STATE_STOPPED )
        {
            // Never go directly from running to stopped; always pause (and autosave) first
            tryPausing();
            CoreInterface.shutdownEmulator();
        }
    }
}