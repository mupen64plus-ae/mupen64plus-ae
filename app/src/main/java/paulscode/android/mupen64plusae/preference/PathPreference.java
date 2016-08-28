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

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Environment;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.preference.DialogPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;

import org.mupen64plusae.v3.fzurita.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import paulscode.android.mupen64plusae.compat.AppCompatPreferenceActivity.OnPreferenceDialogListener;
import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.util.FileUtil;

/**
 * A {@link DialogPreference} that is specifically for choosing a directory path or file on a device.
 */
public class PathPreference extends DialogPreference implements OnPreferenceDialogListener, DialogInterface.OnClickListener 
{
    /** The user must select a directory. No files will be shown in the list. */
    public static final int SELECTION_MODE_DIRECTORY = 0;
    
    /** The user must select a file. The dialog will only close when a file is selected. */
    public static final int SELECTION_MODE_FILE = 1;
    
    /** The user may select a file or a directory. The Ok button must be used. */
    public static final int SELECTION_MODE_ANY = 2;
    
    private static final String STORAGE_DIR = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final String DEFAULT_DIR = "mupen64plus-fz";

    private final boolean mUseDefaultSummary;
    private int mSelectionMode = SELECTION_MODE_ANY;
    private boolean mDoReclick = false;
    private final List<CharSequence> mNames = new ArrayList<CharSequence>();
    private final List<String> mPaths = new ArrayList<String>();
    private String mNewValue;
    private String mValue;

    /**
     * Constructor
     *
     * @param context The {@link Context} that this PathPreference is being used in.
     * @param attrs   A collection of attributes, as found associated with a tag in an XML document.
     */
    public PathPreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        
        mUseDefaultSummary = TextUtils.isEmpty( getSummary() );
        
        // Get the selection mode from the XML file, if provided
        TypedArray a = context.obtainStyledAttributes( attrs, R.styleable.PathPreference );
        mSelectionMode = a.getInteger( R.styleable.PathPreference_selectionMode, SELECTION_MODE_ANY );
        a.recycle();
        
        setOnPreferenceChangeListener(null);
    }

    /**
     * Sets the path that PathPrefence will use.
     * 
     * @param value The path that this PathPreference instance will use.
     */
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

    /**
     * Sets the specific selection mode to use.
     * 
     * @param value The selection mode to use.</p>
     *              <li>0 = Directories can only be used as a choice.
     *              <li>1 = Files can only be used as a choice.
     *              <li>2 = Directories and files can be used as a choice.</li>
     */
    public void setSelectionMode( int value )
    {
        mSelectionMode = value;
    }

    /**
     * Gets the path value being used.
     * 
     * @return The path value being used by this PathPreference.
     */
    public String getValue()
    {
        return mValue;
    }

    /**
     * Gets the current selection mode being used.
     * 
     * @return The current selection mode being used by this PathPreference.
     */
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
    public void onPrepareDialogBuilder( Context context, Builder builder )
    {        
        // Add the list entries

        // Holo theme has folder icons and "Parent folder" text
        ArrayAdapter<String> adapter = Prompt.createFilenameAdapter( getContext(), mPaths, mNames );
        builder.setAdapter( adapter, this );
        
        // Remove the Ok button when user must choose a file
        if( mSelectionMode == SELECTION_MODE_FILE )
            builder.setPositiveButton( null, null );
    }
    
    @Override
    public void onClick( DialogInterface dialog, int which )
    {
        // If the user clicked a list item...
        if( which >= 0 && which < mPaths.size() )
        {
            //Don't allow setting path outside default storage dir. We don't support external
            //SD cards or drives
            if(mPaths.get( which ).contains(STORAGE_DIR))
            {
                mNewValue = mPaths.get( which );
                File path = new File( mNewValue );

                if( path.isDirectory())
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
            else
            {
                mDoReclick = true;
            }
        }
    }
    
    @Override
    public void onDialogClosed( boolean positiveResult )
    {        
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
    }

    // Populates the dialog view with files and folders on the device.
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
        FileUtil.populate( startPath, true, true, isFilesIncluded, mNames, mPaths );
    }

    public static String validate( String value )
    {
        if( TextUtils.isEmpty( value ) )
        {
            // Use storage directory if value is empty
            value = STORAGE_DIR + "/" + DEFAULT_DIR;
        }
        else if(value.startsWith( "!" ))
        {
            // Build the absolute path if necessary
            value = STORAGE_DIR + "/" + value.substring( 1 );
        }

        // Ensure the parent directories exist if requested
        FileUtil.makeDirs(value);

        return value;
    }

    @Override
    public void onBindDialogView(View view, FragmentActivity associatedActivity)
    {
        //Nothing to do here
    }
}
