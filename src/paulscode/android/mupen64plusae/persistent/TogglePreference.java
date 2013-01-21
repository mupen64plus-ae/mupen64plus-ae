package paulscode.android.mupen64plusae.persistent;

import paulscode.android.mupen64plusae.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class TogglePreference extends Preference implements OnCheckedChangeListener
{
    private boolean mValue;
    
    public TogglePreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        
        // Add a toggle to the preference
        setWidgetLayoutResource( R.layout.widget_toggle );
    }
    
    public void setValue( boolean value )
    {
        mValue = value;
        if( shouldPersist() )
            persistBoolean( mValue );
    }
    
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
