/**
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
package paulscode.android.mupen64plusae.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider.OnInputListener;
import paulscode.android.mupen64plusae.input.provider.AxisProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider.ImeFormula;
import paulscode.android.mupen64plusae.persistent.AppData;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

/**
 * A utility class that generates dialogs to prompt the user for information.
 */
public class Prompt
{
    /**
     * An interface that simplifies the population of list items.
     * 
     * @param <T> The type of the data to be wrapped.
     * @see Prompt#createAdapter(Context, List, int, int, ListItemPopulator)
     */
    public interface ListItemPopulator<T>
    {
        public void onPopulateListItem( T item, int position, View view );
    }
    
    /**
     * An interface that simplifies the population of list items having two text fields and an icon.
     * 
     * @param <T> The type of the data to be wrapped.
     * @see Prompt#createAdapter(Context, List, ListItemTwoTextIconPopulator)
     */
    public interface ListItemTwoTextIconPopulator<T>
    {
        public void onPopulateListItem( T item, int position, TextView text1, TextView text2,
                ImageView icon );
    }
    
    /**
     * The listener interface for handling confirmations.
     * 
     * @see Prompt#promptConfirm
     */
    public interface OnConfirmListener
    {
        /**
         * Handle the user's confirmation.
         */
        public void onConfirm();
    }
    
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
     * Create a {@link ListAdapter} where each list item has a specified layout.
     * 
     * @param <T> The type of the data to be wrapped.
     * @param context The current context.
     * @param items The data source for the list items.
     * @param layoutResId The layout resource to be used for each list item.
     * @param textResId The {@link TextView} resource within the layout to be populated by default.
     * @param populator The object to populate the fields in each list item.
     * @return An adapter that can be used to create list dialogs.
     */
    public static <T> ArrayAdapter<T> createAdapter( Context context, List<T> items,
            final int layoutResId, final int textResId, final ListItemPopulator<T> populator )
    {
        return new ArrayAdapter<T>( context, layoutResId, textResId, items )
        {
            @Override
            public View getView( int position, View convertView, ViewGroup parent )
            {
                View row;
                if( convertView == null )
                {
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                            Context.LAYOUT_INFLATER_SERVICE );
                    row = (View) inflater.inflate( layoutResId, null );
                }
                else
                {
                    row = (View) convertView;
                }
                
                populator.onPopulateListItem( getItem( position ), position, row );
                return row;
            }
        };
    }
    
    /**
     * Create a {@link ListAdapter} where each list item has two text fields and an icon.
     * 
     * @param <T> The type of the data to be wrapped.
     * @param context The activity context.
     * @param items The data source for list items.
     * @param populator The object to populate the fields in each list item.
     * @return An adapter that can be used to create list dialogs.
     */
    public static <T> ArrayAdapter<T> createAdapter( Context context, List<T> items,
            final ListItemTwoTextIconPopulator<T> populator )
    {
        return createAdapter( context, items, R.layout.list_item_two_text_icon, R.id.text1,
                new ListItemPopulator<T>()
                {
                    @Override
                    public void onPopulateListItem( T item, int position, View view )
                    {
                        TextView text1 = (TextView) view.findViewById( R.id.text1 );
                        TextView text2 = (TextView) view.findViewById( R.id.text2 );
                        ImageView icon = (ImageView) view.findViewById( R.id.icon );
                        populator.onPopulateListItem( item, position, text1, text2, icon );
                    }
                } );
    }
    
    public static ArrayAdapter<String> createFilenameAdapter( Context context, List<String> paths,
            final List<CharSequence> names )
    {
        return createAdapter( context, paths, new ListItemTwoTextIconPopulator<String>()
        {
            @Override
            public void onPopulateListItem( String path, int position, TextView text1,
                    TextView text2, ImageView icon )
            {
                if( !TextUtils.isEmpty( path ) )
                {
                    String name = names.get( position ).toString();
                    if( name.equals( ".." ) )
                    {
                        text1.setText( R.string.pathPreference_parentFolder );
                        icon.setVisibility( View.VISIBLE );
                        icon.setImageResource( R.drawable.ic_arrow_u );
                    }
                    else
                    {
                        File file = new File( path );
                        text1.setText( name );
                        if( file.isDirectory() )
                        {
                            icon.setVisibility( View.VISIBLE );
                            icon.setImageResource( R.drawable.ic_folder );
                        }
                        else
                        {
                            icon.setVisibility( View.GONE );
                            icon.setImageResource( 0 );
                        }
                    }
                    text2.setVisibility( View.GONE );
                    text2.setText( null );
                }
            }
        } );
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
            final OnConfirmListener listener )
    {
        // When the user clicks Ok, notify the downstream listener
        OnClickListener internalListener = new OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                    listener.onConfirm();
            }
        };
        
        // Create and launch a simple confirmation dialog
        prefillBuilder( context, title, message, internalListener ).create().show();
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
        final List<CharSequence> names = new ArrayList<CharSequence>();
        final List<String> paths = new ArrayList<String>();
        FileUtil.populate( startPath, false, false, true, names, paths );
        
        // When the user clicks a file, notify the downstream listener
        OnClickListener internalListener = new OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                if( which >= 0 && which < names.size() )
                    listener.onFile( new File( paths.get( which ) ),
                            DialogInterface.BUTTON_POSITIVE );
                else
                    listener.onFile( null, which );
            }
        };
        
        // Create adapter for displaying files in list
        ArrayAdapter<String> adapter = Prompt.createFilenameAdapter( context, paths, names );
        
        // Create and launch the dialog, removing Ok button and populating list in the process
        prefillBuilder( context, title, message, internalListener ).setPositiveButton( null, null )
                .setAdapter( adapter, internalListener ).create().show();
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
        
        OnInputListener inputListener = new OnInputListener()
        {
            @Override
            public void onInput( int[] inputCodes, float[] strengths, int hardwareId )
            {
                if( inputCodes == null || strengths == null )
                    return;
                
                // Find the strongest input
                float maxStrength = 0;
                int strongestInputCode = 0;
                for( int i = 0; i < inputCodes.length; i++ )
                {
                    // Identify the strongest input
                    float strength = strengths[i];
                    if( strength > maxStrength )
                    {
                        maxStrength = strength;
                        strongestInputCode = inputCodes[i];
                    }
                }
                
                // Call the overloaded method with the strongest found
                onInput( strongestInputCode, maxStrength, hardwareId );
            }
            
            @Override
            public void onInput( int inputCode, float strength, int hardwareId )
            {
                if( inputCode != 0 && strength > AbstractProvider.STRENGTH_THRESHOLD )
                {
                    listener.OnInputCode( inputCode, hardwareId );
                    dialog.dismiss();
                }
            }
        };
        
        // Connect the upstream key event listener
        new KeyProvider( view, ImeFormula.DEFAULT, ignoredKeyCodes )
                .registerListener( inputListener );
        
        // Connect the upstream motion event listener
        if( AppData.IS_HONEYCOMB_MR1 )
            new AxisProvider( view ).registerListener( inputListener );
        
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
