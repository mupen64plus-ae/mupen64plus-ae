package paulscode.android.mupen64plus;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

// TODO: Comment thoroughly
public class MenuSettingsCoreConfigureActivity extends ListActivity implements IScancodeListener
{
    public static MenuSettingsCoreConfigureActivity mInstance = null;
    private OptionArrayAdapter optionArrayAdapter;  // Array of menu options
    private ScancodeDialog scancodeDialog = null;

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mInstance = this;

        Globals.checkLocale( this );

        List<MenuOption>optionList = new ArrayList<MenuOption>();
        addOption( optionList, getString( R.string.core_stop ), "Kbd Mapping Stop" );
        addOption( optionList, getString( R.string.core_save_state ), "Kbd Mapping Save State" );
        addOption( optionList, getString( R.string.core_load_state ), "Kbd Mapping Load State" );
        addOption( optionList, getString( R.string.core_increment_slot ), "Kbd Mapping Increment Slot" );
        addOption( optionList, getString( R.string.core_reset ), "Kbd Mapping Reset" );
        addOption( optionList, getString( R.string.core_speed_down ), "Kbd Mapping Speed Down" );
        addOption( optionList, getString( R.string.core_speed_up ), "Kbd Mapping Speed Up" );
        addOption( optionList, getString( R.string.core_pause ), "Kbd Mapping Pause" );
        addOption( optionList, getString( R.string.core_fast_forward ), "Kbd Mapping Fast Forward" );
        addOption( optionList, getString( R.string.core_frame_advance ), "Kbd Mapping Frame Advance" );
        addOption( optionList, getString( R.string.core_gameshark ), "Kbd Mapping Gameshark" );
        optionList.add( new MenuOption( getString( R.string.core_disable_volume_keys ), 
                getString( R.string.core_use_as_core_functions ), "menuSettingsCoreConfigureVolume", Globals.volumeKeysDisabled ) );
        optionArrayAdapter = new OptionArrayAdapter( this, R.layout.menu_option, optionList );
        setListAdapter( optionArrayAdapter );
    }

    public void addOption( List<MenuOption> optionList, String name, String info )
    {
        if( info == null )
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

    public void returnCode( int scancode, int codeType )
    {
        MenuActivity.mupen64plus_cfg.put( "CoreEvents", ScancodeDialog.menuItemInfo, String.valueOf( scancode ) );
        optionArrayAdapter.remove( optionArrayAdapter.getOption( ScancodeDialog.menuItemPosition ) );
        optionArrayAdapter.insert( new MenuOption( ScancodeDialog.menuItemName, getString( R.string.button_keycode ) + " " + scancode,
                                   ScancodeDialog.menuItemInfo ), ScancodeDialog.menuItemPosition );
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

        if( menuOption.info.equals( "menuSettingsCoreConfigureVolume" ) ) 
        {
            Globals.volumeKeysDisabled = !Globals.volumeKeysDisabled;
            MenuActivity.gui_cfg.put( "KEYS", "disable_volume_keys", Globals.volumeKeysDisabled ? "1" : "0" );
            optionArrayAdapter.remove( menuOption );
            optionArrayAdapter.add( new MenuOption( getString( R.string.core_disable_volume_keys ), 
                    getString( R.string.core_use_as_core_functions ), "menuSettingsCoreConfigureVolume", Globals.volumeKeysDisabled ) );
        }
        else
        {
            ScancodeDialog.parent = this;
            ScancodeDialog.menuItemName = menuOption.name;
            ScancodeDialog.menuItemInfo = menuOption.info;
            ScancodeDialog.menuItemPosition = position;
            scancodeDialog.show();
        }
    }
}
