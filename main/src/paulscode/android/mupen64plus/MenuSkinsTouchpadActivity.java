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
import android.widget.ListView;

// TODO: Comment thoroughly
public class MenuSkinsTouchpadActivity extends ListActivity
{
    public static MenuSkinsTouchpadActivity mInstance = null;
    public static String chosenTouchpad = "";
    public static boolean analogAsOctagon = true;
    public static boolean enabled = true;
    private OptionArrayAdapter optionArrayAdapter;  // array of menu options

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mInstance = this;

        chosenTouchpad = MenuActivity.gui_cfg.get( "TOUCH_PAD", "which_pad" );

        if( chosenTouchpad == null || chosenTouchpad.length() < 1 )
        {
            DataInputStream in = null;
            try
            {
                FileInputStream fstream = new FileInputStream( Globals.DataDir + "/skins/touchpads/touchpad_list.ini" );
                in = new DataInputStream( fstream );
                BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
                int c = 0;
                chosenTouchpad = br.readLine();
            }
            catch( Exception e )
            {
                Log.e( "MenuSkinsTouchpadActivity", "Problem reading touchpad list, message: " + e.getMessage() );
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

        String val = MenuActivity.gui_cfg.get( "TOUCH_PAD", "analog_octagon" );
        if( val != null )
            analogAsOctagon = ( val.equals( "1" ) ? true : false );
        val = MenuActivity.gui_cfg.get( "TOUCH_PAD", "enabled" );
        if( val != null )
            enabled = ( val.equals( "1" ) ? true : false );

        List<MenuOption>optionList = new ArrayList<MenuOption>();
        optionList.add( new MenuOption( getString( R.string.touchpad_change ), chosenTouchpad, "menuSkinsTouchpadChange" ) );
        optionList.add( new MenuOption( getString( R.string.gamepad_accurate_n64_stick ), getString( R.string.gamepad_analg_as_octgon ), "menuSkinsTouchpadOctagon", analogAsOctagon ) );
        optionList.add( new MenuOption( getString( R.string.touchpad_enable ), getString( R.string.touchpad_use_touchpad ), "menuSkinsTouchpadEnabled", enabled ) );

        optionArrayAdapter = new OptionArrayAdapter( this, R.layout.menu_option, optionList );
        setListAdapter( optionArrayAdapter );
    }
    public void updateTouchpadString()
    {
        optionArrayAdapter.remove( optionArrayAdapter.getItem( 0 ) );
        optionArrayAdapter.insert( new MenuOption( getString( R.string.touchpad_change ), chosenTouchpad, "menuSkinsTouchpadChange" ), 0 );
        MenuActivity.gui_cfg.put( "TOUCH_PAD", "which_pad", chosenTouchpad );
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
        if( menuOption.info.equals( "menuSkinsTouchpadChange" ) )
        {
            Intent intent = new Intent( mInstance, MenuSkinsTouchpadChangeActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
        }
        else if( menuOption.info.equals( "menuSkinsTouchpadOctagon" ) ) 
        {
            analogAsOctagon = !analogAsOctagon;
            optionArrayAdapter.remove( menuOption );
            optionArrayAdapter.insert( new MenuOption( getString( R.string.gamepad_accurate_n64_stick ), getString( R.string.gamepad_analg_as_octgon ), "menuSkinsTouchpadOctagon",
                                                       analogAsOctagon ), position );
            MenuActivity.gui_cfg.put( "TOUCH_PAD", "analog_octagon", analogAsOctagon ? "1" : "0" );
        }
        else if( menuOption.info.equals( "menuSkinsTouchpadEnabled" ) ) 
        {
            enabled = !enabled;
            optionArrayAdapter.remove( menuOption );
            optionArrayAdapter.add( new MenuOption( getString( R.string.touchpad_enable ), getString( R.string.touchpad_use_touchpad ), "menuSkinsTouchpadEnabled",
                                                    enabled ) );
            MenuActivity.gui_cfg.put( "TOUCH_PAD", "enabled", enabled ? "1" : "0" );
        }
    }
}

