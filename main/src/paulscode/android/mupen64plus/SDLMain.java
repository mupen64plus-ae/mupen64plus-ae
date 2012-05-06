package paulscode.android.mupen64plus;


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
