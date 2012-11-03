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
package paulscode.android.mupen64plusae.input.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import paulscode.android.mupen64plusae.Globals;
import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.util.Image;
import paulscode.android.mupen64plusae.util.SafeMethods;
import paulscode.android.mupen64plusae.util.SubscriptionManager;
import paulscode.android.mupen64plusae.util.Utility;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

public class TouchMap
{
    public interface Listener
    {
        public void onAllChanged( TouchMap touchMap );
        
        public void onHatChanged( TouchMap touchMap, float x, float y );
        
        public void onFpsChanged( TouchMap touchMap, int fps );
    }
    
    // Pseudo-button indices, indicating simultaneous press of two d-pad buttons
    public static final int DPD_RU = AbstractController.NUM_BUTTONS;
    public static final int DPD_RD = DPD_RU + 1;
    public static final int DPD_LD = DPD_RU + 2;
    public static final int DPD_LU = DPD_RU + 3;
    
    // Mask colors
    private int[] maskColors;
    
    // Buttons
    private ArrayList<Image> masks;
    private ArrayList<Image> buttons;
    private ArrayList<Integer> xpercents;
    private ArrayList<Integer> ypercents;
    
    // Analog background
    private Image analogImage;
    private int analogXpercent;
    private int analogYpercent;
    private int analogPadding;
    private int analogDeadzone;
    private int analogMaximum;
    
    // Analog stick
    private Image hatImage;
    private int hatX;
    private int hatY;
    
    // Frame rate indicator
    private Image fpsImage;
    private int fpsXpercent;
    private int fpsYpercent;
    private int fpsNumXpercent;
    private int fpsNumYpercent;
    private int fpsRecalcRate;
    private int fpsValue;
    private String fpsFont;
    private Image[] numeralImages;
    private Image[] fpsDigits;
    
    private Resources mResources;
    private SubscriptionManager<Listener> mPublisher;
    private static final HashMap<String, Integer> BUTTON_HASHMAP;
    
    static
    {
        BUTTON_HASHMAP = new HashMap<String, Integer>();
        BUTTON_HASHMAP.put( "right", AbstractController.DPD_R );
        BUTTON_HASHMAP.put( "left", AbstractController.DPD_L );
        BUTTON_HASHMAP.put( "down", AbstractController.DPD_D );
        BUTTON_HASHMAP.put( "up", AbstractController.DPD_U );
        BUTTON_HASHMAP.put( "start", AbstractController.START );
        BUTTON_HASHMAP.put( "z", AbstractController.BTN_Z );
        BUTTON_HASHMAP.put( "b", AbstractController.BTN_B );
        BUTTON_HASHMAP.put( "a", AbstractController.BTN_A );
        BUTTON_HASHMAP.put( "cright", AbstractController.CPD_R );
        BUTTON_HASHMAP.put( "cleft", AbstractController.CPD_L );
        BUTTON_HASHMAP.put( "cdown", AbstractController.CPD_D );
        BUTTON_HASHMAP.put( "cup", AbstractController.CPD_U );
        BUTTON_HASHMAP.put( "r", AbstractController.BTN_R );
        BUTTON_HASHMAP.put( "l", AbstractController.BTN_L );
        BUTTON_HASHMAP.put( "upright", DPD_RU );
        BUTTON_HASHMAP.put( "rightdown", DPD_RD );
        BUTTON_HASHMAP.put( "leftdown", DPD_LD );
        BUTTON_HASHMAP.put( "leftup", DPD_LU );
    }
    
    public TouchMap()
    {
        mPublisher = new SubscriptionManager<TouchMap.Listener>();
        numeralImages = new Image[10];
        fpsDigits = new Image[4];
        maskColors = new int[BUTTON_HASHMAP.size()];
        clear();
    }
    
    public void clear()
    {
        buttons = new ArrayList<Image>();
        masks = new ArrayList<Image>();
        xpercents = new ArrayList<Integer>();
        ypercents = new ArrayList<Integer>();
        analogImage = null;
        analogXpercent = analogYpercent = 0;
        analogPadding = 32;
        analogDeadzone = 2;
        analogMaximum = 360;
        hatImage = null;
        hatX = hatY = -1;
        fpsImage = null;
        fpsXpercent = fpsYpercent = 0;
        fpsNumXpercent = fpsNumYpercent = 50;
        fpsRecalcRate = 15;
        fpsValue = 0;
        fpsFont = "Mupen64Plus-AE-Contrast-Blue";
        for( int i = 0; i < numeralImages.length; i++ )
            numeralImages[i] = null;
        for( int i = 0; i < fpsDigits.length; i++ )
            fpsDigits[i] = null;
        for( int i = 0; i < maskColors.length; i++ )
            maskColors[i] = -1;
    }
    
    public void registerListener( Listener listener )
    {
        mPublisher.subscribe( listener );
    }
    
    public void unregisterListener( Listener listener )
    {
        mPublisher.unsubscribe( listener );
    }
    
    public void setResources( Resources resources )
    {
        mResources = resources;
        for( int i = 0; i < numeralImages.length; i++ )
            numeralImages[i] = new Image( resources, Globals.paths.fontsDir + fpsFont + "/"
                    + i + ".png" );
    }
    
    public void resize( int w, int h )
    {
        // Position the buttons
        for( int i = 0; i < buttons.size(); i++ )
        {
            buttons.get( i ).fitCenter(
                    (int) ( (float) w * ( (float) xpercents.get( i ) / 100.0f ) ),
                    (int) ( (float) h * ( (float) ypercents.get( i ) / 100.0f ) ), w, h );
            masks.get( i ).fitCenter(
                    (int) ( (float) w * ( (float) xpercents.get( i ) / 100.0f ) ),
                    (int) ( (float) h * ( (float) ypercents.get( i ) / 100.0f ) ), w, h );
        }
        
        // Position the analog control
        if( analogImage != null )
        {
            analogImage.fitCenter( (int) ( (float) w * ( (float) analogXpercent / 100.0f ) ),
                    (int) ( (float) h * ( (float) analogYpercent / 100.0f ) ), w, h );
        }
        
        // Position the FPS box
        if( fpsImage != null )
        {
            // Position the background image and draw it
            fpsImage.fitCenter( (int) ( (float) w * ( (float) fpsXpercent / 100.0f ) ),
                    (int) ( (float) h * ( (float) fpsYpercent / 100.0f ) ), w, h );
        }
        
        // Notify listeners that everything has changed
        for( Listener listener : mPublisher.getSubscribers() )
            listener.onAllChanged( this );
    }
    
    public void updateHat( float axisFractionX, float axisFractionY )
    {
        // Move the analog hat based on analog state
        hatX = analogImage.hWidth + (int) ( axisFractionX * (float) analogMaximum );
        hatY = analogImage.hHeight - (int) ( axisFractionY * (float) analogMaximum );
        
        // Notify listeners that analog hat changed
        for( Listener listener : mPublisher.getSubscribers() )
            listener.onHatChanged( this, axisFractionX, axisFractionY );
    }
    
    public void updateFps( int fps )
    {
        // Clamp to positive, four digits max
        fps = Math.max( Math.min( fps, 9999 ), 0 );
        
        // Quick return if user has disabled FPS or it hasn't changed
        if( !Globals.userPrefs.isFrameRateEnabled || fpsValue == fps )
            return;
        
        // Store the new value
        fpsValue = fps;
        
        // Assemble a sprite for the FPS value
        String fpsString = Integer.toString( fpsValue );
        for( int i = 0; i < 4; i++ )
        {
            // Create a new sequence of numeral images
            if( i < fpsString.length() )
            {
                try
                {
                    // Clone the numeral from the font images
                    fpsDigits[i] = new Image( mResources,
                            numeralImages[Integer.valueOf( fpsString.substring( i, i + 1 ) )] );
                }
                catch( NumberFormatException nfe )
                {
                    // Skip this digit, there was a problem
                    fpsDigits[i] = null;
                }
            }
            else
            {
                // Skip this digit
                fpsDigits[i] = null;
            }
        }
        
        // Notify listeners that FPS sprite changed
        for( Listener listener : mPublisher.getSubscribers() )
            listener.onFpsChanged( this, fpsValue );
    }
    
    public Rect getAnalogBounds()
    {
        return analogImage.drawRect;
    }
    
    public Rect getFpsBounds()
    {
        return fpsImage.drawRect;
    }
    
    public int getFpsRecalcRate()
    {
        return fpsRecalcRate;
    }
    
    public int getButtonPress( int xLocation, int yLocation )
    {
        for( Image mask : masks )
        {
            if( mask != null )
            {
                int left = mask.x;
                int right = left + mask.width;
                int bottom = mask.y;
                int top = bottom + mask.height;
                
                // Check each one in sequence
                if( xLocation >= left && xLocation < right && yLocation >= bottom
                        && yLocation < top )
                {
                    // It is inside this button, check the color mask
                    int c = mask.image.getPixel( xLocation - mask.x, yLocation - mask.y );
                    
                    // Ignore the alpha component if any
                    int rgb = (int) ( c & 0x00ffffff );
                    
                    // Ignore black and modify the button states
                    if( rgb > 0 )
                        return getButtonFromColor( rgb );
                }
            }
        }
        return -1;
    }
    
    public Point getAnalogDisplacement( int xLocation, int yLocation )
    {
        if( analogImage == null )
            return new Point( 0, 0 );
        
        // Distance from center along x-axis
        int dX = xLocation - ( analogImage.x + analogImage.hWidth );
        
        // Distance from center along y-axis
        int dY = yLocation - ( analogImage.y + analogImage.hHeight );
        
        return new Point( dX, dY );
    }
    
    public Point getConstrainedDisplacement( int dX, int dY )
    {
        return Utility.constrainToOctagon( dX, dY, analogMaximum );
    }
    
    public float getAnalogStrength( float displacement )
    {
        // Fraction of full-throttle, clamped to range [0-1]
        float p = ( displacement - analogDeadzone ) / (float) ( analogMaximum - analogDeadzone );
        return Math.max( Math.min( p, 1 ), 0 );
    }
    
    public boolean isAnalogCaptured( float displacement )
    {
        return ( displacement >= analogDeadzone )
                && ( displacement < analogMaximum + analogPadding );
    }
    
    public void drawStatic( Canvas canvas )
    {
        // Draw the buttons onto the canvas
        for( Image button : buttons )
            button.draw( canvas );
    }
    
    public void drawAnalog( Canvas canvas )
    {
        if( analogImage == null )
            return;
        
        // Draw the background image first
        analogImage.draw( canvas );
        
        // Then draw the movable part of the stick
        if( hatImage != null )
        {
            // Reposition the image and draw it
            int hX = hatX;
            int hY = hatY;
            if( hX == -1 )
                hX = analogImage.hWidth;
            if( hY == -1 )
                hY = analogImage.hHeight;
            hatImage.fitCenter( analogImage.x + hX, analogImage.y + hY, analogImage.x,
                    analogImage.y, analogImage.width, analogImage.height );
            hatImage.draw( canvas );
        }
    }
    
    public void drawFps( Canvas canvas )
    {
        // Redraw the FPS indicator
        int x = 0;
        int y = 0;
        
        if( fpsImage != null )
        {
            x = fpsImage.x + (int) ( (float) fpsImage.width * ( (float) fpsNumXpercent / 100.0f ) );
            y = fpsImage.y + (int) ( (float) fpsImage.height * ( (float) fpsNumYpercent / 100.0f ) );
            fpsImage.draw( canvas );
        }
        
        int totalWidth = 0;
        
        // Calculate the width of the FPS text
        for( int i = 0; i < fpsDigits.length; i++ )
        {
            if( fpsDigits[i] != null )
                totalWidth += fpsDigits[i].width;
        }
        
        // Calculate the starting position of the FPS text
        x = x - (int) ( (float) totalWidth / 2f );
        
        // Draw each digit of the FPS number
        for( int i = 0; i < fpsDigits.length; i++ )
        {
            if( fpsDigits[i] != null )
            {
                fpsDigits[i].setPos( x, y - fpsDigits[i].hHeight );
                fpsDigits[i].draw( canvas );
                x += fpsDigits[i].width;
            }
        }
    }
    
    public void load( String directory )
    {
        // Always clear the skin
        clear();
        
        // If no skin to display, quit
        if( !Globals.userPrefs.isTouchscreenEnabled && !Globals.userPrefs.isFrameRateEnabled )
            return;
        
        // Load the configuration file (pad.ini)
        ConfigFile pad_ini = new ConfigFile( directory + "/pad.ini" );
        
        // Look up the mask colors
        readMaskColors( pad_ini );
        
        // Loop through all the sections
        readFiles( pad_ini, directory );
        
        // Free the data that was loaded from the config file:
        pad_ini.clear();
        pad_ini = null;
    }
    
    private void readMaskColors( ConfigFile pad_ini )
    {
        ConfigSection section = pad_ini.get( "MASK_COLOR" );
        if( section != null )
        {
            Set<String> keys = section.keySet();
            Iterator<String> iter = keys.iterator();
            while( iter.hasNext() )
            {
                // Loop through the param=val pairs
                String param = iter.next();
                String val = section.get( param );
                
                // Assign the map colors to the appropriate N64 button
                maskColors[BUTTON_HASHMAP.get( param.toLowerCase() )] = SafeMethods.toInt( val, -1 );
            }
        }
    }
    
    private void readFiles( ConfigFile pad_ini, final String layoutFolder )
    {
        for( String filename : pad_ini.keySet() )
        {
            // Make sure it's a filename
            if( isFilename( filename ) )
            {
                ConfigSection section = pad_ini.get( filename );
                if( section != null )
                {
                    // Get the type of control
                    String value = section.get( "info" );
                    if( value != null )
                    {
                        // Let's not make this part case-sensitive
                        value = value.toLowerCase();
                        
                        if( value.contains( "analog" ) )
                            readAnalogLayout( layoutFolder, filename, section,
                                    value.contains( "hat" ) );
                        else if( value.contains( "fps" ) )
                            readFpsLayout( layoutFolder, filename, section );
                        else
                            readStaticLayout( layoutFolder, filename, section );
                    }
                }
            }
        }
    }
    
    private boolean isFilename( String key )
    {
        return key != null && key.length() > 0 && !key.equals( "INFO" )
                && !key.equals( "MASK_COLOR" ) && !key.equals( "[<sectionless!>]" );
    }
    
    private void readStaticLayout( final String layoutFolder, String filename, ConfigSection section )
    {
        // A button control. The drawable image is in PNG image format. The
        // color mask image is in BMP image format (doesn't actually get drawn).
        buttons.add( new Image( mResources, layoutFolder + "/" + filename + ".png" ) );
        masks.add( new Image( mResources, layoutFolder + "/" + filename + ".bmp" ) );
        
        // Position (percentages of the screen dimensions)
        xpercents.add( SafeMethods.toInt( section.get( "x" ), 0 ) );
        ypercents.add( SafeMethods.toInt( section.get( "y" ), 0 ) );
    }
    
    private void readAnalogLayout( final String layoutFolder, String filename,
            ConfigSection section, boolean hasHat )
    {
        analogImage = new Image( mResources, layoutFolder + "/" + filename + ".png" );
        if( hasHat )
        {
            // There's a "stick" image.. same name, with "_2" appended
            hatImage = new Image( mResources, layoutFolder + "/" + filename + "_2.png" );
        }
        
        // Position (percentages of the screen dimensions)
        analogXpercent = SafeMethods.toInt( section.get( "x" ), 0 );
        analogYpercent = SafeMethods.toInt( section.get( "y" ), 0 );
        
        // Sensitivity (percentages of the radius, i.e. half the image width)
        analogDeadzone = (int) ( (float) analogImage.hWidth * ( SafeMethods.toFloat(
                section.get( "min" ), 1 ) / 100.0f ) );
        analogMaximum = (int) ( (float) analogImage.hWidth * ( SafeMethods.toFloat(
                section.get( "max" ), 55 ) / 100.0f ) );
        analogPadding = (int) ( (float) analogImage.hWidth * ( SafeMethods.toFloat(
                section.get( "buff" ), 55 ) / 100.0f ) );
    }
    
    private void readFpsLayout( final String layoutFolder, String filename, ConfigSection section )
    {
        fpsImage = new Image( mResources, layoutFolder + "/" + filename + ".png" );
        
        // Position (percentages of the screen dimensions)
        fpsXpercent = SafeMethods.toInt( section.get( "x" ), 0 );
        fpsYpercent = SafeMethods.toInt( section.get( "y" ), 0 );
        
        // Number position (percentages of the FPS indicator dimensions)
        fpsNumXpercent = SafeMethods.toInt( section.get( "numx" ), 50 );
        fpsNumYpercent = SafeMethods.toInt( section.get( "numy" ), 50 );
        
        // Refresh rate (in frames.. integer greater than 1)
        fpsRecalcRate = SafeMethods.toInt( section.get( "rate" ), 15 );
        
        // Need at least 2 frames to calculate FPS
        if( fpsRecalcRate < 2 )
            fpsRecalcRate = 2;
        
        // Numeral font
        fpsFont = section.get( "font" );
        if( fpsFont != null && fpsFont.length() > 0 )
        {
            // Load the font images
            int i = 0;
            try
            {
                // Make sure we can load them (they might not even exist)
                for( i = 0; i < 10; i++ )
                    numeralImages[i] = new Image( mResources, Globals.paths.fontsDir
                            + fpsFont + "/" + i + ".png" );
            }
            catch( Exception e )
            {
                // Problem, let the user know
                Log.e( "GamePad", "Problem loading font '" + Globals.paths.fontsDir + fpsFont + "/"
                        + i + ".png', error message: " + e.getMessage() );
            }
        }
    }
    
    private int getButtonFromColor( int color )
    {
        // TODO: Android is not precise: the color is different than it should be!
        // Find the closest match among the N64 buttons
        int closestMatch = -1;
        int matchDif = Integer.MAX_VALUE;
        for( int i = 0; i < maskColors.length; i++ )
        {
            int dif = Math.abs( maskColors[i] - color );
            if( dif < matchDif )
            {
                closestMatch = i;
                matchDif = dif;
            }
        }
        return closestMatch;
    }
}
