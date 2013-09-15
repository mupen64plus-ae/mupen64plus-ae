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
package paulscode.android.mupen64plusae;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;

//@formatter:off
/**
 * (start)
 *    |
 * onCreate <-- (killed) <---------\
 *    |                            |
 * onStart  <-- onRestart <-----\  |
 *    |                         |  |
 * onResume <----------------\  |  |
 *    |                      |  |  |
 * [*onSurfaceCreated*]      |  |  |
 *    |                      |  |  |
 * [*onSurfaceChanged*]      |  |  |
 *    |                      |  |  |
 * [*onWindowFocusChanged*]  |  |  |
 *    |                      |  |  |
 * (running)                 |  |  |
 *    |                      |  |  |
 * [*onWindowFocusChanged*]  |  |  |
 *    |                      |  |  |
 * onPause ------------------/  |  |
 *    |                         |  |
 * [*onSurfaceDestroyed*]       |  |
 *    |                         |  |
 * onStop ----------------------/--/
 *    |
 * onDestroy
 *    |
 * (end)
 * 
 * 
 * [*non-deterministic sequence*]
 * 
 * 
 */
//@formatter:on

public class GameLifecycleTracker
{
    /** True if the window is focused. */
    private boolean mIsFocused = false;
    
    /** True if the activity is resumed. */
    private boolean mIsResumed = false;
    
    /** True if the surface is available. */
    private boolean mIsSurface = false;
    
    private boolean isSafeToRender()
    {
        return mIsFocused && mIsResumed && mIsSurface;
    }
    
    private void tryRunning()
    {
        int state = CoreInterfaceNative.emuGetState();
        if( isSafeToRender() && ( state != CoreInterface.EMULATOR_STATE_RUNNING ) )
        {
            switch( state )
            {
                case CoreInterface.EMULATOR_STATE_UNKNOWN:
                    CoreInterface.startupEmulator();
                    break;
                case CoreInterface.EMULATOR_STATE_PAUSED:
                    CoreInterface.resumeEmulator();
                    break;
                default:
                    break;
            }
        }
    }
    
    private void tryPausing()
    {
        if( CoreInterfaceNative.emuGetState() != CoreInterface.EMULATOR_STATE_PAUSED )
        {
            CoreInterface.pauseEmulator( true );
        }
    }
    
    private void tryStopping()
    {
        if( CoreInterfaceNative.emuGetState() != CoreInterface.EMULATOR_STATE_STOPPED )
        {
            // Never go directly from running to stopped; always pause (and autosave) first
            tryPausing();
            CoreInterface.shutdownEmulator();
        }
    }
    
    public void onCreate( Bundle savedInstanceState )
    {
        Log.i( "GameLifecycleTracker", "onCreate" );
    }
    
    public void onStart()
    {
        Log.i( "GameLifecycleTracker", "onStart" );
    }
    
    public void onResume()
    {
        Log.i( "GameLifecycleTracker", "onResume" );
        mIsResumed = true;
        tryRunning();
    }
    
    public void surfaceCreated( SurfaceHolder holder )
    {
        Log.i( "GameLifecycleTracker", "surfaceCreated" );
    }
    
    public void surfaceChanged( SurfaceHolder holder, int format, int width, int height )
    {
        Log.i( "GameLifecycleTracker", "surfaceChanged" );
        mIsSurface = true;
        tryRunning();
    }
    
    public void onWindowFocusChanged( boolean hasFocus )
    {
        // Only try to run; don't try to pause. User may just be touching the in-game menu.
        Log.i( "GameLifecycleTracker", "onWindowFocusChanged: " + hasFocus );
        mIsFocused = hasFocus;
        tryRunning();
    }
    
    public void onPause()
    {
        Log.i( "GameLifecycleTracker", "onPause" );
        mIsResumed = false;
        tryPausing();
    }
    
    public void surfaceDestroyed( SurfaceHolder holder )
    {
        Log.i( "GameLifecycleTracker", "surfaceDestroyed" );
        mIsSurface = false;
        tryStopping();
    }
    
    public void onStop()
    {
        Log.i( "GameLifecycleTracker", "onStop" );
    }
    
    public void onDestroy()
    {
        Log.i( "GameLifecycleTracker", "onDestroy" );
    }
}
