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
 * Authors: Paul Lamb, littleguy77
 */
package paulscode.android.mupen64plusae.jni;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;

import com.sun.jna.Callback;
import com.sun.jna.JNIEnv;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.linux.LibC;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.io.File;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.io.FileUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.RomHeader;
import paulscode.android.mupen64plusae.util.SevenZInputStream;

/**
 * Call-outs made from Java to the native core library. Any function names changed here should
 * also be changed in the corresponding C code, and vice versa.
 *
 * @see CoreService
 */
@SuppressWarnings({"SameParameterValue", "UnusedReturnValue", "unused", "ConstantConditions", "FieldCanBeLocal", "WeakerAccess"})
public
class CoreInterface
{
    static {
        // This is needed for JNI_OnLoad on ae-bridge. ae-bridge needs it to call
        // mJavaVM->AttachCurrentThread(&env, nullptr) and mJavaVM->DetachCurrentThread()
        System.loadLibrary( "ae-bridge" );
    }

    interface OnStateCallbackListener
    {
        /**
         * Called when an emulator state/parameter has changed
         *
         * @param paramChanged The parameter ID.
         * @param newValue The new value of the parameter.
         */
        void onStateCallback( int paramChanged, int newValue );
    }

    public interface OnFpsChangedListener
    {
        /**
         * Called when the frame rate has changed.
         *
         * @param newValue The new FPS value.
         */
        void onFpsChanged( int newValue );
    }

    /**
     * Generic library functions
     */
    public interface PluginLibrary extends Library {

        interface DebugCallback extends Callback {
            void invoke(Pointer Context, int level, String message);
        }

        int PluginStartup(Pointer CoreLibHandle, Pointer Context, DebugCallback debugCallBack);

        int PluginGetVersion(IntByReference PluginType, IntByReference PluginVersion, IntByReference APIVersion, PointerByReference PluginNamePtr, IntByReference Capabilities);

        int PluginShutdown();
    }

    final static long MAX_7ZIP_FILE_SIZE = 100*1024*1024;

    // Core state callbacks - used by NativeImports
    private final ArrayList<OnStateCallbackListener> mStateCallbackListeners = new ArrayList<>();
    private final Object mStateCallbackLock = new Object();

    //Frame rate info - used by ae-vidext
    private final ArrayList<OnFpsChangedListener> mFpsListeners = new ArrayList<>();

    private CoreLibrary mMupen64PlusLibrary = Native.load("mupen64plus-core", CoreLibrary.class);
    private AeBridgeLibrary mAeBridgeLibrary = Native.load("ae-bridge", AeBridgeLibrary.class, Collections.singletonMap(Library.OPTION_ALLOW_OBJECTS, Boolean.TRUE));
    private HashMap<CoreTypes.m64p_plugin_type, PluginLibrary> mPlugins = new HashMap<>();

    private Pointer mCoreContext;

    private SparseArray<String> mGbRomPaths = new SparseArray<>(4);
    private SparseArray<String> mGbRamPaths = new SparseArray<>(4);
    private String mDdRom = null;
    private String mDdDisk = null;
    private static final String GB_ROM_NAME = "gb_rom.gb";
    private static final String GB_RAM_NAME = "gb_ram.gb";
    private static final String DD_ROM_NAME = "dd_rom.n64";
    private static final String DD_DISK_NAME = "dd_disk.ndd";
    private File mWorkingPath = null;

    private HashMap<CoreTypes.m64p_plugin_type, Pointer> mPluginContext = new HashMap<>();

    private CoreLibrary.DebugCallback mDebugCallBackCore = new CoreLibrary.DebugCallback() {
        public void invoke(Pointer Context, int level, String message) {
            DebugCallback(Context, level, message);
        }
    };

    private PluginLibrary.DebugCallback mDebugCallBackPlugin = new PluginLibrary.DebugCallback() {
        public void invoke(Pointer Context, int level, String message) {
            DebugCallback(Context, level, message);
        }
    };

    private CoreLibrary.StateCallback mStateCallBack = new CoreLibrary.StateCallback() {

        /**
         * Callback for when an emulator's state/parameter has changed.
         *
         * @param param_type The changed parameter's ID.
         * @param new_value The new value of the changed parameter.
         * see mupen64plus-ae/app/src/main/jni/ae-bridge/ae_imports.cpp
         */
        public void invoke(Pointer Context, int param_type, int new_value) {
            synchronized(mStateCallbackLock)
            {
                for(int i = mStateCallbackListeners.size(); i > 0; i-- )
                {
                    // Traverse the list backwards in case any listeners remove themselves
                    mStateCallbackListeners.get( i - 1 ).onStateCallback( param_type, new_value );
                }
            }
        }
    };

    private CoreTypes.m64p_media_loader.get_gb_cart_rom mGbCartRomCallback = new CoreTypes.m64p_media_loader.get_gb_cart_rom() {
        public String invoke(Pointer cb_data, int controller_num) {
            if (new File(mGbRomPaths.get(controller_num)).exists()) {
                return mGbRomPaths.get(controller_num);
            } else {
                return null;
            }
        }
    };

    private CoreTypes.m64p_media_loader.get_gb_cart_ram mGbCartRamCallback = new CoreTypes.m64p_media_loader.get_gb_cart_ram() {
        public String invoke(Pointer cb_data, int controller_num) {
            if (new File(mGbRamPaths.get(controller_num)).exists()) {
                return mGbRamPaths.get(controller_num);
            } else {
                return null;
            }
        }
    };

    private CoreTypes.m64p_media_loader.get_dd_rom mDdRomCallback = new CoreTypes.m64p_media_loader.get_dd_rom() {
        public String invoke(Pointer cb_data) {
            if (new File(mDdRom).exists()) {
                return mDdRom;
            } else {
                return null;
            }
        }
    };

    private CoreTypes.m64p_media_loader.get_dd_disk mDdDiskCallback = new CoreTypes.m64p_media_loader.get_dd_disk() {
        public String invoke(Pointer cb_data) {
            if (new File(mDdDisk).exists()) {
                return mDdDisk;
            } else {
                return null;
            }
        }
    };

    private AeBridgeLibrary.FpsCounterCallback mFpsCounterCallback = new AeBridgeLibrary.FpsCounterCallback() {
        public void invoke(int fps) {
            synchronized (mFpsListeners)
            {
                for (OnFpsChangedListener listener: mFpsListeners) {
                    listener.onFpsChanged(fps);
                }
            }
        }
    };

    private CoreTypes.m64p_media_loader mMediaLoaderCallbacks = new CoreTypes.m64p_media_loader();

    CoreInterface()
    {
        mMediaLoaderCallbacks.cb_data = null;
        mMediaLoaderCallbacks.gbCartRomCallback = mGbCartRomCallback;
        mMediaLoaderCallbacks.gbCartRamCallback = mGbCartRamCallback;
        mMediaLoaderCallbacks.ddRomCallback = mDdRomCallback;
        mMediaLoaderCallbacks.ddDiskCallback = mDdDiskCallback;
    }

    void setWorkingPath(String path) {
        mWorkingPath = new File(path);

        mDdRom = mWorkingPath + "/" + DD_ROM_NAME;
        mDdDisk = mWorkingPath + "/" + DD_DISK_NAME;

        for (int player = 1; player <= 4; ++player) {
            mGbRomPaths.put(player, mWorkingPath + "/player" + player + "_" + GB_ROM_NAME);
            mGbRamPaths.put(player, mWorkingPath + "/player" + player + "_" + GB_RAM_NAME);
        }
    }

    private void DebugCallback(Pointer Context, int level, String message)
    {
        if (level == CoreTypes.m64p_msg_level.M64MSG_ERROR.ordinal())
            Log.e(Context.getString(0), message);
        else if (level == CoreTypes.m64p_msg_level.M64MSG_WARNING.ordinal())
            Log.w(Context.getString(0), message);
        else if (level == CoreTypes.m64p_msg_level.M64MSG_INFO.ordinal())
            Log.i(Context.getString(0), message);
        else if (level == CoreTypes.m64p_msg_level.M64MSG_STATUS.ordinal())
            Log.d(Context.getString(0), message);
        else if (level == CoreTypes.m64p_msg_level.M64MSG_VERBOSE.ordinal())
        {
            Log.v(Context.getString(0), message);
        } else {
            Log.v(Context.getString(0), message);
        }
    }

    boolean openRom(Context context, String romFileUri)
    {
        boolean success = false;
        byte[] romBuffer = null;

        try (ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(Uri.parse(romFileUri), "r")){
            InputStream is;
            romBuffer = IOUtils.toByteArray(new FileInputStream(parcelFileDescriptor.getFileDescriptor()));
            success = true;
        } catch (IOException|OutOfMemoryError|java.lang.IllegalArgumentException e) {
            e.printStackTrace();
        }

        if (success)
        {
            int romLength = romBuffer.length;

            Pointer parameter = new Memory(romLength);
            parameter.write(0, romBuffer, 0, romBuffer.length);
            mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_ROM_OPEN.ordinal(), romLength, parameter);
        }

        return success;
    }

    private byte[] extractZip(Context context, String romFileName, String zipPathUri) {

        byte[] returnData = null;

        boolean lbFound = false;

        try (ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(Uri.parse(zipPathUri), "r");
             ZipInputStream zipfile = new ZipInputStream(new FileInputStream(parcelFileDescriptor.getFileDescriptor()))) {

            ZipEntry zipEntry = zipfile.getNextEntry();
            while (zipEntry != null && !lbFound) {

                try {
                    final String entryName = new File(zipEntry.getName()).getName();

                    lbFound = (entryName.equals(romFileName) || romFileName == null) && zipEntry.getSize() > 0;

                    if (lbFound) {
                        returnData = new byte[(int) zipEntry.getSize()];

                        int numBytesRead = 0;

                        while (numBytesRead < returnData.length) {
                            numBytesRead += zipfile.read(returnData, numBytesRead, returnData.length - numBytesRead);
                        }

                        if (numBytesRead != returnData.length) {
                            returnData = null;
                        }
                    }

                } catch (final IOException e) {
                    Log.w("CoreInterface", e);
                    returnData = null;
                }

                zipEntry = zipfile.getNextEntry();
            }
        } catch (final IOException | ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
            Log.w("CoreInterface", e);
            returnData = null;
        }

        return returnData;
    }

    private byte[] extractSevenZ(Context context, String romFileName, String zipPath)
    {
        byte[] returnData = null;

        boolean lbFound = false;

        try (ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(Uri.parse(zipPath), "r")) {
            if (parcelFileDescriptor != null) {
                FileInputStream fileInputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
                if (fileInputStream.getChannel().size() < MAX_7ZIP_FILE_SIZE) {
                    SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel(
                            IOUtils.toByteArray(fileInputStream));

                    SevenZFile zipFile = new SevenZFile(channel);
                    SevenZArchiveEntry zipEntry;

                    while( (zipEntry = zipFile.getNextEntry()) != null && !lbFound)
                    {
                        try (InputStream zipStream = new SevenZInputStream(zipFile)) {
                            final String entryName = new File(zipEntry.getName()).getName();

                            lbFound = (entryName.equals(romFileName) || romFileName == null) && zipEntry.getSize() > 0;

                            if (lbFound) {
                                returnData = new byte[(int) zipEntry.getSize()];

                                int numBytesRead = 0;

                                while (numBytesRead < returnData.length) {
                                    numBytesRead += zipStream.read(returnData, numBytesRead, returnData.length - numBytesRead);
                                }

                                if (numBytesRead != returnData.length) {
                                    returnData = null;
                                }
                            }

                        } catch (final IOException e) {
                            Log.w("CoreInterface", e);
                            returnData = null;
                        }
                    }
                }
            }
        } catch (final IOException | ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
            Log.w("CoreInterface", e);
            returnData = null;
        }
        catch (java.lang.OutOfMemoryError e)
        {
            Log.w( "CoreInterface", "Out of memory while extracting 7zip entry: " + romFileName );
            returnData = null;
        }

        return returnData;
    }

    boolean openZip(Context context, String zipPathUri, String romName)
    {
        byte[] romBuffer = null;
        final RomHeader romHeader = new RomHeader(context, Uri.parse(zipPathUri));

        if (romHeader.isZip) {
            romBuffer = extractZip(context, romName, zipPathUri);
        } else if (romHeader.is7Zip) {
            romBuffer = extractSevenZ(context, romName, zipPathUri);
        }

        boolean success = romBuffer != null;

        if (success)
        {
            int romLength = romBuffer.length;

            Pointer parameter = new Memory(romLength);
            parameter.write(0, romBuffer, 0, romBuffer.length);
            mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_ROM_OPEN.ordinal(), romLength, parameter);
        }

        return success;
    }

    public void setGbRomPath(Context context, SparseArray<String> romUris)
    {
        for (int player = 1; player <= 4; ++player) {

            if (!TextUtils.isEmpty(romUris.get(player))) {

                byte[] romBuffer;
                final RomHeader romHeader = new RomHeader(context, Uri.parse(romUris.get(player)));

                if (romHeader.isZip || romHeader.is7Zip) {

                    if (romHeader.isZip) {
                        romBuffer = extractZip(context, null, romUris.get(player));
                    } else {
                        romBuffer = extractSevenZ(context, null, romUris.get(player));
                    }

                    if (romBuffer != null) {
                        try {
                            FileUtils.writeByteArrayToFile(new File(mGbRomPaths.get(player)), romBuffer);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    FileUtil.copySingleFile(context, Uri.parse(romUris.get(player)), new File(mGbRomPaths.get(player)));
                }
            }
        }
    }

    public void setGbRamPath(Context context, SparseArray<String> ramUri)
    {
        for (int player = 1; player <= 4; ++player) {
            if (!TextUtils.isEmpty(ramUri.get(player))) {
                FileUtil.copySingleFile(context, Uri.parse(ramUri.get(player)), new File(mGbRamPaths.get(player)));
            }
        }
    }

    public void setDdRomPath(Context context, String ddRomUri)
    {
        if (TextUtils.isEmpty(ddRomUri)) {
            return;
        }

        byte[] romBuffer;
        final RomHeader romHeader = new RomHeader(context, Uri.parse(ddRomUri));

        if (romHeader.isZip || romHeader.is7Zip) {

            if (romHeader.isZip) {
                romBuffer = extractZip(context, null, ddRomUri);
            } else {
                romBuffer = extractSevenZ(context, null, ddRomUri);
            }

            if (romBuffer != null) {
                try {
                    FileUtils.writeByteArrayToFile(new File(mDdRom), romBuffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Log.e("CoreInterface", "Copying DD ROM: " + ddRomUri);
            FileUtil.copySingleFile(context, Uri.parse(ddRomUri), new File(mDdRom));
        }
    }

    public void setDdDiskPath(Context context, String ddDiskUri)
    {
        if (TextUtils.isEmpty(ddDiskUri)) {
            return;
        }

        FileUtil.copySingleFile(context, Uri.parse(ddDiskUri), new File(mDdDisk));
    }

    public void writeGbRamData(Context context, SparseArray<String> ramUri)
    {
        for (int player = 1; player <= 4; ++player) {

            if (!TextUtils.isEmpty(ramUri.get(player))) {
                FileUtil.copyFile(context, new File(mGbRamPaths.get(player)), Uri.parse(ramUri.get(player)));
            }
        }
    }

    public void writeDdDiskData(Context context, String ddDiskUri)
    {
        if (!TextUtils.isEmpty(ddDiskUri)) {
            FileUtil.copyFile(context, new File(mDdDisk), Uri.parse(ddDiskUri) );
        }
    }

    /* coreStartup()
     *
     * This function initializes libmupen64plus for use by allocating memory,
     * creating data structures, and loading the configuration file.
     */
    int coreStartup(String configDirPath, String dataDirPath, String userDataPath, String userCachePath)
    {
        LibC.INSTANCE.setenv("XDG_DATA_HOME", userDataPath, 1);
        LibC.INSTANCE.setenv("XDG_CACHE_HOME", userCachePath, 1);

        String coreContextText = "Core";
        mCoreContext = new Memory(coreContextText.length()+1);
        mCoreContext.setString(0, coreContextText);

        CoreLibrary.DebugCallback debugCallback = null;
        if (!new File(mDdRom).exists())
            debugCallback = mDebugCallBackCore;
        else
            Log.i("CoreInterface", "Disable core debug due to 64DD ROM found");

        int returnValue = mMupen64PlusLibrary.CoreStartup(CoreLibrary.coreAPIVersion, configDirPath,
                dataDirPath, mCoreContext, debugCallback, null, mStateCallBack);
        mAeBridgeLibrary.overrideAeVidExtFuncs();
        mAeBridgeLibrary.registerFpsCounterCallback(mFpsCounterCallback);
        return returnValue;
    }

    /* coreAttachPlugin()
     *
     * This function attaches the given plugin to the emulator core. There can only
     * be one plugin of each type attached to the core at any given time.
     */
    int coreAttachPlugin(CoreTypes.m64p_plugin_type pluginType, String pluginName)
    {
        mPlugins.put(pluginType, Native.load(pluginName, PluginLibrary.class));

        IntByReference pluginTypeInt = new IntByReference(0);
        IntByReference pluginVersion = new IntByReference(0);
        IntByReference apiVersion = new IntByReference(0);
        PointerByReference pluginNameReference = new PointerByReference();
        IntByReference capabilities = new IntByReference(0);

        mPlugins.get(pluginType).PluginGetVersion(pluginTypeInt, pluginVersion, apiVersion, pluginNameReference, capabilities);

        Pointer coreHandle = mAeBridgeLibrary.loadLibrary("mupen64plus-core");
        mPluginContext.put(pluginType, new Memory(pluginName.length() + 1));

        mPluginContext.get(pluginType).setString(0, pluginName);
        mPlugins.get(pluginType).PluginStartup(coreHandle, mPluginContext.get(pluginType), mDebugCallBackPlugin);

        Pointer handle = mAeBridgeLibrary.loadLibrary(pluginName);
        return mMupen64PlusLibrary.CoreAttachPlugin(pluginType.ordinal(), handle);
    }

    /* coreDetachPlugin()
     *
     * This function detaches the given plugin from the emulator core, and re-attaches
     * the 'dummy' plugin functions.
     */
    int coreDetachPlugin(CoreTypes.m64p_plugin_type pluginType)
    {
        return mMupen64PlusLibrary.CoreDetachPlugin(pluginType.ordinal());
    }

    /* coreAddCheat()
     *
     * This function will add a Cheat Function to a list of currently active cheats
     * which are applied to the open ROM.
     */
    int coreAddCheat(String cheatName, ArrayList<CoreTypes.m64p_cheat_code> codes)
    {
        CoreTypes.m64p_cheat_code cheatCode = new CoreTypes.m64p_cheat_code();
        CoreTypes.m64p_cheat_code[] codesArray = (CoreTypes.m64p_cheat_code[])cheatCode.toArray(codes.size());

        for(int index = 0; index < codes.size(); ++index) {
            codesArray[index].address = codes.get(index).address;
            codesArray[index].value = codes.get(index).value;
        }
        return mMupen64PlusLibrary.CoreAddCheat(cheatName, codesArray, codes.size());
    }

    /* coreCheatEnabled()
     *
     * This function will enable or disable a Cheat Function which is in the list of
     * currently active cheats.
     */
    int coreCheatEnabled(String cheatName, boolean enabled)
    {
        return mMupen64PlusLibrary.CoreCheatEnabled(cheatName, enabled ? 1 : 0);
    }

    void emuStart()
    {
        mMediaLoaderCallbacks.write();
        int returnValue = mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_SET_MEDIA_LOADER.ordinal(), mMediaLoaderCallbacks.size(), mMediaLoaderCallbacks.getPointer());

        Pointer parameter = null;
        mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_EXECUTE.ordinal(), 0, parameter);
    }

    void emuStop()
    {
        Pointer parameter = null;
        mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_STOP.ordinal(), 0, parameter);
    }

    void emuShutdown()
    {
        mMupen64PlusLibrary.CoreShutdown();
    }

    void closeRom()
    {
        Pointer parameter = null;
        mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_ROM_CLOSE.ordinal(), 0, parameter);
    }

    void emuResume()
    {
        mAeBridgeLibrary.resumeEmulator();

        Pointer parameter = null;
        mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_RESUME.ordinal(), 0, parameter);
    }

    void emuPause()
    {
        mAeBridgeLibrary.pauseEmulator();

        Pointer parameter = null;
        mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_PAUSE.ordinal(), 0, parameter);
    }

    void emuAdvanceFrame()
    {
        mAeBridgeLibrary.resumeEmulator();

        Pointer parameter = null;
        mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_ADVANCE_FRAME.ordinal(), 0, parameter);
    }

    void emuSetSpeed( int percent )
    {
        IntByReference parameter = new IntByReference(percent);
        mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_CORE_STATE_SET.ordinal(), CoreTypes.m64p_core_param.M64CORE_SPEED_FACTOR.ordinal(), parameter.getPointer());
    }

    void emuSetFramelimiter( boolean enabled )
    {
        IntByReference parameter = new IntByReference(enabled ? 1 : 0);
        mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_CORE_STATE_SET.ordinal(), CoreTypes.m64p_core_param.M64CORE_SPEED_LIMITER.ordinal(), parameter.getPointer());
    }

    void emuSetSlot( int slotID )
    {
        Pointer parameter = null;
        mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_STATE_SET_SLOT.ordinal(), slotID, parameter);
    }

    void emuLoadSlot()
    {
        Pointer parameter = null;
        mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_STATE_LOAD.ordinal(), 0, parameter);
    }

    void emuSaveSlot()
    {
        Pointer parameter = null;
        mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_STATE_SAVE.ordinal(), 1, parameter);
    }

    void emuLoadFile( String filename )
    {
        Pointer parameterPointer = new Memory(filename.length() + 1);
        parameterPointer.setString(0, filename);
        mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_STATE_LOAD.ordinal(), 0, parameterPointer);
    }

    void emuSaveFile( String filename )
    {
        Pointer parameterPointer = new Memory(filename.length() + 1);
        parameterPointer.setString(0, filename);
        mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_STATE_SAVE.ordinal(), 1, parameterPointer);
    }

    void emuScreenshot()
    {
        Pointer parameter = null;
        mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_TAKE_NEXT_SCREENSHOT.ordinal(), 0, parameter);
    }

    void emuGameShark( boolean pressed )
    {
        IntByReference parameter = new IntByReference(pressed ? 1 : 0);
        mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_CORE_STATE_SET.ordinal(), CoreTypes.m64p_core_param.M64CORE_INPUT_GAMESHARK.ordinal(), parameter.getPointer());
    }

    CoreTypes.m64p_emu_state emuGetState()
    {
        IntByReference state = new IntByReference();
        mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_CORE_STATE_QUERY.ordinal(), CoreTypes.m64p_core_param.M64CORE_EMU_STATE.ordinal(), state.getPointer());

        return CoreTypes.m64p_emu_state.getState(state.getValue());
    }

    int emuGetSpeed()
    {
        IntByReference speed = new IntByReference(100);
        mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_CORE_STATE_QUERY.ordinal(), CoreTypes.m64p_core_param.M64CORE_SPEED_FACTOR.ordinal(), speed.getPointer());
        return speed.getValue();
    }

    boolean emuGetFramelimiter()
    {
        IntByReference enabled = new IntByReference(1);
        mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_CORE_STATE_QUERY.ordinal(), CoreTypes.m64p_core_param.M64CORE_SPEED_LIMITER.ordinal(), enabled.getPointer());
        return enabled.getValue() == 1;
    }

    int emuGetSlot()
    {
        IntByReference slot = new IntByReference(1);
        mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_CORE_STATE_QUERY.ordinal(), CoreTypes.m64p_core_param.M64CORE_SAVESTATE_SLOT.ordinal(), slot.getPointer());
        return slot.getValue();
    }

    void emuReset()
    {
        Pointer parameter = null;
        mMupen64PlusLibrary.CoreDoCommand(CoreTypes.m64p_command.M64CMD_RESET.ordinal(), 0, parameter);
    }

    void setNativeWindow(Surface surface)
    {
        mAeBridgeLibrary.setNativeWindow(JNIEnv.CURRENT, surface);
    }

    void unsetNativeWindow()
    {
        mAeBridgeLibrary.unsetNativeWindow();
    }

    void emuDestroySurface()
    {
        mAeBridgeLibrary.emuDestroySurface();
    }

    void FPSEnabled(int recalc)
    {
        mAeBridgeLibrary.FPSEnabled(recalc);
    }

    void addOnStateCallbackListener( OnStateCallbackListener listener )
    {
        synchronized(mStateCallbackLock)
        {
            // Do not allow multiple instances, in case listeners want to remove themselves
            if( !mStateCallbackListeners.contains( listener ) )
                mStateCallbackListeners.add( listener );
        }
    }

    void removeOnStateCallbackListener( OnStateCallbackListener listener )
    {
        synchronized(mStateCallbackLock)
        {
            mStateCallbackListeners.remove( listener );
        }
    }

    void removeOnFpsChangedListener(OnFpsChangedListener fpsListener)
    {
        synchronized (mFpsListeners)
        {
            mFpsListeners.remove(fpsListener);
        }
    }

    void addOnFpsChangedListener(OnFpsChangedListener fpsListener, int fpsRecalcPeriod, CoreInterface coreInterface )
    {
        synchronized (mFpsListeners)
        {
            if(fpsListener != null && !mFpsListeners.contains(fpsListener))
            {
                mFpsListeners.add(fpsListener);
                coreInterface.FPSEnabled(fpsRecalcPeriod);
            }
        }
    }
}
