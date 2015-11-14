package paulscode.android.mupen64plusae.dialog;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class MenuDialogFragment extends DialogFragment
{
    private static final String STATE_TITLE = "title";
    private static final String STATE_NUM_ITEMS = "num_items";
    private static final String STATE_ITEMS = "items";

    public static MenuDialogFragment newInstance(String title, List<CharSequence> items)
    {
        MenuDialogFragment frag = new MenuDialogFragment();
        Bundle args = new Bundle();
        args.putString(STATE_TITLE, title);
        args.putInt(STATE_NUM_ITEMS, items.size());
        
        for(int index = 0; index < items.size(); ++index)
        {
            CharSequence seq = items.get(index);
            args.putCharSequence(STATE_ITEMS + index, seq);
        }

        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        setRetainInstance(true);
        
        String title = getArguments().getString(STATE_TITLE);
        int numItems = getArguments().getInt(STATE_NUM_ITEMS);
        
        ArrayList<CharSequence> items = new ArrayList<CharSequence>();
        
        for(int index = 0; index < numItems; ++index)
        {
            CharSequence seq = getArguments().getCharSequence(STATE_ITEMS + index);
            items.add(seq);
        }
        
        Builder builder = new Builder( getActivity() );
        builder.setTitle( title );
        
        CharSequence[] itemsArray = new CharSequence[items.size()];
        itemsArray = items.toArray(itemsArray);
        
        if(getActivity() instanceof OnClickListener)
        {
            builder.setItems( itemsArray, (OnClickListener) getActivity() );
        }
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