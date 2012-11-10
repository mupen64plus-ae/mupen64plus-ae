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
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.input;

import paulscode.android.mupen64plusae.input.map.InputMap;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae.input.provider.AxisProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider;

/**
 * A class for generating N64 controller commands from peripheral hardware (gamepads, joysticks,
 * keyboards, mice, etc.).
 */
public class PeripheralController extends AbstractController implements AbstractProvider.Listener,
        InputMap.Listener
{
    /** The map from hardware codes to N64 commands. */
    private final InputMap mInputMap;
    
    /** The provider for button/key inputs. */
    private final KeyProvider mKeyProvider;
    
    /** The provider for analog inputs (may be null depending on API level). */
    private final AxisProvider mAxisProvider;
    
    /** The positive analog-x strength, between 0 and 1, inclusive. */
    private float mStrengthXpos;
    
    /** The negative analog-x strength, between 0 and 1, inclusive. */
    private float mStrengthXneg;
    
    /** The positive analog-y strength, between 0 and 1, inclusive. */
    private float mStrengthYpos;
    
    /** The negative analogy-y strength, between 0 and 1, inclusive. */
    private float mStrengthYneg;
    
    /**
     * Instantiates a new peripheral controller.
     * 
     * @param inputMap The map from hardware codes to N64 commands.
     * @param keyProvider The key provider. Null values are safe.
     * @param axisProvider The axis provider. Null values are safe.
     */
    public PeripheralController( InputMap inputMap, KeyProvider keyProvider,
            AxisProvider axisProvider )
    {
        // Assign the map and listen for changes
        mInputMap = inputMap;
        if( mInputMap != null )
        {
            onMapChanged( mInputMap );
            mInputMap.registerListener( this );
        }
        
        // Assign the providers
        mKeyProvider = keyProvider;
        mAxisProvider = axisProvider;
        
        // Start listening to the providers
        if( mKeyProvider != null )
            mKeyProvider.registerListener( this );
        if( mAxisProvider != null )
            mAxisProvider.registerListener( this );
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
        if( mInputMap != null )
        {
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
        if( mInputMap != null )
        {
            // Apply user changes to the controller state
            for( int i = 0; i < inputCodes.length; i++ )
                apply( inputCodes[i], strengths[i] );
            
            // Notify the core that controller state has changed
            notifyChanged();
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * paulscode.android.mupen64plusae.input.map.InputMap.Listener#onMapChanged(paulscode.android
     * .mupen64plusae.input.map.InputMap)
     */
    @Override
    public void onMapChanged( InputMap map )
    {
        // Update the axis provider's notification filter
        if( mAxisProvider != null )
            mAxisProvider.setInputCodeFilter( map.getMappedInputCodes() );
    }
    
    /**
     * Apply user input to the N64 controller state.
     * 
     * @param inputCode The universal input code that was dispatched.
     * @param strength The input strength, between 0 and 1, inclusive.
     * @return True, if controller state changed.
     */
    private boolean apply( int inputCode, float strength )
    {
        boolean state = strength > AbstractProvider.STRENGTH_THRESHOLD;
        int n64Index = mInputMap.get( inputCode );
        
        if( n64Index >= 0 && n64Index < NUM_N64_BUTTONS )
        {
            mButtonState[n64Index] = state;
        }
        else
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
            
            // Update the net position of the analog stick
            mAxisFractionX = mStrengthXpos - mStrengthXneg;
            mAxisFractionY = mStrengthYpos - mStrengthYneg;
        }
        return true;
    }
}
