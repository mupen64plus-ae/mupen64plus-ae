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
 * Authors: paulscode, lioncash, littleguy77
 */
package paulscode.android.mupen64plusae.input.map;

import java.util.ArrayList;

import paulscode.android.mupen64plusae.GameOverlay;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.util.Image;
import paulscode.android.mupen64plusae.util.SafeMethods;
import paulscode.android.mupen64plusae.util.Utility;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.util.Log;

/**
 * A kind of touch map that can be drawn on a canvas.
 * 
 * @see TouchMap
 * @see GameOverlay
 */
public class VisibleTouchMap extends TouchMap
{
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
    
    /** The number of frames over which to compute FPS. */
    private int mFpsRecalcPeriod;
    
    /** The current FPS value. */
    private int mFpsValue;
    
    /** True if the FPS indicator should be drawn. */
    private final boolean mFpsEnabled;
    
    /** The file directory containing the FPS font resources. */
    private final String mFontsDir;
    
    /** The set of images representing the FPS string. */
    private final ArrayList<Image> mFpsDigits;
    
    /** The set of images representing the numerals 0, 1, 2, ..., 9. */
    private final Image[] mNumerals;
    
    /**
     * Instantiates a new visible touch map.
     * 
     * @param resources The resources of the activity associated with this touch map.
     * @param fpsEnabled True to display the FPS indicator.
     * @param fontsDirectory The directory containing the FPS font resources.
     */
    public VisibleTouchMap( Resources resources, boolean fpsEnabled, String fontsDirectory )
    {
        super( resources );
        mFpsEnabled = fpsEnabled;
        mFontsDir = fontsDirectory;
        mFpsDigits = new ArrayList<Image>();
        mNumerals = new Image[10];
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see paulscode.android.mupen64plusae.input.map.TouchMap#clear()
     */
    @Override
    public void clear()
    {
        super.clear();
        mFpsFrame = null;
        mFpsFrameX = mFpsFrameY = 0;
        mFpsTextX = mFpsTextY = 50;
        mFpsRecalcPeriod = 15;
        mFpsValue = 0;
        mFpsDigits.clear();
        for( int i = 0; i < mNumerals.length; i++ )
            mNumerals[i] = null;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see paulscode.android.mupen64plusae.input.map.TouchMap#resize(int, int)
     */
    @Override
    public void resize( int w, int h )
    {
        super.resize( w, h );
        
        // Compute analog foreground location (centered)
        if( analogBackImage != null && analogForeImage != null )
        {
            int cX = analogBackImage.x + analogBackImage.hWidth;
            int cY = analogBackImage.y + analogBackImage.hHeight;
            analogForeImage.fitCenter( cX, cY, analogBackImage.x, analogBackImage.y,
                    analogBackImage.width, analogBackImage.height );
        }
        
        // Compute FPS frame location
        if( mFpsFrame != null )
        {
            int cX = (int) ( (float) w * ( (float) mFpsFrameX / 100f ) );
            int cY = (int) ( (float) h * ( (float) mFpsFrameY / 100f ) );
            mFpsFrame.fitCenter( cX, cY, w, h );
        }
        
        // Compute the FPS digit locations
        refreshFpsPositions();
    }

    /**
     * Draws the buttons.
     * 
     * @param canvas The canvas on which to draw.
     */
    public void drawButtons( Canvas canvas )
    {
        // Draw the buttons onto the canvas
        for( Image button : buttonImages )
            button.draw( canvas );
    }
    
    /**
     * Draws the analog stick.
     * 
     * @param canvas The canvas on which to draw.
     */
    public void drawAnalog( Canvas canvas )
    {
        // Draw the background image
        if( analogBackImage != null )
            analogBackImage.draw( canvas );
        
        // Draw the movable foreground (the stick)
        if( analogForeImage != null )
            analogForeImage.draw( canvas );
    }
    
    /**
     * Draws the FPS indicator.
     * 
     * @param canvas The canvas on which to draw.
     */
    public void drawFps( Canvas canvas )
    {
        // Redraw the FPS indicator
        if( mFpsFrame != null )
            mFpsFrame.draw( canvas );
        
        // Draw each digit of the FPS number
        for( Image digit : mFpsDigits )
            digit.draw( canvas );
    }
    
    /**
     * Updates the analog stick assets to reflect a new position.
     * 
     * @param axisFractionX The x-axis fraction, between -1 and 1, inclusive.
     * @param axisFractionY The y-axis fraction, between -1 and 1, inclusive.
     * @return True if the analog assets changed.
     */
    public boolean updateAnalog( float axisFractionX, float axisFractionY )
    {
        if( analogForeImage != null && analogBackImage != null )
        {
            // Get the location of stick center
            int hX = analogBackImage.hWidth + (int) ( axisFractionX * (float) analogMaximum );
            int hY = analogBackImage.hHeight - (int) ( axisFractionY * (float) analogMaximum );
            
            // Use other values if invalid
            if( hX < 0 )
                hX = analogBackImage.hWidth;
            if( hY < 0 )
                hY = analogBackImage.hHeight;
            
            // Update the position of the stick
            int cX = analogBackImage.x + hX;
            int cY = analogBackImage.y + hY;
            analogForeImage.fitCenter( cX, cY, analogBackImage.x, analogBackImage.y,
                    analogBackImage.width, analogBackImage.height );
            return true;
        }
        return false;
    }    

    /**
     * Updates the FPS indicator assets to reflect a new value.
     * 
     * @param fps The new FPS value.
     * @return True if the FPS assets changed.
     */
    public boolean updateFps( int fps )
    {
        // Clamp to positive, four digits max [0 - 9999]
        fps = Utility.clamp( fps, 0, 9999 );
        
        // Quick return if user has disabled FPS or it hasn't changed
        if( !mFpsEnabled || mFpsValue == fps )
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
            x = mFpsFrame.x + (int) ( (float) mFpsFrame.width * ( (float) mFpsTextX / 100f ) );
            y = mFpsFrame.y + (int) ( (float) mFpsFrame.height * ( (float) mFpsTextY / 100f ) );
        }
        
        // Compute the width of the FPS text
        int totalWidth = 0;
        for( Image digit : mFpsDigits )
            totalWidth += digit.width;
        
        // Compute the starting position of the FPS text
        x = x - (int) ( (float) totalWidth / 2f );
        
        // Compute the position of each digit
        for( Image digit : mFpsDigits )
        {
            digit.setPos( x, y - digit.hHeight );
            x += digit.width;
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see paulscode.android.mupen64plusae.input.map.TouchMap#loadAssetSection(java.lang.String,
     * java.lang.String, paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection,
     * java.lang.String)
     */
    @Override
    protected void loadAssetSection( String directory, String filename, ConfigSection section,
            String assetType )
    {
        if( assetType.contains( "fps" ) )
            loadFpsIndicator( directory, filename, section );
        else
            super.loadAssetSection( directory, filename, section, assetType );
    }

    /**
     * Loads FPS indicator assets and properties from the filesystem.
     * 
     * @param directory The directory containing the FPS indicator assets.
     * @param filename The filename of the FPS indicator assets, without extension.
     * @param section The configuration section containing the FPS indicator properties.
     */
    private void loadFpsIndicator( final String directory, String filename, ConfigSection section )
    {
        mFpsFrame = new Image( mResources, directory + "/" + filename + ".png" );
        
        // Position (percentages of the screen dimensions)
        mFpsFrameX = SafeMethods.toInt( section.get( "x" ), 0 );
        mFpsFrameY = SafeMethods.toInt( section.get( "y" ), 0 );
        
        // Number position (percentages of the FPS indicator dimensions)
        mFpsTextX = SafeMethods.toInt( section.get( "numx" ), 50 );
        mFpsTextY = SafeMethods.toInt( section.get( "numy" ), 50 );
        
        // Refresh rate (in frames.. integer greater than 1)
        mFpsRecalcPeriod = SafeMethods.toInt( section.get( "rate" ), 15 );
        
        // Need at least 2 frames to calculate FPS
        if( mFpsRecalcPeriod < 2 )
            mFpsRecalcPeriod = 2;
        
        // Numeral font
        String fpsFont = section.get( "font" );
        if( fpsFont != null && fpsFont.length() > 0 )
        {
            // Load the font images
            int i = 0;
            try
            {
                // Make sure we can load them (they might not even exist)
                for( i = 0; i < mNumerals.length; i++ )
                    mNumerals[i] = new Image( mResources, mFontsDir + fpsFont + "/" + i + ".png" );
            }
            catch( Exception e )
            {
                // Problem, let the user know
                Log.e( "VisibleTouchMap", "Problem loading font '" + mFontsDir + fpsFont + "/" + i
                        + ".png', error message: " + e.getMessage() );
            }
        }
    }
}
