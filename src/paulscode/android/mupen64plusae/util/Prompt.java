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

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider.OnInputListener;
import paulscode.android.mupen64plusae.input.provider.AxisProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider.ImeFormula;
import paulscode.android.mupen64plusae.input.provider.LazyProvider;
import paulscode.android.mupen64plusae.persistent.AppData;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * A utility class that generates dialogs to prompt the user for information.
 */
public class Prompt
{
    /**
     * The listener interface for receiving a file selected by the user.
     * 
     * @see Prompt#promptFile
     */
    public interface OnFileListener
    {
        /**
         * Process the file selected by the user.
         * 
         * @param file The file selected by the user, or null.
         * @param which The DialogInterface button pressed by the user.
         */
        public void onFile( File file, int which );
    }
    
    /**
     * The listener interface for receiving text provided by the user.
     * 
     * @see Prompt#promptText
     */
    public interface OnTextListener
    {
        /**
         * Process the text provided by the user.
         * 
         * @param text The text provided by the user, or null.
         * @param which The DialogInterface button pressed by the user.
         */
        public void onText( CharSequence text, int which );
    }
    
    /**
     * The listener interface for receiving an input code provided by the user.
     * 
     * @see Prompt#promptInputCode
     */
    public interface OnInputCodeListener
    {
        /**
         * Process the input code provided by the user.
         * 
         * @param inputCode The input code provided by the user, or 0.
         * @param hardwareId The identifier of the source device.
         */
        public void OnInputCode( int inputCode, int hardwareId );
    }
    
    /**
     * Open a dialog to prompt the user for a confirmation (Ok/Cancel).
     * 
     * @param context The activity context.
     * @param title The title of the dialog.
     * @param message The message to be shown inside the dialog.
     * @param listener The listener to process the confirmation.
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
                    listener.onFile( new File( absolutePaths.get( which ) ),
                            DialogInterface.BUTTON_POSITIVE );
                else
                    listener.onFile( null, which );
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
     * @param context The activity context.
     * @param title The title of the dialog.
     * @param message The message to be shown inside the dialog.
     * @param hint The hint to be shown inside the text edit widget.
     * @param inputType The type of input expected, e.g. InputType.TYPE_CLASS_NUMBER.
     * @param listener The listener to process the text, when provided.
     */
    public static void promptText( Context context, CharSequence title, CharSequence message,
            CharSequence hint, int inputType, final OnTextListener listener )
    {
        // Create an edit-text widget, and add the hint text
        final EditText editText = new EditText( context );
        editText.setHint( hint );
        editText.setRawInputType( inputType );
        
        // When the user clicks Ok, notify the downstream listener
        OnClickListener internalListener = new OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                    listener.onText( editText.getText().toString(), which );
                else
                    listener.onText( null, which );
            }
        };
        
        // Create and launch the dialog, adding the edit-text widget in the process
        prefillBuilder( context, title, message, internalListener ).setView( editText ).create()
                .show();
    }
    
    /**
     * Open a dialog to prompt the user for an input code.
     * 
     * @param context The activity context.
     * @param title The title of the dialog.
     * @param message The message to be shown inside the dialog.
     * @param positiveButtonText The text to be shown on the positive button, or null.
     * @param ignoredKeyCodes The key codes to ignore.
     * @param listener The listener to process the input code, when provided.
     */
    public static void promptInputCode( Context context, CharSequence title, CharSequence message,
            CharSequence positiveButtonText, List<Integer> ignoredKeyCodes,
            final OnInputCodeListener listener )
    {
        // Create a widget to dispatch key/motion event data
        FrameLayout view = new FrameLayout( context );
        ImageView image = new ImageView( context );
        image.setImageResource( R.drawable.ic_controller );
        EditText dummyImeListener = new EditText( context );
        dummyImeListener.setVisibility( View.INVISIBLE );
        dummyImeListener.setHeight( 0 );
        view.addView( image );
        view.addView( dummyImeListener );
        
        // Set the focus parameters of the view so that it will dispatch events
        view.setFocusable( true );
        view.setFocusableInTouchMode( true );
        view.requestFocus();
        
        // Notify the client when the user clicks the dialog's positive button
        DialogInterface.OnClickListener clickListener = new OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                    listener.OnInputCode( 0, 0 );
            }
        };
        
        // Create the dialog, customizing the view and button text in the process
        final AlertDialog dialog = prefillBuilder( context, title, message, clickListener )
                .setPositiveButton( positiveButtonText, clickListener ).setView( view ).create();
        
        // Construct an object to aggregate key and motion event data
        LazyProvider provider = new LazyProvider();
        
        // Connect the upstream key event listener
        provider.addProvider( new KeyProvider( view, ImeFormula.DEFAULT, ignoredKeyCodes ) );
        
        // Connect the upstream motion event listener
        if( AppData.IS_HONEYCOMB_MR1 )
            provider.addProvider( new AxisProvider( view ) );
        
        // Connect the downstream listener
        provider.registerListener( new OnInputListener()
        {
            @Override
            public void onInput( int[] inputCodes, float[] strengths, int hardwareId )
            {
            }
            
            @Override
            public void onInput( int inputCode, float strength, int hardwareId )
            {
                if( inputCode != 0 )
                {
                    listener.OnInputCode( inputCode, hardwareId );
                    dialog.dismiss();
                }
            }
        } );
        
        // Launch the dialog
        dialog.show();
    }
    
    /**
     * A convenience method for consistently initializing dialogs across the various methods.
     * 
     * @param context The activity context.
     * @param title The title of the dialog.
     * @param message The message to be shown inside the dialog.
     * @param listener The listener to process user clicks.
     * @return The builder for the dialog
     */
    private static Builder prefillBuilder( Context context, CharSequence title,
            CharSequence message, OnClickListener listener )
    {
        return new Builder( context ).setTitle( title ).setMessage( message ).setCancelable( false )
                .setNegativeButton( context.getString( android.R.string.cancel ), listener )
                .setPositiveButton( context.getString( android.R.string.ok ), listener );
    }
}
