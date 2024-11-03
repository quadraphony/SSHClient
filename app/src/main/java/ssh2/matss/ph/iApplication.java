package ssh2.matss.ph;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;

import java.util.Arrays;

import ssh2.matss.library.AppConstants;
import ssh2.matss.ph.helper.AppOpenHelper;
import ssh2.matss.sshtunnel.NotificationChannels;
import ssh2.matss.sshtunnel.utils.LocaleHelper;

public class iApplication extends Application {

    @Override
    public void onCreate() {
       // LocaleHelper.setDesiredLocale(this);
        super.onCreate();
        NotificationChannels.init(this);
        setTheme();
        mobileAds("4319F6F677171800441B141187193811");
       // new AppOpenHelper(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
      //  LocaleHelper.updateResources(base)
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //LocaleHelper.onConfigurationChange(this);
    }

    private void mobileAds(String test){
        MobileAds.initialize(this, initializationStatus -> {});
        MobileAds.setRequestConfiguration(
                new RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList(test))
                        .build());
    }

    private void setTheme() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        AppCompatDelegate.setDefaultNightMode(
                (prefs.getBoolean(AppConstants.APP_THEME, false) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO));
    }

}