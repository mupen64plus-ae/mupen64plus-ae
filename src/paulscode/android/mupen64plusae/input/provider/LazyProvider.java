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
package paulscode.android.mupen64plusae.input.provider;

import java.util.ArrayList;

import android.view.MotionEvent;

/**
 * A provider class that aggregates inputs from other providers, and lazily notifies listeners only
 * when the aggregate input has changed significantly.
 */
public class LazyProvider extends AbstractProvider implements AbstractProvider.OnInputListener
{
    /** The delta-strength threshold above which an input is considered "changed". */
    private static final float STRENGTH_HYSTERESIS = 0.05f;
    
    /** The code of the most recent input. */
    private int mCurrentCode = 0;
    
    /** The strength of the most recent input, ranging from 0 to 1, inclusive. */
    private float mCurrentStrength = 0;
    
    /** The strength biases associated with each input channel, used to re-center raw analog values. */
    private float[] mStrengthBiases = null;
    
    /** The providers whose inputs are aggregated. */
    private final ArrayList<AbstractProvider> providers = new ArrayList<AbstractProvider>();
    
    /**
     * Adds an upstream provider to the aggregate.
     * 
     * @param provider The provider to add. Null values are safe.
     */
    public void addProvider( AbstractProvider provider )
    {
        if( provider != null )
        {
            provider.registerListener( this );
            providers.add( provider );
        }
    }
    
    /**
     * Removes an upstream provider from the aggregate.
     * 
     * @param provider The provider to remove. Null values are safe.
     */
    public void removeProvider( AbstractProvider provider )
    {
        if( provider != null )
        {
            provider.unregisterListener( this );
            providers.remove( provider );
        }
    }
    
    /**
     * Removes all upstream providers from the aggregate.
     */
    public void removeAllProviders()
    {
        for( AbstractProvider provider : providers )
            provider.unregisterListener( this );
        
        providers.clear();
    }
    
    /**
     * Resets the strength biases.
     */
    public void resetBiases()
    {
        // Setting biases to null will force a refresh on next input
        mStrengthBiases = null;
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
        if( inputCodes == null || strengths == null )
            return;
        
        // Reset the biases if necessary
        if( mStrengthBiases == null )
            mStrengthBiases = new float[strengths.length];
        
        // Update the strength biases if necessary
        updateBiases( inputCodes, strengths );
        
        // Find the strongest input
        float maxStrength = AbstractProvider.STRENGTH_THRESHOLD;
        int strongestInputCode = 0;
        for( int i = 0; i < inputCodes.length; i++ )
        {
            int inputCode = inputCodes[i];
            float strength = strengths[i];
            
            // Remove the bias in the channel
            if( mStrengthBiases != null )
                strength -= mStrengthBiases[i];
            
            // Cache the strongest input
            if( strength > maxStrength )
            {
                maxStrength = strength;
                strongestInputCode = inputCode;
            }
        }
        
        // Call the overloaded method with the strongest found
        onInput( strongestInputCode, maxStrength, hardwareId );
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
        // Filter the input before passing on to listeners
        
        // Determine the input conditions
        boolean inputChanged = inputCode != mCurrentCode;
        boolean strengthChanged = Math.abs( strength - mCurrentStrength ) > STRENGTH_HYSTERESIS;
        
        // Only notify listeners if the input has changed significantly
        if( strengthChanged || inputChanged )
        {
            mCurrentCode = inputCode;
            mCurrentStrength = strength;
            notifyListeners( mCurrentCode, mCurrentStrength, hardwareId );
        }
    }
    
    /**
     * Initializes the strength biases on each analog channel if necessary.
     * 
     * @param inputCodes Universal input codes for each channel.
     * @param rawStrengths Raw (biased) strength for each channel.
     */
    private void updateBiases( int[] inputCodes, float[] rawStrengths )
    {
        for( int i = 0; i < rawStrengths.length; i++ )
        {
            // Perfect 0 indicates that the bias for this axis is unset
            if( mStrengthBiases[i] != 0 )
                continue;
            
            // Get the strength of the axis
            float rawStrength = rawStrengths[i];
            
            // Due to a quirk in Android, analog axes whose center-point is not zero (e.g. an analog
            // trigger whose rest position is -1) still produce a perfect zero value at rest until
            // they have been wiggled a little bit. After that point, their rest position is
            // correctly recorded. Therefore we treat perfect zeros as suspicious and wait until we
            // are sure that we have a real strength value.
            if( rawStrength == 0 )
                continue;
            
            int axis = AbstractProvider.inputToAxisCode( inputCodes[i] );
            switch( axis )
            {
                default:
                    // Round and record the bias to -1, 0, or 1
                    mStrengthBiases[i] = Math.round( rawStrength );
                    break;
                case MotionEvent.AXIS_HAT_X:
                case MotionEvent.AXIS_HAT_Y:
                    // The resting value for these axes should always be zero. If we used the
                    // default method for these, their bias might be incorrectly stored as +/-
                    // 1. Subtracting this incorrect bias would then make one direction of the
                    // d-pad unusable. So we do nothing here, to ensure the bias remains zero
                    // for these axes.
                    break;
            }
            
            // Add a tiny number to indicate that the bias for this axis is now set
            mStrengthBiases[i] += Float.MIN_VALUE;
        }
    }
}
