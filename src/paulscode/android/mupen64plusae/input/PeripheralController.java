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

import java.util.ArrayList;

import paulscode.android.mupen64plusae.NativeMethods;
import paulscode.android.mupen64plusae.input.map.InputMap;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import android.util.Log;

/**
 * A class for generating N64 controller commands from peripheral hardware (gamepads, joysticks,
 * keyboards, mice, etc.).
 */
public class PeripheralController extends AbstractController implements AbstractProvider.OnInputListener,
        InputMap.Listener
{
    /** The map from hardware codes to N64 commands. */
    private final InputMap mInputMap;
    
    /** The ID of the hardware to listen to. */
    private int mHardwareIdFilter;
    
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
    
    /**
     * Instantiates a new peripheral controller.
     * 
     * @param player The player number, between 1 and 4, inclusive.
     * @param inputMap The map from hardware codes to N64 commands.
     * @param providers The user input providers. Null elements are safe.
     */
    public PeripheralController( int player, InputMap inputMap, AbstractProvider... providers )
    {
        setPlayerNumber( player );
        
        // Assign the map and listen for changes
        mInputMap = inputMap;
        if( mInputMap != null )
            mInputMap.registerListener( this );
        
        mHardwareIdFilter = 0;          
        // TODO: Create user interface for mapping peripheral to player, and set filter here
        // TODO: Only set filter in multi-player mode (otherwise assume everything player 1)
        
        // Assign the non-null input providers
        mProviders = new ArrayList<AbstractProvider>();
        for( AbstractProvider provider : providers )
            if( provider != null )
            {
                mProviders.add( provider );
                provider.registerListener( this );
            }
        
        // Make listening optimizations based on the input map
        onMapChanged( mInputMap );
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
            if( mHardwareIdFilter == 0 || hardwareId == 0 || mHardwareIdFilter == hardwareId )
            {            
                // Apply user changes to the controller state
                apply( inputCode, strength );
                
                // Notify the core that controller state has changed
                notifyChanged();
            }
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
        // TODO: This optimization seems to have negative side-effects... buttons can't be held for long.
        // Update any axis providers' notification filters
        // for( AbstractProvider provider : mProviders )
        //     if( provider instanceof AxisProvider )
        //         ( (AxisProvider) provider ).setInputCodeFilter( map.getMappedInputCodes() );
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
            
            // Update the net position of the analog stick
            mState.axisFractionX = mStrengthXpos - mStrengthXneg;
            mState.axisFractionY = mStrengthYpos - mStrengthYneg;
            return true;
        }
        else if( keyDown )
        {
            switch( n64Index )
            {
                case InputMap.BTN_RUMBLE:
                    Log.v( "PeripheralController", "BTN_RUMBLE" );
                    // TODO: Invoke rumble
                    break;
                case InputMap.BTN_MEMPAK:
                    Log.v( "PeripheralController", "BTN_MEMPAK" );
                    // TODO: Invoke mempak
                    break;
                case InputMap.FUNC_INCREMENT_SLOT:
                    Log.v( "PeripheralController", "FUNC_INCREMENT_SLOT" );
                    // TODO: Invoke increment slot
                    break;
                case InputMap.FUNC_SAVE_SLOT:
                    Log.v( "PeripheralController", "FUNC_SAVE_SLOT" );
                    NativeMethods.stateSaveEmulator();
                    break;
                case InputMap.FUNC_LOAD_SLOT:
                    Log.v( "PeripheralController", "FUNC_LOAD_SLOT" );
                    NativeMethods.stateLoadEmulator();
                    break;
                case InputMap.FUNC_RESET:
                    Log.v( "PeripheralController", "FUNC_RESET" );
                    // TODO: NativeMethods.resetEmulator() needs some fine-tuning
                    break;
                case InputMap.FUNC_STOP:
                    Log.v( "PeripheralController", "FUNC_STOP" );
                    // TODO: Invoke stop
                    break;
                case InputMap.FUNC_PAUSE:
                    Log.v( "PeripheralController", "FUNC_PAUSE" );
                    // TODO: Invoke pause
                    break;
                case InputMap.FUNC_FAST_FORWARD:
                    Log.v( "PeripheralController", "FUNC_FAST_FORWARD" );
                    // TODO: Invoke fast forward
                    break;
                case InputMap.FUNC_FRAME_ADVANCE:
                    Log.v( "PeripheralController", "FUNC_FRAME_ADVANCE" );
                    // TODO: Invoke frame advance
                    break;
                case InputMap.FUNC_SPEED_UP:
                    Log.v( "PeripheralController", "FUNC_SPEED_UP" );
                    // TODO: Invoke speed up
                    break;
                case InputMap.FUNC_SPEED_DOWN:
                    Log.v( "PeripheralController", "FUNC_SPEED_DOWN" );
                    // TODO: Invoke speed down
                    break;
                case InputMap.FUNC_GAMESHARK:
                    Log.v( "PeripheralController", "FUNC_GAMESHARK" );
                    // TODO: Invoke gameshark
                    break;
                default:
                    return false;
            }
            return true;
        }
        return false;
    }
}
