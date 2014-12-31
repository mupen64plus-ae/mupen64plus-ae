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
import java.util.List;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.input.DiagnosticActivity;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.profile.ManageControllerProfilesActivity;
import paulscode.android.mupen64plusae.profile.ManageEmulationProfilesActivity;
import paulscode.android.mupen64plusae.profile.ManageTouchscreenProfilesActivity;
import paulscode.android.mupen64plusae.task.CacheRomInfoTask;
import paulscode.android.mupen64plusae.task.CacheRomInfoTask.CacheRomInfoListener;
import paulscode.android.mupen64plusae.task.ComputeMd5Task;
import paulscode.android.mupen64plusae.task.ComputeMd5Task.ComputeMd5Listener;
import paulscode.android.mupen64plusae.util.ChangeLog;
import paulscode.android.mupen64plusae.util.DeviceUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.Prompt.PromptFileListener;
import paulscode.android.mupen64plusae.util.Utility;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

public class GalleryActivity extends Activity implements OnItemClickListener, ComputeMd5Listener, CacheRomInfoListener
{
    // App data and user preferences
    private AppData mAppData = null;
    private UserPrefs mUserPrefs = null;
    
    // Widgets
    private GridView mGridView;
    
    @Override
    protected void onNewIntent( Intent intent )
    {
        // If the activity is already running and is launched again (e.g. from a file manager app),
        // the existing instance will be reused rather than a new one created. This behavior is
        // specified in the manifest (launchMode = singleTask). In that situation, any activities
        // above this on the stack (e.g. GameActivity, PlayMenuActivity) will be destroyed
        // gracefully and onNewIntent() will be called on this instance. onCreate() will NOT be
        // called again on this instance. Currently, the only info that may be passed via the intent
        // is the selected game path, so we only need to refresh that aspect of the UI.  This will
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
        mUserPrefs = new UserPrefs( this );
        mUserPrefs.enforceLocale( this );
        
        int lastVer = mAppData.getLastAppVersionCode();
        int currVer = mAppData.appVersionCode;
        if( lastVer != currVer )
        {
            // First run after install/update, greet user with changelog, then help dialog
            popupFaq();
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
            String givenRomPath = extras.getString( Keys.Extras.ROM_PATH );
            if( !TextUtils.isEmpty( givenRomPath ) )
                launchPlayMenuActivity( givenRomPath );
        }
        
        // Lay out the content
        setContentView( R.layout.gallery_activity );
        mGridView = (GridView) findViewById( R.id.gridview );
        refreshGrid( new ConfigFile( mUserPrefs.romInfoCache_cfg ) );
        
        // Popup a warning if the installation appears to be corrupt
        if( !mAppData.isValidInstallation )
        {
            CharSequence title = getText( R.string.invalidInstall_title );
            CharSequence message = getText( R.string.invalidInstall_message );
            new Builder( this ).setTitle( title ).setMessage( message ).create().show();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        getMenuInflater().inflate( R.menu.gallery_activity, menu );
        return super.onCreateOptionsMenu( menu );
    }
    
    @Override
    public boolean onMenuItemSelected( int featureId, MenuItem item )
    {
        switch( item.getItemId() )
        {
            case R.id.menuItem_refreshRoms:
                promptSearchPath( null );
                return true;
            case R.id.menuItem_globalSettings:
                startActivity( new Intent( this, SettingsGlobalActivity.class ) );
                return true;
            case R.id.menuItem_emulationProfiles:
                startActivity( new Intent( this, ManageEmulationProfilesActivity.class ) );
                return true;
            case R.id.menuItem_touchscreenProfiles:
                startActivity( new Intent( this, ManageTouchscreenProfilesActivity.class ) );
                return true;
            case R.id.menuItem_controllerProfiles:
                startActivity( new Intent( this, ManageControllerProfilesActivity.class ) );
                return true;
            case R.id.menuItem_faq:
                popupFaq();
                return true;
            case R.id.menuItem_helpForum:
                Utility.launchUri( GalleryActivity.this, R.string.uri_forum );
                return true;
            case R.id.menuItem_controllerDiagnostics:
                startActivity( new Intent( this, DiagnosticActivity.class ) );
                return true;
            case R.id.menuItem_reportBug:
                Utility.launchUri( GalleryActivity.this, R.string.uri_bugReport );
                return true;
            case R.id.menuItem_appVersion:
                popupAppVersion();
                return true;
            case R.id.menuItem_changelog:
                new ChangeLog( getAssets() ).show( GalleryActivity.this, 0, mAppData.appVersionCode );
                return true;
            case R.id.menuItem_logcat:
                popupLogcat();
                return true;
            case R.id.menuItem_hardwareInfo:
                popupHardwareInfo();
                return true;
            case R.id.menuItem_credits:
                Utility.launchUri( GalleryActivity.this, R.string.uri_credits );
                return true;
            case R.id.menuItem_localeOverride:
                mUserPrefs.changeLocale( this );
                return true;
            default:
                return super.onMenuItemSelected( featureId, item );
        }
    }
    
    @Override
    public void onItemClick( AdapterView<?> parent, View view, int position, long id )
    {
        GalleryItem item = (GalleryItem) parent.getItemAtPosition( position );
        if( item == null )
            Log.e( "GalleryActivity", "No item selected" );
        else if( item.romFile == null )
            Log.e( "GalleryActivity", "No ROM file available" );
        else
            launchPlayMenuActivity( item.romFile.getAbsolutePath(), item.md5 );
    }
    
    private void launchPlayMenuActivity( final String romPath )
    {
        // Asynchronously compute MD5 and launch play menu when finished
        Notifier.showToast( this, String.format( getString( R.string.toast_loadingGameInfo ) ) );
        new ComputeMd5Task( new File( romPath ), this ).execute();
    }
    
    @Override
    public void onComputeMd5Finished( File file, String md5 )
    {
        launchPlayMenuActivity( file.getAbsolutePath(), md5 );
    }
    
    private void launchPlayMenuActivity( String romPath, String md5 )
    {
        if( !TextUtils.isEmpty( romPath ) && !TextUtils.isEmpty( md5 ) )
        {
            Intent intent = new Intent( GalleryActivity.this, PlayMenuActivity.class );
            intent.putExtra( Keys.Extras.ROM_PATH, romPath );
            intent.putExtra( Keys.Extras.ROM_MD5, md5 );
            startActivity( intent );
        }
    }
    
    private void promptSearchPath( File startDir )
    {
        // Prompt for search path, then asynchronously search for ROMs
        if( startDir == null || !startDir.exists() )
            startDir = new File( Environment.getExternalStorageDirectory().getAbsolutePath() );
        
        Prompt.promptFile( this, startDir.getPath(), null, startDir, true, true, false, true,
                new PromptFileListener()
                {
                    @Override
                    public void onDialogClosed( File file, int which )
                    {
                        if( which == DialogInterface.BUTTON_POSITIVE )
                        {
                            refreshRoms( file );
                        }
                        else if( file != null )
                        {
                            promptSearchPath( file );
                        }
                    }
                } );
    }
    
    private void refreshRoms( final File startDir )
    {
        // Asynchronously search for ROMs
        Notifier.showToast( this, "Searching for ROMs in " + startDir.getName() );
        new CacheRomInfoTask( startDir, mAppData.mupen64plus_ini, mUserPrefs.romInfoCache_cfg, mUserPrefs.galleryDataDir, this ).execute();
    }
    
    @Override
    public void onCacheRomInfoProgress( ConfigSection section )
    {
        Notifier.showToast( this, section.get( "goodName" ) );
    }
    
    @Override
    public void onCacheRomInfoFinished( ConfigFile config )
    {
        Notifier.showToast( this, "Finished" );
        refreshGrid( config );
    }
    
    private void refreshGrid( ConfigFile config )
    {
        List<GalleryItem> items = new ArrayList<GalleryItem>();
        for( String md5 : config.keySet() )
        {
            if( !ConfigFile.SECTIONLESS_NAME.equals( md5 ) )
            {
                String goodName = config.get( md5, "goodName" );
                String romPath = config.get( md5, "romPath" );
                String artPath = config.get( md5, "artPath" );
                items.add( new GalleryItem( this, md5, goodName, romPath, artPath ) );
            }
        }
        Collections.sort( items );
        mGridView.setAdapter( new GalleryItem.Adapter( this, R.id.text1, items ) );
        mGridView.setOnItemClickListener( this );
    }
    
    private void popupFaq()
    {
        CharSequence title = getText( R.string.menuItem_faq );
        CharSequence message = getText( R.string.popup_faq );
        new Builder( this ).setTitle( title ).setMessage( message ).create().show();
    }
    
    private void popupLogcat()
    {
        String title = getString( R.string.menuItem_logcat );
        String message = DeviceUtil.getLogCat();
        popupShareableText( title, message );
    }
    
    private void popupHardwareInfo()
    {
        String title = getString( R.string.menuItem_hardwareInfo );
        String axisInfo = DeviceUtil.getAxisInfo();
        String peripheralInfo = DeviceUtil.getPeripheralInfo();
        String cpuInfo = DeviceUtil.getCpuInfo();
        String message = axisInfo + peripheralInfo + cpuInfo;
        popupShareableText( title, message );
    }
    
    private void popupShareableText( String title, final String message )
    {
        // Set up click handler to share text with a user-selected app (email, clipboard, etc.)
        DialogInterface.OnClickListener shareHandler = new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                // See http://android-developers.blogspot.com/2012/02/share-with-intents.html
                Intent intent = new Intent( android.content.Intent.ACTION_SEND );
                intent.setType( "text/plain" );
                intent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET );
                intent.putExtra( Intent.EXTRA_TEXT, message );
                // intent.putExtra( Intent.EXTRA_SUBJECT, subject );
                // intent.putExtra( Intent.EXTRA_EMAIL, new String[] { emailTo } );
                startActivity( Intent.createChooser( intent, getText( R.string.actionShare_title ) ) );
            }
        };
        
        new Builder( this ).setTitle( title ).setMessage( message.toString() )
                .setNeutralButton( R.string.actionShare_title, shareHandler ).create().show();
    }
    
    private void popupAppVersion()
    {
        String title = getString( R.string.menuItem_appVersion );
        String message = getString( R.string.popup_version, mAppData.appVersion, mAppData.appVersionCode );
        new Builder( this ).setTitle( title ).setMessage( message ).create().show();
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
        mUserPrefs = new UserPrefs( this );
        
        // Refresh the action bar
        if( AppData.IS_HONEYCOMB )
            invalidateOptionsMenu();
    }
}
