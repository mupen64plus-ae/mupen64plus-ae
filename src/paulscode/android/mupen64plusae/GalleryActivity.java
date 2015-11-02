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

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.GameSidebar.GameSidebarActionHandler;
import paulscode.android.mupen64plusae.dialog.ChangeLog;
import paulscode.android.mupen64plusae.dialog.Popups;
import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptConfirmListener;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.task.ComputeMd5Task;
import paulscode.android.mupen64plusae.task.ComputeMd5Task.ComputeMd5Listener;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.RomDatabase;
import paulscode.android.mupen64plusae.util.RomHeader;
import paulscode.android.mupen64plusae.util.RomDatabase.RomDetail;
import android.annotation.TargetApi;
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

public class GalleryActivity extends AppCompatActivity implements GameSidebarActionHandler
{
    // Saved instance states
    public static final String STATE_QUERY = "query";
    public static final String STATE_SIDEBAR = "sidebar";
    public static final String STATE_CACHE_ROM_INFO_FRAGMENT= "cache_rom_info_fragment";
    
    // App data and user preferences
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;
    
    // Widgets
    private RecyclerView mGridView;
    private DrawerLayout mDrawerLayout;
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
    
    private CacheRomInfoFragment mCacheRomInfoFragment = null;
    
    //True if the restart promp is enabled
    boolean mRestartPromptEnabled = true;
    
    @Override
    protected void onNewIntent( Intent intent )
    {
        // If the activity is already running and is launched again (e.g. from a file manager app),
        // the existing instance will be reused rather than a new one created. This behavior is
        // specified in the manifest (launchMode = singleTask). In that situation, any activities
        // above this on the stack (e.g. GameActivity, GamePrefsActivity) will be destroyed
        // gracefully and onNewIntent() will be called on this instance. onCreate() will NOT be
        // called again on this instance. Currently, the only info that may be passed via the intent
        // is the selected game path, so we only need to refresh that aspect of the UI. This will
        // happen anyhow in onResume(), so we don't really need to do much here.
        super.onNewIntent( intent );
        
        // Only remember the last intent used
        setIntent( intent );
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
        
        int lastVer = mAppData.getLastAppVersionCode();
        int currVer = mAppData.appVersionCode;
        if( lastVer != currVer )
        {
            // First run after install/update, greet user with changelog, then help dialog
            Popups.showFaq( this );
            ChangeLog log = new ChangeLog( getAssets() );
            if( log.show( this, lastVer + 1, currVer ) )
            {
                mAppData.putLastAppVersionCode( currVer );
            }
        }
        
        // Get the ROM path if it was passed from another activity/app
        Bundle extras = getIntent().getExtras();
        if( extras != null )
        {
            String givenRomPath = extras.getString( ActivityHelper.Keys.ROM_PATH );
            if( !TextUtils.isEmpty( givenRomPath ) )
            {
                // Asynchronously compute MD5 and launch game when finished
                Notifier.showToast( this, String.format( getString( R.string.toast_loadingGameInfo ) ) );
                ComputeMd5Task task = new ComputeMd5Task( new File( givenRomPath ), new ComputeMd5Listener()
                {
                    @Override
                    public void onComputeMd5Finished( File file, String md5 )
                    {
                        RomHeader header = new RomHeader(file);
                        
                        final RomDatabase database = RomDatabase.getInstance();
                        
                        if(!database.hasDatabaseFile())
                        {
                            database.setDatabaseFile(mAppData.mupen64plus_ini);
                        }
                        
                        RomDetail detail = database.lookupByMd5WithFallback( md5, file, header.crc );
                        launchGameActivity( file.getAbsolutePath(), null, true, md5, header.crc, header.name,
                            header.countryCode, null, detail.goodName, false );
                    }
                } );
                task.execute();
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
        
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics( metrics );
        
        int width = metrics.widthPixels - galleryHalfSpacing * 2;
        galleryColumns = (int) Math
                .ceil( width * 1.0 / ( galleryMaxWidth + galleryHalfSpacing * 2 ) );
        galleryWidth = width / galleryColumns - galleryHalfSpacing * 2;
        
        GridLayoutManager layoutManager = (GridLayoutManager) mGridView.getLayoutManager();
        layoutManager.setSpanCount( galleryColumns );
        mGridView.getAdapter().notifyDataSetChanged();
        
        // Add the toolbar to the activity (which supports the fancy menu/arrow animation)
        Toolbar toolbar = (Toolbar) findViewById( R.id.toolbar );
        toolbar.setTitle( R.string.app_name );
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
            String md5 = savedInstanceState.getString( STATE_SIDEBAR );
            if( md5 != null )
            {
                // Repopulate the game sidebar
                for( GalleryItem item : mGalleryItems )
                {
                    if( md5.equals( item.md5 ) )
                    {
                        onGalleryItemClick( item );
                        break;
                    }
                }
            }
            
            String query = savedInstanceState.getString( STATE_QUERY );
            if( query != null )
                mSearchQuery = query;
        }
        
        // find the retained fragment on activity restarts
        FragmentManager fm = getSupportFragmentManager();
        mCacheRomInfoFragment = (CacheRomInfoFragment) fm.findFragmentByTag(STATE_CACHE_ROM_INFO_FRAGMENT);
        
        if(mCacheRomInfoFragment == null)
        {
            mCacheRomInfoFragment = new CacheRomInfoFragment();
            fm.beginTransaction().add(mCacheRomInfoFragment, STATE_CACHE_ROM_INFO_FRAGMENT).commit();
        }
        
        // Set the sidebar opacity on the two sidebars
        mDrawerList.setBackgroundDrawable( new DrawerDrawable(
                mGlobalPrefs.displayActionBarTransparency ) );
        mGameSidebar.setBackgroundDrawable( new DrawerDrawable(
                mGlobalPrefs.displayActionBarTransparency ) );
    }
    
    @Override
    public void onSaveInstanceState( Bundle savedInstanceState )
    {
        if( mSearchView != null )
            savedInstanceState.putString( STATE_QUERY, mSearchView.getQuery().toString() );
        if( mSelectedItem != null )
            savedInstanceState.putString( STATE_SIDEBAR, mSelectedItem.md5 );
        
        super.onSaveInstanceState( savedInstanceState );
    }
    
    public void hideSoftKeyboard()
    {
        // Hide the soft keyboard if needed
        if( mSearchView == null )
            return;
        
        InputMethodManager imm = (InputMethodManager) getSystemService( Context.INPUT_METHOD_SERVICE );
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
        
        MenuItem searchItem = menu.findItem( R.id.menuItem_search );
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
            public boolean onQueryTextSubmit( String query )
            {
                
                return false;
            }
            
            public boolean onQueryTextChange( String query )
            {
                mSearchQuery = query;
                refreshGrid();
                return false;
            }
        } );
        
        if( !"".equals( mSearchQuery ) )
        {
            String query = mSearchQuery;
            MenuItemCompat.expandActionView( searchItem );
            mSearchView.setQuery( query, true );
        }
        
        return super.onCreateOptionsMenu( menu );
    }
    
    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch( item.getItemId() )
        {
            case R.id.menuItem_refreshRoms:
                ActivityHelper.StartRomScanService(this);
                return true;
            case R.id.menuItem_library:
                mDrawerLayout.closeDrawer( GravityCompat.START );
                return true;
            case R.id.menuItem_settings:
                ActivityHelper.startGlobalPrefsActivity( this );
                return true;
            case R.id.menuItem_emulationProfiles:
                ActivityHelper.startManageEmulationProfilesActivity( this );
                return true;
            case R.id.menuItem_touchscreenProfiles:
                ActivityHelper.startManageTouchscreenProfilesActivity( this );
                return true;
            case R.id.menuItem_controllerProfiles:
                ActivityHelper.startManageControllerProfilesActivity( this );
                return true;
            case R.id.menuItem_faq:
                Popups.showFaq( this );
                return true;
            case R.id.menuItem_helpForum:
                ActivityHelper.launchUri( this, R.string.uri_forum );
                return true;
            case R.id.menuItem_controllerDiagnostics:
                ActivityHelper.startDiagnosticActivity( this );
                return true;
            case R.id.menuItem_reportBug:
                ActivityHelper.launchUri( this, R.string.uri_bugReport );
                return true;
            case R.id.menuItem_appVersion:
                Popups.showAppVersion( this );
                return true;
            case R.id.menuItem_changelog:
                new ChangeLog( getAssets() ).show( this, 0, mAppData.appVersionCode );
                return true;
            case R.id.menuItem_logcat:
                Popups.showLogcat( this );
                return true;
            case R.id.menuItem_hardwareInfo:
                Popups.showHardwareInfo( this );
                return true;
            case R.id.menuItem_credits:
                ActivityHelper.launchUri( GalleryActivity.this, R.string.uri_credits );
                return true;
            case R.id.menuItem_localeOverride:
                mGlobalPrefs.changeLocale( this );
                return true;
            default:
                return super.onOptionsItemSelected( item );
        }
    }
    
    @Override
    public void onGameSidebarAction(MenuItem menuItem)
    {
        GalleryItem item = mSelectedItem;
        if( item == null )
            return;
        
        final GalleryItem finalItem = item;
        
        switch( menuItem.getItemId() )
        {
            case R.id.menuItem_resume:
                launchGameActivity( finalItem.romFile.getAbsolutePath(),
                       finalItem.zipFile == null ? null : finalItem.zipFile.getAbsolutePath(),
                       finalItem.isExtracted, finalItem.md5, finalItem.crc, finalItem.headerName,
                       finalItem.countryCode, finalItem.artPath, finalItem.goodName, false );
                break;
            case R.id.menuItem_restart:
                //Don't show the prompt if this is the first time we start a game
                if(mRestartPromptEnabled)
                {
                    CharSequence title = getText( R.string.confirm_title );
                    CharSequence message = getText( R.string.confirmResetGame_message );
                    Prompt.promptConfirm( this, title, message,
                            new PromptConfirmListener()
                            {
                                @Override
                                public void onDialogClosed(int which)
                                {
                                    if( which == DialogInterface.BUTTON_POSITIVE )
                                    {
                                        launchGameActivity( finalItem.romFile.getAbsolutePath(),
                                                finalItem.zipFile == null ? null : finalItem.zipFile.getAbsolutePath(),
                                                finalItem.isExtracted, finalItem.md5, finalItem.crc, 
                                                finalItem.headerName, finalItem.countryCode, finalItem.artPath,
                                                finalItem.goodName, true );
                                    }
                                }
                            } );
                }
                else
                {
                    launchGameActivity( finalItem.romFile.getAbsolutePath(),
                        finalItem.zipFile == null ? null : finalItem.zipFile.getAbsolutePath(),
                        finalItem.isExtracted, finalItem.md5, finalItem.crc, 
                        finalItem.headerName, finalItem.countryCode, finalItem.artPath,
                        finalItem.goodName, true );
                }

                break;
            case R.id.menuItem_settings:
                ActivityHelper.startGamePrefsActivity( GalleryActivity.this, finalItem.romFile.getAbsolutePath(),
                    finalItem.md5, finalItem.crc, finalItem.headerName, finalItem.countryCode );
                break;
            default:
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
        String gameDataPath = GamePrefs.getGameDataPath(mSelectedItem.md5, mSelectedItem.headerName,
            RomHeader.countryCodeToSymbol(mSelectedItem.countryCode), mGlobalPrefs);
        String autoSavePath = gameDataPath + "/" + GamePrefs.AUTO_SAVES_DIR + "/";

        File autoSavePathFile = new File(autoSavePath);
        File[] allFilesInSavePath = autoSavePathFile.listFiles();

        //No saves, go ahead and remove it
        boolean visible = allFilesInSavePath != null && allFilesInSavePath.length != 0;
        
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
                Bundle extras = data.getExtras();
                String searchPath = extras.getString( ActivityHelper.Keys.SEARCH_PATH );
                boolean searchZips = extras.getBoolean( ActivityHelper.Keys.SEARCH_ZIPS );
                boolean downloadArt = extras.getBoolean( ActivityHelper.Keys.DOWNLOAD_ART );
                boolean clearGallery = extras.getBoolean( ActivityHelper.Keys.CLEAR_GALLERY );
                
                if (searchPath != null)
                {
                    refreshRoms(new File(searchPath), searchZips, downloadArt, clearGallery);
                }
            }
        }
    }
    
    private void refreshRoms( final File startDir, boolean searchZips, boolean downloadArt, boolean clearGallery )
    {
        mCacheRomInfoFragment.refreshRoms(startDir, searchZips, downloadArt, clearGallery, mAppData, mGlobalPrefs);
    }

    void refreshGrid( ){
        
        ConfigFile config = new ConfigFile( mGlobalPrefs.romInfoCache_cfg );
        
        String query = mSearchQuery.toLowerCase( Locale.US );
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
        
        for( String md5 : config.keySet() )
        {
            if( !ConfigFile.SECTIONLESS_NAME.equals( md5 ) )
            {
                ConfigSection section = config.get( md5 );
                String goodName;
                if( mGlobalPrefs.isFullNameShown || !section.keySet().contains( "baseName" ) )
                    goodName = section.get( "goodName" );
                else
                    goodName = section.get( "baseName" );
                
                boolean matchesSearch = true;
                if( searches != null && searches.length > 0 )
                {
                    // Make sure the ROM name contains every token in the query
                    String lowerName = goodName.toLowerCase( Locale.US );
                    for( String search : searches )
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
                    String romPath = config.get( md5, "romPath" );
                    String zipPath = config.get( md5, "zipPath" );
                    String artPath = config.get( md5, "artPath" );
                    String crc = config.get( md5, "crc" );
                    String headerName = config.get( md5, "headerName" );
                    String countryCodeString = config.get( md5, "countryCode" );
                    byte countryCode = 0;
                    
                    if(countryCodeString != null)
                    {
                        countryCode = Byte.parseByte(countryCodeString);
                    }
                    String lastPlayedStr = config.get( md5, "lastPlayed" );
                    String extracted = config.get( md5, "extracted" );
                    
                    if(zipPath == null || crc == null || headerName == null || countryCodeString == null || extracted == null)
                    {
                        File file = new File(romPath);
                        RomHeader header = new RomHeader(file);
                        
                        zipPath = "";
                        crc = header.crc;
                        headerName = header.name;
                        countryCode = header.countryCode;
                        extracted = "false";
                        
                        config.put( md5, "zipPath", zipPath );
                        config.put( md5, "crc", crc );
                        config.put( md5, "headerName", headerName );
                        config.put( md5, "countryCode", Byte.toString(countryCode) );
                        config.put( md5, "extracted", extracted );
                    }
                    
                    int lastPlayed = 0;
                    if( lastPlayedStr != null )
                        lastPlayed = Integer.parseInt( lastPlayedStr );
                    
                    GalleryItem item = new GalleryItem( this, md5, crc, headerName, countryCode,
                            goodName, romPath, zipPath, extracted.equals("true"), artPath, lastPlayed );
                    items.add( item );
                    if( mGlobalPrefs.isRecentShown
                            && currentTime - item.lastPlayed <= 60 * 60 * 24 * 7 ) // 7 days
                    {
                        recentItems.add( item );
                    }
                    //Delete any old files that already exist inside a zip file
                    else if(!zipPath.equals("") && extracted.equals("true"))
                    {
                        File deleteFile = new File(romPath);
                        deleteFile.delete();
                        
                        config.put( md5, "extracted", "false" );
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
        GridLayoutManager layoutManager = new GridLayoutManager( this, galleryColumns );
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
    
    @TargetApi( 11 )
    private void refreshViews()
    {
        // Refresh the preferences object in case another activity changed the data
        mGlobalPrefs = new GlobalPrefs( this, mAppData );
        
        // Refresh the gallery
        refreshGrid();
    }
    
    public void launchGameActivity( String romPath, String zipPath, boolean extracted, String romMd5, String romCrc,
            String romHeaderName, byte romCountryCode, String romArtPath, String romGoodName, boolean isRestarting )
    {
        // Make sure that the storage is accessible
        if( !mAppData.isSdCardAccessible() )
        {
            Log.e( "GalleryActivity", "SD Card not accessible" );
            Notifier.showToast( this, R.string.toast_sdInaccessible );
            return;
        }
        
        // Notify user that the game activity is starting
        Notifier.showToast( this, R.string.toast_launchingEmulator );
        
        // Update the ConfigSection with the new value for lastPlayed
        String lastPlayed = Integer.toString( (int) ( new Date().getTime() / 1000 ) );
        ConfigFile config = new ConfigFile( mGlobalPrefs.romInfoCache_cfg );
        if( config != null )
        {
            config.put( romMd5, "lastPlayed", lastPlayed );
            
            if(zipPath != null)
            {
                ExtractFileIfNeeded(romMd5, config, romPath, zipPath, extracted);
            }
            
            config.save();
        }
        
        refreshGrid();

        // Launch the game activity
        ActivityHelper.startGameActivity( this, romPath, romMd5, romCrc, romHeaderName, romCountryCode,
                    romArtPath, romGoodName, isRestarting, mGlobalPrefs.isTouchpadEnabled );
    }
    
    private void ExtractFileIfNeeded(String md5, ConfigFile config, String romPath, String zipPath, boolean isExtracted)
    {
        File romFile = new File(romPath);
        RomHeader romHeader = new RomHeader( zipPath );

        boolean isZip = romHeader.isZip;

        if(isZip && (!romFile.exists() || !isExtracted))
        {
            boolean lbFound = false;
            
            try
            {
                ZipFile zipFile = new ZipFile( zipPath );
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while( entries.hasMoreElements() && !lbFound)
                {
                    ZipEntry zipEntry = entries.nextElement();
                    
                    try
                    {
                        InputStream zipStream = zipFile.getInputStream( zipEntry );
                        
                        File destDir = new File( mGlobalPrefs.unzippedRomsDir );
                        String entryName = new File( zipEntry.getName() ).getName();
                        File tempRomPath = new File( destDir, entryName );
                        boolean fileExisted = tempRomPath.exists();
                        
                        if( !fileExisted )
                        {
                            tempRomPath = FileUtil.extractRomFile( destDir, zipEntry, zipStream );
                        }
                        
                        String computedMd5 = ComputeMd5Task.computeMd5( tempRomPath );
                        lbFound = computedMd5.equals(md5);

                        //only deleye the file if we extracted our selves
                        if(!lbFound && !fileExisted)
                        {
                            tempRomPath.delete();
                        }

                        zipStream.close();
                    }
                    catch( IOException e )
                    {
                        Log.w( "CacheRomInfoTask", e );
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
            
            if(lbFound || romFile.exists())
            {
                config.put(md5, "extracted", "true");
            }
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event)
    {
        //Nothing to do
        return false;
    }
}

