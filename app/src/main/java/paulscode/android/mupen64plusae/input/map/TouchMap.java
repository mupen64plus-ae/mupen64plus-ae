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

import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.HashMap;

import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.input.TouchController;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.profile.Profile;
import paulscode.android.mupen64plusae.util.Image;
import paulscode.android.mupen64plusae.util.SafeMethods;
import paulscode.android.mupen64plusae.util.Utility;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

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
    private static final int OFFSET_EXTRAS = AbstractController.NUM_N64_BUTTONS;
    
    /** N64 pseudo-button: dpad-right-up. */
    public static final int DPD_RU = OFFSET_EXTRAS;
    
    /** N64 pseudo-button: dpad-right-down. */
    public static final int DPD_RD = OFFSET_EXTRAS + 1;
    
    /** N64 pseudo-button: dpad-left-down. */
    public static final int DPD_LD = OFFSET_EXTRAS + 2;
    
    /** N64 pseudo-button: dpad-left-up. */
    public static final int DPD_LU = OFFSET_EXTRAS + 3;
    
    /** N64 pseudo-button: enable/disable the accelerometer sensor. */
    public static final int TOGGLE_SENSOR = OFFSET_EXTRAS + 4;
    
    /** Total number of N64 (pseudo-)buttons. */
    static final int NUM_N64_PSEUDOBUTTONS = OFFSET_EXTRAS + 5;
    
    /** Folder containing the images. */
    String skinFolder;
    
    /** Scaling factor to apply to images. */
    protected float scale = 1.0f;
    
    /** Button scaling factor. */
    ArrayList<Float> buttonScaling;
    
    /** Button images. */
    ArrayList<Image> buttonImages;
    
    /** Button masks. */
    private final ArrayList<Image> buttonMasks;
    
    /** X-coordinates of the buttons, in percent. */
    private final ArrayList<Integer> buttonX;
    
    /** Y-coordinates of the buttons, in percent. */
    private final ArrayList<Integer> buttonY;
    
    /** names of the buttons. */
    final ArrayList<String> buttonNames;
    
    /** true if analog is enabled */
    private boolean isAnalogEnabled = true;
    
    /** Analog background scaling. */
    float analogBackScaling;
    
    /** Analog background image (fixed). */
    Image analogBackImage;
    
    /** Analog foreground image (movable). */
    Image analogForeImage;
    
    /** X-coordinate of the analog background, in percent. */
    private int analogBackX;
    
    /** Y-coordinate of the analog background, in percent. */
    private int analogBackY;

    /** Where is the X current origin of the analog image in pixels */
    int currentAnalogX;

    /** Where is the Y current origin of the analog image in pixels */
    int currentAnalogY;

    /** Where is the X original origin of the analog image in pixels */
    private int originalAnalogX;

    /** Where is the Y original origin of the analog image in pixels */
    private int originalAnalogY;
    
    /** Deadzone of the analog stick, in pixels. */
    private int analogDeadzone;
    
    /** Maximum displacement of the analog stick, in pixels. */
    int analogMaximum;
    
    /** Extra region beyond maximum in which the analog stick can be captured, in pixels. */
    private int analogPadding;
    
    /** The resources of the associated activity. */
    final Resources mResources;
    
    /** Map from N64 (pseudo-)button to mask color. */
    private final int[] mN64ToColor;
    
    /** The map from strings in the skin.ini file to N64 button indices. */
    static final HashMap<String, Integer> MASK_KEYS = new HashMap<>();

    /** The map from N64 button indices to asset name prefixes in the skin folder. */
    public static SparseArray<String> ASSET_NAMES = new SparseArray<>();
    
    /** The error in RGB (256x256x256) space that we tolerate when matching mask colors. */
    private static final int MATCH_TOLERANCE = 10;
    
    /** True if A/B buttons are split */
    boolean mSplitAB;
    
    static
    {
        // Define the map from skin.ini keys to N64 button indices
        // @formatter:off
        MASK_KEYS.put( "Dr",  AbstractController.DPD_R );
        MASK_KEYS.put( "Dl",  AbstractController.DPD_L );
        MASK_KEYS.put( "Dd",  AbstractController.DPD_D );
        MASK_KEYS.put( "Du",  AbstractController.DPD_U );
        MASK_KEYS.put( "S",   AbstractController.START );
        MASK_KEYS.put( "Z",   AbstractController.BTN_Z );
        MASK_KEYS.put( "B",   AbstractController.BTN_B );
        MASK_KEYS.put( "A",   AbstractController.BTN_A );
        MASK_KEYS.put( "Cr",  AbstractController.CPD_R );
        MASK_KEYS.put( "Cl",  AbstractController.CPD_L );
        MASK_KEYS.put( "Cd",  AbstractController.CPD_D );
        MASK_KEYS.put( "Cu",  AbstractController.CPD_U );
        MASK_KEYS.put( "R",   AbstractController.BTN_R );
        MASK_KEYS.put( "L",   AbstractController.BTN_L );
        MASK_KEYS.put( "Dru", DPD_RU );
        MASK_KEYS.put( "Drd", DPD_RD );
        MASK_KEYS.put( "Dld", DPD_LD );
        MASK_KEYS.put( "Dlu", DPD_LU );
        MASK_KEYS.put( "Sen", TOGGLE_SENSOR );
        // @formatter:on
        
        // Define the map from N64 button indices to profile key prefixes
        ASSET_NAMES.put( AbstractController.DPD_R, "dpad" );
        ASSET_NAMES.put( AbstractController.DPD_L, "dpad" );
        ASSET_NAMES.put( AbstractController.DPD_D, "dpad" );
        ASSET_NAMES.put( AbstractController.DPD_U, "dpad" );
        ASSET_NAMES.put( AbstractController.START, "buttonS" );
        ASSET_NAMES.put( AbstractController.BTN_Z, "buttonZ" );
        ASSET_NAMES.put( AbstractController.BTN_B, "" );
        ASSET_NAMES.put( AbstractController.BTN_A, "" );
        ASSET_NAMES.put( AbstractController.CPD_R, "groupC" );
        ASSET_NAMES.put( AbstractController.CPD_L, "groupC" );
        ASSET_NAMES.put( AbstractController.CPD_D, "groupC" );
        ASSET_NAMES.put( AbstractController.CPD_U, "groupC" );
        ASSET_NAMES.put( AbstractController.BTN_R, "buttonR" );
        ASSET_NAMES.put( AbstractController.BTN_L, "buttonL" );
        ASSET_NAMES.put( DPD_LU, "dpad" );
        ASSET_NAMES.put( DPD_LD, "dpad" );
        ASSET_NAMES.put( DPD_RD, "dpad" );
        ASSET_NAMES.put( DPD_RU, "dpad" );
        ASSET_NAMES.put( TOGGLE_SENSOR, "buttonSen" );
    }
    
    /**
     * Instantiates a new touch map.
     * 
     * @param resources The resources of the activity associated with this touch map.
     */
    TouchMap( Resources resources )
    {
        mResources = resources;
        mN64ToColor = new int[NUM_N64_PSEUDOBUTTONS];
        buttonImages = new ArrayList<>();
        buttonMasks = new ArrayList<>();
        buttonX = new ArrayList<>();
        buttonY = new ArrayList<>();
        buttonNames = new ArrayList<>();
        buttonScaling = new ArrayList<>();
    }
    
    /**
     * Clears the map data.
     */
    public void clear()
    {
        buttonScaling.clear();
        buttonImages.clear();
        buttonMasks.clear();
        buttonX.clear();
        buttonY.clear();
        buttonNames.clear();
        analogBackScaling = 0;
        analogBackImage = null;
        analogForeImage = null;
        analogBackX = analogBackY = 0;
        currentAnalogX = 0;
        currentAnalogY = 0;
        originalAnalogX = 0;
        originalAnalogY = 0;
        analogPadding = 32;
        analogDeadzone = 2;
        analogMaximum = 360;

        //Defaults in case skin.ini is not present
        mN64ToColor[AbstractController.DPD_R] = 0x00FFF0;
        mN64ToColor[AbstractController.DPD_L] = 0xD62D4D;
        mN64ToColor[AbstractController.DPD_D] = 0xCC00FF;
        mN64ToColor[AbstractController.DPD_U] = 0xFF0000;
        mN64ToColor[AbstractController.START] = 0xB45D5D;
        mN64ToColor[AbstractController.BTN_Z] = 0x42A6EC;
        mN64ToColor[AbstractController.BTN_B] = 0x4B4B4B;
        mN64ToColor[AbstractController.BTN_A] = 0x007F46;
        mN64ToColor[AbstractController.CPD_R] = 0xFF635C;
        mN64ToColor[AbstractController.CPD_L] = 0x5A6B1F;
        mN64ToColor[AbstractController.CPD_D] = 0x84A1D5;
        mN64ToColor[AbstractController.CPD_U] = 0x00E0CA;
        mN64ToColor[AbstractController.BTN_R] = 0x6B1F49;
        mN64ToColor[AbstractController.BTN_L] = 0xFFB400;
        mN64ToColor[DPD_RU] = 0xFFFC00;
        mN64ToColor[DPD_RD] = 0xFF9600;
        mN64ToColor[DPD_LD] = 0xA000FF;
        mN64ToColor[DPD_LU] = 0x00FF5A;
    }

    /**
     * Adjusts Y so that in portrait mode, the screen starts in the bottom half of the screen
     * @param y Percentage before adjustment
     * @return New percentage at the bottom of the screen
     */
    int getAdjustedYPos(int y)
    {
        if(mResources.getConfiguration().orientation == ORIENTATION_PORTRAIT)
        {
            return 50 + (int)((y/100.0) * 50.0f);
        }
        else
        {
            return y;
        }
    }

    /**
     * Adjusts Y so that in portrait mode, the FPS shows up in the top half of the screen
     * @param y Percentage before adjustment
     * @return New percentage at the bottom of the screen
     */
    int getAdjustedFpsYPos(int y)
    {
        if(mResources.getConfiguration().orientation == ORIENTATION_PORTRAIT)
        {
            return (int)((y/100.0) * 45.0f);
        }
        else
        {
            return y;
        }
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
            buttonImages.get( i ).setScale( ( buttonScaling.get( i ) * scale ) );
            buttonImages.get( i ).fitPercent( buttonX.get( i ), getAdjustedYPos(buttonY.get( i )), w, h );
            buttonMasks.get( i ).setScale( ( buttonScaling.get( i ) * scale ) );
            buttonMasks.get( i ).fitPercent( buttonX.get( i ), getAdjustedYPos(buttonY.get( i )), w, h );
        }
        
        // Recompute analog background location
        if( analogBackImage != null )
        {
            analogBackImage.setScale( ( analogBackScaling * scale ) );
            analogBackImage.fitPercent( analogBackX, getAdjustedYPos(analogBackY), w, h );

            currentAnalogX = analogBackImage.x;
            currentAnalogY = analogBackImage.y;
            originalAnalogX = analogBackImage.x;
            originalAnalogY = analogBackImage.y;
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
        for( int i = 0; i < buttonMasks.size(); i++ )
        {
            if( buttonMasks.get( i ) != null )
            {
                int left = buttonMasks.get( i ).x;
                int right = left + (int) ( buttonMasks.get( i ).width * buttonMasks.get( i ).scale );
                int bottom = buttonMasks.get( i ).y;
                int top = bottom + (int) ( buttonMasks.get( i ).height * buttonMasks.get( i ).scale );
                
                // See if the touch falls in the vicinity of the button (conservative test)
                if( xLocation >= left && xLocation < right && yLocation >= bottom
                        && yLocation < top )
                {
                    // Get the mask color at this location
                    int c = buttonMasks.get( i ).image.getPixel( (int) ( ( xLocation - buttonMasks.get( i ).x ) / 
                            ( buttonScaling.get( i ) * scale ) ), (int) ( ( yLocation - buttonMasks.get( i ).y ) / ( buttonScaling.get( i ) * scale ) ) );
                    
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
     * Gets the frame for the N64 button with a given asset name
     * 
     * @param assetName The asset name for the button
     * 
     * @return The frame for the N64 button with the given asset name
     * 
     */
    public Rect getButtonFrame( String assetName )
    {
        for( int i = 0; i < buttonNames.size(); i++ )
        {
            if ( buttonNames.get( i ).equals( assetName ) )
                return new Rect( buttonMasks.get( i ).drawRect );
        }
        return new Rect(0, 0, 0, 0);
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
        // Find the N64 button whose mask matches the given color. Because we scale the mask images,
        // the mask boundaries can get softened. Therefore we tolerate a bit of error in the match.
        int closestMatch = UNMAPPED;
        int matchDif = MATCH_TOLERANCE * MATCH_TOLERANCE;
        
        // Get the RGB values of the given color
        int r = ( color & 0xFF0000 ) >> 16;
        int g = ( color & 0x00FF00 ) >> 8;
        int b = ( color & 0x0000FF );
        
        // Find the mask color with the smallest squared error
        for( int i = 0; i < mN64ToColor.length; i++ )
        {
            int color2 = mN64ToColor[i];
            
            // Compute squared error in RGB space
            int difR = r - ( ( color2 & 0xFF0000 ) >> 16 );
            int difG = g - ( ( color2 & 0x00FF00 ) >> 8 );
            int difB = b - ( ( color2 & 0x0000FF ) );
            int dif = difR * difR + difG * difG + difB * difB;
            
            if( dif < matchDif )
            {
                closestMatch = i;
                matchDif = dif;
            }
        }
        return closestMatch;
    }

    /**
     * Updates the position of the analog stick
     *
     * @param xLocation The x-coordinate of the touch, in pixels.
     * @param yLocation The y-coordinate of the touch, in pixels.
     *
     */
    public void updateAnalogPosition( int xLocation, int yLocation )
    {
        Point displacement = getAnalogDisplacementOriginal(xLocation, yLocation);
        currentAnalogX = originalAnalogX + displacement.x;
        currentAnalogY = originalAnalogY + displacement.y;
    }

    /**
     * Resets position of analog stick
     */
    public void resetAnalogPosition()
    {
        currentAnalogX = originalAnalogX;
        currentAnalogY = originalAnalogY;
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
        if( analogBackImage == null || !isAnalogEnabled )
            return new Point( 0, 0 );
        
        // Distance from center along x-axis
        int dX = xLocation - ( currentAnalogX + (int) ( analogBackImage.hWidth * ( analogBackScaling * scale ) ) );
        
        // Distance from center along y-axis
        int dY = yLocation - ( currentAnalogY + (int) ( analogBackImage.hHeight * ( analogBackScaling * scale ) ) );
        
        return new Point( dX, dY );
    }

    /**
     * Gets the displacement of the given position from the starting analog location
     *
     * @param xLocation The x-coordinate of the touch, in pixels.
     * @param yLocation The y-coordinate of the touch, in pixels.
     *
     * @return The analog displacement, in pixels.
     */
    public Point getAnalogDisplacementOriginal( int xLocation, int yLocation )
    {
        if( analogBackImage == null || !isAnalogEnabled )
            return new Point( 0, 0 );

        // Distance from center along x-axis
        int dX = xLocation - ( originalAnalogX + (int) ( analogBackImage.hWidth * ( analogBackScaling * scale ) ) );

        // Distance from center along y-axis
        int dY = yLocation - ( originalAnalogY + (int) ( analogBackImage.hHeight * ( analogBackScaling * scale ) ) );

        return new Point( dX, dY );
    }
    
    /**
     * Gets the N64 analog stick's frame.
     * 
     * @return The analog stick's frame.
     */
    public Rect getAnalogFrame()
    {
        if( analogBackImage != null )
            return new Rect( analogBackImage.drawRect );
        return new Rect(0, 0, 0, 0);
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
        final float dC = (int) ( analogMaximum * ( analogBackScaling * scale ) );
        final float dA = (float) (dC * Math.sqrt( 0.5f ));
        final float signX = (dX < 0) ? -1 : 1;
        final float signY = (dY < 0) ? -1 : 1;
        
        Point crossPt = new Point();
        crossPt.x = dX;
        crossPt.y = dY;
        
        if( ( signX * dX ) > ( signY * dY ) )
            segsCross( 0, 0, dX, dY, signX * dC, 0, signX * dA, signY * dA, crossPt );
        else
            segsCross( 0, 0, dX, dY, 0, signY * dC, signX * dA, signY * dA, crossPt );
        
        return crossPt;
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
        displacement /= ( analogBackScaling * scale );
        float p = ( displacement - analogDeadzone ) / ( analogMaximum - analogDeadzone );
        return Utility.clamp( p, 0.0f, 1.0f );
    }
    
    /**
     * Checks if a touch is within capture range of the analog stick.
     * 
     * @param point Point location
     * 
     * @return True, if the touch is in capture range of the stick.
     */
    public boolean isInCaptureRange( Point point )
    {
        // Compute the pythagorean displacement of the stick
        int dX = point.x;
        int dY = point.y;
        float displacement = (float) Math.sqrt( ( dX * dX ) + ( dY * dY ) );

        displacement /= ( analogBackScaling * scale );
        return ( displacement >= analogDeadzone ) && ( displacement < analogMaximum + analogPadding );
    }
    
    /**
     * Loads all touch map data from the filesystem.
     * 
     * @param skinDir The directory containing the skin.ini and image files.
     * @param profile  The name of the layout profile.
     * @param animated True to load the analog assets in two parts for animation.
     */
    public void load( String skinDir, Profile profile, boolean animated )
    {
        // Clear any old assets and map data
        clear();
        
        // Load the configuration files
        skinFolder = skinDir;
        ConfigFile skin_ini = new ConfigFile( skinFolder + "/skin.ini" );
        
        mSplitAB = SafeMethods.toBoolean( skin_ini.get( "INFO", "split-AB" ), false);
        
        // Look up the mask colors
        loadMaskColors( skin_ini );
        
        // Loop through all the configuration sections
        loadAllAssets( profile, animated );
        
        
        if( mSplitAB )
        {
            ASSET_NAMES.setValueAt( AbstractController.BTN_B, "buttonB" );
            ASSET_NAMES.setValueAt( AbstractController.BTN_A, "buttonA" );
        }
        else
        {
            ASSET_NAMES.setValueAt( AbstractController.BTN_B, "groupAB" );
            ASSET_NAMES.setValueAt( AbstractController.BTN_A, "groupAB" );
        }
    }
    
    /**
     * Update the position of a button.
     * 
     * @param profile  The name of the layout profile.
     * @param name     The name of the button.
     */
    void updateButton( Profile profile, String name, int w, int h )
    {
        int x = profile.getInt( name + "-x", 0 );
        int y = profile.getInt( name + "-y", 95 );
        
        if( x >= 0 && y >= 0 )
        {
            if( name.equals( "analog" ) )
            {
                analogBackX = x;
                analogBackY = y;
                analogBackImage.fitPercent( analogBackX, getAdjustedYPos(analogBackY), w, h );

                currentAnalogX = analogBackImage.x;
                currentAnalogY = analogBackImage.y;

                originalAnalogX = analogBackImage.x;
                originalAnalogY = analogBackImage.y;
                
                if( analogForeImage != null )
                {
                    int cX = currentAnalogX + (int) ( analogBackImage.hWidth * ( analogBackScaling * scale ) );
                    int cY = currentAnalogY + (int) ( analogBackImage.hHeight * ( analogBackScaling * scale ) );
                    analogForeImage.fitCenter( cX, cY, currentAnalogX, currentAnalogY,
                            (int) ( analogBackImage.width * ( analogBackScaling * scale ) ), (int) ( analogBackImage.height * ( analogBackScaling * scale ) ) );
                }
            }
            else
            {
                for( int i = 0; i < buttonNames.size(); i++ )
                {
                    if ( buttonNames.get( i ).equals( name ) )
                    {
                        buttonX.set( i, x );
                        buttonY.set( i, y );
                        buttonImages.get( i ).fitPercent( buttonX.get( i ), getAdjustedYPos(buttonY.get( i )), w, h );
                        buttonMasks.get( i ).fitPercent( buttonX.get( i ), getAdjustedYPos(buttonY.get( i )), w, h );
                    }
                }
            }
        }
    }
    
    public void setAnalogEnabled(boolean enabled) {
        isAnalogEnabled = enabled;
    }

    /**
     * Loads the mask colors from a configuration file.
     * 
     * @param skin_ini The configuration file containing mask info.
     */
    private void loadMaskColors( ConfigFile skin_ini )
    {
        ConfigSection section = skin_ini.get( "MASK_COLOR" );
        if( section != null )
        {
            // Loop through the key-value pairs
            for( String key : section.keySet() )
            {
                // Assign the map colors to the appropriate N64 button
                String val = section.get( key );
                Integer index = MASK_KEYS.get( key );
                if( index != null )
                {
                    try
                    {
                        mN64ToColor[index] = Integer.parseInt( val, 16 );
                    }
                    catch( NumberFormatException ex )
                    {
                        Log.w( "TouchMap", "Invalid mask color '" + val + "' in " + skinFolder + "/skin.ini" );
                    }
                }
            }
        }
    }
    
    /**
     * Loads all assets and properties specified in a profile.
     * 
     * @param profile  The touchscreen profile.
     * @param animated True to load the analog assets in two parts for animation.
     */
    protected void loadAllAssets( Profile profile, boolean animated )
    {
        if( profile != null )
        {
            loadAnalog( profile, animated );
            loadButton( profile, "dpad" );
            if( mSplitAB )
            {
                loadButton( profile, "buttonA" );
                loadButton( profile, "buttonB" );
            }
            else
                loadButton( profile, "groupAB" );
            loadButton( profile, "groupC" );
            loadButton( profile, "buttonL" );
            loadButton( profile, "buttonR" );
            loadButton( profile, "buttonZ" );
            loadButton( profile, "buttonS" );
            loadButton( profile, "buttonSen" );
        }
    }
    
    /**
     * Loads analog assets and properties from the filesystem.
     * 
     * @param profile  The touchscreen profile containing the analog properties.
     * @param animated True to load the assets in two parts for animation.
     */
    private void loadAnalog( Profile profile, boolean animated )
    {
        int x = profile.getInt( "analog-x", -1 );
        int y = profile.getInt( "analog-y", -1 );
        int scaling = profile.getInt("analog-scale", 100);
        
        if( x >= 0 && y >= 0 )
        {
            // Position (percentages of the digitizer dimensions)
            analogBackX = x;
            analogBackY = y;
            
            // The images (used by touchscreens) are in PNG image format.
            if( animated )
            {
                 analogBackImage = new Image( mResources, skinFolder + "/analog-back.png" );
                 analogForeImage = new Image( mResources, skinFolder + "/analog-fore.png" );
            }
            else
            {
                analogBackImage = new Image( mResources, skinFolder + "/analog.png" );
            }
            
            // Sensitivity (percentages of the radius, i.e. half the image width)
            analogDeadzone = (int) ( analogBackImage.hWidth * ( profile.getFloat( "analog-min", 1 ) / 100.0f ) );
            analogMaximum = (int) ( analogBackImage.hWidth * ( profile.getFloat( "analog-max", 55 ) / 100.0f ) );
            analogPadding = (int) ( analogBackImage.hWidth * ( profile.getFloat( "analog-buff", 55 ) / 100.0f ) );
            analogBackScaling = (float) scaling / 100.f;
        }
    }
    
    /**
     * Loads button assets and properties from the filesystem.
     * 
     * @param profile The touchscreen profile containing the button properties.
     * @param name    The name of the button/group to load.
     */
    private void loadButton( Profile profile, String name )
    {
        int x = profile.getInt( name + "-x", -1 );
        int y = profile.getInt( name + "-y", -1 );
        int scaling = profile.getInt( name + "-scale", 100);
        
        if( x >= 0 && y >= 0 )
        {
            // Position (percentages of the digitizer dimensions)
            buttonX.add( x );
            buttonY.add( y );
            buttonNames.add( name );
            
            // Load the displayed and mask images
            buttonImages.add( new Image( mResources, skinFolder + "/" + name + ".png" ) );
            buttonMasks.add( new Image( mResources, skinFolder + "/" + name + "-mask.png" ) );
            buttonScaling.add( (float) scaling / 100.f );
        }
    }

    /**
     * Determines if the two specified line segments intersect with each other, and calculates where
     * the intersection occurs if they do.
     * 
     * @param seg1pt1_x X-coordinate for the first end of the first line segment.
     * @param seg1pt1_y Y-coordinate for the first end of the first line segment.
     * @param seg1pt2_x X-coordinate for the second end of the first line segment.
     * @param seg1pt2_y Y-coordinate for the second end of the first line segment.
     * @param seg2pt1_x X-coordinate for the first end of the second line segment.
     * @param seg2pt1_y Y-coordinate for the first end of the second line segment.
     * @param seg2pt2_x X-coordinate for the second end of the second line segment.
     * @param seg2pt2_y Y-coordinate for the second end of the second line segment.
     * @param crossPt Changed to the point of intersection if there is one, otherwise unchanged.
     * 
     * @return True if the two line segments intersect.
     */
    @SuppressWarnings({"SameParameterValue", "UnusedReturnValue"})
    private static boolean segsCross(float seg1pt1_x, float seg1pt1_y, float seg1pt2_x,
                                     float seg1pt2_y, float seg2pt1_x, float seg2pt1_y, float seg2pt2_x, float seg2pt2_y,
                                     Point crossPt )
    {
        float vec1_x = seg1pt2_x - seg1pt1_x;
        float vec1_y = seg1pt2_y - seg1pt1_y;
        
        float vec2_x = seg2pt2_x - seg2pt1_x;
        float vec2_y = seg2pt2_y - seg2pt1_y;
        
        float div = ( -vec2_x * vec1_y + vec1_x * vec2_y );
        
        // Segments don't cross
        if( div == 0 )
            return false;
        
        float s = ( -vec1_y * ( seg1pt1_x - seg2pt1_x ) + vec1_x * ( seg1pt1_y - seg2pt1_y ) ) / div;
        float t = ( vec2_x  * ( seg1pt1_y - seg2pt1_y ) - vec2_y * ( seg1pt1_x - seg2pt1_x ) ) / div;
        
        if( s >= 0 && s < 1 && t >= 0 && t <= 1 )
        {
            // Segments cross, point of intersection stored in 'crossPt'
            crossPt.x = (int) ( seg1pt1_x + ( t * vec1_x ) );
            crossPt.y = (int) ( seg1pt1_y + ( t * vec1_y ) );
            return true;
        }
        
        // Segments don't cross
        return false;
    }
}
