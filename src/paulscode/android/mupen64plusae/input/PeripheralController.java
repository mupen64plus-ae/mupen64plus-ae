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

public class PeripheralController extends AbstractController implements AbstractProvider.Listener,
        InputMap.Listener
{
    private InputMap mInputMap;
    private KeyProvider mKeyProvider;
    private AxisProvider mAxisProvider;
    protected float mAxisFractionXpos;
    protected float mAxisFractionXneg;
    protected float mAxisFractionYpos;
    protected float mAxisFractionYneg;
    
    public PeripheralController( InputMap inputMap, KeyProvider keyProvider, AxisProvider axisProvider )
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
        
        // Connect the providers' destination
        if( mKeyProvider != null )
            mKeyProvider.registerListener( this );
        if( mAxisProvider != null )
            mAxisProvider.registerListener( this );
    }
    
    @Override
    public void onInput( int inputCode, float strength )
    {
        // Process user inputs from keyboard, gamepad, etc.
        if( mInputMap != null )
        {
            mAxisFractionXneg = mAxisFractionXpos = mAxisFractionYneg = mAxisFractionYpos = 0;
            apply( inputCode, strength );
            mAxisFractionX = mAxisFractionXpos - mAxisFractionXneg;
            mAxisFractionY = mAxisFractionYpos - mAxisFractionYneg;
            notifyChanged();
        }
    }
    
    @Override
    public void onInput( int[] inputCodes, float[] strengths )
    {
        // Process batch user inputs from gamepad, keyboard, etc.
        if( mInputMap != null )
        {
            mAxisFractionXneg = mAxisFractionXpos = mAxisFractionYneg = mAxisFractionYpos = 0;
            for( int i = 0; i < inputCodes.length; i++ )
                apply( inputCodes[i], strengths[i] );
            mAxisFractionX = mAxisFractionXpos - mAxisFractionXneg;
            mAxisFractionY = mAxisFractionYpos - mAxisFractionYneg;
            notifyChanged();
        }
    }
    
    @Override
    public void onMapChanged( InputMap map )
    {
        // If the button/axis mappings change, update the transform's listening filter
        if( mAxisProvider != null )
            mAxisProvider.setInputCodeFilter( map.getMappedInputCodes() );
    }
    
    public boolean apply( int inputCode, float strength )
    {
        boolean state = strength > InputMap.STRENGTH_THRESHOLD;
        int n64Index = mInputMap.get( inputCode );
        
        if( n64Index >= 0 && n64Index < NUM_BUTTONS )
        {
            mButtons[n64Index] = state;
        }
        else
        {
            switch( n64Index )
            {
                case InputMap.AXIS_R:
                    mAxisFractionXpos = strength;
                    break;
                case InputMap.AXIS_L:
                    mAxisFractionXneg = strength;
                    break;
                case InputMap.AXIS_D:
                    mAxisFractionYneg = strength;
                    break;
                case InputMap.AXIS_U:
                    mAxisFractionYpos = strength;
                    break;
                default:
                    return false;
            }
        }
        return true;
    }
}
