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

import paulscode.android.mupen64plusae.Globals;
import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.util.FileUtil;
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
    
    private int mSelectionMode = SELECTION_MODE_ANY;
    private boolean mDoRefresh = true;
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
        mSelectionMode = a
                .getInteger( R.styleable.PathPreference_selectionMode, SELECTION_MODE_ANY );
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
        // Refresh the cached entry, value, and summary
        // This occurs first time through, and after user presses Cancel
        if( mDoRefresh )
        {
            mDoRefresh = false;
            String defaultFilename = Environment.getExternalStorageDirectory().getAbsolutePath();
            File file = new File( getPersistedString( defaultFilename ) );
            
            // Make sure the file still exists (file may have been moved to another directory)
            if( !file.exists() )
                file = new File( defaultFilename );
            
            populate( file );
            setSummary( mSelectionMode == SELECTION_MODE_FILE ? mEntry : mValue );
        }
        return super.onCreateView( parent );
    }
    
    @Override
    protected void onPrepareDialogBuilder( Builder builder )
    {
        super.onPrepareDialogBuilder( builder );
        
        // Refresh the list again whenever the dialog is reopened
        populate( new File( mValue ) );
        
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
            File path = new File( mValues.get( which ) );
            mEntry = mEntries.get( which ).toString();
            mValue = mValues.get( which );
            if( path.isDirectory() )
            {
                // ...navigate into...
                populate( path );
                mDoReclick = true;
            }
            else
            {
                // ...or select and close
                mEntry = mEntries.get( which ).toString();
                mValue = mValues.get( which );
                PathPreference.this.onClick( dialog, DialogInterface.BUTTON_POSITIVE );
                dialog.dismiss();
            }
        }
        else if( which == DialogInterface.BUTTON_NEGATIVE )
        {
            // User manually clicked Cancel, next time refresh from the persisted value
            mDoRefresh = true;
            notifyChanged();
        }
    }
    
    @Override
    protected void onDialogClosed( boolean positiveResult )
    {
        super.onDialogClosed( positiveResult );
        
        if( positiveResult )
        {
            // Save the preference data if user clicked Ok
            persistString( mValue );
            setSummary( mSelectionMode == SELECTION_MODE_FILE
                    ? mEntry
                    : mValue );
            notifyChanged();
        }
        else if( mDoReclick )
        {
            // Automatically reopen the dialog after it closes
            mDoReclick = false;
            onClick();
        }
    }
    
    private void populate( File startPath )
    {
        // Refresh the currently selected entry and value
        mEntry = startPath.getName();
        mValue = startPath.getPath();
        
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
