package paulscode.android.mupen64plusae.input.provider;

import java.util.ArrayList;

public class LazyProvider extends AbstractProvider implements AbstractProvider.Listener
{
    public static final float STRENGTH_THRESHOLD = 0.5f;
    public static final float STRENGTH_HYSTERESIS = 0.05f;
    
    private int mActiveCode = 0;
    private int mCurrentCode = 0;
    private float mCurrentStrength = 0;
    private float[] mStrengthBiases = null;
    private final ArrayList<AbstractProvider> providers = new ArrayList<AbstractProvider>();
    
    public void addProvider( AbstractProvider provider )
    {
        if( provider != null )
        {
            provider.registerListener( this );
            providers.add( provider );
        }
    }
    
    public void removeProvider( AbstractProvider provider )
    {
        if( provider != null )
        {
            provider.unregisterListener( this );
            providers.remove( provider );
        }
    }
    
    public void removeAllProviders()
    {
        for( AbstractProvider provider : providers )
            provider.unregisterListener( this );

        providers.removeAll( providers );
    }
    
    public void resetBiases()
    {
        mStrengthBiases = null;
    }
    
    public int getActiveCode()
    {
        return mActiveCode;
    }
    
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
        float maxStrength = STRENGTH_THRESHOLD;
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
    
    @Override
    public void onInput( int inputCode, float strength )
    {
        // Filter the input before passing on to listeners
        
        // Determine the input conditions
        boolean isActive = strength > STRENGTH_THRESHOLD;
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
