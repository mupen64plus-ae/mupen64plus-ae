package paulscode.android.mupen64plus;

import java.util.ArrayList;
import java.util.List;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.TextView;

// TODO: Comment thoroughly
public class MenuSettingsInputConfigureActivity extends ListActivity
{
    public static MenuSettingsInputConfigureActivity mInstance = null;
    private OptionArrayAdapter optionArrayAdapter;  // Array of menu options

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mInstance = this;

        Globals.checkLocale( this );

        List<MenuOption>optionList = new ArrayList<MenuOption>();
        optionList.add( new MenuOption( getString( R.string.input_controller_1 ), getString( R.string.input_map_ctrl_1_buttons ), "menuSettingsInputConfigureController1" ) );
        optionList.add( new MenuOption( getString( R.string.input_controller_2 ), getString( R.string.input_map_ctrl_2_buttons ), "menuSettingsInputConfigureController2" ) );
        optionList.add( new MenuOption( getString( R.string.input_controller_3 ), getString( R.string.input_map_ctrl_3_buttons ), "menuSettingsInputConfigureController3" ) );
        optionList.add( new MenuOption( getString( R.string.input_controller_4 ), getString( R.string.input_map_ctrl_4_buttons ), "menuSettingsInputConfigureController4" ) );
        optionList.add( new MenuOption( getString( R.string.input_special_buttons ) , getString( R.string.input_map_core_funcs_buttons ), "menuSettingsInputConfigureCoreFunctions" ) );

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
        if( menuOption.info.equals( "menuSettingsInputConfigureController1" ) )
        {  // Open the menu to map controller buttons
            MenuSettingsInputConfigureButtonsActivity.controllerNum = 1;
            Intent intent = new Intent( mInstance, MenuSettingsInputConfigureButtonsActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
        }
        else if( menuOption.info.equals( "menuSettingsInputConfigureController2" ) )
        {  // Open the menu to map controller buttons
            MenuSettingsInputConfigureButtonsActivity.controllerNum = 2;
            Intent intent = new Intent( mInstance, MenuSettingsInputConfigureButtonsActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
        }
        else if( menuOption.info.equals( "menuSettingsInputConfigureController3" ) )
        {  // Open the menu to map controller buttons
            MenuSettingsInputConfigureButtonsActivity.controllerNum = 3;
            Intent intent = new Intent( mInstance, MenuSettingsInputConfigureButtonsActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
        }
        else if( menuOption.info.equals( "menuSettingsInputConfigureController4" ) )
        {  // Open the menu to map controller buttons
            MenuSettingsInputConfigureButtonsActivity.controllerNum = 4;
            Intent intent = new Intent( mInstance, MenuSettingsInputConfigureButtonsActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
        }
        else if( menuOption.info.equals( "menuSettingsInputConfigureCoreFunctions" ) )
        {  // Open the menu to map controller buttons
            MenuSettingsInputConfigureButtonsActivity.controllerNum = -1;
            Intent intent = new Intent( mInstance, MenuSettingsInputConfigureButtonsActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
        }
    }
}
