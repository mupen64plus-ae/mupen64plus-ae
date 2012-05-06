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
 * The GamePad class handles the virtual game pad.
 *
 * @author: Paul Lamb
 * 
 * http://www.paulscode.com
 * 
 */
public class GamePad extends View 
{
    // Maximum number of buttons that a gamepad layout can have:
    public static final int MAX_BUTTONS = 30;

    public String name = "";
    public String version = "";
    public String about = "";
    public String author = "";
    public RedrawThread redrawThread = null;

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
    // Not standard mp64p buttons, but simulated here for better control of virtual gamepad:
    private static final int UpRight	= 14;
    private static final int RightDown	= 15;
    private static final int LeftDown	= 16;
    private static final int LeftUp	= 17;

    private Image analogImage = null;
    private int analogXpercent = 0;
    private int analogYpercent = 0;
    private Image hatImage = null;
    private int hatX = -1;
    private int hatY = -1;
    private int analogPadding = 32;
    private int analogDeadzone = 2;
    private int analogMaximum = 360;
    private int analogPid = -1;

    // All button images and associated mask images, including both 
    // normal N64 buttons and SDL buttons:
    private Image[] buttons = new Image[MAX_BUTTONS];
    private Image[] masks = new Image[MAX_BUTTONS];
    private int[] xpercents = new int[MAX_BUTTONS];
    private int[] ypercents = new int[MAX_BUTTONS];

    private int buttonCount = 0;  // total number of buttons
    private int SDLButtonCount = 0;  // number of SDL buttons

    private Image fpsImage = null;
    private int fpsXpercent = 0;
    private int fpsYpercent = 0;
    private int fpsNumXpercent = 50;
    private int fpsNumYpercent = 50;
    public int fpsRate = 15;
    private String fpsFont = "Mupen64Plus-AE-Contrast-Blue";
    private int fpsValue = 0;
    private Image[] fpsDigits = new Image[4];

    private boolean drawEverything = true;
    private boolean drawHat = true;
    private boolean drawFPS = true;
    private Image[] numberImages = new Image[10];

    private Resources resources = null;
    boolean initialized = false;
    int canvasW = 0;
    int canvasH = 0;

    /**
     * Constructor: Instantiates the game pad
     * @param context Handle to the app context.
     * @param attribs Handle to the app attribute set.
     */
    public GamePad( Context context, AttributeSet attribs )
    {
        super( context, attribs );
        if( Config.gui_cfg == null )
            return;
/*        String oct = Config.gui_cfg.get( "GAME_PAD", "analogAsOctagon" );
        if( oct != null && oct.equals( "1" ) )
            analogAsOctagon = true;*/
    }
    
    /**
     * Provides a handle to the resources used to access Images
     * @param resources Handle to the app resources.
     */
    public void setResources( Resources resources )
    {
        this.resources = resources;
        for( int x = 0; x < 10; x++ )
            numberImages[x] = new Image( resources, Globals.DataDir + "/skins/fonts/" +
                                         fpsFont + "/" + x + ".png" );
    }
    
    /**
     * Updates the FPS indicator with a new value.
     * @param fps current FPS
     */
    public void updateFPS( int fps )
    {
        if( fpsValue == fps )
            return;  // No need to redraw if it hasn't changed
        fpsValue = fps;
        if( fpsValue < 0 )
            fpsValue = 0;  // Can't have a negative FPS (what is this, time travel?)
        if( fpsValue > 9999 )
            fpsValue = 9999;  // No more than 4 digits
        if( !MenuSkinsGamepadActivity.showFPS )
            return;
        String fpsString = Integer.toString( fpsValue );
        for( int x = 0; x < 4; x++ )
        {  // Create a new sequence of number digit images
            if( x < fpsString.length() )
            {
                try
                {  // Clone the digit from the font images
                    fpsDigits[x] = new Image( resources, numberImages[Integer.valueOf( fpsString.substring( x, x+1 ) )] );
                }
                catch( NumberFormatException nfe )
                {  // Skip this digit, there was a problem
                    fpsDigits[x] = null;
                }
            }
            else
            {  // Skip this digit
                fpsDigits[x] = null;
            }
        }
        drawFPS = true;
        if( redrawThread != null )
            redrawThread.redrawFPS = true;  // Tell thread it's time to redraw the FPS image
    }

    /**
     * Draws only what needs to be redrawn.
     * @param canvas Canvas to draw the gamepad on
     */
    @Override
    protected void onDraw( Canvas canvas )
    {
        if( !initialized )
            return;
        if( MenuSkinsGamepadActivity.redrawAll )
            drawEverything = true;
        if( drawEverything )
        {  // Redraw the entire gamepad
            int x;
            if( canvas.getWidth() != canvasW || canvas.getHeight() != canvasH )
            {  // Canvas changed its dimensions, recalculate the control positions
                canvasW = canvas.getWidth();
                canvasH = canvas.getHeight();
                if( analogImage != null )
                {  // Position the analog control
                    analogImage.fitCenter( (int)((float) canvasW * ((float) analogXpercent / 100.0f)),
                                           (int)((float) canvasH * ((float) analogYpercent / 100.0f)), canvasW, canvasH );
                }
                for( x = 0; x < buttonCount; x++ )
                {  // Position the buttons
                    buttons[x].fitCenter( (int)((float) canvasW * ((float) xpercents[x] / 100.0f)),
                                          (int)((float) canvasH * ((float) ypercents[x] / 100.0f)), canvasW, canvasH );
                    masks[x].fitCenter( (int)((float) canvasW * ((float) xpercents[x] / 100.0f)),
                                        (int)((float) canvasH * ((float) ypercents[x] / 100.0f)), canvasW, canvasH );
                }
            }
            for( x = 0; x < buttonCount; x++ )
            {  // Draw the buttons onto the canvas
                buttons[x].draw( canvas );
            }
        }
        if( drawEverything || drawHat )
        {  // Redraw the analog stick
            if( analogImage != null )
                analogImage.draw( canvas );  // Draw the background image first
            if( hatImage != null && analogImage != null )
            {  // Reposition the image and draw it
                int hX = hatX;
                int hY = hatY;
                if( hX == -1 )
                    hX = analogImage.hWidth;
                if( hY == -1 )
                    hY = analogImage.hHeight;
                hatImage.fitCenter( analogImage.x + hX, analogImage.y + hY, analogImage.x, analogImage.y,
                                    analogImage.width, analogImage.height );
                hatImage.draw( canvas );
            }
        }
        if( MenuSkinsGamepadActivity.showFPS && (drawEverything || drawFPS) )
        {  // Redraw the FPS indicator
            int x = 0;
            int y = 0;
            if( fpsImage != null )
            {  // Position the backtround image and draw it
                fpsImage.fitCenter( (int)((float) canvasW * ((float) fpsXpercent / 100.0f)),
                                    (int)((float) canvasH * ((float) fpsYpercent / 100.0f)), canvasW, canvasH );
                x = fpsImage.x + (int) ((float) fpsImage.width * ((float) fpsNumXpercent / 100.0f));
                y = fpsImage.y + (int) ((float) fpsImage.height * ((float) fpsNumYpercent / 100.0f));
                fpsImage.draw( canvas );
            }
            
            int totalWidth = 0;
            int c;
            // Calculate the width and postion of the FPS number:
            for( c = 0; c < 4; c++ )
                if( fpsDigits[c] != null )
                    totalWidth += fpsDigits[c].width;
            x = x - (int) ((float) totalWidth / 2.0f);
            for( c = 0; c < 4; c++ )
            {  // draw each digit of the FPS number
                if( fpsDigits[c] != null )
                {
                    fpsDigits[c].setPos( x, y - fpsDigits[c].hHeight );
                    fpsDigits[c].draw( canvas );
                    x += fpsDigits[c].width;
                }
            }
        }
        drawEverything = false;
        drawHat = false;
        drawFPS = false;
    }

    /**
     * Determines which controls are pressed based on where the screen is being touched.
     * @param pointers Array indicating which pointers are touching the screen.
     * @param pointerX Array containing the X-coordinate of each pointer.
     * @param pointerY Array containing the Y-coordinate of each pointer.
     * @param maxPid Maximum ID of the pointers that have changed (speed optimization)
     */
    protected void updatePointers( boolean[] pointers, int[] pointerX, int[] pointerY, int maxPid )
    {
        if( !initialized )
            return;

        int i, x, y, m, c, rgb;
        float d, p, dX, dY;
        // Clear any previous pointer data:
        int axisX = 0;
        int axisY = 0;
        int prevHatX = hatX;
        int prevHatY = hatY;
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
        {  // Process each pointer in sequence
            if( i == analogPid && !pointers[i] )
                analogPid = -1;  // Release analog if it's pointer is not touching the screen
            if( pointers[i] )
            {  // Pointer is touching the screen
                x = pointerX[i];
                y = pointerY[i];
                
//Log.v( "GamePad.java", "pointer " + i + "(" + x + "," + y + ")  [analogPid=" + analogPid + "]" );
                if( i != analogPid )
                {  // Not the analog control, check the buttons
                    for( m = 0; m < buttonCount; m++ )
                    {  // Check each one in sequence
                        if( x >= masks[m].x && x < masks[m].x + masks[m].width &&
                            y >= masks[m].y && y < masks[m].y + masks[m].height )
                        {  // it is inside this button, check the color mask
                            c = masks[m].image.getPixel( x - masks[m].x, y - masks[m].y );
//Log.v( "GamePad.java", "    pointer is inside a mask image, c=" + c );
                            rgb = (int)(c & 0x00ffffff);  // Ignore the alpha component if any
                            if( rgb > 0 )  // Ignore black
                                pressColor( rgb );  // Determine what was pressed
                        }
                    }
                }
                if( analogImage != null )
                {
                    dX = (float)( x - (analogImage.x + analogImage.hWidth) );   // Distance from center along x-axis
                    dY = (float)( (analogImage.y + analogImage.hHeight) - y );  // Distance from center along y-axis
                    d = FloatMath.sqrt( (dX * dX) + (dY * dY) );  // Distance from center
                    if( (i == analogPid) || (d >= analogDeadzone && d < analogMaximum + analogPadding) )
                    {  // Inside the analog control
                        if( MenuSkinsGamepadActivity.analogAsOctagon )
                        {  // Emulate the analog control as an octagon (like the real N64 controller)
                            Point crossPt = new Point();
                            float dC = analogImage.hWidth;
                            float dA = FloatMath.sqrt( (dC * dC) / 2.0f );
                        
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
                            d = FloatMath.sqrt( (dX * dX) + (dY * dY) );  // Distance from center
                        }
                        analogPid = i;  // "Capture" the analog control
                        touchedAnalog = true;
                        hatX = x - analogImage.x;
                        hatY = y - analogImage.y;

                        p = (d - (float)analogDeadzone) / (float)(analogMaximum - analogDeadzone);  // Percentage of full-throttle
                        if( p < 0 )
                            p = 0;
                        if( p > 1 )
                            p = 1;
                        // From the N64 func ref: The 3D Stick data is of type signed char and in
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
//Log.v( "GamePad.java", "    pointer " + analogPid + " is attached to analog, converted axis: (" + axisX + "," + axisY + ")" );
                    }
                }
            }
        }
        if( (!touchedAnalog || maxPid == 0) && (prevHatX != -1 || prevHatY != -1) )
        {
            drawHat = true;
            if( redrawThread != null )
                redrawThread.redraw = true;
        }
        else if( prevHatX != hatX || prevHatY != hatY )
        {
            drawHat = true;
            if( redrawThread != null )
                redrawThread.redraw = true;
        }
        GameActivityCommon.updateVirtualGamePadStates( 0, mp64pButtons, axisX, axisY );  // TODO: implement multi-controller
        GameActivityCommon.updateSDLButtonStates( SDLButtonPressed, SDLButtonCodes, SDLButtonCount );
    }

    /**
     * Determines which button was pressed based on the closest mask color.
     * TODO: Android is not precise: the color is different than it should be!)
     * @param color Color of the pixel that the user pressed.
     */
    protected void pressColor( int color )
    {
//Log.v( "GamePad.java", "    pressColor called, color=" + color );
        int closestMatch = 0;  // start with the first N64 button
        int closestSDLButtonMatch = -1;  // disable this to start with
        int matchDif = Math.abs( maskColors[0] - color );
        int x, dif;
        
        for( x = 1; x < 18; x++ )
        {  // Go through the N64 button mask colors first
            dif = Math.abs( maskColors[x] - color );
            if( dif < matchDif )
            {  // This is a closer match
                closestMatch = x;
                matchDif = dif;
            }
        }
        
        for( x = 0; x < SDLButtonCount; x++ )
        {  // Now see if any of the SDL button mask colors are closer
            dif = Math.abs( SDLButtonMaskColors[x] - color );
            if( dif < matchDif )
            {  // This is a closer match
                closestSDLButtonMatch = x;
                matchDif = dif;
            }
        }

        if( closestSDLButtonMatch > -1 )
        {  // found an SDL button that matches the color
            SDLButtonPressed[closestSDLButtonMatch] = true;
//Log.v( "GamePad.java", "    SDL button pressed, index=" + closestSDLButtonMatch );
        }
        else
        {  // one of the N64 buttons matched the color
//Log.v( "GamePad.java", "    N64 button pressed, index=" + closestMatch );
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

    /**
     * Loads the specified gamepad skin
     * @param skin Name of the layout skin to load.
     */
    protected void loadPad( String skin )
    {
        initialized = false;  // Stop anything accessing settings while loading
        // Kill the FPS image redrawer and create a new one
        if( redrawThread != null )
        {
            redrawThread.alive = false;
            redrawThread.redraw = false;
            redrawThread.redrawFPS = false;
            try
            {
                redrawThread.join( 500 );
            }
            catch( InterruptedException ie )
            {}
        }
        redrawThread = new RedrawThread();  // There is no "restart", so start fresh
        // Clear everything out to be re-populated with the new settings:
        name = "";
        version = "";
        about = "";
        author = "";
        analogImage = null;
        analogXpercent = 0;
        analogYpercent = 0;
        hatImage = null;
        hatX = -1;
        hatY = -1;
        buttons = new Image[MAX_BUTTONS];
        masks = new Image[MAX_BUTTONS];
        xpercents = new int[MAX_BUTTONS];
        ypercents = new int[MAX_BUTTONS];
        buttonCount = 0;
        SDLButtonCount = 0;
        fpsImage = null;
        fpsXpercent = 0;
        fpsYpercent = 0;
        fpsNumXpercent = 50;
        fpsNumYpercent = 50;
        fpsRate = 15;
        fpsFont = "Mupen64Plus-AE-Contrast-Blue";
        int xpercent = 0;
        int ypercent = 0;
        String filename;
        canvasW = 0;
        canvasH = 0;
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
            return;  // No skin was specified, so we are done.. quit
        // Load the configuration file (pad.ini):
        Config pad_ini = new Config( Globals.DataDir + "/skins/gamepads/" + skin + "/pad.ini" );

        // Look up the game-pad layout credits:
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
                param = param.toLowerCase();  // Lets not make this part case-sensitive
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
                    {  // Make sure a valid integer was used for the scancode
                        SDLButtonCodes[SDLButtonCount] = Integer.valueOf( param.substring( 9, param.length() ) );
                        SDLButtonMaskColors[SDLButtonCount] = valI;
                        SDLButtonCount++;
                    }
                    catch( NumberFormatException nfe )
                    {}  // Skip it if this happens
                }
            }
        }
        Set<String> mKeys = pad_ini.keySet();
        for( String mKey : mKeys )  // Loop through all the sections
        {  
            filename = mKey;  // the rest of the sections are filenames
            if( filename != null && filename.length() > 0 &&
                    !filename.equals( "INFO" ) && !filename.equals( "MASK_COLOR" ) &&
                    !filename.equals( "[<sectionless!>]" ) )
            {  
                // Yep, its definitely a filename
                section = pad_ini.get(filename);
                if( section != null )
                {  
                    // Process the parameters for this section
                    val = section.get( "info" );  // What type of control
                    if( val != null )
                    {
                        val = val.toLowerCase();  // Lets not make this part case-sensitive
                        if( val.contains( "analog" ) )
                        {  // Analog control (PNG image format)
                            analogImage = new Image( resources, Globals.DataDir + "/skins/gamepads/" +
                                                     skin + "/" + filename + ".png" );
                            if( val.contains( "hat" ) )
                            {  // There's a "stick" image.. same name, with "_2" appended:
                                hatImage = new Image( resources, Globals.DataDir + "/skins/gamepads/" +
                                                      skin + "/" + filename + "_2.png" );
                                // Create the thread for redrawing the "stick" when it moves:
                                redrawThread.redraw = false;
                                redrawThread.alive = true;
                                try
                                {
                                    redrawThread.start();  // Fire that thread up!
                                }
                                catch( IllegalThreadStateException itse )
                                {}  // Problem.. the "stick" is not going to redraw
                            }
                            // Position (percentages of the screen dimensions):
                            analogXpercent = toInt( section.get( "x" ), 0 );
                            analogYpercent = toInt( section.get( "y" ), 0 );
                            // Sensitivity (percentages of the radius, i.e. half the image width):
                            analogDeadzone = (int) ( (float) analogImage.hWidth * 
                                                             ( toFloat( section.get( "min" ), 1 ) / 100.0f ) );
                            analogMaximum = (int) ( (float) analogImage.hWidth *
                                                            ( toFloat( section.get( "max" ), 55 ) / 100.0f ) );
                            analogPadding = (int) ( (float) analogImage.hWidth *
                                                            ( toFloat( section.get( "buff" ), 55 ) / 100.0f ) );
                        }
                        else if( val.contains( "fps" ) )
                        {  // FPS indicator (PNG image format)
                            if( MenuSkinsGamepadActivity.showFPS )
                            {
                                fpsImage = new Image( resources, Globals.DataDir + "/skins/gamepads/" +
                                                      skin + "/" + filename + ".png" );
                                // Position (percentages of the screen dimensions):
                                fpsXpercent = toInt( section.get( "x" ), 0 );
                                fpsYpercent = toInt( section.get( "y" ), 0 );
                                // Number position (percentages of the FPS indicator dimensions):
                                fpsNumXpercent = toInt( section.get( "numx" ), 50 );
                                fpsNumYpercent = toInt( section.get( "numy" ), 50 );
                                // Refresh rate (in frames.. integer greater than 1):
                                fpsRate = toInt( section.get( "rate" ), 15 );
                                // Need at least 2 frames to calculate FPS (duh!):
                                if( fpsRate < 2 )
                                    fpsRate = 2;
                                // Number font:
                                fpsFont = section.get( "font" );
                                if( fpsFont != null && fpsFont.length() > 0 )
                                {  // Load the font images
                                    int x = 0;
                                    try
                                    {  // Make sure we can actually load them (they might not even exist)
                                        for( x = 0; x < 10; x++ )
                                            numberImages[x] = new Image( resources, Globals.DataDir + "/skins/fonts/" +
                                                                         fpsFont + "/" + x + ".png" );
                                    }
                                    catch( Exception e )
                                    {  // Problem, let the user know
                                        Log.e( "GamePad", "Problem loading font '" + Globals.DataDir + "/skins/fonts/" +
                                               fpsFont + "/" + x + ".png', error message: " + e.getMessage() );
                                    }
                                }
                                // Create the thread for redrawing the FPS when it changes:
                                redrawThread.redrawFPS = false;
                                redrawThread.alive = true;
                                try
                                {
                                    redrawThread.start();   // go!
                                }
                                catch( IllegalThreadStateException itse )
                                {}  // problem.. the FPS is not going to refresh
                            }
                        }
                        else
                        {  // A button control (may contain one or more N64 buttons and/or SDL buttons)
                            // The drawable image is in PNG image format
                            // The color mask image is in BMP image format (doesn't actually get drawn)
                            buttons[buttonCount] = new Image( resources, Globals.DataDir + "/skins/gamepads/" +
                                                              skin + "/" + filename + ".png" );
                            masks[buttonCount] = new Image( resources, Globals.DataDir + "/skins/gamepads/" +
                                                            skin + "/" + filename + ".bmp" );
                            // Position (percentages of the screen dimensions):
                            xpercents[buttonCount] = toInt( section.get( "x" ), 0 );
                            ypercents[buttonCount] = toInt( section.get( "y" ), 0 );
                            buttonCount++;
                        }
                    }
                }
            }
        }
        // Free the data that was loaded from the config file:
        pad_ini.clear();
        pad_ini = null;
        
        initialized = true;     // Everything is loaded now
        drawEverything = true;  // Need to redraw everything, since it all changed
        invalidate();           // Let Android know it needs to redraw
    }
    
    /**
     * Determines if the two specified line segments intersect with each other, and calculates
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
    
    /**
     * Converts a string into an integer.
     * @param val String containing the number to convert.
     * @param fail Value to use if unable to convert val to an integer.
     * @return The converted integer, or the specified value if unsuccessful.
     */
    private static int toInt( String val, int fail )
    {
        if( val == null || val.length() < 1 )
            return fail;  // Not a number
        try
        {
            return Integer.valueOf( val );  // Convert to integer
        }
        catch( NumberFormatException nfe )
        {}

        return fail;  // Conversion failed
    }
    
    /**
     * Converts a string into a float.
     * @param val String containing the number to convert.
     * @param fail Value to use if unable to convert val to an float.
     * @return The converted float, or the specified value if unsuccessful.
     */
    private static float toFloat( String val, float fail )
    {
        if( val == null || val.length() < 1 )
            return fail;  // Not a number
        try
        {
            return Float.valueOf( val );  // Convert to float
        }
        catch( NumberFormatException nfe )
        {}

        return fail;  // Conversion failed
    }

    /**
     * The Point class is a basic interface for storing 2D float coordinates.
     */
    private static class Point
    {
        public float x;
        public float y;
        
        /**
         * Constructor: Creates a new point at the origin
         */
        public Point()
        {
            x = 0;
            y = 0;
        }
    }

    /**
     * The GamePadListing class reads in the listing of gamepads from gamepad_list.ini.
     */
    public static class GamePadListing
    {
        public int numPads = 0;
        public String[] padNames = new String[256];

        /**
         * Constructor: Reads in the list of gamepads
         * @param filename File containing the list of gamepads (typically gamepad_list.ini).
         */
        public GamePadListing( String filename )
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
                Log.e( "GamePad.GamePadListing", "Exception, error message: " + e.getMessage() );
            }
        }
    }

    /**
     * The Image class provides a simple interface to common image manipulation methods.
     */
    private static class Image
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

        /**
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
        
        /**
         * Constructor: Creates a clone copy of another Image.
         * @param res Handle to the app resources.
         * @param clone Image to copy.
         */
        public Image( Resources res, Image clone )
        {
            if( clone != null )
            {
                image = clone.image;
                drawable = new BitmapDrawable( res, image );
                width = clone.width;
                hWidth = clone.hWidth;
                height = clone.height;
                hHeight = clone.hHeight;
            }
            drawRect = new Rect();
        }
        
        /**
         * Sets the screen position of the image (in pixels).
         * @param x X-coordinate.
         * @param y Y-coordinate.
         */
        public void setPos( int x, int y )
        {
            this.x = x;
            this.y = y;
            if( drawRect != null )
                drawRect.set( x, y, x + width, y + height );
            if( drawable != null )
                drawable.setBounds( drawRect );
        }
        
        /**
         * Centers the image at the specified coordinates, without going beyond the
         * specified screen dimensions.
         * @param centerX X-coordinate to center the image at.
         * @param centerY Y-coordinate to center the image at.
         * @param screenW Horizontal screen dimension (in pixels).
         * @param screenH Vertical screen dimension (in pixels).
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
            if( drawRect != null )
            {
                drawRect.set( x, y, x + width, y + height );
                if( drawable != null )
                    drawable.setBounds( drawRect );
            }
        }
        
        /**
         * Centers the image at the specified coordinates, without going beyond the
         * edges of the specified rectangle.
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
            if( drawRect != null )
            {
                drawRect.set( x, y, x + width, y + height );
                if( drawable != null )
                    drawable.setBounds( drawRect );
            }
        }
        
        /**
         * Draws the image.
         * @param canvas Canvas to draw the image on.
         */
        public void draw( Canvas canvas )
        {
            if( drawable != null )
                drawable.draw( canvas );
        }
    }
    
    /**
     * The RedrawThread class handles periodic redraws of the analog stick and FPS indicator.
     */
    private class RedrawThread extends Thread
    {
        public boolean alive = true;
        public boolean redraw = false;
        public boolean redrawFPS = false;
        // Runnable for the analog stick:
        private Runnable redrawer = new Runnable()
                                    {
                                        int x1, y1, x2, y2;
                                        public void run()
                                        {
                                            if( MenuSkinsGamepadActivity.redrawAll )
                                            {
                                                invalidate();  // Redraw everything
                                            }
                                            else
                                            {
                                                x1 = analogImage.x;
                                                if( hatImage != null && hatImage.x < x1 )
                                                    x1 = hatImage.x;
                                                y1 = analogImage.y;
                                                if( hatImage != null && hatImage.y < y1 )
                                                    y1 = hatImage.y;
                                                x2 = analogImage.x + analogImage.width;
                                                if( hatImage != null && hatImage.x + hatImage.width > x2 )
                                                    x2 = hatImage.x + hatImage.width;
                                                y2 = analogImage.y + analogImage.height;
                                                if( hatImage != null && hatImage.y + hatImage.height > y2 )
                                                    y2 = hatImage.y + hatImage.height;
                                                invalidate( x1, y1, x2, y2 );  // Only redraw what has changed
                                            }
                                         }
                                     };
        // Runnable for the FPS indicator:
        private Runnable redrawerFPS = new Runnable()
                                    {
                                        int x1, y1, x2, y2;
                                        public void run()
                                        {
                                            if( MenuSkinsGamepadActivity.redrawAll )
                                            {
                                                invalidate();  // Redraw everything
                                            }
                                            else
                                            {
                                                x1 = fpsImage.x;
                                                y1 = fpsImage.y;
                                                x2 = fpsImage.x + fpsImage.width;
                                                y2 = fpsImage.y + fpsImage.height;
                                                invalidate( x1, y1, x2, y2 );  // Only redraw what has changed
                                            }
                                         }
                                     };
        /**
         * Main loop.  Periodically checks if redrawing is necessary.
         */
        public void run()
        {
            while( alive )
            {  // Shut down by setting alive=false from another thread
                if( redraw )
                {  // Need to redraw the analog stick
                    GameActivityCommon.mSingleton.runOnUiThread( redrawer );
                }
                redraw = false;  // So it doesn't keep on redrawing every time
                if( redrawFPS )
                {  // Need to redraw the FPS indicator
                    GameActivityCommon.mSingleton.runOnUiThread( redrawerFPS );
                }
                redrawFPS = false;  // So it doesn't keep redrawing every time
                // Sleep for a while, to save the CPU:
                try{ Thread.sleep( 100 ); } catch( InterruptedException ie ) {}
            }
        }
    }
}
