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
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.compat;

import paulscode.android.mupen64plusae.compat.AppCompatPreferenceFragment.OnDisplayDialogListener;
import paulscode.android.mupen64plusae.compat.AppCompatPreferenceFragment.OnFragmentCreationListener;
import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartScreenCallback;
import androidx.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;

public class AppCompatPreferenceActivity extends AppCompatActivity implements OnDisplayDialogListener, OnPreferenceStartScreenCallback, OnFragmentCreationListener
{
    public interface OnPreferenceDialogListener
    {
        /**
         * Called while preparing the dialog builder
         * @param context
         * @param builder
         */
        public void onPrepareDialogBuilder(Context context, Builder builder);

        /**
         * Called when the dialog view is binded
         */
        public void onBindDialogView(View view, FragmentActivity associatedActivity);

        /**
         * Called when the dialog is closed
         */
        public void onDialogClosed(boolean result);
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
        public void onPrepareDialogBuilder(Builder builder)
        {
            super.onPrepareDialogBuilder(builder);

            if (getPreference() instanceof OnPreferenceDialogListener)
            {
                ((OnPreferenceDialogListener) getPreference()).onPrepareDialogBuilder(getActivity(), builder);
            }
            else
            {
                Log.e("PreferenceDialog", "DialogPreference must implement OnPreferenceDialogListener");
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
                Log.e("PreferenceDialog", "DialogPreference must implement OnPreferenceDialogListener");
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
                Log.e("PreferenceDialog", "DialogPreference must implement OnPreferenceDialogListener");
            }
        }
    }

    public static final String STATE_PREFERENCE_FRAGMENT = "STATE_PREFERENCE_FRAGMENT";
    
    private String mSharedPrefsName = null;
    private int mPreferencesResId;

    // Preference fragment
    private AppCompatPreferenceFragment mPrefFrag = null;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null)
        {
            final FragmentManager fm = getSupportFragmentManager();
            mPrefFrag = (AppCompatPreferenceFragment) fm.findFragmentById(android.R.id.content);   
        }
    }

    public void addPreferencesFromResource(String sharedPrefsName, int preferencesResId)
    {
        mSharedPrefsName = sharedPrefsName;
        mPreferencesResId = preferencesResId;
        
        if(mPrefFrag == null)
        {
            mPrefFrag = AppCompatPreferenceFragment.newInstance(sharedPrefsName, preferencesResId, null);
            getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, mPrefFrag, STATE_PREFERENCE_FRAGMENT).commit();
        }
    }
    
    public void resetPreferences()
    {        
        getSupportFragmentManager().beginTransaction().remove(mPrefFrag);
        mPrefFrag = AppCompatPreferenceFragment.newInstance(mSharedPrefsName, mPreferencesResId, null);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, mPrefFrag, STATE_PREFERENCE_FRAGMENT).commit();
    }


    public Preference findPreference(CharSequence key)
    {
        return mPrefFrag.findPreference(key);
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
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        AppCompatPreferenceFragment fragment = AppCompatPreferenceFragment.newInstance(mSharedPrefsName,
            mPreferencesResId, preferenceScreen.getKey());
        ft.replace(android.R.id.content, fragment, preferenceScreen.getKey());
        ft.addToBackStack(null);
        ft.commit();
        
        return true;
    }
    
    protected void OnPreferenceScreenChange(String key)
    {
        //Nothing to do by default
    }

    @Override
    public void onFragmentCreation(AppCompatPreferenceFragment currentFragment)
    {
        //TODO: DIdn't fix it
        if(mPrefFrag != null)
        {
            View fragView = mPrefFrag.getView();
            
            if(fragView != null)
            {
                fragView.setVisibility(View.GONE);
            }
        }

        mPrefFrag = currentFragment;
        
        View fragView = mPrefFrag.getView();
        
        if(fragView != null)
        {
            fragView.setVisibility(View.VISIBLE);
        }
        
        OnPreferenceScreenChange(mPrefFrag.getTag());
    }
    
    protected Context getPreferenceManagerContext()
    {
        return mPrefFrag.getPreferenceManager().getContext();
    }
    
    protected AppCompatPreferenceFragment getPreferenceFragment()
    {
        return mPrefFrag;
    }
    
    
}