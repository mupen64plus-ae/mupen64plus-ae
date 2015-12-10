package paulscode.android.mupen64plusae.compat;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.appcompat.R;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class AppCompatPreferenceFragment extends PreferenceFragmentCompat
{
    public interface OnDisplayDialogListener
    {
        /**
         * Called when a preference dialog is being displayed. This must return
         * the appropriate DialogFragment for the preference.
         * 
         * @param preference
         *            The preference dialog
         * @return The dialog fragment for the preference
         */
        public DialogFragment getPreferenceDialogFragment(Preference preference);
    }
    
    public interface OnFragmentCreationListener
    {
        /**
         * Called when a preference dialog is being displayed. This must return
         * the appropriate DialogFragment for the preference.
         * 
         * @param preference
         *            The preference dialog
         * @return The dialog fragment for the preference
         */
        public void onFragmentCreation(AppCompatPreferenceFragment currentFragment);
    }

    private static final String STATE_SHATED_PREFS_NAME = "STATE_SHATED_PREFS_NAME";
    private static final String STATE_RESOURCE_ID = "STATE_RESOURCE_ID";

    public static AppCompatPreferenceFragment newInstance(String sharedPrefsName, int resourceId, String rootKey)
    {
        AppCompatPreferenceFragment frag = new AppCompatPreferenceFragment();
        Bundle args = new Bundle();
        args.putString(STATE_SHATED_PREFS_NAME, sharedPrefsName);
        args.putInt(STATE_RESOURCE_ID, resourceId);
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, rootKey);

        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        setRetainInstance(true);
        
        final String sharedPrefsName = getArguments().getString(STATE_SHATED_PREFS_NAME);
        final int resourceId = getArguments().getInt(STATE_RESOURCE_ID);

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        if (getActivity() instanceof OnFragmentCreationListener)
        {
            ((OnFragmentCreationListener) getActivity()).onFragmentCreation(this);
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference)
    {
        DialogFragment fragment = null;

        if (getActivity() instanceof OnDisplayDialogListener)
        {
            fragment = ((OnDisplayDialogListener) getActivity()).getPreferenceDialogFragment(preference);

            if (fragment != null)
            {
                fragment.setTargetFragment(this, 0);
                fragment.show(getFragmentManager(), "android.support.v7.preference.PreferenceFragment.DIALOG");
            }
        }

        if (fragment == null)
        {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        // Set the default white background in the view so as to avoid
        // transparency
        view.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.background_material_dark));

    }
}