package paulscode.android.mupen64plusae.util;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.widget.EditText;

public class Prompt
{
    public interface OnTextListener
    {
        public void onText( CharSequence text );
    }
    
    public static void promptText( Context context, CharSequence title, CharSequence message,
            CharSequence hint, final OnTextListener listener )
    {
        final EditText editText = new EditText( context );
        editText.setHint( hint );
        
        OnClickListener internalListener = new OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                    listener.onText( editText.getText().toString() );
            }
        };
        
        prefillBuilder( context, title, message, internalListener ).setView( editText ).create()
                .show();
    }
    
    public static void promptConfirm( Context context, CharSequence title, CharSequence message,
            OnClickListener listener )
    {
        prefillBuilder( context, title, message, listener ).create().show();
    }
    
    private static Builder prefillBuilder( Context context, CharSequence title,
            CharSequence message, OnClickListener listener )
    {
        return new Builder( context ).setTitle( title ).setMessage( message )
                .setNegativeButton( context.getString( android.R.string.cancel ), listener )
                .setPositiveButton( context.getString( android.R.string.ok ), listener );
    }
}
