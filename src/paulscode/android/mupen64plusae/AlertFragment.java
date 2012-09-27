// TODO: Is there a way this class can be made to handle the Android
// resources (ints) AND Strings without requiring
// getString to be called outside of the class.
//
// It would be much more dev-friendly if this was the case.

package paulscode.android.mupen64plusae;

import android.app.AlertDialog;
import android.app.Dialog;

import android.app.DialogFragment;  // <-- Requires Honeycomb or higher

/**
 *  import android.support.v4.app.*;
 *  
 *  This would be the solution, except it requires everything to extend FragmentActivity,
 *  in order to be able to call getSupportFragmentManager().  But we are using
 *  PreferenceActivity everywhere, so something will have to be changed.
 */

import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Class that constructs DialogFragments. </p>
 * Used to kill deprecation warnings
 * (not to mention implement newer API).
 */
public class AlertFragment extends DialogFragment
{
    /**
     * Constructs an AlertFragment to display.
     * 
     * @param title   Resource ID of the desired title of the dialog
     * @param message Resource ID of the desired message to display in the dialog
     * @return An AlertFragment
     */
    public static AlertFragment newInstance(String title, String message)
    {
        AlertFragment alertFrag = new AlertFragment();
        
        Bundle args = new Bundle();
        args.putString("title", title);      // Set the title   
        args.putString("message", message);  // Set the display message
        alertFrag.setArguments(args);
        
        return alertFrag;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        String title = getArguments().getString("title");
        String message = getArguments().getString("message");
        
        return new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.icon)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK",
                
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Handle on click for OK
                        }
                    }
                )
                .create();
    }
}
