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

import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Utility;
import android.os.AsyncTask;
import android.text.TextUtils;

public class ExtractTexturesTask extends AsyncTask<Void, Void, Boolean>
{
    public interface ExtractTexturesListener
    {
        public void onExtractTexturesFinished( boolean success );
    }
    
    public ExtractTexturesTask( String srcFile, String dstDir, ExtractTexturesListener listener )
    {
        if( TextUtils.isEmpty( srcFile ) )
            throw new IllegalArgumentException( "Source file cannot be null or empty" );
        if( TextUtils.isEmpty( dstDir ) )
            throw new IllegalArgumentException( "Destination directory cannot be null or empty" );
        if( listener == null )
            throw new IllegalArgumentException( "Listener cannot be null" );
        
        mSrcFile = srcFile;
        mDstDir = dstDir;
        mListener = listener;
    }
    
    private final String mSrcFile;
    private final String mDstDir;
    private final ExtractTexturesListener mListener;
    
    @Override
    protected Boolean doInBackground( Void... params )
    {
        String headerName = Utility.getTexturePackName( mSrcFile );
        if( !TextUtils.isEmpty( headerName ) )
        {
            String outputFolder = mDstDir + headerName;
            FileUtil.deleteFolder( new File( outputFolder ) );
            return Utility.unzipAll( new File( mSrcFile ), outputFolder );
        }
        return false;
    }
    
    @Override
    protected void onPostExecute( Boolean result )
    {
        mListener.onExtractTexturesFinished( result );
    }
}
