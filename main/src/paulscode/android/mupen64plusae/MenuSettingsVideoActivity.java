package paulscode.android.mupen64plusae;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;

// TODO: Comment thoroughly
public class MenuSettingsVideoActivity extends PreferenceActivity implements IOptionChooser
{
    public static MenuSettingsVideoActivity mInstance = null;
    public static String currentPlugin = "(none)";
    public static boolean rgba8888 = false;

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mInstance = this;

        Globals.checkLocale( this );

        currentPlugin = "(none)";

        String filename = MenuActivity.mupen64plus_cfg.get( "UI-Console", "VideoPlugin" );
        if( filename == null || filename.length() < 1 || filename.equals( "\"\"" ) || filename.equals( "\"dummy\"" ) )
            filename = MenuActivity.gui_cfg.get( "VIDEO_PLUGIN", "last_choice" );
        if( filename != null )
        {
            MenuActivity.gui_cfg.put( "VIDEO_PLUGIN", "last_choice", filename );
            filename = filename.replace( "\"", "" );
            int x = filename.lastIndexOf( "/" );
            if( x > -1 && x < (filename.length() - 1) )
            {
                currentPlugin = filename.substring( x + 1, filename.length() );
                if( currentPlugin == null || currentPlugin.length() < 1 )
                    currentPlugin = "(none)";
            }
        }
        
        // Load preferences from XML
        addPreferencesFromResource( R.layout.preferences_video );
        
        // Change Video Plugin Setting
        final Preference settingsChangeVideo = findPreference( "menuSettingsVideoChange" );
        settingsChangeVideo.setSummary( currentPlugin );
        settingsChangeVideo.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
             // Open the menu to choose a plug-in
                Intent intent = new Intent( mInstance, MenuSettingsVideoChangeActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });
        
        // Configure Video Plugin Setting
        final Preference settingsConfigureVideo = findPreference( "menuSettingsVideoConfigure" );
        settingsConfigureVideo.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                // Open the menu to configure video plug-ins
                Intent intent = new Intent( mInstance, MenuSettingsVideoConfigureActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });
        
        // RGBA-8888 Mode Setting
        final CheckBoxPreference settingsVideoRGBA8888 = (CheckBoxPreference) findPreference( "menuSettingsVideoRGBA8888" );
        settingsVideoRGBA8888.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                rgba8888 = !rgba8888;
                MenuActivity.gui_cfg.put( "VIDEO_PLUGIN", "rgba8888", (settingsVideoRGBA8888.isChecked() ? "1" : "0") );
                return true;
            }
        });
        
        // Enable Plugin Setting
        final CheckBoxPreference settingsVideoEnabled = (CheckBoxPreference) findPreference( "menuSettingsVideoEnabled" );
        settingsVideoEnabled.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                MenuActivity.gui_cfg.put( "VIDEO_PLUGIN", "enabled", (settingsVideoEnabled.isChecked() ? "1" : "0") );
                MenuActivity.mupen64plus_cfg.put( "UI-Console", "VideoPlugin",
                    (settingsVideoEnabled.isChecked() ? MenuActivity.gui_cfg.get( "VIDEO_PLUGIN", "last_choice" ) : "\"dummy\"") );
                return true;
            }
        });
    }

    public void optionChosen( String option )
    {
        currentPlugin = "(none)";

        if( option != null )
        {
            MenuActivity.gui_cfg.put( "VIDEO_PLUGIN", "last_choice", "\"" + option + "\"" );
            MenuActivity.mupen64plus_cfg.put( "UI-Console", "VideoPlugin", "\"" + option + "\"" );
            int x = option.lastIndexOf( "/" );
            if( x > -1 && x < ( option.length() - 1 ) )
            {
                currentPlugin = option.substring( x + 1, option.length() );
                if( currentPlugin == null || currentPlugin.length() < 1 )
                    currentPlugin = "(none)";
            }
            else
                currentPlugin = option;
            
            // Remember to update the summary description if the .so plugin is changed
            final Preference settingsChangeVideo = findPreference( "menuSettingsVideoChange" );
            settingsChangeVideo.setSummary( currentPlugin );
        }
    }
}
