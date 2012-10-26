package paulscode.android.mupen64plusae.input;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Set;

import paulscode.android.mupen64plusae.Globals;
import paulscode.android.mupen64plusae.NativeMethods;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.util.Utility;
import android.content.Context;
import android.content.res.Resources;
import android.util.FloatMath;
import android.util.Log;

/**
 * The XperiaPlayController class provides a customizable interface with the Xperia Play touchpad.
 * 
 * @author: Paul Lamb
 * 
 *          http://www.paulscode.com
 * 
 */
public class XperiaPlayController extends AbstractController
{
    // Maximum number of buttons that a touchpad layout can have:
    public static final int MAX_BUTTONS = 30;
    // "Pixel" dimensions of the pad (assuming they're constant):
    public static final int PAD_WIDTH = 966;
    public static final int PAD_HEIGHT = 360;
    
    public String name = "";
    public String version = "";
    public String about = "";
    public String author = "";
    
    // Mask colors for normal N64 buttons:
    private int[] maskColors = new int[18];
    // Normal N64 button-states associated with the mask colors:
    private boolean[] buttonPressed = new boolean[18];
    
    // --Special buttons that behave like hardware buttons--
    // Mask colors for the SDL buttons:
    private int[] SDLButtonMaskColors = new int[MAX_BUTTONS];
    // SDL scancodes associated with the mask colors:
    private int[] SDLButtonCodes = new int[MAX_BUTTONS];
    // SDL button-states associates with the mask colors:
    private boolean[] SDLButtonPressed = new boolean[MAX_BUTTONS];
    //
    private boolean[] mp64pButtons = new boolean[14];

    
    private Utility.Image analogMask = null;
    private int analogXpercent = 0;
    private int analogYpercent = 0;
    private int analogPadding  = 32;
    private int analogDeadzone = 2;
    private int analogMaximum  = 360;
    private int analogPid      = -1;
    
    // All button images and associated mask images, including both
    // normal N64 buttons and SDL buttons:
    private Utility.Image[] masks = new Utility.Image[MAX_BUTTONS];
    private int[] xpercents = new int[MAX_BUTTONS];
    private int[] ypercents = new int[MAX_BUTTONS];
    
    private int buttonCount = 0; // Total number of buttons
    private int SDLButtonCount = 0; // Number of SDL buttons
    
    public Resources resources = null;
    boolean initialized = false;
    public static int whichTouchPad = 0;
    public static int[] touchPadPointerY = new int[256];
    public static int[] touchPadPointerX = new int[256];
    public static boolean[] touchPadPointers = new boolean[256];
    
    /**
     * Constructor: Instantiates the touch pad layout
     * 
     * @param context
     *            Handle to the app context.
     * @param res
     *            Handle to the app resources.
     */
    public XperiaPlayController( Context context, Resources res )
    {
        for( int x = 0; x < 256; x++ )
        {
            XperiaPlayController.touchPadPointers[x] = false;
            XperiaPlayController.touchPadPointerX[x] = -1;
            XperiaPlayController.touchPadPointerY[x] = -1;
            TouchscreenController.touchScreenPointers[x] = false;
            TouchscreenController.touchScreenPointerX[x] = -1;
            TouchscreenController.touchScreenPointerY[x] = -1;
        }
        for( int x = 0; x < 30; x++ )
        {
            TouchscreenController.previousKeyStates[x] = false;
        }
        resources = res;
    }
    
    /**
     * Determines which controls are pressed based on where the pad is being touched.
     * 
     * @param pointers
     *            Array indicating which pointers are touching the pad.
     * @param pointerX
     *            Array containing the X-coordinate of each pointer.
     * @param pointerY
     *            Array containing the Y-coordinate of each pointer.
     * @param maxPid
     *            Maximum ID of the pointers that have changed (speed optimization)
     */
    public void updatePointers( boolean[] pointers, int[] pointerX, int[] pointerY, int maxPid )
    {
        if( !initialized )
            return;
        
        int x, y, c, rgb;
        float d, p, dX, dY;
        
        // Clear any previous pointer data:
        int axisX = 0;
        int axisY = 0;
        
        // Clear any data about which buttons were pressed:
        for( int i = 0; i < 18; i++ )
            buttonPressed[i] = false;
        for( int i = 0; i < SDLButtonCount; i++ )
            SDLButtonPressed[i] = false;
        for( int i = 0; i < 14; i++ )
            mp64pButtons[i] = false;
        
        // Process each pointer in sequence
        for( int i = 0; i <= maxPid; i++ )
        {
            if( i == analogPid && !pointers[i] )
            {
                // Release analog if it's pointer is not touching the pad
                analogPid = -1;
            }
                
            // Pointer is touching the pad
            if( pointers[i] )
            {
                x = pointerX[i];
                y = pointerY[i];
                
                // Not the analog control, check the buttons
                if( i != analogPid )
                {
                    // Check each one in sequence
                    for( int m = 0; m < buttonCount; m++ )
                    {
                        // If it is inside this button, check the color mask
                        if( x >= masks[m].x && x < masks[m].x + masks[m].width && y >= masks[m].y
                                && y < masks[m].y + masks[m].height )
                        {
                            c = masks[m].image.getPixel( x - masks[m].x, y - masks[m].y );
                            
                            // Ignore the alpha component if any
                            rgb = (int) ( c & 0x00ffffff );
                            
                            // Ignore black
                            if( rgb > 0 )
                            {
                                // Determine what was pressed
                                pressColor( rgb );
                            }
                        }
                    }
                }
                
                if( analogMask != null )
                {
                    // Distance from center along x-axis
                    dX = (float) ( x - ( analogMask.x + analogMask.hWidth ) ); 
                    
                    // Distance from center along y-axis
                    dY = (float) ( ( analogMask.y + analogMask.hHeight ) - y );
                    
                    // Distance from center
                    d = FloatMath.sqrt( ( dX * dX ) + ( dY * dY ) ); 
                    
                    // Inside the analog control
                    if( ( i == analogPid )
                            || ( d >= analogDeadzone && d < analogMaximum + analogPadding ) )
                    {
                        // Emulate the analog control as an octagon (like the real N64 controller)
                        if( Globals.userPrefs.isOctagonalJoystick )
                        {
                            Utility.Point crossPt = new Utility.Point();
                            float dC = analogMask.hWidth;
                            float dA = FloatMath.sqrt( ( dC * dC ) / 2.0f );
                            
                            if( dX > 0 && dY > 0 ) // Quadrant I
                            {
                                if( segsCross( 0, 0, dX, dY, 0, dC, dA, dA, crossPt )
                                        || segsCross( 0, 0, dX, dY, dA, dA, 80, 0, crossPt ) )
                                {
                                    dX = crossPt.x;
                                    dY = crossPt.y;
                                }
                            }
                            else if( dX < 0 && dY > 0 ) // Quadrant II
                            {
                                if( segsCross( 0, 0, dX, dY, 0, dC, -dA, dA, crossPt )
                                        || segsCross( 0, 0, dX, dY, -dA, dA, -dC, 0, crossPt ) )
                                {
                                    dX = crossPt.x;
                                    dY = crossPt.y;
                                }
                            }
                            else if( dX < 0 && dY < 0 ) // Quadrant III
                            {
                                if( segsCross( 0, 0, dX, dY, -dC, 0, -dA, -dA, crossPt )
                                        || segsCross( 0, 0, dX, dY, -dA, -dA, 0, -dC, crossPt ) )
                                {
                                    dX = crossPt.x;
                                    dY = crossPt.y;
                                }
                            }
                            else if( dX > 0 && dY < 0 ) // Quadrant IV
                            {
                                if( segsCross( 0, 0, dX, dY, 0, -dC, dA, -dA, crossPt )
                                        || segsCross( 0, 0, dX, dY, dA, -dA, dC, 0, crossPt ) )
                                {
                                    dX = crossPt.x;
                                    dY = crossPt.y;
                                }
                            }
                            d = FloatMath.sqrt( ( dX * dX ) + ( dY * dY ) ); // distance from center
                        }
                        
                        // "Capture" the analog control
                        analogPid = i;
                        
                        // Percentage of full-throttle, clamped to range [0-1]
                        p = ( d - (float) analogDeadzone )
                                / (float) ( analogMaximum - analogDeadzone );

                        p = Math.max( Math.min( p, 1 ), 0 );
                        
                        // From the N64 function reference: The 3D Stick data is of type signed char and
                        // in the range between 80 and -80. (32768 / 409 = ~80.1)
                        axisX = (int) ( ( dX / d ) * p * 80.0f );
                        axisY = (int) ( ( dY / d ) * p * 80.0f );
                        axisX = Math.max( Math.min( axisX, 80 ), -80 );
                        axisY = Math.max( Math.min( axisY, 80 ), -80 );
                    }
                }
            }
        }
        
        NativeMethods.updateVirtualGamePadStates( 0, mp64pButtons, axisX, axisY );
        TouchscreenController.updateSDLButtonStates( Globals.surfaceInstance, SDLButtonPressed,
                SDLButtonCodes, SDLButtonCount );
    }
    
    /**
     * Determines which button was pressed based on the closest mask color. 
     * </p>
     * TODO: Android is not precise: the color is different than it should be!)
     * </p>
     * @param color
     *            Color of the pixel that the user pressed.
     */
    protected void pressColor( int color )
    {
        int closestMatch = 0; // Start with the first N64 button
        int closestSDLButtonMatch = -1; // Disable this to start with
        int matchDif = Math.abs( maskColors[0] - color );
        int dif;
        
        for( int x = 1; x < 18; x++ )
        { 
            // Go through the N64 button mask colors first
            dif = Math.abs( maskColors[x] - color );
            if( dif < matchDif )
            { 
                // This is a closer match
                closestMatch = x;
                matchDif = dif;
            }
        }
        
        for( int x = 0; x < SDLButtonCount; x++ )
        { 
            // Now see if any of the SDL button mask colors are closer
            dif = Math.abs( SDLButtonMaskColors[x] - color );
            if( dif < matchDif )
            { 
                // This is a closer match
                closestSDLButtonMatch = x;
                matchDif = dif;
            }
        }
        
        if( closestSDLButtonMatch > -1 )
        {
            // Found an SDL button that matches the color
            SDLButtonPressed[closestSDLButtonMatch] = true;
        }
        else
        {
            // One of the N64 buttons matched the color
            buttonPressed[closestMatch] = true;
            
            // Only 14 buttons in Mupen64Plus API
            if( closestMatch < 14 )
            {
                mp64pButtons[closestMatch] = true;
            }
            // Simulate the remaining buttons:
            else if( closestMatch == Controls.UpRight )
            {
                mp64pButtons[Controls.Up] = true;
                mp64pButtons[Controls.Right] = true;
            }
            else if( closestMatch == Controls.RightDown )
            {
                mp64pButtons[Controls.Right] = true;
                mp64pButtons[Controls.Down] = true;
            }
            else if( closestMatch == Controls.LeftDown )
            {
                mp64pButtons[Controls.Left] = true;
                mp64pButtons[Controls.Down] = true;
            }
            else if( closestMatch == Controls.LeftUp )
            {
                mp64pButtons[Controls.Left] = true;
                mp64pButtons[Controls.Up] = true;
            }
        }
    }
    
    public void loadPad()
    {
        // TODO: Encapsulate call to overloaded method
        // if( !Globals.user.touchscreenEnabled )
        // loadPad( null );
        // else if( !Globals.user.touchscreenLayoutIndex.isEmpty() )
        // loadPad( Globals.user.touchscreenLayoutIndex );
        // else if( Globals.Game.mTouchPadListing.numPads > 0 )
        // loadPad( mTouchPadListing.padNames[0] );
        // else
        // {
        // loadPad( null );
        // Log.v( "GameActivityXperiaPlay", "No touchpad skins found" );
        // }
    }
    
    /**
     * Loads the specified touchpad skin
     * 
     * @param skin
     *            Name of the layout skin to load.
     */
    protected void loadPad( String skin )
    {
        // Stop anything accessing settings while loading
        initialized = false;
        
        // Clear everything out to be re-populated with the new settings:
        name = "";
        version = "";
        about = "";
        author = "";
        analogMask = null;
        analogXpercent = 0;
        analogYpercent = 0;
        masks = new Utility.Image[MAX_BUTTONS];
        xpercents = new int[MAX_BUTTONS];
        ypercents = new int[MAX_BUTTONS];
        buttonCount = 0;
        SDLButtonCount = 0;
        String filename;
        
        for( int i = 0; i < 18; i++ )
        {
            maskColors[i] = -1;
            buttonPressed[i] = false;
        }
       
        for( int i = 0; i < MAX_BUTTONS; i++ )
        {
            SDLButtonMaskColors[i] = -1;
            SDLButtonCodes[i] = -1;
            SDLButtonPressed[i] = false;
        }
        
        for( int i = 0; i < 14; i++ )
        {
            mp64pButtons[i] = false;
        }
        
        if( skin == null )
            return; // No skin was specified, so we are done.. quit
        // Load the configuration file (pad.ini):
        ConfigFile pad_ini = new ConfigFile( Globals.paths.dataDir + "/skins/touchpads/" + skin
                + "/pad.ini" );
        
        // Look up the touch-pad layout credits:
        name = pad_ini.get( "INFO", "name" );
        version = pad_ini.get( "INFO", "version" );
        about = pad_ini.get( "INFO", "about" );
        author = pad_ini.get( "INFO", "author" );
        
        Set<String> keys;
        Iterator<String> iter;
        String param, val;
        int valI;
        // Look up the mask colors:
        ConfigFile.ConfigSection section = pad_ini.get( "MASK_COLOR" );
        if( section != null )
        {
            keys = section.keySet();
            iter = keys.iterator();
            
            // Loop through the param=val pairs
            while( iter.hasNext() )
            {
                param = iter.next();
                val = section.get( param );
                valI = Utility.toInt( val, -1 ); // -1 (undefined) in case of number format problem
                param = param.toLowerCase(); // lets not make this part case-sensitive
                if( param.equals( "cup" ) )
                    maskColors[Controls.CUp] = valI;
                else if( param.equals( "cright" ) )
                    maskColors[Controls.CRight] = valI;
                else if( param.equals( "cdown" ) )
                    maskColors[Controls.CDown] = valI;
                else if( param.equals( "cleft" ) )
                    maskColors[Controls.CLeft] = valI;
                else if( param.equals( "a" ) )
                    maskColors[Controls.A] = valI;
                else if( param.equals( "b" ) )
                    maskColors[Controls.B] = valI;
                else if( param.equals( "l" ) )
                    maskColors[Controls.L] = valI;
                else if( param.equals( "r" ) )
                    maskColors[Controls.R] = valI;
                else if( param.equals( "z" ) )
                    maskColors[Controls.Z] = valI;
                else if( param.equals( "start" ) )
                    maskColors[Controls.Start] = valI;
                else if( param.equals( "leftup" ) )
                    maskColors[Controls.LeftUp] = valI;
                else if( param.equals( "up" ) )
                    maskColors[Controls.Up] = valI;
                else if( param.equals( "upright" ) )
                    maskColors[Controls.UpRight] = valI;
                else if( param.equals( "right" ) )
                    maskColors[Controls.Right] = valI;
                else if( param.equals( "rightdown" ) )
                    maskColors[Controls.RightDown] = valI;
                else if( param.equals( "leftdown" ) )
                    maskColors[Controls.LeftDown] = valI;
                else if( param.equals( "down" ) )
                    maskColors[Controls.Down] = valI;
                else if( param.equals( "left" ) )
                    maskColors[Controls.Left] = valI;
                else if( param.contains( "scancode_" ) )
                {
                    try
                    { 
                        // Make sure a valid integer was used for the scancode
                        SDLButtonCodes[SDLButtonCount] = Integer.valueOf( param.substring( 9,
                                param.length() ) );
                        SDLButtonMaskColors[SDLButtonCount] = valI;
                        SDLButtonCount++;
                    }
                    catch( NumberFormatException nfe )
                    {
                        // Skip it if this happens
                    }
                }
            }
        }
        
        Set<String> mKeys = pad_ini.keySet();
        
        // Loop through all the sections
        for( String mKey : mKeys )
        {
            // The rest of the sections are filenames
            filename = mKey;
            
            if( filename != null && filename.length() > 0 && !filename.equals( "INFO" )
                    && !filename.equals( "MASK_COLOR" ) && !filename.equals( "[<sectionless!>]" ) )
            { 
                // Yep, its definitely a filename
                section = pad_ini.get( filename );
                if( section != null )
                { 
                    // Process the parameters for this section
                    val = section.get( "info" ); // what type of control
                    if( val != null )
                    {
                        // Lets not make this part case-sensitive
                        val = val.toLowerCase();
                        
                        if( val.contains( "analog" ) )
                        { 
                            // Analog color mask image in BMP image format (doesn't actually get drawn)
                            analogMask = new Utility.Image( resources, Globals.paths.dataDir
                                    + "/skins/touchpads/" + skin + "/" + filename + ".bmp" );
                            
                            // Position (percentages of the screen dimensions):
                            analogXpercent = Utility.toInt( section.get( "x" ), 0 );
                            analogYpercent = Utility.toInt( section.get( "y" ), 0 );
                            
                            // Sensitivity (percentages of the radius, i.e. half the image width):
                            analogDeadzone = (int) ( (float) analogMask.hWidth * ( Utility.toFloat(
                                    section.get( "min" ), 1 ) / 100.0f ) );
                            analogMaximum = (int) ( (float) analogMask.hWidth * ( Utility.toFloat(
                                    section.get( "max" ), 55 ) / 100.0f ) );
                            analogPadding = (int) ( (float) analogMask.hWidth * ( Utility.toFloat(
                                    section.get( "buff" ), 55 ) / 100.0f ) );
                            analogMask
                                    .fitCenter(
                                            (int) ( (float) PAD_WIDTH * ( (float) analogXpercent / 100.0f ) ),
                                            (int) ( (float) PAD_HEIGHT * ( (float) analogYpercent / 100.0f ) ),
                                            PAD_WIDTH, PAD_HEIGHT );
                        }
                        else
                        { 
                            // A button control (may contain one or more N64 buttons and/or SDL buttons)
                            // Button color mask image in BMP image format (doesn't actually get drawn)
                            masks[buttonCount] = new Utility.Image( resources, Globals.paths.dataDir
                                    + "/skins/touchpads/" + skin + "/" + filename + ".bmp" );
                           
                            // Position (percentages of the screen dimensions):
                            xpercents[buttonCount] = Utility.toInt( section.get( "x" ), 0 );
                            ypercents[buttonCount] = Utility.toInt( section.get( "y" ), 0 );
                            masks[buttonCount]
                                    .fitCenter(
                                            (int) ( (float) PAD_WIDTH * ( (float) xpercents[buttonCount] / 100.0f ) ),
                                            (int) ( (float) PAD_HEIGHT * ( (float) ypercents[buttonCount] / 100.0f ) ),
                                            PAD_WIDTH, PAD_HEIGHT );
                            
                            Log.v( "TouchPad", "Adding button grouping " + buttonCount + ", ("
                                    + xpercents[buttonCount] + ", " + ypercents[buttonCount] + ")" );
                            Log.v( "TouchPad", "Fit x center to "
                                            + (int) ( (float) PAD_WIDTH * ( (float) xpercents[buttonCount] / 100.0f ) ) );
                            Log.v( "TouchPad", "Fit y center to "
                                            + (int) ( (float) PAD_HEIGHT * ( (float) ypercents[buttonCount] / 100.0f ) ) );
                            Log.v( "TouchPad", "Converted max coordinates: ("
                                    + masks[buttonCount].x + ", " + masks[buttonCount].y + ")" );
                            
                            buttonCount++;
                        }
                    }
                }
            }
        }
        // Free the data that was loaded from the config file:
        pad_ini.clear();
        pad_ini = null;
        
        // Everything is loaded now
        initialized = true;
    }
    
    public void touchPadBeginEvent()
    {
    }
    
    public void touchPadPointerDown( int pointer_id )
    {
        XperiaPlayController.touchPadPointers[pointer_id] = true;
    }
    
    public void touchPadPointerUp( int pointer_id )
    {
        XperiaPlayController.touchPadPointers[pointer_id] = false;
        XperiaPlayController.touchPadPointerX[pointer_id] = -1;
        XperiaPlayController.touchPadPointerY[pointer_id] = -1;
    }
    
    public void touchPadPointerPosition( int pointer_id, int x, int y )
    {
        XperiaPlayController.touchPadPointers[pointer_id] = true;
        XperiaPlayController.touchPadPointerX[pointer_id] = x;
        XperiaPlayController.touchPadPointerY[pointer_id] = XperiaPlayController.PAD_HEIGHT - y;
        
        // the Xperia Play's touchpad y-axis is flipped for some reason
    }
    
    public void touchPadEndEvent()
    {
        if( Globals.surfaceInstance != null )
        {
            Globals.surfaceInstance.onTouchPad( XperiaPlayController.touchPadPointers,
                    XperiaPlayController.touchPadPointerX, XperiaPlayController.touchPadPointerY,
                    64 );
        }
    }
    
    public void touchScreenBeginEvent()
    {
    }
    
    public void touchScreenPointerDown( int pointer_id )
    {
        TouchscreenController.touchScreenPointers[pointer_id] = true;
    }
    
    public void touchScreenPointerUp( int pointer_id )
    {
        TouchscreenController.touchScreenPointers[pointer_id] = false;
        TouchscreenController.touchScreenPointerX[pointer_id] = -1;
        TouchscreenController.touchScreenPointerY[pointer_id] = -1;
    }
    
    public void touchScreenPointerPosition( int pointer_id, int x, int y )
    {
        TouchscreenController.touchScreenPointers[pointer_id] = true;
        TouchscreenController.touchScreenPointerX[pointer_id] = x;
        TouchscreenController.touchScreenPointerY[pointer_id] = y;
    }
    
    public void touchScreenEndEvent()
    {
        if( Globals.surfaceInstance != null )
            Globals.surfaceInstance.onTouchScreen( TouchscreenController.touchScreenPointers,
                    TouchscreenController.touchScreenPointerX,
                    TouchscreenController.touchScreenPointerY, 64 );
    }
    
    public boolean onNativeKey( int action, int keycode )
    {
        if( Globals.surfaceInstance == null )
            return false;
        return Globals.surfaceInstance.onKey( keycode, action );
    }
    
    /**
     * Determines if the two specified line segments intersect with each other, and calculates where
     * the intersection occurs if they do.
     * 
     * @param seg1pt1_x
     *            X-coordinate for the first end of the first line segment.
     * @param seg1pt1_y
     *            Y-coordinate for the first end of the first line segment.
     * @param seg1pt2_x
     *            X-coordinate for the second end of the first line segment.
     * @param seg1pt2_y
     *            Y-coordinate for the second end of the first line segment.
     * @param seg2pt1_x
     *            X-coordinate for the first end of the second line segment.
     * @param seg2pt1_y
     *            Y-coordinate for the first end of the second line segment.
     * @param seg2pt2_x
     *            X-coordinate for the second end of the second line segment.
     * @param seg2pt2_y
     *            Y-coordinate for the second end of the second line segment.
     * @param crossPt
     *            Changed to the point of intersection if there is one, otherwise unchanged.
     * @return True if the two line segments intersect.
     */
    public static boolean segsCross( float seg1pt1_x, float seg1pt1_y, float seg1pt2_x,
            float seg1pt2_y, float seg2pt1_x, float seg2pt1_y, float seg2pt2_x, float seg2pt2_y,
            Utility.Point crossPt )
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
        float t = ( vec2_x * ( seg1pt1_y - seg2pt1_y ) - vec2_y * ( seg1pt1_x - seg2pt1_x ) ) / div;
        
        if( s >= 0 && s < 1 && t >= 0 && t <= 1 )
        {
            // Segments cross, point of intersection stored in 'crossPt'
            crossPt.x = seg1pt1_x + ( t * vec1_x );
            crossPt.y = seg1pt1_y + ( t * vec1_y );
            return true;
        }
        
        // Segments don't cross
        return false;
    }
    
    /**
     * The TouchPadListing class reads in the listing of touchpads from touchpad_list.ini.
     */
    public static class TouchPadListing
    {
        public int numPads = 0;
        public String[] padNames = new String[256];
        
        /**
         * Constructor: Reads in the list of touchpads
         * 
         * @param filename
         *            File containing the list of touchpads (typically touchpad_list.ini).
         */
        public TouchPadListing( String filename )
        {
            try
            {
                FileInputStream fstream = new FileInputStream( filename );
                DataInputStream in = new DataInputStream( fstream );
                BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
                String strLine;
                while( ( strLine = br.readLine() ) != null )
                {
                    if( strLine.length() > 0 )
                    {
                        padNames[numPads] = strLine;
                        numPads++;
                    }
                }
                in.close();
            }
            catch( Exception e )
            {
                Log.e( "TouchPad.TouchPadListing", "Exception, error message: " + e.getMessage() );
            }
        }
    }
}
