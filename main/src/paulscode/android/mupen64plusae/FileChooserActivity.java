package paulscode.android.mupen64plusae;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

/**
 * The FileChooserActivity class is used to navigate the file system
 * and choose a file.  Each menu option consists of two lines: the
 * file or folder name and a comment stating either that it is a
 * folder or the file's size in bytes.  Each menu option also stores
 * the full path to that file or folder.
 *
 * @author: Paul Lamb
 * 
 * http://www.paulscode.com
 * 
 */
public class FileChooserActivity extends ListActivity
{
    private File currentFolder;  // Folder being browsed at the moment
    private OptionArrayAdapter optionArrayAdapter;  // array of menu options

    public static String startPath = null;
    public static String extensions = null;
    public static IOptionChooser parentMenu = null;

    public static final int FUNCTION_UNKNOWN = 0;
    public static final int FUNCTION_ROM     = 1;
    public static final int FUNCTION_SO      = 2;
    public static final int FUNCTION_SKIN    = 3;
    public static final int FUNCTION_TEXTURE = 4;


    public static int function = FUNCTION_UNKNOWN;

    public static FileChooserActivity mInstance = null;

    /**
     * Populates the menu with the current directory
     * @param savedInstanceState Used by Android.
     */
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mInstance = this;

        Globals.checkLocale( this );

        if( startPath == null )
            startPath = Globals.StorageDir;
        currentFolder = new File( startPath ); // Get the start folder
        populate( currentFolder );
    }

    /**
     * Populates the menu with the specified folder
     * @param folder Folder containing the files to list.
     */
    private void populate( File folder )
    {
        if( folder == null || !folder.isDirectory() )
        {
            folder = new File( Globals.StorageDir );
            if( !folder.isDirectory() )
            {
                Log.e( "FileChooserActivity", "SD Card not accessable in method populate" );
                Runnable toastMessager = new Runnable()
                {
                    public void run()
                    {
                        Toast toast = Toast.makeText( MenuActivity.mInstance, getString( R.string.app_data_inaccessible ), Toast.LENGTH_LONG );
                        toast.setGravity( Gravity.BOTTOM, 0, 0 );
                        toast.show();
                    }
                };
                runOnUiThread( toastMessager );
                return;
            }
        }
        File[] fileList = folder.listFiles();
        setTitle( getString( R.string.file_current_folder ) + ": " + folder.getName() );
        List<MenuOption>folders = new ArrayList<MenuOption>();
        List<MenuOption>files = new ArrayList<MenuOption>();
        String filename, ext;
        try
        { // Separate the folders and files
            int p;
            for( File file: fileList )
            { // Check if it is a folder or a file
                if( file.isDirectory() )
                    folders.add( new MenuOption( file.getName(), getString( R.string.file_parent_folder ), file.getAbsolutePath() ) );
                else
                {
                    filename = file.getName();
                    if( filename != null && filename.length() > 2 )
                    { // check what type of file it is
                        p = filename.lastIndexOf( "." );
                        if( p > -1 && p < filename.length() )
                        {
                            ext = filename.substring( p, filename.length() ).toLowerCase();
                            if( ext != null && ext.length() > 1 && extensions.contains( ext) )
                                files.add( new MenuOption( filename, getString( R.string.file_size ) + ": " + file.length(), file.getAbsolutePath() ) );
                        }
                    }
                }
            }
        }
        catch( Exception e )
        {}
        // Sort the folders and files separately before recombining them
        Collections.sort( folders );
        Collections.sort( files );
        folders.addAll( files );
        if( folder.getName() != null && folder.getName().length() != 0 ) // Make sure we aren't at root folder
            folders.add( 0, new MenuOption( "..", getString( R.string.file_parent_folder ), folder.getParent() ) );
        optionArrayAdapter = new OptionArrayAdapter( this, R.layout.menu_option, folders );
        setListAdapter( optionArrayAdapter );
    }

    /**
     * Determines what to do, based on if the user chose a folder or a file 
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
        if( menuOption.comment.equals( getString( R.string.file_current_folder ) ) || menuOption.comment.equals( getString( R.string.file_parent_folder ) ) )
        {  // Repopulate the menu with the folder that was chosen
            currentFolder = new File( menuOption.info );
            if( currentFolder != null )
            {
                switch( function )
                {
                    case FUNCTION_ROM:
                        MenuActivity.gui_cfg.put( "LAST_SESSION", "rom_folder", currentFolder.getAbsolutePath() );
                        break;
                    case FUNCTION_SO:
                        MenuActivity.gui_cfg.put( "LAST_SESSION", "so_folder", currentFolder.getAbsolutePath() );
                        break;
                    case FUNCTION_SKIN:
                        MenuActivity.gui_cfg.put( "LAST_SESSION", "skin_folder", currentFolder.getAbsolutePath() );
                        break;
                    case FUNCTION_TEXTURE:
                        MenuActivity.gui_cfg.put( "LAST_SESSION", "texture_folder", currentFolder.getAbsolutePath() );
                        break;
                }
            }
            populate( currentFolder );
        }
        else
        { // User picked a file
            onFileClick( menuOption );
        }
    }
    
    /**
     * Performs the desired action using the file chosen by the user.
     * @param menuOption Which file the user chose.
     */
    private void onFileClick( MenuOption menuOption )
    {
        String filename = menuOption.info;
        String ext = filename.substring( filename.length() - 3, filename.length() ).toLowerCase();
        if( parentMenu != null )
            parentMenu.optionChosen( menuOption.info );
        else
        {
            Globals.chosenROM = menuOption.info;
            File f = new File( Globals.StorageDir );
            if( !f.exists() )
            {
                Log.e( "FileChooserActivity", "SD Card not accessable in method onFileClick" );
                Runnable toastMessager = new Runnable()
                {
                    public void run()
                    {
                        Toast toast = Toast.makeText( MenuActivity.mInstance, getString( R.string.app_data_inaccessible ), Toast.LENGTH_LONG );
                        toast.setGravity( Gravity.BOTTOM, 0, 0 );
                        toast.show();
                    }
                };
                runOnUiThread( toastMessager );
                return;
            }
            MenuActivity.mupen64plus_cfg.save();
            //MenuActivity.InputAutoCfg_ini.save();
            MenuActivity.gui_cfg.put( "LAST_SESSION", "rom", Globals.chosenROM );
            MenuActivity.gui_cfg.save();
            GameActivityCommon.resumeLastSession = false;

            Intent intent;
            if( Globals.isXperiaPlay )
                intent = new Intent( this, GameActivityXperiaPlay.class );
            else
                intent = new Intent( this, GameActivity.class );

            intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
            startActivity( intent );
            if( MenuActivity.mInstance != null )
            {
                MenuActivity.mInstance.finish();
                MenuActivity.mInstance = null;
            }
        }
        finish();
        mInstance = null;
    }
}
