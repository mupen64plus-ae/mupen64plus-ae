package paulscode.android.mupen64plus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

// TODO: Comment thoroughly
public class MenuActivity extends ListActivity implements IOptionChooser
{
    public static MenuActivity mInstance = null;
    private OptionArrayAdapter optionArrayAdapter;  // Array of menu options
    private static NotificationManager notificationManager = null;

    public static Config mupen64plus_cfg = new Config( Globals.DataDir + "/mupen64plus.cfg" );
//    public static Config InputAutoCfg_ini = new Config( Globals.DataDir + "/data/InputAutoCfg.ini" );
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

        String first_run = gui_cfg.get( "GENERAL", "first_run" );
        int width, height;

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

        List<MenuOption>optionList = new ArrayList<MenuOption>();
        optionList.add( new MenuOption( getString( R.string.main_choose_game ), getString( R.string.main_select_a_game_to_play ), "menuOpenROM" ) );

        val = gui_cfg.get( "LAST_SESSION", "rom" );
        if( val != null && val.length() > 0 )
            optionList.add( new MenuOption( getString( R.string.main_resume ), getString( R.string.main_continue_your_last_game ), "menuResume" ) );

        optionList.add( new MenuOption( getString( R.string.main_settings ), getString( R.string.main_configure_plug_ins ), "menuSettings" ) );
        optionList.add( new MenuOption( getString( R.string.main_help ), getString( R.string.main_rept_bugs ), "menuHelp" ) );
        optionList.add( new MenuOption( getString( R.string.main_credits ), getString( R.string.main_devs_contribs_tstrs ), "menuCredits" ) );
        optionList.add( new MenuOption( getString( R.string.main_cheat ), getString( R.string.main_choose_game_to_hack ), "menuCheats" ) );
        optionList.add( new MenuOption( getString( R.string.settings_language ), getString( R.string.settings_manually_change_language ),
                                        "menuSettingsLanguage" ) );
        optionList.add( new MenuOption( getString( R.string.main_close ), getString( R.string.main_exit_the_app ), "menuClose" ) );

        optionArrayAdapter = new OptionArrayAdapter( this, R.layout.menu_option, optionList );
        setListAdapter( optionArrayAdapter );

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

    /**
     * Determines what to do, based on what option the user chose 
     * @param listView Used by Android.
     * @param view Used by Android.
     * @param position Which item the user chose.
     * @param id Used by Android.
     */
    @Override
    protected void onListItemClick( ListView listView, View view, int position, long id )
    {
        super.onListItemClick( listView, view, position, id );
        MenuOption menuOption = optionArrayAdapter.getOption( position );
        if( menuOption.info.equals( "menuOpenROM" ) )
        {  // Open the file chooser to pick a ROM
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
        }
        else if( menuOption.info.equals( "menuResume" ) ) 
        {  // Resume the last game
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
                return;
            }
            mupen64plus_cfg.save();
            //InputAutoCfg_ini.save();
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
        }
        else if( menuOption.info.equals( "menuSettings" ) ) 
        {  // Configure the plug-ins
            Intent intent = new Intent( mInstance, MenuSettingsActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
        }
        else if( menuOption.info.equals( "menuHelp" ) ) 
        {  // Visit the FAQ page (opens browser)
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
        }
        else if( menuOption.info.equals( "menuCheats" ) ) 
        {  // Open the file chooser to pick a ROM
            String path = gui_cfg.get( "LAST_SESSION", "rom_folder" );

            if( path == null || path.length() < 1 )
                FileChooserActivity.startPath = Globals.DataDir;
            else
                FileChooserActivity.startPath = path;
            FileChooserActivity.extensions = ".z64.v64.n64.zip";
            FileChooserActivity.parentMenu = this;
            FileChooserActivity.function = FileChooserActivity.FUNCTION_ROM;
            Intent intent = new Intent( mInstance, FileChooserActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
        }
        else if( menuOption.info.equals( "menuCredits" ) ) 
        {  // Show the credits
            showDialog( Globals.ABOUT_ID );
        }
        else if( menuOption.info.equals( "menuSettingsLanguage" ) ) 
        {
            Intent intent = new Intent( mInstance, MenuSettingsLanguageActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
        }
        else if( menuOption.info.equals( "menuClose" ) ) 
        {  // Shut down the app
            File f = new File( Globals.StorageDir );
            if( f.exists() )
            {
                mupen64plus_cfg.save();
                //InputAutoCfg_ini.save();
                gui_cfg.save();
            }
            mInstance.finish();
        }
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

