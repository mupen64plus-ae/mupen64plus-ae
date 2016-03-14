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
 * Authors: Paul Lamb
 */
package paulscode.android.mupen64plusae.persistent;

import java.io.File;
import java.util.ArrayList;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.cheat.CheatEditorActivity;
import paulscode.android.mupen64plusae.cheat.CheatPreference;
import paulscode.android.mupen64plusae.cheat.CheatUtils.Cheat;
import paulscode.android.mupen64plusae.compat.AppCompatPreferenceActivity;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog.PromptConfirmListener;
import paulscode.android.mupen64plusae.dialog.PromptInputCodeDialog.PromptInputCodeListener;
import paulscode.android.mupen64plusae.hack.MogaHack;
import paulscode.android.mupen64plusae.preference.PlayerMapPreference;
import paulscode.android.mupen64plusae.preference.PrefUtil;
import paulscode.android.mupen64plusae.preference.ProfilePreference;
import paulscode.android.mupen64plusae.profile.Profile;
import paulscode.android.mupen64plusae.task.ExtractCheatsTask;
import paulscode.android.mupen64plusae.task.ExtractCheatsTask.ExtractCheatListener;
import paulscode.android.mupen64plusae.util.RomDatabase;
import paulscode.android.mupen64plusae.util.RomDatabase.RomDetail;
import paulscode.android.mupen64plusae.util.RomHeader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import com.bda.controller.Controller;


public class GamePrefsActivity extends AppCompatPreferenceActivity implements OnPreferenceClickListener,
        OnSharedPreferenceChangeListener, ExtractCheatListener, PromptInputCodeListener, PromptConfirmListener
{
    private static final int RESET_GAME_PREFS_CONFIRM_DIALOG_ID = 0;
    private static final String RESET_GAME_PREFS_CONFIRM_DIALOG_STATE = "RESET_GAME_PREFS_CONFIRM_DIALOG_STATE";

    // These constants must match the keys used in res/xml/preferences_play.xml
    private static final String SCREEN_ROOT = "screenRoot";
    private static final String SCREEN_CHEATS = "screenCheats";
    private static final String CATEGORY_CHEATS = "categoryCheats";

    private static final String ACTION_CHEAT_EDITOR = "actionCheatEditor";
    private static final String ACTION_WIKI = "actionWiki";
    private static final String ACTION_RESET_GAME_PREFS = "actionResetGamePrefs";

    // App data and user preferences
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;
    private GamePrefs mGamePrefs = null;
    private SharedPreferences mPrefs = null;

    // ROM info
    private String mRomPath = null;
    private String mRomMd5 = null;
    private String mRomCrc = null;
    private String mRomHeaderName = null;
    private byte mRomCountryCode = 0;
    private RomDatabase mRomDatabase = null;
    private RomDetail mRomDetail = null;

    // Preference menu items
    private ProfilePreference mEmulationProfile = null;
    private ProfilePreference mTouchscreenProfile = null;
    private ProfilePreference mControllerProfile1 = null;
    private ProfilePreference mControllerProfile2 = null;
    private ProfilePreference mControllerProfile3 = null;
    private ProfilePreference mControllerProfile4 = null;
    private PreferenceScreen mScreenCheats = null;
    private PreferenceGroup mCategoryCheats = null;

    private boolean mClearCheats = false;

    // MOGA controller interface
    private final Controller mMogaController = Controller.getInstance( this );

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        // Get the ROM path and MD5 that was passed to the activity
        final Bundle extras = getIntent().getExtras();
        if( extras == null )
            throw new Error( "ROM path and MD5 must be passed via the extras bundle" );
        mRomPath = extras.getString( ActivityHelper.Keys.ROM_PATH );
        mRomMd5 = extras.getString( ActivityHelper.Keys.ROM_MD5 );
        mRomCrc = extras.getString( ActivityHelper.Keys.ROM_CRC );
        mRomHeaderName = extras.getString( ActivityHelper.Keys.ROM_HEADER_NAME );
        mRomCountryCode = extras.getByte( ActivityHelper.Keys.ROM_COUNTRY_CODE );

        if( TextUtils.isEmpty( mRomMd5 ) )
            throw new Error( "MD5 must be passed via the extras bundle" );

        // Initialize MOGA controller API
        // TODO: Remove hack after MOGA SDK is fixed
        // mMogaController.init();
        MogaHack.init( mMogaController, this );

        // Get app data and user preferences
        mAppData = new AppData( this );
        mGlobalPrefs = new GlobalPrefs( this, mAppData );
        mGamePrefs = new GamePrefs( this, mRomMd5, mRomCrc, mRomHeaderName,
            RomHeader.countryCodeToSymbol(mRomCountryCode), mAppData, mGlobalPrefs );
        mGlobalPrefs.enforceLocale( this );
        mPrefs = getSharedPreferences( mGamePrefs.sharedPrefsName, MODE_PRIVATE );

        // Get the detailed info about the ROM
        mRomDatabase = RomDatabase.getInstance();

        if(!mRomDatabase.hasDatabaseFile())
        {
            mRomDatabase.setDatabaseFile(mAppData.mupen64plus_ini);
        }

        mRomDetail = mRomDatabase.lookupByMd5WithFallback( mRomMd5, new File( mRomPath ), mRomCrc );

        // Load user preference menu structure from XML and update view
        addPreferencesFromResource( mGamePrefs.sharedPrefsName, R.xml.preferences_game );
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        mPrefs.registerOnSharedPreferenceChangeListener( this );
        mMogaController.onResume();
    }

    protected void updateActivity()
    {
        mEmulationProfile = (ProfilePreference) findPreference( GamePrefs.EMULATION_PROFILE );
        mTouchscreenProfile = (ProfilePreference) findPreference( GamePrefs.TOUCHSCREEN_PROFILE );
        mControllerProfile1 = (ProfilePreference) findPreference( GamePrefs.CONTROLLER_PROFILE1 );
        mControllerProfile2 = (ProfilePreference) findPreference( GamePrefs.CONTROLLER_PROFILE2 );
        mControllerProfile3 = (ProfilePreference) findPreference( GamePrefs.CONTROLLER_PROFILE3 );
        mControllerProfile4 = (ProfilePreference) findPreference( GamePrefs.CONTROLLER_PROFILE4 );

        // Set some game-specific strings
        setTitle( mRomDetail.goodName );

        // Handle certain menu items that require extra processing or aren't actually preferences
        PrefUtil.setOnPreferenceClickListener( this, ACTION_WIKI, this );
        PrefUtil.setOnPreferenceClickListener( this, ACTION_RESET_GAME_PREFS, this );

        // Remove wiki menu item if not applicable
        if( TextUtils.isEmpty( mRomDetail.wikiUrl ) )
        {
            PrefUtil.removePreference( this, SCREEN_ROOT, ACTION_WIKI );
        }

        // Setup controller profiles settings based on ROM's number of players
        if( mRomDetail.players == 1 )
        {
            // Simplify name of "controller 1" to just "controller" to eliminate confusion
            final Preference player1Pref = findPreference( GamePrefs.CONTROLLER_PROFILE1 );

            ///This can be null if we are at preference sub screen
            if(player1Pref != null)
            {
                player1Pref.setTitle( R.string.controllerProfile_title );
            }

            // Remove unneeded preference items
            PrefUtil.removePreference( this, SCREEN_ROOT, GamePrefs.CONTROLLER_PROFILE2 );
            PrefUtil.removePreference( this, SCREEN_ROOT, GamePrefs.CONTROLLER_PROFILE3 );
            PrefUtil.removePreference( this, SCREEN_ROOT, GamePrefs.CONTROLLER_PROFILE4 );
            PrefUtil.removePreference( this, SCREEN_ROOT, GamePrefs.PLAYER_MAP );
        }
        else
        {
            // Remove unneeded preference items
            if( mRomDetail.players < 4 )
                PrefUtil.removePreference( this, SCREEN_ROOT, GamePrefs.CONTROLLER_PROFILE4 );
            if( mRomDetail.players < 3 )
                PrefUtil.removePreference( this, SCREEN_ROOT, GamePrefs.CONTROLLER_PROFILE3 );
            if( mRomDetail.players < 2 )
                PrefUtil.removePreference( this, SCREEN_ROOT, GamePrefs.CONTROLLER_PROFILE2 );
        }

        //Remove touch screen profile if TV mode
        if(mGlobalPrefs.isBigScreenMode)
        {
            PrefUtil.removePreference( this, SCREEN_ROOT, GamePrefs.TOUCHSCREEN_PROFILE);
        }

        refreshViews();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener( this );
        mMogaController.onPause();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mMogaController.exit();
    }

    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
    {
        refreshViews();
        if( key.equals( GamePrefs.PLAY_SHOW_CHEATS ) )
        {
            refreshCheatsCategory();
        }
    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data )
    {
        if( requestCode == 111 )
        {
            if( resultCode == RESULT_OK)
            {
                //If the user cheats were saved, reset all selected cheatd
                mClearCheats = true;
                refreshCheatsCategory();
            }
        }
    }

    private void refreshViews()
    {
        mPrefs.unregisterOnSharedPreferenceChangeListener( this );

        // Refresh the preferences objects
        mGlobalPrefs = new GlobalPrefs( this, mAppData );
        mGamePrefs = new GamePrefs( this, mRomMd5, mRomCrc, mRomHeaderName,
            RomHeader.countryCodeToSymbol(mRomCountryCode), mAppData, mGlobalPrefs );

        // Populate the profile preferences
        if(mEmulationProfile != null)
        {
            final String openGlVersion = AppData.getOpenGlEsVersion(this);
            final ArrayList<Profile> exclusions = new ArrayList<Profile>();

            if (openGlVersion.equals("2.0"))
            {
                exclusions.add(new Profile(true, "GlideN64-GLES-3.0", null));
                exclusions.add(new Profile(true, "GlideN64-GLES-3.1", null));

            }
            else if (openGlVersion.equals("3.0"))
            {
                exclusions.add(new Profile(true, "GlideN64-GLES-3.1", null));
            }

            mEmulationProfile.populateProfiles( mAppData.GetEmulationProfilesConfig(),
                mGlobalPrefs.GetEmulationProfilesConfig(), mGlobalPrefs.getEmulationProfileDefault(), exclusions);
            mEmulationProfile.setSummary(mEmulationProfile.getCurrentValue());
        }

        if(mTouchscreenProfile != null)
        {
            mTouchscreenProfile.populateProfiles( mAppData.GetTouchscreenProfilesConfig(),
                mGlobalPrefs.GetTouchscreenProfilesConfig(), mGlobalPrefs.getTouchscreenProfileDefault(), null );
            mTouchscreenProfile.setSummary(mTouchscreenProfile.getCurrentValue());
        }

        if(mControllerProfile1 != null)
        {
            mControllerProfile1.populateProfiles( mAppData.GetControllerProfilesConfig(),
                mGlobalPrefs.GetControllerProfilesConfig(), mGlobalPrefs.getControllerProfileDefault(1), null );
            mControllerProfile1.setSummary(mControllerProfile1.getCurrentValue());
        }

        if(mControllerProfile2 != null)
        {
            mControllerProfile2.populateProfiles( mAppData.GetControllerProfilesConfig(),
                mGlobalPrefs.GetControllerProfilesConfig(), mGlobalPrefs.getControllerProfileDefault(2), null );
            mControllerProfile2.setSummary(mControllerProfile2.getCurrentValue());
        }

        if(mControllerProfile3 != null)
        {
            mControllerProfile3.populateProfiles( mAppData.GetControllerProfilesConfig(),
                mGlobalPrefs.GetControllerProfilesConfig(), mGlobalPrefs.getControllerProfileDefault(3), null );
            mControllerProfile3.setSummary(mControllerProfile3.getCurrentValue());
        }

        if(mControllerProfile4 != null)
        {
            mControllerProfile4.populateProfiles( mAppData.GetControllerProfilesConfig(),
                mGlobalPrefs.GetControllerProfilesConfig(), mGlobalPrefs.getControllerProfileDefault(4), null );
            mControllerProfile4.setSummary(mControllerProfile4.getCurrentValue());
        }

        // Refresh the preferences objects in case populate* changed a value
        mGlobalPrefs = new GlobalPrefs( this, mAppData );
        mGamePrefs = new GamePrefs( this, mRomMd5, mRomCrc, mRomHeaderName, RomHeader.countryCodeToSymbol(mRomCountryCode),
            mAppData, mGlobalPrefs );

        // Set cheats screen summary text
        mScreenCheats = (PreferenceScreen) findPreference( SCREEN_CHEATS );
        mScreenCheats.setSummary( mGamePrefs.isCheatOptionsShown
                ? R.string.screenCheats_summaryEnabled
                : R.string.screenCheats_summaryDisabled );

        // Enable/disable player map item as necessary
        PrefUtil.enablePreference( this, GamePrefs.PLAYER_MAP, mGamePrefs.playerMap.isEnabled() );

        // Define which buttons to show in player map dialog
        final PlayerMapPreference playerPref = (PlayerMapPreference) findPreference( GamePrefs.PLAYER_MAP );
        if( playerPref != null )
        {
            // Check null in case preference has been removed
            final boolean enable1 = mGamePrefs.isControllerEnabled1;
            final boolean enable2 = mGamePrefs.isControllerEnabled2 && mRomDetail.players > 1;
            final boolean enable3 = mGamePrefs.isControllerEnabled3 && mRomDetail.players > 2;
            final boolean enable4 = mGamePrefs.isControllerEnabled4 && mRomDetail.players > 3;
            playerPref.setControllersEnabled( enable1, enable2, enable3, enable4 );

            // Set the initial value
            playerPref.setValue( mGamePrefs.playerMap.serialize() );
        }

        mPrefs.registerOnSharedPreferenceChangeListener( this );
    }

    private void refreshCheatsCategory()
    {
        if (mCategoryCheats != null)
        {
            if (mGamePrefs.isCheatOptionsShown)
            {
                if (mCategoryCheats.getPreferenceCount() == 0 || mClearCheats)
                {
                    ExtractCheatsTask cheatsTask = new ExtractCheatsTask(this, this, mAppData.mupencheat_txt, mRomCrc,
                        mRomCountryCode);
                    cheatsTask.execute((String) null);
                }
            }
            else
            {
                mScreenCheats.removePreference(mCategoryCheats);
                mCategoryCheats.removeAll();
            }
        }
    }

    @Override
    public void onExtractFinished(ArrayList<Cheat> cheats)
    {
        mCategoryCheats.removeAll();

        // Layout the menu, populating it with appropriate cheat options
        for (final Cheat cheat : cheats)
        {
            // Get the short title of the cheat (shown in the menu)
            String title;
            if( cheat.name == null )
            {
                // Title not available, just use a default string for the menu
                title = getString( R.string.cheats_defaultName, cheat.cheatIndex );
            }
            else
            {
                // Title available, remove the leading/trailing quotation marks
                title = cheat.name;
            }
            final String notes = cheat.desc;
            final String options = cheat.option;
            String[] optionStrings = null;
            if( !TextUtils.isEmpty( options ) )
            {
                optionStrings = options.split( "\n" );
            }

            // Create the menu item associated with this cheat
            final CheatPreference pref = new CheatPreference( getPreferenceManagerContext(),
                cheat.cheatIndex, title, notes, optionStrings );

            //We store the cheat index in the key as a string
            final String key = mRomCrc + " Cheat" + cheat.cheatIndex ;
            pref.setKey( key );

            // Add the preference menu item to the cheats category
            mCategoryCheats.addPreference( pref );

            // We reset if the list was changed by the user
            if (mClearCheats)
            {
                pref.onOptionChoice(0);
            }
        }

        //Check again because the user could had disabled cheats before
        //cheats finished displaying
        if(mGamePrefs.isCheatOptionsShown)
        {
            mScreenCheats.addPreference( mCategoryCheats );

            if(mClearCheats)
            {
                //Reset this to false if it was set
                mPrefs.edit().apply();

                mClearCheats = false;
            }
        }
    }

    @Override
    public boolean onPreferenceClick( Preference preference )
    {
        final String key = preference.getKey();
        if( key.equals( ACTION_CHEAT_EDITOR ) )
        {
            final Intent intent = new Intent( this, CheatEditorActivity.class );
            intent.putExtra( ActivityHelper.Keys.ROM_CRC, mRomCrc );
            intent.putExtra( ActivityHelper.Keys.ROM_HEADER_NAME, mRomHeaderName );
            intent.putExtra( ActivityHelper.Keys.ROM_COUNTRY_CODE, mRomCountryCode );
            startActivityForResult( intent, 111 );
        }
        else if( key.equals( ACTION_WIKI ) )
        {
            ActivityHelper.launchUri( this, mRomDetail.wikiUrl );
        }
        else if( key.equals( ACTION_RESET_GAME_PREFS ) )
        {
            actionResetGamePrefs();
        }
        return false;
    }

    private void actionResetGamePrefs()
    {
        final String title = getString( R.string.confirm_title );
        final String message = getString( R.string.actionResetGamePrefs_popupMessage );

        final ConfirmationDialog confirmationDialog =
            ConfirmationDialog.newInstance(RESET_GAME_PREFS_CONFIRM_DIALOG_ID, title, message);

        final FragmentManager fm = getSupportFragmentManager();
        confirmationDialog.show(fm, RESET_GAME_PREFS_CONFIRM_DIALOG_STATE);
    }

    @Override
    public void onPromptDialogClosed(int id, int which)
    {
        if( id == RESET_GAME_PREFS_CONFIRM_DIALOG_ID &&
            which == DialogInterface.BUTTON_POSITIVE )
        {
            // Reset the user preferences
            mPrefs.unregisterOnSharedPreferenceChangeListener( GamePrefsActivity.this );
            mPrefs.edit().clear().commit();
            PreferenceManager.setDefaultValues( GamePrefsActivity.this, R.xml.preferences_game, true );

            // Also reset any manual overrides the user may have made in the config file
            final File configFile = new File( mGamePrefs.mupen64plus_cfg );
            if( configFile.exists() )
                configFile.delete();

            // Rebuild the menu system by restarting the activity
            ActivityHelper.restartActivity( GamePrefsActivity.this );
        }
    }

    @Override
    protected void OnPreferenceScreenChange(String key)
    {
        if(key.equals(SCREEN_CHEATS))
        {
            mScreenCheats = (PreferenceScreen) findPreference( SCREEN_CHEATS );

            if(mCategoryCheats == null)
            {
                mCategoryCheats = (PreferenceGroup) findPreference( CATEGORY_CHEATS );
            }

            // Handle certain menu items that require extra processing or aren't actually preferences
            PrefUtil.setOnPreferenceClickListener( this, ACTION_CHEAT_EDITOR, this );

            refreshCheatsCategory();
        }
        else //then we are the root view
        {
            updateActivity();
        }
    }

    @Override
    public void onDialogClosed(int inputCode, int hardwareId, int which)
    {
        final PlayerMapPreference playerPref = (PlayerMapPreference) findPreference( GamePrefs.PLAYER_MAP );
        playerPref.onDialogClosed(inputCode, hardwareId, which);

        if( playerPref.getValue().equals( mGlobalPrefs.getString( GamePrefs.PLAYER_MAP, "" ) ) )
            playerPref.setValue( "" );
    }

    @Override
    public Controller getMogaController()
    {
        return mMogaController;
    }
}