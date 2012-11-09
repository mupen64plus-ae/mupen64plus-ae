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
import paulscode.android.mupen64plusae.input.map.TouchMap;
import paulscode.android.mupen64plusae.input.map.VisibleTouchMap;
import android.annotation.TargetApi;
import android.graphics.Point;
import android.os.Build;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * A class for generating N64 controller commands from a touchscreen.
 */
public class TouchscreenController extends AbstractController implements OnTouchListener
{
    /** The maximum number of pointers to query. */
    private static final int MAX_POINTER_IDS = 256;
    
    /** The map from screen coordinates to N64 controls. */
    private final VisibleTouchMap mTouchMap;
    
    /** The touch state of each pointer. True indicates down, false indicates up. */
    private final boolean[] mTouchState = new boolean[MAX_POINTER_IDS];
    
    /** The x-coordinate of each pointer, between 0 and (screenwidth-1), inclusive. */
    private final int[] mPointerX = new int[MAX_POINTER_IDS];
    
    /** The y-coordinate of each pointer, between 0 and (screenheight-1), inclusive. */
    private final int[] mPointerY = new int[MAX_POINTER_IDS];
    
    /**
     * The identifier of the pointer associated with the analog stick. -1 indicates the stick has
     * been released.
     */
    private int analogPid = -1;
    
    /**
     * Instantiates a new touchscreen controller.
     * 
     * @param touchMap The map from screen coordinates to N64 controls.
     * @param view The view receiving touch event data.
     */
    public TouchscreenController( VisibleTouchMap touchMap, View view )
    {
        mTouchMap = touchMap;
        view.setOnTouchListener( this );
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
     */
    @Override
    @TargetApi( 5 )
    public boolean onTouch( View view, MotionEvent event )
    {
        // Eclair is needed for multi-touch tracking (getPointerId, getPointerCount)
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR )
            return false;
        
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        
        int pid = -1;
        switch( actionCode )
        {
            case MotionEvent.ACTION_POINTER_DOWN:
                // A non-primary touch has been made
                pid = event.getPointerId( action >> MotionEvent.ACTION_POINTER_INDEX_SHIFT );
                mTouchState[pid] = true;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                // A non-primary touch has been released
                pid = event.getPointerId( action >> MotionEvent.ACTION_POINTER_INDEX_SHIFT );
                mTouchState[pid] = false;
                mPointerX[pid] = -1;
                mPointerY[pid] = -1;
                break;
            case MotionEvent.ACTION_DOWN:
                // A touch gesture has started (e.g. analog stick movement)
                for( int i = 0; i < event.getPointerCount(); i++ )
                {
                    pid = event.getPointerId( i );
                    mTouchState[pid] = true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // A touch gesture has ended or canceled (e.g. analog stick movement)
                for( int i = 0; i < event.getPointerCount(); i++ )
                {
                    pid = event.getPointerId( i );
                    mTouchState[pid] = false;
                    mPointerX[pid] = -1;
                    mPointerY[pid] = -1;
                }
                break;
            default:
                break;
        }
        
        // Update the coordinates of down pointers and record max PID for speed
        int maxPid = -1;
        for( int i = 0; i < event.getPointerCount(); i++ )
        {
            pid = event.getPointerId( i );
            if( pid > maxPid )
                maxPid = pid;
            if( mTouchState[pid] )
            {
                mPointerX[pid] = (int) event.getX( i );
                mPointerY[pid] = (int) event.getY( i );
            }
        }
        
        // Process each touch
        processTouches( mTouchState, mPointerX, mPointerY, maxPid );
        
        return true;
    }
    
    /**
     * Sets the N64 controller state based on where the screen is (multi-) touched. Values outside
     * the ranges listed below are safe.
     * 
     * @param touchstate The touch state of each pointer. True indicates down, false indicates up.
     * @param pointerX The x-coordinate of each pointer, between 0 and (screenwidth-1), inclusive.
     * @param pointerY The y-coordinate of each pointer, between 0 and (screenheight-1), inclusive.
     * @param maxPid Maximum ID of the pointers that have changed (speed optimization).
     */
    private void processTouches( boolean[] touchstate, int[] pointerX, int[] pointerY, int maxPid )
    {
        // Clear button/axis state using super method
        clearState();
        boolean analogMoved = false;
        
        // Process each pointer in sequence
        for( int pid = 0; pid <= maxPid; pid++ )
        {
            // Release analog if its pointer is not touching the screen
            if( pid == analogPid && !touchstate[pid] )
            {
                analogMoved = true;
                analogPid = -1;
            }
            
            // Process touches
            if( touchstate[pid] )
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
    
    /**
     * Process a touch as if intended for a button. Values outside the ranges listed below are safe.
     * 
     * @param xLocation The x-coordinate of the touch, between 0 and (screenwidth-1), inclusive.
     * @param yLocation The y-coordinate of the touch, between 0 and (screenheight-1), inclusive.
     */
    private void processButtonTouch( int xLocation, int yLocation )
    {
        // Determine the index of the button that was pressed
        int index = mTouchMap.getButtonPress( xLocation, yLocation );
        
        // Set the button state if a button was actually touched
        if( index != TouchMap.UNMAPPED )
        {
            if( index < AbstractController.NUM_N64_BUTTONS )
            {
                // A single button was pressed
                mButtonState[index] = true;
            }
            else
            {
                // Two d-pad buttons pressed simultaneously
                switch( index )
                {
                    case TouchMap.DPD_RU:
                        mButtonState[DPD_R] = true;
                        mButtonState[DPD_U] = true;
                        break;
                    case TouchMap.DPD_RD:
                        mButtonState[DPD_R] = true;
                        mButtonState[DPD_D] = true;
                        break;
                    case TouchMap.DPD_LD:
                        mButtonState[DPD_L] = true;
                        mButtonState[DPD_D] = true;
                        break;
                    case TouchMap.DPD_LU:
                        mButtonState[DPD_L] = true;
                        mButtonState[DPD_U] = true;
                        break;
                    default:
                        break;
                }
            }
        }
    }
    
    /**
     * Process a touch as if intended for the analog stick. Values outside the ranges listed below
     * are safe.
     * 
     * @param pointerId The pointer identifier.
     * @param xLocation The x-coordinate of the touch, between 0 and (screenwidth-1), inclusive.
     * @param yLocation The y-coordinate of the touch, between 0 and (screenheight-1), inclusive.
     * @return True, if the analog state changed.
     */
    private boolean processAnalogTouch( int pointerId, int xLocation, int yLocation )
    {
        // Get the cartesian displacement of the analog stick
        Point point = mTouchMap.getAnalogDisplacement( xLocation, yLocation );
        
        // Compute the pythagorean displacement of the stick
        int dX = point.x;
        int dY = point.y;
        float displacement = FloatMath.sqrt( ( dX * dX ) + ( dY * dY ) );
        
        // "Capture" the analog control
        if( mTouchMap.isInCaptureRange( displacement ) )
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
            
            // Fraction of full-throttle, between 0 and 1, inclusive
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
