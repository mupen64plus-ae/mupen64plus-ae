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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
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
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import paulscode.android.mupen64plusae.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.cheat.CheatUtils;
import paulscode.android.mupen64plusae.game.GameActivity;
import paulscode.android.mupen64plusae.game.GameDataManager;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.util.CountryCode;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.PixelBuffer;
import paulscode.android.mupen64plusae.util.RomHeader;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class CoreService extends Service implements CoreInterface.OnFpsChangedListener, RaphnetControllerHandler.DeviceReadyListener {

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
         * Called when the service has been started
         */
        void onCoreServiceStarted();

        /**
         * Called when we are ready for netplay
         */
        void onNetplayReady();

        /**
         * Called when the service has been destroyed
         */
        void onCoreServiceDestroyed();

        /**
         * Called when the frame rate has changed.
         *
         * @param newValue The new FPS value.
         */
        void onFpsChanged( int newValue );
    }

    interface LoadingDataListener
    {
        /**
         * Called on when ROM extraction starts
         */
        void loadingStarted();

        /**
         * Called on when ROM extraction finishes
         */
        void loadingFinished();
    }
    
    private static final String TAG = "CoreService";

    public static final String COMPLETE_EXTENSION = "complete";
    public static final String SERVICE_EVENT = "M64P_SERVICE_EVENT";
    public static final String SERVICE_RESUME = "M64P_SERVICE_RESUME";
    public static final String SERVICE_QUIT = "M64P_SERVICE_QUIT";
    final static String NOTIFICATION_CHANNEL_ID = "CoreServiceChannel";
    final static String NOTIFICATION_CHANNEL_ID_V2 = "CoreServiceChannelV2";


    // Slot info - used internally
    private static final int NUM_SLOTS = 10;

    // Startup info - used internally
    private String mRomGoodName = null;
    private String mRomDisplayName = null;
    private String mRomPath = null;
    private String mZipPath = null;
    private ArrayList<CheatUtils.Cheat> mCheats = null;
    private boolean mIsRestarting = false;
    private boolean mUseRaphnetDevicesIfAvailable = false;
    private boolean mIsRunning = false;
    private boolean mIsPaused = false;
    private String mArtPath = null;
    private String mRomMd5 = null;
    private String mRomCrc = null;
    private String mRomHeaderName = null;
    private byte mRomCountryCode = 0;
    private int mVideoRenderWidth = 0;
    private int mVideoRenderHeight = 0;
    private PixelBuffer mPixelBuffer = null;

    private final Object mWaitForNetPlay = new Object();
    private boolean mNetplayReady = false;
    private boolean mUsingNetplay = false;
    private boolean mNetplayInitSuccess = false;
    private String netplayVideoPlugin;
    private String netplayRspPlugin;

    //Service attributes
    private int mStartId;
    private ServiceHandler mServiceHandler;
    private boolean mIsShuttingDown = false;

    private final IBinder mBinder = new LocalBinder();
    private CoreServiceListener mListener = null;
    private LoadingDataListener mLoadingDataListener = null;
    private RaphnetControllerHandler mRaphnetHandler = null;
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;
    private GamePrefs mGamePrefs = null;

    private GameDataManager mGameDataManager = null;

    private static final CoreInterface mCoreInterface = new CoreInterface();

    /**
     * Last time we received an FPS changed callback. This is used to determine if the core
     * is locked up since these won't happen if it is.
     */
    private long mLastFpsChangedTime;
    private final Handler mFpsCangedHandler = new Handler(Looper.getMainLooper());
    private final Handler mPeriodicActionHandler = new Handler(Looper.getMainLooper());
    private final Handler mShutdownHandler = new Handler(Looper.getMainLooper());

    final static int ONGOING_NOTIFICATION_ID = 1;

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "SERVICE_EVENT" is broadcasted.
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            boolean resumeMessage = intent.getBooleanExtra(SERVICE_RESUME, false);
            boolean quitMessage = intent.getBooleanExtra(SERVICE_QUIT, false);

            if (!mIsShuttingDown && mIsRunning) {
                if (resumeMessage) {
                    ActivityHelper.startGameActivity( getBaseContext(), mRomPath, mZipPath, mRomMd5, mRomCrc,
                            mRomHeaderName, mRomCountryCode, mArtPath, mRomGoodName, mRomDisplayName, mIsRestarting);
                }

                if (quitMessage) {

                    //Stop the service immediately
                    forceExit();
                }
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
        mLastFpsChangedTime = System.currentTimeMillis() / 1000L;
        tryShutdown();

        synchronized (mWaitForNetPlay) {
            mNetplayReady = true;
            mWaitForNetPlay.notify();
        }
    }

    private void tryShutdown()
    {
        mFpsCangedHandler.removeCallbacks(mLastFpsChangedChecker);
        mFpsCangedHandler.postDelayed(mLastFpsChangedChecker, 500);

        mIsShuttingDown = true;
        updateNotification();

        // Tell the core to quit
        mCoreInterface.emuStop();
    }

    void resumeEmulator()
    {
        Log.e(TAG, "resume emulator");

        if(!mIsShuttingDown)
        {
            Log.e(TAG, "Actually resume emulator");

            mIsPaused = false;
            mCoreInterface.emuResume();

            updateNotification();
        }
    }

    void autoSaveState(final boolean shutdownOnFinish)
    {
        final String latestSave = mGameDataManager.getAutoSaveFileName();

        // Auto-save in case device doesn't resume properly (e.g. OS kills process, battery dies, etc.)

        Log.i(TAG, "Saving file: " + latestSave);

        if (shutdownOnFinish)
        {
            mLastFpsChangedTime = System.currentTimeMillis() / 1000L;
            mFpsCangedHandler.removeCallbacks(mLastFpsChangedChecker);
            mFpsCangedHandler.postDelayed(mLastFpsChangedChecker, 500);

            mIsShuttingDown = true;

            mCoreInterface.setVolume(0);

            Log.e(TAG, "Autosave started");
        }

        CoreInterface.OnStateCallbackListener saveComplete = new CoreInterface.OnStateCallbackListener() {
            @Override
            public void onStateCallback(int paramChanged, int newValue) {
                if (paramChanged == CoreTypes.m64p_core_param.M64CORE_STATE_SAVECOMPLETE.ordinal()) {
                    Log.e(TAG, "Save completed: " + latestSave);

                    //newValue == 1, then it was successful
                    if (newValue == 1) {
                        try {
                            if (!new File(latestSave + "." + COMPLETE_EXTENSION).createNewFile()) {
                                Log.e(TAG, "Unable to save file due to file write failure: " + latestSave);
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Unable to save file due to file write failure: " + latestSave);
                        }
                    } else {
                        Log.e(TAG, "Unable to save file due to bad return: " + latestSave);
                    }

                    final CoreInterface.OnStateCallbackListener saveCompleteListener = this;

                    // Don't do this on teh same thread since the core doesn't like to be called
                    // back to again on a call back
                    mShutdownHandler.postDelayed(() -> {
                        mCoreInterface.removeOnStateCallbackListener(this);

                        if (shutdownOnFinish) {
                            shutdownEmulator();
                        }
                    }, 500);

                } else {
                    Log.i(TAG, "Param changed = " + paramChanged + " value = " + newValue);
                }
            }
        };

        mCoreInterface.addOnStateCallbackListener(saveComplete);

        mCoreInterface.emuSaveFile( latestSave );
    }

    void saveState(String filename)
    {
        File currentSaveStateFile = new File( mGamePrefs.getUserSaveDir() + "/" + filename );
        mCoreInterface.emuSaveFile( currentSaveStateFile.getAbsolutePath() );
    }

    void pauseEmulator()
    {
        if (!mUsingNetplay) {
            mIsPaused = true;
            mCoreInterface.emuPause();

            updateNotification();
        }
    }

    void togglePause()
    {
        if (!mUsingNetplay) {
            CoreTypes.m64p_emu_state state = mCoreInterface.emuGetState();
            if (state == CoreTypes.m64p_emu_state.M64EMU_PAUSED) {
                mIsPaused = false;
                mCoreInterface.emuResume();
            } else if (state == CoreTypes.m64p_emu_state.M64EMU_RUNNING) {
                mIsPaused = true;
                mCoreInterface.emuPause();
            }

            updateNotification();
        }
    }

    boolean isPaused()
    {
        return mIsPaused;
    }

    void toggleFramelimiter()
    {
        if (!mUsingNetplay) {
            boolean state = mCoreInterface.emuGetFramelimiter();
            mCoreInterface.emuSetFramelimiter( !state );
        }
    }

    boolean getFramelimiter()
    {
        return mCoreInterface.emuGetFramelimiter();
    }

    void setSlot(int value)
    {
        int slot = value % NUM_SLOTS;
        mCoreInterface.emuSetSlot( slot );
    }

    int getSlot()
    {
        return mCoreInterface.emuGetSlot();
    }

    int getSlotQuantity()
    {
        return NUM_SLOTS;
    }

    void saveSlot()
    {
        mCoreInterface.emuSaveSlot();
    }

    void loadSlot()
    {
        mCoreInterface.emuLoadSlot();
    }

    void loadState(File file)
    {
        mCoreInterface.emuLoadFile( file.getAbsolutePath() );
    }

    void screenshot()
    {
        mCoreInterface.emuScreenshot();
    }

    void setCustomSpeed(int value)
    {
        mCoreInterface.emuSetSpeed( value );
    }

    void updateControllerConfig(int player, boolean plugged, CoreTypes.PakType pakType)
    {
        if (!mUseRaphnetDevicesIfAvailable)
        {
            NativeInput.setConfig( player, plugged, pakType.ordinal() );
        }
    }

    void advanceFrame()
    {
        mCoreInterface.emuAdvanceFrame();
    }

    void emuGameShark(boolean pressed)
    {
        mCoreInterface.emuGameShark(pressed);
    }

    void setControllerState( int controllerNum, boolean[] buttons, double axisX, double axisY, boolean isDigital )
    {
        if (!mUseRaphnetDevicesIfAvailable) {
            NativeInput.setState( controllerNum, buttons, axisX, axisY, isDigital );
        }
    }

    void registerVibrator( int player, Vibrator vibrator )
    {
        if (!mUseRaphnetDevicesIfAvailable) {
            NativeInput.registerVibrator( player, vibrator );
        }
    }

    void restart()
    {
        mCoreInterface.emuReset();
    }

    void connectForNetplay(int regId, int player, String videoPlugin, String rspPlugin, InetAddress address, int port) {

        netplayVideoPlugin = videoPlugin;
        netplayRspPlugin = rspPlugin;

        mNetplayInitSuccess = address.getHostAddress() != null && mCoreInterface.netplayInit(address.getHostAddress(), port);

        if (mNetplayInitSuccess)
        {
            mCoreInterface.netplaySetController(player, regId);
        }

        if (!mNetplayInitSuccess) {
            Log.e(TAG, "Netplay init unsuccessful host=" + address.getHostAddress() + " port=" + port);
        }
    }

    void startNetplay() {
        synchronized (mWaitForNetPlay) {
            mNetplayReady = true;
            mWaitForNetPlay.notify();
        }
    }

    CoreTypes.m64p_emu_state getState()
    {
        return mCoreInterface.emuGetState();
    }

    PixelBuffer.SurfaceTextureWithSize getSurfaceTexture() {
        return mPixelBuffer.getSurfaceTexture();
    }

    private void setSurface(Surface surface)
    {
        mCoreInterface.setNativeWindow(surface);
    }

    private void unsetSurface()
    {
        mCoreInterface.unsetNativeWindow();
    }

    private void destroySurface()
    {
        mCoreInterface.emuDestroySurface();
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
        public void handleMessage(@NonNull Message msg)
        {
            // Only increase priority if we have more than one processor. The call to check the number of
            // processors is only available in API level 17
            if(Runtime.getRuntime().availableProcessors() > 1 && mGlobalPrefs.useHighPriorityThread) {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                Log.i(TAG, "Using high priority mode");
            }

            if (!mUseRaphnetDevicesIfAvailable) {
                NativeInput.init();
                NativeInput.setConfig( 0, mGamePrefs.isPlugged[0], mGamePrefs.getPakType(1).ordinal() );
                NativeInput.setConfig( 1, mGamePrefs.isPlugged[1], mGamePrefs.getPakType(2).ordinal() );
                NativeInput.setConfig( 2, mGamePrefs.isPlugged[2], mGamePrefs.getPakType(3).ordinal() );
                NativeInput.setConfig( 3, mGamePrefs.isPlugged[3], mGamePrefs.getPakType(4).ordinal() );
            }

            String workingDir = getApplicationContext().getCacheDir().getAbsolutePath() + "/" + AppData.CORE_WORKING_DIR_NAME;

            if (mLoadingDataListener != null) mLoadingDataListener.loadingStarted();

            // Clean up the working directory
            FileUtil.deleteFolder(new File(workingDir));

            // Cleanup previously dumped textures
            FileUtil.deleteFolder(new File(mGlobalPrefs.textureDumpDir));

            // Copy game data from external storage
            if (mGlobalPrefs.useExternalStorge) {
                copyGameContentsFromSdCard();
            }

            FileUtil.makeDirs(workingDir);
            mCoreInterface.setWorkingPath(workingDir);
            FileUtil.makeDirs(mGlobalPrefs.textureDumpDir);

            SparseArray<String> gbRomPaths = new SparseArray<>(4);
            SparseArray<String> gbRamPaths = new SparseArray<>(4);

            for (int player = 1; player < 5; ++player) {
                gbRomPaths.put(player, mGamePrefs.getTransferPakRom(player));
                gbRamPaths.put(player, mGamePrefs.getTransferPakRam(player));
            }

            mCoreInterface.setGbRomPath(getApplicationContext(), gbRomPaths);
            mCoreInterface.setGbRamPath(getApplicationContext(), gbRamPaths);

            boolean isNdd = false;
            boolean isRom = false;
            // First check to see if it's a disk or a ROM
            if (TextUtils.isEmpty(mZipPath)) {
                RomHeader header = new RomHeader(getApplicationContext(), Uri.parse(mRomPath));
                isNdd = header.isNdd;
            } else {
                RomHeader header = new RomHeader(getApplicationContext(), Uri.parse(mZipPath));
                if (header.isZip || header.is7Zip) {
                    RomHeader extractedHeader;
                    if (header.isZip) {
                        extractedHeader = FileUtil.getHeaderFromZip(getApplicationContext(), mRomPath, mZipPath);
                    } else {
                        extractedHeader = FileUtil.getHeaderFromSevenZip(getApplicationContext(), mRomPath, mZipPath);
                    }

                    if (extractedHeader != null) {
                        isNdd = extractedHeader.isNdd;
                    }
                }
            }

            if(!NativeConfigFiles.syncConfigFiles( getApplicationContext(), mGamePrefs, mGlobalPrefs, mAppData,
                    mVideoRenderWidth, mVideoRenderHeight, mUsingNetplay)) {
                //Stop the service
                forceExit();
                return;
            }

            if (isNdd) {
                mCoreInterface.setDdRomPath(getApplicationContext(), mGlobalPrefs.japanIplPath);
                if (TextUtils.isEmpty(mZipPath)) {
                    DocumentFile file = FileUtil.getDocumentFileSingle(getApplicationContext(), Uri.parse(mRomPath));
                    if (file != null) {
                        mCoreInterface.setDdDiskPath(getApplicationContext(), file.getName(), mRomPath);
                    }
                } else {
                    mCoreInterface.setDdDiskPath(getApplicationContext(), mRomPath, mZipPath);
                }
            }
            else {
                mCoreInterface.setDdRomPath(getApplicationContext(), mGamePrefs.idlPath64Dd);
                mCoreInterface.setDdDiskPath(getApplicationContext(), mRomHeaderName, mGamePrefs.diskPath64Dd);
            }

            boolean loadingSuccess;

            loadingSuccess = mCoreInterface.coreStartup(mGamePrefs.getCoreUserConfigDir(), null, mGlobalPrefs.coreUserDataDir,
                    mGlobalPrefs.coreUserCacheDir) == 0;

            // Disk only games still require a ROM image, so use a dummy test ROM
            if (loadingSuccess) {
                if (isNdd) {
                    loadingSuccess = !TextUtils.isEmpty(mGlobalPrefs.japanIplPath);

                    InputStream inputStream;
                    try {
                        inputStream = getApplicationContext().getAssets().open(mAppData.mupen64plus_test_rom_v64);
                        loadingSuccess = loadingSuccess && mCoreInterface.openRom(getApplicationContext(), inputStream);
                    } catch (IOException e) {
                        loadingSuccess = false;
                    }
                } else {
                    if (TextUtils.isEmpty(mZipPath)) {
                        loadingSuccess = mCoreInterface.openRom(getApplicationContext(), mRomPath);
                    } else {
                        loadingSuccess = mCoreInterface.openZip(getApplicationContext(), mZipPath, mRomPath);
                    }
                }
            }

            if (mLoadingDataListener != null) mLoadingDataListener.loadingFinished();

            if (loadingSuccess)
            {
                // Attach all the plugins
                try {
                    // When using netplay, these plugins will be set when the server tell us what they are
                    if (!mUsingNetplay) {
                        mCoreInterface.coreAttachPlugin(CoreTypes.m64p_plugin_type.M64PLUGIN_GFX, mGamePrefs.videoPluginLib.getPluginLib(), true);
                        mCoreInterface.coreAttachPlugin(CoreTypes.m64p_plugin_type.M64PLUGIN_AUDIO, mGamePrefs.audioPluginLib.getPluginLib(), true);

                        if (mUseRaphnetDevicesIfAvailable) {
                            mCoreInterface.coreAttachPlugin(CoreTypes.m64p_plugin_type.M64PLUGIN_INPUT, AppData.InputPlugin.RAPHNET.getPluginLib(), false);
                        } else {
                            mCoreInterface.coreAttachPlugin(CoreTypes.m64p_plugin_type.M64PLUGIN_INPUT, AppData.InputPlugin.ANDROID.getPluginLib(), true);
                        }

                        mCoreInterface.coreAttachPlugin(CoreTypes.m64p_plugin_type.M64PLUGIN_RSP, mGamePrefs.rspPluginLib.getPluginLib(), false);

                    } else {
                        if (mListener != null) {
                            mListener.onNetplayReady();
                        }

                        synchronized (mWaitForNetPlay) {
                            while (!mNetplayReady) {
                                try {
                                    Log.i(TAG, "Waiting on netplay to start");
                                    mWaitForNetPlay.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        Log.i(TAG, "Netplay is ready!");

                        if (mNetplayInitSuccess) {
                            mCoreInterface.coreAttachPlugin(CoreTypes.m64p_plugin_type.M64PLUGIN_GFX, netplayVideoPlugin, true);
                            mCoreInterface.coreAttachPlugin(CoreTypes.m64p_plugin_type.M64PLUGIN_AUDIO, mGamePrefs.audioPluginLib.getPluginLib(), true);

                            if (mUseRaphnetDevicesIfAvailable) {
                                mCoreInterface.coreAttachPlugin(CoreTypes.m64p_plugin_type.M64PLUGIN_INPUT, AppData.InputPlugin.RAPHNET.getPluginLib(), false);
                            } else {
                                mCoreInterface.coreAttachPlugin(CoreTypes.m64p_plugin_type.M64PLUGIN_INPUT, AppData.InputPlugin.ANDROID.getPluginLib(), true);
                            }

                            mCoreInterface.coreAttachPlugin(CoreTypes.m64p_plugin_type.M64PLUGIN_RSP, netplayRspPlugin, false);
                        } else {
                            loadingSuccess = false;
                        }
                    }

                    mCoreInterface.setSelectedAudioPlugin(mGamePrefs.audioPluginLib);

                } catch (IllegalArgumentException e) {
                    loadingSuccess = false;
                }
            }

            if (loadingSuccess) {

                if (mUsingNetplay) {
                    mCoreInterface.emuSetFramelimiter(true);
                    mCoreInterface.usingNetplay(true);
                } else {
                    mCoreInterface.emuSetFramelimiter(mGlobalPrefs.isFramelimiterEnabled);

                    for (GamePrefs.CheatSelection selection : mGamePrefs.getEnabledCheats())
                    {
                        if (selection.getIndex() < mCheats.size()) {
                            CheatUtils.Cheat cheatText = mCheats.get(selection.getIndex());
                            ArrayList<CoreTypes.m64p_cheat_code> cheats = getCheat(cheatText, selection.getOption());
                            if (!cheats.isEmpty()) {
                                mCoreInterface.coreAddCheat(cheatText.name, cheats);
                            }
                        }
                    }
                }

                if (mListener != null) {
                    mListener.onCoreServiceStarted();
                }

                if (!mIsShuttingDown) {
                    if (!mIsRestarting)
                    {
                        final String latestSave = mGameDataManager.getLatestAutoSave();
                        mCoreInterface.emuLoadFile(latestSave);
                    }

                    // This call blocks until emulation is stopped
                    mCoreInterface.emuStart();
                }

                if (mNetplayInitSuccess) {
                    mCoreInterface.closeNetplay();
                }

                // Detach all the plugins
                mCoreInterface.coreDetachPlugin(CoreTypes.m64p_plugin_type.M64PLUGIN_GFX);
                mCoreInterface.coreDetachPlugin(CoreTypes.m64p_plugin_type.M64PLUGIN_RSP);
                mCoreInterface.coreDetachPlugin(CoreTypes.m64p_plugin_type.M64PLUGIN_AUDIO);
                mCoreInterface.coreDetachPlugin(CoreTypes.m64p_plugin_type.M64PLUGIN_INPUT);

                mCoreInterface.writeGbRamData(getApplicationContext(), gbRamPaths);
            }

            // Clean up the working directory
            FileUtil.deleteFolder(new File(workingDir));

            if (loadingSuccess) {
                mGameDataManager.clearOldest();

                if (mGlobalPrefs.useExternalStorge) {
                    copyGameContentsToSdCard();
                }

                mCoreInterface.closeRom();
                mCoreInterface.emuShutdown();
            }

            if(mListener != null && !mIsShuttingDown)
            {
                if(!loadingSuccess)
                {
                    mListener.onFailure(1);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                mListener.onFinish();
                mListener = null;
            }

            //Stop the service
            exitGracefully();
        }
    }

    private void copyGameContentsFromSdCard()
    {
        File gameDataFolder = new File(mAppData.gameDataDir);

        DocumentFile sourceLocation = FileUtil.getDocumentFileTree(getApplicationContext(), Uri.parse(mGlobalPrefs.externalFileStoragePath));
        if (sourceLocation != null) {
            DocumentFile gameDataDir = sourceLocation.findFile(gameDataFolder.getName());

            if (gameDataDir != null) {
                DocumentFile gameDataDirSpecific = gameDataDir.findFile(mGamePrefs.getGameDataDirName());

                FileUtil.copyFilesThatStartWith(getApplicationContext(), gameDataDir, gameDataFolder, mRomGoodName, false);
                FileUtil.copyFilesThatStartWith(getApplicationContext(), gameDataDir, gameDataFolder, mRomHeaderName, false);

                if (gameDataDirSpecific != null) {
                    FileUtil.copyFolder(getApplicationContext(), gameDataDirSpecific, new File(mGamePrefs.getGameDataDir()), false);
                }
            }
        }
    }

    private void copyGameContentsToSdCard()
    {
        File gameDataFolder = new File(mAppData.gameDataDir);

        // Make sure all the right folders exists
        DocumentFile destLocation = FileUtil.getDocumentFileTree(getApplicationContext(), Uri.parse(mGlobalPrefs.externalFileStoragePath));
        if (destLocation != null) {
            destLocation = FileUtil.createFolderIfNotPresent(getApplicationContext(), destLocation, gameDataFolder.getName());
            if (destLocation != null) {

                // Delete the autosaves folder before copying, otherwise the autosaves accumulate
                DocumentFile gameFolder = destLocation.findFile(mGamePrefs.getGameDataDirName());
                if (gameFolder != null) {
                    DocumentFile autoSaveFolder = gameFolder.findFile(GamePrefs.AUTO_SAVES_DIR);
                    if (autoSaveFolder != null) {
                        autoSaveFolder.delete();
                    }
                }

                boolean copySuccess = FileUtil.copyFolder(getApplicationContext(), new File(mGamePrefs.getGameDataDir()), destLocation);

                copySuccess = copySuccess && FileUtil.copyFilesThatStartWith(getApplicationContext(), gameDataFolder, destLocation, mRomGoodName);
                copySuccess = copySuccess && FileUtil.copyFilesThatStartWith(getApplicationContext(), gameDataFolder, destLocation, mRomHeaderName);

                // Delete the old folder if copy was successful
                if (copySuccess) {
                    FileUtil.deleteFolder(new File(mGamePrefs.getGameDataDir()));
                    FileUtil.deleteFileFilter(gameDataFolder, mRomGoodName);
                    FileUtil.deleteFileFilter(gameDataFolder, mRomHeaderName);
                }
            }
        }
    }

    private ArrayList<CoreTypes.m64p_cheat_code> getCheat(CheatUtils.Cheat cheatText, int selectedOption)
    {
        ArrayList<CoreTypes.m64p_cheat_code> codes = new ArrayList<>();

        int indexOfOption = 0;
        int codeIndex = 0;

        //Convert address string to a list of addresses
        if( !TextUtils.isEmpty( cheatText.code ) )
        {
            String[] addressStrings;
            addressStrings = cheatText.code.split("\n");

            for(String address : addressStrings)
            {
                CoreTypes.m64p_cheat_code code = new CoreTypes.m64p_cheat_code();

                String addressString = address.substring(0, 8);
                String valueString = address.substring(address.length()-4);

                code.address = Long.valueOf(addressString, 16).intValue();
                if(!valueString.contains("?"))
                {
                    code.value = Integer.valueOf(valueString, 16);
                }
                else
                {
                    indexOfOption = codeIndex;
                }
                ++codeIndex;
                codes.add(code);
            }
        }

        //Convert options into a list of options
        if( !TextUtils.isEmpty( cheatText.option ) )
        {
            String[] optionStrings;
            optionStrings = cheatText.option.split( "\n" );

            if (selectedOption >= optionStrings.length)
            {
                selectedOption = 0;
            }

            String option = optionStrings[selectedOption];

            String valueString = option.substring(option.length()-4);
            codes.get(indexOfOption).value = Integer.valueOf(valueString, 16);
        }
        
        return codes;
    }

    @Override
    public void onCreate() {

        Log.i(TAG, "OnCreate");

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

        mAppData = new AppData(this);
        mGlobalPrefs = new GlobalPrefs( this, mAppData );

        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "SERVICE_EVENT".
        ContextCompat.registerReceiver(
                this,
                mMessageReceiver,
                new IntentFilter(SERVICE_EVENT),
                ContextCompat.RECEIVER_EXPORTED);
    }

    public void initChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_V2,
                mRomDisplayName, NotificationManager.IMPORTANCE_LOW);
        channel.enableVibration(false);
        channel.setSound(null,null);

        if(notificationManager != null) {
            notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void updateNotification()
    {
        //Show the notification

        //Intent for resuming game
        Intent notificationIntent = new Intent(this, GameActivity.class);
        notificationIntent.putExtra( ActivityHelper.Keys.ROM_PATH, mRomPath );
        notificationIntent.putExtra( ActivityHelper.Keys.ZIP_PATH, mZipPath );
        notificationIntent.putExtra( ActivityHelper.Keys.ROM_MD5, mRomMd5 );
        notificationIntent.putExtra( ActivityHelper.Keys.ROM_CRC, mRomCrc );
        notificationIntent.putExtra( ActivityHelper.Keys.ROM_HEADER_NAME, mRomHeaderName );
        notificationIntent.putExtra( ActivityHelper.Keys.ROM_COUNTRY_CODE, mRomCountryCode );
        notificationIntent.putExtra( ActivityHelper.Keys.ROM_ART_PATH, mArtPath );
        notificationIntent.putExtra( ActivityHelper.Keys.ROM_GOOD_NAME, mRomGoodName );
        notificationIntent.putExtra( ActivityHelper.Keys.ROM_DISPLAY_NAME, mRomDisplayName );
        notificationIntent.putExtra( ActivityHelper.Keys.DO_RESTART, mIsRestarting );
        notificationIntent.putExtra( ActivityHelper.Keys.EXIT_GAME, false );
        notificationIntent.putExtra( ActivityHelper.Keys.FORCE_EXIT_GAME, false );
        notificationIntent.putExtra( ActivityHelper.Keys.VIDEO_RENDER_WIDTH, mVideoRenderWidth );
        notificationIntent.putExtra( ActivityHelper.Keys.VIDEO_RENDER_HEIGHT, mVideoRenderHeight );
        notificationIntent.putExtra( ActivityHelper.Keys.NETPLAY_ENABLED, mUsingNetplay );
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT |
                PendingIntent.FLAG_IMMUTABLE);

        //Intent for closing the game
        Intent exitIntent = (Intent)notificationIntent.clone();
        exitIntent.putExtra( ActivityHelper.Keys.EXIT_GAME, true );
        PendingIntent pendingExitIntent = PendingIntent.getActivity(this, 1, exitIntent,
                PendingIntent.FLAG_UPDATE_CURRENT |
                PendingIntent.FLAG_IMMUTABLE);

        //Intent for force closing the game
        Intent forceExitIntent = (Intent)notificationIntent.clone();
        forceExitIntent.putExtra( ActivityHelper.Keys.FORCE_EXIT_GAME, true );
        PendingIntent pendingForceExitIntent = PendingIntent.getActivity(this, 2, forceExitIntent,
                PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_IMMUTABLE);

        initChannels(getBaseContext());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_V2)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(mRomDisplayName)
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

        builder.addAction(R.drawable.ic_box, getString(R.string.inputMapActivity_stop), pendingExitIntent);
        builder.addAction(R.drawable.ic_undo, getString(R.string.menuItem_exit), pendingForceExitIntent);

        startForeground(ONGOING_NOTIFICATION_ID, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");

        if(intent != null)
        {
            Bundle extras = intent.getExtras();

            if(extras == null) {
                throw new IllegalArgumentException("Invalid parameters passed to CoreService");
            }

            mRomGoodName = extras.getString( ActivityHelper.Keys.ROM_GOOD_NAME );
            mRomDisplayName  = extras.getString( ActivityHelper.Keys.ROM_DISPLAY_NAME );
            mRomPath = extras.getString( ActivityHelper.Keys.ROM_PATH );
            mZipPath = extras.getString( ActivityHelper.Keys.ZIP_PATH );

            mIsRestarting = extras.getBoolean( ActivityHelper.Keys.DO_RESTART, false );
            mUseRaphnetDevicesIfAvailable = extras.getBoolean( ActivityHelper.Keys.USE_RAPHNET_DEVICES, false );

            mRomMd5 = extras.getString( ActivityHelper.Keys.ROM_MD5 );
            mRomCrc = extras.getString( ActivityHelper.Keys.ROM_CRC );
            mRomHeaderName = extras.getString( ActivityHelper.Keys.ROM_HEADER_NAME );
            mRomCountryCode = extras.getByte( ActivityHelper.Keys.ROM_COUNTRY_CODE );
            mArtPath = extras.getString( ActivityHelper.Keys.ROM_ART_PATH );
            mVideoRenderWidth = extras.getInt( ActivityHelper.Keys.VIDEO_RENDER_WIDTH );
            mVideoRenderHeight = extras.getInt( ActivityHelper.Keys.VIDEO_RENDER_HEIGHT );
            mUsingNetplay = extras.getBoolean(ActivityHelper.Keys.NETPLAY_ENABLED);

            mGamePrefs = new GamePrefs( this, mRomMd5, mRomCrc, mRomHeaderName, mRomGoodName,
                    CountryCode.getCountryCode(mRomCountryCode).toString(), mAppData, mGlobalPrefs );

            mGameDataManager = new GameDataManager(mGlobalPrefs, mGamePrefs, mGlobalPrefs.maxAutoSaves);
            mGameDataManager.makeDirs();

            if (mGamePrefs.getEnabledCheats() != null && mGamePrefs.getEnabledCheats().size() != 0) {
                String countryString = String.format("%02x", mRomCountryCode).substring(0, 2);
                String regularExpression = "^" + mRomCrc.replace( ' ', '-') + "-C:" + countryString + ".*";

                BufferedReader cheatLocation = CheatUtils.getCheatsLocation(regularExpression, mAppData.mupencheat_txt);
                if( cheatLocation == null  )
                {
                    Log.w( TAG, "No cheat section found for '" + mRomCrc + "'" );
                }

                mCheats = CheatUtils.populateWithPosition( cheatLocation, mRomCrc, mRomCountryCode, getBaseContext() );
            }

            // Load the native libraries, this must be done outside the thread to prevent race conditions
            // that depend on the libraries being loaded after this call is made
            mCoreInterface.addOnFpsChangedListener( CoreService.this, 15, mCoreInterface );

            mRaphnetHandler = new RaphnetControllerHandler(getBaseContext(), this);

            if (mUseRaphnetDevicesIfAvailable) {
                mRaphnetHandler.requestDeviceAccess();
            }

            mPeriodicActionHandler.removeCallbacks(mPeriodicAction);
            mPeriodicActionHandler.postDelayed(mPeriodicAction, 500);

            // This must happen here instead of OnCreate because we only find out the rendering
            // resolution here.
            if (mPixelBuffer == null) {
                mPixelBuffer = new PixelBuffer(mVideoRenderWidth, mVideoRenderHeight);
                setSurface(mPixelBuffer.getSurface());
                mPixelBuffer.destroyGlContext();
            }

            updateNotification();
        }

        mStartId = startId;

        // Don't restart service if killed
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy()
    {
        Log.i(TAG, "onDestroy");

        // Unregister since the activity is about to be closed.
        unregisterReceiver(mMessageReceiver);

        if (mPixelBuffer != null) {
            mPixelBuffer.releaseSurfaceTexture();
            unsetSurface();
            destroySurface();
        }

        forceExit();
    }

    public void forceExit()
    {
        exitGracefully();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public void exitGracefully()
    {
        if (mUseRaphnetDevicesIfAvailable) {
            mRaphnetHandler.shutdownAccess();
        }

        //Stop the service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setCoreServiceListener(CoreServiceListener coreServiceListener)
    {
        Log.i(TAG, "setCoreServiceListener");
        mListener = coreServiceListener;

        if(!mIsRunning)
        {
            mIsRunning = true;

            // Don't start yet unless the Raphnet device is ready or we are not using a
            // Raphnet device
            if (!mUseRaphnetDevicesIfAvailable || mRaphnetHandler.isReady()) {
                // For each start request, send a message to start a job and deliver the
                // start ID so we know which request we're stopping when we finish the job
                Message msg = mServiceHandler.obtainMessage();
                msg.arg1 = mStartId;
                mServiceHandler.sendMessage(msg);
            }
        }
    }

    public void setLoadingDataListener(LoadingDataListener loadingDataListener)
    {
        Log.i(TAG, "setLoadingDataListener");
        mLoadingDataListener = loadingDataListener;
    }

    @Override
    public void onDeviceReady() {

        // Raphnet device is ready, start if we have been requested to start running
        if(mIsRunning)
        {
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = mStartId;
            mServiceHandler.sendMessage(msg);
        }
    }

    Runnable mPeriodicAction = new Runnable() {
        @Override
        public void run() {
            //If we are paused and we are still somehow swapping buffers
            //then pause again. This can happen if the user pauses the emulator
            //before it's done starting.
            if(mIsPaused)
            {
                mCoreInterface.emuPause();
            }

            synchronized (mWaitForNetPlay) {
                if (mNetplayReady) {
                    mWaitForNetPlay.notify();
                }
            }

            mPeriodicActionHandler.removeCallbacks(mPeriodicAction);
            mPeriodicActionHandler.postDelayed(mPeriodicAction, 500);
        }
    };

    Runnable mLastFpsChangedChecker = new Runnable() {
        @Override
        public void run() {
            long seconds = System.currentTimeMillis() / 1000L;

            //Use a 60 second timeout to save before killing the core process
            if(seconds - mLastFpsChangedTime > 60)
            {
                Log.e(TAG, "Killing Core due to no response");
                forceExit();
            }

            // Call shutdown if it has not been done already
            tryShutdown();

            mFpsCangedHandler.removeCallbacks(mLastFpsChangedChecker);
            mFpsCangedHandler.postDelayed(mLastFpsChangedChecker, 500);
        }
    };

    @Override
    public void onFpsChanged(int newValue) {
        mLastFpsChangedTime = System.currentTimeMillis() / 1000L;

        if (mListener != null) {
            mListener.onFpsChanged(newValue);
        }
    }
}
