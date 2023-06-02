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
 * Authors: fzurita
 */
package paulscode.android.mupen64plusae-mpn.persistent;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import org.mupen64plusae-mpn.v3.alpha.R;

import paulscode.android.mupen64plusae-mpn.compat.AppCompatPreferenceActivity;
import paulscode.android.mupen64plusae-mpn.preference.PrefUtil;
import paulscode.android.mupen64plusae-mpn.util.LocaleContextWrapper;

public class NetplayPrefsActivity extends AppCompatPreferenceActivity implements
    SharedPreferences.OnSharedPreferenceChangeListener
{
    private GlobalPrefs mGlobalPrefs = null;

    private SharedPreferences mPrefs = null;

    private static class PortFilterText implements InputFilter {

        private final int mMax;

        PortFilterText () {
            this.mMax = 65535;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            try {
                int input = Integer.parseInt(dest.toString() + source.toString());
                if (isInRange(mMax, input))
                    return null;
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }
            return "";
        }

        private boolean isInRange(int max, int input) {
            return input <= max;
        }
    }

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
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Get app data and user preferences
        // App data and user preferences
        AppData appData = new AppData(this);
        mGlobalPrefs = new GlobalPrefs(this, appData);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    protected String getSharedPrefsName() {
        return null;
    }

    @Override
    protected int getSharedPrefsId()
    {
        return R.xml.preferences_netplay;
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        refreshViews();
        mPrefs.registerOnSharedPreferenceChangeListener( this );
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener( this );
    }

    @Override
    protected void OnPreferenceScreenChange(String key)
    {
        refreshViews();
    }

    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
    {
        refreshViews();
    }

    private void refreshViews()
    {
        AppData appData = new AppData(this);
        mGlobalPrefs = new GlobalPrefs(this, appData);

        Preference netplayRoomTcpPortPref = findPreference(GlobalPrefs.ROOM_TCP_PORT);
        if (netplayRoomTcpPortPref != null) {
            int netplayRoomTcpPort = mGlobalPrefs.netplayRoomTcpPort;
            netplayRoomTcpPortPref.setSummary(Integer.toString(netplayRoomTcpPort));

            EditTextPreference pref = (EditTextPreference) netplayRoomTcpPortPref;
            pref.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(5),
                        new PortFilterText()});
            });
        }

        Preference netplayServerUdpTcpPortPref = findPreference(GlobalPrefs.SERVER_UDP_TCP_PORT);
        if (netplayServerUdpTcpPortPref != null) {
            int netplayServerUdpTcpPort = mGlobalPrefs.netplayServerUdpTcpPort;
            netplayServerUdpTcpPortPref.setSummary(Integer.toString(netplayServerUdpTcpPort));

            EditTextPreference pref = (EditTextPreference) netplayServerUdpTcpPortPref;
            pref.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(5),
                        new PortFilterText()});
            });
        }

        PrefUtil.enablePreference(this, GlobalPrefs.ROOM_TCP_PORT, !mGlobalPrefs.useUpnpToMapNetplayPorts);
        PrefUtil.enablePreference(this, GlobalPrefs.SERVER_UDP_TCP_PORT, !mGlobalPrefs.useUpnpToMapNetplayPorts);
    }
}
