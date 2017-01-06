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

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import org.mupen64plusae.v3.alpha.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.GalleryActivity;
import paulscode.android.mupen64plusae.dialog.ProgressDialog;
import paulscode.android.mupen64plusae.dialog.ProgressDialog.OnCancelListener;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.RomDatabase;
import paulscode.android.mupen64plusae.util.RomDatabase.RomDetail;
import paulscode.android.mupen64plusae.util.RomHeader;

public class CacheRomInfoService extends Service
{
    private String mSearchPath;
    private String mDatabasePath;
    private String mConfigPath;
    private String mArtDir;
    private String mUnzipDir;
    private boolean mSearchZips;
    private boolean mDownloadArt;
    private boolean mClearGallery;
    private boolean mSearchSubdirectories;
    private boolean mbStopped;
    
    private int mStartId;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    
    private final IBinder mBinder = new LocalBinder();
    private CacheRomInfoListener mListener = null;

    final static int ONGOING_NOTIFICATION_ID = 1;
    
    public interface CacheRomInfoListener
    {
        //This is called once the ROM scan is finished
        public void onCacheRomInfoFinished();
        
        //This is called when the service is destroyed
        public void onCacheRomInfoServiceDestroyed();
        
        //This is called to get a progress dialog object
        public ProgressDialog GetProgressDialog();
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public CacheRomInfoService getService() {
            // Return this instance of CacheRomInfoService so clients can call public methods
            return CacheRomInfoService.this;
        }
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        
        @Override
        public void handleMessage(Message msg) {

            File searchPathFile = new File(mSearchPath);
            
            if( mSearchPath == null )
                throw new IllegalArgumentException( "Root path cannot be null" );
            if( TextUtils.isEmpty( mDatabasePath ) )
                throw new IllegalArgumentException( "ROM database path cannot be null or empty" );
            if( TextUtils.isEmpty( mConfigPath ) )
                throw new IllegalArgumentException( "Config file path cannot be null or empty" );
            if( TextUtils.isEmpty( mArtDir ) )
                throw new IllegalArgumentException( "Art directory cannot be null or empty" );
            if( TextUtils.isEmpty( mUnzipDir ) )
                throw new IllegalArgumentException( "Unzip directory cannot be null or empty" );
            
            // Ensure destination directories exist
            FileUtil.makeDirs(mArtDir);
            FileUtil.makeDirs(mUnzipDir);
            
            // Create .nomedia file to hide cover art from Android Photo Gallery
            // http://android2know.blogspot.com/2013/01/create-nomedia-file.html
            touchFile( mArtDir + "/.nomedia" );
            
            final List<File> files = getAllFiles( searchPathFile, 0 );
            final RomDatabase database = RomDatabase.getInstance();
            if(!database.hasDatabaseFile())
            {
                database.setDatabaseFile(mDatabasePath);
            }
            
            final ConfigFile config = new ConfigFile( mConfigPath );
            if (mClearGallery)
                config.clear();
            
            mListener.GetProgressDialog().setMaxProgress( files.size() );
            for( final File file : files )
            {
                mListener.GetProgressDialog().setSubtext( "" );
                mListener.GetProgressDialog().setText( file.getName() );
                mListener.GetProgressDialog().setMessage( R.string.cacheRomInfo_searching );
                
                if( mbStopped ) break;
                RomHeader header = new RomHeader( file );
                if( header.isValid )
                {
                    cacheFile( file, database, config, null );
                }
                else if( header.isZip && mSearchZips && !ConfigHasZip(config, file.getPath()))
                {
                    Log.i( "CacheRomInfoService", "Found zip file " + file.getName() );
                    try
                    {
                        ZipFile zipFile = new ZipFile( file );
                        Enumeration<? extends ZipEntry> entries = zipFile.entries();
                        while( entries.hasMoreElements() )
                        {
                            try
                            {
                                ZipEntry zipEntry = entries.nextElement();
                                mListener.GetProgressDialog().setSubtext( new File(zipEntry.getName()).getName() );
                                mListener.GetProgressDialog().setMessage( R.string.cacheRomInfo_searchingZip );

                                if( mbStopped ) break;

                                InputStream zipStream = zipFile.getInputStream( zipEntry );
                                mListener.GetProgressDialog().setMessage( R.string.cacheRomInfo_extractingZip );
                                File extractedFile = FileUtil.extractRomFile( new File( mUnzipDir ), zipEntry, zipStream );
                                
                                if( mbStopped ) break;
                                if( extractedFile != null)
                                {
                                    RomHeader extractedHeader = new RomHeader( extractedFile );
                                    if(extractedHeader.isValid)
                                    {
                                        cacheFile( extractedFile, database, config, file );
                                    }
                 
                                    extractedFile.delete();
                                }

                                zipStream.close();
                            }
                            catch( IOException|IllegalArgumentException e  )
                            {
                                Log.w( "CacheRomInfoService", e );
                            }
                        }
                        zipFile.close();
                    }
                    catch( IOException|ArrayIndexOutOfBoundsException e )
                    {
                        Log.w( "CacheRomInfoService", e );
                    }
                }
                mListener.GetProgressDialog().incrementProgress( 1 );
            }

            CleanupMissingFiles(config);
            downloadCoverArt(database, config);

            config.save();
            
            if (mListener != null)
            {
                mListener.onCacheRomInfoFinished();
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
      // Start up the thread running the service.  Note that we create a
      // separate thread because the service normally runs in the process's
      // main thread, which we don't want to block.  We also make it
      // background priority so CPU-intensive work will not disrupt our UI.
      HandlerThread thread = new HandlerThread("ServiceStartArguments",
              Process.THREAD_PRIORITY_BACKGROUND);
      thread.start();

      // Get the HandlerThread's Looper and use it for our Handler
      mServiceLooper = thread.getLooper();
      mServiceHandler = new ServiceHandler(mServiceLooper);

      //Show the notification
      Intent notificationIntent = new Intent(this, GalleryActivity.class);
      PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
      NotificationCompat.Builder builder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.icon)
          .setContentTitle(getString(R.string.scanning_title))
          .setContentText(getString(R.string.toast_pleaseWait))
          .setContentIntent(pendingIntent);
      startForeground(ONGOING_NOTIFICATION_ID, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null)
        {
            Bundle extras = intent.getExtras();
            mSearchPath = extras.getString( ActivityHelper.Keys.SEARCH_PATH );
            mDatabasePath = extras.getString( ActivityHelper.Keys.DATABASE_PATH );
            mConfigPath = extras.getString( ActivityHelper.Keys.CONFIG_PATH );
            mArtDir = extras.getString( ActivityHelper.Keys.ART_DIR );
            mUnzipDir = extras.getString( ActivityHelper.Keys.UNZIP_DIR );
            mSearchZips = extras.getBoolean( ActivityHelper.Keys.SEARCH_ZIPS );
            mDownloadArt = extras.getBoolean( ActivityHelper.Keys.DOWNLOAD_ART );
            mClearGallery = extras.getBoolean( ActivityHelper.Keys.CLEAR_GALLERY );
            mSearchSubdirectories = extras.getBoolean( ActivityHelper.Keys.SEARCH_SUBDIR );
        }

        mbStopped = false;
        mStartId = startId;

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    /**
     * Get all files in a directory and subdirectories
     * @param searchPath Path to start search on
     * @param count How many levels deep we currently are
     * @return List of files
     */
    private List<File> getAllFiles( File searchPath, int count )
    {
        List<File> result = new ArrayList<File>();
        if( searchPath.isDirectory())
        {
            File[] allFiles = searchPath.listFiles();
            if(allFiles != null)
            {
                for( File file : allFiles )
                {
                    if( mbStopped ) break;

                    //Search subdirectories if option is enabled and we less than 10 levels deep
                    if(mSearchSubdirectories && count < 10)
                    {
                        result.addAll( getAllFiles( file, ++count ) );
                    }
                    else if(!file.isDirectory())
                    {
                        result.add(file);
                    }

                }
            }
        }
        else
        {
            result.add( searchPath );
        }
        return result;
    }
    
    private void cacheFile( File file, RomDatabase database, ConfigFile config, File zipFileLocation )
    {
        if( mbStopped ) return;
        mListener.GetProgressDialog().setMessage( R.string.cacheRomInfo_computingMD5 );
        String md5 = ComputeMd5Task.computeMd5( file );
        RomHeader header = new RomHeader(file);
        
        if( mbStopped ) return;
        mListener.GetProgressDialog().setMessage( R.string.cacheRomInfo_searchingDB );
        RomDetail detail = database.lookupByMd5WithFallback( md5, file, header.crc );
        String artPath = mArtDir + "/" + detail.artName;
        config.put( md5, "goodName", detail.goodName );
        if (detail.baseName != null && detail.baseName.length() != 0)
            config.put( md5, "baseName", detail.baseName );
        config.put( md5, "romPath", file.getAbsolutePath() );
        config.put( md5, "zipPath", zipFileLocation == null ? "":zipFileLocation.getAbsolutePath() );
        config.put( md5, "artPath", artPath );
        config.put( md5, "crc", header.crc );
        config.put( md5, "headerName", header.name );

        String countryCodeString = Byte.toString(header.countryCode);
        config.put( md5, "countryCode",  countryCodeString);
        config.put( md5, "extracted", "false" );

        mListener.GetProgressDialog().setMessage( R.string.cacheRomInfo_refreshingUI );
    }
    
    private static Throwable touchFile( String destPath )
    {
        try
        {
            OutputStream outStream = new FileOutputStream( destPath );
            try
            {
                outStream.close();
            }
            catch( IOException e )
            {
                Log.w( "CacheRomInfoService", e );
                return e;
            }
        }
        catch( FileNotFoundException e )
        {
            Log.w( "CacheRomInfoService", e );
            return e;
        }
        return null;
    }
    
    private Throwable downloadFile( String sourceUrl, String destPath )
    {
        File destFile = new File(destPath);
        boolean fileCreationSuccess = true;
        
        Throwable returnThrowable = null;

        // Be sure destination directory exists
        FileUtil.makeDirs(destFile.getParentFile().getPath());
        
        // Download file
        InputStream inStream = null;
        OutputStream outStream = null;
        try
        {
            // Open the streams (throws exceptions)
            URL url = new URL( sourceUrl );
            inStream = url.openStream();
            outStream = new FileOutputStream( destPath );

            // Buffer the streams
            inStream = new BufferedInputStream( inStream );
            outStream = new BufferedOutputStream( outStream );
            
            // Read/write the streams (throws exceptions)
            byte[] buffer = new byte[1024];
            int n;
            while( ( n = inStream.read( buffer ) ) >= 0 && !mbStopped)
            {
                outStream.write( buffer, 0, n );
            }
        }
        catch( Throwable e )
        {
            Log.w( "CacheRomInfoService", e );
            fileCreationSuccess = false;
            
            returnThrowable = e;
        }
        finally
        {
            // Flush output stream and guarantee no memory leaks
            if( outStream != null )
                try
                {
                    outStream.close();
                }
                catch( IOException e )
                {
                    Log.w( "CacheRomInfoService", e );
                }
            if( inStream != null )
                try
                {
                    inStream.close();
                }
                catch( IOException e )
                {
                    Log.w( "CacheRomInfoService", e );
                }
        }
        
        if (!fileCreationSuccess && !destFile.isDirectory())
        {
            // Delete any remnants if there was an exception. We don't want a
            // corrupted graphic
            destFile.delete();
        }
        
        return returnThrowable;
    }
    
    @Override
    public void onDestroy()
    {
        mbStopped = true;
        
        if (mListener != null)
        {
            mListener.onCacheRomInfoServiceDestroyed();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    public void SetCacheRomInfoListener(CacheRomInfoListener cacheRomInfoListener)
    {
        mListener = cacheRomInfoListener;
        mListener.GetProgressDialog().setOnCancelListener(new OnCancelListener()
        {
            @Override
            public void OnCancel()
            {
                Stop();
            }
        });
        
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = mStartId;
        mServiceHandler.sendMessage(msg);
    }

    public void Stop()
    {
        mbStopped = true;        
    }

    /**
     * Return true if the config file already contains the given zip file, this is because
     * exctracting zip files takes a long time
     * @param zipFile Zip file to search config file for
     * @return true if zip file is present
     */
    private boolean ConfigHasZip(ConfigFile theConfigFile, String zipFile)
    {
        Set<String> keys = theConfigFile.keySet();
        boolean found = false;

        Iterator iter = keys.iterator();
        while (iter.hasNext() && !found) {
            String key = (String) iter.next();
            String foundZipPath = theConfigFile.get(key, "zipPath");
            found = foundZipPath != null && foundZipPath.equals(zipFile);
        }
        return found;
    }

    /**
     * Cleanup any missing files from the config file
     * @param theConfigFile
     */
    private void CleanupMissingFiles(ConfigFile theConfigFile)
    {
        Set<String> keys = theConfigFile.keySet();

        Iterator iter = keys.iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            String foundZipPath = theConfigFile.get(key, "zipPath");
            String foundRomPath = theConfigFile.get(key, "romPath");

            //Check if this is a zip file first
            if(!TextUtils.isEmpty(foundZipPath))
            {
                File zipFile = new File(foundZipPath);

                //Zip file doesn't exist, check if the ROM path exists
                if(!zipFile.exists())
                {
                    if(!TextUtils.isEmpty(foundRomPath))
                    {
                        File romFile = new File(foundRomPath);

                        //Cleanup the ROM file since this is a zip file
                        if(!romFile.exists())
                        {
                            Log.w( "CacheRomInfoService", "Removing md5=" + key );
                            if(!romFile.isDirectory()) romFile.delete();
                        }
                    }

                    theConfigFile.remove(key);
                    keys = theConfigFile.keySet();
                    iter = keys.iterator();
                }
            }
            //This was not a zip file, just check the ROM path
            else if(!TextUtils.isEmpty(foundRomPath))
            {
                File romFile = new File(foundRomPath);

                //Cleanup the ROM file since this is a zip file
                if(!romFile.exists())
                {
                    Log.w( "CacheRomInfoService", "Removing md5=" + key );

                    theConfigFile.remove(key);
                    keys = theConfigFile.keySet();
                    iter = keys.iterator();
                }
            }
        }
    }

    private void downloadCoverArt(RomDatabase database, ConfigFile theConfigFile)
    {
        if( mDownloadArt )
        {
            Set<String> keys = theConfigFile.keySet();

            mListener.GetProgressDialog().setMaxProgress( keys.size() );
            mListener.GetProgressDialog().setSubtext( getString(R.string.cacheRomInfo_downloadingArt) );

            Iterator iter = keys.iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                String artPath = theConfigFile.get(key, "artPath");
                String romFile = theConfigFile.get(key, "romPath");
                String crc = theConfigFile.get(key, "crc");

                if(!TextUtils.isEmpty(artPath) && !TextUtils.isEmpty(romFile) && !TextUtils.isEmpty(crc))
                {
                    RomDetail detail = database.lookupByMd5WithFallback( key, new File(romFile), crc );

                    mListener.GetProgressDialog().setText( new File(romFile).getName() );

                    //Only download art if it's not already present
                    if(!(new File(artPath)).exists())
                    {
                        Log.i( "CacheRomInfoService", "Start art download: " +  artPath);
                        downloadFile( detail.artUrl, artPath );

                        Log.i( "CacheRomInfoService", "End art download: " +  artPath);
                    }
                }

                mListener.GetProgressDialog().incrementProgress(1);

                if( mbStopped ) return;
            }
        }
    }
}
