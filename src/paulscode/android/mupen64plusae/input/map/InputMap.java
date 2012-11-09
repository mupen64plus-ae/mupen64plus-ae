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
package paulscode.android.mupen64plusae.input.map;

import paulscode.android.mupen64plusae.InputMapPreference;
import paulscode.android.mupen64plusae.input.AbstractController;
import paulscode.android.mupen64plusae.input.PeripheralController;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae.util.SafeMethods;
import paulscode.android.mupen64plusae.util.SubscriptionManager;
import android.util.SparseIntArray;

/**
 * A class for mapping arbitrary user inputs to N64 buttons/axes.
 * 
 * @see AbstractProvider
 * @see PeripheralController
 * @see InputMapPreference
 */
public class InputMap
{
    public interface Listener
    {
        public void onMapChanged( InputMap map );
    }
    
    // Array indices for extra N64 controls
    // @formatter:off
    public static final int UNMAPPED        = -1;
    public static final int OFFSET_EXTRAS   = AbstractController.NUM_BUTTONS;
    public static final int AXIS_R          = OFFSET_EXTRAS;
    public static final int AXIS_L          = OFFSET_EXTRAS + 1;
    public static final int AXIS_D          = OFFSET_EXTRAS + 2;
    public static final int AXIS_U          = OFFSET_EXTRAS + 3;
    public static final int BTN_RUMBLE      = OFFSET_EXTRAS + 4;
    public static final int BTN_MEMPAK      = OFFSET_EXTRAS + 5;
    public static final int NUM_N64INPUTS   = OFFSET_EXTRAS + 6;
    
    // Array indices for special functions
    public static final int OFFSET_FUNCS    = NUM_N64INPUTS;
    public static final int FUNC_STOP       = OFFSET_FUNCS;
    public static final int FUNC_FULLSCREEN = OFFSET_FUNCS + 1;
    public static final int FUNC_SAVESTATE  = OFFSET_FUNCS + 2;
    public static final int FUNC_LOADSTATE  = OFFSET_FUNCS + 3;
    public static final int FUNC_INCSLOT    = OFFSET_FUNCS + 4;
    public static final int FUNC_RESET      = OFFSET_FUNCS + 5;
    public static final int FUNC_SPEEDUP    = OFFSET_FUNCS + 6;
    public static final int FUNC_SPEEDDOWN  = OFFSET_FUNCS + 7;
    public static final int FUNC_SCREENSHOT = OFFSET_FUNCS + 8;
    public static final int FUNC_PAUSE      = OFFSET_FUNCS + 9;
    public static final int FUNC_MUTE       = OFFSET_FUNCS + 10;
    public static final int FUNC_VOLUP      = OFFSET_FUNCS + 11;
    public static final int FUNC_VOLDOWN    = OFFSET_FUNCS + 12;
    public static final int FUNC_FFWD       = OFFSET_FUNCS + 13;
    public static final int FUNC_FRAMEADV   = OFFSET_FUNCS + 14;
    public static final int FUNC_GAMESHARK  = OFFSET_FUNCS + 15;
    public static final int NUM_MAPPABLES   = OFFSET_FUNCS + 16;
    // @formatter:on
    
    // Strength above which button/axis is considered pressed
    public static final float STRENGTH_THRESHOLD = 0.5f;
    
    // Bidirectional map implementation
    private int[] mN64ToCode;
    private SparseIntArray mCodeToN64;
    
    // Enabled/disabled state
    private boolean mEnabled;
    
    // Listener management
    private SubscriptionManager<Listener> mPublisher;
    
    public InputMap()
    {
        mN64ToCode = new int[NUM_MAPPABLES];
        mCodeToN64 = new SparseIntArray( NUM_MAPPABLES );
        mPublisher = new SubscriptionManager<InputMap.Listener>();
    }
    
    public InputMap( String serializedMap )
    {
        this();
        deserialize( serializedMap );
    }
    
    public int get( int inputCode )
    {
        return mCodeToN64.get( inputCode, UNMAPPED );
    }
    
    public int[] getMappedInputCodes()
    {
        return mN64ToCode.clone();
    }
    
    public boolean isEnabled()
    {
        return mEnabled;
    }
    
    public void setEnabled( boolean value )
    {
        mEnabled = value;
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
        String result = mEnabled + ":";
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
            String[] groups = s.split( ":" );
            if( groups.length > 1 )
            {
                // Read the enabled state
                mEnabled = SafeMethods.toBoolean( groups[0], false );
                s = groups[1];
            }
            else
            {
                // Safety valve in case format changes
                mEnabled = false;
            }
            
            // Read the input mappings
            String[] inputs = s.split( "," );
            for( int i = 0; i < Math.min( NUM_MAPPABLES, inputs.length ); i++ )
                mapInput( SafeMethods.toInt( inputs[i], 0 ), i, false );
        }
        
        // Notify the listeners that the map has changed
        notifyListeners();
    }
    
    private void mapInput( int inputCode, int n64Index, boolean notify )
    {
        // Map the input if a valid index was given
        if( n64Index >= 0 && n64Index < NUM_MAPPABLES )
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
