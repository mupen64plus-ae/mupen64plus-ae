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
package paulscode.android.mupen64plusae;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MenuItemCompat.OnActionExpandListener;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import org.mupen64plusae.v3.fzurita.R;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import paulscode.android.mupen64plusae.GameSidebar.GameSidebarActionHandler;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog.PromptConfirmListener;
import paulscode.android.mupen64plusae.dialog.Popups;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.task.ComputeMd5Task;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.RomDatabase;
import paulscode.android.mupen64plusae.util.RomHeader;

public class GalleryActivity extends AppCompatActivity implements GameSidebarActionHandler, PromptConfirmListener
{
    // Saved instance states
    private static final String STATE_QUERY = "query";
    private static final String STATE_SIDEBAR = "sidebar";
    private static final String STATE_CACHE_ROM_INFO_FRAGMENT= "STATE_CACHE_ROM_INFO_FRAGMENT";
    private static final String STATE_EXTRACT_TEXTURES_FRAGMENT= "STATE_EXTRACT_TEXTURES_FRAGMENT";
    private static final String STATE_GALLERY_REFRESH_NEEDED= "STATE_GALLERY_REFRESH_NEEDED";
    private static final String STATE_RESTART_CONFIRM_DIALOG = "STATE_RESTART_CONFIRM_DIALOG";
    private static final String STATE_CLEAR_CONFIRM_DIALOG = "STATE_CLEAR_CONFIRM_DIALOG";
    private static final int RESTART_CONFIRM_DIALOG_ID = 0;
    private static final int CLEAR_CONFIRM_DIALOG_ID = 1;

    // App data and user preferences
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;

    // Widgets
    private RecyclerView mGridView;
    private DrawerLayout mDrawerLayout = null;
    private ActionBarDrawerToggle mDrawerToggle;
    private MenuListView mDrawerList;
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
    private List<GalleryItem> mGalleryItems = null;
    private GalleryItem mSelectedItem = null;
    private boolean mDragging = false;

    private ScanRomsFragment mCacheRomInfoFragment = null;
    private ExtractTexturesFragment mExtractTexturesFragment = null;
    
    //True if the restart promp is enabled
    boolean mRestartPromptEnabled = true;

    //If this is set to true, the gallery will be refreshed next time this activity is resumed
    boolean mRefreshNeeded = false;

    @Override
    protected void onNewIntent( Intent intent )
    {
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
        if( extras != null)
        {
            final String givenRomPath = extras.getString( ActivityHelper.Keys.ROM_PATH );

            if( !TextUtils.isEmpty( givenRomPath ) )
            {
                getIntent().removeExtra(ActivityHelper.Keys.ROM_PATH);
                launchGameOnCreation(givenRomPath);
                finish();
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        // Get app data and user preferences
        mAppData = new AppData( this );
        mGlobalPrefs = new GlobalPrefs( this, mAppData );
        mGlobalPrefs.enforceLocale( this );

        if(!mGlobalPrefs.cacheRecentlyPlayed)
        {
            FileUtil.deleteFolder(new File(mGlobalPrefs.unzippedRomsDir));
        }

        // Get the ROM path if it was passed from another activity/app
        final Bundle extras = getIntent().getExtras();
        if( extras != null)
        {
            final String givenRomPath = extras.getString( ActivityHelper.Keys.ROM_PATH );

            if( !TextUtils.isEmpty( givenRomPath ) )
            {
                getIntent().removeExtra(ActivityHelper.Keys.ROM_PATH);
                launchGameOnCreation(givenRomPath);
                finish();
            }
        }

        // Lay out the content
        setContentView( R.layout.gallery_activity );
        mGridView = (RecyclerView) findViewById( R.id.gridview );
        refreshGrid();

        // Update the grid layout
        galleryMaxWidth = (int) getResources().getDimension( R.dimen.galleryImageWidth );
        galleryHalfSpacing = (int) getResources().getDimension( R.dimen.galleryHalfSpacing );
        galleryAspectRatio = galleryMaxWidth * 1.0f
                / getResources().getDimension( R.dimen.galleryImageHeight );

        final DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics( metrics );

        final int width = metrics.widthPixels - galleryHalfSpacing * 2;
        galleryColumns = (int) Math
                .ceil( width * 1.0 / ( galleryMaxWidth + galleryHalfSpacing * 2 ) );
        galleryWidth = width / galleryColumns - galleryHalfSpacing * 2;

        final GridLayoutManager layoutManager = (GridLayoutManager) mGridView.getLayoutManager();
        layoutManager.setSpanCount( galleryColumns );
        mGridView.getAdapter().notifyDataSetChanged();
        mGridView.setFocusable(false);
        mGridView.setFocusableInTouchMode(false);

        // Add the toolbar to the activity (which supports the fancy menu/arrow animation)
        final Toolbar toolbar = (Toolbar) findViewById( R.id.toolbar );
        toolbar.setTitle( R.string.app_name );
        final View firstGridChild = mGridView.getChildAt(0);

        if(firstGridChild != null)
        {
            toolbar.setNextFocusDownId(firstGridChild.getId());
        }

        setSupportActionBar( toolbar );

        // Configure the navigation drawer
        mDrawerLayout = (DrawerLayout) findViewById( R.id.drawerLayout );
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
                mSelectedItem = null;

                super.onDrawerClosed( drawerView );
            }

            @Override
            public void onDrawerOpened( View drawerView )
            {
                hideSoftKeyboard();
                super.onDrawerOpened( drawerView );
            }
        };
        mDrawerLayout.setDrawerListener( mDrawerToggle );

        // Configure the list in the navigation drawer
        mDrawerList = (MenuListView) findViewById( R.id.drawerNavigation );
        mDrawerList.setMenuResource( R.menu.gallery_drawer );

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
        mGameSidebar = (GameSidebar) findViewById( R.id.gameSidebar );

        // Handle events from the side bar
        mGameSidebar.setActionHandler(this, R.menu.gallery_game_drawer);

        if( savedInstanceState != null )
        {
            mSelectedItem = null;
            final String md5 = savedInstanceState.getString( STATE_SIDEBAR );
            if( md5 != null )
            {
                // Repopulate the game sidebar
                for( final GalleryItem item : mGalleryItems )
                {
                    if( md5.equals( item.md5 ) )
                    {
                        onGalleryItemClick( item );
                        break;
                    }
                }
            }

            final String query = savedInstanceState.getString( STATE_QUERY );
            if( query != null )
                mSearchQuery = query;

            mRefreshNeeded = savedInstanceState.getBoolean(STATE_GALLERY_REFRESH_NEEDED);
        }

        // find the retained fragment on activity restarts
        final FragmentManager fm = getSupportFragmentManager();
        mCacheRomInfoFragment = (ScanRomsFragment) fm.findFragmentByTag(STATE_CACHE_ROM_INFO_FRAGMENT);
        mExtractTexturesFragment = (ExtractTexturesFragment) fm.findFragmentByTag(STATE_EXTRACT_TEXTURES_FRAGMENT);

        if(mCacheRomInfoFragment == null)
        {
            mCacheRomInfoFragment = new ScanRomsFragment();
            fm.beginTransaction().add(mCacheRomInfoFragment, STATE_CACHE_ROM_INFO_FRAGMENT).commit();
        }
        
        if(mExtractTexturesFragment == null)
        {
            mExtractTexturesFragment = new ExtractTexturesFragment();
            fm.beginTransaction().add(mExtractTexturesFragment, STATE_EXTRACT_TEXTURES_FRAGMENT).commit();
        }

        // Set the sidebar opacity on the two sidebars
        mDrawerList.setBackgroundDrawable( new DrawerDrawable(
                mGlobalPrefs.displayActionBarTransparency ) );
        mGameSidebar.setBackgroundDrawable( new DrawerDrawable(
                mGlobalPrefs.displayActionBarTransparency ) );
    }

    @Override
    public void onResume()
    {
        super.onResume();

        //mRefreshNeeded will be set to true whenever a game is launched
        if(mRefreshNeeded)
        {
            mRefreshNeeded = false;
            refreshGrid();

            mGameSidebar.setVisibility( View.GONE );
            mDrawerList.setVisibility( View.VISIBLE );
        }
    }

    @Override
    public void onSaveInstanceState( Bundle savedInstanceState )
    {
        if( mSearchView != null )
            savedInstanceState.putString( STATE_QUERY, mSearchView.getQuery().toString() );
        if( mSelectedItem != null )
            savedInstanceState.putString( STATE_SIDEBAR, mSelectedItem.md5 );
        savedInstanceState.putBoolean(STATE_GALLERY_REFRESH_NEEDED, mRefreshNeeded);

        super.onSaveInstanceState( savedInstanceState );
    }

    public void hideSoftKeyboard()
    {
        // Hide the soft keyboard if needed
        if( mSearchView == null )
            return;

        final InputMethodManager imm = (InputMethodManager) getSystemService( Context.INPUT_METHOD_SERVICE );
        imm.hideSoftInputFromWindow( mSearchView.getWindowToken(), 0 );
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
        MenuItemCompat.setOnActionExpandListener( searchItem, new OnActionExpandListener()
        {
            @Override
            public boolean onMenuItemActionCollapse( MenuItem item )
            {
                mSearchQuery = "";
                refreshGrid();
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand( MenuItem item )
            {
                return true;
            }
        } );

        mSearchView = (SearchView) MenuItemCompat.getActionView( searchItem );
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
                mSearchQuery = query;
                refreshGrid();
                return false;
            }
        } );

        if( !"".equals( mSearchQuery ) )
        {
            final String query = mSearchQuery;
            MenuItemCompat.expandActionView( searchItem );
            mSearchView.setQuery( query, true );
        }

        return super.onCreateOptionsMenu( menu );
    }

    private void launchGameOnCreation(String givenRomPath)
    {
        String finalRomPath = givenRomPath;

        boolean isZip = givenRomPath.toLowerCase().endsWith("zip");

        if(isZip)
        {
            finalRomPath = ExtractFirstROMFromZip(givenRomPath);
        }

        // Asynchronously compute MD5 and launch game when finished
        final String computedMd5 = ComputeMd5Task.computeMd5( new File( finalRomPath ) );

        if(computedMd5 != null)
        {
            final RomHeader header = new RomHeader(finalRomPath);

            final RomDatabase database = RomDatabase.getInstance();

            if(!database.hasDatabaseFile())
            {
                database.setDatabaseFile(mAppData.mupen64plus_ini);
            }

            final RomDatabase.RomDetail detail = database.lookupByMd5WithFallback( computedMd5, new File( finalRomPath), header.crc );
            String artPath = mGlobalPrefs.coverArtDir + "/" + detail.artName;

            launchGameActivity( finalRomPath, null, true, computedMd5, header.crc, header.name,
                    header.countryCode, artPath, detail.goodName, false );
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
            Popups.showLogcat(this);
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
        {
            String title = getString( R.string.confirm_title );
            String message = getString( R.string.confirmClearData_message );

            ConfirmationDialog confirmationDialog =
                    ConfirmationDialog.newInstance(CLEAR_CONFIRM_DIALOG_ID, title, message);

            FragmentManager fm = getSupportFragmentManager();
            confirmationDialog.show(fm, STATE_CLEAR_CONFIRM_DIALOG);
        }
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onGameSidebarAction(MenuItem menuItem)
    {
        final GalleryItem item = mSelectedItem;
        if( item == null )
            return;

        switch( menuItem.getItemId() )
        {
            case R.id.menuItem_resume:
                launchGameActivity( item.romFile.getAbsolutePath(),
                        item.zipFile == null ? null : item.zipFile.getAbsolutePath(),
                        item.isExtracted, item.md5, item.crc, item.headerName,
                        item.countryCode, item.artPath, item.goodName, false );
                break;
            case R.id.menuItem_restart:
                //Don't show the prompt if this is the first time we start a game
                if(mRestartPromptEnabled)
                {
                    final CharSequence title = getText( R.string.confirm_title );
                    final CharSequence message = getText( R.string.confirmResetGame_message );

                    final ConfirmationDialog confirmationDialog =
                        ConfirmationDialog.newInstance(RESTART_CONFIRM_DIALOG_ID, title.toString(), message.toString());

                    final FragmentManager fm = getSupportFragmentManager();
                    confirmationDialog.show(fm, STATE_RESTART_CONFIRM_DIALOG);
                }
                else
                {
                    launchGameActivity( item.romFile.getAbsolutePath(),
                            item.zipFile == null ? null : item.zipFile.getAbsolutePath(),
                            item.isExtracted, item.md5, item.crc,
                            item.headerName, item.countryCode, item.artPath,
                            item.goodName, true );
                }

                break;
            case R.id.menuItem_settings:
            {
                String romLegacySaveFileName = null;
                
                if(item.zipFile != null)
                {
                    romLegacySaveFileName = item.zipFile.getName();
                }
                else
                {
                    romLegacySaveFileName = item.romFile.getName();
                }
                ActivityHelper.startGamePrefsActivity( GalleryActivity.this, item.romFile.getAbsolutePath(),
                        item.md5, item.crc, item.headerName, item.goodName, item.countryCode,
                        romLegacySaveFileName);
                break;

            }
            default:
        }
    }

    @Override
    public void onPromptDialogClosed(int id, int which)
    {
        if( which == DialogInterface.BUTTON_POSITIVE )
        {
            if(id == RESTART_CONFIRM_DIALOG_ID && mSelectedItem != null)
            {
                launchGameActivity( mSelectedItem.romFile.getAbsolutePath(),
                    mSelectedItem.zipFile == null ? null : mSelectedItem.zipFile.getAbsolutePath(),
                    mSelectedItem.isExtracted, mSelectedItem.md5, mSelectedItem.crc,
                    mSelectedItem.headerName, mSelectedItem.countryCode, mSelectedItem.artPath,
                    mSelectedItem.goodName, true );
            }
            if(id == CLEAR_CONFIRM_DIALOG_ID)
            {
                FileUtil.deleteFolder(new File(mGlobalPrefs.coreUserDataDir));
                FileUtil.deleteFolder(new File(mGlobalPrefs.coreUserCacheDir));
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

        // Set the cover art in the sidebar
        item.loadBitmap();
        mGameSidebar.setImage(item.artBitmap);

        // Set the game title
        mGameSidebar.setTitle(item.goodName);

        // If there are no saves for this game, disable the resume
        // option
        final String gameDataPath = GamePrefs.getGameDataPath(mSelectedItem.md5, mSelectedItem.headerName,
            RomHeader.countryCodeToSymbol(mSelectedItem.countryCode), mAppData);
        final String autoSavePath = gameDataPath + "/" + GamePrefs.AUTO_SAVES_DIR + "/";

        final File autoSavePathFile = new File(autoSavePath);
        final File[] allFilesInSavePath = autoSavePathFile.listFiles();

        //No saves, go ahead and remove it
        final boolean visible = allFilesInSavePath != null && allFilesInSavePath.length != 0;

        if (visible)
        {
            // Restore the menu
            mGameSidebar.setActionHandler(GalleryActivity.this, R.menu.gallery_game_drawer);
            mRestartPromptEnabled = true;
        }
        else
        {
            // Disable the action handler
            mGameSidebar.getMenu().removeItem(R.id.menuItem_resume);
            mGameSidebar.reload();
            mRestartPromptEnabled = false;
        }

        // Open the navigation drawer
        mDrawerLayout.openDrawer(GravityCompat.START);
    }

    public boolean onGalleryItemLongClick( GalleryItem item )
    {
        launchGameActivity( item.romFile.getAbsolutePath(),
            item.zipFile == null ? null : item.zipFile.getAbsolutePath(),
            item.isExtracted, item.md5, item.crc, item.headerName, item.countryCode,
            item.artPath, item.goodName, false );
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
                final String searchPath = extras.getString( ActivityHelper.Keys.SEARCH_PATH );
                final boolean searchZips = extras.getBoolean( ActivityHelper.Keys.SEARCH_ZIPS );
                final boolean downloadArt = extras.getBoolean( ActivityHelper.Keys.DOWNLOAD_ART );
                final boolean clearGallery = extras.getBoolean( ActivityHelper.Keys.CLEAR_GALLERY );
                final boolean searchSubdirectories = extras.getBoolean( ActivityHelper.Keys.SEARCH_SUBDIR );

                if (searchPath != null)
                {
                    refreshRoms(new File(searchPath), searchZips, downloadArt, clearGallery, searchSubdirectories);
                }
            }
        }
        else if(requestCode == ActivityHelper.EXTRACT_TEXTURES_CODE)
        {
            // Make sure the request was successful
            if (resultCode == RESULT_OK && data != null)
            {
                final Bundle extras = data.getExtras();
                final String searchPath = extras.getString(ActivityHelper.Keys.SEARCH_PATH);

                if (searchPath != null)
                {
                    mExtractTexturesFragment.extractTextures(new File(searchPath));
                }
            }
        }
    }

    private void refreshRoms(final File startDir, boolean searchZips, boolean downloadArt, boolean clearGallery, boolean searchSubdirectories)
    {
        mCacheRomInfoFragment.refreshRoms(startDir, searchZips, downloadArt, clearGallery, searchSubdirectories, mAppData, mGlobalPrefs);
    }

    void refreshGrid( ){

        //Reload global prefs
        mAppData = new AppData( this );
        mGlobalPrefs = new GlobalPrefs( this, mAppData );

        final ConfigFile config = new ConfigFile( mGlobalPrefs.romInfoCache_cfg );

        final String query = mSearchQuery.toLowerCase( Locale.US );
        String[] searches = null;
        if( query.length() > 0 )
            searches = query.split( " " );

        List<GalleryItem> items = new ArrayList<GalleryItem>();
        List<GalleryItem> recentItems = null;
        int currentTime = 0;
        if( mGlobalPrefs.isRecentShown )
        {
            recentItems = new ArrayList<GalleryItem>();
            currentTime = (int) ( new Date().getTime() / 1000 );
        }

        for( final String md5 : config.keySet() )
        {
            if( !ConfigFile.SECTIONLESS_NAME.equals( md5 ) )
            {
                final ConfigSection section = config.get( md5 );
                String goodName;
                if( mGlobalPrefs.isFullNameShown || !section.keySet().contains( "baseName" ) )
                    goodName = section.get( "goodName" );
                else
                    goodName = section.get( "baseName" );

                boolean matchesSearch = true;
                if( searches != null && searches.length > 0 )
                {
                    // Make sure the ROM name contains every token in the query
                    final String lowerName = goodName.toLowerCase( Locale.US );
                    for( final String search : searches )
                    {
                        if( search.length() > 0 && !lowerName.contains( search ) )
                        {
                            matchesSearch = false;
                            break;
                        }
                    }
                }

                if( matchesSearch )
                {
                    final String romPath = config.get( md5, "romPath" );
                    String zipPath = config.get( md5, "zipPath" );
                    final String artFullPath = config.get( md5, "artPath" );

                    //We get the file name to support the old gallery format
                    String artPath = !TextUtils.isEmpty(artFullPath) ? new File(artFullPath).getName() : null;

                    if(artPath != null)
                        artPath = mGlobalPrefs.coverArtDir + "/" + artPath;
                    
                    String crc = config.get( md5, "crc" );
                    String headerName = config.get( md5, "headerName" );
                    final String countryCodeString = config.get( md5, "countryCode" );
                    byte countryCode = 0;

                    //We can't really do much if the rompath is null
                    if (romPath != null)
                    {
                        if (countryCodeString != null)
                        {
                            countryCode = Byte.parseByte(countryCodeString);
                        }
                        final String lastPlayedStr = config.get(md5, "lastPlayed");
                        String extracted = config.get(md5, "extracted");

                        if (zipPath == null || crc == null || headerName == null || countryCodeString == null
                            || extracted == null)
                        {
                            final File file = new File(romPath);
                            final RomHeader header = new RomHeader(file);

                            zipPath = "";
                            crc = header.crc;
                            headerName = header.name;
                            countryCode = header.countryCode;
                            extracted = "false";

                            config.put(md5, "zipPath", zipPath);
                            config.put(md5, "crc", crc);
                            config.put(md5, "headerName", headerName);
                            config.put(md5, "countryCode", Byte.toString(countryCode));
                            config.put(md5, "extracted", extracted);
                        }

                        int lastPlayed = 0;
                        if (lastPlayedStr != null)
                            lastPlayed = Integer.parseInt(lastPlayedStr);

                        final GalleryItem item = new GalleryItem(this, md5, crc, headerName, countryCode, goodName, romPath,
                            zipPath, extracted.equals("true"), artPath, lastPlayed);
                        items.add(item);
                        boolean isNotOld = currentTime - item.lastPlayed <= 60 * 60 * 24 * 7; // 7 days
                        if (mGlobalPrefs.isRecentShown && isNotOld )
                        {
                            recentItems.add(item);
                        }

                        // Delete any old files that already exist inside a zip
                        // file
                        if (!isNotOld && !zipPath.equals("") && extracted.equals("true"))
                        {
                            final File deleteFile = new File(romPath);

                            if(!deleteFile.isDirectory())
                            {
                                deleteFile.delete();
                            }

                            config.put(md5, "extracted", "false");
                        }
                    }
                }
            }
        }

        config.save();

        Collections.sort( items, new GalleryItem.NameComparator() );
        if( recentItems != null )
            Collections.sort( recentItems, new GalleryItem.RecentlyPlayedComparator() );

        List<GalleryItem> combinedItems = items;
        if( mGlobalPrefs.isRecentShown && recentItems.size() > 0 )
        {
            combinedItems = new ArrayList<GalleryItem>();

            combinedItems
                    .add( new GalleryItem( this, getString( R.string.galleryRecentlyPlayed ) ) );
            combinedItems.addAll( recentItems );

            combinedItems.add( new GalleryItem( this, getString( R.string.galleryLibrary ) ) );
            combinedItems.addAll( items );

            items = combinedItems;
        }

        mGalleryItems = items;
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
    }

    public void launchGameActivity( String romPath, String zipPath, boolean extracted, String romMd5, String romCrc,
            String romHeaderName, byte romCountryCode, String romArtPath, String romGoodName, boolean isRestarting)
    {
        // Make sure that the storage is accessible
        if( !mAppData.isSdCardAccessible() )
        {
            Log.e( "GalleryActivity", "SD Card not accessible" );
            Notifier.showToast( this, R.string.toast_sdInaccessible );

            mAppData.putAssetVersion(0);
            ActivityHelper.startSplashActivity(this);
            finish();
            return;
        }

        // Notify user that the game activity is starting
        Notifier.showToast( this, R.string.toast_launchingEmulator );

        // Update the ConfigSection with the new value for lastPlayed
        final String lastPlayed = Integer.toString( (int) ( new Date().getTime() / 1000 ) );
        final ConfigFile config = new ConfigFile( mGlobalPrefs.romInfoCache_cfg );
        if( config.get(romMd5) != null)
        {
            config.put( romMd5, "lastPlayed", lastPlayed );

            if(zipPath != null)
            {
                romPath = ExtractFileIfNeeded(romMd5, config, romPath, zipPath, extracted);
            }

            config.save();
        }

        ///Drawer layout can be null if this method is called from onCreate
        if(mDrawerLayout != null)
        {
            //Close drawer without animation
            mDrawerLayout.closeDrawer( GravityCompat.START, false );
        }
        mRefreshNeeded = true;

        mSelectedItem = null;
        
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

        // Launch the game activity
        ActivityHelper.startGameActivity( this, romPath, romMd5, romCrc, romHeaderName, romCountryCode,
                    romArtPath, romGoodName, romLegacySaveFileName, isRestarting );
    }

    private String ExtractFirstROMFromZip(String zipPath)
    {
        try
        {
            ZipFile zipFile = new ZipFile( zipPath );
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while( entries.hasMoreElements() )
            {
                ZipEntry zipEntry = entries.nextElement();

                try
                {
                    InputStream zipStream = zipFile.getInputStream( zipEntry );
                    File extractedFile = FileUtil.extractRomFile( new File( mGlobalPrefs.unzippedRomsDir ), zipEntry, zipStream );

                    if( extractedFile != null)
                    {
                        zipStream.close();
                        return extractedFile.getPath();
                    }
                }
                catch( IOException e )
                {
                    Log.w( "CacheRomInfoService", e );
                }
            }
            zipFile.close();
        }
        catch( ZipException e )
        {
            Log.w( "GalleryActivity", e );
        }
        catch( IOException e )
        {
            Log.w( "GalleryActivity", e );
        }
        catch( ArrayIndexOutOfBoundsException e )
        {
            Log.w( "GalleryActivity", e );
        }

        return null;
    }

    private String ExtractFileIfNeeded(String md5, ConfigFile config, String romPath, String zipPath, boolean isExtracted)
    {
        final File romFile = new File(romPath);
        String romFileName = romFile.getName();
        final File extractedRomFile = new File(mGlobalPrefs.unzippedRomsDir + "/" + romFileName);
        final RomHeader romHeader = new RomHeader( zipPath );

        final boolean isZip = romHeader.isZip;

        if(isZip && (!extractedRomFile.exists() || !isExtracted))
        {
            boolean lbFound = false;

            try
            {
                final ZipFile zipFile = new ZipFile( zipPath );
                final Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while( entries.hasMoreElements() && !lbFound)
                {
                    final ZipEntry zipEntry = entries.nextElement();

                    try
                    {
                        final InputStream zipStream = zipFile.getInputStream( zipEntry );

                        final File destDir = new File( mGlobalPrefs.unzippedRomsDir );
                        final String entryName = new File( zipEntry.getName() ).getName();
                        File tempRomPath = new File( destDir, entryName );
                        final boolean fileExisted = tempRomPath.exists();

                        if( !fileExisted )
                        {
                            tempRomPath = FileUtil.extractRomFile( destDir, zipEntry, zipStream );
                        }

                        final String computedMd5 = ComputeMd5Task.computeMd5( tempRomPath );
                        lbFound = computedMd5 != null && computedMd5.equals(md5);

                        //only delete the file if we extracted our selves
                        if(!lbFound && !fileExisted && tempRomPath != null && !tempRomPath.isDirectory())
                        {
                            tempRomPath.delete();
                        }

                        zipStream.close();
                    }
                    catch( final IOException e )
                    {
                        Log.w( "CacheRomInfoTask", e );
                    }
                }
                zipFile.close();
            }
            catch( final IOException e)
            {
                Log.w( "GalleryActivity", e );
            }
            catch(ArrayIndexOutOfBoundsException e )
            {
                Log.w( "GalleryActivity", e );
            }

            if(lbFound || romFile.exists())
            {
                config.put(md5, "extracted", "true");
            }
        }

        return extractedRomFile.getPath();
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event)
    {
        //Nothing to do
        return false;
    }
}

