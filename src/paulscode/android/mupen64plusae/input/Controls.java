package paulscode.android.mupen64plusae.input;

/**
 * Class which centralizes the control values.
 * </p>
 * These values are directly equal to what is found
 * in the plugin.h file within the input-sdl JNI project.
 */
public final class Controls
{
    // Must be the same order as EButton listing in plugin.h! (input-sdl plug-in)
    public static final int Right     = 0;
    public static final int Left      = 1;
    public static final int Down      = 2;
    public static final int Up        = 3;
    public static final int Start     = 4;
    public static final int Z         = 5;
    public static final int B         = 6;
    public static final int A         = 7;
    public static final int CRight    = 8;
    public static final int CLeft     = 9;
    public static final int CDown     = 10;
    public static final int CUp       = 11;
    public static final int R         = 12;
    public static final int L         = 13;
    
    // Non-standard mupen64plus buttons, but simulated here for better control:
    public static final int UpRight   = 14;
    public static final int RightDown = 15;
    public static final int LeftDown  = 16;
    public static final int LeftUp    = 17;
}
