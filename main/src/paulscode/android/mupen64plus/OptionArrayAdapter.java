package paulscode.android.mupen64plus;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * The OptionArrayAdapter class is used to handle an array of menu options.
 *
 * @author: Paul Lamb
 * 
 * http://www.paulscode.com
 * 
 */
public class OptionArrayAdapter extends ArrayAdapter<MenuOption>
{
    private Context context;
    private int id;
    private List<MenuOption> options;
    
    /**
     * Constructor: Instantiates the array adapter
     * @param context Context for accessing resources.
     * @param viewResourceId Id of the view to inflate for each menu option.
     * @param options List of menu options.
     */
    public OptionArrayAdapter( Context context, int viewResourceId,
                               List<MenuOption> options )
    {
        super( context, viewResourceId, options );
        this.context = context;
        id = viewResourceId;
        this.options = options;
    }
    
    /**
     * Returns a handle to the specified menu option
     * @param x Index of the desired menu option.
     */
    public MenuOption getOption( int x )
    {
        return options.get( x );
    }
    
    /**
     * Inflates the menu option view and populates it with the correct text
     * @param position Menu option to create a view for.
     * @param convertView Handle to an existing view, or null if one needs to be created.
     * @param parent Not used.
     * @return The newly created view.
     */
    @Override
    public View getView( int position, View convertView, ViewGroup parent )
    {
        View view = convertView;
        if( view == null )
        { // Create a new one
            LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            view = inflater.inflate( id, null );
        }
        final MenuOption menuOption = options.get( position );
        if( menuOption != null )
        { // Populate it with the correct text
            TextView name = (TextView) view.findViewById( R.id.menuOptionName );
            TextView comment = (TextView) view.findViewById( R.id.menuOptionComment );
            TextView check = (TextView) view.findViewById( R.id.menuOptionChecked );
            if( name!=null )
                name.setText( menuOption.name );
            if( comment!=null )
                comment.setText( menuOption.comment );
            if( check != null )
            {
                if( menuOption.hasCheckbox )
                {
                    check.setText( "X" );
                    if( menuOption.checked )
                        check.setTextColor( 0xff88ff99 );
                    else
                        check.setTextColor( 0xff333333 );
                }
                else 
                    check.setText( " " );
            }
        }
        return view;  // Done
    } 
}
