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
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class FolderPreference extends ListPreference
{
    String mPath;
    
    public FolderPreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
    }
    
    @Override
    protected void onPrepareDialogBuilder( Builder builder )
    {
        refreshItems();
        super.onPrepareDialogBuilder( builder );
    }
    
    @Override
    public void onClick( DialogInterface dialog, int which )
    {
        if ( which < 0 )
        {
            // A list item was clicked
            // Refresh the list instead of closing the dialog
            refreshItems();
        }
        else
        {
            // Ok or Cancel was clicked
            // Close the dialog, default behavior
            super.onClick( dialog, which );
        }
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
        
        // Get all visible folders in this folder
        // TODO: Surely there must be an easier syntax here?
        File[] folderArray = startPath.listFiles( new FolderFilter());
        List<File> folders = new ArrayList<File>( );
        for( File file : folderArray )
            folders.add( file );
        Collections.sort( folders, new FileComparer() );
        
        // Construct the key-value pairs for the list entries
        boolean hasParent = startPath.getParentFile() != null;
        ArrayList<String> entries = new ArrayList<String>();
        ArrayList<String> values = new ArrayList<String>();
        if( hasParent )
        {
            entries.add( ".." );
            values.add( startPath.getParentFile().getAbsolutePath() );
        }
        for( File folder : folders )
        {
            entries.add( folder.getName() );
            values.add( folder.getAbsolutePath() );
        }
        
        // Populate the list
        setEntries( entries.toArray( new String[entries.size()] ) );
        setEntryValues( values.toArray( new String[values.size()] ) );
        
        // Update the menu text
        setDialogTitle( startPath.getName() );
        notifyChanged();
    }
    
    public class FolderFilter implements FileFilter
    {
        public boolean accept( File pathname )
        {
            return ( pathname != null ) && ( pathname.isDirectory() ) && ( !pathname.isHidden() )
                    && ( !pathname.getName().startsWith( "." ) );
        }        
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
