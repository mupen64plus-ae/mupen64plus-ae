/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2012 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with Mupen64PlusAE. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.widget.EditText;

/**
 * A utility class that generates dialogs to prompt the user for information.
 */
public class Prompt
{
    /**
     * The listener interface for receiving a file selected by the user.
     * 
     * @see promptFile
     */
    public interface OnFileListener
    {
        /**
         * Process the file selected by the user.
         * 
         * @param file the file selected by the user.
         */
        public void onFile( File file );
    }
    
    /**
     * The listener interface for receiving text provided by the user.
     * 
     * @see promptText
     */
    public interface OnTextListener
    {
        /**
         * Process the text provided by the user.
         * 
         * @param text the text provided by the user
         */
        public void onText( CharSequence text );
    }
    
    /**
     * Open a dialog to prompt the user for a confirmation (Ok/Cancel).
     * 
     * @param context the context
     * @param title the title of the dialog
     * @param message the message to be shown inside the dialog
     * @param listener the listener to process the confirmation
     */
    public static void promptConfirm( Context context, CharSequence title, CharSequence message,
            OnClickListener listener )
    {
        // Create and launch a simple confirmation dialog
        prefillBuilder( context, title, message, listener ).create().show();
    }
    
    /**
     * Open a dialog to prompt the user for a file.
     * 
     * @param context the context
     * @param title the title of the dialog
     * @param message the message to be shown inside the dialog
     * @param startPath the parent directory holding the files to select from
     * @param listener the listener to process the file, when selected
     */
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
    
    /**
     * Open a dialog to prompt the user for text.
     * 
     * @param context the context
     * @param title the title of the dialog
     * @param message the message to be shown inside the dialog
     * @param hint the hint to be shown inside the text edit widget
     * @param listener the listener to process the text, when provided
     */
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
    
    /**
     * A convenience method for consistently initializing dialogs across the various methods.
     * 
     * @param context the context
     * @param title the title of the dialog
     * @param message the message to be shown inside the dialog
     * @param listener the listener to process user clicks
     * @return the builder for the dialog
     */
    private static Builder prefillBuilder( Context context, CharSequence title,
            CharSequence message, OnClickListener listener )
    {
        return new Builder( context ).setTitle( title ).setMessage( message )
                .setNegativeButton( context.getString( android.R.string.cancel ), listener )
                .setPositiveButton( context.getString( android.R.string.ok ), listener );
    }
}
