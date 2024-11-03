package ssh2.matss.ph.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatRadioButton;

import ssh2.matss.library.AppConstants;
import ssh2.matss.ph.preferences.PrefsUtil;
import ssh2.matss.ph.R;
import ssh2.matss.ph.databinding.GeneratorPayloadBinding;

public class PayloadGenerator {

    private final GeneratorPayloadBinding binding;
    private final Context context;
    private final SharedPreferences sp;

    private final AlertDialog ad;

    private RadioGroup A;
    private AppCompatRadioButton B;
    private Spinner C;
    private Spinner D;
    private EditText E;
    private Spinner F;
    private CheckBox o;
    private CheckBox p;
    private CheckBox q;
    private CheckBox r;
    private CheckBox s;
    private CheckBox t;
    private CheckBox u;
    private CheckBox vj;
    private CheckBox w;
    private CheckBox x;
    private CheckBox y;

    public PayloadGenerator(Context context){
        this.context = context;
        binding = GeneratorPayloadBinding.inflate(LayoutInflater.from(context));
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        setViews();
        MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context);
        adb.setTitle("Payload Generator");
        adb.setView(binding.getRoot());
        ad = adb.create();
    }

    private void setViews(){
        this.E = binding.editTextInjectUrl;
        this.E.setText(this.sp.getString("xHost", ""));
        this.C = binding.spinnerRequestMethod;
        this.C.setSelection(this.sp.getInt("RequestMethod", 0));
        this.D = binding.spinnerInjectMethod;
        this.D.setSelection(this.sp.getInt("InjectMethod", 0));
        this.o = binding.checkBoxFrontQuery;
        this.o.setChecked(this.sp.getBoolean("xFrontQuery", false));
        this.p = binding.checkBoxBackQuery;
        this.p.setChecked(this.sp.getBoolean("xBackQuery", false));
        this.q = binding.checkBoxOnlineHost;
        this.q.setChecked(this.sp.getBoolean("xOnlineHost", false));
        this.r = binding.checkBoxForwardedFor;
        this.r.setChecked(this.sp.getBoolean("xForwardedFor", false));
        this.s = binding.checkBoxForwardHost;
        this.s.setChecked(this.sp.getBoolean("xForwardHost", false));
        this.t = binding.checkBoxKeepAlive;
        this.t.setChecked(this.sp.getBoolean("xKeepAlive", false));
        this.u = binding.checkBoxUserAgent;
        this.u.setChecked(this.sp.getBoolean("xUserAgent", false));
        this.F = binding.spinner2;
        this.F.setSelection(this.sp.getInt("UserAgent", 0));
        this.vj = binding.checkBoxRealRequest;
        this.vj.setChecked(this.sp.getBoolean("xRealRequest", false));
        this.w = binding.checkBoxDualConnect;
        this.w.setChecked(this.sp.getBoolean("xDualConnect", false));
        Button n = binding.buttonGenerate;
        this.F.setEnabled(false);
        this.o.setOnClickListener(view -> p.setChecked(false));
        this.p.setOnClickListener(view -> o.setChecked(false));
        this.u.setOnClickListener(view -> F.setEnabled(!F.isEnabled()));
        this.A = binding.radioGeneratorPayload;
        this.B = binding.getRoot().findViewById(A.getCheckedRadioButtonId());
        this.x = binding.rotationMethodCheckbox;
        this.y = binding.splitNoDelayCheckbox;
        this.A.setOnCheckedChangeListener((radioGroup, i) -> {
            B = (AppCompatRadioButton) A.findViewById(i);
            if (A.indexOfChild(B) == 1) {
                y.setEnabled(true);
                y.setChecked(sp.getBoolean("xSplitNoDelay", false));
                return;
            }
            y.setEnabled(false);
            y.setChecked(false);
        });
        this.A.check(getRadioButtonId(this.sp.getInt("xRadioGroup", 1)));
        this.x.setOnCheckedChangeListener((compoundButton, z) -> {
            if (z) {
                E.setHint("ex. bug1.com;bug2.com");
            } else {
                E.setHint("ex. bug.com");
            }
        });
        this.x.setChecked(this.sp.getBoolean("xRotation", false));
        n.setOnClickListener(view -> {
            String i1 = B.getText().toString();
            StringBuilder sb = new StringBuilder();
            String obj = D.getSelectedItem().toString();
            String obj2 = C.getSelectedItem().toString();
            String replace = E.getText().toString().replace("http://", "").replace("https://", "");
            if (x.isChecked()) {
                replace = "[rotation=" + replace + "]";
            }
            StringBuilder sb2 = new StringBuilder();
            if (o.isChecked()) {
                sb2.append(replace).append("@");
            }
            sb2.append("[host_port]");
            if (p.isChecked()) {
                sb2.append("@").append(replace);
            }
            String sb3 = sb2.toString();
            String str = "";
            if (i1.equals("SPLIT")) {
                str = y.isChecked() ? obj.equals("Back Inject") ? "[crlf][splitNoDelay]" : "[splitNoDelay]" : obj.equals("Back Inject") ? "[crlf][split]" : "[split]";
            }
            if (obj.equals("Front Inject")) {
                sb.append(obj2).append(" http://").append(replace).append("/ HTTP/1.1[crlf]");
            } else if (obj.equals("Back Inject")) {
                sb.append("CONNECT ").append(sb3).append(" HTTP/1.1[crlf][crlf]").append(str).append(obj2).append(" http://").append(replace).append("/ [protocol][crlf]");
            } else if (!vj.isChecked()) {
                sb.append(obj2).append(" ").append(sb3).append(" [protocol][crlf]");
            } else if (o.isChecked() || p.isChecked()) { // obj.equals("Back Inject") ||
                sb.append(obj2).append(" ").append(sb3).append(" [protocol][crlf]");
            } else {
                sb.append("[netData][crlf]");
            }
            sb.append("Host: ").append(replace).append("[crlf]");
            if (q.isChecked()) {
                sb.append("X-Online-Host: ").append(replace).append("[crlf]");
            }
            if (s.isChecked()) {
                sb.append("X-Forward-Host: ").append(replace).append("[crlf]");
            }
            if (r.isChecked()) {
                sb.append("X-Forwarded-For: ").append(replace).append("[crlf]");
            }
            if (t.isChecked()) {
                sb.append("Connection: Keep-Alive[crlf]");
            }
            if (u.isChecked()) {
                String b = F.getSelectedItem().toString();
                switch (b) {
                    case "Firefox":
                        sb.append("User-Agent: Mozilla/5.0 (Android; Mobile; rv:35.0) Gecko/35.0 Firefox/35.0\r\n");
                        break;
                    case "Chrome":
                        sb.append("User-Agent: Mozilla/5.0 (Linux; Android 4.4.2; SAMSUNG-SM-T537A Build/KOT49H) AppleWebKit/537.36 (KHTML like Gecko) Chrome/35.0.1916.141 Safari/537.36[crlf]");
                        break;
                    case "Opera Mini":
                        sb.append("User-Agent: Mozilla/5.0 (Linux; Android 5.1.1; Nexus 7 Build/LMY47V) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.78 Safari/537.36 OPR/30.0.1856.93524[crlf]");
                        break;
                    case "Puffin":
                        sb.append("User-Agent: Mozilla/5.0 (X11; U; Linux x86_64; en-gb) AppleWebKit/534.35 (KHTML, like Gecko) Chrome/11.0.696.65 Safari/534.35 Puffin/2.9174AP[crlf]");
                        break;
                    case "Safari":
                        sb.append("User-Agent: Mozilla/5.0 (Linux; U; Android 2.0; en-us; Droid Build/ESD20) AppleWebKit/530.17 (KHTML, like Gecko) Version/4.0 Mobile Safari/530.17[crlf]");
                        break;
                    case "UCBrowser":
                        sb.append("User-Agent: Mozilla/5.0 (Linux; U; Android 2.3.3; en-us ; LS670 Build/GRI40) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1/UCBrowser/8.6.1.262/145/355[crlf]");
                        break;
                    case "Default":
                        sb.append("User-Agent: [ua][crlf]");
                        break;
                }
            }
            if (w.isChecked()) {
                sb.append("CONNECT [host_port] [protocol][crlf]");
            }
            sb.append("[crlf]");
            if (obj.equals("Front Inject")) {
                if (!vj.isChecked()) {
                    sb.append(str).append("CONNECT ").append(sb3).append(" [protocol][crlf][crlf]");
                } else if (o.isChecked() || p.isChecked()) {
                    sb.append(str).append("CONNECT ").append(sb3).append(" [protocol][crlf][crlf]");
                } else {
                    sb.append(str).append("[netData][crlf][crlf]");
                }
            }

           // editor.putString(OharaConstants.PAYLOAD, sb.toString()).apply();
            new PrefsUtil(context).sharedPreferences()
                    .edit().putString(AppConstants.CUSTOM_PAYLOAD, sb.toString()).apply();
            SharedPreferences.Editor edit2 = sp.edit();
            edit2.putString("xHost", E.getText().toString());
            edit2.putBoolean("xFrontQuery", o.isChecked());
            edit2.putBoolean("xBackQuery", p.isChecked());
            edit2.putBoolean("xOnlineHost", q.isChecked());
            edit2.putBoolean("xForwardedFor", r.isChecked());
            edit2.putBoolean("xForwardHost", s.isChecked());
            edit2.putBoolean("xKeepAlive", t.isChecked());
            edit2.putBoolean("xUserAgent", u.isChecked());
            edit2.putBoolean("xRealRequest", vj.isChecked());
            edit2.putBoolean("xDualConnect", w.isChecked());
            edit2.putBoolean("xRotation", x.isChecked());
            edit2.putBoolean("xSplitNoDelay", y.isChecked());
            int i = 1;
            int checkedRadioButtonId = A.getCheckedRadioButtonId();
            if (checkedRadioButtonId == R.id.radioGeneratorPayloadMerger) {
                i = 1;
            } else if (checkedRadioButtonId == R.id.radioGeneratorPayloadSplit) {
                i = 2;
            }
            edit2.putInt("xRadioGroup", i);
            edit2.putInt("RequestMethod", C.getSelectedItemPosition());
            edit2.putInt("InjectMethod", D.getSelectedItemPosition());
            edit2.putInt("UserAgent", F.getSelectedItemPosition());
            edit2.apply();
            dismiss();
        });
    }

    private int getRadioButtonId(int i) {
        if (i == 2) {
            return R.id.radioGeneratorPayloadSplit;
        }
        return R.id.radioGeneratorPayloadMerger;
    }

    public void dismiss() {
        ad.dismiss();
    }

    public void show() {
        ad.show();
    }

}
