/*
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.bda.controller.Controller;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.DrawerDrawable;
import paulscode.android.mupen64plusae.GameSidebar;
import paulscode.android.mupen64plusae.GameSidebar.GameSidebarActionHandler;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog.PromptConfirmListener;
import paulscode.android.mupen64plusae.dialog.Popups;
import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptIntegerListener;
import paulscode.android.mupen64plusae.hack.MogaHack;
import paulscode.android.mupen64plusae.input.PeripheralController;
import paulscode.android.mupen64plusae.input.SensorController;
import paulscode.android.mupen64plusae.input.TouchController;
import paulscode.android.mupen64plusae.input.map.VisibleTouchMap;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae.input.provider.AxisProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider.ImeFormula;
import paulscode.android.mupen64plusae.input.provider.MogaProvider;
import paulscode.android.mupen64plusae.jni.CoreFragment;
import paulscode.android.mupen64plusae.jni.CoreFragment.CoreEventListener;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs.PakType;
import paulscode.android.mupen64plusae.profile.ControllerProfile;
import paulscode.android.mupen64plusae.util.CountryCode;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.RomDatabase;
import paulscode.android.mupen64plusae.util.RomDatabase.RomDetail;

import static paulscode.android.mupen64plusae.ActivityHelper.Keys.ROM_PATH;
import static paulscode.android.mupen64plusae.persistent.GlobalPrefs.DEFAULT_LOCALE_OVERRIDE;

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

public class GameActivity extends AppCompatActivity implements PromptConfirmListener, SurfaceHolder.Callback,
        GameSidebarActionHandler, CoreEventListener, View.OnTouchListener
{
    // Activity and views
    private GameOverlay mOverlay;
    private GameDrawerLayout mDrawerLayout;
    private GameSidebar mGameSidebar;
    private SurfaceView mSurfaceView;

    // Input resources
    private VisibleTouchMap mTouchscreenMap;
    private KeyProvider mKeyProvider;
    private AxisProvider mAxisProvider;
    private Controller mMogaController;
    TouchController mTouchscreenController;
    private SensorController mSensorController;
    private int mLastTouchTime;
    private Handler mHandler;

    // args data
    private boolean mShouldExit = false;
    private String mRomPath = null;
    private String mRomMd5 = null;
    private String mRomCrc = null;
    private String mRomGoodName = null;
    private String mRomHeaderName = null;
    private byte mRomCountryCode = 0;
    private String mRomArtPath = null;
    private String mRomLegacySave = null;
    private boolean mDoRestart = false;

    // App data and user preferences
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;
    private GamePrefs mGamePrefs = null;
    private GameDataManager mGameDataManager = null;

    private static final String STATE_DRAWER_OPEN = "STATE_DRAWER_OPEN";
    private boolean mDrawerOpenState = false;

    private static final String STATE_CORE_FRAGMENT = "STATE_CORE_FRAGMENT";
    private CoreFragment mCoreFragment = null;

    @Override
    protected void attachBaseContext(Context newBase) {


        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences( newBase );

        // Locale
        String localeCode = preferences.getString( GlobalPrefs.KEY_LOCALE_OVERRIDE, DEFAULT_LOCALE_OVERRIDE );

        if(TextUtils.isEmpty(localeCode))
        {
            super.attachBaseContext(newBase);
        }
        else
        {
            super.attachBaseContext(LocaleContextWrapper.wrap(newBase,localeCode));
        }
    }

    @Override
    protected void onNewIntent( Intent intent )
    {
        Log.i("GameActivity", "onNewIntent");
        // If the activity is already running and is launched again (e.g. from a file manager app),
        // the existing instance will be reused rather than a new one created. This behavior is
        // specified in the manifest (launchMode = singleTask). In that situation, any activities
        // above this on the stack (e.g. GameActivity, GamePrefsActivity) will be destroyed
        // gracefully and onNewIntent() will be called on this instance. onCreate() will NOT be
        // called again on this instance.
        super.onNewIntent( intent );

        // Only remember the last intent used
        setIntent( intent );
        final Bundle extras = this.getIntent().getExtras();

        if(extras != null)
        {
            mShouldExit = extras.getBoolean(ActivityHelper.Keys.EXIT_GAME);

            Log.i("GameActivity", "mShouldExit=" + mShouldExit);

            if(mShouldExit && mCoreFragment != null)
            {
                mCoreFragment.shutdownEmulator();
                finishAffinity();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i("GameActivity", "onCreate");
        super.onCreate(savedInstanceState);
        super.setTheme( android.support.v7.appcompat.R.style.Theme_AppCompat_NoActionBar );

        mAppData = new AppData( this );

        mMogaController = Controller.getInstance( this );

        // Initialize the objects and data files interfacing to the emulator core
        final FragmentManager fm = this.getSupportFragmentManager();
        mCoreFragment = (CoreFragment) fm.findFragmentByTag(STATE_CORE_FRAGMENT);

        if(mCoreFragment == null)
        {
            mCoreFragment = new CoreFragment();
            fm.beginTransaction().add(mCoreFragment, STATE_CORE_FRAGMENT).commit();
        }

        mCoreFragment.setCoreEventListener(this);

        // Get the intent data
        final Bundle extras = this.getIntent().getExtras();
        if( extras == null )
        {
            finish();
            return;
        }

        mShouldExit = extras.getBoolean(ActivityHelper.Keys.EXIT_GAME);

        Log.i("GameActivity", "mShouldExit=" + mShouldExit);

        mRomPath = extras.getString( ROM_PATH );
        mRomMd5 = extras.getString( ActivityHelper.Keys.ROM_MD5 );
        mRomCrc = extras.getString( ActivityHelper.Keys.ROM_CRC );
        mRomHeaderName = extras.getString( ActivityHelper.Keys.ROM_HEADER_NAME );
        mRomCountryCode = extras.getByte( ActivityHelper.Keys.ROM_COUNTRY_CODE );
        mRomArtPath = extras.getString( ActivityHelper.Keys.ROM_ART_PATH );
        mRomGoodName = extras.getString( ActivityHelper.Keys.ROM_GOOD_NAME );
        mRomLegacySave = extras.getString( ActivityHelper.Keys.ROM_LEGACY_SAVE );
        mDoRestart = extras.getBoolean( ActivityHelper.Keys.DO_RESTART, false );
        if( TextUtils.isEmpty( mRomPath ) || TextUtils.isEmpty( mRomMd5 ) )
            finish();

        // Initialize MOGA controller API
        // TODO: Remove hack after MOGA SDK is fixed
        // mMogaController.init();
        MogaHack.init( mMogaController, this );

        // Get app data and user preferences
        mGlobalPrefs = new GlobalPrefs( this, mAppData );

        //Allow volume keys to control media volume if they are not mapped

        if (!mGlobalPrefs.volKeysMappable && mGlobalPrefs.audioPlugin.enabled)
        {
            this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }

        mGamePrefs = new GamePrefs( this, mRomMd5, mRomCrc, mRomHeaderName, mRomGoodName,
            CountryCode.getCountryCode(mRomCountryCode).toString(), mAppData, mGlobalPrefs, mRomLegacySave );

        mGameDataManager = new GameDataManager(mGlobalPrefs, mGamePrefs, mGlobalPrefs.maxAutoSaves);
        mGameDataManager.makeDirs();
        mGameDataManager.moveFromLegacy();

        final Window window = this.getWindow();

        // Enable full-screen mode
        window.setFlags( LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN );
        window.setFlags(LayoutParams.FLAG_LAYOUT_IN_SCREEN, LayoutParams.FLAG_LAYOUT_IN_SCREEN);

        // Keep screen from going to sleep
        window.setFlags( LayoutParams.FLAG_KEEP_SCREEN_ON, LayoutParams.FLAG_KEEP_SCREEN_ON );

        // Set the screen orientation
        if(mGlobalPrefs.displayOrientation != -1)
        {
            this.setRequestedOrientation( mGlobalPrefs.displayOrientation );
        }

        // Lay out content and get the views
        this.setContentView( R.layout.game_activity);
        mSurfaceView = this.findViewById( R.id.gameSurface );

        mOverlay = findViewById(R.id.gameOverlay);
        mDrawerLayout = findViewById(R.id.drawerLayout);
        mGameSidebar = findViewById(R.id.gameSidebar);

        // Don't darken the game screen when the drawer is open
        mDrawerLayout.setScrimColor(0x0);
        mDrawerLayout.setSwipGestureEnabled(mGlobalPrefs.inGameMenuIsSwipGesture);
        mDrawerLayout.setBackgroundColor(0xFF000000);

        if (!TextUtils.isEmpty(mRomArtPath) && new File(mRomArtPath).exists())
            mGameSidebar.setImage(new BitmapDrawable(this.getResources(), mRomArtPath));

        mGameSidebar.setTitle(mRomGoodName);

        // Handle events from the side bar
        mGameSidebar.setActionHandler(this, R.menu.game_drawer);

        // Listen to game surface events (created, changed, destroyed)
        mSurfaceView.getHolder().addCallback( this );

        // Update the SurfaceView size
        mSurfaceView.getHolder().setFixedSize( mGamePrefs.videoRenderWidth, mGamePrefs.videoRenderHeight );
        final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mSurfaceView.getLayoutParams();
        params.width = Math.round ( mGamePrefs.videoSurfaceWidth * ( mGamePrefs.videoSurfaceZoom / 100.f ) );
        params.height = Math.round ( mGamePrefs.videoSurfaceHeight * ( mGamePrefs.videoSurfaceZoom / 100.f ) );
        params.gravity = Gravity.CENTER_HORIZONTAL;

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT )
        {
            params.gravity |= Gravity.TOP;
        }
        else
        {
            params.gravity |= Gravity.CENTER_VERTICAL;
        }

        mSurfaceView.setLayoutParams( params );

        if (savedInstanceState == null)
        {
            // Show the drawer at the start and have it hide itself
            // automatically
            mDrawerLayout.openDrawer(GravityCompat.START);
            ReloadAllMenus();
        }
        else
        {
            mDrawerOpenState = savedInstanceState.getBoolean(STATE_DRAWER_OPEN);
        }

        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener(){

            @Override
            public void onDrawerClosed(View arg0)
            {
                if (mCoreFragment != null) {
                    mCoreFragment.resumeEmulator();
                }

                mDrawerOpenState = false;
            }

            @Override
            public void onDrawerOpened(View arg0)
            {
                if(mCoreFragment != null)
                {
                    mCoreFragment.pauseEmulator();
                }
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

        // Initialize the screen elements
        if( mGamePrefs.isTouchscreenEnabled || mGlobalPrefs.isFpsEnabled )
        {
            // The touch map and overlay are needed to display frame rate and/or controls
            mTouchscreenMap = new VisibleTouchMap( this.getResources() );
            mTouchscreenMap.load( mGamePrefs.touchscreenSkin, mGamePrefs.touchscreenProfile,
                    mGlobalPrefs.isTouchscreenAnimated, mGlobalPrefs.isFpsEnabled, mGlobalPrefs.fpsXPosition,
                    mGlobalPrefs.fpsYPosition, mGlobalPrefs.touchscreenScale, mGlobalPrefs.touchscreenTransparency );
            mOverlay.initialize(mCoreFragment, mTouchscreenMap, !mGamePrefs.isTouchscreenHidden, mGlobalPrefs.isFpsEnabled,
                    mGamePrefs.isAnalogHiddenWhenSensor, mGlobalPrefs.isTouchscreenAnimated);
        }

        // Initialize user interface devices
        initControllers(mOverlay);

        // Override the peripheral controllers' key provider, to add some extra
        // functionality
        mOverlay.setOnKeyListener(this);
        mOverlay.requestFocus();

        // Check periodically for touch input to determine if we should
        // hide the controls
        mHandler = new Handler();
        Calendar calendar = Calendar.getInstance();
        mLastTouchTime = calendar.get(Calendar.SECOND);

        if(mGlobalPrefs.touchscreenAutoHideEnabled)
            mHandler.postDelayed(mLastTouchChecker, 500);
    }

    @Override
    public void onStart()
    {
        super.onStart();
        Log.i("GameActivity", "onStart");

        //This can happen if the screen is turn off while the emulator is running then turned back on
        tryRunning();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.i("GameActivity", "onResume");

        if (mSensorController != null) {
            mSensorController.onResume();
        }

        // Set the sidebar opacity
        mGameSidebar.setBackground(new DrawerDrawable(
                mGlobalPrefs.displayActionBarTransparency));

        mMogaController.onResume();

        if(mDrawerOpenState)
        {
            if(mCoreFragment != null)
            {
                mCoreFragment.pauseEmulator();
            }
            mDrawerLayout.openDrawer(GravityCompat.START);
            ReloadAllMenus();
        }
    }

    @Override
    public void onSaveInstanceState( Bundle savedInstanceState )
    {
        savedInstanceState.putBoolean(STATE_DRAWER_OPEN, mDrawerOpenState);

        super.onSaveInstanceState( savedInstanceState );
    }

    @Override
    public void onStop()
    {
        super.onStop();

        Log.i( "GameActivity", "onStop" );

        //Don't pause emulation when rotating the screen or the core fragment has been set to null
        //on a shutdown
        if(!this.isChangingConfigurations() && mCoreFragment != null)
        {
            if(mGlobalPrefs.maxAutoSaves != 0)
            {
                mCoreFragment.autoSaveState(mGameDataManager.getAutoSaveFileName(), false);
            }

            mCoreFragment.pauseEmulator();

            mGameDataManager.clearOldest();
        }
        
        if (mSensorController != null) {
            mSensorController.onPause();
        }

        mMogaController.onPause();
    }

    //This is only called once when fragment is destroyed due to rataining the state
    @Override
    public void onDestroy()
    {
        Log.i( "GameActivity", "onDestroy" );

        super.onDestroy();

        if(mCoreFragment != null)
        {
            mCoreFragment.clearOnFpsChangedListener();
        }

        // Some devices are crashing here at times as reported in google play store
        // It's hard to tell what exactly is NULL when that happens.
        if(mSurfaceView != null)
        {
            SurfaceHolder surfaceHolder = mSurfaceView.getHolder();

            if (surfaceHolder != null) {
                surfaceHolder.removeCallback( this );
            }
        }

        mHandler.removeCallbacks(mLastTouchChecker);
    }

    @Override
    public void onWindowFocusChanged( boolean hasFocus )
    {
        // Only try to run; don't try to pause. User may just be touching the in-game menu.
        Log.i( "GameActivity", "onWindowFocusChanged: " + hasFocus );
        if( hasFocus )
        {
            hideSystemBars();
        }
    }

    @Override
    public void onPromptDialogClosed(int id, int which)
    {
        if(mCoreFragment != null)
        {
            mCoreFragment.onPromptDialogClosed(id, which);
        }
    }

    private void ReloadAllMenus()
    {
        if(mCoreFragment == null) return;

            //Reload currently selected speed setting
        final MenuItem toggleSpeedItem =
            mGameSidebar.getMenu().findItem(R.id.menuItem_toggle_speed);
        toggleSpeedItem.setTitle(this.getString(R.string.menuItem_toggleSpeed, mCoreFragment.getCurrentSpeed()));

        //Reload currently selected slot
        final MenuItem slotItem = mGameSidebar.getMenu().findItem(R.id.menuItem_set_slot);
        slotItem.setTitle(this.getString(R.string.menuItem_setSlot, mCoreFragment.getSlot()));

        final int resId = mCoreFragment.getFramelimiter() ?
            R.string.menuItem_enableFramelimiter :
            R.string.menuItem_disableFramelimiter;

        //Reload the menu with the new frame limiter setting
        final MenuItem frameLimiterItem =
            mGameSidebar.getMenu().findItem(R.id.menuItem_disable_frame_limiter);
        frameLimiterItem.setTitle(this.getString(resId));

        //Reload player pak settings
        UpdateControllerMenu(R.id.menuItem_player_one, mGamePrefs.isPlugged1, 1);
        UpdateControllerMenu(R.id.menuItem_player_two, mGamePrefs.isPlugged2, 2);
        UpdateControllerMenu(R.id.menuItem_player_three, mGamePrefs.isPlugged3, 3);
        UpdateControllerMenu(R.id.menuItem_player_four, mGamePrefs.isPlugged4, 4);

        mGameSidebar.reload();
    }

    private void UpdateControllerMenu(int menuItemId, boolean isPlugged, int playerNumber)
    {
        final MenuItem pakGroupItem = mGameSidebar.getMenu().findItem(R.id.menuItem_paks);

        if(mGameSidebar.getMenu().findItem(menuItemId) != null)
        {
            if(!isPlugged)
            {
                pakGroupItem.getSubMenu().removeItem(menuItemId);
            }
            else
            {
                final MenuItem playerItem = mGameSidebar.getMenu().findItem(menuItemId);
                playerItem.setTitleCondensed(this.getString(mGlobalPrefs.getPakType(playerNumber).getResourceString()));
            }
        }
    }

    @Override
    public void onPromptFinished()
    {
        if(mCoreFragment == null) return;

        //In here we only reload things that are updated through prompts

        //reload menu item with new slot
        final MenuItem slotItem = mGameSidebar.getMenu().findItem(R.id.menuItem_set_slot);
        slotItem.setTitle(this.getString(R.string.menuItem_setSlot, mCoreFragment.getSlot()));

        //Reload the menu with the new speed
        final MenuItem toggleSpeedItem =
            mGameSidebar.getMenu().findItem(R.id.menuItem_toggle_speed);
        toggleSpeedItem.setTitle(this.getString(R.string.menuItem_toggleSpeed, mCoreFragment.getCurrentSpeed()));

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
        if(mCoreFragment == null) return;

        switch (menuItem.getItemId())
        {
        case R.id.menuItem_exit:
            mCoreFragment.exit();
            break;
        case R.id.menuItem_toggle_speed:
            mCoreFragment.toggleSpeed();

            //Reload the menu with the new speed
            final MenuItem toggleSpeedItem =
                mGameSidebar.getMenu().findItem(R.id.menuItem_toggle_speed);
            toggleSpeedItem.setTitle(this.getString(R.string.menuItem_toggleSpeed, mCoreFragment.getCurrentSpeed()));
            mGameSidebar.reload();
            break;
        case R.id.menuItem_set_speed:
            mCoreFragment.setCustomSpeedFromPrompt();
            break;
        case R.id.menuItem_screenshot:
            mCoreFragment.screenshot();
            break;
        case R.id.menuItem_set_slot:
            mCoreFragment.setSlotFromPrompt();
            break;
        case R.id.menuItem_slot_load:
            mCoreFragment.loadSlot();
            break;
        case R.id.menuItem_slot_save:
            mCoreFragment.saveSlot();

            if( mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
            {
                mDrawerLayout.closeDrawer( GravityCompat.START );
            }
            break;
        case R.id.menuItem_file_load:
            mCoreFragment.loadFileFromPrompt();
            break;
        case R.id.menuItem_file_save:
            mCoreFragment.saveFileFromPrompt();
            break;
        case R.id.menuItem_file_load_auto_save:
            mCoreFragment.loadAutoSaveFromPrompt();
            break;
        case R.id.menuItem_disable_frame_limiter:
            mCoreFragment.toggleFramelimiter();

            final int resId = mCoreFragment.getFramelimiter() ?
                R.string.menuItem_enableFramelimiter :
                R.string.menuItem_disableFramelimiter;

            //Reload the menu with the new speed
            final MenuItem frameLimiterItem =
                mGameSidebar.getMenu().findItem(R.id.menuItem_disable_frame_limiter);
            frameLimiterItem.setTitle(this.getString(resId));
            mGameSidebar.reload();
            break;
        case R.id.menuItem_player_one:
            setPakTypeFromPrompt(1);
            break;
        case R.id.menuItem_player_two:
            setPakTypeFromPrompt(2);
            break;
        case R.id.menuItem_player_three:
            setPakTypeFromPrompt(3);
            break;
        case R.id.menuItem_player_four:
            setPakTypeFromPrompt(4);
            break;
        case R.id.menuItem_setIme:
            final InputMethodManager imeManager = (InputMethodManager)
                this.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imeManager != null)
                imeManager.showInputMethodPicker();
            break;
        case R.id.menuItem_reset:
            mCoreFragment.restart();
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

    public void setPakTypeFromPrompt(final int player)
    {
        //First get the prompt title
        final CharSequence title = GetPlayerTextFromId(player);
        final MenuItem playerMenuItem = GetPlayerMenuItemFromId(player);

        //Generate possible pak types
        final ArrayList<CharSequence> selections = new ArrayList<>();
        for(final PakType pakType:PakType.values())
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
                            if(mCoreFragment != null)
                            {
                                mCoreFragment.updateControllerConfig(player - 1, true, PakType.values()[value].getNativeValue());
                            }

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

    private void tryRunning()
    {
        if (mCoreFragment.hasServiceStarted()) {
            if (!mDrawerLayout.isDrawerOpen(GravityCompat.START) && !mDrawerOpenState) {
                mCoreFragment.resumeEmulator();
            } else {

                mCoreFragment.resumeEmulator();

                //Sleep for a bit to allow screen to refresh while running, then pause
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mCoreFragment.pauseEmulator();
            }
        }
    }

    @Override
    public void surfaceChanged( SurfaceHolder holder, int format, int width, int height )
    {
        Log.i( "GameActivity", "surfaceChanged" );

        if(mCoreFragment != null)
        {
            mCoreFragment.setSurface(holder.getSurface());

            if (!mCoreFragment.IsInProgress()) {
                final String latestSave = mGameDataManager.getLatestAutoSave();
                mCoreFragment.startCore(mAppData, mGlobalPrefs, mGamePrefs, mRomGoodName, mRomPath,
                        mRomMd5, mRomCrc, mRomHeaderName, mRomCountryCode, mRomArtPath, mRomLegacySave,
                        mGamePrefs.getCheatArgs(), mDoRestart, latestSave);
            }

            // Try running now in case the core service has already started
            // If it hasn't started running yet, then check again when the core service connection happens
            // in onCoreServiceStarted
            tryRunning();
        }
    }

    @Override
    public void surfaceDestroyed( SurfaceHolder holder )
    {
        Log.i( "GameActivity", "surfaceDestroyed" );

        if(mCoreFragment != null)
        {
            mCoreFragment.destroySurface();
        }
    }

    @Override
    public void onRestart(boolean shouldRestart)
    {
        if(shouldRestart)
        {
            if(mCoreFragment != null)
            {
                mCoreFragment.restartEmulator();
            }

            if( mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
            {
                mDrawerLayout.closeDrawer( GravityCompat.START );
            }
        }
        else if( !mDrawerLayout.isDrawerOpen( GravityCompat.START ) && mCoreFragment != null)
        {
            mCoreFragment.resumeEmulator();
        }
    }

    @Override
    public void onCoreServiceStarted()
    {
        Log.i("GameActivity", "onCoreServiceStarted");

        if(mCoreFragment == null) return;

        final Vibrator vibrator = (Vibrator) this.getSystemService( Context.VIBRATOR_SERVICE );
        mCoreFragment.registerVibrator(1, vibrator);

        ReloadAllMenus();

        mDrawerLayout.closeDrawer(GravityCompat.START);

        if(mShouldExit)
        {
            mCoreFragment.shutdownEmulator();
            finishAffinity();
        }

        if(mCoreFragment.isShuttingDown())
        {
            Log.i("GameActivity", "Shutting down because previous instance hasn't finished");

            Notifier.showToast( this, R.string.toast_not_done_shutting_down );

            finishActivity();
        }
        else
        {
            //This can happen if GameActivity is killed while service is running
            tryRunning();
        }
    }

    @Override
    public void onExitRequested(boolean shouldExit)
    {
        Log.i( "GameActivity", "onExitRequested" );
        if(shouldExit)
        {
            mMogaController.exit();
            shutdownEmulator();
        }
        else if( !mDrawerLayout.isDrawerOpen( GravityCompat.START ) && mCoreFragment != null)
        {
            mCoreFragment.resumeEmulator();
        }
    }

    public void finishActivity()
    {
        // Set the screen orientation
        if (mGlobalPrefs.displayOrientation != -1) {
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }

        if(mCoreFragment != null)
        {
            mCoreFragment.setCoreEventListener(null);
            mCoreFragment.destroySurface();
            mCoreFragment = null;
        }

        setResult(RESULT_OK, null);
        finish();
    }

    /**
     * Handle view onKey callbacks
     * @param view If view is NULL then this keycode will not be handled by the key provider. This is to avoid
     *             the situation where user maps the menu key to the menu command.
     * @param keyCode key code
     * @param event key event
     * @return True if handled
     */
    @Override
    public boolean onKey( View view, int keyCode, KeyEvent event )
    {
        final boolean keyDown = event.getAction() == KeyEvent.ACTION_DOWN;

        boolean handled = false;

        // Attempt to reconnect any disconnected devices
        mGamePrefs.playerMap.reconnectDevice( AbstractProvider.getHardwareId( event ) );

        if( !mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
        {
            // If PeripheralControllers exist and handle the event,
            // they return true. Else they return false, signaling
            // Android to handle the event (menu button, vol keys).
            if( mKeyProvider != null && view != null)
            {
                handled = mKeyProvider.onKey(view, keyCode, event);

                //Don't use built in keys in the device to hide the touch controls
                if(handled &&
                        keyCode != KeyEvent.KEYCODE_MENU &&
                        keyCode != KeyEvent.KEYCODE_BACK &&
                        keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
                        keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
                        keyCode != KeyEvent.KEYCODE_VOLUME_MUTE &&
                        mGlobalPrefs.touchscreenAutoHideEnabled)
                {
                    mOverlay.onTouchControlsHide();
                }
            }
        }

        if(!handled)
        {
            if( keyDown && keyCode == KeyEvent.KEYCODE_MENU )
            {
                if( mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
                {
                    mDrawerLayout.closeDrawer( GravityCompat.START );
                    mOverlay.requestFocus();
                }
                else {
                    if(mCoreFragment != null)
                    {
                        mCoreFragment.pauseEmulator();
                    }
                    mDrawerLayout.openDrawer(GravityCompat.START);
                    ReloadAllMenus();
                    mDrawerOpenState = true;
                    mGameSidebar.requestFocus();
                    mGameSidebar.smoothScrollToPosition(0);
                }
                return true;
            }
            else if( keyDown && keyCode == KeyEvent.KEYCODE_BACK )
            {
                if( mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
                {
                    mDrawerLayout.closeDrawer( GravityCompat.START );
                    mOverlay.requestFocus();
                }
                else
                {
                    //We are using the slide gesture for the menu, so the back key can be used to exit
                    if(mGlobalPrefs.inGameMenuIsSwipGesture)
                    {
                        if(mCoreFragment != null)
                        {
                            mCoreFragment.exit();
                        }
                    }
                    //Else the back key bring up the in-game menu
                    else
                    {
                        if(mCoreFragment != null)
                        {
                            mCoreFragment.pauseEmulator();
                        }
                        mDrawerLayout.openDrawer( GravityCompat.START );
                        ReloadAllMenus();
                        mDrawerOpenState = true;
                        mGameSidebar.requestFocus();
                        mGameSidebar.smoothScrollToPosition(0);
                    }
                }
                return true;
            }
        }

        return handled;
    }

    @SuppressLint( "InlinedApi" )
    private void initControllers( View inputSource )
    {
        // By default, send Player 1 rumbles through phone vibrator
        final Vibrator vibrator = (Vibrator) this.getSystemService( Context.VIBRATOR_SERVICE );

        // Create the touchscreen controls
        if( mGamePrefs.isTouchscreenEnabled )
        {
            if (!mGamePrefs.sensorAxisX.isEmpty() || !mGamePrefs.sensorAxisY.isEmpty()) {
                // Create the sensor controller
                final SensorManager sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
                mSensorController = new SensorController(mCoreFragment, sensorManager, mOverlay, mGamePrefs.sensorAxisX,
                        mGamePrefs.sensorSensitivityX, mGamePrefs.sensorAngleX, mGamePrefs.sensorAxisY,
                        mGamePrefs.sensorSensitivityY, mGamePrefs.sensorAngleY);
                if (mGamePrefs.sensorActivateOnStart) {
                    mSensorController.setSensorEnabled(true);
                    mOverlay.onSensorEnabled(true);
                }
            }

            // Create the touchscreen controller
            mTouchscreenController = new TouchController(mCoreFragment, mTouchscreenMap,
                    mOverlay, vibrator, mGamePrefs.touchscreenAutoHold,
                    mGlobalPrefs.isTouchscreenFeedbackEnabled, mGamePrefs.touchscreenNotAutoHoldables,
                    mSensorController, mGamePrefs.invertTouchXAxis, mGamePrefs.invertTouchYAxis );
            inputSource.setOnTouchListener(this);
            mDrawerLayout.setTouchMap( mTouchscreenMap );
        }

        //Check for controller configuration
        boolean needs1 = false;
        boolean needs2 = false;
        boolean needs3 = false;
        boolean needs4 = false;

        // Popup the multi-player dialog if necessary and abort if any players are unassigned
        final RomDatabase romDatabase = RomDatabase.getInstance();

        if(!romDatabase.hasDatabaseFile())
        {
            romDatabase.setDatabaseFile(mAppData.mupen64plus_ini);
        }

        if( mGamePrefs.playerMap.isEnabled() && mGlobalPrefs.getPlayerMapReminder() )
        {
            mGamePrefs.playerMap.removeUnavailableMappings();
            needs1 = mGamePrefs.isControllerEnabled1 && !mGamePrefs.playerMap.isMapped( 1 );
            needs2 = mGamePrefs.isControllerEnabled2 && !mGamePrefs.playerMap.isMapped( 2 );
            needs3 = mGamePrefs.isControllerEnabled3 && !mGamePrefs.playerMap.isMapped( 3 );
            needs4 = mGamePrefs.isControllerEnabled4 && !mGamePrefs.playerMap.isMapped( 4 );

            if( needs1 || needs2 || needs3 || needs4 )
            {
                Popups.showNeedsPlayerMap( this );
            }
        }

        // Create the input providers shared among all peripheral controllers
        mKeyProvider = new KeyProvider( inputSource, ImeFormula.DEFAULT,
                mGlobalPrefs.unmappableKeyCodes );
        final MogaProvider mogaProvider = new MogaProvider( mMogaController );
        mAxisProvider = new AxisProvider();

        // Request focus for proper listening
        inputSource.requestFocus();
        // Create the peripheral controls to handle key/stick presses
        if( mGamePrefs.isControllerEnabled1 && !needs1)
        {
            final ControllerProfile p = mGamePrefs.controllerProfile1;
            new PeripheralController( mCoreFragment, 1, mGamePrefs.playerMap, p.getMap(), p.getDeadzone(),
                    p.getSensitivityX(), p.getSensitivityY(), mOverlay, this, mSensorController, mKeyProvider, mAxisProvider, mogaProvider );
            Log.i("GameActivity", "Player 1 has been enabled");
        }
        if( mGamePrefs.isControllerEnabled2 && !needs2)
        {
            final ControllerProfile p = mGamePrefs.controllerProfile2;
            new PeripheralController( mCoreFragment, 2, mGamePrefs.playerMap, p.getMap(), p.getDeadzone(),
                    p.getSensitivityX(), p.getSensitivityY(), mOverlay, this, null, mKeyProvider, mAxisProvider, mogaProvider );
            Log.i("GameActivity", "Player 2 has been enabled");
        }
        if( mGamePrefs.isControllerEnabled3 && !needs3)
        {
            final ControllerProfile p = mGamePrefs.controllerProfile3;
            new PeripheralController( mCoreFragment, 3, mGamePrefs.playerMap, p.getMap(), p.getDeadzone(),
                    p.getSensitivityX(), p.getSensitivityY(), mOverlay, this, null, mKeyProvider, mAxisProvider, mogaProvider );
            Log.i("GameActivity", "Player 3 has been enabled");
        }
        if( mGamePrefs.isControllerEnabled4 && !needs4)
        {
            final ControllerProfile p = mGamePrefs.controllerProfile4;
            new PeripheralController( mCoreFragment, 4, mGamePrefs.playerMap, p.getMap(), p.getDeadzone(),
                    p.getSensitivityX(), p.getSensitivityY(), mOverlay, this, null, mKeyProvider, mAxisProvider, mogaProvider );
            Log.i("GameActivity", "Player 4 has been enabled");
        }
    }

    private void hideSystemBars()
    {
        if( mDrawerLayout != null )
        {
            if( mGlobalPrefs.isImmersiveModeEnabled )
            {
                mDrawerLayout.setSystemUiVisibility( View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN );
            }
        }
    }

    private void showSystemBars()
    {
        final Window window = this.getWindow();

        window.clearFlags(LayoutParams.FLAG_FULLSCREEN
                | LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | LayoutParams.FLAG_KEEP_SCREEN_ON);

        if( mDrawerLayout != null )
        {
            mDrawerLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private void shutdownEmulator()
    {
        Log.i( "GameActivity", "shutdownEmulator" );

        if(mCoreFragment != null && mCoreFragment.hasServiceStarted())
        {
            //Generate auto save file
            if(mGlobalPrefs.maxAutoSaves != 0)
            {
                final String saveFileName = mGameDataManager.getAutoSaveFileName();
                mCoreFragment.autoSaveState(saveFileName, true);
            }
            else
            {
                mCoreFragment.shutdownEmulator();
            }

            mGameDataManager.clearOldest();
        }

        finishActivity();
    }

    Runnable mLastTouchChecker = new Runnable() {
        @Override
        public void run() {
            Calendar calendar = Calendar.getInstance();
            int seconds = calendar.get(Calendar.SECOND);

            if(seconds - mLastTouchTime > mGlobalPrefs.touchscreenAutoHideSeconds)
            {
                mOverlay.onTouchControlsHide();
            }

            mHandler.postDelayed(mLastTouchChecker, 500);
        }
    };

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        Calendar calendar = Calendar.getInstance();
        mLastTouchTime = calendar.get(Calendar.SECOND);

        return mTouchscreenController.onTouch(view, motionEvent);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        if(mGlobalPrefs.touchscreenAutoHideEnabled)
            mOverlay.onTouchControlsHide();

        // Attempt to reconnect any disconnected devices
        mGamePrefs.playerMap.reconnectDevice( AbstractProvider.getHardwareId( motionEvent ) );

        return (mAxisProvider.onGenericMotion(null, motionEvent) && !mDrawerLayout.isDrawerOpen( GravityCompat.START )) ||
                super.onGenericMotionEvent(motionEvent);
    }
}
