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
 * Authors: TODO
 */
package paulscode.android.mupen64plusae;

import java.util.Iterator;
import java.util.Set;

import paulscode.android.mupen64plusae.input.TouchscreenController;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.util.Utility;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.View;

/**
 * The GamePad class handles the virtual game pad.
 * 
 * @author: Paul Lamb
 * 
 *          http://www.paulscode.com
 * 
 */
public class TouchscreenView extends View
{
    // Maximum number of buttons that a gamepad layout can have:
    public static final int MAX_BUTTONS = 30;
    
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
    private RedrawThread redrawThread = null;
    
    private boolean[] mp64pButtons = new boolean[14];
    
    // Must be the same order as EButton listing in plugin.h! (input-sdl plug-in)
    private static final int Right = 0;
    private static final int Left = 1;
    private static final int Down = 2;
    private static final int Up = 3;
    private static final int Start = 4;
    private static final int Z = 5;
    private static final int B = 6;
    private static final int A = 7;
    private static final int CRight = 8;
    private static final int CLeft = 9;
    private static final int CDown = 10;
    private static final int CUp = 11;
    private static final int R = 12;
    private static final int L = 13;
    // Not standard mp64p buttons, but simulated here for better control of virtual gamepad:
    private static final int UpRight = 14;
    private static final int RightDown = 15;
    private static final int LeftDown = 16;
    private static final int LeftUp = 17;
    
    // Variables for drawing the analog stick
    private Utility.Image analogImage = null;
    private int analogXpercent = 0;
    private int analogYpercent = 0;
    private Utility.Image hatImage = null;
    private int hatX = -1;
    private int hatY = -1;
    private int analogPadding = 32;
    private int analogDeadzone = 2;
    private int analogMaximum = 360;
    private int analogPid = -1;
    
    // All button images and associated mask images, including both
    // normal N64 buttons and SDL buttons:
    private Utility.Image[] buttons = new Utility.Image[MAX_BUTTONS];
    private Utility.Image[] masks = new Utility.Image[MAX_BUTTONS];
    private int[] xpercents = new int[MAX_BUTTONS];
    private int[] ypercents = new int[MAX_BUTTONS];
    
    private int buttonCount = 0; // total number of buttons
    private int SDLButtonCount = 0; // number of SDL buttons
    
    private Utility.Image fpsImage = null;
    private int fpsXpercent = 0;
    private int fpsYpercent = 0;
    private int fpsNumXpercent = 50;
    private int fpsNumYpercent = 50;
    public int fpsRate = 15;
    private String fpsFont = "Mupen64Plus-AE-Contrast-Blue";
    private int fpsValue = 0;
    private Utility.Image[] fpsDigits = new Utility.Image[4];
    
    private boolean drawEverything = true;
    private boolean drawHat = true;
    private boolean drawFPS = true;
    private Utility.Image[] numberImages = new Utility.Image[10];
    
    private Resources resources = null;
    boolean initialized = false;
    int canvasW = 0;
    int canvasH = 0;
    
    /**
     * Constructor: Instantiates the game pad
     * 
     * @param context
     *            Handle to the app context.
     * @param attribs
     *            Handle to the app attribute set.
     */
    public TouchscreenView( Context context, AttributeSet attribs )
    {
        super( context, attribs );
    }
    
    /**
     * Provides a handle to the resources used to access Images
     * 
     * @param resources
     *            Handle to the app resources.
     */
    public void setResources( Resources resources )
    {
        this.resources = resources;
        for( int x = 0; x < 10; x++ )
            numberImages[x] = new Utility.Image( resources, Globals.paths.fontsDir + fpsFont + "/" + x
                    + ".png" );
    }
    
    /**
     * Updates the FPS indicator with a new value.
     * 
     * @param fps
     *            current FPS
     */
    public void updateFPS( int fps )
    {
        // Quick return if user has disabled FPS or it hasn't changed
        if( !Globals.userPrefs.isFrameRateEnabled || fpsValue == fps )
            return;
        
        // Clamp to positive, four digits max
        fpsValue = Math.max( Math.min( fps, 9999 ), 0 );
        
        String fpsString = Integer.toString( fpsValue );
        for( int x = 0; x < 4; x++ )
        {
            // Create a new sequence of number digit images
            if( x < fpsString.length() )
            {
                try
                {
                    // Clone the digit from the font images
                    fpsDigits[x] = new Utility.Image( resources, numberImages[Integer.valueOf( fpsString
                            .substring( x, x + 1 ) )] );
                }
                catch( NumberFormatException nfe )
                {
                    // Skip this digit, there was a problem
                    fpsDigits[x] = null;
                }
            }
            else
            {
                // Skip this digit
                fpsDigits[x] = null;
            }
        }
        drawFPS = true;
        if( redrawThread != null )
            redrawThread.redrawFPS = true; // Tell thread it's time to redraw the FPS image
    }
    
    /**
     * Draws only what needs to be redrawn.
     * 
     * @param canvas
     *            Canvas to draw the gamepad on
     */
    @Override
    protected void onDraw( Canvas canvas )
    {
        if( !initialized )
            return;
        if( Globals.userPrefs.isTouchscreenRedrawAll )
            drawEverything = true;
        if( drawEverything )
        {
            // Redraw the entire gamepad
            if( canvas.getWidth() != canvasW || canvas.getHeight() != canvasH )
            {
                // Canvas changed its dimensions, recalculate the control positions
                canvasW = canvas.getWidth();
                canvasH = canvas.getHeight();
                if( analogImage != null )
                {
                    // Position the analog control
                    analogImage.fitCenter(
                            (int) ( (float) canvasW * ( (float) analogXpercent / 100.0f ) ),
                            (int) ( (float) canvasH * ( (float) analogYpercent / 100.0f ) ),
                            canvasW, canvasH );
                }
                for( int x = 0; x < buttonCount; x++ )
                {
                    // Position the buttons
                    buttons[x].fitCenter(
                            (int) ( (float) canvasW * ( (float) xpercents[x] / 100.0f ) ),
                            (int) ( (float) canvasH * ( (float) ypercents[x] / 100.0f ) ), canvasW,
                            canvasH );
                    masks[x].fitCenter(
                            (int) ( (float) canvasW * ( (float) xpercents[x] / 100.0f ) ),
                            (int) ( (float) canvasH * ( (float) ypercents[x] / 100.0f ) ), canvasW,
                            canvasH );
                }
            }
            for( int x = 0; x < buttonCount; x++ )
            {
                // Draw the buttons onto the canvas
                buttons[x].draw( canvas );
            }
        }
        if( drawEverything || drawHat )
        {
            // Redraw the analog stick
            if( analogImage != null )
                analogImage.draw( canvas ); // Draw the background image first
            if( hatImage != null && analogImage != null )
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
        if( Globals.userPrefs.isFrameRateEnabled && ( drawEverything || drawFPS ) )
        {
            // Redraw the FPS indicator
            int x = 0;
            int y = 0;
            if( fpsImage != null )
            { // Position the backtround image and draw it
                fpsImage.fitCenter( (int) ( (float) canvasW * ( (float) fpsXpercent / 100.0f ) ),
                        (int) ( (float) canvasH * ( (float) fpsYpercent / 100.0f ) ), canvasW,
                        canvasH );
                x = fpsImage.x
                        + (int) ( (float) fpsImage.width * ( (float) fpsNumXpercent / 100.0f ) );
                y = fpsImage.y
                        + (int) ( (float) fpsImage.height * ( (float) fpsNumYpercent / 100.0f ) );
                fpsImage.draw( canvas );
            }
            
            int totalWidth = 0;
            
            // Calculate the width and position of the FPS number:
            for( int c = 0; c < 4; c++ )
            {
                if( fpsDigits[c] != null )
                    totalWidth += fpsDigits[c].width;
            }
            
            x = x - (int) ( (float) totalWidth / 2.0f );
            
            for( int c = 0; c < 4; c++ )
            {
                // draw each digit of the FPS number
                if( fpsDigits[c] != null )
                {
                    fpsDigits[c].setPos( x, y - fpsDigits[c].hHeight );
                    fpsDigits[c].draw( canvas );
                    x += fpsDigits[c].width;
                }
            }
        }
        
        // Reset the flags
        drawEverything = false;
        drawHat = false;
        drawFPS = false;
    }
    
    /**
     * Determines which controls are pressed based on where the screen is being touched.
     * 
     * @param pointers
     *            Array indicating which pointers are touching the screen.
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
        int prevHatX = hatX;
        int prevHatY = hatY;
        hatX = -1;
        hatY = -1;
        boolean touchedAnalog = false;
        // Clear any data about which buttons were pressed:
        for( int i = 0; i < 18; i++ )
            buttonPressed[i] = false;
        for( int i = 0; i < SDLButtonCount; i++ )
            SDLButtonPressed[i] = false;
        for( int i = 0; i < 14; i++ )
            mp64pButtons[i] = false;
        
        for( int i = 0; i <= maxPid; i++ )
        {
            // Process each pointer in sequence
            if( i == analogPid && !pointers[i] )
                analogPid = -1; // Release analog if its pointer is not touching the screen
            if( pointers[i] )
            {
                // Pointer is touching the screen
                x = pointerX[i];
                y = pointerY[i];
                
                if( i != analogPid )
                {
                    // Not the analog control, check the buttons
                    for( int m = 0; m < buttonCount; m++ )
                    {
                        // Check each one in sequence
                        if( x >= masks[m].x && x < masks[m].x + masks[m].width && y >= masks[m].y
                                && y < masks[m].y + masks[m].height )
                        {
                            // It is inside this button, check the color mask
                            c = masks[m].image.getPixel( x - masks[m].x, y - masks[m].y );
                            rgb = (int) ( c & 0x00ffffff ); // Ignore the alpha component if any
                            
                            // Ignore black and determine what was pressed
                            if( rgb > 0 )
                                pressColor( rgb );
                        }
                    }
                }
                if( analogImage != null )
                {
                    // Distance from center along x-axis
                    dX = (float) ( x - ( analogImage.x + analogImage.hWidth ) );
                    
                    // Distance from center along y-axis
                    dY = (float) ( ( analogImage.y + analogImage.hHeight ) - y );
                    
                    // Distance from center
                    d = FloatMath.sqrt( ( dX * dX ) + ( dY * dY ) );
                    
                    if( ( i == analogPid )
                            || ( d >= analogDeadzone && d < analogMaximum + analogPadding ) )
                    {
                        // Inside the analog control
                        if( Globals.userPrefs.isOctagonalJoystick )
                        {
                            // Emulate the analog control as an octagon (like the real N64
                            // controller)
                            Utility.Point crossPt = new Utility.Point();
                            float dC = analogImage.hWidth;
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
                            d = FloatMath.sqrt( ( dX * dX ) + ( dY * dY ) ); // Distance from center
                        }
                        
                        // "Capture" the analog control
                        analogPid = i;
                        touchedAnalog = true;
                        hatX = x - analogImage.x;
                        hatY = y - analogImage.y;
                        
                        // Percentage of full-throttle, clamped to range [0-1]
                        p = ( d - (float) analogDeadzone )
                                / (float) ( analogMaximum - analogDeadzone );
                        p = Math.max( Math.min( p, 1 ), 0 );
                        
                        // From the N64 function ref: The 3D Stick data is of type signed char and
                        // in the range between 80 and -80. (32768 / 409 = ~80.1)
                        axisX = (int) ( ( dX / d ) * p * 80.0f );
                        axisY = (int) ( ( dY / d ) * p * 80.0f );
                        axisX = Math.max( Math.min( axisX, 80 ), -80 );
                        axisY = Math.max( Math.min( axisY, 80 ), -80 );
                    }
                }
            }
        }
        if( ( !touchedAnalog || maxPid == 0 ) && ( prevHatX != -1 || prevHatY != -1 ) )
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
        
        // TODO: Implement multi-controller
        NativeMethods.updateVirtualGamePadStates( 0, mp64pButtons, axisX, axisY );
        TouchscreenController.updateSDLButtonStates( Globals.surfaceInstance, SDLButtonPressed,
                SDLButtonCodes, SDLButtonCount );
    }
    
    /**
     * Determines which button was pressed based on the closest mask color.
     * 
     * TODO: Android is not precise: the color is different than it should be!
     * 
     * @param color
     *            Color of the pixel that the user pressed.
     */
    protected void pressColor( int color )
    {
        // Log.v( "GamePad.java", "    pressColor called, color=" + color );
        int closestMatch = 0; // start with the first N64 button
        int closestSDLButtonMatch = -1; // disable this to start with
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
            // found an SDL button that matches the color
            SDLButtonPressed[closestSDLButtonMatch] = true;
            // Log.v( "GamePad.java", "    SDL button pressed, index=" + closestSDLButtonMatch );
        }
        else
        {
            // one of the N64 buttons matched the color
            // Log.v( "GamePad.java", "    N64 button pressed, index=" + closestMatch );
            buttonPressed[closestMatch] = true;
            if( closestMatch < 14 )
            {
                // only 14 buttons in Mupen64Plus API
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
     * Loads the touchscreen controller skin
     */
    protected void loadPad()
    {
        // Stop anything accessing settings while loading
        initialized = false;
        
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
            {
            }
        }
        
        // There is no "restart", so start fresh
        redrawThread = new RedrawThread();
        
        // Clear everything out to be re-populated with the new settings:
        analogImage = null;
        analogXpercent = 0;
        analogYpercent = 0;
        hatImage = null;
        hatX = -1;
        hatY = -1;
        buttons = new Utility.Image[MAX_BUTTONS];
        masks = new Utility.Image[MAX_BUTTONS];
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
        
        String filename;
        canvasW = 0;
        canvasH = 0;
        
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
            mp64pButtons[i] = false;
        
        // If no skin to display, quit TODO: why isn't this at the top of the function?
        if( !Globals.userPrefs.isTouchscreenEnabled && !Globals.userPrefs.isFrameRateEnabled )
            return;
        
        // Load the configuration file (pad.ini):
        final String layoutFolder = Globals.userPrefs.touchscreenLayoutFolder;
        ConfigFile pad_ini = new ConfigFile( layoutFolder + "/pad.ini" );
        
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
            while( iter.hasNext() )
            {
                // Loop through the param=val pairs
                param = iter.next();
                val = section.get( param );
                valI = Utility.toInt( val, -1 ); // -1 (undefined) in case of number format problem
                param = param.toLowerCase(); // Lets not make this part case-sensitive
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
                    {
                        // Make sure a valid integer was used for the scancode
                        SDLButtonCodes[SDLButtonCount] = Integer.valueOf( param.substring( 9,
                                param.length() ) );
                        SDLButtonMaskColors[SDLButtonCount] = valI;
                        SDLButtonCount++;
                    }
                    catch( NumberFormatException nfe )
                    {
                    } // Skip it if this happens
                }
            }
        }
        Set<String> mKeys = pad_ini.keySet();
        
        // Loop through all the sections
        for( String mKey : mKeys )
        {
            filename = mKey; // the rest of the sections are filenames
            if( filename != null && filename.length() > 0 && !filename.equals( "INFO" )
                    && !filename.equals( "MASK_COLOR" ) && !filename.equals( "[<sectionless!>]" ) )
            {
                // Yep, its definitely a filename
                section = pad_ini.get( filename );
                if( section != null )
                {
                    // Process the parameters for this section
                    val = section.get( "info" ); // What type of control
                    if( val != null )
                    {
                        val = val.toLowerCase(); // Lets not make this part case-sensitive
                        if( val.contains( "analog" ) )
                        {
                            // Analog control (PNG image format)
                            analogImage = new Utility.Image( resources, layoutFolder + "/" + filename
                                    + ".png" );
                            if( val.contains( "hat" ) )
                            {
                                // There's a "stick" image.. same name, with "_2" appended:
                                hatImage = new Utility.Image( resources, layoutFolder + "/" + filename
                                        + "_2.png" );
                                // Create the thread for redrawing the "stick" when it moves:
                                redrawThread.redraw = false;
                                redrawThread.alive = true;
                                try
                                {
                                    redrawThread.start(); // Fire that thread up!
                                }
                                catch( IllegalThreadStateException itse )
                                {
                                } // Problem.. the "stick" is not going to redraw
                            }
                            
                            // Position (percentages of the screen dimensions):
                            analogXpercent = Utility.toInt( section.get( "x" ), 0 );
                            analogYpercent = Utility.toInt( section.get( "y" ), 0 );
                            
                            // Sensitivity (percentages of the radius, i.e. half the image width):
                            analogDeadzone = (int) ( (float) analogImage.hWidth * ( Utility
                                    .toFloat( section.get( "min" ), 1 ) / 100.0f ) );
                            analogMaximum = (int) ( (float) analogImage.hWidth * ( Utility.toFloat(
                                    section.get( "max" ), 55 ) / 100.0f ) );
                            analogPadding = (int) ( (float) analogImage.hWidth * ( Utility.toFloat(
                                    section.get( "buff" ), 55 ) / 100.0f ) );
                        }
                        else if( val.contains( "fps" ) )
                        {
                            // FPS indicator (PNG image format)
                            if( Globals.userPrefs.isFrameRateEnabled )
                            {
                                fpsImage = new Utility.Image( resources, layoutFolder + "/" + filename
                                        + ".png" );
                                
                                // Position (percentages of the screen dimensions):
                                fpsXpercent = Utility.toInt( section.get( "x" ), 0 );
                                fpsYpercent = Utility.toInt( section.get( "y" ), 0 );
                                
                                // Number position (percentages of the FPS indicator dimensions):
                                fpsNumXpercent = Utility.toInt( section.get( "numx" ), 50 );
                                fpsNumYpercent = Utility.toInt( section.get( "numy" ), 50 );
                                
                                // Refresh rate (in frames.. integer greater than 1):
                                fpsRate = Utility.toInt( section.get( "rate" ), 15 );
                                
                                // Need at least 2 frames to calculate FPS (duh!):
                                if( fpsRate < 2 )
                                    fpsRate = 2;
                                
                                // Number font:
                                fpsFont = section.get( "font" );
                                if( fpsFont != null && fpsFont.length() > 0 )
                                {
                                    // Load the font images
                                    int x = 0;
                                    try
                                    {
                                        // Make sure we can load them (they might not even exist)
                                        for( x = 0; x < 10; x++ )
                                            numberImages[x] = new Utility.Image( resources,
                                                    Globals.paths.fontsDir + fpsFont + "/" + x
                                                            + ".png" );
                                    }
                                    catch( Exception e )
                                    {
                                        // Problem, let the user know
                                        Log.e( "GamePad", "Problem loading font '"
                                                + Globals.paths.fontsDir + fpsFont + "/" + x
                                                + ".png', error message: " + e.getMessage() );
                                    }
                                }
                                
                                // Create the thread for redrawing the FPS when it changes:
                                redrawThread.redrawFPS = false;
                                redrawThread.alive = true;
                                try
                                {
                                    redrawThread.start(); // go!
                                }
                                catch( IllegalThreadStateException itse )
                                {
                                } // problem.. the FPS is not going to refresh
                            }
                        }
                        else
                        {
                            // A button control (may contain one or more N64 buttons and/or SDL
                            // buttons). The drawable image is in PNG image format. The color mask
                            // image is in BMP image format (doesn't actually get drawn).
                            buttons[buttonCount] = new Utility.Image( resources, layoutFolder + "/"
                                    + filename + ".png" );
                            masks[buttonCount] = new Utility.Image( resources, layoutFolder + "/"
                                    + filename + ".bmp" );
                            
                            // Position (percentages of the screen dimensions):
                            xpercents[buttonCount] = Utility.toInt( section.get( "x" ), 0 );
                            ypercents[buttonCount] = Utility.toInt( section.get( "y" ), 0 );
                            buttonCount++;
                        }
                    }
                }
            }
        }
        
        // Free the data that was loaded from the config file:
        pad_ini.clear();
        pad_ini = null;
        
        initialized = true; // Everything is loaded now
        drawEverything = true; // Need to redraw everything, since it all changed
        invalidate(); // Let Android know it needs to redraw
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
            
        float s = ( -vec1_y * ( seg1pt1_x - seg2pt1_x ) + vec1_x * ( seg1pt1_y - seg2pt1_y ) )
                / div;
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
     * The RedrawThread class handles periodic redraws of the analog stick and FPS indicator.
     */
    private class RedrawThread extends Thread
    {
        public boolean alive = true;
        public boolean redraw = false;
        public boolean redrawFPS = false;
        
        // Runnable for the analog stick
        private Runnable redrawer = new Runnable()
        {
            int x1, y1, x2, y2;
            
            public void run()
            {
                if( Globals.userPrefs.isTouchscreenRedrawAll )
                {
                    // Redraw everything
                    invalidate();
                }
                else
                {
                    // Only redraw what has changed
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
                    invalidate( x1, y1, x2, y2 );
                }
            }
        };
        
        // Runnable for the FPS indicator
        private Runnable redrawerFPS = new Runnable()
        {
            int x1, y1, x2, y2;
            
            public void run()
            {
                if( Globals.userPrefs.isTouchscreenRedrawAll )
                {
                    // Redraw everything
                    invalidate();
                }
                else
                {
                    // Only redraw what has changed
                    x1 = fpsImage.x;
                    y1 = fpsImage.y;
                    x2 = fpsImage.x + fpsImage.width;
                    y2 = fpsImage.y + fpsImage.height;
                    invalidate( x1, y1, x2, y2 );
                }
            }
        };
        
        /**
         * Main loop. Periodically checks if redrawing is necessary.
         */
        public void run()
        {
            final int millis = Globals.userPrefs.isTouchscreenRedrawAll
                    ? 150
                    : 100;
            
            while( alive )
            {
                // Shut down by setting alive=false from another thread
                if( redraw )
                {
                    // Need to redraw the analog stick
                    redraw = false;
                    Globals.gameInstance.runOnUiThread( redrawer );
                }
                
                if( redrawFPS )
                {
                    // Need to redraw the FPS indicator
                    redrawFPS = false;
                    Globals.gameInstance.runOnUiThread( redrawerFPS );
                }
                
                // Sleep for a while, to save the CPU:
                Utility.safeSleep( millis );
            }
        }
    }
}
