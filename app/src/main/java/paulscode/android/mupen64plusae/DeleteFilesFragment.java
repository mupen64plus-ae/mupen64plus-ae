/*
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * <p>
 * Copyright (C) 2015 Paul Lamb
 * <p>
 * This file is part of Mupen64PlusAE.
 * <p>
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
 * not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Authors: fzurita
 */

package paulscode.android.mupen64plusae;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.fragment.app.Fragment;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;
import java.util.ArrayList;

import paulscode.android.mupen64plusae.dialog.ProgressDialog;
import paulscode.android.mupen64plusae.task.DeleteFilesService;
import paulscode.android.mupen64plusae.task.DeleteFilesService.DeleteFilesListener;
import paulscode.android.mupen64plusae.task.DeleteFilesService.LocalBinder;

public class DeleteFilesFragment extends Fragment implements DeleteFilesListener {

    public interface DeleteFilesFinishedListener {
        //This is called once the ROM scan is finished
        void onDeleteFilesFinished();
    }

    //Progress dialog for delete files
    private ProgressDialog mProgress = null;

    //Service connection for the progress dialog
    private ServiceConnection mServiceConnection;

    private boolean mCachedDeleteFiles = false;

    private ArrayList<String> mDeleteFilesPath = new ArrayList<>();
    private ArrayList<String> mFilter = new ArrayList<>();

    private boolean mInProgress = false;

    // this method is only called once for this fragment
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mInProgress) {
            CharSequence title = getString(R.string.pathDeletingFilesTask_title);
            CharSequence message = getString(R.string.toast_pleaseWait);

            mProgress = new ProgressDialog(mProgress, getActivity(), title, "", message, false);
            mProgress.show();
        }

        if (mCachedDeleteFiles) {
            actuallyDeleteFiles(getActivity());
            mCachedDeleteFiles = false;
        }
    }

    @Override
    public void onDetach() {
        //This can be null if this fragment is never utilized and this will be called on shutdown
        if (mProgress != null) {
            mProgress.dismiss();
        }

        super.onDetach();
    }

    @Override
    public void onDestroy() {
        if (mServiceConnection != null && mInProgress && getActivity() != null) {
            ActivityHelper.stopDeleteFilesService(getActivity().getApplicationContext(), mServiceConnection);
        }

        super.onDestroy();
    }

    @Override
    public void onDeleteFilesFinished() {
        if (getActivity() != null && getActivity() instanceof DeleteFilesFinishedListener) {
            ((DeleteFilesFinishedListener)getActivity()).onDeleteFilesFinished();
        }
    }

    @Override
    public void onDeleteFilesServiceDestroyed() {
        mInProgress = false;

        if (getActivity() != null) {
            mProgress.dismiss();
        }
    }

    @Override
    public ProgressDialog GetProgressDialog() {
        return mProgress;
    }

    public void deleteFiles(ArrayList<String> deletePath, ArrayList<String> filter) throws RuntimeException
    {
        this.mDeleteFilesPath = deletePath;
        this.mFilter = filter;

        if (deletePath.size() != filter.size()) {
            throw new RuntimeException("Both arrays must be the same size");
        }

        if (getActivity() != null) {
            actuallyDeleteFiles(getActivity());
        } else {
            mCachedDeleteFiles = true;
        }
    }

    private void actuallyDeleteFiles(Activity activity) {
        mInProgress = true;

        CharSequence title = getString(R.string.pathDeletingFilesTask_title);
        CharSequence message = getString(R.string.toast_pleaseWait);

        mProgress = new ProgressDialog(mProgress, getActivity(), title, "", message, false);
        mProgress.show();

        // Defines callbacks for service binding, passed to bindService() */
        mServiceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {

                // We've bound to LocalService, cast the IBinder and get LocalService instance
                LocalBinder binder = (LocalBinder) service;
                DeleteFilesService deleteFilesService = binder.getService();
                deleteFilesService.setDeleteFilesListener(DeleteFilesFragment.this);
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                //Nothing to do here
            }
        };

        // Asynchronously delete files
        ActivityHelper.startDeleteFilesService(activity.getApplicationContext(), mServiceConnection,
                mDeleteFilesPath, mFilter);
    }

    public boolean IsInProgress() {
        return mInProgress;
    }
}