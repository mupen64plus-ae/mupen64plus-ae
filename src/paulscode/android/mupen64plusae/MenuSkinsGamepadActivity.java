package paulscode.android.mupen64plusae;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;

// TODO: Comment thoroughly
public class MenuSkinsGamepadActivity extends PreferenceActivity implements IOptionChooser
{
    public static MenuSkinsGamepadActivity mInstance = null;
    public static String chosenGamepad = "";
    public static boolean redrawAll = true;
    public static boolean analogAsOctagon = true;
    public static boolean showFPS = false;
    public static boolean enabled = true;

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mInstance = this;

        Globals.checkLocale( this );

        chosenGamepad = MenuActivity.gui_cfg.get( "GAME_PAD", "which_pad" );

        if( chosenGamepad == null || chosenGamepad.length() < 1 )
        {
            DataInputStream in = null;
            BufferedReader br = null;
            try
            {
                FileInputStream fstream = new FileInputStream( Globals.DataDir + "/skins/gamepads/gamepad_list.ini" );
                in = new DataInputStream( fstream );
                br = new BufferedReader( new InputStreamReader( in ) );
                chosenGamepad = br.readLine();
            }
            catch( Exception e )
            {
                Log.e( "MenuSkinsGamepadActivity", "Problem reading gamepad list, message: " + e.getMessage() );
            }
            
            try
            {
                if( in != null )
                    in.close();
            }
            catch( Exception e )
            {
                Log.e( "MenuSkinsGamepadActivity", "Problem closing gamepad list file, error message: " + e.getMessage() );
            }
        }

        // Load preferences from XML
        addPreferencesFromResource( R.layout.preferences_gamepad );
        
        // Change Layout Setting
        final Preference skinsGamepadChange = findPreference( "menuSkinsGamepadChange" );
        skinsGamepadChange.setSummary( chosenGamepad );
        skinsGamepadChange.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                Intent intent = new Intent( mInstance, MenuSkinsGamepadChangeActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });
        
        
        // Redraw All Setting
        final CheckBoxPreference settingsGamepadRedraw = (CheckBoxPreference) findPreference( "menuSkinsGamepadRedraw" );
        settingsGamepadRedraw.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                redrawAll = !redrawAll;
                MenuActivity.gui_cfg.put( "GAME_PAD", "redraw_all", settingsGamepadRedraw.isChecked() ? "1" : "0" );
                return true;
            }
        });
        
        
        // Accurate N64 Stick Setting
        final CheckBoxPreference settingsGamepadOctagon = (CheckBoxPreference) findPreference( "menuSkinsGamepadOctagon" );
        settingsGamepadOctagon.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                analogAsOctagon = !analogAsOctagon;
                MenuActivity.gui_cfg.put( "GAME_PAD", "analog_octagon", settingsGamepadOctagon.isChecked() ? "1" : "0" );
                return true;
            }
        });
        
        
        // Display FPS Setting
        final CheckBoxPreference settingsGamepadFPS = (CheckBoxPreference) findPreference( "menuSkinsGamepadFPS" );
        settingsGamepadFPS.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                showFPS = !showFPS;
                MenuActivity.gui_cfg.put( "GAME_PAD", "show_fps", settingsGamepadFPS.isChecked() ? "1" : "0" );
                return true;
            }
        });
        
        
        // Enable Virtual Gamepad Setting
        final CheckBoxPreference settingsGamepadEnabled = (CheckBoxPreference) findPreference( "menuSkinsGamepadEnabled" );
        settingsGamepadEnabled.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                enabled = !enabled;
                MenuActivity.gui_cfg.put( "GAME_PAD", "enabled", settingsGamepadEnabled.isChecked() ? "1" : "0" );
                return true;
            }
        });
    }
    
    public void optionChosen( String option )
    {
        chosenGamepad = option;
        MenuActivity.gui_cfg.put( "GAME_PAD", "which_pad", chosenGamepad );
        
        // Update the summary description for the Change Layout setting when a different layout is chosen
        final Preference skinsGamepadChange = findPreference( "menuSkinsGamepadChange" );
        skinsGamepadChange.setSummary( chosenGamepad );
    }
}

