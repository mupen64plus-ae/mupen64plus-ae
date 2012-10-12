package paulscode.android.mupen64plusae;

import java.io.File;

import paulscode.android.mupen64plusae.preference.Config;
import paulscode.android.mupen64plusae.preference.FilePreference;
import paulscode.android.mupen64plusae.preference.Settings;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

// Use some deprecated functions to simplify backwards-compatibility
@SuppressWarnings( "deprecation" )
public class MenuActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
    public static MenuActivity mInstance = null;
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mInstance = this;
        
        // Load preferences from XML and update view
        addPreferencesFromResource( R.xml.preferences );
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences( this );
        Settings.refreshUser( sharedPreferences );
        refreshPreferenceSummaries( sharedPreferences );
        
        NotificationManager notificationManager = (NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE );
        notificationManager.cancel( Settings.NOTIFICATION_ID );
        
        // Load the legacy configuration files
        Settings.mupen64plus_cfg = new Config( Settings.paths.mupen64plus_cfg );
        Settings.gui_cfg = new Config( Settings.paths.gui_cfg );
        Settings.error_log = new Config( Settings.paths.error_log );
        
        Updater.checkFirstRun( this );
        if( !Updater.checkv1_9( this ) )
        {
            finish();
            return;
        }
        Updater.checkCfgVer( this );
        
        findPreference( "menuResume" ).setOnPreferenceClickListener(
                new OnPreferenceClickListener()
                {
                    public boolean onPreferenceClick( Preference preference )
                    {
                        // Resume the last game
                        File f = new File( Settings.paths.storageDir );
                        if( !f.exists() )
                        {
                            Log.e( "MenuActivity",
                                    "SD Card not accessable in method onListItemClick (menuResume)" );
                            Runnable toastMessager = new Runnable()
                            {
                                public void run()
                                {
                                    Toast toast = Toast
                                            .makeText(
                                                    MenuActivity.mInstance,
                                                    "App data not accessible (cable plugged in \"USB Mass Storage Device\" mode?)",
                                                    Toast.LENGTH_LONG );
                                    toast.setGravity( Gravity.BOTTOM, 0, 0 );
                                    toast.show();
                                }
                            };
                            runOnUiThread( toastMessager );
                            return true;
                        }
                        Settings.mupen64plus_cfg.save();
                        Settings.gui_cfg.save();
                        Settings.chosenROM = Settings.gui_cfg.get( "LAST_SESSION", "rom" );
                        GameActivityCommon.resumeLastSession = true;
                        
                        Intent intent;
                        if( Settings.user.xperiaEnabled )
                            intent = new Intent( mInstance, GameActivityXperiaPlay.class );
                        else
                            intent = new Intent( mInstance, GameActivity.class );
                        
                        intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                        startActivity( intent );
                        mInstance.finish();
                        mInstance = null;
                        return true;
                    }
                } );
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences( this )
                .registerOnSharedPreferenceChangeListener( this );
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences( this )
                .unregisterOnSharedPreferenceChangeListener( this );
    }
    
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
    {
        // Update the global convenience class
        Settings.refreshUser( sharedPreferences );
        refreshPreferenceSummaries( sharedPreferences );
    }
    
    private void refreshPreferenceSummaries( SharedPreferences sharedPreferences )
    {
        for( String key : sharedPreferences.getAll().keySet() )
        {
            // Update the summary text in the menu for list preferences
            // Inspired by http://stackoverflow.com/a/531927/254218
            Preference preference = findPreference( key );
            if( preference instanceof FilePreference )
            {
                ( (FilePreference) preference ).refreshItems();
            }
            if( preference instanceof ListPreference )
            {
                preference.setSummary( ( (ListPreference) preference ).getEntry() );
            }
        }
    }
}
