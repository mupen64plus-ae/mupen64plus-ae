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

import android.app.Activity;

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
    
    public static void startSplashActivity()
    {
        // TODO
    }
    
    public static void startGalleryActivity()
    {
        // TODO
    }
    
    public static void startGameActivity()
    {
        // TODO
    }
    
    public static void startGlobalPrefsActivity()
    {
        // TODO
    }
    
    public static void startGamePrefsActivity()
    {
        // TODO
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
