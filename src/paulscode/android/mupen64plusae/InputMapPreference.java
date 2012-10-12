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
package paulscode.android.mupen64plusae;

import java.util.HashMap;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class InputMapPreference extends DialogPreference
{
    // Indices into N64 inputs array
    public static final int DPD_R = 0;
    public static final int DPD_L = 1;
    public static final int DPD_D = 2;
    public static final int DPD_U = 3;
    public static final int START = 4;
    public static final int BTN_Z = 5;
    public static final int BTN_B = 6;
    public static final int BTN_A = 7;
    public static final int CPD_R = 8;
    public static final int CPD_L = 9;
    public static final int CPD_D = 10;
    public static final int CPD_U = 11;
    public static final int BTN_R = 12;
    public static final int BTN_L = 13;
    public static final int AXIS_R = 14;
    public static final int AXIS_L = 15;
    public static final int AXIS_D = 16;
    public static final int AXIS_U = 17;
    public static final int NUM_INPUTS = 18;
    
    private int[] mInputCodes;
    private HashMap<Integer, Integer> mN64Indices;
    
    public InputMapPreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        
        mInputCodes = new int[NUM_INPUTS];
        mN64Indices = new HashMap<Integer, Integer>( NUM_INPUTS );
        
        setDialogLayoutResource( R.layout.preferences_inputmap );
    }
    
    public void mapInput( int inputCode, int n64Index )
    {
        if( n64Index >= 0 && n64Index < NUM_INPUTS )
        {
            // Get the last code mapped to this index
            Integer oldInputCode = mInputCodes[n64Index];
            
            // Map the new code to this index
            mInputCodes[n64Index] = inputCode;
            
            // Get the last index mapped to this code
            // n64Indices(inputCode)
            
        }
    }
    
    public void unmapInput( int n64index )
    {
        
    }
    
    // public int getN64Axis(int axisCode)
    // {
    //
    // }
    //
    // public int getN64Button(int buttonCode)
    // {
    //
    // }
    //
    // public int getAxisCode(int n64Button)
    // {
    //
    // }
    //
    // public int getButtonCode(int n64Button)
    // {
    //
    // }
    //
    // public static String getCodeText(int code)
    // {
    //
    // }
    //
    // public int getHashFromN64Button(int hash)
    // {
    //
    // }
    //
    // public int getN64ButtonFromHash(int n64Button)
    // {
    //
    // }
}
