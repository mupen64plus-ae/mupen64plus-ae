package paulscode.android.mupen64plusae.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Insets;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.annotation.NonNull;

public class DisplayWrapper {

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public static Display getDisplay(Activity activity)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return activity.getDisplay();
        } else {
            return activity.getWindowManager().getDefaultDisplay();
        }
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public static Display getDisplay(Context context)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return context.getDisplay();
        } else {
            final WindowManager windowManager = (WindowManager) context.getSystemService(android.content.Context.WINDOW_SERVICE);
            return windowManager != null ? windowManager.getDefaultDisplay() : null;
        }
    }

    public static void getRealSize(Context context, Point dimensions)
    {
        Display display = DisplayWrapper.getDisplay(context);
        if( display != null ) {
            display.getRealSize(dimensions);
        }
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public static void getSize(Context context, Point dimensions)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowManager windowManager = (WindowManager) context.getSystemService(android.content.Context.WINDOW_SERVICE);

            if (windowManager != null) {
                final WindowMetrics metrics = windowManager.getCurrentWindowMetrics();
                // Gets all excluding insets
                final WindowInsets windowInsets = metrics.getWindowInsets();
                Insets insets = windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()
                        | WindowInsets.Type.displayCutout());

                int insetsWidth = insets.right + insets.left;
                int insetsHeight = insets.top + insets.bottom;

                // Legacy size that Display#getSize reports
                final Rect bounds = metrics.getBounds();
                dimensions.set(bounds.width() - insetsWidth, bounds.height() - insetsHeight);
            }

        } else {
            Display display = DisplayWrapper.getDisplay(context);
            if (display != null) {
                display.getSize(dimensions);
            }
        }
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public static int getScreenWidth(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = activity.getWindowManager().getCurrentWindowMetrics();
            Insets insets = windowMetrics.getWindowInsets()
                    .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
            return windowMetrics.getBounds().width() - insets.left - insets.right;
        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            return displayMetrics.widthPixels;
        }
    }
}