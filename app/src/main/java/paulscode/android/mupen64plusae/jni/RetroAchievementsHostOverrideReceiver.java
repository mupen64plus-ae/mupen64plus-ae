package paulscode.android.mupen64plusae.jni;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RetroAchievementsHostOverrideReceiver extends BroadcastReceiver {

    private static final String TAG = "RetroAchievements";

    private static final String SET_ACTION_SUFFIX   = ".action.SET_RETROACHIEVEMENTS_HOST_OVERRIDE";
    private static final String CLEAR_ACTION_SUFFIX = ".action.CLEAR_RETROACHIEVEMENTS_HOST_OVERRIDE";
    private static final String HOST_EXTRA = "host";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null || intent.getAction() == null) return;
        String action = intent.getAction();

        if (action.endsWith(SET_ACTION_SUFFIX)) {
            String host = intent.getStringExtra(HOST_EXTRA);
            if (host == null || host.trim().isEmpty()) {
                Log.w(TAG, "SET host override ignored: missing '" + HOST_EXTRA + "' extra");
                return;
            }
            RetroAchievementsManager.setHostOverride(context, host);
        } else if (action.endsWith(CLEAR_ACTION_SUFFIX)) {
            RetroAchievementsManager.setHostOverride(context, null);
        } else {
            Log.w(TAG, "Unknown host-override action: " + action);
        }
    }
}
