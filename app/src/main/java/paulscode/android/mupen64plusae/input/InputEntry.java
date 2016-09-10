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

/**
 * A class for getting the N64 index and specifying the strength of the input.
 */
public class InputEntry
{
    /**
     * An object wrapping a float that can be changed.
     */
    public static class MutableFloat
    {
        /** The value of the mutable float object. */
        public float mValue;
        
        /**
         * Gets the float value of the object.
         * 
         * @return The float value of the object.
         */
        public float get()
        {
            return mValue;
        }
        
        /**
         * Sets the float value of the object.
         * 
         * @param value The float value to set the object to.
         */
        public void set( float value )
        {
            mValue = value;
        }
    }
    
    /** The index of the N64 control that is affected by this input. */
    public final int mN64Index;
    
    /** The strength of the last detected input. */
    private MutableFloat nInputStrength = new MutableFloat();
    
    /**
     * Instantiates a new input entry.
     * 
     * @param n64Index The index of the N64 control that will be affected by the input.
     */
    public InputEntry(int n64Index)
    {
        mN64Index = n64Index;
    }
    
    /**
     * Gets the input strength of this entry.
     * 
     * @return The input strength of the entry.
     */
    public MutableFloat getStrength()
    {
        return nInputStrength;
    }
}