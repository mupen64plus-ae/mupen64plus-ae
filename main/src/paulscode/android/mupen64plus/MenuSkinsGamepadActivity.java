package paulscode.android.mupen64plus;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.TextView;

// TODO: Comment thoroughly
public class MenuSkinsGamepadActivity extends ListActivity
{
    public static MenuSkinsGamepadActivity mInstance = null;
    public static String chosenGamepad = "";
    public static boolean redrawAll = false;
    public static boolean analogAsOctagon = true;
    public static boolean showFPS = false;
    public static boolean enabled = true;
    private OptionArrayAdapter optionArrayAdapter;  // array of menu options

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
            try
            {
                FileInputStream fstream = new FileInputStream( Globals.DataDir + "/skins/gamepads/gamepad_list.ini" );
                in = new DataInputStream( fstream );
                BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
                int c = 0;
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

        String val = MenuActivity.gui_cfg.get( "GAME_PAD", "redraw_all" );
        if( val != null )
            redrawAll = ( val.equals( "1" ) ? true : false );
        val = MenuActivity.gui_cfg.get( "GAME_PAD", "analog_octagon" );
        if( val != null )
            analogAsOctagon = ( val.equals( "1" ) ? true : false );
        val = MenuActivity.gui_cfg.get( "GAME_PAD", "show_fps" );
        if( val != null )
            showFPS = ( val.equals( "1" ) ? true : false );
        val = MenuActivity.gui_cfg.get( "GAME_PAD", "enabled" );
        if( val != null )
            enabled = ( val.equals( "1" ) ? true : false );

        List<MenuOption>optionList = new ArrayList<MenuOption>();
        optionList.add( new MenuOption( getString( R.string.gamepad_change_layout ), chosenGamepad, "menuSkinsGamepadChange" ) );
        optionList.add( new MenuOption( getString( R.string.gamepad_redraw_all ), getString ( R.string.gamepad_misbut_bug ), "menuSkinsGamepadRedraw", redrawAll ) );
        optionList.add( new MenuOption( getString( R.string.gamepad_accurate_n64_stick ), getString( R.string.gamepad_analg_as_octgon ), "menuSkinsGamepadOctagon", analogAsOctagon ) );
        optionList.add( new MenuOption( getString( R.string.gamepad_display_fps ), getString( R.string.gamepad_shw_frm_sec ), "menuSkinsGamepadFPS", showFPS ) );
        optionList.add( new MenuOption( getString( R.string.gamepad_enable ), getString( R.string.gamepad_use_virt_gpad ), "menuSkinsGamepadEnabled", enabled ) );

        optionArrayAdapter = new OptionArrayAdapter( this, R.layout.menu_option, optionList );
        setListAdapter( optionArrayAdapter );
    }
    public void updateGamepadString()
    {
        optionArrayAdapter.remove( optionArrayAdapter.getItem( 0 ) );
        optionArrayAdapter.insert( new MenuOption( "Change", chosenGamepad, "menuSkinsGamepadChange" ), 0 );
        MenuActivity.gui_cfg.put( "GAME_PAD", "which_pad", chosenGamepad );
    }
    /*
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
        if( menuOption.info.equals( "menuSkinsGamepadChange" ) )
        {
            Intent intent = new Intent( mInstance, MenuSkinsGamepadChangeActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
        }
        else if( menuOption.info.equals( "menuSkinsGamepadRedraw" ) ) 
        {
            redrawAll = !redrawAll;
            optionArrayAdapter.remove( menuOption );
            optionArrayAdapter.insert( new MenuOption( getString( R.string.gamepad_redraw_all ), 
                    getString( R.string.gamepad_misbut_bug ), "menuSkinsGamepadRedraw", redrawAll ), position );
            MenuActivity.gui_cfg.put( "GAME_PAD", "redraw_all", redrawAll ? "1" : "0" );
        }
        else if( menuOption.info.equals( "menuSkinsGamepadOctagon" ) ) 
        {
            analogAsOctagon = !analogAsOctagon;
            optionArrayAdapter.remove( menuOption );
            optionArrayAdapter.insert( new MenuOption( getString( R.string.gamepad_accurate_n64_stick ), 
                    getString( R.string.gamepad_analg_as_octgon ), "menuSkinsGamepadOctagon", analogAsOctagon ), position );
            MenuActivity.gui_cfg.put( "GAME_PAD", "analog_octagon", analogAsOctagon ? "1" : "0" );
        }
        else if( menuOption.info.equals( "menuSkinsGamepadFPS" ) ) 
        {
            showFPS = !showFPS;
            optionArrayAdapter.remove( menuOption );
            optionArrayAdapter.insert( new MenuOption( getString( R.string.gamepad_display_fps ), 
                    getString( R.string.gamepad_shw_frm_sec ), "menuSkinsGamepadFPS", showFPS ), position );
            MenuActivity.gui_cfg.put( "GAME_PAD", "show_fps", showFPS ? "1" : "0" );
        }
        else if( menuOption.info.equals( "menuSkinsGamepadEnabled" ) ) 
        {
            enabled = !enabled;
            optionArrayAdapter.remove( menuOption );
            optionArrayAdapter.add( new MenuOption( getString( R.string.gamepad_enable ), 
                    getString( R.string.gamepad_use_virt_gpad ), "menuSkinsGamepadEnabled", enabled ) );
            MenuActivity.gui_cfg.put( "GAME_PAD", "enabled", enabled ? "1" : "0" );
        }
    }
}

