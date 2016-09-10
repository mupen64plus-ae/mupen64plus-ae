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
 * Authors: Paul Lamb, littleguy77
 */
package paulscode.android.mupen64plusae.jni;


/**
 * Call-ins made from the native ae-imports library to Java. Any function names changed here should
 * also be changed in the corresponding C code, and vice versa.
 * 
 * @see jni/ae-bridge/ae_imports.cpp
 * @see CoreInterface
 */
public class NativeImports extends CoreInterface
{
    /**
     * Callback for when an emulator's state/parameter has changed.
     * 
     * @param paramChanged The changed parameter's ID.
     * @param newValue The new value of the changed parameter.
     * @see jni/ae-bridge/ae_imports.cpp
     */
    public static void stateCallback( int paramChanged, int newValue )
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
}
