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
 * Authors: Paul Lamb, littleguy77
 */
package paulscode.android.mupen64plusae.jni;

import java.util.ArrayList;

/**
 * Call-ins made from the native ae-imports library to Java. Any function names changed here should
 * also be changed in the corresponding C code, and vice versa.
 *
 * see jni/ae-bridge/ae_imports.cpp
 */
public class NativeImports
{
    interface OnStateCallbackListener
    {
        /**
         * Called when an emulator state/parameter has changed
         *
         * @param paramChanged The parameter ID.
         * @param newValue The new value of the parameter.
         */
        void onStateCallback( int paramChanged, int newValue );
    }

    public interface OnFpsChangedListener
    {
        /**
         * Called when the frame rate has changed.
         *
         * @param newValue The new FPS value.
         */
        void onFpsChanged( int newValue );
    }

    // Core state callbacks - used by NativeImports
    private static final ArrayList<OnStateCallbackListener> sStateCallbackListeners = new ArrayList<>();
    private static final Object sStateCallbackLock = new Object();

    //Frame rate info - used by ae-vidext
    private static final ArrayList<OnFpsChangedListener> sFpsListeners = new ArrayList<>();

    static void addOnStateCallbackListener( OnStateCallbackListener listener )
    {
        synchronized( sStateCallbackLock )
        {
            // Do not allow multiple instances, in case listeners want to remove themselves
            if( !sStateCallbackListeners.contains( listener ) )
                sStateCallbackListeners.add( listener );
        }
    }

    static void removeOnStateCallbackListener( OnStateCallbackListener listener )
    {
        synchronized( sStateCallbackLock )
        {
            sStateCallbackListeners.remove( listener );
        }
    }

    /**
     * Callback for when an emulator's state/parameter has changed.
     * 
     * @param paramChanged The changed parameter's ID.
     * @param newValue The new value of the changed parameter.
     * see mupen64plus-ae/app/src/main/jni/ae-bridge/ae_imports.cpp
     */
    static void stateCallback( int paramChanged, int newValue )
    {
        synchronized( sStateCallbackLock )
        {
            for( int i = sStateCallbackListeners.size(); i > 0; i-- )
            {
                // Traverse the list backwards in case any listeners remove themselves
                sStateCallbackListeners.get( i - 1 ).onStateCallback( paramChanged, newValue );
            }
        }
    }

    static void removeOnFpsChangedListener(OnFpsChangedListener fpsListener)
    {
        synchronized (sFpsListeners)
        {
            sFpsListeners.remove(fpsListener);
        }
    }

    static void addOnFpsChangedListener(OnFpsChangedListener fpsListener, int fpsRecalcPeriod )
    {
        synchronized (sFpsListeners)
        {
            if(fpsListener != null && !sFpsListeners.contains(fpsListener))
            {
                sFpsListeners.add(fpsListener);
                NativeExports.FPSEnabled(fpsRecalcPeriod);
            }
        }
    }

    static void FPSCounter (int fps)
    {
        synchronized (sFpsListeners)
        {
            for (OnFpsChangedListener listener: sFpsListeners ) {
                listener.onFpsChanged(fps);
            }
        }
    }
}
