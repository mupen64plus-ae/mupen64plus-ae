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
package paulscode.android.mupen64plusae.input.map;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.util.DisplayMetrics;

import java.util.Arrays;

import paulscode.android.mupen64plusae.game.GameOverlay;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.profile.Profile;
import paulscode.android.mupen64plusae.util.Image;

/**
 * A kind of touch map that can be drawn on a canvas.
 * 
 * @see TouchMap
 * @see GameOverlay
 */
public class VisibleTouchMap extends TouchMap
{
    /** The factor to scale images by. */
    private float mScalingFactor = 1.0f;
    
    /** Touchscreen opacity. */
    private int mTouchscreenTransparency;
    
    /** The last width passed to {@link #resize(int, int, DisplayMetrics)}. */
    private int cacheWidth = 0;
    
    /** The last height passed to {@link #resize(int, int, DisplayMetrics)}. */
    private int cacheHeight = 0;
    
    /** The last height passed to {@link #resize(int, int, DisplayMetrics)}. */
    private DisplayMetrics cacheMetrics;
    
    /** Auto-hold overlay images. */
    private final Image[] autoHoldImages;

    /** Auto-hold overlay images pressed status */
    private final boolean[] autoHoldImagesPressed;
    
    /** X-coordinates of the AutoHold mask, in percent. */
    private final int[] autoHoldX;
    
    /** Y-coordinates of the AutoHold mask, in percent. */
    private final int[] autoHoldY;
    
    /**
     * Instantiates a new visible touch map.
     * 
     * @param resources  The resources of the activity associated with this touch map.
     */
    public VisibleTouchMap( Resources resources )
    {
        super( resources );
        autoHoldImages = new Image[NUM_N64_PSEUDOBUTTONS];
        autoHoldImagesPressed = new boolean[NUM_N64_PSEUDOBUTTONS];
        autoHoldX = new int[NUM_N64_PSEUDOBUTTONS];
        autoHoldY = new int[NUM_N64_PSEUDOBUTTONS];
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
        for( int i = 0; i < autoHoldImages.length; i++ )
        {
            autoHoldImagesPressed[i] = false;
            autoHoldImages[i] = null;
        }
        Arrays.fill(autoHoldX, 0);
        Arrays.fill(autoHoldY, 0);
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
    
    /**
     * Returns true if A/B buttons are split
     * 
     */
    public boolean isABSplit()
    {
        return mSplitAB;
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
            int cX = analogBackImage.x + (int) ( analogBackImage.hWidth * ( analogBackScaling * scale ) );
            int cY = analogBackImage.y + (int) ( analogBackImage.hHeight * ( analogBackScaling * scale ) );
            analogForeImage.setScale( ( analogBackScaling * scale ) );
            analogForeImage.fitCenter( cX, cY, analogBackImage.x, analogBackImage.y,
                    (int) ( analogBackImage.width * ( analogBackScaling * scale ) ), (int) ( analogBackImage.height * ( analogBackScaling * scale ) ) );
        }
        
        // Compute auto-hold overlay locations
        for( int i = 0; i < autoHoldImages.length; i++ )
        {
            if( autoHoldImages[i] != null )
            {
                String name = ASSET_NAMES.get( i );
                float scaling = 1.f;
                
                for( int j = 0; j < buttonNames.size(); j++ )
                {
                    if ( buttonNames.get( j ).equals( name ) )
                        scaling = buttonScaling.get( j );
                }
                
                autoHoldImages[i].setScale( ( scaling * scale ) );
                autoHoldImages[i].fitPercent( autoHoldX[i], autoHoldY[i], w, h );
            }
        }
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
     * Updates the analog stick assets to reflect a new position.
     * 
     * @param axisFractionX The x-axis fraction, between -1 and 1, inclusive.
     * @param axisFractionY The y-axis fraction, between -1 and 1, inclusive.
     * 
     * @return True if the analog assets changed.
     */
    public boolean updateAnalog( float axisFractionX, float axisFractionY )
    {
        if( analogForeImage != null && analogBackImage != null )
        {
            // Get the location of stick center
            int hX = (int) ( ( analogBackImage.hWidth + ( axisFractionX * analogMaximum ) ) * ( analogBackScaling * scale ) );
            int hY = (int) ( ( analogBackImage.hHeight - ( axisFractionY * analogMaximum ) ) * ( analogBackScaling * scale ) );
            
            // Use other values if invalid
            if( hX < 0 )
                hX = (int) ( analogBackImage.hWidth * ( analogBackScaling * scale ) );
            if( hY < 0 )
                hY = (int) ( analogBackImage.hHeight * ( analogBackScaling * scale ) );

            int width = (int) ( analogBackImage.width * ( analogBackScaling * scale ) );
            int height = (int) ( analogBackImage.height * ( analogBackScaling * scale ) );

            // Update position of the surrounding graphic
            analogBackImage.fitCenter(currentAnalogX + hX, currentAnalogY + hY, currentAnalogX, currentAnalogY, width, height);

            // Update the position of the stick
            analogForeImage.fitCenter(currentAnalogX + hX, currentAnalogY + hY, currentAnalogX, currentAnalogY, width, height );
            return true;
        }
        return false;
    }
    
    /**
     * Updates the auto-hold assets to reflect a new value.
     * 
     * @param pressed The new autohold state value.
     * @param index   The index of the auto-hold mask.
     * 
     * @return True if the autohold assets changed.
     */
    public boolean updateAutoHold( boolean pressed, int index )
    {
        autoHoldImagesPressed[index] = pressed;

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
     * Loads all touch map data from the filesystem.
     * 
     * @param skinDir    The directory containing the skin.ini and image files.
     * @param profile    The name of the touchscreen profile.
     * @param animated   True to load the analog assets in two parts for animation.
     * @param scale      The factor to scale images by.
     * @param alpha      The opacity of the visible elements.
     */
    public void load(Context context, String skinDir, Profile profile, boolean animated, float scale, int alpha )
    {
        mScalingFactor = scale;
        mTouchscreenTransparency = alpha;
        ConfigFile skin_ini = new ConfigFile( context, skinDir + "/skin.ini" );

        super.load( context, skin_ini, skinDir, profile, animated );

        // Scale the assets to the last screensize used
        resize( cacheWidth, cacheHeight, cacheMetrics );
    }
    
    /**
     * Refreshes the position of a touchscreen button image.
     *
     * @param profile    The name of the touchscreen profile.
     * @param name       The name of the button.
     */
    public void refreshButtonPosition( Profile profile, String name )
    {
        super.updateButton( profile, name, cacheWidth, cacheHeight );
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * paulscode.android.mupen64plusae.input.map.TouchMap#loadAllAssets(paulscode.android.mupen64plusae
     * .profile.Profile, boolean)
     */
    @Override
    protected void loadAllAssets( Context context, Profile profile, boolean animated )
    {
        super.loadAllAssets( context, profile, animated );
        
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

        if( profile != null )
        {
            if( mSplitAB  )
            {
                loadAutoHoldImages(context, profile, "buttonA-holdA");
                loadAutoHoldImages(context, profile, "buttonB-holdB");
            }
            else
            {
                loadAutoHoldImages(context, profile, "groupAB-holdA");
                loadAutoHoldImages(context, profile, "groupAB-holdB");
            }
            loadAutoHoldImages(context, profile, "groupC-holdCu");
            loadAutoHoldImages(context, profile, "groupC-holdCd");
            loadAutoHoldImages(context, profile, "groupC-holdCl");
            loadAutoHoldImages(context, profile, "groupC-holdCr");
            loadAutoHoldImages(context, profile, "buttonL-holdL");
            loadAutoHoldImages(context, profile, "buttonR-holdR");
            loadAutoHoldImages(context, profile, "buttonZ-holdZ");
            loadAutoHoldImages(context, profile, "buttonS-holdS");
            loadAutoHoldImages(context, profile, "buttonSen-holdSen");
        }
    }
    
    @Override
    public void setAnalogEnabled(boolean enabled) {
        super.setAnalogEnabled(enabled);
        if (analogBackImage != null) {
            if (enabled) {
                analogBackImage.setAlpha(mTouchscreenTransparency);
            } else {
                analogBackImage.setAlpha(0);
            }
        }
        if (analogForeImage != null) {
            if (enabled) {
                analogForeImage.setAlpha(mTouchscreenTransparency);
            } else {
                analogForeImage.setAlpha(0);
            }
        }
    }



    /**
     * Hides the touch controller
     */
    public boolean hideTouchController()
    {
        // Set the transparency of the images
        for( Image buttonImage : buttonImages )
        {
            if(buttonImage != null)
                buttonImage.setAlpha( 0 );
        }

        for( Image autoHoldImage : autoHoldImages)
        {
            if(autoHoldImage != null )
                autoHoldImage.setAlpha( 0 );
        }

        if( analogBackImage != null )
        {
            analogBackImage.setAlpha( 0 );
        }
        if( analogForeImage != null )
        {
            analogForeImage.setAlpha( 0 );
        }

        return true;
    }

    /**
     * Shows the touch controller
     */
    public void setTouchControllerAlpha(double alpha)
    {
        if (alpha < 0) {
            alpha = 0;
        } else if (alpha > 1.0) {
            alpha = 1.0;
        }
        // Set the transparency of the images
        for( Image buttonImage : buttonImages )
        {
            if(buttonImage != null)
                buttonImage.setAlpha( (int)(mTouchscreenTransparency*alpha) );
        }

        for( int index = 0; index < autoHoldImages.length; ++index)
        {

            Image autoHoldImage = autoHoldImages[index];
            if(autoHoldImage != null && autoHoldImagesPressed[index] )
                autoHoldImage.setAlpha( (int)(mTouchscreenTransparency*alpha) );
        }

        if (analogBackImage != null) {
            analogBackImage.setAlpha((int)(mTouchscreenTransparency*alpha));
        }
        if (analogForeImage != null) {
            analogForeImage.setAlpha((int)(mTouchscreenTransparency*alpha));
        }
    }

    /**
     * Shows the touch controller
     */
    public boolean showTouchController()
    {
        // Set the transparency of the images
        for( Image buttonImage : buttonImages )
        {
            if(buttonImage != null)
                buttonImage.setAlpha( mTouchscreenTransparency );
        }

        for( int index = 0; index < autoHoldImages.length; ++index)
        {

            Image autoHoldImage = autoHoldImages[index];
            if(autoHoldImage != null && autoHoldImagesPressed[index] )
                autoHoldImage.setAlpha( mTouchscreenTransparency );
        }

        if (analogBackImage != null) {
            analogBackImage.setAlpha(mTouchscreenTransparency);
        }
        if (analogForeImage != null) {
            analogForeImage.setAlpha(mTouchscreenTransparency);
        }
        return true;
    }

    /**
     * Loads auto-hold assets and properties from the filesystem.
     * 
     * @param profile The touchscreen profile containing the auto-hold properties.
     * @param name The name of the image to load.
     */
    private void loadAutoHoldImages( Context context, Profile profile, String name )
    {
        if ( !name.contains("-hold") )
            return;
        
        String[] fields = name.split( "-hold" );
        String group = fields[0];
        String hold = fields[1];
        
        int x = profile.getInt( group + "-x", -1 );
        int y = profile.getInt( group + "-y", -1 );
        Integer index = MASK_KEYS.get( hold );
        
        if( x >= 0 && y >= 0 && index != null )
        {
            // Position (percentages of the digitizer dimensions)
            autoHoldX[index] = x;
            autoHoldY[index] = y;
            
            // The drawable image is in PNG image format.
            autoHoldImages[index] = new Image( context, mResources, skinFolder + "/" + name + ".png" );
            autoHoldImages[index].setAlpha( 0 );
        }
    }
}
