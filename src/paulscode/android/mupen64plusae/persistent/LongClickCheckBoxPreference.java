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
 * Authors: Paul Lamb
 */
package paulscode.android.mupen64plusae.persistent;

import android.content.Context;
import android.view.View;
import android.preference.CheckBoxPreference;

public class LongClickCheckBoxPreference extends CheckBoxPreference implements View.OnLongClickListener, View.OnClickListener
{
    private OnPreferenceLongClickListener mListener = null;
    
    public LongClickCheckBoxPreference( Context context )
    {
        super( context );
    }

    @Override
    protected void onBindView( View view )
    {
        super.onBindView( view );
        view.setOnLongClickListener( this );
        view.setOnClickListener( this );
    }

    public void setLongClickListener( OnPreferenceLongClickListener listener )
    {
        mListener = listener;
    }
    
    public boolean onLongClick( View view )
    {
        if( mListener != null )
            mListener.onPreferenceLongClick( this );
        return false;
    }
    public void onClick( View view )
    {
        // Setting the View's OnLongClickListener causes this.onClick() to stop firing
        onClick();
    }
}
