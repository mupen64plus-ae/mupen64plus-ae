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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.util.RomDatabase;
import paulscode.android.mupen64plusae.util.RomDatabase.RomDetail;
import android.os.AsyncTask;
import android.text.TextUtils;

public class CacheRomInfoTask extends AsyncTask<Void, ConfigSection, ConfigFile>
{
    public interface CacheRomInfoListener
    {
        public void onCacheRomInfoProgress( ConfigSection section );
        
        public void onCacheRomInfoFinished( ConfigFile file );
    }
    
    public CacheRomInfoTask( File searchPath, String databasePath, String configPath, String artDir, CacheRomInfoListener listener )
    {
        if( searchPath == null )
            throw new IllegalArgumentException( "Root path cannot be null" );
        if( !searchPath.exists() )
            throw new IllegalArgumentException( "Root path does not exist: " + searchPath.getAbsolutePath() );
        if( TextUtils.isEmpty( databasePath ) )
            throw new IllegalArgumentException( "ROM database path cannot be null or empty" );
        if( TextUtils.isEmpty( configPath ) )
            throw new IllegalArgumentException( "Config file path cannot be null or empty" );
        if( TextUtils.isEmpty( artDir ) )
            throw new IllegalArgumentException( "Art directory cannot be null or empty" );
        if( listener == null )
            throw new IllegalArgumentException( "Listener cannot be null" );
        
        mSearchPath = searchPath;
        mDatabasePath = databasePath;
        mConfigPath = configPath;
        mArtDir = artDir;
        mListener = listener;
    }
    
    private final File mSearchPath;
    private final String mDatabasePath;
    private final String mConfigPath;
    private final String mArtDir;
    private final CacheRomInfoListener mListener;
    
    @Override
    protected ConfigFile doInBackground( Void... params )
    {
        final List<File> files = getRomFiles( mSearchPath );
        final RomDatabase database = new RomDatabase( mDatabasePath );
        final ConfigFile config = new ConfigFile( mConfigPath );
        config.clear();
        
        for( final File file : files )
        {
            String md5 = ComputeMd5Task.computeMd5( file );
            RomDetail detail = database.lookupByMd5WithFallback( md5, file );
            String artPath = mArtDir + "/" + detail.artName;
            config.put( md5, "goodName", detail.goodName );
            config.put( md5, "romPath", file.getAbsolutePath() );
            config.put( md5, "artPath", artPath );
            downloadFile( detail.artUrl, artPath );
            
            this.publishProgress( config.get( md5 ) );
        }
        config.save();
        return config;
    }
    
    @Override
    protected void onProgressUpdate( ConfigSection... values )
    {
        mListener.onCacheRomInfoProgress( values[0] );
    }
    
    @Override
    protected void onPostExecute( ConfigFile result )
    {
        mListener.onCacheRomInfoFinished( result );
    }
    
    private List<File> getRomFiles( File searchPath )
    {
        List<File> result = new ArrayList<File>();
        if( searchPath.isDirectory() )
        {
            for( File file : searchPath.listFiles() )
                result.addAll( getRomFiles( file ) );
        }
        else
        {
            String name = searchPath.getName().toLowerCase( Locale.US );
            if( name.matches( ".*\\.(n64|v64|z64|zip)$" ) )
                result.add( searchPath );
        }
        return result;
    }
    
    private Throwable downloadFile( String sourceUrl, String destPath )
    {
        // Be sure destination directory exists
        new File( destPath ).getParentFile().mkdirs();
        
        // Download file
        URL url = null;
        DataInputStream input = null;
        FileOutputStream fos = null;
        DataOutputStream output = null;
        try
        {
            url = new URL( sourceUrl );
            input = new DataInputStream( url.openStream() );
            fos = new FileOutputStream( destPath );
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
}