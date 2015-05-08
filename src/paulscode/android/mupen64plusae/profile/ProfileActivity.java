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

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.compat.AppCompatPreferenceActivity;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.ConfigFile.ConfigSection;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;

/**
 * A base class for implementing simple list-based profile editor activities. This class is meant to
 * be subclassed the same way that a {@link PreferenceActivity} would be, and the process for
 * defining the structure and behavior is virtually identical. That is, the hierarchical menu
 * structure and default values are defined using an xml resource, using identical syntax to that
 * used for a <code>PreferenceActivity</code>. Similarly, the behavior of the activity (lifecycle,
 * inflation, menu visibility, change notifications, etc.) is implemented just as it would be for a
 * <code>PreferenceActivity</code>. It is thus trivial to migrate implementation between a
 * <code>PreferenceActivity</code> (backed by a {@link SharedPreferences} object) and a
 * <code>ProfileActivity</code> (backed by a {@link ConfigSection} object).
 * <p>
 * The one important caveat is that this class persists all settings as <code>String</code>s. This
 * means that some of the standard {@link Preference} classes must be extended a bit. For example,
 * the {@link CheckBoxPreference} class persists to <code>boolean</code>. Therefore, to make a
 * checkbox-style preference that works with <code>ProfileActivity</code>, extend
 * <code>CheckBoxPreference</code> and override the following methods:
 * 
 * <pre>
 * &#064;Override
 * protected boolean persistBoolean( boolean value )
 * {
 *     return persistString( value ? trueString : falseString );
 * }
 * 
 * &#064;Override
 * protected boolean getPersistedBoolean( boolean defaultReturnValue )
 * {
 *     if( getSharedPreferences().contains( getKey() ) )
 *     {
 *         String strValue = getPersistedString( defaultReturnValue ? trueString : falseString );
 *         return trueString.equals( strValue );
 *     }
 *     else
 *     {
 *         return defaultReturnValue;
 *     }
 * }
 * </pre>
 */
public abstract class ProfileActivity extends AppCompatPreferenceActivity implements
        OnSharedPreferenceChangeListener
{
    /**
     * Gets the XML resource that defines the preference hierarchy.
     * 
     * @return the XML resource to inflate
     */
    abstract protected int getPrefsResId();
    
    /**
     * Gets the absolute path of the backing {@link ConfigFile}.
     * 
     * @return the absolute path of the config file
     */
    abstract protected String getConfigFilePath();
    
    /**
     * Refreshes the UI. This method is called whenever the activity resumes and whenever a
     * preference value has changed.
     */
    abstract protected void refreshViews();
    
    // Working cache for preference data while activity is running
    protected SharedPreferences mPrefs = null;
    private static String PREFS_NAME = "tempProfileActivity";
    
    // Backing config file and profile name
    private ConfigFile mConfigFile;
    private String mProfileName;
    
    @SuppressWarnings( "deprecation" )
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        
        // Set locale
        new GlobalPrefs( this ).enforceLocale( this );
        
        // Load the profile; fail fast if there are any programmer usage errors
        Bundle extras = getIntent().getExtras();
        if( extras == null )
            throw new Error( "Invalid usage: bundle must indicate profile name" );
        mProfileName = extras.getString( ActivityHelper.Keys.PROFILE_NAME );
        if( TextUtils.isEmpty( mProfileName ) )
            throw new Error( "Invalid usage: profile name cannot be null or empty" );
        
        // Set the title of the activity
        setTitle( mProfileName );
        
        // Get the subclass-specific information
        final int resId = getPrefsResId();
        final String configPath = getConfigFilePath();
        
        // Load the config file and working cache
        mConfigFile = new ConfigFile( configPath );
        mPrefs = getSharedPreferences( PREFS_NAME, MODE_PRIVATE );
        transcribe( mConfigFile, mPrefs, mProfileName );
        
        // Populate any missing fields with defaults
        PreferenceManager.setDefaultValues( this, PREFS_NAME, MODE_PRIVATE, resId, false );
        
        // Load user preference menu structure from XML
        getPreferenceManager().setSharedPreferencesName( PREFS_NAME );
        addPreferencesFromResource( resId );
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        mPrefs.registerOnSharedPreferenceChangeListener( this );
        refreshViews();
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener( this );
        transcribe( mPrefs, mConfigFile, mProfileName );
    }
    
    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
    {
        refreshViews();
    }
    
    private static void transcribe( ConfigFile source, SharedPreferences target, String sectionName )
    {
        // Copy key-value data from config section to shared prefs object
        ConfigSection section = source.get( sectionName );
        if( section != null )
        {
            Editor editor = target.edit();
            editor.clear();
            for( String key : section.keySet() )
                editor.putString( key, section.get( key ) );
            editor.commit();
        }
    }
    
    private static void transcribe( SharedPreferences source, ConfigFile target, String sectionName )
    {
        // Copy key-value data from shared prefs object to config section
        target.remove( sectionName );
        for( String key : source.getAll().keySet() )
            target.put( sectionName, key, source.getString( key, null ) );
        target.save();
    }
}
