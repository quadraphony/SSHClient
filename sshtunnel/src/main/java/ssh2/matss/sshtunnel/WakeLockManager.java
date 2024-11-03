package ssh2.matss.sshtunnel;

import android.content.Context;
import android.os.PowerManager;

public class WakeLockManager {
    private Context mContext;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    public WakeLockManager(Context context) {
        this.mContext = context;
        this.powerManager = ((PowerManager) mContext.getSystemService("power"));
    }

    public void start() {
        this.wakeLock = powerManager.newWakeLock(1, "MTS Wakelock");
        this.wakeLock.acquire();
    }

    public void stop() {
        if (this.wakeLock != null) {
            this.wakeLock.release();
            this.wakeLock = null;
        }
    }
}