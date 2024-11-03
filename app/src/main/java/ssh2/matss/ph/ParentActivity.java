package ssh2.matss.ph;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import ssh2.matss.library.AppConstants;



public abstract class ParentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        setTheme();
        super.onCreate(savedInstanceState);
    }

    protected SharedPreferences dsp(){
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    protected void setTheme() {
        AppCompatDelegate.setDefaultNightMode(
                (dsp().getBoolean(AppConstants.APP_THEME, false)
                        ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO));

    }

    protected void showToast(String msg, int length){
        Toast.makeText(this, msg, length).show();
    }

    protected String uniqueDevice(){
        @SuppressLint("HardwareIds") String android_id = android.provider.Settings.Secure.getString(
                getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);
        return md5(android_id).toUpperCase();
    }
    
    public void identity() {
        ClipboardManager myClipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Identification");
        builder.setMessage(uniqueDevice());
        builder.setCancelable(true);
        builder.setPositiveButton("COPY", (dialogInterface, i) -> {
            dialogInterface.dismiss();
            ClipData myClip = ClipData.newPlainText("text", uniqueDevice());
            myClipboard.setPrimaryClip(myClip);
            showToast("Copied: " + uniqueDevice(), Toast.LENGTH_SHORT);
        });
        final MaterialAlertDialogBuilder dialog = builder;
        dialog.show();
    }

    private String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) hexString.append(Integer.toHexString(0xFF & b));
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    protected String vb()
    {
        try
        {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
            return "" + packageInfo.versionName + " (MTS Build " + packageInfo.versionCode + ")";
        }
        catch (Exception e)
        {
            return "1.0";
        }
    }

    protected void channel() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Join Us on Telegram?");
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setMessage("We have a telegram support channel where we post and discuss about settings, new features and also assist our users. \n \nWould you like to join us there ?");
        builder.setCancelable(false);
        builder.setPositiveButton("YES PLEASE", (dialogInterface, i) -> {
            dsp().edit().putBoolean(AppConstants.IS_TELE, false).apply();
            intent_uri(AppConstants.CHANNEL_URL);
        });
        builder.setNeutralButton("LATER", (dialogInterface, i) -> {


        });
        final MaterialAlertDialogBuilder dialog = builder;
        dialog.show();

    }

    private void intent_uri(String url){
        Intent telegramIntent = new Intent(Intent.ACTION_VIEW);
        telegramIntent.setData(Uri.parse(url));
        startActivity(telegramIntent);
    }


}
