/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2013 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
 * not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: littleguy77
 * 
 * References: http://developer.android.com/guide/topics/ui/settings.html#CustomSaveState
 */
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
        @Override
        public SavedIntegerState createFromParcel( Parcel in )
        {
            return new SavedIntegerState( in );
        }

        @Override
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
