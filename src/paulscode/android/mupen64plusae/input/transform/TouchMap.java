package paulscode.android.mupen64plusae.input.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import paulscode.android.mupen64plusae.Globals;
import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.input.TouchscreenController;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.util.SubscriptionManager;
import paulscode.android.mupen64plusae.util.Utility;
import paulscode.android.mupen64plusae.util.Utility.Image;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;

// TODO: Move some functionality into TouchscreenView if TouchMap also used for XperiaPlayController
public class TouchMap
{
    public interface Listener
    {
        public void onAllChanged( TouchMap touchMap );
        
        public void onHatChanged( TouchMap touchMap, float x, float y );
        
        public void onFpsChanged( TouchMap touchMap, int fps );
    }
    
    // Mask colors
    public int[] maskColors;
    
    // Buttons
    public ArrayList<Utility.Image> masks;
    private ArrayList<Utility.Image> buttons;
    private ArrayList<Integer> xpercents;
    private ArrayList<Integer> ypercents;
    
    // Analog background
    public Utility.Image analogImage;
    private int analogXpercent;
    private int analogYpercent;
    public float analogPadding;
    public float analogDeadzone;
    public float analogMaximum;

    // Analog stick
    private Utility.Image hatImage;
    private int hatX;
    private int hatY;
    
    // Frame rate display
    private Utility.Image fpsImage;
    private int fpsXpercent;
    private int fpsYpercent;
    private int fpsNumXpercent;
    private int fpsNumYpercent;
    public int fpsRate;
    private int fpsValue;
    private String fpsFont;
    private Utility.Image[] numeralImages;
    private Utility.Image[] fpsDigits;
    
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
        BUTTON_HASHMAP.put( "upright", TouchscreenController.DPD_RU );
        BUTTON_HASHMAP.put( "rightdown", TouchscreenController.DPD_RD );
        BUTTON_HASHMAP.put( "leftdown", TouchscreenController.DPD_LD );
        BUTTON_HASHMAP.put( "leftup", TouchscreenController.DPD_LU );
    }
    
    public TouchMap()
    {
        mPublisher = new SubscriptionManager<TouchMap.Listener>();
        numeralImages = new Utility.Image[10];
        fpsDigits = new Utility.Image[4];
        maskColors = new int[TouchscreenController.NUM_PSEUDO_BUTTONS];
        clear();
    }
    
    public void clear()
    {
        buttons = new ArrayList<Utility.Image>();
        masks = new ArrayList<Utility.Image>();
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
        fpsRate = 15;
        fpsValue = 0;
        fpsFont = "Mupen64Plus-AE-Contrast-Blue";
        for( int i = 0; i < numeralImages.length; i++)
            numeralImages[i] = null;
        for( int i = 0; i < fpsDigits.length; i++)
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
            numeralImages[i] = new Utility.Image( resources, Globals.paths.fontsDir + fpsFont + "/"
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
        
        // Notify listeners that it has changed
        for( Listener listener : mPublisher.getSubscribers() )
            listener.onAllChanged( this );
    }
    
    public void updateAnalog( float axisFractionX, float axisFractionY )
    {
        // Move the analog hat based on analog state
        hatX = analogImage.hWidth + (int) ( axisFractionX * (float) analogMaximum);
        hatY = analogImage.hHeight - (int) ( axisFractionY * (float) analogMaximum);
        
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
                    fpsDigits[i] = new Utility.Image( mResources,
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
        
        // Load the configuration file (pad.ini)
        ConfigFile pad_ini = new ConfigFile( Globals.userPrefs.touchscreenLayoutFolder + "/pad.ini" );
        
        // Look up the mask colors
        readMaskColors( pad_ini );
        
        // Loop through all the sections
        readFiles( pad_ini, Globals.userPrefs.touchscreenLayoutFolder );
        
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
                maskColors[BUTTON_HASHMAP.get( param.toLowerCase() )] = Utility.toInt( val, -1 );
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
                    numeralImages[i] = new Utility.Image( mResources, Globals.paths.fontsDir
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
