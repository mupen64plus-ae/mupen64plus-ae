package paulscode.android.mupen64plusae.persistent;

import paulscode.android.mupen64plusae.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * A type of {@link Preference} that allows setting its internal value to true or false.
 */
public class TogglePreference extends Preference implements OnCheckedChangeListener
{
    // internal value
    private boolean mValue;

    /**
     * Constructor
     *
     * @param context The {@link Context} that this TogglePreference is being used in.
     * @param attrs   A collection of attributes, as found associated with a tag in an XML document.
     */
    public TogglePreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        
        // Add a toggle to the preference
        setWidgetLayoutResource( R.layout.widget_toggle );
    }

    /**
     * Sets the internal value of this TogglePreference according to the value passed.
     * @param value The value to set the internal value of this TogglePreference to.
     */
    public void setValue( boolean value )
    {
        mValue = value;
        if( shouldPersist() )
            persistBoolean( mValue );
    }

    /**
     * Gets the internal value that is currently set.
     * @return The currently set internal value for this TogglePreference.
     */
    public boolean getValue()
    {
        return mValue;
    }
    
    @Override
    protected Object onGetDefaultValue( TypedArray a, int index )
    {
        return a.getBoolean( index, false );
    }
    
    @Override
    protected void onSetInitialValue( boolean restorePersistedValue, Object defaultValue )
    {
        setValue( restorePersistedValue ? getPersistedBoolean( mValue ) : (Boolean) defaultValue );
    }
    
    @Override
    protected void onBindView( View view )
    {
        // Set up the menu item seen in the preferences menu
        super.onBindView( view );
        
        // Get the toggle widget, set its state, and define its callback
        CompoundButton toggleWidget = (CompoundButton) view.findViewById( R.id.widgetToggle );
        toggleWidget.setChecked( mValue );
        toggleWidget.setOnCheckedChangeListener( this );
    }
    
    @Override
    public void onCheckedChanged( CompoundButton buttonView, boolean isChecked )
    {
        setValue( isChecked );
    }
}
