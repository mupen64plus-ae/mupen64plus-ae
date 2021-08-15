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
import paulscode.android.mupen64plusae.input.PeripheralController;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae.profile.ControllerProfileActivity;

/**
 * A class for mapping arbitrary user inputs to N64 buttons/axes.
 * 
 * @see AbstractProvider
 * @see PeripheralController
 * @see ControllerProfileActivity
 */
public class InputMap extends SerializableMap
{
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
    
    /** Mupen64Plus function: Screenshot. */
    public static final int FUNC_SCREENSHOT             = OFFSET_GLOBAL_FUNCS + 13;
    
    /** Mupen64Plus function: activate/deactivate sensor. */
    public static final int FUNC_SENSOR_TOGGLE          = OFFSET_GLOBAL_FUNCS + 14;

    /** Mupen64Plus function: decrement slot. */
    public static final int FUNC_DECREMENT_SLOT         = OFFSET_GLOBAL_FUNCS + 15;

    /** Total number of mappable controls/functions. */
    public static final int NUM_MAPPABLES               = OFFSET_GLOBAL_FUNCS + 16;

    // @formatter:on
    
    /**
     * Returns true if the given input code is an analog input and false otherwise.
     * 
     * @param inputCode The standardized input code.
     */
    public static final boolean isAnalogInput( int inputCode )
    {
        return inputCode < 0;
    }
    
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
        StringBuilder result = new StringBuilder();
        for( int i = 0; i < mMap.size(); i++ )
        {
            if( mMap.valueAt( i ) == command )
            {
                result.append(AbstractProvider.getInputName(mMap.keyAt(i))).append("\n");
            }
        }
        return result.toString().trim();
    }
    
    /**
     * Gets the size of the map.
     * 
     * @return The size of the map.
     */
    public int size()
    {
        return mMap.size();
    }
    
    /**
     * Gets the key at the specified index in the map.
     * 
     * @return The key at the specified index of the map.
     */
    public int keyAt( int index )
    {
        return mMap.keyAt( index );
    }
    
    /**
     * Gets the value at the specified index in the map.
     * 
     * @return The value at the specified index of the map.
     */
    public int valueAt( int index )
    {
        return mMap.valueAt( index );
    }
}
