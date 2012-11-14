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

import paulscode.android.mupen64plusae.input.XperiaPlayController;
import paulscode.android.mupen64plusae.input.map.TouchMap;
import paulscode.android.mupen64plusae.util.FileUtil;
import android.annotation.TargetApi;
import android.app.NativeActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

@TargetApi( 9 )
public class GameActivityXperiaPlay extends NativeActivity
{
    private TouchMap mXperiaPlayMap;
    @SuppressWarnings( "unused" )
    private XperiaPlayController mXperiaPlayController;
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
        Log.i( "GameActivity", "onCreate: " );       
        super.onCreate( savedInstanceState );
        mLifecycleHandler.onCreate( savedInstanceState );
        
        // We should only be here if enabled
        assert ( Globals.userPrefs.isXperiaEnabled );
        
        // Additional Xperia Play configuration
        getWindow().takeSurface( null );
        NativeMethods.RegisterThis();
        FileUtil.loadNativeLibName( "xperia-touchpad" );
        mXperiaPlayMap = new TouchMap( getResources() );
        mXperiaPlayMap.load( Globals.userPrefs.xperiaLayout );
        if( Globals.userPrefs.inputPlugin.enabled )
            mXperiaPlayController = new XperiaPlayController( mXperiaPlayMap );
    }    
    
    @Override
    protected void onStart()
    {
        Log.i( "GameActivity", "onStart: ");
        super.onStart();
    }
    
    @Override
    protected void onResume()
    {
        Log.i( "GameActivity", "onResume: ");
        super.onResume();
        mLifecycleHandler.onResume();
    }
    
    @Override
    protected void onRestoreInstanceState( Bundle savedInstanceState )
    {
        Log.i( "GameActivity", "onRestoreInstanceState: ");
        super.onRestoreInstanceState( savedInstanceState );
    }
    
    @Override
    protected void onSaveInstanceState( Bundle outState )
    {
        Log.i( "GameActivity", "onSaveInstanceState: " );
        super.onSaveInstanceState( outState );
    }
    
    @Override
    protected void onPause()
    {
        Log.i( "GameActivity", "onPause: " );
        super.onPause();
        mLifecycleHandler.onPause();
    }
    
    @Override
    protected void onStop()
    {
        Log.i( "GameActivity", "onStop: " );
        super.onStop();
    }
    
    @Override
    protected void onDestroy()
    {
        Log.i( "GameActivity", "onDestroy: " );
        super.onDestroy();
    }
    
    @Override
    protected void onRestart()
    {
        Log.i( "GameActivity", "onRestart: " );
        super.onRestart();
    }
}
