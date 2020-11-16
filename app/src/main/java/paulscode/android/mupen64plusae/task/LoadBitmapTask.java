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
package paulscode.android.mupen64plusae.task;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.widget.ImageView;

import paulscode.android.mupen64plusae.util.FileUtil;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class LoadBitmapTask
{
    ExecutorService mExecutorService;
    HashMap<Integer, LoadBitmapRunnable> mPendingJobs = new HashMap<>();
    HashMap<Integer, Future<?>> mPendingFutures = new HashMap<>();
    private final WeakReference<Activity> mActivity;

    class LoadBitmapRunnable implements Runnable {

        private boolean mCancel = false;
        private final String mBitmapPath;
        private final WeakReference<ImageView> mArtView;

        public LoadBitmapRunnable(String bitmapPath, ImageView artView)
        {
            mBitmapPath = bitmapPath;
            mArtView = new WeakReference<>(artView);
        }

        @Override
        public void run() {
            Activity tempActivity = mActivity.get();
            BitmapDrawable artBitmap = null;

            boolean cancelled = Thread.currentThread().isInterrupted() || mCancel;

            if( !TextUtils.isEmpty( mBitmapPath ) && new File( mBitmapPath ).exists() && tempActivity != null && !cancelled)
            {
                // Check if valid image
                if (FileUtil.isFileImage(new File(mBitmapPath))) {
                    artBitmap = new BitmapDrawable( tempActivity.getResources(), mBitmapPath );
                }

                cancelled = Thread.currentThread().isInterrupted() || mCancel;
                if(!cancelled)
                {
                    BitmapDrawable finalArtBitmap = artBitmap;

                    tempActivity.runOnUiThread(() -> {
                        if (!mCancel) {
                            ImageView tempArtView = mArtView.get();

                            if( tempArtView != null ) {
                                tempArtView.setImageDrawable(finalArtBitmap);
                                tempArtView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            }
                        }
                    });
                }
            }
        }

        public void cancel()
        {
            mCancel = true;
        }
    }

    public LoadBitmapTask( Activity context)
    {
        mExecutorService = Executors.newFixedThreadPool(3);
        mActivity = new WeakReference<>(context);
    }

    public void Shutdown()
    {
        mExecutorService.shutdown();
    }

    public void loadInBackGround(int itemId, String bitmapPath, ImageView artView)
    {
        LoadBitmapRunnable loadRunnable = new LoadBitmapRunnable(bitmapPath, artView);

        Future<?> future = mExecutorService.submit(loadRunnable);
        mPendingJobs.put(itemId, loadRunnable);
        mPendingFutures.put(itemId, future);
    }

    public void cancel(int itemId) {
        LoadBitmapRunnable pendingJob = mPendingJobs.get(itemId);
        if (pendingJob != null) {
            pendingJob.cancel();
            mPendingJobs.remove(itemId);
        }

        Future<?> future = mPendingFutures.get(itemId);
        if (future != null) {
            future.cancel(true);
            mPendingFutures.remove(itemId);
        }
    }
}