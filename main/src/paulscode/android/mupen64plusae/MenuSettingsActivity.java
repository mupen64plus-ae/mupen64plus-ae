package paulscode.android.mupen64plusae;

import java.io.File;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;

// TODO: Comment thoroughly
public class MenuSettingsActivity extends PreferenceActivity
{
    public static MenuSettingsActivity mInstance = null;
    private Context ctx = this;
    

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Load preferences from XML
        addPreferencesFromResource( R.layout.preferences_base );
        
        mInstance = this;
        Globals.checkLocale( this );
        
        // Video Settings
        final Preference settingsVideo = findPreference( "menuSettingsVideo" );
        settingsVideo.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                Intent intent = new Intent( mInstance, MenuSettingsVideoActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });

        // Audio Settings
        final Preference settingsAudio = findPreference( "menuSettingsAudio" );
        settingsAudio.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                Intent intent = new Intent( mInstance, MenuSettingsAudioActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });
        
        // Input Settings
        final Preference settingsInput = findPreference( "menuSettingsInput" );
        settingsInput.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                Intent intent = new Intent( mInstance, MenuSettingsInputActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });
        
        // Virtual Gamepad Settings
        final Preference settingsGamepad = findPreference( "menuSkinsGamepad" );
        settingsGamepad.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                Intent intent = new Intent( mInstance, MenuSkinsGamepadActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });
        
        // RSP Settings
        final Preference settingsRSP = findPreference( "menuSettingsRSP" );
        settingsRSP.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                Intent intent = new Intent( mInstance, MenuSettingsRSPActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });
        
        // Core Settings
        final Preference settingsCore = findPreference( "menuSettingsCore" );
        settingsCore.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                Intent intent = new Intent( mInstance, MenuSettingsCoreActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });
        
        // Xperia Play Optimized Setting
        final CheckBoxPreference xperiaPlayOptimized = (CheckBoxPreference) findPreference( "menuSettingsXperiaPlayOptimized" );
        xperiaPlayOptimized.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                if (xperiaPlayOptimized.isChecked())
                {
                    MenuActivity.gui_cfg.put( "GAME_PAD", "enabled", "0" );  // Turn off the virtual gamepad
                    Globals.isXperiaPlay = true;
                }
                else
                {
                    MenuActivity.gui_cfg.put( "GAME_PAD", "enabled", "1" );  // Turn on the virtual gamepad
                    Globals.isXperiaPlay = false;
                }
                
                MenuActivity.gui_cfg.put( "TOUCH_PAD", "is_xperia_play", ( xperiaPlayOptimized.isChecked() ? "1" : "0" ) );
                return true;
            }
        });
        
        // Touchpad Settings
        final Preference settingsTouchpad = findPreference( "menuSkinsTouchpad" );
        settingsTouchpad.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                Intent intent = new Intent( mInstance, MenuSkinsTouchpadActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });
        
        // Reset Default Setting
        final Preference settingsResetDefaults = findPreference( "menuSettingsResetDefaults" );
        settingsResetDefaults.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                resetDefaults();
                return true;
            }
        });
        
        // Restore App Data Setting
        final Preference settingsRestoreAppData = findPreference( "menuSettingsRestoreAppData" );
        settingsRestoreAppData.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                showDialog( Globals.SURE_ID );
                return true;
            }
        });
        
        // Auto Save Setting
        final CheckBoxPreference settingsAutoSave = (CheckBoxPreference) findPreference( "menuSettingsAutoSave" );
        settingsAutoSave.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                if ( settingsAutoSave.isChecked() )
                    Globals.auto_save = true;
                else
                    Globals.auto_save = false;
                
                MenuActivity.gui_cfg.put( "GENERAL", "auto_save", ( settingsAutoSave.isChecked() ? "1" : "0") );
                return true;
            }
        });
    }
    
    // TODO: Remove this eventually and have it within its own listener
    // Method for resetting defaults
    private final void resetDefaults()
    {
        // Get the default preferences set for the PreferenceScreen
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences( ctx );
        // Clear any settings that are different than default (reset to defaults)
        sp.edit().clear().commit();
        
        if( !Updater.restoreDefaults( this ) )
        {
            Runnable toastMessager = new Runnable()
            {
                public void run()
                {
                    Toast toast = Toast.makeText( MenuActivity.mInstance, getString( R.string.settings_problem_resetting ), Toast.LENGTH_LONG );
                    toast.setGravity( Gravity.BOTTOM, 0, 0 );
                    toast.show();
                }
            };
            
            runOnUiThread( toastMessager );
            return;
        }
        
        mInstance.finish();
    }
   
    
    @Override
    protected Dialog onCreateDialog( int id )
    {
        switch( id )
        {
            case Globals.SURE_ID:
            {
                AlertDialog.Builder d = new AlertDialog.Builder( this );
                d.setTitle( getString( R.string.are_you_sure ) );
                d.setIcon( R.drawable.icon );
                d.setPositiveButton( getString( R.string.dialog_yes ),
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick( DialogInterface dialog, int which )
                        {
                            File appData = new File( Globals.DataDir );
                            Utility.copyFile( new File( Globals.DataDir + "/data/save" ),
                                              new File( Globals.StorageDir + "/mp64p_tmp_asdf1234lkjh0987/data/save" )  );
                            Utility.deleteFolder( appData );
                            Intent intent = new Intent( mInstance, MainActivity.class );
                            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                            startActivity( intent );
                            mInstance.finish();
                            MenuActivity.mInstance.finish();
                        }
                    });
                d.setNegativeButton( getString( R.string.dialog_no ), null );
                View v = LayoutInflater.from( this ).inflate( R.layout.about_dialog, null );
                TextView text = (TextView) v.findViewById( R.id.about_text );
                text.setText( getString( R.string.restore_info ) );
                d.setView( v );
                return d.create();
            }
        }
        return( super.onCreateDialog( id ) );
    }
}

