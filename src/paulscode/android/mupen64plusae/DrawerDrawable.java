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

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

public class DrawerDrawable extends Drawable
{
    private Paint mPaint;
    int mAlpha = 100;
    
    public DrawerDrawable( int alpha )
    {
        mPaint = new Paint();
        if( alpha < 0 )
            alpha = 0;
        if( alpha > 255 )
            alpha = 255;
        mAlpha = alpha;
    }
    
    @Override
    public void draw( Canvas canvas )
    {
        int width = getBounds().width();
        int height = getBounds().height();
        int alpha = mAlpha << 24;
        
        mPaint.setColor( alpha + 0x000000 );
        canvas.drawRect( 0, 0, width, height, mPaint );
        
        mPaint.setColor( 0xFF555555 );
        canvas.drawRect( width - 1, 0, width, height, mPaint );
    }
    
    @Override
    public int getOpacity()
    {
        return ( mAlpha == 255 ) ? PixelFormat.OPAQUE : PixelFormat.TRANSLUCENT;
    }
    
    @Override
    public void setAlpha( int alpha )
    {
        mPaint.setAlpha( alpha );
    }
    
    @Override
    public void setColorFilter( ColorFilter filter )
    {
        mPaint.setColorFilter( filter );
    }
}
