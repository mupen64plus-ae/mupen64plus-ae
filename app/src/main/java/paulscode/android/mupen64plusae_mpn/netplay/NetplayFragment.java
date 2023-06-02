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

package paulscode.android.mupen64plusae_mpn.netplay;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.mupen64plusae_mpn.v3.alpha.R;

import paulscode.android.mupen64plusae_mpn.util.Notifier;

@SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
public class NetplayFragment extends Fragment implements NetplayService.NetplayServiceListener
{
    public interface NetplayListener {
        /**
         * Called once a port is obtained
         * @param port Obtained port number
         */
        void onPortObtained(int port);

        /**
         * Callback when a UDP port has been mapped
         * @param tcpPort1 Port for room server
         * @param tcpPort2 Port for TCP netplay server
         * @param udpPort2 Port for UDP netplay server
         */
        void onUpnpPortsObtained(int tcpPort1, int tcpPort2, int udpPort2);
    }

    public static class DataViewModel extends ViewModel {

        public DataViewModel() {}

        //Service connection for the progress dialog
        private ServiceConnection mServiceConnection;
        private NetplayService.LocalBinder mNetplayServiceBinder;

        private boolean mIsNetplayRunning = false;
        private NetplayFragment mCurrentFragment = null;
    }

    DataViewModel mViewModel;

    public void startNetplayServer()
    {
        try {
            actuallyStartNetplayService(requireActivity());
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void mapPorts(int roomPort)
    {
        if (mViewModel != null && mViewModel.mNetplayServiceBinder != null) {
            mViewModel.mNetplayServiceBinder.getService().mapPorts(roomPort);
        }
    }

    public boolean isNetplayStarted()
    {
        return mViewModel != null && mViewModel.mIsNetplayRunning;
    }
    
    private void actuallyStartNetplayService(Activity activity)
    {
        /* Defines callbacks for service binding, passed to bindService() */
        mViewModel.mServiceConnection = new ServiceConnection() {
            
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                mViewModel.mNetplayServiceBinder = (NetplayService.LocalBinder) service;
                mViewModel.mNetplayServiceBinder.getService().startListening(mViewModel.mCurrentFragment);
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                //Nothing to do here
                Log.i("NetplayFragment", "Netplay servic has been unbound");
            }
        };

        Intent intent = new Intent(activity.getApplicationContext(), NetplayService.class);
        activity.getApplicationContext().startService(intent);
        activity.getApplicationContext().bindService(intent, mViewModel.mServiceConnection, 0);

        mViewModel.mIsNetplayRunning = true;
    }

    @Override
    public void onPortObtained(int port) {

        try {
            Activity activity = requireActivity();

            if (activity instanceof NetplayListener) {
                NetplayListener listner = (NetplayListener)activity;
                listner.onPortObtained(port);
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDesync(int vi) {
        try {
            Activity activity = requireActivity();
            activity.runOnUiThread(() -> Notifier.showToast(activity, R.string.netplay_toastDesyncDetected));

        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFinish() {
        if(mViewModel != null && mViewModel.mServiceConnection != null)
        {
            try {
                if (mViewModel.mNetplayServiceBinder != null) {
                    mViewModel.mNetplayServiceBinder.getService().stopServers();
                }

                Activity activity = requireActivity();
                Intent intent = new Intent(activity.getApplicationContext(), NetplayService.class);
                activity.stopService(intent);
                mViewModel.mServiceConnection = null;

            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    /*
     * Callback when a UDP port has been mapped
     * @param tcpPort1 Port for room server
     * @param tcpPort2 Port for TCP netplay server
     * @param udpPort2 Port for UDP netplay server
     */
    public void onUpnpPortsObtained(int tcpPort1, int tcpPort2, int udpPort2)
    {
        try {
            Activity activity = requireActivity();

            if (activity instanceof NetplayListener) {
                NetplayListener listner = (NetplayListener)activity;
                listner.onUpnpPortsObtained(tcpPort1, tcpPort2, udpPort2);
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        mViewModel = new ViewModelProvider(requireActivity()).get(DataViewModel.class);
        mViewModel.mCurrentFragment = this;

        if (!mViewModel.mIsNetplayRunning) {
            startNetplayServer();
        }

        if (mViewModel.mNetplayServiceBinder != null) {
            mViewModel.mNetplayServiceBinder.getService().startListening(mViewModel.mCurrentFragment);
        }
    }
}