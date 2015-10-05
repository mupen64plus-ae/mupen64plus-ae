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
package paulscode.android.mupen64plusae.game;

import paulscode.android.mupen64plusae.jni.CoreInterface;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import android.app.Activity;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

public class GameActivity extends Activity
{
    private GameLifecycleHandler mLifecycleHandler;
    private GameMenuHandler mMenuHandler;
    
    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        mMenuHandler.onCreateOptionsMenu( menu );
        return super.onCreateOptionsMenu( menu );
    }
    
    @Override
    public boolean onPrepareOptionsMenu( Menu menu )
    {
        mMenuHandler.onPrepareOptionsMenu( menu );
        return super.onPrepareOptionsMenu( menu );
    }
    
    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        mMenuHandler.onOptionsItemSelected( item );
        return super.onOptionsItemSelected( item );
    }
    
    @Override
    public void onWindowFocusChanged( boolean hasFocus )
    {
        super.onWindowFocusChanged( hasFocus );
        mLifecycleHandler.onWindowFocusChanged( hasFocus );
    }
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.setTheme( android.support.v7.appcompat.R.style.Theme_AppCompat_NoActionBar );
        mMenuHandler = new GameMenuHandler( this );
        CoreInterface.addOnStateCallbackListener( mMenuHandler  );
        
        //Allow volume keys to control media volume if they are not mapped
        SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean volKeyMapped = mPreferences.getBoolean("inputVolumeMappable", false);
        GlobalPrefs mGlobalPrefs = new GlobalPrefs(this);
        if (!volKeyMapped && mGlobalPrefs.audioPlugin.enabled)
        {
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }
        
        mLifecycleHandler = new GameLifecycleHandler( this );
        mLifecycleHandler.onCreateBegin( savedInstanceState );
        super.onCreate( savedInstanceState );        
        mLifecycleHandler.onCreateEnd( savedInstanceState );
    }
    
    @Override
    protected void onStart()
    {
        super.onStart();
        mLifecycleHandler.onStart();
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        mLifecycleHandler.onResume();
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
        mLifecycleHandler.onPause();
    }
    
    @Override
    protected void onStop()
    {
        super.onStop();
        mLifecycleHandler.onStop();
    }
    
    @Override
    protected void onDestroy()
    {
        CoreInterface.removeOnStateCallbackListener( mMenuHandler  );
        
        super.onDestroy();
        mLifecycleHandler.onDestroy();
    }
}
