package paulscode.android.mupen64plusae;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.util.Utility;
import paulscode.android.mupen64plusae.util.Utility.Image;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;

public class TouchscreenSkin
{
    public interface Listener
    {
        public void onAllChanged( TouchscreenSkin skin );
        
        public void onHatChanged( TouchscreenSkin skin, int x, int y );
        
        public void onFpsChanged( TouchscreenSkin skin, int fps );
    }
    
    // Pseudo-buttons, indicating simultaneous press of two d-pad buttons
    public static final int DPD_RU = AbstractController.NUM_BUTTONS;
    public static final int DPD_RD = AbstractController.NUM_BUTTONS + 1;
    public static final int DPD_LD = AbstractController.NUM_BUTTONS + 2;
    public static final int DPD_LU = AbstractController.NUM_BUTTONS + 3;
    public static final int NUM_PSEUDO_BUTTONS = AbstractController.NUM_BUTTONS + 4;
    
    // Mask colors
    public int[] maskColors = new int[TouchscreenSkin.NUM_PSEUDO_BUTTONS];
    
    // Analog stick geometry
    public Utility.Image analogImage = null;
    public float analogPadding = 32;
    public float analogDeadzone = 2;
    public float analogMaximum = 360;
    
    // Variables for drawing the analog stick
    public int analogXpercent = 0;
    public int analogYpercent = 0;
    public Utility.Image hatImage = null;
    public int hatX = -1;
    public int hatY = -1;
    
    // All button images and associated mask images
    public ArrayList<Utility.Image> buttons;
    public ArrayList<Utility.Image> masks;
    public ArrayList<Integer> xpercents;
    public ArrayList<Integer> ypercents;
    
    public Utility.Image fpsImage = null;
    public int fpsXpercent = 0;
    public int fpsYpercent = 0;
    public int fpsNumXpercent = 50;
    public int fpsNumYpercent = 50;
    public int fpsRate = 15;
    public String fpsFont = "Mupen64Plus-AE-Contrast-Blue";
    public Utility.Image[] numberImages = new Utility.Image[10];
    private int fpsValue = 0;
    private Utility.Image[] fpsDigits = new Utility.Image[4];
    
    public Resources mResources = null;
    public boolean initialized = false;
    
    public void clear()
    {
        // Clear everything out to be re-populated with the new settings
        buttons = new ArrayList<Utility.Image>();
        masks = new ArrayList<Utility.Image>();
        xpercents = new ArrayList<Integer>();
        ypercents = new ArrayList<Integer>();
        analogImage = null;
        analogXpercent = analogYpercent = 0;
        hatImage = null;
        hatX = hatY = -1;
        fpsImage = null;
        fpsXpercent = fpsYpercent = 0;
        fpsNumXpercent = fpsNumYpercent = 50;
        fpsRate = 15;
        fpsValue = 0;
        fpsFont = "Mupen64Plus-AE-Contrast-Blue";
        for( int i = 0; i < maskColors.length; i++ )
            maskColors[i] = -1;
    }
    
    public void setResources( Resources resources )
    {
        mResources = resources;
        for( int i = 0; i < 10; i++ )
            numberImages[i] = new Utility.Image( resources, Globals.paths.fontsDir + fpsFont + "/"
                    + i + ".png" );
    }
    
    public void resize( int canvasW, int canvasH )
    {
        // Position the buttons
        for( int i = 0; i < xpercents.size(); i++ )
        {
            buttons.get( i ).fitCenter(
                    (int) ( (float) canvasW * ( (float) xpercents.get( i ) / 100.0f ) ),
                    (int) ( (float) canvasH * ( (float) ypercents.get( i ) / 100.0f ) ), canvasW,
                    canvasH );
            masks.get( i ).fitCenter(
                    (int) ( (float) canvasW * ( (float) xpercents.get( i ) / 100.0f ) ),
                    (int) ( (float) canvasH * ( (float) ypercents.get( i ) / 100.0f ) ), canvasW,
                    canvasH );
        }
        
        // Position the analog control
        if( analogImage != null )
        {
            analogImage.fitCenter( (int) ( (float) canvasW * ( (float) analogXpercent / 100.0f ) ),
                    (int) ( (float) canvasH * ( (float) analogYpercent / 100.0f ) ), canvasW,
                    canvasH );
        }
        
        // Position the FPS box
        if( fpsImage != null )
        {
            // Position the background image and draw it
            fpsImage.fitCenter( (int) ( (float) canvasW * ( (float) fpsXpercent / 100.0f ) ),
                    (int) ( (float) canvasH * ( (float) fpsYpercent / 100.0f ) ), canvasW, canvasH );
        }
    }
    
    public void updateAnalog( float axisFractionX, float axisFractionY )
    {
        // TODO: update the member fields
        // TODO: call listeners
    }
    
    public void updateFps( int fps )
    {
        // Quick return if user has disabled FPS or it hasn't changed
        if( !Globals.userPrefs.isFrameRateEnabled || fpsValue == fps )
            return;
        
        // Clamp to positive, four digits max
        fpsValue = Math.max( Math.min( fps, 9999 ), 0 );
        
        String fpsString = Integer.toString( fpsValue );
        for( int i = 0; i < 4; i++ )
        {
            // Create a new sequence of number digit images
            if( i < fpsString.length() )
            {
                try
                {
                    // Clone the digit from the font images
                    fpsDigits[i] = new Utility.Image( mResources,
                            numberImages[Integer.valueOf( fpsString.substring( i, i + 1 ) )] );
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
        
        // TODO: call listeners
    }
    
    public Rect getAnalogBounds()
    {
        // TODO what if analog is null?
        int x1 = analogImage.x;
        int y1 = analogImage.y;
        int x2 = analogImage.x + analogImage.width;
        int y2 = analogImage.y + analogImage.height;
        
        if( hatImage != null )
        {
            // Expand invalidation box if necessary
            x1 = Math.min( x1, hatImage.x );
            y1 = Math.min( y1, hatImage.y );
            x2 = Math.max( x2, hatImage.x + hatImage.width );
            y2 = Math.max( y2, hatImage.y + hatImage.height );
        }
        
        return new Rect( x1, y1, x2, y2 );
    }

    public Rect getFpsBounds()
    {
        return new Rect( fpsImage.x, fpsImage.y, fpsImage.x + fpsImage.width, fpsImage.y
                + fpsImage.height );
    }

    public void drawStatic( Canvas canvas )
    {
        for( Image button : buttons )
        {
            // Draw the buttons onto the canvas
            button.draw( canvas );
        }
    }
    
    public void drawAnalog( Canvas canvas )
    {
        if( analogImage != null )
        {
            // Draw the background image first
            analogImage.draw( canvas );
            
            // Then draw the moveable part of the stick
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
        x = x - (int) ( (float) totalWidth / 2.0f );
        
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
    
    public void loadPad()
    {
        // Always clear the skin
        clear();
        
        // If no skin to display, quit
        if( !Globals.userPrefs.isTouchscreenEnabled && !Globals.userPrefs.isFrameRateEnabled )
            return;
        
        // Stop anything accessing settings while loading
        initialized = false;
        
        // Load the configuration file (pad.ini)
        ConfigFile pad_ini = new ConfigFile( Globals.userPrefs.touchscreenLayoutFolder + "/pad.ini" );
        
        // Look up the mask colors
        readMaskColors( pad_ini );
        
        // Loop through all the sections
        readFiles( pad_ini, Globals.userPrefs.touchscreenLayoutFolder );
        
        // Free the data that was loaded from the config file:
        pad_ini.clear();
        pad_ini = null;
        
        // Everything is loaded now
        initialized = true;
        
        // TODO: notify listeners
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
                int intVal = Utility.toInt( val, -1 );
                param = param.toLowerCase(); // Lets not make this part case-sensitive
                
                if( param.equals( "right" ) )
                    maskColors[AbstractController.DPD_R] = intVal;
                
                else if( param.equals( "left" ) )
                    maskColors[AbstractController.DPD_L] = intVal;
                
                else if( param.equals( "down" ) )
                    maskColors[AbstractController.DPD_D] = intVal;
                
                else if( param.equals( "up" ) )
                    maskColors[AbstractController.DPD_U] = intVal;
                
                else if( param.equals( "start" ) )
                    maskColors[AbstractController.START] = intVal;
                
                else if( param.equals( "z" ) )
                    maskColors[AbstractController.BTN_Z] = intVal;
                
                else if( param.equals( "b" ) )
                    maskColors[AbstractController.BTN_B] = intVal;
                
                else if( param.equals( "a" ) )
                    maskColors[AbstractController.BTN_A] = intVal;
                
                else if( param.equals( "cright" ) )
                    maskColors[AbstractController.CPD_R] = intVal;
                
                else if( param.equals( "cleft" ) )
                    maskColors[AbstractController.CPD_L] = intVal;
                
                else if( param.equals( "cdown" ) )
                    maskColors[AbstractController.CPD_D] = intVal;
                
                else if( param.equals( "cup" ) )
                    maskColors[AbstractController.CPD_U] = intVal;
                
                else if( param.equals( "r" ) )
                    maskColors[AbstractController.BTN_R] = intVal;
                
                else if( param.equals( "l" ) )
                    maskColors[AbstractController.BTN_L] = intVal;
                
                else if( param.equals( "upright" ) )
                    maskColors[DPD_RU] = intVal;
                
                else if( param.equals( "rightdown" ) )
                    maskColors[DPD_RD] = intVal;
                
                else if( param.equals( "leftdown" ) )
                    maskColors[DPD_LD] = intVal;
                
                else if( param.equals( "leftup" ) )
                    maskColors[DPD_LU] = intVal;
                
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
        buttons.add( new Utility.Image( mResources, layoutFolder + "/" + filename + ".png" ) );
        masks.add( new Utility.Image( mResources, layoutFolder + "/" + filename + ".bmp" ) );
        
        // Position (percentages of the screen dimensions)
        xpercents.add( Utility.toInt( section.get( "x" ), 0 ) );
        ypercents.add( Utility.toInt( section.get( "y" ), 0 ) );
    }
    
    private void readAnalogLayout( final String layoutFolder, String filename,
            ConfigSection section, boolean hasHat )
    {
        analogImage = new Utility.Image( mResources, layoutFolder + "/" + filename + ".png" );
        if( hasHat )
        {
            // There's a "stick" image.. same name, with "_2" appended
            hatImage = new Utility.Image( mResources, layoutFolder + "/" + filename + "_2.png" );
        }
        
        // Position (percentages of the screen dimensions)
        analogXpercent = Utility.toInt( section.get( "x" ), 0 );
        analogYpercent = Utility.toInt( section.get( "y" ), 0 );
        
        // Sensitivity (percentages of the radius, i.e. half the image width)
        analogDeadzone = (int) ( (float) analogImage.hWidth * ( Utility.toFloat(
                section.get( "min" ), 1 ) / 100.0f ) );
        analogMaximum = (int) ( (float) analogImage.hWidth * ( Utility.toFloat(
                section.get( "max" ), 55 ) / 100.0f ) );
        analogPadding = (int) ( (float) analogImage.hWidth * ( Utility.toFloat(
                section.get( "buff" ), 55 ) / 100.0f ) );
    }
    
    private void readFpsLayout( final String layoutFolder, String filename, ConfigSection section )
    {
        fpsImage = new Utility.Image( mResources, layoutFolder + "/" + filename + ".png" );
        
        // Position (percentages of the screen dimensions)
        fpsXpercent = Utility.toInt( section.get( "x" ), 0 );
        fpsYpercent = Utility.toInt( section.get( "y" ), 0 );
        
        // Number position (percentages of the FPS indicator dimensions)
        fpsNumXpercent = Utility.toInt( section.get( "numx" ), 50 );
        fpsNumYpercent = Utility.toInt( section.get( "numy" ), 50 );
        
        // Refresh rate (in frames.. integer greater than 1)
        fpsRate = Utility.toInt( section.get( "rate" ), 15 );
        
        // Need at least 2 frames to calculate FPS
        if( fpsRate < 2 )
            fpsRate = 2;
        
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
                    numberImages[i] = new Utility.Image( mResources, Globals.paths.fontsDir
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
}
