/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2013 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import paulscode.android.mupen64plusae.persistent.ConfigFile;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

public class RomCache
{
    public interface OnFinishedListener
    {
        public void onProgress( String name );
        
        public void onFinished( ConfigFile config );
    }
    
    public static void refreshRoms( final File startDir, final String configPath, final String cacheDir,
            final OnFinishedListener listener )
    {
        // Asynchronously search for ROMs
        new AsyncTask<File, Void, List<File>>()
        {
            @Override
            protected List<File> doInBackground( File... params )
            {
                return getRomFiles( params[0] );
            }
            
            private List<File> getRomFiles( File rootPath )
            {
                List<File> allfiles = new ArrayList<File>();
                if( rootPath != null && rootPath.exists() )
                {
                    if( rootPath.isDirectory() )
                    {
                        for( File file : rootPath.listFiles() )
                            allfiles.addAll( getRomFiles( file ) );
                    }
                    else
                    {
                        String name = rootPath.getName().toLowerCase( Locale.US );
                        if( name.matches( ".*\\.(n64|v64|z64|zip)$" ) )
                            allfiles.add( rootPath );
                    }
                }
                return allfiles;
            }
            
            @Override
            protected void onPostExecute( List<File> files )
            {
                super.onPostExecute( files );
                cacheRomInfo( files, configPath, cacheDir, listener );
            }
        }.execute( startDir );
    }
    
    private static void cacheRomInfo( List<File> files, final String configPath, final String cacheDir,
            final OnFinishedListener listener )
    {
        new AsyncTask<File, String, ConfigFile>()
        {
            @Override
            protected ConfigFile doInBackground( File... files )
            {
                final ConfigFile config = new ConfigFile( configPath );
                config.clear();
                for( final File file : files )
                {
                    String md5 = RomDetail.computeMd5( file );
                    RomDetail detail = RomDetail.lookupByMd5( md5 );
                    if( detail == null )
                    {
                        // MD5 not in the database; lookup by CRC instead
                        String crc = new RomHeader( file ).crc;
                        RomDetail[] romDetails = RomDetail.lookupByCrc( crc );
                        if( romDetails.length == 0 )
                        {
                            // CRC not in the database; create best guess
                            Log.w( "RomCache", "No meta-info entry found for ROM " + file.getAbsolutePath() );
                            Log.i( "RomCache", "Constructing a best guess for the meta-info");
                            String goodName = file.getName().split( "\\." )[0];
                            detail = RomDetail.createAssumption( md5, crc, goodName );
                        }
                        else if( romDetails.length > 1 )
                        {
                            // CRC in the database more than once; let user pick best match
                            // TODO Implement popup selector
                            Log.w( "RomCache", "Multiple meta-info entries found for ROM " + file.getAbsolutePath() );
                            Log.i( "RomCache", "Defaulting to first entry");
                            detail = romDetails[0];
                        }
                        else
                        {
                            // CRC in the database exactly once; use it
                            detail = romDetails[0];
                        }
                    }
                    String artPath = cacheDir + "/" + detail.artName;
                    
                    this.publishProgress( detail.goodName );
                    config.put( md5, "goodName", detail.goodName );
                    config.put( md5, "romPath", file.getAbsolutePath() );
                    config.put( md5, "artPath", artPath );
                }
                config.save();
                return config;
            }
            
            @Override
            protected void onProgressUpdate( String... values )
            {
                super.onProgressUpdate( values );
                if( listener != null )
                    listener.onProgress( values[0] );
            }
            
            @Override
            protected void onPostExecute( ConfigFile result )
            {
                super.onPostExecute( result );
                if( listener != null )
                    listener.onFinished( result );
            }
        }.execute( files.toArray( new File[files.size()] ) );
    }
    
    public static void refreshArt( final String configPath, final String cacheDir,
            final OnFinishedListener listener )
    {
        new AsyncTask<Void, String, ConfigFile>()
        {
            @Override
            protected ConfigFile doInBackground( Void... values )
            {
                final ConfigFile config = new ConfigFile( configPath );
                for( String md5 : config.keySet() )
                {
                    if( !ConfigFile.SECTIONLESS_NAME.equals( md5 ) )
                    {
                        RomDetail detail = RomDetail.lookupByMd5( md5 );
                        String artPath = config.get( md5, "artPath" );
                        downloadArt( detail.artUrl, artPath );
                        
                        this.publishProgress( detail.goodName );
                    }
                }
                return config;
            }
            
            @Override
            protected void onProgressUpdate( String... values )
            {
                super.onProgressUpdate( values );
                if( listener != null )
                    listener.onProgress( values[0] );
            }
            
            @Override
            protected void onPostExecute( ConfigFile result )
            {
                super.onPostExecute( result );
                if( listener != null )
                    listener.onFinished( result );
            }
        }.execute();
    }
    
    private static boolean downloadArt( String artUrl, String destination )
    {
        if( TextUtils.isEmpty( artUrl ) )
            return false;
        
        URL url = null;
        DataInputStream input = null;
        FileOutputStream fos = null;
        DataOutputStream output = null;
        try
        {
            url = new URL( artUrl );
            input = new DataInputStream( url.openStream() );
            fos = new FileOutputStream( destination );
            output = new DataOutputStream( fos );
            
            int contentLength = url.openConnection().getContentLength();
            byte[] buffer = new byte[contentLength];
            input.readFully( buffer );
            output.write( buffer );
            output.flush();
        }
        catch( Exception ignored )
        {
            return false;
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
        return true;
    }
}
