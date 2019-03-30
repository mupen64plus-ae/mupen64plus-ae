/*
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2012 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with Mupen64PlusAE. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: littleguy77
 */

package paulscode.android.mupen64plusae;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import paulscode.android.mupen64plusae.game.GameActivity;
import paulscode.android.mupen64plusae.input.DiagnosticActivity;
import paulscode.android.mupen64plusae.jni.CoreService;
import paulscode.android.mupen64plusae.persistent.AudioPrefsActivity;
import paulscode.android.mupen64plusae.persistent.DataPrefsActivity;
import paulscode.android.mupen64plusae.persistent.DefaultsPrefsActivity;
import paulscode.android.mupen64plusae.persistent.DisplayPrefsActivity;
import paulscode.android.mupen64plusae.persistent.GamePrefsActivity;
import paulscode.android.mupen64plusae.persistent.InputPrefsActivity;
import paulscode.android.mupen64plusae.persistent.LibraryPrefsActivity;
import paulscode.android.mupen64plusae.persistent.TouchscreenPrefsActivity;
import paulscode.android.mupen64plusae.profile.ControllerProfileActivity;
import paulscode.android.mupen64plusae.profile.ControllerProfileActivityBigScreen;
import paulscode.android.mupen64plusae.profile.EmulationProfileActivity;
import paulscode.android.mupen64plusae.profile.ManageControllerProfilesActivity;
import paulscode.android.mupen64plusae.profile.ManageEmulationProfilesActivity;
import paulscode.android.mupen64plusae.profile.ManageTouchscreenProfilesActivity;
import paulscode.android.mupen64plusae.profile.TouchscreenProfileActivity;
import paulscode.android.mupen64plusae.task.CacheRomInfoService;
import paulscode.android.mupen64plusae.task.DeleteFilesService;
import paulscode.android.mupen64plusae.task.ExtractRomService;
import paulscode.android.mupen64plusae.task.ExtractTexturesService;
import paulscode.android.mupen64plusae.util.LogcatActivity;

import static android.content.Context.ACTIVITY_SERVICE;
import static paulscode.android.mupen64plusae.GalleryActivity.KEY_IS_LEANBACK;

/**
 * Utility class that encapsulates and standardizes interactions between activities.
 */
@SuppressWarnings("SameParameterValue")
public class ActivityHelper
{
    /**
     * Keys used to pass data to activities via the intent extras bundle. It's good practice to
     * namespace the keys to avoid conflicts with other apps. By convention this is usually the
     * package name but it's not a strict requirement. We'll use the fully qualified name of this
     * class since it's easy to get.
     */
    public static class Keys
    {
        private static final String NAMESPACE = Keys.class.getCanonicalName() + ".";
        //@formatter:off
        public static final String ROM_PATH             = NAMESPACE + "ROM_PATH";
        public static final String ZIP_PATH             = NAMESPACE + "ZIP_PATH";
        public static final String EXTRACT_ZIP_PATH     = NAMESPACE + "EXTRACT_ZIP_PATH";
        public static final String ROM_MD5              = NAMESPACE + "ROM_MD5";
        public static final String ROM_CRC              = NAMESPACE + "ROM_CRC";
        public static final String ROM_HEADER_NAME      = NAMESPACE + "ROM_HEADER_NAME";
        public static final String ROM_COUNTRY_CODE     = NAMESPACE + "ROM_COUNTRY_CODE";
        public static final String ROM_GOOD_NAME        = NAMESPACE + "ROM_GOOD_NAME";
        public static final String ROM_DISPLAY_NAME     = NAMESPACE + "ROM_DISPLAY_NAME";
        public static final String ROM_ART_PATH         = NAMESPACE + "ROM_ART_PATH";
        public static final String ROM_LEGACY_SAVE      = NAMESPACE + "ROM_LEGACY_SAVE";
        public static final String DO_RESTART           = NAMESPACE + "DO_RESTART";
        public static final String PROFILE_NAME         = NAMESPACE + "PROFILE_NAME";
        public static final String SEARCH_PATH          = NAMESPACE + "GALLERY_SEARCH_PATH";
        public static final String DATABASE_PATH        = NAMESPACE + "GALLERY_DATABASE_PATH";
        public static final String CONFIG_PATH          = NAMESPACE + "GALLERY_CONFIG_PATH";
        public static final String ART_DIR              = NAMESPACE + "GALLERY_ART_PATH";
        public static final String UNZIP_DIR            = NAMESPACE + "GALLERY_UNZIP_PATH";
        public static final String SEARCH_ZIPS          = NAMESPACE + "GALLERY_SEARCH_ZIP";
        public static final String DOWNLOAD_ART         = NAMESPACE + "GALLERY_DOWNLOAD_ART";
        public static final String CLEAR_GALLERY        = NAMESPACE + "GALLERY_CLEAR_GALLERY";
        public static final String SEARCH_SUBDIR        = NAMESPACE + "GALLERY_SEARCH_SUBDIR";
        public static final String DELETE_PATH          = NAMESPACE + "DELETE_PATH";
        public static final String DELETE_FILTER        = NAMESPACE + "DELETE_FILTER";
        public static final String CHEAT_ARGS           = NAMESPACE + "CHEAT_ARGS";
        public static final String SAVE_TO_LOAD         = NAMESPACE + "SAVE_TO_LOAD";
        public static final String CORE_LIB             = NAMESPACE + "CORE_LIB";
        public static final String HIGH_PRIORITY_THREAD = NAMESPACE + "HIGH_PRIORITY_THREAD";
        public static final String USE_RAPHNET_DEVICES  = NAMESPACE + "USE_RAPHNET_DEVICES";
        public static final String PAK_TYPE_ARRAY       = NAMESPACE + "PAK_TYPE_ARRAY";
        public static final String IS_PLUGGED_ARRAY     = NAMESPACE + "IS_PLUGGED_ARRAY";
        public static final String IS_FPS_LIMIT_ENABLED = NAMESPACE + "IS_FPS_LIMIT_ENABLED";
        public static final String CORE_USER_DATA_DIR   = NAMESPACE + "CORE_USER_DATA_DIR";
        public static final String CORE_USER_CACHE_DIR  = NAMESPACE + "CORE_USER_CACHE_DIR";
        public static final String CORE_USER_CONFIG_DIR = NAMESPACE + "CORE_USER_CONFIG_DIR";
        public static final String USER_SAVE_DIR        = NAMESPACE + "USER_SAVE_DIR";
        public static final String LIBS_DIR             = NAMESPACE + "LIBS_DIR";
        public static final String EXIT_GAME            = NAMESPACE + "EXIT_GAME";
        public static final String FORCE_EXIT_GAME      = NAMESPACE + "FORCE_EXIT_GAME";

        //@formatter:on
    }
    
    static final int SCAN_ROM_REQUEST_CODE = 1;
    static final int GAME_ACTIVITY_CODE = 3;

    static final String coreServiceProcessName = "paulscode.android.mupen64plusae.GameActivity";

    static void launchUri( Context context, int resId )
    {
        launchUri( context, context.getString( resId ) );
    }
    
    public static void launchUri( Context context, String uriString )
    {
        launchUri( context, Uri.parse( uriString ) );
    }
    
    static private void launchUri( Context context, Uri uri )
    {
        try
        {
            context.startActivity( new Intent( Intent.ACTION_VIEW, uri ) );
        }
        catch(java.lang.SecurityException|ActivityNotFoundException e)
        {
            Log.e("ActivityHelper", "Failed to launch link to due exception: " + e.toString());
        }
    }
    
    @SuppressLint( "InlinedApi" )
    public static void launchPlainText( Context context, String text, CharSequence chooserTitle )
    {
        // See http://android-developers.blogspot.com/2012/02/share-with-intents.html
        Intent intent = new Intent( android.content.Intent.ACTION_SEND );
        intent.setType( "text/plain" );

        //Put a limit on this to avoid android.os.TransactionTooLargeException exception
        int limit = 1024*100;
        if(text.length() > limit)
        {
            text = text.substring(text.length()-limit);
        }

        intent.putExtra( Intent.EXTRA_TEXT, text );

        intent = Intent.createChooser( intent, chooserTitle );

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try
        {
            context.startActivity(intent);
        }
        catch (java.lang.RuntimeException e)
        {
            Log.e("ActivityHelper", "Transaction too large");
        }
    }
    
    public static void restartActivity( Activity activity )
    {
        activity.finish();
        activity.startActivity( activity.getIntent() );
    }
    
    public static void startSplashActivity( Context context )
    {
        context.startActivity( new Intent( context, SplashActivity.class ) );
    }
    
    static void startGalleryActivity( Context context, Intent data )
    {
        if (data.getData() != null)
        {
            Intent intent = new Intent( context, GalleryActivity.class );
            if( !TextUtils.isEmpty( data.getData().getPath() ) )
                intent.putExtra( ActivityHelper.Keys.ROM_PATH, data.getData().getPath() );
            context.startActivity( intent );
        }
        else
        {
            Intent intent = new Intent( context, GalleryActivity.class );
            intent.putExtras(data);
            context.startActivity( intent );
            //noinspection ConstantConditions
            intent.replaceExtras((Bundle)null);
        }
    }

    static void startGameActivity(Activity activity, String romPath, String romMd5, String romCrc,
                                  String romHeaderName, byte romCountryCode, String romArtPath, String romGoodName, String romDisplayName,
                                  String romLegacySave, boolean doRestart) {
        Intent intent = new Intent(activity, GameActivity.class);
        intent.putExtra( ActivityHelper.Keys.ROM_PATH, romPath );
        intent.putExtra( ActivityHelper.Keys.ROM_MD5, romMd5 );
        intent.putExtra( ActivityHelper.Keys.ROM_CRC, romCrc );
        intent.putExtra( ActivityHelper.Keys.ROM_HEADER_NAME, romHeaderName );
        intent.putExtra( ActivityHelper.Keys.ROM_COUNTRY_CODE, romCountryCode );
        intent.putExtra( ActivityHelper.Keys.ROM_ART_PATH, romArtPath );
        intent.putExtra( ActivityHelper.Keys.ROM_GOOD_NAME, romGoodName );
        intent.putExtra( ActivityHelper.Keys.ROM_DISPLAY_NAME, romDisplayName );
        intent.putExtra( ActivityHelper.Keys.ROM_LEGACY_SAVE, romLegacySave );
        intent.putExtra( ActivityHelper.Keys.DO_RESTART, doRestart );
        activity.startActivityForResult(intent, GAME_ACTIVITY_CODE);
    }

    public static void startGameActivity( Context context, String romPath, String romMd5, String romCrc,
         String romHeaderName, byte romCountryCode, String romArtPath, String romGoodName, String romDisplayName,
         String romLegacySave, boolean doRestart)
    {
        Intent intent = new Intent( context, GameActivity.class );
        intent.putExtra( ActivityHelper.Keys.ROM_PATH, romPath );
        intent.putExtra( ActivityHelper.Keys.ROM_MD5, romMd5 );
        intent.putExtra( ActivityHelper.Keys.ROM_CRC, romCrc );
        intent.putExtra( ActivityHelper.Keys.ROM_HEADER_NAME, romHeaderName );
        intent.putExtra( ActivityHelper.Keys.ROM_COUNTRY_CODE, romCountryCode );
        intent.putExtra( ActivityHelper.Keys.ROM_ART_PATH, romArtPath );
        intent.putExtra( ActivityHelper.Keys.ROM_GOOD_NAME, romGoodName );
        intent.putExtra( ActivityHelper.Keys.ROM_DISPLAY_NAME, romDisplayName );
        intent.putExtra( ActivityHelper.Keys.ROM_LEGACY_SAVE, romLegacySave );
        intent.putExtra( ActivityHelper.Keys.DO_RESTART, doRestart );
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity( intent );
    }
    
    static void startAudioPrefsActivity( Context context )
    {
        Intent intent = new Intent( context, AudioPrefsActivity.class );
        context.startActivity( intent );    
    }
    
    static void startDataPrefsActivity( Context context )
    {
        Intent intent = new Intent( context, DataPrefsActivity.class );
        context.startActivity( intent );    
    }
    
    static void startDisplayPrefsActivity( Context context )
    {
        Intent intent = new Intent( context, DisplayPrefsActivity.class );
        context.startActivity( intent );    
    }
    
    static void startInputPrefsActivity( Context context )
    {
        Intent intent = new Intent( context, InputPrefsActivity.class );
        context.startActivity( intent );    
    }

    static void startDefaultPrefsActivity( Context context )
    {
        Intent intent = new Intent( context, DefaultsPrefsActivity.class );
        context.startActivity( intent );
    }
    
    static void startLibraryPrefsActivity( Context context )
    {
        Intent intent = new Intent( context, LibraryPrefsActivity.class );
        context.startActivity( intent );    
    }
    
    public static void startTouchscreenPrefsActivity( Context context )
    {
        Intent intent = new Intent( context, TouchscreenPrefsActivity.class );
        context.startActivity( intent );    
    }
    
    static void startGamePrefsActivity( Context context, String romPath, String romMd5,
        String romCrc, String romHeaderName, String romGoodName, String romDisplayName,
        byte romCountryCode, String romLegacySave )
    {
        Intent intent = new Intent( context, GamePrefsActivity.class );
        intent.putExtra( ActivityHelper.Keys.ROM_PATH, romPath );
        intent.putExtra( Keys.ROM_MD5, romMd5 );
        intent.putExtra( Keys.ROM_CRC, romCrc );
        intent.putExtra( Keys.ROM_HEADER_NAME, romHeaderName );
        intent.putExtra( Keys.ROM_GOOD_NAME, romGoodName );
        intent.putExtra( Keys.ROM_DISPLAY_NAME, romDisplayName );
        intent.putExtra( Keys.ROM_LEGACY_SAVE, romLegacySave );
        intent.putExtra( Keys.ROM_COUNTRY_CODE, romCountryCode );
        context.startActivity( intent );
    }
    
    static void startManageEmulationProfilesActivity( Context context )
    {
        context.startActivity( new Intent( context, ManageEmulationProfilesActivity.class ) );
    }
    
    static void startManageTouchscreenProfilesActivity( Context context )
    {
        context.startActivity( new Intent( context, ManageTouchscreenProfilesActivity.class ) );
    }
    
    static void startManageControllerProfilesActivity( Context context )
    {
        context.startActivity( new Intent( context, ManageControllerProfilesActivity.class ) );
    }
    
    public static void startEmulationProfileActivity( Context context, String profileName )
    {
        Intent intent = new Intent( context, EmulationProfileActivity.class );
        intent.putExtra( Keys.PROFILE_NAME, profileName );
        context.startActivity( intent );
    }
    
    public static void startTouchscreenProfileActivity( Context context, String profileName )
    {
        Intent intent = new Intent( context, TouchscreenProfileActivity.class );
        intent.putExtra( Keys.PROFILE_NAME, profileName );
        context.startActivity( intent );
    }
    
    public static void startControllerProfileActivity( Context context, String profileName )
    {
        Intent intent = new Intent( context, ControllerProfileActivity.class );
        intent.putExtra( Keys.PROFILE_NAME, profileName );
        context.startActivity( intent );
    }
    
    public static void startControllerProfileActivityBigScreen( Context context, String profileName )
    {
        Intent intent = new Intent( context, ControllerProfileActivityBigScreen.class );
        intent.putExtra( Keys.PROFILE_NAME, profileName );
        context.startActivity( intent );
    }
    
    static void startDiagnosticActivity( Context context )
    {
        context.startActivity( new Intent( context, DiagnosticActivity.class ) );
    }

    static void startLogcatActivity( Context context )
    {
        context.startActivity( new Intent( context, LogcatActivity.class) );
    }
    
    static void startCacheRomInfoService(Context context, ServiceConnection serviceConnection,
        String searchPath, String databasePath, String configPath, String artDir, String unzipDir,
        boolean searchZips, boolean downloadArt, boolean clearGallery, boolean searchSubdirectories)
    {
        Intent intent = new Intent(context, CacheRomInfoService.class);
        intent.putExtra(Keys.SEARCH_PATH, searchPath);
        intent.putExtra(Keys.DATABASE_PATH, databasePath);
        intent.putExtra(Keys.CONFIG_PATH, configPath);
        intent.putExtra(Keys.ART_DIR, artDir);
        intent.putExtra(Keys.UNZIP_DIR, unzipDir);
        intent.putExtra(Keys.SEARCH_ZIPS, searchZips);
        intent.putExtra(Keys.DOWNLOAD_ART, downloadArt);
        intent.putExtra(Keys.CLEAR_GALLERY, clearGallery);
        intent.putExtra(Keys.SEARCH_SUBDIR, searchSubdirectories);

        context.startService(intent);
        context.bindService(intent, serviceConnection, 0);
    }

    static void stopCacheRomInfoService(Context context, ServiceConnection serviceConnection)
    {
        Intent intent = new Intent(context, CacheRomInfoService.class);
        
        context.unbindService(serviceConnection);
        context.stopService(intent);
    }

    static void startRomScanActivity(Activity activity)
    {
        Intent intent = new Intent(activity, ScanRomsActivity.class);
        activity.startActivityForResult( intent, SCAN_ROM_REQUEST_CODE );
    }

    static void startExtractTexturesService(Context context, ServiceConnection serviceConnection,
        String searchPath)
    {
        Intent intent = new Intent(context, ExtractTexturesService.class);
        intent.putExtra(Keys.SEARCH_PATH, searchPath);

        context.startService(intent);
        context.bindService(intent, serviceConnection, 0);
    }

    static void stopExtractTexturesService(Context context, ServiceConnection serviceConnection)
    {
        Intent intent = new Intent(context, ExtractTexturesService.class);

        context.unbindService(serviceConnection);
        context.stopService(intent);
    }

    static void startDeleteFilesService(Context context, ServiceConnection serviceConnection,
                                            ArrayList<String> deletePath, ArrayList<String> deleteFilter)
    {
        Intent intent = new Intent(context, DeleteFilesService.class);
        intent.putStringArrayListExtra(Keys.DELETE_PATH, deletePath);
        intent.putStringArrayListExtra(Keys.DELETE_FILTER, deleteFilter);

        context.startService(intent);
        context.bindService(intent, serviceConnection, 0);
    }

    static void stopDeleteFilesService(Context context, ServiceConnection serviceConnection)
    {
        Intent intent = new Intent(context, DeleteFilesService.class);

        context.unbindService(serviceConnection);
        context.stopService(intent);
    }
    
    static void starExtractTextureActivity(Activity activity)
    {
        Intent intent = new Intent(activity, ExtractTexturesActivity.class);
        activity.startActivity( intent );
    }

    static void startDeleteTextureActivity(Activity activity)
    {
        Intent intent = new Intent(activity, DeleteTexturesActivity.class);
        activity.startActivity( intent );
    }

    public static void startCoreService(Context context, ServiceConnection serviceConnection, String romGoodName, String romDisplayName,
        String romPath, String romMd5, String romCrc, String romHeaderName, byte romCountryCode, String romArtPath,
        String romLegacySave, String cheatOptions, boolean isRestarting, String saveToLoad, String coreLib,
        boolean useHighPriorityThread, ArrayList<Integer> pakTypes, boolean[] isPlugged, boolean isFrameLimiterEnabled,
        String coreUserDataDir, String coreUserCacheDir, String coreUserConfigDir, String userSaveDir, String libsDir,
        boolean useRaphnetDevicesIfAvailable)
    {
        Intent intent = new Intent(context, CoreService.class);
        intent.putExtra(Keys.ROM_GOOD_NAME, romGoodName);
        intent.putExtra(Keys.ROM_DISPLAY_NAME, romDisplayName);
        intent.putExtra(Keys.ROM_PATH, romPath);
        intent.putExtra(Keys.CHEAT_ARGS, cheatOptions);
        intent.putExtra(Keys.DO_RESTART, isRestarting);
        intent.putExtra(Keys.SAVE_TO_LOAD, saveToLoad);
        intent.putExtra(Keys.CORE_LIB, coreLib);
        intent.putExtra(Keys.HIGH_PRIORITY_THREAD, useHighPriorityThread);
        intent.putExtra(Keys.USE_RAPHNET_DEVICES, useRaphnetDevicesIfAvailable);
        intent.putIntegerArrayListExtra(Keys.PAK_TYPE_ARRAY, pakTypes);
        intent.putExtra(Keys.IS_PLUGGED_ARRAY, isPlugged);
        intent.putExtra(Keys.IS_FPS_LIMIT_ENABLED, isFrameLimiterEnabled);
        intent.putExtra(Keys.CORE_USER_DATA_DIR, coreUserDataDir);
        intent.putExtra(Keys.CORE_USER_CACHE_DIR, coreUserCacheDir);
        intent.putExtra(Keys.CORE_USER_CONFIG_DIR, coreUserConfigDir);
        intent.putExtra(Keys.USER_SAVE_DIR, userSaveDir);
        intent.putExtra(Keys.LIBS_DIR, libsDir);

        intent.putExtra(Keys.ROM_MD5, romMd5);
        intent.putExtra(Keys.ROM_CRC, romCrc);
        intent.putExtra(Keys.ROM_HEADER_NAME, romHeaderName);
        intent.putExtra(Keys.ROM_COUNTRY_CODE, romCountryCode);
        intent.putExtra(Keys.ROM_ART_PATH, romArtPath);
        intent.putExtra(Keys.ROM_LEGACY_SAVE, romLegacySave);

        context.startService(intent);
        context.bindService(intent, serviceConnection, 0);
    }

    public static void stopCoreService(Context context, ServiceConnection serviceConnection)
    {
        Intent intent = new Intent(context, CoreService.class);
        context.unbindService(serviceConnection);
        context.stopService(intent);
    }

    static boolean isServiceRunning(Context context, String processName) {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);

        List<ActivityManager.RunningAppProcessInfo> processInfos = null;
        if (manager != null) {
            processInfos = manager.getRunningAppProcesses();
        }

        if (processInfos != null) {
            for (ActivityManager.RunningAppProcessInfo process : processInfos){
                if(processName.equals(process.processName)) {
                    return true;
                }
            }
        }

        return false;
    }

    static void startExtractRomService(Context context, ServiceConnection serviceConnection,
       String zipPath, String extractRomPath, String romPath, String romMd5)
    {
        Intent intent = new Intent(context, ExtractRomService.class);
        intent.putExtra(Keys.ZIP_PATH, zipPath);
        intent.putExtra(Keys.EXTRACT_ZIP_PATH, extractRomPath);
        intent.putExtra(Keys.ROM_PATH, romPath);
        intent.putExtra(Keys.ROM_MD5, romMd5);

        context.startService(intent);
        context.bindService(intent, serviceConnection, 0);
    }

    static void stopExtractRomService(Context context, ServiceConnection serviceConnection)
    {
        Intent intent = new Intent(context, ExtractRomService.class);

        context.unbindService(serviceConnection);
        context.stopService(intent);
    }

}
