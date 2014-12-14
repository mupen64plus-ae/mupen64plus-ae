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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import android.os.AsyncTask;
import android.text.TextUtils;

/**
 * Asynchronously copies a file from a network source to a local destination.
 */
public class DownloadFileTask extends AsyncTask<Void, Void, Throwable>
{
    public interface DownloadFileListener
    {
        public void onDownloadFinished( String sourceUrl, String destPath, Throwable error );
    }
    
    /**
     * @param sourceUrl     URL of the source file
     * @param destPath      Full path of the destination file
     */
    public DownloadFileTask( String sourceUrl, String destPath, DownloadFileListener listener )
    {
        if( TextUtils.isEmpty( sourceUrl ) )
            throw new IllegalArgumentException( "Source URL cannot be null or empty" );
        if( TextUtils.isEmpty( destPath ) )
            throw new IllegalArgumentException( "Destination path cannot be null or empty" );
        if( listener == null )
            throw new IllegalArgumentException( "Listener cannot be null" );
        
        mSourceUrl = sourceUrl;
        mDestPath = destPath;
        mListener = listener;
    }
    
    private final String mSourceUrl;
    private final String mDestPath;
    private final DownloadFileListener mListener;
    
    @Override
    protected Throwable doInBackground( Void... params )
    {
        // Be sure destination directory exists
        new File( mDestPath ).getParentFile().mkdirs();
        
        // Download file
        URL url = null;
        DataInputStream input = null;
        FileOutputStream fos = null;
        DataOutputStream output = null;
        try
        {
            url = new URL( mSourceUrl );
            input = new DataInputStream( url.openStream() );
            fos = new FileOutputStream( mDestPath );
            output = new DataOutputStream( fos );
            
            int contentLength = url.openConnection().getContentLength();
            byte[] buffer = new byte[contentLength];
            input.readFully( buffer );
            output.write( buffer );
            output.flush();
        }
        catch( Throwable error )
        {
            return error;
        }
        finally
        {
            if( output != null )
                try
                {
                    output.close();
                }
                catch( IOException ignored )
                {
                }
            if( fos != null )
                try
                {
                    fos.close();
                }
                catch( IOException ignored )
                {
                }
            if( input != null )
                try
                {
                    input.close();
                }
                catch( IOException ignored )
                {
                }
        }
        return null;
    }
    
    @Override
    protected void onPostExecute( Throwable result )
    {
        mListener.onDownloadFinished( mSourceUrl, mDestPath, result );
    }
}