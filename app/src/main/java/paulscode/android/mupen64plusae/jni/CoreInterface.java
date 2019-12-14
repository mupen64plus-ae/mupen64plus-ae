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

import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;

import com.sun.jna.Callback;
import com.sun.jna.JNIEnv;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.linux.LibC;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.io.File;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Call-outs made from Java to the native core library. Any function names changed here should
 * also be changed in the corresponding C code, and vice versa.
 *
 * @see CoreService
 */
@SuppressWarnings({"SameParameterValue", "UnusedReturnValue", "unused", "ConstantConditions", "FieldCanBeLocal", "WeakerAccess"})
class CoreInterface
{
    enum m64p_error {
        M64ERR_SUCCESS,
        M64ERR_NOT_INIT,        /* Function is disallowed before InitMupen64Plus() is called */
        M64ERR_ALREADY_INIT,    /* InitMupen64Plus() was called twice */
        M64ERR_INCOMPATIBLE,    /* API versions between components are incompatible */
        M64ERR_INPUT_ASSERT,    /* Invalid parameters for function call, such as ParamValue=NULL for GetCoreParameter() */
        M64ERR_INPUT_INVALID,   /* Invalid input data, such as ParamValue="maybe" for SetCoreParameter() to set a BOOL-type value */
        M64ERR_INPUT_NOT_FOUND, /* The input parameter(s) specified a particular item which was not found */
        M64ERR_NO_MEMORY,       /* Memory allocation failed */
        M64ERR_FILES,           /* Error opening, creating, reading, or writing to a file */
        M64ERR_INTERNAL,        /* Internal error (bug) */
        M64ERR_INVALID_STATE,   /* Current program state does not allow operation */
        M64ERR_PLUGIN_FAIL,     /* A plugin function returned a fatal error */
        M64ERR_SYSTEM_FAIL,     /* A system function call, such as an SDL or file operation, failed */
        M64ERR_UNSUPPORTED,     /* Function call is not supported (ie, core not built with debugger) */
        M64ERR_WRONG_TYPE       /* A given input type parameter cannot be used for desired operation */
    }

    enum m64p_plugin_type {
        M64PLUGIN_NULL,
        M64PLUGIN_RSP,
        M64PLUGIN_GFX,
        M64PLUGIN_AUDIO,
        M64PLUGIN_INPUT,
        M64PLUGIN_CORE
    }

    enum m64p_core_param {
        M64CORE_DUMMY,
        M64CORE_EMU_STATE,
        M64CORE_VIDEO_MODE,
        M64CORE_SAVESTATE_SLOT,
        M64CORE_SPEED_FACTOR,
        M64CORE_SPEED_LIMITER,
        M64CORE_VIDEO_SIZE,
        M64CORE_AUDIO_VOLUME,
        M64CORE_AUDIO_MUTE,
        M64CORE_INPUT_GAMESHARK,
        M64CORE_STATE_LOADCOMPLETE,
        M64CORE_STATE_SAVECOMPLETE
    }

    enum m64p_command{
        M64CMD_NOP,
        M64CMD_ROM_OPEN,
        M64CMD_ROM_CLOSE,
        M64CMD_ROM_GET_HEADER,
        M64CMD_ROM_GET_SETTINGS,
        M64CMD_EXECUTE,
        M64CMD_STOP,
        M64CMD_PAUSE,
        M64CMD_RESUME,
        M64CMD_CORE_STATE_QUERY,
        M64CMD_STATE_LOAD,
        M64CMD_STATE_SAVE,
        M64CMD_STATE_SET_SLOT,
        M64CMD_SEND_SDL_KEYDOWN,
        M64CMD_SEND_SDL_KEYUP,
        M64CMD_SET_FRAME_CALLBACK,
        M64CMD_TAKE_NEXT_SCREENSHOT,
        M64CMD_CORE_STATE_SET,
        M64CMD_READ_SCREEN,
        M64CMD_RESET,
        M64CMD_ADVANCE_FRAME,
        M64CMD_SET_MEDIA_LOADER
    }

    enum m64p_msg_level {
        M64MSG_DUMMY,
        M64MSG_ERROR,
        M64MSG_WARNING,
        M64MSG_INFO,
        M64MSG_STATUS,
        M64MSG_VERBOSE
    }

    enum m64p_emu_state{
        M64EMU_DUMMY,
        M64EMU_STOPPED,
        M64EMU_RUNNING,
        M64EMU_PAUSED
    }

    @FieldOrder({ "address", "value" })
    public static class m64p_cheat_code extends Structure
    {
        public int address;
        public int value;

        m64p_cheat_code() {
            address = 0;
            value = 0;
        }
    }


    @FieldOrder({ "cb_data", "gbCartRomCallback", "gbCartRamCallback", "ddRomCallback", "ddDiskCallback" })
    public static class m64p_media_loader extends Structure {

        interface get_gb_cart_rom extends Callback {
            String invoke(Pointer cb_data, int controller_num);
        }

        interface get_gb_cart_ram extends Callback {
            String invoke(Pointer cb_data, int controller_num);
        }

        interface get_dd_rom extends Callback {
            String invoke(Pointer cb_data);
        }

        interface get_dd_disk extends Callback {
            String invoke(Pointer cb_data);
        }

        /* Frontend-defined callback data. */
        public Pointer cb_data;

        /* Allow the frontend to specify the GB cart ROM file to load
         * cb_data: points to frontend-defined callback data.
         * controller_num: (0-3) tell the frontend which controller is about to load a GB cart
         * Returns a NULL-terminated string owned by the core specifying the GB cart ROM filename to load.
         * Empty or NULL string results in no GB cart being loaded (eg. empty transferpak).
         */
        public get_gb_cart_rom gbCartRomCallback;

        /* Allow the frontend to specify the GB cart RAM file to load
         * cb_data: points to frontend-defined callback data.
         * controller_num: (0-3) tell the frontend which controller is about to load a GB cart
         * Returns a NULL-terminated string owned by the core specifying the GB cart RAM filename to load
         * Empty or NULL string results in the core generating a default save file with empty content.
         */
        public get_gb_cart_ram gbCartRamCallback;

        /* Allow the frontend to specify the DD IPL ROM file to load
         * cb_data: points to frontend-defined callback data.
         * Returns a NULL-terminated string owned by the core specifying the DD IPL ROM filename to load
         * Empty or NULL string results in disabled 64DD.
         */
        public get_dd_rom ddRomCallback;

        /* Allow the frontend to specify the DD disk file to load
         * cb_data: points to frontend-defined callback data.
         * Returns a NULL-terminated string owned by the core specifying the DD disk filename to load
         * Empty or NULL string results in no DD disk being loaded (eg. empty disk drive).
         */
        public get_dd_disk ddDiskCallback;

        m64p_media_loader()
        {

        }
    }

    /**
     * Core library
     */
    public interface Mupen64PlusLibrary extends Library {

        int coreAPIVersion = 0x20001;

        interface DebugCallback extends Callback {
            void invoke(Pointer Context, int level, String message);
        }

        interface StateCallback extends Callback {
            void invoke(Pointer Context, int param_type, int new_value);
        }

        /* CoreStartup()
         *
         * This function initializes libmupen64plus for use by allocating memory,
         * creating data structures, and loading the configuration file.
         */
        int CoreStartup(int APIVersion, String ConfigPath, String DataPath, Pointer Context, DebugCallback debugCallBack,
                        Pointer Context2, StateCallback stateCallback);

        /* CoreShutdown()
         *
         * This function saves the configuration file, then destroys data structures
         * and releases memory allocated by the core library.
         */
        int CoreShutdown();

        /* CoreAttachPlugin()
         *
         * This function attaches the given plugin to the emulator core. There can only
         * be one plugin of each type attached to the core at any given time.
         */
        int CoreAttachPlugin(int PluginType, Pointer PluginLibHandle);

        /* CoreDetachPlugin()
         *
         * This function detaches the given plugin from the emulator core, and re-attaches
         * the 'dummy' plugin functions.
         */
        int CoreDetachPlugin(int PluginType);

        /* CoreDoCommand()
         *
         * This function sends a command to the emulator core.
         */
        int CoreDoCommand(int Command, int ParamInt, Pointer ParamPtr);

        /* CoreAddCheat()
         *
         * This function will add a Cheat Function to a list of currently active cheats
         * which are applied to the open ROM.
         */
        int CoreAddCheat(String CheatName, m64p_cheat_code[] CodeList, int NumCodes);

        /* CoreCheatEnabled()
         *
         * This function will enable or disable a Cheat Function which is in the list of
         * currently active cheats.
         */
        int CoreCheatEnabled(String CheatName, int Enabled);
    }

    /**
     * Library used to interface with AE Vid Ext implementation
     */
    public interface AeVidExtLibrary extends Library {

        interface FpsCounterCallback extends Callback {
            void invoke(int fps);
        }
        // Enable or disble VSYNC
        void vsyncEnabled(int enabled);

        // Notify that emulation has been paused
        void pauseEmulator();

        // Notify that emulation has been resumed
        void resumeEmulator();

        // Set FPS recalculation period
        void FPSEnabled(int recalc);

        // Set Native window
        void setNativeWindow(JNIEnv env, Object arg);

        // Unset native window
        void unsetNativeWindow();

        // Destroy surface
        void emuDestroySurface();

        void overrideAeVidExtFuncs();

        void registerFpsCounterCallback(FpsCounterCallback fpsCounterCallback);

        // Load a library using dlopen
        Pointer loadLibrary(String libName);

        // Unload library using dlclose
        int unloadLibrary(Pointer handle, String libName);
    }

    /**
     * Library used to interface with AE Vid Ext implementation
     */
    public interface PluginLibrary extends Library {

        interface DebugCallback extends Callback {
            void invoke(Pointer Context, int level, String message);
        }

        int PluginStartup(Pointer CoreLibHandle, Pointer Context, DebugCallback debugCallBack);

        int PluginGetVersion(IntByReference PluginType, IntByReference PluginVersion, IntByReference APIVersion, PointerByReference PluginNamePtr, IntByReference Capabilities);

        int PluginShutdown();
    }

    private Mupen64PlusLibrary mMupen64PlusLibrary = Native.load("mupen64plus-core", Mupen64PlusLibrary.class);
    private AeVidExtLibrary mAeVidExtLibrary = Native.load("ae-vidext", AeVidExtLibrary.class, Collections.singletonMap(Library.OPTION_ALLOW_OBJECTS, Boolean.TRUE));
    private HashMap<m64p_plugin_type, PluginLibrary> mPlugins = new HashMap<>();

    private Pointer mCoreContext;

    private SparseArray<String> mGbRomPaths = new SparseArray<>(4);
    private SparseArray<String> mGbRamPaths = new SparseArray<>(4);
    private String mDdRom = null;
    private String mDdDisk = null;

    private HashMap<m64p_plugin_type, Pointer> mPluginContext = new HashMap<>();

    private Mupen64PlusLibrary.DebugCallback mDebugCallBackCore = new Mupen64PlusLibrary.DebugCallback() {
        public void invoke(Pointer Context, int level, String message) {
            DebugCallback(Context, level, message);
        }
    };

    private PluginLibrary.DebugCallback mDebugCallBackPlugin = new PluginLibrary.DebugCallback() {
        public void invoke(Pointer Context, int level, String message) {
            DebugCallback(Context, level, message);
        }
    };

    private Mupen64PlusLibrary.StateCallback mStateCallBack = new Mupen64PlusLibrary.StateCallback() {
        public void invoke(Pointer Context, int param_type, int new_value) {
            NativeImports.stateCallback(param_type, new_value);
        }
    };

    private m64p_media_loader.get_gb_cart_rom mGbCartRomCallback = new m64p_media_loader.get_gb_cart_rom() {
        public String invoke(Pointer cb_data, int controller_num) {
            return mGbRomPaths.get(controller_num);
        }
    };

    private m64p_media_loader.get_gb_cart_ram mGbCartRamCallback = new m64p_media_loader.get_gb_cart_ram() {
        public String invoke(Pointer cb_data, int controller_num) {
            return mGbRamPaths.get(controller_num);
        }
    };

    private m64p_media_loader.get_dd_rom mDdRomCallback = new m64p_media_loader.get_dd_rom() {
        public String invoke(Pointer cb_data) {
            return mDdRom;
        }
    };

    private m64p_media_loader.get_dd_disk mDdDiskCallback = new m64p_media_loader.get_dd_disk() {
        public String invoke(Pointer cb_data) {
            return mDdDisk;
        }
    };

    private AeVidExtLibrary.FpsCounterCallback mFpsCounterCallback = new AeVidExtLibrary.FpsCounterCallback() {
        public void invoke(int fps) {
            NativeImports.FPSCounter(fps);
        }
    };

    private m64p_media_loader mMediaLoaderCallbacks = new m64p_media_loader();

    CoreInterface()
    {
        mMediaLoaderCallbacks.cb_data = null;
        mMediaLoaderCallbacks.gbCartRomCallback = mGbCartRomCallback;
        mMediaLoaderCallbacks.gbCartRamCallback = mGbCartRamCallback;
        mMediaLoaderCallbacks.ddRomCallback = mDdRomCallback;
        mMediaLoaderCallbacks.ddDiskCallback = mDdDiskCallback;
    }

    private void DebugCallback(Pointer Context, int level, String message)
    {
        if (level == m64p_msg_level.M64MSG_ERROR.ordinal())
            Log.e(Context.getString(0), message);
        else if (level == m64p_msg_level.M64MSG_WARNING.ordinal())
            Log.w(Context.getString(0), message);
        else if (level == m64p_msg_level.M64MSG_INFO.ordinal())
            Log.i(Context.getString(0), message);
        else if (level == m64p_msg_level.M64MSG_STATUS.ordinal())
            Log.d(Context.getString(0), message);
        else if (level == m64p_msg_level.M64MSG_VERBOSE.ordinal())
        {
            Log.v(Context.getString(0), message);
        }
    }

    boolean openRom(File romFile)
    {
        boolean success = false;
        byte[] romBuffer = null;

        try {
            romBuffer = FileUtils.readFileToByteArray(romFile);
            success = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (success)
        {
            int romLength = (int)romFile.length();

            Pointer parameter = new Memory(romLength);
            parameter.write(0, romBuffer, 0, romBuffer.length);
            mMupen64PlusLibrary.CoreDoCommand(m64p_command.M64CMD_ROM_OPEN.ordinal(), romLength, parameter);
        }

        return success;
    }

    public void setGbRomPath(SparseArray<String> romPaths) {
        mGbRomPaths = romPaths;
    }

    public void setGbRamPath(SparseArray<String> ramPaths) {
        mGbRamPaths = ramPaths;
    }

    public void setDdRomPath(String ddRomPath) {
        this.mDdRom = ddRomPath;
    }

    public void setDdDiskPath(String ddDiskPath) {
        this.mDdDisk = ddDiskPath;
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

        int returnValue = mMupen64PlusLibrary.CoreStartup(Mupen64PlusLibrary.coreAPIVersion, configDirPath,
                dataDirPath, mCoreContext, mDebugCallBackCore, null, mStateCallBack);
        mAeVidExtLibrary.overrideAeVidExtFuncs();
        mAeVidExtLibrary.registerFpsCounterCallback(mFpsCounterCallback);
        return returnValue;
    }

    /* coreAttachPlugin()
     *
     * This function attaches the given plugin to the emulator core. There can only
     * be one plugin of each type attached to the core at any given time.
     */
    int coreAttachPlugin(m64p_plugin_type pluginType, String pluginName)
    {
        mPlugins.put(pluginType, Native.load(pluginName, PluginLibrary.class));

        IntByReference pluginTypeInt = new IntByReference(0);
        IntByReference pluginVersion = new IntByReference(0);
        IntByReference apiVersion = new IntByReference(0);
        PointerByReference pluginNameReference = new PointerByReference();
        IntByReference capabilities = new IntByReference(0);

        mPlugins.get(pluginType).PluginGetVersion(pluginTypeInt, pluginVersion, apiVersion, pluginNameReference, capabilities);

        Pointer coreHandle = mAeVidExtLibrary.loadLibrary("mupen64plus-core");
        mPluginContext.put(pluginType, new Memory(pluginName.length() + 1));

        mPluginContext.get(pluginType).setString(0, pluginName);
        mPlugins.get(pluginType).PluginStartup(coreHandle, mPluginContext.get(pluginType), mDebugCallBackPlugin);

        Pointer handle = mAeVidExtLibrary.loadLibrary(pluginName);
        return mMupen64PlusLibrary.CoreAttachPlugin(pluginType.ordinal(), handle);
    }

    /* coreDetachPlugin()
     *
     * This function detaches the given plugin from the emulator core, and re-attaches
     * the 'dummy' plugin functions.
     */
    int coreDetachPlugin(m64p_plugin_type pluginType)
    {
        return mMupen64PlusLibrary.CoreDetachPlugin(pluginType.ordinal());
    }

    /* coreAddCheat()
     *
     * This function will add a Cheat Function to a list of currently active cheats
     * which are applied to the open ROM.
     */
    int coreAddCheat(String cheatName, ArrayList<m64p_cheat_code> codes)
    {
        m64p_cheat_code[] codesArray = (m64p_cheat_code[])new m64p_cheat_code().toArray(codes.size());

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
        int returnValue = mMupen64PlusLibrary.CoreDoCommand(m64p_command.M64CMD_SET_MEDIA_LOADER.ordinal(), mMediaLoaderCallbacks.size(), mMediaLoaderCallbacks.getPointer());

        IntByReference parameter = new IntByReference(0);
        mMupen64PlusLibrary.CoreDoCommand(m64p_command.M64CMD_EXECUTE.ordinal(), 0, parameter.getPointer());
    }

    void emuStop()
    {
        IntByReference parameter = new IntByReference(0);
        mMupen64PlusLibrary.CoreDoCommand(m64p_command.M64CMD_STOP.ordinal(), 0, parameter.getPointer());
    }

    void emuShutdown()
    {
        mMupen64PlusLibrary.CoreShutdown();
    }

    void closeRom()
    {
        IntByReference parameter = new IntByReference(0);
        mMupen64PlusLibrary.CoreDoCommand(m64p_command.M64CMD_ROM_CLOSE.ordinal(), 0, parameter.getPointer());
    }

    void emuResume()
    {
        mAeVidExtLibrary.resumeEmulator();

        IntByReference parameter = new IntByReference(0);
        mMupen64PlusLibrary.CoreDoCommand(m64p_command.M64CMD_RESUME.ordinal(), 0, parameter.getPointer());
    }

    void emuPause()
    {
        mAeVidExtLibrary.pauseEmulator();

        IntByReference parameter = new IntByReference(0);
        mMupen64PlusLibrary.CoreDoCommand(m64p_command.M64CMD_PAUSE.ordinal(), 0, parameter.getPointer());
    }

    void emuAdvanceFrame()
    {
        mAeVidExtLibrary.resumeEmulator();

        IntByReference parameter = new IntByReference(0);
        mMupen64PlusLibrary.CoreDoCommand(m64p_command.M64CMD_ADVANCE_FRAME.ordinal(), 0, parameter.getPointer());
    }

    void emuSetSpeed( int percent )
    {
        IntByReference parameter = new IntByReference(percent);
        mMupen64PlusLibrary.CoreDoCommand(m64p_command.M64CMD_CORE_STATE_SET.ordinal(), m64p_core_param.M64CORE_SPEED_FACTOR.ordinal(), parameter.getPointer());
    }

    void emuSetFramelimiter( boolean enabled )
    {
        IntByReference parameter = new IntByReference(enabled ? 1 : 0);
        mMupen64PlusLibrary.CoreDoCommand(m64p_command.M64CMD_CORE_STATE_SET.ordinal(), m64p_core_param.M64CORE_SPEED_LIMITER.ordinal(), parameter.getPointer());
    }

    void emuSetSlot( int slotID )
    {
        IntByReference parameter = new IntByReference(0);
        mMupen64PlusLibrary.CoreDoCommand(m64p_command.M64CMD_STATE_SET_SLOT.ordinal(), slotID, parameter.getPointer());
    }

    void emuLoadSlot()
    {
        IntByReference parameter = new IntByReference(0);
        mMupen64PlusLibrary.CoreDoCommand(m64p_command.M64CMD_STATE_LOAD.ordinal(), 0, parameter.getPointer());
    }

    void emuSaveSlot()
    {
        IntByReference parameter = new IntByReference(0);
        mMupen64PlusLibrary.CoreDoCommand(m64p_command.M64CMD_STATE_SAVE.ordinal(), 1, parameter.getPointer());
    }

    void emuLoadFile( String filename )
    {
        Pointer parameterPointer = new Memory(filename.length() + 1);
        parameterPointer.setString(0, filename);
        mMupen64PlusLibrary.CoreDoCommand(m64p_command.M64CMD_STATE_LOAD.ordinal(), 0, parameterPointer);
    }

    void emuSaveFile( String filename )
    {
        Pointer parameterPointer = new Memory(filename.length() + 1);
        parameterPointer.setString(0, filename);
        mMupen64PlusLibrary.CoreDoCommand(m64p_command.M64CMD_STATE_SAVE.ordinal(), 0, parameterPointer);
    }

    void emuScreenshot()
    {
        IntByReference parameter = new IntByReference(0);
        mMupen64PlusLibrary.CoreDoCommand(m64p_command.M64CMD_TAKE_NEXT_SCREENSHOT.ordinal(), 0, parameter.getPointer());
    }

    void emuGameShark( boolean pressed )
    {
        IntByReference parameter = new IntByReference(pressed ? 1 : 0);
        mMupen64PlusLibrary.CoreDoCommand(m64p_command.M64CMD_CORE_STATE_SET.ordinal(), m64p_core_param.M64CORE_INPUT_GAMESHARK.ordinal(), parameter.getPointer());
    }

    int emuGetState()
    {
        IntByReference state = new IntByReference();
        mMupen64PlusLibrary.CoreDoCommand(m64p_command.M64CMD_CORE_STATE_QUERY.ordinal(), m64p_core_param.M64CORE_EMU_STATE.ordinal(), state.getPointer());

        if (state.getValue() == m64p_emu_state.M64EMU_STOPPED.ordinal())
            return 1;
        else if (state.getValue() == m64p_emu_state.M64EMU_RUNNING.ordinal())
            return 2;
        else if (state.getValue() == m64p_emu_state.M64EMU_PAUSED.ordinal())
            return 3;
        else
            return 0;
    }

    int emuGetSpeed()
    {
        IntByReference speed = new IntByReference(100);
        mMupen64PlusLibrary.CoreDoCommand(m64p_command.M64CMD_CORE_STATE_QUERY.ordinal(), m64p_core_param.M64CORE_SPEED_FACTOR.ordinal(), speed.getPointer());
        return speed.getValue();
    }

    boolean emuGetFramelimiter()
    {
        IntByReference enabled = new IntByReference(1);
        mMupen64PlusLibrary.CoreDoCommand(m64p_command.M64CMD_CORE_STATE_QUERY.ordinal(), m64p_core_param.M64CORE_SPEED_LIMITER.ordinal(), enabled.getPointer());
        return enabled.getValue() == 1;
    }

    int emuGetSlot()
    {
        IntByReference slot = new IntByReference(1);
        mMupen64PlusLibrary.CoreDoCommand(m64p_command.M64CMD_CORE_STATE_QUERY.ordinal(), m64p_core_param.M64CORE_SAVESTATE_SLOT.ordinal(), slot.getPointer());
        return slot.getValue();
    }

    void emuReset()
    {
        IntByReference parameter = new IntByReference(0);
        mMupen64PlusLibrary.CoreDoCommand(m64p_command.M64CMD_RESET.ordinal(), 0, parameter.getPointer());
    }

    void setNativeWindow(Surface surface)
    {
        mAeVidExtLibrary.setNativeWindow(JNIEnv.CURRENT, surface);
    }

    void unsetNativeWindow()
    {
        mAeVidExtLibrary.unsetNativeWindow();
    }

    void emuDestroySurface()
    {
        mAeVidExtLibrary.emuDestroySurface();
    }

    void FPSEnabled(int recalc)
    {
        mAeVidExtLibrary.FPSEnabled(recalc);
    }
}
