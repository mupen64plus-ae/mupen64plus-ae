package paulscode.android.mupen64plus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

// TODO: Comment thoroughly
public class MenuSettingsActivity extends ListActivity
{
    public static MenuSettingsActivity mInstance = null;
    private OptionArrayAdapter optionArrayAdapter;  // array of menu options

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mInstance = this;

        Globals.checkLocale( this );

        String val = MenuActivity.gui_cfg.get( "TOUCH_PAD", "is_xperia_play" );
        if( val != null )
            Globals.isXperiaPlay = ( val.equals( "1" ) ? true : false );


        List<MenuOption>optionList = new ArrayList<MenuOption>();
        optionList.add( new MenuOption( getString( R.string.settings_video ), getString( R.string.settings_config_graphics ), "menuSettingsVideo" ) );
        optionList.add( new MenuOption( getString( R.string.settings_audio ), getString( R.string.settings_audio_settings ), "menuSettingsAudio" ) );
        optionList.add( new MenuOption( getString( R.string.settings_input ), getString( R.string.settings_map_controller_buttons ), "menuSettingsInput" ) );
        optionList.add( new MenuOption( getString( R.string.settings_virtual_gamepad ), getString( R.string.settings_config_virtual_gamepad ), "menuSkinsGamepad" ) );
        optionList.add( new MenuOption( getString( R.string.settings_rsp ), getString( R.string.settings_reality_signal_processor ), "menuSettingsRSP" ) );
        optionList.add( new MenuOption( getString( R.string.settings_core ), getString( R.string.settings_emu_core_settings ), "menuSettingsCore" ) );

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD )
        { // If we are at least GB, this device could be an XperiaPlay:
            optionList.add( new MenuOption( getString( R.string.settings_xperia_play_optimized ), getString( R.string.settings_xperia_play_only ),
                                            "menuSettingsXperiaPlayOptimized", Globals.isXperiaPlay ) );
        }
        else
        { // This shouldn't be necessary, but just to be safe:
            Globals.isXperiaPlay = false;
            MenuActivity.gui_cfg.put( "TOUCH_PAD", "is_xperia_play", "0" );
        }
//        optionList.add( new MenuOption( "Touchpad", "configure the Xperia Play touchpad", "menuSkinsTouchpad" ) );
        if( Globals.isXperiaPlay )
            optionList.add( new MenuOption( getString( R.string.settings_touchpad ), getString( R.string.settings_config_touchpad ), "menuSkinsTouchpad" ) );

        optionList.add( new MenuOption( getString( R.string.settings_reset_defaults ), getString( R.string.settings_reset_to_deflt_vals ), "menuSettingsResetDefaults" ) );
        optionList.add( new MenuOption( getString( R.string.settings_restore_app_data ), getString( R.string.settings_imports_will_be_lost ), "menuSettingsRestoreAppData" ) );
        optionList.add( new MenuOption( getString( R.string.settings_enable_auto_save ), getString( R.string.settings_auto_save_game_on_exit ),
                                        "menuSettingsAutoSave", Globals.auto_save ) );
//        optionList.add( new MenuOption( getString( R.string.settings_language ), getString( R.string.settings_manually_change_language ),
//                                        "menuSettingsLanguage" ) );    // Moved to MenuActivity, in case language changed accidentally

        optionArrayAdapter = new OptionArrayAdapter( this, R.layout.menu_option, optionList );
        setListAdapter( optionArrayAdapter );
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
        if( menuOption.info.equals( "menuSettingsVideo" ) )
        {
            Intent intent = new Intent( mInstance, MenuSettingsVideoActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
        }
        else if( menuOption.info.equals( "menuSettingsAudio" ) ) 
        {
            Intent intent = new Intent( mInstance, MenuSettingsAudioActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
        }
        else if( menuOption.info.equals( "menuSettingsInput" ) ) 
        {
            Intent intent = new Intent( mInstance, MenuSettingsInputActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
        }
        else if( menuOption.info.equals( "menuSkinsGamepad" ) )
        {
            Intent intent = new Intent( mInstance, MenuSkinsGamepadActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
        }
        else if( menuOption.info.equals( "menuSettingsRSP" ) ) 
        {
            Intent intent = new Intent( mInstance, MenuSettingsRSPActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
        }
        else if( menuOption.info.equals( "menuSettingsXperiaPlayOptimized" ) ) 
        {
            Globals.isXperiaPlay = !Globals.isXperiaPlay;

            optionArrayAdapter.remove( menuOption );

            if( Globals.isXperiaPlay )
                MenuActivity.gui_cfg.put( "GAME_PAD", "enabled", "0" );  // Turn off the virtual gamepad
            else
            {
                MenuActivity.gui_cfg.put( "GAME_PAD", "enabled", "1" );  // Turn on the virtual gamepad
                optionArrayAdapter.remove( optionArrayAdapter.getOption( position ) );  // Remove touchpad settings
            }

            if( Globals.isXperiaPlay )
                optionArrayAdapter.insert( new MenuOption( getString( R.string.settings_touchpad ),
                                                           getString( R.string.settings_config_touchpad ), "menuSkinsTouchpad" ), position );
            optionArrayAdapter.insert( new MenuOption( getString( R.string.settings_xperia_play_optimized ), 
                    getString( R.string.settings_xperia_play_only ), "menuSettingsXperiaPlayOptimized", Globals.isXperiaPlay ), position );

            MenuActivity.gui_cfg.put( "TOUCH_PAD", "is_xperia_play", ( Globals.isXperiaPlay ? "1" : "0" ) );
        }
        else if( menuOption.info.equals( "menuSkinsTouchpad" ) )
        {
            Intent intent = new Intent( mInstance, MenuSkinsTouchpadActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
        }
        else if( menuOption.info.equals( "menuSettingsCore" ) ) 
        {
            Intent intent = new Intent( mInstance, MenuSettingsCoreActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
        }
        else if( menuOption.info.equals( "menuSettingsResetDefaults" ) ) 
        {
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
        else if( menuOption.info.equals( "menuSettingsRestoreAppData" ) ) 
        {
            showDialog( Globals.SURE_ID );
        }
        else if( menuOption.info.equals( "menuSettingsAutoSave" ) ) 
        {
            Globals.auto_save = !Globals.auto_save;
            optionArrayAdapter.remove( menuOption );
            optionArrayAdapter.insert( new MenuOption( getString( R.string.settings_enable_auto_save ), 
                    getString( R.string.settings_auto_save_game_on_exit ), "menuSettingsAutoSave", Globals.auto_save ), position );
            MenuActivity.gui_cfg.put( "GENERAL", "auto_save", ( Globals.auto_save ? "1" : "0") );
        }
//        else if( menuOption.info.equals( "menuSettingsLanguage" ) ) 
//        {
//            Intent intent = new Intent( mInstance, MenuSettingsLanguageActivity.class );
//            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
//            startActivity( intent );
//        }    // Moved to MenuActivity, in case language changed accidentally
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

