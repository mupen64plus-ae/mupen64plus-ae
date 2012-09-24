package paulscode.android.mupen64plusae;

import java.io.File;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

// TODO: Comment thoroughly
public class MenuActivity extends PreferenceActivity implements IOptionChooser
{
    public static MenuActivity mInstance = null;
//    private OptionArrayAdapter optionArrayAdapter;  // Array of menu options
    private static NotificationManager notificationManager = null;

    public static Config mupen64plus_cfg = new Config( Globals.DataDir + "/mupen64plus.cfg" );
    public static Config gui_cfg = new Config( Globals.DataDir + "/data/gui.cfg" );
    public static Config error_log = new Config( Globals.DataDir + "/error.log" );

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mInstance = this;

        Globals.checkLocale( this );

        if( notificationManager == null )
            notificationManager = (NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE );
        notificationManager.cancel( Globals.NOTIFICATION_ID );

        if( Globals.DataDir == null || Globals.DataDir.length() == 0 || !Globals.DataDirChecked ) //NOTE: isEmpty() not supported on some devices
        {
            Globals.PackageName = getPackageName();
            Globals.LibsDir = "/data/data/" + Globals.PackageName;
	        Globals.StorageDir = Globals.DownloadToSdcard ?
                    Environment.getExternalStorageDirectory().getAbsolutePath() : getFilesDir().getAbsolutePath();

	        Globals.DataDir = Globals.DownloadToSdcard ?
                    Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" +
            Globals.PackageName : getFilesDir().getAbsolutePath();
         
            Globals.DataDirChecked = true;

            mupen64plus_cfg = new Config( Globals.DataDir + "/mupen64plus.cfg" );
            gui_cfg = new Config( Globals.DataDir + "/data/gui.cfg" );
            error_log = new Config( Globals.DataDir + "/error.log" );
        }

        Updater.checkFirstRun( this );
        if( !Updater.checkv1_9( this ) )
        {
            finish();
            return;
        }
        Updater.checkCfgVer( this );

        String val = gui_cfg.get( "GAME_PAD", "redraw_all" );
        if( val != null )
            MenuSkinsGamepadActivity.redrawAll = ( val.equals( "1" ) ? true : false );
        val = gui_cfg.get( "KEYS", "disable_volume_keys" );
        if( val != null )  // Remember the choice that was made about the volume keys
            Globals.volumeKeysDisabled = val.equals( "1" ) ? true : false;
        val = gui_cfg.get( "VIDEO_PLUGIN", "screen_stretch" );
        if( val != null )
            Globals.screen_stretch = ( val.equals( "1" ) ? true : false );
        val = gui_cfg.get( "VIDEO_PLUGIN", "auto_frameskip" );
        if( val != null )
            Globals.auto_frameskip = ( val.equals( "1" ) ? true : false );
        val = gui_cfg.get( "VIDEO_PLUGIN", "max_frameskip" );
        if( val != null )
        {
            try
            {  // Make sure a valid integer was entered
                Globals.max_frameskip = Integer.valueOf( val );
            }
            catch( NumberFormatException nfe )
            {}  // Skip it if this happens
        }
        val = gui_cfg.get( "GENERAL", "auto_save" );
        if( val != null )
            Globals.auto_save = ( val.equals( "1" ) ? true : false );

        val = gui_cfg.get( "TOUCH_PAD", "is_xperia_play" );
        if( val != null )
            Globals.isXperiaPlay = ( val.equals( "1" ) ? true : false );

        // Load preferences from XML
        addPreferencesFromResource( R.layout.preferences_menu );
        
        final Preference menuOpenROM = findPreference( "menuOpenROM" );
        menuOpenROM.setOnPreferenceClickListener( new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick( Preference preference )
            {   // Open the file chooser to pick a ROM
                String path = gui_cfg.get( "LAST_SESSION", "rom_folder" );

                if( path == null || path.length() < 1 )
                    FileChooserActivity.startPath = Globals.DataDir;
                else
                    FileChooserActivity.startPath = path;
                FileChooserActivity.extensions = ".z64.v64.n64.zip";
                FileChooserActivity.parentMenu = null;
                FileChooserActivity.function = FileChooserActivity.FUNCTION_ROM;
                Intent intent = new Intent( mInstance, FileChooserActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });
        
        final Preference menuResume = findPreference( "menuResume" );
        menuResume.setOnPreferenceClickListener( new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick( Preference preference )
            {   // Resume the last game
                File f = new File( Globals.StorageDir );
                if( !f.exists() )
                {
                    Log.e( "MenuActivity", "SD Card not accessable in method onListItemClick (menuResume)" );
                    Runnable toastMessager = new Runnable()
                    {
                        public void run()
                        {
                            Toast toast = Toast.makeText( MenuActivity.mInstance, "App data not accessible (cable plugged in \"USB Mass Storage Device\" mode?)", Toast.LENGTH_LONG );
                            toast.setGravity( Gravity.BOTTOM, 0, 0 );
                            toast.show();
                        }
                    };
                    runOnUiThread( toastMessager );
                    return true;
                }
                mupen64plus_cfg.save();
                gui_cfg.save();
                Globals.chosenROM = gui_cfg.get( "LAST_SESSION", "rom" );
                GameActivityCommon.resumeLastSession = true;

                Intent intent;
                if( Globals.isXperiaPlay )
                    intent = new Intent( mInstance, GameActivityXperiaPlay.class );
                else
                    intent = new Intent( mInstance, GameActivity.class );

                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                mInstance.finish();
                mInstance = null;
                return true;
            }
        });

        final Preference menuSettings = findPreference( "menuSettings" );
        menuSettings.setOnPreferenceClickListener( new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick( Preference preference )
            {   // Configure the plug-ins
                Intent intent = new Intent( mInstance, MenuSettingsActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });

        final Preference menuHelp = findPreference( "menuHelp" );
        menuHelp.setOnPreferenceClickListener( new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick( Preference preference )
            {   // Visit the FAQ page (opens browser)
                try
                {
                    Intent browserIntent = new Intent( Intent.ACTION_VIEW, Uri.parse( "http://www.paulscode.com/forum/index.php?topic=197.msg3018#msg3018" ) );
                    startActivity( browserIntent );
                }
                catch( Exception e )
                {
                    Log.e( "MenuActivity", "Unable to open the FAQ page", e );
                    Runnable toastMessager = new Runnable()
                    {
                        public void run()
                        {
                            Toast toast = Toast.makeText( MenuActivity.mInstance, "Problem opening the browser, please report at paulscode.com", Toast.LENGTH_LONG );
                            toast.setGravity( Gravity.BOTTOM, 0, 0 );
                            toast.show();
                        }
                    };
                    runOnUiThread( toastMessager );
                }
                return true;
            }
        });

        final Preference menuCredits = findPreference( "menuCredits" );
        menuCredits.setOnPreferenceClickListener( new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick( Preference preference )
            {  // Show the credits
                showDialog( Globals.ABOUT_ID );
                return true;
            }
        });

        final Preference menuCheats = findPreference( "menuCheats" );
        menuCheats.setOnPreferenceClickListener( new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick( Preference preference )
            {   // Open the file chooser to pick a ROM
                String path = gui_cfg.get( "LAST_SESSION", "rom_folder" );

                if( path == null || path.length() < 1 )
                    FileChooserActivity.startPath = Globals.DataDir;
                else
                    FileChooserActivity.startPath = path;
                FileChooserActivity.extensions = ".z64.v64.n64.zip";
                FileChooserActivity.parentMenu = MenuActivity.this;
                FileChooserActivity.function = FileChooserActivity.FUNCTION_ROM;
                Intent intent = new Intent( mInstance, FileChooserActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });

        final Preference menuSettingsLanguage = findPreference( "menuSettingsLanguage" );
        menuSettingsLanguage.setOnPreferenceClickListener( new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick( Preference preference )
            {
                Intent intent = new Intent( mInstance, MenuSettingsLanguageActivity.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                startActivity( intent );
                return true;
            }
        });

        final Preference menuClose = findPreference( "menuClose" );
        menuClose.setOnPreferenceClickListener( new OnPreferenceClickListener()
        {
            public boolean onPreferenceClick( Preference preference )
            {   // Shut down the app
                File f = new File( Globals.StorageDir );
                if( f.exists() )
                {
                    mupen64plus_cfg.save();
                    gui_cfg.save();
                }
                mInstance.finish();
                return true;
            }
        });
        
        Globals.errorMessage = error_log.get( "OPEN_ROM", "fail_crash" );
        if( Globals.errorMessage != null && Globals.errorMessage.length() > 0 )
        {
            Runnable toastMessager = new Runnable()
            {
                public void run()
                {
                    Toast toast = Toast.makeText( MenuActivity.mInstance, Globals.errorMessage, Toast.LENGTH_LONG );
                    toast.setGravity( Gravity.BOTTOM, 0, 0 );
                    toast.show();
                }
            };
            runOnUiThread( toastMessager );
        }
        error_log.put( "OPEN_ROM", "fail_crash", "" );
        error_log.save();
        Globals.errorMessage = null;
    }
    
    @Override
    public boolean onKeyDown( int keyCode, KeyEvent event )
    {
        if( keyCode == KeyEvent.KEYCODE_BACK )
            return true;
        return false;
    }
    @Override
    public boolean onKeyUp( int keyCode, KeyEvent event )
    {
        if( keyCode == KeyEvent.KEYCODE_BACK )
            return true;
        return false;
    }

    public void optionChosen( String option )
    { // selected a ROM file for cheats
        if( option == null )
        {
            Log.e( "MenuActivity", "option null in method optionChosen" );
            return;
        }
        MenuCheatsActivity.CRC = Utility.getHeaderCRC( option );
        if( MenuCheatsActivity.CRC == null )
        {
            Log.e( "MenuActivity", "getHeaderCRC returned null in method optionChosen" );
            return;
        }
        MenuCheatsActivity.ROM = option;
        Intent intent = new Intent( mInstance, MenuCheatsActivity.class );
        intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
        startActivity( intent );
    }

    @Override
    protected Dialog onCreateDialog( int id )
    {
        switch( id )
        {
            case Globals.ABOUT_ID:
            {
                AlertDialog.Builder d = new AlertDialog.Builder( this );
                d.setTitle( R.string.main_credits );
                d.setIcon( R.drawable.icon );
                d.setNegativeButton( R.string.main_close, null );
                View v = LayoutInflater.from( this ).inflate( R.layout.about_dialog, null );
                TextView text = (TextView) v.findViewById( R.id.about_text );
                text.setText( getString( R.string.app_credits ) );
                d.setView( v );
                return d.create();
            }
        }
        return( super.onCreateDialog( id ) );
    }
}

