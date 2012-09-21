package paulscode.android.mupen64plusae;


/**
 * Simple nativeInit() runnable
 */
class SDLMain implements Runnable
{
    public void run()
    {
        // Runs SDL_main()
        GameActivityCommon.nativeInit();
    }
}
