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
import android.content.UriPermission;
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
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import paulscode.android.mupen64plusae.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import androidx.documentfile.provider.DocumentFile;
import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.GalleryActivity;
import paulscode.android.mupen64plusae.dialog.ProgressDialog;
import paulscode.android.mupen64plusae.persistent.AppData;
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
    private boolean mSearchZips;
    private boolean mDownloadArt;
    private boolean mClearGallery;
    private boolean mSearchSubdirectories;
    private boolean mSearchSingleFile;
    private boolean mbStopped;
    private int mCurrentProgress = 0;
    private int mCurrentMaxProgress = 0;
    private String mCurrentDialogText = "";
    private String mCurrentDialogSubText = "";
    private String mCurrentDialogMessage = "";

    private int mStartId;
    private ServiceHandler mServiceHandler;
    
    private final IBinder mBinder = new LocalBinder();
    private CacheRomInfoListener mListener = null;

    final static int ONGOING_NOTIFICATION_ID = 1;

    final static String NOTIFICATION_CHANNEL_ID_V2 = "CacheRomInfoServiceChannelV2";

    final static int MAX_ROM_FILE_NAME_SIZE = 25;
    
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

            // Ensure destination directories exist
            FileUtil.makeDirs(mArtDir);

            List<Uri> filesToSearch;
            if (mSearchSingleFile)
            {
                filesToSearch = new ArrayList<>();

                if (mSearchUri != null) {
                    filesToSearch.add(mSearchUri);
                }
            }
            else
            {
                filesToSearch = FileUtil.listAllFiles(getApplicationContext(), mSearchUri, mSearchSubdirectories);

                if (filesToSearch.isEmpty()) {

                    DocumentFile fileTree = FileUtil.getDocumentFileTree(getApplicationContext(), mSearchUri);
                    filesToSearch = FileUtil.listAllFilesLegacy(fileTree, mSearchSubdirectories);
                }
            }

            final RomDatabase database = RomDatabase.getInstance();
            if(!database.hasDatabaseFile())
            {
                database.setDatabaseFile(mDatabasePath);
            }
            
            final ConfigFile config = new ConfigFile( mConfigPath );
            if (mClearGallery)
                config.clear();

            // Don't do this if we are trying to just quickly search a single file
            if (!mSearchSingleFile)
            {
                removeLegacyEntries(config);
                cleanupMissingFiles(config);
            }

            mCurrentProgress = 0;
            mCurrentMaxProgress = filesToSearch.size();
            updateDialog();

            for( final Uri file : filesToSearch )
            {
                mCurrentDialogSubText = "";
                updateDialog();

                String fileName = FileUtil.getFileName(getApplicationContext(), file);
                if (fileName == null) {
                    continue;
                }

                mCurrentDialogText = getShortFileName(fileName);
                mCurrentDialogMessage = getString(R.string.cacheRomInfo_searching);
                updateDialog();

                if( mbStopped ) break;
                RomHeader header = new RomHeader( getApplicationContext(), file );
                if( header.isValid || header.isNdd ) {
                    cacheFile( file, database, config);
                } else if (mSearchZips && !configHasZip(config, file)) {
                    if (header.isZip) {
                        if (AppData.IS_NOUGAT) {
                            cacheZipFast(database, file, config);
                        } else {
                            cacheZip(database, file, config);
                        }
                    } else if (header.is7Zip && AppData.IS_NOUGAT) {
                        cache7Zip(database, file, config);
                    }
                }

                ++mCurrentProgress;
                updateDialog();
            }

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
      PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
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
            mSearchZips = extras.getBoolean( ActivityHelper.Keys.SEARCH_ZIPS );
            mDownloadArt = extras.getBoolean( ActivityHelper.Keys.DOWNLOAD_ART );
            mClearGallery = extras.getBoolean( ActivityHelper.Keys.CLEAR_GALLERY );
            mSearchSubdirectories = extras.getBoolean( ActivityHelper.Keys.SEARCH_SUBDIR );
            mSearchSingleFile = extras.getBoolean( ActivityHelper.Keys.SEARCH_SINGLE_FILE );
        }

        mbStopped = false;
        mStartId = startId;

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    private void cacheZip(RomDatabase database, Uri file, ConfigFile config)
    {
        Log.i( "CacheRomInfoService", "Found zip file " + file.toString() );
        ZipInputStream zipfile = null;

        try(ParcelFileDescriptor parcelFileDescriptor = getApplicationContext().getContentResolver().openFileDescriptor(file, "r"))
        {
            if (parcelFileDescriptor != null) {
                zipfile = new ZipInputStream( new BufferedInputStream(new FileInputStream(parcelFileDescriptor.getFileDescriptor()) ));

                ZipEntry entry = zipfile.getNextEntry();

                // Assume only one entry per zip file
                if( entry != null && !mbStopped)
                {
                    mCurrentDialogSubText = getShortFileName(new File(entry.getName()).getName());
                    mCurrentDialogMessage = getString(R.string.cacheRomInfo_searchingZip);
                    updateDialog();

                    InputStream zipStream = new BufferedInputStream(zipfile);
                    mCurrentDialogMessage = getString(R.string.cacheRomInfo_extractingZip);
                    updateDialog();

                    cacheZipFileFromInputStream(database, file, config, new File(entry.getName()).getName(), zipStream);
                }
                zipfile.close();
            }
        }
        catch (Exception|OutOfMemoryError e )
        {
            Log.w( "CacheRomInfoService", e );
        }
        finally
        {
            try {
                if( zipfile != null ) {
                    zipfile.close();
                }
            } catch (IOException ignored) {
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void cacheZipFast(RomDatabase database, Uri file, ConfigFile config)
    {
        Log.i( "CacheRomInfoService", "Found zip file " + file.toString() );

        try (ParcelFileDescriptor parcelFileDescriptor = getApplicationContext().getContentResolver().openFileDescriptor(file, "r")) {
            if (parcelFileDescriptor != null) {
                FileInputStream fileInputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());

                ZipFile zipFile = new ZipFile(fileInputStream.getChannel());
                Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();

                // Limit how many times we will look for ROMs in a large zip file
                final int maxChances = 10;
                int currentChance = 0;

                while (entries.hasMoreElements() && !mbStopped && currentChance < maxChances) {

                    ZipArchiveEntry zipEntry = entries.nextElement();

                    InputStream zipStream;

                    mCurrentDialogSubText = getShortFileName(new File(zipEntry.getName()).getName());
                    mCurrentDialogMessage = getString(R.string.cacheRomInfo_searchingZip);
                    updateDialog();

                    zipStream = new BufferedInputStream(zipFile.getInputStream(zipEntry));
                    mCurrentDialogMessage = getString(R.string.cacheRomInfo_extractingZip);
                    updateDialog();

                    if (cacheZipFileFromInputStream(database, file, config, new File(zipEntry.getName()).getName(), zipStream)) {
                        currentChance = 0;
                    } else {
                        ++currentChance;
                    }
                }
                zipFile.close();
                fileInputStream.close();
            }
        } catch (Exception|OutOfMemoryError e) {
            Log.w("CacheRomInfoService", "IOException: " + e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void cache7Zip(RomDatabase database, Uri file, ConfigFile config)
    {
        Log.i( "CacheRomInfoService", "Found 7zip file " + file.toString() );

        try (ParcelFileDescriptor parcelFileDescriptor = getApplicationContext().getContentResolver().openFileDescriptor(file, "r")) {
            if (parcelFileDescriptor != null) {
                FileInputStream fileInputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());

                SevenZFile zipFile = new SevenZFile(fileInputStream.getChannel());
                SevenZArchiveEntry zipEntry;

                // Limit how many times we will look for ROMs in a large zip file
                final int maxChances = 10;
                int currentChance = 0;

                while ((zipEntry = zipFile.getNextEntry()) != null && !mbStopped && currentChance < maxChances) {

                    // Skip entries with null file names
                    if (zipEntry.getName() == null) {
                        continue;
                    }

                    InputStream zipStream;
                    mCurrentDialogSubText = getShortFileName(new File(zipEntry.getName()).getName());
                    mCurrentDialogMessage = getString(R.string.cacheRomInfo_searchingZip);
                    updateDialog();

                    zipStream = new BufferedInputStream(new SevenZInputStream(zipFile));
                    mCurrentDialogMessage = getString(R.string.cacheRomInfo_extractingZip);
                    updateDialog();

                    if (cacheZipFileFromInputStream(database, file, config, new File(zipEntry.getName()).getName(), zipStream)) {
                        currentChance = 0;
                    } else {
                        ++currentChance;
                    }
                }
                zipFile.close();
                fileInputStream.close();

            }
        } catch (Exception|OutOfMemoryError e) {
            Log.w("CacheRomInfoService", "IOException: " + e);
        }
    }

    private boolean cacheZipFileFromInputStream(RomDatabase database, Uri zipFile, ConfigFile config, String name,
                                             InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        //First get the rom header
        inputStream.mark(500);
        byte[] romHeader = FileUtil.extractRomHeader(inputStream);
        RomHeader extractedHeader;
        if(romHeader != null) {
            extractedHeader = new RomHeader( romHeader );

            if(extractedHeader.isValid || extractedHeader.isNdd)
            {
                Log.i( "FileUtil", "Found ROM entry " + name);

                //Then extract the ROM file
                inputStream.reset();
                mCurrentDialogMessage = getString(R.string.cacheRomInfo_computingMD5);
                updateDialog();
                String md5 = FileUtil.computeMd5( inputStream );

                cacheFile(null, name, extractedHeader, md5, database, config, zipFile );

                return true;
            }
        }

        return false;
    }

    private void cacheFile(@Nullable Uri uri, @NonNull String name, RomHeader header, String md5, RomDatabase database, ConfigFile config, Uri zipFileLocation )
    {
        // Only add the file if it doesn't already exist
        if (config.get(md5) == null || mSearchSingleFile) {
            mCurrentDialogMessage = getString(R.string.cacheRomInfo_searchingDB);
            updateDialog();

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
        }
    }

    private void cacheFile(Uri file, RomDatabase database, ConfigFile config )
    {
        String fileName = FileUtil.getFileName(getApplicationContext(), file);
        mCurrentDialogSubText = getShortFileName(fileName);
        updateDialog();

        try (ParcelFileDescriptor parcelFileDescriptor = getApplicationContext().getContentResolver().openFileDescriptor(file, "r")) {

            if (parcelFileDescriptor != null) {
                InputStream bufferedStream = new BufferedInputStream(new FileInputStream(parcelFileDescriptor.getFileDescriptor()));
                String md5 = FileUtil.computeMd5(bufferedStream);
                RomHeader header = new RomHeader(getApplicationContext(), file);

                if (fileName != null) {
                    cacheFile(file, fileName, header, md5, database, config, null);
                }
            }

        } catch (Exception|OutOfMemoryError e) {
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

            // Check if downloaded file is valid
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
        mListener.GetProgressDialog().setOnCancelListener(this::stop);
        updateDialog();

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

        Iterator<String> iter = keys.iterator();
        String key = null;
        while (iter.hasNext() && !found) {
            key = iter.next();
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
    private void cleanupMissingFiles(ConfigFile theConfigFile )
    {
        List<UriPermission> permissions = getContentResolver().getPersistedUriPermissions();
        List<Uri> allFiles = new LinkedList<>();
        for (UriPermission permission : permissions) {
            List<Uri> files = FileUtil.listAllFiles(getApplicationContext(), permission.getUri(), true);
            allFiles.addAll(files);
        }

        Set<String> keys = theConfigFile.keySet();

        Iterator<String> iter = keys.iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            String foundZipPath = theConfigFile.get(key, "zipPathUri");
            String foundRomPath = theConfigFile.get(key, "romPathUri");

            Uri uri = null;

            //Check if this is a zip file first
            if(!TextUtils.isEmpty(foundZipPath))
            {
                uri = Uri.parse(foundZipPath);
            }
            //This was not a zip file, just check the ROM path
            else if(!TextUtils.isEmpty(foundRomPath))
            {
                uri = Uri.parse(foundRomPath);
            }

            if (uri != null) {

                //Remove the entry since it doesn't exist
                boolean removeEntry;
                if (!allFiles.isEmpty()) {
                    removeEntry = !allFiles.contains(uri);
                } else {
                    DocumentFile romFile = FileUtil.getDocumentFileSingle(getApplicationContext(), uri);
                    //Remove the entry since it doesn't exist
                    removeEntry = romFile == null || !romFile.exists();
                }

                if (removeEntry) {
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

        Iterator<String> iter = keys.iterator();
        while (iter.hasNext()) {
            String key = iter.next();
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

            mCurrentProgress = 0;
            mCurrentMaxProgress = keys.size();
            mCurrentDialogMessage = "";
            mCurrentDialogSubText = getString(R.string.cacheRomInfo_downloadingArt);
            updateDialog();

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
                    //Only download art if it's not already present or current art is not a valid image
                    File artPathFile = new File (artPath);
                    if(!artPathFile.exists() || !FileUtil.isFileImage(artPathFile))
                    {
                        boolean downloadArt = true;

                        if (mSearchSingleFile)
                        {
                            String zipUri = theConfigFile.get(key, "zipPathUri");
                            String romUri = theConfigFile.get(key, "romPathUri");
                            try {
                                String decodedPath = URLDecoder.decode(mSearchUri.toString(), "UTF-8");
                                String decodedItemZip = zipUri != null ? URLDecoder.decode(zipUri, "UTF-8") : null;
                                String decodedItemRom = romUri != null ? URLDecoder.decode(romUri, "UTF-8") : null;

                                downloadArt = (decodedItemZip != null && decodedItemZip.equals(decodedPath)) ||
                                        (decodedItemRom != null && decodedItemRom.equals(decodedPath));
                            } catch (UnsupportedEncodingException|java.lang.IllegalArgumentException e) {
                                e.printStackTrace();
                            }
                        }

                        if (downloadArt)
                        {
                            RomDetail detail = database.lookupByMd5WithFallback( key, romGoodName, crc, countryCode );
                            mCurrentDialogText = getShortFileName(romGoodName);
                            updateDialog();
                            Log.i( "CacheRomInfoService", "Start art download: " +  artPath);
                            downloadFile( detail.artUrl, artPath );
                            Log.i( "CacheRomInfoService", "End art download: " +  artPath);
                        }
                    }
                }

                ++mCurrentProgress;
                updateDialog();

                if( mbStopped ) break;
            }
        }

        mCurrentDialogMessage = "";
        mCurrentDialogSubText = "";
        mCurrentDialogText = "";
        updateDialog();
    }

    void updateDialog()
    {
        mListener.GetProgressDialog().setMessage(mCurrentDialogMessage);
        mListener.GetProgressDialog().setSubtext(mCurrentDialogSubText);
        mListener.GetProgressDialog().setText(mCurrentDialogText);
        mListener.GetProgressDialog().setMaxProgress(mCurrentMaxProgress);
        mListener.GetProgressDialog().setProgress(mCurrentProgress);

    }
}
