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
 * Authors: paulscode, littleguy77
 */
package paulscode.android.mupen64plusae;

import paulscode.android.mupen64plusae.input.transform.TouchMap;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

// TODO: Doesn't work as expected when Globals.userPrefs.isTouchscreenRedrawAll == false
public class TouchscreenView extends View implements TouchMap.Listener
{
    private boolean mInitialized;
    private boolean mRefreshAll;
    private boolean mRefreshHat;
    private boolean mRefreshFps;
    private TouchMap mTouchMap;
    
    public TouchscreenView( Context context, AttributeSet attribs )
    {
        super( context, attribs );
        mInitialized = false;
    }
    
    public void initialize( TouchMap touchMap )
    {
        // Stop listening
        if( mTouchMap != null )
            mTouchMap.unregisterListener( this );
        
        // Suspend drawing
        mInitialized = false;
        mTouchMap = touchMap;
        
        // There is no "restart", so start fresh
        mRefreshAll = true;
        
        // Start listening
        if( mTouchMap != null )
            mTouchMap.registerListener( this );
        
        // Resume drawing
        mInitialized = true;
    }
    
    @Override
    public void onAllChanged( TouchMap touchMap )
    {
        // Refresh everything
        mRefreshAll = true;
        invalidate();
    }

    @Override
    public void onHatChanged( TouchMap touchMap, float x, float y )
    {
        // Refresh analog stick
        mRefreshHat = true;
        if( Globals.userPrefs.isTouchscreenRedrawAll )
            invalidate();
        else
            invalidate( mTouchMap.getAnalogBounds() );
    }

    @Override
    public void onFpsChanged( TouchMap touchMap, int fps )
    {
        // Refresh FPS text
        mRefreshFps = true;
        if( Globals.userPrefs.isTouchscreenRedrawAll )
            invalidate();
        else
            invalidate( mTouchMap.getFpsBounds() );
    }
    
    @Override
    protected void onSizeChanged( int w, int h, int oldw, int oldh )
    {
        // Recompute skin layout geometry
        mTouchMap.resize( w, h );
        super.onSizeChanged( w, h, oldw, oldh );
    }

    @Override
    protected void onDraw( Canvas canvas )
    {
        if( !mInitialized )
            return;
        
        // Refresh everything if the user preference is set
        mRefreshAll |= Globals.userPrefs.isTouchscreenRedrawAll;
        
        // Redraw the static elements of the gamepad
        if( mRefreshAll )
            mTouchMap.drawStatic( canvas );
        
        // Redraw the dynamic analog stick
        if( mRefreshAll || mRefreshHat )
            mTouchMap.drawAnalog( canvas );
        
        // Redraw the dynamic frame rate info
        if( ( mRefreshAll || mRefreshFps ) && Globals.userPrefs.isFrameRateEnabled )
            mTouchMap.drawFps( canvas );
        
        // Reset the lazy refresh flags
        mRefreshAll = false;
        mRefreshHat = false;
        mRefreshFps = false;
    }
}
