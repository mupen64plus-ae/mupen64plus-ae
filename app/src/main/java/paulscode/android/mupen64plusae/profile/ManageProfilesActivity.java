/*
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

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.mupen64plusae.v3.alpha.BuildConfig;
import org.mupen64plusae.v3.alpha.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import paulscode.android.mupen64plusae.MenuListView;
import paulscode.android.mupen64plusae.compat.AppCompatListActivity;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog;
import paulscode.android.mupen64plusae.dialog.ConfirmationDialog.PromptConfirmListener;
import paulscode.android.mupen64plusae.dialog.MenuDialogFragment;
import paulscode.android.mupen64plusae.dialog.MenuDialogFragment.OnDialogMenuItemSelectedListener;
import paulscode.android.mupen64plusae.dialog.ProfileNameEditDialog;
import paulscode.android.mupen64plusae.dialog.ProfileNameEditDialog.OnProfileNameDialogButtonListener;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.ConfigFile;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import paulscode.android.mupen64plusae.util.LocaleContextWrapper;

abstract public class ManageProfilesActivity extends AppCompatListActivity implements OnDialogMenuItemSelectedListener, OnProfileNameDialogButtonListener,
    PromptConfirmListener
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
    abstract protected ConfigFile getConfigFile( boolean isBuiltin );
    
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
    
    /**
     * Returns the title of the activity resource id
     * @return title of the activity resource id
     */
    abstract protected int getWindowTitleResource();
    
    private static final String STATE_MENU_DIALOG_FRAGMENT = "STATE_MENU_DIALOG_FRAGMENT";
    private static final String STATE_PROFILE_EDIT_DIALOG_FRAGMENT = "STATE_PROFILE_EDIT_DIALOG_FRAGMENT";
    private static final String STATE_CURRENT_SELECTED_ITEM = "STATE_CURRENT_SELECTED_ITEM";
    private static final String STATE_CURRENT_SELECTED_OPERATION = "STATE_CURRENT_SELECTED_OPERATION";
    private static final int DELETE_PROFILE_CONFIRM_DIALOG_ID = 0;
    private static final String DELETE_PROFILE_CONFIRM_DIALOG_STATE = "DELETE_PROFILE_CONFIRM_DIALOG_STATE";
    
    /** The back-end store for the built-in profiles, which subclasses should read from. */
    protected ConfigFile mConfigBuiltin;
    
    /** The back-end store for the custom profiles, which subclasses should read from and write to. */
    protected ConfigFile mConfigCustom;
    
    /** The application data wrapper, available as a convenience to subclasses. */
    protected AppData mAppData;
    
    /** The user preferences wrapper, available as a convenience to subclasses. */
    protected GlobalPrefs mGlobalPrefs;
    
    private final List<String> mProfileNames = new ArrayList<>();
    
    /** Profile list adapter */
    private ProfileListAdapter mProfileListAdapter = null;
    
    /** Profile list **/
    List<Profile> mProfileList = new ArrayList<>();
    
    /** Current listview position */
    private int mListViewPosition = 0;
    
    /** Current selectedOperation */
    private int mSelectedOperation = 0;

    @Override
    protected void attachBaseContext(Context newBase) {
        if(TextUtils.isEmpty(LocaleContextWrapper.getLocalCode()))
        {
            super.attachBaseContext(newBase);
        }
        else
        {
            super.attachBaseContext(LocaleContextWrapper.wrap(newBase,LocaleContextWrapper.getLocalCode()));
        }
    }

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
         
        setContentView( R.layout.manage_profiles_activity );
        
        // Add the toolbar to the activity (which supports the fancy menu/arrow animation)
        Toolbar toolbar = findViewById( R.id.toolbar );
        toolbar.setTitle( getWindowTitleResource() );
        setSupportActionBar( toolbar );
        
        mAppData = new AppData( this );
        mGlobalPrefs = new GlobalPrefs( this, mAppData );
        
        // Get the config files from the subclass-specified paths
        mConfigBuiltin = getConfigFile( true );
        mConfigCustom = getConfigFile( false );
        
        if( savedInstanceState != null )
        {
            mListViewPosition = savedInstanceState.getInt(STATE_CURRENT_SELECTED_ITEM);
            mSelectedOperation = savedInstanceState.getInt(STATE_CURRENT_SELECTED_OPERATION);
        }
        
        refreshList();
    }
    
    @Override
    public void onSaveInstanceState( Bundle savedInstanceState )
    {
        savedInstanceState.putInt(STATE_CURRENT_SELECTED_ITEM, mListViewPosition);
        savedInstanceState.putInt(STATE_CURRENT_SELECTED_OPERATION, mSelectedOperation);
        
        super.onSaveInstanceState( savedInstanceState );
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
    
    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        mSelectedOperation = item.getItemId();
        
        switch( item.getItemId() )
        {
            case R.id.menuItem_new:
                promptNameComment( R.string.menuItem_new, "", "", false);
                return true;
            case R.id.menuItem_toggleBuiltins:
                setBuiltinVisibility( !getBuiltinVisibility() );
                invalidateOptionsMenu();
                refreshList();
                return true;
            default:
                return super.onOptionsItemSelected( item );
        }
    }

    /**
     * Returns the menu resource assicated with a specific profile
     * @param isBuiltin True if the menu resource should be the built in one
     * @return The menu resource based on whether it's a built in type
     */
    protected int getMenuResource(final boolean isBuiltin)
    {
        return isBuiltin
                ? R.menu.profile_click_menu_builtin
                : R.menu.profile_click_menu_custom;
    }

    
    @Override
    protected void onListItemClick( ListView l, View v, int position, long id )
    {
        mListViewPosition = position;
        // Popup a dialog with a context-sensitive list of options for the profile
        final Profile profile = (Profile) getListView().getItemAtPosition( position );
        if( profile != null )
        {
            int resId = getMenuResource(profile.isBuiltin);

            int stringId = profile.isBuiltin
                    ? R.string.popup_titleBuiltin
                    : R.string.popup_titleCustom;
            
            MenuDialogFragment menuDialogFragment = MenuDialogFragment.newInstance(0,
                getString( stringId, profile.name ), resId);
            
            FragmentManager fm = getSupportFragmentManager();
            menuDialogFragment.show(fm, STATE_MENU_DIALOG_FRAGMENT);
        }
        super.onListItemClick( l, v, position, id );
    }
    
    @Override
    public void onPrepareMenuList(MenuListView listView)
    {

        // Popup a dialog with a context-sensitive list of options for the profile
        final Profile profile = (Profile) getListView().getItemAtPosition( mListViewPosition );

        if( profile.name.equals( getDefaultProfile() ) )
        {
            MenuItem defaultProfileItem = profile.isBuiltin ?
                listView.getMenu().findItem(R.id.menuItem_setUnsetDefaultBuiltinProfile) :
                listView.getMenu().findItem(R.id.menuItem_setUnsetDefaultCustomProfile);
                defaultProfileItem.setTitle(getString( R.string.listItem_unsetDefault ));
        }
    }
    
    @Override
    public void onDialogMenuItemSelected( int dialogId, MenuItem item)
    {
        //We can only get here if mListViewPosition is valid, so profile shouldn't be null
        final Profile profile = (Profile) getListView().getItemAtPosition( mListViewPosition );
        final boolean isDefault = profile.name.equals( getDefaultProfile() );
        
        mSelectedOperation = item.getItemId();
        
        switch (mSelectedOperation)
        {
        case R.id.menuItem_setUnsetDefaultCustomProfile:
            putDefaultProfile( isDefault
                ? getNoDefaultProfile()
                : profile.name );
            refreshList();
            break;
        case R.id.menuItem_editCustomProfile:
            editProfile( profile );
            break;
        case R.id.menuItem_copyCustomProfile:
            promptNameComment( R.string.listItem_copy, profile.name, profile.comment, false);
            break;
        case R.id.menuItem_renameCustomProfile:
            promptNameComment( R.string.listItem_rename, profile.name, profile.comment, true);
            break;
        case R.id.menuItem_deleteCustomProfile:
            deleteProfile( profile );
            break;
        case R.id.menuItem_setUnsetDefaultBuiltinProfile:
            putDefaultProfile( isDefault
                ? getNoDefaultProfile()
                : profile.name );
            refreshList();
            break;
        case R.id.menuItem_copyBUiltinProfile:
            promptNameComment( R.string.listItem_copy, profile.name, profile.comment, false);
            break;
        }
    }
    
    private void editProfile( Profile profile )
    {
        if(BuildConfig.DEBUG && profile.isBuiltin)
            throw new RuntimeException();
        
        onEditProfile( profile );
    }
    
    private void addProfile(String name, String comment)
    {
        if (BuildConfig.DEBUG && mConfigCustom.keySet().contains(name))
            throw new RuntimeException();

        Profile profile = new Profile(false, name, comment);
        profile.writeTo(mConfigCustom);
        mConfigCustom.save();
        refreshList();
        editProfile(profile);
    }
    
    private void copyProfile(String name, String comment)
    {
        final Profile profile = (Profile) getListView().getItemAtPosition(mListViewPosition);

        if (BuildConfig.DEBUG && mConfigCustom.keySet().contains(name))
            throw new RuntimeException();

        Profile newProfile = profile.copy(name, comment);
        newProfile.writeTo(mConfigCustom);
        mConfigCustom.save();
        refreshList();
        editProfile(newProfile);
    }
    
    private void renameProfile(String name, String comment)
    {
        Profile profile = (Profile) getListView().getItemAtPosition(mListViewPosition);

        mConfigCustom.remove(profile.name);
        Profile newProfile = profile.copy(name, comment);
        newProfile.writeTo(mConfigCustom);
        mConfigCustom.save();
        refreshList();
    }
    
    private void deleteProfile( Profile profile)
    {
        if(BuildConfig.DEBUG && profile.isBuiltin)
            throw new RuntimeException();

        String title = getString( R.string.confirm_title );
        String message = getString( R.string.confirmDeleteProfile_message, profile.name );
        
        ConfirmationDialog confirmationDialog =
            ConfirmationDialog.newInstance(DELETE_PROFILE_CONFIRM_DIALOG_ID, title, message);
        
        FragmentManager fm = getSupportFragmentManager();
        confirmationDialog.show(fm, DELETE_PROFILE_CONFIRM_DIALOG_STATE);
    }
    
    @Override
    public void onPromptDialogClosed(int id, int which)
    {        
        if( id == DELETE_PROFILE_CONFIRM_DIALOG_ID &&
            which == DialogInterface.BUTTON_POSITIVE )
        {
            Profile profile = (Profile) getListView().getItemAtPosition( mListViewPosition );
            boolean isDefault = profile.name.equals( getDefaultProfile() );
            
            if(BuildConfig.DEBUG && !mConfigCustom.keySet().contains( profile.name ))
                throw new RuntimeException();
        
            //If this was the default profile, pick another default profile
            if(isDefault)
            {
                putDefaultProfile(getNoDefaultProfile());
            }

            mConfigCustom.remove( profile.name );
            mConfigCustom.save();
            refreshList();
        }
    }
    
    private void promptNameComment( int titleId, final String name, String comment,
            final boolean allowSameName )
    {
        
        ProfileNameEditDialog profileNameEditDialogFragment = ProfileNameEditDialog.newInstance(0,
            getString(titleId), name, comment, mProfileNames, allowSameName);
        
        FragmentManager fm = getSupportFragmentManager();
        profileNameEditDialogFragment.show(fm, STATE_PROFILE_EDIT_DIALOG_FRAGMENT);
    }
    
    @Override
    public void onProfileNameDialogButton( int dialogId, int selectedButton, String name, String comment )
    {
        if( selectedButton == DialogInterface.BUTTON_POSITIVE )
        {            
            switch (mSelectedOperation)
            {
            case R.id.menuItem_new:
                addProfile(name, comment);
                break;
            case R.id.menuItem_copyCustomProfile:
                copyProfile(name, comment);
                break;
            case R.id.menuItem_renameCustomProfile:
                renameProfile(name, comment);
                break;
            case R.id.menuItem_copyBUiltinProfile:
                copyProfile(name, comment);
                break;
            }
        }
    }
    
    private void setBuiltinVisibility( boolean visible )
    {
        // Persist builtin visibility for this specific subclass
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
        prefs.edit().putBoolean( getBuiltinVisibilityKey(), visible ).apply();
    }
    
    private boolean getBuiltinVisibility()
    {
        // Retrieve builtin visibility for this specific subclass
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
        return prefs.getBoolean( getBuiltinVisibilityKey(), true );
    }

    /*
     * Key to use when showing builtin profiles
     */
    abstract protected String getBuiltinVisibilityKey();
    
    protected void refreshList()
    {
        // Get the profiles to be shown to the user
        mProfileList.clear();
        mProfileList.addAll( Profile.getProfiles( mConfigCustom, false ));
        if( getBuiltinVisibility() )
            mProfileList.addAll( Profile.getProfiles( mConfigBuiltin, true ) );
        Collections.sort( mProfileList );
        
        if(mProfileListAdapter == null)
        {
            mProfileListAdapter = new ProfileListAdapter( this, mProfileList );
            setListAdapter( mProfileListAdapter );
        }

        mProfileListAdapter.notifyDataSetChanged();
        
        // Get all profiles, for validating unique names
        List<Profile> profiles2 = Profile.getProfiles( mConfigCustom, false );
        profiles2.addAll( Profile.getProfiles( mConfigBuiltin, true ) );
        
        // Add reserved profile names
        profiles2.add(new Profile( true, getText( R.string.default_profile_title ).toString(), null));
        profiles2.add(new Profile( true, getText( R.string.listItem_disabled ).toString(), null));

        mProfileNames.clear();
        for( Profile profile : profiles2 )
            mProfileNames.add( profile.name );
    }
    
    private class ProfileListAdapter extends ArrayAdapter<Profile>
    {
        private static final int RESID = R.layout.list_item_two_text_icon;
        
        ProfileListAdapter( Context context, List<Profile> profiles )
        {
            super( context, RESID, profiles );
        }
        
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent )
        {
            Context context = getContext();
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            View view = convertView;
            if( view == null && inflater != null)
                view = inflater.inflate( RESID, null );

            //Be extra careful
            if( view == null )
                view = new View(ManageProfilesActivity.this);
            
            Profile item = getItem( position );
            if( item != null)
            {
                TextView text1 = view.findViewById( R.id.text1 );
                TextView text2 = view.findViewById( R.id.text2 );
                ImageView icon = view.findViewById( R.id.icon );
                
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
    
    protected void RemoveProfile(String profileName)
    {
        Profile profile = new Profile(true, profileName, null);
        mProfileList.remove(profile);
    }
}
