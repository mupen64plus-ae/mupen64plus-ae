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
 * Authors: Paul Lamb
 */
package paulscode.android.mupen64plusae.util;

import android.app.AlertDialog;
import android.app.Activity;

public class TaskHandler
{
    public interface Task
    {
        public void run();
        public void onComplete();
    }
    
    public static void run( Activity activity, String title, String message, Task task )
    {
        final AlertDialog mDialog = new AlertDialog.Builder( activity ).setTitle( title ).setMessage( message ).create();
        final Activity mActivity = activity;
        final Task mTask = task;

        mDialog.show();
        new Thread( title )
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
        }.start();
    }
}
