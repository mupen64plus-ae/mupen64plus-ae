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
    
    /** The code of the last active input. */
    private int mActiveCode = 0;
    
    /** The code of the most recent input (not necessarily active). */
    private int mCurrentCode = 0;
    
    /** The strength of the most recent input, ranging from 0 to 1, inclusive. */
    private float mCurrentStrength = 0;
    
    /** The strength biases associated with each input channel, used to re-center raw analog values. */
    private float[] mStrengthBiases = null;
    
    /** The raw strengths associated with each input channel, from the most recent input event. */
    private float[] mLastRawStrengths = null;
    
    /** The codes associated with each input channel, from the most recent input event. */
    private int[] mLastInputCodes = null;
    
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
     * Resets the strength biases based on the next input event.
     */
    public void resetBiasesNext()
    {
        // Setting biases to null will force a refresh on next input
        mStrengthBiases = null;
    }
    
    /**
     * Resets the strength biases based on the last input event.
     */
    public void resetBiasesLast()
    {
        setBiases( mLastInputCodes, mLastRawStrengths );
    }
    
    /**
     * Sets the strength biases on each analog channel.
     * @param inputCodes Universal input codes for each channel.
     * @param rawStrengths Raw (biased) strength for each channel.
     */
    private void setBiases( int[] inputCodes, float[] rawStrengths )
    {
        if( inputCodes == null || rawStrengths == null )
            return;
        
        mStrengthBiases = new float[rawStrengths.length];
        
        for( int i = 0; i < rawStrengths.length; i++ )
        {
            int inputCode = inputCodes[i];
            float rawStrength = rawStrengths[i];
            
            // Record the strength bias
            int axis = AbstractProvider.inputToAxisCode( inputCode );
            switch( axis )
            {
                default:
                    // Round the bias to -1, 0, or 1
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
        }
    }
    
    /**
     * Gets the universal input code for the last active input.
     * 
     * @return The code for the last active input.
     */
    public int getActiveCode()
    {
        return mActiveCode;
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
        // Get strength biases if necessary
        if( mStrengthBiases == null )
            setBiases( inputCodes, strengths );
        
        // Cache the input codes and raw strengths
        mLastInputCodes = inputCodes.clone();
        mLastRawStrengths = strengths.clone();
        
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
        boolean isActive = strength > AbstractProvider.STRENGTH_THRESHOLD;
        boolean inputChanged = inputCode != mCurrentCode;
        boolean strengthChanged = Math.abs( strength - mCurrentStrength ) > STRENGTH_HYSTERESIS;
        
        // Cache the last active code
        if( isActive )
            mActiveCode = inputCode;
        
        // Only notify listeners if the input has changed significantly
        if( strengthChanged || inputChanged )
        {
            mCurrentCode = inputCode;
            mCurrentStrength = strength;
            notifyListeners( mCurrentCode, mCurrentStrength, hardwareId );
        }
    }
}
