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
 * Authors: Gillou68310
 */
package paulscode.android.mupen64plusae.persistent;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.preference.DialogPreference;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.LinearLayout;


public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener
{
    private static final String androidns="http://schemas.android.com/apk/res/android";

    private SeekBar mSeekBar;
    private TextView mValueText;
    private Context mContext;

    private int mDefault;
    private int mMax;
    private int mProgress;
    private int mValue = 0;

    public SeekBarPreference(Context context, AttributeSet attrs)
    { 
        super(context,attrs); 
        mContext = context;

        mDefault = attrs.getAttributeIntValue(androidns,"defaultValue", 100);
        mMax = attrs.getAttributeIntValue(androidns,"max", 100);
    }

    @Override 
    protected View onCreateDialogView()
    {
        LinearLayout.LayoutParams params;
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(6,6,6,6);

        mValueText = new TextView(mContext);
        mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
        params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.addView(mValueText, params);

        mSeekBar = new SeekBar(mContext);
        mSeekBar.setOnSeekBarChangeListener(this);
        layout.addView(mSeekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        if (shouldPersist())
            mValue = getPersistedInt(mDefault);

        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mValue);
        return layout;
    }
  
    @Override 
    protected void onBindDialogView(View v)
    {
        super.onBindDialogView(v);
        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mValue);
    }
  
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue)  
    {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
        if (restorePersistedValue) 
            mValue = shouldPersist() ? getPersistedInt(mDefault) : 0;
        else 
            mValue = (Integer)defaultValue;
    }

    @Override
    public void onProgressChanged(SeekBar seek, int progress, boolean fromTouch)
    {
        mProgress = progress;
        mValueText.setText(String.valueOf(progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seek) {}

    @Override
    public void onStopTrackingTouch(SeekBar seek) {}
  
    @Override
    protected void onDialogClosed( boolean positiveResult )
    {
        super.onDialogClosed( positiveResult );
      
        // Clicking Cancel or Ok returns us to the parent preference menu. For these cases we return
        // to a clean state by persisting or restoring the value.
        if( positiveResult )
        {
            // User clicked Ok: clean the state by persisting value
            persistInt( mProgress );
            notifyChanged();
            callChangeListener( mProgress );
        }
    }
}

