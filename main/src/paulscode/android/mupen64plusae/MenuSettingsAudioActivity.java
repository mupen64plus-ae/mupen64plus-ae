package paulscode.android.mupen64plusae;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;

// TODO: Comment thoroughly
public class MenuSettingsAudioActivity extends PreferenceActivity implements IOptionChooser
{
    public static MenuSettingsAudioActivity mInstance = null;
    public static String currentPlugin = "(none)";

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mInstance = this;

        Globals.checkLocale( this );

        currentPlugin = "(none)";

        String filename = MenuActivity.mupen64plus_cfg.get( "UI-Console", "AudioPlugin" );
        if( filename == null || filename.length() < 1 || filename.equals( "\"\"" ) || filename.equals( "\"dummy\"" ) )
            filename = MenuActivity.gui_cfg.get( "AUDIO_PLUGIN", "last_choice" );
        if( filename != null )
        {
            MenuActivity.gui_cfg.put( "AUDIO_PLUGIN", "last_choice", filename );
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
        addPreferencesFromResource( R.layout.preferences_audio );
        
        final Preference settingsAudioChange = findPreference( "menuSettingsAudioChange" );
        settingsAudioChange.setSummary( currentPlugin );
        settingsAudioChange.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                Intent intent = new Intent( mInstance, MenuSettingsAudioChangeActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });
        
        
        // Enable Plugin setting
        final CheckBoxPreference settingsEnableAudio = (CheckBoxPreference) findPreference( "menuSettingsAudioEnabled" );
        settingsEnableAudio.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                MenuActivity.gui_cfg.put( "AUDIO_PLUGIN", "enabled", (settingsEnableAudio.isChecked() ? "1" : "0") );
                MenuActivity.mupen64plus_cfg.put( "UI-Console", "AudioPlugin", (settingsEnableAudio.isChecked() ? MenuActivity.gui_cfg.get( "AUDIO_PLUGIN", "last_choice" ) : "\"dummy\"") );
                return true;
            }
        });
    }
    
    public void optionChosen( String option )
    {
        currentPlugin = "(none)";

        if( option != null )
        {
            MenuActivity.gui_cfg.put( "AUDIO_PLUGIN", "last_choice", "\"" + option + "\"" );
            MenuActivity.mupen64plus_cfg.put( "UI-Console", "AudioPlugin", "\"" + option + "\"" );
            int x = option.lastIndexOf( "/" );
            if( x > -1 && x < ( option.length() - 1 ) )
            {
                currentPlugin = option.substring( x + 1, option.length() );
                if( currentPlugin == null || currentPlugin.length() < 1 )
                    currentPlugin = "(none)";
            }
            else
                currentPlugin = option;
            
            // Make sure to update the summary description if the .so plugin is changed
            final Preference settingsAudioChange = findPreference( "menuSettingsAudioChange" );
            settingsAudioChange.setSummary( currentPlugin );
        }
    }
}
