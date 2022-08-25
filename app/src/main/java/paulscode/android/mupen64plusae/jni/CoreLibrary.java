package paulscode.android.mupen64plusae.jni;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Pointer;

/**
 * Core library
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface CoreLibrary extends Library {

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
    int CoreAddCheat(String CheatName, CoreTypes.m64p_cheat_code[] CodeList, int NumCodes);

    /* CoreCheatEnabled()
     *
     * This function will enable or disable a Cheat Function which is in the list of
     * currently active cheats.
     */
    int CoreCheatEnabled(String CheatName, int Enabled);
}