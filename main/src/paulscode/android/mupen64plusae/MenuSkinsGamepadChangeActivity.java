package paulscode.android.mupen64plusae;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
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
public class MenuSkinsGamepadChangeActivity extends ListActivity implements IOptionChooser
{
    public static MenuSkinsGamepadChangeActivity mInstance = null;
    private OptionArrayAdapter optionArrayAdapter;  // Array of menu options

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mInstance = this;

        Globals.checkLocale( this );

        List<MenuOption>optionList = new ArrayList<MenuOption>();

        optionList.add( new MenuOption( getString( R.string.gamepad_import ), getString( R.string.gamepad_new_skin ), "MenuSkinsGamepadChangeImport" ) );
        DataInputStream in = null;
        try
        {
            FileInputStream fstream = new FileInputStream( Globals.DataDir + "/skins/gamepads/gamepad_list.ini" );
            in = new DataInputStream( fstream );
            BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
            String strLine;
            int c = 0;
            while( ( strLine = br.readLine() ) != null )
            {
                if( strLine.length() > 0 )
                {
                    optionList.add( new MenuOption( strLine, "", "" ) );
                }
            }
        }
        catch( Exception e )
        {
            Log.e( "MenuSkinsGamepadChangeActivity", "Problem reading gamepad list, message: " + e.getMessage() );
        }
        try
        {
            if( in != null )
                in.close();
        }
        catch( Exception e )
        {
            Log.e( "MenuSkinsGamepadChangeActivity", "Problem closing gamepad list file, error message: " + e.getMessage() );
        }
        optionArrayAdapter = new OptionArrayAdapter( this, R.layout.menu_option, optionList );
        setListAdapter( optionArrayAdapter );
    }

    public void optionChosen( String option )
    {
        if( option == null )
        {
            Log.e( "MenuSkinsGamepadChangeActivity", "option null in method optionChosen" );
            return;
        }
        File archive = new File( option );
        String padName = archive.getName();
        if( padName == null )
        {
            Log.e( "MenuSkinsGamepadChangeActivity", "pad name null in method optionChosen" );
            return;
        }
        padName = padName.substring( 0, padName.length() - 4 );
        if( Utility.unzipAll( archive, Globals.DataDir + "/skins/gamepads/" + padName ) )
        {
            try
            {
                FileWriter fw = new FileWriter( Globals.DataDir + "/skins/gamepads/gamepad_list.ini", true );
                fw.write( padName + "\n" );
                fw.flush();
                fw.close();
            }
            catch( Exception e )
            {
                Log.e( "MenuSkinsGamepadChangeActivity", "error writing to gamepad_list.ini: " + e );
                return;
            }
            optionArrayAdapter.add( new MenuOption( padName, "", "" ) );
        }
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
        if( menuOption.info.equals( "MenuSkinsGamepadChangeImport" ) )
        {  // Open the file menu to choose a skin
            String path = MenuActivity.gui_cfg.get( "LAST_SESSION", "skin_folder" );

            if( path == null || path.length() < 1 )
                FileChooserActivity.startPath = Globals.StorageDir;
            else
                FileChooserActivity.startPath = path;
            FileChooserActivity.extensions = ".zip";
            FileChooserActivity.parentMenu = mInstance;
            FileChooserActivity.function = FileChooserActivity.FUNCTION_SKIN;
            Intent intent = new Intent( mInstance, FileChooserActivity.class );
            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
        }
        else
        {
            MenuSkinsGamepadActivity.chosenGamepad = menuOption.name;
            if( MenuSkinsGamepadActivity.mInstance != null )
                MenuSkinsGamepadActivity.mInstance.updateGamepadString();
            mInstance.finish();
        }
    }
}
