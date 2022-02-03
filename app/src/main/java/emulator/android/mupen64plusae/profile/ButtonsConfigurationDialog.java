package emulator.android.mupen64plusae.profile;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.View;
import android.widget.CheckBox;
import org.mupen64plusae.v3.alpha.R;
import emulator.android.mupen64plusae.input.map.VisibleTouchMap;

class ButtonsConfigurationDialog implements OnClickListener {

    private final Context mContext;
    private final Profile mProfile;
    private final View view;

    private final TouchscreenProfileActivity mTouchscreenProfileActivity;
    private final VisibleTouchMap mTouchscreenMap;
    private final CheckBox separateButtonsABCheckbox, separateButtonsCCheckbox;

    public ButtonsConfigurationDialog(Context context, Profile profile, TouchscreenProfileActivity touchscreenProfileActivity, VisibleTouchMap touchscreenMap) {
        mContext = context;
        mProfile = profile;
        mTouchscreenProfileActivity = touchscreenProfileActivity;
        mTouchscreenMap = touchscreenMap;
        view = View.inflate(context, R.layout.buttons_configuration, null);

        separateButtonsABCheckbox = view.findViewById(R.id.buttonsConfig_separateButtonsAB);
        separateButtonsCCheckbox = view.findViewById(R.id.buttonsConfig_separateButtonsC);

        // If Skin split A/B buttons is supported, otherwise the setting will be disabled.
        if(mTouchscreenMap.isSplitABSkin())
        {
            separateButtonsABCheckbox.setChecked(Boolean.valueOf(mProfile.get("separateButtonsAB")));
            separateButtonsABCheckbox.setEnabled(true);
        }
        else
        {
            separateButtonsABCheckbox.setChecked(false);
            separateButtonsABCheckbox.setEnabled(false);
        }

        // If Skin split C buttons is supported, otherwise the setting will be disabled.
        if(mTouchscreenMap.isSplitCSkin())
        {
            separateButtonsCCheckbox.setChecked(Boolean.valueOf(mProfile.get("separateButtonsC")));
            separateButtonsCCheckbox.setEnabled(true);
        }
        else
        {
            separateButtonsCCheckbox.setChecked(false);
            separateButtonsCCheckbox.setEnabled(false);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            // If settings has been changed, enable / disable required buttons.
            if(Boolean.valueOf(mProfile.get("separateButtonsAB")) != separateButtonsABCheckbox.isChecked())
                mTouchscreenProfileActivity.setGroupABButtons(!separateButtonsABCheckbox.isChecked());
            mProfile.put("separateButtonsAB", String.valueOf(separateButtonsABCheckbox.isChecked()));

            // If settings has been changed, enable / disable required buttons.
            if(Boolean.valueOf(mProfile.get("separateButtonsC")) != separateButtonsCCheckbox.isChecked())
                mTouchscreenProfileActivity.setGroupCButtons(!separateButtonsCCheckbox.isChecked());
            mProfile.put("separateButtonsC", String.valueOf(separateButtonsCCheckbox.isChecked()));
        }
    }


    /** Create and show this popup dialog */
    void show() {
        Builder builder = new Builder(mContext);
        builder.setTitle(mContext.getString(R.string.menuItem_buttonsConfiguration));
        builder.setView(view);
        builder.setNegativeButton(mContext.getString(android.R.string.cancel), this);
        builder.setPositiveButton(mContext.getString(android.R.string.ok), this);
        builder.setCancelable(true);
        builder.create().show();
    }
}
