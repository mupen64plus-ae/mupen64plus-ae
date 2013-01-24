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
package paulscode.android.mupen64plusae.persistent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.util.FileUtil;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Environment;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.text.TextUtils;
import android.util.AttributeSet;

public class PathPreference extends DialogPreference
{
    /** The user must select a directory. No files will be shown in the list. */
    public static final int SELECTION_MODE_DIRECTORY = 0;
    
    /** The user must select a file. The dialog will only close when a file is selected. */
    public static final int SELECTION_MODE_FILE = 1;
    
    /** The user may select a file or a directory. The Ok button must be used. */
    public static final int SELECTION_MODE_ANY = 2;
    
    private static final String STORAGE_DIR = Environment.getExternalStorageDirectory().getAbsolutePath();
    
    private final boolean mUseDefaultSummary;
    private int mSelectionMode = SELECTION_MODE_ANY;
    private boolean mDoReclick = false;
    private List<CharSequence> mEntries = new ArrayList<CharSequence>();
    private List<String> mValues = new ArrayList<String>();
    private String mNewValue;
    private String mValue;
    
    public PathPreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        
        mUseDefaultSummary = TextUtils.isEmpty( getSummary() );
        
        // Get the selection mode from the XML file, if provided
        TypedArray a = context.obtainStyledAttributes( attrs, R.styleable.PathPreference );
        mSelectionMode = a.getInteger( R.styleable.PathPreference_selectionMode, SELECTION_MODE_ANY );
        a.recycle();
    }
    
    public void setValue( String value )
    {
        mValue = validate( value );
        if( shouldPersist() )
            persistString( mValue );
        
        // Summary always reflects the true/persisted value, does not track the temporary/new value
        if( mUseDefaultSummary )
            setSummary( mSelectionMode == SELECTION_MODE_FILE ? new File( mValue ).getName() : mValue );
        
        // Reset the dialog info
        populate( mValue );
    }
    
    public void setSelectionMode( int value )
    {
        mSelectionMode = value;
    }
    
    public String getValue()
    {
        return mValue;
    }
    
    public int getSelectionMode()
    {
        return mSelectionMode;
    }
    
    @Override
    protected Object onGetDefaultValue( TypedArray a, int index )
    {
        return a.getString( index );
    }
    
    @Override
    protected void onSetInitialValue( boolean restorePersistedValue, Object defaultValue )
    {
        setValue( restorePersistedValue ? getPersistedString( mValue ) : (String) defaultValue );
    }
    
    @Override
    protected void onPrepareDialogBuilder( Builder builder )
    {
        super.onPrepareDialogBuilder( builder );
        
        // Add the list entries
        builder.setItems( mEntries.toArray( new CharSequence[mEntries.size()] ), this );
        
        // Remove the Ok button when user must choose a file
        if( mSelectionMode == SELECTION_MODE_FILE )
            builder.setPositiveButton( null, null );
    }
    
    @Override
    public void onClick( DialogInterface dialog, int which )
    {
        // If the user clicked a list item...
        if( mValues != null && which >= 0 && which < mValues.size() )
        {
            mNewValue = mValues.get( which );
            File path = new File( mNewValue );
            if( path.isDirectory() )
            {
                // ...navigate into...
                populate( mNewValue );
                mDoReclick = true;
            }
            else
            {
                // ...or close dialog positively
                which = DialogInterface.BUTTON_POSITIVE;
            }
        }
        
        // Call super last, parameters may have changed above
        super.onClick( dialog, which );        
    }
    
    @Override
    protected void onDialogClosed( boolean positiveResult )
    {
        super.onDialogClosed( positiveResult );
        
        if( positiveResult && callChangeListener( mNewValue ) )
        {
            // User clicked Ok: clean the state by persisting value
            setValue( mNewValue );
        }
        else if( mDoReclick )
        {
            // User clicked a list item: maintain dirty value and re-open
            mDoReclick = false;
            onClick();
        }
        else
        {
            // User clicked Cancel/Back: clean state by restoring persisted value
            populate( mValue );
        }
    }
    
    @Override
    protected Parcelable onSaveInstanceState()
    {
        final SavedStringState myState = new SavedStringState( super.onSaveInstanceState() );
        myState.mValue = mNewValue;
        return myState;
    }
    
    @Override
    protected void onRestoreInstanceState( Parcelable state )
    {
        if( state == null || !state.getClass().equals( SavedStringState.class ) )
        {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState( state );
            return;
        }
        
        final SavedStringState myState = (SavedStringState) state;
        super.onRestoreInstanceState( myState.getSuperState() );
        populate( myState.mValue );
        
        // If the dialog is already showing, we must close and reopen to refresh the contents
        // TODO: Find a less hackish solution, if one exists
        if( getDialog() != null )
        {
            mDoReclick = true;
            getDialog().dismiss();
        }
    }
    
    private void populate( String path )
    {
        // Cache the path to persist on Ok
        mNewValue = path;
        
        // Quick exit if null
        if( path == null )
            return;
        
        // If start path is a file, list it and its siblings in the parent directory
        File startPath = new File( path );
        if( startPath.isFile() )
            startPath = startPath.getParentFile();
        
        // Set the dialog title based on the selection mode
        switch( mSelectionMode )
        {
            case SELECTION_MODE_FILE:
                // If selecting only files, set title to parent directory name
                setDialogTitle( startPath.getPath() );
                break;
            case SELECTION_MODE_DIRECTORY:
            case SELECTION_MODE_ANY:
                // Otherwise clarify the directory that will be selected if user clicks Ok
                setDialogTitle( getContext().getString( R.string.pathPreference_dialogTitle,
                        startPath.getPath() ) );
                break;
        }
        
        // Populate the key-value pairs for the list entries
        boolean isFilesIncluded = mSelectionMode != SELECTION_MODE_DIRECTORY;
        FileUtil.populate( startPath, true, true, isFilesIncluded, mEntries, mValues );
    }

    private static String validate( String value )
    {
        if( TextUtils.isEmpty( value ) )
        {
            // Use storage directory if value is empty
            value = STORAGE_DIR;
        }
        else
        {
            // Non-empty string provided
            // Prefixes encode additional information:
            // ! and ~ mean path is relative to storage dir
            // ! means parent dirs should be created if path does not exist
            // ~ means storage dir should be used if path does not exist
            boolean isRelativePath = value.startsWith( "!" ) || value.startsWith( "~" );
            boolean forceParentDirs = value.startsWith( "!" );
            
            // Build the absolute path if necessary
            if( isRelativePath )
                value = STORAGE_DIR + "/" + value.substring( 1 );
            
            // Ensure the parent directories exist if requested
            File file = new File( value );
            if( forceParentDirs )
                file.mkdirs();
            else if( !file.exists() )
                value = STORAGE_DIR;
        }        
        return value;
    }
}
