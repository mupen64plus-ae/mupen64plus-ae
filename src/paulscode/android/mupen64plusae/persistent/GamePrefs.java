package paulscode.android.mupen64plusae.persistent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.mupen64plusae.v3.alpha.R;
import paulscode.android.mupen64plusae.input.map.PlayerMap;
import paulscode.android.mupen64plusae.profile.ControllerProfile;
import paulscode.android.mupen64plusae.profile.Profile;
import paulscode.android.mupen64plusae.util.Plugin;
import paulscode.android.mupen64plusae.util.Utility;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.text.TextUtils;
import android.util.DisplayMetrics;

public class GamePrefs
{
    /** The name of the game-specific {@link SharedPreferences} object.*/
    public final String sharedPrefsName;
    
    /** The emulation profile. */
    public final Profile emulationProfile;
    
    /** The touchscreen profile. */
    public final Profile touchscreenProfile;
    
    /** The input profile for Player 1. */
    public final ControllerProfile controllerProfile1;
    
    /** The input profile for Player 2. */
    public final ControllerProfile controllerProfile2;
    
    /** The input profile for Player 3. */
    public final ControllerProfile controllerProfile3;
    
    /** The input profile for Player 4. */
    public final ControllerProfile controllerProfile4;
    
    /** The player map for multi-player gaming. */
    public final PlayerMap playerMap;
    
    /** True if the cheats category should be shown in the menu. */
    public final boolean isCheatOptionsShown;
    
    /** The selected R4300 emulator. */
    public final String r4300Emulator;
    
    /** The selected video plug-in. */
    public final Plugin videoPlugin;
    
    /** True if gln64 video plug-in is enabled. */
    public final boolean isGln64Enabled;
    
    /** True if rice video plug-in is enabled. */
    public final boolean isRiceEnabled;
    
    /** True if glide64 video plug-in is enabled. */
    public final boolean isGlide64Enabled;
    
    /** The maximum frameskip in the gln64 library. */
    public final int gln64MaxFrameskip;
    
    /** True if auto-frameskip is enabled in the gln64 library. */
    public final boolean isGln64AutoFrameskipEnabled;
    
    /** True if fog is enabled in the gln64 library. */
    public final boolean isGln64FogEnabled;
    
    /** True if SaI texture filtering is enabled in the gln64 library. */
    public final boolean isGln64SaiEnabled;
    
    /** True if force screen clear is enabled in the gln64 library. */
    public final boolean isGln64ScreenClearEnabled;
    
    /** True if alpha test is enabled in the gln64 library. */
    public final boolean isGln64AlphaTestEnabled;
    
    /** True if depth coordinates hack is enabled in the gln64 library. */
    public final boolean isGln64HackDepthEnabled;
    
    /** True if auto-frameskip is enabled in the rice library. */
    public final boolean isRiceAutoFrameskipEnabled;
    
    /** True if fast texture loading is enabled in the rice library. */
    public final boolean isRiceFastTextureLoadingEnabled;
    
    /** True if force texture filter is enabled in the rice library. */
    public final boolean isRiceForceTextureFilterEnabled;
    
    /** The screen update setting to use in rice */
    public final String riceScreenUpdateType;
    
    /** The texture enhancement algorithm to be used in the rice library */
    public final String riceTextureEnhancement;
    
    /** True if hi-resolution textures are enabled in the rice library. */
    public final boolean isRiceHiResTexturesEnabled;
    
    /** True if fog is enabled in the rice library. */
    public final boolean isRiceFogEnabled;
    
    /** The maximum frameskip in the glide64 library. */
    public final int glide64MaxFrameskip;
    
    /** True if auto-frameskip is enabled in the glide64 library. */
    public final boolean isGlide64AutoFrameskipEnabled;
    
    /** True if the touchscreen is enabled. */
    public final boolean isTouchscreenEnabled;
    
    /** The set of auto-holdable button commands. */
    public final Set<Integer> touchscreenAutoHoldables;
    
    /** True if the touchscreen overlay is hidden. */
    public final boolean isTouchscreenHidden;
    
    /** The folder name of the selected touchscreen layout. */
    public final String touchscreenLayout;
    
    /** True if a custom touchscreen is provided. */
    public final boolean isTouchscreenCustom;
    
    /** True if Player 1's controller is enabled. */
    public final boolean isControllerEnabled1;
    
    /** True if Player 2's controller is enabled. */
    public final boolean isControllerEnabled2;
    
    /** True if Player 3's controller is enabled. */
    public final boolean isControllerEnabled3;
    
    /** True if Player 4's controller is enabled. */
    public final boolean isControllerEnabled4;
    
    /** True if any type of AbstractController is enabled for Player 1. */
    public final boolean isPlugged1;
    
    /** True if any type of AbstractController is enabled for Player 2. */
    public final boolean isPlugged2;
    
    /** True if any type of AbstractController is enabled for Player 3. */
    public final boolean isPlugged3;
    
    /** True if any type of AbstractController is enabled for Player 4. */
    public final boolean isPlugged4;
    
    private final SharedPreferences mPreferences;
    
    public GamePrefs( Context context, String romMd5 )
    {
        final AppData appData = new AppData( context );
        final UserPrefs userPrefs = new UserPrefs( context );
        
        sharedPrefsName = romMd5.replace(' ', '_' ) + "_preferences";
        mPreferences = context.getSharedPreferences( sharedPrefsName, Context.MODE_PRIVATE );
        
        // Emulation profile
        emulationProfile = loadProfile( mPreferences, "emulationProfile",
                UserPrefs.DEFAULT_EMULATION_PROFILE_DEFAULT,
                userPrefs.emulationProfiles_cfg, appData.emulationProfiles_cfg );
        
        // Touchscreen profile
        touchscreenProfile = loadProfile( mPreferences, "touchscreenProfile",
                UserPrefs.DEFAULT_TOUCHSCREEN_PROFILE_DEFAULT,
                userPrefs.touchscreenProfiles_cfg, appData.touchscreenProfiles_cfg );
        
        // Controller profiles
        controllerProfile1 = loadControllerProfile( mPreferences, "controllerProfile1",
                UserPrefs.DEFAULT_CONTROLLER_PROFILE_DEFAULT,
                userPrefs.controllerProfiles_cfg, appData.controllerProfiles_cfg );
        controllerProfile2 = loadControllerProfile( mPreferences, "controllerProfile2",
                "",
                userPrefs.controllerProfiles_cfg, appData.controllerProfiles_cfg );
        controllerProfile3 = loadControllerProfile( mPreferences, "controllerProfile3",
                "",
                userPrefs.controllerProfiles_cfg, appData.controllerProfiles_cfg );
        controllerProfile4 = loadControllerProfile( mPreferences, "controllerProfile4",
                "",
                userPrefs.controllerProfiles_cfg, appData.controllerProfiles_cfg );
        
        // Player map
        playerMap = new PlayerMap( mPreferences.getString( "playerMap", "" ) );
        
        // Cheats menu
        isCheatOptionsShown = mPreferences.getBoolean( "playShowCheats", false );
        
        // Emulation prefs
        r4300Emulator = emulationProfile.get( "r4300Emulator", "2" );
        videoPlugin = new Plugin( emulationProfile, appData.libsDir, "videoPlugin" );
        
        // Video prefs - gln64
        isGln64Enabled = videoPlugin.name.equals( "libmupen64plus-video-gln64.so" );
        int maxFrameskip = getSafeInt( emulationProfile, "gln64Frameskip", 0 );
        isGln64AutoFrameskipEnabled = maxFrameskip < 0;
        gln64MaxFrameskip = Math.abs( maxFrameskip );
        isGln64FogEnabled = emulationProfile.get( "gln64Fog", "0" ).equals( "1" );
        isGln64SaiEnabled = emulationProfile.get( "gln64Sai", "0" ).equals( "1" );
        isGln64ScreenClearEnabled = emulationProfile.get( "gln64ScreenClear", "1" ).equals( "1" );
        isGln64AlphaTestEnabled = emulationProfile.get( "gln64AlphaTest", "1" ).equals( "1" );
        isGln64HackDepthEnabled = emulationProfile.get( "gln64HackDepth", "1" ).equals( "1" );
        
        // Video prefs - rice
        isRiceEnabled = videoPlugin.name.equals( "libmupen64plus-video-rice.so" );
        isRiceAutoFrameskipEnabled = emulationProfile.get( "riceAutoFrameskip", "False" ).equals( "True" );
        isRiceFastTextureLoadingEnabled = emulationProfile.get( "riceFastTexture", "False" ).equals( "True" );
        isRiceForceTextureFilterEnabled = emulationProfile.get( "riceForceTextureFilter", "False" ).equals( "True" );
        riceScreenUpdateType = emulationProfile.get( "riceScreenUpdate", "4" );
        riceTextureEnhancement = emulationProfile.get( "riceTextureEnhancement", "0" );
        isRiceHiResTexturesEnabled = emulationProfile.get( "riceHiResTextures", "True" ).equals( "True" );
        isRiceFogEnabled = emulationProfile.get( "riceFog", "False" ).equals( "True" );
        
        // Video prefs - glide64
        isGlide64Enabled = videoPlugin.name.equals( "libmupen64plus-video-glide64mk2.so" );
        maxFrameskip = getSafeInt( emulationProfile, "glide64Frameskip", 0 );
        isGlide64AutoFrameskipEnabled = maxFrameskip < 0;
        glide64MaxFrameskip = Math.abs( maxFrameskip );
        
        // Touchscreen prefs
        isTouchscreenEnabled = touchscreenProfile != null;
        
        // Determine the touchscreen layout
        boolean isCustom = false;
        String folder = "";
        if( isTouchscreenEnabled )
        {
            touchscreenAutoHoldables = getSafeIntSet( touchscreenProfile,
                    "touchscreenAutoHoldables" );
            
            String layout = touchscreenProfile.get( "touchscreenLayout", "" );
            if( layout.equals( "Custom" ) )
            {
                isCustom = true;
                folder = touchscreenProfile.get( "pathCustomTouchscreen", "" );
            }
            else
            {
                // Use the "No-stick" skin if analog input is shown but stick ("hat") is not
                // animated
                if( layout.equals( "Mupen64Plus-AE-Analog" )
                        || layout.equals( "Mupen64Plus-AE-All" ) )
                {
                    if( userPrefs.touchscreenRefresh == 0 )
                        layout += "-Nostick";
                    else
                        layout += "-Stick";
                }
                
                String height = touchscreenProfile.get( "touchscreenHeight", "" );
                if( TextUtils.isEmpty( height ) )
                {
                    // Use the "Tablet" skin if the device is a tablet or is in portrait orientation
                    if( context instanceof Activity )
                    {
                        DisplayMetrics metrics = new DisplayMetrics();
                        ( (Activity) context ).getWindowManager().getDefaultDisplay()
                                .getMetrics( metrics );
                        float screenWidthInches = (float) metrics.widthPixels
                                / (float) metrics.xdpi;
                        float screenHeightInches = (float) metrics.heightPixels
                                / (float) metrics.ydpi;
                        float screenSizeInches = (float) Math
                                .sqrt( ( screenWidthInches * screenWidthInches )
                                        + ( screenHeightInches * screenHeightInches ) );
                        if( screenSizeInches >= Utility.MINIMUM_TABLET_SIZE
                                || userPrefs.displayOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                || userPrefs.displayOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT )
                        {
                            layout += "-Half-Height";
                        }
                        else
                        {
                            layout += "-Full-Height";
                        }
                    }
                }
                else
                {
                    layout += height;
                }
                
                folder = appData.touchscreenSkinsDir + layout;
            }
        }
        else
        {
            // Touchscreen disabled, profile is null
            if( userPrefs.isFpsEnabled )
            {
                folder = appData.touchscreenSkinsDir
                        + context.getString( R.string.touchscreenLayout_fpsOnly );
            }
            touchscreenAutoHoldables = null;
        }
        isTouchscreenHidden = !isTouchscreenEnabled || userPrefs.touchscreenTransparency == 0;
        isTouchscreenCustom = isCustom;
        touchscreenLayout = folder;
        
        // Determine which peripheral controllers are enabled
        isControllerEnabled1 = controllerProfile1 != null;
        isControllerEnabled2 = controllerProfile2 != null;
        isControllerEnabled3 = controllerProfile3 != null;
        isControllerEnabled4 = controllerProfile4 != null;
        
        // Determine whether controller deconfliction is needed
        int numControllers = 0;
        numControllers += isControllerEnabled1 ? 1 : 0;
        numControllers += isControllerEnabled2 ? 1 : 0;
        numControllers += isControllerEnabled3 ? 1 : 0;
        numControllers += isControllerEnabled4 ? 1 : 0;
        playerMap.setEnabled( numControllers > 1 && !userPrefs.isControllerShared );
        
        // Determine which players are "plugged in"
        isPlugged1 = isControllerEnabled1 || isTouchscreenEnabled || userPrefs.isTouchpadEnabled;
        isPlugged2 = isControllerEnabled2;
        isPlugged3 = isControllerEnabled3;
        isPlugged4 = isControllerEnabled4;
    }
    
    private static Profile loadProfile( SharedPreferences prefs, String key, String defaultName,
            String customPath, String builtinPath )
    {
        final ConfigFile custom = new ConfigFile( customPath );
        final ConfigFile builtin = new ConfigFile( builtinPath );
        final String name = prefs.getString( key, defaultName );
        
        if( custom.keySet().contains( name ) )
            return new Profile( false, custom.get( name ) );
        else if( builtin.keySet().contains( name ) )
            return new Profile( true, builtin.get( name ) );
        else if( builtin.keySet().contains( defaultName ) )
            return new Profile( true, builtin.get( defaultName ) );
        else
            return null;
    }
    
    private static ControllerProfile loadControllerProfile( SharedPreferences prefs, String key,
            String defaultName, String customPath, String builtinPath )
    {
        final ConfigFile custom = new ConfigFile( customPath );
        final ConfigFile builtin = new ConfigFile( builtinPath );
        final String name = prefs.getString( key, defaultName );
        
        if( custom.keySet().contains( name ) )
            return new ControllerProfile( false, custom.get( name ) );
        else if( builtin.keySet().contains( name ) )
            return new ControllerProfile( true, builtin.get( name ) );
        else if( builtin.keySet().contains( defaultName ) )
            return new ControllerProfile( true, builtin.get( defaultName ) );
        else
            return null;
    }
    
    private static int getSafeInt( Profile profile, String key, int defaultValue )
    {
        try
        {
            return Integer.parseInt( profile.get( key, String.valueOf( defaultValue ) ) );
        }
        catch( NumberFormatException ex )
        {
            return defaultValue;
        }
    }
    
    private static Set<Integer> getSafeIntSet( Profile profile, String key )
    {
        Set<Integer> mutableSet = new HashSet<Integer>();
        String elements = profile.get( key, "" );
        for( String element : MultiSelectListPreference.deserialize( elements ) )
        {
            try
            {
                mutableSet.add( Integer.valueOf( element ) );
            }
            catch( NumberFormatException ignored )
            {
            }
        }
        return Collections.unmodifiableSet( mutableSet );
    }
}
