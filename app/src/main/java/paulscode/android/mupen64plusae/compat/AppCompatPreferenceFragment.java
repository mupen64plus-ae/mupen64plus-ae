package paulscode.android.mupen64plusae.compat;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import androidx.appcompat.R;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
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
        DialogFragment getPreferenceDialogFragment(Preference preference);
    }
    
    public interface OnFragmentCreationListener
    {
        /**
         * Called when a preference dialog is being displayed. This must return
         * the appropriate DialogFragment for the preference.
         * 
         * @param currentFragment Current fragment
         */
        void onFragmentCreation(AppCompatPreferenceFragment currentFragment);
    }

    private static final String STATE_SHATED_PREFS_NAME = "STATE_SHATED_PREFS_NAME";
    private static final String STATE_RESOURCE_ID = "STATE_RESOURCE_ID";
    
    private boolean mHasFocusBeenSet = false;

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

        Bundle arguments = getArguments();

        if (arguments == null) {
            return;
        }
        
        final String sharedPrefsName = arguments.getString(STATE_SHATED_PREFS_NAME);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        if (getActivity() instanceof OnFragmentCreationListener)
        {
            ((OnFragmentCreationListener) getActivity()).onFragmentCreation(this);
        }
        
        mHasFocusBeenSet = false;
        
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

                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager != null) {
                    fragment.show(getFragmentManager(), "androidx.preference.PreferenceFragment.DIALOG");
                }
            }
        }

        if (fragment == null)
        {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        // Set the default white background in the view so as to avoid
        // transparency
        view.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.background_material_dark));
    }
    
    @Override
    protected void onBindPreferences()
    {
        // Detect when a view is added to the preference fragment and request focus if it's the first view
        final RecyclerView recyclerView = getListView();

        final RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();

        recyclerView.addOnChildAttachStateChangeListener(new OnChildAttachStateChangeListener()
        {
            @Override
            public void onChildViewAttachedToWindow(View childView)
            {
                final LinearLayoutManager layoutManager = (LinearLayoutManager)recyclerView.getLayoutManager();

                //Prevent scrolling past the top
                childView.setOnKeyListener(new OnKeyListener() {

                    @Override
                    public boolean onKey(View v, int keyCode, KeyEvent event)
                    {
                        View focusedChild = layoutManager.getFocusedChild();
                        int selectedPos = recyclerView.getChildAdapterPosition(focusedChild);
                        
                        if (event.getAction() == KeyEvent.ACTION_DOWN) {
                            switch(keyCode) {
                                case KeyEvent.KEYCODE_DPAD_DOWN:
                                    return !(selectedPos < adapter.getItemCount() - 1);
                                case KeyEvent.KEYCODE_DPAD_UP:
                                    return selectedPos == 0;
                                case KeyEvent.KEYCODE_DPAD_RIGHT:
                                    return true;
                            }
                            return false;
                        }
                        return false;
                    }
                });
                
                //Make sure all views are focusable
                childView.setFocusable(true);

                if(adapter.getItemCount() > 0)
                {
                    int firstItem = layoutManager.findFirstCompletelyVisibleItemPosition();

                    //Get focus on the first visible item the first time it's displayed
                    if (firstItem != -1) {
                        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForItemId(adapter.getItemId(firstItem));
                        if(holder != null)
                        {
                            if (!mHasFocusBeenSet)
                            {
                                mHasFocusBeenSet = true;
                                holder.itemView.requestFocus();
                            }
                        }
                    }
                }
            }

            @Override
            public void onChildViewDetachedFromWindow(View arg0)
            {
                // Nothing to do here
            }
        });
    }
}