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
 * Authors: Paul Lamb, lioncash, littleguy77
 */
package paulscode.android.mupen64plusae.util;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

/**
 * A small class to encapsulate the notification process for Mupen64PlusAE.
 */
public final class Notifier
{
    private static final int NOTIFICATION_ID = 10001;
    
    private static NotificationManager mManager = null;
    private static Toast sToast = null;
    private static Runnable sToastMessager = null;
    
    /**
     * Initialize service and clear any notifications from a previous session.
     * 
     * @param activity the main activity of the app
     */
    public static void initialize( Activity activity )
    {
        if( mManager == null )
            mManager = (NotificationManager) activity
                    .getSystemService( Context.NOTIFICATION_SERVICE );
        clear();
    }
    
    /**
     * Clear the current notification from the status bar.
     */
    public static void clear()
    {
        if( mManager != null )
            mManager.cancel( NOTIFICATION_ID );
    }
    
    /**
     * Place a notification in the status bar.
     * 
     * @param notification the notification to place in the status bar
     */
    public static void notify( Notification notification )
    {
        if( mManager != null )
            mManager.notify( NOTIFICATION_ID, notification );
    }
    
    /**
     * Pop up a temporary message on the device.
     * 
     * @param activity   The activity to display from.
     * @param resId      The resource identifier of the message string
     * @param formatArgs The format arguments that will be used for substitution.
     */
    public static void showToast( Activity activity, int resId, Object... formatArgs )
    {
        showToast( activity, activity.getString( resId, formatArgs ) );
    }
    
    /**
     * Pop up a temporary message on the device.
     * 
     * @param activity The activity to display from
     * @param message  The message string to display.
     */
    public static void showToast( Activity activity, String message )
    {
        if( activity == null )
            return;
        
        if( sToast != null )
        {
            // Toast exists, just change the text
            Notifier.sToast.setText( message );
        }
        else
        {
            // Message short in duration, and at the bottom of the screen
            sToast = Toast.makeText( activity, message, Toast.LENGTH_SHORT );
            sToast.setGravity( Gravity.BOTTOM, 0, 0 );
        }
        
        // Create a messaging task if it doesn't already exist
        if( sToastMessager == null )
        {
            sToastMessager = new Runnable()
            {
                @Override
                public void run()
                {
                    // Just show the toast message
                    if( sToast != null )
                        sToast.show();
                }
            };
        }
        
        // Toast messages must be run on the UiThread, which looks ugly as hell, but works
        activity.runOnUiThread( sToastMessager );
    }
}
