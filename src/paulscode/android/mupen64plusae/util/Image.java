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
 * Authors: Paul Lamb
 */
package paulscode.android.mupen64plusae.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;

/**
 * The Image class provides a simple interface to common image manipulation methods.
 */
public final class Image
{
    public final Bitmap image;
    public final BitmapDrawable drawable;
    public final int width;
    public final int height;
    public final int hWidth;
    public final int hHeight;
    
    public float scale = 1.0f;
    public int x = 0;
    public int y = 0;
    public Rect drawRect = null;
    
    /**
     * Constructor: Loads an image file and sets the initial properties.
     * 
     * @param res
     *            A handle to the app resources.
     * @param filename
     *            The path to the image file.
     */
    public Image( Resources res, String filename )
    {
        image = BitmapFactory.decodeFile( filename );
        drawable = new BitmapDrawable( res, image );
        
        if( image == null )
        {
            width  = 0;
            height = 0;
        }
        else
        {
            width = image.getWidth();
            height = image.getHeight();
        }
        
        hWidth  = (int) ( width  / 2.0f ); 
        hHeight = (int) ( height / 2.0f );
        
        drawRect = new Rect();
    }
    
    /**
     * Constructor: Creates a clone copy of a given Image.
     * 
     * @param res
     *            A handle to the app resources.
     * @param clone
     *            The Image to make a copy of.
     */
    public Image( Resources res, Image clone )
    {
        if( clone == null )
        {
            image = null;
            drawable = null;
            width = 0;
            height = 0;
            hWidth = 0;
            hHeight = 0;
        }
        else
        {
            image = clone.image;
            drawable = new BitmapDrawable( res, image );
            width = clone.width;
            height = clone.height;
            hWidth = clone.hWidth;
            hHeight = clone.hHeight;
            scale = clone.scale;
        }
        drawRect = new Rect();
    }
    
    /**
     * Sets the scaling factor of the image.
     * 
     * @param scale
     *            Factor to scale the image by.
     */
    public void setScale( float scale )
    {
        this.scale = scale;
        setPos( x, y );  // Apply the new scaling factor
    }
    
    /**
     * Sets the screen position of the image (in pixels).
     * 
     * @param x
     *            X-coordinate.
     * @param y
     *            Y-coordinate.
     */
    public void setPos( int x, int y )
    {
        this.x = x;
        this.y = y;
        
        if( drawRect != null )
            drawRect.set( x, y, x + (int) ( width * scale ), y + (int) ( height * scale ) );
        if( drawable != null )
            drawable.setBounds( drawRect );
    }
    
    /**
     * Centers the image at the specified coordinates, without going beyond the specified screen
     * dimensions.
     * 
     * @param centerX
     *            X-coordinate to center the image at.
     * @param centerY
     *            Y-coordinate to center the image at.
     * @param screenW
     *            Horizontal screen dimension (in pixels).
     * @param screenH
     *            Vertical screen dimension (in pixels).
     */
    public void fitCenter( int centerX, int centerY, int screenW, int screenH )
    {
        float cx = centerX;
        float cy = centerY;
        
        if( cx < hWidth * scale )
            cx = hWidth * scale;
        if( cy < hHeight * scale )
            cy = hHeight * scale;
        if( cx + ( hWidth * scale ) > screenW )
            cx = screenW - ( hWidth * scale );
        if( cy + ( hHeight * scale ) > screenH )
            cy = screenH - ( hHeight * scale );
        
        x = (int) ( cx - ( hWidth * scale ) );
        y = (int) ( cy - ( hHeight * scale ) );
        
        if( drawRect != null )
        {
            drawRect.set( x, y, x + (int) ( width * scale ), y + (int) ( height * scale ) );
            if( drawable != null )
                drawable.setBounds( drawRect );
        }
    }
    
    /**
     * Centers the image at the specified coordinates, without going beyond the edges of the
     * specified rectangle.
     * 
     * @param centerX
     *            X-coordinate to center the image at.
     * @param centerY
     *            Y-coordinate to center the image at.
     * @param rectX
     *            X-coordinate of the bounding rectangle.
     * @param rectY
     *            Y-coordinate of the bounding rectangle.
     * @param rectW
     *            Horizontal bounding rectangle dimension (in pixels).
     * @param rectH
     *            Vertical bounding rectangle dimension (in pixels).
     */
    public void fitCenter( int centerX, int centerY, int rectX, int rectY, int rectW, int rectH )
    {
        float cx = centerX;
        float cy = centerY;
        
        if( cx < rectX + ( hWidth * scale ) )
            cx = rectX + ( hWidth * scale );
        if( cy < rectY + ( hHeight * scale ) )
            cy = rectY + ( hHeight * scale );
        if( cx + ( hWidth * scale ) > rectX + rectW )
            cx = rectX + rectW - ( hWidth * scale );
        if( cy + ( hHeight * scale ) > rectY + rectH )
            cy = rectY + rectH - ( hHeight * scale );
        
        x = (int) ( cx - ( hWidth * scale ) );
        y = (int) ( cy - ( hHeight * scale ) );
        
        if( drawRect != null )
        {
            drawRect.set( x, y, x + (int) ( width * scale ), y + (int) ( height * scale ) );
            if( drawable != null )
                drawable.setBounds( drawRect );
        }
    }
    
    /**
     * Draws the image.
     * 
     * @param canvas
     *            Canvas to draw the image on.
     */
    public void draw( Canvas canvas )
    {
        if( drawable != null )
            drawable.draw( canvas );
    }
    
    /**
     * Sets the alpha value of the image.
     * 
     * @param alpha
     *            Alpha value.
     */
    public void setAlpha( int alpha ) 
    {
        if( drawable != null )
            drawable.setAlpha( alpha );
    }
}
