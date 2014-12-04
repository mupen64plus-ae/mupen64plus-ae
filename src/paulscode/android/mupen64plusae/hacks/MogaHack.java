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
 * Authors: Paul Lamb
 */
package paulscode.android.mupen64plusae.hacks;

import paulscode.android.mupen64plusae.persistent.AppData;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.bda.controller.Controller;

/**
 * Temporary hack for crash in MOGA library on Lollipop. This hack can be removed once MOGA fixes
 * their library. The actual issue is caused by the use of implicit service intents, which are
 * illegal in Lollipop, as seen in the logcat message below.
 * 
 * <pre>
 * {@code Service Intent must be explicit: Intent { act=com.bda.controller.IControllerService } }
 * </pre>
 * 
 * @see <a href="http://www.mogaanywhere.com/developers/">MOGA developer site</a>
 * @see <a href="http://commonsware.com/blog/2014/06/29/dealing-deprecations-bindservice.html">
 *      Discussion on explicit intents</a>
 */
public class MogaHack
{
    public static void init( Controller controller, Context context )
    {
        if( AppData.IS_LOLLIPOP )
        {
            boolean mIsBound = false;
            java.lang.reflect.Field fIsBound = null;
            android.content.ServiceConnection mServiceConnection = null;
            java.lang.reflect.Field fServiceConnection = null;
            try
            {
                Class<?> cMogaController = controller.getClass();
                fIsBound = cMogaController.getDeclaredField( "mIsBound" );
                fIsBound.setAccessible( true );
                mIsBound = fIsBound.getBoolean( controller );
                fServiceConnection = cMogaController.getDeclaredField( "mServiceConnection" );
                fServiceConnection.setAccessible( true );
                mServiceConnection = ( android.content.ServiceConnection ) fServiceConnection.get( controller );        
            }
            catch( NoSuchFieldException e )
            {
                Log.e( "PlayMenuActivity", "MOGA Lollipop Hack NoSuchFieldException (get)", e );
            }
            catch( IllegalAccessException e )
            {
                Log.e( "PlayMenuActivity", "MOGA Lollipop Hack IllegalAccessException (get)", e );
            }
            catch( IllegalArgumentException e )
            {
                Log.e( "PlayMenuActivity", "MOGA Lollipop Hack IllegalArgumentException (get)", e );
            }
            if( ( !mIsBound ) && ( mServiceConnection != null ) )
            {
                Intent intent = new Intent( context, com.bda.controller.IControllerService.class );
                context.startService( intent );
                context.bindService( intent, mServiceConnection, 1 );
                try
                {
                    fIsBound.setBoolean( controller, true );
                }
                catch( IllegalAccessException e )
                {
                    Log.e( "PlayMenuActivity", "MOGA Lollipop Hack IllegalAccessException (set)", e );
                }
                catch( IllegalArgumentException e )
                {
                    Log.e( "PlayMenuActivity", "MOGA Lollipop Hack IllegalArgumentException (set)", e );
                }
            }
        }
        else
        {
            controller.init();
        }
    }
}
