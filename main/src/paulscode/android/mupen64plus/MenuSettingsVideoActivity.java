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
public class MenuSettingsVideoActivity extends ListActivity implements IOptionChooser
{
    public static MenuSettingsVideoActivity mInstance = null;
    private OptionArrayAdapter optionArrayAdapter;  // Array of menu options
    public static String currentPlugin = "(none)";
    public static boolean enabled = true;
    public static boolean rgba8888 = false;

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mInstance = this;

        Globals.checkLocale( this );

        currentPlugin = "(none)";

        String filename = MenuActivity.mupen64plus_cfg.get( "UI-Console", "VideoPlugin" );
        if( filename == null || filename.length() < 1 || filename.equals( "\"\"" ) || filename.equals( "\"dummy\"" ) )
            filename = MenuActivity.gui_cfg.get( "VIDEO_PLUGIN", "last_choice" );
        if( filename != null )
        {
            MenuActivity.gui_cfg.put( "VIDEO_PLUGIN", "last_choice", filename );
            filename = filename.replace( "\"", "" );
            int x = filename.lastIndexOf( "/" );
            if( x > -1 && x < (filename.length() - 1) )
            {
                currentPlugin = filename.substring( x + 1, filename.length() );
                if( currentPlugin == null || currentPlugin.length() < 1 )
                    currentPlugin = "(none)";
            }
        }
        String en = MenuActivity.gui_cfg.get( "VIDEO_PLUGIN", "enabled" );
        if( en != null )
            enabled = en.equals( "1" ) ? true : false;
        en = MenuActivity.gui_cfg.get( "VIDEO_PLUGIN", "rgba8888" );
        if( en != null )
            rgba8888 = en.equals( "1" ) ? true : false;

        List<MenuOption>optionList = new ArrayList<MenuOption>();
        optionList.add( new MenuOption( getString( R.string.video_change_plug_in ), currentPlugin, "menuSettingsVideoChange" ) );
        optionList.add( new MenuOption( getString( R.string.video_configure ), getString( R.string.video_change_the_settings ), "menuSettingsVideoConfigure" ) );
        optionList.add( new MenuOption( getString( R.string.video_rgba_8888_mode ), getString( R.string.video_improve_gfx_devices ), "menuSettingsVideoRGBA8888", rgba8888 ) );
        optionList.add( new MenuOption( getString( R.string.video_enable ), getString( R.string.video_use_this_plug_in ), "menuSettingsVideoEnabled", enabled ) );

        optionArrayAdapter = new OptionArrayAdapter( this, R.layout.menu_option, optionList );
        setListAdapter( optionArrayAdapter );
    }

    public void optionChosen( String option )
    {
        currentPlugin = "(none)";

        if( option != null )
        {
            MenuActivity.gui_cfg.put( "VIDEO_PLUGIN", "last_choice", "\"" + option + "\"" );
            MenuActivity.mupen64plus_cfg.put( "UI-Console", "VideoPlugin", "\"" + option + "\"" );
            int x = option.lastIndexOf( "/" );
            if( x > -1 && x < ( option.length() - 1 ) )
            {
                currentPlugin = option.substring( x + 1, option.length() );
                if( currentPlugin == null || currentPlugin.length() < 1 )
                    currentPlugin = "(none)";
            }
            else
                currentPlugin = option;
        }

        optionArrayAdapter.remove( optionArrayAdapter.getItem( 0 ) );
        optionArrayAdapter.insert( new MenuOption( getString( R.string.video_change ), currentPlugin, "menuSettingsVideoChange" ), 0 );
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
        if( menuOption.info.equals( "menuSettingsVideoChange" ) )
        {  // Open the menu to choose a plug-in
            Intent intent = new Intent( mInstance, MenuSettingsVideoChangeActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
        }
        else if( menuOption.info.equals( "menuSettingsVideoConfigure" ) ) 
        {
            Intent intent = new Intent( mInstance, MenuSettingsVideoConfigureActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
        }
        else if( menuOption.info.equals( "menuSettingsVideoRGBA8888" ) ) 
        {
            rgba8888 = !rgba8888;
            optionArrayAdapter.remove( menuOption );
            optionArrayAdapter.insert( new MenuOption( getString( R.string.video_rgba_8888_mode ), 
                    getString( R.string.video_improve_gfx_devices ), "menuSettingsVideoRGBA8888", rgba8888 ), position );
            MenuActivity.gui_cfg.put( "VIDEO_PLUGIN", "rgba8888", (rgba8888 ? "1" : "0") );
        }
        else if( menuOption.info.equals( "menuSettingsVideoEnabled" ) ) 
        {
            enabled = !enabled;
            optionArrayAdapter.remove( menuOption );
            optionArrayAdapter.add( new MenuOption( getString( R.string.video_enable ), getString( R.string.video_use_this_plug_in )
                    , "menuSettingsVideoEnabled", enabled ) );
            MenuActivity.gui_cfg.put( "VIDEO_PLUGIN", "enabled", (enabled ? "1" : "0") );
            MenuActivity.mupen64plus_cfg.put( "UI-Console", "VideoPlugin",
                (enabled ? MenuActivity.gui_cfg.get( "VIDEO_PLUGIN", "last_choice" ) : "\"dummy\"") );
        }
    }
}

