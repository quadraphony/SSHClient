package ssh2.matss.ph.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;

import ssh2.matss.library.AppConstants;
import ssh2.matss.library.utils.MTSHelper;
import ssh2.matss.ph.R;
import ssh2.matss.ph.activities.ExportActivity;
import ssh2.matss.ph.adapter.OharaAdapter;
import ssh2.matss.ph.cipher.AESCrypt;
import ssh2.matss.ph.databinding.DialogInputPasswordBinding;
import ssh2.matss.ph.databinding.FragmentHomeBinding;
import ssh2.matss.ph.config.Profiling;
import ssh2.matss.ph.viewmodel.HomeViewModel;
import ssh2.matss.ph.preferences.PrefsUtil;
import ssh2.matss.ph.services.ConnectionTimer;
import ssh2.matss.sshtunnel.LaunchVpn;
import ssh2.matss.sshtunnel.logger.VpnStatus;
import ssh2.matss.sshtunnel.tunnel.TunnelManagerHelper;
import ssh2.matss.sshtunnel.tunnel.TunnelUtils;

public class HomeFragment extends Fragment {

    private final String TAG = "HomeFragment:MTS";

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private Profiling vpnProfile;

    private final ArrayList<HashMap<String, String>> listServer = new ArrayList<>();
    private final ArrayList<HashMap<String, String>> listNetwork = new ArrayList<>();

    private OharaAdapter serverAdapter, networkAdapter;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public CountDownTimer cdt;
    private AdView mAdView;
    private RewardedAd rewardedAd;

    private final long REWARD_TIMER = 12;
    private boolean gameOver;
    private boolean gamePaused;
    private long timeRemaining;
    private boolean isLoading;

    private TextView tvAuthor, tvTimer, tvBytesIn, tvBytesOut;
    private Button  btnAddTime, btnClaim;
    private Spinner spServer, spNetwork;
    private TextInputLayout inputPayload;
    private TextInputEditText et_payload;
    private LinearLayout layBytes;
    private MaterialSwitch switchCustom;
    private FloatingActionButton btnConnection;
    private TextView tvButtonLabel;

    private final ActivityResultLauncher<Intent> startActivityIntent = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    StringBuilder sb = new StringBuilder();
                    try {
                        BufferedReader bufferedReader = new BufferedReader(
                                new InputStreamReader(
                                        requireActivity().getContentResolver()
                                                .openInputStream(data != null ? data.getData() : null)));
                        while (true)
                        {
                            String readLine = bufferedReader.readLine();
                            if (readLine == null) break;
                            sb.append(readLine);
                        }
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    readImportedFile(sb.toString());
                }
            });

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        initInstances();
        initBindingViews();
        initBindingMethod();
        instanceObjects();
        initAdView();
        loadRewardedAd();
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initObservers();
        loadServer();
        loadNetwork(sharedPreferences.getBoolean(AppConstants.USE_CUSTOM, false));
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle("Home");
    }

    private void initInstances(){
        viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        vpnProfile = new Profiling(requireActivity());
        sharedPreferences = new PrefsUtil(requireActivity()).sharedPreferences();
        editor = sharedPreferences.edit();
    }

    private void instanceObjects(){
        //vpnProfile.extractServers();
    }

    private void initBindingViews(){
        spServer = binding.spServer;
        spNetwork = binding.spNetwork;
        btnConnection = binding.btnConnection;
        tvAuthor = binding.tvAuthor;
        tvTimer = binding.tvUserTimer;
        switchCustom = binding.switchCustom;
        inputPayload = binding.inputPayload;
        et_payload = binding.etPayload;
        tvBytesIn = binding.tvBytesIn;
        tvBytesOut = binding.tvBytesOut;
        layBytes = binding.layBytes;
        btnAddTime = binding.btnAddTimer;
        btnClaim = binding.btnClaim;
        tvButtonLabel = binding.tvButtonLabel;
    }

    private void initBindingMethod(){
        inputPayload.setVisibility(View.GONE);
        tvAuthor.setVisibility(View.GONE);
        serverAdapter = new OharaAdapter(requireActivity(), listServer, true);
        networkAdapter = new OharaAdapter(requireActivity(), listNetwork, false);
        spServer.setAdapter(serverAdapter);
        spNetwork.setAdapter(networkAdapter);
        btnConnection.setOnClickListener(view -> {
            if(switchCustom.isChecked() &&
                    spNetwork.getSelectedItemPosition() == 5 &&
                    sharedPreferences.getString(AppConstants.CONFIG_STORED, "").isEmpty()
            ) {
                showToast("No config is imported!", Toast.LENGTH_LONG);
                return;
            }

            if(sharedPreferences.getLong(AppConstants.USER_TIMER,0) == 0){
                showToast("Timer length is ", Toast.LENGTH_SHORT);
                return;
            }

            if(VpnStatus.isVPNActive()){
                viewModel.setEnableViews(true);
                TunnelManagerHelper.stopSocksHttp(requireActivity());
            } else {
                viewModel.setEnableViews(false);
                startSSH(vpnProfile.generateConfig(
                        switchCustom.isChecked(),
                        spServer.getSelectedItemPosition(),
                        sharedPreferences.getInt(switchCustom.isChecked() ?
                                AppConstants.SELECTED_CUSTOM : AppConstants.SELECTED_TWEAKS, 0)));
            }
        });

        btnAddTime.setOnClickListener(view -> startGameforReward());
        btnClaim.setOnClickListener(view -> showRewardedVideo());
        switchCustom.setOnCheckedChangeListener((compoundButton, b) -> {
            editor.putBoolean(AppConstants.USE_CUSTOM, b).apply();
            loadNetwork(b);
            int selected = sharedPreferences.getInt((b ? AppConstants.SELECTED_CUSTOM : AppConstants.SELECTED_TWEAKS), 0);
            viewModel.setAuthorView(new Pair<>(
                    sharedPreferences.getString(AppConstants.AUTHOR_MSG, ""), selected));
            viewModel.enableWidgetCustom(new Pair<>(switchCustom.isChecked(), selected));
            spNetwork.setSelection(selected);
        });

        spServer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editor.putInt(AppConstants.SELECTED_SERVER, i).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        spNetwork.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editor.putInt((switchCustom.isChecked() ? AppConstants.SELECTED_CUSTOM : AppConstants.SELECTED_TWEAKS), i).apply();
                viewModel.enableWidgetCustom(new Pair<>(switchCustom.isChecked(), i));
                viewModel.setAuthorView(new Pair<>(
                        sharedPreferences.getString(AppConstants.AUTHOR_MSG, ""), i));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        et_payload.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                int selected = spNetwork.getSelectedItemPosition();
                if(selected == 1 || selected == 2){
                    editor.putString(AppConstants.CUSTOM_PAYLOAD, editable.toString());
                } else if(selected == 3){
                    editor.putString(AppConstants.CUSTOM_SNI, editable.toString());
                } else if(selected == 4){
                    editor.putString(AppConstants.CUSTOM_DNS, editable.toString());
                }
                editor.apply();
            }
        });

        /*
         * update
         */

        switchCustom.setChecked(sharedPreferences.getBoolean(AppConstants.USE_CUSTOM, false));
        spServer.setSelection(sharedPreferences.getInt(AppConstants.SELECTED_SERVER, 0));
        spNetwork.setSelection(
                sharedPreferences.getInt(
                        (sharedPreferences.getBoolean(AppConstants.USE_CUSTOM, false)
                                ? AppConstants.SELECTED_CUSTOM : AppConstants.SELECTED_TWEAKS), 0));
    }

    private void initObservers() {
        viewModel.btnConnectLabel.observe(getViewLifecycleOwner(), label -> {
            // Update the text of the TextView
            tvButtonLabel.setText(label);

            // Optionally, you can also update the icon of the FAB
            btnConnection.setImageResource(R.drawable.play_24px);
        });

      /*  viewModel.btnTimerLabel.observe(getViewLifecycleOwner(), pair -> {
            btnAddTime.setText(pair.first);
            btnAddTime.setVisibility(pair.second ? View.VISIBLE : View.GONE);
        });
        viewModel.btnClaimLabel.observe(getViewLifecycleOwner(), pair -> {
            btnClaim.setText(pair.first);
            btnClaim.setVisibility(pair.second ? View.VISIBLE : View.GONE);
        });

       */
        viewModel.bytesInOut.observe(getViewLifecycleOwner(), pair ->{
            tvBytesIn.setText(pair.first);
            tvBytesOut.setText(pair.second);
        });
        viewModel.getAuthorView.observe(getViewLifecycleOwner(), pair->{
            tvAuthor.setVisibility(pair.second == 5 &&
                    !sharedPreferences.getString(AppConstants.CONFIG_STORED, "").isEmpty() &&
                    switchCustom.isChecked() &&
                    !pair.first.isEmpty() ? View.VISIBLE : View.GONE);
            tvAuthor.setText(pair.first);
        });
        viewModel.widgetCustom.observe(getViewLifecycleOwner(), is -> {
            inputPayload.setVisibility(is.first && (is.second > 0 && 5 > is.second) ? View.VISIBLE : View.GONE);
           // tvAuthor.setVisibility(is.first && is.second == 5 ? View.VISIBLE : View.GONE);
            if(is.first && (is.second == 1 || is.second == 2)){
                inputPayload.setHint(getString(R.string.hint_payload));
                et_payload.setText(sharedPreferences.getString(AppConstants.CUSTOM_PAYLOAD, ""));
            } else if(is.first && is.second == 3) {
                inputPayload.setHint(getString(R.string.hint_sni));
                et_payload.setText(sharedPreferences.getString(AppConstants.CUSTOM_SNI, ""));
            } else if(is.first && is.second == 4) {
                inputPayload.setHint(getString(R.string.hint_dns));
                et_payload.setText(sharedPreferences.getString(AppConstants.CUSTOM_DNS, ""));
            }
        });
        viewModel.enableViews.observe(getViewLifecycleOwner(), is -> {
            spServer.setEnabled(is);
            spNetwork.setEnabled(is);
            switchCustom.setEnabled(is);
            et_payload.setEnabled(is);
            layBytes.setVisibility(is ? View.GONE : View.VISIBLE);
        });
        viewModel.getUserTimer.observe(getViewLifecycleOwner(), label -> tvTimer.setText(label));
    }

    private void initAdView(){
        RelativeLayout adContainer = binding.rlAdview;
        mAdView = new AdView(requireActivity());
        mAdView.setAdSize(AdSize.BANNER);
        mAdView.setAdUnitId(AppConstants.BANNER_ID);
        adContainer.addView(mAdView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    private void loadServer(){
        if(listServer.size() > 0){
            listServer.clear();
            serverAdapter.notifyDataSetChanged();
        }

        JSONArray ja = vpnProfile.server_ja();
        for(int i = 0; i < ja.length(); i++){
            try {
                JSONObject obj = ja.getJSONObject(i);
                String name = obj.getString("name");
                String info = obj.getString("info");
                String flag = obj.getString("flag");

                HashMap<String, String> server = new HashMap<>();
                server.put("mName", name);
                server.put("mInfo", info);
                server.put("mIcon", flag);

                listServer.add(server);
                serverAdapter.notifyDataSetChanged();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadNetwork(boolean is){
        if(listNetwork.size() > 0){
            listNetwork.clear();
            networkAdapter.notifyDataSetChanged();
        }
        JSONArray ja = is ? vpnProfile.extractCustoms(): vpnProfile.network_ja();
        for(int i = 0; i < ja.length(); i++){
            try {
                JSONObject obj = ja.getJSONObject(i);
                String name = obj.getString("name");
                String info = obj.getString("info");
                String flag = obj.getString("icon");

                HashMap<String, String> tweaks = new HashMap<>();
                tweaks.put("mName", name);
                tweaks.put("mInfo", info);
                tweaks.put("mIcon", flag);

                listNetwork.add(tweaks);
                networkAdapter.notifyDataSetChanged();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_home, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Menu Itens
        int itemId = item.getItemId();
        if (itemId == R.id.importFile) {
            Intent intent = new Intent("android.intent.action.GET_CONTENT");
            intent.setType("*/*");
            startActivityIntent.launch(intent);
        } else if (itemId == R.id.exportFile) {
           if(switchCustom.isChecked() && (spNetwork.getSelectedItemPosition() != 0 && spNetwork.getSelectedItemPosition() != 5)) {
               startActivity(new Intent(requireActivity(), ExportActivity.class));
           } else if(VpnStatus.isVPNActive()){
               showToast("Vpn is running!", Toast.LENGTH_SHORT);
           } else {
               showToast((switchCustom.isChecked() ? "This profile can't be export!" : "Switch to custom!"), Toast.LENGTH_SHORT);
           }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void showToast(String msg, int length){
        Toast.makeText(getActivity(), msg, length).show();
    }

    private void startSSH(Bundle config){
        //TunnelManagerHelper.startSocksHttp(requireActivity(), config);
        Intent intent = new Intent(requireActivity(), LaunchVpn.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.putExtras(config);
        startActivity(intent);
    }


    private void readImportedFile(String file){
        try {
            MTSHelper helper = new MTSHelper(requireActivity());
            JSONObject obj = new JSONObject(AESCrypt.decrypt(AppConstants.CIPHER_KEY, file));
            JSONObject password = obj.getJSONObject("password");
            JSONObject hwid = obj.getJSONObject("hwid");
            if(hwid.getBoolean("required") &&
                    !sharedPreferences.getString(AppConstants.HARDWARE_ID, "").equals(hwid.getString("client"))) {
                showToast("Hardware ID does not match this config", Toast.LENGTH_SHORT);
            } else if(password.getBoolean("required")){
                is_password(obj);
            } else if(obj.getInt("validity") > 0 && helper.isValidityExpired(obj.getInt("validity"))) {
                showToast(getString(R.string.expired), Toast.LENGTH_SHORT);
            } else if(obj.getBoolean("blockroot") && helper.isRooted()){
                showToast("Setting blocked for devices with root access", Toast.LENGTH_SHORT);
            } else {
                parseImportedFile(obj);
            }
        } catch (JSONException | GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    private void is_password(JSONObject jo)
    {
        DialogInputPasswordBinding binding = DialogInputPasswordBinding.
                inflate(LayoutInflater.from(requireActivity()));
        MaterialAlertDialogBuilder ab = new MaterialAlertDialogBuilder(requireActivity());
        ab.setView(binding.getRoot());
        ab.setCancelable(false);
        ab.setTitle("Input Password");

        EditText edTxtPutPass = binding.etPassword;
        ab.setPositiveButton(getString(R.string.save), (dialog, id) -> {
            String edTxtPass = edTxtPutPass.getText().toString();
            try {
                JSONObject password = jo.getJSONObject("password");
                if(!password.getString("pin").equals(edTxtPass))
                {
                    Snackbar snackbar = Snackbar
                            .make(requireActivity().findViewById(android.R.id.content),
                                    getString(R.string.wrong_password),
                                    Snackbar.LENGTH_LONG)
                            .setAction("RE-INPUT", view -> is_password(jo));
                    snackbar.show();
                } else {
                    parseImportedFile(jo);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            dialog.cancel();
        });
        ab.setNeutralButton(getString(R.string.cancel), (dialog, id) -> dialog.cancel());
        MaterialAlertDialogBuilder alertDialog = ab;
        alertDialog.show();
    }
    
    private void parseImportedFile(JSONObject obj)
    {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        try{
          
            JSONObject tweaks = obj.getJSONObject("conf");
            String msg = obj.getString("message");
            editor.putString(AppConstants.AUTHOR_MSG, msg);
            editor.apply();

            JSONObject store = new JSONObject();
            store.put("connection", tweaks.getInt("connection"));
            store.put("payload", tweaks.getString("payload"));
            store.put("sni", tweaks.getString("sni"));
            store.put("dns",tweaks.getString("dns"));
            
            editor.putString(AppConstants.CONFIG_STORED, store.toString()).apply();
            showToast("Import Successfully", Toast.LENGTH_SHORT);
            switchCustom.setChecked(true);
            spNetwork.setSelection(5);
            viewModel.setAuthorView(new Pair<>(msg, 5));
        } catch (JSONException e){
            Log.d("JSONException", e.getMessage());
        }
    }


    /*
        Rewards Ads
     */

    // Create the game timer, which counts down to the end of the level
    // and shows the "retry" button.
    private void createTimer(long time) {
        // final TextView textView = findViewById(R.id.timer);
        if (cdt != null) {
            cdt.cancel();
        }
        cdt = new CountDownTimer(time * 1000, 1000) {
                    @Override
                    public void onTick(long millisUnitFinished) {
                        timeRemaining = (millisUnitFinished / 1000);
                        // textView.setText("seconds remaining: " + timeRemaining);
                        btnAddTime.setText(String.format("%s %s", timeRemaining, "secs"));
                        btnAddTime.setVisibility(View.VISIBLE);
                        btnAddTime.setEnabled(false);
                        btnClaim.setVisibility(View.GONE);
                    }

                    @Override
                    public void onFinish() {
                        if (rewardedAd != null) {
                            btnClaim.setVisibility(View.VISIBLE);
                            btnClaim.setText(getString(R.string.label_claim));
                            btnAddTime.setVisibility(View.GONE);
                        } else {
                            btnClaim.setVisibility(View.GONE);
                            btnAddTime.setVisibility(View.VISIBLE);
                            btnAddTime.setEnabled(true);
                            btnAddTime.setText(getString(R.string.label_retry));
                        }
                        // textView.setText("You Lose!");
                        //addCoins(GAME_OVER_REWARD);
                        //retryButton.setVisibility(View.VISIBLE);
                        gameOver = true;
                    }
                };
        cdt.start();
    }

    private void startGameforReward() {
        // Hide the retry button, load the ad, and start the timer.
        if (rewardedAd != null && !isLoading) {
            loadRewardedAd();
        }
        createTimer(REWARD_TIMER);
        gamePaused = false;
        gameOver = false;
    }

    public void loadRewardedAd() {
        if (rewardedAd == null) {
            isLoading = true;
            AdRequest adRequest = new AdRequest.Builder().build();
            RewardedAd.load(
                    requireActivity(),
                    AppConstants.REWARD_ID,
                    adRequest,
                    new RewardedAdLoadCallback() {
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            // Handle the error.
                            // Log.d(TAG, loadAdError.getMessage());
                            rewardedAd = null;
                            HomeFragment.this.isLoading = false;
                            //Toast.makeText(MainActivity.this, "onAdFailedToLoad", Toast.LENGTH_SHORT).show();

                            loadRewardedAd();
                            startGameforReward();
                        }

                        @Override
                        public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                            HomeFragment.this.rewardedAd = rewardedAd;
                            //Log.d(TAG, "onAdLoaded");
                            HomeFragment.this.isLoading = false;
                            //Toast.makeText(MainActivity.this, "onAdLoaded", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    public void showRewardedVideo() {

        if (rewardedAd == null) {
            //Log.d("TAG", "The rewarded ad wasn't ready yet.");
            return;
        }
        //btn_claim.setVisibility(View.INVISIBLE);

        rewardedAd.setFullScreenContentCallback(
                new FullScreenContentCallback() {
                    @Override
                    public void onAdShowedFullScreenContent() {
                        //onAdShowedFullScreenContent"
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        rewardedAd = null;
                        // "onAdFailedToShowFullScreenContent"
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        // Called when ad is dismissed.
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        rewardedAd = null;

                        btnClaim.setVisibility(View.GONE);
                        btnAddTime.setVisibility(View.VISIBLE);
                        btnAddTime.setEnabled(true);
                        btnAddTime.setText(getString(R.string.label_add_time));
                        // Preload the next rewarded ad.
                        loadRewardedAd();
                        startGameforReward();
                    }
                });
        Activity activityContext = requireActivity();
        rewardedAd.show(
                activityContext,
                rewardItem -> {
                    onUserEarned();
                    showToast("Congratulations!, you earned a reward", Toast.LENGTH_SHORT);
                });
    }

    public void onUserEarned(){
        if(TunnelUtils.isActiveVpn(requireActivity())){
            startTimer(false);
            addTime();
            startTimer(true);
        } else {
            addTime();
        }

    }

    public void addTime()
    {
        long add = AppConstants.REWARDS;
        long timer_tick = sharedPreferences.getLong(AppConstants.TIMER_TICK, 0);
        long timer = sharedPreferences.getLong(AppConstants.USER_TIMER, timer_tick) + add;
        editor.putLong(AppConstants.USER_TIMER, timer).apply();
        tvTimer.setText(new MTSHelper(requireActivity()).secondsToString(timer));
    }

    private void startTimer(boolean is)
    {
        requireActivity().runOnUiThread(() -> {
            Intent intent = new Intent(requireActivity(), ConnectionTimer.class);
            if(is)
                requireActivity().startService(intent);
            else
                requireActivity().stopService(intent);

        });
    }
    
}