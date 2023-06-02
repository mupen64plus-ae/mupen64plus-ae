/*
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
 * Authors: Paul Lamb, littleguy77
 */
package paulscode.android.mupen64plusae_mpn.game;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

import paulscode.android.mupen64plusae_mpn.input.map.TouchMap;
import paulscode.android.mupen64plusae_mpn.jni.CoreInterface;
import paulscode.android.mupen64plusae_mpn.persistent.ConfigFile;
import paulscode.android.mupen64plusae_mpn.util.DeviceUtil;
import paulscode.android.mupen64plusae_mpn.util.Image;
import paulscode.android.mupen64plusae_mpn.util.SafeMethods;
import paulscode.android.mupen64plusae_mpn.util.Utility;

/**
 * A kind of touch map that can be drawn on a canvas.
 * 
 * @see TouchMap
 * @see GameOverlay
 */
public class FpsOverlay extends View implements CoreInterface.OnFpsChangedListener
{
    /** Folder containing the images. */
    String skinFolder;

    /** The resources of the associated activity. */
    Resources mResources;

    /** The factor to scale images by. */
    private float mScalingFactor = 1.0f;

    /** Scaling factor to apply to images. */
    protected float scale = 1.0f;

    /** The last width passed to {@link #resize(int, int, DisplayMetrics)}. */
    private int cacheWidth = 0;

    /** The last height passed to {@link #resize(int, int, DisplayMetrics)}. */
    private int cacheHeight = 0;

    /** The last height passed to {@link #resize(int, int, DisplayMetrics)}. */
    private DisplayMetrics cacheMetrics;

    /** FPS frame image. */
    private Image mFpsFrame;

    /** X-coordinate of the FPS frame, in percent. */
    private int mFpsFrameX;

    /** Y-coordinate of the FPS frame, in percent. */
    private int mFpsFrameY;

    /** X-coordinate of the FPS text centroid, in percent. */
    private int mFpsTextX;

    /** Y-coordinate of the FPS text centroid, in percent. */
    private int mFpsTextY;

    /** The current FPS value. */
    private int mFpsValue;

    /** The minimum size of the FPS indicator in pixels. */
    private float mFpsMinPixels;

    /** The minimum size to scale the FPS indicator. */
    private float mFpsMinScale;

    /* FPS indicator X position */
    private int mFpsXPos;

    /* FPS indicator Y position */
    private int mFpsYPos;

    /** The set of images representing the FPS string. */
    private final CopyOnWriteArrayList<Image> mFpsDigits;

    /** The set of images representing the numerals 0, 1, 2, ..., 9. */
    private final Image[] mNumerals;

    /**
     * Instantiates a new visible touch map.
     *
     */


    public FpsOverlay(Context context, AttributeSet attribs )
    {
        super( context, attribs );

        mFpsDigits = new CopyOnWriteArrayList<>();
        mNumerals = new Image[10];
    }

    public void clear()
    {
        mFpsFrame = null;
        mFpsFrameX = mFpsFrameY = 0;
        mFpsTextX = mFpsTextY = 50;
        mFpsValue = 0;
        mFpsDigits.clear();
        Arrays.fill(mNumerals, null);
    }
    
    /**
     * Recomputes the map data for a given digitizer size, and
     * recalculates the scaling factor.
     * 
     * @param w The width of the digitizer, in pixels.
     * @param h The height of the digitizer, in pixels.
     * @param metrics Metrics about the display (for use in scaling).
     */
    public void resize( int w, int h, DisplayMetrics metrics )
    {
        // Cache the width and height in case we need to reload assets
        cacheWidth = w;
        cacheHeight = h;
        cacheMetrics = metrics;
        scale = 1.0f;
        
        if( metrics != null )
        {
            scale = metrics.densityDpi/260.0f;
        }
        // Apply the global scaling factor (derived from user prefs)
        scale *= mScalingFactor;
        
        resize( w, h );
    }

    public void resize( int w, int h )
    {
        // Compute FPS frame location
        float fpsScale = scale;
        if( mFpsMinScale > scale )
            fpsScale = mFpsMinScale;
        if( mFpsFrame != null )
        {
            mFpsFrame.setScale( fpsScale );
            mFpsFrame.fitPercent( mFpsFrameX, mFpsFrameY, w, h );
        }
        for( Image image : mNumerals)
        {
            if( image != null )
                image.setScale( fpsScale );
        }
        
        // Compute the FPS digit locations
        refreshFpsImages();
        refreshFpsPositions();
    }
    
    /**
     * Draws the FPS indicator.
     * 
     * @param canvas The canvas on which to draw.
     */
    public void drawFps( Canvas canvas )
    {
        if( canvas == null )
            return;
        
        // Redraw the FPS indicator
        if( mFpsFrame != null )
            mFpsFrame.draw( canvas );
        
        // Draw each digit of the FPS number
        for( Image digit : mFpsDigits )
            digit.draw( canvas );
    }
    
    /**
     * Updates the FPS indicator assets to reflect a new value.
     * 
     * @param fps The new FPS value.
     * 
     * @return True if the FPS assets changed.
     */
    public boolean updateFps( int fps )
    {
        // Clamp to positive, four digits max [0 - 9999]
        fps = Utility.clamp( fps, 0, 9999 );
        
        // Quick return if user has disabled FPS or it hasn't changed
        if( mFpsValue == fps )
            return false;
        
        // Store the new value
        mFpsValue = fps;
        
        // Refresh the FPS digits
        refreshFpsImages();
        refreshFpsPositions();
        
        return true;
    }
    
    /**
     * Refreshes the images used to draw the FPS string.
     */
    private void refreshFpsImages()
    {
        // Refresh the list of FPS digits
        String fpsString = Integer.toString( mFpsValue );
        mFpsDigits.clear();
        for( int i = 0; i < 4; i++ )
        {
            // Create a new sequence of numeral images
            if( i < fpsString.length() )
            {
                int numeral = SafeMethods.toInt( fpsString.substring( i, i + 1 ), -1 );
                if( numeral > -1 && numeral < 10 )
                {
                    // Clone the numeral from the font images and move to next digit
                    mFpsDigits.add( new Image( mResources, mNumerals[numeral] ) );
                }
            }
        }
    }
    
    /**
     * Refreshes the positions of the FPS images.
     */
    private void refreshFpsPositions()
    {
        // Compute the centroid of the FPS text
        int x = 0;
        int y = 0;
        if( mFpsFrame != null )
        {
            x = mFpsFrame.x + (int) ( ( mFpsFrame.width * mFpsFrame.scale ) * ( mFpsTextX / 100f ) );
            y = mFpsFrame.y + (int) ( ( mFpsFrame.height * mFpsFrame.scale ) * ( mFpsTextY / 100f ) );
        }
        
        // Compute the width of the FPS text
        int totalWidth = 0;
        for( Image digit : mFpsDigits )
            totalWidth += (int) ( digit.width * digit.scale );
        
        // Compute the starting position of the FPS text
        x -= (int) ( totalWidth / 2f );
        
        // Compute the position of each digit
        for( Image digit : mFpsDigits )
        {
            digit.setPos( x, y - (int) ( digit.hHeight * digit.scale ) );
            x += (int) ( digit.width * digit.scale );
        }
    }
    
    /**
     * Loads all touch map data from the filesystem.
     * 
     * @param skinDir    The directory containing the skin.ini and image files.
     * @param scale      The factor to scale images by.
     */
    public void load(Context context, Resources resources, String skinDir, int fpsXPos, int fpsYPos, float scale)
    {
        // Clear any old assets and map data
        clear();

        mResources = resources;

        // Load the configuration files
        skinFolder = skinDir;
        mFpsXPos = fpsXPos;
        mFpsYPos = fpsYPos;
        mScalingFactor = scale;
        ConfigFile skin_ini = new ConfigFile( context, skinFolder + "/skin.ini" );

        // Looad FPS assets
        loadFpsIndicator( context );

        mFpsTextX = SafeMethods.toInt( skin_ini.get( "INFO", "fps-numx" ), 27 );
        mFpsTextY = SafeMethods.toInt( skin_ini.get( "INFO", "fps-numy" ), 50 );
        mFpsMinPixels = SafeMethods.toInt( skin_ini.get( "INFO", "fps-minPixels" ), 75 );

        // Scale the assets to the last screensize used
        resize( cacheWidth, cacheHeight, cacheMetrics );
    }

    /**
     * Loads FPS indicator assets and properties from the filesystem.
     */
    private void loadFpsIndicator(Context context)
    {
        if( mFpsXPos >= 0 && mFpsYPos >= 0 ) 
        {
            // Position (percentages of the screen dimensions)
            mFpsFrameX = mFpsXPos;
            mFpsFrameY = mFpsYPos;
            
            // Load frame image
            mFpsFrame = new Image( context, mResources, skinFolder + "/fps.png" );
            
            // Minimum factor the FPS indicator can be scaled by
            mFpsMinScale = mFpsMinPixels / (float) mFpsFrame.width;
            
            // Load numeral images
            String filename = "";
            try
            {
                // Make sure we can load them (they might not even exist)
                for( int i = 0; i < mNumerals.length; i++ )
                {
                    filename = skinFolder + "/fps-" + i + ".png";
                    mNumerals[i] = new Image( context, mResources, filename );
                }
            }
            catch( Exception e )
            {
                // Problem, let the user know
                Log.e( "FpsOverlay", "Problem loading fps numeral '" + filename
                        + "', error message: " + e.getMessage() );
            }
        }
    }

    @Override
    public void onFpsChanged( int fps )
    {
        // Update the FPS indicator assets, and redraw if required
        if( updateFps( fps ) ) {
            postInvalidate();
        }
    }

    @Override
    protected void onDraw( Canvas canvas )
    {
        if( canvas == null )
            return;

        // Redraw the dynamic frame rate info
        drawFps( canvas );
    }

    @Override
    protected void onSizeChanged( int w, int h, int oldw, int oldh )
    {
        // Recompute skin layout geometry
        resize( w, h, DeviceUtil.getDisplayMetrics( this ) );
        super.onSizeChanged( w, h, oldw, oldh );
    }
}
