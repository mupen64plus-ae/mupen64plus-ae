package paulscode.android.mupen64plus;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.Integer;
import java.lang.Math;
import java.lang.NumberFormatException;
import java.util.Iterator;
import java.util.Set;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.View;

/**
 * The TouchPad class provides a customizable interface with the
 * Xperia Play touch pad.
 *
 * Author: Paul Lamb
 * 
 * http://www.paulscode.com
 * 
 */
public class TouchPad 
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
    // Must be the same order as EButton listing in plugin.h! (input-sdl plug-in) 
    private static final int Right	=  0;
    private static final int Left	=  1;
    private static final int Down	=  2;
    private static final int Up		=  3;
    private static final int Start	=  4;
    private static final int Z		=  5;
    private static final int B		=  6;
    private static final int A		=  7;
    private static final int CRight	=  8;
    private static final int CLeft	=  9;
    private static final int CDown	= 10;
    private static final int CUp	= 11;
    private static final int R		= 12;
    private static final int L		= 13;
    // Not standard mp64p buttons, but simulated here for better control:
    private static final int UpRight	= 14;
    private static final int RightDown	= 15;
    private static final int LeftDown	= 16;
    private static final int LeftUp	= 17;

    private Image analogMask = null;
    private int analogXpercent = 0;
    private int analogYpercent = 0;
    private int analogPadding = 32;
    private int analogDeadzone = 2;
    private int analogMaximum = 360;
    private int analogPid = -1;

    // All button images and associated mask images, including both 
    // normal N64 buttons and SDL buttons:
    private Image[] masks = new Image[MAX_BUTTONS];
    private int[] xpercents = new int[MAX_BUTTONS];
    private int[] ypercents = new int[MAX_BUTTONS];

    private int buttonCount = 0;  // total number of buttons
    private int SDLButtonCount = 0;  // number of SDL buttons

    public Resources resources = null;
    boolean initialized = false;

    /*
     * Constructor: Instantiates the touch pad layout
     * @param context Handle to the app context.
     * @param attribs Handle to the app resources.
     */
    public TouchPad( Context context, Resources res )
    {
        resources = res;
    }

    /*
     * Determines which controls are pressed based on where the pad is being touched.
     * @param pointers Array indicating which pointers are touching the pad.
     * @param pointerX Array containing the X-coordinate of each pointer.
     * @param pointerY Array containing the Y-coordinate of each pointer.
     * @param maxPid Maximum ID of the pointers that have changed (speed optimization)
     */
    protected void updatePointers( boolean[] pointers, int[] pointerX, int[] pointerY, int maxPid )
    {
        if( !initialized )
            return;

        int i, x, y, m, c, rgb, hatX, hatY;
        float d, p, dX, dY;
        // Clear any previous pointer data:
        int axisX = 0;
        int axisY = 0;
        hatX = -1;
        hatY = -1;
        boolean touchedAnalog = false;
        // Clear any data about which buttons were pressed:
        for( i = 0; i < 18; i++ )
            buttonPressed[i] = false;
        for( i = 0; i < SDLButtonCount; i++ )
            SDLButtonPressed[i] = false;
        for( i = 0; i < 14; i++ )
            mp64pButtons[i] = false;

        for( i = 0; i <= maxPid; i++ )
        {  // process each pointer in sequence
            if( i == analogPid && !pointers[i] )
                analogPid = -1;  // release analog if it's pointer is not touching the pad
            if( pointers[i] )
            {  // pointer is touching the pad
                x = pointerX[i];
                y = pointerY[i];
                
                if( i != analogPid )
                {  // not the analog control, check the buttons
                    for( m = 0; m < buttonCount; m++ )
                    {  // check each one in sequence
                        if( x >= masks[m].x && x < masks[m].x + masks[m].width &&
                            y >= masks[m].y && y < masks[m].y + masks[m].height )
                        {  // it is inside this button, check the color mask
                            c = masks[m].image.getPixel( x - masks[m].x, y - masks[m].y );
                            rgb = (int)(c & 0x00ffffff);  // ignore the alpha component if any
                            if( rgb > 0 )  // ignore black
                                pressColor( rgb );  // determine what was pressed
                        }
                    }
                }
                if( analogMask != null )
                {
                    dX = (float)( x - (analogMask.x + analogMask.hWidth) );  // distance from center along x-axis
                    dY = (float)( (analogMask.y + analogMask.hHeight) - y );  // distance from center along y-axis
                    d = (float) FloatMath.sqrt( (dX * dX) + (dY * dY) );  // distance from center
                    if( (i == analogPid) || (d >= analogDeadzone && d < analogMaximum + analogPadding) )
                    {  // inside the analog control
                        if( MenuSkinsTouchpadActivity.analogAsOctagon )
                        {  // emulate the analog control as an octagon (like the real N64 controller)
                            Point crossPt = new Point();
                            float dC = analogMask.hWidth;
                            float dA = (float) FloatMath.sqrt( (dC * dC) / 2.0f );
                        
                            if( dX > 0 && dY > 0 )  // Quadrant I
                            {
                                if( segsCross( 0, 0, dX, dY, 0, dC, dA, dA, crossPt ) ||
                                    segsCross( 0, 0, dX, dY, dA, dA, 80, 0, crossPt ) )
                                {
                                    dX = crossPt.x;
                                    dY = crossPt.y;
                                }
                            }
                            else if( dX < 0 && dY > 0 )  // Quadrant II
                            {
                                if( segsCross( 0, 0, dX, dY, 0, dC, -dA, dA, crossPt ) ||
                                    segsCross( 0, 0, dX, dY, -dA, dA, -dC, 0, crossPt ) )
                                {
                                    dX = crossPt.x;
                                    dY = crossPt.y;
                                }
                            }
                            else if( dX < 0 && dY < 0 )  // Quadrant III
                            {
                                if( segsCross( 0, 0, dX, dY, -dC, 0, -dA, -dA, crossPt ) ||
                                    segsCross( 0, 0, dX, dY, -dA, -dA, 0, -dC, crossPt ) )
                                {
                                    dX = crossPt.x;
                                    dY = crossPt.y;
                                }
                            }
                            else if( dX > 0 && dY < 0 )  // Quadrant IV
                            {
                                if( segsCross( 0, 0, dX, dY, 0, -dC, dA, -dA, crossPt ) ||
                                    segsCross( 0, 0, dX, dY, dA, -dA, dC, 0, crossPt ) )
                                {
                                    dX = crossPt.x;
                                    dY = crossPt.y;
                                }
                            }
                            d = (float) FloatMath.sqrt( (dX * dX) + (dY * dY) );  // distance from center
                        }
                        analogPid = i;  // "Capture" the analog control
                        touchedAnalog = true;
                        hatX = x - analogMask.x;
                        hatY = y - analogMask.y;

                        p = (d - (float)analogDeadzone) / (float)(analogMaximum - analogDeadzone);  // percentage of full-throttle
                        if( p < 0 )
                            p = 0;
                        if( p > 1 )
                            p = 1;
                        // from the N64 func ref: The 3D Stick data is of type signed char and in
                        // the range between 80 and -80. (32768 / 409 = ~80.1)
                        axisX = (int) ( (dX / d) * p * 80.0f );
                        axisY = (int) ( (dY / d) * p * 80.0f );
                        if( axisX > 80 )
                            axisX = 80;
                        if( axisX < -80 )
                            axisX = -80;
                        if( axisY > 80 )
                            axisY = 80;
                        if( axisY < -80 )
                            axisY = -80;
                    }
                }
            }
        }
        GameActivityCommon.updateVirtualGamePadStates( 0, mp64pButtons, axisX, axisY );
        GameActivityCommon.updateSDLButtonStates( SDLButtonPressed, SDLButtonCodes, SDLButtonCount );
    }

    /*
     * Determines which button was pressed based on the closest mask color.
     * TODO: Android is not precise: the color is different than it should be!)
     * @param color Color of the pixel that the user pressed.
     */
    protected void pressColor( int color )
    {
        int closestMatch = 0;  // start with the first N64 button
        int closestSDLButtonMatch = -1;  // disable this to start with
        int matchDif = Math.abs( maskColors[0] - color );
        int x, dif;
        for( x = 1; x < 18; x++ )
        {  // go through the N64 button mask colors first
            dif = Math.abs( maskColors[x] - color );
            if( dif < matchDif )
            {  // this is a closer match
                closestMatch = x;
                matchDif = dif;
            }
        }
        for( x = 0; x < SDLButtonCount; x++ )
        {  // now see if any of the SDL button mask colors are closer
            dif = Math.abs( SDLButtonMaskColors[x] - color );
            if( dif < matchDif )
            {  // this is a closer match
                closestSDLButtonMatch = x;
                matchDif = dif;
            }
        }

        if( closestSDLButtonMatch > -1 )
        {  // found an SDL button that matches the color
            SDLButtonPressed[closestSDLButtonMatch] = true;
        }
        else
        {  // one of the N64 buttons matched the color
            buttonPressed[closestMatch] = true;
            if( closestMatch < 14 )
            {  // only 14 buttons in Mupen64Plus API
                mp64pButtons[closestMatch] = true;
            }
            // simulate the remaining buttons:
            else if( closestMatch == UpRight )
            {
                mp64pButtons[Up] = true;
                mp64pButtons[Right] = true;
            }
            else if( closestMatch == RightDown )
            {
                mp64pButtons[Right] = true;
                mp64pButtons[Down] = true;
            }
            else if( closestMatch == LeftDown )
            {
                mp64pButtons[Left] = true;
                mp64pButtons[Down] = true;
            }
            else if( closestMatch == LeftUp )
            {
                mp64pButtons[Left] = true;
                mp64pButtons[Up] = true;
            }
        }
    }

    /*
     * Loads the specifed touchpad skin
     * @param skin Name of the layout skin to load.
     */
    protected void loadPad( String skin )
    {
        initialized = false;  // stop anything accessing settings while loading
        // Clear everything out to be re-populated with the new settings:
        name = "";
        version = "";
        about = "";
        author = "";
        analogMask = null;
        analogXpercent = 0;
        analogYpercent = 0;
        masks = new Image[MAX_BUTTONS];
        xpercents = new int[MAX_BUTTONS];
        ypercents = new int[MAX_BUTTONS];
        buttonCount = 0;
        SDLButtonCount = 0;
        int xpercent = 0;
        int ypercent = 0;
        String filename;
        int i;
        for( i = 0; i < 18; i++ )
        {
            maskColors[i] = -1;
            buttonPressed[i] = false;
        }
        for( i = 0; i < MAX_BUTTONS; i++ )
        {
            SDLButtonMaskColors[i] = -1;
            SDLButtonCodes[i] = -1;
            SDLButtonPressed[i] = false;
        }
        for( i = 0; i < 14; i++ )
            mp64pButtons[i] = false;

        if( skin == null )
            return;  // no skin was specified, so we are done.. quit
        // Load the configuration file (pad.ini):
        Config pad_ini = new Config( Globals.DataDir + "/skins/touchpads/" + skin + "/pad.ini" );

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
        Config.ConfigSection section = pad_ini.get( "MASK_COLOR" );
        if( section != null )
        {
            keys = section.keySet();
            iter = keys.iterator();
            while( iter.hasNext() )
            {   // Loop through the param=val pairs
                param = iter.next();
                val = section.get( param );
                valI = toInt( val, -1 ); // -1 (undefined) in case of number format problem
                param = param.toLowerCase();  // lets not make this part case-sensitive
                if( param.equals( "cup" ) )
                    maskColors[CUp] = valI;
                else if( param.equals( "cright" ) )
                    maskColors[CRight] = valI;
                else if( param.equals( "cdown" ) )
                    maskColors[CDown] = valI;
                else if( param.equals( "cleft" ) )
                    maskColors[CLeft] = valI;
                else if( param.equals( "a" ) )
                    maskColors[A] = valI;
                else if( param.equals( "b" ) )
                    maskColors[B] = valI;
                else if( param.equals( "l" ) )
                    maskColors[L] = valI;
                else if( param.equals( "r" ) )
                    maskColors[R] = valI;
                else if( param.equals( "z" ) )
                    maskColors[Z] = valI;
                else if( param.equals( "start" ) )
                    maskColors[Start] = valI;
                else if( param.equals( "leftup" ) )
                    maskColors[LeftUp] = valI;
                else if( param.equals( "up" ) )
                    maskColors[Up] = valI;
                else if( param.equals( "upright" ) )
                    maskColors[UpRight] = valI;
                else if( param.equals( "right" ) )
                    maskColors[Right] = valI;
                else if( param.equals( "rightdown" ) )
                    maskColors[RightDown] = valI;
                else if( param.equals( "leftdown" ) )
                    maskColors[LeftDown] = valI;
                else if( param.equals( "down" ) )
                    maskColors[Down] = valI;
                else if( param.equals( "left" ) )
                    maskColors[Left] = valI;
                else if( param.contains( "scancode_" ) )
                {
                    try
                    {  // make sure a valid integer was used for the scancode
                        SDLButtonCodes[SDLButtonCount] = Integer.valueOf( param.substring( 9, param.length() ) ).intValue();
                        SDLButtonMaskColors[SDLButtonCount] = valI;
                        SDLButtonCount++;
                    }
                    catch( NumberFormatException nfe )
                    {}  // skip it if this happens
                }
            }
        }
        Set<String> mKeys = pad_ini.keySet();
        Iterator<String> mIter = mKeys.iterator();
        while( mIter.hasNext() )
        {   // Loop through all the sections
            filename = mIter.next();  // the rest of the sections are filenames
            if( filename != null && filename.length() > 0 &&
                !filename.equals( "INFO" ) && !filename.equals( "MASK_COLOR" ) &&
                !filename.equals( "[<sectionless!>]" ) )
            {  // yep, its definitely a filename
                section = pad_ini.get( filename );
                if( section != null )
                {  // process the parameters for this section
                    val = section.get( "info" );  // what type of control
                    if( val != null )
                    {
                        val = val.toLowerCase();  // lets not make this part case-sensitive
                        if( val.contains( "analog" ) )
                        {  // Analog color mask image in BMP image format (doesn't actually get drawn)
                            analogMask = new Image( resources, Globals.DataDir + "/skins/touchpads/" +
                                                     skin + "/" + filename + ".bmp" );
                            // Position (percentages of the screen dimensions):
                            analogXpercent = toInt( section.get( "x" ), 0 );
                            analogYpercent = toInt( section.get( "y" ), 0 );
                            // Sensitivity (percentages of the radius, i.e. half the image width):
                            analogDeadzone = (int) ( (float) analogMask.hWidth * 
                                                             ( toFloat( section.get( "min" ), 1 ) / 100.0f ) );
                            analogMaximum = (int) ( (float) analogMask.hWidth *
                                                            ( toFloat( section.get( "max" ), 55 ) / 100.0f ) );
                            analogPadding = (int) ( (float) analogMask.hWidth *
                                                            ( toFloat( section.get( "buff" ), 55 ) / 100.0f ) );
                            analogMask.fitCenter( (int) ( (float) PAD_WIDTH * ((float) analogXpercent / 100.0f) ),
                                                  (int) ( (float) PAD_HEIGHT * ((float) analogYpercent / 100.0f) ),
                                                  PAD_WIDTH, PAD_HEIGHT );
                        }
                        else
                        {  // A button control (may contain one or more N64 buttons and/or SDL buttons)
                            // Button color mask image in BMP image format (doesn't actually get drawn)
                            masks[buttonCount] = new Image( resources, Globals.DataDir + "/skins/touchpads/" +
                                                            skin + "/" + filename + ".bmp" );
                            // Position (percentages of the screen dimensions):
                            xpercents[buttonCount] = toInt( section.get( "x" ), 0 );
                            ypercents[buttonCount] = toInt( section.get( "y" ), 0 );
                            masks[buttonCount].fitCenter( (int) ( (float) PAD_WIDTH * ((float) xpercents[buttonCount] / 100.0f) ),
                                                          (int) ( (float) PAD_HEIGHT * ((float) ypercents[buttonCount] / 100.0f) ),
                                                          PAD_WIDTH, PAD_HEIGHT );

Log.v( "TouchPad", "Adding button grouping " + buttonCount + ", (" + xpercents[buttonCount] + ", " + ypercents[buttonCount] + ")" );
Log.v( "TouchPad", "Fit x center to " + (int) ( (float) PAD_WIDTH * ((float) xpercents[buttonCount] / 100.0f) ) );
Log.v( "TouchPad", "Fit y center to " + (int) ( (float) PAD_HEIGHT * ((float) ypercents[buttonCount] / 100.0f) ) );
Log.v( "TouchPad", "Converted max coordinates: (" + masks[buttonCount].x + ", " + masks[buttonCount].y + ")" );

                            buttonCount++;
                        }
                    }
                }
            }
        }
        // Free the data that was loaded from the config file:
        pad_ini.clear();
        pad_ini = null;
        
        initialized = true;  // everything is loaded now
    }
    /*
     * Determines if the two speciied line segments intersect with each other, and calculates
     * where the intersection occurs if they do.
     * @param seg1pt1_x X-coordinate for the first end of the first line segment.
     * @param seg1pt1_y Y-coordinate for the first end of the first line segment.
     * @param seg1pt2_x X-coordinate for the second end of the first line segment.
     * @param seg1pt2_y Y-coordinate for the second end of the first line segment.
     * @param seg2pt1_x X-coordinate for the first end of the second line segment.
     * @param seg2pt1_y Y-coordinate for the first end of the second line segment.
     * @param seg2pt2_x X-coordinate for the second end of the second line segment.
     * @param seg2pt2_y Y-coordinate for the second end of the second line segment.
     * @param crossPt Changed to the point of intersection if there is one, otherwise unchanged.
     * @return True if the two line segments intersect.
     */
    public static boolean segsCross( float seg1pt1_x, float seg1pt1_y, float seg1pt2_x, float seg1pt2_y,
                                     float seg2pt1_x, float seg2pt1_y, float seg2pt2_x, float seg2pt2_y,
                                     Point crossPt )
    {
        float vec1_x = seg1pt2_x - seg1pt1_x;
        float vec1_y = seg1pt2_y - seg1pt1_y;
        
        float vec2_x = seg2pt2_x - seg2pt1_x;
        float vec2_y = seg2pt2_y - seg2pt1_y;
        
        float div = (-vec2_x * vec1_y + vec1_x * vec2_y );
        if( div == 0 )
            return false;  // Segments don't cross
        
        float s = (-vec1_y * (seg1pt1_x - seg2pt1_x) + vec1_x * (seg1pt1_y - seg2pt1_y) ) / div;
        float t = ( vec2_x * (seg1pt1_y - seg2pt1_y) - vec2_y * (seg1pt1_x - seg2pt1_x) ) / div;
        
        if( s >= 0 && s < 1 && t >= 0 && t <= 1 )
        {
            crossPt.x = seg1pt1_x + (t * vec1_x);
            crossPt.y = seg1pt1_y + (t * vec1_y);
            return true;  // Segments cross, point of intersection stored in 'crossPt'
        }
        
        return false;  // Segments don't cross
    }
    /*
     * Converts a string into an integer.
     * @param val String containing the number to convert.
     * @param fail Value to use if unable to convert val to an integer.
     * @return The converted integer, or the specified value if unsucessful.
     */
    private static int toInt( String val, int fail )
    {
        if( val == null || val.length() < 1 )
            return fail;  // not a number
        try
        {
            return Integer.valueOf( val ).intValue();  // convert to integer
        }
        catch( NumberFormatException nfe )
        {}

        return fail;  // conversion failed
    }
    /*
     * Converts a string into a float.
     * @param val String containing the number to convert.
     * @param fail Value to use if unable to convert val to an float.
     * @return The converted float, or the specified value if unsucessful.
     */
    private static float toFloat( String val, float fail )
    {
        if( val == null || val.length() < 1 )
            return fail;  // not a number
        try
        {
            return Float.valueOf( val ).floatValue();  // convert to float
        }
        catch( NumberFormatException nfe )
        {}

        return fail;  // conversion failed
    }

    /**
     * The Point class is a basic interface for storing 2D float coordinates.
     */
    private static class Point
    {
        public float x;
        public float y;
        /*
         * Constructor: Creates a new point at the origin
         */
        public Point()
        {
            x = 0;
            y = 0;
        }
    }

    /**
     * The TouchPadListing class reads in the listing of touchpads from touchpad_list.ini.
     */
    public static class TouchPadListing
    {
        public int numPads = 0;
        public String[] padNames = new String[256];

        /*
         * Constructor: Reads in the list of touchpads
         * @param filename File containing the list of touchpads (typicaly touchpad_list.ini).
         */
        public TouchPadListing( String filename )
        {
            try
            {
                FileInputStream fstream = new FileInputStream( filename );
                DataInputStream in = new DataInputStream( fstream );
                BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
                String strLine;
                int c = 0;
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

    /**
     * The Image class provides a simple interface to common image manipulation methods.
     */
    private class Image
    {
        public BitmapDrawable drawable = null;
        public Bitmap image = null;
        public Rect drawRect = null;

        public int x = 0;
        public int y = 0;
        public int width = 0;
        public int height = 0;
        public int hWidth = 0;
        public int hHeight = 0;

        /*
         * Constructor: Loads an image file and sets the initial properties.
         * @param res Handle to the app resources.
         * @param filename Path to the image file.
         */
        public Image( Resources res, String filename )
        {
            image = BitmapFactory.decodeFile( filename );
            drawable = new BitmapDrawable( res, image );
            if( image != null )
                width = image.getWidth();
            hWidth = (int) ((float)width / 2.0f);
            if( image != null )
                height = image.getHeight();
            hHeight = (int) ((float)height / 2.0f);
            drawRect = new Rect();
        }
        /*
         * Constructor: Creates a clone copy of another Image.
         * @param res Handle to the app resources.
         * @param clone Image to copy.
         */
        public Image( Resources res, Image clone )
        {
            image = clone.image;
            drawable = new BitmapDrawable( res, image );
            width = clone.width;
            hWidth = clone.hWidth;
            height = clone.height;
            hHeight = clone.hHeight;
            drawRect = new Rect();
        }
        /*
         * Sets the screen position of the image (in pixels).
         * @param x X-coordinate.
         * @param y Y-coordinate.
         */
        public void setPos( int x, int y )
        {
            this.x = x;
            this.y = y;
            drawRect.set( x, y, x + width, y + height );
            drawable.setBounds( drawRect );
        }
        /*
         * Centers the image at the specified coordinates, without going beyond the
         * specifed screen dimensions.
         * @param centerX X-coordinate to center the image at.
         * @param centerY Y-coordinate to center the image at.
         * @param screenW Horizontal screen dimension (in pixels).
         * @param centerY Vertical screen dimension (in pixels).
         */
        public void fitCenter( int centerX, int centerY, int screenW, int screenH )
        {
            int cx = centerX;
            int cy = centerY;
            if( cx < hWidth )
                cx = hWidth;
            if( cy < hHeight )
                cy = hHeight;
            if( cx + hWidth > screenW )
                cx = screenW - hWidth;
            if( cy + hHeight > screenH )
                cy = screenH - hHeight;
            x = cx - hWidth;
            y = cy - hHeight;
            drawRect.set( x, y, x + width, y + height );
            drawable.setBounds( drawRect );
        }
        /*
         * Centers the image at the specified coordinates, without going beyond the
         * edges of the specifed rectangle.
         * @param centerX X-coordinate to center the image at.
         * @param centerY Y-coordinate to center the image at.
         * @param rectX X-coordinate of the bounding rectangle.
         * @param rectY Y-coordinate of the bounding rectangle.
         * @param rectW Horizontal bounding rectangle dimension (in pixels).
         * @param rectH Vertical bounding rectangle dimension (in pixels).
         */
        public void fitCenter( int centerX, int centerY, int rectX, int rectY, int rectW, int rectH )
        {
            int cx = centerX;
            int cy = centerY;
            if( cx < rectX + hWidth )
                cx = rectX + hWidth;
            if( cy < rectY + hHeight )
                cy = rectY + hHeight;
            if( cx + hWidth > rectX + rectW )
                cx = rectX + rectW - hWidth;
            if( cy + hHeight > rectY + rectH )
                cy = rectY + rectH - hHeight;
            x = cx - hWidth;
            y = cy - hHeight;
            drawRect.set( x, y, x + width, y + height );
            drawable.setBounds( drawRect );
        }
        /*
         * Draws the image.
         * @param canvas Canvas to draw the image on.
         */
        public void draw( Canvas canvas )
        {
            drawable.draw( canvas );
        }
    }
}
