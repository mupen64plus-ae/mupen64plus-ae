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
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.game;

import java.io.File;
import java.util.ArrayList;

import org.mupen64plusae.v3.alpha.R;

import com.bda.controller.Controller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.DrawerDrawable;
import paulscode.android.mupen64plusae.GameSidebar;
import paulscode.android.mupen64plusae.GameSidebar.GameSidebarActionHandler;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog.PromptConfirmListener;
import paulscode.android.mupen64plusae.dialog.Popups;
import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptIntegerListener;
import paulscode.android.mupen64plusae.game.GameSurface.GameSurfaceCreatedListener;
import paulscode.android.mupen64plusae.hack.MogaHack;
import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.input.PeripheralController;
import paulscode.android.mupen64plusae.input.SensorController;
import paulscode.android.mupen64plusae.input.TouchController;
import paulscode.android.mupen64plusae.input.map.VisibleTouchMap;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae.input.provider.AxisProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider.ImeFormula;
import paulscode.android.mupen64plusae.input.provider.MogaProvider;
import paulscode.android.mupen64plusae.jni.CoreInterface;
import paulscode.android.mupen64plusae.jni.CoreInterface.OnExitListener;
import paulscode.android.mupen64plusae.jni.CoreInterface.OnPromptFinishedListener;
import paulscode.android.mupen64plusae.jni.CoreInterface.OnRestartListener;
import paulscode.android.mupen64plusae.jni.CoreInterface.OnSaveLoadListener;
import paulscode.android.mupen64plusae.jni.NativeConstants;
import paulscode.android.mupen64plusae.jni.NativeExports;
import paulscode.android.mupen64plusae.jni.NativeInput;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs.PakType;
import paulscode.android.mupen64plusae.profile.ControllerProfile;
import paulscode.android.mupen64plusae.util.RomDatabase;
import paulscode.android.mupen64plusae.util.RomDatabase.RomDetail;
import paulscode.android.mupen64plusae.util.RomHeader;

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

public class GameActivity extends AppCompatActivity implements PromptConfirmListener, SurfaceHolder.Callback, GameSidebarActionHandler,
OnPromptFinishedListener, OnSaveLoadListener, GameSurfaceCreatedListener, OnExitListener, OnRestartListener
{
    // Activity and views
    private GameSurface mSurface;
    private GameOverlay mOverlay;
    private GameDrawerLayout mDrawerLayout;
    private GameSidebar mGameSidebar;
    
    // Input resources
    private ArrayList<AbstractController> mControllers;
    private VisibleTouchMap mTouchscreenMap;
    private KeyProvider mKeyProvider;
    private Controller mMogaController;
    private SensorController mSensorController;
    
    // Intent data
    private String mRomPath;
    private String mRomMd5;
    private String mRomCrc;
    private String mRomHeaderName;
    private byte mRomCountryCode;
    private String mCheatArgs;
    private boolean mDoRestart;
    private String mArtPath;
    private String mRomGoodName;
    
    // Lifecycle state tracking
    private boolean mIsResumed = false;     // true if the activity is resumed
    private boolean mIsSurface = false;     // true if the surface is available
    
    // App data and user preferences
    private GlobalPrefs mGlobalPrefs;
    private GamePrefs mGamePrefs;
    private GameAutoSaveManager mAutoSaveManager;
    private boolean mFirstStart;
    private boolean mWaitingOnConfirmation = false;
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        Log.i( "GameActivity", "onCreate" );
        super.setTheme( android.support.v7.appcompat.R.style.Theme_AppCompat_NoActionBar );
        
        //Allow volume keys to control media volume if they are not mapped
        SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean volKeyMapped = mPreferences.getBoolean("inputVolumeMappable", false);
        AppData appData = new AppData( this );
        GlobalPrefs globalPrefs = new GlobalPrefs(this, appData);
        if (!volKeyMapped && globalPrefs.audioPlugin.enabled)
        {
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }
        
        mControllers = new ArrayList<AbstractController>();
        mMogaController = Controller.getInstance( this );
        
        // Get the intent data
        Bundle extras = this.getIntent().getExtras();
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
        
        // Initialize MOGA controller API
        // TODO: Remove hack after MOGA SDK is fixed
        // mMogaController.init();
        MogaHack.init( mMogaController, this );
        
        // Get app data and user preferences
        mGlobalPrefs = new GlobalPrefs( this, appData );

        mGamePrefs = new GamePrefs( this, mRomMd5, mRomCrc, mRomHeaderName,
            RomHeader.countryCodeToSymbol(mRomCountryCode), appData, mGlobalPrefs );
        mCheatArgs =  mGamePrefs.getCheatArgs();
        
        mAutoSaveManager = new GameAutoSaveManager(mGamePrefs, mGlobalPrefs.maxAutoSaves);

        mGlobalPrefs.enforceLocale( this );

        Window window = this.getWindow();
        
        // Enable full-screen mode
        window.setFlags( LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN );
        window.setFlags(LayoutParams.FLAG_LAYOUT_IN_SCREEN, LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        
        // Keep screen from going to sleep
        window.setFlags( LayoutParams.FLAG_KEEP_SCREEN_ON, LayoutParams.FLAG_KEEP_SCREEN_ON );
        
        // Set the screen orientation
        this.setRequestedOrientation( mGlobalPrefs.displayOrientation );
        
        // If the orientation changes, the screensize info changes, so we must refresh dependencies
        mGlobalPrefs = new GlobalPrefs( this, appData );
        mGamePrefs = new GamePrefs( this, mRomMd5, mRomCrc, mRomHeaderName,
                RomHeader.countryCodeToSymbol(mRomCountryCode), appData, mGlobalPrefs );
        
        mFirstStart = true;
        
        //TODO: Figure out why we call this in the middle
        super.onCreate( savedInstanceState );
        
        // Lay out content and get the views
        this.setContentView( R.layout.game_activity );
        mSurface = (GameSurface) this.findViewById( R.id.gameSurface );
        mOverlay = (GameOverlay) this.findViewById(R.id.gameOverlay);
        mDrawerLayout = (GameDrawerLayout) this.findViewById(R.id.drawerLayout);
        mGameSidebar = (GameSidebar) this.findViewById(R.id.gameSidebar);
        
        // Don't darken the game screen when the drawer is open
        mDrawerLayout.setScrimColor(0x0);

        // Make the background solid black
        mSurface.getRootView().setBackgroundColor(0xFF000000);
        mSurface.SetGameSurfaceCreatedListener(this);

        if (!TextUtils.isEmpty(mArtPath) && new File(mArtPath).exists())
            mGameSidebar.setImage(new BitmapDrawable(this.getResources(), mArtPath));

        mGameSidebar.setTitle(mRomGoodName);
        // Initialize the objects and data files interfacing to the emulator core
        CoreInterface.initialize( this, mSurface, mGamePrefs, mRomPath, mRomMd5, mCheatArgs, mDoRestart );

        // Handle events from the side bar
        mGameSidebar.setActionHandler(this, R.menu.game_drawer);
        
        //Reload menus
        ReloadAllMenus();
        
        // Listen to game surface events (created, changed, destroyed)
        mSurface.getHolder().addCallback( this );
        
        // Update the GameSurface size
        mSurface.getHolder().setFixedSize( mGamePrefs.videoRenderWidth, mGamePrefs.videoRenderHeight );
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mSurface.getLayoutParams();
        params.width = Math.round ( (float) mGamePrefs.videoSurfaceWidth * ( (float) mGamePrefs.videoSurfaceZoom / 100.f ) );
        params.height = Math.round ( (float) mGamePrefs.videoSurfaceHeight * ( (float) mGamePrefs.videoSurfaceZoom / 100.f ) );

        if( (mGlobalPrefs.displayOrientation & 1) == 1 ) 
            params.gravity = mGlobalPrefs.displayPosition | Gravity.CENTER_HORIZONTAL;
        else
            params.gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;
        
        mSurface.setLayoutParams( params );
        
        // Initialize the screen elements
        if( mGamePrefs.isTouchscreenEnabled || mGlobalPrefs.isFpsEnabled )
        {
            // The touch map and overlay are needed to display frame rate and/or controls
            mTouchscreenMap = new VisibleTouchMap( this.getResources() );
            mTouchscreenMap.load( mGamePrefs.touchscreenSkin, mGamePrefs.touchscreenProfile,
                    mGamePrefs.isTouchscreenAnimated, mGlobalPrefs.isFpsEnabled,
                    mGlobalPrefs.touchscreenScale, mGlobalPrefs.touchscreenTransparency );
            mOverlay.initialize(mTouchscreenMap, !mGamePrefs.isTouchscreenHidden, mGlobalPrefs.isFpsEnabled,
                    mGamePrefs.isAnalogHiddenWhenSensor, mGamePrefs.isTouchscreenAnimated);
        }
        
        // Initialize user interface devices
        initControllers(mOverlay);

        // Override the peripheral controllers' key provider, to add some extra
        // functionality
        mOverlay.setOnKeyListener(this);

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
        
        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener(){

            @Override
            public void onDrawerClosed(View arg0)
            {
                NativeExports.emuResume();
            }

            @Override
            public void onDrawerOpened(View arg0)
            {
                NativeExports.emuPause();
                ReloadAllMenus();
            }

            @Override
            public void onDrawerSlide(View arg0, float arg1)
            {

            }

            @Override
            public void onDrawerStateChanged(int newState)
            {

            }
            
        });
    }
    
    @SuppressWarnings("deprecation")
    @Override
    protected void onResume()
    {
        super.onResume();

        Log.i("GameActivity", "onResume");
        mIsResumed = true;

        tryRunning();

        mSensorController.onResume();

        // Set the sidebar opacity
        mGameSidebar.setBackgroundDrawable(new DrawerDrawable(
            mGlobalPrefs.displayActionBarTransparency));

        mMogaController.onResume();
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();

        Log.i( "GameActivity", "onPause" );
        mIsResumed = false;
        tryPausing();

        mSensorController.onPause();
        mMogaController.onPause();
    }
    
    @Override
    public void onWindowFocusChanged( boolean hasFocus )
    {
        super.onWindowFocusChanged( hasFocus );
        
        // Only try to run; don't try to pause. User may just be touching the in-game menu.
        Log.i( "GameActivity", "onWindowFocusChanged: " + hasFocus );
        if( hasFocus )
            hideSystemBars();
        
        //We don't want to do this every time the user uses a dialog,
        //only do it when the activity is first created.
        if(mFirstStart)
        {
            tryRunning();
            mFirstStart = false;
        }
    }

    @Override
    public void onPromptDialogClosed(int id, int which)
    {
        CoreInterface.onPromptDialogClosed(id, which);
    }
    
    private void ReloadAllMenus()
    {
        //Reload currently selected speed setting
        MenuItem toggleSpeedItem = 
            mGameSidebar.getMenu().findItem(R.id.menuItem_toggle_speed);
        toggleSpeedItem.setTitle(this.getString(R.string.menuItem_toggleSpeed, NativeExports.emuGetSpeed()));
        
        //Reload currently selected slot
        MenuItem slotItem = mGameSidebar.getMenu().findItem(R.id.menuItem_set_slot);
        slotItem.setTitle(this.getString(R.string.menuItem_setSlot, NativeExports.emuGetSlot()));
        
        int resId = NativeExports.emuGetFramelimiter() ?
            R.string.menuItem_disableFramelimiter :
            R.string.menuItem_enableFramelimiter;
        
        //Reload the menu with the new frame limiter setting
        MenuItem frameLimiterItem = 
            mGameSidebar.getMenu().findItem(R.id.menuItem_disable_frame_limiter);
        frameLimiterItem.setTitle(this.getString(resId, NativeExports.emuGetSpeed()));
        
        //Reload player pak settings
        UpdateControllerMenu(R.id.menuItem_player_one, mGamePrefs.isPlugged1, 1);
        UpdateControllerMenu(R.id.menuItem_player_two, mGamePrefs.isPlugged2, 2);
        UpdateControllerMenu(R.id.menuItem_player_three, mGamePrefs.isPlugged3, 3);
        UpdateControllerMenu(R.id.menuItem_player_four, mGamePrefs.isPlugged4, 4);
        
        mGameSidebar.reload();
    }
    
    private void UpdateControllerMenu(int menuItemId, boolean isPlugged, int playerNumber)
    {
        MenuItem pakGroupItem = mGameSidebar.getMenu().findItem(R.id.menuItem_paks);
        
        if(mGameSidebar.getMenu().findItem(menuItemId) != null)
        {
            if(!isPlugged)
            {
                pakGroupItem.getSubMenu().removeItem(menuItemId);
            }
            else
            {
                MenuItem playerItem = mGameSidebar.getMenu().findItem(menuItemId);
                playerItem.setTitleCondensed(this.getString(mGlobalPrefs.getPakType(playerNumber).getResourceString()));
            }
        }
    }
    
    @Override
    public void onPromptFinished()
    {
        //In here we only reload things that are updated through prompts
        
        //reload menu item with new slot
        MenuItem slotItem = mGameSidebar.getMenu().findItem(R.id.menuItem_set_slot);
        slotItem.setTitle(this.getString(R.string.menuItem_setSlot, NativeExports.emuGetSlot()));
        
        //Reload the menu with the new speed
        MenuItem toggleSpeedItem = 
            mGameSidebar.getMenu().findItem(R.id.menuItem_toggle_speed);
        toggleSpeedItem.setTitle(this.getString(R.string.menuItem_toggleSpeed, NativeExports.emuGetSpeed()));
        
        mGameSidebar.reload();
    }
    
    @Override
    public void onSaveLoad()
    {
        if( mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
        {
            mDrawerLayout.closeDrawer( GravityCompat.START );
        }
    }
    
    @Override
    public void onGameSidebarAction(MenuItem menuItem)
    {
        switch (menuItem.getItemId())
        {
        case R.id.menuItem_exit:
            mWaitingOnConfirmation = true;
            CoreInterface.exit();
            break;
        case R.id.menuItem_toggle_speed:
            CoreInterface.toggleSpeed();

            //Reload the menu with the new speed
            MenuItem toggleSpeedItem = 
                mGameSidebar.getMenu().findItem(R.id.menuItem_toggle_speed);
            toggleSpeedItem.setTitle(this.getString(R.string.menuItem_toggleSpeed, NativeExports.emuGetSpeed()));
            mGameSidebar.reload();
            break;
        case R.id.menuItem_set_speed:
            CoreInterface.setCustomSpeedFromPrompt(this);
            break;
        case R.id.menuItem_screenshot:
            CoreInterface.screenshot();
            break;
        case R.id.menuItem_set_slot:
            CoreInterface.setSlotFromPrompt(this);
            break;
        case R.id.menuItem_slot_load:
            CoreInterface.loadSlot(this);
            break;
        case R.id.menuItem_slot_save:
            CoreInterface.saveSlot(this);
            if( mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
            {
                mDrawerLayout.closeDrawer( GravityCompat.START );
            }
            break;
        case R.id.menuItem_file_load:
            CoreInterface.loadFileFromPrompt(this);
            break;
        case R.id.menuItem_file_save:
            CoreInterface.saveFileFromPrompt();
            break;
        case R.id.menuItem_file_load_auto_save:
            CoreInterface.loadAutoSaveFromPrompt(this);
            break;
        case R.id.menuItem_disable_frame_limiter:
            CoreInterface.toggleFramelimiter();
            
            int resId = NativeExports.emuGetFramelimiter() ?
                R.string.menuItem_disableFramelimiter :
                R.string.menuItem_enableFramelimiter;
            
            //Reload the menu with the new speed
            MenuItem frameLimiterItem = 
                mGameSidebar.getMenu().findItem(R.id.menuItem_disable_frame_limiter);
            frameLimiterItem.setTitle(this.getString(resId, NativeExports.emuGetSpeed()));
            mGameSidebar.reload();
            break;
        case R.id.menuItem_player_one:
            setPakTypeFromPrompt(1, mGlobalPrefs.getPakType(1).ordinal(), this);
            break;
        case R.id.menuItem_player_two:
            setPakTypeFromPrompt(2, mGlobalPrefs.getPakType(2).ordinal(), this);
            break;
        case R.id.menuItem_player_three:
            setPakTypeFromPrompt(3, mGlobalPrefs.getPakType(3).ordinal(), this);
            break;
        case R.id.menuItem_player_four:
            setPakTypeFromPrompt(4, mGlobalPrefs.getPakType(4).ordinal(), this);
            break;
        case R.id.menuItem_setIme:
            InputMethodManager imeManager = (InputMethodManager) this
                .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imeManager != null)
                imeManager.showInputMethodPicker();
            break;
        case R.id.menuItem_reset:
            mWaitingOnConfirmation = true;
            CoreInterface.restart();
            break;
        default:
        }
    }
    
    private CharSequence GetPlayerTextFromId(int playerId)
    {
        CharSequence title = null;

        switch(playerId)
        {
        case 1:
            title = this.getString(R.string.menuItem_player_one);
            break;
        case 2:
            title = this.getString(R.string.menuItem_player_two);
            break;
        case 3:
            title = this.getString(R.string.menuItem_player_three);
            break;
        case 4:
            title = this.getString(R.string.menuItem_player_four);
            break;
        }
        
        return title;
    }
    
    private MenuItem GetPlayerMenuItemFromId(int playerId)
    {
        MenuItem playerMenuItem = null;

        switch(playerId)
        {
        case 1:
            playerMenuItem = mGameSidebar.getMenu().findItem(R.id.menuItem_player_one);
            break;
        case 2:
            playerMenuItem = mGameSidebar.getMenu().findItem(R.id.menuItem_player_two);
            break;
        case 3:
            playerMenuItem = mGameSidebar.getMenu().findItem(R.id.menuItem_player_three);
            break;
        case 4:
            playerMenuItem = mGameSidebar.getMenu().findItem(R.id.menuItem_player_four);
            break;
        }
        
        return playerMenuItem;
    }
    
    public void setPakTypeFromPrompt(final int player, final int selectedPakType,
        final OnPromptFinishedListener promptFinishedListener)
    {
        //First get the prompt title
        CharSequence title = GetPlayerTextFromId(player);
        final MenuItem playerMenuItem = GetPlayerMenuItemFromId(player);
        
        //Generate possible pak types
        final ArrayList<CharSequence> selections = new ArrayList<CharSequence>();
        for(PakType pakType:PakType.values())
        {
            selections.add(this.getString(pakType.getResourceString()));
        }
            
        Prompt.promptListSelection( this, title, selections,
                new PromptIntegerListener()
                {
                    @Override
                    public void onDialogClosed( Integer value, int which )
                    {
                        if( which == DialogInterface.BUTTON_POSITIVE )
                        {
                            mGlobalPrefs.putPakType(player, PakType.values()[value]);
                            
                            // Set the pak in the core
                            NativeInput.setConfig( player - 1, true, PakType.values()[value].getNativeValue() );
                            
                            //Update the menu
                            playerMenuItem.setTitleCondensed(GameActivity.this.getString(mGlobalPrefs.getPakType(player).getResourceString()));
                            mGameSidebar.reload();
                        }
                    }
                } );
    }
    
    @Override
    public void surfaceCreated( SurfaceHolder holder )
    {
        Log.i( "GameActivity", "surfaceCreated" );
    }
    
    @Override
    public void surfaceChanged( SurfaceHolder holder, int format, int width, int height )
    {
        Log.i( "GameActivity", "surfaceChanged" );
        NativeExports.notifySDLSurfaceReady();
        mIsSurface = true;
        tryRunning();
    }
    
    @Override
    public void surfaceDestroyed( SurfaceHolder holder )
    {
        Log.i( "GameActivity", "surfaceDestroyed" );
        NativeExports.notifySDLSurfaceDestroyed();
        mSurface.setEGLContextNotReady();
        mIsSurface = false;
        tryPausing();
    }
    
    @Override
    public void onRestart(boolean shouldRestart)
    {        
        if(shouldRestart)
        {
            CoreInterface.restartEmulator();
            
            if( mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
            {
                mDrawerLayout.closeDrawer( GravityCompat.START );
            }
        }
        else if( !mDrawerLayout.isDrawerOpen( GravityCompat.START ))
        {
            NativeExports.emuResume();
        }
        
        mWaitingOnConfirmation = false;
    }
    
    @Override
    public void onExit(boolean shouldExit)
    {
        if(shouldExit)
        {
            // Never go directly from running to stopped; always pause (and autosave) first
            String saveFileName = mAutoSaveManager.getAutoSaveFileName();
            CoreInterface.pauseEmulator( true, saveFileName );
            mAutoSaveManager.clearOldest();
            CoreInterface.shutdownEmulator();
            
            mMogaController.exit();
            
            this.finish();
        }
        else if( !mDrawerLayout.isDrawerOpen( GravityCompat.START ))
        {
            NativeExports.emuResume();
        }
        
        mWaitingOnConfirmation = false;
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
            {
                mDrawerLayout.closeDrawer( GravityCompat.START );
            }
            else
            {
                mWaitingOnConfirmation = true;
                CoreInterface.exit();
            }
            return true;
        }
        
        // Let the PeripheralControllers and Android handle everything else
        else
        {
            if( !mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
            {
                // If PeripheralControllers exist and handle the event,
                // they return true. Else they return false, signaling
                // Android to handle the event (menu button, vol keys).
                if( mKeyProvider != null )
                    return mKeyProvider.onKey( view, keyCode, event );
            }
            
            return false;
        }
    }
    
    @SuppressLint( "InlinedApi" )
    private void initControllers( View inputSource )
    {
        // By default, send Player 1 rumbles through phone vibrator
        Vibrator vibrator = (Vibrator) this.getSystemService( Context.VIBRATOR_SERVICE );
        CoreInterface.registerVibrator( 1, vibrator );
        
        // Create the touchscreen controls
        if( mGamePrefs.isTouchscreenEnabled )
        {
            if (!mGamePrefs.sensorAxisX.isEmpty() || !mGamePrefs.sensorAxisY.isEmpty()) {
                // Create the sensor controller
                SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                mSensorController = new SensorController(sensorManager, mOverlay, mGamePrefs.sensorAxisX,
                        mGamePrefs.sensorSensitivityX, mGamePrefs.sensorAngleX, mGamePrefs.sensorAxisY,
                        mGamePrefs.sensorSensitivityY, mGamePrefs.sensorAngleY);
                mControllers.add(mSensorController);
                if (mGamePrefs.sensorActivateOnStart) {
                    mSensorController.setSensorEnabled(true);
                    mOverlay.onSensorEnabled(true);
                }
            }

            // Create the touchscreen controller
            TouchController touchscreenController = new TouchController( mTouchscreenMap,
                    inputSource, mOverlay, vibrator, mGlobalPrefs.touchscreenAutoHold,
                    mGlobalPrefs.isTouchscreenFeedbackEnabled, mGamePrefs.touchscreenAutoHoldables,
                    mSensorController );
            mControllers.add( touchscreenController );
            
            mDrawerLayout.setTouchMap( mTouchscreenMap );
        }
        
        //Check for controller configuration
        boolean needs1 = false;
        boolean needs2 = false;
        boolean needs3 = false;
        boolean needs4 = false;

        // Popup the multi-player dialog if necessary and abort if any players are unassigned
        RomDatabase romDatabase = RomDatabase.getInstance();
          
        if(!romDatabase.hasDatabaseFile())
        {
            AppData appData = new AppData(this);
            romDatabase.setDatabaseFile(appData.mupen64plus_ini);
        }
        
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
                Popups.showNeedsPlayerMap( this );
            }
        }
        
        // Create the input providers shared among all peripheral controllers
        mKeyProvider = new KeyProvider( inputSource, ImeFormula.DEFAULT,
                mGlobalPrefs.unmappableKeyCodes );
        MogaProvider mogaProvider = new MogaProvider( mMogaController );
        AbstractProvider axisProvider = new AxisProvider( inputSource );
        
        // Create the peripheral controls to handle key/stick presses
        if( mGamePrefs.isControllerEnabled1 && !needs1)
        {
            ControllerProfile p = mGamePrefs.controllerProfile1;
            mControllers.add( new PeripheralController( 1, mGamePrefs.playerMap, p.getMap(), p.getDeadzone(),
                    p.getSensitivity(), mOverlay, mSensorController, mKeyProvider, axisProvider, mogaProvider ) );
        }
        if( mGamePrefs.isControllerEnabled2 && !needs2)
        {
            ControllerProfile p = mGamePrefs.controllerProfile2;
            mControllers.add( new PeripheralController( 2, mGamePrefs.playerMap, p.getMap(), p.getDeadzone(),
                    p.getSensitivity(), mOverlay, null, mKeyProvider, axisProvider, mogaProvider ) );
        }
        if( mGamePrefs.isControllerEnabled3 && !needs3)
        {
            ControllerProfile p = mGamePrefs.controllerProfile3;
            mControllers.add( new PeripheralController( 3, mGamePrefs.playerMap, p.getMap(), p.getDeadzone(),
                    p.getSensitivity(), mOverlay, null, mKeyProvider, axisProvider, mogaProvider ) );
        }
        if( mGamePrefs.isControllerEnabled4 && !needs4)
        {
            ControllerProfile p = mGamePrefs.controllerProfile4;
            mControllers.add( new PeripheralController( 4, mGamePrefs.playerMap, p.getMap(), p.getDeadzone(),
                    p.getSensitivity(), mOverlay, null, mKeyProvider, axisProvider, mogaProvider ) );
        }
    }
    
    @SuppressLint( "InlinedApi" )
    private void hideSystemBars()
    {
        if( mDrawerLayout != null )
        {
            if( AppData.IS_KITKAT && mGlobalPrefs.isImmersiveModeEnabled )
            {
                mDrawerLayout.setSystemUiVisibility( View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN );
            }
            else
            {
                mDrawerLayout.setSystemUiVisibility( View.SYSTEM_UI_FLAG_LOW_PROFILE ); // == STATUS_BAR_HIDDEN for Honeycomb
            }
        }
    }
    
    private boolean isSafeToRender()
    {
        return mIsResumed && mIsSurface;
    }
    
    private void tryRunning()
    {        
        int state = NativeExports.emuGetState();
        if( isSafeToRender() && ( state != NativeConstants.EMULATOR_STATE_RUNNING ))
        {
            switch( state )
            {
                case NativeConstants.EMULATOR_STATE_UNKNOWN:
                    String latestSave = mAutoSaveManager.getLatestAutoSave();
                    CoreInterface.startupEmulator(latestSave);
                    break;
                case NativeConstants.EMULATOR_STATE_PAUSED:
                    if( mSurface.isEGLContextReady() && !mDrawerLayout.isDrawerOpen( GravityCompat.START )
                        && !mWaitingOnConfirmation)
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
            CoreInterface.pauseEmulator( false, null );
    }

    @Override
    public void onGameSurfaceCreated()
    {
        if( !mDrawerLayout.isDrawerOpen( GravityCompat.START ) && !mWaitingOnConfirmation)
        {
            NativeExports.emuResume();
        }
        else
        {
            //Advance 1 frame so that something is shown instead of a black screen
            CoreInterface.advanceFrame();
        }
    }
}
