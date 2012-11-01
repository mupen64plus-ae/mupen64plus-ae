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
 * Authors: paulscode, lioncash
 */
package paulscode.android.mupen64plusae;

import paulscode.android.mupen64plusae.util.Utility;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class TouchscreenView extends View implements TouchscreenSkin.Listener
{
    public TouchscreenSkin mSkin;
    
    private RedrawThread redrawThread = null;
    private boolean mRefreshAll = true;
    private boolean mRefreshHat = true;
    private boolean mRefreshFps = true;
    private int canvasW = 0;
    private int canvasH = 0;
    
    public TouchscreenView( Context context, AttributeSet attribs )
    {
        super( context, attribs );
    }
    
    public void initialize()
    {
        // Kill the FPS image redraw thread and create a new one
        if( redrawThread != null )
        {
            redrawThread.alive = false;
            redrawThread.redrawHat = false;
            redrawThread.redrawFps = false;
            try
            {
                redrawThread.join( 500 );
            }
            catch( InterruptedException ie )
            {
            }
        }        
        
        // There is no "restart", so start fresh
        mRefreshAll = true;
        canvasW = 0;
        canvasH = 0;
        redrawThread = new RedrawThread();
        
        // Start the thread
        try
        {
            redrawThread.start();
        }
        catch( IllegalThreadStateException itse )
        {
            // Problem... dynamic elements will not draw
            Log.w("TouchscreenView", "Could not start redraw thread.");
        }
    }
    
    @Override
    protected void onDraw( Canvas canvas )
    {
        // Recompute image locations if necessary
        if( canvas.getWidth() != canvasW || canvas.getHeight() != canvasH )
        {
            // Canvas changed its dimensions, recalculate the control positions
            canvasW = canvas.getWidth();
            canvasH = canvas.getHeight();
            mSkin.resize( canvasW, canvasH );
        }
        
        // Redraw the static elements of the gamepad
        if( Globals.userPrefs.isTouchscreenRedrawAll || mRefreshAll )
            mSkin.drawStatic( canvas );
        
        // Redraw the dynamic analog stick
        if( mRefreshAll || mRefreshHat )
            mSkin.drawAnalog( canvas );
        
        // Redraw the dynamic framerate info
        if( Globals.userPrefs.isFrameRateEnabled && ( mRefreshAll || mRefreshFps ) )
            mSkin.drawFps( canvas );
        
        // Reset the lazy refresh flags
        mRefreshAll = false;
        mRefreshHat = false;
        mRefreshFps = false;
    }
    
    @Override
    public void onAllChanged( TouchscreenSkin skin )
    {
        mRefreshAll = true;
        invalidate();
    }

    @Override
    public void onHatChanged( TouchscreenSkin skin, int x, int y )
    {
        mRefreshHat = true;
        if( redrawThread != null )
            redrawThread.redrawHat = true;        
    }

    @Override
    public void onFpsChanged( TouchscreenSkin skin, int fps )
    {
        mRefreshFps = true;
        if( redrawThread != null )
            redrawThread.redrawFps = true;        
    }

    /**
     * The RedrawThread class handles periodic redraws of the analog stick and FPS indicator.
     */
    private class RedrawThread extends Thread
    {
        public boolean alive = true;
        public boolean redrawHat = false;
        public boolean redrawFps = false;
        
        // Runnable for the analog stick
        private Runnable redrawer = new Runnable()
        {
            public void run()
            {
                if( Globals.userPrefs.isTouchscreenRedrawAll )
                {
                    // Redraw everything
                    invalidate();
                }
                else
                {
                    // Define invalidation box to redraw only what has changed
                    invalidate( mSkin.getAnalogBounds() );
                }
            }
        };
        
        // Runnable for the FPS indicator
        private Runnable redrawerFps = new Runnable()
        {
            public void run()
            {
                if( Globals.userPrefs.isTouchscreenRedrawAll )
                {
                    // Redraw everything
                    TouchscreenView.this.invalidate();
                }
                else
                {
                    // Define invalidation box to redraw only what has changed
                    invalidate( mSkin.getFpsBounds() );
                }
            }
        };
        
        /**
         * Main loop. Periodically checks if redrawing is necessary.
         */
        public void run()
        {
            final int millis = Globals.userPrefs.isTouchscreenRedrawAll
                    ? 150
                    : 100;
            
            while( alive )
            {
                // Shut down by setting alive=false from another thread
                if( redrawHat )
                {
                    // Need to redraw the analog stick
                    redrawHat = false;
                    GameImplementation.runOnUiThread( redrawer );
                }
                
                if( redrawFps )
                {
                    // Need to redraw the FPS indicator
                    redrawFps = false;
                    GameImplementation.runOnUiThread( redrawerFps );
                }
                
                // Sleep for a while, to save the CPU:
                Utility.safeSleep( millis );
            }
        }
    }
}
