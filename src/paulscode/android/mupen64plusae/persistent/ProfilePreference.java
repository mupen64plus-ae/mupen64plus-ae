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
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.TypedArray;
import android.text.Html;
import android.text.TextUtils;
import android.util.AttributeSet;

public class ProfilePreference extends CompatibleListPreference
{
    private static final boolean DEFAULT_ALLOW_DISABLE = false;
    
    private final boolean mAllowDisable;
    private final String mManagerAction;
    
    public ProfilePreference( Context context )
    {
        super( context );
        mAllowDisable = DEFAULT_ALLOW_DISABLE;
        mManagerAction = null;
    }
    
    public ProfilePreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        TypedArray a = context.obtainStyledAttributes( attrs, R.styleable.ProfilePreference );
        mAllowDisable = a.getBoolean( R.styleable.ProfilePreference_allowDisable,
                DEFAULT_ALLOW_DISABLE );
        mManagerAction = a.getString( R.styleable.ProfilePreference_managerAction );
        a.recycle();
    }
    
    @Override
    protected void onPrepareDialogBuilder( Builder builder )
    {
        super.onPrepareDialogBuilder( builder );
        if( !TextUtils.isEmpty( mManagerAction ) )
        {
            builder.setNeutralButton( R.string.profile_manage_profiles, new OnClickListener()
            {
                @Override
                public void onClick( DialogInterface dialog, int which )
                {
                    Context context = ProfilePreference.this.getContext();
                    context.startActivity( new Intent( mManagerAction ) );
                }
            } );
        }
    }
    
    public void populateProfiles( String builtinPath, String customPath, String defaultValue )
    {
        ConfigFile configBuiltin = new ConfigFile( builtinPath );
        ConfigFile configCustom = new ConfigFile( customPath );
        List<Profile> profiles = new ArrayList<Profile>();
        profiles.addAll( Profile.getProfiles( configBuiltin, true ) );
        profiles.addAll( Profile.getProfiles( configCustom, false ) );
        Collections.sort( profiles );
        
        int offset = mAllowDisable ? 1 : 0;
        int numEntries = profiles.size() + offset;
        CharSequence[] entries = new CharSequence[numEntries];
        String[] values = new String[numEntries];
        if( mAllowDisable )
        {
            entries[0] = getContext().getText( R.string.listItem_disabled );
            values[0] = "";
        }
        for( int i = 0; i < profiles.size(); i++ )
        {
            Profile profile = profiles.get( i );
            String entryHtml = profile.name;
            if( !TextUtils.isEmpty( profile.comment ) )
                entryHtml += "<br><small>" + profile.comment + "</small>";
            entries[i + offset] = Html.fromHtml( entryHtml );
            values[i + offset] = profile.name;
        }
        
        // Set the list entries and values; select default if persisted selection no longer exists
        setEntries( entries );
        setEntryValues( values );
        String selectedValue = getPersistedString( null );
        if( !ArrayUtils.contains( values, selectedValue ) )
            persistString( defaultValue );
        selectedValue = getPersistedString( null );
        setValue( selectedValue );
    }
}
