package paulscode.android.mupen64plusae;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

// TODO: Comment thoroughly
public class MenuSettingsInputConfigureActivity extends PreferenceActivity
{
    public static MenuSettingsInputConfigureActivity mInstance = null;

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mInstance = this;

        Globals.checkLocale( this );
        
        // Load preferences from XML
        addPreferencesFromResource( R.layout.preferences_input_config_controllers );
        
        // Map Controller 1 Setting
        final Preference settingsInputMapControllerOne = findPreference( "menuSettingsInputConfigureController1" );
        settingsInputMapControllerOne.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                // Open the menu to map controller buttons
                MenuSettingsInputConfigureButtonsActivity.controllerNum = 1;
                Intent intent = new Intent( mInstance, MenuSettingsInputConfigureButtonsActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });
        
        // Map Controller 2 Setting
        final Preference settingsInputMapControllerTwo = findPreference( "menuSettingsInputConfigureController2" );
        settingsInputMapControllerTwo.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                // Open the menu to map controller buttons
                MenuSettingsInputConfigureButtonsActivity.controllerNum = 2;
                Intent intent = new Intent( mInstance, MenuSettingsInputConfigureButtonsActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });
        
        // Map Controller 3 Setting
        final Preference settingsInputMapControllerThree = findPreference( "menuSettingsInputConfigureController3" );
        settingsInputMapControllerThree.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                // Open the menu to map controller buttons
                MenuSettingsInputConfigureButtonsActivity.controllerNum = 3;
                Intent intent = new Intent( mInstance, MenuSettingsInputConfigureButtonsActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });
        
        // Map Controller 4 Setting
        final Preference settingsInputMapControllerFour = findPreference( "menuSettingsInputConfigureController4" );
        settingsInputMapControllerFour.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                // Open the menu to map controller buttons
                MenuSettingsInputConfigureButtonsActivity.controllerNum = 4;
                Intent intent = new Intent( mInstance, MenuSettingsInputConfigureButtonsActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });
        
        // Map Special Buttons Setting
        final Preference settingsInputMapSpecialButtons = findPreference( "menuSettingsInputConfigureCoreFunctions" );
        settingsInputMapSpecialButtons.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                // Open the menu to map controller buttons
                MenuSettingsInputConfigureButtonsActivity.controllerNum = -1;
                Intent intent = new Intent( mInstance, MenuSettingsInputConfigureButtonsActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });
    }
}
