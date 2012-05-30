package paulscode.android.mupen64plusae;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

// TODO: Comment thoroughly
public class MenuSettingsCoreActivity extends PreferenceActivity implements IOptionChooser
{
    public static MenuSettingsCoreActivity mInstance = null;

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mInstance = this;
        Globals.checkLocale( this );
        
        // Load preferences from XML
        addPreferencesFromResource( R.layout.preferences_core );
        
        // Change Core setting
        final Preference settingsChangeCore = findPreference( "menuSettingsCoreChanged" );
        settingsChangeCore.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
             // Open the menu to choose a core
                Intent intent = new Intent( mInstance, MenuSettingsCoreChangeActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });
    }

    public void optionChosen( String option )
    {
        //TODO: implement
/*
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
        }
*/
    }
}
