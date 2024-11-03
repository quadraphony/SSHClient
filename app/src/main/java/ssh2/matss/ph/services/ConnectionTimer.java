package ssh2.matss.ph.services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import ssh2.matss.library.AppConstants;
import ssh2.matss.ph.preferences.PrefsUtil;
import ssh2.matss.sshtunnel.logger.VpnStatus;
import ssh2.matss.sshtunnel.tunnel.TunnelManagerHelper;

public class ConnectionTimer extends Service {

    public static final String COUNTDOWN_BR = "ConnectionTimer:MTS";
    private final String TAG = "BroadcastService";
    private final Intent intent = new Intent(COUNTDOWN_BR);
    private CountDownTimer cdt;
    private SharedPreferences sp;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"Starting timer...");

        sp = new PrefsUtil(getApplication()).sharedPreferences();
        long millis = sp.getLong(AppConstants.USER_TIMER,0);

        cdt = new CountDownTimer(millis * 1000,1000)
        {
            @Override
            public void onTick(long milliseconds)
            {
                intent.putExtra(AppConstants.COUNTDOWN, milliseconds);
                sendBroadcast(intent);
            }

            @Override
            public void onFinish() {
                if(VpnStatus.isVPNActive()){
                    TunnelManagerHelper.stopSocksHttp(getApplication());
                }
                Log.i(TAG,"Finished!");
            }
        };
        cdt.start();
    }

    @Override
    public void onDestroy() {
        cdt.cancel();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
