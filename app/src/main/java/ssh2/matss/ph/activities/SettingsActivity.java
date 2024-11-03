package ssh2.matss.ph.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.google.android.material.appbar.MaterialToolbar;
import ssh2.matss.ph.R;
import ssh2.matss.ph.databinding.ActivitySettingsBinding;
import ssh2.matss.ph.preferences.SettingsPreference;


public class SettingsActivity extends AppCompatActivity
        implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback
{

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        PreferenceFragmentCompat preference = new SettingsPreference();
       // Intent intent = getIntent();

        // add preference settings
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_configLinearLayout, preference)
                .commit();


        // toolbar
        MaterialToolbar mToolbar = binding.toolbarMain.toolbarMain;
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null)
        {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle bundle = pref.getExtras();
        final Fragment fragment = Fragment.instantiate(this, pref.getFragment(), bundle);

        fragment.setTargetFragment(caller, 0);

        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_configLinearLayout, fragment)
                .addToBackStack(null)
                .commit();

        return true;
    }

    @Override
    public boolean onSupportNavigateUp()
    {
        onBackPressed();
        return true;
    }

}



