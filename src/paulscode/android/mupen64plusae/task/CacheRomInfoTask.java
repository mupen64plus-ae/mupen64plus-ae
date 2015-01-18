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

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.dialog.ProgressDialog;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.util.RomDatabase;
import paulscode.android.mupen64plusae.util.RomDatabase.RomDetail;
import paulscode.android.mupen64plusae.util.RomHeader;
import android.app.Activity;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

public class CacheRomInfoTask extends AsyncTask<Void, ConfigSection, ConfigFile>
{
    public interface CacheRomInfoListener
    {
        public void onCacheRomInfoProgress( ConfigSection section );
        
        public void onCacheRomInfoFinished( ConfigFile file, boolean canceled );
    }
    
    public CacheRomInfoTask( Activity activity, File searchPath, String databasePath, String configPath, String artDir, String unzipDir, CacheRomInfoListener listener )
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
        if( TextUtils.isEmpty( unzipDir ) )
            throw new IllegalArgumentException( "Unzip directory cannot be null or empty" );
        if( listener == null )
            throw new IllegalArgumentException( "Listener cannot be null" );
        
        mSearchPath = searchPath;
        mDatabasePath = databasePath;
        mConfigPath = configPath;
        mArtDir = artDir;
        mUnzipDir = unzipDir;
        mListener = listener;
        
        CharSequence title = activity.getString( R.string.scanning_title );
        CharSequence message = activity.getString( R.string.toast_pleaseWait );
        mProgress = new ProgressDialog( activity, this, title, mSearchPath.getAbsolutePath(), message, true );
        mProgress.show();
    }
    
    private final File mSearchPath;
    private final String mDatabasePath;
    private final String mConfigPath;
    private final String mArtDir;
    private final String mUnzipDir;
    private final CacheRomInfoListener mListener;
    private final ProgressDialog mProgress;
    
    @Override
    protected ConfigFile doInBackground( Void... params )
    {
        // Ensure destination directories exist
        new File( mArtDir ).mkdirs();
        new File( mUnzipDir ).mkdirs();
        
        // Create .nomedia file to hide cover art from Android Photo Gallery
        // http://android2know.blogspot.com/2013/01/create-nomedia-file.html
        touchFile( mArtDir + "/.nomedia" );
        
        final List<File> files = getAllFiles( mSearchPath );
        final RomDatabase database = new RomDatabase( mDatabasePath );
        final ConfigFile config = new ConfigFile( mConfigPath );
        config.clear();
        
        mProgress.setMaxProgress( files.size() );
        for( final File file : files )
        {
            mProgress.setMaxSubprogress( 0 );
            mProgress.setSubtext( "" );
            mProgress.setText( file.getAbsolutePath().substring( mSearchPath.getAbsolutePath().length() ) );
            
            if( isCancelled() ) break;
            RomHeader header = new RomHeader( file );
            if( header.isValid )
            {
                cacheFile( file, database, config );
            }
            else if( header.isZip )
            {
                Log.i( "CacheRomInfoTask", "Found zip file " + file.getName() );
                try
                {
                    ZipFile zipFile = new ZipFile( file );
                    mProgress.setMaxSubprogress( zipFile.size() );
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while( entries.hasMoreElements() )
                    {
                        ZipEntry zipEntry = entries.nextElement();
                        mProgress.setSubtext( zipEntry.getName() );
                        
                        if( isCancelled() ) break;
                        try
                        {
                            InputStream zipStream = zipFile.getInputStream( zipEntry );
                            File extractedFile = extractRomFile( new File( mUnzipDir ), zipEntry, zipStream );
                            
                            if( isCancelled() ) break;
                            if( extractedFile != null )
                                cacheFile( extractedFile, database, config );
                            zipStream.close();
                        }
                        catch( IOException e )
                        {
                            Log.w( "CacheRomInfoTask", e );
                        }
                        mProgress.incrementSubprogress( 1 );
                    }
                    zipFile.close();
                }
                catch( ZipException e )
                {
                    Log.w( "CacheRomInfoTask", e );
                }
                catch( IOException e )
                {
                    Log.w( "CacheRomInfoTask", e );
                }
            }
            mProgress.incrementProgress( 1 );
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
        mListener.onCacheRomInfoFinished( result, false );
        mProgress.dismiss();
    }
    
    @Override
    protected void onCancelled( ConfigFile result )
    {
        mListener.onCacheRomInfoFinished( result, true );
        mProgress.dismiss();
    }
    
    private List<File> getAllFiles( File searchPath )
    {
        List<File> result = new ArrayList<File>();
        if( searchPath.isDirectory() )
        {
            for( File file : searchPath.listFiles() )
            {
                if( isCancelled() ) break;
                result.addAll( getAllFiles( file ) );
            }
        }
        else
        {
            result.add( searchPath );
        }
        return result;
    }
    
    private void cacheFile( File file, RomDatabase database, ConfigFile config )
    {
        if( isCancelled() ) return;
        String md5 = ComputeMd5Task.computeMd5( file );
        
        if( isCancelled() ) return;
        RomDetail detail = database.lookupByMd5WithFallback( md5, file );
        
        if( isCancelled() ) return;
        String artPath = mArtDir + "/" + detail.artName;
        config.put( md5, "goodName", detail.goodName );
        config.put( md5, "romPath", file.getAbsolutePath() );
        config.put( md5, "artPath", artPath );
        downloadFile( detail.artUrl, artPath );
        
        if( isCancelled() ) return;
        this.publishProgress( config.get( md5 ) );
    }
    
    private File extractRomFile( File destDir, ZipEntry zipEntry, InputStream zipStream )
    {
        if( zipEntry.isDirectory() )
            return null;
        
        // Read the first 4 bytes of the entry
        byte[] buffer = new byte[1024];
        try
        {
            if( zipStream.read( buffer, 0, 4 ) != 4 )
                return null;
        }
        catch( IOException e )
        {
            Log.w( "CacheRomInfoTask", e );
            return null;
        }
        
        // See if this entry is a valid ROM (copy bits in case RomHeader twiddles them)
        if( !new RomHeader( new byte[] { buffer[0], buffer[1], buffer[2], buffer[3] } ).isValid )
            return null;
        
        // This entry appears to be a valid ROM, extract it
        Log.i( "CacheRomInfoTask", "Found zip entry " + zipEntry.getName() );
        String entryName = new File( zipEntry.getName() ).getName();
        File extractedFile = new File( destDir, entryName );
        try
        {
            FileOutputStream outStream = new FileOutputStream( extractedFile );
            try
            {
                BufferedOutputStream boutStream = new BufferedOutputStream( outStream );
                
                // Write the first four bytes we already peeked at
                boutStream.write( buffer, 0, 4 );
                
                // Read/write the remainder of the zip entry
                int n;
                while( ( n = zipStream.read( buffer ) ) >= 0 )
                {
                    boutStream.write( buffer, 0, n );
                }
                boutStream.close();
                return extractedFile;
            }
            catch( IOException e )
            {
                Log.w( "CacheRomInfoTask", e );
                return null;
            }
            finally
            {
                outStream.close();
            }
        }
        catch( IOException e )
        {
            Log.w( "CacheRomInfoTask", e );
            return null;
        }
    }
    
    private static Throwable touchFile( String destPath )
    {
        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream( destPath );
        }
        catch( Throwable error )
        {
            return error;
        }
        finally
        {
            if( fos != null )
                try
                {
                    fos.close();
                }
                catch( IOException ignored )
                {
                }
        }
        return null;
    }
    
    private static Throwable downloadFile( String sourceUrl, String destPath )
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