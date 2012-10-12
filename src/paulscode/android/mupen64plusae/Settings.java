package paulscode.android.mupen64plusae;

import android.content.SharedPreferences;

public class Settings
{
    public final SharedPreferences sharedPreferences;
    
    public final String language;
    public final boolean autoSaveEnabled;
    
    public final boolean videoEnabled;
    public final String videoPlugin;
    public final boolean videoStretch;
    public final boolean videoRGBA8888;
    
    public final boolean touchscreenEnabled;
    public final String touchscreenLayout;
    public final boolean touchscreenFrameRate;
    public final boolean touchscreenOctagonJoystick;
    public final boolean touchscreenRedrawAll;
    
    public final boolean gamepadEnabled;
    public final String gamepadPlugin;
    
    public final boolean xperiaEnabled;
    public final String xperiaLayout;
    
    public final String audioPlugin;
    public final String rspPlugin;
    public final String corePlugin;
    
    public Settings(SharedPreferences preferences)
    {
        sharedPreferences = preferences;
        
        language = sharedPreferences.getString( "language", "0" );
        autoSaveEnabled = sharedPreferences.getBoolean( "autoSaveEnabled", true );
        
        videoEnabled = sharedPreferences.getBoolean( "videoEnabled", true );
        videoPlugin = sharedPreferences.getString( "videoPlugin", "" );
        videoStretch = sharedPreferences.getBoolean( "videoStretch", false );
        videoRGBA8888 = sharedPreferences.getBoolean( "videoRGBA8888", false );
        
        touchscreenEnabled = sharedPreferences.getBoolean( "touchscreenEnabled", true );
        touchscreenLayout = sharedPreferences.getString( "touchscreenLayout", "" );
        touchscreenFrameRate = sharedPreferences.getBoolean( "touchscreenFrameRate", false );
        touchscreenOctagonJoystick = sharedPreferences.getBoolean( "touchscreenOctagonJoystick", true );
        touchscreenRedrawAll = sharedPreferences.getBoolean( "touchscreenRedrawAll", false );
        
        gamepadEnabled = sharedPreferences.getBoolean( "gamepadEnabled", true );
        gamepadPlugin = sharedPreferences.getString( "gamepadPlugin", "" );
        
        xperiaEnabled = sharedPreferences.getBoolean( "xperiaEnabled", false );
        xperiaLayout = sharedPreferences.getString( "xperiaLayout", "" );

        audioPlugin = sharedPreferences.getString( "audioPlugin", "" );
        rspPlugin = sharedPreferences.getString( "rspPlugin", "" );
        corePlugin = sharedPreferences.getString( "corePlugin", "" );
    }
}
