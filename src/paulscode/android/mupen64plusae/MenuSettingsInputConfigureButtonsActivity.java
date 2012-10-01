package paulscode.android.mupen64plusae;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

// TODO: Comment thoroughly
public class MenuSettingsInputConfigureButtonsActivity extends ListActivity implements IScancodeListener
{
    public static MenuSettingsInputConfigureButtonsActivity mInstance = null;
    private OptionArrayAdapter optionArrayAdapter;  // Array of menu options
    private ScancodeDialog scancodeDialog = null;
    public static int controllerNum = -1;
    public static boolean plugged = true;
    public static String plugin;

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mInstance = this;

        Globals.checkLocale( this );

        List<MenuOption>optionList = new ArrayList<MenuOption>();

        // TODO: remove (function in the background)
        optionList.add( new MenuOption( getString( R.string.core_disable_volume_keys ), 
                                getString( R.string.core_use_as_core_functions ), "menuSettingsInputConfigureVolume",
                                            Globals.volumeKeysDisabled ) );

        if( controllerNum > 0 && controllerNum < 5 )
        {
            if( controllerNum == 1 )
                plugged = true;
            else
                plugged = false;

            String val = MenuActivity.mupen64plus_cfg.get( "Input-SDL-Control" + controllerNum, "plugged" );
            if( val != null )
                plugged = ( val.equals( "True" ) ? true : false );

            optionList.add( new MenuOption( getString( R.string.plugged_in ),
                                            getString( R.string.connect_controller ) + " " + controllerNum, "plugged", plugged ) );

            plugin = getString( R.string.expansion_mempak );
            val = MenuActivity.mupen64plus_cfg.get( "Input-SDL-Control" + controllerNum, "plugin" );
            if( val != null )
            {
                if( val.equals( "1" ) )
                    plugin = getString( R.string.expansion_none );
                else if( val.equals( "5" ) )
                    plugin = getString( R.string.expansion_rumblepak );
                else
                    plugin = getString( R.string.expansion_mempak );
            }

            optionList.add( new MenuOption( getString( R.string.expansion ), plugin, "plugin" ) );
        }

        addOption( optionList, getString( R.string.button_dpad_right ),      "DPad R"           );
        addOption( optionList, getString( R.string.button_dpad_left ),       "DPad L"           );
        addOption( optionList, getString( R.string.button_dpad_down ),       "DPad D"           );
        addOption( optionList, getString( R.string.button_dpad_up ),         "DPad U"           );
        addOption( optionList, getString( R.string.button_start ),           "Start"            );
        addOption( optionList, getString( R.string.button_z ),               "Z Trig"           );
        addOption( optionList, getString( R.string.button_b ),               "B Button"         );
        addOption( optionList, getString( R.string.button_a ),               "A Button"         );
        addOption( optionList, getString( R.string.button_cpad_right ),      "C Button R"       );
        addOption( optionList, getString( R.string.button_cpad_left ),       "C Button L"       );
        addOption( optionList, getString( R.string.button_cpad_down ),       "C Button D"       );
        addOption( optionList, getString( R.string.button_cpad_up ),         "C Button U"       );
        addOption( optionList, getString( R.string.button_r ),               "R Trig"           );
        addOption( optionList, getString( R.string.button_l ),               "L Trig"           );
        addOption( optionList, getString( R.string.mempak_switch ),          "Mempak switch"    );
        addOption( optionList, getString( R.string.rumblepak_switch ),       "Rumblepak switch" );
        addOption( optionList, getString( R.string.button_analog_right ),    "X Axis2"          );
        addOption( optionList, getString( R.string.button_analog_left ),     "X Axis1"          );
        addOption( optionList, getString( R.string.button_analog_down ),     "Y Axis2"          );
        addOption( optionList, getString( R.string.button_analog_up ),       "Y Axis1"          );

//        optionList.add( new MenuOption( "--SPECIAL FUNCTIONS--", "", "line" ) );

        addCoreOption( optionList, getString( R.string.core_stop ), "Kbd Mapping Stop" );
        addCoreOption( optionList, getString( R.string.core_save_state ), "Kbd Mapping Save State" );
        addCoreOption( optionList, getString( R.string.core_load_state ), "Kbd Mapping Load State" );
        addCoreOption( optionList, getString( R.string.core_increment_slot ), "Kbd Mapping Increment Slot" );
        addCoreOption( optionList, getString( R.string.core_reset ), "Kbd Mapping Reset" );
        addCoreOption( optionList, getString( R.string.core_speed_down ), "Kbd Mapping Speed Down" );
        addCoreOption( optionList, getString( R.string.core_speed_up ), "Kbd Mapping Speed Up" );
        addCoreOption( optionList, getString( R.string.core_pause ), "Kbd Mapping Pause" );
        addCoreOption( optionList, getString( R.string.core_fast_forward ), "Kbd Mapping Fast Forward" );
        addCoreOption( optionList, getString( R.string.core_frame_advance ), "Kbd Mapping Frame Advance" );
        addCoreOption( optionList, getString( R.string.core_gameshark ), "Kbd Mapping Gameshark" );

        optionArrayAdapter = new OptionArrayAdapter( this, R.layout.menu_option, optionList );
        setListAdapter( optionArrayAdapter );
    }

    public void addCoreOption( List<MenuOption> optionList, String name, String info )
    {
        if( controllerNum > 0 || info == null )
            return;

        int scancode = 0;
        String val = MenuActivity.mupen64plus_cfg.get( "CoreEvents", info );
        if( val != null )
        {
            try
            {  // Make sure a valid integer was entered
                scancode = Integer.valueOf( val );
            }
            catch( NumberFormatException nfe )
            {}  // Skip it if this happens
        }
        optionList.add( new MenuOption( name,
                                        ((scancode > 0) ? (getString( R.string.button_keycode ) + " " + scancode) :
                               getString( R.string.button_not_mapped )), info ) );
    }

    public void addOption( List<MenuOption> optionList, String name, String info )
    {
        if( controllerNum < 1 || controllerNum > 4 || info == null )
            return;

        int scancode = 0;
        String val;

        if( info.contains( "Axis" ) )
            val = MenuActivity.mupen64plus_cfg.get( "Input-SDL-Control" + controllerNum,
                                                        info.substring( 0, info.length() - 1 ) );
        else
            val = MenuActivity.mupen64plus_cfg.get( "Input-SDL-Control" + controllerNum, info.substring( 0, info.length() ) );

        if( val == null )
            return;

        int x = val.indexOf( "(" );
        int y = val.indexOf( ")" );
        if( x < 0 || y < 0 || y <= x )
            return;
        val = val.substring( x + 1, y ).trim();

        if( val == null || val.length() < 1 )
            return;

        if( info.contains( "Axis" ) )
        {
            x = val.indexOf( "," );
            if( x < 0 )
                return;
            try
            {  // Make sure a valid integer was entered
                if( info.contains( "Axis1" ) )
                {
                    scancode = Integer.valueOf( val.substring( 0, x ) );
                }
                else
                {
                    scancode = Integer.valueOf( val.substring( x + 1, val.length() ) );
                }
            }
            catch( NumberFormatException nfe )
            {}  // Skip it if this happens
        }
        else
        {
            try
            {  // Make sure a valid integer was entered
                scancode = Integer.valueOf( val );
            }
            catch( NumberFormatException nfe )
            {}  // Skip it if this happens
        }
        optionList.add( new MenuOption( name,
                                        ((scancode > 0) ? (getString( R.string.button_keycode ) + " " + scancode) :
                                                           getString( R.string.button_not_mapped )), info ) );
    }

    public void returnCode( int scancode, int codeType )
    {
        if( codeType == 1 )
        {
            MenuActivity.mupen64plus_cfg.put( "CoreEvents", ScancodeDialog.menuItemInfo, String.valueOf( scancode ) );
            optionArrayAdapter.remove( optionArrayAdapter.getOption( ScancodeDialog.menuItemPosition ) );
            optionArrayAdapter.insert( new MenuOption( ScancodeDialog.menuItemName, getString( R.string.button_keycode ) + " " + scancode,
                                       ScancodeDialog.menuItemInfo ), ScancodeDialog.menuItemPosition );
            return;
        }

        String param = ScancodeDialog.menuItemInfo;
        if( param == null )
            return;
        param = param.trim();
        String val;

        if( param.contains( "Axis" ) )
            val = MenuActivity.mupen64plus_cfg.get( "Input-SDL-Control" + controllerNum,
                                                    param.substring( 0, param.length() - 1 ) );
        else
            val = MenuActivity.mupen64plus_cfg.get( "Input-SDL-Control" + controllerNum,
                                                    param.substring( 0, param.length() ) );

        if( val == null )
            return;

        int x = val.indexOf( "(" );
        int y = val.indexOf( ")" );
        if( x < 0 || y < 0 || y <= x )
            return;

        val = val.substring( x + 1, y ).trim();

        if( param.contains( "Axis" ) )
        {
            x = val.indexOf( "," );
            if( x < 0 )
                return;
            if( param.contains( "Axis1" ) )
                val = "(" + scancode + "," + val.substring( x + 1, val.length() ) + ")";
            else
                val = "(" + val.substring( 0, x ) + "," + scancode + ")";
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + controllerNum,
                                              param.substring( 0, param.length() - 1 ), "key" + val );
        }
        else
        {
            val = "(" + scancode + ")";
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + controllerNum, param, "key" + val );
        }
        optionArrayAdapter.remove( optionArrayAdapter.getOption( ScancodeDialog.menuItemPosition ) );
        optionArrayAdapter.insert( new MenuOption( ScancodeDialog.menuItemName,
                                                   ((scancode > 0) ? (getString( R.string.button_keycode ) + " " + scancode) :
                                         getString( R.string.button_not_mapped )), param ),
                                   ScancodeDialog.menuItemPosition );
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
        if( scancodeDialog == null )
            scancodeDialog = new ScancodeDialog( mInstance );

        if( menuOption.info.equals( "menuSettingsInputConfigureVolume" ) ) 
        {
            Globals.volumeKeysDisabled = !Globals.volumeKeysDisabled;
            MenuActivity.gui_cfg.put( "KEYS", "disable_volume_keys", Globals.volumeKeysDisabled ? "1" : "0" );
            optionArrayAdapter.remove( menuOption );
            optionArrayAdapter.insert( new MenuOption( getString( R.string.core_disable_volume_keys ), 
                                    getString( R.string.core_use_as_core_functions ),
                                           "menuSettingsInputConfigureVolume", Globals.volumeKeysDisabled ), position );
        }
        else if( menuOption.info.equals( "plugged" ) ) 
        {
            plugged = !plugged;

            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + controllerNum, "plugged", plugged ? "True" : "False" );
            optionArrayAdapter.remove( menuOption );
            optionArrayAdapter.insert( new MenuOption( getString( R.string.plugged_in ),
                                            getString( R.string.connect_controller ) + " " + controllerNum,
                                            "plugged", plugged ), position );
        }
        else if( menuOption.info.equals( "plugin" ) ) 
        {
            String p = "2";
            if( plugin.equals( getString( R.string.expansion_mempak ) ) )
            {
                plugin = getString( R.string.expansion_rumblepak );
                p = "5";
            }
            else if( plugin.equals( getString( R.string.expansion_rumblepak ) ) )
            {
                plugin = getString( R.string.expansion_none );
                p = "1";
            }
            else
                plugin = getString( R.string.expansion_mempak );
            MenuActivity.mupen64plus_cfg.put( "Input-SDL-Control" + controllerNum, "plugin", p );
            optionArrayAdapter.remove( menuOption );
            optionArrayAdapter.insert( new MenuOption( getString( R.string.expansion ), plugin, "plugin" ), position );
        }
        else if( !menuOption.info.equals( "line" ) )
        {
            ScancodeDialog.parent = this;

            if
            (   menuOption.info.equals( "Kbd Mapping Stop" ) ||
                menuOption.info.equals( "Kbd Mapping Save State" ) ||
                menuOption.info.equals( "Kbd Mapping Load State" ) ||
                menuOption.info.equals( "Kbd Mapping Increment Slot" ) ||
                menuOption.info.equals( "Kbd Mapping Reset" ) ||
                menuOption.info.equals( "Kbd Mapping Speed Down" ) ||
                menuOption.info.equals( "Kbd Mapping Speed Up" ) ||
                menuOption.info.equals( "Kbd Mapping Pause" ) ||
                menuOption.info.equals( "Kbd Mapping Fast Forward" ) ||
                menuOption.info.equals( "Kbd Mapping Frame Advance" ) ||
                menuOption.info.equals( "Kbd Mapping Gameshark" )
            )
                ScancodeDialog.codeType = 1;
            else
                ScancodeDialog.codeType = 0;

            ScancodeDialog.menuItemName = menuOption.name;
            ScancodeDialog.menuItemInfo = menuOption.info;
            ScancodeDialog.menuItemPosition = position;
            scancodeDialog.show();
        }
    }
}
