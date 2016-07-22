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
package paulscode.android.mupen64plusae.dialog;

import org.mupen64plusae.v3.fzurita.R;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.util.DeviceUtil;
import android.annotation.SuppressLint;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;

public class Popups
{
    public static void showInvalidInstall( Context context )
    {
        CharSequence title = context.getText( R.string.invalidInstall_title );
        CharSequence message = context.getText( R.string.invalidInstall_message );
        new Builder( context ).setTitle( title ).setMessage( message ).create().show();
    }
    
    public static void showFaq( Context context )
    {
        CharSequence title = context.getText( R.string.menuItem_faq );
        CharSequence message = context.getText( R.string.popup_faq );
        new Builder( context ).setTitle( title ).setMessage( message ).create().show();
    }
    
    public static void showLogcat( Context context )
    {
        String title = context.getString( R.string.menuItem_logcat );
        String message = DeviceUtil.getLogCat();
        showShareableText( context, title, message );
    }
    
    public static void showHardwareInfo( Context context )
    {
        String title = context.getString( R.string.menuItem_hardwareInfo );
        String axisInfo = DeviceUtil.getAxisInfo();
        String peripheralInfo = DeviceUtil.getPeripheralInfo();
        String cpuInfo = DeviceUtil.getCpuInfo();
        String message = axisInfo + peripheralInfo + cpuInfo;
        showShareableText( context, title, message );
    }
    
    public static void showShareableText( final Context context, String title, final String message )
    {
        // Set up click handler to share text with a user-selected app (email, clipboard, etc.)
        DialogInterface.OnClickListener shareHandler = new DialogInterface.OnClickListener()
        {
            @SuppressLint( "InlinedApi" )
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                ActivityHelper.launchPlainText( context, message,
                        context.getText( R.string.actionShare_title ) );
            }
        };
        
        new Builder( context ).setTitle( title ).setMessage( message.toString() )
                .setNeutralButton( R.string.actionShare_title, shareHandler ).create().show();
    }
    
    public static void showAppVersion( Context context )
    {
        AppData appData = new AppData( context );
        String title = context.getString( R.string.menuItem_appVersion );
        String message = context.getString( R.string.popup_version, appData.appVersion,
                appData.appVersionCode );
        new Builder( context ).setTitle( title ).setMessage( message ).create().show();
    }
    
    public static void showNeedsPlayerMap( Context context )
    {
        String title = context.getString( R.string.playerMap_title );
        String message = context.getString( R.string.playerMap_needed );
        new Builder( context ).setTitle( title ).setMessage( message ).create().show();
    }
}
