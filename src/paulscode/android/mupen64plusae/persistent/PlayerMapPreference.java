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
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class PlayerMapPreference extends DialogPreference implements
        DialogInterface.OnClickListener, View.OnClickListener
{
    private final PlayerMap mMap = new PlayerMap();
    private List<Integer> mUnmappableKeyCodes;
    
    private Button buttonPlayer1;
    private Button buttonPlayer2;
    private Button buttonPlayer3;
    private Button buttonPlayer4;
    
    private String mValue = "";
    
    private void log( String func )
    {
        log( func, 0 );
    }
    
    private void log( String func, int num )
    {
        Log.w( "PlayerMapPreference", func + num + " val(" + mValue + ") map(" + mMap.serialize() + ")" );
    }
    
    public PlayerMapPreference( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        
        // Setup the layout
        setDialogLayoutResource( R.layout.player_map_preference );
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
    
    @Override
    protected Object onGetDefaultValue( TypedArray a, int index )
    {
        log( "onGetDefaultValue" );
        return a.getString( index );
    }
    
    @Override
    protected void onSetInitialValue( boolean restorePersistedValue, Object defaultValue )
    {
        log( "onSetInitialValue" );
        setValue( restorePersistedValue ? getPersistedString( mValue ) : (String) defaultValue );
        log( "onSetInitialValue", 1 );
    }
    
    @Override
    protected View onCreateView( ViewGroup parent )
    {
        log( "onCreateView" );
        return super.onCreateView( parent );
    }
    
    @Override
    protected void onBindView( View view )
    {
        log( "onBindView" );
        super.onBindView( view );
    }
    
    @Override
    protected View onCreateDialogView()
    {
        log( "onCreateDialogView" );
        return super.onCreateDialogView();
    }
    
    @Override
    protected void onBindDialogView( View view )
    {
        log( "onBindDialogView" );
        // Set up the dialog view seen when the preference menu item is clicked
        super.onBindDialogView( view );
        
        // Set the member variables
        UserPrefs prefs = new UserPrefs( getContext() );
        mUnmappableKeyCodes = prefs.unmappableKeyCodes;
        mMap.deserialize( mValue );
        log( "onBindDialogView", 1 );

        // Initialize and refresh the widgets
        buttonPlayer1 = setupButton( view, R.id.btnPlayer1, prefs.inputMap1.isEnabled() );
        buttonPlayer2 = setupButton( view, R.id.btnPlayer2, prefs.inputMap2.isEnabled() );
        buttonPlayer3 = setupButton( view, R.id.btnPlayer3, prefs.inputMap3.isEnabled() );
        buttonPlayer4 = setupButton( view, R.id.btnPlayer4, prefs.inputMap4.isEnabled() );
        updateViews();
    }
    
    @Override
    protected void onPrepareDialogBuilder( Builder builder )
    {
        log( "onPrepareDialogBuilder" );
        super.onPrepareDialogBuilder( builder );
    }
    
    @Override
    protected void onDialogClosed( boolean positiveResult )
    {
        log( "onDialogClosed" );
        super.onDialogClosed( positiveResult );
        
        if( positiveResult )
        {
            String value = mMap.serialize();
            if( callChangeListener( value ) )
                setValue( value );
        }
    }
    
    @Override
    protected Parcelable onSaveInstanceState()
    {
        log( "onSaveInstanceState" );
        final SavedStringState myState = new SavedStringState( super.onSaveInstanceState() );
        myState.mValue = mMap.serialize();
        return myState;
    }
    
    @Override
    protected void onRestoreInstanceState( Parcelable state )
    {
        final SavedStringState myState = (SavedStringState) state;
        super.onRestoreInstanceState( myState.getSuperState() );
        mMap.deserialize( myState.mValue );
        updateViews();
        log( "onRestoreInstanceState" );
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
        Context context = getContext();
        String title = context.getString( R.string.playerMapPreference_popupTitle, player );
        String message = context.getString( R.string.playerMapPreference_popupMessage, player,
                mMap.getDeviceSummary( context, player ) );
        String btnText = context.getString( R.string.playerMapPreference_popupUnmap );
        
        Prompt.promptInputCode( getContext(), title, message, btnText, mUnmappableKeyCodes,
                new OnInputCodeListener()
                {
                    @Override
                    public void OnInputCode( int inputCode, int hardwareId )
                    {
                        log( "promptPlayer" );
                        if( inputCode == 0 )
                            mMap.unmapPlayer( player );
                        else
                            mMap.map( hardwareId, player );
                        log( "promptPlayer", 1 );
                        updateViews();
                        log( "promptPlayer", 2 );
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
            Context context = getContext();
            String deviceInfo = mMap.getDeviceSummary( context, player );
            String buttonText = context.getString( R.string.playerMapPreference_button, player,
                    deviceInfo );
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
