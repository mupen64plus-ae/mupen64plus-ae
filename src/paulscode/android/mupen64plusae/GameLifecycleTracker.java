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
    
    /** True if the emulator has started up (i.e. is ready to run). */
    private boolean mIsStarted = false;
    
    /** True if the emulator is running (i.e. not paused). */
    private boolean mIsRunning = false;
    
    private void tryStartup()
    {
        if( mIsFocused && mIsResumed && mIsSurface && !mIsStarted )
        {
            mIsStarted = true;
            CoreInterface.startupEmulator();
        }
    }
    
    private void tryRunning()
    {
        if( mIsFocused && mIsResumed && mIsSurface && !mIsRunning )
        {
            tryStartup();
            mIsRunning = true;
            CoreInterface.resumeEmulator();
        }
    }
    
    private void tryHalting()
    {
        if( mIsRunning )
        {
            mIsRunning = false;
            CoreInterface.pauseEmulator( true );
        }
    }
    
    private void tryShutdown()
    {
        if( mIsStarted )
        {
            tryHalting();
            mIsStarted = false;
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
        CoreInterfaceNative.loadLibraries();
        tryStartup();
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
        CoreInterface.onResize( format, width, height );
        mIsSurface = true;
        tryRunning();
    }
    
    public void onWindowFocusChanged( boolean hasFocus )
    {
        // Only try to run; don't try to halt. User might just be touching the in-game menu. If the
        // window loses focus to another app, halt will be invoked on pause or surface destroyed.
        Log.i( "GameLifecycleTracker", "onWindowFocusChanged: " + hasFocus );
        mIsFocused = hasFocus;
        tryRunning();
    }
    
    public void onPause()
    {
        Log.i( "GameLifecycleTracker", "onPause" );
        mIsResumed = false;
        tryHalting();
    }
    
    public void surfaceDestroyed( SurfaceHolder holder )
    {
        Log.i( "GameLifecycleTracker", "surfaceDestroyed" );
        mIsSurface = false;
        tryHalting();
    }
    
    public void onStop()
    {
        Log.i( "GameLifecycleTracker", "onStop" );
        tryShutdown();
        CoreInterfaceNative.unloadLibraries();
    }
    
    public void onDestroy()
    {
        Log.i( "GameLifecycleTracker", "onDestroy" );
    }
}
