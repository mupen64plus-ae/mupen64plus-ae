/*
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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.documentfile.provider.DocumentFile;

import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.mupen64plusae.v3.alpha.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.GalleryActivity;
import paulscode.android.mupen64plusae.dialog.ProgressDialog;
import paulscode.android.mupen64plusae.dialog.ProgressDialog.OnCancelListener;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.util.CountryCode;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.RomDatabase;
import paulscode.android.mupen64plusae.util.RomDatabase.RomDetail;
import paulscode.android.mupen64plusae.util.RomHeader;
import paulscode.android.mupen64plusae.util.SevenZInputStream;

public class CacheRomInfoService extends Service
{
    private Uri mSearchUri = null;
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
    private ServiceHandler mServiceHandler;
    
    private final IBinder mBinder = new LocalBinder();
    private CacheRomInfoListener mListener = null;

    final static int ONGOING_NOTIFICATION_ID = 1;

    final static String NOTIFICATION_CHANNEL_ID = "CacheRomInfoServiceChannel";
    final static String NOTIFICATION_CHANNEL_ID_V2 = "CacheRomInfoServiceChannelV2";

    final static long MAX_7ZIP_FILE_SIZE = 100*1024*1024;
    final static int MAX_ROM_FILE_NAME_SIZE = 30;
    
    public interface CacheRomInfoListener
    {
        //This is called once the ROM scan is finished
        void onCacheRomInfoFinished();
        
        //This is called when the service is destroyed
        void onCacheRomInfoServiceDestroyed();
        
        //This is called to get a progress dialog object
        ProgressDialog GetProgressDialog();
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

        ServiceHandler(Looper looper) {
            super(looper);
        }
        
        @Override
        public void handleMessage(@NonNull Message msg) {

            if( mSearchUri == null )
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

            DocumentFile rootDocumentFile = FileUtil.getDocumentFileTree(getApplicationContext(), mSearchUri);

            final List<DocumentFile> files = getAllFiles( rootDocumentFile, 0 );

            final RomDatabase database = RomDatabase.getInstance();
            if(!database.hasDatabaseFile())
            {
                database.setDatabaseFile(mDatabasePath);
            }
            
            final ConfigFile config = new ConfigFile( mConfigPath );
            if (mClearGallery)
                config.clear();

            removeLegacyEntries(config);
            
            mListener.GetProgressDialog().setMaxProgress( files.size() );
            for( final DocumentFile file : files )
            {
                mListener.GetProgressDialog().setSubtext( "" );
                mListener.GetProgressDialog().setText( getShortFileName(file.getName()));
                mListener.GetProgressDialog().setMessage( R.string.cacheRomInfo_searching );
                
                if( mbStopped ) break;
                RomHeader header = new RomHeader( getApplicationContext(), file.getUri() );
                if( header.isValid || header.isNdd ) {
                    cacheFile( file, database, config);
                } else if (mSearchZips && !configHasZip(config, file.getUri())) {
                    if (header.isZip) {
                        cacheZip(database, file.getUri(), config);
                    } else if (header.is7Zip) {
                        cache7Zip(database, file.getUri(), config);
                    }
                }

                mListener.GetProgressDialog().incrementProgress( 1 );
            }

            cleanupMissingFiles(config);
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

    private static String getShortFileName(@Nullable String fileName)
    {
        String shortFileName = fileName != null ? fileName : "";
        if (shortFileName.length() > MAX_ROM_FILE_NAME_SIZE) {
            shortFileName = fileName.substring(0, MAX_ROM_FILE_NAME_SIZE-7) + "..." + fileName.substring(fileName.length()-5);
        }
        return shortFileName;
    }

    public void initChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_V2,
                getString(R.string.scanning_title), NotificationManager.IMPORTANCE_LOW);
        channel.enableVibration(false);
        channel.setSound(null,null);

        if(notificationManager != null) {
            notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID);
            notificationManager.createNotificationChannel(channel);
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
      Looper serviceLooper;
      serviceLooper = thread.getLooper();
      mServiceHandler = new ServiceHandler(serviceLooper);

      //Show the notification
      initChannels(getApplicationContext());
      Intent notificationIntent = new Intent(this, GalleryActivity.class);
      PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
      NotificationCompat.Builder builder =
          new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_V2).setSmallIcon(R.drawable.icon)
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

            if(extras == null)
            {
                throw new IllegalArgumentException("Invalid parameters passed to CacheRomInfoService");
            }

            String searchUriString = extras.getString( ActivityHelper.Keys.SEARCH_PATH );
            if ( searchUriString != null) {
                mSearchUri = Uri.parse(searchUriString);
            }
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

    List<DocumentFile> getAllFiles(DocumentFile documentFile, int count) {

        List<DocumentFile> result = new ArrayList<>();

        if (documentFile != null) {
            if( documentFile.isDirectory())
            {
                DocumentFile[] allFiles = documentFile.listFiles();

                for( DocumentFile file : allFiles )
                {
                    if( mbStopped ) break;

                    //Search subdirectories if option is enabled and we less than 10 levels deep
                    if(mSearchSubdirectories && count < 10)
                    {
                        result.addAll( getAllFiles( file, ++count ) );
                    }
                    else if(!file.isDirectory())
                    {
                        if (file.getName() != null)
                            result.add(file);
                    }
                }
            } else {
                if (documentFile.getName() != null)
                    result.add( documentFile );
            }
        }

        return result;
    }

    private void cacheZip(RomDatabase database, Uri file, ConfigFile config)
    {
        Log.i( "CacheRomInfoService", "Found zip file " + file.toString() );
        ParcelFileDescriptor parcelFileDescriptor = null;
        ZipInputStream zipfile = null;

        try
        {
            parcelFileDescriptor = getApplicationContext().getContentResolver().openFileDescriptor(file, "r");

            if (parcelFileDescriptor != null) {
                zipfile = new ZipInputStream( new BufferedInputStream(new FileInputStream(parcelFileDescriptor.getFileDescriptor()) ));

                ZipEntry entry = zipfile.getNextEntry();

                while( entry != null && !mbStopped)
                {
                    try
                    {
                        mListener.GetProgressDialog().setSubtext( getShortFileName(new File(entry.getName()).getName()));
                        mListener.GetProgressDialog().setMessage( R.string.cacheRomInfo_searchingZip );

                        InputStream zipStream = new BufferedInputStream(zipfile);
                        mListener.GetProgressDialog().setMessage( R.string.cacheRomInfo_extractingZip );

                        cacheZipFileFromInputStream(database, file, config, new File(entry.getName()).getName(), zipStream);

                        entry = zipfile.getNextEntry();
                    }
                    catch( IOException|NoSuchAlgorithmException|IllegalArgumentException e  )
                    {
                        Log.w( "CacheRomInfoService", e );
                    }
                }

                zipfile.close();
            }
        }
        catch( IOException|ArrayIndexOutOfBoundsException|java.lang.NullPointerException e )
        {
            Log.w( "CacheRomInfoService", e );
        }
        finally
        {
            try {
                if( zipfile != null ) {
                    zipfile.close();
                }
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
            } catch (IOException ignored) {
            }
        }
    }

    private void cache7Zip(RomDatabase database, Uri file, ConfigFile config)
    {
        Log.i( "CacheRomInfoService", "Found 7zip file " + file.toString() );

        try (ParcelFileDescriptor parcelFileDescriptor = getApplicationContext().getContentResolver().openFileDescriptor(file, "r")) {

            if (parcelFileDescriptor != null) {
                FileInputStream fileInputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
                if (fileInputStream.getChannel().size() < MAX_7ZIP_FILE_SIZE) {
                    SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel(
                            IOUtils.toByteArray(fileInputStream));

                    SevenZFile zipFile = new SevenZFile(channel);
                    SevenZArchiveEntry zipEntry;
                    while ((zipEntry = zipFile.getNextEntry()) != null && !mbStopped) {
                        InputStream zipStream;
                        try {
                            mListener.GetProgressDialog().setSubtext(getShortFileName(new File(zipEntry.getName()).getName()));
                            mListener.GetProgressDialog().setMessage(R.string.cacheRomInfo_searchingZip);

                            zipStream = new BufferedInputStream(new SevenZInputStream(zipFile));
                            mListener.GetProgressDialog().setMessage(R.string.cacheRomInfo_extractingZip);

                            cacheZipFileFromInputStream(database, file, config, new File(zipEntry.getName()).getName(),
                                    zipStream);

                        } catch (IOException | NoSuchAlgorithmException | IllegalArgumentException e) {
                            Log.w("CacheRomInfoService", e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.w("CacheRomInfoService", "IOException: " + e);
        } catch (OutOfMemoryError e) {
            Log.w("CacheRomInfoService", "Out of memory while extracting 7zip entry: " + file.getPath());
        }
    }

    private void cacheZipFileFromInputStream(RomDatabase database, Uri zipFile, ConfigFile config, String name,
                                             InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        //First get the rom header
        inputStream.mark(500);
        byte[] romHeader = FileUtil.extractRomHeader(inputStream);
        RomHeader extractedHeader;
        if(romHeader != null) {
            extractedHeader = new RomHeader( romHeader );

            if(extractedHeader.isValid)
            {
                Log.i( "FileUtil", "Found ROM entry " + name);

                //Then extract the ROM file
                inputStream.reset();

                String md5 = ComputeMd5Task.computeMd5( inputStream );

                cacheFile(null, name, extractedHeader, md5, database, config, zipFile );
            }
        }
    }

    private void cacheFile(@Nullable Uri uri, @NonNull String name, RomHeader header, String md5, RomDatabase database, ConfigFile config, Uri zipFileLocation )
    {
        mListener.GetProgressDialog().setMessage( R.string.cacheRomInfo_computingMD5 );

        mListener.GetProgressDialog().setMessage( R.string.cacheRomInfo_searchingDB );
        RomDetail detail = database.lookupByMd5WithFallback( md5, name, header.crc, header.countryCode );
        String artPath = mArtDir + "/" + detail.artName;
        config.put( md5, "goodName", detail.goodName );
        if (detail.baseName != null && detail.baseName.length() != 0)
            config.put( md5, "baseName", detail.baseName );
        config.put( md5, "romPathUri", uri == null ? name : uri.toString() );
        config.put( md5, "zipPathUri", zipFileLocation == null ? "":zipFileLocation.toString() );
        config.put( md5, "artPath", artPath );
        config.put( md5, "crc", header.crc );
        config.put( md5, "headerName", header.name );

        String countryCodeString = Byte.toString(header.countryCode.getValue());
        config.put( md5, "countryCode",  countryCodeString);

        mListener.GetProgressDialog().setMessage( R.string.cacheRomInfo_refreshingUI );
    }

    private void cacheFile(DocumentFile file, RomDatabase database, ConfigFile config )
    {
        mListener.GetProgressDialog().setSubtext(getShortFileName(file.getName()));

        try (ParcelFileDescriptor parcelFileDescriptor = getApplicationContext().getContentResolver().openFileDescriptor(file.getUri(), "r")) {

            if (parcelFileDescriptor != null) {
                InputStream bufferedStream = new BufferedInputStream(new FileInputStream(parcelFileDescriptor.getFileDescriptor()));
                String md5 = ComputeMd5Task.computeMd5(bufferedStream);
                RomHeader header = new RomHeader(getApplicationContext(), file.getUri());

                String fileName = file.getName();
                if (fileName != null) {
                    cacheFile(file.getUri(), fileName, header, md5, database, config, null);
                }
            }

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void downloadFile( String sourceUrl, String destPath )
    {
        File destFile = new File(destPath);
        boolean fileCreationSuccess = true;

        File parentFile = destFile.getParentFile();

        if (parentFile != null) {
            // Be sure destination directory exists
            FileUtil.makeDirs(destFile.getParentFile().getPath());
        }

        // Delete the file if it already exists, we are replacing it
        if (destFile.exists())
        {
            if (destFile.delete())
            {
                Log.w( "CacheRomInfoService", "Unable to delete " + destFile.getName());
            }
        }

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
            while( ( n = inStream.read( buffer ) ) >= 0)
            {
                outStream.write( buffer, 0, n );
            }

            // Check if downloaded file is valud
            if (!FileUtil.isFileImage(destFile))
            {
                if (destFile.delete())
                {
                    Log.w( "CacheRomInfoService", "Deleting invalid image " + destFile.getName());
                }
            }
        }
        catch( Throwable e )
        {
            Log.w( "CacheRomInfoService", e );
            fileCreationSuccess = false;
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
            if (!destFile.delete()) {
                Log.w("CacheRomInfoService", "Unable to delete " + destFile.getName());
            }
        }
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
                stop();
            }
        });

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = mStartId;
        mServiceHandler.sendMessage(msg);
    }

    public void stop()
    {
        mbStopped = true;        
    }

    /**
     * Return true if the config file already contains the given zip file, this is because
     * exctracting zip files takes a long time
     * @param zipFile Zip file to search config file for
     * @return true if zip file is present
     */
    private boolean configHasZip(ConfigFile theConfigFile, Uri zipFile)
    {
        Set<String> keys = theConfigFile.keySet();
        boolean found = false;

        Iterator iter = keys.iterator();
        String key = null;
        while (iter.hasNext() && !found) {
            key = (String) iter.next();
            String foundZipPath = theConfigFile.get(key, "zipPathUri");
            found = foundZipPath != null && foundZipPath.equals(zipFile.toString());
        }

        // If found,make sure it also has  valid data
        if (found && key != null) {
            String crc = theConfigFile.get( key, "crc" );
            String headerName = theConfigFile.get( key, "headerName" );
            final String countryCodeString = theConfigFile.get( key, "countryCode" );

            found = crc != null && headerName != null && countryCodeString != null;
        }

        return found;
    }

    /**
     * Cleanup any missing files from the config file
     * @param theConfigFile Config file to clean up
     */
    private void cleanupMissingFiles(ConfigFile theConfigFile)
    {
        Set<String> keys = theConfigFile.keySet();

        Iterator iter = keys.iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            String foundZipPath = theConfigFile.get(key, "zipPathUri");
            String foundRomPath = theConfigFile.get(key, "romPathUri");

            //Check if this is a zip file first
            if(!TextUtils.isEmpty(foundZipPath))
            {

                DocumentFile zipFile = FileUtil.getDocumentFileSingle(getApplicationContext(), Uri.parse(foundZipPath));

                //Zip file doesn't exist, check if the ROM path exists
                if(zipFile == null || !zipFile.exists())
                {
                    if(!TextUtils.isEmpty(foundRomPath))
                    {
                        DocumentFile romFile = FileUtil.getDocumentFileSingle(getApplicationContext(), Uri.parse(foundRomPath));

                        //Cleanup the ROM file since this is a zip file
                        if(romFile != null && !romFile.exists())
                        {
                            Log.i( "CacheRomInfoService", "Removing md5=" + key );
                            if(!romFile.isDirectory() && romFile.delete()) {
                                Log.w( "CacheRomInfoService", "Unable to delete " + romFile.getName() );
                            }
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
                DocumentFile romFile = FileUtil.getDocumentFileSingle(getApplicationContext(), Uri.parse(foundRomPath));

                //Remove the entry since it doesn't exist
                if(romFile == null || !romFile.exists())
                {
                    Log.w( "CacheRomInfoService", "Removing md5=" + key );

                    theConfigFile.remove(key);
                    keys = theConfigFile.keySet();
                    iter = keys.iterator();
                }
            }
        }
    }

    /**
     * Removes legacy entries from a config file
     * @param theConfigFile Config file to parse
     */
    private void removeLegacyEntries(ConfigFile theConfigFile)
    {
        Set<String> keys = theConfigFile.keySet();

        Iterator iter = keys.iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            String foundZipPath = theConfigFile.get(key, "zipPath");
            String foundRomPath = theConfigFile.get(key, "romPath");

            //Check if this is a legacy entry, if it is remove it
            if(!TextUtils.isEmpty(foundRomPath) || !TextUtils.isEmpty(foundZipPath))
            {
                Log.i( "CacheRomInfoService", "Removing md5=" + key );

                theConfigFile.remove(key);
                keys = theConfigFile.keySet();
                iter = keys.iterator();
            }
        }
    }

    private void downloadCoverArt(RomDatabase database, ConfigFile theConfigFile)
    {
        if( mDownloadArt )
        {
            Set<String> keys = theConfigFile.keySet();

            mListener.GetProgressDialog().setMaxProgress( keys.size() );

            mListener.GetProgressDialog().setMessage( "" );
            mListener.GetProgressDialog().setSubtext( getString(R.string.cacheRomInfo_downloadingArt) );

            for (String key : keys) {
                String artPath = theConfigFile.get(key, "artPath");
                String romGoodName = theConfigFile.get(key, "goodName");
                String crc = theConfigFile.get(key, "crc");
                final String countryCodeString = theConfigFile.get( key, "countryCode" );
                CountryCode countryCode = CountryCode.UNKNOWN;
                if (countryCodeString != null)
                {
                    countryCode = CountryCode.getCountryCode(Byte.parseByte(countryCodeString));
                }

                if(!TextUtils.isEmpty(artPath) && !TextUtils.isEmpty(romGoodName) && !TextUtils.isEmpty(crc))
                {
                    RomDetail detail = database.lookupByMd5WithFallback( key, romGoodName, crc, countryCode );
                    mListener.GetProgressDialog().setText(getShortFileName(romGoodName));

                    //Only download art if it's not already present or current art is not a valid image
                    File artPathFile = new File (artPath);
                    if(!artPathFile.exists() || !FileUtil.isFileImage(artPathFile))
                    {
                        Log.i( "CacheRomInfoService", "Start art download: " +  artPath);
                        downloadFile( detail.artUrl, artPath );

                        Log.i( "CacheRomInfoService", "End art download: " +  artPath);
                    }
                }

                mListener.GetProgressDialog().incrementProgress(1);

                if( mbStopped ) break;
            }
        }
    }
}
