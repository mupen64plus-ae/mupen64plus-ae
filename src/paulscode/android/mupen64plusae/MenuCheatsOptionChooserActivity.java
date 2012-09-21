package paulscode.android.mupen64plusae;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

// TODO: Comment thoroughly
public class MenuCheatsOptionChooserActivity extends ListActivity
{
    public static MenuCheatsOptionChooserActivity mInstance = null;
    private OptionArrayAdapter optionArrayAdapter;  // array of menu options
    public static IOptionChooser parent = null;
    public static String optionsString = null;
    public static int optionIndex = -1;
    private static String pickedDescription = null;

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mInstance = this;

        Globals.checkLocale( this );

        List<MenuOption>optionList = new ArrayList<MenuOption>();
        optionList.add( new MenuOption( getString( R.string.cheats_disable ), getString( R.string.cheats_dont_use ), "menuCheatsOptionChooserDisable" ) );

        if( optionsString != null && optionsString.length() > 0 )
        {
            String[] options = optionsString.split( "," );
            int x;
            String opt;
            for( String option : options )
            {
                opt = option;
                if( opt != null )
                    opt = opt.trim();
                if( opt != null && opt.length() > 0 )
                {
                    x = opt.indexOf( " " );
                    if( x > -1 && x < opt.length() - 1 )
                        optionList.add( new MenuOption( opt.substring( x + 1 ), getString( R.string.cheats_long_full_text ),
                                                        opt.substring( x + 1 ) ) );
                    else
                        optionList.add( new MenuOption( opt, getString( R.string.cheats_long_full_text ), opt ) );
                }
            }
        }

        optionArrayAdapter = new OptionArrayAdapter( this, R.layout.menu_option, optionList );
        setListAdapter( optionArrayAdapter );

        ListView listView = getListView();
        if( listView == null )
        {
            Log.e( "MenuCheatsOptionChooserActivity", "getListView() returned null in method onCreate" );
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

    protected void onLongListItemClick( View view, int position, long id )
    {
        MenuOption menuOption = optionArrayAdapter.getOption( position );
        if( menuOption.info != null && !menuOption.info.equals( "menuCheatsOptionChooserDisable" ) )
        {
            pickedDescription = menuOption.info.trim();
            removeDialog( Globals.CHEAT_N_ID );
            showDialog( Globals.CHEAT_N_ID );                        
        }
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
        optionIndex = position - 1;
        if( parent != null )
            parent.optionChosen( menuOption.info );
        finish();
    }

    @Override
    protected Dialog onCreateDialog( int id )
    {
        switch( id )
        {
            case Globals.CHEAT_N_ID:
            {
                AlertDialog.Builder d = new AlertDialog.Builder( this );

                d.setTitle( getString( R.string.cheats_full_text ) );

                d.setIcon( R.drawable.icon );
                d.setNegativeButton( getString( R.string.cheats_close ), null );
                View v = LayoutInflater.from( this ).inflate( R.layout.about_dialog, null );
                TextView text = (TextView) v.findViewById( R.id.about_text );

                if( pickedDescription == null )
                    text.setText( getString( R.string.cheats_no_description ) );
                else
                    text.setText( pickedDescription );

                d.setView( v );
                return d.create();
            }
        }
        return( super.onCreateDialog( id ) );
    }
}
