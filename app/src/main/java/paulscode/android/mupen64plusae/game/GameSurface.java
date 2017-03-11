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
 * Authors: Paul Lamb, littleguy77, Gillou68310
 */
package paulscode.android.mupen64plusae.game;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

/**
 * Represents a graphical area of memory that can be drawn to.
 */
public class GameSurface extends SurfaceView
{
    //Game surface created listener
    GameSurfaceCreatedListener mGameSurfaceCreatedListener = null;
    //Listener for a game surface created event
    public interface GameSurfaceCreatedListener {
        //This is called every time the game surface is created
        void onGameSurfaceCreated();
    }

    // LogCat strings for debugging, defined here to simplify maintenance/lookup
    protected static final String TAG = "GameSurface";

    /**
     * Constructor that is called when inflating a view from XML. This is called when a view is
     * being constructed from an XML file, supplying attributes that were specified in the XML file.
     * This version uses a default style of 0, so the only attribute values applied are those in the
     * Context's Theme and the given AttributeSet. The method onFinishInflate() will be called after
     * all children have been added.
     *
     * @param context The Context the view is running in, through which it can access the current
     *            theme, resources, etc.
     * @param attribs The attributes of the XML tag that is inflating the view.
     */
    public GameSurface(Context context, AttributeSet attribs )
    {
        super( context, attribs );
    }

    /**
     * Set the game surface created listener
     * @param gameSurfaceCreatedListener Game surface created listener
     */
    public void SetGameSurfaceCreatedListener(GameSurfaceCreatedListener gameSurfaceCreatedListener)
    {
        mGameSurfaceCreatedListener = gameSurfaceCreatedListener;
    }
}
