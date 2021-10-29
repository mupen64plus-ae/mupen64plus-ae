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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;

import org.mupen64plusae.v3.alpha.R;

import java.io.File;
import java.util.ArrayList;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.DeleteFilesFragment;
import paulscode.android.mupen64plusae.cheat.CheatEditorActivity;
import paulscode.android.mupen64plusae.cheat.CheatFile;
import paulscode.android.mupen64plusae.cheat.CheatPreference;
import paulscode.android.mupen64plusae.cheat.CheatUtils;
import paulscode.android.mupen64plusae.cheat.CheatUtils.Cheat;
import paulscode.android.mupen64plusae.compat.AppCompatPreferenceActivity;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog;
import paulscode.android.mupen64plusae.dialog.PromptInputCodeDialog.PromptInputCodeListener;
import paulscode.android.mupen64plusae.preference.PlayerMapPreference;
import paulscode.android.mupen64plusae.preference.PrefUtil;
import paulscode.android.mupen64plusae.preference.ProfilePreference;
import paulscode.android.mupen64plusae.task.ExtractCheatsTask;
import paulscode.android.mupen64plusae.task.ExtractCheatsTask.ExtractCheatListener;
import paulscode.android.mupen64plusae.util.CountryCode;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.LegacyFilePicker;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.RomDatabase;
import paulscode.android.mupen64plusae.util.RomDatabase.RomDetail;


public class GamePrefsActivity extends AppCompatPreferenceActivity implements OnPreferenceClickListener,
        OnSharedPreferenceChangeListener, ExtractCheatListener, PromptInputCodeListener, ConfirmationDialog.PromptConfirmListener
{
    private static final int LEGACY_FILE_PICKER_REQUEST_CODE = 1;
    private static final int PICK_FILE_REQUEST_CODE = 2;
    private static final int EDIT_CHEATS_REQUEST_CODE = 111;

    // These constants must match the keys used in res/xml/preferences_play.xml
    private static final String SCREEN_ROOT = "screenRoot";
    private static final String SCREEN_CHEATS = "screenCheats";
    private static final String CATEGORY_CHEATS = "categoryCheats";
    private static final String COUNT_PER_OP = "screenAdvancedCountPerOp";
    private static final String VI_REFRESH = "screenAdvancedViRefreshRate";

    private static final String SHOW_BUILTIN_CHEAT_CODES = "showBuiltInCheatCodes";
    private static final String ACTION_CHEAT_EDITOR = "actionCheatEditor";
    private static final String ACTION_WIKI = "actionWiki";
    private static final String ACTION_DELETE_GAME_DATA = "deleteGameData";

    private static final String STATE_FILE_PICKER_KEY = "STATE_FILE_PICKER_KEY";

    public static final int CLEAR_CONFIRM_DIALOG_ID = 0;
    private static final String STATE_CONFIRM_DIALOG = "STATE_CONFIRM_DIALOG";
    private static final String STATE_DELETE_FILES_FRAGMENT= "STATE_DELETE_FILES_FRAGMENT";

    // App data and user preferences
    private AppData mAppData = null;
    private GlobalPrefs mGlobalPrefs = null;
    private GamePrefs mGamePrefs = null;
    private SharedPreferences mPrefs = null;

    private DeleteFilesFragment mDeleteFilesFragment = null;

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

    private String mCurrentFilePickerKey = null;

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

        // This is a little hacky, if rom is a zip file, then the file name is passed as
        // the romPath and not a URI.
        String fileName = file == null ? null : file.getName();

        // If this is a ROM not in the database inside a zip file, this will happen
        if (TextUtils.isEmpty(fileName)) {
            fileName = romPath;
        }

        mRomDetail = romDatabase.lookupByMd5WithFallback( mRomMd5, fileName, mRomCrc, CountryCode.getCountryCode(mRomCountryCode) );

        // Load user preference menu structure from XML and update view
        addPreferencesFromResource( mGamePrefs.getSharedPrefsName(), R.xml.preferences_game );

        if( savedInstanceState != null )
        {
            mCurrentFilePickerKey = savedInstanceState.getString( STATE_FILE_PICKER_KEY );
        }

        final FragmentManager fm = getSupportFragmentManager();
        mDeleteFilesFragment = (DeleteFilesFragment) fm.findFragmentByTag(STATE_DELETE_FILES_FRAGMENT);

        if(mDeleteFilesFragment == null)
        {
            mDeleteFilesFragment = new DeleteFilesFragment();
            fm.beginTransaction().add(mDeleteFilesFragment, STATE_DELETE_FILES_FRAGMENT).commit();
        }
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
    }

    private void setFilePickerPreferenceSummary(String filePickerPreferenceKey, String value)
    {
        Preference currentPreference = findPreference( filePickerPreferenceKey );
        if (currentPreference != null ) {
            if (value != null ) {
                try {
                    DocumentFile file = FileUtil.getDocumentFileSingle(this, Uri.parse(value));
                    String summary = file == null ? "" : file.getName();
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
        PrefUtil.setOnPreferenceClickListener(this, ACTION_DELETE_GAME_DATA, this);
        PrefUtil.setOnPreferenceClickListener( this, GamePrefs.IDL_PATH_64DD, this );
        PrefUtil.setOnPreferenceClickListener( this, GamePrefs.DISK_PATH_64DD, this );
        PrefUtil.setOnPreferenceClickListener( this, GamePrefs.CHANGE_COVERT_ART, this );
        PrefUtil.setOnPreferenceClickListener( this, GamePrefs.CLEAR_COVERT_ART, this );

        for (int player = 1; player <= GamePrefs.NUM_CONTROLLERS; ++player) {
            PrefUtil.setOnPreferenceClickListener( this, mGamePrefs.getTransferPakRomKey(player), this );
            PrefUtil.setOnPreferenceClickListener( this, mGamePrefs.getTransferPakRamKey(player), this );
        }

        // Update the summary of all the file preferences
        setFilePickerPreferenceSummary(GamePrefs.IDL_PATH_64DD, mGamePrefs.idlPath64Dd);
        setFilePickerPreferenceSummary(GamePrefs.DISK_PATH_64DD, mGamePrefs.diskPath64Dd);

        for (int player = 1; player <= GamePrefs.NUM_CONTROLLERS; ++player) {
            setFilePickerPreferenceSummary(mGamePrefs.getTransferPakRomKey(player), mGamePrefs.getTransferPakRom(player));
            setFilePickerPreferenceSummary(mGamePrefs.getTransferPakRamKey(player), mGamePrefs.getTransferPakRam(player));
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
    }

    @Override
    public void onSaveInstanceState( @NonNull Bundle savedInstanceState )
    {
        Log.i("GalleryActivity", "onSaveInstanceState");
        savedInstanceState.putString(STATE_FILE_PICKER_KEY, mCurrentFilePickerKey);

        super.onSaveInstanceState( savedInstanceState );
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

                    Preference currentPreference = findPreference(mCurrentFilePickerKey);
                    if (currentPreference != null && fileUri != null) {
                        getContentResolver().takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                        if (mCurrentFilePickerKey.equals(GamePrefs.CHANGE_COVERT_ART)) {
                            copyGalleryImageAndUpdateConfig(fileUri);
                        } else {
                            DocumentFile file = FileUtil.getDocumentFileSingle(this, fileUri);
                            String summary = file == null ? "" : file.getName();
                            currentPreference.setSummary(summary);
                            mGamePrefs.putString(mCurrentFilePickerKey, fileUri.toString());
                        }
                    }
                }
            } else if (requestCode == LEGACY_FILE_PICKER_REQUEST_CODE) {
                final Bundle extras = data.getExtras();

                if (extras != null) {
                    final String searchUri = extras.getString(ActivityHelper.Keys.SEARCH_PATH);
                    Uri fileUri = Uri.parse(searchUri);

                    Preference currentPreference = findPreference(mCurrentFilePickerKey);
                    if (currentPreference != null && fileUri != null && fileUri.getPath() != null) {

                        if (mCurrentFilePickerKey.equals(GamePrefs.CHANGE_COVERT_ART)) {
                            copyGalleryImageAndUpdateConfig(fileUri);
                        } else {
                            File file = new File(fileUri.getPath());
                            currentPreference.setSummary(file.getName());
                            mGamePrefs.putString(mCurrentFilePickerKey, fileUri.toString());
                        }
                    }
                }
            }
        }
    }

    private void copyGalleryImageAndUpdateConfig(Uri uri)
    {
        if (FileUtil.isFileImage(getApplicationContext(), uri)) {
            DocumentFile file = FileUtil.getDocumentFileSingle(getApplicationContext(), uri);
            if (file != null && FileUtil.copyFolder(getApplicationContext(), file, new File(mGlobalPrefs.coverArtDir + "/" + file.getName() ) )) {

                ConfigFile configFile = new ConfigFile(mGlobalPrefs.romInfoCacheCfg);
                configFile.put(mRomMd5, "artPath", mGlobalPrefs.coverArtDir + "/" + file.getName());
                configFile.save();
            }
        }
    }

    private void clearCoverArt()
    {
        RomDatabase romDatabase = RomDatabase.getInstance();
        RomDetail detail = romDatabase.lookupByMd5WithFallback( mRomMd5, mRomDisplayName, mRomCrc, CountryCode.getCountryCode(mRomCountryCode) );
        ConfigFile configFile = new ConfigFile(mGlobalPrefs.romInfoCacheCfg);
        configFile.put(mRomMd5, "artPath", mGlobalPrefs.coverArtDir + "/" + detail.artName);
        configFile.save();

        Notifier.showToast(getApplicationContext(), R.string.actionClearGameCoverArt_toast);
    }

    private void deleteGameData()
    {
        String title = getString( R.string.confirm_title );
        String message = getString( R.string.actionDeleteGameData_confirmation );

        ConfirmationDialog confirmationDialog =
                ConfirmationDialog.newInstance(CLEAR_CONFIRM_DIALOG_ID, title, message);

        FragmentManager fm1 = getSupportFragmentManager();
        confirmationDialog.show(fm1, STATE_CONFIRM_DIALOG);
    }

    @Override
    public void onPromptDialogClosed(int id, int which)
    {
        if(id == CLEAR_CONFIRM_DIALOG_ID)
        {
            if (which == DialogInterface.BUTTON_POSITIVE) {

                ArrayList<String> foldersToDelete = new ArrayList<>();
                ArrayList<String> filters = new ArrayList<>();

                foldersToDelete.add(mGamePrefs.getGameDataDir());
                filters.add("");
                foldersToDelete.add(mAppData.gameDataDir);
                filters.add(mRomGoodName);
                foldersToDelete.add(mAppData.gameDataDir);
                filters.add(mRomHeaderName);

                mDeleteFilesFragment.deleteFiles(foldersToDelete, filters);
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

        PrefUtil.enablePreference( this, COUNT_PER_OP, !mGamePrefs.useDefaultCountPerOp );
        PrefUtil.enablePreference( this, VI_REFRESH, !mGamePrefs.useDefaultViRefreshRate );

        mPrefs.registerOnSharedPreferenceChangeListener( this );
    }

    private void refreshCheatsCategory()
    {
        if (mCategoryCheats != null)
        {
            ExtractCheatsTask cheatsTask = new ExtractCheatsTask(this, this, mAppData.mupencheat_txt, mRomCrc, mRomCountryCode);
            cheatsTask.doInBackground();
        }
        else
        {
            Log.e("Cheats", "category is NULL");
        }
    }

    @Override
    synchronized public void onExtractFinished(ArrayList<Cheat> cheats)
    {
        runOnUiThread(() -> {

            mCategoryCheats.removeAll();

            if (mCategoryCheats.getPreferenceCount() == 0 || mClearCheats)
            {
                //We don't extract user cheats in a separate task since there aren't as many
                CheatFile usrcheat_txt = new CheatFile( mGlobalPrefs.customCheats_txt, true );

                // This list will be used to check if the cheat is in the custom cheat list.
                ArrayList<Cheat> custom_cheats = CheatUtils.populate( mRomCrc, mRomCountryCode, usrcheat_txt, this );

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

                    // Check if we need to display this cheat code (built-in / custom cheat codes)
                    if(mClearCheats || mGamePrefs.showBuiltInCheatCodes || cheat.isInArrayList(custom_cheats))
                        // Add the preference menu item to the cheats category
                        mCategoryCheats.addPreference( pref );

                    // We reset if the list was changed by the user
                    if (mClearCheats)
                    {
                        pref.onOptionChoice(0);
                    }
                }

                if (mCategoryCheats.getParent() == null) {
                    mScreenCheats.addPreference( mCategoryCheats );
                }

                if(mClearCheats)
                {
                    //Reset this to false if it was set
                    mPrefs.edit().apply();

                    mClearCheats = false;

                    // We need to refresh cheat code to display only the custom one.
                    if(!mGamePrefs.showBuiltInCheatCodes)
                        refreshCheatsCategory();
                }
            }
        });
    }

    @Override
    public boolean onPreferenceClick( Preference preference )
    {
        final String key = preference.getKey();

        if (key.equals(SHOW_BUILTIN_CHEAT_CODES)) {
            refreshCheatsCategory();
        } else if (key.equals(ACTION_CHEAT_EDITOR)) {
            final Intent intent = new Intent( this, CheatEditorActivity.class );
            intent.putExtra( ActivityHelper.Keys.ROM_CRC, mRomCrc );
            intent.putExtra( ActivityHelper.Keys.ROM_HEADER_NAME, mRomHeaderName );
            intent.putExtra( ActivityHelper.Keys.ROM_COUNTRY_CODE, mRomCountryCode );
            startActivityForResult( intent, EDIT_CHEATS_REQUEST_CODE );
        } else if (key.equals(ACTION_WIKI)) {
            ActivityHelper.launchUri( this, mRomDetail.wikiUrl );
        } else if (key.equals(GamePrefs.IDL_PATH_64DD) ||
                key.equals(GamePrefs.DISK_PATH_64DD) || key.contains(GamePrefs.TRANSFER_PAK)){
            mCurrentFilePickerKey = key;
            startFilePicker(false);
        } else if (key.equals(GamePrefs.CHANGE_COVERT_ART)) {
            mCurrentFilePickerKey = key;
            startFilePicker(true);
        } else if (key.equals(GamePrefs.CLEAR_COVERT_ART)) {
            clearCoverArt();
        } else if (key.equals(ACTION_DELETE_GAME_DATA)) {
            deleteGameData();
        }

        return false;
    }

    private void startFilePicker(boolean selectImage)
    {
        AppData appData = new AppData( this );
        if (appData.useLegacyFileBrowser) {
            Intent intent = new Intent(this, LegacyFilePicker.class);
            intent.putExtra( ActivityHelper.Keys.CAN_SELECT_FILE, true );
            intent.putExtra( ActivityHelper.Keys.CAN_VIEW_EXT_STORAGE, true);
            startActivityForResult( intent, LEGACY_FILE_PICKER_REQUEST_CODE );
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            if (selectImage) {
                intent.setType("image/*");
            } else {
                intent.setType("*/*");
            }
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
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
            PrefUtil.setOnPreferenceClickListener( this, SHOW_BUILTIN_CHEAT_CODES, this );
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
        playerPref.onDialogClosed(hardwareId, which);

        if( playerPref.getValue().equals( mGlobalPrefs.getString( GamePrefs.PLAYER_MAP, "" ) ) )
            playerPref.setValue( "" );
    }
}
