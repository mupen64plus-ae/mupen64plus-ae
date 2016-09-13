package paulscode.android.mupen64plusae.dialog;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import paulscode.android.mupen64plusae.MenuListView;

public class DynamicMenuDialogFragment extends DialogFragment
{
    private static final String STATE_DIALOG_ID = "STATE_DIALOG_ID";
    private static final String STATE_TITLE = "STATE_TITLE";
    private static final String STATE_NUM_OPTION_ITEMS = "STATE_NUM_ITEMS";
    private static final String STATE_OPTION_ITEMS_IDS = "STATE_OPTION_ITEMS_IDS";
    private static final String STATE_OPTION_ITEMS_TITLES = "STATE_OPTION_ITEMS_TITLES";
    public interface OnDynamicDialogMenuItemSelectedListener
    {
        /*
         * Called when creating the menu
         */
        public void onPrepareMenuList(MenuListView listView);

        /**
         * Called when a dialog menu item is selected
         * 
         * @param dialogId
         *            The parameter ID.
         * @param selectItem
         *            Selected menu item.
         */
        public void onDialogMenuItemSelected(int dialogId, String selectItem);
    }

    private List<String> mOptionItems = null;
    private List<String> mOptionTitles = null;

    public static DynamicMenuDialogFragment newInstance(int dialogId, String title, List<String> options, List<String> optionTitles)
    {
        DynamicMenuDialogFragment frag = new DynamicMenuDialogFragment();
        Bundle args = new Bundle();
        args.putInt(STATE_DIALOG_ID, dialogId);
        args.putString(STATE_TITLE, title);


        if(options != null)
        {
            args.putInt(STATE_NUM_OPTION_ITEMS, options.size());
            for (int index = 0; index < options.size(); ++index)
            {
                String option = options.get(index);
                String optionTitle = optionTitles.get(index);
                args.putString(STATE_OPTION_ITEMS_IDS + index, option);
                args.putString(STATE_OPTION_ITEMS_TITLES + index, optionTitle);
            }
        }
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        setRetainInstance(true);

        final int dialogId = getArguments().getInt(STATE_DIALOG_ID);
        String title = getArguments().getString(STATE_TITLE);


        final int numOptionItems = getArguments().getInt(STATE_NUM_OPTION_ITEMS);
        mOptionItems = new ArrayList<String>();
        mOptionTitles = new ArrayList<String>();

        //Fill the values
        for (int index = 0; index < numOptionItems; ++index)
        {
            String optionId = getArguments().getString(STATE_OPTION_ITEMS_IDS + index);
            String optionTitle = getArguments().getString(STATE_OPTION_ITEMS_TITLES + index);

            mOptionItems.add(optionId);
            mOptionTitles.add(optionTitle);
        }

        MenuListView menuList = new MenuListView(getContext(), null);

        if (getActivity() instanceof OnDynamicDialogMenuItemSelectedListener)
        {
            menuList.setMenuList(mOptionTitles);

            ((OnDynamicDialogMenuItemSelectedListener) getActivity()).onPrepareMenuList(menuList);
            // Handle menu item selections
            menuList.setOnClickListener(new MenuListView.OnClickListener()
            {
                @Override
                public void onClick(MenuItem menuItem)
                {
                    ((OnDynamicDialogMenuItemSelectedListener) getActivity()).onDialogMenuItemSelected(dialogId, mOptionItems.get(menuItem.getItemId()));

                    dismiss();
                }
            });

        }
        else
        {
            Log.e("MenuDialogFragment", "Activity doesn't implement OnDialogMenuItemSelected");
        }

        Builder builder = new Builder(getActivity());
        builder.setTitle(title);
        builder.setView(menuList);

        return builder.create();
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
}