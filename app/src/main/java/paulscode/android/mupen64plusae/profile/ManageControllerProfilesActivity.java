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

import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.persistent.ConfigFile;

public class ManageControllerProfilesActivity extends ManageProfilesActivity
{
    public static final String SHOW_BUILT_IN_PREF_KEY = "ShowBuiltIns_ManageControllerProfilesActivity";

    @Override
    protected ConfigFile getConfigFile( boolean isBuiltin )
    {
        return isBuiltin ? mAppData.GetControllerProfilesConfig() : mGlobalPrefs.GetControllerProfilesConfig();
    }

    @Override
    protected String getNoDefaultProfile()
    {
        return "";
    }

    @Override
    protected String getNoSecondaryDefaultProfile()
    {
        return "";
    }
    
    @Override
    protected String getDefaultProfile()
    {
        return "";
    }

    @Override
    protected String getSecondaryDefaultProfile()
    {
        return "";
    }

    @Override
    protected void putDefaultProfile( String name )
    {

    }

    @Override
    protected void putSecondaryDefaultProfile( String name )
    {

    }
    
    @Override
    protected void onEditProfile(ActivityResultLauncher<Intent> launcher, Profile profile )
    {
        Intent intent = new Intent( this, ControllerProfileActivityBigScreen.class );
        intent.putExtra( ActivityHelper.Keys.PROFILE_NAME, profile.name );
        launcher.launch(intent);
    }

    @Override
    protected int getWindowTitleResource()
    {
        return R.string.ManageControllerProfilesActivity_title;
    }

    @Override
    protected String getBuiltinVisibilityKey()
    {
        return SHOW_BUILT_IN_PREF_KEY;
    }
}
