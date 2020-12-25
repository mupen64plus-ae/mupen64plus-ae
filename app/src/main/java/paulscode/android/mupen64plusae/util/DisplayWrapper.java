package paulscode.android.mupen64plusae.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Insets;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

@SuppressWarnings({"unused","deprecation", "RedundantSuppression"})

public class DisplayWrapper {

    public static Display getDisplay(Activity activity)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Display display = null;
            try {
                display = activity.getDisplay();
            } catch (java.lang.UnsupportedOperationException e) {
                Log.e("DisplayWrapper", "Can't get display from service");
            }
            return display;
        } else {
            return activity.getWindowManager().getDefaultDisplay();
        }
    }

    public static Display getDisplay(Context context)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Display display = null;
            try {
                display = context.getDisplay();
            } catch (java.lang.UnsupportedOperationException e) {
                Log.e("DisplayWrapper", "Can't get display from service");
            }
            return display;
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

    public static void setFullScreen(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Window window = activity.getWindow();
            if (window != null) {
                // Force the decor view to be initialized so that the follow up call doesn't crash
                window.getDecorView();
                final WindowInsetsController insetsController = window.getInsetsController();
                if (insetsController != null) {
                    insetsController.hide(WindowInsets.Type.statusBars());
                }
            }
        } else {
            activity.getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
        }
    }

    public static void enableImmersiveMode(Activity activity) {
        // TODO: This is buggy in Android 11 multiform Windowed mode, the window title bar doesn't go away
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.getWindow().setDecorFitsSystemWindows(false);

            Window window = activity.getWindow();
            if (window != null) {
                // Force the decor view to be initialized so that the follow up call doesn't crash
                window.getDecorView();
                WindowInsetsController controller = activity.getWindow().getInsetsController();
                if(controller != null) {
                    controller.hide(WindowInsets.Type.displayCutout() | WindowInsets.Type.systemBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            }
        } else */{
            Window window = activity.getWindow();
            if (window != null) {
                View decorView = window.getDecorView();
                decorView.setSystemUiVisibility( View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

                decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        decorView.setSystemUiVisibility( View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                    }
                });
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Window window = activity.getWindow();
            if (window != null) {
                WindowManager.LayoutParams param = window.getAttributes();
                if (param != null) {
                    param.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                }
            }
        }
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression", "unused"})
    public static void drawBehindSystemBars(Activity activity) {
        // TODO: This is buggy in Android 11, the same functionality can't be obtained when in free form window mode
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.getWindow().setDecorFitsSystemWindows(false);
        } else */{
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }
}