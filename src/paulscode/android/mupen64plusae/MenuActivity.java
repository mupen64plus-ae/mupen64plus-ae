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
import paulscode.android.mupen64plusae.persistent.PathPreference;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.ChangeLog;
import paulscode.android.mupen64plusae.util.DeviceUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.RomDetail;
import paulscode.android.mupen64plusae.util.Utility;
import android.annotation.TargetApi;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

public class MenuActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
    // These constants must match the keys used in res/xml/preferences.xml
    private static final String PATH_SELECTED_GAME = "pathSelectedGame";
    private static final String LOCALE_OVERRIDE = "localeOverride";
    
    // App data and user preferences
    private AppData mAppData = null;
    private UserPrefs mUserPrefs = null;
    private SharedPreferences mPrefs = null;
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
    
    @SuppressWarnings( "deprecation" )
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Get app data and user preferences
        mAppData = new AppData( this );
        mUserPrefs = new UserPrefs( this );
        mUserPrefs.enforceLocale( this );
        mPrefs = PreferenceManager.getDefaultSharedPreferences( this );
        
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
        
        // Load user preference menu structure from XML and update view
        addPreferencesFromResource( R.xml.preferences );
        
        // Populate the language menu
        ListPreference languagePref = (ListPreference) findPreference( LOCALE_OVERRIDE );
        languagePref.setEntryValues( mUserPrefs.localeCodes );
        languagePref.setEntries( mUserPrefs.localeNames );
        
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
        getMenuInflater().inflate( R.menu.menu_activity, menu );
        menu.findItem( R.id.menuItem_axisInfo ).setVisible( AppData.IS_HONEYCOMB_MR1 );
        return super.onCreateOptionsMenu( menu );
    }
    
    @Override
    public boolean onPrepareOptionsMenu( Menu menu )
    {
        MenuItem item = menu.findItem( R.id.menuItem_gameSettings );
        String romName = mRomDetail.baseName;
        boolean isValid = romName != null;
        String title;
        if( !TextUtils.isEmpty( romName ) )
            title = getString( R.string.menuItem_gameSettingsNamed, romName );
        else
            title = getString( R.string.menuItem_gameSettings );
        item.setTitle( title );
        item.setEnabled( isValid );
        return super.onPrepareOptionsMenu( menu );
    }
    
    @Override
    public boolean onMenuItemSelected( int featureId, MenuItem item )
    {
        switch( item.getItemId() )
        {
            case R.id.menuItem_play:
                startActivity( new Intent( this, PlayMenuActivity.class ) );
                return true;
            case R.id.menuItem_gameSettings:
                // TODO startActivity( new Intent( this, SettingsGameActivity.class ) );
                popupGameSettingsTodo();
                return true;
            case R.id.menuItem_globalSettings:
                startActivity( new Intent( this, SettingsGlobalActivity.class ) );
                return true;
            case R.id.menuItem_touchscreenProfiles:
                // TODO
                popupTodo();
                return true;
            case R.id.menuItem_controllerProfiles:
                // TODO
                popupTodo();
                return true;
            case R.id.menuItem_customCheats:
                // TODO
                popupTodo();
                return true;
            case R.id.menuItem_faq:
                popupFaq();
                return true;
            case R.id.menuItem_helpForum:
                Utility.launchUri( MenuActivity.this, R.string.uri_forum );
                return true;
            case R.id.menuItem_controllerDiagnostics:
                startActivity( new Intent( this, DiagnosticActivity.class ) );
                return true;
            case R.id.menuItem_submitBugReport:
                Utility.launchUri( MenuActivity.this, R.string.uri_bugReport );
                return true;
            case R.id.menuItem_appVersion:
                popupAppVersion();
                return true;
            case R.id.menuItem_changelog:
                new ChangeLog( getAssets() ).show( MenuActivity.this, 0, mAppData.appVersionCode );
                return true;
            case R.id.menuItem_axisInfo:
                popupAxisInfo();
                return true;
            case R.id.menuItem_controllerInfo:
                popupControllerInfo();
                return true;
            case R.id.menuItem_deviceInfo:
                popupDeviceInfo();
                return true;
            case R.id.menuItem_credits:
                Utility.launchUri( MenuActivity.this, R.string.uri_credits );
                return true;
            default:
                return super.onMenuItemSelected( featureId, item );
        }
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
        String title = getString( R.string.menuItem_deviceInfo );
        String message = DeviceUtil.getCpuInfo();
        new Builder( this ).setTitle( title ).setMessage( message ).create().show();
    }
    
    private void popupAppVersion()
    {
        String title = getString( R.string.menuItem_appVersion );
        String message = getString( R.string.popup_version, mAppData.appVersion, mAppData.appVersionCode );
        new Builder( this ).setTitle( title ).setMessage( message ).create().show();
    }
    
    private void popupGameSettingsTodo()
    {
        Notifier.showToast( this, String.format( getString( R.string.toast_loadingGameSettings ), mRomDetail.baseName ) );
        
        new AsyncTask<Void, Void, RomDetail>()
        {
            private Bitmap art;
            
            @Override
            protected RomDetail doInBackground( Void... params )
            {
                String md5 = RomDetail.computeMd5( new File( mUserPrefs.selectedGame ) );
                RomDetail result = RomDetail.lookupByMd5( md5 );
                art = result.getCoverArt( true );
                return result;
            }
            
            @Override
            protected void onPostExecute( RomDetail result )
            {
                ImageView view = new ImageView( MenuActivity.this );
                if( art != null )
                    view.setImageBitmap( art );
                else
                    view.setImageResource( R.drawable.default_coverart );
                new Builder( MenuActivity.this ).setTitle( "TODO" ).setMessage( result.goodName ).setView( view )
                        .create().show();
            }
        }.execute();
    }
    
    private void popupTodo()
    {
        new Builder( this ).setMessage( "TODO" ).create().show();
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener( this );
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        mPrefs.registerOnSharedPreferenceChangeListener( this );
        refreshViews();
    }
    
    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
    {
        if( key.equals( LOCALE_OVERRIDE ) )
        {
            // Sometimes one preference change affects the hierarchy or layout of the views. In this
            // case it's easier just to restart the activity than try to figure out what to fix.
            // Examples:
            // * Restore the preference categories that were removed in refreshViews(...)
            // * Change the input mapping layout when Xperia Play touchpad en/disabled
            finish();
            startActivity( getIntent() );
        }
        else
        {
            // Just refresh the preference screens in place
            refreshViews();
        }
    }
    
    @TargetApi( 11 )
    @SuppressWarnings( "deprecation" )
    private void refreshViews()
    {
        // Refresh the preferences object
        mUserPrefs = new UserPrefs( this );
        mRomDetail = RomDetail.lookupByCrc( mUserPrefs.selectedGameHeader.crc );
        
        // Refresh the action bar
        if( AppData.IS_HONEYCOMB )
            invalidateOptionsMenu();
        
        // Update the summary text for the selected game
        File selectedGame = new File( mUserPrefs.selectedGame );
        PathPreference pp = (PathPreference) findPreference( PATH_SELECTED_GAME );
        if( pp != null )
            pp.setSummary( selectedGame.getName() );
    }
}
