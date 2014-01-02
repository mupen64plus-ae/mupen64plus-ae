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
package paulscode.android.mupen64plusae.profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.UserPrefs;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.Prompt.PromptConfirmListener;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * The Class ProfileActivity.
 * 
 * @param <T> the generic type
 */
abstract public class ProfileActivity<T extends Profile> extends ListActivity
{
    
    /**
     * Gets the list of available profiles. Subclasses should implement this method to search for
     * and load the profiles from disk. Subclasses need not sort the list; this is handled by the
     * base class.
     * 
     * @param includeBuiltins true to include built-in profiles in the result, false to omit
     * @return the list of available profiles
     */
    abstract protected List<T> getProfiles( boolean includeBuiltins );
    
    /**
     * Edits a profile using a subclass-specific UI. Subclasses should implement this method to
     * launch a dialog or activity, to allow the user to modify the given profile. Subclasses are
     * responsible for persisting the profile data to disk when the dialog or activity finishes.
     * 
     * @param profile the profile to be edited
     */
    abstract protected void onEditProfile( T profile );
    
    /**
     * Adds a profile using a subclass-specific persistence mechanism. Subclasses should implement
     * this method to persist a new (default or empty) profile immediately to disk. This method
     * should be used simply to create and persist the profile; it should not launch any editor UI.
     * 
     * @param name the unique name of the new profile
     * @param comment an optional brief description of the new profile
     * @return the newly created profile
     */
    abstract protected T onAddProfile( String name, String comment );
    
    /**
     * Copies a profile using a subclass-specific persistence mechanism. Subclasses should implement
     * this method to persist a new (cloned) profile immediately to disk. This method should be used
     * simply to clone and persist the profile; it should not launch any editor UI.
     * 
     * @param profile the profile to be copied
     * @param newName the unique name of the cloned profile
     * @param newComment an optional brief description of the cloned profile
     * @param the newly created profile
     */
    abstract protected T onCopyProfile( T profile, String newName, String newComment );
    
    /**
     * Renames a profile using a subclass-specific persistence mechanism. Subclasses should
     * implement this method to persist the revised name and/or comment immediately to disk. This
     * method should be used simply to persist the revisions; it should not launch any editor UI.
     * 
     * @param profile the profile to rename
     * @param newName the new (unique) name of the profile
     * @param newComment a new (optional) brief description of the profile
     */
    abstract protected void onRenameProfile( T profile, String newName, String newComment );
    
    /**
     * Deletes a profile using a subclass-specific persistence mechanism. Subclasses should
     * implement this method to remove the profile from persistent storage. This method should be
     * used simply to un-persist the profile; a basic confirmation UI capability is already provided
     * by the base class.
     * 
     * @param profile the profile
     */
    abstract protected void onDeleteProfile( T profile );
    
    /** The application data wrapper, available as a convenience to subclasses. */
    protected AppData mAppData;
    
    /** The user preferences wrapper, available as a convenience to subclasses. */
    protected UserPrefs mUserPrefs;
    
    private final List<String> mProfileNames = new ArrayList<String>();
    
    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mAppData = new AppData( this );
        mUserPrefs = new UserPrefs( this );
        mUserPrefs.enforceLocale( this );
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        refreshList();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        getMenuInflater().inflate( R.menu.profile_activity, menu );
        return super.onCreateOptionsMenu( menu );
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onPrepareOptionsMenu( Menu menu )
    {
        menu.findItem( R.id.menuItem_toggleBuiltins ).setTitle(
                getBuiltinVisibility()
                        ? R.string.menuItem_hideBuiltins
                        : R.string.menuItem_showBuiltins );
        return super.onPrepareOptionsMenu( menu );
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @TargetApi( 11 )
    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch( item.getItemId() )
        {
            case R.id.menuItem_new:
                addProfile();
                return true;
            case R.id.menuItem_toggleBuiltins:
                setBuiltinVisibility( !getBuiltinVisibility() );
                if( AppData.IS_HONEYCOMB )
                    invalidateOptionsMenu();
                refreshList();
                return true;
            default:
                return super.onOptionsItemSelected( item );
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View,
     * int, long)
     */
    @Override
    protected void onListItemClick( ListView l, View v, int position, long id )
    {
        // Popup a dialog with a context-sensitive list of options for the profile
        @SuppressWarnings( "unchecked" )
        final T profile = (T) getListView().getItemAtPosition( position );
        if( profile != null )
        {
            int resId = profile.isBuiltin
                    ? R.array.profileClickBuiltin_entries
                    : R.array.profileClickCustom_entries;
            Builder builder = new Builder( this );
            builder.setTitle( profile.name );
            builder.setItems( getResources().getTextArray( resId ),
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick( DialogInterface dialog, int which )
                        {
                            if( which >= 0 )
                            {
                                if( !profile.isBuiltin )
                                {
                                    // Custom profiles are writable
                                    switch( which )
                                    {
                                        case 0:
                                            editProfile( profile );
                                            break;
                                        case 1:
                                            copyProfile( profile );
                                            break;
                                        case 2:
                                            renameProfile( profile );
                                            break;
                                        case 3:
                                            deleteProfile( profile );
                                            break;
                                    }
                                }
                                else
                                {
                                    // Built-in profiles are read-only
                                    switch( which )
                                    {
                                        case 0:
                                            copyProfile( profile );
                                            break;
                                    }
                                }
                            }
                        }
                    } );
            builder.create().show();
        }
        super.onListItemClick( l, v, position, id );
    }
    
    private void editProfile( T profile )
    {
        assert ( !profile.isBuiltin );
        onEditProfile( profile );
    }
    
    private void addProfile()
    {
        promptNameComment( R.string.menuItem_new, "", "", false, new NameCommentListener()
        {
            @Override
            public void onAccept( String name, String comment )
            {
                editProfile( onAddProfile( name, comment ) );
                refreshList();
            }
        } );
    }
    
    private void copyProfile( final T profile )
    {
        promptNameComment( R.string.listItem_copy, profile.name, profile.comment, false,
                new NameCommentListener()
                {
                    @Override
                    public void onAccept( String name, String comment )
                    {
                        editProfile( onCopyProfile( profile, name, comment ) );
                        refreshList();
                    }
                } );
    }
    
    private void renameProfile( final T profile )
    {
        assert ( !profile.isBuiltin );
        promptNameComment( R.string.listItem_rename, profile.name, profile.comment, true,
                new NameCommentListener()
                {
                    @Override
                    public void onAccept( String name, String comment )
                    {
                        onRenameProfile( profile, name, comment );
                        refreshList();
                    }
                } );
    }
    
    private void deleteProfile( final T profile )
    {
        assert ( !profile.isBuiltin );
        String title = getString( R.string.confirm_title );
        String message = getString( R.string.confirmDeleteProfile_message, profile.name );
        Prompt.promptConfirm( this, title, message, new PromptConfirmListener()
        {
            @Override
            public void onConfirm()
            {
                onDeleteProfile( profile );
                refreshList();
            }
        } );
    }
    
    private interface NameCommentListener
    {
        void onAccept( String name, String comment );
    }
    
    private void promptNameComment( int titleId, final String name, String comment,
            final boolean allowSameName, final NameCommentListener listener )
    {
        // Create the name editor
        final EditText editName = new EditText( this );
        editName.setText( name );
        editName.setHint( R.string.hint_profileName );
        editName.setRawInputType( InputType.TYPE_CLASS_TEXT );
        
        // Create the comment editor
        final EditText editComment = new EditText( this );
        editComment.setText( comment );
        editComment.setHint( R.string.hint_profileComment );
        editComment.setRawInputType( InputType.TYPE_CLASS_TEXT );
        
        // Create the warning label
        final TextView textWarning = new TextView( this );
        int dp = 10;
        int px = Math.round( dp * getResources().getDisplayMetrics().density );
        textWarning.setPadding( px, 0, px, 0 );
        
        // Put the editors in a container
        final LinearLayout layout = new LinearLayout( this );
        layout.setOrientation( LinearLayout.VERTICAL );
        layout.addView( textWarning );
        layout.addView( editName );
        layout.addView( editComment );
        
        // Create listener for OK/cancel button clicks
        OnClickListener clickListener = new OnClickListener()
        {
            @Override
            public void onClick( DialogInterface dialog, int which )
            {
                if( which == DialogInterface.BUTTON_POSITIVE )
                {
                    String name = editName.getText().toString();
                    String comment = editComment.getText().toString();
                    listener.onAccept( name, comment );
                }
            }
        };
        
        // Create the alert dialog
        Builder builder = new Builder( this );
        builder.setTitle( titleId );
        builder.setView( layout );
        builder.setPositiveButton( android.R.string.ok, clickListener );
        builder.setNegativeButton( android.R.string.cancel, clickListener );
        final AlertDialog dialog = builder.create();
        
        // Show the dialog
        dialog.show();
        
        // Dynamically disable the OK button if the name is not unique
        final Button okButton = dialog.getButton( DialogInterface.BUTTON_POSITIVE );
        String warning = isValidName( name, name, allowSameName );
        textWarning.setText( warning );
        okButton.setEnabled( TextUtils.isEmpty( warning ) );
        editName.addTextChangedListener( new TextWatcher()
        {
            @Override
            public void onTextChanged( CharSequence s, int start, int before, int count )
            {
            }
            
            @Override
            public void beforeTextChanged( CharSequence s, int start, int count, int after )
            {
            }
            
            @Override
            public void afterTextChanged( Editable s )
            {
                String warning = isValidName( name, s.toString(), allowSameName );
                textWarning.setText( warning );
                okButton.setEnabled( TextUtils.isEmpty( warning ) );
            }
        } );
    }
    
    /**
     * Checks whether a candidate name is unique, non-empty, and contains only safe characters.
     * Unsafe characters are: '[', ']'.
     * 
     * @param oldName the old name
     * @param newName the new name
     * @param allowSameName set true to permit old and new names to be the same
     * @return empty string if the profile name is safe to use, otherwise a warning message
     */
    private String isValidName( String oldName, String newName, boolean allowSameName )
    {
        boolean isNotEmpty = !TextUtils.isEmpty( newName );
        boolean isLegal = !Pattern.matches( ".*[\\[\\]].*", newName );
        boolean isSameName = oldName.equals( newName );
        boolean isUnique = !mProfileNames.contains( newName ) || ( isSameName && allowSameName );
        
        if( !isNotEmpty )
            return getString( R.string.profile_name_cannot_be_empty );
        else if( !isLegal )
            return getString( R.string.profile_name_cannot_contain_brackets );
        else if( !isUnique )
            return getString( R.string.profile_name_must_be_unique );
        else
            return "";
    }
    
    private void setBuiltinVisibility( boolean visible )
    {
        // Persist builtin visibility for this specific subclass
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
        prefs.edit().putBoolean( getBuiltinVisibilityKey(), visible ).commit();
    }
    
    private boolean getBuiltinVisibility()
    {
        // Retrieve builtin visibility for this specific subclass
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
        return prefs.getBoolean( getBuiltinVisibilityKey(), true );
    }
    
    private String getBuiltinVisibilityKey()
    {
        // Get the subclass-specific key for persisting builtin visibility
        return "builtinsVisible" + getClass().getSimpleName();
    }
    
    private void refreshList()
    {
        // Get the profiles to be shown to the user
        List<T> profiles = getProfiles( getBuiltinVisibility() );
        Collections.sort( profiles );
        setListAdapter( new ProfileListAdapter<T>( this, profiles ) );
        
        // Get all profiles, for validating unique names
        profiles = getProfiles( true );
        mProfileNames.clear();
        for( T profile : profiles )
            mProfileNames.add( profile.name );
    }
}
