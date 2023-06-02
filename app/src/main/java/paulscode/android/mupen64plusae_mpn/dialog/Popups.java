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
package paulscode.android.mupen64plusae_mpn.dialog;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.mupen64plusae_mpn.v3.alpha.R;

import paulscode.android.mupen64plusae_mpn.ActivityHelper;
import paulscode.android.mupen64plusae_mpn.persistent.AppData;
import paulscode.android.mupen64plusae_mpn.util.DeviceUtil;

public class Popups extends DialogFragment
{
    private static final String STATE_TITLE = "STATE_TITLE";
    private static final String STATE_MESSAGE = "STATE_MESSAGE";
    private static final String STATE_HARDWARE_INFO_POPUP = "STATE_HARDWARE_INFO_POPUP";
    private static final String STATE_SHOW_APP_VERSION_POPUP = "STATE_SHOW_APP_VERSION_POPUP";
    private static final String STATE_FAQ_POPUP = "STATE_FAQ_POPUP";

    public static Popups newInstance(Context context, String title)
    {
        Popups frag = new Popups();
        Bundle args = new Bundle();
        args.putString(STATE_TITLE, title);
        switch(title){
            case STATE_HARDWARE_INFO_POPUP:
                args.putString(STATE_TITLE, context.getString( R.string.menuItem_hardwareInfo ));
                args.putString(STATE_MESSAGE, frag.hardwareInfo());
                break;
            case STATE_SHOW_APP_VERSION_POPUP:
                args.putString(STATE_TITLE, context.getString( R.string.menuItem_appVersion ));
                args.putString(STATE_MESSAGE, frag.appVersion(context));
                break;
            case STATE_FAQ_POPUP:
                args.putString(STATE_TITLE, String.valueOf(context.getText( R.string.menuItem_faq )));
                args.putString(STATE_MESSAGE, frag.faq(context));
                break;
            default:
                break;
        }

        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        final String title = getArguments() != null ? getArguments().getString(STATE_TITLE) : "";
        final String message = getArguments() != null ? getArguments().getString(STATE_MESSAGE) : "";

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(false);

        return builder.create();
    }

    public String faq( Context context )
    {
        return String.valueOf(context.getText( R.string.popup_faq ));
    }

    private String hardwareInfo()
    {
        String axisInfo = DeviceUtil.getAxisInfo();
        String peripheralInfo = DeviceUtil.getPeripheralInfo();
        String cpuInfo = DeviceUtil.getCpuInfo();
        return axisInfo + peripheralInfo + cpuInfo;
    }
    
    public static void showShareableText( final Context context, String title, final String message )
    {
        // Set up click handler to share text with a user-selected app (email, clipboard, etc.)
        DialogInterface.OnClickListener shareHandler = (dialog, which) -> ActivityHelper.launchPlainText( context, message,
                context.getText( R.string.actionShare_title ) );
        
        new Builder( context ).setTitle( title ).setMessage(message)
                .setNeutralButton( R.string.actionShare_title, shareHandler ).create().show();
    }

    public String appVersion(Context context)
    {
        AppData appData = new AppData( context );
        return context.getString( R.string.popup_version, appData.appVersion,
                appData.appVersionCode );
    }
    
    public static void showNeedsPlayerMap( Context context )
    {
        String title = context.getString( R.string.playerMap_title );
        String message = context.getString( R.string.playerMap_needed );
        new Builder( context ).setTitle( title ).setMessage( message ).create().show();
    }
}
