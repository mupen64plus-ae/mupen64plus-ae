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
package paulscode.android.mupen64plusae.persistent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Utility;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Environment;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class PathPreference extends DialogPreference
{
    /** The user must select a directory. No files will be shown in the list. */
    public static final int SELECTION_MODE_DIRECTORY = 0;
    
    /** The user must select a file. The dialog will only close when a file is selected. */
    public static final int SELECTION_MODE_FILE = 1;
    
    /** The user may select a file or a directory. The Ok button must be used. */
    public static final int SELECTION_MODE_ANY = 2;
    
    private static final String STORAGE_DIR = Environment.getExternalStorageDirectory().getAbsolutePath();
    
    private int mSelectionMode = SELECTION_MODE_ANY;
    private boolean mDoReclick = false;
    private List<CharSequence> mEntries = new ArrayList<CharSequence>();
    private List<String> mValues = new ArrayList<String>();
    private CharSequence mEntry;
    private String mValue;
    
    public PathPreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        
        // Get the selection mode from the XML file, if provided
        TypedArray a = context.obtainStyledAttributes( attrs, R.styleable.PathPreference );
        mSelectionMode = a.getInteger( R.styleable.PathPreference_selectionMode, SELECTION_MODE_ANY );
        a.recycle();
    }
    
    public int getSelectionMode()
    {
        return mSelectionMode;
    }
    
    public void setSelectionMode( int value )
    {
        mSelectionMode = value;
    }
    
    @Override
    protected View onCreateView( ViewGroup parent )
    {
        // Restore the persisted value
        mValue = getPersistedString( "" );
        populate( new File( mValue ) );
        setSummary( mSelectionMode == SELECTION_MODE_FILE ? mEntry : mValue );

        return super.onCreateView( parent );
    }
    
    @Override
    protected Object onGetDefaultValue( TypedArray a, int index )
    {
        String value = a.getString( index );
        
        if( Utility.isNullOrEmpty( value ) )
        {
            // Use storage directory if no default provided in XML file
            value = STORAGE_DIR;
        }
        else
        {
            // Default value was provided in XML file
            // Prefixes encode additional information:
            // ! means path is relative to storage dir, and parent dirs should be created if path does not exist
            // ~ means path is relative to storage dir, and that storage dir should be used if path does not exist
            boolean isRelativePath = value.startsWith( "!" ) || value.startsWith( "~" );
            boolean forceParentDirs = value.startsWith( "!" );
            
            // Build the absolute path if necessary
            if( isRelativePath )
                value = STORAGE_DIR + "/" + value.substring( 1 );
            
            // Ensure the parent directories exist if requested
            if( forceParentDirs )
                ( new File( value ) ).mkdirs();
        }
        
        return value;
    }
    
    @Override
    protected void onSetInitialValue( boolean restorePersistedValue, Object defaultValue )
    {
        if( restorePersistedValue )
        {
            // Restore persisted value
            mValue = this.getPersistedString( STORAGE_DIR );
            
            // Fall back to storage directory if path no longer exists
            if( !( new File( mValue ) ).exists() )
            {
                mValue = STORAGE_DIR;
                persistString( mValue );
            }
        }
        else
        {
            // Set default state from the XML attribute
            mValue = (String) defaultValue;
            persistString( mValue );
        }
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
        super.onClick( dialog, which );
        
        // If the user clicked a list item...
        if( mValues != null && which >= 0 && which < mValues.size() )
        {
            mValue = mValues.get( which );
            File path = new File( mValue );
            if( path.isDirectory() )
            {
                // ...navigate into...
                populate( path );
                mDoReclick = true;
            }
            else
            {
                // ...or close dialog positively
                onClick( dialog, DialogInterface.BUTTON_POSITIVE );
                dialog.dismiss();
            }
        }
    }
    
    @Override
    protected void onDialogClosed( boolean positiveResult )
    {
        super.onDialogClosed( positiveResult );
        
        // Clicking Cancel or Ok returns us to the parent preference menu. For these cases we return
        // to a clean state by persisting or restoring the value.
        if( positiveResult )
        {
            // User clicked Ok: clean the state by persisting value
            persistString( mValue );
            notifyChanged();
            callChangeListener( mValue );
        }
        else if( mDoReclick )
        {
            // User clicked a list item: maintain dirty value and re-open
            mDoReclick = false;
            onClick();
        }
        else
        {
            // User clicked Cancel/Back: clean state by restoring value
            mValue = getPersistedString( "" );
            populate( new File( mValue ) );
        }
    }
    
    private void populate( File startPath )
    {
        // Refresh the currently selected entry
        mEntry = startPath.getName();
        
        // If start path is a file, list it and its siblings in the parent directory
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
}
