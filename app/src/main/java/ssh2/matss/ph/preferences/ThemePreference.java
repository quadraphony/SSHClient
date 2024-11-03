package ssh2.matss.ph.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.DropDownPreference;
import androidx.preference.PreferenceManager;
import ssh2.matss.library.AppConstants;

public class ThemePreference extends DropDownPreference {

    public ThemePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Set display names and values for Light and Dark modes
        setEntries(new CharSequence[]{"Light Mode", "Dark Mode"});
        setEntryValues(new CharSequence[]{"light", "dark"});

        // Set default value if no preference exists
        setDefaultValue("light");

        // Set the summary based on current theme setting
        setSummaryProvider(preference -> {
            String currentValue = getValue();
            return "dark".equals(currentValue) ? "Dark Mode" : "Light Mode";
        });

        setOnPreferenceChangeListener((preference, newValue) -> {
            boolean isDarkMode = "dark".equals(newValue);

            // Update shared preferences
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            sharedPrefs.edit().putBoolean(AppConstants.APP_THEME, isDarkMode).apply();

            // Apply dark mode or light mode
            AppCompatDelegate.setDefaultNightMode(
                    isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );

            return true;
        });
    }
}
