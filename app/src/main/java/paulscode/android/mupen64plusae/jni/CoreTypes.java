package paulscode.android.mupen64plusae.jni;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import org.mupen64plusae.v3.alpha.R;

@SuppressWarnings({"WeakerAccess", "unused"})
public class CoreTypes {

    //Pak Type
    public enum PakType {
        DUMMY(0),
        NONE(R.string.menuItem_pak_empty),
        MEMORY(R.string.menuItem_pak_mem),
        RAMBLE(R.string.menuItem_pak_rumble),
        TRANSFER(R.string.menuItem_pak_transfer),
        RAW(0),
        BIO(0);

        private final int mResourceStringName;

        PakType(int resourceStringName)
        {
            mResourceStringName = resourceStringName;
        }

        public static PakType getPakTypeFromNativeValue(int nativeValue)
        {
            return nativeValue < PakType.values().length ? PakType.values()[nativeValue] : NONE;
        }

        public int getResourceString()
        {
            return mResourceStringName;
        }
    }

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
        M64CMD_SET_MEDIA_LOADER,
        M64CMD_SET_RESOLUTION_RESET,
        M64CMD_NETPLAY_INIT,
        M64CMD_NETPLAY_CONTROL_PLAYER,
        M64CMD_NETPLAY_GET_VERSION,
        M64CMD_NETPLAY_CLOSE
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
        M64EMU_UNKNOWN,
        M64EMU_STOPPED,
        M64EMU_RUNNING,
        M64EMU_PAUSED;

        public static m64p_emu_state getState(int ordinal)
        {
            return ordinal < m64p_emu_state.values().length ? m64p_emu_state.values()[ordinal] : M64EMU_UNKNOWN;
        }
    }

    @Structure.FieldOrder({ "address", "value" })
    public static class m64p_cheat_code extends Structure
    {
        public int address;
        public int value;

        public m64p_cheat_code() {
            address = 0;
            value = 0;
        }
    }

    @Structure.FieldOrder({ "cb_data", "gbCartRomCallback", "gbCartRamCallback", "ddRomCallback", "ddDiskCallback" })
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
}
