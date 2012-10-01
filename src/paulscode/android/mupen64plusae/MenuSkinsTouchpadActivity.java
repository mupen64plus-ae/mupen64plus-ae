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
public class MenuSkinsTouchpadActivity extends PreferenceActivity implements IOptionChooser
{
    public static MenuSkinsTouchpadActivity mInstance = null;
    public static String chosenTouchpad = "";
    public static boolean analogAsOctagon = true;
    public static boolean enabled = true;

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mInstance = this;

        chosenTouchpad = MenuActivity.gui_cfg.get( "TOUCH_PAD", "which_pad" );

        if( chosenTouchpad == null || chosenTouchpad.length() < 1 )
        {
            DataInputStream in = null;
            BufferedReader br = null;
            try
            {
                FileInputStream fstream = new FileInputStream( Globals.DataDir + "/skins/touchpads/touchpad_list.ini" );
                in = new DataInputStream( fstream );
                br = new BufferedReader( new InputStreamReader( in ) );
                chosenTouchpad = br.readLine();
            }
            catch( Exception e )
            {
                Log.e( "MenuSkinsTouchpadActivity", "Problem reading touchpad list, message: " + e.getMessage() );
            }
            try
            {
                if( br != null )
                    br.close();
            }
            catch( Exception e )
            {
                Log.e( "MenuSkinsTouchpadActivity", "Problem closing touchpad list reader, error message: " + e.getMessage() );
            }
            try
            {
                if( in != null )
                    in.close();
            }
            catch( Exception e )
            {
                Log.e( "MenuSkinsTouchpadActivity", "Problem closing touchpad list file, error message: " + e.getMessage() );
            }
        }

        // Load preferences from XML
        addPreferencesFromResource( R.layout.preferences_touchpad );
        
        // Change Layout Setting
        final Preference skinsTouchpadChange = findPreference( "menuSkinsTouchpadChange" );
        skinsTouchpadChange.setSummary( chosenTouchpad );
        skinsTouchpadChange.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                Intent intent = new Intent( mInstance, MenuSkinsTouchpadChangeActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });
        
        
        // Accurate N64 Stick Setting
        final CheckBoxPreference skinsTouchpadOctagon = (CheckBoxPreference) findPreference( "menuSkinsTouchpadOctagon" );
        skinsTouchpadOctagon.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                analogAsOctagon = !analogAsOctagon;
                MenuActivity.gui_cfg.put( "TOUCH_PAD", "analog_octagon", skinsTouchpadOctagon.isChecked() ? "1" : "0" );
                return true;
            }
        });
        
        
        // Touchpad Enable Setting
        final CheckBoxPreference skinsTouchpadEnable = (CheckBoxPreference) findPreference( "menuSkinsTouchpadEnabled" );
        skinsTouchpadOctagon.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                enabled = !enabled;
                MenuActivity.gui_cfg.put( "TOUCH_PAD", "enabled", skinsTouchpadEnable.isChecked() ? "1" : "0" );
                return true;
            }
        });
    }
    
    
    public void optionChosen( String option )
    {
        chosenTouchpad = option;
        MenuActivity.gui_cfg.put( "TOUCH_PAD", "which_pad", chosenTouchpad );
        
        // Update the summary description for the Change setting when a different layout is chosen
        final Preference skinsTouchpadChange = findPreference( "menuSkinsTouchpadChange" );
        skinsTouchpadChange.setSummary( chosenTouchpad );
    }
}

