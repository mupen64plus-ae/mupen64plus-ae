package paulscode.android.mupen64plusae.dialog;

import static paulscode.android.mupen64plusae.persistent.GlobalPrefs.AUDIO_SAMPLING_TYPE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.EditTextPreference;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.mupen64plusae.v3.alpha.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import paulscode.android.mupen64plusae.ActivityHelper;
import paulscode.android.mupen64plusae.DrawerDrawable;
import paulscode.android.mupen64plusae.compat.AppCompatPreferenceFragment;
import paulscode.android.mupen64plusae.game.ShaderLoader;
import paulscode.android.mupen64plusae.netplay.NetplayFragment;
import paulscode.android.mupen64plusae.netplay.TcpServer;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.AudioPrefsActivity;
import paulscode.android.mupen64plusae.persistent.DataPrefsActivity;
import paulscode.android.mupen64plusae.persistent.DisplayPrefsActivity;
import paulscode.android.mupen64plusae.persistent.GamePrefs;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.persistent.InputPrefsActivity;
import paulscode.android.mupen64plusae.persistent.ShaderPrefsActivity;
import paulscode.android.mupen64plusae.persistent.TouchscreenPrefsActivity;
import paulscode.android.mupen64plusae.preference.PlayerMapPreference;
import paulscode.android.mupen64plusae.preference.ShaderPreference;
import paulscode.android.mupen64plusae.util.FileUtil;
import paulscode.android.mupen64plusae.util.LegacyFilePicker;
import paulscode.android.mupen64plusae.util.Notifier;
import paulscode.android.mupen64plusae.util.RomHeader;

public class GameSettingsDialog extends DialogFragment implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener, ShaderPreference.OnRemove, PreferenceFragmentCompat.OnPreferenceStartScreenCallback
{
    private SettingsFragment mSettingsFragment;
    private Vibrator mVibrator;
    private InputMethodManager mImm;
    private View mView;
    private final ArrayList<String> mValidSkinFiles = new ArrayList<>();
    private final String TAG = "GameSettingsDialog";
    private static final String ACTION_IMPORT_TOUCHSCREEN_GRAPHICS = "actionImportTouchscreenGraphics";
    private static final String STATE_CURRENT_RESOURCE_ID = "STATE_CURRENT_RESOURCE_ID";
    private static final String STATE_SHARED_PREFS_NAME = "STATE_SHARED_PREFS_NAME";
    private static final String STATE_RESOURCE_ID = "STATE_RESOURCE_ID";
    private static final String STATE_SETTINGS_RESET = "STATE_SETTINGS_RESET";
    private static final String STATE_RECREATE_LATER = "STATE_RECREATE_LATER";
    private static final String STATE_SCREEN_ROTATING = "STATE_SCREEN_ROTATING";
    private static final String STATE_DELETE_EXTRA_DIALOG = "STATE_DELETE_EXTRA_DIALOG";
    private static final String STATE_LAUNCHING_ACTIVITY = "STATE_LAUNCHING_ACTIVITY";
    private static final String STATE_PROMPT_INPUT_CODE_DIALOG = "STATE_PROMPT_INPUT_CODE_DIALOG";
    private static final String RESET_SETTINGS_CONFIRM_DIALOG = "RESET_SETTINGS_CONFIRM_DIALOG";
    private static final int VIDEO_HARDWARE_TYPE_CUSTOM = 999;
    private static final int FEEDBACK_VIBRATE_TIME = 50;
    private static final int RESET_SETTINGS_CONFIRM_DIALOG_ID = 6;
    private static int mCurrentResourceId = 0;
    private boolean mLaunchingActivity = false;
    private boolean mSettingsReset = false;
    private boolean mRecreateLater = false;
    private boolean mScreenRotating = false;
    private boolean mLongClick = false;
    private int mDeleteExtraDialog = 0;

    // Shader

    private PreferenceGroup mCategoryPasses = null;
    private static final String SCREEN_ROOT = "screenRoot";
    private static final String CATEGORY_PASSES = "categoryShaderPasses";
    private static final String ADD_PREFERENCE = "addShader";
    private static final String SHADER_PASS_KEY = "shaderpass,";
    private static final int MAX_SHADER_PASSES = 5;
    private boolean mRemoveFirstPassShader = false;
    private int mRemoveShader = -1;

    // Preferences

    private static SharedPreferences mPrefs;
    private static final String mSharedPrefsName = null;
    private static final int mPreferencesResId = 0;

    public interface OnGameSettingsDialogListener {
        void settingsViewReset();
        void recreateSurface();
        void recreate();
        Context getApplicationContext();
        Object getSystemService(String name);
        FragmentManager getSupportFragmentManager();
        NetplayFragment getNetplayFragment();
        AppData getAppData();
        GlobalPrefs getGlobalPrefs();
        GamePrefs getGamePrefs();
        boolean isChangingConfigurations();
        boolean getSettingsReset();
        boolean getNetplayEnabled();
        boolean isDdActive();
        void setDialogFragmentKey(String key);
        String getDialogFragmentKey();
        void setAssociatedDialogFragment(int associatedDialogFragment);
        int getAssociatedDialogFragment();
        void onComplete(String key);
    }

    private static OnGameSettingsDialogListener mGameActivity;

    /* Activity Results */

    // Used in Touchscreen for graphic import
    ActivityResultLauncher<Intent> mLaunchFilePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && data != null) {
                    Uri fileUri = getUri(data);

                    if(importCustomSkin(fileUri)){
                        mGameActivity.getGlobalPrefs().putBoolean("isCustomTouchscreenSkin",true);
                        mGameActivity.getGlobalPrefs().putString("touchscreenSkin_v2","Custom");
                    }
                    mGameActivity.onComplete("resetTouchscreenController");
                    mGameActivity.recreateSurface();
                    recreateAndPause();
                }
            });

    // Used in Data for save location
    ActivityResultLauncher<Intent> mLaunchGameDataFolderPicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && data != null) {

                    // The result data contains a URI for the document or directory that
                    // the user selected.
                    Uri fileUri = data.getData();
                    if (fileUri != null) {
                        if (!mGameActivity.getAppData().useLegacyFileBrowser) {
                            requireActivity().getContentResolver().takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        }
                        mGameActivity.getGlobalPrefs().putString(GlobalPrefs.PATH_GAME_SAVES, fileUri.toString());
                        mSettingsReset = true;
                    }
                    else
                        waitToResetPreferences();
                }
                else
                    waitToResetPreferences();
            });

    // Used in Data for 64DD IPL
    ActivityResultLauncher<Intent> mLaunchIdlFilePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && data != null) {
                    Uri fileUri = getUri(data);
                    if (fileUri != null) {
                        if (!mGameActivity.getAppData().useLegacyFileBrowser) {
                            requireActivity().getContentResolver().takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }
                        requireActivity().getContentResolver().takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        mGameActivity.getGlobalPrefs().putString(GlobalPrefs.PATH_JAPAN_IPL_ROM, fileUri.toString());
                        mGameActivity.onComplete("iplFile");
                    }
                    else
                        waitToResetPreferences();
                }
                else
                    waitToResetPreferences();
            });

    public static GameSettingsDialog newInstance() {
        GameSettingsDialog frag = new GameSettingsDialog();
        Bundle args = new Bundle();
        args.putInt(STATE_CURRENT_RESOURCE_ID,mCurrentResourceId);

        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onStop() {
        super.onStop();
        // check if we're launching activity from settings menu
        if (!mGameActivity.getSettingsReset() && !mLaunchingActivity) {
            if(!mRecreateLater && !mSettingsReset)
                mGameActivity.settingsViewReset();
            try {
                dismiss();
                mGameActivity.setDialogFragmentKey("");

                if(mSettingsReset) {
                    mSettingsReset = false;
                    mGameActivity.onComplete("settingsResetFalse"); // reset core variable
                    mGameActivity.onComplete("settingsReset");
                    mGameActivity.settingsViewReset();
                }

                // need to do this when changing a playerMap setting because recreating with
                // the extra dialog fragment can mess things up
                if(mRecreateLater){
                    mRecreateLater = false;
                    mGameActivity.onComplete("settingsRecreate");
                    mGameActivity.settingsViewReset();
                }
                mGameActivity.onComplete("openDrawerState");
            } catch (IllegalStateException e) {
                mScreenRotating = true;
                mGameActivity.onComplete("pauseEmulator");
                e.printStackTrace();

                // on long click from player map needs this here (it dismisses a dialog or
                // resets the fragment key or something) otherwise we'd always set to 1
                if(!mGameActivity.getDialogFragmentKey().equals(""))//("playerMap"))
                    mDeleteExtraDialog = 1;

                if(!keyboardOpen() && mGameActivity.getDialogFragmentKey().equals("videoPolygonOffset")) {//also make sure we're not exiting
                    mGameActivity.setDialogFragmentKey("");// set delete extra dialog too? (0 or 1?)
                }
            }
            mGameActivity.onComplete("gameSettingDialogClosed");
        }
    }

    @Override
    public void onRemove(String key) {

        String[] currentPassSplitString = key.split(",");

        if (currentPassSplitString.length == 2) {
            ArrayList<ShaderLoader> shaderPasses = mGameActivity.getGlobalPrefs().getShaderPasses();
            int changedPass = Integer.parseInt(currentPassSplitString[1]) - 1;

            if(shaderPasses.size() <= 1)
                mRemoveFirstPassShader = true;
            else
                mRemoveShader = changedPass;

            if (changedPass >= 0 && changedPass < shaderPasses.size()) {
                shaderPasses.remove(changedPass);
                mPrefs.edit().remove(key).apply();
                mGameActivity.getGlobalPrefs().putShaderPasses(shaderPasses);

                refreshShaderViews();
                mGameActivity.recreate();
            }
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mGameActivity = (OnGameSettingsDialogListener) context;
    }

    @Override
    public void onDestroy() {
        Log.i( TAG, "onDestroy" );
        super.onDestroy();
        mGameActivity.onComplete("settingsResetFalse");
        mDeleteExtraDialog = 1;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mValidSkinFiles.add("analog-back.png");
        mValidSkinFiles.add("analog-fore.png");
        mValidSkinFiles.add("analog.png");
        mValidSkinFiles.add("buttonL-holdL.png");
        mValidSkinFiles.add("buttonL-mask.png");
        mValidSkinFiles.add("buttonL.png");
        mValidSkinFiles.add("buttonR-holdR.png");
        mValidSkinFiles.add("buttonR-mask.png");
        mValidSkinFiles.add("buttonR.png");
        mValidSkinFiles.add("buttonS-holdS.png");
        mValidSkinFiles.add("buttonS-mask.png");
        mValidSkinFiles.add("buttonS.png");
        mValidSkinFiles.add("buttonSen-holdSen.png");
        mValidSkinFiles.add("buttonSen-mask.png");
        mValidSkinFiles.add("buttonSen.png");
        mValidSkinFiles.add("buttonZ-holdZ.png");
        mValidSkinFiles.add("buttonZ-mask.png");
        mValidSkinFiles.add("buttonZ.png");
        mValidSkinFiles.add("dpad-mask.png");
        mValidSkinFiles.add("dpad.png");
        mValidSkinFiles.add("fps-0.png");
        mValidSkinFiles.add("fps-1.png");
        mValidSkinFiles.add("fps-2.png");
        mValidSkinFiles.add("fps-3.png");
        mValidSkinFiles.add("fps-4.png");
        mValidSkinFiles.add("fps-5.png");
        mValidSkinFiles.add("fps-6.png");
        mValidSkinFiles.add("fps-7.png");
        mValidSkinFiles.add("fps-8.png");
        mValidSkinFiles.add("fps-9.png");
        mValidSkinFiles.add("fps.png");
        mValidSkinFiles.add("groupAB-holdA.png");
        mValidSkinFiles.add("groupAB-holdB.png");
        mValidSkinFiles.add("groupAB-mask.png");
        mValidSkinFiles.add("groupAB.png");
        mValidSkinFiles.add("buttonA-holdA.png");
        mValidSkinFiles.add("buttonA-mask.png");
        mValidSkinFiles.add("buttonA.png");
        mValidSkinFiles.add("buttonB-holdB.png");
        mValidSkinFiles.add("buttonB-mask.png");
        mValidSkinFiles.add("buttonB.png");
        mValidSkinFiles.add("groupC-holdCd.png");
        mValidSkinFiles.add("groupC-holdCl.png");
        mValidSkinFiles.add("groupC-holdCr.png");
        mValidSkinFiles.add("groupC-holdCu.png");
        mValidSkinFiles.add("groupC-mask.png");
        mValidSkinFiles.add("groupC.png");
        mValidSkinFiles.add("buttonCr-holdCr.png");
        mValidSkinFiles.add("buttonCr-mask.png");
        mValidSkinFiles.add("buttonCr.png");
        mValidSkinFiles.add("buttonCl-holdCl.png");
        mValidSkinFiles.add("buttonCl-mask.png");
        mValidSkinFiles.add("buttonCl.png");
        mValidSkinFiles.add("buttonCd-holdCd.png");
        mValidSkinFiles.add("buttonCd-mask.png");
        mValidSkinFiles.add("buttonCd.png");
        mValidSkinFiles.add("buttonCu-holdCu.png");
        mValidSkinFiles.add("buttonCu-mask.png");
        mValidSkinFiles.add("buttonCu.png");
        mValidSkinFiles.add("skin.ini");

        Bundle args = getArguments();
        if( args != null ){
            mCurrentResourceId = args.getInt(STATE_CURRENT_RESOURCE_ID,0);
            mSettingsReset = args.getBoolean(STATE_SETTINGS_RESET, false);
            mRecreateLater = args.getBoolean(STATE_RECREATE_LATER, false);
            mScreenRotating = args.getBoolean(STATE_SCREEN_ROTATING, false);
        }

        setStyle(STYLE_NO_FRAME, R.style.MupenTheme_Dark_Translucent);

        if(savedInstanceState != null){
            mSettingsReset = savedInstanceState.getBoolean(STATE_SETTINGS_RESET, false);
            mRecreateLater = savedInstanceState.getBoolean(STATE_RECREATE_LATER, false);
            mScreenRotating = savedInstanceState.getBoolean(STATE_SCREEN_ROTATING, false);
            mLaunchingActivity = savedInstanceState.getBoolean(STATE_LAUNCHING_ACTIVITY, false);
            mDeleteExtraDialog = savedInstanceState.getInt(STATE_DELETE_EXTRA_DIALOG,0);
            mCurrentResourceId = savedInstanceState.getInt(STATE_CURRENT_RESOURCE_ID,0);
        }

        mPrefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(STATE_SETTINGS_RESET,mSettingsReset);
        outState.putBoolean(STATE_RECREATE_LATER,mRecreateLater);
        outState.putBoolean(STATE_SCREEN_ROTATING,mScreenRotating);
        outState.putBoolean(STATE_LAUNCHING_ACTIVITY, mLaunchingActivity);
        outState.putInt(STATE_DELETE_EXTRA_DIALOG, mDeleteExtraDialog);
        outState.putInt(STATE_CURRENT_RESOURCE_ID,mCurrentResourceId);
        super.onSaveInstanceState(outState);
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private Vibrator getVibrator(){
        Vibrator vibrator;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) requireActivity().getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = manager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) requireActivity().getSystemService( Context.VIBRATOR_SERVICE );
        }
        return vibrator;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mImm = (InputMethodManager) requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);

        if (mScreenRotating && mGameActivity != null) {
            try {
                Preference preference = mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[mCurrentResourceId].
                        findPreference(mGameActivity.getDialogFragmentKey());
                final PlayerMapPreference playerPref = (PlayerMapPreference) preference;
                if (playerPref != null) {
                    playerPref.setValue(mGameActivity.getGamePrefs().playerMap.serialize());
                    playerPref.dismissFragments(getActivity());// causing issues
                }
            }
            catch(NullPointerException e){
                e.printStackTrace();
            }
        }

        mScreenRotating = false;

        // By default, send Player 1 rumbles through phone vibrator
        mVibrator = getVibrator();

        if(mGameActivity.getDialogFragmentKey().equals("videoPolygonOffset") && container != null)
            showKeyboard(requireContext(),container.findFocus());

        return inflater.inflate(R.layout.game_settings_header, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        mView = view;
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction().setReorderingAllowed(true);
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        mSettingsFragment = new SettingsFragment(this);
        transaction.replace(R.id.settings_view, mSettingsFragment).commit();
        mView.setBackground(new DrawerDrawable(mGameActivity.getGlobalPrefs().displayGameSettingsTransparency));
    }

    public void recreateView(){
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction().setReorderingAllowed(true);
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        mSettingsFragment = new SettingsFragment(this);
        transaction.replace(R.id.settings_view, mSettingsFragment).commit();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        mPrefs.unregisterOnSharedPreferenceChangeListener(this);

        if(!mGameActivity.isChangingConfigurations()){
            Log.i(TAG,"Set stuff needed from onStop here.");
        }
        else{
            Log.i(TAG,"Set stuff needed from orientation change but that's not as necessary.");
            mScreenRotating = true;
        }
    }

    @Override
    public void onResume()
    {
        Log.i(TAG,"onResume");
        super.onResume();

        // Since merging in_game_settings with the master branch, this is needed
        // if a settings reset occurs and then the user tries to open an activity
        // from the in game settings, the game will resume upon returning from that
        // activity even though that's not desired, should be a temporary fix but may stay
        if(!mGameActivity.getSettingsReset() && mLaunchingActivity) {
            try{
                Thread.sleep(500);
            }
            catch(InterruptedException e){
                e.printStackTrace();
            }
            mGameActivity.onComplete("pauseEmulator");
            mLaunchingActivity = false;
        }

        // Resetting the IPL preference so it shows
        if(mCurrentResourceId == 5)
            waitToResetPreferences();

        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroyView()
    {
        // This is needed because of this:
        // https://code.google.com/p/android/issues/detail?id=17423

        if (getDialog() != null)
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    /* When setting the preferences for the player map, this will be the last check and sets the
    *  available controller input ports based on who is currently on the server */
    public boolean checkOnlinePlayers(int playerNumber){
        if(playerNumber > 3 || playerNumber < 0)
            return false;
        TcpServer.PlayerData playerData = mGameActivity.getNetplayFragment().getTcpServer().getPlayerData(playerNumber);
        return playerData != null;
    }

    public boolean settingsFragmentCheck(){
        return mSettingsFragment != null && mSettingsFragment.fragmentAdapter != null &&
                mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[mCurrentResourceId] != null;
    }

    public void OnPreferenceScreenChange(String key)
    {
        if(!settingsFragmentCheck())
            return;
        mCategoryPasses = mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[mCurrentResourceId].findPreference( CATEGORY_PASSES );

        if (mCategoryPasses == null) {
            resetPreferences();
        } else {
            Preference preference = mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[mCurrentResourceId].findPreference( key );
            if( preference != null )
                preference.setOnPreferenceClickListener( this );

            refreshShaderViews();
        }
    }

    public PlayerMapPreference findPlayerMapPreferenceSettings(){
        if(mSettingsFragment == null || mSettingsFragment.fragmentAdapter == null ||
                mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[4] == null)
            return null;
        return mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[4].findPreference(GlobalPrefs.PLAYER_MAP);
    }

    private void setPreference(String preferenceString, boolean value){
        if(!settingsFragmentCheck())
            return;
        Preference preference;
        preference = mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[mCurrentResourceId].findPreference(preferenceString);
        if (preference != null)
            preference.setEnabled(value);
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private void vibrate(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mVibrator.vibrate(VibrationEffect.createOneShot(FEEDBACK_VIBRATE_TIME, 100));
        } else {
            mVibrator.vibrate(FEEDBACK_VIBRATE_TIME);
        }
    }

    public void showKeyboard(Context context, View view) {
        mImm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        mImm.showSoftInput(view,InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    public boolean keyboardOpen(){
        if(!mGameActivity.getDialogFragmentKey().equals("videoPolygonOffset"))
            return false;
        return mImm != null && mImm.isActive();
    }

    private void playerMapSet(PlayerMapPreference playerPref){
        if (playerPref != null) {
            // Check null in case preference has been removed
            boolean enable1 = mGameActivity.getGlobalPrefs().controllerProfile1 != null;
            boolean enable2 = mGameActivity.getGlobalPrefs().controllerProfile2 != null;
            boolean enable3 = mGameActivity.getGlobalPrefs().controllerProfile3 != null;
            boolean enable4 = mGameActivity.getGlobalPrefs().controllerProfile4 != null;
            if (mGameActivity.getNetplayEnabled()) {
                enable1 = checkOnlinePlayers(0);
                enable2 = checkOnlinePlayers(1);
                enable3 = checkOnlinePlayers(2);
                enable4 = checkOnlinePlayers(3);
            }
            playerPref.setControllersEnabled(enable1, enable2, enable3, enable4);
            playerPref.setValue(mGameActivity.getGamePrefs().playerMap.serialize());
        }
    }

    /* Used to disable settings that reset the game (needed to prevent disconnection in netplay
    *  and data loss if user chooses not to use auto saves) */
    private void disableSettingsThatReset(int currentResource){
        boolean setBool = !(mGameActivity.getGlobalPrefs().maxAutoSaves == 0 || mGameActivity.getNetplayEnabled());
        if(setBool)
            return;

        switch(currentResource) {
            // Display
            case 0:
                if(mGameActivity.getGamePrefs().videoPlugin.name.toLowerCase().contains("glide64"))
                    setPreference("displayResolution", setBool);
                setPreference("displayScaling", setBool);
                setPreference("videoHardwareType", setBool);
                setPreference("threadedGLideN64", setBool);
                setPreference("hybridTextureFilter_v2", setBool);
                setPreference("displayImmersiveMode_v2", setBool);
                setPreference("videoPolygonOffset", setBool);
                break;
            // Audio
            case 2:
                setPreference("audioVolume", setBool);
                setPreference("audioBufferSize", setBool);
                setPreference("audioTimeStretch", setBool);
                setPreference("audioFloatingPoint", setBool);
                setPreference("audioSynchronize", setBool);
                setPreference("audioSwapChannels", setBool);
                setPreference("lowPerformanceMode", setBool);
                setPreference("useHighPriorityThread_v2", setBool);
                break;
            // Input
            case 4:
                setPreference("navigationMode", setBool);
                setPreference("useRaphnetAdapter", setBool);
                break;
            // Data
            case 5:
                setPreference("gameDataStorageType", setBool);
                setPreference("useFlatGameDataPath", setBool);
                setPreference("japanIdlPath64dd", setBool);
                break;
            default:
                break;
        }
    }

    /* Used to remove "Auto" choice in displayOrientation preference to prevent resetting */
    private void disableOrientationListPreference(){
        if (mGameActivity.getGlobalPrefs().maxAutoSaves == 0 || mGameActivity.getNetplayEnabled()) {
            ListPreference displayOri = mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[mCurrentResourceId].findPreference("displayOrientation");
            if(displayOri != null) {
                displayOri.setEntries(new String[]{"Landscape","Reverse Landscape","Portrait","Reverse Portrait"});
                displayOri.setEntryValues(new String[]{"0", "8", "1", "9"});
                if(displayOri.getValue().equals("-1"))
                    displayOri.setSummary("Auto");
                else
                    displayOri.setSummary(displayOri.getEntry());
            }
        }
    }

    /* Waits before resetting the preferences */
    private void waitToResetPreferences(){
        final Thread handler = new Thread(() -> {
            int count = 0;
            while(count++ < 500 && (!settingsFragmentCheck())) {
                try { Thread.sleep(10); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }
            resetPreferences();
            recreateView();
        }); handler.start();
    }

    /* Resets the preferences after the game has successfully reset and there is visual feedback
    *  from the graphics output */
    public void resetPreferencesFromSettingsReset(){
        if(mSettingsFragment == null || mSettingsFragment.fragmentAdapter == null)
            return;
        for(int i = 0; i <= 5; i++) {
            if(mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[i] == null)
                continue;
            mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[i].settingsResetPreferences(true);
        }
        resetPreferences();
    }

    /* This gets updated as soon as a change in settings occurs */
    public void resetPreferences(){
        switch(mCurrentResourceId) {
            case 0:
                if(mGameActivity.getGlobalPrefs().maxAutoSaves == 0)
                    setPreference("videoPolygonOffset",false);
                else
                    setPreference("videoPolygonOffset",mGameActivity.getGlobalPrefs().videoHardwareType == VIDEO_HARDWARE_TYPE_CUSTOM);
                disableSettingsThatReset(mCurrentResourceId);
                disableOrientationListPreference();
                break;
            case 1:
                if(mRemoveShader != -1)
                    mRemoveShader = -1;
                break;
            case 2:
                if(mGameActivity.getGlobalPrefs().maxAutoSaves == 0)
                    setPreference("audioSamplingType",false);
                else
                    setPreference(AUDIO_SAMPLING_TYPE,!mGameActivity.getGlobalPrefs().enableAudioTimeSretching);
                disableSettingsThatReset(mCurrentResourceId);
                break;
            case 3:
                setPreference(GlobalPrefs.KEY_TOUCHSCREEN_SKIN_CUSTOM_PATH,
                        !TextUtils.isEmpty(mGameActivity.getGlobalPrefs().touchscreenSkin) &&
                                mGameActivity.getGlobalPrefs().touchscreenSkin.equals("Custom"));
                break;
            case 4:
                setPreference(GlobalPrefs.PLAYER_MAP,!mGameActivity.getGlobalPrefs().autoPlayerMapping && !mGameActivity.getGlobalPrefs().isControllerShared);
                setPreference("inputVolumeMappable",false);
                setPreference("inputBackMappable",false);
                setPreference("inputMenuMappable",false);

                if(settingsFragmentCheck()) {
                    final PlayerMapPreference playerPref = mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[4].findPreference(GlobalPrefs.PLAYER_MAP);
                    playerMapSet(playerPref);
                }
                disableSettingsThatReset(mCurrentResourceId);
                break;
            case 5 :
                if(mGameActivity.getGlobalPrefs().maxAutoSaves == 0)
                    setPreference("gameDataStoragePath",false);
                else
                    setPreference("gameDataStoragePath",
                            mPrefs.getString(GlobalPrefs.GAME_DATA_STORAGE_TYPE, "internal").equals("external"));
                if(mGameActivity.getNetplayEnabled()) {
                    setPreference("gameDataStoragePath",false);
                    setPreference("gameAutoSaves", false);
                }
                if(!mGameActivity.isDdActive())
                    setPreference("japanIdlPath64dd", false);
                else if(settingsFragmentCheck()) {
                    Preference currentPreference = mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[mCurrentResourceId].findPreference(GlobalPrefs.PATH_JAPAN_IPL_ROM);
                    if (currentPreference != null) {
                        String uri = mGameActivity.getGlobalPrefs().getString(GlobalPrefs.PATH_JAPAN_IPL_ROM, "");

                        if (!TextUtils.isEmpty(uri)) {
                            DocumentFile file = FileUtil.getDocumentFileSingle(mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[mCurrentResourceId].getContext(), Uri.parse(uri));
                            currentPreference.setSummary(file == null ? "" : file.getName());
                        }
                    }
                }
                disableSettingsThatReset(mCurrentResourceId);
                break;
            default:
                break;
        }
    }

    private void shaderLoader(ArrayList<ShaderLoader> shaderPasses, String key){

        String value = mPrefs.getString(key, ShaderLoader.DEFAULT.toString());

        String[] currentPassSplitString = key.split(",");

        if (currentPassSplitString.length == 2) {
            ShaderLoader valueEnum;
            try {
                valueEnum = ShaderLoader.valueOf(value);

            } catch (java.lang.IllegalArgumentException e) {
                valueEnum = null;
            }

            int changedPass = Integer.parseInt(currentPassSplitString[1]) - 1;

            if (changedPass >= 0 && changedPass < shaderPasses.size() && valueEnum != null) {
                shaderPasses.set(changedPass, valueEnum);
                mGameActivity.getGlobalPrefs().putShaderPasses(shaderPasses);

                if(this.mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[mCurrentResourceId] != null)
                    refreshShaderViews();
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        mGameActivity.onComplete(key);

        //CHECK ALL RECREATE VALUES HERE
        if(key.equals("inputShareController") || key.equals("holdButtonForMenu") ||
                (key.equals("displayOrientation") && mGameActivity.getGlobalPrefs().displayOrientation != -1) ||
                (key.equals("displayImmersiveMode_v2") && !mGameActivity.getGlobalPrefs().isImmersiveModeEnabled) )
            mGameActivity.onComplete("settingsRecreate");

        //CHECK ALL RESET VALUES HERE
        if(key.equals("threadedGLideN64") || key.equals("hybridTextureFilter_v2") ||
                key.equals("navigationMode") || key.equals("useRaphnetAdapter") ||
                key.equals("gameDataStorageType") || key.equals("useFlatGameDataPath") ||
                (key.equals("displayImmersiveMode_v2") && mGameActivity.getGlobalPrefs().isImmersiveModeEnabled) ||
                mSettingsFragment.viewPager.getCurrentItem() == 2){
            mSettingsReset = true;
        }

        if(key.equals("playerMap")) {
            mRecreateLater = true;
            mGameActivity.setAssociatedDialogFragment(0);
            if(!mLongClick)
                dialogDeleted();
            else
                mLongClick = false;
            mGameActivity.onComplete("initiateControllers");
        }

        if(key.equals("displayGameSettingsTransparency")){
            mGameActivity.onComplete("resetAppData");
            mView.setBackground(new DrawerDrawable(mGameActivity.getGlobalPrefs().displayGameSettingsTransparency));
        }

        // Vibrating if they activate haptic feedback
        if(key.equals("touchscreenFeedback") && mGameActivity.getGlobalPrefs().isTouchscreenFeedbackEnabled &&
                mVibrator != null){
            vibrate();
        }

        // Checking shaders
        ArrayList<ShaderLoader> shaderPasses = mGameActivity.getGlobalPrefs().getShaderPasses();

        if(mRemoveFirstPassShader){
            mRemoveFirstPassShader = false;
            String keyy = "shaderpass,1";
            shaderLoader(shaderPasses,keyy);
            mGameActivity.onComplete("resetShadersFirstPass");
            shaderPasses.remove(0);
            mPrefs.edit().remove(keyy).apply();
            mGameActivity.getGlobalPrefs().putShaderPasses(shaderPasses);
            mGameActivity.recreate();
        }
        else if (key.startsWith(SHADER_PASS_KEY)) {
            shaderLoader(shaderPasses,key);

            mGameActivity.onComplete("resetShaderScaleFactor");
            mGameActivity.recreate();
        }
        else if(mRemoveShader != -1 && key.equals("shaderPass")){
            mGameActivity.onComplete("resetShaders");
            mRemoveShader = -1;
        }
        else if(key.equals("shaderScaleFactor")){
            shaderLoader(shaderPasses,"shaderpass,1");
            mGameActivity.onComplete("resetSurface");
            mGameActivity.recreate();
        }

        // Setting open dialog values
        mGameActivity.setAssociatedDialogFragment(0);
        if(!key.equals("playerMap")) {
            mGameActivity.setDialogFragmentKey("");
            mDeleteExtraDialog = 0;
        }

        //if change custom
        refreshViews();

        //if need to reset preferences (key.equals("videoHardware") || ...
        resetPreferences();
    }

    public void addShaderPass(ShaderLoader shader, int shaderPass) {
        if (mCategoryPasses != null) {
            ShaderPreference preference = new ShaderPreference(this.mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[mCurrentResourceId].getPreferenceManager().getContext());//getPreferenceManagerContext());
            String key = SHADER_PASS_KEY + shaderPass;
            preference.setKey(key);
            preference.populateShaderOptions(getActivity());

            String title = getString(R.string.shadersPass_title) + " " + shaderPass;
            preference.setTitle(title);
            preference.setSummary(shader.getFriendlyName());
            preference.setValue(shader.toString());
            preference.setOnRemoveCallback(this);

            mCategoryPasses.addPreference(preference);
        }
    }

    private void refreshShaderViews(){
        // Refresh the preferences object
        refreshViews();
        if(!settingsFragmentCheck())
            return;
        PreferenceGroup screenRoot = this.mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[mCurrentResourceId].findPreference(SCREEN_ROOT);
        PreferenceGroup categoryPasses = this.mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[mCurrentResourceId].findPreference(CATEGORY_PASSES);

        if (mCategoryPasses != null) {
            mCategoryPasses.removeAll();
        }

        ArrayList<ShaderLoader> shaderPasses = mGameActivity.getGlobalPrefs().getShaderPasses();

        for (int index = 0; index < shaderPasses.size(); ++index) {
            addShaderPass(shaderPasses.get(index), index + 1);
        }

        // If there are no shaders, then remove the category
        if (mCategoryPasses != null && screenRoot != null) {
            if (shaderPasses.isEmpty()) {
                screenRoot.removePreference(mCategoryPasses);
            } else if (categoryPasses == null) {
                screenRoot.addPreference(mCategoryPasses);
            }
        }
    }

    private void refreshViews(){
        mGameActivity.onComplete("resetAppData");
    }

    /* Used in Data for 64DD IPL */
    public void startFilePicker()
    {
        mLaunchingActivity = true;
        AppData appData = new AppData( mGameActivity.getApplicationContext() );
        Intent intent;
        if (appData.useLegacyFileBrowser) {
            intent = new Intent(getActivity(), LegacyFilePicker.class);
            intent.putExtra( ActivityHelper.Keys.CAN_SELECT_FILE, true );
            intent.putExtra( ActivityHelper.Keys.CAN_VIEW_EXT_STORAGE, true);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
        }
        mLaunchIdlFilePicker.launch(intent);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // Handle the clicks on certain menu items that aren't actually preferences
        final String key = preference.getKey();

        // Tell Android if we handled the click or not
        return ACTION_IMPORT_TOUCHSCREEN_GRAPHICS.equals(key) || ADD_PREFERENCE.equals(key) ||
                GlobalPrefs.PATH_GAME_SAVES.equals(key) || GlobalPrefs.PATH_JAPAN_IPL_ROM.equals(key);
    }

    private Uri getUri(Intent data)
    {
        AppData appData = new AppData( requireActivity().getApplicationContext() );
        Uri returnValue = null;
        if (appData.useLegacyFileBrowser) {
            final Bundle extras = data.getExtras();

            if (extras != null) {
                final String searchUri = extras.getString(ActivityHelper.Keys.SEARCH_PATH);
                returnValue = Uri.parse(searchUri);
            }
        } else {
            returnValue = data.getData();
        }

        return returnValue;
    }

    private boolean importCustomSkin(Uri uri) {

        RomHeader header = new RomHeader( requireActivity().getApplicationContext(), uri );

        if (!header.isZip) {
            Log.e(TAG, "Invalid custom skin file");
            Notifier.showToast(requireActivity().getApplicationContext(), R.string.importExportActivity_invalidCustomSkinFile);
            return false;
        }

        boolean validZip = true;

        ZipInputStream zipfile = null;

        try(ParcelFileDescriptor parcelFileDescriptor = requireActivity().getApplicationContext().getContentResolver().openFileDescriptor(uri, "r"))
        {
            if (parcelFileDescriptor != null) {
                zipfile = new ZipInputStream( new BufferedInputStream(new FileInputStream(parcelFileDescriptor.getFileDescriptor()) ));

                ZipEntry entry = zipfile.getNextEntry();

                while( entry != null && validZip)
                {
                    validZip = mValidSkinFiles.contains(entry.getName());

                    if (!validZip) {
                        Log.e(TAG, entry.getName());
                    }
                    entry = zipfile.getNextEntry();
                }
                zipfile.close();
            }
        }
        catch( Exception|OutOfMemoryError e )
        {
            Log.w(TAG, e);
        }
        finally
        {
            try {
                if( zipfile != null ) {
                    zipfile.close();
                }
            } catch (IOException ignored) {
            }
        }

        if (!validZip) {
            Notifier.showToast(requireActivity().getApplicationContext(), R.string.importExportActivity_invalidCustomSkinFile);
            Log.e(TAG, "Invalid custom skin zip");
            return false;
        }

        File customSkinDir = new File(mGameActivity.getGlobalPrefs().touchscreenCustomSkinsDir);
        FileUtil.deleteFolder(customSkinDir);
        FileUtil.makeDirs(mGameActivity.getGlobalPrefs().touchscreenCustomSkinsDir);
        FileUtil.unzipAll(requireActivity().getApplicationContext(), uri, mGameActivity.getGlobalPrefs().touchscreenCustomSkinsDir);
        return true;
    }

    /* Prevents emulator resuming when coming back from Activity after settings like
    /  inputShareController and holdButtonForMenu have been changed */
    private void recreateAndPause(){
        mGameActivity.recreate();
        mGameActivity.onComplete("pauseEmulator");
    }

    public static class SettingsFragment extends Fragment {
        public GameSettingsDialog gameSettingsDialog;
        public SettingsFragmentAdapter fragmentAdapter;
        public ViewPager2 viewPager;

        public SettingsFragment(){
        }

        public SettingsFragment(GameSettingsDialog mGameSettingsDialog){
            gameSettingsDialog = mGameSettingsDialog;
        }

        public static SettingsFragment newInstance(int resourceId)
        {
            SettingsFragment frag = new SettingsFragment();
            Bundle args = new Bundle();
            args.putInt(STATE_RESOURCE_ID, resourceId);

            frag.setArguments(args);
            return frag;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.game_settings_menu, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            fragmentAdapter = new SettingsFragmentAdapter(this, gameSettingsDialog);

            viewPager = view.findViewById(R.id.settings_view_pager);
            viewPager.setAdapter(fragmentAdapter);

            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {

                @Override
                public void onPageScrollStateChanged(int state) {
                    super.onPageScrollStateChanged(state);
                }

                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                }

                @Override
                public void onPageSelected(int position) {
                    mCurrentResourceId = position;

                    if(fragmentAdapter.mSettingsFragmentPreference[mCurrentResourceId] != null) {
                        fragmentAdapter.mSettingsFragmentPreference[mCurrentResourceId].resetPreferences();

                        if(mCurrentResourceId == 1){
                            if(gameSettingsDialog != null)
                                gameSettingsDialog.OnPreferenceScreenChange("");
                        }
                    }
                    super.onPageSelected(position);
                }
            });
            viewPager.setCurrentItem(mCurrentResourceId,false);

            TabLayout tabLayout = view.findViewById(R.id.tab_layout);
            new TabLayoutMediator(tabLayout, viewPager,
                    (tab, position) -> tab.setText(getResources().getStringArray(R.array.settingsGame_entries)[position])
            ).attach();
        }

        @Override
        public void onSaveInstanceState( Bundle savedInstanceState )
        {
            savedInstanceState.putInt(STATE_CURRENT_RESOURCE_ID, mCurrentResourceId);

            super.onSaveInstanceState( savedInstanceState );
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                mCurrentResourceId = savedInstanceState.getInt(STATE_CURRENT_RESOURCE_ID);
            }
            super.onCreate(savedInstanceState);
        }

    }

    public static class SettingsFragmentPreference extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

        private GameSettingsDialog gameSettingsDialog;
        private int resourceId;

        public SettingsFragmentPreference() {}

        public SettingsFragmentPreference(GameSettingsDialog mGameSettingsDialog, int resourceId) {
            gameSettingsDialog = mGameSettingsDialog;
            this.resourceId = resourceId;
        }
        @Override
        public void onPause()
        {
            super.onPause();
        }

        @Override
        public void onResume()
        {
            super.onResume();
        }

        @Override
        public void onSaveInstanceState( Bundle savedInstanceState )
        {
            savedInstanceState.putInt("resourceId", resourceId);
            super.onSaveInstanceState( savedInstanceState );
        }

        public SettingsFragmentPreference newInstance(String sharedPrefsName, int resourceId, String rootKey)
        {
            SettingsFragmentPreference frag = new SettingsFragmentPreference();//gameSettingsDialog, resourceId);
            Bundle args = new Bundle();
            args.putString(STATE_SHARED_PREFS_NAME, sharedPrefsName);
            args.putInt(STATE_RESOURCE_ID, resourceId);
            args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, rootKey);

            frag.setArguments(args);
            return frag;
        }

        @SuppressWarnings({"deprecation"})
        private void setTargetFragment(DialogFragment fragment){
            if (fragment != null) {
                fragment.setTargetFragment(this, 0);

                try {
                    FragmentManager fm = getParentFragmentManager();
                    fragment.show(fm, "androidx.preference.PreferenceFragment.DIALOG");
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            Log.i("GameSettingsDialogPref","onCreate");
            if(savedInstanceState != null) {
                resourceId = savedInstanceState.getInt("resourceId");
            }
            super.onCreate(savedInstanceState);

            if(!mGameActivity.getDialogFragmentKey().equals("")) {
                Log.i("GameSettingsDialogPref","DialogFragmentKey = "+mGameActivity.getDialogFragmentKey());
                DialogFragment fragment;
                Preference preference = findPreference(mGameActivity.getDialogFragmentKey());

                if(mGameActivity.getDialogFragmentKey().equals("videoPolygonOffset")) {
                    EditTextPreference pref = (EditTextPreference) preference;
                    if(pref != null)
                        onDisplayPreferenceDialog(pref);
                    return;
                }

                fragment = ((AppCompatPreferenceFragment.OnDisplayDialogListener) new DisplayPrefsActivity()).
                        getPreferenceDialogFragment(preference);
                setTargetFragment(fragment);

                // make another
                if(mGameActivity.getAssociatedDialogFragment() != 0){
                    final PlayerMapPreference playerPref = (PlayerMapPreference) preference;
                    if (playerPref != null && gameSettingsDialog != null)
                    {
                        gameSettingsDialog.playerMapSet(playerPref);
                        playerPref.rePromptPlayer(mGameActivity.getAssociatedDialogFragment(),
                                getActivity());// change 1 with int of whichever we change
                    }
                }
            }
        }

        @SuppressLint("NonConstantResourceId")
        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            mGameActivity.setDialogFragmentKey(preference.getKey());
            DialogFragment fragment;
            switch(resourceId){
                case R.xml.preferences_display: default:
                    fragment = ((AppCompatPreferenceFragment.OnDisplayDialogListener) new DisplayPrefsActivity()).
                            getPreferenceDialogFragment(preference);
                    break;
                case R.xml.preferences_audio:
                    fragment = ((AppCompatPreferenceFragment.OnDisplayDialogListener) new AudioPrefsActivity()).
                            getPreferenceDialogFragment(preference);
                    break;
                case R.xml.preferences_data:
                    fragment = ((AppCompatPreferenceFragment.OnDisplayDialogListener) new DataPrefsActivity()).
                            getPreferenceDialogFragment(preference);
                    break;
                case R.xml.preferences_shader:
                    fragment = ((AppCompatPreferenceFragment.OnDisplayDialogListener) new ShaderPrefsActivity()).
                            getPreferenceDialogFragment(preference);
                    break;
                case R.xml.preferences_touchscreen:
                    fragment = ((AppCompatPreferenceFragment.OnDisplayDialogListener) new TouchscreenPrefsActivity()).
                            getPreferenceDialogFragment(preference);
                    break;
                case R.xml.preferences_input:
                    fragment = ((AppCompatPreferenceFragment.OnDisplayDialogListener) new InputPrefsActivity()).
                            getPreferenceDialogFragment(preference);
                    break;
            }
            if(fragment != null)
                setTargetFragment(fragment);
            else super.onDisplayPreferenceDialog(preference);
        }

        /* Used in Touchscreen for graphic import */
        private void startFilePickerForSingle()
        {
            gameSettingsDialog.mLaunchingActivity = true;
            AppData appData = new AppData( requireActivity());
            Intent intent;
            if (appData.useLegacyFileBrowser) {
                intent = new Intent(getActivity(), LegacyFilePicker.class);
                intent.putExtra( ActivityHelper.Keys.CAN_VIEW_EXT_STORAGE, true);
                intent.putExtra( ActivityHelper.Keys.CAN_SELECT_FILE, true );
            } else {
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
            }
            gameSettingsDialog.mLaunchFilePicker.launch(intent);
        }

        /* Used in Data for save location */
        private void startFolderPicker()
        {
            gameSettingsDialog.mLaunchingActivity = true;
            Intent intent;
            AppData appData = new AppData( mGameActivity.getApplicationContext() );
            if (appData.useLegacyFileBrowser) {
                intent = new Intent(getActivity(), LegacyFilePicker.class);
                intent.putExtra( ActivityHelper.Keys.CAN_SELECT_FILE, false );
                intent.putExtra( ActivityHelper.Keys.CAN_VIEW_EXT_STORAGE, false);
            } else {
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                        Intent.FLAG_GRANT_READ_URI_PERMISSION|
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            }
            gameSettingsDialog.mLaunchGameDataFolderPicker.launch(intent);
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            final String key = preference.getKey();

            if (ACTION_IMPORT_TOUCHSCREEN_GRAPHICS.equals(key)) {
                startFilePickerForSingle();
            }
            else if (ADD_PREFERENCE.equals(key)) {
                if(gameSettingsDialog.mCategoryPasses == null){
                    gameSettingsDialog.mCategoryPasses = findPreference( CATEGORY_PASSES );
                }
                ArrayList<ShaderLoader> shaderPasses = mGameActivity.getGlobalPrefs().getShaderPasses();
                if (shaderPasses.size() < MAX_SHADER_PASSES) {
                    shaderPasses.add(ShaderLoader.DEFAULT);
                    mGameActivity.getGlobalPrefs().putShaderPasses(shaderPasses);
                    gameSettingsDialog.refreshShaderViews();
                    if(gameSettingsDialog.mCategoryPasses == null) {
                        gameSettingsDialog.recreateView();
                    }
                }
            }
            else if (GlobalPrefs.PATH_GAME_SAVES.equals(key)) {
                startFolderPicker();
            }
            else if (GlobalPrefs.PATH_JAPAN_IPL_ROM.equals(key)) {
                try {
                    FragmentActivity activity = requireActivity();
                    String title = activity.getString(R.string.confirm_title);
                    String message = activity.getString(R.string.confirmResetGame_message);

                    ConfirmationDialog confirmationDialog =
                            ConfirmationDialog.newInstance(RESET_SETTINGS_CONFIRM_DIALOG_ID, title, message);

                    FragmentManager fm = activity.getSupportFragmentManager();
                    confirmationDialog.show(fm, RESET_SETTINGS_CONFIRM_DIALOG);
                } catch (java.lang.IllegalStateException e) {
                    e.printStackTrace();
                }
            }
            else {// Let Android handle all other preference clicks
                return false;
            }

            // Tell Android that we handled the click
            return super.onPreferenceTreeClick(preference);
        }

        public void resetPreferences(){
            if(gameSettingsDialog != null) {
                if (mCurrentResourceId == 1)
                    gameSettingsDialog.OnPreferenceScreenChange("");
                else
                    gameSettingsDialog.resetPreferences();
            }

            // Waiting for plugins to initialize
            if(mGameActivity.getSettingsReset())
                settingsResetPreferences(false);
        }

        public void settingsResetPreferences(boolean enabled){
            if(getPreferenceManager() == null || getPreferenceScreen() == null)
                return;
            for(int i = 0; i <= getPreferenceScreen().getPreferenceCount()-1; i++)
                getPreferenceManager().getPreferenceScreen().getPreference(i).setEnabled(enabled);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

            Bundle arguments = getArguments();

            if (arguments == null) {
                setPreferencesFromResource(resourceId, rootKey);
                resetPreferences();
                return;
            }

            final String sharedPrefsName = arguments.getString(STATE_SHARED_PREFS_NAME);
            final int resourceId = arguments.getInt(STATE_RESOURCE_ID);

            // Load the preferences from an XML resource

            if (sharedPrefsName != null)
            {
                getPreferenceManager().setSharedPreferencesName(sharedPrefsName);
            }

            if (rootKey == null)
            {
                addPreferencesFromResource(resourceId);
            }
            else
            {
                setPreferencesFromResource(resourceId, rootKey);
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        }
    }

    public static class SettingsFragmentAdapter extends FragmentStateAdapter {

        public SettingsFragmentPreference[] mSettingsFragmentPreference = new SettingsFragmentPreference[6];
        private final GameSettingsDialog gameSettingsDialog;

        public SettingsFragmentAdapter(@NonNull Fragment fragment, GameSettingsDialog mGameSettingsDialog) {
            super(fragment);
            this.gameSettingsDialog = mGameSettingsDialog;
        }

        @Override
        public long getItemId(int position) {
            return super.getItemId(position);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:     // Display
                    return mSettingsFragmentPreference[0] = new SettingsFragmentPreference(gameSettingsDialog, R.xml.preferences_display);
                case 1:     // Shaders
                    return mSettingsFragmentPreference[1] = new SettingsFragmentPreference(gameSettingsDialog, R.xml.preferences_shader);
                case 2:     // Audio
                    return mSettingsFragmentPreference[2] = new SettingsFragmentPreference(gameSettingsDialog, R.xml.preferences_audio);
                case 3:     // Touchscreen
                    return mSettingsFragmentPreference[3] = new SettingsFragmentPreference(gameSettingsDialog, R.xml.preferences_touchscreen);
                case 4:     // Input
                    return mSettingsFragmentPreference[4] = new SettingsFragmentPreference(gameSettingsDialog, R.xml.preferences_input);
                case 5:     // Data
                    return mSettingsFragmentPreference[5] = new SettingsFragmentPreference(gameSettingsDialog, R.xml.preferences_data);
                default:
                    return new Fragment();
            }
        }

        @Override
        public int getItemCount() {
            return 6;
        }
    }

    public interface OnPreferenceDialogListener
    {
        /**
         * Called when the dialog is closed
         */
        void onDialogClosed(boolean result);
    }

    @Override
    public boolean onPreferenceStartScreen(@NonNull PreferenceFragmentCompat preferenceFragmentCompat,
                                           PreferenceScreen preferenceScreen)
    {
        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        AppCompatPreferenceFragment fragment = AppCompatPreferenceFragment.newInstance(mSharedPrefsName,
                mPreferencesResId, preferenceScreen.getKey());
        ft.replace(android.R.id.content, fragment, preferenceScreen.getKey());
        ft.addToBackStack(null);
        ft.commit();

        return true;
    }

    // Deleting extra dialog when screen gets rotated
    public void extraDialogCheck(){
        Log.i("GameSettingsDialog","Dialog Check DeleteExtraDialog = "+mDeleteExtraDialog+
                " DialogFragmentKey = "+mGameActivity.getDialogFragmentKey());
        if(mDeleteExtraDialog >= 1)
            mDeleteExtraDialog = 0;
        else if(!mScreenRotating) {
            Log.i("GameSettingsDialog","extraDialogCheck resetting dialogFragmentKey which is currently "+mGameActivity.getDialogFragmentKey());
            mGameActivity.setDialogFragmentKey("");
        }
    }

    // Checking for nested dialog (like someone setting a controller within player map)
    // when we close the settings fragment
    public void nestedDialogCheck(){
        if(!mScreenRotating)
            mGameActivity.setAssociatedDialogFragment(0);
    }

    // Checking for player map dialog when we close the settings fragment
    public void playerMapDialogCheck(int mSelectedPlayer){
        if(!mScreenRotating){
            int associatedFragment = 0;
            final FragmentManager fm = mGameActivity.getSupportFragmentManager();
            PromptInputCodeDialog promptInputCodeDialog = (PromptInputCodeDialog) fm.findFragmentByTag(STATE_PROMPT_INPUT_CODE_DIALOG);
            if (promptInputCodeDialog != null)
                associatedFragment = mSelectedPlayer;
            mGameActivity.setAssociatedDialogFragment(associatedFragment);
        }
    }

    // Used when we get rid of a nested dialog to indicate we don't need to delete more later
    public void dialogDeleted(){
        if(mDeleteExtraDialog == 1){
            mDeleteExtraDialog++;
        }
        else if(mDeleteExtraDialog >= 2)
            mDeleteExtraDialog = 0;
    }

    // This happens when using a long click to disable a controller in player map, it triggers
    // something to blank out mDialogFragmentKey so we become aware when it happens and try to
    // ignore its effects
    public void setLongClickOnDialog(boolean longClick){
        this.mLongClick = longClick;
    }

}