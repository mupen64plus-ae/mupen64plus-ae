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
package paulscode.android.mupen64plusae.profile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import paulscode.android.mupen64plusae.input.InputMapActivity;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import android.content.Intent;
import android.os.Bundle;

public class ControllerProfileActivity extends ProfileActivity<ControllerProfile>
{
    private ConfigFile mConfigBuiltin;
    private ConfigFile mConfigCustom;
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        new File( mUserPrefs.controllerProfiles_cfg ).mkdirs();
        mConfigBuiltin = new ConfigFile( mAppData.controllerProfiles_cfg );
        mConfigCustom = new ConfigFile( mUserPrefs.controllerProfiles_cfg );
    }
    
    @Override
    protected void onResume()
    {
        // Reload in case we're returning from the map editor
        mConfigCustom.reload();
        super.onResume();
    }
    
    @Override
    protected List<ControllerProfile> getProfiles( boolean showBuiltins )
    {
        List<ControllerProfile> list = new ArrayList<ControllerProfile>();
        list.addAll( ControllerProfile.getProfiles( mConfigCustom, false ) );
        if( showBuiltins )
            list.addAll( ControllerProfile.getProfiles( mConfigBuiltin, true ) );
        return list;
    }
    
    @Override
    protected void onEditProfile( ControllerProfile profile )
    {
        Intent intent = new Intent( this, InputMapActivity.class );
        intent.putExtra( InputMapActivity.EXTRA_PROFILE_NAME, profile.name );
        startActivity( intent );
    }
    
    @Override
    protected void onAddProfile( String name, String comment )
    {
        assert ( !mConfigCustom.keySet().contains( name ) );
        ControllerProfile.write( mConfigCustom, new ControllerProfile( name, comment, false ) );
        mConfigCustom.save();
    }
    
    @Override
    protected void onCopyProfile( ControllerProfile profile, String newName, String newComment )
    {
        assert ( !mConfigCustom.keySet().contains( newName ) );
        ControllerProfile.write( mConfigCustom, new ControllerProfile( newName, newComment, false,
                profile.map, profile.deadzone, profile.sensitivity ) );
        mConfigCustom.save();
    }
    
    @Override
    protected void onRenameProfile( ControllerProfile profile, String newName, String newComment )
    {
        mConfigCustom.remove( profile.name );
        ControllerProfile.write( mConfigCustom, new ControllerProfile( newName, newComment, false,
                profile.map, profile.deadzone, profile.sensitivity ) );
        mConfigCustom.save();
    }
    
    @Override
    protected void onDeleteProfile( ControllerProfile profile )
    {
        assert ( mConfigCustom.keySet().contains( profile.name ) );
        mConfigCustom.remove( profile.name );
        mConfigCustom.save();
    }
}
