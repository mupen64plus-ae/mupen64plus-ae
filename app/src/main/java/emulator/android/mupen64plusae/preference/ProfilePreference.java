/*
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
package emulator.android.mupen64plusae.preference;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;

import org.apache.commons.lang3.ArrayUtils;
import org.mupen64plusae.v3.alpha.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import emulator.android.mupen64plusae.compat.AppCompatPreferenceActivity.OnPreferenceDialogListener;
import emulator.android.mupen64plusae.persistent.AppData;
import emulator.android.mupen64plusae.persistent.ConfigFile;
import emulator.android.mupen64plusae.profile.Profile;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class ProfilePreference extends ListPreference implements OnPreferenceDialogListener
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
    public void onPrepareDialogBuilder( Context context, Builder builder )
    {
        if( !TextUtils.isEmpty( mManagerAction ) )
        {
            ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                context, R.layout.list_preference, getEntries());
            
            int currentIndex = findIndexOfValue(getCurrentValue(null));
            builder.setTitle(getTitle());
            builder.setPositiveButton(null, null);
            builder.setSingleChoiceItems(adapter, currentIndex, (dialog, item) -> {
                setValue(getEntryValues()[item].toString());
                dialog.dismiss();
            });
            builder.setNeutralButton( R.string.profile_manage_profiles, (dialog, which) -> {
                Context context1 = ProfilePreference.this.getContext();

                Intent activityIntent = new Intent();
                activityIntent.setAction(mManagerAction);
                activityIntent.setPackage(context1.getPackageName());

                context1.startActivity( activityIntent );
            });
        }
    }

    /**
     *
     * @param configBuiltin Built in configuration
     * @param configCustom Custom configuration
     * @param allowDefaultProfile Allow using a default profile
     * @param defaultValue This has dual use. If a global default is enabled, it's used for its value. Otherwise
     *                     it's used for the default value if one isn't defined
     * @param exclusions Exclude These profiles
     * @param showBuiltins Show built in profiles
     */
    public void populateProfiles( ConfigFile configBuiltin, ConfigFile configCustom, boolean allowDefaultProfile,
        String defaultValue, List<Profile> exclusions, boolean showBuiltins )
    {
        //ConfigFile configBuiltin = new ConfigFile( builtinPath );
        //ConfigFile configCustom = new ConfigFile( customPath );
        List<Profile> profiles = new ArrayList<>();
        if(showBuiltins)
        {
            profiles.addAll( Profile.getProfiles( configBuiltin, true ) );
        }

        profiles.addAll( Profile.getProfiles( configCustom, false ) );
        
        if(exclusions != null)
        {
            profiles.removeAll(exclusions);
        }
        
        Collections.sort( profiles );

        Profile defaultProfile = null;
        CharSequence defaultProfileTitle = null;

        //Add Global default option
        if(allowDefaultProfile) {
            //Find the default profile and add it
            if (configCustom.keySet().contains(defaultValue)) {
                defaultProfile = new Profile(false, configCustom.get(defaultValue));
            } else if (configBuiltin.keySet().contains(defaultValue)) {
                defaultProfile = new Profile(true, configBuiltin.get(defaultValue));
            }

            //This is a fake profile that doesn't exist in any config file.
            //Selecting this profile will make us fall back to the current default profile
            defaultProfileTitle = getContext().getText(R.string.default_profile_title);

            //Label it as default
            if (defaultProfile != null) {
                String defaultProfileComment = defaultProfile.getName();

                if (defaultProfile.getComment() != null) {
                    defaultProfileComment += ": " + defaultProfile.getComment();
                }
                defaultProfile.setComment(defaultProfileComment);
                defaultProfile.setName(defaultProfileTitle.toString());

                //Add it at the beginning
                profiles.add(0, defaultProfile);
            } else if (mAllowDisable) {
                defaultProfile = new Profile(true, defaultProfileTitle.toString(), getContext().getText(
                        R.string.listItem_disabled).toString());
                // Add it at the beginning
                profiles.add(0, defaultProfile);
            }
        }

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
            entries[i + offset] = AppData.fromHtml( entryHtml );
            values[i + offset] = profile.name;
        }
        
        // Set the list entries and values; select default if persisted selection no longer exists
        setEntries( entries );
        setEntryValues( values );
        String selectedValue = getPersistedString( null );

        //If the provided selected value no longer exists, revert to the default
        if( !ArrayUtils.contains( values, selectedValue ) )
        {
            //If a global default is allowed, use the default, if not, use provided
            // default directly, else use disabled
            if(allowDefaultProfile && defaultProfile != null)
            {
                persistString( defaultProfileTitle.toString() );
            }
            else if (defaultValue != null)
            {
                persistString( defaultValue );
            }
            else if(mAllowDisable)
            {
                persistString("");
            }
        }

        selectedValue = getPersistedString( null );
        setValue( selectedValue );
    }
    
    public String getCurrentValue(String defaultValue)
    {
        return getPersistedString( defaultValue );
    }

    @Override
    public void onBindDialogView(View view, FragmentActivity associatedActivity)
    {
        //Nothing to do here
    }

    @Override
    public void onDialogClosed(boolean result)
    {
        //Nothing to do here
    }
}