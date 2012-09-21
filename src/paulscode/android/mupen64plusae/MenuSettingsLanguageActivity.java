package paulscode.android.mupen64plusae;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

// TODO: Comment thoroughly
public class MenuSettingsLanguageActivity extends ListActivity
{
    public static MenuSettingsLanguageActivity mInstance = null;
    private OptionArrayAdapter optionArrayAdapter;  // Array of menu options
    //private ScancodeDialog scancodeDialog = null;

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mInstance = this;

        Globals.checkLocale( this );

        List<MenuOption>optionList = new ArrayList<MenuOption>();

        optionList.add( new MenuOption( getString( R.string.language_default ), getString( R.string.language_use_system_locale ), "default" ) );
        optionList.add( new MenuOption( getString( R.string.native_croatian ), getString( R.string.language_croatian ), "hr" ) );
        optionList.add( new MenuOption( getString( R.string.native_dutch ), getString( R.string.language_dutch ), "nl" ) );
        optionList.add( new MenuOption( getString( R.string.native_english ), getString( R.string.language_english ), "en" ) );
        optionList.add( new MenuOption( getString( R.string.native_french ), getString( R.string.language_french ), "fr") );
        optionList.add( new MenuOption( getString( R.string.native_german ), getString( R.string.language_german ), "de" ) );
        optionList.add( new MenuOption( getString( R.string.native_japanese ), getString( R.string.language_japanese ), "ja" ) );
        optionList.add( new MenuOption( getString( R.string.native_norwegian ), getString( R.string.language_norwegian ), "no" ) );
        optionList.add( new MenuOption( getString( R.string.native_portuguese ), getString( R.string.language_portuguese ), "pt" ) );
        optionList.add( new MenuOption( getString( R.string.native_spanish ), getString( R.string.language_spanish ), "es" ) );

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

        if( menuOption.info.equals( "default" ) )
            Globals.locale = Globals.locale_default;
        else
        {
            if( Globals.locale_default == null )
                Globals.locale_default = getBaseContext().getResources().getConfiguration().locale;
            Globals.locale = new Locale( menuOption.info );
        }

//        MenuSettingsActivity.mInstance.finish();
//        MenuSettingsActivity.mInstance = null;   // Moved to MenuActivity, in case language changed accidentally
        MenuActivity.mInstance.finish();
        MenuActivity.mInstance = null;
        Intent intent = new Intent( this, MenuActivity.class );
        intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
        startActivity( intent );
        finish();
        mInstance = null;
    }
}
