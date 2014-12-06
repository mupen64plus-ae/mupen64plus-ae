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
 * Authors: Gillou68310, littleguy77
 */
package paulscode.android.mupen64plusae.persistent;

import org.mupen64plusae.v3.alpha.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * A type of {@link DialogPreference} that uses a {@link SeekBar} as a means of selecting a desired option.
 */
public class SeekBarPreference extends DialogPreference implements OnSeekBarChangeListener
{
    private static final int DEFAULT_VALUE = 50;
    private static final int DEFAULT_MIN = 0;
    private static final int DEFAULT_MAX = 100;
    private static final int DEFAULT_STEP = 10;
    private static final String DEFAULT_UNITS = "%";
    
    private int mValue = DEFAULT_VALUE;
    private int mMinValue = DEFAULT_MIN;
    private int mMaxValue = DEFAULT_MAX;
    private int mStepSize = DEFAULT_STEP;
    private String mUnits = DEFAULT_UNITS;
    
    private TextView mTextView;
    private SeekBar mSeekBar;

    /**
     * Constructor
     *
     * @param context The {@link Context} this SeekBarPreference is being used in.
     * @param attrs   A collection of attributes, as found associated with a tag in an XML document.
     */
    public SeekBarPreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        
        // Get the attributes from the XML file, if provided
        TypedArray a = context.obtainStyledAttributes( attrs, R.styleable.SeekBarPreference );
        setMinValue( a.getInteger( R.styleable.SeekBarPreference_minimumValue, DEFAULT_MIN ) );
        setMaxValue( a.getInteger( R.styleable.SeekBarPreference_maximumValue, DEFAULT_MAX ) );
        setStepSize( a.getInteger( R.styleable.SeekBarPreference_stepSize, DEFAULT_STEP ) );
        setUnits( a.getString( R.styleable.SeekBarPreference_units ) );
        a.recycle();
        
        // Setup the layout
        setDialogLayoutResource( R.layout.seek_bar_preference );
    }

    /**
     * Constructor
     *
     * @param context The {@link Context} this SeekBarPreference will be used in.
     */
    public SeekBarPreference( Context context )
    {
        this( context, null );
    }

    /**
     * Sets this SeekBarPreference to a specified value.
     *
     * @param value The value to set the SeekBarPreference to.
     */
    public void setValue( int value )
    {
        mValue = validate( value );
        if( shouldPersist() )
            persistInt( mValue );
        notifyChanged();
    }

    /**
     * Sets the minimum value this SeekBarPreference may have.
     *
     * @param minValue The minimum value for this SeekBarPreference.
     */
    public void setMinValue( int minValue )
    {
        mMinValue = minValue;
    }

    /**
     * Sets the maximum value this SeekBarPreference may have.
     *
     * @param maxValue The maximum value for this SeekBarPreference.
     */
    public void setMaxValue( int maxValue )
    {
        mMaxValue = maxValue;
    }

    /**
     * Sets the size of each increment in this SeekBarPreference.
     *
     * @param stepSize The size of each increment.
     */
    public void setStepSize( int stepSize )
    {
        mStepSize = stepSize;
    }

    /**
     * Sets the type of units this SeekBarPreference uses (e.g. "%").
     * 
     * @param units The unit type for this SeekBarPreference to use.
     */
    public void setUnits( String units )
    {
        mUnits = units;
    }

    /**
     * Gets the currently set value.
     * 
     * @return The currently set value in this SeekBarPreference.
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Gets the currently set minimum value.
     * 
     * @return The currently set minimum value for this SeekBarPreference.
     */
    public int getMinValue()
    {
        return mMinValue;
    }

    /**
     * Gets the currently set maximum value.
     * 
     * @return The currently set maximum value for this SeekBarPreference.
     */
    public int getMaxValue()
    {
        return mMaxValue;
    }

    /**
     * Gets the currently set increment step size.
     * 
     * @return The currently set increment step size for this SeekBarPreference.
     */
    public int getStepSize()
    {
        return mStepSize;
    }

    /**
     * Gets the currently set units.
     * 
     * @return The currently set unit type this SeekBarPreference uses.
     */
    public String getUnits()
    {
        return mUnits;
    }

    /**
     * Gets the value as a string with units appended.
     * 
     * @param value The value to use in the string.
     * 
     * @return The value as a String.
     */
    public String getValueString( int value )
    {
        return getContext().getString( R.string.seekBarPreference_summary, value, mUnits );
    }
    
    @Override
    protected Object onGetDefaultValue( TypedArray a, int index )
    {
        return a.getInteger( index, DEFAULT_VALUE );
    }
    
    @Override
    protected void onSetInitialValue( boolean restorePersistedValue, Object defaultValue )
    {
        setValue( restorePersistedValue ? getPersistedInt( mValue ) : (Integer) defaultValue );
    }
    
    @Override
    protected void onBindView( View view )
    {
        setSummary( getValueString( mValue ) );
        super.onBindView( view );
    }
    
    @Override
    protected void onBindDialogView( View view )
    {
        // Setup the dialog that is shown when the menu item is clicked
        super.onBindDialogView( view );
        
        // Grab the widget references
        mTextView = (TextView) view.findViewById( R.id.textFeedback );
        mSeekBar = (SeekBar) view.findViewById( R.id.seekbar );
        
        // Initialize and refresh the widgets
        mSeekBar.setMax( mMaxValue - mMinValue);
        mSeekBar.setOnSeekBarChangeListener( this );
        mSeekBar.setProgress( mValue - mMinValue );
        mTextView.setText( getValueString( mValue ) );
    }
    
    @Override
    protected void onDialogClosed( boolean positiveResult )
    {
        super.onDialogClosed( positiveResult );
        
        if( positiveResult )
        {
            int value = mSeekBar.getProgress() + mMinValue;
            if( callChangeListener( value ) )
                setValue( value );
        }
    }
    
    @Override
    protected Parcelable onSaveInstanceState()
    {
        final SavedIntegerState myState = new SavedIntegerState( super.onSaveInstanceState() );
        if( mSeekBar != null )
            myState.mValue = mSeekBar.getProgress() + mMinValue;
        return myState;
    }
    
    @Override
    protected void onRestoreInstanceState( Parcelable state )
    {
        if( state == null || !state.getClass().equals( SavedIntegerState.class ) )
        {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState( state );
            return;
        }
        
        final SavedIntegerState myState = (SavedIntegerState) state;
        super.onRestoreInstanceState( myState.getSuperState() );
        if( mSeekBar != null )
            mSeekBar.setProgress( myState.mValue - mMinValue );
    }
    
    @Override
    public void onProgressChanged( SeekBar seekBar, int progress, boolean fromUser )
    {
        int value = validate( progress + mMinValue );
        if( value != ( progress + mMinValue ) )
            seekBar.setProgress( value - mMinValue );
        mTextView.setText( getValueString( value ) );
    }
    
    @Override
    public void onStartTrackingTouch( SeekBar seekBar )
    {
    }
    
    @Override
    public void onStopTrackingTouch( SeekBar seekBar )
    {
    }

    private int validate( int value )
    {
        // Round to nearest integer multiple of mStepSize
        int newValue = Math.round( value / (float) mStepSize ) * mStepSize;
        
        // Address issues when mStepSize is not an integral factor of mMaxValue
        // e.g. mMaxValue = 100, mMinValue = 0, mStepSize = 9, progress = 100 --> newValue = 99 (should be 100)
        // e.g. mMaxValue = 100, mMinValue = 0, mStepSize = 6, progress = 99 --> newValue = 102 (should be 100)
        if( value == mMinValue || newValue < mMinValue )
            newValue = mMinValue;
        if( value == mMaxValue || newValue > mMaxValue )
            newValue = mMaxValue;
        
        return newValue;
    }
}
