/*
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
package emulator.android.mupen64plusae.preference;

import android.content.SharedPreferences;
import android.content.res.Resources;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceGroup;

import org.apache.commons.lang3.ArrayUtils;

import emulator.android.mupen64plusae.compat.AppCompatPreferenceActivity;

public final class PrefUtil
{
    public static void setOnPreferenceClickListener( AppCompatPreferenceActivity activity, String key,
            OnPreferenceClickListener listener )
    {
        Preference preference = activity.findPreference( key );
        if( preference != null )
            preference.setOnPreferenceClickListener( listener );
    }
    
    public static void enablePreference( AppCompatPreferenceActivity activity, String key, boolean enabled )
    {
        Preference preference = activity.findPreference( key );
        if( preference != null )
            preference.setEnabled( enabled );
    }
    
    public static void removePreference( AppCompatPreferenceActivity activity, String keyParent,
            String keyChild )
    {
        Preference parent = activity.findPreference( keyParent );
        Preference child = activity.findPreference( keyChild );
        if( parent instanceof PreferenceGroup && child != null )
            ( (PreferenceGroup) parent ).removePreference( child );
    }

    public static void validateListPreference( Resources res, SharedPreferences prefs, String key, int defaultResId, int arrayResId )
    {
        String value = prefs.getString( key, null );
        String defValue = res.getString( defaultResId );
        String[] validValues = res.getStringArray( arrayResId );
        if( !ArrayUtils.contains( validValues, value ) )
        {
            prefs.edit().putString( key, defValue ).apply();
        }
    }
}
