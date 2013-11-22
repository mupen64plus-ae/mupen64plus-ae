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
package paulscode.android.mupen64plusae.input.map;

import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.input.InputMapActivity;
import paulscode.android.mupen64plusae.input.PeripheralController;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;

/**
 * A class for mapping arbitrary user inputs to N64 buttons/axes.
 * 
 * @see AbstractProvider
 * @see PeripheralController
 * @see InputMapActivity
 */
public class InputMap extends SerializableMap
{
    // Default maps for various popular devices
    public static final String DEFAULT_INPUT_MAP_STRING_GENERIC = "0:22,1:21,2:20,3:19,4:108,5:-35,6:99,7:96,8:-23,9:-24,10:-29,11:-30,12:103,13:102,16:-1,17:-2,18:-3,19:-4";
    public static final String DEFAULT_INPUT_MAP_STRING_OUYA = "0:22,1:21,2:20,3:19,4:100,5:-35,6:99,7:96,8:-23,9:-24,10:-29,11:-30,12:103,13:102,16:-1,17:-2,18:-3,19:-4,32:97";
    public static final String DEFAULT_INPUT_MAP_STRING_N64_ADAPTER = "0:201,1:203,2:202,3:200,4:197,5:196,6:190,7:189,8:-30,9:-29,10:-23,11:-24,12:195,13:194,16:-1,17:-2,18:-3,19:-4"; 
    public static final String DEFAULT_INPUT_MAP_STRING_PS3 = "0:22,1:21,2:20,3:19,4:108,5:-35,6:99,7:96,8:-23,9:-24,10:-29,11:-30,12:103,13:102,16:-1,17:-2,18:-3,19:-4";
    public static final String DEFAULT_INPUT_MAP_STRING_XBOX360 = "0:-31,1:-32,2:-33,3:-34,4:108,5:-23,6:99,7:96,8:-25,9:-26,10:-27,11:-28,12:103,13:102,16:-1,17:-2,18:-3,19:-4";
    public static final String DEFAULT_INPUT_MAP_STRING_XPERIA_PLAY = "0:22,1:21,2:20,3:19,4:108,5:102,6:99,7:23,12:103";
    
    // @formatter:off
    /** Map flag: Input code is not mapped. */
    public static final int UNMAPPED                    = -1;

    /** Map offset: N64 non-button controls. */
    public static final int OFFSET_EXTRAS               = AbstractController.NUM_N64_BUTTONS;
    
    /** N64 control: analog-right. */
    public static final int AXIS_R                      = OFFSET_EXTRAS;
    
    /** N64 control: analog-left. */
    public static final int AXIS_L                      = OFFSET_EXTRAS + 1;
    
    /** N64 control: analog-down. */
    public static final int AXIS_D                      = OFFSET_EXTRAS + 2;
    
    /** N64 control: analog-up. */
    public static final int AXIS_U                      = OFFSET_EXTRAS + 3;
    
    /** Total number of N64 controls. */
    public static final int NUM_N64_CONTROLS            = OFFSET_EXTRAS + 4;
    
    /** Map offset: Mupen64Plus global functions. */
    public static final int OFFSET_GLOBAL_FUNCS         = NUM_N64_CONTROLS;

    /** Mupen64Plus function: increment slot. */
    public static final int FUNC_INCREMENT_SLOT         = OFFSET_GLOBAL_FUNCS;
    
    /** Mupen64Plus function: save state. */
    public static final int FUNC_SAVE_SLOT              = OFFSET_GLOBAL_FUNCS + 1;
    
    /** Mupen64Plus function: load state. */
    public static final int FUNC_LOAD_SLOT              = OFFSET_GLOBAL_FUNCS + 2;
    
    /** Mupen64Plus function: reset. */
    public static final int FUNC_RESET                  = OFFSET_GLOBAL_FUNCS + 3;
    
    /** Mupen64Plus function: stop. */
    public static final int FUNC_STOP                   = OFFSET_GLOBAL_FUNCS + 4;
    
    /** Mupen64Plus function: pause. */
    public static final int FUNC_PAUSE                  = OFFSET_GLOBAL_FUNCS + 5;
    
    /** Mupen64Plus function: fast-forward. */
    public static final int FUNC_FAST_FORWARD           = OFFSET_GLOBAL_FUNCS + 6;
    
    /** Mupen64Plus function: advance frame. */
    public static final int FUNC_FRAME_ADVANCE          = OFFSET_GLOBAL_FUNCS + 7;
    
    /** Mupen64Plus function: increase speed. */
    public static final int FUNC_SPEED_UP               = OFFSET_GLOBAL_FUNCS + 8;
    
    /** Mupen64Plus function: decrease speed. */
    public static final int FUNC_SPEED_DOWN             = OFFSET_GLOBAL_FUNCS + 9;
    
    /** Mupen64Plus function: Gameshark. */
    public static final int FUNC_GAMESHARK              = OFFSET_GLOBAL_FUNCS + 10;
    
    /** Simulated key-press function: Back. */
    public static final int FUNC_SIMULATE_BACK          = OFFSET_GLOBAL_FUNCS + 11;
    
    /** Simulated key-press function: Menu. */
    public static final int FUNC_SIMULATE_MENU          = OFFSET_GLOBAL_FUNCS + 12;
    
    /** Total number of mappable controls/functions. */
    public static final int NUM_MAPPABLES               = OFFSET_GLOBAL_FUNCS + 13;
    // @formatter:on
    
    /**
     * Instantiates a new input map.
     */
    public InputMap()
    {
    }
    
    /**
     * Instantiates a new input map from a serialization.
     * 
     * @param serializedMap The serialization of the map.
     */
    public InputMap( String serializedMap )
    {
        super( serializedMap );
    }
    
    /**
     * Gets the N64/Mupen command mapped to a given input code.
     * 
     * @param inputCode The standardized input code.
     * 
     * @return The N64/Mupen command the code is mapped to, or UNMAPPED.
     * 
     * @see AbstractProvider
     * @see InputMap#UNMAPPED
     */
    public int get( int inputCode )
    {
        return mMap.get( inputCode, UNMAPPED );
    }
    
    /**
     * Maps an input code to an N64/Mupen command.
     * 
     * @param inputCode The standardized input code to be mapped.
     * @param command   The index to the N64/Mupen command.
     */
    public void map( int inputCode, int command )
    {
        // Map the input if a valid index was given
        if( command >= 0 && command < NUM_MAPPABLES && inputCode != 0 )
        {
            if( inputCode < 0 )
            {
                // If an analog input is mapped, it should be the only thing mapped to this command
                unmapCommand( command );
            }
            else
            {
                // If a digital input is mapped, no analog inputs can be mapped to this command
                for( int i = mMap.size() - 1; i >= 0; i-- )
                {
                    if( mMap.valueAt( i ) == command && mMap.keyAt( i ) < 0 )
                        mMap.removeAt( i );
                }                
            }
            mMap.put( inputCode, command );
        }
    }
    
    /**
     * Unmaps an input code.
     * 
     * @param inputCode The standardized input code to be unmapped.
     */
    public void unmap( int inputCode )
    {
        mMap.delete( inputCode );
    }
    
    /**
     * Unmaps an N64/Mupen command.
     * 
     * @param command The index to the N64/Mupen command.
     */
    public void unmapCommand( int command )
    {
        // Remove any matching key-value pairs (count down to accommodate removal)
        for( int i = mMap.size() - 1; i >= 0; i-- )
        {
            if( mMap.valueAt( i ) == command )
                mMap.removeAt( i );
        }
    }
    
    /**
     * Checks if an N64/Mupen command is mapped to at least one input code.
     * 
     * @param command The index to the N64/Mupen64 command.
     * 
     * @return True, if the mapping exists.
     */
    public boolean isMapped( int command )
    {
        return mMap.indexOfValue( command ) >= 0;
    }
    
    /**
     * Gets a description of the input codes mapped to an N64/Mupen command.
     * 
     * @param command The index to the N64/Mupen command.
     * 
     * @return Description of the input codes mapped to the given command.
     */
    public String getMappedCodeInfo( int command )
    {
        String result = "";
        for( int i = 0; i < mMap.size(); i++ )
        {
            if( mMap.valueAt( i ) == command )
            {
                result += AbstractProvider.getInputName( mMap.keyAt( i ) ) + "\n";
            }
        }
        return result.trim();
    }
}
