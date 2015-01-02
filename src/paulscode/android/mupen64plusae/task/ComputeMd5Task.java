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
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Locale;

import android.os.AsyncTask;

public class ComputeMd5Task extends AsyncTask<Void, Void, String>
{
    public interface ComputeMd5Listener
    {
        public void onComputeMd5Finished( File file, String md5 );
    }
    
    public ComputeMd5Task( File file, ComputeMd5Listener listener )
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
    private final ComputeMd5Listener mListener;
    
    @Override
    protected String doInBackground( Void... params )
    {
        return computeMd5( mFile );
    }
    
    @Override
    protected void onPostExecute( String result )
    {
        mListener.onComputeMd5Finished( mFile, result );
    }
    
    public static String computeMd5( File file )
    {
        // From http://stackoverflow.com/a/16938703
        InputStream inputStream = null;
        try
        {
            inputStream = new FileInputStream( file );
            MessageDigest digester = MessageDigest.getInstance( "MD5" );
            byte[] bytes = new byte[8192];
            int byteCount;
            while( ( byteCount = inputStream.read( bytes ) ) > 0 )
            {
                digester.update( bytes, 0, byteCount );
            }
            return convertHashToString( digester.digest() );
        }
        catch( Exception e )
        {
            return null;
        }
        finally
        {
            if( inputStream != null )
            {
                try
                {
                    inputStream.close();
                }
                catch( Exception e )
                {
                }
            }
        }
    }
    
    private static String convertHashToString( byte[] md5Bytes )
    {
        // From http://stackoverflow.com/a/16938703
        String returnVal = "";
        for( int i = 0; i < md5Bytes.length; i++ )
        {
            returnVal += Integer.toString( ( md5Bytes[i] & 0xff ) + 0x100, 16 ).substring( 1 );
        }
        return returnVal.toUpperCase( Locale.US );
    }
}