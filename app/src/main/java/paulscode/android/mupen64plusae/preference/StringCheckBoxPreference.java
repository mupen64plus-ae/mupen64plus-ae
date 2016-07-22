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
 */
package paulscode.android.mupen64plusae.preference;

import org.mupen64plusae.v3.fzurita.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.CheckBoxPreference;
import android.util.AttributeSet;

/**
 * A simple extension of the standard CheckBoxPreference that persists to string rather than
 * boolean. By default, the boolean value <code>true</code> is persisted as "True" and
 * <code>false</code> is persisted as "False". These strings can be overridden in the preferences
 * xml file using the attributes <code>mupen64:trueString</code> and
 * <code>mupen64:falseString</code>.
 */
public class StringCheckBoxPreference extends CheckBoxPreference
{
    private static final String DEFAULT_TRUE_STRING = "True";
    private static final String DEFAULT_FALSE_STRING = "False";
    private final String trueString;
    private final String falseString;
    
    public StringCheckBoxPreference( Context context )
    {
        super( context );
        trueString = DEFAULT_TRUE_STRING;
        falseString = DEFAULT_FALSE_STRING;
    }
    
    public StringCheckBoxPreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        
        // Get the attributes from the XML file, if provided
        TypedArray a = context.obtainStyledAttributes( attrs, R.styleable.StringCheckBoxPreference );
        String _trueString = a.getString( R.styleable.StringCheckBoxPreference_trueString );
        String _falseString = a.getString( R.styleable.StringCheckBoxPreference_falseString );
        trueString = _trueString == null ? DEFAULT_TRUE_STRING : _trueString;
        falseString = _falseString == null ? DEFAULT_FALSE_STRING : _falseString;
        a.recycle();
    }
    
    @Override
    protected boolean persistBoolean( boolean value )
    {
        return persistString( value ? trueString : falseString );
    }
    
    @Override
    protected boolean getPersistedBoolean( boolean defaultReturnValue )
    {
        if( getSharedPreferences().contains( getKey() ) )
        {
            String strValue = getPersistedString( defaultReturnValue ? trueString : falseString );
            return trueString.equals( strValue );
        }
        else
        {
            return defaultReturnValue;
        }
    }
}
