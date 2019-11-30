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
package paulscode.android.mupen64plusae.profile;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;

import org.mupen64plusae.v3.alpha.R;

import java.util.ArrayList;
import java.util.Arrays;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.preference.CompatListPreference;
import paulscode.android.mupen64plusae.preference.PrefUtil;

public class EmulationProfileActivity extends ProfileActivity
{
    // These constants must match the keys found in preferences_emulation.xml
    private static final String SCREEN_ROOT = "screenRoot";
    private static final String CATEGORY_RICE = "categoryRice";
    private static final String CATEGORY_GLN64 = "categoryGln64";
    private static final String CATEGORY_GLIDE64 = "categoryGlide64";
    private static final String CATEGORY_GLIDE64_ADVANCED = "categoryGlide64Advanced";
    private static final String CATEGORY_GLIDEN64_TEXTURE = "categoryGliden64Texture";
    private static final String CATEGORY_GLIDEN64_GENERAL = "categoryGliden64General";
    private static final String CATEGORY_GLIDEN64_2D = "categoryGliden642d";
    private static final String CATEGORY_GLIDEN64_FRAME_BUFFER = "categoryGliden64FrameBuffer";
    private static final String CATEGORY_GLIDEN64_TEXTURE_FILTERING = "categoryGliden64TextureFiltering";
    private static final String CATEGORY_GLIDEN64_GAMMA = "categoryGliden64Gamma";
    private static final String CATEGORY_ANGRYLION = "categoryAngrylion";

    private static final String RSP_PLUGIN = "rspSetting";
    private static final String VIDEO_PLUGIN = "videoPlugin";

    // These constants must match the entry-values found in arrays.xml
    private static final String LIBGLIDE64_SO = "libmupen64plus-video-glide64mk2.so";
    private static final String LIBGLIDEN64_SO = "mupen64plus-video-GLideN64.so";
    private static final String LIBRICE_SO = "libmupen64plus-video-rice.so";
    private static final String LIBGLN64_SO = "libmupen64plus-video-gln64.so";
    private static final String LIBANGRYLION_SO = "mupen64plus-video-angrylion-plus.so";

    private String mCurrentVideoPlugin = null;
    
    @Override
    protected int getPrefsResId()
    {
        return R.xml.profile_emulation;
    }
    
    @Override
    protected String getConfigFilePath()
    {
        AppData appData = new AppData( this );
        return new GlobalPrefs( this, appData ).emulationProfiles_cfg;
    }
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Ensure that selected plugin names and other list preferences are valid
        Resources res = getResources();
        PrefUtil.validateListPreference( res, mPrefs, VIDEO_PLUGIN, R.string.videoPlugin_default,
                R.array.videoPlugin_values );
    }
    
    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
    {
        if(key.equals("videoPlugin"))
        {
            resetPreferences();
        }

        Preference pref = findPreference(key);
        
        if (pref instanceof EditTextPreference)
        {
            EditTextPreference editTextPref = (EditTextPreference) pref;
            pref.setSummary(editTextPref.getText());
        }

        super.onSharedPreferenceChanged( sharedPreferences, key );
    }

    @Override
    protected void refreshViews()
    {
        // Get some menu items for use later
        PreferenceGroup screenRoot = (PreferenceGroup) findPreference( SCREEN_ROOT );
        PreferenceCategory categoryN64 = (PreferenceCategory) findPreference( CATEGORY_GLN64 );
        PreferenceCategory categoryRice = (PreferenceCategory) findPreference( CATEGORY_RICE );
        PreferenceCategory categoryGlide64 = (PreferenceCategory) findPreference( CATEGORY_GLIDE64 );
        PreferenceCategory categoryGlide64Advanced = (PreferenceCategory) findPreference( CATEGORY_GLIDE64_ADVANCED);
        PreferenceCategory categoryGliden64Texture = (PreferenceCategory) findPreference( CATEGORY_GLIDEN64_TEXTURE );
        PreferenceCategory categoryGliden64General = (PreferenceCategory) findPreference( CATEGORY_GLIDEN64_GENERAL );
        PreferenceCategory categoryGliden642d = (PreferenceCategory) findPreference( CATEGORY_GLIDEN64_2D );
        PreferenceCategory categoryGliden64FrameBuffer = (PreferenceCategory) findPreference( CATEGORY_GLIDEN64_FRAME_BUFFER );
        PreferenceCategory categoryGliden64TextureFiltering = (PreferenceCategory) findPreference( CATEGORY_GLIDEN64_TEXTURE_FILTERING );
        PreferenceCategory categoryGliden64Gamma = (PreferenceCategory) findPreference( CATEGORY_GLIDEN64_GAMMA );
        PreferenceCategory categoryAngrylion = (PreferenceCategory) findPreference( CATEGORY_ANGRYLION );


        CompatListPreference preferenceRspPlugin = (CompatListPreference) findPreference( RSP_PLUGIN );
        CompatListPreference preferenceVideoPlugin = (CompatListPreference) findPreference( VIDEO_PLUGIN );

        // Get the current values
        String videoPlugin = mPrefs.getString( VIDEO_PLUGIN, null );
        
        String openGlVersion = AppData.getOpenGlEsVersion(this);

        //Remove or add options depending on GLES version
        if(openGlVersion.equals("2.0"))
        {
            if(preferenceVideoPlugin != null) {
                //Don't allow angrylion
                ArrayList<CharSequence> videoEntriesArray = new ArrayList<>(Arrays.asList(preferenceVideoPlugin.getEntries()));
                ArrayList<CharSequence> videoValuesArray = new ArrayList<>(Arrays.asList(preferenceVideoPlugin.getEntryValues()));

                int angryLionIndex = videoEntriesArray.indexOf(getText(R.string.videoPlugin_entryAngrylion));
                if(angryLionIndex != -1)
                {
                    videoEntriesArray.remove(angryLionIndex);
                    videoValuesArray.remove("mupen64plus-video-angrylion-plus.so");
                }

                preferenceVideoPlugin.setEntries(videoEntriesArray.toArray(new CharSequence[0]));
                preferenceVideoPlugin.setEntryValues(videoValuesArray.toArray(new CharSequence[0]));
            }
        }
        
        // Hide certain categories altogether if they're not applicable. Normally we just rely on
        // the built-in dependency disabler, but here the categories are so large that hiding them
        // provides a better user experience.
        
        if(categoryN64 != null)
        {
            if( LIBGLN64_SO.equals( videoPlugin ) )
            {
                screenRoot.addPreference( categoryN64 );
            }
            else
            {
                screenRoot.removePreference( categoryN64 );
            }
        }

        if(categoryRice != null)
        {
            if( LIBRICE_SO.equals( videoPlugin ) )
            {
                screenRoot.addPreference( categoryRice );
            }
            else
            {
                screenRoot.removePreference( categoryRice );
            }

        }

        if(categoryGlide64 != null && categoryGlide64Advanced != null)
        {
            if( LIBGLIDE64_SO.equals( videoPlugin ) )
            {
                screenRoot.addPreference( categoryGlide64 );
                screenRoot.addPreference( categoryGlide64Advanced );
            }
            else
            {
                screenRoot.removePreference( categoryGlide64 );
                screenRoot.removePreference( categoryGlide64Advanced );
            }
        }

        if(categoryAngrylion != null)
        {
            if( LIBANGRYLION_SO.equals( videoPlugin ) )
            {
                screenRoot.addPreference( categoryAngrylion );
            }
            else
            {
                screenRoot.removePreference( categoryAngrylion );
            }
        }

        if(categoryGliden64Texture != null &&
            categoryGliden64General != null &&
            categoryGliden64FrameBuffer != null &&
            categoryGliden64TextureFiltering != null &&
            categoryGliden64Gamma != null &&
            categoryGliden642d != null)
        {
            if( LIBGLIDEN64_SO.equals( videoPlugin ) )
            {
                screenRoot.addPreference( categoryGliden64Texture );
                screenRoot.addPreference( categoryGliden64General );
                screenRoot.addPreference( categoryGliden642d );
                screenRoot.addPreference( categoryGliden64FrameBuffer );
                screenRoot.addPreference( categoryGliden64TextureFiltering );
                screenRoot.addPreference( categoryGliden64Gamma );
            }
            else
            {
                screenRoot.removePreference( categoryGliden64Texture );
                screenRoot.removePreference( categoryGliden64General );
                screenRoot.removePreference( categoryGliden642d );
                screenRoot.removePreference( categoryGliden64FrameBuffer );
                screenRoot.removePreference( categoryGliden64TextureFiltering );
                screenRoot.removePreference( categoryGliden64Gamma );
            }
        }

        //Limit RSP options based on plugin
        if(preferenceRspPlugin != null)
        {
            ArrayList<String> entries = new ArrayList<>();
            ArrayList<String> values = new ArrayList<>();

            if( LIBGLN64_SO.equals( videoPlugin ) || LIBRICE_SO.equals( videoPlugin ) || LIBGLIDE64_SO.equals( videoPlugin ))
            {
                //Don't allow LLE mode
                entries.add(getString(R.string.rsp_hle));
                entries.add(getString(R.string.rsp_cxd4_hle));
                values.add("rsp-hle");
                values.add("rsp-cxd4-hle");
            }
            else if(LIBANGRYLION_SO.equals( videoPlugin ))
            {
                //Don't allow HLE
                entries.add(getString(R.string.rsp_cxd4_lle));
                values.add("rsp-cxd4-lle");
            }
            else
            {
                //All options available
                entries.add(getString(R.string.rsp_hle));
                entries.add(getString(R.string.rsp_cxd4_hle));
                entries.add(getString(R.string.rsp_cxd4_lle));
                values.add("rsp-hle");
                values.add("rsp-cxd4-hle");
                values.add("rsp-cxd4-lle");
            }

            String[] entriesArray = entries.toArray(new String[0]);
            String[] valuesArray = values.toArray(new String[0]);

            preferenceRspPlugin.setEntries(entriesArray);
            preferenceRspPlugin.setEntryValues(valuesArray);

            //Only update the selected option if the plugin changed
            if(mCurrentVideoPlugin != null && !mCurrentVideoPlugin.equals(videoPlugin))
            {
                if(preferenceRspPlugin.getEntryValues().length != 0)
                {
                    preferenceRspPlugin.setValue(preferenceRspPlugin.getEntryValues()[0].toString());
                }

                mPrefs.edit().apply();
            }
        }

        if(videoPlugin != null)
        {
            mCurrentVideoPlugin = videoPlugin;
        }
    }

    /**
     * Check for override for a specific key
     * @param key Key to check for override value
     * @param currentValue The current value for the key
     * @return The overriden value for the key
     */
    @Override
    protected String checkForOverride(final String key, final String currentValue)
    {
        String value = currentValue;
        if(value != null)
        {
            //Support older string value for video plugin that could support multiple GLideN64 versions
            //There is now only one version
            if(key.equals("videoPlugin") &&
                    value.toLowerCase().contains("libmupen64plus-video-gliden64") &&
                    !value.equals("mupen64plus-video-GLideN64.so"))
            {
                value = "mupen64plus-video-GLideN64.so";
            }

            //Fix old angrylion plugin library
            if(key.equals("videoPlugin") &&
                    value.toLowerCase().contains("angrylion") &&
                    !value.equals("mupen64plus-video-angrylion-plus.so"))
            {
                value = "mupen64plus-video-angrylion-plus.so";
            }
        }

        return value;
    }
}
