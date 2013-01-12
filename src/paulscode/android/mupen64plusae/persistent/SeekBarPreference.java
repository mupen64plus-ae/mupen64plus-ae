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
 * Authors: Gillou68310, littleguy77
 */
package paulscode.android.mupen64plusae.persistent;

import paulscode.android.mupen64plusae.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SeekBarPreference extends DialogPreference implements OnSeekBarChangeListener
{
    private static final int DEFAULT_VALUE = 50;
    private static final int DEFAULT_MAX = 100;
    private static final int DEFAULT_STEP = 10;
    private static final String DEFAULT_UNITS = "%";
    
    private int mValue = DEFAULT_VALUE;
    private int mMaxValue = DEFAULT_MAX;
    private int mStepSize = DEFAULT_STEP;
    private String mUnits = DEFAULT_UNITS;
    
    private TextView mTextView;
    private SeekBar mSeekBar;
    
    public SeekBarPreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        
        // Get the attributes from the XML file, if provided
        TypedArray a = context.obtainStyledAttributes( attrs, R.styleable.SeekBarPreference );
        setMaxValue( a.getInteger( R.styleable.SeekBarPreference_maximumValue, DEFAULT_MAX ) );
        setStepSize( a.getInteger( R.styleable.SeekBarPreference_stepSize, DEFAULT_STEP ) );
        setUnits( a.getString( R.styleable.SeekBarPreference_units ) );
        a.recycle();
        
        // Setup the layout
        setDialogLayoutResource( R.layout.seek_bar_preference );
    }
    
    public SeekBarPreference( Context context )
    {
        this( context, null );
    }
    
    public void setValue( int value )
    {
        mValue = validate( value );
        if( shouldPersist() )
            persistInt( mValue );
    }
    
    public void setMaxValue( int maxValue )
    {
        mMaxValue = maxValue;
    }
    
    public void setStepSize( int stepSize )
    {
        mStepSize = stepSize;
    }
    
    public void setUnits( String units )
    {
        mUnits = units;
    }
    
    public int getValue()
    {
        return mValue;
    }
    
    public int getMaxValue()
    {
        return mMaxValue;
    }
    
    public int getStepSize()
    {
        return mStepSize;
    }
    
    public String getUnits()
    {
        return mUnits;
    }
    
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
        mSeekBar.setMax( mMaxValue );
        mSeekBar.setOnSeekBarChangeListener( this );
        mSeekBar.setProgress( mValue );
        mTextView.setText( getValueString( mValue ) );
    }
    
    @Override
    protected void onDialogClosed( boolean positiveResult )
    {
        super.onDialogClosed( positiveResult );
        
        if( positiveResult )
        {
            int value = mSeekBar.getProgress();
            if( callChangeListener( value ) )
                setValue( value );
        }
    }
    
    @Override
    protected Parcelable onSaveInstanceState()
    {
        final SavedIntegerState myState = new SavedIntegerState( super.onSaveInstanceState() );
        if( mSeekBar != null )
            myState.mValue = mSeekBar.getProgress();
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
            mSeekBar.setProgress( myState.mValue );
    }
    
    @Override
    public void onProgressChanged( SeekBar seekBar, int progress, boolean fromUser )
    {
        int value = validate( progress );
        if( value != progress )
            seekBar.setProgress( value );
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
        // e.g. mMaxValue = 100, mStepSize = 9, progress = 100 --> newValue = 99 (should be 100)
        // e.g. mMaxValue = 100, mStepSize = 6, progress = 99 --> newValue = 102 (should be 100)
        if( value == mMaxValue || newValue > mMaxValue )
            newValue = mMaxValue;
        
        return newValue;
    }
}
