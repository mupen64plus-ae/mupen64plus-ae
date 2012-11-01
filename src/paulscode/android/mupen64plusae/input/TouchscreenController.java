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
 * Authors: paulscode, lioncash
 */
package paulscode.android.mupen64plusae.input;

import paulscode.android.mupen64plusae.Globals;
import paulscode.android.mupen64plusae.TouchscreenSkin;
import paulscode.android.mupen64plusae.util.Utility;
import android.annotation.TargetApi;
import android.os.Build;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class TouchscreenController extends AbstractController implements OnTouchListener
{
    public TouchscreenSkin mSkin;
    public int analogPid = -1;
    
    public TouchscreenController( View view )
    {
        view.setOnTouchListener( this );
    }
    
    @Override
    @TargetApi( 5 )
    public boolean onTouch( View view, MotionEvent event )
    {
        if( !Globals.userPrefs.isInputEnabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR )
            return false;
        
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        
        boolean[] pointerTouching = new boolean[256];
        int[] pointerX = new int[256];
        int[] pointerY = new int[256];
        
        int maxPid = 0;
        int pid;
        
        switch( actionCode )
        {
            case MotionEvent.ACTION_POINTER_DOWN:
                pid = event.getPointerId( action >> MotionEvent.ACTION_POINTER_INDEX_SHIFT );
                pointerTouching[pid] = true;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                pid = event.getPointerId( action >> MotionEvent.ACTION_POINTER_INDEX_SHIFT );
                pointerTouching[pid] = false;
                break;
            case MotionEvent.ACTION_DOWN:
                for( int i = 0; i < event.getPointerCount(); i++ )
                {
                    pid = event.getPointerId( i );
                    pointerTouching[pid] = true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                for( int i = 0; i < 256; i++ )
                {
                    pointerTouching[i] = false;
                    pointerX[i] = -1;
                    pointerY[i] = -1;
                }
                break;
            default:
                break;
        }
        
        for( int i = 0; i < event.getPointerCount(); i++ )
        {
            pid = event.getPointerId( i );
            if( pid > maxPid )
                maxPid = pid;
            if( pointerTouching[pid] )
            {
                pointerX[pid] = (int) event.getX( i );
                pointerY[pid] = (int) event.getY( i );
            }
        }
        refreshState( pointerTouching, pointerX, pointerY, maxPid );
        return true;
    }
    
    /**
     * Determines which controls are pressed based on where the screen is being touched.
     * 
     * @param pointerTouching Array indicating which pointers are touching the screen.
     * @param pointerX Array containing the X-coordinate of each pointer.
     * @param pointerY Array containing the Y-coordinate of each pointer.
     * @param maxPid Maximum ID of the pointers that have changed (speed optimization).
     */
    private void refreshState( boolean[] pointerTouching, int[] pointerX, int[] pointerY,
            int maxPid )
    {
        // Clear button/axis state using super method
        clearState();
        
        // Process each pointer in sequence
        for( int pid = 0; pid <= maxPid; pid++ )
        {
            // Release analog if its pointer is not touching the screen
            if( pid == analogPid && !pointerTouching[pid] )
                analogPid = -1;
            
            // Process touches
            if( pointerTouching[pid] )
            {
                if( pid != analogPid )
                {
                    // Process button inputs
                    processButtonTouch( pointerX[pid], pointerY[pid] );
                }
                if( mSkin.analogImage != null )
                {
                    // Process analog inputs
                    processAnalogTouch( pid, pointerX[pid], pointerY[pid] );
                }
            }
        }
        
        // Call the super method to push the input into the core
        notifyChanged();
    }
    
    private void processButtonTouch( int xLocation, int yLocation )
    {
        for( Utility.Image mask : mSkin.masks )
        {
            if( mask != null )
            {
                int left = mask.x;
                int right = left + mask.width;
                int bottom = mask.y;
                int top = bottom + mask.height;
                
                // Check each one in sequence
                if( xLocation >= left && xLocation < right && yLocation >= bottom && yLocation < top )
                {
                    // It is inside this button, check the color mask
                    int c = mask.image.getPixel( xLocation - mask.x, yLocation - mask.y );
                    
                    // Ignore the alpha component if any
                    int rgb = (int) ( c & 0x00ffffff );
                    
                    // Ignore black and modify the button states
                    if( rgb > 0 )
                        pressColor( rgb );
                }
            }
        }
    }

    /**
     * Presses a button based on mask color.
     * 
     * TODO: Android is not precise: the color is different than it should be!
     * 
     * @param color Color of the pixel that the user pressed.
     */
    private void pressColor( int color )
    {
        // Find the closest match among the N64 buttons
        int closestMatch = -1;
        int matchDif = Integer.MAX_VALUE;
        for( int i = 0; i < mSkin.maskColors.length; i++ )
        {
            int dif = Math.abs( mSkin.maskColors[i] - color );
            if( dif < matchDif )
            {
                // This is a closer match
                closestMatch = i;
                matchDif = dif;
            }
        }
        
        // Now set the button flags based on best match
        if( closestMatch > -1 )
        {
            // Ordinary N64 button(s) pressed, update the AbstractController fields
            if( closestMatch < AbstractController.NUM_BUTTONS )
            {
                // A single button was pressed
                mButtons[closestMatch] = true;
            }
            else
            {
                // Two d-pad buttons pressed simultaneously
                switch( closestMatch )
                {
                    case TouchscreenSkin.DPD_RU:
                        mButtons[DPD_R] = true;
                        mButtons[DPD_U] = true;
                        break;
                    case TouchscreenSkin.DPD_RD:
                        mButtons[DPD_R] = true;
                        mButtons[DPD_D] = true;
                        break;
                    case TouchscreenSkin.DPD_LD:
                        mButtons[DPD_L] = true;
                        mButtons[DPD_D] = true;
                        break;
                    case TouchscreenSkin.DPD_LU:
                        mButtons[DPD_L] = true;
                        mButtons[DPD_U] = true;
                        break;
                    default:
                        break;
                }
            }
        }
    }
    
    private void processAnalogTouch( int pointerId, int xLocation, int yLocation )
    {
        Utility.Image image = mSkin.analogImage;
        
        // Distance from center along x-axis
        float dX = (float) ( xLocation - ( image.x + image.hWidth ) );
        
        // Distance from center along y-axis
        float dY = (float) ( ( image.y + image.hHeight ) - yLocation );
        
        // Distance from center
        float d = FloatMath.sqrt( ( dX * dX ) + ( dY * dY ) );
        
        // "Capture" the analog control
        if( d >= mSkin.analogDeadzone && d < mSkin.analogMaximum + mSkin.analogPadding )
            analogPid = pointerId;
        
        if( pointerId == analogPid )
        {
            // User is controlling the analog stick
            if( Globals.userPrefs.isOctagonalJoystick )
            {
                // Limit range of motion to an octagon (like the real N64 controller)
                Utility.Point point = Utility.intersectOctagon( dX, dY, image.hWidth );
                dX = point.x;
                dY = point.y;
                d = FloatMath.sqrt( ( dX * dX ) + ( dY * dY ) );
            }
            
            // Percentage of full-throttle, clamped to range [0-1]
            float p = ( d - mSkin.analogDeadzone ) / ( mSkin.analogMaximum - mSkin.analogDeadzone );
            p = Math.max( Math.min( p, 1 ), 0 );
            
            // Store the axis values in the super fields
            mAxisFractionX = p * dX / d;
            mAxisFractionY = p * dY / d;
            
            // Update the skin to show the new position
            mSkin.updateAnalog( mAxisFractionX, mAxisFractionY );
        }
    }
}
