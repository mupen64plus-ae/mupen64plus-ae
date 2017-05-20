/*
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
 * Authors: fzurita
 */
package paulscode.android.mupen64plusae.jni;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.game.GameActivity;

import static paulscode.android.mupen64plusae.jni.NativeExports.emuGetFramelimiter;
import static paulscode.android.mupen64plusae.jni.NativeImports.removeOnStateCallbackListener;

public class CoreService extends Service
{
    interface CoreServiceListener
    {
        /**
         * Called on failure with an error code
         * @param errorCode Error number
         */
        void onFailure(int errorCode);

        /**
         * Called once emulation has stopped
         */
        void onFinish();

        /**
         * Called when the service has been destroyed
         */
        void onCoreServiceDestroyed();
    }

    public static final String COMPLETE_EXTENSION = "complete";
    public static final String SERVICE_EVENT = "SERVICE_EVENT";

    // Slot info - used internally
    private static final int NUM_SLOTS = 10;

    // Startup info - used internally
    private String mRomGoodName = null;
    private String mRomPath = null;
    private String mCheatOptions = null;
    private boolean mIsRestarting = false;
    private String mSaveToLoad = null;
    private String mCoreLib = null;
    private boolean mUseHighPriorityThread = false;
    private ArrayList<Integer> mPakType = null;
    private ArrayList<Boolean> mIsPlugged = null;
    private boolean mIsFrameLimiterEnabled = true;
    private String mCoreUserDataDir = null;
    private String mCoreUserCacheDir = null;
    private String mCoreUserConfigDir = null;
    private String mUserSaveDir = null;
    private boolean mIsRunning = false;
    private boolean mIsPaused = false;
    private String mArtPath = null;
    private String mRomMd5 = null;
    private String mRomCrc = null;
    private String mRomHeaderName = null;
    private byte mRomCountryCode = 0;
    private String mLegacySaveName = null;

    //Service attributes
    private int mStartId;
    private ServiceHandler mServiceHandler;
    private boolean mIsShuttingDown = false;

    private final IBinder mBinder = new LocalBinder();
    private CoreServiceListener mListener = null;

    final static int ONGOING_NOTIFICATION_ID = 1;

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "SERVICE_EVENT" is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            boolean message = intent.getBooleanExtra("resume", false);

            if(message && !mIsShuttingDown)
            {
                ActivityHelper.startGameActivity( getBaseContext(), mRomPath, mRomMd5, mRomCrc,
                        mRomHeaderName, mRomCountryCode, mArtPath, mRomGoodName, mLegacySaveName, mIsRestarting);
            }
        }
    };

    boolean isRunning()
    {
        return mIsRunning;
    }

    boolean isShuttingDown()
    {
        return mIsShuttingDown;
    }

    void shutdownEmulator()
    {
        mIsShuttingDown = true;
        updateNotification();

        // Tell the core to quit
        NativeExports.emuStop();
    }

    void resumeEmulator()
    {
        mIsPaused = false;
        NativeExports.emuResume();

        updateNotification();
    }

    void autoSaveState(final String latestSave, final boolean shutdownOnFinish)
    {
        // Auto-save in case device doesn't resume properly (e.g. OS kills process, battery dies, etc.)

        //Resume to allow save to take place
        resumeEmulator();
        Log.i("CoreService", "Saving file: " + latestSave);

        NativeImports.addOnStateCallbackListener(new NativeImports.OnStateCallbackListener()
        {
            @Override
            public void onStateCallback( int paramChanged, int newValue )
            {
                if( paramChanged == NativeConstants.M64CORE_STATE_SAVECOMPLETE )
                {
                    removeOnStateCallbackListener( this );

                    //newValue == 1, then it was successful
                    if(newValue == 1)
                    {
                        try {
                            new File(latestSave + "." + COMPLETE_EXTENSION).createNewFile();
                        } catch (IOException e) {
                            Log.e("CoreService", "Unable to save file due to file write failure: " + latestSave);
                        }
                    }
                    else
                    {
                        Log.e("CoreService", "Unable to save file due to bad return: " + latestSave);
                    }

                    if(shutdownOnFinish)
                    {
                        shutdownEmulator();
                    }
                }
                else
                {
                    Log.i("CoreService", "Param changed = " + paramChanged + " value = " + newValue);
                }
            }
        } );

        NativeExports.emuSaveFile( latestSave );

        if(shutdownOnFinish)
        {
            //Set a 10 second timeout to save before killing the core process
            Handler killHandler = new Handler();
            killHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.e("CoreService", "Killing Core due to no response");

                    //Stop the service
                    stopForeground(true);
                    stopSelf();

                    android.os.Process.killProcess(android.os.Process.myPid());
                } }, 10000);
        }
    }

    void saveState(String filename)
    {
        File currentSaveStateFile = new File( mUserSaveDir + "/" + filename );
        NativeExports.emuSaveFile( currentSaveStateFile.getAbsolutePath() );
    }

    void pauseEmulator()
    {
        mIsPaused = true;
        NativeExports.emuPause();

        updateNotification();
    }

    void togglePause()
    {
        int state = NativeExports.emuGetState();
        if( state == NativeConstants.EMULATOR_STATE_PAUSED ) {
            mIsPaused = false;
            NativeExports.emuResume();
        }
        else if( state == NativeConstants.EMULATOR_STATE_RUNNING ){
            mIsPaused = true;
            NativeExports.emuPause();
        }

        updateNotification();
    }

    boolean isPaused()
    {
        return mIsPaused;
    }

    void toggleFramelimiter()
    {
        boolean state = emuGetFramelimiter();
        NativeExports.emuSetFramelimiter( !state );
    }

    boolean getFramelimiter()
    {
        return NativeExports.emuGetFramelimiter();
    }

    void setSlot(int value)
    {
        int slot = value % NUM_SLOTS;
        NativeExports.emuSetSlot( slot );
    }

    int getSlot()
    {
        return NativeExports.emuGetSlot();
    }

    void saveSlot()
    {
        NativeExports.emuSaveSlot();
    }

    void loadSlot()
    {
        NativeExports.emuLoadSlot();
    }

    void loadState(File file)
    {
        NativeExports.emuLoadFile( file.getAbsolutePath() );
    }

    void screenshot()
    {
        NativeExports.emuScreenshot();
    }

    void setCustomSpeed(int value)
    {
        NativeExports.emuSetSpeed( value );
    }

    void updateControllerConfig(int player, boolean plugged, int value)
    {
        NativeInput.setConfig( player, plugged, value );
    }

    void advanceFrame()
    {
        NativeExports.emuPause();
        NativeExports.emuAdvanceFrame();
    }

    void emuGameShark(boolean pressed)
    {
        NativeExports.emuGameShark(pressed);
    }

    void setOnFpsChangedListener(NativeImports.OnFpsChangedListener fpsListener, int fpsRecalcPeriod )
    {
        NativeImports.setOnFpsChangedListener( fpsListener, fpsRecalcPeriod );
    }

    void setControllerState( int controllerNum, boolean[] buttons, int axisX, int axisY )
    {
        NativeInput.setState( controllerNum, buttons, axisX, axisY );
    }

    void registerVibrator( int player, Vibrator vibrator )
    {
        NativeInput.registerVibrator( player, vibrator );
    }

    void restart()
    {
        NativeExports.emuReset();
    }

    int getState()
    {
        return NativeExports.emuGetState();
    }

    void setSurface(Surface surface)
    {
        NativeExports.setNativeWindow(surface);
    }

    void destroySurface()
    {
        NativeExports.emuDestroySurface();
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    class LocalBinder extends Binder {
        public CoreService getService() {
            // Return this instance of CoreService so clients can call public methods
            return CoreService.this;
        }
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg)
        {
            // Only increase priority if we have more than one processor. The call to check the number of
            // processors is only available in API level 17
            if(Runtime.getRuntime().availableProcessors() > 1 && mUseHighPriorityThread) {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                Log.i("CoreService", "Using high priority mode");
            }

            // Initialize input-android plugin (even if we aren't going to use it)
            NativeInput.init();
            NativeInput.setConfig( 0, mIsPlugged.get(0), mPakType.get(0) );
            NativeInput.setConfig( 1, mIsPlugged.get(1), mPakType.get(1) );
            NativeInput.setConfig( 2, mIsPlugged.get(2), mPakType.get(2) );
            NativeInput.setConfig( 3, mIsPlugged.get(3), mPakType.get(3) );

            ArrayList<String> arglist = new ArrayList<>();
            arglist.add( "mupen64plus" );
            arglist.add( "--corelib" );
            arglist.add( mCoreLib );
            arglist.add( "--configdir" );
            arglist.add( mCoreUserConfigDir );

            if(!mIsRestarting)
            {
                arglist.add( "--savestate" );
                arglist.add( mSaveToLoad );
            }

            if( !mIsFrameLimiterEnabled )
            {
                arglist.add( "--nospeedlimit" );
            }
            if( mCheatOptions != null )
            {
                arglist.add( "--cheats" );
                arglist.add( mCheatOptions );
            }
            arglist.add( mRomPath );

            //This call blocks until emulation is stopped
            final int result = NativeExports.emuStart( mCoreUserDataDir, mCoreUserCacheDir, arglist.toArray() );

            if(mListener != null)
            {
                if(result != 0)
                {
                    mListener.onFailure(result);
                }

                mListener.onFinish();
                mListener = null;
            }

            NativeExports.unloadLibrariesIfLoaded();

            //Stop the service
            stopForeground(true);
            stopSelf();

            mIsRunning = false;

            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_DEFAULT);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        Looper serviceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(serviceLooper);

        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "SERVICE_EVENT".
        registerReceiver(mMessageReceiver, new IntentFilter(SERVICE_EVENT));
    }

    private void updateNotification()
    {
        //Show the notification
        Intent notificationIntent = new Intent(this, GameActivity.class);
        notificationIntent.putExtra( ActivityHelper.Keys.ROM_PATH, mRomPath );
        notificationIntent.putExtra( ActivityHelper.Keys.ROM_MD5, mRomMd5 );
        notificationIntent.putExtra( ActivityHelper.Keys.ROM_CRC, mRomCrc );
        notificationIntent.putExtra( ActivityHelper.Keys.ROM_HEADER_NAME, mRomHeaderName );
        notificationIntent.putExtra( ActivityHelper.Keys.ROM_COUNTRY_CODE, mRomCountryCode );
        notificationIntent.putExtra( ActivityHelper.Keys.ROM_ART_PATH, mArtPath );
        notificationIntent.putExtra( ActivityHelper.Keys.ROM_GOOD_NAME, mRomGoodName );
        notificationIntent.putExtra( ActivityHelper.Keys.ROM_LEGACY_SAVE, mLegacySaveName );
        notificationIntent.putExtra( ActivityHelper.Keys.DO_RESTART, mIsRestarting );
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(mRomGoodName)
                .setContentIntent(pendingIntent);

        if(mIsShuttingDown)
        {
            builder.setContentText(getString(R.string.toast_shutting_down));
        }
        else if(mIsPaused)
        {
            builder.setContentText(getString(R.string.toast_paused));
        }
        else
        {
            builder.setContentText(getString(R.string.toast_running));
        }

        if (!TextUtils.isEmpty(mArtPath) && new File(mArtPath).exists())
        {
            builder.setLargeIcon(new BitmapDrawable(this.getResources(), mArtPath).getBitmap());
        }
        startForeground(ONGOING_NOTIFICATION_ID, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null)
        {
            Bundle extras = intent.getExtras();

            mRomGoodName = extras.getString( ActivityHelper.Keys.ROM_GOOD_NAME );
            mRomPath = extras.getString( ActivityHelper.Keys.ROM_PATH );
            mCheatOptions = extras.getString( ActivityHelper.Keys.CHEAT_ARGS );
            mIsRestarting = extras.getBoolean( ActivityHelper.Keys.DO_RESTART, false );
            mSaveToLoad = extras.getString( ActivityHelper.Keys.SAVE_TO_LOAD );
            mCoreLib = extras.getString( ActivityHelper.Keys.CORE_LIB );
            mUseHighPriorityThread = extras.getBoolean( ActivityHelper.Keys.HIGH_PRIORITY_THREAD, false );

            mPakType = extras.getIntegerArrayList(ActivityHelper.Keys.PAK_TYPE_ARRAY);

            boolean[] isPluggedArray = extras.getBooleanArray(ActivityHelper.Keys.IS_PLUGGED_ARRAY);
            mIsPlugged = new ArrayList<>();

            for (Boolean isPlugged: isPluggedArray )
            {
                mIsPlugged.add(isPlugged);
            }

            mIsFrameLimiterEnabled = extras.getBoolean( ActivityHelper.Keys.IS_FPS_LIMIT_ENABLED, true );
            mCoreUserDataDir = extras.getString( ActivityHelper.Keys.CORE_USER_DATA_DIR );
            mCoreUserCacheDir = extras.getString( ActivityHelper.Keys.CORE_USER_CACHE_DIR );
            mCoreUserConfigDir = extras.getString( ActivityHelper.Keys.CORE_USER_CONFIG_DIR );
            mUserSaveDir = extras.getString( ActivityHelper.Keys.USER_SAVE_DIR );
            mRomMd5 = extras.getString( ActivityHelper.Keys.ROM_MD5 );
            mRomCrc = extras.getString( ActivityHelper.Keys.ROM_CRC );
            mRomHeaderName = extras.getString( ActivityHelper.Keys.ROM_HEADER_NAME );
            mRomCountryCode = extras.getByte( ActivityHelper.Keys.ROM_COUNTRY_CODE );
            mLegacySaveName = extras.getString( ActivityHelper.Keys.ROM_LEGACY_SAVE );
            mArtPath = extras.getString( ActivityHelper.Keys.ROM_ART_PATH );

            String libsDir = extras.getString( ActivityHelper.Keys.LIBS_DIR );
            // Load the native libraries, this must be done outside the thread to prevent race conditions
            // that depend on the libraries being loaded after this call is made
            NativeExports.loadLibrariesIfNotLoaded( libsDir, Build.VERSION.SDK_INT );

            updateNotification();
        }

        mStartId = startId;

        // Don't restart service if killed
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy()
    {
        Log.i("CoreService", "onDestroy");
        if (mListener != null)
        {
            mListener.onCoreServiceDestroyed();
        }

        if(mIsRunning)
        {
            shutdownEmulator();
        }

        // Unregister since the activity is about to be closed.
        unregisterReceiver(mMessageReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setCoreServiceListener(CoreServiceListener coreServiceListener)
    {
        Log.i("CoreService", "setCoreServiceListener");
        mListener = coreServiceListener;

        if(!mIsRunning)
        {
            mIsRunning = true;
            // For each start request, send a message to start a job and deliver the
            // start ID so we know which request we're stopping when we finish the job
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = mStartId;
            mServiceHandler.sendMessage(msg);
        }
    }
}
