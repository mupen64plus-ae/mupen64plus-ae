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

import paulscode.android.mupen64plusae.Globals;
import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.util.Image;
import paulscode.android.mupen64plusae.util.SafeMethods;
import paulscode.android.mupen64plusae.util.Utility;
import android.content.res.Resources;
import android.graphics.Point;

public class TouchMap
{
    public static final int DPD_RU = AbstractController.NUM_BUTTONS;
    public static final int DPD_RD = DPD_RU + 1;
    public static final int DPD_LD = DPD_RU + 2;
    public static final int DPD_LU = DPD_RU + 3;
    
    // Mask colors
    private int[] maskColors;
    
    // Buttons
    protected ArrayList<Image> buttons;
    private ArrayList<Image> masks;
    private ArrayList<Integer> xpercents;
    private ArrayList<Integer> ypercents;
    
    // Analog background
    protected Image analogImage;
    private int analogXpercent;
    private int analogYpercent;
    private int analogPadding;
    private int analogDeadzone;
    protected int analogMaximum;
    
    // Analog stick
    protected Image hatImage;
    protected int hatX;
    protected int hatY;
    
    // App resources
    protected Resources mResources;
    
    // Lookup table for mapping strings in file to N64 indices
    protected static final HashMap<String, Integer> BUTTON_HASHMAP;
    
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
        maskColors = new int[BUTTON_HASHMAP.size()];
        clear();
    }
    
    public void setResources( Resources resources )
    {
        mResources = resources;
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
        for( int i = 0; i < maskColors.length; i++ )
            maskColors[i] = -1;
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
        return Utility.clamp(p, 0, 1);
    }
    
    public boolean isAnalogCaptured( float displacement )
    {
        return ( displacement >= analogDeadzone )
                && ( displacement < analogMaximum + analogPadding );
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
            // Loop through the key-value pairs
            for( String key : section.keySet() )
            {
                // Assign the map colors to the appropriate N64 button
                String val = section.get( key );
                int index = BUTTON_HASHMAP.get( key.toLowerCase() );
                maskColors[index] = SafeMethods.toInt( val, -1 );
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
                        readFileSection( layoutFolder, filename, section, value.toLowerCase() );
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
    
    protected void readFileSection( final String layoutFolder, String filename,
            ConfigSection section, String value )
    {
        if( value.contains( "analog" ) )
            readAnalogLayout( layoutFolder, filename, section, value.contains( "hat" ) );
        else
            readButtonLayout( layoutFolder, filename, section );
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
    
    private void readButtonLayout( final String layoutFolder, String filename, ConfigSection section )
    {
        // A button control. The drawable image is in PNG image format. The
        // color mask image is in BMP image format (doesn't actually get drawn).
        buttons.add( new Image( mResources, layoutFolder + "/" + filename + ".png" ) );
        masks.add( new Image( mResources, layoutFolder + "/" + filename + ".bmp" ) );
        
        // Position (percentages of the screen dimensions)
        xpercents.add( SafeMethods.toInt( section.get( "x" ), 0 ) );
        ypercents.add( SafeMethods.toInt( section.get( "y" ), 0 ) );
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