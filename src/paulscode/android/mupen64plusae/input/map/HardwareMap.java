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
package paulscode.android.mupen64plusae.input.map;

import java.util.ArrayList;
import java.util.List;

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.input.provider.AbstractProvider.OnInputListener;
import paulscode.android.mupen64plusae.input.provider.AxisProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider;
import paulscode.android.mupen64plusae.input.provider.KeyProvider.ImeFormula;
import paulscode.android.mupen64plusae.input.provider.LazyProvider;
import paulscode.android.mupen64plusae.persistent.AppData;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.ImageView;

public class HardwareMap
{
    /** Flag indicating whether hardware filtering is enabled. */
    static boolean sDisabled = true;
    
    /** Map from hardware identifier to player number. */
    static final SparseIntArray sMap = new SparseIntArray();
    
    public static boolean testHardware( int hardwareId, int player )
    {
        return sDisabled || sMap.get( hardwareId, 0 ) == player;
    }
    
    public static boolean isEnabled()
    {
        return !sDisabled;
    }
    
    public static void setEnabled( boolean value )
    {
        sDisabled = !value;
    }
    
    public static void map( int hardwareId, int player )
    {
        if( player > 0 && player < 5 )
            sMap.put( hardwareId, player );
        else
            Log.w( "InputMap", "Invalid player specified in mapPlayer: " + player );
    }
    
    public static void unmap( int hardwareId )
    {
        sMap.delete( hardwareId );
    }
    
    public static void unmapAll()
    {
        sMap.clear();
    }
    
    public static class Prompter
    {
        public static void show( Context context, final int player, List<Integer> ignoredKeyCodes )
        {
            // Create a custom view to provide key/motion event data
            // This can be absolutely any kind of view, we just something to dispatch events
            ImageView view = new ImageView( context );
            view.setImageResource( R.drawable.ic_gamepad );
            
            // Set the focus parameters of the view so that it will dispatch events
            view.setFocusable( true );
            view.setFocusableInTouchMode( true );
            view.requestFocus();
            
            // Construct an object to aggregate key and motion event data
            LazyProvider provider = new LazyProvider();
            
            // Connect the upstream key event listener
            provider.addProvider( new KeyProvider( view, ImeFormula.DEFAULT, ignoredKeyCodes ) );
            
            // Connect the upstream motion event listener
            if( AppData.IS_HONEYCOMB_MR1 )
                provider.addProvider( new AxisProvider( view ) );
            
            // The hardware ids to be mapped to the player upon Ok
            final ArrayList<Integer> hardwareIds = new ArrayList<Integer>();
            
            // Connect the downstream listener
            provider.registerListener( new OnInputListener()
            {
                @Override
                public void onInput( int[] inputCodes, float[] strengths, int hardwareId )
                {
                    if( !hardwareIds.contains( hardwareId ) )
                        hardwareIds.add( hardwareId );
                }
                
                @Override
                public void onInput( int inputCode, float strength, int hardwareId )
                {
                    if( !hardwareIds.contains( hardwareId ) )
                        hardwareIds.add( hardwareId );
                }
            } );
            
            // Define the action to take when the dialog closes
            DialogInterface.OnClickListener clickListener = new OnClickListener()
            {
                @Override
                public void onClick( DialogInterface dialog, int which )
                {
                    if( which == DialogInterface.BUTTON_POSITIVE )
                    {
                        for( Integer hardwareId : hardwareIds )
                            map( hardwareId, player );
                    }
                }
            };
            
            // Create and show the dialog
            String title = context.getString( R.string.hardwareMapPrompt_title, player );
            String message = context.getString( R.string.hardwareMapPrompt_message, player );
            new Builder( context )
                    .setTitle( title )
                    .setMessage( message )
                    .setCancelable( false )
                    .setView( view )
                    .setNegativeButton( context.getString( android.R.string.cancel ), clickListener )
                    .setPositiveButton( context.getString( android.R.string.ok ), clickListener )
                    .create().show();
        }
    }
}
