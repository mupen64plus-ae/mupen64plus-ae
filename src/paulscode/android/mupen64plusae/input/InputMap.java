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

import paulscode.android.mupen64plusae.util.SubscriptionManager;
import android.util.SparseIntArray;

public class InputMap
{
    public interface Listener
    {
        public void onMapChanged( InputMap newValue );
    }
    
    // Indices into N64 inputs array
    public static final int UNMAPPED = -1;
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
    
    private int[] mN64ToCode;
    private SparseIntArray mCodeToN64;
    private SubscriptionManager<Listener> mPublisher;
    
    private final float STRENGTH_THRESHOLD = 0.5f;
    
    public InputMap()
    {
        mN64ToCode = new int[NUM_INPUTS];
        mCodeToN64 = new SparseIntArray( NUM_INPUTS );
        mPublisher = new SubscriptionManager<InputMap.Listener>();
    }
    
    public InputMap( String serializedMap )
    {
        this();
        deserialize( serializedMap );
    }
    
    public void apply( int inputCode, float strength, AbstractController controller )
    {
        boolean state = strength > STRENGTH_THRESHOLD;
        switch( get( inputCode ) )
        {
            case DPD_R:
                controller.mDpadR = state;
                break;
            case DPD_L:
                controller.mDpadL = state;
                break;
            case DPD_D:
                controller.mDpadD = state;
                break;
            case DPD_U:
                controller.mDpadU = state;
                break;
            case START:
                controller.mBtnStart = state;
                break;
            case BTN_Z:
                controller.mBtnZ = state;
                break;
            case BTN_B:
                controller.mBtnB = state;
                break;
            case BTN_A:
                controller.mBtnA = state;
                break;
            case CPD_R:
                controller.mBtnCR = state;
                break;
            case CPD_L:
                controller.mBtnCL = state;
                break;
            case CPD_D:
                controller.mBtnCD = state;
                break;
            case CPD_U:
                controller.mBtnCU = state;
                break;
            case BTN_R:
                controller.mBtnR = state;
                break;
            case BTN_L:
                controller.mBtnL = state;
                break;
            case AXIS_R:
                controller.mAxisFractionX = strength;
                break;
            case AXIS_L:
                controller.mAxisFractionX = -strength;
                break;
            case AXIS_D:
                controller.mAxisFractionY = -strength;
                break;
            case AXIS_U:
                controller.mAxisFractionY = strength;
                break;
            default:
                break;
        }
    }
    
    public int get( int inputCode )
    {
        return mCodeToN64.get( inputCode, UNMAPPED );
    }
    
    public int[] getMappedInputCodes()
    {
        return mN64ToCode.clone();
    }
    
    public void mapInput( int n64Index, int inputCode )
    {
        mapInput( inputCode, n64Index, true );
    }
    
    public void unmapInput( int n64Index )
    {
        mapInput( 0, n64Index, true );
    }
    
    public void registerListener( Listener listener )
    {
        mPublisher.subscribe( listener );
    }
    
    public void unregisterListener( Listener listener )
    {
        mPublisher.unsubscribe( listener );
    }
    
    public String serialize()
    {
        // Serialize the map values to a comma-delimited string
        String result = "";
        for( int i = 0; i < mN64ToCode.length; i++ )
        {
            result += mN64ToCode[i] + ",";
        }
        return result;
    }
    
    public void deserialize( String s )
    {
        // Reset the map
        for( int i = 0; i < mN64ToCode.length; i++ )
            mapInput( 0, i, false );
        
        // Parse the new map values from the comma-delimited string
        if( s != null )
        {
            String[] strings = s.split( "," );
            for( int i = 0; i < Math.min( NUM_INPUTS, strings.length ); i++ )
                mapInput( safeParse( strings[i], 0 ), i, false );
        }
        
        // Notify the listeners that the map has changed
        notifyListeners();
    }
    
    private int safeParse( String s, int defaultValue )
    {
        try
        {
            return Integer.parseInt( s );
        }
        catch( NumberFormatException ex )
        {
            return defaultValue;
        }
    }

    private void mapInput( int inputCode, int n64Index, boolean notify )
    {
        // Map the input if a valid index was given
        if( n64Index >= 0 && n64Index < NUM_INPUTS )
        {
            // Get the old code that was mapped to the new index
            int oldInputCode = mN64ToCode[n64Index];
    
            // Get the old index that was mapped to the new code
            int oldN64Index = get( inputCode ); 
    
            // Unmap the new code from the old index
            if( oldN64Index != UNMAPPED )
                mN64ToCode[oldN64Index] = 0;
            
            // Unmap the old code from the new index
            mCodeToN64.delete( oldInputCode );
    
            // Map the new code to the new index
            mN64ToCode[n64Index] = inputCode;
            
            // Map the new index to the new code
            if( inputCode != 0 )
                mCodeToN64.put( inputCode, n64Index );
        }
        
        // Notify listeners that the map has changed
        if( notify )
            notifyListeners();
    }

    protected void notifyListeners()
    {
        for( Listener listener : mPublisher.getSubscribers() )
            listener.onMapChanged( this );
    }
}
