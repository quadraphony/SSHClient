package ssh2.matss.ph.fragments;

import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import ssh2.matss.ph.ParentActivity;
import ssh2.matss.ph.dialog.CustomServer;
import ssh2.matss.ph.R;

public class SettingsFragment extends PreferenceFragmentCompat
        implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.app_preferences, rootKey);

        // Set up the identification preference
        Preference identificationPref = findPreference("identification_preference");
        if (identificationPref != null) {
            identificationPref.setOnPreferenceClickListener(preference -> {
                // Call identity() method from ParentActivity
                ((ParentActivity) requireActivity()).identity();
                return true;
            });
        }

        // Set up the custom server switch
        SwitchPreferenceCompat useCustomServerPref = findPreference("keyUseCustomServer");
        Preference configureCustomServerPref = findPreference("keyConfigureCustomServer");

        if (useCustomServerPref != null) {
            useCustomServerPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isChecked = (Boolean) newValue;
                if (configureCustomServerPref != null) {
                    configureCustomServerPref.setEnabled(isChecked); // Enable or disable the configuration preference
                }
                return true;
            });
        }

        if (configureCustomServerPref != null) {
            configureCustomServerPref.setOnPreferenceClickListener(preference -> {
                showCustomServerDialog();
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle("Settings");
    }

    private void showCustomServerDialog() {
        DialogFragment custom = CustomServer.newInstance("Custom Server");
        custom.show(getParentFragmentManager(), "fragment_edit_name");
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle bundle = pref.getExtras();
        final Fragment fragment = Fragment.instantiate(getContext(), pref.getFragment(), bundle);

        fragment.setTargetFragment(caller, 0);

        // Replace the existing Fragment with the new Fragment
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_configLinearLayout, fragment)
                .addToBackStack(null)
                .commit();

        return true;
    }
}
