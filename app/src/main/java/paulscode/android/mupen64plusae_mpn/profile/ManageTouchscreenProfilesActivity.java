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
package paulscode.android.mupen64plusae_mpn.profile;

import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;

import org.mupen64plusae_mpn.v3.alpha.R;

import paulscode.android.mupen64plusae_mpn.ActivityHelper;
import paulscode.android.mupen64plusae_mpn.persistent.ConfigFile;
import paulscode.android.mupen64plusae_mpn.persistent.GlobalPrefs;

public class ManageTouchscreenProfilesActivity extends ManageProfilesActivity
{
    public static final String SHOW_BUILT_IN_PREF_KEY = "ShowBuiltIns_ManageTouchscreenProfilesActivity";

    @Override
    protected ConfigFile getConfigFile( boolean isBuiltin )
    {
        return isBuiltin ? mAppData.GetTouchscreenProfilesConfig() : mGlobalPrefs.GetTouchscreenProfilesConfig();
    }
    
    @Override
    protected String getNoDefaultProfile()
    {
        return GlobalPrefs.DEFAULT_TOUCHSCREEN_PROFILE_DEFAULT;
    }

    @Override
    protected String getNoSecondaryDefaultProfile()
    {
        return GlobalPrefs.DEFAULT_TOUCHSCREEN_DPAD_PROFILE_DEFAULT;
    }
    
    @Override
    protected String getDefaultProfile()
    {
        return mGlobalPrefs.getTouchscreenProfileDefault();
    }

    @Override
    protected String getSecondaryDefaultProfile()
    {
        return mGlobalPrefs.getTouchscreenDpadProfileDefault();
    }
    
    @Override
    protected void putDefaultProfile( String name )
    {
        mGlobalPrefs.putTouchscreenProfileDefault( name );
    }

    @Override
    protected void putSecondaryDefaultProfile( String name )
    {
        mGlobalPrefs.putTouchscreenDpadProfileDefault( name );
    }
    
    @Override
    protected void onEditProfile(ActivityResultLauncher<Intent> launcher, Profile profile )
    {
        Intent intent = new Intent( this, TouchscreenProfileActivity.class );
        intent.putExtra( ActivityHelper.Keys.PROFILE_NAME, profile.name );
        launcher.launch(intent);
    }
    
    @Override
    protected int getWindowTitleResource()
    {
        return R.string.ManageTouchscreenProfilesActivity_title;
    }

    @Override
    protected String getBuiltinVisibilityKey()
    {
        return SHOW_BUILT_IN_PREF_KEY;
    }
}
