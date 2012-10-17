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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.preference.ListPreference;
import android.text.Html;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.View;

// TODO: Consolidate this with FolderPreference implementation
public class FilePreference extends ListPreference
{
    String mPath;
    
    public FilePreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
    }
    
    @Override
    protected View onCreateDialogView()
    {
        refreshItems();
        return super.onCreateDialogView();
    }
    
    @Override
    protected void onDialogClosed( boolean positiveResult )
    {
        super.onDialogClosed( positiveResult );
        File file = new File(mPath);
        if (positiveResult && file.isDirectory())
            onClick();
    }
    
    public void refreshItems()
    {
        // Restore the persisted state
        mPath = getPersistedString( Settings.path.storageDir );
        populate( new File( mPath ) );        
    }
    
    private void populate( File startPath )
    {
        if( startPath.isFile() )
            startPath = startPath.getParentFile();
        
        // Get all files in this folder
        // TODO: Add file filter?
        File[] fileList = startPath.listFiles();
        List<File> files = new ArrayList<File>();
        for( File file : fileList )
            if( !file.isHidden() )
                files.add( file );
        Collections.sort( files, new FileComparer() );
        
        // Construct the key-value pairs for the list entries
        boolean hasParent = startPath.getParentFile() != null;
        ArrayList<Spanned> entries = new ArrayList<Spanned>();
        ArrayList<String> values = new ArrayList<String>();
        if( hasParent )
        {
            entries.add( Html.fromHtml( ".." ) );
            values.add( startPath.getParentFile().getAbsolutePath() );
        }
        for( File file : files )
        {
            if( file.isDirectory() )
                entries.add( Html.fromHtml( "<i>" + file.getName() + "</i>" ) );
            else
                entries.add( Html.fromHtml( "<b>" + file.getName() + "</b>" ) );
            values.add( file.getAbsolutePath() );
        }
        
        // Populate the list
        setEntries( entries.toArray( new Spanned[entries.size()] ) );
        setEntryValues( values.toArray( new String[values.size()] ) );
        
        // Update the menu text
        setDialogTitle( startPath.getName() );
        //setSummary( getEntry() + " (" + getValue() + ")" );
        notifyChanged();
    }
    
    public class FileComparer implements Comparator<File>
    {
        public int compare( File lhs, File rhs )
        {
            if( lhs.isDirectory() && rhs.isFile() )
                return -1;
            else if( lhs.isFile() && rhs.isDirectory() )
                return 1;
            else
                return lhs.getName().toLowerCase().compareTo( rhs.getName().toLowerCase() );
        }
    }
}
