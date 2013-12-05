/**
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
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae;

import java.io.File;

import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.ErrorLogger;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.PrefUtil;
import paulscode.android.mupen64plusae.util.TaskHandler;
import paulscode.android.mupen64plusae.util.Utility;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class VideoMenuActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener
{
    // These constants must match the keys used in res/xml/preferences.xml
    
    private static final String SCREEN_VIDEO = "screenVideo";
    
    private static final String CATEGORY_GLES2_RICE = "categoryGles2Rice";
    private static final String CATEGORY_GLES2_N64 = "categoryGles2N64";
    private static final String CATEGORY_GLES2_GLIDE64 = "categoryGles2Glide64";
    
    private static final String VIDEO_PLUGIN = "videoPlugin";
    private static final String VIDEO_POLYGON_OFFSET = "videoPolygonOffset";
    private static final String PATH_HI_RES_TEXTURES = "pathHiResTextures";
    
    // User preferences
    private UserPrefs mUserPrefs = null;
    
    @SuppressWarnings( "deprecation" )
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Get user preferences
        mUserPrefs = new UserPrefs( this );
        mUserPrefs.enforceLocale( this );
        
        // Ensure that any missing preferences are populated with defaults (e.g. preference added to
        // new release)
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
        PreferenceManager.setDefaultValues( this, R.xml.preferences, false );
        
        // Ensure that selected plugin names and other list preferences are valid
        Resources res = getResources();
        PrefUtil.validateListPreference( res, prefs, VIDEO_PLUGIN, R.string.videoPlugin_default,
                R.array.videoPlugin_values );
        
        // Load user preference menu structure from XML and update view
        addPreferencesFromResource( R.xml.preferences_video );
        
        // Refresh the preference data wrapper
        mUserPrefs = new UserPrefs( this );
        
        // Hide certain categories altogether if they're not applicable. Normally we just rely on
        // the built-in dependency disabler, but here the categories are so large that hiding them
        // provides a better user experience.
        if( !mUserPrefs.isGles2N64Enabled )
            PrefUtil.removePreference( this, SCREEN_VIDEO, CATEGORY_GLES2_N64 );
        
        if( !mUserPrefs.isGles2RiceEnabled )
            PrefUtil.removePreference( this, SCREEN_VIDEO, CATEGORY_GLES2_RICE );
        
        if( !mUserPrefs.isGles2Glide64Enabled )
            PrefUtil.removePreference( this, SCREEN_VIDEO, CATEGORY_GLES2_GLIDE64 );
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences( this );
        sharedPreferences.unregisterOnSharedPreferenceChangeListener( this );
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences( this );
        sharedPreferences.registerOnSharedPreferenceChangeListener( this );
        refreshViews();
    }
    
    @Override
    public void finish()
    {
        // Disable transition animation to behave like any other screen in the menu hierarchy
        super.finish();
        overridePendingTransition( 0, 0 );
    }
    
    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
    {
        if( key.equals( VIDEO_PLUGIN ) )
        {
            // Rebuild the menu; the easiest way is to simply restart the activity
            finish();
            startActivity( getIntent() );
        }
        else if( key.equals( PATH_HI_RES_TEXTURES ) )
        {
            processTexturePak( sharedPreferences.getString( PATH_HI_RES_TEXTURES, "" ) );
        }
        else
        {
            // Just refresh the preference screens in place
            refreshViews();
        }
    }
    
    private void refreshViews()
    {
        // Refresh the preferences object
        mUserPrefs = new UserPrefs( this );
        
        // Enable the custom hardware profile prefs only when custom video hardware type is
        // selected
        PrefUtil.enablePreference( this, VIDEO_POLYGON_OFFSET, mUserPrefs.videoHardwareType == 999 );
    }
    
    private void processTexturePak( final String filename )
    {
        if( TextUtils.isEmpty( filename ) )
        {
            ErrorLogger.put( "Video", "pathHiResTextures",
                    "Filename not specified in MenuActivity.processTexturePak" );
            Notifier.showToast( this, R.string.pathHiResTexturesTask_errorMessage );
            return;
        }
        
        TaskHandler.Task task = new TaskHandler.Task()
        {
            @Override
            public void run()
            {
                String headerName = Utility.getTexturePackName( filename );
                if( !ErrorLogger.hasError() )
                {
                    if( TextUtils.isEmpty( headerName ) )
                    {
                        ErrorLogger
                                .setLastError( "getTexturePackName returned null in MenuActivity.processTexturePak" );
                        ErrorLogger.putLastError( "Video", "pathHiResTextures" );
                    }
                    else
                    {
                        String outputFolder = mUserPrefs.hiResTextureDir + headerName;
                        FileUtil.deleteFolder( new File( outputFolder ) );
                        Utility.unzipAll( new File( filename ), outputFolder );
                    }
                }
            }
            
            @Override
            public void onComplete()
            {
                if( ErrorLogger.hasError() )
                    Notifier.showToast( VideoMenuActivity.this,
                            R.string.pathHiResTexturesTask_errorMessage );
                ErrorLogger.clearLastError();
            }
        };
        
        String title = getString( R.string.pathHiResTexturesTask_title );
        String message = getString( R.string.pathHiResTexturesTask_message );
        TaskHandler.run( this, title, message, task );
    }
}
