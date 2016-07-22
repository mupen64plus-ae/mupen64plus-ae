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
package paulscode.android.mupen64plusae.preference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mupen64plusae.v3.fzurita.R;

import paulscode.android.mupen64plusae.compat.AppCompatPreferenceActivity.OnPreferenceDialogListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.res.TypedArray;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

/**
 * Multi-select list preference. Honeycomb and above provide this functionality natively, but we
 * need backwards-compatibility and some custom features. This gets the job done without much fuss.
 */
public class MultiSelectListPreference extends ListPreference implements OnMultiChoiceClickListener, OnPreferenceDialogListener
{
    /** The default delimiter for internal serialization/deserialization. */
    private static final String DEFAULT_DELIMITER = "~";
    
    /** The default delimiter for external display of the selected values. */
    private static final String DEFAULT_SEPARATOR = ", ";
    
    /** The delimiter for internal serialization/deserialization. */
    private final String mDelimiter;
    
    /** The delimiter for external display of the selected values. */
    private final String mSeparator;
    
    /** The state of each checkbox in the list. */
    private boolean[] mCheckedStates;
    
    /**
     * Instantiates a new multi-select list preference.
     * 
     * @param context The current context.
     * @param attrs   The attributes provided from the xml resource.
     */
    public MultiSelectListPreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        
        TypedArray a = context
                .obtainStyledAttributes( attrs, R.styleable.MultiSelectListPreference );
        String delimiter = a.getString( R.styleable.MultiSelectListPreference_delimiter );
        String separator = a.getString( R.styleable.MultiSelectListPreference_separator );
        a.recycle();
        
        mDelimiter = TextUtils.isEmpty( delimiter ) ? DEFAULT_DELIMITER : delimiter;
        mSeparator = TextUtils.isEmpty( separator ) ? DEFAULT_SEPARATOR : separator;
        
        setOnPreferenceChangeListener(null);
    }
    
    /**
     * Gets the selected values.
     * 
     * @return The selected values.
     */
    public List<String> getSelectedValues()
    {
        List<String> selectedValues = new ArrayList<String>();
        CharSequence[] allValues = getEntryValues();
        for( int i = 0; i < allValues.length; i++ )
        {
            if( mCheckedStates[i] )
                selectedValues.add( allValues[i].toString() );
        }
        return selectedValues;
    }
    
    /**
     * Gets the selected entries.
     * 
     * @return The selected entries.
     */
    public List<CharSequence> getSelectedEntries()
    {
        List<CharSequence> selectedEntries = new ArrayList<CharSequence>();
        CharSequence[] allEntries = getEntries();
        for( int i = 0; i < allEntries.length; i++ )
        {
            if( mCheckedStates[i] )
                selectedEntries.add( allEntries[i] );
        }
        return selectedEntries;
    }
    
    /**
     * Gets the selected entries as a concatenated string.
     * 
     * @return The selected entries as a string.
     */
    public CharSequence getSelectedEntriesString()
    {
        return TextUtils.join( mSeparator, getSelectedEntries() );
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see android.preference.ListPreference#setEntries(java.lang.CharSequence[])
     */
    @Override
    public void setEntries( CharSequence[] entries )
    {
        super.setEntries( entries );
        synchronizeState( getValue() );
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see android.preference.ListPreference#setValue(java.lang.String)
     */
    @Override
    public void setValue( String value )
    {
        super.setValue( value );
        synchronizeState( getValue() );
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * android.preference.ListPreference#onPrepareDialogBuilder(android.app.AlertDialog.Builder)
     */
    @Override
    public void onPrepareDialogBuilder( Context context, Builder builder )
    {
        synchronizeState( getValue() );
        builder.setMultiChoiceItems( getEntries(), mCheckedStates, this );
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see android.content.DialogInterface.OnMultiChoiceClickListener#onClick(android.content.
     * DialogInterface, int, boolean)
     */
    @Override
    public void onClick( DialogInterface dialog, int which, boolean isChecked )
    {
        mCheckedStates[which] = isChecked;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see android.preference.ListPreference#onDialogClosed(boolean)
     */
    @Override
    public void onDialogClosed( boolean positiveResult )
    {
        // super.onDialogClosed(positiveResult);
        String newValue = serialize( getSelectedValues(), mDelimiter );
        if( positiveResult && callChangeListener( newValue ) )
        {
            // Persist the new value
            setValue( newValue );
        }
    }
    
    /**
     * Synchronize internal state and summary with the selected and available values.
     */
    private void synchronizeState( String value )
    {
        List<String> selectedValues = deserialize( value, mDelimiter );
        CharSequence[] allValues = getEntryValues();
        mCheckedStates = new boolean[allValues.length];
        for( int i = 0; i < allValues.length; i++ )
        {
            mCheckedStates[i] = selectedValues.contains( allValues[i].toString() );
        }
        setSummary( getSelectedEntriesString() );
    }
    
    /**
     * Deserialize the selected values array from a string.
     * 
     * @param value     The serialized value of the array.
     * @param delimiter The delimiter used between array elements in the serialization.
     * 
     * @return The array of selected values.
     */
    public static List<String> deserialize( String value, String delimiter )
    {
        return Arrays.asList( value.split( delimiter ) );
    }
    
    /**
     * Deserialize the selected values array from a string using the default delimiter.
     * 
     * @param value The serialized value of the array.
     * 
     * @return The array of selected values.
     */
    public static List<String> deserialize( String value )
    {
        return deserialize( value, DEFAULT_DELIMITER );
    }
    
    /**
     * Serialize the selected values array to a string.
     * 
     * @param selectedValues The array of selected values.
     * @param delimiter      The delimiter used between array elements in the serialization.
     *
     * @return The serialized value of the array.
     */
    public static String serialize( List<String> selectedValues, String delimiter )
    {
        return selectedValues == null ? "" : TextUtils.join( delimiter, selectedValues );
    }
    
    /**
     * Serialize the selected values array to a string using the default delimiter.
     * 
     * @param selectedValues The array of selected values.
     * 
     * @return The serialized value of the array.
     */
    public static String serialize( List<String> selectedValues )
    {
        return serialize( selectedValues, DEFAULT_DELIMITER );
    }

    @Override
    public void onBindDialogView(View view, FragmentActivity associatedActivity)
    {
        //Nothing to do here
    }
}
