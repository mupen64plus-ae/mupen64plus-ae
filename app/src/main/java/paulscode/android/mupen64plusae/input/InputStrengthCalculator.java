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
 * Authors: Nick Little
 */
package paulscode.android.mupen64plusae.input;

import android.util.SparseArray;
import paulscode.android.mupen64plusae.input.InputEntry.MutableFloat;
import paulscode.android.mupen64plusae.input.map.InputMap;

/**
 * A class for calculating the strength of an input based on the N64 index.
 */
public class InputStrengthCalculator
{
    public static final int INPUT_COUNT = InputMap.NUM_MAPPABLES;

    /** The strength from the last inputs. */
    private final InputEntry.MutableFloat[][] mLastInputStrengths =  new MutableFloat[INPUT_COUNT][];

    /**
     * Creates an input strength calculator for an input map and populates an entry map to match.
     * 
     * @param inputMap  The map from input codes to N64/Mupen commands.
     * @param entryMap  The entry map to populate.
     */
    public InputStrengthCalculator( InputMap inputMap, SparseArray<InputEntry> entryMap )
    {
        // Create the entry map
        int[] entryCount = new int[INPUT_COUNT];
        SparseArray<InputEntry> oldEntries = entryMap.clone();
        
        entryMap.clear();
        
        for( int i = 0; i < inputMap.size(); i++ )
        {
            int key = inputMap.keyAt( i );
            int n64Index = inputMap.valueAt( i );
            InputEntry oldEntry = oldEntries.get( key );
            
            if( oldEntry != null && oldEntry.mN64Index == n64Index )
                entryMap.put( key, oldEntry );
            else
                entryMap.put( key, new InputEntry( n64Index ) );
            
            if( n64Index >= 0 && n64Index < entryCount.length )
                entryCount[n64Index]++;
        }
        
        // Organize the input entries by N64 control index for fast iteration.
        for( int i = 0; i < entryCount.length; i++ )
        {
            mLastInputStrengths[i] = new MutableFloat[entryCount[i]];
        }
        
        int[] entryIndex = new int[INPUT_COUNT];
        
        for( int i = 0; i < inputMap.size(); i++ )
        {
            int n64Index = inputMap.valueAt( i );
            
            if( n64Index >= 0 && n64Index < entryIndex.length )
            {
                mLastInputStrengths[n64Index][entryIndex[n64Index]] =
                        entryMap.get( inputMap.keyAt( i ) ).getStrength();
                entryIndex[n64Index]++;
            }
        }
    }
    
    /**
     * Calculates the strength of a particular input.
     * 
     * @param n64Index  The input to calculate the strength for.
     * @return The strength of the input between 0 and 1.
     */
    public float calculate( int n64Index )
    {
        float strength = 0;
        
        if( n64Index >= 0 && n64Index < mLastInputStrengths.length && mLastInputStrengths[n64Index] != null )
        {
            MutableFloat[] inputStrengths = mLastInputStrengths[n64Index];
            
            // Determine the maximum strength from all possible inputs that map to the control.
            for( int i = 0; i < inputStrengths.length; i++ )
            {
                strength = Math.max( strength, inputStrengths[i].get() );
            }
        }
        
        return strength;
    }
    
}
