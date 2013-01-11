package paulscode.android.mupen64plusae.persistent;

import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.Preference.BaseSavedState;

public class SavedIntegerState extends BaseSavedState
{
    int mValue;
    
    public SavedIntegerState( Parcel source )
    {
        super( source );
        mValue = source.readInt();
    }
    
    @Override
    public void writeToParcel( Parcel dest, int flags )
    {
        super.writeToParcel( dest, flags );
        dest.writeInt( mValue );
    }
    
    public SavedIntegerState( Parcelable superState )
    {
        super( superState );
    }
    
    public static final Parcelable.Creator<SavedIntegerState> CREATOR = new Parcelable.Creator<SavedIntegerState>()
    {
        public SavedIntegerState createFromParcel( Parcel in )
        {
            return new SavedIntegerState( in );
        }
        
        public SavedIntegerState[] newArray( int size )
        {
            return new SavedIntegerState[size];
        }
    };
    
    public static Parcelable onSaveInstanceState( final Parcelable superState,
            Preference preference, int value )
    {
        if( preference.isPersistent() )
        {
            // No need to save instance state since it's persistent
            return superState;
        }
        
        final SavedIntegerState myState = new SavedIntegerState( superState );
        myState.mValue = value;
        return myState;
    }
}
