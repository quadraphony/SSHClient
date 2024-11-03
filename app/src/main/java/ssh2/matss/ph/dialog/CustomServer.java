package ssh2.matss.ph.dialog;


import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.fragment.app.DialogFragment;


import java.util.Objects;

import ssh2.matss.library.AppConstants;
import ssh2.matss.ph.R;
import ssh2.matss.ph.databinding.FragmentDialogCustomServerBinding;
import ssh2.matss.ph.preferences.PrefsUtil;

public class CustomServer extends DialogFragment {

    private FragmentDialogCustomServerBinding binding;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;

    public static CustomServer newInstance(String title) {
        CustomServer frag = new CustomServer();
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        sp = new PrefsUtil(requireActivity()).sharedPreferences();
        editor = sp.edit();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        binding = FragmentDialogCustomServerBinding.inflate(getLayoutInflater());
        assert getArguments() != null;
        String title = getArguments().getString("title");

        binding.etAddress.setText(sp.getString(AppConstants.CUSTOM_SERVER_IP, ""));
        binding.etOpenssh.setText(String.valueOf(sp.getInt(AppConstants.CUSTOM_OPENSSH, 0)));
        binding.etDropbear.setText(String.valueOf(sp.getInt(AppConstants.CUSTOM_DROPBEAR, 0)));
        binding.etStunnel.setText(String.valueOf(sp.getInt(AppConstants.CUSTOM_STUNNEL, 0)));
        binding.etPubKey.setText(sp.getString(AppConstants.CUSTOM_PUB_KEY, ""));
        binding.etNameServer.setText(sp.getString(AppConstants.CUSTOM_NAME_SERVER, ""));
        binding.etProxyAddress.setText(sp.getString(AppConstants.CUSTOM_PROXY_IP, ""));
        binding.etProxyPort.setText(String.valueOf(sp.getInt(AppConstants.CUSTOM_PROXY_PORT, 0)));
        binding.etAuthUsername.setText(sp.getString(AppConstants.CUSTOM_AUTH_USER, ""));
        binding.etAuthPassword.setText(sp.getString(AppConstants.CUSTOM_AUTH_PASS, ""));
        binding.btnSave.setOnClickListener(view -> {
            String server_address = Objects.requireNonNull(binding.etAddress.getText()).toString();
            String openssh = Objects.requireNonNull(binding.etOpenssh.getText()).toString();
            String stunnel = Objects.requireNonNull(binding.etStunnel.getText()).toString();
            String dropbear = Objects.requireNonNull(binding.etDropbear.getText()).toString();
            String pub_key = Objects.requireNonNull(binding.etPubKey.getText()).toString();
            String name_server = Objects.requireNonNull(binding.etNameServer.getText()).toString();
            String proxy_address = Objects.requireNonNull(binding.etProxyAddress.getText()).toString();
            String proxy_port = Objects.requireNonNull(binding.etProxyPort.getText()).toString();
            String auth_user = Objects.requireNonNull(binding.etAuthUsername.getText()).toString();
            String auth_pass = Objects.requireNonNull(binding.etAuthPassword.getText()).toString();

            if(server_address.isEmpty() || openssh.isEmpty() ||
                    stunnel.isEmpty() || dropbear.isEmpty() || pub_key.isEmpty() ||
                    name_server.isEmpty() || proxy_address.isEmpty() || proxy_port.isEmpty() ||
                    auth_user.isEmpty() || auth_pass.isEmpty()) {
                Toast.makeText(requireActivity(), "field cannot be empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            editor.putString(AppConstants.CUSTOM_SERVER_IP, server_address);
            editor.putInt(AppConstants.CUSTOM_OPENSSH, Integer.parseInt(openssh));
            editor.putInt(AppConstants.CUSTOM_STUNNEL, Integer.parseInt(stunnel));
            editor.putInt(AppConstants.CUSTOM_DROPBEAR, Integer.parseInt(dropbear));
            editor.putString(AppConstants.CUSTOM_PUB_KEY, pub_key);
            editor.putString(AppConstants.CUSTOM_NAME_SERVER, name_server);
            editor.putString(AppConstants.CUSTOM_PROXY_IP, proxy_address);
            editor.putInt(AppConstants.CUSTOM_PROXY_PORT, Integer.parseInt(proxy_port));
            editor.putString(AppConstants.CUSTOM_AUTH_USER, auth_user);
            editor.putString(AppConstants.CUSTOM_AUTH_PASS, auth_pass);
            editor.apply();
            dismiss();
        });
        binding.btnCancel.setOnClickListener(view -> dismiss());

        return new MaterialAlertDialogBuilder(requireActivity())
                //.setIcon(R.drawable.alert_dialog_icon)
                .setTitle(title)
                .setView(binding.getRoot())
                .create();
    }

}
