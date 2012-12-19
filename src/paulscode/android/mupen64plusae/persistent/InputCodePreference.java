/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2012 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with Mupen64PlusAE. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.persistent;

import java.util.List;

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.Prompt.OnInputCodeListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class InputCodePreference extends Preference
{
    private int mValue;
    
    public InputCodePreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
    }
    
    @Override
    protected Object onGetDefaultValue( TypedArray a, int index )
    {
        return a.getInteger( index, 0 );
    }
    
    @Override
    protected void onSetInitialValue( boolean restorePersistedValue, Object defaultValue )
    {
        if( restorePersistedValue )
        {
            mValue = getPersistedInt( 0 );
        }
        else
        {
            mValue = (Integer) defaultValue;
            persistInt( mValue );
        }
    }
    
    @Override
    protected View onCreateView( ViewGroup parent )
    {
        View view = super.onCreateView( parent );
        setSummary( mValue == 0 ? "" : AbstractProvider.getInputName( mValue ) );
        return view;
    }
    
    @Override
    protected void onClick()
    {
        super.onClick();
        
        String message = getContext().getString( R.string.inputMapPreference_popupMessage );
        String btnText = getContext().getString( R.string.inputMapPreference_popupPosButtonText );
        List<Integer> unmappableKeyCodes = ( new UserPrefs( getContext() ) ).unmappableKeyCodes;
        
        Prompt.promptInputCode( getContext(), getTitle(), message, btnText, unmappableKeyCodes,
                new OnInputCodeListener()
                {
                    @Override
                    public void OnInputCode( int inputCode, int hardwareId )
                    {
                        mValue = inputCode;
                        persistInt( mValue );
                        notifyChanged();
                    }
                } );
    }
}
