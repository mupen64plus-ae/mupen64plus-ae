package paulscode.android.mupen64plusae.profile;

import java.util.Locale;

import org.mupen64plusae.v3.fzurita.R;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import paulscode.android.mupen64plusae.dialog.Prompt;
import paulscode.android.mupen64plusae.dialog.Prompt.PromptIntegerListener;
import paulscode.android.mupen64plusae.util.SafeMethods;

class SensorConfigurationDialog implements OnClickListener {

    private static final int MIN_SENSITIVITY = 30;
    private static final int MAX_SENSITIVITY = 300;
    private final Context mContext;
    private final Profile mProfile;
    private final View view;

    private final String[] axes;
    private final CheckBox activateOnStart;
    private final Spinner xAxisSpinner, yAxisSpinner;
    private final EditText xAxisEditText, yAxisEditText;
    private final EditText xAngleEditText, yAngleEditText;
    private final Button xSensitivityButton, ySensitivityButton;
    private final CheckBox xInvertCheckbox, yInvertCheckbox;

    public SensorConfigurationDialog(Context context, Profile profile) {
        mContext = context;
        mProfile = profile;
        view = View.inflate(context, R.layout.sensor_configuration, null);
        axes = context.getResources().getStringArray(R.array.sensorConfig_axis_values);
        activateOnStart = (CheckBox) view.findViewById(R.id.sensorConfig_activateOnStart);

        xAxisSpinner = (Spinner) view.findViewById(R.id.sensorConfig_sensorX);
        xAxisEditText = (EditText) view.findViewById(R.id.sensorConfig_customX);
        xAngleEditText = (EditText) view.findViewById(R.id.sensorConfig_angleX);
        xSensitivityButton = (Button) view.findViewById(R.id.sensorConfig_sensitivityX);
        xInvertCheckbox = (CheckBox) view.findViewById(R.id.sensorConfig_invertX);
        yAxisSpinner = (Spinner) view.findViewById(R.id.sensorConfig_sensorY);
        yAxisEditText = (EditText) view.findViewById(R.id.sensorConfig_customY);
        yAngleEditText = (EditText) view.findViewById(R.id.sensorConfig_angleY);
        ySensitivityButton = (Button) view.findViewById(R.id.sensorConfig_sensitivityY);
        yInvertCheckbox = (CheckBox) view.findViewById(R.id.sensorConfig_invertY);

        // Updating values from profile
        activateOnStart.setChecked(Boolean.valueOf(mProfile.get("sensorActivateOnStart")));
        String xAxisValue = mProfile.get("sensorAxisX", "");
        updateSpinner(xAxisSpinner, xAxisValue);
        xAxisEditText.setText(xAxisValue);
        xAngleEditText.setText(mProfile.get("sensorAngleX"));
        xSensitivityButton.setText(mProfile.get("sensorSensitivityX", "100") + "%");
        xInvertCheckbox.setChecked(Boolean.valueOf(mProfile.get("sensorInvertX")));
        String yAxisValue = mProfile.get("sensorAxisY", "");
        updateSpinner(yAxisSpinner, yAxisValue);
        yAxisEditText.setText(yAxisValue);
        yAngleEditText.setText(mProfile.get("sensorAngleY"));
        ySensitivityButton.setText(mProfile.get("sensorSensitivityY", "100") + "%");
        yInvertCheckbox.setChecked(Boolean.valueOf(mProfile.get("sensorInvertY")));

        // Registering listeners
        xAxisEditText.setFilters(newAxisEditTextFilters());
        xAxisEditText.addTextChangedListener(updateSpinnerOnTextChanged(xAxisSpinner));
        xAxisSpinner.setOnItemSelectedListener(modifyEditTextOnItemSelected(xAxisEditText));
        openSeekBarOnClick(xSensitivityButton);
        yAxisEditText.setFilters(newAxisEditTextFilters());
        yAxisEditText.addTextChangedListener(updateSpinnerOnTextChanged(yAxisSpinner));
        yAxisSpinner.setOnItemSelectedListener(modifyEditTextOnItemSelected(yAxisEditText));
        openSeekBarOnClick(ySensitivityButton);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            mProfile.put("sensorActivateOnStart", String.valueOf(activateOnStart.isChecked()));
            mProfile.put("sensorAxisX", fixSensorAxisString(xAxisEditText.getText().toString()));
            mProfile.put("sensorAxisY", fixSensorAxisString(yAxisEditText.getText().toString()));
            mProfile.put("sensorAngleX", String.valueOf(SafeMethods.toFloat(xAngleEditText.getText().toString(), 0)));
            mProfile.put("sensorAngleY", String.valueOf(SafeMethods.toFloat(yAngleEditText.getText().toString(), 0)));
            mProfile.put("sensorSensitivityX", String.valueOf(getSensitivity(xSensitivityButton)));
            mProfile.put("sensorSensitivityY", String.valueOf(getSensitivity(ySensitivityButton)));
            mProfile.put("sensorInvertX", String.valueOf(xInvertCheckbox.isChecked()));
            mProfile.put("sensorInvertY", String.valueOf(yInvertCheckbox.isChecked()));
        }
    }

    /**
     * Complete the String to match the pattern [XYZ]+/[XYZ]+, ignoring case,
     * and denying the same character to appear twice
     * 
     * @param sensorAxis
     *            the String that must already be filtered by
     *            {@link #newAxisEditTextFilters()}
     */
    private String fixSensorAxisString(String sensorAxis) {
        if (sensorAxis.isEmpty()) {
            return sensorAxis;
        }
        // This is already filtered but can be incomplete
        if (sensorAxis.indexOf('/') == -1) {
            sensorAxis = sensorAxis + '/';
        }
        if (sensorAxis.endsWith("/")) {
            String begin = sensorAxis.substring(0, sensorAxis.indexOf('/')).toLowerCase(Locale.getDefault());
            for (char c : "xyz".toCharArray()) {
                if (begin.indexOf(c) == -1) {
                    sensorAxis = sensorAxis + c;
                }
            }
        }
        return sensorAxis;
    }

    /**
     * @return a new listener that calls {@link #updateSpinner(Spinner, String)}
     *         on text change
     */
    private TextWatcher updateSpinnerOnTextChanged(final Spinner spinner) {
        return new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                /* Doing nothing */ }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                /* Doing nothing */ }

            @Override
            public void afterTextChanged(Editable s) {
                updateSpinner(spinner, s.toString());
            }
        };
    }

    /**
     * Update the spinner's selected item from the parameter value
     */
    private void updateSpinner(Spinner spinner, String value) {
        int index = axes.length - 1;// @string/sensorConfig_axisDisabled
        if (value != null && !value.isEmpty()) {
            index = axes.length - 2;// @string/sensorConfig_axisCustom
            // Searching for predefined values on spinner
            for (int i = 0; i < axes.length; i++) {
                if (value.equalsIgnoreCase(axes[i])) {
                    index = i;// Found
                }
            }
            if (index == -1) {// Dead code?
                index = axes.length - 2;
            }
        }
        spinner.setSelection(index);
    }

    /**
     * @return a new listener that updates the editTextToUpdate's value from the
     *         selected item
     */
    private OnItemSelectedListener modifyEditTextOnItemSelected(final EditText editTextToUpdate) {
        return new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Not updating value if @string/sensorConfig_axisCustom is
                // selected
                if (position != axes.length - 2) {
                    editTextToUpdate.setText(axes[position]);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                /* Doing nothing */ }
        };
    }

    /**
     * @return the filter that requires the input text to match the pattern
     *         [XYZ]+/[XYZ], ignoring case, denying the same character to appear
     *         twice, and allowing the input text to be incomplete
     */
    private static InputFilter[] newAxisEditTextFilters() {
        InputFilter filter = new InputFilter() {

            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                StringBuilder modified = new StringBuilder();
                for (int i = start; i < end; i++) {
                    char c = source.charAt(i);
                    // Allowing these characters only
                    if ("XxYyZz/".indexOf(c) != -1) {
                        // Allowing each character only once
                        String writtenCharsUnordered = dest.toString().substring(dend) + dest.subSequence(0, dstart)
                                + modified;
                        if (writtenCharsUnordered.toLowerCase(Locale.getDefault()).indexOf(Character.toLowerCase(c)) == -1) {
                            // Disallowing '/' in the beginning
                            if (c != '/' || modified.length() > 0 || dstart > 0) {
                                // '/' is required and should not be in the end
                                // => depending on beginning and end, '/' may be
                                // required now
                                if (c == '/' || writtenCharsUnordered.indexOf("/") != -1
                                        || (dstart + modified.length() <= 1
                                                && dstart + modified.length() + dest.length() - dend <= 2)) {
                                    modified.append(c);
                                }
                            }
                        }
                    }
                }
                return modified;
            }
        };
        return new InputFilter[] { filter };
    }

    /**
     * Adds a listener to the button, to prompt for the sensitivity using a
     * SeekBar
     */
    private void openSeekBarOnClick(final Button sensitivityButton) {
        final CharSequence title = mContext.getText(R.string.menuItem_sensitivity);
        sensitivityButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Prompt.promptInteger(mContext, title, "%1$d %%", getSensitivity(sensitivityButton), MIN_SENSITIVITY,
                        MAX_SENSITIVITY, new PromptIntegerListener() {
                    @Override
                    public void onDialogClosed(Integer value, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            sensitivityButton.setText(String.valueOf(value) + "%");
                        }
                    }
                });
            }
        });
    }

    /**
     * Removes the '%' from the end of the String and returns the int value
     * 
     * @param sensitivityButton
     *            the button that contains the String to read
     */
    int getSensitivity(Button sensitivityButton) {
        try {
            CharSequence text = sensitivityButton.getText();
            if (text.charAt(text.length() - 1) == '%') {
                text = text.subSequence(0, text.length() - 1);
            }
            return Integer.valueOf(String.valueOf(text));
        } catch (NumberFormatException ex) {
            return 100;
        }
    }

    /** Create and show this popup dialog */
    void show() {
        Builder builder = new Builder(mContext);
        builder.setTitle(mContext.getString(R.string.menuItem_sensorConfiguration));
        builder.setView(view);
        builder.setNegativeButton(mContext.getString(android.R.string.cancel), this);
        builder.setPositiveButton(mContext.getString(android.R.string.ok), this);
        builder.setCancelable(true);
        builder.create().show();
    }
}
