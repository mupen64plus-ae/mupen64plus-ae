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

import paulscode.android.mupen64plusae.input.TouchscreenController;
import paulscode.android.mupen64plusae.input.map.VisibleTouchMap;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

// TODO: Minor glitch: hold analog, then hold button, then release analog -> analog doesn't redraw

public class GameOverlay extends View implements TouchscreenController.OnStateChangedListener,
        GameSurface.OnFpsChangedListener
{
    private VisibleTouchMap mTouchMap;
    
    public GameOverlay( Context context, AttributeSet attribs )
    {
        super( context, attribs );
    }
    
    public void initialize( VisibleTouchMap touchMap )
    {
        // Set the new TouchMap
        mTouchMap = touchMap;
    }
    
    @Override
    public void onAnalogChanged( float axisFractionX, float axisFractionY )
    {
        // Update the analog stick assets, and redraw if required
        if( mTouchMap != null && mTouchMap.updateAnalog( axisFractionX, axisFractionY ) )
            postInvalidate();
    }
    
    @Override
    public void onFpsChanged( int fps )
    {
        // Update the FPS indicator assets, and redraw if required
        if( mTouchMap != null && mTouchMap.updateFps( fps ) )
            postInvalidate();
    }
    
    @Override
    protected void onSizeChanged( int w, int h, int oldw, int oldh )
    {
        // Recompute skin layout geometry
        if( mTouchMap != null )
            mTouchMap.resize( w, h );
        super.onSizeChanged( w, h, oldw, oldh );
    }
    
    @Override
    protected void onDraw( Canvas canvas )
    {
        if( mTouchMap == null )
            return;
        
        // Redraw the static buttons
        mTouchMap.drawButtons( canvas );
        
        // Redraw the dynamic analog stick
        mTouchMap.drawAnalog( canvas );
        
        // Redraw the dynamic frame rate info
        if( Globals.userPrefs.isFrameRateEnabled )
            mTouchMap.drawFps( canvas );
    }
}
