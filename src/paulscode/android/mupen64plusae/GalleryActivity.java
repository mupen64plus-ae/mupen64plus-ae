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

import paulscode.android.mupen64plusae.input.DiagnosticActivity;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.ChangeLog;
import paulscode.android.mupen64plusae.util.DeviceUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.Prompt.PromptFileListener;
import paulscode.android.mupen64plusae.util.RomDetail;
import paulscode.android.mupen64plusae.util.Utility;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class GalleryActivity extends Activity implements OnClickListener
{
    // App data and user preferences
    private AppData mAppData = null;
    private UserPrefs mUserPrefs = null;
    private RomDetail mRomDetail = null;
    
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
        
        // Lay out the content
        setContentView( R.layout.gallery_activity );
        Button button = (Button) findViewById( R.id.button_pathSelectedGame );
        button.setOnClickListener( this );
        
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
        menu.findItem( R.id.menuItem_axisInfo ).setVisible( AppData.IS_HONEYCOMB_MR1 );
        return super.onCreateOptionsMenu( menu );
    }
    
    @Override
    public boolean onMenuItemSelected( int featureId, MenuItem item )
    {
        switch( item.getItemId() )
        {
            case R.id.menuItem_play:
                launchPlayMenuActivity( mUserPrefs.selectedGame );
                return true;
            case R.id.menuItem_globalSettings:
                startActivity( new Intent( this, SettingsGlobalActivity.class ) );
                return true;
            case R.id.menuItem_touchscreenProfiles:
                // TODO
                popupTodo();
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
            case R.id.menuItem_axisInfo:
                popupAxisInfo();
                return true;
            case R.id.menuItem_controllerInfo:
                popupControllerInfo();
                return true;
            case R.id.menuItem_systemInfo:
                popupDeviceInfo();
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
    public void onClick( View v )
    {
        promptFile( new File( mUserPrefs.selectedGame ) );
    }
    
    private void launchPlayMenuActivity( final String romPath )
    {
        // Asynchronously compute MD5 and launch play menu when finished
        Notifier.showToast( this, String.format( getString( R.string.toast_loadingGameSettings ), mRomDetail.baseName ) );
        new AsyncTask<Void, Void, String>()
        {
            @Override
            protected String doInBackground( Void... params )
            {
                return RomDetail.computeMd5( new File( romPath ) );
            }
            
            @Override
            protected void onPostExecute( String result )
            {
                if( !TextUtils.isEmpty( result ) )
                {
                    Intent intent = new Intent( GalleryActivity.this, PlayMenuActivity.class );
                    intent.putExtra( PlayMenuActivity.EXTRA_MD5, result );
                    startActivity( intent );
                }
            }
        }.execute();
    }
    
    private void promptFile( File startPath )
    {
        String title = startPath.getPath();
        String message = null;
        Prompt.promptFile( this, title, message, startPath, true, true, true, new PromptFileListener()
        {
            @Override
            public void onDialogClosed( File file, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                {
                    if( file.isFile() )
                    {
                        mUserPrefs.putPathSelectedGame( file.getAbsolutePath() );
                        refreshViews();
                    }
                    else
                    {
                        promptFile( file );
                    }
                }
            }
        } );
    }

    private void popupFaq()
    {
        CharSequence title = getText( R.string.menuItem_faq );
        CharSequence message = getText( R.string.popup_faq );
        new Builder( this ).setTitle( title ).setMessage( message ).create().show();
    }
    
    private void popupAxisInfo()
    {
        String title = getString( R.string.menuItem_axisInfo );
        String message = DeviceUtil.getAxisInfo();
        new Builder( this ).setTitle( title ).setMessage( message ).create().show();
    }
    
    private void popupControllerInfo()
    {
        String title = getString( R.string.menuItem_controllerInfo );
        String message = DeviceUtil.getPeripheralInfo();
        new Builder( this ).setTitle( title ).setMessage( message ).create().show();
    }
    
    private void popupDeviceInfo()
    {
        String title = getString( R.string.menuItem_systemInfo );
        String message = DeviceUtil.getCpuInfo();
        new Builder( this ).setTitle( title ).setMessage( message ).create().show();
    }
    
    private void popupAppVersion()
    {
        String title = getString( R.string.menuItem_appVersion );
        String message = getString( R.string.popup_version, mAppData.appVersion, mAppData.appVersionCode );
        new Builder( this ).setTitle( title ).setMessage( message ).create().show();
    }
    
    private void popupTodo()
    {
        new Builder( this ).setMessage( "TODO" ).create().show();
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
        mRomDetail = RomDetail.lookupByCrc( mUserPrefs.selectedGameHeader.crc );
        
        // Refresh the action bar
        if( AppData.IS_HONEYCOMB )
            invalidateOptionsMenu();
        
        // Update the button text for the selected game
        File selectedGame = new File( mUserPrefs.selectedGame );
        Button button = (Button) findViewById( R.id.button_pathSelectedGame );
        button.setText( selectedGame.getName() );
    }
}
