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
 * Authors: Paul Lamb
 */
package paulscode.android.mupen64plusae.util;

import android.app.AlertDialog;
import android.app.Activity;

/**
 * The TaskHandler class is a utility for running processes in the background, rather than from
 * the UI thread.  A callback is used to notify the UI thread when the task is complete.
 */
public class TaskHandler
{
    /**
     * The Task interface is used to hold the task and the completion callback method.
     */
    public interface Task
    {
        /**
         * Contains the process to be run.
         */
        public void run();
        /**
         * Callback to notify on completion.
         */
        public void onComplete();
    }
    
    /**
     * Launches the specified task on a separate thread, and displays a "please wait" modal dialog
     * until the process has completed. Upon completion, the task's onComplete() method is called
     * from the specified activity's UI thread.
     * 
     * @param activity Parent activity requesting the task.
     * @param title    Title to give the dialog window and the task thread.
     * @param message  Message to display in the dialog window.
     * @param task     Task to be run.
     * 
     * @return Handle to the new thread running the task.
     */
    public static Thread run( Activity activity, String title, String message, Task task )
    {
        final AlertDialog mDialog = new AlertDialog.Builder( activity ).setTitle( title ).setMessage( message ).create();
        final Activity mActivity = activity;
        final Task mTask = task;

        mDialog.show();
        Thread mThread = new Thread( title )
        {
            @Override
            public void run()
            {
                mTask.run();
                mActivity.runOnUiThread( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mDialog.dismiss();
                        mTask.onComplete();
                    }

                } );
            }
        };
        mThread.start();
        return mThread;
    }
    
    /**
     * Silently launches the specified task on a separate thread, without displaying any messages
     * to the user.  Upon completion, the task's onComplete() method is called from the specified
     * activity's UI thread.
     * 
     * @param activity Parent activity requesting the task.
     * @param title    Title to give the task thread.
     * @param task     Task to be run.
     * 
     * @return Handle to the new thread running the task.
     */
    public static Thread run( Activity activity, String title, Task task )
    {
        final Activity mActivity = activity;
        final Task mTask = task;
        Thread mThread = new Thread( title )
        {
            @Override
            public void run()
            {
                mTask.run();
                mActivity.runOnUiThread( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mTask.onComplete();
                    }

                } );
            }
        };
        mThread.start();
        return mThread;
    }
}
