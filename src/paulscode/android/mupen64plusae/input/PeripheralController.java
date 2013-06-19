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

import java.util.ArrayList;

import paulscode.android.mupen64plusae.CoreInterface;
import paulscode.android.mupen64plusae.CoreInterfaceNative;
import paulscode.android.mupen64plusae.GameMenuHandler;
import paulscode.android.mupen64plusae.input.map.InputMap;
import paulscode.android.mupen64plusae.input.map.PlayerMap;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.util.SafeMethods;
import paulscode.android.mupen64plusae.util.Utility;
import android.annotation.TargetApi;
import android.util.FloatMath;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;

/**
 * A class for generating N64 controller commands from peripheral hardware (gamepads, joysticks,
 * keyboards, mice, etc.).
 */
public class PeripheralController extends AbstractController implements
        AbstractProvider.OnInputListener
{
    /** The map from hardware identifiers to players. */
    private final PlayerMap mPlayerMap;
    
    /** The map from input codes to N64/Mupen commands. */
    private final InputMap mInputMap;
    
    /** The analog deadzone, between 0 and 1, inclusive. */
    private final float mDeadzoneFraction;
    
    /** The analog sensitivity, the amount by which to scale stick values, nominally 1. */
    private final float mSensitivityFraction;
    
    /** The user input providers. */
    private final ArrayList<AbstractProvider> mProviders;
    
    /** The positive analog-x strength, between 0 and 1, inclusive. */
    private float mStrengthXpos;
    
    /** The negative analog-x strength, between 0 and 1, inclusive. */
    private float mStrengthXneg;
    
    /** The positive analog-y strength, between 0 and 1, inclusive. */
    private float mStrengthYpos;
    
    /** The negative analogy-y strength, between 0 and 1, inclusive. */
    private float mStrengthYneg;
    
    /** Toggle so pause function also acts as resume. */
    private boolean doPause = false;
    
    /** Difference in emulation speed after modification by mapped speed-up and slow-down functions. */
    private int speedOffset = 0;
    
    /** Amount to change the speed with each speed-up or slow-down function press. */
    private static final int SPEED_INC = 10;
    
    /**
     * Instantiates a new peripheral controller.
     * 
     * @param player    The player number, between 1 and 4, inclusive.
     * @param playerMap The map from hardware identifiers to players.
     * @param inputMap  The map from input codes to N64/Mupen commands.
     * @param inputDeadzone The analog deadzone in percent.
     * @param inputSensitivity The analog sensitivity in percent.
     * @param providers The user input providers. Null elements are safe.
     */
    public PeripheralController( int player, PlayerMap playerMap, InputMap inputMap,
            int inputDeadzone, int inputSensitivity, AbstractProvider... providers )
    {
        setPlayerNumber( player );
        
        // Assign the maps
        mPlayerMap = playerMap;
        mInputMap = inputMap;
        mDeadzoneFraction = ( (float) inputDeadzone ) / 100f;
        mSensitivityFraction = ( (float) inputSensitivity ) / 100f;
        
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
    @TargetApi( 16 )
    @Override
    public void onInput( int inputCode, float strength, int hardwareId )
    {
        // Process user inputs from keyboard, gamepad, etc.
        if( mPlayerMap.testHardware( hardwareId, mPlayerNumber ) )
        {
            // Update the registered vibrator for this player
            if( AppData.IS_JELLY_BEAN )
            {
                InputDevice device = InputDevice.getDevice( hardwareId );
                if( device != null )
                    CoreInterface.registerVibrator( mPlayerNumber, device.getVibrator() );
            }
            
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
        boolean keyDown = strength > AbstractProvider.STRENGTH_THRESHOLD;
        int n64Index = mInputMap.get( inputCode );
        
        if( n64Index >= 0 && n64Index < NUM_N64_BUTTONS )
        {
            mState.buttons[n64Index] = keyDown;
            return true;
        }
        else if( n64Index < InputMap.NUM_N64_CONTROLS )
        {
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
            float rawX = mSensitivityFraction * ( mStrengthXpos - mStrengthXneg );
            float rawY = mSensitivityFraction * ( mStrengthYpos - mStrengthYneg );
            float magnitude = FloatMath.sqrt( ( rawX * rawX ) + ( rawY * rawY ) );
            
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
        }
        else if( keyDown )
        {
            switch( n64Index )
            {
                case InputMap.FUNC_INCREMENT_SLOT:
                    Log.v( "PeripheralController", "FUNC_INCREMENT_SLOT" );
                    if( GameMenuHandler.sInstance != null )
                        GameMenuHandler.sInstance.setSlot( GameMenuHandler.sInstance.mSlot + 1, true );
                    break;
                case InputMap.FUNC_SAVE_SLOT:
                    Log.v( "PeripheralController", "FUNC_SAVE_SLOT" );
                    CoreInterfaceNative.emuSaveSlot();
                    break;
                case InputMap.FUNC_LOAD_SLOT:
                    Log.v( "PeripheralController", "FUNC_LOAD_SLOT" );
                    CoreInterfaceNative.emuLoadSlot();
                    break;
                case InputMap.FUNC_STOP:
                    Log.v( "PeripheralController", "FUNC_STOP" );
                    CoreInterfaceNative.emuStop();
                    break;
                case InputMap.FUNC_PAUSE:
                    Log.v( "PeripheralController", "FUNC_PAUSE" );
                    if( doPause )
                        CoreInterface.pauseEmulator( false );
                    else
                        CoreInterface.resumeEmulator();
                    doPause = !doPause;
                    break;
                case InputMap.FUNC_FAST_FORWARD:
                    Log.v( "PeripheralController", "FUNC_FAST_FORWARD" );
                    CoreInterfaceNative.emuSetSpeed( 300 );
                    break;
                case InputMap.FUNC_FRAME_ADVANCE:
                    Log.v( "PeripheralController", "FUNC_FRAME_ADVANCE" );
                    CoreInterfaceNative.emuAdvanceFrame();
                    break;
                case InputMap.FUNC_SPEED_UP:
                    Log.v( "PeripheralController", "FUNC_SPEED_UP" );
                    speedOffset += SPEED_INC;
                    setSpeed();
                    break;
                case InputMap.FUNC_SPEED_DOWN:
                    Log.v( "PeripheralController", "FUNC_SPEED_DOWN" );
                    speedOffset -= SPEED_INC;
                    setSpeed();
                    break;
                case InputMap.FUNC_GAMESHARK:
                    Log.v( "PeripheralController", "FUNC_GAMESHARK" );
                    CoreInterfaceNative.emuGameShark( true );
                    break;
                case InputMap.FUNC_SIMULATE_BACK:
                    String[] back_cmd = { "input", "keyevent", String.valueOf( KeyEvent.KEYCODE_BACK ) };
                    SafeMethods.exec( back_cmd, false );
                    break;
                case InputMap.FUNC_SIMULATE_MENU:
                    String[] menu_cmd = { "input", "keyevent", String.valueOf( KeyEvent.KEYCODE_MENU ) };
                    SafeMethods.exec( menu_cmd, false );
                    break;
// TODO: Less hackish method of synchronizing slots and speeds between PeripheralController and GameMenuHandler
                default:
                    return false;
            }
        }
        else
        {
            switch( n64Index )
            {
                case InputMap.FUNC_FAST_FORWARD:
                    Log.v( "PeripheralController", "FUNC_FAST_FORWARD" );
                    if( GameMenuHandler.sInstance != null && GameMenuHandler.sInstance.mCustomSpeed )
                        CoreInterfaceNative.emuSetSpeed( GameMenuHandler.sInstance.mSpeedFactor );
                    else
                        CoreInterfaceNative.emuSetSpeed( 100 );
                    break;
                case InputMap.FUNC_GAMESHARK:
                    Log.v( "PeripheralController", "FUNC_GAMESHARK" );
                    CoreInterfaceNative.emuGameShark( false );
                    break;
                default:
                    return false;
            }
        }
        return true;
    }
    
    private void setSpeed()
    {
        int speed = 100;
        if( GameMenuHandler.sInstance != null && GameMenuHandler.sInstance.mCustomSpeed )
            speed = GameMenuHandler.sInstance.mSpeedFactor;
        
        speed += speedOffset;
        
        // Clamp the speed to valid values.
        speed = Utility.clamp( speed, GameMenuHandler.MIN_SPEED_FACTOR,
                GameMenuHandler.MAX_SPEED_FACTOR );
        
        CoreInterfaceNative.emuSetSpeed( speed );
    }
}
