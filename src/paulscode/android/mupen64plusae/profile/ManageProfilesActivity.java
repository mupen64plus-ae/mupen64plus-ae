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

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.compat.AppCompatListActivity;
import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptConfirmListener;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

abstract public class ManageProfilesActivity extends AppCompatListActivity
{
    /**
     * Gets the absolute path of the {@link ConfigFile} that backs this profile. Subclasses should
     * implement this method to define the locations of the subclass-specific built-in or custom
     * config files.
     * 
     * @param isBuiltin true to return the built-in config file path; false to return the custom
     *            config file path
     * @return the absolute path of the requested config file
     */
    abstract protected String getConfigFilePath( boolean isBuiltin );
    
    /**
     * Gets the name of the profile to use if the user unsets the default. If a profile can be
     * "disabled", subclasses should return an empty string. Otherwise, subclasses should return the
     * name of a builtin profile that is guaranteed to exist (typically the default profile at
     * installation).
     * 
     * @return the default name of the default profile
     */
    abstract protected String getNoDefaultProfile();
    
    /**
     * Gets the name of the default profile. Subclasses should implement this method to retrieve the
     * persisted subclass-specific profile specified by the user.
     * 
     * @return the name of the default profile
     */
    abstract protected String getDefaultProfile();
    
    /**
     * Sets the name of the default profile. Subclasses should implement this method to persist the
     * subclass-specific profile specified by the user.
     * 
     * @param name the name of the new default profile
     */
    abstract protected void putDefaultProfile( String name );
    
    /**
     * Edits a profile using a subclass-specific UI. Subclasses should implement this method to
     * launch a dialog or activity, to allow the user to modify the given profile. Subclasses are
     * responsible for persisting the profile data to disk when the dialog or activity finishes.
     * 
     * @param profile the profile to be edited
     */
    abstract protected void onEditProfile( Profile profile );
    
    /** The back-end store for the built-in profiles, which subclasses should read from. */
    protected ConfigFile mConfigBuiltin;
    
    /** The back-end store for the custom profiles, which subclasses should read from and write to. */
    protected ConfigFile mConfigCustom;
    
    /** The application data wrapper, available as a convenience to subclasses. */
    protected AppData mAppData;
    
    /** The user preferences wrapper, available as a convenience to subclasses. */
    protected GlobalPrefs mGlobalPrefs;
    
    private final List<String> mProfileNames = new ArrayList<String>();
    
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mAppData = new AppData( this );
        mGlobalPrefs = new GlobalPrefs( this );
        mGlobalPrefs.enforceLocale( this );
        
        // Get the config files from the subclass-specified paths
        String customPath = getConfigFilePath( false );
        String builtinPath = getConfigFilePath( true );
        mConfigBuiltin = new ConfigFile( builtinPath );
        mConfigCustom = new ConfigFile( customPath );
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
        
        // Reload in case we're returning from an editor
        mConfigCustom.reload();
        refreshList();
    }
    
    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        getMenuInflater().inflate( R.menu.profile_activity, menu );
        return super.onCreateOptionsMenu( menu );
    }
    
    @Override
    public boolean onPrepareOptionsMenu( Menu menu )
    {
        menu.findItem( R.id.menuItem_toggleBuiltins ).setTitle(
                getBuiltinVisibility()
                        ? R.string.menuItem_hideBuiltins
                        : R.string.menuItem_showBuiltins );
        return super.onPrepareOptionsMenu( menu );
    }
    
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
    
    @Override
    protected void onListItemClick( ListView l, View v, int position, long id )
    {
        // Popup a dialog with a context-sensitive list of options for the profile
        final Profile profile = (Profile) getListView().getItemAtPosition( position );
        if( profile != null )
        {
            final boolean isDefault = profile.name.equals( getDefaultProfile() );
            int resId = profile.isBuiltin
                    ? R.array.profileClickBuiltin_entries
                    : R.array.profileClickCustom_entries;
            CharSequence[] items = getResources().getTextArray( resId );
            if( isDefault )
                items[0] = getString( R.string.listItem_unsetDefault );
            
            Builder builder = new Builder( this );
            int stringId = profile.isBuiltin
                    ? R.string.popup_titleBuiltin
                    : R.string.popup_titleCustom;
            builder.setTitle( getString( stringId, profile.name ) );
            builder.setItems( items, new DialogInterface.OnClickListener()
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
                                            putDefaultProfile( isDefault
                                                    ? getNoDefaultProfile()
                                                    : profile.name );
                                            refreshList();
                                            break;
                                        case 1:
                                            editProfile( profile );
                                            break;
                                        case 2:
                                            copyProfile( profile );
                                            break;
                                        case 3:
                                            renameProfile( profile );
                                            break;
                                        case 4:
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
                                            putDefaultProfile( isDefault
                                                    ? getNoDefaultProfile()
                                                    : profile.name );
                                            refreshList();
                                            break;
                                        case 1:
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
    
    private void editProfile( Profile profile )
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
                assert ( !mConfigCustom.keySet().contains( name ) );
                Profile profile = new Profile( false, name, comment );
                profile.writeTo( mConfigCustom );
                mConfigCustom.save();
                refreshList();
                editProfile( profile );
            }
        } );
    }
    
    private void copyProfile( final Profile profile )
    {
        promptNameComment( R.string.listItem_copy, profile.name, profile.comment, false,
                new NameCommentListener()
                {
                    @Override
                    public void onAccept( String name, String comment )
                    {
                        assert ( !mConfigCustom.keySet().contains( name ) );
                        Profile newProfile = profile.copy( name, comment );
                        newProfile.writeTo( mConfigCustom );
                        mConfigCustom.save();
                        refreshList();
                        editProfile( newProfile );
                    }
                } );
    }
    
    private void renameProfile( final Profile profile )
    {
        assert ( !profile.isBuiltin );
        promptNameComment( R.string.listItem_rename, profile.name, profile.comment, true,
                new NameCommentListener()
                {
                    @Override
                    public void onAccept( String name, String comment )
                    {
                        mConfigCustom.remove( profile.name );
                        Profile newProfile = profile.copy( name, comment );
                        newProfile.writeTo( mConfigCustom );
                        mConfigCustom.save();
                        refreshList();
                    }
                } );
    }
    
    private void deleteProfile( final Profile profile )
    {
        assert ( !profile.isBuiltin );
        String title = getString( R.string.confirm_title );
        String message = getString( R.string.confirmDeleteProfile_message, profile.name );
        Prompt.promptConfirm( this, title, message, new PromptConfirmListener()
        {
            @Override
            public void onConfirm()
            {
                assert ( mConfigCustom.keySet().contains( profile.name ) );
                mConfigCustom.remove( profile.name );
                mConfigCustom.save();
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
        List<Profile> profiles1 = Profile.getProfiles( mConfigCustom, false );
        if( getBuiltinVisibility() )
            profiles1.addAll( Profile.getProfiles( mConfigBuiltin, true ) );
        Collections.sort( profiles1 );
        setListAdapter( new ProfileListAdapter( this, profiles1 ) );
        
        // Get all profiles, for validating unique names
        List<Profile> profiles2 = Profile.getProfiles( mConfigCustom, false );
        profiles2.addAll( Profile.getProfiles( mConfigBuiltin, true ) );
        mProfileNames.clear();
        for( Profile profile : profiles2 )
            mProfileNames.add( profile.name );
    }
    
    private class ProfileListAdapter extends ArrayAdapter<Profile>
    {
        private static final int RESID = R.layout.list_item_two_text_icon;
        
        public ProfileListAdapter( Context context, List<Profile> profiles )
        {
            super( context, RESID, profiles );
        }
        
        @Override
        public View getView( int position, View convertView, ViewGroup parent )
        {
            Context context = getContext();
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            View view = convertView;
            if( view == null )
                view = inflater.inflate( RESID, null );
            
            Profile item = getItem( position );
            if( item != null )
            {
                TextView text1 = (TextView) view.findViewById( R.id.text1 );
                TextView text2 = (TextView) view.findViewById( R.id.text2 );
                ImageView icon = (ImageView) view.findViewById( R.id.icon );
                
                text1.setText( item.name );
                text2.setText( item.comment );
                if( item.name.equals( getDefaultProfile() ) )
                    icon.setImageResource( R.drawable.ic_sliders2 );
                else
                    icon.setImageResource( R.drawable.ic_sliders );
            }
            return view;
        }
    }
}
