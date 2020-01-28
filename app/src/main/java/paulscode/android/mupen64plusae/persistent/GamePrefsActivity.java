/*
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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;

import androidx.documentfile.provider.DocumentFile;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;

import com.bda.controller.Controller;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;
import java.util.ArrayList;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.cheat.CheatEditorActivity;
import paulscode.android.mupen64plusae.cheat.CheatPreference;
import paulscode.android.mupen64plusae.cheat.CheatUtils.Cheat;
import paulscode.android.mupen64plusae.compat.AppCompatPreferenceActivity;
import paulscode.android.mupen64plusae.dialog.PromptInputCodeDialog.PromptInputCodeListener;
import paulscode.android.mupen64plusae.hack.MogaHack;
import paulscode.android.mupen64plusae.preference.PlayerMapPreference;
import paulscode.android.mupen64plusae.preference.PrefUtil;
import paulscode.android.mupen64plusae.preference.ProfilePreference;
import paulscode.android.mupen64plusae.task.ExtractCheatsTask;
import paulscode.android.mupen64plusae.task.ExtractCheatsTask.ExtractCheatListener;
import paulscode.android.mupen64plusae.util.CountryCode;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.LegacyFilePicker;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;
import paulscode.android.mupen64plusae.util.RomDatabase;
import paulscode.android.mupen64plusae.util.RomDatabase.RomDetail;


public class GamePrefsActivity extends AppCompatPreferenceActivity implements OnPreferenceClickListener,
        OnSharedPreferenceChangeListener, ExtractCheatListener, PromptInputCodeListener
{
    private static final int LEGACY_FILE_PICKER_REQUEST_CODE = 1;
    private static final int PICK_FILE_REQUEST_CODE = 2;
    private static final int EDIT_CHEATS_REQUEST_CODE = 111;

    // These constants must match the keys used in res/xml/preferences_play.xml
    private static final String SCREEN_ROOT = "screenRoot";
    private static final String SCREEN_CHEATS = "screenCheats";
    private static final String CATEGORY_CHEATS = "categoryCheats";

    private static final String ACTION_CHEAT_EDITOR = "actionCheatEditor";
    private static final String ACTION_WIKI = "actionWiki";

    // App data and user preferences
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;
    private GamePrefs mGamePrefs = null;
    private SharedPreferences mPrefs = null;

    // ROM info
    private String mRomMd5 = null;
    private String mRomCrc = null;
    private String mRomHeaderName = null;
    private String mRomGoodName = null;
    private String mRomDisplayName = null;
    private byte mRomCountryCode = 0;
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
    private boolean mInCheatsScreen = false;

    private String currentFilePickerKey = null;

    // MOGA controller interface
    private final Controller mMogaController = Controller.getInstance( this );

    @Override
    protected void attachBaseContext(Context newBase) {
        if(TextUtils.isEmpty(LocaleContextWrapper.getLocalCode()))
        {
            super.attachBaseContext(newBase);
        }
        else
        {
            super.attachBaseContext(LocaleContextWrapper.wrap(newBase,LocaleContextWrapper.getLocalCode()));
        }
    }

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        // Get the ROM path and MD5 that was passed to the activity
        final Bundle extras = getIntent().getExtras();
        if( extras == null )
            throw new Error( "ROM path and MD5 must be passed via the extras bundle" );
        String romPath = extras.getString( ActivityHelper.Keys.ROM_PATH );
        if( romPath == null )
            throw new Error( "ROM path and MD5 must be passed via the extras bundle" );

        mRomMd5 = extras.getString( ActivityHelper.Keys.ROM_MD5 );
        mRomCrc = extras.getString( ActivityHelper.Keys.ROM_CRC );
        mRomHeaderName = extras.getString( ActivityHelper.Keys.ROM_HEADER_NAME );
        mRomGoodName = extras.getString( ActivityHelper.Keys.ROM_GOOD_NAME );
        mRomDisplayName = extras.getString( ActivityHelper.Keys.ROM_DISPLAY_NAME );
        mRomCountryCode = extras.getByte( ActivityHelper.Keys.ROM_COUNTRY_CODE );

        if( TextUtils.isEmpty( mRomMd5 ) )
            throw new Error( "MD5 must be passed via the extras bundle" );

        // Initialize MOGA controller API
        MogaHack.init( mMogaController, this );

        // Get app data and user preferences
        mAppData = new AppData( this );
        mGlobalPrefs = new GlobalPrefs( this, mAppData );
        mGamePrefs = new GamePrefs( this, mRomMd5, mRomCrc, mRomHeaderName, mRomGoodName,
            CountryCode.getCountryCode(mRomCountryCode).toString(), mAppData, mGlobalPrefs );
        mPrefs = getSharedPreferences( mGamePrefs.getSharedPrefsName(), MODE_PRIVATE );

        // Get the detailed info about the ROM
        RomDatabase romDatabase = RomDatabase.getInstance();

        if(!romDatabase.hasDatabaseFile())
        {
            romDatabase.setDatabaseFile(mAppData.mupen64plus_ini);
        }

        DocumentFile file = FileUtil.getDocumentFileSingle(this, Uri.parse(romPath));
        String fileName = file.getName();

        // If this is a ROM not in the database inside a zip file, this will happen
        if (TextUtils.isEmpty(fileName)) {
            fileName = romPath;
        }

        mRomDetail = romDatabase.lookupByMd5WithFallback( mRomMd5, fileName, mRomCrc, CountryCode.getCountryCode(mRomCountryCode) );

        // Load user preference menu structure from XML and update view
        addPreferencesFromResource( mGamePrefs.getSharedPrefsName(), R.xml.preferences_game );
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        //Update activity on resume just in case user updated profile list when
        //managing profiles when clicking on profile settings
        if(!mInCheatsScreen)
        {
            refreshViews();
        }

        mPrefs.registerOnSharedPreferenceChangeListener( this );
        mMogaController.onResume();
    }

    private void setFilePickerPreferenceSummary(String filePickerPreferenceKey, String value)
    {
        Preference currentPreference = findPreference( filePickerPreferenceKey );
        if (currentPreference != null ) {
            if (value != null ) {
                try {
                    DocumentFile file = FileUtil.getDocumentFileSingle(this, Uri.parse(value));
                    String summary = file.getName();
                    currentPreference.setSummary(summary);
                } catch (java.lang.SecurityException exception) {
                    Log.e("GamePrefs", "Permission denied, key=" + filePickerPreferenceKey + " value=" + value);
                }
            }
        }
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
        setTitle( mRomDisplayName );

        // Handle certain menu items that require extra processing or aren't actually preferences
        PrefUtil.setOnPreferenceClickListener( this, ACTION_WIKI, this );
        PrefUtil.setOnPreferenceClickListener( this, GamePrefs.IDL_PATH_64DD, this );
        PrefUtil.setOnPreferenceClickListener( this, GamePrefs.DISK_PATH_64DD, this );

        for (int player = 0; player < GamePrefs.NUM_CONTROLLERS; ++player) {
            PrefUtil.setOnPreferenceClickListener( this, mGamePrefs.getTransferPakRomKey(player), this );
            PrefUtil.setOnPreferenceClickListener( this, mGamePrefs.getTransferPakRamKey(player), this );
        }

        // Update the summary of all the file preferences
        setFilePickerPreferenceSummary(GamePrefs.IDL_PATH_64DD, mGamePrefs.idlPath64Dd);
        setFilePickerPreferenceSummary(GamePrefs.DISK_PATH_64DD, mGamePrefs.diskPath64Dd);


        for (int player = 0; player < GamePrefs.NUM_CONTROLLERS; ++player) {
            setFilePickerPreferenceSummary(mGamePrefs.getTransferPakRomKey(player), mGamePrefs.getTransferPakRom(player));
            setFilePickerPreferenceSummary(mGamePrefs.getTransferPakRomKey(player), mGamePrefs.getTransferPakRom(player));
        }

        // Remove wiki menu item if not applicable
        if( TextUtils.isEmpty( mRomDetail.wikiUrl ) )
        {
            PrefUtil.removePreference( this, SCREEN_ROOT, ACTION_WIKI );
        }

        //Remove touch screen profile if TV mode
        if(mGlobalPrefs.isBigScreenMode)
        {
            PrefUtil.removePreference( this, SCREEN_ROOT, GamePrefs.TOUCHSCREEN_PROFILE);
        }

        if (!mGamePrefs.is64DdGame) {
            PrefUtil.removePreference(this, SCREEN_ROOT, GamePrefs.SUPPORT_64DD);
            PrefUtil.removePreference(this, SCREEN_ROOT, GamePrefs.IDL_PATH_64DD);
            PrefUtil.removePreference(this, SCREEN_ROOT, GamePrefs.DISK_PATH_64DD);
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
    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == EDIT_CHEATS_REQUEST_CODE) {
                //If the user cheats were saved, reset all selected cheatd
                mClearCheats = true;
                refreshCheatsCategory();
            } else if (requestCode == PICK_FILE_REQUEST_CODE) {
                // The result data contains a URI for the document or directory that
                // the user selected.
                if (data != null) {
                    Uri fileUri = data.getData();

                    Preference currentPreference = findPreference(currentFilePickerKey);
                    if (currentPreference != null && fileUri != null) {

                        final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        getContentResolver().takePersistableUriPermission(fileUri, takeFlags);

                        DocumentFile file = FileUtil.getDocumentFileSingle(this, fileUri);
                        String summary = file.getName();
                        currentPreference.setSummary(summary);
                        mGamePrefs.putString(currentFilePickerKey, fileUri.toString());
                    }
                }
            } else if (requestCode == LEGACY_FILE_PICKER_REQUEST_CODE) {
                final Bundle extras = data.getExtras();

                if (extras != null) {
                    final String searchUri = extras.getString(ActivityHelper.Keys.SEARCH_PATH);
                    Uri fileUri = Uri.parse(searchUri);

                    Preference currentPreference = findPreference(currentFilePickerKey);
                    if (currentPreference != null && fileUri != null && fileUri.getPath() != null) {
                        File file = new File(fileUri.getPath());
                        currentPreference.setSummary(file.getName());
                        mGamePrefs.putString(currentFilePickerKey, fileUri.toString());
                    }
                }
            }
        }

    }

    private void refreshViews()
    {
        mPrefs.unregisterOnSharedPreferenceChangeListener( this );

        // Refresh the preferences objects
        mGlobalPrefs = new GlobalPrefs( this, mAppData );
        mGamePrefs = new GamePrefs( this, mRomMd5, mRomCrc, mRomHeaderName, mRomGoodName,
                CountryCode.getCountryCode(mRomCountryCode).toString(), mAppData, mGlobalPrefs );

        // Populate the profile preferences
        if(mEmulationProfile != null)
        {
            mEmulationProfile.populateProfiles( mAppData.GetEmulationProfilesConfig(),
                mGlobalPrefs.GetEmulationProfilesConfig(), true, mGlobalPrefs.getEmulationProfileDefault(),
                    null, mGlobalPrefs.showBuiltInEmulationProfiles);
            mEmulationProfile.setSummary(mEmulationProfile.getCurrentValue(null));
        }

        if(mTouchscreenProfile != null)
        {
            mTouchscreenProfile.populateProfiles( mAppData.GetTouchscreenProfilesConfig(),
                mGlobalPrefs.GetTouchscreenProfilesConfig(), true,
                    mGamePrefs.isDpadGame ? mGlobalPrefs.getTouchscreenDpadProfileDefault() : mGlobalPrefs.getTouchscreenProfileDefault(),
                    null, mGlobalPrefs.showBuiltInTouchscreenProfiles );
            mTouchscreenProfile.setSummary(mTouchscreenProfile.getCurrentValue(null));
        }

        if(mControllerProfile1 != null)
        {
            mControllerProfile1.populateProfiles( mAppData.GetControllerProfilesConfig(),
                mGlobalPrefs.GetControllerProfilesConfig(), true, mGlobalPrefs.getControllerProfileDefault(1), null,
                    mGlobalPrefs.showBuiltInControllerProfiles );
            mControllerProfile1.setSummary(mControllerProfile1.getCurrentValue(null));
        }

        if(mControllerProfile2 != null)
        {
            mControllerProfile2.populateProfiles( mAppData.GetControllerProfilesConfig(),
                mGlobalPrefs.GetControllerProfilesConfig(), true, mGlobalPrefs.getControllerProfileDefault(2), null,
                    mGlobalPrefs.showBuiltInControllerProfiles );
            mControllerProfile2.setSummary(mControllerProfile2.getCurrentValue(null));
        }

        if(mControllerProfile3 != null)
        {
            mControllerProfile3.populateProfiles( mAppData.GetControllerProfilesConfig(),
                mGlobalPrefs.GetControllerProfilesConfig(), true, mGlobalPrefs.getControllerProfileDefault(3), null,
                    mGlobalPrefs.showBuiltInControllerProfiles );
            mControllerProfile3.setSummary(mControllerProfile3.getCurrentValue(null));
        }

        if(mControllerProfile4 != null)
        {
            mControllerProfile4.populateProfiles( mAppData.GetControllerProfilesConfig(),
                mGlobalPrefs.GetControllerProfilesConfig(), true, mGlobalPrefs.getControllerProfileDefault(4), null,
                    mGlobalPrefs.showBuiltInControllerProfiles );
            mControllerProfile4.setSummary(mControllerProfile4.getCurrentValue(null));
        }

        // Refresh the preferences objects in case populate* changed a value
        mGlobalPrefs = new GlobalPrefs( this, mAppData );
        mGamePrefs = new GamePrefs( this, mRomMd5, mRomCrc, mRomHeaderName, mRomGoodName,
                CountryCode.getCountryCode(mRomCountryCode).toString(), mAppData, mGlobalPrefs );

        // Set cheats screen summary text
        mScreenCheats = (PreferenceScreen) findPreference( SCREEN_CHEATS );

        // This is null when entering sub screens other than "SCREEN_CHEATS"
        if(mScreenCheats != null)
        {
            mScreenCheats.setSummary( mGamePrefs.isCheatOptionsShown
                    ? R.string.screenCheats_summaryEnabled
                    : R.string.screenCheats_summaryDisabled );
        }

        // Enable/disable player map item as necessary
        PrefUtil.enablePreference( this, GamePrefs.PLAYER_MAP,
                mGamePrefs.playerMap.isEnabled() && !mGamePrefs.useDefaultPlayerMapping );

        PrefUtil.enablePreference( this, GamePrefs.DISPLAY_ZOOM, !mGamePrefs.useDefaultZoom );

        if (mGamePrefs.is64DdGame) {
            // Enable disable 64 preferences as necessary
            PrefUtil.enablePreference( this, GamePrefs.IDL_PATH_64DD, mGamePrefs.enable64DdSupport );
            PrefUtil.enablePreference( this, GamePrefs.DISK_PATH_64DD, mGamePrefs.enable64DdSupport );
        }

        // Define which buttons to show in player map dialog
        final PlayerMapPreference playerPref = (PlayerMapPreference) findPreference( GamePrefs.PLAYER_MAP );
        if( playerPref != null )
        {
            // Check null in case preference has been removed
            final boolean enable1 = mGamePrefs.isControllerEnabled[0];
            final boolean enable2 = mGamePrefs.isControllerEnabled[1];
            final boolean enable3 = mGamePrefs.isControllerEnabled[2];
            final boolean enable4 = mGamePrefs.isControllerEnabled[3];
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
            ExtractCheatsTask cheatsTask = new ExtractCheatsTask(this, this, mAppData.mupencheat_txt, mRomCrc,
                mRomCountryCode);
            cheatsTask.execute((String) null);
        }
        else
        {
            Log.e("Cheats", "category is NULL");
        }
    }

    @Override
    synchronized public void onExtractFinished(ArrayList<Cheat> cheats)
    {
        mCategoryCheats.removeAll();
        
        if (mGamePrefs.isCheatOptionsShown)
        {
            if (mCategoryCheats.getPreferenceCount() == 0 || mClearCheats)
            {
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

                 mScreenCheats.addPreference( mCategoryCheats );

                 if(mClearCheats)
                 {
                     //Reset this to false if it was set
                     mPrefs.edit().apply();

                     mClearCheats = false;
                 }
            }
        }
        else
        {
            mScreenCheats.removePreference(mCategoryCheats);
        }
    }

    @Override
    public boolean onPreferenceClick( Preference preference )
    {
        final String key = preference.getKey();

        if (key.equals(ACTION_CHEAT_EDITOR)) {
            final Intent intent = new Intent( this, CheatEditorActivity.class );
            intent.putExtra( ActivityHelper.Keys.ROM_CRC, mRomCrc );
            intent.putExtra( ActivityHelper.Keys.ROM_HEADER_NAME, mRomHeaderName );
            intent.putExtra( ActivityHelper.Keys.ROM_COUNTRY_CODE, mRomCountryCode );
            startActivityForResult( intent, EDIT_CHEATS_REQUEST_CODE );
        } else if (key.equals(ACTION_WIKI)) {
            ActivityHelper.launchUri( this, mRomDetail.wikiUrl );
        } else if (key.equals(GamePrefs.IDL_PATH_64DD) ||
                key.equals(GamePrefs.DISK_PATH_64DD) || key.contains(GamePrefs.TRANSFER_PAK)){
            currentFilePickerKey = key;
            startFilePicker();
        }

        return false;
    }

    private void startFilePicker()
    {
        AppData appData = new AppData( this );
        if (appData.isAndroidTv) {
            Intent intent = new Intent(this, LegacyFilePicker.class);
            intent.putExtra( ActivityHelper.Keys.CAN_SELECT_FILE, true );
            startActivityForResult( intent, LEGACY_FILE_PICKER_REQUEST_CODE );
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
        }
    }

    @Override
    protected void OnPreferenceScreenChange(String key)
    {
        if(key.equals(SCREEN_CHEATS))
        {
            mInCheatsScreen = true;
            mScreenCheats = (PreferenceScreen) findPreference( SCREEN_CHEATS );
            mCategoryCheats = (PreferenceGroup) findPreference( CATEGORY_CHEATS );

            // Handle certain menu items that require extra processing or aren't actually preferences
            PrefUtil.setOnPreferenceClickListener( this, ACTION_CHEAT_EDITOR, this );

            refreshCheatsCategory();
        }
        else //then we are the root view
        {
            mInCheatsScreen = false;
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