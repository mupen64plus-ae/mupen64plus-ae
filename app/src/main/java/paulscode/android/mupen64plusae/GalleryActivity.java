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
package paulscode.android.mupen64plusae;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.PointerIcon;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import paulscode.android.mupen64plusae.GameSidebar.GameSidebarActionHandler;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog.PromptConfirmListener;
import paulscode.android.mupen64plusae.dialog.LocaleDialog;
import paulscode.android.mupen64plusae.dialog.Popups;
import paulscode.android.mupen64plusae.game.GameActivity;
import paulscode.android.mupen64plusae.jni.CoreService;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.task.ExtractAssetsOrCleanupTask;
import paulscode.android.mupen64plusae.task.GalleryRefreshTask;
import paulscode.android.mupen64plusae.task.GalleryRefreshTask.GalleryRefreshFinishedListener;
import paulscode.android.mupen64plusae.task.SyncProgramsJobService;
import paulscode.android.mupen64plusae.util.CountryCode;
import paulscode.android.mupen64plusae.util.DisplayWrapper;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;
import paulscode.android.mupen64plusae.util.Notifier;

public class GalleryActivity extends AppCompatActivity implements GameSidebarActionHandler, PromptConfirmListener,
        GalleryRefreshFinishedListener
{
    // Saved instance states
    private static final String STATE_QUERY = "STATE_QUERY";
    private static final String STATE_SIDEBAR = "STATE_SIDEBAR";
    private static final String STATE_FILE_TO_DELETE = "STATE_FILE_TO_DELETE";
    private static final String STATE_CACHE_ROM_INFO_FRAGMENT = "STATE_CACHE_ROM_INFO_FRAGMENT";
    private static final String STATE_GALLERY_REFRESH_NEEDED = "STATE_GALLERY_REFRESH_NEEDED";
    private static final String STATE_SCROLL_TO_POSITION = "STATE_SCROLL_TO_POSITION";
    private static final String STATE_LAUNCH_GAME_AFTER_SCAN = "STATE_LAUNCH_GAME_AFTER_SCAN";
    private static final String STATE_GAME_STARTED_EXTERNALLY = "STATE_GAME_STARTED_EXTERNALLY";
    private static final String STATE_REMOVE_FROM_LIBRARY_DIALOG = "STATE_REMOVE_FROM_LIBRARY_DIALOG";
    private static final String STATE_CLEAR_SHADERCACHE_DIALOG = "STATE_CLEAR_SHADERCACHE_DIALOG";
    private static final String STATE_LOCALE_DIALOG = "STATE_LOCALE_DIALOG";
    private static final String STATE_HARDWARE_INFO_POPUP = "STATE_HARDWARE_INFO_POPUP";
    private static final String STATE_SHOW_APP_VERSION_POPUP = "STATE_SHOW_APP_VERSION_POPUP";
    private static final String STATE_FAQ_POPUP = "STATE_FAQ_POPUP";
    public static final String KEY_IS_LEANBACK = "KEY_IS_LEANBACK";
    public static final String KEY_IS_SHORTCUT = "KEY_IS_SHORTCUT";

    public static final int REMOVE_FROM_LIBRARY_DIALOG_ID = 1;
    public static final int CLEAR_SHADER_CACHE_DIALOG_ID = 2;

    // App data and user preferences
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;

    // Widgets
    private RecyclerView mGridView;
    private DrawerLayout mDrawerLayout = null;
    private ActionBarDrawerToggle mDrawerToggle;
    private GameSidebar mDrawerList;
    private GameSidebar mGameSidebar;

    // Searching
    private SearchView mSearchView;
    private String mSearchQuery = "";

    // Resizable gallery thumbnails
    public int galleryWidth;
    public int galleryMaxWidth;
    public int galleryHalfSpacing;
    public int galleryColumns = 2;
    public float galleryAspectRatio;

    // Misc.
    private GalleryItem mSelectedItem = null;
    private boolean mDragging = false;

    private ScanRomsFragment mCacheRomInfoFragment = null;

    //If this is set to true, the gallery will be refreshed next time this activity is resumed
    boolean mRefreshNeeded = false;

    boolean mGameStartedExternally = false;

    String mPathToDelete = null;

    private int mCurrentVisiblePosition = 0;

    // Launch a game after searching is complete
    private String mLaunchGameAfterScan = "";
    private String mScanForGameOnResume = "";

    private ConfigFile mConfig;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    List<GalleryItem> mItemsCache = new ArrayList<>();
    List<GalleryItem> mAllItems = new ArrayList<>();
    List<GalleryItem> mRecentItemsCache = new ArrayList<>();

    ActivityResultLauncher<Intent> mLaunchGame = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Call this here as well since onActivityResult happens before onResume
                    createSearchMenu();

                    mSearchQuery = "";

                    if (mSearchView != null) {
                        mSearchView.setQuery( mSearchQuery, true );
                    }

                    if( mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
                    {
                        mDrawerLayout.closeDrawer( GravityCompat.START );
                    }

                    if(mGameStartedExternally)
                    {
                        finishAffinity();
                    }
                }
            });

    ActivityResultLauncher<Intent> mLaunchScanRoms = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && data != null) {
                    // Call this here as well since onActivityResult happens before onResume
                    createSearchMenu();

                    final Bundle extras = data.getExtras();

                    if (extras != null) {
                        final String searchUri = extras.getString( ActivityHelper.Keys.SEARCH_PATH );
                        final boolean searchZips = extras.getBoolean( ActivityHelper.Keys.SEARCH_ZIPS );
                        final boolean downloadArt = extras.getBoolean( ActivityHelper.Keys.DOWNLOAD_ART );
                        final boolean clearGallery = extras.getBoolean( ActivityHelper.Keys.CLEAR_GALLERY );
                        final boolean searchSubdirectories = extras.getBoolean( ActivityHelper.Keys.SEARCH_SUBDIR );
                        final boolean searchSingleFile = extras.getBoolean( ActivityHelper.Keys.SEARCH_SINGLE_FILE );

                        if (searchUri != null)
                        {
                            refreshRoms(searchUri, searchZips, downloadArt, clearGallery, searchSubdirectories, searchSingleFile);
                        }
                    }
                }
            });

    private void loadGameFromExtras( Bundle extras) {

        Intent intent = new Intent(CoreService.SERVICE_EVENT);
        if (extras != null) {

            // You can also include some extra data.
            intent.putExtra(CoreService.SERVICE_QUIT, true);
            sendBroadcast(intent);

            int currentAttempt = 0;
            while (ActivityHelper.isServiceRunning(this, ActivityHelper.coreServiceProcessName) &&
                    currentAttempt++ < 100) {
                Log.i("GalleryActivity", "Waiting on pevious instance to exit");

                // Sleep for 10 ms to prevent a tight loop
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (!extras.getBoolean(KEY_IS_LEANBACK)) {
                Log.i("GalleryActivity", "Loading ROM from other app");
                final String givenRomPath = extras.getString( ActivityHelper.Keys.ROM_PATH );

                if( !TextUtils.isEmpty( givenRomPath ) ) {
                    getIntent().replaceExtras((Bundle)null);

                    launchGameOnCreation(givenRomPath, true);
                }
            } else {

                mGameStartedExternally = true;
                
                Log.i("GalleryActivity", "Loading ROM from leanback");
                String romPath = extras.getString(ActivityHelper.Keys.ROM_PATH );
                String zipPath = extras.getString(ActivityHelper.Keys.ZIP_PATH );
                String md5 = extras.getString(ActivityHelper.Keys.ROM_MD5);
                String crc = extras.getString(ActivityHelper.Keys.ROM_CRC);
                String headerName = extras.getString(ActivityHelper.Keys.ROM_HEADER_NAME);

                byte countryCode;
                if (extras.getBoolean(KEY_IS_SHORTCUT)) {
                    countryCode = (byte)extras.getInt(ActivityHelper.Keys.ROM_COUNTRY_CODE);
                } else {
                    countryCode = extras.getByte(ActivityHelper.Keys.ROM_COUNTRY_CODE);
                }

                String artPath = extras.getString(ActivityHelper.Keys.ROM_ART_PATH);
                String goodName = extras.getString(ActivityHelper.Keys.ROM_GOOD_NAME);
                String displayName = extras.getString(ActivityHelper.Keys.ROM_DISPLAY_NAME);

                if (displayName == null) {
                    displayName = goodName;
                }

                launchGameActivity( romPath, zipPath,  md5, crc, headerName, countryCode, artPath, goodName, displayName, true,
                        false, false);
                getIntent().replaceExtras((Bundle)null);
            }
        } else {
            // You can also include some extra data.
            intent.putExtra(CoreService.SERVICE_RESUME, true);
            sendBroadcast(intent);
        }
    }

    @Override
    protected void onNewIntent( Intent intent )
    {
        Log.i("GalleryActivity", "onNewIntent");

        // If the activity is already running and is launched again (e.g. from a file manager app),
        // the existing instance will be reused rather than a new one created. This behavior is
        // specified in the manifest (launchMode = singleTask). In that situation, any activities
        // above this on the stack (e.g. GameActivity, GamePrefsActivity) will be destroyed
        // gracefully and onNewIntent() will be called on this instance. onCreate() will NOT be
        // called again on this instance.
        super.onNewIntent( intent );

        // Only remember the last intent used
        setIntent( intent );

        // Get the ROM path if it was passed from another activity/app
        if (getIntent() != null)
        {
            boolean launchedFromHistory = (getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0;
            if (!launchedFromHistory) {
                final Bundle extras = getIntent().getExtras();
                loadGameFromExtras(extras);
            }
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        if(TextUtils.isEmpty(LocaleContextWrapper.getLocalCode()))
        {
            super.attachBaseContext(newBase);
        }
        else
        {
            super.attachBaseContext(LocaleContextWrapper.wrap(newBase,LocaleContextWrapper.getLocalCode()));
        }
    }

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        Log.i("GalleryActivity", "onCreate");

        super.onCreate( savedInstanceState );

        if( savedInstanceState != null )
        {
            mSelectedItem = null;
            final String sideBarMd5 = savedInstanceState.getString( STATE_SIDEBAR );
            if( sideBarMd5 != null )
            {
                mSelectedItem = new GalleryItem(this, sideBarMd5, null, null,
                        CountryCode.DEMO, null, null, null, null, null, 0, 0.0f);
            }

            final String query = savedInstanceState.getString( STATE_QUERY );
            if( query != null )
                mSearchQuery = query;

            mPathToDelete = savedInstanceState.getString( STATE_FILE_TO_DELETE );
            mRefreshNeeded = savedInstanceState.getBoolean(STATE_GALLERY_REFRESH_NEEDED);
            mGameStartedExternally = savedInstanceState.getBoolean(STATE_GAME_STARTED_EXTERNALLY);
            mCurrentVisiblePosition = savedInstanceState.getInt(STATE_SCROLL_TO_POSITION);
            mLaunchGameAfterScan = savedInstanceState.getString(STATE_LAUNCH_GAME_AFTER_SCAN);
        }

        // Get app data and user preferences
        mAppData = new AppData( this );
        mGlobalPrefs = new GlobalPrefs( this, mAppData );
        mConfig = new ConfigFile(mGlobalPrefs.romInfoCacheCfg);

        // Lay out the content
        setContentView( R.layout.gallery_activity );
        mGridView = findViewById( R.id.gridview );

        FloatingActionButton floatingActionButton = findViewById(R.id.menuItem_refreshRoms);

        if (floatingActionButton != null) {
            mGridView.addOnScrollListener(
                    new RecyclerView.OnScrollListener()
                    {
                        @Override
                        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState)
                        {
                            super.onScrollStateChanged(recyclerView, newState);

                            if(newState == RecyclerView.SCROLL_STATE_IDLE)
                            {
                                floatingActionButton.show();
                            }
                            else
                            {
                                floatingActionButton.hide();
                            }
                        }

                        @Override
                        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy)
                        {
                            super.onScrolled(recyclerView, dx, dy);
                        }
                    }
            );
        }

        // Add the toolbar to the activity (which supports the fancy menu/arrow animation)
        final Toolbar toolbar = findViewById( R.id.toolbar );
        toolbar.setTitle( R.string.app_name );
        final View firstGridChild = mGridView.getChildAt(0);

        if(firstGridChild != null)
        {
            toolbar.setNextFocusDownId(firstGridChild.getId());
        }

        setSupportActionBar( toolbar );

        // Configure the navigation drawer
        mDrawerLayout = findViewById( R.id.drawerLayout );
        mDrawerToggle = new ActionBarDrawerToggle( this, mDrawerLayout, toolbar, 0, 0 )
        {
            @Override
            public void onDrawerStateChanged( int newState )
            {
                // Intercepting the drawer open animation and re-closing it causes onDrawerClosed to
                // not fire,
                // So detect when this happens and wait until the drawer closes to handle it
                // manually
                if( newState == DrawerLayout.STATE_DRAGGING )
                {
                    // INTERCEPTED!
                    mDragging = true;
                    hideSoftKeyboard();
                }
                else if( newState == DrawerLayout.STATE_IDLE )
                {
                    if( mDragging && !mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
                    {
                        // onDrawerClosed from dragging it
                        mDragging = false;
                        mDrawerList.setVisibility( View.VISIBLE );
                        mGameSidebar.setVisibility( View.GONE );
                        mSelectedItem = null;
                    }
                }
            }

            @Override
            public void onDrawerClosed( View drawerView )
            {
                // Hide the game information sidebar
                mDrawerList.setVisibility( View.VISIBLE );
                mGameSidebar.setVisibility( View.GONE );
                mGridView.requestFocus();

                if(mGridView.getAdapter() != null && mGridView.getAdapter().getItemCount() != 0)
                {
                    mGridView.getAdapter().notifyItemChanged(0);
                }

                mSelectedItem = null;

                super.onDrawerClosed( drawerView );
            }

            @Override
            public void onDrawerOpened( View drawerView )
            {
                hideSoftKeyboard();
                super.onDrawerOpened( drawerView );

                mDrawerList.requestFocus();
                mDrawerList.setSelection(0);
            }
        };

        mDrawerToggle.setDrawerIndicatorEnabled(false);

        mDrawerLayout.addDrawerListener( mDrawerToggle );

        // Configure the list in the navigation drawer
        mDrawerList = findViewById( R.id.drawerNavigation );
        mDrawerList.setMenuResource( R.menu.gallery_drawer );

        // Set up the header image in the navigation drawer
        mDrawerList.setImage(R.drawable.ouya_icon);
        mDrawerList.hideTitle();

        //Remove touch screen profile configuration if in TV mode
        if(mGlobalPrefs.isBigScreenMode)
        {
            final MenuItem profileGroupItem = mDrawerList.getMenu().findItem(R.id.menuItem_profiles);
            profileGroupItem.getSubMenu().removeItem(R.id.menuItem_touchscreenProfiles);
            
            final MenuItem settingsGroupItem = mDrawerList.getMenu().findItem(R.id.menuItem_settings);
            settingsGroupItem.getSubMenu().removeItem(R.id.menuItem_categoryTouchscreen);
        }

        // Select the Library section
        mDrawerList.getMenu().getItem( 0 ).setChecked( true );

        // Handle menu item selections
        mDrawerList.setOnClickListener(GalleryActivity.this::onOptionsItemSelected);

        mDrawerList.requestFocus();

        // Configure the game information drawer
        mGameSidebar = findViewById( R.id.gameSidebar );

        // Handle events from the side bar
        mGameSidebar.setActionHandler(this, R.menu.gallery_game_drawer);

        // find the retained fragment on activity restarts
        final FragmentManager fm = getSupportFragmentManager();
        mCacheRomInfoFragment = (ScanRomsFragment) fm.findFragmentByTag(STATE_CACHE_ROM_INFO_FRAGMENT);

        if(mCacheRomInfoFragment == null)
        {
            mCacheRomInfoFragment = new ScanRomsFragment();
            fm.beginTransaction().add(mCacheRomInfoFragment, STATE_CACHE_ROM_INFO_FRAGMENT).commit();
        }

        // Don't call the async version otherwise the scroll position is lost
        refreshGrid();

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT )
        {
            DisplayWrapper.drawBehindSystemBars(this);
        }

        CoordinatorLayout coordLayout = findViewById(R.id.coordLayout);

        ViewCompat.setOnApplyWindowInsetsListener(coordLayout, (v, insets) -> {

            Resources r = getResources();
            int margin = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    24,
                    r.getDisplayMetrics()
            );

            if (floatingActionButton != null) {
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)floatingActionButton.getLayoutParams();
                params.bottomMargin = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom + margin;
                floatingActionButton.setLayoutParams(params);
            }

            DrawerLayout.LayoutParams params = (DrawerLayout.LayoutParams)coordLayout.getLayoutParams();
            params.topMargin = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            params.rightMargin = insets.getInsets(WindowInsetsCompat.Type.systemBars()).right;
            coordLayout.setLayoutParams(params);

            return insets;
        });

        mDrawerLayout.setOnHoverListener((v, event) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mHandler.postDelayed(() -> v.setPointerIcon(PointerIcon.getSystemIcon(GalleryActivity.this, PointerIcon.TYPE_ARROW)), 100);
            }
            return false;
        });

        mDrawerLayout.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int oldWidth = oldRight - oldLeft;
            int oldHeight = oldBottom - oldTop;
            if( v.getWidth() != oldWidth || v.getHeight() != oldHeight )
            {
                refreshGrid(mItemsCache, mRecentItemsCache);
            }
        });

        // Get the ROM path if it was passed from another activity/app
        if (getIntent() != null)
        {
            boolean launchedFromHistory = (getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0;
            if (!launchedFromHistory) {
                final Bundle extras = getIntent().getExtras();
                loadGameFromExtras(extras);
            }
        }

        if(ActivityHelper.isServiceRunning(this, ActivityHelper.coreServiceProcessName)) {
            Log.i("GalleryActivity", "CoreService is running");
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if( mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
                {
                    mDrawerLayout.closeDrawer( GravityCompat.START );
                }
                else if(mSearchView != null && !TextUtils.isEmpty(mSearchQuery)) {
                    mSearchQuery = "";
                    mSearchView.setQuery( mSearchQuery, true );
                }
            }
        });

    }

    @Override
    public void onPause() {
        Log.i("GalleryActivity", "onPause");

        super.onPause();

        GridLayoutManager layoutManager = (GridLayoutManager)mGridView.getLayoutManager();
        if (layoutManager != null) {
            mCurrentVisiblePosition = ((GridLayoutManager)mGridView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
        }
    }

    @Override
    public void onResume()
    {
        Log.i("GalleryActivity", "onResume");

        super.onResume();

        //mRefreshNeeded will be set to true whenever a game is launched
        if(mRefreshNeeded)
        {
            mRefreshNeeded = false;
            reloadCacheAndRefreshGrid();

            mGameSidebar.setVisibility( View.GONE );
            mDrawerList.setVisibility( View.VISIBLE );
        }

        // This is called here rather than onCreate otherwise onQueryTextChange is called on creation
        createSearchMenu();

        // A game that did not exist in the gallery was requested to be searched for
        if (!TextUtils.isEmpty(mScanForGameOnResume)) {
            mCacheRomInfoFragment.refreshRoms(mScanForGameOnResume, true, true, false, false,
                    true, mAppData, mGlobalPrefs);
        }
    }

    @Override
    public void onSaveInstanceState( @NonNull Bundle savedInstanceState )
    {
        Log.i("GalleryActivity", "onSaveInstanceState");

        if( mSearchView != null )
            savedInstanceState.putString( STATE_QUERY, mSearchView.getQuery().toString() );
        if( mSelectedItem != null )
            savedInstanceState.putString( STATE_SIDEBAR, mSelectedItem.md5 );
        savedInstanceState.putBoolean(STATE_GALLERY_REFRESH_NEEDED, mRefreshNeeded);
        savedInstanceState.putBoolean(STATE_GAME_STARTED_EXTERNALLY, mGameStartedExternally);
        savedInstanceState.putString(STATE_FILE_TO_DELETE, mPathToDelete);
        savedInstanceState.putInt(STATE_SCROLL_TO_POSITION, mCurrentVisiblePosition);
        savedInstanceState.putString(STATE_LAUNCH_GAME_AFTER_SCAN, mLaunchGameAfterScan);

        super.onSaveInstanceState( savedInstanceState );
    }

    private void tagForRefreshNeeded()
    {
        mRefreshNeeded = true;
        GridLayoutManager layoutManager = (GridLayoutManager)mGridView.getLayoutManager();
        if (layoutManager != null) {
            mCurrentVisiblePosition = ((GridLayoutManager)mGridView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
        }
    }

    public void hideSoftKeyboard()
    {
        // Hide the soft keyboard if needed
        if( mSearchView == null )
            return;

        final InputMethodManager imm = (InputMethodManager) getSystemService( Context.INPUT_METHOD_SERVICE );

        if (imm != null) {
            imm.hideSoftInputFromWindow( mSearchView.getWindowToken(), 0 );
        }
    }

    @Override
    protected void onPostCreate( Bundle savedInstanceState )
    {
        super.onPostCreate( savedInstanceState );
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged( @NonNull Configuration newConfig )
    {
        super.onConfigurationChanged( newConfig );
        mDrawerToggle.onConfigurationChanged( newConfig );
    }

    public void createSearchMenu()
    {
        mSearchView = findViewById(R.id.menuItem_search);
        mSearchView.setOnQueryTextListener( new OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit( String query )
            {
                return false;
            }

            @Override
            public boolean onQueryTextChange( String query )
            {
                if (!mSearchView.isIconified()) {
                    mSearchQuery = query;
                    refreshGridAsync();
                }

                return false;
            }
        } );

        if( !"".equals( mSearchQuery ) )
        {
            mSearchView.setQuery( mSearchQuery, true );
        }

        mSearchView.setOnQueryTextFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                showInputMethod(view.findFocus());
            }
        });
    }

    private void showInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, 0);
        }
    }

    private void launchGameOnCreation(String givenRomPath, boolean scanOnFailure)
    {
        if (givenRomPath == null) {
            return;
        }

        Log.i("GalleryActivity", "Rom path = " + givenRomPath);

        boolean isUri = !new File(givenRomPath).exists();

        mGameStartedExternally = true;

        Uri romPathUri;

        if (isUri) {
            romPathUri = Uri.parse(givenRomPath);
        } else {
            romPathUri = Uri.fromFile(new File(givenRomPath));
        }

        // Check if we have a cache of this game first
        GalleryItem foundItem = null;
        for (GalleryItem item : mAllItems) {

            try {
                String decodedPath = URLDecoder.decode(romPathUri.toString(), "UTF-8");
                String decodedItemZip = item.zipUri != null ? URLDecoder.decode(item.zipUri, "UTF-8") : null;
                String decodedItemRom = item.romUri != null ? URLDecoder.decode(item.romUri, "UTF-8") : null;

                if ((decodedItemZip != null && decodedItemZip.equals(decodedPath)) ||
                        (decodedItemRom != null && decodedItemRom.equals(decodedPath)))
                {
                    foundItem = item;
                    break;
                }

            } catch (UnsupportedEncodingException|java.lang.IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        // We found the item, use the pre-existing one
        if (foundItem != null)
        {
            launchGameActivity(foundItem.romUri,
                    foundItem.zipUri,
                    foundItem.md5, foundItem.crc,
                    foundItem.headerName, foundItem.countryCode.getValue(), foundItem.artPath,
                    foundItem.goodName, foundItem.displayName, true,
                    false, false);
            finishAffinity();
        } else if (scanOnFailure){
            // We want to launch the game after scan completes
            mLaunchGameAfterScan = givenRomPath;
            mScanForGameOnResume = romPathUri.toString();
        }
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        if (item.getItemId() == R.id.menuItem_refreshRoms) {
            Intent intent = new Intent(this, ScanRomsActivity.class);
            mLaunchScanRoms.launch(intent);
            return true;
        } else if (item.getItemId() == R.id.menuItem_categoryLibrary) {
            tagForRefreshNeeded();
            ActivityHelper.startLibraryPrefsActivity(this);
            return true;
        } else if (item.getItemId() == R.id.menuItem_categoryDisplay) {
            tagForRefreshNeeded();
            ActivityHelper.startDisplayPrefsActivity( this );
            return true;
        } else if (item.getItemId() == R.id.menuItem_categoryShaders) {
            tagForRefreshNeeded();
            ActivityHelper.startShadersPrefsActivity( this );
            return true;
        } else if (item.getItemId() == R.id.menuItem_categoryAudio) {
            ActivityHelper.startAudioPrefsActivity( this );
            return true;
        } else if (item.getItemId() == R.id.menuItem_categoryTouchscreen) {
            tagForRefreshNeeded();
            ActivityHelper.startTouchscreenPrefsActivity( this );
            return true;
        } else if (item.getItemId() == R.id.menuItem_categoryInput) {
            ActivityHelper.startInputPrefsActivity( this );
            return true;
        } else if (item.getItemId() == R.id.menuItem_categoryData) {
            tagForRefreshNeeded();
            ActivityHelper.startDataPrefsActivity( this );
            return true;
        } else if (item.getItemId() == R.id.menuItem_categoryNetplay) {
            ActivityHelper.startNetplayPrefsActivity( this );
            return true;
         } else if (item.getItemId() == R.id.menuItem_categoryDefaults) {
            ActivityHelper.startDefaultPrefsActivity( this );
            return true;
        } else if (item.getItemId() == R.id.menuItem_emulationProfiles) {
            ActivityHelper.startManageEmulationProfilesActivity(this);
            return true;
        } else if (item.getItemId() == R.id.menuItem_touchscreenProfiles) {
            ActivityHelper.startManageTouchscreenProfilesActivity(this);
            return true;
        } else if (item.getItemId() == R.id.menuItem_controllerProfiles) {
            ActivityHelper.startManageControllerProfilesActivity(this);
            return true;
        } else if (item.getItemId() == R.id.menuItem_faq) {
            final Popups pop = Popups.newInstance(this, STATE_FAQ_POPUP);
            final FragmentManager fm = getSupportFragmentManager();
            pop.show(fm, STATE_FAQ_POPUP);
            return true;
        } else if (item.getItemId() == R.id.menuItem_helpForum) {
            ActivityHelper.launchUri(this, R.string.uri_forum);
            return true;
        } else if (item.getItemId() == R.id.menuItem_controllerDiagnostics) {
            ActivityHelper.startDiagnosticActivity(this);
            return true;
        } else if (item.getItemId() == R.id.menuItem_reportBug) {
            ActivityHelper.launchUri(this, R.string.uri_bugReport);
            return true;
        } else if (item.getItemId() == R.id.menuItem_appVersion) {
            final Popups pop = Popups.newInstance(this, STATE_SHOW_APP_VERSION_POPUP);
            final FragmentManager fm = getSupportFragmentManager();
            pop.show(fm, STATE_SHOW_APP_VERSION_POPUP);
            return true;
        } else if (item.getItemId() == R.id.menuItem_logcat) {
            ActivityHelper.startLogcatActivity(this);
            return true;
        } else if (item.getItemId() == R.id.menuItem_hardwareInfo) {
            final Popups pop = Popups.newInstance(this, STATE_HARDWARE_INFO_POPUP);
            final FragmentManager fm = getSupportFragmentManager();
            pop.show(fm, STATE_HARDWARE_INFO_POPUP);
            return true;
        } else if (item.getItemId() == R.id.menuItem_credits) {
            ActivityHelper.launchUri(GalleryActivity.this, R.string.uri_credits);
            return true;
        } else if (item.getItemId() == R.id.menuItem_localeOverride) {
            final CharSequence title = getText( R.string.menuItem_localeOverride );
            final LocaleDialog localeDialog = LocaleDialog.newInstance(title.toString());
            final FragmentManager fm = getSupportFragmentManager();
            localeDialog.show(fm, STATE_LOCALE_DIALOG);
            return true;
        } else if (item.getItemId() == R.id.menuItem_extract) {
            ActivityHelper.starExtractTextureActivity(this);
            return true;
        } else if (item.getItemId() == R.id.menuItem_clear) {
            ActivityHelper.startDeleteTextureActivity(this);
            return true;
        } else if (item.getItemId() == R.id.menuItem_clearShaderCache) {
            final CharSequence title = getText( R.string.confirm_title );
            final CharSequence message = getText( R.string.menuItem_ConfirmationClearShaderCache );

            final ConfirmationDialog confirmationDialog =
                    ConfirmationDialog.newInstance(CLEAR_SHADER_CACHE_DIALOG_ID, title.toString(), message.toString());

            final FragmentManager fm = getSupportFragmentManager();
            confirmationDialog.show(fm, STATE_CLEAR_SHADERCACHE_DIALOG);
            return true;
        } else if (item.getItemId() == R.id.menuItem_importExportData) {
            ActivityHelper.startImportExportActivity(this);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void createGameShortcut(GalleryItem item)
    {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {

            Intent gameIntent = new Intent(this, SplashActivity.class);
            gameIntent.putExtra(GalleryActivity.KEY_IS_LEANBACK, true);
            gameIntent.putExtra(GalleryActivity.KEY_IS_SHORTCUT, true);
            gameIntent.putExtra(ActivityHelper.Keys.ROM_PATH, item.romUri);
            gameIntent.putExtra(ActivityHelper.Keys.ZIP_PATH, item.zipUri);
            gameIntent.putExtra(ActivityHelper.Keys.ROM_MD5, item.md5);
            gameIntent.putExtra(ActivityHelper.Keys.ROM_CRC, item.crc);
            gameIntent.putExtra(ActivityHelper.Keys.ROM_HEADER_NAME, item.headerName);
            gameIntent.putExtra(ActivityHelper.Keys.ROM_COUNTRY_CODE, (int) item.countryCode.getValue());
            gameIntent.putExtra(ActivityHelper.Keys.ROM_ART_PATH, item.artPath);
            gameIntent.putExtra(ActivityHelper.Keys.ROM_GOOD_NAME, item.goodName);
            gameIntent.putExtra(ActivityHelper.Keys.ROM_DISPLAY_NAME, item.displayName);
            gameIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            gameIntent.setAction("LOCATION_SHORTCUT");

            Bitmap bitmap;
            if (item.artBitmap != null) {
                bitmap = item.artBitmap.getBitmap();
            } else {
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_coverart);
            }

            if (bitmap != null) {
                int dimension = Math.min(bitmap.getWidth(), bitmap.getHeight());
                Bitmap croppedBitmap = ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension);

                IconCompat icon = IconCompat.createWithBitmap(croppedBitmap);
                ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(this, item.md5)
                        .setIcon(icon)
                        .setIntent(gameIntent)
                        .setShortLabel(item.displayName)
                        .build();

                try {
                    ShortcutManagerCompat.requestPinShortcut(this, shortcut, null);
                } catch (java.lang.IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onGameSidebarAction(MenuItem menuItem)
    {
        final GalleryItem item = mSelectedItem;
        if( item == null || item.romUri == null)
            return;

        if (menuItem.getItemId() == R.id.menuItem_resume) {
            launchGameActivity( item.romUri,
                    item.zipUri,
                    item.md5, item.crc, item.headerName,
                    item.countryCode.getValue(), item.artPath, item.goodName, item.displayName, false,
                    false, false);
        } else if (menuItem.getItemId() == R.id.menuItem_start) {
            launchGameActivity(item.romUri,
                    item.zipUri,
                    item.md5, item.crc,
                    item.headerName, item.countryCode.getValue(), item.artPath,
                    item.goodName, item.displayName, true,
                    false, false);
        } else if (menuItem.getItemId() == R.id.menuItem_connectNetplayServer) {
            launchGameActivity(item.romUri,
                    item.zipUri,
                    item.md5, item.crc,
                    item.headerName, item.countryCode.getValue(), item.artPath,
                    item.goodName, item.displayName, true,
                    true, false);
        } else if (menuItem.getItemId() == R.id.menuItem_startNetplayServer) {
            launchGameActivity(item.romUri,
                    item.zipUri,
                    item.md5, item.crc,
                    item.headerName, item.countryCode.getValue(), item.artPath,
                    item.goodName, item.displayName, true,
                    true, true);
        } else if (menuItem.getItemId() == R.id.menuItem_settings) {
            tagForRefreshNeeded();
            ActivityHelper.startGamePrefsActivity( GalleryActivity.this, item.romUri,
                    item.md5, item.crc, item.headerName, item.goodName, item.displayName, item.countryCode.getValue());
        } else if (menuItem.getItemId() == R.id.menuItem_remove) {
            final CharSequence title = getText( R.string.confirm_title );
            final CharSequence message = getText( R.string.confirmRemoveFromLibrary_message );

            final ConfirmationDialog confirmationDialog =
                    ConfirmationDialog.newInstance(REMOVE_FROM_LIBRARY_DIALOG_ID, title.toString(), message.toString());

            final FragmentManager fm = getSupportFragmentManager();
            confirmationDialog.show(fm, STATE_REMOVE_FROM_LIBRARY_DIALOG);
        } else if (menuItem.getItemId() == R.id.menuItem_createShortcut) {
            createGameShortcut(item);
        }
    }

    @Override
    public void onPromptDialogClosed(int id, int which)
    {
        Log.i( "GalleryActivity", "onPromptDialogClosed" );

        if( which == DialogInterface.BUTTON_POSITIVE )
        {
            if(id == REMOVE_FROM_LIBRARY_DIALOG_ID && mSelectedItem != null)
            {
                mConfig.remove(mSelectedItem.md5);
                mConfig.save();
                mDrawerLayout.closeDrawer( GravityCompat.START, false );
                refreshGridAsync();
            }

            if (id == CLEAR_SHADER_CACHE_DIALOG_ID) {
                FileUtil.deleteFolder(new File(mGlobalPrefs.shaderCacheDir));
            }
        }
    }

    public void onGalleryItemClick(GalleryItem item)
    {
        mSelectedItem = item;

        // Show the game info sidebar
        mDrawerList.setVisibility(View.GONE);
        mGameSidebar.setVisibility(View.VISIBLE);
        mGameSidebar.scrollTo(0, 0);

        // Check if valid image
        if (FileUtil.isFileImage(new File(item.artPath))) {
            // Set the cover art in the sidebar
            item.loadBitmap(this);
            mGameSidebar.setImage(item.artBitmap);
        } else {
            mGameSidebar.setImage(null);
        }

        // Set the game title
        mGameSidebar.setTitle(item.displayName);


        // Restore the menu
        mGameSidebar.setActionHandler(GalleryActivity.this, R.menu.gallery_game_drawer);

        if (!AppData.IS_OREO || mAppData.isAndroidTv)
        {
            mGameSidebar.getMenu().removeItem(R.id.menuItem_createShortcut);
            mGameSidebar.reload();
        }

        // Open the navigation drawer
        mDrawerLayout.openDrawer(GravityCompat.START);

        mGameSidebar.requestFocus();
        mGameSidebar.setSelection(0);
    }

    public boolean onGalleryItemLongClick( GalleryItem item )
    {
        if (item.romUri == null) {
            return false;
        }

        launchGameActivity( item.romUri, item.zipUri,
            item.md5, item.crc, item.headerName, item.countryCode.getValue(),
            item.artPath, item.goodName, item.displayName, false, false, false );
        return true;
    }

    private void refreshRoms(final String searchUri, boolean searchZips, boolean downloadArt, boolean clearGallery, boolean searchSubdirectories,
                             boolean searchSingleFile)
    {
        // Don't let the activity sleep in the middle of scan
        getWindow().setFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );

        mCacheRomInfoFragment.refreshRoms(searchUri, searchZips, downloadArt, clearGallery, searchSubdirectories,
                searchSingleFile, mAppData, mGlobalPrefs);
    }

    void refreshGrid()
    {
        Log.i("GalleryActivity", "refreshGrid");

        //Reload global prefs
        mAppData = new AppData( this );
        mGlobalPrefs = new GlobalPrefs( this, mAppData );

        GalleryRefreshTask galleryRefreshTask = new GalleryRefreshTask(this, this, mGlobalPrefs, mSearchQuery, mConfig);
        
        galleryRefreshTask.generateGridItemsAndSaveConfig(mItemsCache, mAllItems, mRecentItemsCache);
        refreshGrid(mItemsCache, mRecentItemsCache);

        SyncProgramsJobService.syncProgramsForChannel(this, mAppData.getChannelId());
    }

    void refreshGridAsync()
    {
        Log.i("GalleryActivity", "refreshGridAsync");

        //Reload global prefs
        mAppData = new AppData( this );
        mGlobalPrefs = new GlobalPrefs( this, mAppData );

        GalleryRefreshTask galleryRefreshTask = new GalleryRefreshTask(this, this, mGlobalPrefs, mSearchQuery, mConfig);
        galleryRefreshTask.doInBackground();

        SyncProgramsJobService.syncProgramsForChannel(this, mAppData.getChannelId());
    }

    void reloadCacheAndRefreshGrid()
    {
        // This is called once ROM scan is finished, so no longer require the screen to remain on at this point
        getWindow().setFlags( 0, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );

        mConfig = new ConfigFile(mGlobalPrefs.romInfoCacheCfg);

        refreshGridAsync();
    }

    @Override
    public void onGalleryRefreshFinished(List<GalleryItem> items, List<GalleryItem> allItems, List<GalleryItem> recentItems)
    {
        mItemsCache = items;
        mAllItems = allItems;
        mRecentItemsCache = recentItems;
        runOnUiThread(() -> refreshGrid(mItemsCache, mRecentItemsCache));
    }

    synchronized void refreshGrid(List<GalleryItem> items, List<GalleryItem> recentItems)
    {
        if( mGlobalPrefs.isRecentShown && TextUtils.isEmpty(mSearchQuery) && recentItems.size() > 0 )
        {
            List<GalleryItem> combinedItems = new ArrayList<>();

            combinedItems.add( new GalleryItem( this, getString( R.string.galleryRecentlyPlayed ) ) );
            combinedItems.addAll( recentItems );

            combinedItems.add( new GalleryItem( this, getString( R.string.galleryLibrary ) ) );
            combinedItems.addAll( items );

            items = combinedItems;
        }

        // Allow the headings to take up the entire width of the layout
        final List<GalleryItem> finalItems = items;
        final GridLayoutManager layoutManager = new GridLayoutManager( this, galleryColumns );
        layoutManager.setSpanSizeLookup( new GridLayoutManager.SpanSizeLookup()
        {
            @Override
            public int getSpanSize( int position )
            {
                // Headings will take up every span (column) in the grid
                if( finalItems.get( position ).isHeading )
                    return galleryColumns;

                // Games will fit in a single column
                return 1;
            }
        } );

        mGridView.setLayoutManager( layoutManager );

        // Update the grid layout
        galleryMaxWidth = (int) (getResources().getDimension( R.dimen.galleryImageWidth ) * mGlobalPrefs.coverArtScale);
        galleryHalfSpacing = (int) getResources().getDimension( R.dimen.galleryHalfSpacing );
        galleryAspectRatio = galleryMaxWidth * 1.0f
                / getResources().getDimension( R.dimen.galleryImageHeight )/mGlobalPrefs.coverArtScale;

        int widthPixels = mDrawerLayout.getWidth();

        int width = widthPixels - galleryHalfSpacing * 2;
        width = Math.max(width, galleryHalfSpacing*4);
        galleryColumns = (int) Math
                .ceil( width * 1.0 / ( galleryMaxWidth + galleryHalfSpacing * 2 ) );
        galleryWidth = width / galleryColumns - galleryHalfSpacing * 2;

        layoutManager.setSpanCount( galleryColumns );

        mGridView.setFocusable(false);
        mGridView.setFocusableInTouchMode(false);

        List<GalleryItem> galleryItems = items;
        mGridView.setAdapter( new GalleryItem.Adapter( this, items ) );

        if (mSelectedItem != null) {
            // Repopulate the game sidebar
            for (final GalleryItem item : galleryItems) {
                if (mSelectedItem.md5.equals( item.md5 )) {
                    onGalleryItemClick( item );
                    break;
                }
            }
        }

        if (mGridView.getAdapter() != null) {
            mGridView.getAdapter().notifyDataSetChanged();
        }

        if(galleryItems.size() > 0) {
            findViewById(R.id.gallery_empty_icon).setVisibility(View.INVISIBLE);
        } else {
            findViewById(R.id.gallery_empty_icon).setVisibility(View.VISIBLE);
        }

        if (mGridView.getLayoutManager() != null) {
            mGridView.getLayoutManager().scrollToPosition(mCurrentVisiblePosition);
        }
        mCurrentVisiblePosition = 0;

        // We were asked to launch a game after a scan completes, so do it here
        if (!TextUtils.isEmpty(mLaunchGameAfterScan)) {
            launchGameOnCreation(mLaunchGameAfterScan, false);
        }
    }

    public void launchGameActivity( String romPath, String zipPath, String romMd5, String romCrc,
            String romHeaderName, byte romCountryCode, String romArtPath, String romGoodName, String romDisplayName,
            boolean isRestarting, boolean isNetplayEnabled, boolean isNetplayServer)
    {
        Log.i( "GalleryActivity", "launchGameActivity" );

        // Make sure that the storage is accessible
        if( !ExtractAssetsOrCleanupTask.areAllAssetsPresent(SplashActivity.SOURCE_DIR, mAppData.coreSharedDataDir))
        {
            Log.e( "GalleryActivity", "SD Card not accessible" );
            Notifier.showToast( this, R.string.toast_sdInaccessible );

            mAppData.putAssetCheckNeeded(true);
            ActivityHelper.startSplashActivity(this);
            finishAffinity();
            return;
        }

        // Update the ConfigSection with the new value for lastPlayed
        final String lastPlayed = Integer.toString( (int) ( new Date().getTime() / 1000 ) );

        mConfig.put(romMd5, "lastPlayed", lastPlayed);
        mConfig.save();

        ///Drawer layout can be null if this method is called from onCreate
        if (mDrawerLayout != null) {
            //Close drawer without animation
            mDrawerLayout.closeDrawer(GravityCompat.START, false);
        }

        tagForRefreshNeeded();

        mSelectedItem = null;
        // Launch the game activity
        startGameActivity(romPath, zipPath, romMd5, romCrc, romHeaderName, romCountryCode,
                romArtPath, romGoodName, romDisplayName, isRestarting, isNetplayEnabled, isNetplayServer);
    }

    void startGameActivity(String romPath, String zipPath, String romMd5, String romCrc,
                                  String romHeaderName, byte romCountryCode, String romArtPath, String romGoodName, String romDisplayName,
                                  boolean doRestart, boolean isNetplayEnabled, boolean isNetplayServer) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra( ActivityHelper.Keys.ROM_PATH, romPath );
        intent.putExtra( ActivityHelper.Keys.ZIP_PATH, zipPath );
        intent.putExtra( ActivityHelper.Keys.ROM_MD5, romMd5 );
        intent.putExtra( ActivityHelper.Keys.ROM_CRC, romCrc );
        intent.putExtra( ActivityHelper.Keys.ROM_HEADER_NAME, romHeaderName );
        intent.putExtra( ActivityHelper.Keys.ROM_COUNTRY_CODE, romCountryCode );
        intent.putExtra( ActivityHelper.Keys.ROM_ART_PATH, romArtPath );
        intent.putExtra( ActivityHelper.Keys.ROM_GOOD_NAME, romGoodName );
        intent.putExtra( ActivityHelper.Keys.ROM_DISPLAY_NAME, romDisplayName );
        intent.putExtra( ActivityHelper.Keys.DO_RESTART, doRestart );
        intent.putExtra( ActivityHelper.Keys.NETPLAY_ENABLED, isNetplayEnabled );
        intent.putExtra( ActivityHelper.Keys.NETPLAY_SERVER, isNetplayServer );
        mLaunchGame.launch(intent);
    }

    @Override
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        return false;
    }

    public void onOpenDrawerButtonClicked(View view)
    {
        mDrawerLayout.openDrawer(GravityCompat.START);
    }

    public void onFabRefreshRomsClick(View view)
    {
        Intent intent = new Intent(this, ScanRomsActivity.class);
        mLaunchScanRoms.launch(intent);
    }
}
