package ssh2.matss.ph.dialog;


import android.app.Dialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import ssh2.matss.ph.preferences.SettingsPreference;

public class SummaryEditTextDialog extends DialogFragment {
    private static final String ARG_KEY = "key";
    private static final String ARG_DEFAULT_VALUE = "default_value";

    public static SummaryEditTextDialog newInstance(String key, String defaultValue) {
        SummaryEditTextDialog dialog = new SummaryEditTextDialog();
        Bundle args = new Bundle();
        args.putString(ARG_KEY, key);
        args.putString(ARG_DEFAULT_VALUE, defaultValue);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String key = getArguments().getString(ARG_KEY);
        String defaultValue = getArguments().getString(ARG_DEFAULT_VALUE);

        EditText input = new EditText(getActivity());
        input.setText(defaultValue);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        return new MaterialAlertDialogBuilder(getActivity())
                .setTitle("Edit Value")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String value = input.getText().toString();
                    // Update the preference here (you may need to call a method in SettingsPreference)
                    ((SettingsPreference) getParentFragment()).onDialogValueSet(key, value);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .create();
    }
}
