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
package paulscode.android.mupen64plusae.preference;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * An EditTextPreference that implements the formatted summary feature introduced in some Preference
 * types in API 14.
 */
public class CompatibleEditTextPreference extends EditTextPreference
{
    protected String mSummary;
    
    public CompatibleEditTextPreference( Context context )
    {
        super( context );
    }
    
    public CompatibleEditTextPreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        
        // Get the original summary string, including formatting arguments, from the parent
        // EditTextPreference class.
        String text = getText();
        setText( null );
        CharSequence rawSummary = getSummary();
        setText( text );

        mSummary = rawSummary == null ? null : rawSummary.toString();
    }
    
    /**
     * Returns the summary of this EditTextPreference. If the summary has a
     * {@linkplain java.lang.String#format String formatting} marker in it (i.e. "%s" or "%1$s"),
     * then the current entry value will be substituted in its place.
     * 
     * @return The summary with appropriate string substitution.
     */
    @Override
    public CharSequence getSummary()
    {
        final String text = getText();
        
        if( mSummary == null || text == null )
        {
            return super.getSummary();
        }
        else
        {
            return String.format( mSummary, text );
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