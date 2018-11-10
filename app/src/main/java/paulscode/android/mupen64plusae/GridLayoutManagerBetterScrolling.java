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
 * Authors: fzurita
 */
package paulscode.android.mupen64plusae;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

/**
 * {@link GridLayoutManager} extension which introduces workaround for focus
 * finding bug when navigating with dpad. See
 * https://gist.github.com/vganin/8930b41f55820ec49e4d and
 * https://code.google.com/p/android/issues/detail?id=190526&thanks=190526&ts=1445108573
 *
 * @see <a href="http://stackoverflow.com/questions/31596801/recyclerview-focus-scrolling">
 *      http://stackoverflow.com/questions/31596801/recyclerview-focus-scrolling</a>
 *      
 * @see https://code.google.com/p/android/issues/detail?id=81855
 * @see https://code.google.com/p/android/issues/detail?id=81854
 */
public class GridLayoutManagerBetterScrolling extends androidx.recyclerview.widget.GridLayoutManager
{

    public GridLayoutManagerBetterScrolling(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public GridLayoutManagerBetterScrolling(Context context, int spanCount)
    {
        super(context, spanCount);
    }

    public GridLayoutManagerBetterScrolling(Context context, int spanCount, int orientation, boolean reverseLayout)
    {
        super(context, spanCount, orientation, reverseLayout);
    }

    @Override
    public View onFocusSearchFailed(View focused, int focusDirection, RecyclerView.Recycler recycler,
        RecyclerView.State state)
    {
        // Need to be called in order to layout new row/column
        View nextFocus = super.onFocusSearchFailed(focused, focusDirection, recycler, state);

        if (nextFocus == null)
        {
            return null;
        }

        int fromPos = getPosition(focused);
        int nextPos = getNextViewPos(fromPos, focusDirection);
        
        return findViewByPosition(nextPos);
    }

    /**
     * Manually detect next view to focus.
     *
     * @param fromPos
     *            from what position start to seek.
     * @param direction
     *            in what direction start to seek. Your regular
     *            {@code View.FOCUS_*}.
     * @return adapter position of next view to focus. May be equal to
     *         {@code fromPos}.
     */
    protected int getNextViewPos(int fromPos, int direction)
    {
        int offset = calcOffsetToNextView(direction);

        if (hitBorder(fromPos, offset))
        {
            return fromPos;
        }

        return fromPos + offset;
    }

    /**
     * Calculates position offset.
     *
     * @param direction
     *            regular {@code View.FOCUS_*}.
     * @return position offset according to {@code direction}.
     */
    protected int calcOffsetToNextView(int direction)
    {
        int spanCount = getSpanCount();
        int orientation = getOrientation();

        if (orientation == VERTICAL)
        {
            switch (direction)
            {
            case View.FOCUS_DOWN:
                return spanCount;
            case View.FOCUS_UP:
                return -spanCount;
            case View.FOCUS_RIGHT:
                return 1;
            case View.FOCUS_LEFT:
                return -1;
            }
        }
        else if (orientation == HORIZONTAL)
        {
            switch (direction)
            {
            case View.FOCUS_DOWN:
                return 1;
            case View.FOCUS_UP:
                return -1;
            case View.FOCUS_RIGHT:
                return spanCount;
            case View.FOCUS_LEFT:
                return -spanCount;
            }
        }

        return 0;
    }

    /**
     * Checks if we hit borders.
     *
     * @param from
     *            from what position.
     * @param offset
     *            offset to new position.
     * @return {@code true} if we hit border.
     */
    private boolean hitBorder(int from, int offset)
    {
        int spanCount = getSpanCount();

        if (Math.abs(offset) == 1)
        {
            int spanIndex = from % spanCount;
            int newSpanIndex = spanIndex + offset;
            return newSpanIndex < 0 || newSpanIndex >= spanCount;
        }
        else
        {
            int newPos = from + offset;
            return newPos < 0 || newPos >= spanCount;
        }
    }
}