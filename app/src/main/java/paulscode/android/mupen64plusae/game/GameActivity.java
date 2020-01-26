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
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
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

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.DrawerDrawable;
import paulscode.android.mupen64plusae.GameSidebar;
import paulscode.android.mupen64plusae.GameSidebar.GameSidebarActionHandler;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog.PromptConfirmListener;
import paulscode.android.mupen64plusae.dialog.Prompt;
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
import paulscode.android.mupen64plusae.jni.CoreInterface.OnFpsChangedListener;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.jni.CoreTypes.PakType;
import paulscode.android.mupen64plusae.profile.ControllerProfile;
import paulscode.android.mupen64plusae.util.CountryCode;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.RomDatabase;

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
        GameSidebarActionHandler, CoreEventListener, View.OnTouchListener, OnFpsChangedListener
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
    private long mLastTouchTime;
    private Handler mHandler;

    // args data
    private boolean mShouldExit = false;
    private String mRomPath = null;
    private String mZipPath = null;
    private String mRomMd5 = null;
    private String mRomCrc = null;
    private String mRomGoodName = null;
    private String mRomDisplayName = null;
    private String mRomHeaderName = null;
    private byte mRomCountryCode = 0;
    private String mRomArtPath = null;
    private boolean mDoRestart = false;
    private boolean mForceExit = false;

    // App data and user preferences
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;
    private GamePrefs mGamePrefs = null;

    private static final String STATE_DRAWER_OPEN = "STATE_DRAWER_OPEN";
    private boolean mDrawerOpenState = false;

    private static final String STATE_CORE_FRAGMENT = "STATE_CORE_FRAGMENT";
    private CoreFragment mCoreFragment = null;

    private boolean[] isControllerPlugged = new boolean[4];

    private boolean mScreenOrientationSet = false;

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

            mForceExit = extras.getBoolean(ActivityHelper.Keys.FORCE_EXIT_GAME);
            Log.i("GameActivity", "forceExit=" + mForceExit);

            if(mShouldExit && mCoreFragment != null)
            {
                mCoreFragment.shutdownEmulator();
                finish();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i("GameActivity", "onCreate");
        super.onCreate(savedInstanceState);
        super.setTheme( androidx.appcompat.R.style.Theme_AppCompat_NoActionBar );

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

        mForceExit = extras.getBoolean(ActivityHelper.Keys.FORCE_EXIT_GAME);
        Log.i("GameActivity", "forceExit=" + mForceExit);

        mRomPath = extras.getString( ActivityHelper.Keys.ROM_PATH );
        mZipPath = extras.getString( ActivityHelper.Keys.ZIP_PATH );
        mRomMd5 = extras.getString( ActivityHelper.Keys.ROM_MD5 );
        mRomCrc = extras.getString( ActivityHelper.Keys.ROM_CRC );
        mRomHeaderName = extras.getString( ActivityHelper.Keys.ROM_HEADER_NAME );
        mRomCountryCode = extras.getByte( ActivityHelper.Keys.ROM_COUNTRY_CODE );
        mRomArtPath = extras.getString( ActivityHelper.Keys.ROM_ART_PATH );
        mRomGoodName = extras.getString( ActivityHelper.Keys.ROM_GOOD_NAME );
        mRomDisplayName = extras.getString( ActivityHelper.Keys.ROM_DISPLAY_NAME );
        mDoRestart = extras.getBoolean( ActivityHelper.Keys.DO_RESTART, false );
        if( TextUtils.isEmpty( mRomPath ) || TextUtils.isEmpty( mRomMd5 ) )
            finish();

        // Initialize MOGA controller API
        MogaHack.init( mMogaController, this );

        // Get app data and user preferences
        mGlobalPrefs = new GlobalPrefs( this, mAppData );

        //Allow volume keys to control media volume if they are not mapped

        if (!mGlobalPrefs.volKeysMappable && mGlobalPrefs.audioPlugin.enabled)
        {
            this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }

        mGamePrefs = new GamePrefs( this, mRomMd5, mRomCrc, mRomHeaderName, mRomGoodName,
            CountryCode.getCountryCode(mRomCountryCode).toString(), mAppData, mGlobalPrefs );

        final Window window = this.getWindow();

        // Enable full-screen mode
        window.setFlags( LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN );
        window.setFlags(LayoutParams.FLAG_LAYOUT_IN_SCREEN, LayoutParams.FLAG_LAYOUT_IN_SCREEN);

        // Keep screen from going to sleep
        window.setFlags( LayoutParams.FLAG_KEEP_SCREEN_ON, LayoutParams.FLAG_KEEP_SCREEN_ON );

        // Set the screen orientation
        if (mGlobalPrefs.displayOrientation != -1) {
            setRequestedOrientation( mGlobalPrefs.displayOrientation );
            mScreenOrientationSet = true;
        } else {
            // If auto rotation is on, lock orientation until graphics have initialized. We don't
            // want the surface to be destroyed in the middle of initialization
            if (getWindowManager().getDefaultDisplay().getRotation()== Surface.ROTATION_0)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            if (getWindowManager().getDefaultDisplay().getRotation()== Surface.ROTATION_90)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            if (getWindowManager().getDefaultDisplay().getRotation()== Surface.ROTATION_270)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);

            mScreenOrientationSet = false;
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

        mGameSidebar.setTitle(mRomDisplayName);

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
            public void onDrawerClosed(@NonNull View arg0)
            {
                if (mCoreFragment != null) {
                    mCoreFragment.resumeEmulator();
                }

                mDrawerOpenState = false;
            }

            @Override
            public void onDrawerOpened(@NonNull View arg0)
            {
                if(mCoreFragment != null)
                {
                    mCoreFragment.pauseEmulator();
                }
                ReloadAllMenus();
            }

            @Override
            public void onDrawerSlide(@NonNull View arg0, float arg1)
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
            mTouchscreenMap.load( mGlobalPrefs.touchscreenSkinPath, mGamePrefs.touchscreenProfile,
                    mGlobalPrefs.isTouchscreenAnimated, mGlobalPrefs.isFpsEnabled, mGlobalPrefs.fpsXPosition,
                    mGlobalPrefs.fpsYPosition, mGlobalPrefs.touchscreenScale, mGlobalPrefs.touchscreenTransparency );
            mOverlay.initialize(mTouchscreenMap, !mGamePrefs.isTouchscreenHidden, mGlobalPrefs.isFpsEnabled,
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
        mLastTouchTime = System.currentTimeMillis() / 1000L;

        if(mGlobalPrefs.touchscreenAutoHideEnabled)
            mHandler.postDelayed(mPeriodicChecker, 500);
    }

    @Override
    public void onFpsChanged(int newValue)
    {
        // Set the screen orientation the first time there is a screen update
        if (mGlobalPrefs.displayOrientation == -1 && !mScreenOrientationSet) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            mScreenOrientationSet = true;
        }

        if(mGlobalPrefs.isFpsEnabled && mOverlay != null)
        {
            mOverlay.onFpsChanged(newValue);
        }
    }

    @Override
    public void onStart()
    {
        super.onStart();
        Log.i("GameActivity", "onStart");

        //This can happen if the screen is turned off while the emulator is running then turned back on
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
            mGameSidebar.requestFocus();
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
                mCoreFragment.autoSaveState(false);
            }

            mCoreFragment.pauseEmulator();
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
            mCoreFragment.unsetSurface();
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

        // This apparently can happen on rare occasion, not sure how, so protect against it
        if(mHandler != null)
        {
            mHandler.removeCallbacks(mPeriodicChecker);
        }

        if (mOverlay != null) {
            mOverlay.onDestroy();
        }
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
        UpdateControllerMenu(R.id.menuItem_player_one, mGamePrefs.isPlugged[0], 1);
        UpdateControllerMenu(R.id.menuItem_player_two, mGamePrefs.isPlugged[1], 2);
        UpdateControllerMenu(R.id.menuItem_player_three, mGamePrefs.isPlugged[2], 3);
        UpdateControllerMenu(R.id.menuItem_player_four, mGamePrefs.isPlugged[3], 4);

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
                playerItem.setTitleCondensed(this.getString(mGamePrefs.getPakType(playerNumber).getResourceString()));
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
            mOverlay.requestFocus();
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
                mOverlay.requestFocus();
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
        final ArrayList<PakType> selectionPakTypes = new ArrayList<>();
        for(final PakType pakType:PakType.values())
        {
            if (pakType.getResourceString() != 0) {
                selections.add(this.getString(pakType.getResourceString()));
                selectionPakTypes.add(pakType);
            }
        }

        Prompt.promptListSelection( this, title, selections,
                (value, which) -> {
                    if( which == DialogInterface.BUTTON_POSITIVE )
                    {
                        mGamePrefs.putPakType(player, selectionPakTypes.get(value));

                        // Set the pak in the core
                        if(mCoreFragment != null)
                        {
                            mCoreFragment.updateControllerConfig(player - 1, true, selectionPakTypes.get(value));
                        }

                        //Update the menu
                        playerMenuItem.setTitleCondensed(GameActivity.this.getString(mGamePrefs.getPakType(player).getResourceString()));
                        mGameSidebar.reload();
                    }
                });
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

            mCoreFragment.setOnFpsChangedListener(this, 15);
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
                mCoreFragment.startCore(mAppData, mGlobalPrefs, mGamePrefs, mRomGoodName, mRomDisplayName, mRomPath, mZipPath,
                        mRomMd5, mRomCrc, mRomHeaderName, mRomCountryCode, mRomArtPath, mDoRestart);
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
                mOverlay.requestFocus();
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

        if (vibrator != null) {
            mCoreFragment.registerVibrator(1, vibrator);
        }

        ReloadAllMenus();

        mDrawerLayout.closeDrawer(GravityCompat.START);
        mOverlay.requestFocus();

        if(mShouldExit)
        {
            mCoreFragment.shutdownEmulator();
            finish();
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
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
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
        boolean isKeyboard = (event.getSource() & InputDevice.SOURCE_GAMEPAD) != InputDevice.SOURCE_GAMEPAD;
        final boolean keyDown = event.getAction() == KeyEvent.ACTION_DOWN;

        boolean handled = false;

        // Attempt to reconnect any disconnected devices if this is not a keyboard, we don't want to automatically
        // map keyboards
        if (!isKeyboard) {
            checkForNewController(AbstractProvider.getHardwareId( event ) );
        }

        boolean isPlayer1 = mGamePrefs.playerMap.testHardware(AbstractProvider.getHardwareId( event ), 1);

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

        //Only player 1 or keyboards can control menus
        handled = handled || (!isPlayer1 && !isKeyboard && !mGlobalPrefs.useRaphnetDevicesIfAvailable);

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
            if (!TextUtils.isEmpty(mGamePrefs.sensorAxisX) || !TextUtils.isEmpty(mGamePrefs.sensorAxisY)) {
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
                    mSensorController, mGamePrefs.invertTouchXAxis, mGamePrefs.invertTouchYAxis,
                    mGamePrefs.isTouchscreenAnalogRelative );
            inputSource.setOnTouchListener(this);
            mDrawerLayout.setTouchMap( mTouchscreenMap );
        }

        // Popup the multi-player dialog if necessary and abort if any players are unassigned
        final RomDatabase romDatabase = RomDatabase.getInstance();

        if(!romDatabase.hasDatabaseFile())
        {
            romDatabase.setDatabaseFile(mAppData.mupen64plus_ini);
        }

        // Create the input providers shared among all peripheral controllers
        mKeyProvider = new KeyProvider( inputSource, ImeFormula.DEFAULT,
                mGlobalPrefs.unmappableKeyCodes );
        final MogaProvider mogaProvider = new MogaProvider( mMogaController );
        mAxisProvider = new AxisProvider(inputSource);

        // Request focus for proper listening
        inputSource.requestFocus();

        // Create the peripheral controls to handle key/stick presses
        for(int index = 0; index < mGamePrefs.isControllerEnabled.length; ++index) {
            isControllerPlugged[index] = mGamePrefs.isPlugged[index];

            if( mGamePrefs.isControllerEnabled[index])
            {
                final ControllerProfile p = mGamePrefs.controllerProfile[index];
                initSingleController(index + 1, p, mogaProvider);
            }
        }
    }

    private void initSingleController(int player, ControllerProfile p, final MogaProvider mogaProvider)
    {
        if(p != null) {
           new PeripheralController( mCoreFragment, player, mGamePrefs.playerMap, p.getMap(), p.getDeadzone(),
                    p.getSensitivityX(), p.getSensitivityY(), mOverlay, this, null, mKeyProvider, mAxisProvider, mogaProvider );
            Log.i("GameActivity", "Player " + player + " controller has been enabled");
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

    private void shutdownEmulator()
    {
        Log.i( "GameActivity", "shutdownEmulator" );

        if(mCoreFragment != null && mCoreFragment.hasServiceStarted())
        {
            //Generate auto save file
            if(mGlobalPrefs.maxAutoSaves != 0)
            {
                mCoreFragment.autoSaveState(true);
            }
            else
            {
                mCoreFragment.shutdownEmulator();
            }
        }

        finishActivity();
    }

    //Checks a few things every 500ms
    Runnable mPeriodicChecker = new Runnable() {
        @Override
        public void run() {

            //Check for touchscreen activity
            long seconds = System.currentTimeMillis() / 1000L;

            if(seconds - mLastTouchTime > mGlobalPrefs.touchscreenAutoHideSeconds)
            {
                mOverlay.onTouchControlsHide();
            }

            if (mCoreFragment != null && !mGamePrefs.isControllerShared) {

                int startIndex = mGamePrefs.isTouchscreenEnabled || mGamePrefs.playerMap.getNumberOfMappedPlayers() == 0 ? 1 : 0;
                //Check if any controllers have changed state, except for controller 1
                for (int index = startIndex; index < mGamePrefs.controllerProfile.length; ++index) {
                    if (!mGamePrefs.playerMap.isPlayerAvailable(index+1) && isControllerPlugged[index]) {

                        if (!mGlobalPrefs.allEmulatedControllersPlugged) {
                            mCoreFragment.updateControllerConfig(index, false, mGamePrefs.getPakType(index+1));
                        }
                        isControllerPlugged[index] = false;

                        Log.i("GameActivity", "controller " + index + " was unplugged");
                    }
                }
            }

            mHandler.postDelayed(mPeriodicChecker, 500);

            if (mForceExit) {
                Log.w("GameActivity", "Exit forced");

                if (mCoreFragment != null) {
                    mCoreFragment.forceExit();
                }
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        mLastTouchTime = System.currentTimeMillis() / 1000L;

        return mTouchscreenController.onTouch(view, motionEvent);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        if(mGlobalPrefs.touchscreenAutoHideEnabled)
            mOverlay.onTouchControlsHide();

        // Attempt to reconnect any disconnected devices
        checkForNewController(AbstractProvider.getHardwareId( motionEvent ) );

        boolean isPlayer1 = mGamePrefs.playerMap.testHardware(AbstractProvider.getHardwareId( motionEvent ), 1);

        return (mAxisProvider.onGenericMotion(null, motionEvent) && !mDrawerLayout.isDrawerOpen( GravityCompat.START )) || !isPlayer1 ||
                super.onGenericMotionEvent(motionEvent);
    }

    private void checkForNewController(int hardwareId)
    {
        // Attempt to reconnect any disconnected devices
        int player = mGamePrefs.playerMap.reconnectDevice( hardwareId );

        if (player > 0 && !isControllerPlugged[player-1] ) {
            if (!mGlobalPrefs.allEmulatedControllersPlugged) {
                mCoreFragment.updateControllerConfig(player - 1, true, mGamePrefs.getPakType(player));
            }
            isControllerPlugged[player-1] = true;
        }
    }
}
