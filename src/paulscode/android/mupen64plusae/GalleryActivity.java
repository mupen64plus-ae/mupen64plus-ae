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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.dialog.ChangeLog;
import paulscode.android.mupen64plusae.dialog.Popups;
import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptConfirmListener;
import paulscode.android.mupen64plusae.dialog.ScanRomsDialog;
import paulscode.android.mupen64plusae.dialog.ScanRomsDialog.ScanRomsDialogListener;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.task.CacheRomInfoTask;
import paulscode.android.mupen64plusae.task.CacheRomInfoTask.CacheRomInfoListener;
import paulscode.android.mupen64plusae.task.ComputeMd5Task;
import paulscode.android.mupen64plusae.task.ComputeMd5Task.ComputeMd5Listener;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.RomHeader;
import android.annotation.TargetApi;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
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

public class GalleryActivity extends AppCompatActivity implements CacheRomInfoListener
{
    // Saved instance states
    public static final String STATE_QUERY = "query";
    public static final String STATE_SIDEBAR = "sidebar";
    
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
    
    // Background tasks
    private CacheRomInfoTask mCacheRomInfoTask = null;
    
    // Misc.
    private List<GalleryItem> mGalleryItems = null;
    private GalleryItem mSelectedItem = null;
    private boolean mDragging = false;
    
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
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Get app data and user preferences
        mAppData = new AppData( this );
        mGlobalPrefs = new GlobalPrefs( this );
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
                        ActivityHelper.startGamePrefsActivity( GalleryActivity.this, file.getAbsolutePath(), md5 );
                    }
                } );
                task.execute();
            }
        }
        
        // Lay out the content
        setContentView( R.layout.gallery_activity );
        mGridView = (RecyclerView) findViewById( R.id.gridview );
        refreshGrid( new ConfigFile( mGlobalPrefs.romInfoCache_cfg ) );
        
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
        
        // Popup a warning if the installation appears to be corrupt
        if( !mAppData.isValidInstallation )
        {
            CharSequence title = getText( R.string.invalidInstall_title );
            CharSequence message = getText( R.string.invalidInstall_message );
            new Builder( this ).setTitle( title ).setMessage( message ).create().show();
        }
        
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
    
    protected void onStop()
    {
        super.onStop();
        
        // Cancel long-running background tasks
        if( mCacheRomInfoTask != null )
        {
            mCacheRomInfoTask.cancel( false );
            mCacheRomInfoTask = null;
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
        
        MenuItem searchItem = menu.findItem( R.id.menuItem_search );
        MenuItemCompat.setOnActionExpandListener( searchItem, new OnActionExpandListener()
        {
            @Override
            public boolean onMenuItemActionCollapse( MenuItem item )
            {
                mSearchQuery = "";
                refreshGrid( new ConfigFile( mGlobalPrefs.romInfoCache_cfg ) );
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
                refreshGrid( new ConfigFile( mGlobalPrefs.romInfoCache_cfg ) );
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
                promptSearchPath( null );
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
    
    public void updateSidebar()
    {
        GalleryItem item = mSelectedItem;
        if( item == null )
            return;
        
        // Set the game options
        mGameSidebar.clear();
        
        final GalleryItem finalItem = item;
        final Context finalContext = this;
        
        mGameSidebar.addRow( R.drawable.ic_play, getString( R.string.actionResume_title ),
                getString( R.string.actionResume_summary ), new GameSidebar.Action()
                {
                    @Override
                    public void onAction()
                    {
                        launchGameActivity( finalItem.romFile.getAbsolutePath(), finalItem.md5, false );
                    }
                } );
        
        mGameSidebar.addRow( R.drawable.ic_undo, getString( R.string.actionRestart_title ),
                getString( R.string.actionRestart_summary ), new GameSidebar.Action()
                {
                    @Override
                    public void onAction()
                    {
                        CharSequence title = getText( R.string.confirm_title );
                        CharSequence message = getText( R.string.confirmResetGame_message );
                        Prompt.promptConfirm( finalContext, title, message,
                                new PromptConfirmListener()
                                {
                                    @Override
                                    public void onConfirm()
                                    {
                                        launchGameActivity( finalItem.romFile.getAbsolutePath(), finalItem.md5, true );
                                    }
                                } );
                    }
                } );
        
        mGameSidebar.addRow( R.drawable.ic_settings, getString( R.string.menuItem_settings ), null,
                new GameSidebar.Action()
                {
                    @Override
                    public void onAction()
                    {
                        ActivityHelper.startGamePrefsActivity( GalleryActivity.this, finalItem.romFile.getAbsolutePath(), finalItem.md5 );
                    }
                } );
    }
    
    public void onGalleryItemClick( GalleryItem item )
    {
        mSelectedItem = item;
        
        // Show the game info sidebar
        mDrawerList.setVisibility( View.GONE );
        mGameSidebar.setVisibility( View.VISIBLE );
        mGameSidebar.scrollTo( 0, 0 );
        
        // Set the cover art in the sidebar
        item.loadBitmap();
        mGameSidebar.setImage( item.artBitmap );
        
        // Set the game title
        mGameSidebar.setTitle( item.goodName );
        
        updateSidebar();
        
        // Open the navigation drawer
        mDrawerLayout.openDrawer( GravityCompat.START );
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
    
    private void promptSearchPath( File startDir )
    {
        // Prompt for search path, then asynchronously search for ROMs
        if( startDir == null || !startDir.exists() )
            startDir = new File( Environment.getExternalStorageDirectory().getAbsolutePath() );
        
        ScanRomsDialog dialog = new ScanRomsDialog( this, startDir, mGlobalPrefs.getSearchZips(),
                mGlobalPrefs.getDownloadArt(), mGlobalPrefs.getClearGallery(),
                new ScanRomsDialogListener()
                {
                    @Override
                    public void onDialogClosed( File file, int which, boolean searchZips,
                            boolean downloadArt, boolean clearGallery )
                    {
                        mGlobalPrefs.putSearchZips( searchZips );
                        mGlobalPrefs.putDownloadArt( downloadArt );
                        mGlobalPrefs.putClearGallery( clearGallery );
                        if( which == DialogInterface.BUTTON_POSITIVE )
                        {
                            // Search this folder for ROMs
                            refreshRoms( file );
                        }
                        else if( file != null )
                        {
                            if( file.isDirectory() )
                                promptSearchPath( file );
                            else
                            {
                                // The user selected an individual file
                                refreshRoms( file );
                            }
                        }
                    }
                } );
        dialog.show();
    }
    
    private void refreshRoms( final File startDir )
    {
        // Asynchronously search for ROMs
        mCacheRomInfoTask = new CacheRomInfoTask( this, startDir, mAppData.mupen64plus_ini,
                mGlobalPrefs.romInfoCache_cfg, mGlobalPrefs.coverArtDir, mGlobalPrefs.unzippedRomsDir,
                mGlobalPrefs.getSearchZips(), mGlobalPrefs.getDownloadArt(),
                mGlobalPrefs.getClearGallery(), this );
        mCacheRomInfoTask.execute();
    }
    
    @Override
    public void onCacheRomInfoProgress( ConfigSection section )
    {
    }
    
    @Override
    public void onCacheRomInfoFinished( ConfigFile config, boolean canceled )
    {
        mCacheRomInfoTask = null;
        refreshGrid( config );
    }
    
    private void refreshGrid( ConfigFile config )
    {
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
                    String artPath = config.get( md5, "artPath" );
                    String lastPlayedStr = config.get( md5, "lastPlayed" );
                    int lastPlayed = 0;
                    if( lastPlayedStr != null )
                        lastPlayed = Integer.parseInt( lastPlayedStr );
                    
                    GalleryItem item = new GalleryItem( this, md5, goodName, romPath, artPath,
                            lastPlayed );
                    items.add( item );
                    if( mGlobalPrefs.isRecentShown
                            && currentTime - item.lastPlayed <= 60 * 60 * 24 * 7 ) // 7 days
                        recentItems.add( item );
                }
            }
        }
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
    
    @Override
    protected void onResume()
    {
        super.onResume();
        refreshViews();
    }
    
    @TargetApi( 11 )
    private void refreshViews()
    {
        // Refresh the preferences object in case another activity changed the data
        mGlobalPrefs = new GlobalPrefs( this );
        
        // Set the sidebar opacity on the two sidebars
        mDrawerList.setBackgroundDrawable( new DrawerDrawable(
                mGlobalPrefs.displayActionBarTransparency ) );
        mGameSidebar.setBackgroundDrawable( new DrawerDrawable(
                mGlobalPrefs.displayActionBarTransparency ) );
        
        // Refresh the gallery
        refreshGrid( new ConfigFile( mGlobalPrefs.romInfoCache_cfg ) );
    }
    
    public void launchGameActivity( String romPath, String romMd5, boolean isRestarting )
    {
        RomHeader romHeader = new RomHeader( romPath );
        GamePrefs gamePrefs = new GamePrefs( this, romMd5, romHeader );
// TODO FIXME
//        // Popup the multi-player dialog if necessary and abort if any players are unassigned
//        RomDatabase romDatabase = new RomDatabase( mAppData.mupen64plus_ini );
//        RomDetail romDetail = romDatabase.lookupByMd5WithFallback( romMd5, new File( romPath ) );
//        if( romDetail.players > 1 && gamePrefs.playerMap.isEnabled()
//                && mGlobalPrefs.getPlayerMapReminder() )
//        {
//            gamePrefs.playerMap.removeUnavailableMappings();
//            boolean needs1 = gamePrefs.isControllerEnabled1 && !gamePrefs.playerMap.isMapped( 1 );
//            boolean needs2 = gamePrefs.isControllerEnabled2 && !gamePrefs.playerMap.isMapped( 2 );
//            boolean needs3 = gamePrefs.isControllerEnabled3 && !gamePrefs.playerMap.isMapped( 3 )
//                    && romDetail.players > 2;
//            boolean needs4 = gamePrefs.isControllerEnabled4 && !gamePrefs.playerMap.isMapped( 4 )
//                    && romDetail.players > 3;
//            
//            if( needs1 || needs2 || needs3 || needs4 )
//            {
//                @SuppressWarnings( "deprecation" )
//                PlayerMapPreference pref = (PlayerMapPreference) findPreference( "playerMap" );
//                pref.show();
//                return;
//            }
//        }
        
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
            config.save();
        }
        
        // Launch the game activity
        ActivityHelper.startGameActivity( this, romPath, romMd5, gamePrefs.getCheatArgs(), isRestarting, mGlobalPrefs.isTouchpadEnabled );
    }
}
