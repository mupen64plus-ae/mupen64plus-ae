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
package paulscode.android.mupen64plusae.preference;

import java.util.List;

import org.mupen64plusae.v3.alpha.R;

import paulscode.android.mupen64plusae.compat.AppCompatPreferenceActivity.OnPreferenceDialogListener;
import paulscode.android.mupen64plusae.dialog.GameSettingsDialog;
import paulscode.android.mupen64plusae.dialog.PromptInputCodeDialog;
import paulscode.android.mupen64plusae.game.GameActivity;
import paulscode.android.mupen64plusae.input.map.PlayerMap;
import paulscode.android.mupen64plusae.persistent.AppData;
import paulscode.android.mupen64plusae.persistent.GlobalPrefs;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Parcelable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

public class PlayerMapPreference extends DialogPreference implements
        View.OnClickListener, OnPreferenceDialogListener, View.OnLongClickListener
{
    private static final String STATE_PROMPT_INPUT_CODE_DIALOG = "STATE_PROMPT_INPUT_CODE_DIALOG";
    private static final String STATE_SETTINGS_FRAGMENT = "STATE_SETTINGS_FRAGMENT";

    private final PlayerMap mMap = new PlayerMap();
    private List<Integer> mUnmappableKeyCodes;

    private Button buttonPlayer1;
    private Button buttonPlayer2;
    private Button buttonPlayer3;
    private Button buttonPlayer4;

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
        setPositiveButtonText(null);
        setNegativeButtonText(null);
    }

    public void setValue( String value )
    {
        mValue = value;
        if( shouldPersist() )
            persistString( mValue );
    }

    public void rePromptPlayer(int player, FragmentActivity associatedActivity){
        mAssociatedActivity = associatedActivity;
        final AppData appData = new AppData( getContext() );
        final GlobalPrefs prefs = new GlobalPrefs( getContext(), appData );
        mUnmappableKeyCodes = prefs.unmappableKeyCodes;
        mMap.deserialize( mValue );
        promptPlayer(player);
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
    protected void onSetInitialValue( Object defaultValue )
    {
        setValue( getSharedPreferences().contains(getKey()) ? getPersistedString( mValue ) : (String) defaultValue );
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
        updateViews();

        buttonPlayer1.setOnLongClickListener( this );
        buttonPlayer2.setOnLongClickListener( this );
        buttonPlayer3.setOnLongClickListener( this );
        buttonPlayer4.setOnLongClickListener( this );
    }

    @Override
    public boolean onLongClick(View view)
    {
        int id = view.getId();
        if (id == R.id.btnPlayer1) {
            mMap.unmapPlayer(1);
        } else if (id == R.id.btnPlayer2) {
            mMap.unmapPlayer(2);
        } else if (id == R.id.btnPlayer3) {
            mMap.unmapPlayer(3);
        } else if (id == R.id.btnPlayer4) {
            mMap.unmapPlayer(4);
        } else {
            return false;
        }

        final String value = mMap.serialize();
        if( callChangeListener( value ) )
            setValue( value );

        updateViews();
        setLongClickOnDialog(true);
        return true;
    }

    public void dismissFragments(FragmentActivity associatedActivity){
        if(mAssociatedActivity == null)
            mAssociatedActivity = associatedActivity;
        final FragmentManager fm = mAssociatedActivity.getSupportFragmentManager();
        PromptInputCodeDialog promptInputCodeDialog = (PromptInputCodeDialog) fm.findFragmentByTag(STATE_PROMPT_INPUT_CODE_DIALOG);
        if(promptInputCodeDialog != null){
            promptInputCodeDialog.dismiss();
        }
    }

    private void playerMapDialogCheck(){
        if(mAssociatedActivity != null) {
            GameSettingsDialog gameSettings = (GameSettingsDialog) mAssociatedActivity.
                    getSupportFragmentManager().findFragmentByTag(STATE_SETTINGS_FRAGMENT);
            if (gameSettings != null && mSelectedPlayer != 0) {
                gameSettings.playerMapDialogCheck(mSelectedPlayer);
            }
        }
    }

    private void setLongClickOnDialog(boolean longClick){
        if(mAssociatedActivity != null) {
            GameSettingsDialog gameSettings = (GameSettingsDialog) mAssociatedActivity.
                    getSupportFragmentManager().findFragmentByTag(STATE_SETTINGS_FRAGMENT);
            if (gameSettings != null) {
                gameSettings.setLongClickOnDialog(longClick);
            }
        }
    }

    @Override
    public void onDialogClosed( boolean positiveResult )
    {
        playerMapDialogCheck();
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
        int id = view.getId();
        if (id == R.id.btnPlayer1) {
            promptPlayer(1);
        } else if (id == R.id.btnPlayer2) {
            promptPlayer(2);
        } else if (id == R.id.btnPlayer3) {
            promptPlayer(3);
        } else if (id == R.id.btnPlayer4) {
            promptPlayer(4);
        }
    }

    private void promptPlayer( final int player )
    {
        mSelectedPlayer = player;

        final Context context = getContext();
        final String title = context.getString( R.string.playerMapPreference_popupTitle, player );
        final String message = context.getString( R.string.playerMapPreference_popupMessage, player,
                mMap.getDeviceSummary( context, player ) );
        final String btnText = context.getString( R.string.playerMapPreference_popupUnmap );


        final FragmentManager fm = mAssociatedActivity.getSupportFragmentManager();

        PromptInputCodeDialog p = (PromptInputCodeDialog) fm.findFragmentByTag(STATE_PROMPT_INPUT_CODE_DIALOG);
        if (p != null)
            p.dismiss();

        final PromptInputCodeDialog promptInputCodeDialog = PromptInputCodeDialog.newInstance(
                title, message, btnText, mUnmappableKeyCodes);
        promptInputCodeDialog.show(fm, STATE_PROMPT_INPUT_CODE_DIALOG);

        GameSettingsDialog gameSettings = (GameSettingsDialog) fm.findFragmentByTag(STATE_SETTINGS_FRAGMENT);
        if(gameSettings != null) {
            GameActivity game = (GameActivity) mAssociatedActivity;
            game.setAssociatedDialogFragment(mSelectedPlayer);
        }
    }

    private void dialogDeleted(){
        if(mAssociatedActivity != null) {
            GameSettingsDialog gameSettings = (GameSettingsDialog) mAssociatedActivity.
                    getSupportFragmentManager().findFragmentByTag(STATE_SETTINGS_FRAGMENT);
            if (gameSettings != null) {
                gameSettings.dialogDeleted();
            }
        }
    }

    public void onDialogClosed(int hardwareId, int which)
    {
        if( which != DialogInterface.BUTTON_NEGATIVE )
        {
            if( which == DialogInterface.BUTTON_POSITIVE ) {
                mMap.map(hardwareId, mSelectedPlayer);
                dialogDeleted();
            } else {
//                Log.i("TAG","Unmapping "+mMap.getDeviceSummary( getContext(), mSelectedPlayer ));
                if(mMap.getDeviceSummary( getContext(), mSelectedPlayer ) != null &&
                        !mMap.getDeviceSummary( getContext(), mSelectedPlayer ).equals(""))
                    dialogDeleted();
                mMap.unmapPlayer(mSelectedPlayer);
            }
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
        final Button button = parentView.findViewById( resId );
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
