package paulscode.android.mupen64plusae.persistent;

import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.Preference.BaseSavedState;

public class SavedStringState extends BaseSavedState
{
    String mValue;
    
    public SavedStringState( Parcel source )
    {
        super( source );
        mValue = source.readString();
    }
    
    @Override
    public void writeToParcel( Parcel dest, int flags )
    {
        super.writeToParcel( dest, flags );
        dest.writeString( mValue );
    }
    
    public SavedStringState( Parcelable superState )
    {
        super( superState );
    }
    
    public static final Parcelable.Creator<SavedStringState> CREATOR = new Parcelable.Creator<SavedStringState>()
    {
        public SavedStringState createFromParcel( Parcel in )
        {
            return new SavedStringState( in );
        }
        
        public SavedStringState[] newArray( int size )
        {
            return new SavedStringState[size];
        }
    };
    
    public static Parcelable onSaveInstanceState( final Parcelable superState,
            Preference preference, String value )
    {
        if( preference.isPersistent() )
        {
            // No need to save instance state since it's persistent
            return superState;
        }
        
        final SavedStringState myState = new SavedStringState( superState );
        myState.mValue = value;
        return myState;
    }
}
