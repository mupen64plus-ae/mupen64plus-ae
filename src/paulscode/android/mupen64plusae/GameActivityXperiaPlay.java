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
package paulscode.android.mupen64plusae;

import paulscode.android.mupen64plusae.input.map.TouchMap;
import paulscode.android.mupen64plusae.input.provider.XperiaPlayProvider;
import paulscode.android.mupen64plusae.util.FileUtil;
import android.annotation.TargetApi;
import android.app.NativeActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

@TargetApi( 9 )
public class GameActivityXperiaPlay extends NativeActivity
{
    private final GameLifecycleHandler mLifecycleHandler;
    private final GameMenuHandler mMenuHandler;

    public GameActivityXperiaPlay()
    {
        mLifecycleHandler = new GameLifecycleHandler( this );
        mMenuHandler = new GameMenuHandler( this );
    }
    
    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        mMenuHandler.onCreateOptionsMenu( menu );
        return super.onCreateOptionsMenu( menu );
    }
    
    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        mMenuHandler.onOptionsItemSelected( item );
        return super.onOptionsItemSelected( item );
    }
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        FileUtil.loadNativeLibName( "xperia-touchpad" );
        
        mLifecycleHandler.onCreateBegin( savedInstanceState );
        
        super.onCreate( savedInstanceState );
        
        // Take control of the GameSurface
        getWindow().takeSurface( null );
        
        mLifecycleHandler.onCreateEnd( savedInstanceState );
        
        // Create the Xperia PLAY touchpad controller
        XperiaPlayProvider xperiaProvider = new XperiaPlayProvider();
        if( Globals.userPrefs.inputPlugin.enabled )
        {
            TouchMap map = new TouchMap( getResources() );
            map.load( Globals.userPrefs.xperiaLayout );
            //mControllers.add( new XperiaPlayController( map ) );
        }
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
}
