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
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.util;

import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * A class that coordinates a {@link SeekBar}, {@link TextView}, and two {@link Button}s to provide
 * an enhanced UI for selecting an integer. The seekbar snaps to a major grid when the user drags
 * the seekbar, but snaps to a minor grid when the user presses the up/down buttons. The textview
 * shows the string-formatted value of the seekbar.
 */
public class SeekBarGroup implements OnSeekBarChangeListener, View.OnClickListener
{
    public interface Listener
    {
        public void onValueChanged( int value );
    }
    
    private final int initialValue;
    private final SeekBar seekbar;
    private final Button buttonDn;
    private final Button buttonUp;
    private final TextView textView;
    private final String template;
    private final int minorStep;
    private final int majorStep;
    private final int minValue;
    private final int maxValue;
    private final Listener listener;
    
    public SeekBarGroup( int value, View parent, int seekId, int downId, int upId, int textviewId,
            String textTemplate, Listener l )
    {
        this( value, parent, seekId, downId, upId, textviewId, textTemplate, 5, 1, 0, 100, l );
    }
    
    public SeekBarGroup( int value, View parent, int seekId, int downId, int upId, int textviewId,
            String textTemplate, int major, int minor, int min, int max,
            Listener l )
    {
        // Assign the final fields
        initialValue = value;
        seekbar = (SeekBar) parent.findViewById( seekId );
        buttonDn = (Button) parent.findViewById( downId );
        buttonUp = (Button) parent.findViewById( upId );
        textView = (TextView) parent.findViewById( textviewId );
        template = textTemplate;
        minorStep = minor;
        majorStep = major;
        minValue = min;
        maxValue = max;
        listener = l;
        
        // Initialize the seekbar scale and value before listeners are attached
        seekbar.setMax( maxValue - minValue );
        setValue( value );
        
        // Set the listeners last to avoid spurious notifications
        seekbar.setOnSeekBarChangeListener( this );
        buttonDn.setOnClickListener( this );
        buttonUp.setOnClickListener( this );
    }
    
    public int getValue()
    {
        return seekbar.getProgress() + minValue;
    }
    
    public void revertValue()
    {
        setValue( initialValue );
    }
    
    private void setValue( int value )
    {
        value = validate( value, minorStep );
        seekbar.setProgress( value - minValue );
        textView.setText( String.format( template, value ) );
        if( listener != null )
            listener.onValueChanged( value );
    }
    
    @Override
    public void onClick( View view )
    {
        if( view == buttonDn )
            setValue( getValue() - minorStep );
        else if( view == buttonUp )
            setValue( getValue() + minorStep );
    }
    
    @Override
    public void onProgressChanged( SeekBar seekBar, int progress, boolean fromUser )
    {
        if( fromUser )
        {
            int value = validate( progress + minValue, majorStep );
            seekbar.setProgress( value - minValue );
            textView.setText( String.format( template, value ) );
            if( listener != null )
                listener.onValueChanged( value );
        }
    }
    
    @Override
    public void onStartTrackingTouch( SeekBar seekBar )
    {
    }
    
    @Override
    public void onStopTrackingTouch( SeekBar seekBar )
    {
    }
    
    private int validate( int value, int step )
    {
        // Round to nearest integer multiple of step size
        int newValue = Math.round( value / (float) step ) * step;
        
        // Address issues when step size is not an integral factor of max value
        // e.g. max = 100, min = 0, step = 9, value = 100 --> newValue = 99 (should be 100)
        // e.g. max = 100, min = 0, step = 6, value = 99 --> newValue = 102 (should be 100)
        if( value == minValue || newValue < minValue )
            newValue = minValue;
        if( value == maxValue || newValue > maxValue )
            newValue = maxValue;
        
        return newValue;
    }
}