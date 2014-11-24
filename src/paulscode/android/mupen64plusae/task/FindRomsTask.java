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
package paulscode.android.mupen64plusae.task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.os.AsyncTask;

public class FindRomsTask extends AsyncTask<Void, Void, List<File>>
{
    public interface FindRomsListener
    {
        public void onFindRomsFinished( List<File> result );
    }
    
    public FindRomsTask( File rootPath, FindRomsListener listener )
    {
        if( rootPath == null )
            throw new IllegalArgumentException( "Root path cannot be null" );
        if( !rootPath.exists() )
            throw new IllegalArgumentException( "Root path does not exist: " + rootPath.getAbsolutePath() );
        if( listener == null )
            throw new IllegalArgumentException( "Listener cannot be null" );
        
        mRootPath = rootPath;
        mListener = listener;
    }
    
    private final File mRootPath;
    private final FindRomsListener mListener;
    
    @Override
    protected List<File> doInBackground( Void... params )
    {
        return getRomFiles( mRootPath );
    }
    
    private List<File> getRomFiles( File rootPath )
    {
        List<File> result = new ArrayList<File>();
        if( rootPath.isDirectory() )
        {
            for( File file : rootPath.listFiles() )
                result.addAll( getRomFiles( file ) );
        }
        else
        {
            String name = rootPath.getName().toLowerCase( Locale.US );
            if( name.matches( ".*\\.(n64|v64|z64|zip)$" ) )
                result.add( rootPath );
        }
        return result;
    }
    
    @Override
    protected void onPostExecute( List<File> result )
    {
        mListener.onFindRomsFinished( result );
    }
}