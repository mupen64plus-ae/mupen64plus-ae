package paulscode.android.mupen64plusae;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

// TODO: Comment thoroughly
public class MenuSettingsInputActivity extends PreferenceActivity implements IOptionChooser
{
    public static MenuSettingsInputActivity mInstance = null;
    public static String currentPlugin = "(none)";

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mInstance = this;

        Globals.checkLocale( this );
        currentPlugin = "(none)";
        
        String filename = MenuActivity.mupen64plus_cfg.get( "UI-Console", "InputPlugin" );
        if( filename == null || filename.length() < 1 || filename.equals( "\"\"" ) || filename.equals( "\"dummy\"" ) )
            filename = MenuActivity.gui_cfg.get( "INPUT_PLUGIN", "last_choice" );
        
        if( filename != null )
        {
            MenuActivity.gui_cfg.put( "INPUT_PLUGIN", "last_choice", filename );
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
        addPreferencesFromResource( R.layout.preferences_input );
        
        // Change Input Plugin Setting
        final Preference settingsChangeInput = findPreference( "menuSettingsInputChange" );
        settingsChangeInput.setSummary( currentPlugin );
        settingsChangeInput.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
             // Open the menu to choose a plugin
                Intent intent = new Intent( mInstance, MenuSettingsInputChangeActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });
        
        // Map Buttons Setting
        final Preference settingsInputConfigure = findPreference( "menuSettingsInputConfigure" );
        settingsInputConfigure.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
             // Open the menu to choose a plugin
                Intent intent = new Intent( mInstance, MenuSettingsInputConfigureActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });
        
        // Enable Plugin Setting
        final CheckBoxPreference settingsInputEnable = (CheckBoxPreference) findPreference( "menuSettingsInputEnabled" );
        settingsInputEnable.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                MenuActivity.gui_cfg.put( "INPUT_PLUGIN", "enabled", (settingsInputEnable.isChecked() ? "1" : "0") );
                MenuActivity.mupen64plus_cfg.put( "UI-Console", "InputPlugin", (settingsInputEnable.isChecked() 
                        ? MenuActivity.gui_cfg.get( "INPUT_PLUGIN", "last_choice" ) : "\"dummy\"") );
                return true;
            }
        });
    }

    public void optionChosen( String option )
    {
        currentPlugin = "(none)";

        if( option != null )
        {
            MenuActivity.gui_cfg.put( "INPUT_PLUGIN", "last_choice", "\"" + option + "\"" );
            MenuActivity.mupen64plus_cfg.put( "UI-Console", "InputPlugin", "\"" + option + "\"" );
            int x = option.lastIndexOf( "/" );
            if( x > -1 && x < ( option.length() - 1 ) )
            {
                currentPlugin = option.substring( x + 1, option.length() );
                if( currentPlugin == null || currentPlugin.length() < 1 )
                    currentPlugin = "(none)";
            }
            else
                currentPlugin = option;
            
            // Make sure to update the summary description if the .so plugin is changed.
            final Preference settingsChangeInput = findPreference( "menuSettingsInputChange" );
            settingsChangeInput.setSummary( currentPlugin );
        }
    }
}
