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
 * 
 * References:
 * http://stackoverflow.com/questions/4447477/android-how-to-copy-files-in-assets-to-sdcard
 */
package paulscode.android.mupen64plusae.task;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import androidx.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.util.FileUtil;

@SuppressWarnings({"unused", "BooleanMethodIsAlwaysInverted", "RedundantSuppression"})
public class ExtractAssetsOrCleanupTask
{

    private static final HashMap<String, Integer> mAssetVersions = new HashMap<>();

    private static final String TAG = "ExtractAssetsOrCleanup";

    public interface ExtractAssetsListener
    {
        void onExtractAssetsProgress( String nextFileToExtract, int currentAsset, int totalAssets );
        void onExtractAssetsFinished( List<Failure> failures );
    }

    static {
        synchronized (ExtractAssetsOrCleanupTask.class) {
            mAssetVersions.put("mupen64plus_data/Glide64mk2.ini", 2);
            mAssetVersions.put("mupen64plus_data/RiceVideoLinux.ini", 1);
            mAssetVersions.put("mupen64plus_data/font.ttf", 1);
            mAssetVersions.put("mupen64plus_data/gln64.conf", 1);
            mAssetVersions.put("mupen64plus_data/gln64rom.conf", 1);
            mAssetVersions.put("mupen64plus_data/mupen64plus.ini", 9);
            mAssetVersions.put("mupen64plus_data/mupencheat.default", 1);
        }
    }

    private final AssetManager mAssetManager;
    private final String mSrcPath;
    private final String mDstPath;
    private final ExtractAssetsListener mListener;
    private final SharedPreferences mPreferences;
    private int mCurrentAsset = 0;
    private int mTotalAssets = 0;
    private final AppData mAppData;
    private final GlobalPrefs mGlobalPrefs;

    public ExtractAssetsOrCleanupTask(Context context, AssetManager assetManager, AppData appData, GlobalPrefs globalPrefs, String srcPath, String dstPath, ExtractAssetsListener listener )
    {
        if (assetManager == null )
            throw new IllegalArgumentException( "Asset manager cannot be null" );
        if( TextUtils.isEmpty( srcPath ) )
            throw new IllegalArgumentException( "Source path cannot be null or empty" );
        if( TextUtils.isEmpty( dstPath ) )
            throw new IllegalArgumentException( "Destination path cannot be null or empty" );
        if( listener == null )
            throw new IllegalArgumentException( "Listener cannot be null" );
        
        mAssetManager = assetManager;
        mSrcPath = srcPath;
        mDstPath = dstPath;
        mListener = listener;
        // Preference object for persisting app data
        mPreferences = PreferenceManager.getDefaultSharedPreferences( context );
        mAppData = appData;
        mGlobalPrefs = globalPrefs;
    }

    private boolean createBackupAndMove(String srcPath, String dstPath)
    {
        if (!new File(srcPath).exists()) {
            return true;
        }
        
        boolean backupSuccess = FileUtil.copyFile(new File(srcPath), new File(srcPath + ".bak"), false);
        boolean copySuccess = FileUtil.copyFile(new File(srcPath), new File(dstPath), true);

        return backupSuccess && copySuccess;
    }

    public void doInBackground()
    {
        Thread refreshThread = new Thread(() -> {
            List<Failure> failures = startExtractionAndCleanup();
            mListener.onExtractAssetsFinished( failures );
        });
        refreshThread.setDaemon(true);
        refreshThread.start();
    }
    
    protected List<Failure> startExtractionAndCleanup()
    {
        final List<Failure> failures = new ArrayList<>();
        
        mTotalAssets += 5;
        FileUtil.deleteExtensionFolder(new File(mGlobalPrefs.shaderCacheDir), "shaders");
        FileUtil.deleteFolder(new File(mGlobalPrefs.legacyCoreConfigDir));

        // Don't move data from the legacy folders if a user selects to use external storage. This is because a use could select
        // to use external sorage and use the same folder as the legacy data folder.
        if (!mGlobalPrefs.useExternalStorge) {
            //Move data to the new location
            mListener.onExtractAssetsProgress( "Moving: " + mGlobalPrefs.legacyRomInfoCacheCfg, mCurrentAsset, mTotalAssets);
            ++mCurrentAsset;
            if (!createBackupAndMove(mGlobalPrefs.legacyRomInfoCacheCfg, mGlobalPrefs.romInfoCacheCfg)) {
                Failure failure = new Failure( mGlobalPrefs.legacyRomInfoCacheCfg, mGlobalPrefs.romInfoCacheCfg, Failure.Reason.FILE_IO_EXCEPTION );
                Log.e( TAG, failure.toString() );
                failures.add( failure );
            }

            mListener.onExtractAssetsProgress( "Moving: " + mGlobalPrefs.legacyCoverArtDir, mCurrentAsset, mTotalAssets);
            ++mCurrentAsset;
            if (!createBackupAndMove(mGlobalPrefs.legacyCoverArtDir, mGlobalPrefs.coverArtDir)) {
                Failure failure = new Failure( mGlobalPrefs.legacyCoverArtDir, mGlobalPrefs.coverArtDir, Failure.Reason.FILE_IO_EXCEPTION );
                Log.e( TAG, failure.toString() );
                failures.add( failure );
            }

            mListener.onExtractAssetsProgress( "Moving: " + mGlobalPrefs.legacyProfilesDir, mCurrentAsset, mTotalAssets);
            ++mCurrentAsset;
            if (!createBackupAndMove(mGlobalPrefs.legacyProfilesDir, mGlobalPrefs.profilesDir)) {
                Failure failure = new Failure( mGlobalPrefs.legacyProfilesDir, mGlobalPrefs.profilesDir, Failure.Reason.FILE_IO_EXCEPTION );
                Log.e( TAG, failure.toString() );
                failures.add( failure );
            }

            mListener.onExtractAssetsProgress( "Moving: " + mGlobalPrefs.legacyTouchscreenCustomSkinsDir, mCurrentAsset, mTotalAssets);
            ++mCurrentAsset;
            if (!createBackupAndMove(mGlobalPrefs.legacyTouchscreenCustomSkinsDir, mGlobalPrefs.touchscreenCustomSkinsDir)) {
                Failure failure = new Failure( mGlobalPrefs.legacyTouchscreenCustomSkinsDir, mGlobalPrefs.touchscreenCustomSkinsDir, Failure.Reason.FILE_IO_EXCEPTION );
                Log.e( TAG, failure.toString() );
                failures.add( failure );
            }

            mListener.onExtractAssetsProgress( "Moving: " + mAppData.legacyGameDataDir, mCurrentAsset, mTotalAssets);
            ++mCurrentAsset;
            if (!createBackupAndMove(mAppData.legacyGameDataDir, mAppData.gameDataDir)) {
                Failure failure = new Failure( mAppData.legacyGameDataDir, mAppData.gameDataDir, Failure.Reason.FILE_IO_EXCEPTION );
                Log.e( TAG, failure.toString() );
                failures.add( failure );
            }
        }

        return extractAssets(failures, mSrcPath, mDstPath );
    }

    public static final class Failure
    {
        public enum Reason
        {
            FILE_UNWRITABLE,
            FILE_UNCLOSABLE,
            ASSET_UNCLOSABLE,
            ASSET_IO_EXCEPTION,
            FILE_IO_EXCEPTION,
        }
        
        final String srcPath;
        final String dstPath;
        public final Reason reason;
        Failure( String srcPath, String dstPath, Reason reason )
        {
            this.srcPath = srcPath;
            this.dstPath = dstPath;
            this.reason = reason;
        }
        
        @Override
        public String toString()
        {
            switch( reason )
            {
                case FILE_UNWRITABLE:
                    return "Failed to open file " + dstPath;
                case FILE_UNCLOSABLE:
                    return "Failed to close file " + dstPath;
                case ASSET_UNCLOSABLE:
                    return "Failed to close asset " + srcPath;
                case ASSET_IO_EXCEPTION:
                    return "Failed to extract asset " + srcPath + " to file " + dstPath;
                case FILE_IO_EXCEPTION:
                    return "Failed to add file " + srcPath + " to file " + dstPath;
                default:
                    return "Failed using source " + srcPath + " and destination " + dstPath;
            }
        }
    }

    public static boolean areAllAssetsPresent(String srcPath, String dstPath) {

        for (Map.Entry<String, Integer> entry : mAssetVersions.entrySet()) {
            String destinationPath = dstPath + "/" + entry.getKey().replaceAll(srcPath + "/", "");

            if(!(new File(destinationPath).exists())) {
                return false;
            }
        }

        return true;
    }

    static public boolean areAllAssetsValid(SharedPreferences preferences, String srcPath, String dstPath) {

        for (Map.Entry<String, Integer> entry : mAssetVersions.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();
            String destinationPath = dstPath + "/" + entry.getKey().replaceAll(srcPath + "/", "");

            if(preferences.getInt( key, 0 ) != value || !(new File(destinationPath).exists())) {
                return false;
            }
        }

        return true;
    }
    
    private List<Failure> extractAssets( final List<Failure> failures, String srcPath, String dstPath )
    {
        if( srcPath.startsWith( "/" ) )
            srcPath = srcPath.substring( 1 );

        ArrayList<Map.Entry<String, Integer>> assetsToExtract = new ArrayList<>();

        // Ensure the parent directories exist
        FileUtil.makeDirs(dstPath);

        mTotalAssets = 0;
        mCurrentAsset = 0;
        for (Map.Entry<String, Integer> entry : mAssetVersions.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();
            String destinationPath = dstPath + "/" + entry.getKey().replaceAll(srcPath + "/", "");

            if(getInt(key) != value || !(new File(destinationPath).exists())) {
                assetsToExtract.add(entry);
                ++mTotalAssets;
            }
        }

        for(Map.Entry<String, Integer> asset : assetsToExtract) {

            String destinationPath = dstPath + "/" + asset.getKey().replaceAll(srcPath + "/", "");

            // Ensure the parent directories exist
            File directory = new File(destinationPath).getParentFile();

            if(directory != null && !directory.exists()) {
                if(!directory.mkdirs()) {
                    Log.e( TAG, "Unable to create folder" );
                }
            }

            failures.addAll(extractSingleFile(asset.getKey(), destinationPath, asset.getValue()));
            ++mCurrentAsset;
        }

        return failures;
    }

    private List<Failure>  extractSingleFile( String asset, String destination, Integer newVersion)
    {
        final List<Failure> failures = new ArrayList<>();

        // Call the progress listener before extracting
        mListener.onExtractAssetsProgress( "Extracting: " + destination, mCurrentAsset, mTotalAssets);

        // IO objects, initialize null to eliminate lint error
        OutputStream out = null;
        InputStream in = null;

        // Extract the file
        try
        {
            out = new FileOutputStream( destination );
            in = mAssetManager.open( asset );
            byte[] buffer = new byte[1024];
            int read;

            while( ( read = in.read( buffer ) ) != -1 )
            {
                out.write( buffer, 0, read );
            }
            out.flush();

            putInt(asset, newVersion);
        }
        catch( FileNotFoundException e )
        {
            Failure failure = new Failure( asset, destination, Failure.Reason.FILE_UNWRITABLE );
            Log.e( TAG, failure.toString() );
            failures.add( failure );
        }
        catch( IOException e )
        {
            Failure failure = new Failure( asset, destination, Failure.Reason.ASSET_IO_EXCEPTION );
            Log.e( TAG, failure.toString() );
            failures.add( failure );
        }
        finally
        {
            if( out != null )
            {
                try
                {
                    out.close();
                }
                catch( IOException e )
                {
                    Failure failure = new Failure( asset, destination, Failure.Reason.FILE_UNCLOSABLE );
                    Log.e( TAG, failure.toString() );
                    failures.add( failure );
                }
            }
            if( in != null )
            {
                try
                {
                    in.close();
                }
                catch( IOException e )
                {
                    Failure failure = new Failure( asset, destination, Failure.Reason.ASSET_UNCLOSABLE );
                    Log.e( TAG, failure.toString() );
                    failures.add( failure );
                }
            }
        }

        return failures;
    }

    private void putInt( String key, int value )
    {
        mPreferences.edit().putInt( key, value ).apply();
    }

    private int getInt( String key )
    {
        return mPreferences.getInt( key, 0 );
    }
    
    private static String[] getAssetList( AssetManager assetManager, String srcPath )
    {
        String[] srcSubPaths = null;
        
        try
        {
            srcSubPaths = assetManager.list( srcPath );
        }
        catch( IOException e )
        {
            Log.w( TAG, "Failed to get asset file list." );
        }
        
        return srcSubPaths;
    }
}
