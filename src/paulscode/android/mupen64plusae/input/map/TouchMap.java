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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.input.TouchController;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.util.Image;
import paulscode.android.mupen64plusae.util.SafeMethods;
import paulscode.android.mupen64plusae.util.Utility;
import android.content.res.Resources;
import android.graphics.Point;
import android.text.TextUtils;

/**
 * A class for mapping digitizer coordinates to N64 buttons/axes.
 * 
 * @see VisibleTouchMap
 * @see TouchController
 */
public class TouchMap
{
    /** Map flag: Touch location is not mapped. */
    public static final int UNMAPPED = -1;
    
    /** Map offset: N64 pseudo-buttons. */
    public static final int OFFSET_EXTRAS = AbstractController.NUM_N64_BUTTONS;
    
    /** N64 pseudo-button: dpad-right-up. */
    public static final int DPD_RU = OFFSET_EXTRAS;
    
    /** N64 pseudo-button: dpad-right-down. */
    public static final int DPD_RD = OFFSET_EXTRAS + 1;
    
    /** N64 pseudo-button: dpad-left-down. */
    public static final int DPD_LD = OFFSET_EXTRAS + 2;
    
    /** N64 pseudo-button: dpad-left-up. */
    public static final int DPD_LU = OFFSET_EXTRAS + 3;
    
    /** Total number of N64 (pseudo-)buttons. */
    public static final int NUM_N64_PSEUDOBUTTONS = OFFSET_EXTRAS + 4;
    
    /** Folder containing the images (if provided). */
    protected String imageFolder;
    
    /** Reference screen width in pixels (if provided). */
    protected int referenceScreenWidthPixels = 0;
    
    /** Upper limit screen-width in inches to scale the buttons */
    protected float buttonsNoScaleBeyondScreenWidthInches = 0;
    
    /** Scaling factor to apply to images. */
    protected float scale = 1.0f;
    
    /** Button images. */
    protected ArrayList<Image> buttonImages;
    
    /** Button masks. */
    private final ArrayList<Image> buttonMasks;
    
    /** X-coordinates of the buttons, in percent. */
    private final ArrayList<Integer> buttonX;
    
    /** Y-coordinates of the buttons, in percent. */
    private final ArrayList<Integer> buttonY;
    
    /** Analog background image (fixed). */
    protected Image analogBackImage;
    
    /** Analog foreground image (movable). */
    protected Image analogForeImage;
    
    /** X-coordinate of the analog background, in percent. */
    private int analogBackX;
    
    /** Y-coordinate of the analog background, in percent. */
    private int analogBackY;
    
    /** Deadzone of the analog stick, in pixels. */
    private int analogDeadzone;
    
    /** Maximum displacement of the analog stick, in pixels. */
    protected int analogMaximum;
    
    /** Extra region beyond maximum in which the analog stick can be captured, in pixels. */
    private int analogPadding;
    
    /** The resources of the associated activity. */
    protected final Resources mResources;
    
    /** Map from N64 (pseudo-)button to mask color. */
    private final int[] mN64ToColor;
    
    /** The map from strings in the .ini file to N64 button indices. */
    protected static final HashMap<String, Integer> BUTTON_STRING_MAP;
    
    static
    {
        // Define the map from strings in the .ini file to N64 button indices
        // @formatter:off
        BUTTON_STRING_MAP = new HashMap<String, Integer>();
        BUTTON_STRING_MAP.put( "right",     AbstractController.DPD_R );
        BUTTON_STRING_MAP.put( "left",      AbstractController.DPD_L );
        BUTTON_STRING_MAP.put( "down",      AbstractController.DPD_D );
        BUTTON_STRING_MAP.put( "up",        AbstractController.DPD_U );
        BUTTON_STRING_MAP.put( "start",     AbstractController.START );
        BUTTON_STRING_MAP.put( "z",         AbstractController.BTN_Z );
        BUTTON_STRING_MAP.put( "b",         AbstractController.BTN_B );
        BUTTON_STRING_MAP.put( "a",         AbstractController.BTN_A );
        BUTTON_STRING_MAP.put( "cright",    AbstractController.CPD_R );
        BUTTON_STRING_MAP.put( "cleft",     AbstractController.CPD_L );
        BUTTON_STRING_MAP.put( "cdown",     AbstractController.CPD_D );
        BUTTON_STRING_MAP.put( "cup",       AbstractController.CPD_U );
        BUTTON_STRING_MAP.put( "r",         AbstractController.BTN_R );
        BUTTON_STRING_MAP.put( "l",         AbstractController.BTN_L );
        BUTTON_STRING_MAP.put( "rightup",   DPD_RU );
        BUTTON_STRING_MAP.put( "upright",   DPD_RU );
        BUTTON_STRING_MAP.put( "rightdown", DPD_RD );
        BUTTON_STRING_MAP.put( "downright", DPD_RD );
        BUTTON_STRING_MAP.put( "leftdown",  DPD_LD );
        BUTTON_STRING_MAP.put( "downleft",  DPD_LD );
        BUTTON_STRING_MAP.put( "leftup",    DPD_LU );
        BUTTON_STRING_MAP.put( "upleft",    DPD_LU );
        // @formatter:on
    }
    
    /**
     * Instantiates a new touch map.
     * 
     * @param resources The resources of the activity associated with this touch map.
     */
    public TouchMap( Resources resources )
    {
        mResources = resources;
        mN64ToColor = new int[NUM_N64_PSEUDOBUTTONS];
        buttonImages = new ArrayList<Image>();
        buttonMasks = new ArrayList<Image>();
        buttonX = new ArrayList<Integer>();
        buttonY = new ArrayList<Integer>();
    }
    
    /**
     * Clears the map data.
     */
    public void clear()
    {
        buttonImages.clear();
        buttonMasks.clear();
        buttonX.clear();
        buttonY.clear();
        analogBackImage = null;
        analogForeImage = null;
        analogBackX = analogBackY = 0;
        analogPadding = 32;
        analogDeadzone = 2;
        analogMaximum = 360;
        for( int i = 0; i < mN64ToColor.length; i++ )
            mN64ToColor[i] = -1;
    }
    
    /**
     * Recomputes the map data for a given digitizer size.
     * 
     * @param w The width of the digitizer, in pixels.
     * @param h The height of the digitizer, in pixels.
     */
    public void resize( int w, int h )
    {
        // Recompute button locations
        for( int i = 0; i < buttonImages.size(); i++ )
        {
            int cX = (int) ( w * ( (float) buttonX.get( i ) / 100f ) );
            int cY = (int) ( h * ( (float) buttonY.get( i ) / 100f ) );
            buttonImages.get( i ).setScale( scale );
            buttonImages.get( i ).fitCenter( cX, cY, w, h );
            buttonMasks.get( i ).setScale( scale );
            buttonMasks.get( i ).fitCenter( cX, cY, w, h );
        }
        
        // Recompute analog background location
        if( analogBackImage != null )
        {
            int cX = (int) ( w * ( analogBackX / 100f ) );
            int cY = (int) ( h * ( analogBackY / 100f ) );
            analogBackImage.setScale(  scale );
            analogBackImage.fitCenter( cX, cY, w, h );
        }
    }
    
    /**
     * Gets the N64 button mapped to a given touch location.
     * 
     * @param xLocation The x-coordinate of the touch, in pixels.
     * @param yLocation The y-coordinate of the touch, in pixels.
     * 
     * @return The N64 button the location is mapped to, or UNMAPPED.
     * 
     * @see TouchMap#UNMAPPED
     */
    public int getButtonPress( int xLocation, int yLocation )
    {
        // Search through every button mask to see if the corresponding button was touched
        for( Image mask : buttonMasks )
        {
            if( mask != null )
            {
                int left = mask.x;
                int right = left + (int) ( mask.width * mask.scale );
                int bottom = mask.y;
                int top = bottom + (int) ( mask.height * mask.scale );
                
                // See if the touch falls in the vicinity of the button (conservative test)
                if( xLocation >= left && xLocation < right && yLocation >= bottom
                        && yLocation < top )
                {
                    // Get the mask color at this location
                    int c = mask.image.getPixel( (int) ( ( xLocation - mask.x ) / scale ), (int) ( ( yLocation - mask.y ) / scale ) );
                    
                    // Ignore the alpha component if any
                    int rgb = c & 0x00ffffff;
                    
                    // Ignore black and get the N64 button associated with this color
                    if( rgb > 0 )
                        return getButtonFromColor( rgb );
                }
            }
        }
        return UNMAPPED;
    }
    
    /**
     * Gets the N64 button mapped to a given mask color.
     * 
     * @param color The mask color.
     * 
     * @return The N64 button the color is mapped to, or UNMAPPED.
     */
    private int getButtonFromColor( int color )
    {
        // TODO: Android is not precise: the color is different than it should be!
        // Find the closest match among the N64 buttons
        int closestMatch = UNMAPPED;
        int matchDif = Integer.MAX_VALUE;
        for( int i = 0; i < mN64ToColor.length; i++ )
        {
            int dif = Math.abs( mN64ToColor[i] - color );
            if( dif < matchDif )
            {
                closestMatch = i;
                matchDif = dif;
            }
        }
        return closestMatch;
    }
    
    /**
     * Gets the N64 analog stick displacement.
     * 
     * @param xLocation The x-coordinate of the touch, in pixels.
     * @param yLocation The y-coordinate of the touch, in pixels.
     * 
     * @return The analog displacement, in pixels.
     */
    public Point getAnalogDisplacement( int xLocation, int yLocation )
    {
        if( analogBackImage == null )
            return new Point( 0, 0 );
        
        // Distance from center along x-axis
        int dX = xLocation - ( analogBackImage.x + (int) ( analogBackImage.hWidth * scale ) );
        
        // Distance from center along y-axis
        int dY = yLocation - ( analogBackImage.y + (int) ( analogBackImage.hHeight * scale ) );
        
        return new Point( dX, dY );
    }
    
    /**
     * Gets the N64 analog stick displacement, constrained to an octagon.
     * 
     * @param dX The x-displacement of the stick, in pixels.
     * @param dY The y-displacement of the stick, in pixels.
     * 
     * @return The constrained analog displacement, in pixels.
     */
    public Point getConstrainedDisplacement( int dX, int dY )
    {
        return Utility.constrainToOctagon( dX, dY, (int) ( analogMaximum * scale ) );
    }
    
    /**
     * Gets the analog strength, accounting for deadzone and motion limits.
     * 
     * @param displacement The Pythagorean displacement of the analog stick, in pixels.
     * 
     * @return The analog strength, between 0 and 1, inclusive.
     */
    public float getAnalogStrength( float displacement )
    {
        displacement /= scale;
        float p = ( displacement - analogDeadzone ) / ( analogMaximum - analogDeadzone );
        return Utility.clamp( p, 0.0f, 1.0f );
    }
    
    /**
     * Checks if a touch is within capture range of the analog stick.
     * 
     * @param displacement The displacement of the touch with respect to analog center, in pixels.
     * 
     * @return True, if the touch is in capture range of the stick.
     */
    public boolean isInCaptureRange( float displacement )
    {
        displacement /= scale;
        return ( displacement >= analogDeadzone ) && ( displacement < analogMaximum + analogPadding );
    }
    
    /**
     * Loads all touch map data from the filesystem.
     * 
     * @param directory The directory containing the .ini and asset files.
     */
    public void load( String directory )
    {
        // Clear any old assets and map data
        clear();
        
        // Load the configuration file (pad.ini)
        ConfigFile pad_ini = new ConfigFile( directory + "/pad.ini" );
        
        // If a style wasn't chosen, check if an image folder is listed in pad.ini
        if( TextUtils.isEmpty( imageFolder ) )
            imageFolder = pad_ini.get( "INFO", "images" );
        // If no image folder was provided, use the layout directory
        if( TextUtils.isEmpty( imageFolder ) )
            imageFolder = directory;
        else
            imageFolder = directory + "/" + imageFolder;
        
        referenceScreenWidthPixels = SafeMethods.toInt( pad_ini.get( "INFO", "referenceScreenWidthPixels" ), 0 );
        buttonsNoScaleBeyondScreenWidthInches = SafeMethods.toFloat( pad_ini.get( "INFO", "buttonsNoScaleBeyondScreenWidthInches" ), 0 );
        
        // Look up the mask colors
        loadMaskColors( pad_ini );
        
        // Loop through all the configuration sections
        loadAllAssets( pad_ini, directory );
        
        // Free the data that was loaded from the config file:
        pad_ini.clear();
    }
    
    /**
     * Loads the mask colors from a configuration file.
     * 
     * @param pad_ini The configuration file.
     */
    private void loadMaskColors( ConfigFile pad_ini )
    {
        ConfigSection section = pad_ini.get( "MASK_COLOR" );
        if( section != null )
        {
            // Loop through the key-value pairs
            for( String key : section.keySet() )
            {
                // Assign the map colors to the appropriate N64 button
                String val = section.get( key );
                Integer index = BUTTON_STRING_MAP.get( key.toLowerCase( Locale.US ) );
                if( index != null )
                    mN64ToColor[index] = SafeMethods.toInt( val, -1 );
            }
        }
    }
    
    /**
     * Loads all assets and properties specified in a configuration file.
     * 
     * @param pad_ini   The configuration file.
     * @param directory The directory containing the assets.
     */
    protected void loadAllAssets( ConfigFile pad_ini, String directory )
    {
        for( String filename : pad_ini.keySet() )
        {
            // Make sure it's a filename
            if( isFilename( filename ) )
            {
                ConfigSection section = pad_ini.get( filename );
                if( section != null )
                {
                    // Get the type of asset
                    String info = section.get( "info" );
                    if( info != null )
                    {
                        // Let's not make this part case-sensitive
                        loadAssetSection( directory, filename, section, info.toLowerCase( Locale.US ) );
                    }
                }
            }
        }
    }
    
    /**
     * Loads assets and properties for a given configuration section. This method can be overridden
     * in subclasses to handle new asset types.
     * 
     * @param directory The directory containing the assets.
     * @param filename  The name of the asset, without file extension.
     * @param section   The configuration section containing the properties.
     * @param info      The meta-information provided inside the configuration section.
     */
    protected void loadAssetSection( final String directory, String filename,
            ConfigSection section, String info )
    {
        if( info.contains( "analog" ) )
            loadAnalog( imageFolder, filename, section, info.contains( "hat" ) );
        else if( filename.contains( "BUTTON" ) )
            loadButton( imageFolder, filename, section );
    }
    
    /**
     * Loads analog assets and properties from the filesystem.
     * 
     * @param directory      The directory containing the analog assets.
     * @param filename       The filename of the analog assets, without extension.
     * @param section        The configuration section containing the analog properties.
     * @param loadForeground True to load the analog foreground in addition to the background.
     */
    private void loadAnalog( final String directory, String filename, ConfigSection section,
            boolean loadForeground )
    {
        // The images (used by touchscreens) are in PNG image format.
        analogBackImage = new Image( mResources, directory + "/" + filename + ".png" );
        if( loadForeground )
        {
            // There's a "stick" image.. same name, with "_2" appended
            analogForeImage = new Image( mResources, directory + "/" + filename + "_2.png" );
        }
        
        // Load the image in BMP format if not available in PNG format (applies to touchpads).
        if( analogBackImage.width == 0 && analogBackImage.height == 0 )
            analogBackImage = new Image( mResources, directory + "/" + filename + ".bmp" );
        
        // Position (percentages of the digitizer dimensions)
        analogBackX = SafeMethods.toInt( section.get( "x" ), 0 );
        analogBackY = SafeMethods.toInt( section.get( "y" ), 0 );
        
        // Sensitivity (percentages of the radius, i.e. half the image width)
        analogDeadzone = (int) ( analogBackImage.hWidth * ( SafeMethods.toFloat(
                section.get( "min" ), 1 ) / 100.0f ) );
        analogMaximum = (int) ( analogBackImage.hWidth * ( SafeMethods.toFloat(
                section.get( "max" ), 55 ) / 100.0f ) );
        analogPadding = (int) ( analogBackImage.hWidth * ( SafeMethods.toFloat(
                section.get( "buff" ), 55 ) / 100.0f ) );
    }
    
    /**
     * Loads button assets and properties from the filesystem.
     * 
     * @param directory The directory containing the button assets.
     * @param filename  The filename of the button assets, without extension.
     * @param section   The configuration section containing the button properties.
     */
    private void loadButton( final String directory, String filename, ConfigSection section )
    {
        // The drawable image is in PNG image format. The color mask image is in BMP image format
        // (doesn't actually get drawn).
        buttonImages.add( new Image( mResources, directory + "/" + filename + ".png" ) );
        buttonMasks.add( new Image( mResources, directory + "/" + filename + ".bmp" ) );
        
        // Position (percentages of the digitizer dimensions)
        buttonX.add( SafeMethods.toInt( section.get( "x" ), 0 ) );
        buttonY.add( SafeMethods.toInt( section.get( "y" ), 0 ) );
    }
    
    /**
     * Checks if a configuration parameter is a filename.
     * 
     * @param parameter The configuration parameter.
     * 
     * @return True, if it is a filename.
     */
    private boolean isFilename( String parameter )
    {
        return parameter != null && parameter.length() > 0 && !parameter.equals( "INFO" )
                && !parameter.equals( "MASK_COLOR" ) && !parameter.equals( "[<sectionless!>]" );
    }
}
