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

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * A ListPreference that back-ports the formatted summary feature introduced in API 14, and
 * implements a workaround for Android Issue 27867.
 * http://code.google.com/p/android/issues/detail?id=27867
 */
public class CompatibleListPreference extends ListPreference
{
    protected String mSummary;
    
    public CompatibleListPreference( Context context )
    {
        super( context );
    }
    
    public CompatibleListPreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        
        // Get the original summary string, including formatting arguments, from the grandparent
        // Preference class. Looking at the Android source code, the only way to do this for ICS+ is
        // to first set the entry to null (via setValue) so that ListPreference.getSummary() returns
        // Preference.getSummary().
        String value = getValue();
        setValue( null );
        CharSequence rawSummary = super.getSummary();
        setValue( value );
        
        mSummary = rawSummary == null ? null : rawSummary.toString();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see android.preference.ListPreference#setValue(java.lang.String)
     */
    @Override
    public void setValue( final String value )
    {
        // Workaround for Android Issue 27867.
        // http://code.google.com/p/android/issues/detail?id=27867
        super.setValue( value );
        notifyChanged();
    }
    
    /**
     * Returns the summary of this ListPreference. If the summary has a
     * {@linkplain java.lang.String#format String formatting} marker in it (i.e. "%s" or "%1$s"),
     * then the current entry value will be substituted in its place.
     * 
     * @return The summary with appropriate string substitution.
     */
    @Override
    public CharSequence getSummary()
    {
        final CharSequence entry = getEntry();
        
        if( mSummary == null || entry == null )
        {
            return super.getSummary();
        }
        else
        {
            return String.format( mSummary, entry );
        }
    }
    
    /**
     * Sets the summary for this Preference with a CharSequence. If the summary has a
     * {@linkplain java.lang.String#format String formatting} marker in it (i.e. "%s" or "%1$s"),
     * then the current entry value will be substituted in its place when it's retrieved.
     * 
     * @param summary The summary for the preference.
     */
    @Override
    public void setSummary( CharSequence summary )
    {
        super.setSummary( summary );
        if( summary == null && mSummary != null )
        {
            mSummary = null;
        }
        else if( summary != null && !summary.equals( mSummary ) )
        {
            mSummary = summary.toString();
        }
    }
}