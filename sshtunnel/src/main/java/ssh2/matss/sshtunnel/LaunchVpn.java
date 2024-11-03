package ssh2.matss.sshtunnel;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import ssh2.matss.sshtunnel.logger.VpnStatus;
import ssh2.matss.sshtunnel.tunnel.TunnelManagerHelper;
import ssh2.matss.sshtunnel.tunnel.TunnelUtils;
import ssh2.matss.sshtunnel.logger.ConnectionStatus;

public class LaunchVpn extends AppCompatActivity
        implements DialogInterface.OnCancelListener
{
    public static final String EXTRA_HIDELOG = "com.httpdose.dvs.showNoLogWindow";

    private static final int START_VPN_PROFILE = 70;

    private SharedPreferences mConfig;
    private boolean mhideLog = false;

    private Bundle bundle;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.launchvpn);

        startVpnFromIntent();
        //throw new RuntimeException();
    }

    protected void startVpnFromIntent() {
        // Resolve the intent

        final Intent intent = getIntent();
        final String action = intent.getAction();
        bundle = intent.getExtras();

        if (Intent.ACTION_MAIN.equals(action)) {
            // Check if we need to clear the log
           /* if (Preferences.getDefaultSharedPreferences(this).getBoolean(CLEARLOG, true))
                VpnStatus.clearLog(); //mtsbuild

            */
            mhideLog = intent.getBooleanExtra(EXTRA_HIDELOG, false);

            launchVPN();
        }
    }

    @Override
    public void onCancel(DialogInterface p1)
    {
        VpnStatus.updateStateString("USER_VPN_PASSWORD_CANCELLED", "",
                R.string.state_user_vpn_password_cancelled,
                ConnectionStatus.LEVEL_NOTCONNECTED);
        finish();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == START_VPN_PROFILE) {
            if (resultCode == Activity.RESULT_OK) {

                if (!TunnelUtils.isNetworkOnline(this)) {
                    VpnStatus.updateStateString("USER_VPN_PASSWORD_CANCELLED", "",
                            R.string.state_user_vpn_password_cancelled,
                            ConnectionStatus.LEVEL_NOTCONNECTED);

                    Toast.makeText(this, R.string.error_internet_off,
                            Toast.LENGTH_SHORT).show();

                    finish();
                }
                else {

                    TunnelManagerHelper.startSocksHttp(this, bundle);
                    finish();
                }

            } else if (resultCode == Activity.RESULT_CANCELED) {
                // User does not want us to start, so we just vanish
                VpnStatus.updateStateString("USER_VPN_PERMISSION_CANCELLED", "", R.string.state_user_vpn_permission_cancelled,
                        ConnectionStatus.LEVEL_NOTCONNECTED);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    VpnStatus.logError(R.string.nought_alwayson_warning);

                finish();
            }
        }
    }

    private void launchVPN() {
        Intent intent = VpnService.prepare(this);

        if (intent != null) {
            VpnStatus.updateStateString("USER_VPN_PERMISSION", "", R.string.state_user_vpn_permission,
                    ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT);
            // Start the query
            try {
                startActivityForResult(intent, START_VPN_PROFILE);
            } catch (ActivityNotFoundException ane) {
                // Shame on you Sony! At least one user reported that
                // an official Sony Xperia Arc S image triggers this exception
                VpnStatus.logError(R.string.no_vpn_support_image);
            }
        } else {
            onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null);
        }
    }

}
