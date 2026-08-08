package paulscode.android.mupen64plusae.persistent;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.compat.AppCompatPreferenceActivity;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;

public class RetroAchievementsPrefsActivity extends AppCompatPreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "RAPrefs";
    private static final String RA_LOGIN_URL =
            "https://retroachievements.org/dorequest.php?r=login2";

    private SharedPreferences mPrefs;
    private Preference mStatusPref;
    private Preference mUsernamePref;
    private Preference mPasswordPref;
    private Preference mLoginPref;
    private Preference mLogoutPref;

    @Override
    protected void attachBaseContext(Context newBase) {
        if (TextUtils.isEmpty(LocaleContextWrapper.getLocalCode()))
            super.attachBaseContext(newBase);
        else
            super.attachBaseContext(LocaleContextWrapper.wrap(newBase,
                    LocaleContextWrapper.getLocalCode()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    protected String getSharedPrefsName() { return null; }

    @Override
    protected int getSharedPrefsId() { return R.xml.preferences_retroachievements; }

    @Override
    protected void OnPreferenceScreenChange(String key) {
        mStatusPref   = findPreference("retroAchievementsStatus");
        mUsernamePref = findPreference("retroAchievementsUsername");
        mLoginPref    = findPreference("retroAchievementsLogin");
        mLogoutPref   = findPreference("retroAchievementsLogout");

        if (mLoginPref != null) {
            mLoginPref.setOnPreferenceClickListener(p -> { doLogin(); return true; });
        }
        if (mLogoutPref != null) {
            mLogoutPref.setOnPreferenceClickListener(p -> { doLogout(); return true; });
        }

        // Configure password field: mask input, show set/not-set summary
        mPasswordPref = findPreference("retroAchievementsPassword");
        EditTextPreference pwPref = (EditTextPreference) mPasswordPref;
        if (pwPref != null) {
            pwPref.setOnBindEditTextListener(editText ->
                    editText.setInputType(InputType.TYPE_CLASS_TEXT
                            | InputType.TYPE_TEXT_VARIATION_PASSWORD));
            pwPref.setSummaryProvider(pref -> {
                String v = ((EditTextPreference) pref).getText();
                return (v != null && !v.isEmpty())
                        ? getString(R.string.retroAchievementsPassword_set)
                        : getString(R.string.retroAchievementsPassword_notset);
            });
        }

        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        refreshStatus();
    }

    private void refreshStatus() {
        if (mStatusPref == null) return;
        String token = mPrefs.getString("retroAchievementsToken", "");
        String user  = mPrefs.getString("retroAchievementsUsername", "");
        boolean loggedIn = !token.isEmpty() && !user.isEmpty();

        if (loggedIn) {
            mStatusPref.setSummary(getString(R.string.retroAchievementsStatus_loggedIn, user));
        } else {
            mStatusPref.setSummary(getString(R.string.retroAchievementsStatus_notLoggedIn));
        }

        // Show login fields only when logged out; show logout only when logged in
        if (mUsernamePref != null) mUsernamePref.setVisible(!loggedIn);
        if (mPasswordPref != null) mPasswordPref.setVisible(!loggedIn);
        if (mLoginPref  != null)   mLoginPref.setVisible(!loggedIn);
        if (mLogoutPref != null)   mLogoutPref.setVisible(loggedIn);
    }

    private void doLogin() {
        String username = mPrefs.getString("retroAchievementsUsername", "").trim();
        String password = mPrefs.getString("retroAchievementsPassword", "").trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.retroAchievementsLogin_missingFields,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.retroAchievementsLogin_loggingIn,
                Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            String token = null;
            String error = null;
            try {
                String postBody = "u=" + URLEncoder.encode(username, "UTF-8")
                        + "&p=" + URLEncoder.encode(password, "UTF-8");
                HttpURLConnection conn =
                        (HttpURLConnection) new URL(RA_LOGIN_URL).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);
                conn.setRequestProperty("Content-Type",
                        "application/x-www-form-urlencoded");
                conn.setRequestProperty("User-Agent", "mupen64plus-ae/RetroAchievements");
                byte[] bodyBytes = postBody.getBytes(StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(bodyBytes.length);
                conn.getOutputStream().write(bodyBytes);

                int status = conn.getResponseCode();
                InputStream is = status < 400
                        ? conn.getInputStream() : conn.getErrorStream();
                String body = new String(readAll(is), StandardCharsets.UTF_8);
                Log.d(TAG, "Login response: " + body);

                JSONObject json = new JSONObject(body);
                if (json.optBoolean("Success")) {
                    token = json.getString("Token");
                } else {
                    error = json.optString("Error", getString(R.string.retroAchievementsLogin_failed));
                }
            } catch (Exception e) {
                Log.e(TAG, "Login exception", e);
                error = e.getMessage();
            }

            final String finalToken = token;
            final String finalError = error;
            runOnUiThread(() -> {
                if (finalToken != null) {
                    mPrefs.edit()
                            .putString("retroAchievementsToken", finalToken)
                            .putString("retroAchievementsPassword", "") // clear password
                            .apply();
                    Toast.makeText(this,
                            getString(R.string.retroAchievementsLogin_success, username),
                            Toast.LENGTH_LONG).show();
                    refreshStatus();
                } else {
                    Toast.makeText(this,
                            getString(R.string.retroAchievementsLogin_error, finalError),
                            Toast.LENGTH_LONG).show();
                }
            });
        }, "RA-Login").start();
    }

    private void doLogout() {
        mPrefs.edit()
                .putString("retroAchievementsToken", "")
                .putString("retroAchievementsUsername", "")
                .putString("retroAchievementsPassword", "")
                .apply();
        Toast.makeText(this, R.string.retroAchievementsLogout_done,
                Toast.LENGTH_SHORT).show();
        refreshStatus();
    }

    private static byte[] readAll(InputStream is) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) out.write(buf, 0, n);
        return out.toByteArray();
    }
}
