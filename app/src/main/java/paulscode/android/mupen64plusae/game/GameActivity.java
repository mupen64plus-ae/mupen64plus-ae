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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.os.VibratorManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.DrawerDrawable;
import paulscode.android.mupen64plusae.dialog.GameSettingsDialog;
import paulscode.android.mupen64plusae.GameSidebar;
import paulscode.android.mupen64plusae.GameSidebar.GameSidebarActionHandler;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog.PromptConfirmListener;
import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.dialog.PromptInputCodeDialog;
import paulscode.android.mupen64plusae.input.PeripheralController;
import paulscode.android.mupen64plusae.input.SensorController;
import paulscode.android.mupen64plusae.input.TouchController;
import paulscode.android.mupen64plusae.input.map.VisibleTouchMap;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae.input.provider.AxisProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider.ImeFormula;
import paulscode.android.mupen64plusae.jni.CoreFragment;
import paulscode.android.mupen64plusae.jni.CoreFragment.CoreEventListener;
import paulscode.android.mupen64plusae.jni.CoreTypes;
import paulscode.android.mupen64plusae.netplay.NetplayFragment;
import paulscode.android.mupen64plusae.netplay.room.NetplayClientSetupDialog;
import paulscode.android.mupen64plusae.netplay.room.NetplayServerSetupDialog;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.jni.CoreTypes.PakType;
import paulscode.android.mupen64plusae.preference.PlayerMapPreference;
import paulscode.android.mupen64plusae.profile.ControllerProfile;
import paulscode.android.mupen64plusae.util.CountryCode;
import paulscode.android.mupen64plusae.util.DisplayResolutionData;
import paulscode.android.mupen64plusae.util.DisplayWrapper;
import paulscode.android.mupen64plusae.util.FileUtil;
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

public class GameActivity extends AppCompatActivity implements PromptConfirmListener,
        GameSidebarActionHandler, CoreEventListener, View.OnTouchListener,
        NetplayClientSetupDialog.OnServerDialogActionListener,
        NetplayServerSetupDialog.OnClientDialogActionListener, NetplayFragment.NetplayListener,
        GameSettingsDialog.OnGameSettingsDialogListener, PromptInputCodeDialog.PromptInputCodeListener{
    private static final String TAG = "GameActivity";
    // Activity and views
    private GameOverlay mOverlay;
    private FpsOverlay mFpsOverlay;
    private DrawerLayout mDrawerLayout;
    private GameSidebar mGameSidebar;
    private GameSurface mGameSurface;

    // Input resources
    private VisibleTouchMap mTouchscreenMap;
    private KeyProvider mKeyProvider;
    private AxisProvider mAxisProvider;
    TouchController mTouchscreenController;
    private SensorController mSensorController;
    private long mLastTouchTime;
    private Handler mHandler;
    private final Object mSaveLock = new Object();

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
    private boolean mIsNetplayEnabled = false;
    private boolean mIsNetplayServer = false;
    private boolean mForceExit = false;
    private int mServerPort = 0;

    // App data and user preferences
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;
    private GamePrefs mGamePrefs = null;
    private DisplayResolutionData mDisplayResolutionData = null;

    private static final String STATE_DRAWER_OPEN = "STATE_DRAWER_OPEN";
    private boolean mDrawerOpenState = false;

    private static final String STATE_CORE_FRAGMENT = "STATE_CORE_FRAGMENT";
    private CoreFragment mCoreFragment = null;

    private static final String STATE_SETTINGS_RECREATE = "STATE_SETTINGS_RECREATE";
    private boolean mSettingsRecreate = false;

    private static final String STATE_SETTINGS_VIEW = "STATE_SETTINGS_VIEW";
    private boolean mSettingsView = false;

    private static final String STATE_REOPEN_SIDEBAR = "STATE_REOPEN_SIDEBAR";
    private boolean mReOpenSidebar = false;

    private static final String STATE_NETPLAY_FRAGMENT = "STATE_NETPLAY_FRAGMENT";
    private NetplayFragment mNetplayFragment = null;

    private final boolean[] isControllerPlugged = new boolean[4];

    private static final String STATE_CURRENT_FPS = "STATE_CURRENT_FPS";
    private int currentFps = -1;

    private static final String STATE_NETPLAY_CLIENT_DIALOG = "STATE_NETPLAY_CLIENT_DIALOG";
    private NetplayClientSetupDialog mNetplayClientDialog = null;

    private static final String STATE_NETPLAY_SERVER_DIALOG = "STATE_NETPLAY_SERVER_DIALOG";
    private NetplayServerSetupDialog mNetplayServerDialog = null;

    private static final String STATE_SETTINGS_FRAGMENT = "STATE_SETTINGS_FRAGMENT";
    private boolean mSettingsReset = false;

    private static final String STATE_DIALOG_FRAGMENT_KEY = "STATE_DIALOG_FRAGMENT_KEY";
    private String mDialogFragmentKey = "";

    // Used if we open a second dialog (like within player map)
    private static final String STATE_ASSOCIATED_DIALOG_FRAGMENT = "STATE_ASSOCIATED_DIALOG_FRAGMENT";
    private int mAssociatedDialogFragment = 0;

    // When using landscape and resetting from the in game settings
    // the activity will automatically be recreated so we use this
    // to prevent a loop of the activity being recreated continuously.
    private static final String STATE_SETTINGS_BREAKOUT = "STATE_SETTINGS_BREAKOUT";
    private boolean mSettingsBreakout = false;

    // If the user closes the GameActivity during a resolution reset then
    // the mResetMalfunction will be triggered and reset the game to prevent
    // the game continuing when it shouldn't / having other problems
    private static final String STATE_RESET_MALFUNCTION = "STATE_RESET_MALFUNCTION";
    private boolean mResetMalfunction = false;

    private int safeSaveTryingCount = 0;

    @Override
    protected void attachBaseContext(Context newBase) {

        String localeCode;

        try
        {
            // Fire TV Cube (2nd generation) on Android 9 ( Fire OS 7.2.4.2 / PS7242/2216 ) crash here with a null exception when calling getDefaultSharedPreferences.
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences( newBase );

            // Locale
            localeCode = preferences.getString( GlobalPrefs.KEY_LOCALE_OVERRIDE, DEFAULT_LOCALE_OVERRIDE );
        }
        catch(NullPointerException exception)
        {
            Log.i(TAG, "Null exception in attachBaseContext");
            localeCode = LocaleContextWrapper.getLocalCode();
        }

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
        Log.i(TAG, "onNewIntent");
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

            Log.i(TAG, "mShouldExit=" + mShouldExit);

            mForceExit = extras.getBoolean(ActivityHelper.Keys.FORCE_EXIT_GAME);
            Log.i(TAG, "forceExit=" + mForceExit);

            if(mShouldExit && mCoreFragment != null)
            {
                mCoreFragment.shutdownEmulator();
                finish();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        super.setTheme( androidx.appcompat.R.style.Theme_AppCompat_NoActionBar );

        mAppData = new AppData( this );

        // Initialize the objects and data files interfacing to the emulator core
        final FragmentManager fm = this.getSupportFragmentManager();
        CoreFragment fragment = (CoreFragment) fm.findFragmentByTag(STATE_CORE_FRAGMENT);

        if(fragment != null)
            fm.beginTransaction().remove(fragment).commit();

        mCoreFragment = new CoreFragment();
        fm.beginTransaction().add(mCoreFragment, STATE_CORE_FRAGMENT).commit();

        mCoreFragment.setCoreEventListener(this);

        // Get the intent data
        final Bundle extras = this.getIntent().getExtras();
        if( extras == null )
        {
            finish();
            return;
        }

        mShouldExit = extras.getBoolean(ActivityHelper.Keys.EXIT_GAME);
        Log.i(TAG, "mShouldExit=" + mShouldExit);

        mForceExit = extras.getBoolean(ActivityHelper.Keys.FORCE_EXIT_GAME);
        Log.i(TAG, "forceExit=" + mForceExit);

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
        mIsNetplayEnabled = extras.getBoolean( ActivityHelper.Keys.NETPLAY_ENABLED, false );
        mIsNetplayServer = extras.getBoolean( ActivityHelper.Keys.NETPLAY_SERVER, false );
        mSettingsReset = extras.getBoolean(ActivityHelper.Keys.SETTINGS_RESET, false);

        if( TextUtils.isEmpty( mRomPath ) || TextUtils.isEmpty( mRomMd5 ) )
            finish();

        // Get app data and user preferences
        mGlobalPrefs = new GlobalPrefs( this, mAppData );

        //Allow volume keys to control media volume if they are not mapped

        if (!mGlobalPrefs.volKeysMappable)
        {
            this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }

        mGamePrefs = new GamePrefs( this, mRomMd5, mRomCrc, mRomHeaderName, mRomGoodName,
            CountryCode.getCountryCode(mRomCountryCode).toString(), mAppData, mGlobalPrefs );

        final Window window = this.getWindow();

        // Enable full-screen mode
        DisplayWrapper.setFullScreen(this);
        window.setFlags(LayoutParams.FLAG_LAYOUT_IN_SCREEN, LayoutParams.FLAG_LAYOUT_IN_SCREEN);

        // Keep screen from going to sleep
        window.setFlags( LayoutParams.FLAG_KEEP_SCREEN_ON, LayoutParams.FLAG_KEEP_SCREEN_ON );

        // Lay out content and get the views
        this.setContentView( R.layout.game_activity);

        mGameSurface = this.findViewById(R.id.shaderSurface);

        mOverlay = findViewById(R.id.gameOverlay);
        mFpsOverlay = findViewById(R.id.fpsOverlay);
        mDrawerLayout = findViewById(R.id.drawerLayout);
        mGameSidebar = findViewById(R.id.gameSidebar);

        // Don't darken the game screen when the drawer is open
        mDrawerLayout.setScrimColor(0x0);

        if(mGlobalPrefs.inGameMenuIsSwipeGesture)
        {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
        else
        {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }

        mDrawerLayout.setBackgroundColor(0xFF000000);

        if (!TextUtils.isEmpty(mRomArtPath) && new File(mRomArtPath).exists() && FileUtil.isFileImage(new File(mRomArtPath)))
            mGameSidebar.setImage(new BitmapDrawable(this.getResources(), mRomArtPath));

        mGameSidebar.setTitle(mRomDisplayName);

        // Handle events from the side bar
        mGameSidebar.setActionHandler(this, R.menu.game_drawer);

        mDisplayResolutionData = new DisplayResolutionData(mGlobalPrefs, this, mDrawerLayout, mGamePrefs.displayScaling);

        // Set parameters for shader view
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mGameSurface.getLayoutParams();
        params.width = Math.round ( mDisplayResolutionData.getSurfaceResolutionWidth() * ( mGamePrefs.videoSurfaceZoom / 100.f ) );
        params.height = Math.round ( mDisplayResolutionData.getSurfaceResolutionHeight() * ( mGamePrefs.videoSurfaceZoom / 100.f ) );
        params.gravity = Gravity.CENTER_HORIZONTAL;

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT )
        {
            params.gravity |= Gravity.TOP;
        }
        else
        {
            // We need to be center vertical for center vertical in case the screen height in landscape
            // is less than the game render height
            params.gravity |= Gravity.CENTER_VERTICAL;
        }
        mGameSurface.setLayoutParams( params );

        mGameSurface.getHolder().setFixedSize(mDisplayResolutionData.getResolutionWidth(mGamePrefs.verticalRenderResolution)*mGlobalPrefs.shaderScaleFactor,
                mDisplayResolutionData.getResolutionHeight(mGamePrefs.verticalRenderResolution)*mGlobalPrefs.shaderScaleFactor);

        mDrawerLayout.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int oldWidth = oldRight - oldLeft;
            int oldHeight = oldBottom - oldTop;
            if( v.getWidth() != oldWidth || v.getHeight() != oldHeight )
            {
                DisplayResolutionData resolutionData = new DisplayResolutionData(mGlobalPrefs, this, mDrawerLayout, mGamePrefs.displayScaling);
                FrameLayout.LayoutParams newParams = (FrameLayout.LayoutParams) mGameSurface.getLayoutParams();
                newParams.width = Math.round ( resolutionData.getSurfaceResolutionWidth() * ( mGamePrefs.videoSurfaceZoom / 100.f ) );
                newParams.height = Math.round ( resolutionData.getSurfaceResolutionHeight() * ( mGamePrefs.videoSurfaceZoom / 100.f ) );
                mGameSurface.setLayoutParams( newParams );
            }
        });

        mGameSurface.setSelectedShader(mGlobalPrefs.getShaderPasses());
        mGameSurface.setShaderScaleFactor(mGlobalPrefs.shaderScaleFactor);

        ReloadAllMenus();

        if (savedInstanceState == null)
        {
            // Show the drawer at the start and have it hide itself
            // automatically
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
        else // possibly recreate() called
        {
            mDrawerOpenState = savedInstanceState.getBoolean(STATE_DRAWER_OPEN);
            currentFps = savedInstanceState.getInt(STATE_CURRENT_FPS);
            mSettingsView = savedInstanceState.getBoolean(STATE_SETTINGS_VIEW);
            mReOpenSidebar = savedInstanceState.getBoolean(STATE_REOPEN_SIDEBAR);
            mSettingsBreakout = savedInstanceState.getBoolean(STATE_SETTINGS_BREAKOUT);
            mResetMalfunction = savedInstanceState.getBoolean(STATE_RESET_MALFUNCTION);
            mDialogFragmentKey = savedInstanceState.getString(STATE_DIALOG_FRAGMENT_KEY);
            mAssociatedDialogFragment = savedInstanceState.getInt(STATE_ASSOCIATED_DIALOG_FRAGMENT);

            if(mSettingsReset)
                mSettingsReset = false;

            // black screen fix
            mCoreFragment.setRecreateSurface(true);
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
                mDrawerOpenState = true;
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
        if( mGamePrefs.isTouchscreenEnabled )
        {
            // The touch map and overlay are needed to display frame rate and/or controls
            mTouchscreenMap = new VisibleTouchMap( this.getResources() );
            mTouchscreenMap.load( mGlobalPrefs.isCustomTouchscreenSkin ? null : this,
                    mGlobalPrefs.touchscreenSkinPath, mGamePrefs.touchscreenProfile,
                    mGlobalPrefs.touchscreenAnimated, mGlobalPrefs.touchscreenScale, mGlobalPrefs.touchscreenTransparency );

            mOverlay.initialize(mTouchscreenMap, !mGamePrefs.isTouchscreenHidden,
                    mGamePrefs.isAnalogHiddenWhenSensor, mGlobalPrefs.touchscreenAnimated);
        }

        if (mGlobalPrefs.isFpsEnabled) {
            mFpsOverlay.load(mGlobalPrefs.isCustomTouchscreenSkin ? null : this, getResources(), mGlobalPrefs.touchscreenSkinPath, mGlobalPrefs.fpsXPosition,
                    mGlobalPrefs.fpsYPosition, mGlobalPrefs.touchscreenScale);
            mFpsOverlay.onFpsChanged(currentFps);
        }

        // Initialize user interface devices
        initControllers(mOverlay);

        // Override the peripheral controllers' key provider, to add some extra
        // functionality
        mOverlay.setOnKeyListener(this);
        mOverlay.requestFocus();

        // Check periodically for touch input to determine if we should
        // hide the controls
        mHandler = new Handler(Looper.getMainLooper());
        mLastTouchTime = System.currentTimeMillis() / 1000L;

        if(mGlobalPrefs.touchscreenAutoHideEnabled)
            mHandler.postDelayed(mPeriodicChecker, 500);

        mDrawerLayout.setOnHoverListener((v, event) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    mHandler.postDelayed(() -> v.setPointerIcon(PointerIcon.getSystemIcon(GameActivity.this, PointerIcon.TYPE_ARROW)), 100);
                } else {
                    mHandler.postDelayed(() -> v.setPointerIcon(PointerIcon.getSystemIcon(GameActivity.this, PointerIcon.TYPE_NULL)), 100);
                }
            }
            return false;
        });

        hideSystemBars();

        mNetplayClientDialog = (NetplayClientSetupDialog) fm.findFragmentByTag(STATE_NETPLAY_CLIENT_DIALOG);
        mNetplayServerDialog = (NetplayServerSetupDialog) fm.findFragmentByTag(STATE_NETPLAY_SERVER_DIALOG);
    }

    @Override
    public void onFpsChanged(int newValue)
    {
        if(mGlobalPrefs.isFpsEnabled && mFpsOverlay != null && mCoreFragment != null)
        {
            float shaderFps = mGameSurface.getFps();

            int fps;
            if ( mCoreFragment.getCurrentSpeed() == CoreFragment.BASELINE_SPEED) {
                fps = Math.min((int)shaderFps, newValue);
            } else {
                fps = newValue;
            }

            if (fps > 0) {
                mFpsOverlay.onFpsChanged(fps);
                currentFps = fps;
            }
        }
    }

    @Override
    public void onStart()
    {
        super.onStart();
        Log.i(TAG, "onStart");

        final FragmentManager fm = this.getSupportFragmentManager();

        if (mIsNetplayEnabled && mIsNetplayServer) {
            mNetplayFragment = (NetplayFragment) fm.findFragmentByTag(STATE_NETPLAY_FRAGMENT);

            if(mNetplayFragment == null)
            {
                mNetplayFragment = new NetplayFragment();
                fm.beginTransaction().add(mNetplayFragment, STATE_NETPLAY_FRAGMENT).commit();
            }
        }

        if(mCoreFragment != null)
        {
            if (!mCoreFragment.IsInProgress()) {
                mCoreFragment.startCore(mGlobalPrefs, mGamePrefs, mRomGoodName, mRomDisplayName, mRomPath, mZipPath,
                        mRomMd5, mRomCrc, mRomHeaderName, mRomCountryCode, mRomArtPath, mDoRestart,
                        mDisplayResolutionData.getResolutionWidth(mGamePrefs.verticalRenderResolution),
                        mDisplayResolutionData.getResolutionHeight(mGamePrefs.verticalRenderResolution),
                        mIsNetplayEnabled, mSettingsReset);
            }

            // Try running now in case the core service has already started
            // If it hasn't started running yet, then check again when the core service connection happens
            // in onCoreServiceStarted
            tryRunning();
        }

        mGameSurface.startGlContext();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.i(TAG, "onResume");

        if (mSensorController != null) {
            mSensorController.onResume();
        }

        // Set the sidebar opacity
        mGameSidebar.setBackground(new DrawerDrawable(mGlobalPrefs.displayActionBarTransparency));

        if(!mSettingsView && mDrawerOpenState)
        {
            if(mCoreFragment != null)
            {
                mCoreFragment.pauseEmulator();
            }

            mDrawerLayout.openDrawer(GravityCompat.START);
            //mGameSidebar.requestFocus();
            mDrawerLayout.requestFocus();
            ReloadAllMenus();
        }

        if(mSettingsReset && !mResetMalfunction){
            mSettingsView = true;
            mGameSidebar.setVisibility(View.GONE);

            mDrawerLayout.openDrawer(GravityCompat.START);
            mGameSidebar.requestFocus();

            // locking to load data properly
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            runOnUiThread(this::gameSettingsDialogPrompt);
        }
        else if(mResetMalfunction){
            Notifier.showToast(this, R.string.toast_unexpectedExit);
            settingsViewReset();
            settingsReset(false);
            unlockOrientation();
            mSettingsReset = false; // delete?
            mSettingsRecreate = false;
            mResetMalfunction = false;
            getIntent().putExtra("gameOpenReset", true);
            getIntent().putExtra(ActivityHelper.Keys.SETTINGS_RESET, false);
            setResult(RESULT_OK, getIntent());
            finish();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        if(!isChangingConfigurations()){
            if(mSettingsReset)
                mResetMalfunction = true;
        }
    }

    @Override
    public void onSaveInstanceState( Bundle savedInstanceState )
    {
        savedInstanceState.putBoolean(STATE_DRAWER_OPEN, mDrawerOpenState);
        savedInstanceState.putInt(STATE_CURRENT_FPS, currentFps);
        savedInstanceState.putBoolean(STATE_SETTINGS_RECREATE, mSettingsRecreate);
        savedInstanceState.putBoolean(STATE_SETTINGS_VIEW, mSettingsView);
        savedInstanceState.putString(STATE_DIALOG_FRAGMENT_KEY, mDialogFragmentKey);
        savedInstanceState.putInt(STATE_ASSOCIATED_DIALOG_FRAGMENT, mAssociatedDialogFragment);
        savedInstanceState.putBoolean(STATE_REOPEN_SIDEBAR, mReOpenSidebar);
        savedInstanceState.putBoolean(STATE_SETTINGS_BREAKOUT,mSettingsBreakout);
        savedInstanceState.putBoolean(STATE_RESET_MALFUNCTION,mResetMalfunction);

        super.onSaveInstanceState( savedInstanceState );
    }

    @Override
    public void onStop()
    {
        super.onStop();

        Log.i( TAG, "onStop" );

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

        mGameSurface.stopGlContext();
    }

    //This is only called once when fragment is destroyed due to retaining the state
    @Override
    public void onDestroy()
    {
        Log.i( TAG, "onDestroy" );

        super.onDestroy();

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
        Log.i( TAG, "onWindowFocusChanged: " + hasFocus );
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

        mGamePrefs.putCurrentSlot(mCoreFragment.getSlot());
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
    public void onExitFinished()
    {
        finishAndRemoveTask();
    }

    @Override
    public void onBindService()
    {
        if(mCoreFragment.isShuttingDown())
        {
            Log.i(TAG, "Shutting down because previous instance hasn't finished");

            runOnUiThread(() -> Notifier.showToast( getApplicationContext(), R.string.toast_not_done_shutting_down ));

            finishActivity();
        }
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public Vibrator getVibrator(){
        Vibrator vibrator;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) this.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = manager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) this.getSystemService( Context.VIBRATOR_SERVICE );
        }

        // if statement?
        if (vibrator != null) {
            mCoreFragment.registerVibrator(1, vibrator);
        }
        return vibrator;
    }

    private void gameSettingsDialogPrompt(){
        final FragmentManager fm = this.getSupportFragmentManager();
        GameSettingsDialog gameSettings = (GameSettingsDialog) fm.findFragmentByTag(STATE_SETTINGS_FRAGMENT);
        if(gameSettings == null)
        {
            gameSettings = new GameSettingsDialog();
            fm.beginTransaction().add(gameSettings, STATE_SETTINGS_FRAGMENT).commit();
        }
    }

    @Override
    public void onGameSidebarAction(MenuItem menuItem)
    {
        if(mCoreFragment == null) return;

        if (menuItem.getTitle() != null) {
            Log.i(TAG, "User selected: " + menuItem.getTitle().toString());
        }

        if (menuItem.getItemId() ==  R.id.menuItem_exit) {
            mCoreFragment.exit();
        } else if (menuItem.getItemId() ==  R.id.menuItem_toggle_speed) {
            mCoreFragment.toggleSpeed();

            //Reload the menu with the new speed
            final MenuItem toggleSpeedItem =
                    mGameSidebar.getMenu().findItem(R.id.menuItem_toggle_speed);
            toggleSpeedItem.setTitle(this.getString(R.string.menuItem_toggleSpeed, mCoreFragment.getCurrentSpeed()));
            mGameSidebar.reload();
        } else if (menuItem.getItemId() ==  R.id.menuItem_set_speed ) {
            mCoreFragment.setCustomSpeedFromPrompt();
        } else if (menuItem.getItemId() ==  R.id.menuItem_screenshot) {
            mGameSurface.takeScreenshot(mGlobalPrefs.screenshotsDir, mRomGoodName);
        } else if (menuItem.getItemId() ==  R.id.menuItem_set_slot) {
            mCoreFragment.setSlotFromPrompt();
        } else if (menuItem.getItemId() ==  R.id.menuItem_slot_load) {
            mCoreFragment.loadSlot();
        } else if (menuItem.getItemId() ==  R.id.menuItem_slot_save) {
            mCoreFragment.saveSlot();

            if( mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
            {
                mDrawerLayout.closeDrawer( GravityCompat.START );
                mOverlay.requestFocus();
            }
        } else if (menuItem.getItemId() ==  R.id.menuItem_file_load) {
            mCoreFragment.loadFileFromPrompt();
        } else if (menuItem.getItemId() ==  R.id.menuItem_file_save) {
            mCoreFragment.saveFileFromPrompt();
        } else if (menuItem.getItemId() ==  R.id.menuItem_file_load_auto_save) {
            mCoreFragment.loadAutoSaveFromPrompt();
        } else if (menuItem.getItemId() ==  R.id.menuItem_disable_frame_limiter) {
            mCoreFragment.toggleFramelimiter();

            final int resId = mCoreFragment.getFramelimiter() ?
                R.string.menuItem_enableFramelimiter :
                R.string.menuItem_disableFramelimiter;

            //Reload the menu with the new speed
            final MenuItem frameLimiterItem =
                mGameSidebar.getMenu().findItem(R.id.menuItem_disable_frame_limiter);
            frameLimiterItem.setTitle(this.getString(resId));
            mGameSidebar.reload();
        } else if (menuItem.getItemId() ==  R.id.menuItem_player_one) {
            setPakTypeFromPrompt(1);
        } else if (menuItem.getItemId() ==  R.id.menuItem_player_two) {
            setPakTypeFromPrompt(2);
        } else if (menuItem.getItemId() ==  R.id.menuItem_player_three) {
            setPakTypeFromPrompt(3);
        } else if (menuItem.getItemId() ==  R.id.menuItem_player_four) {
            setPakTypeFromPrompt(4);
        } else if (menuItem.getItemId() ==  R.id.menuItem_setIme) {
            final InputMethodManager imeManager = (InputMethodManager)
                this.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imeManager != null)
                imeManager.showInputMethodPicker();
        } else if (menuItem.getItemId() == R.id.menuItem_settings) {
            //Close drawer
            mSettingsView = true;
            mGameSidebar.setVisibility(View.GONE);


            runOnUiThread(this::gameSettingsDialogPrompt);
        } else if (menuItem.getItemId() ==  R.id.menuItem_reset) {
            mCoreFragment.restart();
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

    private void tryRunning()
    {
        if (mCoreFragment != null && mCoreFragment.hasServiceStarted()) {
            mGameSurface.setSurfaceTexture(mCoreFragment.getSurfaceTexture());

            if (mDrawerLayout.isDrawerOpen(GravityCompat.START) || mDrawerOpenState) {
                mCoreFragment.pauseEmulator();
            } else {
                mCoreFragment.resumeEmulator();
            }
        }
    }

    // Tries to save and if the emulation is not in a safe state to save then it will
    // let the emulator run for a second before trying to save again
    private void safeAutoSave(){
        boolean saveNotifier = false;
        if(mCoreFragment == null)
            return;

        synchronized (mSaveLock) {
            while (mCoreFragment.getEmuModeInit() == 0) {
                try {
                    mSaveLock.wait(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if(mGlobalPrefs.maxAutoSaves != 0 && !mIsNetplayEnabled) {
            mCoreFragment.autoSaveState(false);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Log.i(TAG, "Can't sleep");
            }

            // no sleep on ui thread
            final Thread handler = new Thread(() -> {
                synchronized (mSaveLock) {
                    while (mCoreFragment.checkOnStateCallbackListeners()) {
                        try {
                            Log.i(TAG, "Sleeping tryingcount = " + safeSaveTryingCount);
                            mSaveLock.wait(1000);
                        } catch (InterruptedException e) {
                            Log.i(TAG, "Can't sleep");
                        }
                        if (safeSaveTryingCount > 5)
                            break;
                        safeSaveTryingCount++;
                        // do something after 1000ms
                    }
                }
                safeSaveTryingCount = 8;
            });

            handler.start();

            while(safeSaveTryingCount != 8) {
                synchronized (mSaveLock) {
                    try {
                        mSaveLock.wait(100);
                        if (!saveNotifier && safeSaveTryingCount != 8) {
                            saveNotifier = true;
                            runOnUiThread(() -> Notifier.showToast(getApplicationContext(), R.string.toast_savingFile, "to reset properly"));
                        }
                    } catch (InterruptedException e) {
                        Log.i(TAG, "Can't sleep");
                    }
                }
            }
            safeSaveTryingCount = 0;
        }
    }

    private void resetGlContext(){
        mGameSurface.stopGlContext();
        mGameSurface.startGlContext();
    }

    private void resetShaders(){
        resetGameSurfaceResolutionData();
        mGameSurface.setSelectedShader(mGlobalPrefs.getShaderPasses());
        mGameSurface.setShaderScaleFactor(mGlobalPrefs.shaderScaleFactor);
    }

    private void resetAppData(){
        mAppData = new AppData( this );
        mGlobalPrefs = new GlobalPrefs(this,mAppData);

        mGamePrefs = new GamePrefs( this, mRomMd5, mRomCrc, mRomHeaderName, mRomGoodName,
                CountryCode.getCountryCode(mRomCountryCode).toString(), mAppData, mGlobalPrefs );
    }

    private void resetCoreServiceAppData(){
        resetAppData();
        mCoreFragment.resetCoreServiceAppData();
    }

    private void resetGameSurfaceResolutionData(){
        resetAppData();

        mDisplayResolutionData = new DisplayResolutionData(mGlobalPrefs, this, mDrawerLayout, mGamePrefs.displayScaling);

        // Set parameters for shader view
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mGameSurface.getLayoutParams();
        params.width = Math.round ( mDisplayResolutionData.getSurfaceResolutionWidth() * ( mGamePrefs.videoSurfaceZoom / 100.f ) );
        params.height = Math.round ( mDisplayResolutionData.getSurfaceResolutionHeight() * ( mGamePrefs.videoSurfaceZoom / 100.f ) );
        params.gravity = Gravity.CENTER_HORIZONTAL;

        mGameSurface.setLayoutParams( params );

        mGameSurface.getHolder().setFixedSize(mDisplayResolutionData.getResolutionWidth(mGamePrefs.verticalRenderResolution)*mGlobalPrefs.shaderScaleFactor,
                mDisplayResolutionData.getResolutionHeight(mGamePrefs.verticalRenderResolution)*mGlobalPrefs.shaderScaleFactor);

    }

    private void resetTouchscreenControls(){
        // Initialize the screen elements
        if( mGamePrefs.isTouchscreenEnabled )
        {
            resetAppData();
            mTouchscreenMap.clear();

            // The touch map and overlay are needed to display frame rate and/or controls
            mTouchscreenMap.load( mGlobalPrefs.isCustomTouchscreenSkin ? null : this,
                    mGlobalPrefs.touchscreenSkinPath, mGamePrefs.touchscreenProfile,
                    mGlobalPrefs.touchscreenAnimated, mGlobalPrefs.touchscreenScale, mGlobalPrefs.touchscreenTransparency );

            mOverlay.initialize(mTouchscreenMap, !mGamePrefs.isTouchscreenHidden,//
                    mGamePrefs.isAnalogHiddenWhenSensor, mGlobalPrefs.touchscreenAnimated);//

            mOverlay.mShowTouchscreen.run();
        }
    }

    // Resets the auto hold & relative joystick features
    private void resetMoreTouchscreenControls(){
        resetAppData();
        // By default, send Player 1 rumbles through phone vibrator
        Vibrator vibrator = getVibrator();
        // Create the touchscreen controller
        mTouchscreenController = new TouchController(mCoreFragment, mTouchscreenMap,
                mOverlay, vibrator, mGamePrefs.touchscreenAutoHold,
                mGlobalPrefs.isTouchscreenFeedbackEnabled, mGamePrefs.touchscreenNotAutoHoldables,
                mSensorController, mGamePrefs.invertTouchXAxis, mGamePrefs.invertTouchYAxis,
                mGamePrefs.isTouchscreenAnalogRelative );
        resetAppData();
        resetTouchscreenControls();
    }

    // Used to reset and pause the game upon restarting
    private void settingsResetImmediately(){
        safeAutoSave();
        getIntent().putExtra("gameOpenReset", true);
        getIntent().putExtra(ActivityHelper.Keys.SETTINGS_RESET, true);
        setResult(RESULT_OK, getIntent());

        if(mCoreFragment != null && mCoreFragment.hasServiceStarted())
            mCoreFragment.shutdownEmulator();

        finish();

        // testing this get rid of after
        if(mCoreFragment != null && mCoreFragment.isShuttingDown())
        {
            Log.i(TAG, "Shutting down because previous instance hasn't finished");

            finishActivity();
        }
    }

    // Sets intent & core variable
    private void settingsReset(boolean settingsReset){
        mSettingsReset = settingsReset;
        if(mCoreFragment != null)
            mCoreFragment.settingsReset(settingsReset);
        getIntent().removeExtra(ActivityHelper.Keys.SETTINGS_RESET);
        getIntent().putExtra(ActivityHelper.Keys.SETTINGS_RESET, settingsReset);
    }

    private void unlockOrientation(){
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        // Set the screen orientation
        if (mGlobalPrefs.displayOrientation != -1) {
            setRequestedOrientation( mGlobalPrefs.displayOrientation );
        }
    }

    // After the dialog fragment completes, it calls this callback.
    public void onComplete(String string) {
        switch(string){
            case "resetAppData":
                resetAppData();
                break;
            case "resetSurface":
                resetGlContext();
                break;
            case "pauseEmulator":
                if(mSettingsReset)
                    break;
                mSettingsView = true;
                mGameSidebar.setVisibility(View.GONE);
                mDrawerLayout.openDrawer(GravityCompat.START,false);
                if(mCoreFragment != null)
                    mCoreFragment.pauseEmulator();
                break;
            case "resumeEmulator":
                tryRunning();
                break;
            case "settingsResetFalse":
                settingsReset(false);
                break;
            case "displayResolution":
                if(mGamePrefs.videoPlugin.name.equals("angrylion-plus"))
                    break;
                else if(mGamePrefs.videoPlugin.name.toLowerCase().contains("glide64")) {
                    settingsResetImmediately();
                    break;
                }

                int volume = mCoreFragment.getVolume();
                mCoreFragment.setVolume(0);
                resetGameSurfaceResolutionData();

                int width = mDisplayResolutionData.getResolutionWidth(mGlobalPrefs.displayResolution);
                int resolution = (width << 16) | mDisplayResolutionData.getResolutionHeight(mGlobalPrefs.displayResolution);

                mCoreFragment.setResolution(resolution);

                mCoreFragment.resumeEmulator();

                mCoreFragment.resetCoreServiceAppData();

                mDrawerLayout.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    int oldWidth = oldRight - oldLeft;
                    int oldHeight = oldBottom - oldTop;
                    if( v.getWidth() != oldWidth || v.getHeight() != oldHeight )
                    {
                        DisplayResolutionData resolutionData = new DisplayResolutionData(mGlobalPrefs, this, mDrawerLayout, mGamePrefs.displayScaling);
                        FrameLayout.LayoutParams newParams = (FrameLayout.LayoutParams) mGameSurface.getLayoutParams();
                        newParams.width = Math.round ( resolutionData.getSurfaceResolutionWidth() * ( mGamePrefs.videoSurfaceZoom / 100.f ) );
                        newParams.height = Math.round ( resolutionData.getSurfaceResolutionHeight() * ( mGamePrefs.videoSurfaceZoom / 100.f ) );
                        mGameSurface.setLayoutParams( newParams );
                    }
                });

                mCoreFragment.settingsReset(true);
                recreate();

                if(!mIsNetplayEnabled) {
                    int waitingAttempt = 0;
                    while (mCoreFragment.getState() != CoreTypes.m64p_emu_state.M64EMU_PAUSED &&
                        waitingAttempt++ < 30) {
                        if(waitingAttempt <= 1)
                            Notifier.showToast(this,R.string.toast_pleaseWait);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                tryRunning(); // probably not needed

                mCoreFragment.setVolume(volume);
                break;
            case "displayScaling": case "videoHardwareType": case "videoPolygonOffset":
                settingsResetImmediately();
                return;
            case "displayZoomSeek":
                resetGameSurfaceResolutionData();
                break;
            case "displayOrientation":
                resetGameSurfaceResolutionData();
                // Set the screen orientation
                setRequestedOrientation( mGlobalPrefs.displayOrientation );
                // If other changes were made then setting to auto will mess up those settings so we reset here
                if (mGlobalPrefs.displayOrientation == -1)
                    settingsResetImmediately();
                break;
            case "displayActionBarTransparency":
                resetAppData();
                mGameSidebar.setBackground(new DrawerDrawable(mGlobalPrefs.displayActionBarTransparency));
                mGameSidebar.reload();
                break;
            case "displayFpsV2":
                resetAppData();
                mFpsOverlay.clear();
                mFpsOverlay.invalidate();
                if (mGlobalPrefs.isFpsEnabled) {
                    mFpsOverlay.load(mGlobalPrefs.isCustomTouchscreenSkin ? null : this, getResources(), mGlobalPrefs.touchscreenSkinPath, mGlobalPrefs.fpsXPosition,
                            mGlobalPrefs.fpsYPosition, mGlobalPrefs.touchscreenScale);
                    onFpsChanged(currentFps);
                }
                break;
            case "resetShaders": case "resetShadersFirstPass":
                resetShaders();
                resetGlContext();
                break;
            case "resetShaderScaleFactor":
                resetShaders();
                mGameSurface.setShaderScaleFactor(mGlobalPrefs.shaderScaleFactor);
                resetGlContext();
                mGameSurface.setSurfaceTexture(mCoreFragment.getSurfaceTexture());
                break;
            case "coreRecreateSurface":
                mCoreFragment.setRecreateSurface(true);
                break;
            case "touchscreenScaleV2": case "touchscreenTransparencyV2": case "touchscreenAnimated_v2":
            case "touchscreenSkin_v2": case "resetTouchscreenController":
                resetTouchscreenControls();
                break;
            case "touchscreenAutoHoldV2": case "touchscreenAnalogRelative_global": case "touchscreenFeedback":
                resetMoreTouchscreenControls();
                break;
            case "touchscreenAutoHideSeconds":
                recreate();
                break;
            case "inGameMenu":
                resetAppData();
                if(mGlobalPrefs.inGameMenuIsSwipeGesture)
                {
                    mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                }
                else
                {
                    mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                }
                break;
            case "allEmulatedControllersPlugged": case "inputShareController": case "autoPlayerMapping":
            case "holdButtonForMenu": case "useRaphnetAdapter": case "initiateControllers":
                //reset core service
                resetCoreServiceAppData();
                if(!mIsNetplayEnabled)
                    mCoreFragment.resetCoreServiceControllers();
                else
                    mCoreFragment.resetCoreServiceControllersNetplay(mNetplayFragment);
                resetGlContext();
                break;
            case "gameAutoSaves":
                resetCoreServiceAppData();
                recreate(); // enabling preferences
                break;
            case "settingsReset":
                mSettingsReset = true;
                break;
            case "settingsRecreate":
                mSettingsRecreate = true;
                break;
            case "openDrawerState":
                mDrawerOpenState = false;
                break;
            case "iplFile":
                resetAppData();
                getIntent().putExtra("gameOpenReset", true);
                getIntent().putExtra(ActivityHelper.Keys.SETTINGS_RESET, false);
                getIntent().putExtra(ActivityHelper.Keys.DO_RESTART, true);
                setResult(RESULT_OK, getIntent());
                finish();
                return;
            case "gameSettingDialogClosed":
                // Checking orientation lock
                if(!mSettingsReset){
                    if(getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LOCKED){
                        unlockOrientation();
                    }
                }

                settingsReset(false);
                tryRunning();

                if(mCoreFragment != null){
                    // This means we broke out of the settings dialog fragment before
                    // the cpu instruction compiler/interpreter got initiated, so best
                    // to recreate
                    if(mCoreFragment.getEmuModeInit() == 0 && !mSettingsBreakout) {
                        mSettingsBreakout = true;
                        recreate();
                    }
                    else if(mSettingsBreakout)
                        mSettingsBreakout = false;
                }
                break;
            default:
                break;
        }
    }

    public void settingsViewReset(){
        if(mSettingsView){
            if(mReOpenSidebar){
                mGameSidebar.setVisibility(View.GONE);
                mDrawerLayout.openDrawer(GravityCompat.START, false);
                mReOpenSidebar = false;
            }
            mSettingsView = false;

            // Certain settings require the game data to be reloaded
            if(mSettingsReset) {
                mSettingsReset = false;
                mSettingsRecreate = false;
                // User triggered reset and then set max auto saves after,
                // auto save once and then set maxAutoSaves to 0 after reset
                if(mGlobalPrefs.maxAutoSaves == 0) {
                    mGlobalPrefs.putInt("gameAutoSaves",11);
                    resetCoreServiceAppData();
                }
                safeAutoSave();
                getIntent().putExtra("gameOpenReset", true);
                getIntent().putExtra(ActivityHelper.Keys.SETTINGS_RESET, false);
                getIntent().putExtra(ActivityHelper.Keys.DO_RESTART, false);
                setResult(RESULT_OK, getIntent());
                finish();
                return;
            }
            if(mSettingsRecreate){
                recreate();
            }
            mGameSidebar.setVisibility(View.VISIBLE);
            if( mDrawerLayout.isDrawerOpen( GravityCompat.START ) || mDrawerOpenState)
            {
                mDrawerLayout.closeDrawer( GravityCompat.START,false );
                mOverlay.requestFocus();
            }
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
        Log.i(TAG, "onCoreServiceStarted");

        if(mCoreFragment == null) return;

        getVibrator();

        ReloadAllMenus();

        mDrawerLayout.closeDrawer(GravityCompat.START);
        mOverlay.requestFocus();
        mGameSurface.setSurfaceTexture(mCoreFragment.getSurfaceTexture());

        if (mIsNetplayEnabled) {
            final FragmentManager fm = this.getSupportFragmentManager();

            if (mIsNetplayServer && mNetplayServerDialog == null) {
                mNetplayServerDialog = NetplayServerSetupDialog.newInstance(mRomMd5,
                        mGamePrefs.videoPluginLib.getPluginLib(),
                        mGamePrefs.rspPluginLib.getPluginLib(),
                        mServerPort);

                try {
                    mNetplayServerDialog.show(fm, STATE_NETPLAY_SERVER_DIALOG);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }

            if (!mIsNetplayServer && mNetplayClientDialog == null) {
                mNetplayClientDialog = NetplayClientSetupDialog.newInstance(mRomMd5);

                try {
                    mNetplayClientDialog.show(fm, STATE_NETPLAY_CLIENT_DIALOG);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        }

        if(mShouldExit)
        {
            mCoreFragment.shutdownEmulator();
            finish();
        }

        if(!mCoreFragment.isShuttingDown()) {
            //This can happen if GameActivity is killed while service is running
            tryRunning();
        }
    }

    @Override
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public void onRecreateSurface()
    {
        mReOpenSidebar = true;
        tryRunning();
    }

    @Override
    public void onGameStarted()
    {
        final Thread handler = new Thread(() -> {
            int startingGameAttempt = 0;

            // Making sure the cpu recompiler/interpreter is initiated before setting the
            // screen orientation
            while ((mCoreFragment.getEmuModeInit() == 0)
                    && startingGameAttempt++ < 100) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.i(TAG, "Can't sleep");
                }
            }

            // If we're resetting from changing certain video settings in game, then this will
            // make sure we set the screen orientation after the graphics plugin pauses the game
            // to display whatever changes were made
            if(mSettingsReset) {
                startingGameAttempt = 0;
                int loadAttempt = 0;

                while((mCoreFragment == null || mCoreFragment.getAudioInit() == 0)
                        && startingGameAttempt++ < 50){
                    try {
                        if (mCoreFragment != null && loadAttempt == 0) {
                            mCoreFragment.loadLatestAutoSave();
                            loadAttempt = 1;
                        }
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (mCoreFragment != null && loadAttempt == 0)
                    mCoreFragment.loadLatestAutoSave();

                mSettingsReset = false;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }

            // Set the screen orientation
            if (mGlobalPrefs.displayOrientation != -1) {
                setRequestedOrientation( mGlobalPrefs.displayOrientation );
            }

            final FragmentManager fm = getSupportFragmentManager();
            runOnUiThread(() -> {
                GameSettingsDialog gameSettings = (GameSettingsDialog) fm.findFragmentByTag(STATE_SETTINGS_FRAGMENT);
                if (gameSettings != null) {
                    gameSettings.resetPreferencesFromSettingsReset();
                }
                else{
                    // User has exited the settings fragment before everything initialized so we need to reset
                    // everything that would have if they exited properly (after it initialized)
                    settingsViewReset();
                    settingsReset(false);
                    unlockOrientation();
                    mDialogFragmentKey = "";
                }
            });

            // User set max auto save to 0 after setting something to recreate, so we auto saved to prevent
            // data loss and are setting it back to 0 here
            if(mGlobalPrefs.maxAutoSaves == 11) {
                mGlobalPrefs.putInt("gameAutoSaves", 0);
                // deleting leftover auto saves
                resetCoreServiceAppData();
            }
        });

        handler.start();
    }

    @Override
    public void onExitRequested(boolean shouldExit)
    {
        Log.i( TAG, "onExitRequested" );
        if(shouldExit)
        {
            shutdownEmulator();
        }
        else if( !mDrawerLayout.isDrawerOpen( GravityCompat.START ) && mCoreFragment != null &&
                !mSettingsReset)
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
            mCoreFragment = null;
        }

        setResult(RESULT_OK, null);
        finish();
    }

    @Override
    public void onBackPressed()
    {
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK);
        event.setSource(InputDevice.SOURCE_KEYBOARD);
        onKey(mOverlay, KeyEvent.KEYCODE_BACK, event);
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
        boolean isKeyboard = (event.getSource() & InputDevice.SOURCE_GAMEPAD) != InputDevice.SOURCE_GAMEPAD &&
                (event.getSource() & InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK;
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
                    if(mGlobalPrefs.inGameMenuIsSwipeGesture)
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
        Vibrator vibrator = getVibrator();

        // Create the touchscreen controls
        if( mGamePrefs.isTouchscreenEnabled )
        {
            if (!TextUtils.isEmpty(mGamePrefs.sensorAxisX) || !TextUtils.isEmpty(mGamePrefs.sensorAxisY)) {
                // Create the sensor controller
                final SensorManager sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
                mSensorController = new SensorController(mCoreFragment, sensorManager, mOverlay, mGamePrefs.sensorAxisX,
                        mGamePrefs.sensorSensitivityX, mGamePrefs.sensorAxisY,
                        mGamePrefs.sensorSensitivityY);
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
//            mDrawerLayout.setTouchMap( mTouchscreenMap );
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
        mAxisProvider = new AxisProvider(inputSource);

        // Request focus for proper listening
        inputSource.requestFocus();

        // Create the peripheral controls to handle key/stick presses
        for(int index = 0; index < mGamePrefs.isControllerEnabled.length; ++index) {
            isControllerPlugged[index] = mGamePrefs.isPlugged[index];

            if( mGamePrefs.isControllerEnabled[index])
            {
                final ControllerProfile p = mGamePrefs.controllerProfile[index];
                initSingleController(index + 1, p);
            }
        }
    }

    private void initSingleController(int player, ControllerProfile p)
    {
        if(p != null) {
           new PeripheralController( mCoreFragment, mGameSurface, mGlobalPrefs, mRomGoodName,
                   player, mGamePrefs.playerMap, p.getMap(), p.getAutoDeadzone(),
                   p.getDeadzone(), p.getSensitivityX(), p.getSensitivityY(), mGlobalPrefs.holdControllerButtons,
                   mOverlay, this, null, mKeyProvider, mAxisProvider);
            Log.i(TAG, "Player " + player + " controller has been enabled");
        }
    }

    private void hideSystemBars()
    {
        if( mGlobalPrefs.isImmersiveModeEnabled )
        {
            DisplayWrapper.enableImmersiveMode(this);
        }
    }

    private void shutdownEmulator()
    {
        Log.i( TAG, "shutdownEmulator" );

        if(mCoreFragment != null && mCoreFragment.hasServiceStarted())
        {
            if (mNetplayFragment != null) {
                mNetplayFragment.onFinish();
            }

            //Generate auto save file
            if(mGlobalPrefs.maxAutoSaves != 0 && !mIsNetplayEnabled)
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

                        Log.i(TAG, "controller " + index + " was unplugged");
                    }
                }
            }

            mHandler.postDelayed(mPeriodicChecker, 500);

            if (mForceExit) {
                Log.w(TAG, "Exit forced");

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

    @Override
    public void connect(int regId, int player, String videoPlugin, String rspPlugin, InetAddress address, int port) {
        mCoreFragment.connectForNetplay(regId, player, videoPlugin, rspPlugin, address, port);
    }

    @Override
    public void start()
    {
        mCoreFragment.startNetplay();

        if (mIsNetplayServer && mNetplayServerDialog != null) {
            mNetplayServerDialog.dismiss();
        }

        if (!mIsNetplayServer && mNetplayClientDialog != null) {
            mNetplayClientDialog.dismiss();
        }
    }

    @Override
    public void cancel()
    {
        if (mIsNetplayServer && mNetplayServerDialog != null) {
            mNetplayServerDialog.dismiss();
        }

        if (!mIsNetplayServer && mNetplayClientDialog != null) {
            mNetplayClientDialog.dismiss();
        }

        if (mNetplayFragment != null) {
            mNetplayFragment.onFinish();
        }
        mCoreFragment.shutdownEmulator();
    }

    @Override
    public void mapPorts(int roomPort)
    {
        if (mNetplayFragment != null) {
            mNetplayFragment.mapPorts(roomPort);
        }
    }

    @Override
    public void onPortObtained(int port) {
        mServerPort = port;
    }

    /**
     * Callback when a UDP port has been mapped
     * @param tcpPort1 Port for room server
     * @param tcpPort2 Port for TCP netplay server
     * @param udpPort2 Port for UDP netplay server
     */
    @Override
    public void onUpnpPortsObtained(int tcpPort1, int tcpPort2, int udpPort2)
    {
        if (mIsNetplayServer && mNetplayServerDialog != null) {
            mNetplayServerDialog.onUpnpPortsObtained(tcpPort1, tcpPort2, udpPort2);
        }
    }

    @Override
    public void onDialogClosed(int inputCode, int hardwareId, int which) {
        final FragmentManager fm = this.getSupportFragmentManager();
        GameSettingsDialog gameSettings = (GameSettingsDialog) fm.findFragmentByTag(STATE_SETTINGS_FRAGMENT);
        if(gameSettings == null)
            return;
        final PlayerMapPreference playerPref = gameSettings.findPlayerMapPreferenceSettings();
        if (playerPref == null)
            return;
        playerPref.onDialogClosed(hardwareId, which);
    }

    public void recreateSurface(){
        mCoreFragment.setRecreateSurface(true);
    }

    public String getDialogFragmentKey(){
        return mDialogFragmentKey;
    }

    public void setDialogFragmentKey(String key){
        mDialogFragmentKey = key;
    }

    public int getAssociatedDialogFragment(){
        return mAssociatedDialogFragment;
    }

    public void setAssociatedDialogFragment(int associatedDialogFragment){
        mAssociatedDialogFragment = associatedDialogFragment;
    }

    public boolean getSettingsReset(){
        return mSettingsReset;
    }

    public boolean getNetplayEnabled() { return mIsNetplayEnabled; }

    public NetplayFragment getNetplayFragment() { return mNetplayFragment; }

    public AppData getAppData() { return mAppData; }

    public GlobalPrefs getGlobalPrefs(){
        return mGlobalPrefs;
    }

    public GamePrefs getGamePrefs(){
        return mGamePrefs;
    }

    public boolean isDdActive(){
        if(mCoreFragment == null)
            return false;
        return mCoreFragment.isDdActive();
    }
}
