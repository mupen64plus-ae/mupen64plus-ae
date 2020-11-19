package paulscode.android.mupen64plusae.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Insets;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

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

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public static void setDialogToResizeWithKeyboard(AlertDialog dialog, View dialogView) {
        /* Make the dialog resize to the keyboard */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setDecorFitsSystemWindows(false);

                dialogView.setOnApplyWindowInsetsListener((view, insets) -> {
                    WindowInsets imeWindowInsets = view.getRootWindowInsets();

                    if (imeWindowInsets.isVisible(WindowInsets.Type.ime())) {
                        // Move view by the height of the IME
                        Insets imeInsets = view.getRootWindowInsets().getInsets(WindowInsets.Type.ime());
                        view.setTranslationX((float)imeInsets.bottom);
                    } else {
                        view.setTranslationX(0);
                    }

                    return insets;
                });
            }
        } else {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
        }
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public static void setFullScreen(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController insetsController = activity.getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.statusBars());
            }
        } else {
            activity.getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
        }
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public static void enableImmersiveMode(Activity activity, View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.getWindow().setDecorFitsSystemWindows(false);
        } else {
            view.setSystemUiVisibility( View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN );
        }
    }

}