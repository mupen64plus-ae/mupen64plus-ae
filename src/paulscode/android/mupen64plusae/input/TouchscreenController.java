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
package paulscode.android.mupen64plusae.input;

import paulscode.android.mupen64plusae.Globals;
import paulscode.android.mupen64plusae.input.transform.TouchMap;
import paulscode.android.mupen64plusae.input.transform.VisibleTouchMap;
import android.annotation.TargetApi;
import android.graphics.Point;
import android.os.Build;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class TouchscreenController extends AbstractController implements OnTouchListener
{
    private static final int MAX_POINTER_IDS = 256;
    private boolean[] mPointerTouch = new boolean[MAX_POINTER_IDS];
    private int[] mPointerX = new int[MAX_POINTER_IDS];
    private int[] mPointerY = new int[MAX_POINTER_IDS];
    
    private VisibleTouchMap mTouchMap;
    private int analogPid = -1;
    
    public TouchscreenController( VisibleTouchMap touchMap, View view )
    {
        mTouchMap = touchMap;
        view.setOnTouchListener( this );
    }
    
    @Override
    @TargetApi( 5 )
    public boolean onTouch( View view, MotionEvent event )
    {
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR )
            return false;
        
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        
        int pid = -1;
        switch( actionCode )
        {
            case MotionEvent.ACTION_POINTER_DOWN:
                pid = event.getPointerId( action >> MotionEvent.ACTION_POINTER_INDEX_SHIFT );
                mPointerTouch[pid] = true;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                pid = event.getPointerId( action >> MotionEvent.ACTION_POINTER_INDEX_SHIFT );
                mPointerTouch[pid] = false;
                break;
            case MotionEvent.ACTION_DOWN:
                for( int i = 0; i < event.getPointerCount(); i++ )
                {
                    pid = event.getPointerId( i );
                    mPointerTouch[pid] = true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                for( int i = 0; i < MAX_POINTER_IDS; i++ )
                {
                    mPointerTouch[i] = false;
                    mPointerX[i] = -1;
                    mPointerY[i] = -1;
                }
                break;
            default:
                break;
        }
        
        // Record the pointer locations
        int maxPid = 0;
        for( int i = 0; i < event.getPointerCount(); i++ )
        {
            pid = event.getPointerId( i );
            if( pid > maxPid )
                maxPid = pid;
            if( mPointerTouch[pid] )
            {
                mPointerX[pid] = (int) event.getX( i );
                mPointerY[pid] = (int) event.getY( i );
            }
        }
        
        refreshState( mPointerTouch, mPointerX, mPointerY, maxPid );
        
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
    private void refreshState( boolean[] pointerTouching, int[] pointerX, int[] pointerY, int maxPid )
    {
        // Clear button/axis state using super method
        clearState();
        boolean analogMoved = false;
        
        // Process each pointer in sequence
        for( int pid = 0; pid <= maxPid; pid++ )
        {
            // Release analog if its pointer is not touching the screen
            if( pid == analogPid && !pointerTouching[pid] )
            {
                analogMoved = true;
                analogPid = -1;
            }
            
            // Process touches
            if( pointerTouching[pid] )
            {
                if( pid != analogPid )
                {
                    // Process button inputs
                    processButtonTouch( pointerX[pid], pointerY[pid] );
                }
                // Process analog inputs
                analogMoved = processAnalogTouch( pid, pointerX[pid], pointerY[pid] );
            }
        }
        
        // Call the super method to send the input to the core
        notifyChanged();
        
        // Update the skin if the virtual analog stick moved
        if( analogMoved )
            mTouchMap.updateAnalog( mAxisFractionX, mAxisFractionY );
    }
    
    private void processButtonTouch( int xLocation, int yLocation )
    {
        // Determine the index of the button that was pressed
        int index = mTouchMap.getButtonPress( xLocation, yLocation );
        
        // Set the button state if a button was actually touched
        if( index > -1 )
        {
            if( index < AbstractController.NUM_BUTTONS )
            {
                // A single button was pressed
                mButtons[index] = true;
            }
            else
            {
                // Two d-pad buttons pressed simultaneously
                switch( index )
                {
                    case TouchMap.DPD_RU:
                        mButtons[DPD_R] = true;
                        mButtons[DPD_U] = true;
                        break;
                    case TouchMap.DPD_RD:
                        mButtons[DPD_R] = true;
                        mButtons[DPD_D] = true;
                        break;
                    case TouchMap.DPD_LD:
                        mButtons[DPD_L] = true;
                        mButtons[DPD_D] = true;
                        break;
                    case TouchMap.DPD_LU:
                        mButtons[DPD_L] = true;
                        mButtons[DPD_U] = true;
                        break;
                    default:
                        break;
                }
            }
        }
    }
    
    private boolean processAnalogTouch( int pointerId, int xLocation, int yLocation )
    {
        // Get the cartesian displacement of the analog stick
        Point point = mTouchMap.getAnalogDisplacement( xLocation, yLocation );
        
        // Compute the pythagorean displacement of the stick
        int dX = point.x;
        int dY = point.y;
        float displacement = FloatMath.sqrt( ( dX * dX ) + ( dY * dY ) );
        
        // "Capture" the analog control
        if( mTouchMap.isAnalogCaptured( displacement ) )
            analogPid = pointerId;
        
        if( pointerId == analogPid )
        {
            // User is controlling the analog stick
            if( Globals.userPrefs.isOctagonalJoystick )
            {
                // Limit range of motion to an octagon (like the real N64 controller)
                point = mTouchMap.getConstrainedDisplacement( dX, dY );
                dX = point.x;
                dY = point.y;
                displacement = FloatMath.sqrt( ( dX * dX ) + ( dY * dY ) );
            }
            
            // Fraction of full-throttle, clamped to range [0-1]
            float p = mTouchMap.getAnalogStrength( displacement );
            
            // Store the axis values in the super fields (screen y is inverted)
            mAxisFractionX = p * (float) dX / displacement;
            mAxisFractionY = -p * (float) dY / displacement;
            
            // Analog state changed
            return true;
        }
        
        // Analog state did not change
        return false;
    }
}
