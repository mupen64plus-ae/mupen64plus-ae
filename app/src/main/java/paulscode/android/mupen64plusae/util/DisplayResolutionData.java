package paulscode.android.mupen64plusae.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.Log;
import android.view.View;

import paulscode.android.mupen64plusae.persistent.GlobalPrefs;

public class DisplayResolutionData {

    /** The width of the viewing surface, in pixels with the correct aspect ratio. */
    private int videoSurfaceWidthOriginal;

    /** The height of the viewing surface, in pixels with the correct aspect ratio. */
    private int videoSurfaceHeightOriginal;

    /** The rendering width in pixels with the correct aspect ratio. */
    private int videoRenderWidthNative;

    /** The rendering heigh in pixels with the correct aspect ratio. */
    private int videoRenderHeightNative;

    /** Screen aspect ratio */
    private float aspect;

    private GlobalPrefs mGlobalPrefs;

    public DisplayResolutionData(GlobalPrefs globalPrefs, Activity activity, View parentView, GlobalPrefs.DisplayScaling scaling) {
        mGlobalPrefs = globalPrefs;
        determineResolutionData(activity, parentView, scaling);
    }

    private void determineResolutionData(Activity activity, View parentView, GlobalPrefs.DisplayScaling scaling)
    {
        // Determine the pixel dimensions of the rendering context and view surface
        // Screen size

        final Point dimensions = new Point(0,0);
        Point fullscreenSize = new Point(0,0);
        DisplayWrapper.getRealSize(activity, fullscreenSize);

        dimensions.set(parentView.getWidth(), parentView.getHeight());

        switch (scaling) {
            case ORIGINAL:
                aspect = 3f/4f;
                break;
            case STRETCH:
                // Match the screen aspect ratio
                aspect = (float)Math.min(fullscreenSize.x, fullscreenSize.y)/Math.max(fullscreenSize.x, fullscreenSize.y);
                break;
            case STRETCH_169:
                aspect = 9f/16f;
                break;
        }

        int maxDimension = Math.max(dimensions.x, dimensions.y);
        int minDimension = Math.min(dimensions.x, dimensions.y);

        int fullscreenMinDimension = Math.min(fullscreenSize.x, fullscreenSize.y);
        videoRenderHeightNative =  fullscreenMinDimension;
        videoRenderWidthNative =  Math.round( fullscreenMinDimension/aspect );

        // Assume we are are in portrait mode if height is greater than the width
        boolean screenPortrait = dimensions.y > (float)dimensions.x;
        if(screenPortrait)
        {
            videoSurfaceWidthOriginal = minDimension;
            videoSurfaceHeightOriginal = Math.round( minDimension*aspect );
        }
        else
        {
            videoSurfaceWidthOriginal = maxDimension;
            videoSurfaceHeightOriginal = Math.round( maxDimension*aspect );

            if (videoSurfaceHeightOriginal > minDimension) {
                videoSurfaceWidthOriginal = Math.round( minDimension/aspect );
                videoSurfaceHeightOriginal = minDimension;
            }
        }

        Log.i("resolutionData", "result=(" + videoSurfaceWidthOriginal + "," + videoSurfaceHeightOriginal + ")" +
                " full=(" + fullscreenSize.x + "," + fullscreenSize.y + ")" +
                " view=(" + dimensions.x + "," + dimensions.y + ")" +
                " native=(" + videoRenderWidthNative + "," + videoRenderHeightNative + ")" +
                " aspect=" + aspect);
    }

    public int getResolutionWidth(int hResolution)
    {
        if( hResolution == -1)
        {
            hResolution = mGlobalPrefs.displayResolution;
        }

        if (hResolution == 0)
        {
            hResolution = videoRenderHeightNative;
        }

        return Math.round((float)hResolution/aspect);
    }

    public int getResolutionHeight(int hResolution)
    {
        if (hResolution == -1) {
            hResolution = mGlobalPrefs.displayResolution;
        }

        return hResolution == 0 ? videoRenderHeightNative : hResolution;
    }

    public int getSurfaceResolutionHeight()
    {
        return videoSurfaceHeightOriginal;
    }

    public int getSurfaceResolutionWidth()
    {
        return videoSurfaceWidthOriginal;
    }
}
