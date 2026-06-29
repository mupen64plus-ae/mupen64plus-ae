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
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;

import androidx.activity.OnBackPressedCallback;
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
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.PointerIcon;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.HttpURLConnection;
import java.net.URL;

import paulscode.android.mupen64plusae.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.DrawerDrawable;
import paulscode.android.mupen64plusae.jni.RetroAchievementsManager;
import paulscode.android.mupen64plusae.GameSidebar;
import paulscode.android.mupen64plusae.GameSidebar.GameSidebarActionHandler;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog.PromptConfirmListener;
import paulscode.android.mupen64plusae.dialog.Prompt;
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
import paulscode.android.mupen64plusae.netplay.NetplayFragment;
import paulscode.android.mupen64plusae.netplay.room.NetplayClientSetupDialog;
import paulscode.android.mupen64plusae.netplay.room.NetplayServerSetupDialog;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.jni.CoreTypes.PakType;
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
        RetroAchievementsManager.GameLoadListener
{
    private static final String TAG = "GameActivity";

    // Leaderboard tracker views keyed by tracker ID (bottom-right)
    private final android.util.SparseArray<TextView> mLeaderboardTrackers = new android.util.SparseArray<>();
    private LinearLayout mTrackerContainer;

    // Challenge indicator views keyed by achievement ID (top-right) — ImageViews
    private final android.util.SparseArray<View> mChallengeViews = new android.util.SparseArray<>();
    private LinearLayout mChallengeContainer;

    // Single progress indicator (bottom-left): icon + text
    private LinearLayout mProgressContainer;
    private ImageView    mProgressIcon;
    private TextView     mProgressText;

    // Live achievements dialog — non-null while the dialog is open
    private androidx.appcompat.app.AlertDialog mAchievementsDialog;
    private android.widget.ArrayAdapter<AchievementItem> mAchievementsAdapter;
    private ArrayList<AchievementItem> mAchievementItems;


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

    private static final String STATE_NETPLAY_FRAGMENT = "STATE_NETPLAY_FRAGMENT";
    private NetplayFragment mNetplayFragment = null;

    private final boolean[] isControllerPlugged = new boolean[4];

    private static final String STATE_CURRENT_FPS = "STATE_CURRENT_FPS";
    private int currentFps = -1;

    private static final String STATE_NETPLAY_CLIENT_DIALOG = "STATE_NETPLAY_CLIENT_DIALOG";
    private NetplayClientSetupDialog mNetplayClientDialog = null;

    private static final String STATE_NETPLAY_SERVER_DIALOG = "STATE_NETPLAY_SERVER_DIALOG";
    private NetplayServerSetupDialog mNetplayServerDialog = null;

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

        // Container for leaderboard tracker overlays — bottom-right corner
        mTrackerContainer = new LinearLayout(this);
        mTrackerContainer.setOrientation(LinearLayout.VERTICAL);
        float dp = getResources().getDisplayMetrics().density;
        int margin = (int)(8 * dp);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        lp.setMargins(0, 0, margin, margin);
        mTrackerContainer.setLayoutParams(lp);
        ((FrameLayout) mDrawerLayout.getChildAt(0)).addView(mTrackerContainer);

        // Challenge indicator container — top-right
        mChallengeContainer = new LinearLayout(this);
        mChallengeContainer.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        clp.setMargins(0, margin, margin, 0);
        mChallengeContainer.setLayoutParams(clp);
        ((FrameLayout) mDrawerLayout.getChildAt(0)).addView(mChallengeContainer);

        // Progress indicator — bottom-right, horizontal: [icon] [progress numbers], hidden until needed
        int iconSize = (int)(40 * dp);
        mProgressContainer = new LinearLayout(this);
        mProgressContainer.setOrientation(LinearLayout.HORIZONTAL);
        mProgressContainer.setBackgroundColor(0xCC000000);
        mProgressContainer.setPadding(margin / 2, margin / 2, margin / 2, margin / 2);
        mProgressContainer.setVisibility(View.GONE);

        mProgressIcon = new ImageView(this);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconLp.setMargins(0, 0, margin / 2, 0);
        mProgressIcon.setLayoutParams(iconLp);
        mProgressIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mProgressContainer.addView(mProgressIcon);

        mProgressText = new TextView(this);
        mProgressText.setTextColor(Color.WHITE);
        mProgressText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        mProgressText.setTypeface(android.graphics.Typeface.MONOSPACE);
        mProgressContainer.addView(mProgressText);

        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        plp.setMargins(0, 0, margin, margin);
        mProgressContainer.setLayoutParams(plp);
        ((FrameLayout) mDrawerLayout.getChildAt(0)).addView(mProgressContainer);



        // Don't darken the game screen when the drawer is open
        mDrawerLayout.setScrimColor(0x0);

        if(mGlobalPrefs.inGameMenuIsSwipGesture)
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
        else
        {
            mDrawerOpenState = savedInstanceState.getBoolean(STATE_DRAWER_OPEN);
            currentFps = savedInstanceState.getInt(STATE_CURRENT_FPS);
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
                    mGlobalPrefs.isTouchscreenAnimated, mGlobalPrefs.touchscreenScale, mGlobalPrefs.touchscreenTransparency );

            mOverlay.initialize(mTouchscreenMap, !mGamePrefs.isTouchscreenHidden,
                    mGamePrefs.isAnalogHiddenWhenSensor, mGlobalPrefs.isTouchscreenAnimated);
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
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mHandler.postDelayed(() -> v.setPointerIcon(PointerIcon.getSystemIcon(GameActivity.this, PointerIcon.TYPE_ARROW)), 100);
            } else {
                mHandler.postDelayed(() -> v.setPointerIcon(PointerIcon.getSystemIcon(GameActivity.this, PointerIcon.TYPE_NULL)), 100);
            }
            return false;
        });

        hideSystemBars();

        mNetplayClientDialog = (NetplayClientSetupDialog) fm.findFragmentByTag(STATE_NETPLAY_CLIENT_DIALOG);
        mNetplayServerDialog = (NetplayServerSetupDialog) fm.findFragmentByTag(STATE_NETPLAY_SERVER_DIALOG);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK);
                event.setSource(InputDevice.SOURCE_KEYBOARD);
                onKey(mOverlay, KeyEvent.KEYCODE_BACK, event);
            }
        });
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
                        mIsNetplayEnabled);
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
        RetroAchievementsManager.setGameLoadListener(this);

        if (mSensorController != null) {
            mSensorController.onResume();
        }

        // Set the sidebar opacity
        mGameSidebar.setBackground(new DrawerDrawable(mGlobalPrefs.displayActionBarTransparency));

        if(mDrawerOpenState)
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
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        RetroAchievementsManager.setGameLoadListener(null);
        if (mTrackerContainer != null) {
            mTrackerContainer.removeAllViews();
            mLeaderboardTrackers.clear();
        }
        if (mChallengeContainer != null) {
            mChallengeContainer.removeAllViews();
            mChallengeViews.clear();
        }
        if (mProgressContainer != null) {
            mProgressContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSaveInstanceState( Bundle savedInstanceState )
    {
        savedInstanceState.putBoolean(STATE_DRAWER_OPEN, mDrawerOpenState);
        savedInstanceState.putInt(STATE_CURRENT_FPS, currentFps);

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

    //This is only called once when fragment is destroyed due to rataining the state
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
        } else if (menuItem.getItemId() ==  R.id.menuItem_reset) {
            mCoreFragment.restart();
        } else if (menuItem.getItemId() == R.id.menuItem_achievements) {
            showAchievementsDialog();
        }
    }

    private static final class AchievementItem {
        final String title, description, badgeUrl;
        final int points;
        final boolean unlocked;
        AchievementItem(String t, String d, int p, String b, boolean u) {
            title = t; description = d; points = p; badgeUrl = b; unlocked = u;
        }
    }

    private void refreshAchievementsDialog() {
        if (mAchievementsAdapter == null || mAchievementItems == null || mAchievementsDialog == null) return;
        String json = RetroAchievementsManager.getAchievementsJson();
        try {
            JSONArray arr = new JSONArray(json);
            ArrayList<AchievementItem> unlocked = new ArrayList<>();
            ArrayList<AchievementItem> locked   = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                boolean u = o.optInt("u", 0) == 1;
                AchievementItem item = new AchievementItem(
                        o.optString("t", "?"), o.optString("d", ""),
                        o.optInt("p", 0), o.optString("b", ""), u);
                if (u) unlocked.add(item); else locked.add(item);
            }
            mAchievementItems.clear();
            mAchievementItems.addAll(unlocked);
            mAchievementItems.addAll(locked);
            mAchievementsAdapter.notifyDataSetChanged();
            String gameTitle = mRomGoodName != null ? mRomGoodName : "";
            mAchievementsDialog.setTitle(getString(R.string.ra_achievements_title,
                    gameTitle, unlocked.size(), mAchievementItems.size()));
        } catch (Exception e) {
            Log.e(TAG, "Failed to refresh achievements JSON", e);
        }
    }

    private void showAchievementsDialog() {
        String json = RetroAchievementsManager.getAchievementsJson();
        try {
            JSONArray arr = new JSONArray(json);
            if (arr.length() == 0) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(R.string.menuItem_achievements)
                        .setMessage(R.string.ra_achievements_not_loaded)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return;
            }

            ArrayList<AchievementItem> unlocked = new ArrayList<>();
            ArrayList<AchievementItem> locked   = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                boolean u = o.optInt("u", 0) == 1;
                AchievementItem item = new AchievementItem(
                        o.optString("t", "?"),
                        o.optString("d", ""),
                        o.optInt("p", 0),
                        o.optString("b", ""),
                        u);
                if (u) unlocked.add(item); else locked.add(item);
            }
            mAchievementItems = new ArrayList<>(unlocked);
            mAchievementItems.addAll(locked);

            String gameTitle  = mRomGoodName != null ? mRomGoodName : "";
            String dialogTitle = getString(R.string.ra_achievements_title,
                    gameTitle, unlocked.size(), mAchievementItems.size());

            float dp      = getResources().getDisplayMetrics().density;
            int iconSize  = (int)(40 * dp);
            int padSmall  = (int)(4 * dp);
            int padMedium = (int)(8 * dp);

            mAchievementsAdapter =
                    new android.widget.ArrayAdapter<AchievementItem>(this, 0, mAchievementItems) {
                @Override
                public View getView(int position, View convertView, android.view.ViewGroup parent) {
                    LinearLayout row;
                    ImageView icon;
                    TextView titleTv, descTv;
                    if (convertView instanceof LinearLayout) {
                        row = (LinearLayout) convertView;
                        icon = (ImageView) row.getTag(R.id.menuItem_achievements);
                        LinearLayout textBlock = (LinearLayout) row.getChildAt(1);
                        titleTv = (TextView) textBlock.getChildAt(0);
                        descTv  = (TextView) textBlock.getChildAt(1);
                    } else {
                        row = new LinearLayout(getContext());
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setPadding(padMedium, padSmall, padMedium, padSmall);

                        icon = new ImageView(getContext());
                        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(iconSize, iconSize);
                        ilp.setMargins(0, 0, padMedium, 0);
                        ilp.gravity = android.view.Gravity.CENTER_VERTICAL;
                        icon.setLayoutParams(ilp);
                        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        row.addView(icon);
                        row.setTag(R.id.menuItem_achievements, icon);

                        LinearLayout textBlock = new LinearLayout(getContext());
                        textBlock.setOrientation(LinearLayout.VERTICAL);
                        textBlock.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT));

                        titleTv = new TextView(getContext());
                        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
                        textBlock.addView(titleTv);

                        descTv = new TextView(getContext());
                        descTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                        textBlock.addView(descTv);

                        row.addView(textBlock);
                    }

                    AchievementItem item = getItem(position);
                    icon.setImageBitmap(null);
                    icon.setTag(item.badgeUrl);  // URL as tag to discard stale async callbacks
                    icon.setAlpha(item.unlocked ? 1f : 0.5f);
                    titleTv.setText(item.title + " (" + item.points + " pts)");
                    titleTv.setAlpha(item.unlocked ? 1f : 0.6f);
                    descTv.setText(item.description);
                    descTv.setAlpha(item.unlocked ? 0.8f : 0.5f);

                    if (item.badgeUrl != null && !item.badgeUrl.isEmpty()) {
                        String url = item.badgeUrl;
                        loadBitmapAsync(url, bmp -> {
                            if (bmp != null && url.equals(icon.getTag())) icon.setImageBitmap(bmp);
                        });
                    }
                    return row;
                }
            };

            mAchievementsDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(dialogTitle)
                    .setAdapter(mAchievementsAdapter, null)
                    .setPositiveButton(android.R.string.ok, null)
                    .setOnDismissListener(d -> {
                        mAchievementsDialog = null;
                        mAchievementsAdapter = null;
                        mAchievementItems = null;
                    })
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse achievements JSON", e);
        }
    }

    @Override
    public void onRaChallengeIndicator(int type, int id, String title, String badgeUrl) {
        if (mChallengeContainer == null) return;
        float dp = getResources().getDisplayMetrics().density;

        if (type == RetroAchievementsManager.CHALLENGE_HIDE) {
            View v = mChallengeViews.get(id);
            if (v != null) {
                mChallengeContainer.removeView(v);
                mChallengeViews.remove(id);
            }
            return;
        }

        // CHALLENGE_SHOW — display the achievement badge icon
        int size = (int)(40 * dp);
        ImageView img = new ImageView(this);
        img.setBackgroundColor(0xAA000000);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        img.setContentDescription(title);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMargins(0, (int)(2 * dp), 0, 0);
        img.setLayoutParams(lp);
        mChallengeViews.put(id, img);
        mChallengeContainer.addView(img);

        if (badgeUrl != null && !badgeUrl.isEmpty()) {
            loadBitmapAsync(badgeUrl, bmp -> { if (bmp != null) img.setImageBitmap(bmp); });
        }
    }

    @Override
    public void onRaProgressIndicator(int type, String title, String progress, String badgeUrl) {
        if (mProgressContainer == null) return;

        if (type == RetroAchievementsManager.PROGRESS_HIDE) {
            mProgressContainer.setVisibility(View.GONE);
            return;
        }

        // SHOW or UPDATE — display only the progress numbers (e.g. "0/39"), not the title
        boolean hasProgress = progress != null && !progress.trim().isEmpty();
        if (!hasProgress) return;
        mProgressText.setText(progress);
        mProgressContainer.setVisibility(View.VISIBLE);

        if (badgeUrl != null && !badgeUrl.isEmpty()) {
            loadBitmapAsync(badgeUrl, bmp -> { if (bmp != null) mProgressIcon.setImageBitmap(bmp); });
        }
    }

    @Override
    public void onRaLeaderboardTracker(int type, int id, String display) {
        if (mTrackerContainer == null) return;
        float dp = getResources().getDisplayMetrics().density;
        if (type == RetroAchievementsManager.TRACKER_HIDE) {
            TextView tv = mLeaderboardTrackers.get(id);
            if (tv != null) {
                mTrackerContainer.removeView(tv);
                mLeaderboardTrackers.remove(id);
            }
            return;
        }

        if (type == RetroAchievementsManager.TRACKER_SHOW) {
            TextView tv = new TextView(this);
            tv.setTypeface(android.graphics.Typeface.MONOSPACE);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tv.setBackgroundColor(0xCC000000);
            int pad = (int)(8 * dp);
            tv.setPadding(pad, pad / 2, pad, pad / 2);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, (int)(2 * dp), 0, 0);
            tv.setLayoutParams(lp);
            tv.setText(display);
            mLeaderboardTrackers.put(id, tv);
            mTrackerContainer.addView(tv);
        } else { // TRACKER_UPDATE
            TextView tv = mLeaderboardTrackers.get(id);
            if (tv != null) tv.setText(display);
        }
    }

    @Override
    public void onRaGameLoaded(String title, int total, int earned, int unsupported, String gameBadgeUrl) {
        FrameLayout container = (FrameLayout) mDrawerLayout.getChildAt(0);
        if (container == null) return;

        float dp = getResources().getDisplayMetrics().density;
        int pad = (int)(12 * dp);
        int margin = (int)(16 * dp);
        int iconSize = (int)(48 * dp);

        // Horizontal container: [icon] [text block]
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(0xCC000000);
        row.setPadding(pad, pad, pad, pad);

        ImageView icon = new ImageView(this);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconLp.setMargins(0, 0, pad, 0);
        icon.setLayoutParams(iconLp);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        row.addView(icon);

        TextView tv = new TextView(this);
        String text = "RetroAchievements\n" + title + "\n" + earned + "/" + total + " achievements";
        if (unsupported > 0) text += "\n(" + unsupported + " won't trigger)";
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        row.addView(tv);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        lp.setMargins(margin, margin, 0, 0);
        row.setLayoutParams(lp);

        container.addView(row);

        if (gameBadgeUrl != null && !gameBadgeUrl.isEmpty()) {
            loadBitmapAsync(gameBadgeUrl, bmp -> { if (bmp != null) icon.setImageBitmap(bmp); });
        }

        row.postDelayed(() -> row.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction(() -> container.removeView(row))
                .start(), 5000);
    }

    @Override
    public void onRaAchievementTriggered(String title, String description, int points, String badgeUrl, boolean unofficial) {
        refreshAchievementsDialog();

        try {
            Uri notifUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone ringtone = RingtoneManager.getRingtone(this, notifUri);
            if (ringtone != null) ringtone.play();
        } catch (Exception ignored) {}

        FrameLayout container = (FrameLayout) mDrawerLayout.getChildAt(0);
        if (container == null) return;

        float dp = getResources().getDisplayMetrics().density;
        int pad = (int)(12 * dp);
        int margin = (int)(16 * dp);
        int iconSize = (int)(48 * dp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(0xCC000000);
        row.setPadding(pad, pad, pad, pad);

        ImageView icon = new ImageView(this);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconLp.setMargins(0, 0, pad, 0);
        icon.setLayoutParams(iconLp);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        row.addView(icon);

        TextView tv = new TextView(this);
        String header = unofficial ? "Unofficial Achievement Unlocked!" : "Achievement Unlocked!";
        tv.setText(header + "\n" + title + " (" + points + " pts)\n" + description);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        row.addView(tv);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        lp.setMargins(margin, margin, 0, 0);
        row.setLayoutParams(lp);

        container.addView(row);

        if (badgeUrl != null && !badgeUrl.isEmpty()) {
            loadBitmapAsync(badgeUrl, bmp -> { if (bmp != null) icon.setImageBitmap(bmp); });
        }

        row.postDelayed(() -> row.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction(() -> container.removeView(row))
                .start(), 5000);
    }

    @Override
    public void onRaGameCompleted(String title, boolean hardcore, String badgeUrl) {
        FrameLayout container = (FrameLayout) mDrawerLayout.getChildAt(0);
        if (container == null) return;

        float dp = getResources().getDisplayMetrics().density;
        int pad = (int)(12 * dp);
        int margin = (int)(16 * dp);
        int iconSize = (int)(64 * dp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(0xEE000000);
        row.setPadding(pad, pad, pad, pad);

        ImageView icon = new ImageView(this);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconLp.setMargins(0, 0, pad, 0);
        iconLp.gravity = Gravity.CENTER_VERTICAL;
        icon.setLayoutParams(iconLp);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        row.addView(icon);

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);

        TextView headerTv = new TextView(this);
        headerTv.setText(hardcore ? "Mastered!" : "Completed!");
        headerTv.setTextColor(0xFFFFD700);
        headerTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        textBlock.addView(headerTv);

        TextView titleTv = new TextView(this);
        titleTv.setText(title);
        titleTv.setTextColor(Color.WHITE);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
        textBlock.addView(titleTv);

        String username = RetroAchievementsManager.getUsername();
        if (username != null && !username.isEmpty()) {
            TextView userTv = new TextView(this);
            userTv.setText(username);
            userTv.setTextColor(0xFFCCCCCC);
            userTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            textBlock.addView(userTv);
        }

        row.addView(textBlock);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        lp.setMargins(margin, 0, margin, 0);
        row.setLayoutParams(lp);

        container.addView(row);

        if (badgeUrl != null && !badgeUrl.isEmpty()) {
            loadBitmapAsync(badgeUrl, bmp -> { if (bmp != null) icon.setImageBitmap(bmp); });
        }

        row.postDelayed(() -> row.animate()
                .alpha(0f)
                .setDuration(800)
                .withEndAction(() -> container.removeView(row))
                .start(), 6000);
    }

    private void loadBitmapAsync(String url, java.util.function.Consumer<Bitmap> callback) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "mupen64plus-ae/3.0 (Android)");
                conn.connect();
                Bitmap bmp = BitmapFactory.decodeStream(conn.getInputStream());
                runOnUiThread(() -> callback.accept(bmp));
            } catch (Exception e) {
                Log.w(TAG, "RA badge load failed: " + e.getMessage());
            }
        }, "RA-Badge").start();
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
        if (mCoreFragment.hasServiceStarted()) {
            mGameSurface.setSurfaceTexture(mCoreFragment.getSurfaceTexture());

            if (mDrawerLayout.isDrawerOpen(GravityCompat.START) || mDrawerOpenState) {
                mCoreFragment.pauseEmulator();
            } else {
                mCoreFragment.resumeEmulator();
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
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public void onCoreServiceStarted()
    {
        Log.i(TAG, "onCoreServiceStarted");

        if(mCoreFragment == null) return;

        Vibrator vibrator;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) this.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = manager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) this.getSystemService( Context.VIBRATOR_SERVICE );
        }

        if (vibrator != null) {
            mCoreFragment.registerVibrator(1, vibrator);
        }

        ReloadAllMenus();

        mDrawerLayout.closeDrawer(GravityCompat.START);
        mOverlay.requestFocus();
        mGameSurface.setSurfaceTexture(mCoreFragment.getSurfaceTexture());

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
    public void onNetplayReady()
    {
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
    }

    @Override
    public void onGameStarted()
    {
        // Set the screen orientation
        if (mGlobalPrefs.displayOrientation != -1) {
            setRequestedOrientation( mGlobalPrefs.displayOrientation );
        }
    }

    @Override
    public void onExitRequested(boolean shouldExit)
    {
        Log.i( TAG, "onExitRequested" );
        if(shouldExit)
        {
            shutdownEmulator();
        }
        else if( !mDrawerLayout.isDrawerOpen( GravityCompat.START ) && mCoreFragment != null)
        {
            mCoreFragment.resumeEmulator();
        }
    }

    public void finishActivity()
    {
        if(mCoreFragment != null)
        {
            mCoreFragment.setCoreEventListener(null);
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
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private void initControllers( View inputSource )
    {
        // By default, send Player 1 rumbles through phone vibrator
        Vibrator vibrator;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) this.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = manager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) this.getSystemService( Context.VIBRATOR_SERVICE );
        }

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
                   p.getDeadzone(), p.getSensitivityX(), p.getSensitivityY(), mGlobalPrefs.holdControllerBottons,
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
}
