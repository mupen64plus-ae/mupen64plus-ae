package paulscode.android.mupen64plus;

/**
 * The MenuOption class is used to add options to a list-style menu.
 * Each menu option consists of two lines: the name, and a comment.
 * The menu option is able to store additional information as well
 * (such as a full file path, for example).
 *
 * @author: Paul Lamb
 * 
 * http://www.paulscode.com
 * 
 */
public class MenuOption implements Comparable<MenuOption>
{
    public String name;     // The main text for the option
    public String comment;  // An optional supporting comment
    public String info;     // Optional additional information
    public boolean hasCheckbox = false;
    public boolean checked = false;

    /**
     * Constructor: Instantiates the menu option and stores the values
     * @param name Main text for the option.
     * @param comment An optional supporting comment.
     * @param info Optional additional information.
     */
    public MenuOption( String name, String comment, String info )
    {
        this.name = name;
        this.comment = comment;
        this.info = info;
    }
    
    /**
     * Constructor: Instantiates a checkbox menu option and stores the values
     * @param name Main text for the option.
     * @param comment An optional supporting comment.
     * @param info Optional additional information.
     * @param checked Whether or not the box is checked.
     */
    public MenuOption( String name, String comment, String info, boolean checked )
    {
        this.name = name;
        this.comment = comment;
        this.info = info;
        hasCheckbox = true;
        this.checked = checked;
    }
    
    /**
     * Used for sorting menu options alphabetically by name
     * @param o Menu option to compare this one to.
     * @return -1, 0, or 1, representing "before", "equal", or "after".
     */
    @Override
    public int compareTo( MenuOption o )
    {
        if( name != null )
            return name.toLowerCase().compareTo( o.name.toLowerCase() );
        else
            throw new IllegalArgumentException();
    }
}
