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

import paulscode.android.mupen64plusae.input.map.TouchMap;
import paulscode.android.mupen64plusae.persistent.AppData;
import android.annotation.TargetApi;
import android.graphics.Point;
import android.util.FloatMath;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * A class for generating N64 controller commands from a touchscreen.
 */
public class TouchController extends AbstractController implements OnTouchListener
{
    public interface OnStateChangedListener
    {
        /**
         * Called after the analog stick values have changed.
         * 
         * @param axisFractionX The x-axis fraction, between -1 and 1, inclusive.
         * @param axisFractionY The y-axis fraction, between -1 and 1, inclusive.
         */
        public void onAnalogChanged( float axisFractionX, float axisFractionY );
    }
    
    /** The maximum number of pointers to query. */
    private static final int MAX_POINTER_IDS = 256;
    
    /** The state change listener. */
    private final OnStateChangedListener mListener;
    
    /** The map from screen coordinates to N64 controls. */
    private final TouchMap mTouchMap;
    
    /** The map from pointer ids to N64 controls. */
    private final SparseIntArray mPointerMap = new SparseIntArray();
    
    /** Whether the analog stick should be constrained to an octagon. */
    private final boolean mIsOctagonal;
    
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
    
    /** The touch event source to listen to, or 0 to listen to all sources. */
    private int mSourceFilter = 0;
    
    /**
     * Instantiates a new touch controller.
     * 
     * @param touchMap The map from touch coordinates to N64 controls.
     * @param view The view receiving touch event data.
     * @param listener The listener for controller state changes.
     * @param isOctagonal True if the analog stick should be constrained to an octagon.
     */
    public TouchController( TouchMap touchMap, View view, OnStateChangedListener listener,
            boolean isOctagonal )
    {
        mListener = listener;
        mTouchMap = touchMap;
        mIsOctagonal = isOctagonal;
        view.setOnTouchListener( this );
    }
    
    /**
     * Sets the touch event source filter.
     * 
     * @param source The source to listen to, or 0 to listen to all sources.
     */
    public void setSourceFilter( int source )
    {
        mSourceFilter = source;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
     */
    @Override
    @TargetApi( 9 )
    public boolean onTouch( View view, MotionEvent event )
    {
        // Eclair is needed for multi-touch tracking (getPointerId, getPointerCount)
        if( !AppData.IS_ECLAIR )
            return false;
        
        // Filter by source, if applicable
        int source = AppData.IS_GINGERBREAD ? event.getSource() : 0;
        if( mSourceFilter != 0 && mSourceFilter != source )
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
        boolean analogMoved = false;
        
        // Process each pointer in sequence
        for( int pid = 0; pid <= maxPid; pid++ )
        {
            // Release analog if its pointer is not touching the screen
            if( pid == analogPid && !touchstate[pid] )
            {
                analogMoved = true;
                analogPid = -1;
                mState.axisFractionX = 0;
                mState.axisFractionY = 0;
            }
            
            // Process button inputs
            if( pid != analogPid )
                processButtonTouch( touchstate[pid], pointerX[pid], pointerY[pid], pid );
            
            // Process analog inputs
            if( touchstate[pid] )
                analogMoved = processAnalogTouch( pid, pointerX[pid], pointerY[pid] );
        }
        
        // Call the super method to send the input to the core
        notifyChanged();
        
        // Update the skin if the virtual analog stick moved
        if( analogMoved && mListener != null )
            mListener.onAnalogChanged( mState.axisFractionX, mState.axisFractionY );
    }
    
    /**
     * Process a touch as if intended for a button. Values outside the ranges listed below are safe.
     * 
     * @param touched Whether the button is pressed.
     * @param xLocation The x-coordinate of the touch, between 0 and (screenwidth-1), inclusive.
     * @param yLocation The y-coordinate of the touch, between 0 and (screenheight-1), inclusive.
     * @param pid The identifier of the touch pointer.
     */
    private void processButtonTouch( boolean touched, int xLocation, int yLocation, int pid )
    {
        // Determine the index of the button that was pressed
        int index = touched
                ? mTouchMap.getButtonPress( xLocation, yLocation )
                : mPointerMap.get( pid, TouchMap.UNMAPPED );
        
        // Set the button state if a button was actually touched
        if( index != TouchMap.UNMAPPED )
        {
            // Update the pointer map
            if( touched )
            {
                // Check if this pointer was already mapped to a button
                int prevIndex = mPointerMap.get( pid );
                if( prevIndex >= 0 && prevIndex != index )
                {
                    // Release the previous button
                    setTouchState( prevIndex, false );
                }
                mPointerMap.put( pid, index );
            }
            else
                mPointerMap.delete( pid );
            
            setTouchState( index, touched );
        }
    }
    
    /**
     * Sets the state of a button, and handles the D-Pad diagonals.
     * 
     * @param index Which button is affected.
     * @param touched Whether the button is pressed.
     */
    private void setTouchState( int index, boolean touched )
    {
        // Set the button state
        if( index < AbstractController.NUM_N64_BUTTONS )
        {
            // A single button was pressed
            mState.buttons[index] = touched;
        }
        else
        {
            // Two d-pad buttons pressed simultaneously
            switch( index )
            {
                case TouchMap.DPD_RU:
                    mState.buttons[DPD_R] = touched;
                    mState.buttons[DPD_U] = touched;
                    break;
                case TouchMap.DPD_RD:
                    mState.buttons[DPD_R] = touched;
                    mState.buttons[DPD_D] = touched;
                    break;
                case TouchMap.DPD_LD:
                    mState.buttons[DPD_L] = touched;
                    mState.buttons[DPD_D] = touched;
                    break;
                case TouchMap.DPD_LU:
                    mState.buttons[DPD_L] = touched;
                    mState.buttons[DPD_U] = touched;
                    break;
                default:
                    break;
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
            if( mIsOctagonal )
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
            mState.axisFractionX = p * dX / displacement;
            mState.axisFractionY = -p * dY / displacement;
            
            // Analog state changed
            return true;
        }
        
        // Analog state did not change
        return false;
    }
}
