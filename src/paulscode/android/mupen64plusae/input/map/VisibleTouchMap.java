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
 * Authors: Paul Lamb, littleguy77
 */
package paulscode.android.mupen64plusae.input.map;

import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import paulscode.android.mupen64plusae.GameOverlay;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
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
    
    /** Touchscreen opacity. */
    private final int mTouchscreenTransparency;
    
    /** The file directory containing the FPS font resources. */
    private final String mFontsDir;
    
    /** The set of images representing the FPS string. */
    private final CopyOnWriteArrayList<Image> mFpsDigits;
    
    /** The set of images representing the numerals 0, 1, 2, ..., 9. */
    private final Image[] mNumerals;
    
    /** Auto-hold overlay images. */
    public final Image[] autoHoldImages;
    
    /** X-coordinates of the AutoHold mask, in percent. */
    private final int[] autoHoldX;
    
    /** Y-coordinates of the AutoHold mask, in percent. */
    private final int[] autoHoldY;
    
    /**
     * Instantiates a new visible touch map.
     * 
     * @param resources The resources of the activity associated with this touch map.
     * @param fpsEnabled True to display the FPS indicator.
     * @param fontsDir The directory containing the FPS font resources.
     * @param alpha The opacity of the visible elements.
     */
    public VisibleTouchMap( Resources resources, boolean fpsEnabled, String fontsDir, int alpha )
    {
        super( resources );
        mFpsEnabled = fpsEnabled;
        mFontsDir = fontsDir;
        mFpsDigits = new CopyOnWriteArrayList<Image>();
        mNumerals = new Image[10];
        autoHoldImages = new Image[NUM_N64_PSEUDOBUTTONS];
        autoHoldX = new int[NUM_N64_PSEUDOBUTTONS];
        autoHoldY = new int[NUM_N64_PSEUDOBUTTONS];
        mTouchscreenTransparency = alpha;
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
        for( int i = 0; i < autoHoldImages.length; i++ )
            autoHoldImages[i] = null;
        for( int i = 0; i < autoHoldX.length; i++ )
            autoHoldX[i] = 0;
        for( int i = 0; i < autoHoldY.length; i++ )
            autoHoldY[i] = 0;
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
        
        // Compute auto-hold overlay locations
        for( int i = 0; i < autoHoldImages.length; i++ )
        {
            if( autoHoldImages[i] != null )
            {
                int cX = (int) ( w * ( (float) autoHoldX[i] / 100f ) );
                int cY = (int) ( h * ( (float) autoHoldY[i] / 100f ) );
                autoHoldImages[i].fitCenter( cX, cY, w, h );
            }
        }
        
        // Compute FPS frame location
        if( mFpsFrame != null )
        {
            int cX = (int) ( w * ( mFpsFrameX / 100f ) );
            int cY = (int) ( h * ( mFpsFrameY / 100f ) );
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
        {
            button.draw( canvas );
        }
    }
    
    /**
     * Draws the AutoHold mask.
     * 
     * @param canvas The canvas on which to draw.
     */
    public void drawAutoHold( Canvas canvas )
    {
        // Draw the AutoHold mask onto the canvas
        for( Image autoHoldImage : autoHoldImages )
        {
            if( autoHoldImage != null )
            {
                autoHoldImage.draw( canvas );
            }
        }
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
        {
            analogBackImage.draw( canvas );
        }
        
        // Draw the movable foreground (the stick)
        if( analogForeImage != null )
        {
            analogForeImage.draw( canvas );
        }
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
            int hX = analogBackImage.hWidth + (int) ( axisFractionX * analogMaximum );
            int hY = analogBackImage.hHeight - (int) ( axisFractionY * analogMaximum );
            
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
     * Updates the auto-hold assets to reflect a new value.
     * 
     * @param pressed The new autohold state value.
     * @param index The index of the auto-hold mask.
     * @return True if the autohold assets changed.
     */
    public boolean updateAutoHold( boolean pressed, int index )
    {
        if( autoHoldImages[index] != null )
        {
            if( pressed )
                autoHoldImages[index].setAlpha( mTouchscreenTransparency );
            else
                autoHoldImages[index].setAlpha( 0 );
            return true;
        }
        return false;
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
            x = mFpsFrame.x + (int) ( mFpsFrame.width * ( mFpsTextX / 100f ) );
            y = mFpsFrame.y + (int) ( mFpsFrame.height * ( mFpsTextY / 100f ) );
        }
        
        // Compute the width of the FPS text
        int totalWidth = 0;
        for( Image digit : mFpsDigits )
            totalWidth += digit.width;
        
        // Compute the starting position of the FPS text
        x = x - (int) ( totalWidth / 2f );
        
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
     * @see
     * paulscode.android.mupen64plusae.input.map.TouchMap#loadAllAssets(paulscode.android.mupen64plusae
     * .persistent.ConfigFile, java.lang.String)
     */
    @Override
    protected void loadAllAssets( ConfigFile pad_ini, String directory )
    {
        super.loadAllAssets( pad_ini, directory );
        
        // Set the transparency of the images
        for( Image buttonImage : buttonImages )
        {
            buttonImage.setAlpha( mTouchscreenTransparency );
        }
        if( analogBackImage != null )
        {
            analogBackImage.setAlpha( mTouchscreenTransparency );
        }        
        if( analogForeImage != null )
        {
            analogForeImage.setAlpha( mTouchscreenTransparency );
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
            String info )
    {
        if( info.contains( "fps" ) )
            loadFpsIndicator( directory, filename, section );
        else if( filename.contains( "AUTOHOLD" ) )
            loadAutoHold( directory, filename, section, info );
        else
            super.loadAssetSection( directory, filename, section, info );
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
    
    /**
     * Loads auto-hold assets and properties from the filesystem.
     * 
     * @param directory The directory containing the auto-hold assets.
     * @param filename The filename of the auto-hold assets, without extension.
     * @param section The configuration section containing the auto-hold properties.
     * @param info The information section containing the auto-hold button.
     */
    private void loadAutoHold( final String directory, String filename, ConfigSection section,
            String info )
    {
        // Assign the auto-hold option to the appropriate N64 button
        if( info != null )
        {
            // TODO: fix possible crash when info isn't a N64 button
            int index = BUTTON_STRING_MAP.get( info.toLowerCase( Locale.US ) );
            
            // The drawable image is in PNG image format.
            autoHoldImages[index] = new Image( mResources, directory + "/" + filename + ".png" );
            autoHoldImages[index].setAlpha( 0 );
            
            // Position (percentages of the digitizer dimensions)
            autoHoldX[index] = SafeMethods.toInt( section.get( "x" ), 0 );
            autoHoldY[index] = SafeMethods.toInt( section.get( "y" ), 0 );
        }
    }
}
