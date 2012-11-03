package paulscode.android.mupen64plusae.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.widget.EditText;

public class Prompt
{
    public interface OnFileListener
    {
        public void onFile( File file );
    }
    
    public interface OnTextListener
    {
        public void onText( CharSequence text );
    }
    
    public static void promptConfirm( Context context, CharSequence title, CharSequence message,
            OnClickListener listener )
    {
        // Create and launch a simple confirmation dialog
        prefillBuilder( context, title, message, listener ).create().show();
    }
    
    public static void promptFile( Context context, CharSequence title, CharSequence message,
            File startPath, final OnFileListener listener )
    {
        // Don't even open the dialog if the path doesn't exist
        if( !startPath.exists() )
            return;
        
        // Get the filenames and absolute paths
        final List<CharSequence> filenames = new ArrayList<CharSequence>();
        final List<String> absolutePaths = new ArrayList<String>();
        FileUtil.populate( startPath, false, false, true, filenames, absolutePaths );
        
        // When the user clicks a file, notify the downstream listener
        OnClickListener internalListener = new OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                if( which >= 0 && which < filenames.size() )
                    listener.onFile( new File( absolutePaths.get( which ) ) );
            }
        };
        
        // Create and launch the dialog, removing Ok button and populating list in the process
        prefillBuilder( context, title, message, internalListener )
                .setPositiveButton( null, null )
                .setItems( filenames.toArray( new CharSequence[filenames.size()] ),
                        internalListener ).create().show();
    }
    
    public static void promptText( Context context, CharSequence title, CharSequence message,
            CharSequence hint, final OnTextListener listener )
    {
        // Create an edit-text widget, and add the hint text
        final EditText editText = new EditText( context );
        editText.setHint( hint );
        
        // When the user clicks Ok, notify the downstream listener
        OnClickListener internalListener = new OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                    listener.onText( editText.getText().toString() );
            }
        };
        
        // Create and launch the dialog, adding the edit-text widget in the process
        prefillBuilder( context, title, message, internalListener ).setView( editText ).create()
                .show();
    }
    
    private static Builder prefillBuilder( Context context, CharSequence title,
            CharSequence message, OnClickListener listener )
    {
        // Convenience method for consistently initializing dialogs across the various methods
        return new Builder( context ).setTitle( title ).setMessage( message )
                .setNegativeButton( context.getString( android.R.string.cancel ), listener )
                .setPositiveButton( context.getString( android.R.string.ok ), listener );
    }
}
