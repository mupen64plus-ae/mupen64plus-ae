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

/**
 * A provider class that aggregates inputs from other providers, and lazily notifies listeners only
 * when the aggregate input has changed significantly.
 */
public class LazyProvider extends AbstractProvider implements AbstractProvider.Listener
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
        
        providers.removeAll( providers );
    }
    
    /**
     * Resets the strength biases, i.e. re-centers analog inputs.
     */
    public void resetBiases()
    {
        mStrengthBiases = null;
    }
    
    /**
     * Gets the standardized input code for the last active input.
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
     * float[])
     */
    @Override
    public void onInput( int[] inputCodes, float[] strengths )
    {
        // Get strength biases first time through
        boolean refreshBiases = false;
        if( mStrengthBiases == null )
        {
            mStrengthBiases = new float[strengths.length];
            refreshBiases = true;
        }
        
        // Find the strongest input
        float maxStrength = AbstractProvider.STRENGTH_THRESHOLD;
        int strongestInputCode = 0;
        for( int i = 0; i < inputCodes.length; i++ )
        {
            int inputCode = inputCodes[i];
            float strength = strengths[i];
            
            // Record the strength bias and remove its effect
            if( refreshBiases )
                mStrengthBiases[i] = strength;
            strength -= mStrengthBiases[i];
            
            // Cache the strongest input
            if( strength > maxStrength )
            {
                maxStrength = strength;
                strongestInputCode = inputCode;
            }
        }
        
        // Call the overloaded method with the strongest found
        onInput( strongestInputCode, maxStrength );
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see paulscode.android.mupen64plusae.input.provider.AbstractProvider.Listener#onInput(int,
     * float)
     */
    @Override
    public void onInput( int inputCode, float strength )
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
            notifyListeners( mCurrentCode, mCurrentStrength );
        }
    }
}
