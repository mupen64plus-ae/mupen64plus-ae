package paulscode.android.mupen64plusae;

import java.io.File; 

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.Gravity;
import android.widget.Toast;


// TODO: Comment thoroughly
public class MenuSettingsVideoConfigureActivity extends PreferenceActivity implements IOptionChooser
{
    public static MenuSettingsVideoConfigureActivity mInstance = null;
    private Config gles2n64_conf;

//    private String texturePack = null;

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mInstance = this;

        Globals.checkLocale( this );

        // TODO: standardize this, using the gui config.  This is too much of a hack.
        String filename = MenuActivity.mupen64plus_cfg.get( "UI-Console", "VideoPlugin" );
        if( filename != null )
        {
            filename = filename.replace( "\"", "" );
        }
        //

        gles2n64_conf = new Config( Globals.DataDir + "/data/gles2n64.conf" );

        addPreferencesFromResource( R.layout.preferences_video_configure );
        //////// RICE PLUGIN SETTINGS ////////
        
        
        // Enable Frameskip Setting
        final CheckBoxPreference settingsRiceSkipFrame = (CheckBoxPreference) findPreference( "menuSettingsVideoConfigureSkipFrame" );
        settingsRiceSkipFrame.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                MenuActivity.mupen64plus_cfg.put( "Video-Rice", "SkipFrame", (settingsRiceSkipFrame.isChecked() ? "1" : "0") );
                return true;
            }
        });
        
        // Enable Fast Texture CRC Setting
        final CheckBoxPreference settingsRiceFastTextureCRC = (CheckBoxPreference) findPreference( "menuSettingsVideoConfigureFastTextureCRC" );
        settingsRiceFastTextureCRC.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                MenuActivity.mupen64plus_cfg.put( "Video-Rice", "FastTextureCRC", (settingsRiceFastTextureCRC.isChecked() ? "1" : "0") );
                return true;
            }
        });
        
        // Enable Fast Texture Loading Setting
        final CheckBoxPreference settingsRiceFastTextureLoad = (CheckBoxPreference) findPreference( "menuSettingsVideoConfigureFastTexture" );
        settingsRiceFastTextureLoad.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                MenuActivity.mupen64plus_cfg.put( "Video-Rice", "FastTextureLoading", (settingsRiceFastTextureLoad.isChecked() ? "1" : "0") );
                return true;
            }
        });
        
        // Enable Hi-Res Textures Setting
        final CheckBoxPreference settingsRiceUseHiResTextures = (CheckBoxPreference) findPreference( "menuSettingsVideoConfigureUseHiResTextures" );
        settingsRiceUseHiResTextures.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                MenuActivity.mupen64plus_cfg.put( "Video-Rice", "LoadHiResTextures", (settingsRiceUseHiResTextures.isChecked() ? "1" : "0") );
                return true;
            }
        });
        
        // Import Hi-Res Textures Setting
        final Preference settingsRiceImportHiResTextures = findPreference( "menuSettingsVideoConfigureImportHiResTextures" );
        settingsRiceImportHiResTextures.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                String path = MenuActivity.gui_cfg.get( "LAST_SESSION", "texture_folder" );

                if( path == null || path.length() < 1 )
                    FileChooserActivity.startPath = Globals.StorageDir;
                else
                    FileChooserActivity.startPath = path;
                FileChooserActivity.extensions = ".zip";
                FileChooserActivity.parentMenu = mInstance;
                FileChooserActivity.function = FileChooserActivity.FUNCTION_TEXTURE;
                Intent intent = new Intent( mInstance, FileChooserActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });
        
        ////// GLES2N64 SETTINGS ///////
        
        // Stretch Screen Setting
        final CheckBoxPreference settingsVideoStretchScreen = (CheckBoxPreference) findPreference( "menuSettingsVideoConfigureStretch" );
        settingsVideoStretchScreen.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                Globals.screen_stretch = !Globals.screen_stretch;
                MenuActivity.gui_cfg.put( "VIDEO_PLUGIN", "screen_stretch", Globals.screen_stretch ? "1" : "0" );
                return true;
            }
        });
        
        // Auto-Frameskip Setting
        final CheckBoxPreference settingsVideoAutoFrameskip = (CheckBoxPreference) findPreference( "menuSettingsVideoConfigureAutoFrameskip" );
        settingsVideoAutoFrameskip.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                Globals.auto_frameskip = !Globals.auto_frameskip;
                MenuActivity.gui_cfg.put( "VIDEO_PLUGIN", "auto_frameskip", Globals.auto_frameskip ? "1" : "0" );
                return true;
            }
        });
        
        // Max Frameskip Setting
        final ListPreference settingsVideoMaxFrameskip = (ListPreference) findPreference( "menuSettingsVideoConfigureMaxFrameskip" );
        settingsVideoMaxFrameskip.setOnPreferenceChangeListener( new OnPreferenceChangeListener() {
            
            public boolean onPreferenceChange( Preference preference, Object newValue )
            {   
                Globals.max_frameskip = Integer.parseInt( String.valueOf( newValue ) );
                MenuActivity.gui_cfg.put( "VIDEO_PLUGIN", "max_frameskip", String.valueOf( newValue ) );
                return true;
            }
        });
        
        // Enable Fog Setting
        final CheckBoxPreference settingsVideoEnableFog = (CheckBoxPreference) findPreference( "menuSettingsVideoConfigureFog" );
        settingsVideoEnableFog.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                gles2n64_conf.put( "[<sectionless!>]", "enable fog", (settingsVideoEnableFog.isChecked() ? "1" : "0") );
                gles2n64_conf.save();
                return true;
            }
        });
        
        // Force Screen Clear Setting
        final CheckBoxPreference settingsVideoForceScreenClear = (CheckBoxPreference) findPreference( "menuSettingsVideoConfigureScreenClear" );
        settingsVideoForceScreenClear.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                gles2n64_conf.put( "[<sectionless!>]", "force screen clear", (settingsVideoForceScreenClear.isChecked() ? "1" : "0") );
                gles2n64_conf.save();
                return true;
            }
        });
        
        // Alpha Test Setting
        final CheckBoxPreference settingsVideoConfigureAlpha = (CheckBoxPreference) findPreference( "menuSettingsVideoConfigureAlpha" );
        settingsVideoConfigureAlpha.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                gles2n64_conf.put( "[<sectionless!>]", "enable alpha test", (settingsVideoConfigureAlpha.isChecked() ? "1" : "0") );
                gles2n64_conf.save();
                return true;
            }
        });
        
        // Hack Z Setting
        final CheckBoxPreference settingsVideoHackZ = (CheckBoxPreference) findPreference( "menuSettingsVideoConfigureHackZ" );
        settingsVideoHackZ.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                gles2n64_conf.put( "[<sectionless!>]", "hack z", (settingsVideoHackZ.isChecked() ? "1" : "0") );
                gles2n64_conf.save();
                return true;
            }
        });
        
        // 2xSai Texture Filter Setting
        final CheckBoxPreference settingsVideo2xSai = (CheckBoxPreference) findPreference( "menuSettingsVideo2xSai" );
        settingsVideo2xSai.setOnPreferenceClickListener( new OnPreferenceClickListener() {
            
            public boolean onPreferenceClick( Preference preference )
            {
                gles2n64_conf.put( "[<sectionless!>]", "texture 2xSAI", (settingsVideo2xSai.isChecked() ? "1" : "0") );
                gles2n64_conf.save();
                return true;
            }
        });
    }
 
    public void optionChosen( String option )
    {
        String headerName = Utility.getTexturePackName( option );
        if( headerName == null )
        {
            if( Globals.errorMessage != null && Globals.errorMessage.length() > 0 )
                showToast( Globals.errorMessage );
            Globals.errorMessage = null;
            return;
        }
        String outputFolder = Globals.DataDir + "/data/hires_texture/" + headerName;
        Utility.deleteFolder( new File( outputFolder ) );
        Utility.unzipAll( new File( option ), outputFolder );
    }

    private void showToast( String message )
    {
        final String toastMessage = message;
        Runnable toastMessager = new Runnable()
        {
            public void run()
            {
                Toast toast = Toast.makeText( mInstance, toastMessage, Toast.LENGTH_LONG );
                toast.setGravity( Gravity.BOTTOM, 0, 0 );
                toast.show();
            }
        };
        this.runOnUiThread( toastMessager );
    }
}
