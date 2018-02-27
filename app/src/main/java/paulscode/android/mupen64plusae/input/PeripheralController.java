/**
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
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.input;

import android.util.Log;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.View;

import java.util.ArrayList;

import paulscode.android.mupen64plusae.input.TouchController.OnStateChangedListener;
import paulscode.android.mupen64plusae.input.map.InputMap;
import paulscode.android.mupen64plusae.input.map.PlayerMap;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae.jni.CoreFragment;
import paulscode.android.mupen64plusae.util.Utility;

/**
 * A class for generating N64 controller commands from peripheral hardware (gamepads, joysticks,
 * keyboards, mice, etc.).
 */
public class PeripheralController extends AbstractController implements
        AbstractProvider.OnInputListener
{
    /**
     * Pointer to core fragment
     */
    private CoreFragment mCoreFragment = null;

    /** The map from hardware identifiers to players. */
    private final PlayerMap mPlayerMap;
    
    /** The map from input codes to entries w/ the N64 command index. */
    private final SparseArray<InputEntry> mEntryMap = new SparseArray<InputEntry>();
    
    /** The analog deadzone, between 0 and 1, inclusive. */
    private final float mDeadzoneFraction;
    
    /** The analog sensitivity, the amount by which to scale stick values, nominally 1. */
    private final float mSensitivityFractionX;
    private final float mSensitivityFractionY;
    
    /** The state change listener. */
    private final OnStateChangedListener mListener;

    /** The sensor provider, which is also added on {@link #mProviders} */
    private SensorController mSensorController;
    
    /** The user input providers. */
    private final ArrayList<AbstractProvider> mProviders;
    
    /** The calculator for the strength of an input. */
    private final InputStrengthCalculator mStrengthCalculator;
    
    /** The positive analog-x strength, between 0 and 1, inclusive. */
    private float mStrengthXpos;
    
    /** The negative analog-x strength, between 0 and 1, inclusive. */
    private float mStrengthXneg;
    
    /** The positive analog-y strength, between 0 and 1, inclusive. */
    private float mStrengthYpos;
    
    /** The negative analogy-y strength, between 0 and 1, inclusive. */
    private float mStrengthYneg;

    /** Called for menu and back keys */
    private View.OnKeyListener mKeyListener;
    
    /**
     * Instantiates a new peripheral controller.
     *
     * @param coreFragment Core interface fragment
     * @param player    The player number, between 1 and 4, inclusive.
     * @param playerMap The map from hardware identifiers to players.
     * @param inputMap  The map from input codes to N64/Mupen commands.
     * @param inputDeadzone The analog deadzone in percent.
     * @param inputSensitivityX The analog X sensitivity in percent.
     * @param inputSensitivityY The analog X sensitivity in percent.
     * @param providers The user input providers. Null elements are safe.
     */
    public PeripheralController(CoreFragment coreFragment, int player, PlayerMap playerMap, InputMap inputMap,
                                int inputDeadzone, int inputSensitivityX, int inputSensitivityY, OnStateChangedListener listener,
                                View.OnKeyListener keyListener, SensorController sensorController, AbstractProvider... providers )
    {
        super(coreFragment);

        setPlayerNumber( player );
        
        // Assign the maps
        mPlayerMap = playerMap;
        mDeadzoneFraction = ( (float) inputDeadzone ) / 100f;
        mSensitivityFractionX = ( (float) inputSensitivityX ) / 100f;
        mSensitivityFractionY = ( (float) inputSensitivityY ) / 100f;
        mListener = listener;
        mKeyListener = keyListener;
        mSensorController = sensorController;
        mCoreFragment = coreFragment;
        
        // Populate the entry map
        mStrengthCalculator = new InputStrengthCalculator( inputMap, mEntryMap );
        
        // Assign the non-null input providers
        mProviders = new ArrayList<AbstractProvider>();
        for( AbstractProvider provider : providers )
        {
            if( provider != null )
            {
                mProviders.add( provider );
                provider.registerListener( this );
            }
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see paulscode.android.mupen64plusae.input.provider.AbstractProvider.Listener#onInput(int,
     * float, int)
     */
    @Override
    public void onInput( int inputCode, float strength, int hardwareId )
    {
        // Process user inputs from keyboard, gamepad, etc.
        if( mPlayerMap.testHardware( hardwareId, mPlayerNumber ) )
        {
            // Update the registered vibrator for this player
            InputDevice device = InputDevice.getDevice( hardwareId );
            if( device != null )
                mCoreFragment.registerVibrator( mPlayerNumber, device.getVibrator() );
            
            // Apply user changes to the controller state
            apply( inputCode, strength );
            
            // Notify the core that controller state has changed
            notifyChanged();
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see paulscode.android.mupen64plusae.input.provider.AbstractProvider.Listener#onInput(int[],
     * float[], int)
     */
    @Override
    public void onInput( int[] inputCodes, float[] strengths, int hardwareId )
    {
        // Process multiple simultaneous user inputs from gamepad, keyboard, etc.
        if( mPlayerMap.testHardware( hardwareId, mPlayerNumber ) )
        {
            // Apply user changes to the controller state
            for( int i = 0; i < inputCodes.length; i++ )
                apply( inputCodes[i], strengths[i] );
            
            // Notify the core that controller state has changed
            notifyChanged();
        }
    }
    
    /**
     * Apply user input to the N64 controller state.
     * 
     * @param inputCode The universal input code that was dispatched.
     * @param strength  The input strength, between 0 and 1, inclusive.
     * 
     * @return True, if controller state changed.
     */
    private boolean apply( int inputCode, float strength )
    {
        InputEntry entry = mEntryMap.get( inputCode );
        
        if( entry == null )
            return false;
        
        // Evaluate the strengths of the inputs that map to the control.
        entry.getStrength().set( strength );
        int n64Index = entry.mN64Index;
        strength = mStrengthCalculator.calculate( n64Index );
        boolean keyDown = strength > AbstractProvider.STRENGTH_THRESHOLD;
        
        if( n64Index >= 0 && n64Index < InputMap.NUM_N64_CONTROLS )
        {
            if( n64Index < NUM_N64_BUTTONS )
            {
                mState.buttons[n64Index] = strength > AbstractProvider.STRENGTH_THRESHOLD;
                return true;
            }
            
            switch( n64Index )
            {
                case InputMap.AXIS_R:
                    mStrengthXpos = strength;
                    break;
                case InputMap.AXIS_L:
                    mStrengthXneg = strength;
                    break;
                case InputMap.AXIS_D:
                    mStrengthYneg = strength;
                    break;
                case InputMap.AXIS_U:
                    mStrengthYpos = strength;
                    break;
                default:
                    return false;
            }
            
            // Calculate the net position of the analog stick
            float rawX = mSensitivityFractionX * ( mStrengthXpos - mStrengthXneg );
            float rawY = mSensitivityFractionY * ( mStrengthYpos - mStrengthYneg );
            float magnitude = (float) Math.sqrt( ( rawX * rawX ) + ( rawY * rawY ) );
            
            // Update controller state
            if( magnitude > mDeadzoneFraction )
            {
                // Normalize the vector
                float normalizedX = rawX / magnitude;
                float normalizedY = rawY / magnitude;

                // Rescale strength to account for deadzone
                magnitude = ( magnitude - mDeadzoneFraction ) / ( 1f - mDeadzoneFraction );
                magnitude = Utility.clamp( magnitude, 0f, 1f );
                mState.axisFractionX = normalizedX * magnitude;
                mState.axisFractionY = normalizedY * magnitude;
            }
            else
            {
                // In the deadzone 
                mState.axisFractionX = 0;
                mState.axisFractionY = 0;
            }
        } else if(mPlayerNumber == 1) {
            if (keyDown) {
                switch (n64Index) {
                    case InputMap.FUNC_INCREMENT_SLOT:
                        Log.v("PeripheralController", "FUNC_INCREMENT_SLOT");
                        mCoreFragment.incrementSlot();
                        break;
                    case InputMap.FUNC_SAVE_SLOT:
                        Log.v("PeripheralController", "FUNC_SAVE_SLOT");
                        mCoreFragment.saveSlot();
                        break;
                    case InputMap.FUNC_LOAD_SLOT:
                        Log.v("PeripheralController", "FUNC_LOAD_SLOT");
                        mCoreFragment.loadSlot();
                        break;
                    case InputMap.FUNC_RESET:
                        Log.v("PeripheralController", "FUNC_RESET");
                        mCoreFragment.restartEmulator();
                        break;
                    case InputMap.FUNC_STOP:
                        Log.v("PeripheralController", "FUNC_STOP");
                        mCoreFragment.shutdownEmulator();
                        break;
                    case InputMap.FUNC_PAUSE:
                        Log.v("PeripheralController", "FUNC_PAUSE");
                        mCoreFragment.togglePause();
                        break;
                    case InputMap.FUNC_FAST_FORWARD:
                        Log.v("PeripheralController", "FUNC_FAST_FORWARD");
                        mCoreFragment.fastForward(true);
                        break;
                    case InputMap.FUNC_FRAME_ADVANCE:
                        Log.v("PeripheralController", "FUNC_FRAME_ADVANCE");
                        mCoreFragment.advanceFrame();
                        break;
                    case InputMap.FUNC_SPEED_UP:
                        Log.v("PeripheralController", "FUNC_SPEED_UP");
                        mCoreFragment.incrementCustomSpeed();
                        break;
                    case InputMap.FUNC_SPEED_DOWN:
                        Log.v("PeripheralController", "FUNC_SPEED_DOWN");
                        mCoreFragment.decrementCustomSpeed();
                        break;
                    case InputMap.FUNC_GAMESHARK:
                        Log.v("PeripheralController", "FUNC_GAMESHARK");
                        mCoreFragment.emuGameShark(true);
                        break;
                    case InputMap.FUNC_SIMULATE_BACK: {
                        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, 0);
                        mKeyListener.onKey(null, KeyEvent.KEYCODE_BACK, event);
                        break;
                    }
                    case InputMap.FUNC_SIMULATE_MENU: {
                        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, 0);
                        mKeyListener.onKey(null, KeyEvent.KEYCODE_MENU, event);
                        break;
                    }
                    case InputMap.FUNC_SCREENSHOT:
                        Log.v("PeripheralController", "FUNC_SCREENSHOT");
                        mCoreFragment.screenshot();
                        break;
                    case InputMap.FUNC_SENSOR_TOGGLE:
                        Log.v("PeripheralController", "FUNC_SENSOR_TOGGLE");
                        if (mSensorController != null) {
                            boolean sensorEnabled = !mSensorController.isSensorEnabled();
                            if (!sensorEnabled) {
                                mState.axisFractionX = 0;
                                mState.axisFractionY = 0;
                                if (mListener != null) {
                                    mListener.onAnalogChanged(mState.axisFractionX, mState.axisFractionY);
                                }
                            }
                            mSensorController.setSensorEnabled(sensorEnabled);
                            if (mListener != null) {
                                mListener.onSensorEnabled(sensorEnabled);
                            }
                        }
                        break;
                    default:
                        return false;
                }
            } else // keyUp
            {
                switch (n64Index) {
                    case InputMap.FUNC_FAST_FORWARD:
                        Log.v("PeripheralController", "FUNC_FAST_FORWARD");
                        mCoreFragment.fastForward(false);
                        break;
                    case InputMap.FUNC_GAMESHARK:
                        Log.v("PeripheralController", "FUNC_GAMESHARK");
                        mCoreFragment.emuGameShark(false);
                        break;
                    default:
                        return false;
                }
            }
        }
        return true;
    }
}
