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
import android.util.Log;

import java.io.File;
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
import paulscode.android.mupen64plusae.persistent.NetplayPrefsActivity;
import paulscode.android.mupen64plusae.persistent.ShaderPrefsActivity;
import paulscode.android.mupen64plusae.persistent.TouchscreenPrefsActivity;
import paulscode.android.mupen64plusae.profile.ManageControllerProfilesActivity;
import paulscode.android.mupen64plusae.profile.ManageEmulationProfilesActivity;
import paulscode.android.mupen64plusae.profile.ManageTouchscreenProfilesActivity;
import paulscode.android.mupen64plusae.task.CacheRomInfoService;
import paulscode.android.mupen64plusae.task.CopyFromSdService;
import paulscode.android.mupen64plusae.task.CopyToSdService;
import paulscode.android.mupen64plusae.task.DeleteFilesService;
import paulscode.android.mupen64plusae.task.ExtractTexturesService;
import paulscode.android.mupen64plusae.util.LogcatActivity;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * Utility class that encapsulates and standardizes interactions between activities.
 */
@SuppressWarnings({"SameParameterValue", "WeakerAccess"})
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
        public static final String ROM_MD5              = NAMESPACE + "ROM_MD5";
        public static final String ROM_CRC              = NAMESPACE + "ROM_CRC";
        public static final String ROM_HEADER_NAME      = NAMESPACE + "ROM_HEADER_NAME";
        public static final String ROM_COUNTRY_CODE     = NAMESPACE + "ROM_COUNTRY_CODE";
        public static final String ROM_GOOD_NAME        = NAMESPACE + "ROM_GOOD_NAME";
        public static final String ROM_DISPLAY_NAME     = NAMESPACE + "ROM_DISPLAY_NAME";
        public static final String ROM_ART_PATH         = NAMESPACE + "ROM_ART_PATH";
        public static final String DO_RESTART           = NAMESPACE + "DO_RESTART";
        public static final String PROFILE_NAME         = NAMESPACE + "PROFILE_NAME";
        public static final String FILE_PATH            = NAMESPACE + "FILE_PATH";
        public static final String FILE_URI             = NAMESPACE + "FILE_URI";
        public static final String SEARCH_PATH          = NAMESPACE + "GALLERY_SEARCH_PATH";
        public static final String SEARCH_SINGLE_FILE   = NAMESPACE + "SEARCH_SINGLE_FILE";
        public static final String CAN_SELECT_FILE      = NAMESPACE + "CAN_SELECT_FILE";
        public static final String CAN_VIEW_EXT_STORAGE = NAMESPACE + "CAN_VIEW_EXT_STORAGE";
        public static final String DATABASE_PATH        = NAMESPACE + "GALLERY_DATABASE_PATH";
        public static final String CONFIG_PATH          = NAMESPACE + "GALLERY_CONFIG_PATH";
        public static final String ART_DIR              = NAMESPACE + "GALLERY_ART_PATH";
        public static final String SEARCH_ZIPS          = NAMESPACE + "GALLERY_SEARCH_ZIP";
        public static final String DOWNLOAD_ART         = NAMESPACE + "GALLERY_DOWNLOAD_ART";
        public static final String CLEAR_GALLERY        = NAMESPACE + "GALLERY_CLEAR_GALLERY";
        public static final String SEARCH_SUBDIR        = NAMESPACE + "GALLERY_SEARCH_SUBDIR";
        public static final String DELETE_PATH          = NAMESPACE + "DELETE_PATH";
        public static final String DELETE_FILTER        = NAMESPACE + "DELETE_FILTER";
        public static final String USE_RAPHNET_DEVICES  = NAMESPACE + "USE_RAPHNET_DEVICES";
        public static final String EXIT_GAME            = NAMESPACE + "EXIT_GAME";
        public static final String FORCE_EXIT_GAME      = NAMESPACE + "FORCE_EXIT_GAME";
        public static final String VIDEO_RENDER_WIDTH   = NAMESPACE + "VIDEO_RENDER_WIDTH";
        public static final String VIDEO_RENDER_HEIGHT  = NAMESPACE + "VIDEO_RENDER_HEIGHT";
        public static final String NETPLAY_ENABLED      = NAMESPACE + "NETPLAY_ENABLED";
        public static final String NETPLAY_SERVER       = NAMESPACE + "NETPLAY_SERVER";


        //@formatter:on
    }

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
            Log.e("ActivityHelper", "Failed to launch link to due exception: " + e);
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
            intent.putExtra( Keys.ROM_PATH, data.getData().toString() );
            context.startActivity( intent );
        }
        else
        {
            Intent intent = new Intent( context, GalleryActivity.class );
            intent.putExtras(data);
            context.startActivity( intent );
            intent.replaceExtras((Bundle)null);
        }
    }

    public static void startGameActivity( Context context, String romPath, String zipPath, String romMd5, String romCrc,
         String romHeaderName, byte romCountryCode, String romArtPath, String romGoodName, String romDisplayName,
         boolean doRestart)
    {
        Intent intent = new Intent( context, GameActivity.class );
        intent.putExtra( Keys.ROM_PATH, romPath );
        intent.putExtra( Keys.ZIP_PATH, zipPath );
        intent.putExtra( Keys.ROM_MD5, romMd5 );
        intent.putExtra( Keys.ROM_CRC, romCrc );
        intent.putExtra( Keys.ROM_HEADER_NAME, romHeaderName );
        intent.putExtra( Keys.ROM_COUNTRY_CODE, romCountryCode );
        intent.putExtra( Keys.ROM_ART_PATH, romArtPath );
        intent.putExtra( Keys.ROM_GOOD_NAME, romGoodName );
        intent.putExtra( Keys.ROM_DISPLAY_NAME, romDisplayName );
        intent.putExtra( Keys.DO_RESTART, doRestart );
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra( Keys.NETPLAY_ENABLED, false );
        intent.putExtra( Keys.NETPLAY_SERVER, false );
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

    static void startNetplayPrefsActivity( Context context )
    {
        Intent intent = new Intent( context, NetplayPrefsActivity.class );
        context.startActivity( intent );
    }
    
    static void startDisplayPrefsActivity( Context context )
    {
        Intent intent = new Intent( context, DisplayPrefsActivity.class );
        context.startActivity( intent );    
    }

    static void startShadersPrefsActivity( Context context )
    {
        Intent intent = new Intent( context, ShaderPrefsActivity.class );
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
        byte romCountryCode)
    {
        Intent intent = new Intent( context, GamePrefsActivity.class );
        intent.putExtra( Keys.ROM_PATH, romPath );
        intent.putExtra( Keys.ROM_MD5, romMd5 );
        intent.putExtra( Keys.ROM_CRC, romCrc );
        intent.putExtra( Keys.ROM_HEADER_NAME, romHeaderName );
        intent.putExtra( Keys.ROM_GOOD_NAME, romGoodName );
        intent.putExtra( Keys.ROM_DISPLAY_NAME, romDisplayName );
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

    public static void startImportExportActivity( Context context )
    {
        Intent intent = new Intent( context, ImportExportActivity.class );
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
        String searchUri, String databasePath, String configPath, String artDir,
        boolean searchZips, boolean downloadArt, boolean clearGallery, boolean searchSubdirectories,
        boolean singleFile)
    {
        Intent intent = new Intent(context, CacheRomInfoService.class);
        intent.putExtra(Keys.SEARCH_PATH, searchUri);
        intent.putExtra(Keys.DATABASE_PATH, databasePath);
        intent.putExtra(Keys.CONFIG_PATH, configPath);
        intent.putExtra(Keys.ART_DIR, artDir);
        intent.putExtra(Keys.SEARCH_ZIPS, searchZips);
        intent.putExtra(Keys.DOWNLOAD_ART, downloadArt);
        intent.putExtra(Keys.CLEAR_GALLERY, clearGallery);
        intent.putExtra(Keys.SEARCH_SUBDIR, searchSubdirectories);
        intent.putExtra(Keys.SEARCH_SINGLE_FILE, singleFile);

        context.startService(intent);
        context.bindService(intent, serviceConnection, 0);
    }

    static void startExtractTexturesService(Context context, ServiceConnection serviceConnection,
        Uri fileUri)
    {
        Intent intent = new Intent(context, ExtractTexturesService.class);
        intent.putExtra(Keys.FILE_URI, fileUri.toString());

        context.startService(intent);
        context.bindService(intent, serviceConnection, 0);
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

    static void startCopyToSdService(Context context, ServiceConnection serviceConnection,
                                        File source, Uri destination)
    {
        Intent intent = new Intent(context, CopyToSdService.class);
        intent.putExtra(Keys.FILE_PATH, source.getAbsolutePath());
        intent.putExtra(Keys.FILE_URI, destination.toString());

        context.startService(intent);
        context.bindService(intent, serviceConnection, 0);
    }

    static void startCopyFromSdService(Context context, ServiceConnection serviceConnection,
                                     Uri source, File destination)
    {
        Intent intent = new Intent(context, CopyFromSdService.class);
        intent.putExtra(Keys.FILE_URI, source.toString());
        intent.putExtra(Keys.FILE_PATH, destination.getAbsolutePath());

        context.startService(intent);
        context.bindService(intent, serviceConnection, 0);
    }

    static void stopCopyFromSdService(Context context, ServiceConnection serviceConnection)
    {
        Intent intent = new Intent(context, CopyFromSdService.class);

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

    public static void startCoreService(Context context, ServiceConnection serviceConnection, StartCoreServiceParams params)
    {
        Intent intent = new Intent(context, CoreService.class);
        intent.putExtra(Keys.ROM_GOOD_NAME, params.getRomGoodName());
        intent.putExtra(Keys.ROM_DISPLAY_NAME,  params.getRomDisplayName());
        intent.putExtra(Keys.ROM_PATH,  params.getRomPath());
        intent.putExtra(Keys.ZIP_PATH,  params.getZipPath());
        intent.putExtra(Keys.DO_RESTART,  params.isRestarting());
        intent.putExtra(Keys.USE_RAPHNET_DEVICES, params.isUseRaphnetDevicesIfAvailable());

        intent.putExtra(Keys.ROM_MD5, params.getRomMd5());
        intent.putExtra(Keys.ROM_CRC, params.getRomCrc());
        intent.putExtra(Keys.ROM_HEADER_NAME, params.getRomHeaderName());
        intent.putExtra(Keys.ROM_COUNTRY_CODE, params.getRomCountryCode());
        intent.putExtra(Keys.ROM_ART_PATH, params.getRomArtPath());
        intent.putExtra(Keys.VIDEO_RENDER_WIDTH, params.getVideoRenderWidth());
        intent.putExtra(Keys.VIDEO_RENDER_HEIGHT, params.getVideoRenderHeight());
        intent.putExtra(Keys.NETPLAY_ENABLED, params.isUsingNetplay());

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
}
