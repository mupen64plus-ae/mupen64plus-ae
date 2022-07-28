package paulscode.android.mupen64plusae.dialog;

import static android.app.Activity.RESULT_OK;

import static paulscode.android.mupen64plusae.persistent.GlobalPrefs.AUDIO_SAMPLING_TYPE;

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
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
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
import paulscode.android.mupen64plusae.compat.AppCompatPreferenceFragment;
import paulscode.android.mupen64plusae.game.GameActivity;
import paulscode.android.mupen64plusae.game.ShaderLoader;
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
        Preference.OnPreferenceClickListener, ShaderPreference.OnRemove, AppCompatPreferenceFragment.OnDisplayDialogListener,
        PreferenceFragmentCompat.OnPreferenceStartScreenCallback
{
    private SettingsFragment mSettingsFragment;
    private Vibrator mVibrator;
    private InputMethodManager mImm;
    private final ArrayList<String> mValidSkinFiles = new ArrayList<>();
    private final String TAG = "GameSettingsDialog";
    private static final String ACTION_IMPORT_TOUCHSCREEN_GRAPHICS = "actionImportTouchscreenGraphics";
    private static final String STATE_CURRENT_RESOURCE_ID = "STATE_CURRENT_RESOURCE_ID";
    private static final String STATE_SHARED_PREFS_NAME = "STATE_SHARED_PREFS_NAME";
    private static final String STATE_RESOURCE_ID = "STATE_RESOURCE_ID";
    private static final String STATE_PREFERENCE_FRAGMENT = "STATE_PREFERENCE_FRAGMENT";
    private static final String STATE_SETTINGS_RESET = "STATE_SETTINGS_RESET";
    private static final String STATE_RECREATE_LATER = "STATE_RECREATE_LATER";
    private static final String STATE_SCREEN_ROTATING = "STATE_SCREEN_ROTATING";
    private static final String STATE_DELETE_EXTRA_DIALOG = "STATE_DELETE_EXTRA_DIALOG";
    private static final String VIDEO_POLYGON_OFFSET = "videoPolygonOffset";
    private static final int PICK_FILE_IMPORT_TOUCHSCREEN_GRAPHICS_REQUEST_CODE = 5;
    private static final int VIDEO_HARDWARE_TYPE_CUSTOM = 999;
    private static final int FEEDBACK_VIBRATE_TIME = 50;
    private static int mCurrentResourceId = 0;
    private boolean mLaunchingActivity = false;//delete
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
    private static String mSharedPrefsName = null;
    private static int mPreferencesResId;

    public interface OnGameSettingsDialogListener {
        void settingsViewReset();
        void recreateSurface();
        void recreate();
        Context getApplicationContext();
        Object getSystemService(String name);
        FragmentManager getSupportFragmentManager();
        GlobalPrefs getGlobalPrefs();
        GamePrefs getGamePrefs();
        boolean isChangingConfigurations();
        boolean getResolutionReset();
        void setDialogFragmentKey(String key);
        String getDialogFragmentKey();
        void setAssociatedDialogFragment(int associatedDialogFragment);
        int getAssociatedDialogFragment();
        void onComplete(String key);
    }

    private static OnGameSettingsDialogListener mGameActivity;

    public static GameSettingsDialog newInstance() {
        GameSettingsDialog frag = new GameSettingsDialog();
        Bundle args = new Bundle();
        args.putInt(STATE_CURRENT_RESOURCE_ID,mCurrentResourceId);

        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle args = getArguments();
        if( args != null ){
            mCurrentResourceId = args.getInt(STATE_CURRENT_RESOURCE_ID,0);
            mSettingsReset = args.getBoolean(STATE_SETTINGS_RESET, false);
            mRecreateLater = args.getBoolean(STATE_RECREATE_LATER, false);
            mScreenRotating = args.getBoolean(STATE_SCREEN_ROTATING, false);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // check if we're launching activity from settings menu
        if (!mGameActivity.getResolutionReset() && !mLaunchingActivity) {
            if(!mSettingsReset && !mRecreateLater)
                mGameActivity.settingsViewReset();
            try {
                dismiss();
                mGameActivity.setDialogFragmentKey("");

                if(mSettingsReset) {
                    mSettingsReset = false;
                    mGameActivity.onComplete("resolutionRefresh");
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
            } catch (Exception e) {
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
        mGameActivity.onComplete("resolutionRefresh");
        mDeleteExtraDialog = 1;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);

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
        mLaunchingActivity = false;

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
            mDeleteExtraDialog = savedInstanceState.getInt(STATE_DELETE_EXTRA_DIALOG,0);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(STATE_SETTINGS_RESET,mSettingsReset);
        outState.putBoolean(STATE_RECREATE_LATER,mRecreateLater);
        outState.putBoolean(STATE_SCREEN_ROTATING,mScreenRotating);
        outState.putInt(STATE_DELETE_EXTRA_DIALOG, mDeleteExtraDialog);
        super.onSaveInstanceState(outState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        mImm = (InputMethodManager) requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);

        if (mScreenRotating && mGameActivity != null) {
            try {
                Preference preference = mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[mCurrentResourceId].
                        findPreference(mGameActivity.getDialogFragmentKey());
                final PlayerMapPreference playerPref = (PlayerMapPreference) preference;
                if (playerPref != null) {
                    playerPref.setValue(mGameActivity.getGamePrefs().playerMap.serialize());
                    playerPref.dismissFragments((FragmentActivity) getActivity());// causing issues
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

        mScreenRotating = false;

        // By default, send Player 1 rumbles through phone vibrator
        Vibrator vibrator;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) requireActivity().getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = manager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) requireActivity().getSystemService( Context.VIBRATOR_SERVICE );
        }
        mVibrator = vibrator;

        if(mGameActivity.getDialogFragmentKey().equals("videoPolygonOffset") && container != null)
            showKeyboard(requireContext(),container.findFocus());

        return inflater.inflate(R.layout.game_settings_header, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction().setReorderingAllowed(true);
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        mSettingsFragment = new SettingsFragment(this);
        transaction.replace(R.id.settings_view, mSettingsFragment).commit();
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
        }
    }

    @Override
    public void onResume()
    {
        Log.i(TAG,"onResume");
        super.onResume();

        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroyView()
    {
        // This is needed because of this:
        // https://code.google.com/p/android/issues/detail?id=17423

        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }


    public void OnPreferenceScreenChange(String key)
    {
        mCategoryPasses = (PreferenceGroup) mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[mCurrentResourceId].findPreference( CATEGORY_PASSES );

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
        return (PlayerMapPreference) mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[4].findPreference(GlobalPrefs.PLAYER_MAP);
    }

    private void setPreference(String preferenceString, boolean value){
        Preference preference;
        preference = mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[mCurrentResourceId].findPreference(preferenceString);
        if (preference != null)
            preference.setEnabled(value);
    }

    public void showKeyboard(Context context, View view) {
        mImm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        mImm.showSoftInput(view,InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    public boolean keyboardOpen(){
        if(!mGameActivity.getDialogFragmentKey().equals("videoPolygonOffset"))
            return false;
        if(mImm != null && mImm.isActive()) {
            return true;
        }
        return false;
    }

    private void disableSettingsThatReset(int currentResource){
        boolean setBool = true;
        if(mGameActivity.getGlobalPrefs().maxAutoSaves == 0)
            setBool = false;

        switch(currentResource) {
            // Display
            case 0:
                setPreference("displayResolution", setBool);
                setPreference("displayOrientation", setBool);
                setPreference("displayScaling", setBool);
                setPreference("videoHardwareType", setBool);
                setPreference("hybridTextureFilter_v2", setBool);
                setPreference("displayImmersiveMode_v2", setBool);
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

    public void resetPreferencesFromResolutionReset(){
        if(mSettingsFragment == null || mSettingsFragment.fragmentAdapter == null ||
                mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[mCurrentResourceId] == null)
            return;
        mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[mCurrentResourceId].resolutionResetPreferences(true);
        resetPreferences();
    }

    // This gets updated as soon as a change in settings occurs
    public void resetPreferences(){
        switch(mCurrentResourceId) {
            case 0:
                if(mGameActivity.getGlobalPrefs().maxAutoSaves == 0)
                    setPreference("videoPolygonOffset",false);
                else
                    setPreference(VIDEO_POLYGON_OFFSET,mGameActivity.getGlobalPrefs().videoHardwareType == VIDEO_HARDWARE_TYPE_CUSTOM);
                disableSettingsThatReset(mCurrentResourceId);
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

                final PlayerMapPreference playerPref = (PlayerMapPreference) mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[4].findPreference(GlobalPrefs.PLAYER_MAP);
                if (playerPref != null)
                {
                    // Check null in case preference has been removed
                    final boolean enable1 = mGameActivity.getGlobalPrefs().controllerProfile1 != null;
                    final boolean enable2 = mGameActivity.getGlobalPrefs().controllerProfile2 != null;
                    final boolean enable3 = mGameActivity.getGlobalPrefs().controllerProfile3 != null;
                    final boolean enable4 = mGameActivity.getGlobalPrefs().controllerProfile4 != null;
                    playerPref.setControllersEnabled(enable1, enable2, enable3, enable4);


                    playerPref.setValue( mGameActivity.getGamePrefs().playerMap.serialize() );
                }
                disableSettingsThatReset(mCurrentResourceId);
                break;
            case 5 :
                if(mGameActivity.getGlobalPrefs().maxAutoSaves == 0)
                    setPreference("gameDataStoragePath",false);
                else
                    setPreference("gameDataStoragePath",
                            mPrefs.getString(GlobalPrefs.GAME_DATA_STORAGE_TYPE, "internal").equals("external"));
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
        if(key.equals("threadedGLideN64") || key.equals("inputShareController") ||
                key.equals("holdButtonForMenu") ||
                (key.equals("displayOrientation") && mGameActivity.getGlobalPrefs().displayOrientation != -1) ||
                (key.equals("displayImmersiveMode_v2") && !mGameActivity.getGlobalPrefs().isImmersiveModeEnabled) )
            mGameActivity.onComplete("settingsRecreate");

        //CHECK ALL RESET VALUES HERE
        if(key.equals("hybridTextureFilter_v2") || key.equals("navigationMode") ||
                key.equals("useRaphnetAdapter") ||
                (key.equals("displayImmersiveMode_v2") && mGameActivity.getGlobalPrefs().isImmersiveModeEnabled) ||
                mSettingsFragment.viewPager.getCurrentItem() == 2 ||
                (mSettingsFragment.viewPager.getCurrentItem() == 5 && !key.equals("gameDataStorageType"))) {
            mSettingsReset = true;
        }

        if(key.equals("playerMap")) {
            mRecreateLater = true;
            mGameActivity.setAssociatedDialogFragment(0);
            if(!mLongClick)
                dialogDeleted();
            else
                mLongClick = false;
        }

        // Vibrating if they activate haptic feedback
        if(key.equals("touchscreenFeedback") && mGameActivity.getGlobalPrefs().isTouchscreenFeedbackEnabled &&
                mVibrator != null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mVibrator.vibrate(VibrationEffect.createOneShot(FEEDBACK_VIBRATE_TIME, 100));
            } else {
                mVibrator.vibrate(FEEDBACK_VIBRATE_TIME);
            }
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
        PreferenceGroup screenRoot = (PreferenceGroup) this.mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[mCurrentResourceId].findPreference(SCREEN_ROOT);
        PreferenceGroup categoryPasses = (PreferenceGroup) this.mSettingsFragment.fragmentAdapter.mSettingsFragmentPreference[mCurrentResourceId].findPreference(CATEGORY_PASSES);

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

    private void startFilePickerForSingle(int requestCode, int permissions)
    {
        mLaunchingActivity = true;
        AppData appData = new AppData( mGameActivity.getApplicationContext() );
        if (appData.useLegacyFileBrowser) {
            Intent intent = new Intent(getActivity(), LegacyFilePicker.class);
            intent.putExtra( ActivityHelper.Keys.CAN_VIEW_EXT_STORAGE, true);
            intent.putExtra( ActivityHelper.Keys.CAN_SELECT_FILE, true );
            requireActivity().startActivityForResult( intent, requestCode );
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.addFlags(permissions);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
            requireActivity().startActivityForResult( intent, requestCode );
        }
    }

    private void startFolderPicker()
    {
        mLaunchingActivity = true;
        Intent intent;
        int requestCode;
        AppData appData = new AppData( mGameActivity.getApplicationContext() );
        if (appData.useLegacyFileBrowser) {
            intent = new Intent(getActivity(), LegacyFilePicker.class);
            intent.putExtra( ActivityHelper.Keys.CAN_SELECT_FILE, false );
            intent.putExtra( ActivityHelper.Keys.CAN_VIEW_EXT_STORAGE, false);
            requestCode = DataPrefsActivity.LEGACY_FOLDER_PICKER_REQUEST_CODE;
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                    Intent.FLAG_GRANT_READ_URI_PERMISSION|
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            requestCode = DataPrefsActivity.FOLDER_PICKER_REQUEST_CODE;
        }
        requireActivity().startActivityForResult(intent, requestCode);
    }

    private void startFilePicker()
    {
        mLaunchingActivity = true;
        AppData appData = new AppData( mGameActivity.getApplicationContext() );
        if (appData.useLegacyFileBrowser) {
            Intent intent = new Intent(getActivity(), LegacyFilePicker.class);
            intent.putExtra( ActivityHelper.Keys.CAN_SELECT_FILE, true );
            intent.putExtra( ActivityHelper.Keys.CAN_VIEW_EXT_STORAGE, true);
            requireActivity().startActivityForResult( intent, DataPrefsActivity.LEGACY_FILE_PICKER_REQUEST_CODE );
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
            requireActivity().startActivityForResult(intent, DataPrefsActivity.FILE_PICKER_REQUEST_CODE);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // Handle the clicks on certain menu items that aren't actually preferences
        final String key = preference.getKey();

        if (ACTION_IMPORT_TOUCHSCREEN_GRAPHICS.equals(key)) {
            startFilePickerForSingle(PICK_FILE_IMPORT_TOUCHSCREEN_GRAPHICS_REQUEST_CODE, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        else if (GlobalPrefs.PATH_GAME_SAVES.equals(key)) {
            startFolderPicker();
        }
        else if (GlobalPrefs.PATH_JAPAN_IPL_ROM.equals(key)) {
            startFilePicker();
        }
        else {// Let Android handle all other preference clicks
            return false;
        }

        // Tell Android that we handled the click
        return true;
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

    // Prevents emulator resuming when coming back from Activity after settings like
    // inputShareController and holdButtonForMenu have been changed
    private void recreateAndPause(){
        mGameActivity.recreate();
        mGameActivity.onComplete("pauseEmulator");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Uri fileUri;
        if (resultCode == RESULT_OK && data != null) {
            fileUri = getUri(data);

            if (requestCode == PICK_FILE_IMPORT_TOUCHSCREEN_GRAPHICS_REQUEST_CODE) {
                if(importCustomSkin(fileUri)){
                    mGameActivity.getGlobalPrefs().putBoolean("isCustomTouchscreenSkin",true);
                    mGameActivity.getGlobalPrefs().putString("touchscreenSkin_v2","Custom");
                }
                mGameActivity.onComplete("resetTouchscreenController");
                mGameActivity.recreateSurface();
                recreateAndPause();
            }

            if (requestCode == DataPrefsActivity.FOLDER_PICKER_REQUEST_CODE) {
                // The result data contains a URI for the document or directory that
                // the user selected.
                fileUri = data.getData();

                if (fileUri != null) {
                    requireActivity().getContentResolver().takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    mGameActivity.getGlobalPrefs().putString(GlobalPrefs.PATH_GAME_SAVES, fileUri.toString());

                    mGameActivity.onComplete("resetAppData");

                    mGameActivity.onComplete("gameDataStoragePath");
                    recreateAndPause();
                }
            } else if (requestCode == DataPrefsActivity.LEGACY_FOLDER_PICKER_REQUEST_CODE) {
                final Bundle extras = data.getExtras();

                if (extras != null) {
                    final String searchUri = extras.getString(ActivityHelper.Keys.SEARCH_PATH);
                    fileUri = Uri.parse(searchUri);

                    if (fileUri != null) {
                        DocumentFile file = FileUtil.getDocumentFileTree(getActivity(), fileUri);
                        mGameActivity.getGlobalPrefs().putString(GlobalPrefs.PATH_GAME_SAVES, fileUri.toString());

                        mGameActivity.onComplete("resetAppData");
                        recreateAndPause();
                    }
                }
            } else if (requestCode == DataPrefsActivity.FILE_PICKER_REQUEST_CODE) {
                // The result data contains a URI for the document or directory that
                // the user selected.
                if (data != null) {
                    fileUri = data.getData();

                    if (fileUri != null) {

                        requireActivity().getContentResolver().takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        DocumentFile file = FileUtil.getDocumentFileSingle(getActivity(), fileUri);
                        mGameActivity.getGlobalPrefs().putString(GlobalPrefs.PATH_JAPAN_IPL_ROM, fileUri.toString());
                        mGameActivity.onComplete("resetAppData");
                        recreateAndPause();
                    }
                }
            }else if (requestCode == DataPrefsActivity.LEGACY_FILE_PICKER_REQUEST_CODE) {
                final Bundle extras = data.getExtras();

                if (extras != null) {
                    final String searchUri = extras.getString(ActivityHelper.Keys.SEARCH_PATH);
                    fileUri = Uri.parse(searchUri);

                    if (fileUri != null && fileUri.getPath() != null) {
                        mGameActivity.getGlobalPrefs().putString(GlobalPrefs.PATH_JAPAN_IPL_ROM, fileUri.toString());
                        mGameActivity.onComplete("resetAppData");
                        recreateAndPause();
                    }
                }
            }

        }
        else {
            recreateAndPause();
        }
    }

    public void addPreferencesFromResource(String sharedPrefsName, int preferencesResId)
    {
        mSharedPrefsName = sharedPrefsName;
        mPreferencesResId = preferencesResId;

        if(mSettingsFragment == null)
        {
            mSettingsFragment = SettingsFragment.newInstance(mPreferencesResId);
            getParentFragmentManager().beginTransaction()
                    .replace(android.R.id.content, mSettingsFragment, STATE_PREFERENCE_FRAGMENT).commit();
        }
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
            mPrefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());
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
        private static SharedPreferences mPrefs;

        public SettingsFragmentPreference() {}

        public SettingsFragmentPreference(GameSettingsDialog mGameSettingsDialog, int resourceId) {
            gameSettingsDialog = mGameSettingsDialog;
            this.resourceId = resourceId;
        }
        @Override
        public void onPause()
        {
            super.onPause();

            mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onResume()
        {
            super.onResume();

            mPrefs.registerOnSharedPreferenceChangeListener(this);
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


        @Override
        public void onCreate(Bundle savedInstanceState) {
            Log.i("GameSettingsDialogPref","onCreate");
            if(savedInstanceState != null) {
                resourceId = savedInstanceState.getInt("resourceId");
            }
            super.onCreate(savedInstanceState);

            mPrefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());

            if(!mGameActivity.getDialogFragmentKey().equals("")) {
                Log.i("GameSettingsDialogPref","DialogFragmentKey = "+mGameActivity.getDialogFragmentKey());
                DialogFragment fragment = null;
                Preference preference = findPreference(mGameActivity.getDialogFragmentKey());

                if(mGameActivity.getDialogFragmentKey().equals("videoPolygonOffset")) {
                    EditTextPreference pref = (EditTextPreference) preference;
                    if(pref != null)
                        onDisplayPreferenceDialog(pref);
                    return;
                }

                fragment = ((AppCompatPreferenceFragment.OnDisplayDialogListener) new DisplayPrefsActivity()).
                        getPreferenceDialogFragment(preference);
                if (fragment != null) {
                    fragment.setTargetFragment(this, 0);

                    try {
                        FragmentManager fm = getParentFragmentManager();
                        fragment.show(fm, "androidx.preference.PreferenceFragment.DIALOG");
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }

                // make another
                if(mGameActivity.getAssociatedDialogFragment() != 0){
                    final PlayerMapPreference playerPref = (PlayerMapPreference) preference;
                    if (playerPref != null)
                    {
                        // Check null in case preference has been removed
                        final boolean enable1 = mGameActivity.getGlobalPrefs().controllerProfile1 != null;
                        final boolean enable2 = mGameActivity.getGlobalPrefs().controllerProfile2 != null;
                        final boolean enable3 = mGameActivity.getGlobalPrefs().controllerProfile3 != null;
                        final boolean enable4 = mGameActivity.getGlobalPrefs().controllerProfile4 != null;
                        playerPref.setControllersEnabled(enable1, enable2, enable3, enable4);

                        playerPref.setValue( mGameActivity.getGamePrefs().playerMap.serialize() );
                        playerPref.rePromptPlayer(mGameActivity.getAssociatedDialogFragment(),
                                (FragmentActivity) getActivity());// change 1 with int of whichever we change
                    }
                }
            }
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            mGameActivity.setDialogFragmentKey(preference.getKey());
            DialogFragment fragment = null;
            switch(resourceId){
                case R.xml.preferences_display:
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
                default:
                    fragment = null;
                    break;
            }
            if (fragment != null) {
                fragment.setTargetFragment(this, 0);

                try {
                    FragmentManager fm = getParentFragmentManager();
                    fragment.show(fm, "androidx.preference.PreferenceFragment.DIALOG");
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
            else super.onDisplayPreferenceDialog(preference);
        }

        private void startFilePickerForSingle(int requestCode, int permissions)
        {
            gameSettingsDialog.mLaunchingActivity = true;
            AppData appData = new AppData( requireActivity());
            if (appData.useLegacyFileBrowser) {
                Intent intent = new Intent(getActivity(), LegacyFilePicker.class);
                intent.putExtra( ActivityHelper.Keys.CAN_VIEW_EXT_STORAGE, true);
                intent.putExtra( ActivityHelper.Keys.CAN_SELECT_FILE, true );
                requireActivity().startActivityForResult(intent, requestCode);
            } else {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.addFlags(permissions);
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
                requireActivity().startActivityForResult(intent, requestCode);
            }
        }

        private void startFolderPicker()
        {
            gameSettingsDialog.mLaunchingActivity = true;
            Intent intent;
            int requestCode;
            AppData appData = new AppData( mGameActivity.getApplicationContext() );
            if (appData.useLegacyFileBrowser) {
                intent = new Intent(getActivity(), LegacyFilePicker.class);
                intent.putExtra( ActivityHelper.Keys.CAN_SELECT_FILE, false );
                intent.putExtra( ActivityHelper.Keys.CAN_VIEW_EXT_STORAGE, false);
                requestCode = DataPrefsActivity.LEGACY_FOLDER_PICKER_REQUEST_CODE;
            } else {
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                        Intent.FLAG_GRANT_READ_URI_PERMISSION|
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                requestCode = DataPrefsActivity.FOLDER_PICKER_REQUEST_CODE;
            }
            requireActivity().startActivityForResult(intent, requestCode);
        }

        private void startFilePicker()
        {
            gameSettingsDialog.mLaunchingActivity = true;
            AppData appData = new AppData( mGameActivity.getApplicationContext() );
            if (appData.useLegacyFileBrowser) {
                Intent intent = new Intent(getActivity(), LegacyFilePicker.class);
                intent.putExtra( ActivityHelper.Keys.CAN_SELECT_FILE, true );
                intent.putExtra( ActivityHelper.Keys.CAN_VIEW_EXT_STORAGE, true);
                startActivityForResult( intent, DataPrefsActivity.LEGACY_FILE_PICKER_REQUEST_CODE );
            } else {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
                requireActivity().startActivityForResult(intent, DataPrefsActivity.FILE_PICKER_REQUEST_CODE);
            }
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            final String key = preference.getKey();

            if (ACTION_IMPORT_TOUCHSCREEN_GRAPHICS.equals(key)) {
                startFilePickerForSingle(PICK_FILE_IMPORT_TOUCHSCREEN_GRAPHICS_REQUEST_CODE, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            else if (ADD_PREFERENCE.equals(key)) {
                if(gameSettingsDialog.mCategoryPasses == null){
                    gameSettingsDialog.mCategoryPasses = (PreferenceGroup) findPreference( CATEGORY_PASSES );
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
                mGameActivity.onComplete("gameDataStoragePath");
            }
            else if (GlobalPrefs.PATH_JAPAN_IPL_ROM.equals(key)) {
                startFilePicker();
            }
            else {// Let Android handle all other preference clicks
                return false;
            }

            // Tell Android that we handled the click
            return super.onPreferenceTreeClick(preference);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (resultCode == RESULT_OK && data != null) {
                Uri fileUri;
                if (requestCode == DataPrefsActivity.FOLDER_PICKER_REQUEST_CODE) {
                    // The result data contains a URI for the document or directory that
                    // the user selected.
                    fileUri = data.getData();

                    Preference currentPreference = findPreference(GlobalPrefs.PATH_GAME_SAVES);
                    if (currentPreference != null && fileUri != null) {
                        requireActivity().getContentResolver().takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                        DocumentFile file = FileUtil.getDocumentFileTree(getActivity(), fileUri);
                        String summary = file.getName();
                        currentPreference.setSummary(summary);
                        mGameActivity.getGlobalPrefs().putString(GlobalPrefs.PATH_GAME_SAVES, fileUri.toString());

                        resetPreferences();
                        mGameActivity.onComplete("resetAppData");
                    }
                } else if (requestCode == DataPrefsActivity.LEGACY_FOLDER_PICKER_REQUEST_CODE) {
                    final Bundle extras = data.getExtras();
                    if (extras != null) {
                        final String searchUri = extras.getString(ActivityHelper.Keys.SEARCH_PATH);
                        fileUri = Uri.parse(searchUri);
                        Preference currentPreference = findPreference(GlobalPrefs.PATH_GAME_SAVES);

                        if (currentPreference != null && fileUri != null) {
                            DocumentFile file = FileUtil.getDocumentFileTree(getActivity(), fileUri);
                            String summary = file.getName();
                            currentPreference.setSummary(summary);
                            mGameActivity.getGlobalPrefs().putString(GlobalPrefs.PATH_GAME_SAVES, fileUri.toString());

                            resetPreferences();
                            mGameActivity.onComplete("resetAppData");
                        }
                    }
                } else if (requestCode == DataPrefsActivity.FILE_PICKER_REQUEST_CODE) {
                    // The result data contains a URI for the document or directory that
                    // the user selected.
                    if (data != null) {
                        fileUri = data.getData();

                        Preference currentPreference = findPreference(GlobalPrefs.PATH_JAPAN_IPL_ROM);
                        if (currentPreference != null && fileUri != null) {

                            requireActivity().getContentResolver().takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            DocumentFile file = FileUtil.getDocumentFileSingle(getActivity(), fileUri);
                            String summary = file == null ? "" : file.getName();
                            currentPreference.setSummary(summary);
                            mGameActivity.getGlobalPrefs().putString(GlobalPrefs.PATH_JAPAN_IPL_ROM, fileUri.toString());
                        }
                    }
                }else if (requestCode == DataPrefsActivity.LEGACY_FILE_PICKER_REQUEST_CODE) {
                    final Bundle extras = data.getExtras();

                    if (extras != null) {
                        final String searchUri = extras.getString(ActivityHelper.Keys.SEARCH_PATH);
                        fileUri = Uri.parse(searchUri);

                        Preference currentPreference = findPreference(GlobalPrefs.PATH_JAPAN_IPL_ROM);
                        if (currentPreference != null && fileUri != null && fileUri.getPath() != null) {
                            File file = new File(fileUri.getPath());
                            currentPreference.setSummary(file.getName());
                            mGameActivity.getGlobalPrefs().putString(GlobalPrefs.PATH_JAPAN_IPL_ROM, fileUri.toString());
                        }
                    }
                }
            }
        }

        public void addPreferencesFromResource(String sharedPrefsName, int preferencesResId)
        {
            mSharedPrefsName = sharedPrefsName;
            mPreferencesResId = preferencesResId;
        }

        private void setPreference(String preferenceString, boolean value){
            Preference preference;
            preference = findPreference(preferenceString);
            if (preference != null)
                preference.setEnabled(value);
        }

        private void disableSettingsThatReset(int currentResource){
            boolean setBool = true;
            if(mGameActivity.getGlobalPrefs().maxAutoSaves == 0)
                setBool = false;

            switch(currentResource){
                // Display
                case 0:
                    setPreference("displayResolution",setBool);
                    setPreference("displayOrientation",setBool);
                    setPreference("displayScaling",setBool);
                    setPreference("videoHardwareType",setBool);
                    setPreference("hybridTextureFilter_v2",setBool);
                    setPreference("displayImmersiveMode_v2",setBool);
                    break;
                // Audio
                case 2:
                    setPreference("audioVolume",setBool);
                    setPreference("audioBufferSize",setBool);
                    setPreference("audioTimeStretch",setBool);
                    setPreference("audioFloatingPoint",setBool);
                    setPreference("audioSynchronize",setBool);
                    setPreference("audioSwapChannels",setBool);
                    setPreference("lowPerformanceMode",setBool);
                    setPreference("useHighPriorityThread_v2",setBool);
                    break;
                // Input
                case 4:
                    setPreference("navigationMode",setBool);
                    setPreference("useRaphnetAdapter",setBool);
                    break;
                // Data
                case 5:
                    setPreference("gameDataStorageType",setBool);
                    setPreference("useFlatGameDataPath",setBool);
                    setPreference("japanIdlPath64dd",setBool);
                    break;
                default:
                    break;
            }
        }

        public void resetPreferences(){
            switch(mCurrentResourceId){
                case 0:
                    if(mGameActivity.getGlobalPrefs() == null)
                        break;
                    if(mGameActivity.getGlobalPrefs().maxAutoSaves == 0)
                        setPreference("videoPolygonOffset",false);
                    else
                        setPreference(VIDEO_POLYGON_OFFSET,mGameActivity.getGlobalPrefs().videoHardwareType == VIDEO_HARDWARE_TYPE_CUSTOM);
                    disableSettingsThatReset(mCurrentResourceId);
                    break;
                case 1:
                    if(gameSettingsDialog != null)
                        gameSettingsDialog.OnPreferenceScreenChange("");
                    break;
                case 2:
                    if(mGameActivity.getGlobalPrefs() == null)
                        break;
                    if(mGameActivity.getGlobalPrefs().maxAutoSaves == 0)
                        setPreference("audioSamplingType",false);
                    else
                        setPreference(AUDIO_SAMPLING_TYPE,!mGameActivity.getGlobalPrefs().enableAudioTimeSretching);
                    disableSettingsThatReset(mCurrentResourceId);
                    break;
                case 3:
                    if(mGameActivity.getGlobalPrefs() == null)
                        break;
                    setPreference(GlobalPrefs.KEY_TOUCHSCREEN_SKIN_CUSTOM_PATH,
                            !TextUtils.isEmpty(mGameActivity.getGlobalPrefs().touchscreenSkin) &&
                                    mGameActivity.getGlobalPrefs().touchscreenSkin.equals("Custom"));
                    break;
                case 4:
                    if(mGameActivity.getGlobalPrefs() == null)
                        break;
                    setPreference(GlobalPrefs.PLAYER_MAP,!mGameActivity.getGlobalPrefs().autoPlayerMapping && !mGameActivity.getGlobalPrefs().isControllerShared);
                    setPreference("inputVolumeMappable",false);
                    setPreference("inputBackMappable",false);
                    setPreference("inputMenuMappable",false);

                    final PlayerMapPreference playerPref = (PlayerMapPreference) findPreference(GlobalPrefs.PLAYER_MAP);
                    if (playerPref != null)
                    {
                        // Check null in case preference has been removed
                        final boolean enable1 = mGameActivity.getGlobalPrefs().controllerProfile1 != null;
                        final boolean enable2 = mGameActivity.getGlobalPrefs().controllerProfile2 != null;
                        final boolean enable3 = mGameActivity.getGlobalPrefs().controllerProfile3 != null;
                        final boolean enable4 = mGameActivity.getGlobalPrefs().controllerProfile4 != null;
                        playerPref.setControllersEnabled(enable1, enable2, enable3, enable4);

                        playerPref.setValue( mGameActivity.getGamePrefs().playerMap.serialize() );
                    }
                    disableSettingsThatReset(mCurrentResourceId);
                    break;
                case 5:
                    if(mGameActivity.getGlobalPrefs() == null)
                        break;
                    if(mGameActivity.getGlobalPrefs().maxAutoSaves == 0)
                        setPreference("gameDataStoragePath",false);
                    else
                        setPreference("gameDataStoragePath",
                                mPrefs.getString(GlobalPrefs.GAME_DATA_STORAGE_TYPE, "internal").equals("external"));
                    disableSettingsThatReset(mCurrentResourceId);
                    break;
                default:
                    break;
            }

            if(mGameActivity.getResolutionReset())
                resolutionResetPreferences(false);
        }

        public void resolutionResetPreferences(boolean enabled){
            for(int i = 0; i <= getPreferenceScreen().getPreferenceCount()-1; i++)
                getPreferenceManager().getPreferenceScreen().getPreference(i).setEnabled(enabled);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

            setRetainInstance(true);

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
         * Called while preparing the dialog builder
         * @param context Contextz
         * @param builder dialog builder
         */
        void onPrepareDialogBuilder(Context context, AlertDialog.Builder builder);

        /**
         * Called when the dialog view is binded
         */
        void onBindDialogView(View view, FragmentActivity associatedActivity);

        /**
         * Called when the dialog is closed
         */
        void onDialogClosed(boolean result);
    }

    //Generic preference dialog to be used for all preference dialog fragments
    public static class PreferenceDialog extends PreferenceDialogFragmentCompat
    {
        public static PreferenceDialog newInstance(Preference preference)
        {
            PreferenceDialog frag = new PreferenceDialog();
            Bundle args = new Bundle();
            // This has to be the string "key"
            args.putString("key", preference.getKey());
            preference.getSummary();

            frag.setArguments(args);
            return frag;
        }

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            setRetainInstance(true);
        }

        @Override
        public void onDestroyView()
        {
            // This is needed because of this:
            // https://code.google.com/p/android/issues/detail?id=17423

            if (getDialog() != null && getRetainInstance())
                getDialog().setDismissMessage(null);
            super.onDestroyView();
        }

        @Override
        public void onPrepareDialogBuilder(AlertDialog.Builder builder)
        {
            super.onPrepareDialogBuilder(builder);

            if (getPreference() instanceof OnPreferenceDialogListener)
            {
                ((OnPreferenceDialogListener) getPreference()).onPrepareDialogBuilder(getActivity(), builder);
            }
            else
            {
                Log.e("GameSettingsDialog", "DialogPreference must implement OnPreferenceDialogListener");
            }
        }

        @Override
        public void onBindDialogView(View view)
        {
            super.onBindDialogView(view);

            if (getPreference() instanceof OnPreferenceDialogListener)
            {
                ((OnPreferenceDialogListener) getPreference()).onBindDialogView(view, getActivity());
            }
            else
            {
                Log.e("GameSettingsDialog", "DialogPreference must implement OnPreferenceDialogListener");
            }
        }

        @Override
        public void onDialogClosed(boolean result)
        {
            if (getPreference() instanceof OnPreferenceDialogListener)
            {

                ((OnPreferenceDialogListener) getPreference()).onDialogClosed(result);
            }
            else
            {
                Log.e("GameSettingsDialog", "DialogPreference must implement OnPreferenceDialogListener");
            }
        }
    }

    @Override
    public DialogFragment getPreferenceDialogFragment(Preference preference)
    {
        DialogFragment returnFragment = null;

        if (preference instanceof OnPreferenceDialogListener)
        {
            returnFragment = PreferenceDialog.newInstance(preference);
        }
        return returnFragment;
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat preferenceFragmentCompat,
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
        if(mDeleteExtraDialog == 1)
            mDeleteExtraDialog++;
        else if(mDeleteExtraDialog >= 2)
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
            try {
                int associatedFragment = 0;
                final FragmentManager fm = mGameActivity.getSupportFragmentManager();
                PromptInputCodeDialog promptInputCodeDialog = (PromptInputCodeDialog) fm.findFragmentByTag("STATE_PROMPT_INPUT_CODE_DIALOG");
                if (promptInputCodeDialog != null)
                    associatedFragment = mSelectedPlayer;
                mGameActivity.setAssociatedDialogFragment(associatedFragment);
            } catch (Exception e) {
                e.printStackTrace();
            }
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
