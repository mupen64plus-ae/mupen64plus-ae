/*
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2015 Paul Lamb
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
 * Authors: fzurita
 */

package paulscode.android.mupen64plusae.netplay;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.fragment.app.Fragment;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.CopyToSdFragment;
import paulscode.android.mupen64plusae.jni.CoreService;
import paulscode.android.mupen64plusae.task.CopyToSdService;

@SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
public class NetplayFragment extends Fragment implements NetplayService.OnFinishListener
{
    //Service connection for the progress dialog
    private ServiceConnection mServiceConnection;
    private NetplayService mNetPlayService;

    private int mServerPort = 0;

    // this method is only called once for this fragment
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }
    
    @Override
    public void onDestroy()
    {        
        if(mServiceConnection != null)
        {
            try {
                Activity activity = requireActivity();
                Intent intent = new Intent(activity, NetplayService.class);
                activity.unbindService(mServiceConnection);
                activity.stopService(intent);

            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        
        super.onDestroy();
    }

    public void startNetplayServer(int port )
    {
        this.mServerPort = port;
        try {
            actuallyStartNetplayService(requireActivity());
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }
    
    private void actuallyStartNetplayService(Activity activity)
    {
        /* Defines callbacks for service binding, passed to bindService() */
        mServiceConnection = new ServiceConnection() {
            
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                NetplayService.LocalBinder binder = (NetplayService.LocalBinder) service;

                mNetPlayService = binder.getService();
                mNetPlayService.startListening(NetplayFragment.this);
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                //Nothing to do here
                Log.i("NetplayFragment", "Netplay servic has been unbound");
            }
        };

        Intent intent = new Intent(activity.getApplicationContext(), NetplayService.class);
        intent.putExtra(ActivityHelper.Keys.NETPLAY_SERVER_PORT, mServerPort);

        activity.getApplicationContext().startService(intent);
        activity.getApplicationContext().bindService(intent, mServiceConnection, 0);
    }

    @Override
    public void onFinish() {
        if(mServiceConnection != null)
        {
            try {
                Activity activity = requireActivity();
                Intent intent = new Intent(activity.getApplicationContext(), NetplayService.class);
                activity.stopService(intent);
                mServiceConnection = null;

            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }
}