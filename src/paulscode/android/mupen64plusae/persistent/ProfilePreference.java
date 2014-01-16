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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.profile.Profile;
import android.content.Context;
import android.text.Html;
import android.text.TextUtils;
import android.util.AttributeSet;

public class ProfilePreference extends CompatibleListPreference
{
    private String mDefaultValue = null;
    
    public ProfilePreference( Context context )
    {
        super( context );
    }
    
    public ProfilePreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
    }
    
    @Override
    protected void onSetInitialValue( boolean restoreValue, Object defaultValue )
    {
        super.onSetInitialValue( restoreValue, defaultValue );
        mDefaultValue = (String) defaultValue;
    }
    
    public void populateProfiles( String builtinPath, String customPath, boolean allowDisable )
    {
        ConfigFile configBuiltin = new ConfigFile( builtinPath );
        ConfigFile configCustom = new ConfigFile( customPath );
        List<Profile> profiles = new ArrayList<Profile>();
        profiles.addAll( Profile.getProfiles( configBuiltin, true ) );
        profiles.addAll( Profile.getProfiles( configCustom, false ) );
        Collections.sort( profiles );
        
        int offset = allowDisable ? 1 : 0;
        int numEntries = profiles.size() + offset;
        CharSequence[] entries = new CharSequence[numEntries];
        String[] values = new String[numEntries];
        if( allowDisable )
        {
            entries[0] = getContext().getText( R.string.listItem_disabled );
            values[0] = "";
        }
        for( int i = 0; i < profiles.size(); i++ )
        {
            Profile profile = profiles.get( i );
            int resId = profile.isBuiltin
                    ? R.string.listItem_profileBuiltin
                    : R.string.listItem_profileCustom;
            String entryHtml = getContext().getString( resId, profile.name );
            if( !TextUtils.isEmpty( profile.comment ) )
                entryHtml += "<br><small>" + profile.comment + "</small>";
            entries[i + offset] = Html.fromHtml( entryHtml );
            values[i + offset] = profile.name;
        }
        
        populateListPreference( entries, values );
    }
    
    private void populateListPreference( CharSequence[] entries, String[] values )
    {
        setEntries( entries );
        setEntryValues( values );
        String selectedValue = getPersistedString( null );
        if( !ArrayUtils.contains( values, selectedValue ) )
            persistString( mDefaultValue );
        selectedValue = getPersistedString( null );
        setValue( selectedValue );
    }
}
