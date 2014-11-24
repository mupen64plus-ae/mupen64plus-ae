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

import paulscode.android.mupen64plusae.util.RomDetail;
import paulscode.android.mupen64plusae.util.RomHeader;
import android.os.AsyncTask;

public class ComputeFileHashesTask extends AsyncTask<Void, Void, String[]>
{
    public interface ComputeFileHashesListener
    {
        public void onComputeFileHashesFinished( File file, String md5, String crc );
    }
    
    public ComputeFileHashesTask( File file, ComputeFileHashesListener listener )
    {
        if( file == null)
            throw new IllegalArgumentException( "File cannot be null" );
        if( !file.exists() )
            throw new IllegalArgumentException( "File does not exist: " + file.getAbsolutePath() );
        if( listener == null)
            throw new IllegalArgumentException( "Listener cannot be null" );
        
        mFile = file;
        mListener = listener;
    }
    
    private final File mFile;
    private final ComputeFileHashesListener mListener;
    
    @Override
    protected String[] doInBackground( Void... params )
    {
        String md5 = RomDetail.computeMd5( mFile );
        String crc = new RomHeader( mFile ).crc;
        String[] result = { md5, crc };
        return result;
    }
    
    @Override
    protected void onPostExecute( String[] result )
    {
        mListener.onComputeFileHashesFinished( mFile, result[0], result[1] );
    }
}