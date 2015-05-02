/**
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

import paulscode.android.mupen64plusae.game.GameActivity;
import paulscode.android.mupen64plusae.game.GameActivityXperiaPlay;
import paulscode.android.mupen64plusae.input.DiagnosticActivity;
import paulscode.android.mupen64plusae.persistent.GamePrefsActivity;
import paulscode.android.mupen64plusae.persistent.GlobalPrefsActivity;
import paulscode.android.mupen64plusae.profile.ControllerProfileActivity;
import paulscode.android.mupen64plusae.profile.EmulationProfileActivity;
import paulscode.android.mupen64plusae.profile.ManageControllerProfilesActivity;
import paulscode.android.mupen64plusae.profile.ManageEmulationProfilesActivity;
import paulscode.android.mupen64plusae.profile.ManageTouchscreenProfilesActivity;
import paulscode.android.mupen64plusae.profile.TouchscreenProfileActivity;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

/**
 * Utility class that encapsulates and standardizes interactions between activities.
 */
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
        public static final String ROM_MD5              = NAMESPACE + "ROM_MD5";
        public static final String CHEAT_ARGS           = NAMESPACE + "CHEAT_ARGS";
        public static final String DO_RESTART           = NAMESPACE + "DO_RESTART";
        public static final String PROFILE_NAME         = NAMESPACE + "PROFILE_NAME";
        public static final String MENU_DISPLAY_MODE    = NAMESPACE + "MENU_DISPLAY_MODE";
        //@formatter:on
    }

    public static void launchUri( Context context, int resId )
    {
        launchUri( context, context.getString( resId ) );
    }
    
    public static void launchUri( Context context, String uriString )
    {
        launchUri( context, Uri.parse( uriString ) );
    }
    
    public static void launchUri( Context context, Uri uri )
    {
        context.startActivity( new Intent( Intent.ACTION_VIEW, uri ) );
    }
    
    @SuppressLint( "InlinedApi" )
    public static void launchPlainText( Context context, String text, CharSequence chooserTitle )
    {
        // See http://android-developers.blogspot.com/2012/02/share-with-intents.html
        Intent intent = new Intent( android.content.Intent.ACTION_SEND );
        intent.setType( "text/plain" );
        intent.addFlags( Intent.FLAG_ACTIVITY_NEW_DOCUMENT );
        intent.putExtra( Intent.EXTRA_TEXT, text );
        // intent.putExtra( Intent.EXTRA_SUBJECT, subject );
        // intent.putExtra( Intent.EXTRA_EMAIL, new String[] { emailTo } );
        context.startActivity( Intent.createChooser( intent, chooserTitle ) );
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
    
    public static void startGalleryActivity( Context context, Uri romPath )
    {
        startGalleryActivity( context, romPath == null ? null : romPath.getPath() );
    }
    
    public static void startGalleryActivity( Context context, String romPath )
    {
        Intent intent = new Intent( context, GalleryActivity.class );
        if( !TextUtils.isEmpty( romPath ) )
            intent.putExtra( ActivityHelper.Keys.ROM_PATH, romPath );
        context.startActivity( intent );
    }
    
    public static void startGameActivity( Context context, String romPath, String romMd5,
            String cheatArgs, boolean doRestart, boolean isXperiaPlay )
    {
        Intent intent = isXperiaPlay
                ? new Intent( context, GameActivityXperiaPlay.class )
                : new Intent( context, GameActivity.class );
        intent.putExtra( ActivityHelper.Keys.ROM_PATH, romPath );
        intent.putExtra( ActivityHelper.Keys.ROM_MD5, romMd5 );
        intent.putExtra( ActivityHelper.Keys.CHEAT_ARGS, cheatArgs );
        intent.putExtra( ActivityHelper.Keys.DO_RESTART, doRestart );
        context.startActivity( intent );
    }
    
    public static void startGlobalPrefsActivity( Context context )
    {
        startGlobalPrefsActivity( context, 0 );
    }
    
    public static void startGlobalPrefsActivity( Context context, int menuDisplayMode )
    {
        Intent intent = new Intent( context, GlobalPrefsActivity.class );
        intent.putExtra( ActivityHelper.Keys.MENU_DISPLAY_MODE, menuDisplayMode );
        context.startActivity( intent );
    }
    
    public static void startGamePrefsActivity( Context context, String romPath, String romMd5 )
    {
        Intent intent = new Intent( context, GamePrefsActivity.class );
        intent.putExtra( ActivityHelper.Keys.ROM_PATH, romPath );
        intent.putExtra( ActivityHelper.Keys.ROM_MD5, romMd5 );
        context.startActivity( intent );
    }
    
    public static void startManageEmulationProfilesActivity( Context context )
    {
        context.startActivity( new Intent( context, ManageEmulationProfilesActivity.class ) );
    }
    
    public static void startManageTouchscreenProfilesActivity( Context context )
    {
        context.startActivity( new Intent( context, ManageTouchscreenProfilesActivity.class ) );
    }
    
    public static void startManageControllerProfilesActivity( Context context )
    {
        context.startActivity( new Intent( context, ManageControllerProfilesActivity.class ) );
    }
    
    public static void startEmulationProfileActivity( Context context, String profileName )
    {
        Intent intent = new Intent( context, EmulationProfileActivity.class );
        intent.putExtra( ActivityHelper.Keys.PROFILE_NAME, profileName );
        context.startActivity( intent );
    }
    
    public static void startTouchscreenProfileActivity( Context context, String profileName )
    {
        Intent intent = new Intent( context, TouchscreenProfileActivity.class );
        intent.putExtra( ActivityHelper.Keys.PROFILE_NAME, profileName );
        context.startActivity( intent );
    }
    
    public static void startControllerProfileActivity( Context context, String profileName )
    {
        Intent intent = new Intent( context, ControllerProfileActivity.class );
        intent.putExtra( ActivityHelper.Keys.PROFILE_NAME, profileName );
        context.startActivity( intent );
    }
    
    public static void startDiagnosticActivity( Context context )
    {
        context.startActivity( new Intent( context, DiagnosticActivity.class ) );
    }
}
