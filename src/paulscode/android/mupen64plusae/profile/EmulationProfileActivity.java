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
package paulscode.android.mupen64plusae.profile;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.preference.PrefUtil;
import paulscode.android.mupen64plusae.task.ExtractTexturesTask;
import paulscode.android.mupen64plusae.task.ExtractTexturesTask.ExtractTexturesListener;
import paulscode.android.mupen64plusae.util.Notifier;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.text.TextUtils;
import android.util.Log;

public class EmulationProfileActivity extends ProfileActivity
{
    // These constants must match the keys found in preferences_emulation.xml
    private static final String SCREEN_ROOT = "screenRoot";
    private static final String CATEGORY_RICE = "categoryRice";
    private static final String CATEGORY_GLN64 = "categoryGln64";
    private static final String CATEGORY_GLIDE64 = "categoryGlide64";
    private static final String CATEGORY_GLIDEN64_TEXTURE = "categoryGliden64Texture";
    private static final String CATEGORY_GLIDEN64_GENERAL = "categoryGliden64General";
    private static final String CATEGORY_GLIDEN64_FRAME_BUFFER = "categoryGliden64FrameBuffer";
    private static final String CATEGORY_GLIDEN64_TEXTURE_FILTERING = "categoryGliden64TextureFiltering";
    private static final String CATEGORY_GLIDEN64_BLOOM = "categoryGliden64Bloom";
    private static final String CATEGORY_GLIDEN64_GAMMA = "categoryGliden64Gamma";
    
    private static final String VIDEO_PLUGIN = "videoPlugin";
    private static final String VIDEO_SUB_PLUGIN = "videoSubPlugin";
    private static final String PATH_HI_RES_TEXTURES = "pathHiResTextures";
    private static final String GLIDEN64_MULTI_SAMPLING = "MultiSampling";
    private static final String GLIDEN64_ENABLE_LOD = "EnableLOD";
    private static final String GLIDEN64_ENABLE_SHADER_STORAGE = "EnableShaderStorage";
    private static final String GLIDEN64_ENABLE_COPY_DEPTH_TO_RDRAM = "EnableCopyDepthToRDRAM";
    private static final String GLIDEN64_ENABLE_N64_DEPTH_COMPARE = "EnableN64DepthCompare";
    private static final String GLIDEN64_ENABLE_FB_EMULATION = "EnableFBEmulation";
    private static final String GLIDEN64_WIDESCREEN_HACK = "WidescreenHack";
    private static final String GLIDEN64_FORCE_GAMMA_CORRECTION = "ForceGammaCorrection";
    private static final String GLIDEN64_GAMMA_CORRECTION_LEVEL = "GammaCorrectionLevel";
    
    // These constants must match the entry-values found in arrays.xml
    private static final String LIBGLIDE64_SO = "libmupen64plus-video-glide64mk2.so";
    private static final String LIBGLIDEN64_SO = "libmupen64plus-video-gliden64%1$s.so";
    private static final String LIBRICE_SO = "libmupen64plus-video-rice.so";
    private static final String LIBGLN64_SO = "libmupen64plus-video-gln64.so";
    private static final String GLES20 = "-gles20";
    private static final String GLES31 = "-gles31";
    
    // Preference menu items
    private PreferenceGroup mScreenRoot = null;
    private PreferenceCategory mCategoryN64 = null;
    private PreferenceCategory mCategoryRice = null;
    private PreferenceCategory mCategoryGlide64 = null;
    private PreferenceCategory mCategoryGliden64Texture = null;
    private PreferenceCategory mCategoryGliden64General = null;
    private PreferenceCategory mCategoryGliden64FrameBuffer = null;
    private PreferenceCategory mCategoryGliden64TextureFiltering = null;
    private PreferenceCategory mCategoryGliden64Bloom = null;
    private PreferenceCategory mCategoryGliden64Gamma = null;
    
    private Preference mPreferenceVideoSubPlugin = null;
    
    @Override
    protected int getPrefsResId()
    {
        return R.xml.profile_emulation;
    }
    
    @Override
    protected String getConfigFilePath()
    {
        AppData appData = new AppData( this );
        return new GlobalPrefs( this, appData ).emulationProfiles_cfg;
    }
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Ensure that selected plugin names and other list preferences are valid
        Resources res = getResources();
        PrefUtil.validateListPreference( res, mPrefs, VIDEO_PLUGIN, R.string.videoPlugin_default,
                R.array.videoPlugin_values );
    }
    
    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
    {
        if(key.equals("videoPlugin") || key.equals("videoSubPlugin"))
        {
            resetPreferences();
        }

        Preference pref = findPreference(key);
        
        if (pref instanceof EditTextPreference)
        {
            EditTextPreference editTextPref = (EditTextPreference) pref;
            pref.setSummary(editTextPref.getText());
        }

        super.onSharedPreferenceChanged( sharedPreferences, key );
        if( key.equals( PATH_HI_RES_TEXTURES ) )
            processTexturePak( sharedPreferences.getString( PATH_HI_RES_TEXTURES, "" ) );
    }

    @Override
    protected void refreshViews()
    {        
        // Get some menu items for use later
        mScreenRoot = (PreferenceGroup) findPreference( SCREEN_ROOT );
        mCategoryN64 = (PreferenceCategory) findPreference( CATEGORY_GLN64 );
        mCategoryRice = (PreferenceCategory) findPreference( CATEGORY_RICE );
        mCategoryGlide64 = (PreferenceCategory) findPreference( CATEGORY_GLIDE64 );
        mCategoryGliden64Texture = (PreferenceCategory) findPreference( CATEGORY_GLIDEN64_TEXTURE );
        mCategoryGliden64General = (PreferenceCategory) findPreference( CATEGORY_GLIDEN64_GENERAL );
        mCategoryGliden64FrameBuffer = (PreferenceCategory) findPreference( CATEGORY_GLIDEN64_FRAME_BUFFER );
        mCategoryGliden64TextureFiltering = (PreferenceCategory) findPreference( CATEGORY_GLIDEN64_TEXTURE_FILTERING );
        mCategoryGliden64Bloom = (PreferenceCategory) findPreference( CATEGORY_GLIDEN64_BLOOM );
        mCategoryGliden64Gamma = (PreferenceCategory) findPreference( CATEGORY_GLIDEN64_GAMMA );

        mPreferenceVideoSubPlugin = findPreference( VIDEO_SUB_PLUGIN );
                
        // Get the current values
        String videoPlugin = mPrefs.getString( VIDEO_PLUGIN, null );
        String videoSubPlugin = mPrefs.getString( VIDEO_SUB_PLUGIN, null );
        
        // Hide certain categories altogether if they're not applicable. Normally we just rely on
        // the built-in dependency disabler, but here the categories are so large that hiding them
        // provides a better user experience.
        
        if(mCategoryN64 != null)
        {
            if( LIBGLN64_SO.equals( videoPlugin ) )
            {
                mScreenRoot.addPreference( mCategoryN64 );
            }
            else
            {
                mScreenRoot.removePreference( mCategoryN64 );
            }
        }

        if(mCategoryRice != null)
        {
            if( LIBRICE_SO.equals( videoPlugin ) )
            {
                mScreenRoot.addPreference( mCategoryRice );
            }
            else
            {
                mScreenRoot.removePreference( mCategoryRice );
            }

        }

        if(mCategoryGlide64 != null)
        {
            if( LIBGLIDE64_SO.equals( videoPlugin ) )
            {
                mScreenRoot.addPreference( mCategoryGlide64 );
            }
            else
            {
                mScreenRoot.removePreference( mCategoryGlide64 );
            }
        }

        if(mCategoryGliden64Texture != null &&
            mCategoryGliden64General != null &&
            mCategoryGliden64FrameBuffer != null &&
            mCategoryGliden64TextureFiltering != null &&
            mCategoryGliden64Bloom != null &&
            mCategoryGliden64Gamma != null)
        {
            if( LIBGLIDEN64_SO.equals( videoPlugin ) )
            {
                mScreenRoot.addPreference( mCategoryGliden64Texture );
                mScreenRoot.addPreference( mCategoryGliden64General );
                mScreenRoot.addPreference( mCategoryGliden64FrameBuffer );
                mScreenRoot.addPreference( mCategoryGliden64TextureFiltering );
                mScreenRoot.addPreference( mCategoryGliden64Bloom );
                mScreenRoot.addPreference( mCategoryGliden64Gamma );

                boolean isGles20 = GLES20.equals( videoSubPlugin );
                boolean isGles31 = GLES31.equals( videoSubPlugin );
                findPreference( GLIDEN64_MULTI_SAMPLING ).setEnabled( isGles31 );
                findPreference( GLIDEN64_ENABLE_LOD ).setEnabled( !isGles20 );
                findPreference( GLIDEN64_ENABLE_SHADER_STORAGE ).setEnabled( !isGles20 );
                findPreference( GLIDEN64_ENABLE_COPY_DEPTH_TO_RDRAM ).setEnabled( !isGles20 );
                findPreference( GLIDEN64_ENABLE_N64_DEPTH_COMPARE ).setEnabled( isGles31 );
                
                String enableFBEmulation = mPrefs.getString( GLIDEN64_ENABLE_FB_EMULATION, null );
                findPreference( GLIDEN64_WIDESCREEN_HACK ).setEnabled( enableFBEmulation.equals("True") );
                
                String forceGammaCorrection = mPrefs.getString( GLIDEN64_FORCE_GAMMA_CORRECTION, null );
                findPreference( GLIDEN64_GAMMA_CORRECTION_LEVEL ).setEnabled( forceGammaCorrection.equals("True") );
            }
            else
            {

                mScreenRoot.removePreference( mCategoryGliden64Texture );
                mScreenRoot.removePreference( mCategoryGliden64General );
                mScreenRoot.removePreference( mCategoryGliden64FrameBuffer );
                mScreenRoot.removePreference( mCategoryGliden64TextureFiltering );
                mScreenRoot.removePreference( mCategoryGliden64Bloom );
                mScreenRoot.removePreference( mCategoryGliden64Gamma );
            }
        }
        
        if(mPreferenceVideoSubPlugin != null)
        {
            if( videoPlugin.contains( "%1$s" ) )
                mScreenRoot.addPreference( mPreferenceVideoSubPlugin );
            else
                mScreenRoot.removePreference( mPreferenceVideoSubPlugin );
        }
    }
    
    private void processTexturePak( final String filename )
    {
        if( TextUtils.isEmpty( filename ) )
        {
            Log.e( "EmulationProfileEditActivity", "Filename not specified in processTexturePak" );
            Notifier.showToast( this, R.string.pathHiResTexturesTask_errorMessage );
            return;
        }
        
        // Display popup window
        String title = getString( R.string.pathHiResTexturesTask_title );
        String message = getString( R.string.pathHiResTexturesTask_message );
        final AlertDialog dialog = new Builder( this ).setTitle( title ).setMessage( message ).create();
        dialog.show();
        
        // Asynchronously extract textures and dismiss popup
        AppData appData = new AppData( EmulationProfileActivity.this );
        GlobalPrefs globalPrefs = new GlobalPrefs( EmulationProfileActivity.this, appData );
        new ExtractTexturesTask( filename, globalPrefs.hiResTextureDir, new ExtractTexturesListener()
        {
            @Override
            public void onExtractTexturesFinished( boolean success )
            {
                dialog.dismiss();
                if( !success )
                    Notifier.showToast( EmulationProfileActivity.this,
                            R.string.pathHiResTexturesTask_errorMessage );
            }
        } ).execute();
    }
}
