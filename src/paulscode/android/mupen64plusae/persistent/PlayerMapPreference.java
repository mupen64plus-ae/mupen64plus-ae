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
package paulscode.android.mupen64plusae.persistent;

import java.util.List;

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.input.map.PlayerMap;
import paulscode.android.mupen64plusae.util.Prompt;
import paulscode.android.mupen64plusae.util.Prompt.OnInputCodeListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

public class PlayerMapPreference extends DialogPreference implements
        DialogInterface.OnClickListener, View.OnClickListener
{
    private final PlayerMap mMap;
    private List<Integer> mUnmappableKeyCodes;
    
    private Button buttonPlayer1;
    private Button buttonPlayer2;
    private Button buttonPlayer3;
    private Button buttonPlayer4;
    
    public PlayerMapPreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        
        mMap = new PlayerMap();
        setDialogLayoutResource( R.layout.player_map_preference );
    }
    
    @Override
    protected Object onGetDefaultValue( TypedArray a, int index )
    {
        return a.getString( index );
    }
    
    @Override
    protected void onSetInitialValue( boolean restorePersistedValue, Object defaultValue )
    {
        if( restorePersistedValue )
        {
            // Restore persisted value
            mMap.deserialize( getPersistedString( "" ) );
        }
        else
        {
            // Set default state from the XML attribute
            String value = (String) defaultValue;
            persistString( value );
            mMap.deserialize( value );
        }
        updateViews();
    }
    
    @Override
    protected void onBindView( View view )
    {
        // Set up the menu item seen in the preferences menu
        super.onBindView( view );
        
        // Restore the persisted map
        mMap.deserialize( getPersistedString( "" ) );
    }
    
    @Override
    protected void onBindDialogView( View view )
    {
        // Set up the dialog view seen when the preference menu item is clicked
        super.onBindDialogView( view );
        
        // Disable any buttons that do not apply
        UserPrefs prefs = new UserPrefs( getContext() );
        mUnmappableKeyCodes = prefs.unmappableKeyCodes;
        buttonPlayer1 = setupButton( view, R.id.btnPlayer1, prefs.inputMap1.isEnabled() );
        buttonPlayer2 = setupButton( view, R.id.btnPlayer2, prefs.inputMap2.isEnabled() );
        buttonPlayer3 = setupButton( view, R.id.btnPlayer3, prefs.inputMap3.isEnabled() );
        buttonPlayer4 = setupButton( view, R.id.btnPlayer4, prefs.inputMap4.isEnabled() );
        
        // Refresh the dialog view
        updateViews();
    }
    
    @Override
    protected void onDialogClosed( boolean positiveResult )
    {
        if( positiveResult )
        {
            // User pressed Ok: clean the state by persisting map
            persistString( mMap.serialize() );
            notifyChanged();
        }
        else
        {
            // User pressed Cancel/Back: clean the state by restoring map
            mMap.deserialize( getPersistedString( "" ) );
        }
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
    
    private void promptPlayer( final int player )
    {
        String title = getContext().getString( R.string.playerMapPreference_popupTitle, player );
        String message = getContext().getString( R.string.playerMapPreference_popupMessage, player,
                mMap.getMappedDeviceInfo( player ) );
        String btnText = getContext().getString( R.string.playerMapPreference_popupUnmap );
        
        Prompt.promptInputCode( getContext(), title, message, btnText, mUnmappableKeyCodes,
                new OnInputCodeListener()
                {
                    @Override
                    public void OnInputCode( int inputCode, int hardwareId )
                    {
                        if( inputCode == 0 )
                            mMap.unmapPlayer( player );
                        else
                            mMap.map( hardwareId, player );
                        updateViews();
                    }
                } );
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
            String deviceInfo = mMap.getMappedDeviceInfo( player );
            String buttonText = getContext().getString( R.string.playerMapPreference_btnPlayer,
                    player, deviceInfo );
            button.setText( buttonText );
        }
    }
    
    private Button setupButton( View parentView, int resId, boolean isEnabled )
    {
        Button button = (Button) parentView.findViewById( resId );
        button.setEnabled( isEnabled );
        button.setOnClickListener( this );
        return button;
    }
}
