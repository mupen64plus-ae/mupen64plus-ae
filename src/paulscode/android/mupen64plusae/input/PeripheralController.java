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

import paulscode.android.mupen64plusae.GameMenuHandler;
import paulscode.android.mupen64plusae.NativeMethods;
import paulscode.android.mupen64plusae.input.map.InputMap;
import paulscode.android.mupen64plusae.input.map.PlayerMap;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import android.util.Log;
import paulscode.android.mupen64plusae.util.Utility;

/**
 * A class for generating N64 controller commands from peripheral hardware (gamepads, joysticks,
 * keyboards, mice, etc.).
 */
public class PeripheralController extends AbstractController implements
        AbstractProvider.OnInputListener, InputMap.Listener
{
    /** The map from hardware identifiers to players. */
    private final PlayerMap mPlayerMap;
    
    /** The map from input codes to N64/Mupen commands. */
    private final InputMap mInputMap;
    
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
     * @param player The player number, between 1 and 4, inclusive.
     * @param playerMap The map from hardware identifiers to players.
     * @param inputMap The map from input codes to N64/Mupen commands.
     * @param providers The user input providers. Null elements are safe.
     */
    public PeripheralController( int player, PlayerMap playerMap, InputMap inputMap,
            AbstractProvider... providers )
    {
        setPlayerNumber( player );
        
        // Assign the maps and listen for changes
        mPlayerMap = playerMap;
        mInputMap = inputMap;
        if( mInputMap != null )
            mInputMap.registerListener( this );
        
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
        if( mPlayerMap.testHardware( hardwareId, mPlayerNumber ) )
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
        if( mPlayerMap.testHardware( hardwareId, mPlayerNumber ) )
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
                case InputMap.FUNC_INCREMENT_SLOT:
                    Log.v( "PeripheralController", "FUNC_INCREMENT_SLOT" );
                    if( GameMenuHandler.sInstance != null )
                        GameMenuHandler.sInstance.setSlot( GameMenuHandler.sInstance.mSlot + 1, true );
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
                    NativeMethods.resetEmulator();
                    // TODO: NativeMethods.resetEmulator() needs some fine-tuning
                    break;
                case InputMap.FUNC_STOP:
                    Log.v( "PeripheralController", "FUNC_STOP" );
                    NativeMethods.stopEmulator();
                    break;
                case InputMap.FUNC_PAUSE:
                    Log.v( "PeripheralController", "FUNC_PAUSE" );
                    if( doPause )
                        NativeMethods.pauseEmulator();
                    else
                        NativeMethods.resumeEmulator();
                    doPause = !doPause;
                    break;
                case InputMap.FUNC_FAST_FORWARD:
                    Log.v( "PeripheralController", "FUNC_FAST_FORWARD" );
                    NativeMethods.stateSetSpeed( 300 );
                    break;
                case InputMap.FUNC_FRAME_ADVANCE:
                    Log.v( "PeripheralController", "FUNC_FRAME_ADVANCE" );
/*****
TODO:
Frame advance is hard-coded into the core, without an equivalent function in the API to call from the front-end.
Possible implementation utilizing the available M64CMD_SET_FRAME_CALLBACK instead:
   1) Pause the emulator (utilize state change callback to ensure emulation has paused)
   2) Register a frame callback
   3) Start the emulator
   4) When frame callback is called, pause the emulator
*****/
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
/*****
TODO:
Gameshark button emulation is hard-coded into the core, without an equivalent function in the API to call from the front-end.
Possible impementation without modifying the core?  Maybe inject M64CMD_SEND_SDL_KEYUP and M64CMD_SEND_SDL_KEYDOWN?
*****/
                    break;
// TODO: Less hackish method of synchronizing slots and speeds between PeripheralController and GameMenuHandler
                default:
                    return false;
            }
            return true;
        }
        else if( !keyDown )
        {
            switch( n64Index )
            {
                case InputMap.FUNC_FAST_FORWARD:
                    Log.v( "PeripheralController", "FUNC_FAST_FORWARD" );
                    if( GameMenuHandler.sInstance != null && GameMenuHandler.sInstance.mCustomSpeed )
                        NativeMethods.stateSetSpeed( GameMenuHandler.sInstance.mSpeedFactor );
                    else
                        NativeMethods.stateSetSpeed( 100 );
                    break;
                case InputMap.FUNC_GAMESHARK:
                    Log.v( "PeripheralController", "FUNC_GAMESHARK" );
                    // TODO: Release gameshark button
                    break;
                default:
                    return false;
            }
            return true;
        }
        return false;
    }
    private void setSpeed()
    {
        int speed = 100;
        if( GameMenuHandler.sInstance != null && GameMenuHandler.sInstance.mCustomSpeed )
            speed = GameMenuHandler.sInstance.mSpeedFactor;

        speed += speedOffset;

        // Clamp the speed to valid values.
        speed = Utility.clamp(speed, GameMenuHandler.MIN_SPEED_FACTOR, GameMenuHandler.MAX_SPEED_FACTOR);

        NativeMethods.stateSetSpeed( speed );
    }
}
