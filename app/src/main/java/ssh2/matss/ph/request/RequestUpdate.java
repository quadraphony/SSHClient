package ssh2.matss.ph.request;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import ssh2.matss.library.AppConstants;
import ssh2.matss.ph.cipher.AESCrypt;
import ssh2.matss.ph.config.Profiling;

public class RequestUpdate {

    private final Context context;
    private AlertDialog progressDialog; // Declare the progress dialog

    public RequestUpdate(Context context, String url) {
        this.context = context;
        Profiling p = new Profiling(context);

        // Create a LinearLayout to center the progress indicator
        LinearLayout layout = new LinearLayout(context);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setGravity(Gravity.CENTER); // Center the content
        layout.setOrientation(LinearLayout.VERTICAL); // Vertical orientation

        // Create and add CircularProgressIndicator to the layout
        LinearProgressIndicator progressIndicator = new LinearProgressIndicator(context);
        progressIndicator.setIndeterminate(true); // Set to indeterminate mode
        progressIndicator.setVisibility(android.view.View.VISIBLE); // Make it visible
        layout.addView(progressIndicator); // Add indicator to layout

        // Create the AlertDialog with the centered layout
        progressDialog = new MaterialAlertDialogBuilder(context)
                .setTitle("Request for Update")
                .setMessage("Please wait, while checking resources...")
                .setView(layout) // Add centered layout
                .setCancelable(false) // Prevents the dialog from being dismissed
                .create();
        progressDialog.show();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                dismissProgressDialog(); // Dismiss dialog on failure
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String result = response.body().string();
                    ((Activity) context).runOnUiThread(() -> {
                        dismissProgressDialog(); // Dismiss dialog on response
                        handleResponse(result, p); // Handle the response
                    });
                } else {
                    dismissProgressDialog(); // Dismiss dialog on unsuccessful response
                }
            }
        });
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void handleResponse(String result, Profiling p) {
        try {
            JSONObject results = new JSONObject(
                    AESCrypt.decrypt(AppConstants.CIPHER_KEY, result));
            String version = results.getString("version");
            String msg = results.getString("message");
            if (p.check_version(version)) {
                updateResources(result, msg);
            } else {
                Toast.makeText(context, "No update is available!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateResources(String result, String msg) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("New Update Available")
                .setMessage(msg)
                .setPositiveButton("APPLY", (dialogInterface, i) -> {
                    try {
                        File file = new File(context.getFilesDir(), "configs.json");
                        OutputStream out = new FileOutputStream(file);
                        out.write(result.getBytes());
                        out.flush();
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    triggerRebirth();
                })
                .setNeutralButton("LATER", null)
                .create().show();
    }

    private void triggerRebirth() {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        context.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }
}
