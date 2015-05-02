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
import paulscode.android.mupen64plusae.persistent.GamePrefsActivity;
import paulscode.android.mupen64plusae.persistent.GlobalPrefsActivity;
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
            intent.putExtra( Keys.Extras.ROM_PATH, romPath );
        context.startActivity( intent );
    }
    
    public static void startGameActivity( Context context, String romPath, String romMd5,
            String cheatArgs, boolean doRestart, boolean isXperiaPlay )
    {
        Intent intent = isXperiaPlay
                ? new Intent( context, GameActivityXperiaPlay.class )
                : new Intent( context, GameActivity.class );
        intent.putExtra( Keys.Extras.ROM_PATH, romPath );
        intent.putExtra( Keys.Extras.ROM_MD5, romMd5 );
        intent.putExtra( Keys.Extras.CHEAT_ARGS, cheatArgs );
        intent.putExtra( Keys.Extras.DO_RESTART, doRestart );
        context.startActivity( intent );
    }
    
    public static void startGlobalPrefsActivity( Context context )
    {
        startGlobalPrefsActivity( context, 0 );
    }
    
    public static void startGlobalPrefsActivity( Context context, int menuDisplayMode )
    {
        Intent intent = new Intent( context, GlobalPrefsActivity.class );
        intent.putExtra( Keys.Extras.MENU_DISPLAY_MODE, menuDisplayMode );
        context.startActivity( intent );
    }
    
    public static void startGamePrefsActivity( Context context, String romPath, String romMd5 )
    {
        Intent intent = new Intent( context, GamePrefsActivity.class );
        intent.putExtra( Keys.Extras.ROM_PATH, romPath );
        intent.putExtra( Keys.Extras.ROM_MD5, romMd5 );
        context.startActivity( intent );
    }
    
    public static void startManageEmulationProfilesActivity()
    {
        // TODO
    }
    
    public static void startManageTouchscreenProfilesActivity()
    {
        // TODO
    }
    
    public static void startManageControllerProfilesActivity()
    {
        // TODO
    }
    
    public static void startEmulationProfileActivity()
    {
        // TODO
    }
    
    public static void startTouchscreenProfileActivity()
    {
        // TODO
    }
    
    public static void startControllerProfileActivity()
    {
        // TODO
    }
    
    public static void startDiagnosticActivity()
    {
        // TODO
    }
}
