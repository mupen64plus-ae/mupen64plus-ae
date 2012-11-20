package paulscode.android.mupen64plusae;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

// TODO: Comment thoroughly
public class MenuCheatsActivity extends ListActivity implements IOptionChooser
{
    public static MenuCheatsActivity mInstance = null;
    private OptionArrayAdapter optionArrayAdapter;  // array of menu options

    public static String ROM = null;
    public static String CRC = null;
    private static String ROM_name = null;
    private static String whichCheat = null;

    private static HashMap<String, String> Cheat_title = null;
    private static HashMap<String, String> Cheat_N = null;
    private static HashMap<String, String> Cheat_O = null;
    private static HashMap<String, String> Cheat_O_choice = null;

    private static int cheatIndex = -1;

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mInstance = this;

        Globals.checkLocale( this );

        List<MenuOption>optionList = new ArrayList<MenuOption>();
        optionList.add( new MenuOption( getString( R.string.cheats_start ), getString( R.string.cheats_launch_selected ), "menuCheatsActivityLaunch" ) );

        if( CRC != null )
        {
            Config mupen64plus_cht = new Config( Globals.DataDir + "/data/mupen64plus.cht" );
            Config.ConfigSection configSection = mupen64plus_cht.match( "^" + CRC.replace( ' ', '.' ) + ".*" );
            if( configSection == null )
            {
                Log.e( "MenuCheatsActivity", "No cheat section found for '" + CRC + "'" );
            }
            else
            {
                ROM_name = configSection.get( "Name" );
                if( ROM_name == null || ROM_name.length() < 1 )
                    setTitle( getString( R.string.manifest_cheats ) );
                else
                    setTitle( getString( R.string.cheats_for ) + ROM_name );

                Cheat_title = new HashMap<String, String>();
                Cheat_N = new HashMap<String, String>();
                Cheat_O = new HashMap<String, String>();
                Cheat_O_choice = new HashMap<String, String>();

                int x;
                String val_N, val_O, name, info;
                String val = " ";
                for( int i = 0; val != null && val.length() > 0; i++ )
                {
                    val = configSection.get( "Cheat" + i );
                    if( val != null && val.length() > 0 )
                    {
                        x = val.indexOf( "," );
                        if( x < 3 || x >= val.length() )
                            name = getString( R.string.cheats_default_name ) + " " + i;
                        else
                            name = val.substring( 1, x - 1 );
                        Cheat_title.put( "Cheat" + i, name );
                        info = "";
                        val_N = configSection.get( "Cheat" + i + "_N" );
                        if( val_N != null && val_N.length() > 0 )
                        {
                            Cheat_N.put( "Cheat" + i, val_N );
                            info = getString( R.string.cheats_long_notes );
                        }
                        val_O = configSection.get( "Cheat" + i + "_O" );
                        if( val_O != null && val_O.length() > 0 )
                            Cheat_O.put( "Cheat" + i, val_O );
                        Cheat_O_choice.put( "Cheat" + i, "false" );
                        optionList.add( new MenuOption( name, info, "Cheat" + i, false ) );
                    }
                }
            }
        }

        optionArrayAdapter = new OptionArrayAdapter( this, R.layout.menu_option, optionList );
        setListAdapter( optionArrayAdapter );

        ListView listView = getListView();
        if( listView == null )
        {
            Log.e( "MenuCheatsActivity", "getListView() returned null in method onCreate" );
        }
        else
        {
            listView.setOnItemLongClickListener
            (
                new AdapterView.OnItemLongClickListener()
                {
                    public boolean onItemLongClick( AdapterView<?> adapterView, View view, int position, long id )
                    {
                        onLongListItemClick( view, position, id );
                        return true;
                    }
                }
            );
        }
    }

    public void optionChosen( String option )
    { // selected an option for one of the cheats

        MenuOption menuOption = optionArrayAdapter.getOption( cheatIndex );

        if( option.equals( "menuCheatsOptionChooserDisable" ) )
        {
            Cheat_O_choice.put( menuOption.info, "false" );

            String comment = "";
            String N = Cheat_N.get( menuOption.info );
            if( N != null )
                comment = getString( R.string.cheats_long_notes );

            optionArrayAdapter.remove( menuOption );
            optionArrayAdapter.insert( new MenuOption( menuOption.name, comment, menuOption.info,
                                                       false ), cheatIndex );
        }
        else
        {
            Cheat_O_choice.put( menuOption.info, String.valueOf( MenuCheatsOptionChooserActivity.optionIndex ) );
            optionArrayAdapter.remove( menuOption );
            optionArrayAdapter.insert( new MenuOption( menuOption.name, option, menuOption.info,
                                                       true ), cheatIndex );
        }
    }

    protected void onLongListItemClick( View view, int position, long id )
    {
        MenuOption menuOption = optionArrayAdapter.getOption( position );
        if( menuOption.info != null && !menuOption.info.equals( "menuCheatsActivityLaunch" ))
        {
            String title;   // Title of the dialog
            String message; // Dialog text
            
            whichCheat = menuOption.info;
            
            // Determine dialog title
            if (Cheat_title == null)
                title = getString(R.string.cheats_notes);
            else
            {
                title = Cheat_title.get(whichCheat);
                if(title == null || title.length() < 1)
                {
                    title = getString(R.string.cheats_notes);
                }
            }
            
            // Determine dialog message
            if (Cheat_N == null)
                message = getString(R.string.cheats_no_notes);
            else
            {
                message = Cheat_N.get(whichCheat);
                if(message == null || message.length() < 1)
                {
                    message = getString(R.string.cheats_no_notes);
                }
            }
            
            AlertFragment cheatFrag = AlertFragment.newInstance(title, message);
            cheatFrag.show(getFragmentManager(), "cheatFrag");               
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

        if( menuOption.info != null )
        {
            if( menuOption.info.equals( "menuCheatsActivityLaunch" ) )
            {
                Globals.chosenROM = ROM;
                File f = new File( Globals.StorageDir );
                if( !f.exists() )
                {
                    Log.e( "MenuCheatsActivity", "SD Card not accessable in method onListItemClick" );
                    Runnable toastMessager = new Runnable()
                    {
                        public void run()
                        {
                            Toast toast = Toast.makeText( MenuCheatsActivity.mInstance,
                                getString( R.string.app_data_inaccessible ),
                                Toast.LENGTH_LONG );
                                toast.setGravity( Gravity.BOTTOM, 0, 0 );
                                toast.show();
                        }
                    };
                    this.runOnUiThread( toastMessager );
                    return;
                }
                Globals.extraArgs = "-1"; // TODO: should hold "--cheats" arg too, not just cheat-specs
                String opt = " ";
                for( int i = 0; opt != null && opt.length() > 0; i++ )
                {
                    opt = Cheat_O_choice.get( "Cheat" + i );
                    if( opt != null && opt.length() > 0 && !opt.equals( "false" ) )
                    {
                        if( Globals.extraArgs.equals( "-1" ) )
                            Globals.extraArgs = "";
                        else
                            Globals.extraArgs += ",";
                        Globals.extraArgs += i;
                        if( !opt.equals( "true" ) )
                            Globals.extraArgs += "-" + opt;
                    }
                }

                MenuActivity.mupen64plus_cfg.save();
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
                finish();
                mInstance = null;
            }
            else
            {
                String option = Cheat_O.get( menuOption.info );
                if( option != null && option.length() > 0 )
                { // cheat has options, let user pick one
                    cheatIndex = position;
                    MenuCheatsOptionChooserActivity.optionsString = option;
                    MenuCheatsOptionChooserActivity.parent = this;
                    Intent intent = new Intent( mInstance, MenuCheatsOptionChooserActivity.class );
                    intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                    startActivity( intent );
                }
                else
                { // cheat is a boolean on/off
                    option = Cheat_O_choice.get( menuOption.info );
                    if( option != null )
                    {
                        boolean b = !option.equals( "true" );
                        if( b )
                            Cheat_O_choice.put( menuOption.info, "true" );
                        else
                            Cheat_O_choice.put( menuOption.info, "false" );

                        optionArrayAdapter.remove( menuOption );
                        optionArrayAdapter.insert( new MenuOption( menuOption.name, menuOption.comment, menuOption.info,
                                                                   b ), position );
                    }
                }
            }
        }
    }
}

