package ssh2.matss.ph.preferences;


import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import java.util.Locale;

import ssh2.matss.library.AppConstants;
import ssh2.matss.ph.MainActivity;
import ssh2.matss.ph.R;
import ssh2.matss.sshtunnel.logger.ConnectionStatus;
import ssh2.matss.sshtunnel.logger.VpnStatus;

public class SettingsPreference extends PreferenceFragmentCompat
        implements VpnStatus.StateListener,
        Preference.OnPreferenceChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences mPref;
    private Handler mHandler;

    private EditTextPreference udpResolverPreference;
    private EditTextPreference primaryDNS;
    private EditTextPreference secondaryDNS;
    private ListPreference selectLang;
    private SwitchPreferenceCompat udpForwardPreference;
    private SwitchPreferenceCompat dnsForwardPreference;

    private final String[] settings_disabled_keys = {
            AppConstants.DNS_FORWARD,
            AppConstants.DNS_PRIMARY,
            AppConstants.DNS_SECONDARY,
            AppConstants.UDP_FORWARD,
            AppConstants.UDP_RESOLVER,
            AppConstants.SSH_PINGER,
            AppConstants.WAKELOCK,
            AppConstants.DATA_COMPRESS,
            AppConstants.APP_LANG
    };


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        // Load the Preferences from the XML file
        setPreferencesFromResource(R.xml.app_preferences, rootKey);

        mPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        udpForwardPreference = findPreference(AppConstants.UDP_FORWARD);
        dnsForwardPreference = findPreference(AppConstants.DNS_FORWARD);

        udpResolverPreference = findPreference(AppConstants.UDP_RESOLVER);
        primaryDNS = findPreference(AppConstants.DNS_PRIMARY);
        secondaryDNS = findPreference(AppConstants.DNS_SECONDARY);
        selectLang = findPreference(AppConstants.APP_LANG);


        udpForwardPreference.setOnPreferenceChangeListener(this);
        dnsForwardPreference.setOnPreferenceChangeListener(this);
        selectLang.setOnPreferenceChangeListener(this);

        PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(this);
        // update view
        setRunningTunnel(VpnStatus.isVPNActive());
    }

    @Override
    public void onResume()
    {
        super.onResume();
        VpnStatus.addStateListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        VpnStatus.removeStateListener(this);
    }

    @Override
    public void onDestroy() {
        VpnStatus.removeStateListener(this);
        PreferenceManager.getDefaultSharedPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    private void onChangeUseVpn(boolean use_vpn){

        for (String key : settings_disabled_keys){
            getPreferenceManager().findPreference(key).setEnabled(use_vpn);
        }

        use_vpn = true;
        if (use_vpn) {
            boolean isUdpForward = mPref.getBoolean(AppConstants.UDP_FORWARD, false);
            boolean isDnsForward = mPref.getBoolean(AppConstants.DNS_FORWARD, false);

            udpResolverPreference.setEnabled(isUdpForward);
            primaryDNS.setEnabled(isDnsForward);
            secondaryDNS.setEnabled(isDnsForward);
            //findPreference.setEnabled(isDnsForward);
        }
        else {
            String[] list = {
                    AppConstants.UDP_FORWARD,
                    AppConstants.UDP_RESOLVER,
                    AppConstants.DNS_FORWARD
            };
            for (String key : list) {
                getPreferenceManager().findPreference(key)
                        .setEnabled(false);
            }
        }
    }

    private void setRunningTunnel(boolean isRunning) {
        if (isRunning) {
            for (String key : settings_disabled_keys){
                getPreferenceManager().findPreference(key).setEnabled(false);
            }
        } else {
            onChangeUseVpn(true);
        }
    }

    /**
     * Preference.OnPreferenceChangeListener
     */

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue)
    {
        switch (pref.getKey()) {
            case AppConstants.UDP_FORWARD:
                boolean isUdpForward = (boolean) newValue;
                udpResolverPreference.setEnabled(isUdpForward);
                break;

            case AppConstants.DNS_FORWARD:
                boolean isDnsForward = (boolean) newValue;
                primaryDNS.setEnabled(isDnsForward);
                secondaryDNS.setEnabled(isDnsForward);
                break;
            case AppConstants.APP_LANG:
                final String lang = (String) newValue;
                setLocale(lang);
                break;
        }
        return true;
    }

    @Override
    public void updateState(String state, String logMessage, int localizedResId, ConnectionStatus level, Intent intent)
    {
        mHandler.post(() -> setRunningTunnel(VpnStatus.isVPNActive()));
    }

    @Override
    public void setConnectedVPN(String uuid) {

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

    }

    public void setLocale(String lang) {
        Locale myLocale = new Locale(lang);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
        Intent refresh = new Intent(requireActivity(), MainActivity.class);
        requireActivity().finish();
        startActivity(refresh);
    }

    public void onDialogValueSet(String key, String value) {
        SharedPreferences.Editor editor = mPref.edit();
        editor.putString(key, value);
        editor.apply();

        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setSummary(value);
        }
    }


}
