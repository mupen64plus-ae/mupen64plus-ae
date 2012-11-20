package paulscode.android.mupen64plusae;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

// TODO: Comment thoroughly
public class MenuSettingsRSPActivity extends PreferenceActivity implements IOptionChooser
{
    public static MenuSettingsRSPActivity mInstance = null;
    public static String currentPlugin = "(none)";

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mInstance = this;

        Globals.checkLocale( this );

        currentPlugin = "(none)";

        String filename = MenuActivity.mupen64plus_cfg.get( "UI-Console", "RspPlugin" );
        if( filename == null || filename.length() < 1 || filename.equals( "\"\"" ) || filename.equals( "\"dummy\"" ) )
            filename = MenuActivity.gui_cfg.get( "RSP_PLUGIN", "last_choice" );
        if( filename != null )
        {
            MenuActivity.gui_cfg.put( "RSP_PLUGIN", "last_choice", filename );
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
        addPreferencesFromResource( R.layout.preferences_rsp );
        
        // Change RSP Plugin Setting
        final Preference settingsChangeRSP = findPreference( "menuSettingsRSPChange" );
        settingsChangeRSP.setSummary( currentPlugin );
        settingsChangeRSP.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                // Open the menu to choose a plugin
                Intent intent = new Intent( mInstance, MenuSettingsRSPChangeActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });
        
        // Enable RSP Plugin Setting
        final CheckBoxPreference settingsRSPEnabled = (CheckBoxPreference) findPreference( "menuSettingsRSPEnabled" );
        settingsRSPEnabled.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                MenuActivity.gui_cfg.put( "RSP_PLUGIN", "enabled", (settingsRSPEnabled.isChecked() ? "1" : "0") );
                MenuActivity.mupen64plus_cfg.put( "UI-Console", "RspPlugin",
                    (settingsRSPEnabled.isChecked() ? MenuActivity.gui_cfg.get( "RSP_PLUGIN", "last_choice" ) : "\"dummy\"") );
                return true;
            }
        });
    }
    
    
    public void optionChosen( String option )
    {
        currentPlugin = "(none)";

        if( option != null )
        {
            String plugin = option.replace( "$libsDir", Globals.LibsDir + "/lib" );
            MenuActivity.gui_cfg.put( "RSP_PLUGIN", "last_choice", "\"" + plugin + "\"" );
            MenuActivity.mupen64plus_cfg.put( "UI-Console", "RspPlugin", "\"" + plugin + "\"" );
            int x = plugin.lastIndexOf( "/" );
            if( x > -1 && x < ( plugin.length() - 1 ) )
            {
                currentPlugin = plugin.substring( x + 1, plugin.length() );
                if( currentPlugin == null || currentPlugin.length() < 1 )
                    currentPlugin = "(none)";
            }
            else
                currentPlugin = plugin;
            
            // Make sure we update the summary description if the .so plugin is changed
            final Preference settingsChangeRSP = findPreference( "menuSettingsRSPChange" );
            settingsChangeRSP.setSummary( currentPlugin );
        }
    }
}
