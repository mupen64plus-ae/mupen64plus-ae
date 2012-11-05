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

import paulscode.android.mupen64plusae.input.transform.VisibleTouchMap;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

public class TouchscreenView extends View implements VisibleTouchMap.Listener
{
    private boolean mInitialized;
    private VisibleTouchMap mTouchMap;
    
    public TouchscreenView( Context context, AttributeSet attribs )
    {
        super( context, attribs );
        mInitialized = false;
    }
    
    public void initialize( VisibleTouchMap touchMap )
    {
        // Suspend drawing
        mInitialized = false;
        
        // Stop listening
        if( mTouchMap != null )
            mTouchMap.unregisterListener( this );
        
        // Set the new TouchMap
        mTouchMap = touchMap;
        
        // Start listening
        if( mTouchMap != null )
            mTouchMap.registerListener( this );
        
        // Resume drawing
        mInitialized = true;
    }
    
    @Override
    public void onAllChanged( VisibleTouchMap touchMap )
    {
        // Tell Android to redraw on the UI thread
        postInvalidate();
    }
    
    @Override
    public void onHatChanged( VisibleTouchMap touchMap, float x, float y )
    {
        // Tell Android to redraw on the UI thread
        postInvalidate();
    }
    
    @Override
    public void onFpsChanged( VisibleTouchMap touchMap, int fps )
    {
        // Tell Android to redraw on the UI thread
        postInvalidate();
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
        
        // Redraw the static elements of the gamepad
        mTouchMap.drawButtons( canvas );
        
        // Redraw the dynamic analog stick
        mTouchMap.drawAnalog( canvas );
        
        // Redraw the dynamic frame rate info
        if( Globals.userPrefs.isFrameRateEnabled )
            mTouchMap.drawFps( canvas );
    }
}
