package ssh2.matss.ph;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.navigation.NavigationView;


import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;

import ssh2.matss.library.AppConstants;
import ssh2.matss.library.utils.MTSHelper;
import ssh2.matss.ph.activities.AboutActivity;
import ssh2.matss.ph.activities.SettingsActivity;
import ssh2.matss.ph.adapter.ViewPagerAdapter;
import ssh2.matss.ph.databinding.ActivityMainBinding;
import ssh2.matss.ph.dialog.CustomServer;
import ssh2.matss.ph.helper.PayloadGenerator;
import ssh2.matss.ph.preferences.SummaryEditTextPreference;
import ssh2.matss.ph.viewmodel.HomeViewModel;
import ssh2.matss.ph.preferences.PrefsUtil;
import ssh2.matss.ph.request.RequestUpdate;
import ssh2.matss.ph.services.ConnectionTimer;
import ssh2.matss.sshtunnel.StatisticGraphData;
import ssh2.matss.sshtunnel.logger.VpnStatus;
import ssh2.matss.sshtunnel.logger.ConnectionStatus;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends ParentActivity implements VpnStatus.StateListener {

    //private final String TAG = "MainActivity:MTS";
    private static final String UPDATE_VIEWS = "MainUpdate";
    private static final String STOP_VPN = "stopVPN";
    private final int PERMISSION_REQUEST_CODE = 1;
    private ActivityMainBinding binding;
    private HomeViewModel viewModel;
    private SharedPreferences sp;

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private NavigationView nav;
    private DrawerLayout drawer;
    private MaterialToolbar toolbar;
    private MaterialSwitch customServer;

    public CountDownTimer cdt;
    private final long INTERSTITIAL_TIMER = 3;

    private InterstitialAd interstitialAd;
    public boolean gameIsInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkMode = sharedPrefs.getBoolean(AppConstants.APP_THEME, false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        toolbar = binding.include.includeToolbar.toolbarMain;
        setSupportActionBar(toolbar);

        initInstances();
        initBindingViews();
        initBindingMethods();
        initInterstitial();

        if(dsp().getBoolean(AppConstants.IS_FIRST, true)){
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(AppConstants.HARDWARE_ID, uniqueDevice());
            editor.putString(AppConstants.CUSTOM_PAYLOAD, AppConstants.PAYLOAD_BUGHOST);
            editor.putString(AppConstants.CUSTOM_SNI, AppConstants.SNI_BUGHOST);
            editor.putString(AppConstants.CUSTOM_DNS, AppConstants.DNS_DEFAULT);
            editor.putLong(AppConstants.USER_TIMER, AppConstants.REWARDS_FOR_INSTALL);
            editor.putBoolean(AppConstants.USE_CUSTOM_SERVER, false).apply();
            editor.putBoolean(AppConstants.USE_CUSTOM, false);
            editor.apply();

            SharedPreferences.Editor prefsEditor = dsp().edit();
            prefsEditor.putBoolean(AppConstants.IS_FIRST, false).apply();
            prefsEditor.putBoolean(AppConstants.DNS_FORWARD, true).apply();
            prefsEditor.putString(AppConstants.DNS_PRIMARY, "8.8.8.8").apply();
            prefsEditor.putString(AppConstants.DNS_SECONDARY, "8.8.4.4").apply();
            prefsEditor.putBoolean(AppConstants.UDP_FORWARD, true).apply();
            prefsEditor.putString(AppConstants.UDP_RESOLVER, "127.0.0.1:7300").apply();
            prefsEditor.putString(AppConstants.MAX_THREADS, "8").apply();
            prefsEditor.putBoolean(AppConstants.DATA_COMPRESS, true).apply();
            prefsEditor.putBoolean(AppConstants.WAKELOCK, false).apply();
            prefsEditor.putString(AppConstants.SSH_PINGER, "3").apply();
        }

        if(dsp().getBoolean(AppConstants.IS_TELE, true)) channel();

        // receive local data
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPDATE_VIEWS);
        filter.addAction(STOP_VPN);

        LocalBroadcastManager.getInstance(this).registerReceiver(mActivityReceiver, filter);
        long connection_timer = sp.getLong(AppConstants.USER_TIMER, 0);
        viewModel.setTimerUser(new MTSHelper(this).secondsToString(connection_timer));
        requestPermissionInfo();
    }

    @Override
    public void onResume(){
        super.onResume();
        VpnStatus.addStateListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(mActivityReceiver, new IntentFilter(ConnectionTimer.COUNTDOWN_BR), Context.RECEIVER_NOT_EXPORTED);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        VpnStatus.removeStateListener(this);
    }

    @Override
    public void onDestroy() {
        VpnStatus.removeStateListener(this);
        //VpnStatus.removeByteCountListener(this);
        super.onDestroy();
    }

    private void initInstances(){
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        sp = new PrefsUtil(this).sharedPreferences();
    }

    private void initBindingViews(){
        nav = binding.navDrawer;
        drawer = binding.drawerLayout;

    }

    private void initBindingMethods() {
        viewPager = findViewById(R.id.viewPager);
        bottomNavigationView = findViewById(R.id.bottom_navigation);


        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);


        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                viewPager.setCurrentItem(0);
            } else if (item.getItemId() == R.id.nav_logs) {
                viewPager.setCurrentItem(1);
            } else if (item.getItemId() == R.id.nav_info) {
                viewPager.setCurrentItem(2);
            } else if (item.getItemId() == R.id.nav_settings) {
                viewPager.setCurrentItem(3);
            } else {
                return false;
            }
            return true;
        });



        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        bottomNavigationView.setSelectedItemId(R.id.nav_home);
                        break;
                    case 1:
                        bottomNavigationView.setSelectedItemId(R.id.nav_logs);
                        break;
                    case 2:
                        bottomNavigationView.setSelectedItemId(R.id.nav_info);
                        break;
                    case 3:
                        bottomNavigationView.setSelectedItemId(R.id.nav_settings);
                        break;
                }
            }
        });


        View header = nav.getHeaderView(0);
        TextView tv_version = header.findViewById(R.id.tv_version);
        tv_version.setText(vb());

        nav.setNavigationItemSelectedListener(menuItem -> {
            drawer.closeDrawers();
            int itemId = menuItem.getItemId();
            if (itemId == R.id.use_custom_server) {
                DialogFragment custom = CustomServer.newInstance("Custom Server");
                custom.show(getSupportFragmentManager(), "fragment_edit_name");
            }
            return true;
        });
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawer,
                toolbar,
                R.string.app_name,
                R.string.app_name);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        MenuItem menuItem = nav.getMenu().findItem(R.id.use_custom_server);
        customServer = menuItem.getActionView().findViewById(R.id.switch_custom_server);
        customServer.setChecked(sp.getBoolean(AppConstants.USE_CUSTOM_SERVER, false));
        customServer.setOnCheckedChangeListener((compoundButton, b) -> {
            sp.edit().putBoolean(AppConstants.USE_CUSTOM_SERVER, b).apply();
        });
        viewModel.setImgTheme(dsp().getBoolean(AppConstants.APP_THEME, false));
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

       if (id == R.id.action_update) {
            new RequestUpdate(this, AppConstants.RESOURCES_URL);
            return true;
       } else if (id == R.id.telegram) {
           channel();
           return true;
       }else if(id == R.id.menu_clear){
           restore_app_config();
       } else if (id == R.id.payload_generator) {
           new PayloadGenerator(this).show();
           return true;
       }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Receive locations Broadcast
     */

    private final BroadcastReceiver mActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null)
                return;

           /* if (action.equals(UPDATE_VIEWS) && getActivity() != null)
            {
                updateViews();
            } else if (action.equals(STOP_VPN) && getActivity() != null) {
                stopSSH();
            }

            */
            updateGUI(intent);
        }
    };

    private void updateGUI(Intent intent) {
        if (intent.getExtras() != null) {
            SharedPreferences.Editor edit = sp.edit();
            long millisUntilFinished = intent.getLongExtra(AppConstants.COUNTDOWN,0) / 1000;
            edit.putLong(AppConstants.USER_TIMER, millisUntilFinished).apply();
            edit.putLong(AppConstants.TIMER_TICK, millisUntilFinished).apply();
            viewModel.setTimerUser(new MTSHelper(this).secondsToString(millisUntilFinished));
        }
    }


    /**
     * permission
     */

    private void requestPermissionInfo() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Information dialog in case the user has already denied permissions at least once
            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
            dialog.setTitle(R.string.title_permission_request);
            dialog.setMessage(R.string.message_permission_request);
            //finish();
            dialog.setOnCancelListener(DialogInterface::dismiss);
            dialog.setPositiveButton(R.string.ok, (dialogInterface, position) -> {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                dialogInterface.dismiss();
            });
            dialog.setNegativeButton(R.string.cancel, (dialog1, position) -> {
                dialog1.dismiss();
                // finish();
            });
            dialog.show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void updateState(String state, String logmessage, int localizedResId, ConnectionStatus level, Intent Intent) {
        switch (state) {
            case AppConstants.STATE_CONNECTED:
                startTimer(true);

                new Timer().schedule(new TimerTask(){
                    @Override
                    public void run(){
                        new Handler(Looper.getMainLooper()).post(() -> updateHeaderCallback(true));
                    }
                }, 0,1000);
                break;

            case AppConstants.STATE_DISCONNECTED:
                startTimer(false);
                updateHeaderCallback(false);
                break;

            default:
        }

        runOnUiThread(() -> {
            String stat;

            switch (state) {
                case AppConstants.STATE_CONNECTED:
                    stat = "STOP";


                    break;
                case AppConstants.STATE_CONNECTING:
                case "AUTH":
                    stat = "CONNECTING";

                    break;

                case AppConstants.STATE_DISCONNECTED:
                case "NOPROCESS":
                    stat = "START";

                    break;
                case AppConstants.STATE_RECONNECTING:
                    stat = "RECONNECTING";

                    break;
                case AppConstants.STATE_STOPPING:
                    stat = "STOPPING";

                    break;
                default:
                    stat = "PLEASE WAIT";

            }



            viewModel.setBtnConnectLabel(stat);
            viewModel.setEnableViews(!VpnStatus.isVPNActive());
        });
    }

    @Override
    public void setConnectedVPN(String uuid) {

    }

    private void updateHeaderCallback(boolean is) {
        if(is){
            StatisticGraphData.DataTransferStats dataTransferStats = StatisticGraphData.getStatisticData().getDataTransferStats();
            String download = dataTransferStats.byteCountToDisplaySize(dataTransferStats.getTotalBytesReceived(), false);
            String upload = dataTransferStats.byteCountToDisplaySize(dataTransferStats.getTotalBytesSent(), false);
            viewModel.setBytesInOut(new Pair<>(upload, download));
        } else {
            runOnUiThread(() -> {
                StatisticGraphData.getStatisticData().getDataTransferStats().stop();
                viewModel.setBytesInOut(new Pair<>("0Kb", "0Kb"));
            });
        }
    }

    private void startTimer(boolean is)
    {
        runOnUiThread(() -> {
            Intent intent = new Intent(this, ConnectionTimer.class);
            if(is)
                startService(intent);
            else
                stopService(intent);

           showInterstitial();
        });
       // Log.i(TAG,is ? "Service Started": "Service Stopped");
    }

    /**
     * Timer for Admob Ads
     * Create timer
     * build by CodeXs
     */

    private void createTimer(final long milliseconds) {
        // Create the game timer, which counts down to the end of the level
        // and shows the "retry" button.
        if (cdt != null) {
            cdt.cancel();
        }
        cdt = new CountDownTimer(milliseconds * 1000, 50) {
            @Override
            public void onTick(long millisUnitFinished) {
                //timeRemaining = millisUnitFinished;
            }

            @Override
            public void onFinish() {
                gameIsInProgress = false;
            }
        };
    }

    /*
     Interstitial Ads
     */

    private void initInterstitial(){
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(
                this,
                AppConstants.INTERSTITIAL_ID,
                adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        MainActivity.this.interstitialAd = interstitialAd;
                        //Toast.makeText(con, "onAdLoaded()", Toast.LENGTH_SHORT).show();
                        interstitialAd.setFullScreenContentCallback(
                                new FullScreenContentCallback() {
                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        // Called when fullscreen content is dismissed.
                                        // Make sure to set your reference to null so you don't
                                        // show it a second time.
                                        MainActivity.this.interstitialAd = null;
                                        Log.d("TAG", "The ad was dismissed.");
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                                        // Called when fullscreen content failed to show.
                                        // Make sure to set your reference to null so you don't
                                        // show it a second time.
                                        MainActivity.this.interstitialAd = null;
                                        Log.d("TAG", "The ad failed to show.");
                                    }

                                    @Override
                                    public void onAdShowedFullScreenContent() {
                                        // Called when fullscreen content is shown.
                                        Log.d("TAG", "The ad was shown.");
                                    }
                                });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        interstitialAd = null;
                    }
                });
    }

    private void showInterstitial() {
        // Show the ad if it's ready. Otherwise toast and restart the game.
        if (interstitialAd != null) {
            interstitialAd.show(this);
        } else {
            startGameforInterstitial();
        }
    }

    private void startGameforInterstitial() {
        // Request a new ad if one isn't already loaded, hide the button, and kick off the timer.
        if (interstitialAd == null) {
            initInterstitial();
        }
        resumeGame(INTERSTITIAL_TIMER);
    }

    private void resumeGame(long milliseconds) {
        // Create a new timer for the correct length and start it.
        gameIsInProgress = true;
        createTimer(milliseconds);
        cdt.start();
    }

    private void restore_app_config() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Restore Settings");
        builder.setMessage("Do you want to restore the default settings?");
        builder.setPositiveButton("YES", (dialogInterface, i) -> {

            LinearLayout layout = new LinearLayout(this);
            layout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            layout.setOrientation(LinearLayout.VERTICAL);


            LinearProgressIndicator progressIndicator = new LinearProgressIndicator(this);
            progressIndicator.setIndeterminate(true);
            progressIndicator.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            layout.addView(progressIndicator); // Add to layout


            MaterialAlertDialogBuilder progressDialogBuilder = new MaterialAlertDialogBuilder(this)
                    .setView(layout)
                    .setMessage("Restoring Data...")
                    .setCancelable(false);

            AlertDialog progressDialog = progressDialogBuilder.create();
            progressDialog.show();


            Handler mHandler = new Handler();
            mHandler.postDelayed(() -> {
                doRestore();
                progressDialog.dismiss();
                showToast("Data is restored!", Toast.LENGTH_SHORT);
            }, 1200);
        });

        builder.setNegativeButton("NO", (dialogInterface, i) -> dialogInterface.cancel());
        MaterialAlertDialogBuilder ad = builder;
        ad.show();
    }

    private void doRestore(){
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(AppConstants.CONFIG_STORED);
        editor.putString(AppConstants.CUSTOM_PAYLOAD, AppConstants.PAYLOAD_BUGHOST);
        editor.putString(AppConstants.CUSTOM_SNI, AppConstants.SNI_BUGHOST);
        editor.putString(AppConstants.CUSTOM_DNS, AppConstants.DNS_DEFAULT);
        editor.apply();
    }

}
