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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.fragment.app.FragmentManager;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.view.MenuItem.OnActionExpandListener;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import paulscode.android.mupen64plusae.GameSidebar.GameSidebarActionHandler;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog.PromptConfirmListener;
import paulscode.android.mupen64plusae.dialog.Popups;
import paulscode.android.mupen64plusae.jni.CoreService;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.task.ComputeMd5Task;
import paulscode.android.mupen64plusae.task.ExtractAssetsOrCleanupTask;
import paulscode.android.mupen64plusae.task.GalleryRefreshTask;
import paulscode.android.mupen64plusae.task.GalleryRefreshTask.GalleryRefreshFinishedListener;
import paulscode.android.mupen64plusae.task.UpdateLeanbackProgramsTask;
import paulscode.android.mupen64plusae.util.CountryCode;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.ProviderUtil;
import paulscode.android.mupen64plusae.util.RomDatabase;
import paulscode.android.mupen64plusae.util.RomHeader;

public class GalleryActivity extends AppCompatActivity implements GameSidebarActionHandler, PromptConfirmListener,
        GalleryRefreshFinishedListener
{
    // Saved instance states
    private static final String STATE_QUERY = "STATE_QUERY";
    private static final String STATE_SIDEBAR = "STATE_SIDEBAR";
    private static final String STATE_FILE_TO_DELETE = "STATE_FILE_TO_DELETE";
    private static final String STATE_CACHE_ROM_INFO_FRAGMENT= "STATE_CACHE_ROM_INFO_FRAGMENT";
    private static final String STATE_GALLERY_REFRESH_NEEDED= "STATE_GALLERY_REFRESH_NEEDED";
    private static final String STATE_GAME_STARTED_EXTERNALLY = "STATE_GAME_STARTED_EXTERNALLY";
    private static final String STATE_REMOVE_FROM_LIBRARY_DIALOG = "STATE_REMOVE_FROM_LIBRARY_DIALOG";
    public static final String KEY_IS_LEANBACK = "KEY_IS_LEANBACK";
    public static final String KEY_IS_SHORTCUT = "KEY_IS_SHORTCUT";

    public static final int REMOVE_FROM_LIBRARY_DIALOG_ID = 1;

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

    private ConfigFile mConfig;

    private void loadGameFromExtras( Bundle extras) {

        if (extras != null) {

            Intent intent = new Intent(CoreService.SERVICE_EVENT);
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

                    launchGameOnCreation(givenRomPath);
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

                launchGameActivity( romPath, zipPath,  md5, crc, headerName, countryCode, artPath, goodName, displayName, true );
                getIntent().replaceExtras((Bundle)null);
            }
        } else {
            Intent intent = new Intent(CoreService.SERVICE_EVENT);
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
        final Bundle extras = getIntent().getExtras();

        loadGameFromExtras(extras);
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

        // Get app data and user preferences
        mAppData = new AppData( this );
        mGlobalPrefs = new GlobalPrefs( this, mAppData );
        mConfig = new ConfigFile(mGlobalPrefs.romInfoCache_cfg);

        // Lay out the content
        setContentView( R.layout.gallery_activity );
        mGridView = findViewById( R.id.gridview );

        // Do empty initialization of the GridView
        List<GalleryItem> items = new ArrayList<>();
        mGridView.setAdapter( new GalleryItem.Adapter(this, items));
        final GridLayoutManager layoutManager = new GridLayoutManagerBetterScrolling( this, galleryColumns );
        mGridView.setLayoutManager( layoutManager );

        refreshGridAsync();

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
        mDrawerList.setOnClickListener( new MenuListView.OnClickListener()
        {
            @Override
            public void onClick( MenuItem menuItem )
            {
                GalleryActivity.this.onOptionsItemSelected( menuItem );
            }
        } );

        // Configure the game information drawer
        mGameSidebar = findViewById( R.id.gameSidebar );

        // Handle events from the side bar
        mGameSidebar.setActionHandler(this, R.menu.gallery_game_drawer);

        if( savedInstanceState != null )
        {
            mSelectedItem = null;
            final String sideBarMd5 = savedInstanceState.getString( STATE_SIDEBAR );
            if( sideBarMd5 != null )
            {
                mSelectedItem = new GalleryItem(getApplicationContext(), sideBarMd5, null, null,
                        CountryCode.DEMO, null, null, null, null, null, 0, 0.0f);
            }

            final String query = savedInstanceState.getString( STATE_QUERY );
            if( query != null )
                mSearchQuery = query;

            mPathToDelete = savedInstanceState.getString( STATE_FILE_TO_DELETE );
            mRefreshNeeded = savedInstanceState.getBoolean(STATE_GALLERY_REFRESH_NEEDED);
            mGameStartedExternally = savedInstanceState.getBoolean(STATE_GAME_STARTED_EXTERNALLY);
        }

        // find the retained fragment on activity restarts
        final FragmentManager fm = getSupportFragmentManager();
        mCacheRomInfoFragment = (ScanRomsFragment) fm.findFragmentByTag(STATE_CACHE_ROM_INFO_FRAGMENT);

        if(mCacheRomInfoFragment == null)
        {
            mCacheRomInfoFragment = new ScanRomsFragment();
            fm.beginTransaction().add(mCacheRomInfoFragment, STATE_CACHE_ROM_INFO_FRAGMENT).commit();
        }

        // Get the ROM path if it was passed from another activity/app
        final Bundle extras = getIntent().getExtras();
        loadGameFromExtras(extras);

        if(ActivityHelper.isServiceRunning(this, ActivityHelper.coreServiceProcessName)) {
            Log.i("GalleryActivity", "CoreService is running");
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
            refreshGridAsync();

            mGameSidebar.setVisibility( View.GONE );
            mDrawerList.setVisibility( View.VISIBLE );
        }
    }

    @Override
    public void onSaveInstanceState( Bundle savedInstanceState )
    {
        Log.i("GalleryActivity", "onSaveInstanceState");

        if( mSearchView != null )
            savedInstanceState.putString( STATE_QUERY, mSearchView.getQuery().toString() );
        if( mSelectedItem != null )
            savedInstanceState.putString( STATE_SIDEBAR, mSelectedItem.md5 );
        savedInstanceState.putBoolean(STATE_GALLERY_REFRESH_NEEDED, mRefreshNeeded);
        savedInstanceState.putBoolean(STATE_GAME_STARTED_EXTERNALLY, mGameStartedExternally);
        savedInstanceState.putString( STATE_FILE_TO_DELETE, mPathToDelete);

        super.onSaveInstanceState( savedInstanceState );
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
    public void onConfigurationChanged( Configuration newConfig )
    {
        super.onConfigurationChanged( newConfig );
        mDrawerToggle.onConfigurationChanged( newConfig );
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        getMenuInflater().inflate( R.menu.gallery_activity, menu );

        final MenuItem searchItem = menu.findItem( R.id.menuItem_search );
        searchItem.setOnActionExpandListener( new OnActionExpandListener()
        {
            @Override
            public boolean onMenuItemActionCollapse( MenuItem item )
            {
                mSearchQuery = "";
                refreshGridAsync();
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand( MenuItem item )
            {
                return true;
            }
        } );

        mSearchView = (SearchView) searchItem.getActionView();
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
            final String query = mSearchQuery;
            searchItem.expandActionView();
            mSearchView.setIconified(false);
            mSearchView.setQuery( query, true );
        }

        //On Android 8.0+ this is necessary to be able to type text using a controller
        mSearchView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                mSearchView.setIconified(false);
            }
        });

        return super.onCreateOptionsMenu( menu );
    }

    private void launchGameOnCreation(String givenRomPath)
    {
        if (givenRomPath == null) {
            return;
        }

        mGameStartedExternally = true;
        String finalRomPath = givenRomPath;

        RomHeader header = new RomHeader(finalRomPath);

        boolean successful = false;
        if(header.isZip)
        {
            finalRomPath = FileUtil.ExtractFirstROMFromZip(givenRomPath, mGlobalPrefs.unzippedRomsDir);
        }
        else if (header.is7Zip) {
            finalRomPath = FileUtil.ExtractFirstROMFromSevenZ(givenRomPath, mGlobalPrefs.unzippedRomsDir);
        }

        if(finalRomPath != null)
        {
            // Asynchronously compute MD5 and launch game when finished
            final String computedMd5 = ComputeMd5Task.computeMd5( new File( finalRomPath ) );

            if(computedMd5 != null)
            {
                header = new RomHeader(finalRomPath);

                final RomDatabase database = RomDatabase.getInstance();

                if(!database.hasDatabaseFile())
                {
                    database.setDatabaseFile(mAppData.mupen64plus_ini);
                }

                successful = true;

                final RomDatabase.RomDetail detail = database.lookupByMd5WithFallback( computedMd5, new File(finalRomPath).getName(), header.crc, header.countryCode );
                String artPath = mGlobalPrefs.coverArtDir + "/" + detail.artName;
                launchGameActivity( finalRomPath, null, computedMd5, header.crc, header.name,
                        header.countryCode.getValue(), artPath, detail.goodName, detail.goodName, false );
            }
        }

        if (!successful) {
            Notifier.showToast(this, R.string.toast_nativeMainFailure07);
        }
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch (item.getItemId())
        {
        case R.id.menuItem_refreshRoms:
            ActivityHelper.startRomScanActivity(this);
            return true;
        case R.id.menuItem_library:
            mDrawerLayout.closeDrawer(GravityCompat.START);
            return true;
        case R.id.menuItem_categoryLibrary:
            mRefreshNeeded = true;
            ActivityHelper.startLibraryPrefsActivity( this );
            return true;
        case R.id.menuItem_categoryDisplay:
            mRefreshNeeded = true;
            ActivityHelper.startDisplayPrefsActivity( this );
            return true;
        case R.id.menuItem_categoryAudio:
            ActivityHelper.startAudioPrefsActivity( this );
            return true;
        case R.id.menuItem_categoryTouchscreen:
            mRefreshNeeded = true;
            ActivityHelper.startTouchscreenPrefsActivity( this );
            return true;
        case R.id.menuItem_categoryInput:
            ActivityHelper.startInputPrefsActivity( this );
            return true;
        case R.id.menuItem_categoryData:
            mRefreshNeeded = true;
            ActivityHelper.startDataPrefsActivity( this );
            return true;
         case R.id.menuItem_categoryDefaults:
            ActivityHelper.startDefaultPrefsActivity( this );
            return true;
        case R.id.menuItem_emulationProfiles:
            ActivityHelper.startManageEmulationProfilesActivity(this);
            return true;
        case R.id.menuItem_touchscreenProfiles:
            ActivityHelper.startManageTouchscreenProfilesActivity(this);
            return true;
        case R.id.menuItem_controllerProfiles:
            ActivityHelper.startManageControllerProfilesActivity(this);
            return true;
        case R.id.menuItem_faq:
            Popups.showFaq(this);
            return true;
        case R.id.menuItem_helpForum:
            ActivityHelper.launchUri(this, R.string.uri_forum);
            return true;
        case R.id.menuItem_controllerDiagnostics:
            ActivityHelper.startDiagnosticActivity(this);
            return true;
        case R.id.menuItem_reportBug:
            ActivityHelper.launchUri(this, R.string.uri_bugReport);
            return true;
        case R.id.menuItem_appVersion:
            Popups.showAppVersion(this);
            return true;
        case R.id.menuItem_logcat:
            ActivityHelper.startLogcatActivity(this);
            return true;
        case R.id.menuItem_hardwareInfo:
            Popups.showHardwareInfo(this);
            return true;
        case R.id.menuItem_credits:
            ActivityHelper.launchUri(GalleryActivity.this, R.string.uri_credits);
            return true;
        case R.id.menuItem_localeOverride:
            mGlobalPrefs.changeLocale(this);
            return true;
        case R.id.menuItem_extract:
            ActivityHelper.starExtractTextureActivity(this);
            return true;
        case R.id.menuItem_clear:
            ActivityHelper.startDeleteTextureActivity(this);
            return true;
        default:
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

            int dimension = Math.min(bitmap.getWidth(), bitmap.getHeight());
            Bitmap croppedBitmap = ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension);

            IconCompat icon = IconCompat.createWithBitmap(croppedBitmap);
            ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(this, item.md5)
                    .setIcon(icon)
                    .setIntent(gameIntent)
                    .setShortLabel(item.displayName)
                    .build();
            ShortcutManagerCompat.requestPinShortcut(this, shortcut, null);
        }
    }

    @Override
    public void onGameSidebarAction(MenuItem menuItem)
    {
        final GalleryItem item = mSelectedItem;
        if( item == null || item.romUri == null)
            return;

        switch( menuItem.getItemId() )
        {
            case R.id.menuItem_resume:
                launchGameActivity( item.romUri,
                        item.zipUri,
                        item.md5, item.crc, item.headerName,
                        item.countryCode.getValue(), item.artPath, item.goodName, item.displayName, false );
                break;
            case R.id.menuItem_start:
                launchGameActivity( item.romUri,
                        item.zipUri,
                        item.md5, item.crc,
                        item.headerName, item.countryCode.getValue(), item.artPath,
                        item.goodName, item.displayName, true );
                break;
            case R.id.menuItem_settings:
            {
                String romLegacySaveFileName;
                
                if(item.zipUri != null)
                {
                    romLegacySaveFileName = ProviderUtil.getFileName(this, Uri.parse(item.zipUri));
                }
                else
                {
                    romLegacySaveFileName = ProviderUtil.getFileName(this, Uri.parse(item.romUri));
                }
                ActivityHelper.startGamePrefsActivity( GalleryActivity.this, item.romUri,
                        item.md5, item.crc, item.headerName, item.goodName, item.displayName, item.countryCode.getValue(),
                        romLegacySaveFileName);
                break;

            }
            case R.id.menuItem_remove:
            {
                final CharSequence title = getText( R.string.confirm_title );
                final CharSequence message = getText( R.string.confirmRemoveFromLibrary_message );

                final ConfirmationDialog confirmationDialog =
                        ConfirmationDialog.newInstance(REMOVE_FROM_LIBRARY_DIALOG_ID, title.toString(), message.toString());

                final FragmentManager fm = getSupportFragmentManager();
                confirmationDialog.show(fm, STATE_REMOVE_FROM_LIBRARY_DIALOG);
                break;
            }
            case R.id.menuItem_createShortcut:
                createGameShortcut(item);
                break;
            default:
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
            item.loadBitmap();
            mGameSidebar.setImage(item.artBitmap);
        } else {
            mGameSidebar.setImage(null);
        }

        // Set the game title
        mGameSidebar.setTitle(item.displayName);


        // Restore the menu
        mGameSidebar.setActionHandler(GalleryActivity.this, R.menu.gallery_game_drawer);

        // If there are no saves for this game, disable the resume option

        final String autoSavePath = GamePrefs.getGameDataPath(mSelectedItem.md5, mSelectedItem.headerName,
                mSelectedItem.countryCode.toString(), mAppData) + "/" + GamePrefs.AUTO_SAVES_DIR + "/";

        //Alternate paths in case we have file system problems
        final String gameAlternate = GamePrefs.getAlternateGameDataPath(mSelectedItem.md5, mSelectedItem.headerName,
                mSelectedItem.countryCode.toString(), mAppData) + "/" + GamePrefs.AUTO_SAVES_DIR + "/";
        final String game2ndAlternate = GamePrefs.getSecondAlternateGameDataPath(mSelectedItem.md5, mAppData) +
                "/" + GamePrefs.AUTO_SAVES_DIR + "/";

        final File[] allFilesInSavePath = new File(autoSavePath).listFiles();
        final File[] alternateAllFilesInSavePath = new File(gameAlternate).listFiles();
        final File[] secondAlternateAllFilesInSavePath = new File(game2ndAlternate).listFiles();

        //No saves, go ahead and remove it
        final boolean visible = ((allFilesInSavePath != null && allFilesInSavePath.length != 0) ||
                (alternateAllFilesInSavePath != null && alternateAllFilesInSavePath.length != 0) ||
                (secondAlternateAllFilesInSavePath != null && secondAlternateAllFilesInSavePath.length != 0)) &&
                mGlobalPrefs.maxAutoSaves > 0;

        if (!visible)
        {
            // Disable the action handler
            mGameSidebar.getMenu().removeItem(R.id.menuItem_resume);
            mGameSidebar.reload();
        }

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
            item.artPath, item.goodName, item.displayName, false );
        return true;
    }

    @Override
    public boolean onKeyDown( int keyCode, KeyEvent event )
    {
        if( keyCode == KeyEvent.KEYCODE_MENU )
        {
            // Show the navigation drawer when the user presses the Menu button
            // http://stackoverflow.com/q/22220275
            if( mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
            {
                mDrawerLayout.closeDrawer( GravityCompat.START );
            }
            else
            {
                mDrawerLayout.openDrawer( GravityCompat.START );
            }
            return true;
        }

        return super.onKeyDown( keyCode, event );
    }

    @Override
    public void onBackPressed()
    {
        if( mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
        {
            mDrawerLayout.closeDrawer( GravityCompat.START );
        }
        else if(mSearchView != null && !mSearchView.isIconified()) {
            mSearchView.onActionViewCollapsed();
            mSearchQuery = "";
        }
        else
        {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == ActivityHelper.SCAN_ROM_REQUEST_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK && data != null)
            {
                final Bundle extras = data.getExtras();

                if (extras != null) {
                    final String searchUri = extras.getString( ActivityHelper.Keys.SEARCH_PATH );
                    final boolean searchZips = extras.getBoolean( ActivityHelper.Keys.SEARCH_ZIPS );
                    final boolean downloadArt = extras.getBoolean( ActivityHelper.Keys.DOWNLOAD_ART );
                    final boolean clearGallery = extras.getBoolean( ActivityHelper.Keys.CLEAR_GALLERY );
                    final boolean searchSubdirectories = extras.getBoolean( ActivityHelper.Keys.SEARCH_SUBDIR );

                    if (searchUri != null)
                    {
                        refreshRoms(searchUri, searchZips, downloadArt, clearGallery, searchSubdirectories);
                    }
                }
            }
        }
        else if(requestCode == ActivityHelper.GAME_ACTIVITY_CODE)
        {
            if( mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
            {
                mDrawerLayout.closeDrawer( GravityCompat.START );
            }

            if(mGameStartedExternally)
            {
                finishAffinity();
            }
        }
    }

    private void refreshRoms(final String searchUri, boolean searchZips, boolean downloadArt, boolean clearGallery, boolean searchSubdirectories)
    {
        mCacheRomInfoFragment.refreshRoms(searchUri, searchZips, downloadArt, clearGallery, searchSubdirectories, mAppData, mGlobalPrefs);
    }

    void refreshGridAsync()
    {
        //Reload global prefs
        mAppData = new AppData( this );
        mGlobalPrefs = new GlobalPrefs( this, mAppData );

        GalleryRefreshTask galleryRefreshTask = new GalleryRefreshTask(this, this, mGlobalPrefs, mSearchQuery, mConfig);
        galleryRefreshTask.execute();
    }

    void reloadCacheAndRefreshGrid()
    {
        mConfig = new ConfigFile(mGlobalPrefs.romInfoCache_cfg);

        refreshGridAsync();
    }

    @Override
    public void onGalleryRefreshFinished(List<GalleryItem> items, List<GalleryItem> recentItems) {
        refreshGrid(items, recentItems);
    }

    synchronized void refreshGrid(List<GalleryItem> items, List<GalleryItem> recentItems){

        if( mGlobalPrefs.isRecentShown && TextUtils.isEmpty(mSearchQuery) && recentItems.size() > 0 )
        {
            List<GalleryItem> combinedItems = new ArrayList<>();

            combinedItems.add( new GalleryItem( this, getString( R.string.galleryRecentlyPlayed ) ) );
            combinedItems.addAll( recentItems );

            combinedItems.add( new GalleryItem( this, getString( R.string.galleryLibrary ) ) );
            combinedItems.addAll( items );

            items = combinedItems;
        }

        List<GalleryItem> galleryItems = items;
        mGridView.setAdapter( new GalleryItem.Adapter( this, items ) );

        // Allow the headings to take up the entire width of the layout
        final List<GalleryItem> finalItems = items;
        final GridLayoutManager layoutManager = new GridLayoutManagerBetterScrolling( this, galleryColumns );
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

        final DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics( metrics );

        final int width = metrics.widthPixels - galleryHalfSpacing * 2;
        galleryColumns = (int) Math
                .ceil( width * 1.0 / ( galleryMaxWidth + galleryHalfSpacing * 2 ) );
        galleryWidth = width / galleryColumns - galleryHalfSpacing * 2;

        layoutManager.setSpanCount( galleryColumns );
        if (mGridView.getAdapter() != null) {
            mGridView.getAdapter().notifyDataSetChanged();
        }
        mGridView.setFocusable(false);
        mGridView.setFocusableInTouchMode(false);

        if (mAppData.isAndroidTv && AppData.IS_OREO) {
            UpdateLeanbackProgramsTask updateLeanbackPrograms = new UpdateLeanbackProgramsTask(getApplicationContext(), recentItems,
                    mAppData.getChannelId());
            updateLeanbackPrograms.execute();
        }

        if (mSelectedItem != null) {
            // Repopulate the game sidebar
            for (final GalleryItem item : galleryItems) {
                if (mSelectedItem.md5.equals( item.md5 )) {
                    onGalleryItemClick( item );
                    break;
                }
            }
        }
    }

    public void launchGameActivity( String romPath, String zipPath, String romMd5, String romCrc,
            String romHeaderName, byte romCountryCode, String romArtPath, String romGoodName, String romDisplayName,
            boolean isRestarting)
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

        String romLegacySaveFileName;

        //Convoluted way of moving legacy save file names to the new format
        if(zipPath != null)
        {
            File zipFile = new File(zipPath);
            romLegacySaveFileName = zipFile.getName();
        }
        else
        {
            File romFile = new File(romPath);
            romLegacySaveFileName = romFile.getName();
        }

        mConfig.put(romMd5, "lastPlayed", lastPlayed);
        mConfig.save();

        ///Drawer layout can be null if this method is called from onCreate
        if (mDrawerLayout != null) {
            //Close drawer without animation
            mDrawerLayout.closeDrawer(GravityCompat.START, false);
        }

        if (mSearchView != null) {
            mSearchView.onActionViewCollapsed();
        }
        mSearchQuery = "";

        mRefreshNeeded = true;

        mSelectedItem = null;
        // Launch the game activity
        ActivityHelper.startGameActivity(this, romPath, zipPath, romMd5, romCrc, romHeaderName, romCountryCode,
                romArtPath, romGoodName, romDisplayName, romLegacySaveFileName, isRestarting);
    }

    @Override
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        return false;
    }
}
