package ssh2.matss.ph.activities;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;


import org.json.JSONObject;

import java.io.FileOutputStream;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import ssh2.matss.library.AppConstants;
import ssh2.matss.ph.cipher.AESCrypt;
import ssh2.matss.ph.preferences.PrefsUtil;
import ssh2.matss.ph.R;
import ssh2.matss.ph.databinding.ActivityExportBinding;

public class ExportActivity extends AppCompatActivity {

    private long validity = 0;
    private String inputString;

    private ActivityExportBinding binding;
    private SharedPreferences sharedPreferences;

    private TextInputEditText fileName, etMessage, etHWID, etPassword;
    private CheckBox cbMessage, cbMobileData, cbRoot, cbPassword, cbHWID, cbValidity;
    private TextView tvValid;
    private Button btnExport;

    private final ActivityResultLauncher<Intent> startActivityIntent = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    try
                    {
                        FileOutputStream fileOutputStream = (FileOutputStream) getContentResolver().openOutputStream(data != null ? data.getData() : null);
                        fileOutputStream.write(inputString.getBytes());
                        fileOutputStream.flush();
                        fileOutputStream.close();
                        Toast.makeText(this, "Successfully Exported!", Toast.LENGTH_SHORT).show();
                        //onBackPressed();
                    } catch (Exception e) {
                        Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        MaterialToolbar toolbar = binding.toolbarMain.toolbarMain;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
        {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        sharedPreferences = new PrefsUtil(this).sharedPreferences();
        initBindingViews();
        initBindingMethods();
    }

    private void initBindingViews(){
        fileName = binding.etFileName;
        etHWID = binding.etHwid;
        etPassword = binding.etPassword;
        cbMessage = binding.cbMessage;
        etMessage = binding.etMessage;
        cbMobileData = binding.cbMobileData;
        cbRoot = binding.cbBlockRoot;
        cbPassword = binding.cbPassword;
        cbHWID = binding.cbHwid;
        cbValidity = binding.cbValidityCheck;
        tvValid = binding.tvValidity;
        btnExport = binding.btnExport;
    }

    private void initBindingMethods(){
        LinearLayout layHWID = binding.layHWID;
        LinearLayout layPass = binding.layPassword;

        layHWID.setVisibility(View.GONE);
        layPass.setVisibility(View.GONE);
        cbValidity.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b){
                setValidityDate();
            } else {
                validity = 0;
                if (tvValid != null) {
                    tvValid.setVisibility(View.INVISIBLE);
                    tvValid.setText("");
                }
            }
        });

        cbHWID.setOnCheckedChangeListener((compoundButton, b) -> {
            layHWID.setVisibility(b ? View.VISIBLE : View.GONE);
        });
        cbPassword.setOnCheckedChangeListener((compoundButton, b) -> {
            layPass.setVisibility(b ? View.VISIBLE : View.GONE);
        });
        cbMessage.setOnCheckedChangeListener((compoundButton, b) -> {
            etMessage.setEnabled(b);
        });
        btnExport.setOnClickListener(view -> {
            generateConfig();
        });
    }

    private void generateConfig(){
        int connectFrom = sharedPreferences.getInt(AppConstants.SELECTED_CUSTOM, 0);
        String payload = sharedPreferences.getString(AppConstants.CUSTOM_PAYLOAD, "");
        String sni = sharedPreferences.getString(AppConstants.CUSTOM_SNI, "");
        String dns = sharedPreferences.getString(AppConstants.CUSTOM_DNS, "");

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("message", etMessage.getText().toString());
            jsonObject.put("validity", validity);
            // jsonObject.put("app_version", getBuildId(this));

            JSONObject jsonConf = new JSONObject();
            jsonConf.put("connection", connectFrom);
            jsonConf.put("payload", payload);
            jsonConf.put("sni", sni);
            jsonConf.put("dns", dns);
            jsonObject.put("conf", jsonConf);

            jsonObject.put("mobiledata", cbMobileData.isChecked());
            jsonObject.put("blockroot", cbRoot.isChecked());

            JSONObject jsonPassword = new JSONObject();
            jsonPassword.put("required", cbPassword.isChecked());
            jsonPassword.put("pin", etPassword.getText().toString());
            jsonObject.put("password", jsonPassword);

            JSONObject jsonHWID = new JSONObject();
            jsonHWID.put("required", cbHWID.isChecked());
            jsonHWID.put("client", etHWID.getText().toString());
            jsonObject.put("hwid", jsonHWID);

            String json = jsonObject.toString(1);
            createFile(
                    fileName.getText().toString(),
                    "mts",
                    AESCrypt.encrypt(AppConstants.CIPHER_KEY, json));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createFile(String fileName, String ext, String json)
    {
        inputString = json;
        Intent intent = new Intent("android.intent.action.CREATE_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("*text/plain");
        intent.putExtra("android.intent.extra.TITLE", fileName + "." + ext);
        startActivityIntent.launch(intent);
    }

    @Override
    public boolean onSupportNavigateUp()
    {
        onBackPressed();
        return true;
    }

    private void setValidityDate() {
        // Get current date
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        final long timeToday = c.getTimeInMillis();

        // Build constraints to set minimum selectable date as today
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now());

        // Create MaterialDatePicker instance
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Validity Date")
                .setSelection(timeToday + (1000L * 60 * 60 * 24))  // Set default date to tomorrow
                .setCalendarConstraints(constraintsBuilder.build())
                .build();

        // Set positive button click listener
        datePicker.addOnPositiveButtonClickListener(selection -> {
            validity = selection;

            long daysRemaining = (validity - timeToday) / (1000 * 60 * 60 * 24);
            DateFormat df = DateFormat.getDateInstance();

            if (tvValid != null) {
                tvValid.setVisibility(View.VISIBLE);
                tvValid.setText(String.format("%s (%s)", daysRemaining, df.format(validity)));
            }
        });

        // Set negative button and cancel behavior
        datePicker.addOnNegativeButtonClickListener(view -> {
            validity = 0;
            cbValidity.setChecked(false);
        });

        datePicker.addOnCancelListener(dialog -> {
            validity = 0;
            cbValidity.setChecked(false);
        });

        // Show the dialog
        datePicker.show(getSupportFragmentManager(), "MATERIAL_DATE_PICKER");
    }


}