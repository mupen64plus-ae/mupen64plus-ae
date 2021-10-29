/*
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2013 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
 * not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: Paul Lamb, Gillou68310, littleguy77
 */
package paulscode.android.mupen64plusae.input;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import java.util.Set;

import paulscode.android.mupen64plusae.input.map.TouchMap;
import paulscode.android.mupen64plusae.jni.CoreFragment;

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
        void onAnalogChanged( float axisFractionX, float axisFractionY );
        
        /**
         * Called after auto-hold button state changed.
         * 
         * @param pressed The auto-hold state.
         * @param index The index of the auto-hold mask.
         */
        void onAutoHold( boolean pressed, int index );

        /**
         * Called after the sensor has been enabled or disabled
         * 
         * @param enabled
         *            true if enabled, false if disabled
         */
        void onSensorEnabled(boolean enabled);

        /**
         * Called when the touch controls should be shown
         */
        void onTouchControlsShow();

        /**
         * Called when the touch controls should be hidden
         */
        void onTouchControlsHide();
    }

    private static final int AUTOHOLD_METHOD_DISABLED = 0;
    private static final int AUTOHOLD_METHOD_LONGPRESS = 1;
    private static final int AUTOHOLD_METHOD_SLIDEOUT = 2;
    
    /** The number of milliseconds to wait before auto-holding (long-press method). */
    private static final int AUTOHOLD_LONGPRESS_TIME = 1000;
    
    /** The pattern vibration when auto-hold is engaged. */
    private static final long[] AUTOHOLD_VIBRATE_PATTERN = { 0, 50, 50, 50 };
    
    /** The number of milliseconds of vibration when pressing a key. */
    private static final int FEEDBACK_VIBRATE_TIME = 50;
    
    /** The maximum number of pointers to query. */
    private static final int MAX_POINTER_IDS = 256;
    
    /** The state change listener. */
    private final OnStateChangedListener mListener;
    
    /** The map from screen coordinates to N64 controls. */
    private final TouchMap mTouchMap;
    
    /** The map from pointer ids to N64 controls. */
    private final SparseIntArray mPointerMap = new SparseIntArray();
    
    /** The method used for auto-holding buttons. */
    private final int mAutoHoldMethod;
    
    /** The set of auto-holdable buttons. */
    private final Set<Integer> mNotAutoHoldables;
    
    /** Whether touchscreen feedback is enabled. */
    private final boolean mTouchscreenFeedback;
    
    /** The touch state of each pointer. True indicates down, false indicates up. */
    private final boolean[] mTouchState = new boolean[MAX_POINTER_IDS];
    
    /** The x-coordinate of each pointer, between 0 and (screenwidth-1), inclusive. */
    private final int[] mPointerX = new int[MAX_POINTER_IDS];
    
    /** The y-coordinate of each pointer, between 0 and (screenheight-1), inclusive. */
    private final int[] mPointerY = new int[MAX_POINTER_IDS];
    
    /** The pressed start time of each pointer. */
    private final long[] mStartTime = new long[MAX_POINTER_IDS];
    
    /** The time between press and release of each pointer. */
    private final long[] mElapsedTime = new long[MAX_POINTER_IDS];

    /** Invert the analog x axis */
    private final boolean mInvertXAxis;

    /** Invert the analog y axis */
    private final boolean mInvertYAxis;


    /** True if relative joystick is enabled */
    private boolean mRelativeJoystick;
    
    /**
     * The identifier of the pointer associated with the analog stick. -1 indicates the stick has
     * been released.
     */
    private int mAnalogPid = -1;
    
    private Vibrator mVibrator;

    /** The user sensor input provider. */
    private SensorController mSensorController;
    
    /**
     * Instantiates a new touch controller.
     *
     * @param coreFragment        Core fragment
     * @param touchMap            The map from touch coordinates to N64 controls.
     * @param listener            The listener for controller state changes.
     * @param vibrator            The haptic feedback device. MUST BE NULL if vibrate permission not granted.
     * @param autoHoldMethod      The method for auto-holding buttons.
     * @param touchscreenFeedback True if haptic feedback should be used.
     * @param notAutoHoldableButtons The N64 commands that correspond to NOT auto-holdable buttons.
     * @param sensorController Sensor controller instance
     * @param invertXAxis True if X axis should be inverted
     * @param invertYAxis True if Y axis should be inverted
     */
    public TouchController(CoreFragment coreFragment, TouchMap touchMap, OnStateChangedListener listener,
                           Vibrator vibrator, int autoHoldMethod, boolean touchscreenFeedback,
                           Set<Integer> notAutoHoldableButtons, SensorController sensorController,
                           boolean invertXAxis, boolean invertYAxis, boolean relativeJoystick )
    {
        super(coreFragment);

        mListener = listener;
        mTouchMap = touchMap;
        mVibrator = vibrator;
        mAutoHoldMethod = autoHoldMethod;
        mTouchscreenFeedback = touchscreenFeedback;
        mNotAutoHoldables = notAutoHoldableButtons;
        mSensorController = sensorController;
        mInvertXAxis = invertXAxis;
        mInvertYAxis = invertYAxis;
        mRelativeJoystick = relativeJoystick;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
     */
    @SuppressLint( "ClickableViewAccessibility" )
    @Override
    public boolean onTouch( View view, MotionEvent event )
    {
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        
        int pid;
        switch( actionCode )
        {
            case MotionEvent.ACTION_POINTER_DOWN:
                // A non-primary touch has been made
                pid = event.getPointerId( action >> MotionEvent.ACTION_POINTER_INDEX_SHIFT );
                mStartTime[pid] = System.currentTimeMillis();
                mTouchState[pid] = true;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                // A non-primary touch has been released
                pid = event.getPointerId( action >> MotionEvent.ACTION_POINTER_INDEX_SHIFT );
                mElapsedTime[pid] = System.currentTimeMillis() - mStartTime[pid];
                mTouchState[pid] = false;
                break;
            case MotionEvent.ACTION_DOWN:
                // A touch gesture has started (e.g. analog stick movement)
                for( int i = 0; i < event.getPointerCount(); i++ )
                {
                    pid = event.getPointerId( i );
                    mStartTime[pid] = System.currentTimeMillis();
                    mTouchState[pid] = true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // A touch gesture has ended or canceled (e.g. analog stick movement)
                for( int i = 0; i < event.getPointerCount(); i++ )
                {
                    pid = event.getPointerId( i );
                    mElapsedTime[pid] = System.currentTimeMillis() - mStartTime[pid];
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
        processTouches( mTouchState, mPointerX, mPointerY, mElapsedTime, maxPid, actionCode );
        
        return true;
    }
    
    /**
     * Sets the N64 controller state based on where the screen is (multi-) touched. Values outside
     * the ranges listed below are safe.
     * 
     * @param touchstate The touch state of each pointer. True indicates down, false indicates up.
     * @param pointerX   The x-coordinate of each pointer, between 0 and (screenwidth-1), inclusive.
     * @param pointerY   The y-coordinate of each pointer, between 0 and (screenheight-1), inclusive.
     * @param maxPid     Maximum ID of the pointers that have changed (speed optimization).
     * @param actionCode The the action code
     */
    private void processTouches( boolean[] touchstate, int[] pointerX, int[] pointerY,
            long[] elapsedTime, int maxPid, int actionCode )
    {
        boolean analogMoved = false;
        
        // Process each pointer in sequence
        for( int pid = 0; pid <= maxPid; pid++ )
        {
            // Release analog if its pointer is not touching the screen
            if( pid == mAnalogPid && !touchstate[pid] )
            {
                analogMoved = true;
                mAnalogPid = -1;
                mState.axisFractionX = 0;
                mState.axisFractionY = 0;
                mTouchMap.resetAnalogPosition();
            }
            
            // Process button inputs
            if( pid != mAnalogPid )
                processButtonTouch( touchstate[pid], pointerX[pid], pointerY[pid],
                        elapsedTime[pid], pid, actionCode );
            
            // Process analog inputs
            if( touchstate[pid] && processAnalogTouch( pid, pointerX[pid], pointerY[pid] ) )
                analogMoved = true;
        }
        
        // Call the super method to send the input to the core
        notifyChanged(false);

        float invertXAxis = mInvertXAxis ? -1.0f:1.0f;
        float invertYAxis = mInvertYAxis ? -1.0f:1.0f;
        
        // Update the skin if the virtual analog stick moved
        if( analogMoved && mListener != null )
            mListener.onAnalogChanged( mState.axisFractionX*invertXAxis, mState.axisFractionY*invertYAxis );
    }
    
    /**
     * Process a touch as if intended for a button. Values outside the ranges listed below are safe.
     * 
     * @param touched   Whether the button is pressed or not.
     * @param xLocation The x-coordinate of the touch, between 0 and (screenwidth-1), inclusive.
     * @param yLocation The y-coordinate of the touch, between 0 and (screenheight-1), inclusive.
     * @param pid       The identifier of the touch pointer.
     * @param actionCode The the action code
     */
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private void processButtonTouch( boolean touched, int xLocation, int yLocation,
            long timeElapsed, int pid, int actionCode )
    {
        mListener.onTouchControlsShow();
        // Determine the index of the button that was pressed
        int index = touched
                ? mTouchMap.getButtonPress( xLocation, yLocation )
                : mPointerMap.get( pid, TouchMap.UNMAPPED );

        // Update the pointer map
        if( !touched )
        {
            // Finger lifted off screen, forget what this pointer was touching
            mPointerMap.delete( pid );
        }
        else
        {
            // Determine where the finger came from if is was slid
            int prevIndex = mPointerMap.get( pid, TouchMap.UNMAPPED );

            // Finger touched somewhere on screen, remember what this pointer is touching
            mPointerMap.put( pid, index );

            if( prevIndex != index )
            {
                // Finger slid from somewhere else, act accordingly
                // There are three possibilities:
                // - old button --> new button
                // - nothing --> new button
                // - old button --> nothing

                // Reset this pointer's start time
                mStartTime[pid] = System.currentTimeMillis();

                if( prevIndex != TouchMap.UNMAPPED )
                {
                    // Slid off a valid button
                    if( isNotAutoHoldable( prevIndex ) || mAutoHoldMethod == AUTOHOLD_METHOD_DISABLED )
                    {
                        // Slid off a non-auto-hold button
                        setTouchState( prevIndex, false );
                        mListener.onAutoHold( false, prevIndex );
                    }
                    else
                    {
                        // Slid off an auto-hold button
                        switch( mAutoHoldMethod )
                        {
                            case AUTOHOLD_METHOD_LONGPRESS:
                                // Using long-press method, release auto-hold button
                                mListener.onAutoHold( false, prevIndex );
                                setTouchState( prevIndex, false );
                                break;

                            case AUTOHOLD_METHOD_SLIDEOUT:
                                // Using slide-off method, engage auto-hold button
                                if( mVibrator != null )
                                {
                                    mVibrator.cancel();

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        mVibrator.vibrate(VibrationEffect.createWaveform(AUTOHOLD_VIBRATE_PATTERN, -1));
                                    } else {
                                        mVibrator.vibrate(AUTOHOLD_VIBRATE_PATTERN, -1 );
                                    }
                                }
                                mListener.onAutoHold( true, prevIndex );
                                setTouchState( prevIndex, true );
                                break;
                        }
                    }
                }
            }
        }

        if( index != TouchMap.UNMAPPED )
        {
            // Finger is on a valid button

            // process the TOGGLE_SENSOR button
            if (index == TouchMap.TOGGLE_SENSOR && mSensorController != null
                    && (actionCode == MotionEvent.ACTION_DOWN || actionCode == MotionEvent.ACTION_POINTER_DOWN)) {
                boolean sensorEnabled = !mSensorController.isSensorEnabled();
                if (!sensorEnabled) {
                    mState.axisFractionX = 0;
                    mState.axisFractionY = 0;
                    mListener.onAnalogChanged(mState.axisFractionX, mState.axisFractionY);
                }
                mSensorController.setSensorEnabled(sensorEnabled);
                mListener.onSensorEnabled(sensorEnabled);
            }

            // Provide simple vibration feedback for any valid button when first touched
            if( touched && mTouchscreenFeedback && mVibrator != null )
            {
                boolean firstTouched;
                if( index < NUM_N64_BUTTONS )
                {
                    // Single button pressed
                    firstTouched = !mState.buttons[index];
                }
                else
                {
                    // Two d-pad buttons pressed simultaneously
                    switch( index )
                    {
                        case TouchMap.DPD_RU:
                            firstTouched = !( mState.buttons[DPD_R] && mState.buttons[DPD_U] );
                            break;
                        case TouchMap.DPD_RD:
                            firstTouched = !( mState.buttons[DPD_R] && mState.buttons[DPD_D] );
                            break;
                        case TouchMap.DPD_LD:
                            firstTouched = !( mState.buttons[DPD_L] && mState.buttons[DPD_D] );
                            break;
                        case TouchMap.DPD_LU:
                            firstTouched = !( mState.buttons[DPD_L] && mState.buttons[DPD_U] );
                            break;
                        default:
                            firstTouched = false;
                            break;
                    }
                }

                if( firstTouched )
                {
                    mVibrator.cancel();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mVibrator.vibrate(VibrationEffect.createOneShot(FEEDBACK_VIBRATE_TIME, 100));
                    } else {
                        mVibrator.vibrate(FEEDBACK_VIBRATE_TIME);
                    }
                }
            }

            // Set the controller state accordingly
            if( touched || isNotAutoHoldable( index ) || mAutoHoldMethod == AUTOHOLD_METHOD_DISABLED )
            {
                mListener.onAutoHold( touched, index );

                // Finger just touched a button (any kind) OR
                // Finger just lifted off non-auto-holdable button
                setTouchState( index, touched );
                // Do not provide auto-hold feedback yet
            }
            else
            {
                // Finger just lifted off an auto-holdable button
                switch( mAutoHoldMethod )
                {
                    case AUTOHOLD_METHOD_SLIDEOUT:
                        // Release auto-hold button if using slide-off method
                        mListener.onAutoHold( false, index );
                        setTouchState( index, false );
                        break;

                    case AUTOHOLD_METHOD_LONGPRESS:
                        if( timeElapsed < AUTOHOLD_LONGPRESS_TIME )
                        {
                            // Release auto-hold if short-pressed
                            mListener.onAutoHold( false, index );
                            setTouchState( index, false );
                        }
                        else
                        {
                            // Engage auto-hold if long-pressed
                            if( mVibrator != null )
                            {
                                mVibrator.cancel();

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    mVibrator.vibrate(VibrationEffect.createWaveform(AUTOHOLD_VIBRATE_PATTERN, -1));
                                } else {
                                    mVibrator.vibrate(AUTOHOLD_VIBRATE_PATTERN, -1 );
                                }
                            }
                            mListener.onAutoHold( true, index );
                            setTouchState( index, true );
                        }
                        break;
                }
            }
        }
    }

    /**
     * Checks if the button mapped to an N64 command is not auto-holdable.
     * 
     * @param commandIndex The index to the N64 command.
     * 
     * @return True if the button mapped to the command is auto-holdable.
     */
    private boolean isNotAutoHoldable(int commandIndex )
    {
        return mNotAutoHoldables != null && mNotAutoHoldables.contains( commandIndex );
    }
    
    /**
     * Sets the state of a button, and handles the D-Pad diagonals.
     * 
     * @param index   Which button is affected.
     * @param touched Whether the button is pressed or not.
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
     * 
     * @return True, if the analog state changed.
     */
    private boolean processAnalogTouch( int pointerId, int xLocation, int yLocation )
    {
        Point displacementPoint = mTouchMap.getAnalogDisplacementOriginal( xLocation, yLocation);

        // "Capture" the analog control
        if( mTouchMap.isInCaptureRange( displacementPoint ) && mAnalogPid != pointerId) {
            mAnalogPid = pointerId;

            if (mRelativeJoystick) {
                mTouchMap.updateAnalogPosition(xLocation, yLocation);
            }
        }

        // User is controlling the analog stick
        if( pointerId == mAnalogPid )
        {
            // Get the cartesian displacement of the analog stick
            Point point = mTouchMap.getAnalogDisplacement( xLocation, yLocation );

            // Compute the pythagorean displacement of the stick
            int dX = point.x;
            int dY = point.y;
            float displacement = (float) Math.sqrt( ( dX * dX ) + ( dY * dY ) );
            
            // Fraction of full-throttle, between 0 and 1, inclusive
            float p = mTouchMap.getAnalogStrength( displacement );

            if (displacement == 0.0) {
                mState.axisFractionX = 0.0f;
                mState.axisFractionY = 0.0f;
            } else {
                // Store the axis values in the super fields (screen y is inverted)
                mState.axisFractionX = p * dX / displacement * (mInvertXAxis ? -1.0f:1.0f);
                mState.axisFractionY = -p * dY / displacement * (mInvertYAxis ? -1.0f:1.0f);

                // Scale to a square deadzone of 0.07 to simulate a real N64 controller
                float deadzone = 0.07f;
                // Use a square deadzone to simulate original N64 controllers more closely
                if (Math.abs(mState.axisFractionX) > deadzone) {
                    mState.axisFractionX = Math.signum(mState.axisFractionX)*(Math.abs(mState.axisFractionX) - deadzone) / (1.0f - deadzone);
                } else {
                    mState.axisFractionX = 0;
                }
                if (Math.abs(mState.axisFractionY) > deadzone) {
                    mState.axisFractionY = Math.signum(mState.axisFractionY)*(Math.abs(mState.axisFractionY) - deadzone) / (1.0f - deadzone);
                } else {
                    mState.axisFractionY = 0;
                }
            }
            
            // Analog state changed
            return true;
        }
        
        // Analog state did not change
        return false;
    }
}
