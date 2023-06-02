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
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae_mpn.input;

import android.util.Log;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.View;

import paulscode.android.mupen64plusae_mpn.game.GameSurface;
import paulscode.android.mupen64plusae_mpn.input.TouchController.OnStateChangedListener;
import paulscode.android.mupen64plusae_mpn.input.map.InputMap;
import paulscode.android.mupen64plusae_mpn.input.map.PlayerMap;
import paulscode.android.mupen64plusae_mpn.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae_mpn.jni.CoreFragment;
import paulscode.android.mupen64plusae_mpn.persistent.GlobalPrefs;

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
    private final CoreFragment mCoreFragment;

    /**
     * Pointer to game surface
     */
    private final GameSurface mGameSurface;

    /**
     * Global preferences
     */
    private final GlobalPrefs mGlobalPrefs;

    /**
     * ROM good game
     */
    private final String mRomGoodName;

    /** The map from hardware identifiers to players. */
    private final PlayerMap mPlayerMap;
    
    /** The map from input codes to entries w/ the N64 command index. */
    private final SparseArray<InputEntry> mEntryMap = new SparseArray<>();
    
    /** True if deadzone should be set automatically */
    private final boolean mAutoDeadzone;

    /** The analog deadzone, between 0 and 1, inclusive. */
    private final float mDeadzoneFraction;
    
    /** The analog sensitivity, the amount by which to scale stick values, nominally 1. */
    private final float mSensitivityFractionX;
    private final float mSensitivityFractionY;
    
    /** The state change listener. */
    private final OnStateChangedListener mListener;

    /** The sensor provider */
    private final SensorController mSensorController;
    
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
    private final View.OnKeyListener mKeyListener;

    /** True if the N64 analog is being controlled by a digital input */
    private boolean mIsAnalogDigitalInput = false;

    /** True if we should hold controller buttons for a certain amount of time for
     * some functions to take effect */
    private final boolean mHoldControllerBottons;

    /** Max flat value to determine if we should override it */
    private static final float MAX_FLAT = 0.5f;

    /** Minimum flat value to determine if we should override it */
    private static final float MIN_FLAT = 0.15f;
    
    /**
     * Instantiates a new peripheral controller.
     *
     * @param coreFragment Core interface fragment
     * @param gameSurface Game surface
     * @param globalPrefs GLobal preferences
     * @param romGoodName ROM good name
     * @param player    The player number, between 1 and 4, inclusive.
     * @param playerMap The map from hardware identifiers to players.
     * @param inputMap  The map from input codes to N64/Mupen commands.
     * @param autoDeadzone True if deadzone should be set automatically
     * @param inputDeadzone The analog deadzone in percent.
     * @param inputSensitivityX The analog X sensitivity in percent.
     * @param inputSensitivityY The analog X sensitivity in percent.
     * @param providers The user input providers. Null elements are safe.
     */
    public PeripheralController(CoreFragment coreFragment, GameSurface gameSurface, GlobalPrefs globalPrefs, String romGoodName, int player, PlayerMap playerMap, InputMap inputMap,
                                boolean autoDeadzone, int inputDeadzone, int inputSensitivityX, int inputSensitivityY, boolean holdForFunctions,
                                OnStateChangedListener listener, View.OnKeyListener keyListener, SensorController sensorController, AbstractProvider... providers )
    {
        super(coreFragment);

        setPlayerNumber( player );
        
        // Assign the maps
        mPlayerMap = playerMap;
        mAutoDeadzone = autoDeadzone;
        mDeadzoneFraction = ( (float) inputDeadzone ) / 100f;
        mSensitivityFractionX = ( (float) inputSensitivityX ) / 100f;
        mSensitivityFractionY = ( (float) inputSensitivityY ) / 100f;
        mHoldControllerBottons = holdForFunctions;
        mListener = listener;
        mKeyListener = keyListener;
        mSensorController = sensorController;
        mCoreFragment = coreFragment;
        mGameSurface = gameSurface;
        mGlobalPrefs = globalPrefs;
        mRomGoodName = romGoodName;
        
        // Populate the entry map
        mStrengthCalculator = new InputStrengthCalculator( inputMap, mEntryMap );
        
        // Assign the non-null input providers
        for( AbstractProvider provider : providers )
        {
            if( provider != null )
            {
                provider.registerListener( this );
            }
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see paulscode.android.mupen64plusae_mpn.input.provider.AbstractProvider.Listener#onInput(int,
     * float, int)
     */
    @Override
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public void onInput( int inputCode, float strength, int hardwareId, int repeatCount, int source )
    {
        // Process user inputs from keyboard, gamepad, etc.
        if( mPlayerMap.testHardware( hardwareId, mPlayerNumber ) )
        {
            // Update the registered vibrator for this player
            InputDevice device = InputDevice.getDevice( hardwareId );
            if( device != null ) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    mCoreFragment.registerVibrator(mPlayerNumber, device.getVibratorManager().getDefaultVibrator());
                } else {
                    mCoreFragment.registerVibrator(mPlayerNumber, device.getVibrator());
                }
            }
            
            // Apply user changes to the controller state
            apply( inputCode, strength, false, repeatCount, device, source );
            
            // Notify the core that controller state has changed
            notifyChanged(mIsAnalogDigitalInput);
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see paulscode.android.mupen64plusae_mpn.input.provider.AbstractProvider.Listener#onInput(int[],
     * float[], int)
     */
    @Override
    public void onInput( int[] inputCodes, float[] strengths, int hardwareId, int source )
    {
        // Process multiple simultaneous user inputs from gamepad, keyboard, etc.
        if( mPlayerMap.testHardware( hardwareId, mPlayerNumber ) )
        {
            InputDevice device = InputDevice.getDevice( hardwareId );

            // Apply user changes to the controller state
            for( int i = 0; i < inputCodes.length; i++ )
                apply( inputCodes[i], strengths[i], true, 0, device, source );
            
            // Notify the core that controller state has changed
            notifyChanged(mIsAnalogDigitalInput);
        }
    }
    
    /**
     * Apply user input to the N64 controller state.
     * 
     * @param inputCode The universal input code that was dispatched.
     * @param strength  The input strength, between 0 and 1, inclusive.
     * @param isAxis True if the input comes from an axis
     * @param repeatCount How many intervals the button has been held for
     * @param device Input device associated with the motion
     * @param source Source of input
     */
    private void apply(int inputCode, float strength, boolean isAxis, int repeatCount, InputDevice device, int source )
    {
        InputEntry entry = mEntryMap.get( inputCode );

        if( entry == null )
            return;

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
                return;
            }
            
            switch( n64Index )
            {
                case InputMap.AXIS_R:
                    mStrengthXpos = strength;
                    mIsAnalogDigitalInput = mIsAnalogDigitalInput || !isAxis;
                    break;
                case InputMap.AXIS_L:
                    mStrengthXneg = strength;
                    mIsAnalogDigitalInput = mIsAnalogDigitalInput || !isAxis;
                    break;
                case InputMap.AXIS_D:
                    mStrengthYneg = strength;
                    mIsAnalogDigitalInput = mIsAnalogDigitalInput || !isAxis;
                    break;
                case InputMap.AXIS_U:
                    mStrengthYpos = strength;
                    mIsAnalogDigitalInput = mIsAnalogDigitalInput || !isAxis;
                    break;
                default:
                    return;
            }
            
            // Calculate the net position of the analog stick
            float rawX = mSensitivityFractionX * ( mStrengthXpos - mStrengthXneg );
            float rawY = mSensitivityFractionY * ( mStrengthYpos - mStrengthYneg );
            float magnitude = (float) Math.sqrt( ( rawX * rawX ) + ( rawY * rawY ) );
            
            // Update controller state
            float deadzone = mDeadzoneFraction;

            if (mAutoDeadzone)
            {
                int axisCode = inputToAxisCode( inputCode );
                deadzone = getAutoDeadZone(source, device, axisCode);
            }
            
            // Use a square deadzone to simulate original N64 controllers more closely
            if (Math.abs(rawX) > deadzone) {
                rawX = Math.signum(rawX)*(Math.abs(rawX) - deadzone) / (1.0f - deadzone);
            } else {
                rawX = 0;
            }
            if (Math.abs(rawY) > deadzone) {
                rawY = Math.signum(rawY)*(Math.abs(rawY) - deadzone) / (1.0f - deadzone);
            } else {
                rawY = 0;
            }

            mState.axisFractionX = rawX;
            mState.axisFractionY = rawY;

        } else if(mPlayerNumber == 1) {
            // Button must be held for some inputs to prevent accidental presses
            boolean ignoreInput = !isAxis && repeatCount < 10 && mHoldControllerBottons;

            if (keyDown) {
                switch (n64Index) {
                    case InputMap.FUNC_INCREMENT_SLOT:
                        Log.v("PeripheralController", "FUNC_INCREMENT_SLOT");
                        mCoreFragment.incrementSlot();
                        break;
                    case InputMap.FUNC_DECREMENT_SLOT:
                        Log.v("PeripheralController", "FUNC_DECREMENT_SLOT");
                        mCoreFragment.decrementSlot();
                        break;
                    case InputMap.FUNC_SAVE_SLOT:
                        Log.v("PeripheralController", "FUNC_SAVE_SLOT");
                        mCoreFragment.saveSlot();
                        break;
                    case InputMap.FUNC_LOAD_SLOT:
                        if (!ignoreInput) {
                            Log.v("PeripheralController", "FUNC_LOAD_SLOT");
                            mCoreFragment.loadSlot();
                        }
                        break;
                    case InputMap.FUNC_RESET:
                        if (!ignoreInput) {
                            Log.v("PeripheralController", "FUNC_RESET");
                            mCoreFragment.restartEmulator();
                        }
                        break;
                    case InputMap.FUNC_STOP:
                        if (!ignoreInput) {
                            Log.v("PeripheralController", "FUNC_STOP");
                            mCoreFragment.shutdownEmulator();
                        }
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
                    case InputMap.FUNC_SIMULATE_BACK:
                        if (!ignoreInput) {
                            KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, 0);
                            mKeyListener.onKey(null, KeyEvent.KEYCODE_BACK, event);
                        }
                        break;
                    case InputMap.FUNC_SIMULATE_MENU:
                        if (!ignoreInput) {
                            KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, 0);
                            mKeyListener.onKey(null, KeyEvent.KEYCODE_MENU, event);
                        }
                        break;
                    case InputMap.FUNC_SCREENSHOT:
                        Log.v("PeripheralController", "FUNC_SCREENSHOT");
                        mGameSurface.takeScreenshot(mGlobalPrefs.screenshotsDir, mRomGoodName);
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
                }
            }
        }
    }

    private static float getAutoDeadZone(int source, InputDevice device, int axis)
    {
        final InputDevice.MotionRange range = device != null ? device.getMotionRange(axis, source) : null;

        // A joystick at rest does not always report an absolute position of
        // (0,0). Use the getFlat() method to determine the range of values
        // bounding the joystick axis center.
        float flat = MIN_FLAT;

        if (range != null) {
            flat = range.getFlat();
        }

        //Some devices with bad drivers report invalid flat regions
        if(flat > MAX_FLAT || flat < MIN_FLAT)
        {
            flat = MIN_FLAT;
        }

        return flat;
    }

    /**
     * Utility for child classes. Converts a universal input code to an Android axis code.
     *
     * @param inputCode The universal input code.
     *
     * @return The corresponding Android axis code.
     */
    private static int inputToAxisCode( int inputCode )
    {
        return ( -inputCode - 1 ) / 2;
    }
}
