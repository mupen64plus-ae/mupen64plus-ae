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
package paulscode.android.mupen64plusae.preference;

import java.util.List;

import org.mupen64plusae.v3.fzurita.R;

import paulscode.android.mupen64plusae.compat.AppCompatPreferenceActivity.OnPreferenceDialogListener;
import paulscode.android.mupen64plusae.dialog.PromptInputCodeDialog;
import paulscode.android.mupen64plusae.input.map.PlayerMap;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class PlayerMapPreference extends DialogPreference implements
        View.OnClickListener, OnCheckedChangeListener, OnPreferenceDialogListener, View.OnLongClickListener
{
    public static final String STATE_PROMPT_INPUT_CODE_DIALOG = "STATE_PROMPT_INPUT_CODE_DIALOG";
    public static final String STATE_SELECTED_POPUP_INDEX = "STATE_SELECTED_POPUP_INDEX";

    private final PlayerMap mMap = new PlayerMap();
    private List<Integer> mUnmappableKeyCodes;

    private Button buttonPlayer1;
    private Button buttonPlayer2;
    private Button buttonPlayer3;
    private Button buttonPlayer4;
    private CheckBox checkBoxReminder;

    private String mValue = "";
    private boolean isControllerEnabled1 = true;
    private boolean isControllerEnabled2 = true;
    private boolean isControllerEnabled3 = true;
    private boolean isControllerEnabled4 = true;

    private int mSelectedPlayer = 0;
    private FragmentActivity mAssociatedActivity = null;

    public PlayerMapPreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        setDialogLayoutResource( R.layout.player_map_preference );
        setDialogMessage( getSummary() );
        setOnPreferenceChangeListener(null);
    }

    public void setValue( String value )
    {
        mValue = value;
        if( shouldPersist() )
            persistString( mValue );
    }

    public String getValue()
    {
        return mValue;
    }

    public void setControllersEnabled( boolean player1, boolean player2, boolean player3, boolean player4 )
    {
        isControllerEnabled1 = player1;
        isControllerEnabled2 = player2;
        isControllerEnabled3 = player3;
        isControllerEnabled4 = player4;
    }

    @Override
    protected Object onGetDefaultValue( TypedArray a, int index )
    {
        return a.getString( index );
    }

    @Override
    protected void onSetInitialValue( boolean restorePersistedValue, Object defaultValue )
    {
        setValue( restorePersistedValue ? getPersistedString( mValue ) : (String) defaultValue );
    }

    @Override
    public void onBindDialogView( View view, FragmentActivity associatedActivity )
    {
        // Set the member variables
        mAssociatedActivity = associatedActivity;
        final AppData appData = new AppData( getContext() );
        final GlobalPrefs prefs = new GlobalPrefs( getContext(), appData );
        mUnmappableKeyCodes = prefs.unmappableKeyCodes;
        mMap.deserialize( mValue );

        // Initialize and refresh the widgets
        buttonPlayer1 = setupButton( view, R.id.btnPlayer1, isControllerEnabled1 );
        buttonPlayer2 = setupButton( view, R.id.btnPlayer2, isControllerEnabled2 );
        buttonPlayer3 = setupButton( view, R.id.btnPlayer3, isControllerEnabled3 );
        buttonPlayer4 = setupButton( view, R.id.btnPlayer4, isControllerEnabled4 );
        checkBoxReminder = (CheckBox) view.findViewById( R.id.checkBox );
        checkBoxReminder.setChecked( prefs.getPlayerMapReminder() );
        checkBoxReminder.setOnCheckedChangeListener( this );
        updateViews();

        buttonPlayer1.setOnLongClickListener( this );
        buttonPlayer2.setOnLongClickListener( this );
        buttonPlayer3.setOnLongClickListener( this );
        buttonPlayer4.setOnLongClickListener( this );
    }

    @Override
    public boolean onLongClick(View view)
    {
        switch( view.getId() )
        {
            case R.id.btnPlayer1:
                mMap.unmapPlayer( 1 );
                break;
            case R.id.btnPlayer2:
                mMap.unmapPlayer( 2 );
                break;
            case R.id.btnPlayer3:
                mMap.unmapPlayer( 3 );
                break;
            case R.id.btnPlayer4:
                mMap.unmapPlayer( 4 );
                break;
            default: return false;
        }

        updateViews();
        return true;
    }

    @Override
    public void onDialogClosed( boolean positiveResult )
    {

    }

    @Override
    protected Parcelable onSaveInstanceState()
    {
        final SavedStringState myState = new SavedStringState( super.onSaveInstanceState() );
        myState.mValue = mMap.serialize();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState( Parcelable state )
    {
        if( state == null || !state.getClass().equals( SavedStringState.class ) )
        {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState( state );
            return;
        }

        final SavedStringState myState = (SavedStringState) state;
        super.onRestoreInstanceState( myState.getSuperState() );
        mMap.deserialize( myState.mValue );
        updateViews();
    }

    @Override
    public void onClick( View view )
    {
        switch( view.getId() )
        {
            case R.id.btnPlayer1:
                promptPlayer( 1 );
                break;
            case R.id.btnPlayer2:
                promptPlayer( 2 );
                break;
            case R.id.btnPlayer3:
                promptPlayer( 3 );
                break;
            case R.id.btnPlayer4:
                promptPlayer( 4 );
                break;
        }
    }

    @Override
    public void onCheckedChanged( CompoundButton buttonView, boolean isChecked )
    {
        final AppData appData = new AppData( getContext() );
        new GlobalPrefs( getContext(), appData ).putPlayerMapReminder( isChecked );
    }

    private void promptPlayer( final int player )
    {
        mSelectedPlayer = player;

        final Context context = getContext();
        final String title = context.getString( R.string.playerMapPreference_popupTitle, player );
        final String message = context.getString( R.string.playerMapPreference_popupMessage, player,
                mMap.getDeviceSummary( context, player ) );
        final String btnText = context.getString( R.string.playerMapPreference_popupUnmap );


        final PromptInputCodeDialog promptInputCodeDialog = PromptInputCodeDialog.newInstance(
            title, message, btnText, mUnmappableKeyCodes);

        final FragmentManager fm = mAssociatedActivity.getSupportFragmentManager();
        promptInputCodeDialog.show(fm, STATE_PROMPT_INPUT_CODE_DIALOG);
    }

    public void onDialogClosed( int inputCode, int hardwareId, int which )
    {
        if( which != DialogInterface.BUTTON_NEGATIVE )
        {
            if( which == DialogInterface.BUTTON_POSITIVE )
                mMap.map( hardwareId, mSelectedPlayer );
            else
                mMap.unmapPlayer( mSelectedPlayer );
            updateViews();

            final String value = mMap.serialize();
            if( callChangeListener( value ) )
                setValue( value );
        }
    }

    private void updateViews()
    {
        updatePlayerButton( buttonPlayer1, 1 );
        updatePlayerButton( buttonPlayer2, 2 );
        updatePlayerButton( buttonPlayer3, 3 );
        updatePlayerButton( buttonPlayer4, 4 );
    }

    private void updatePlayerButton( Button button, int player )
    {
        if( button != null )
        {
            final Context context = getContext();
            final String deviceInfo = mMap.getDeviceSummary( context, player );
            final String buttonText = context.getString( R.string.playerMapPreference_button, player,
                    deviceInfo );
            button.setText( buttonText );
        }
    }

    private Button setupButton( View parentView, int resId, boolean isEnabled )
    {
        final Button button = (Button) parentView.findViewById( resId );
        button.setVisibility( isEnabled ? View.VISIBLE : View.GONE );
        button.setOnClickListener( this );
        return button;
    }

    @Override
    public void onPrepareDialogBuilder(Context context, Builder builder)
    {
        builder.setNegativeButton(null, null);
    }
}
