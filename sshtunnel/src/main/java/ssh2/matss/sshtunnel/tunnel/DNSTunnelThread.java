package ssh2.matss.sshtunnel.tunnel;

import android.content.Context;
import android.os.Bundle;

import java.io.File;
import java.io.IOException;

import ssh2.matss.library.AppConstants;
import ssh2.matss.sshtunnel.logger.VpnStatus;
import ssh2.matss.sshtunnel.utils.CustomNativeLoader;
import ssh2.matss.sshtunnel.utils.StreamGobbler;
import ssh2.matss.sshtunnel.utils.VpnUtils;

public class DNSTunnelThread extends Thread {

    private Context mContext;
    private static final String DNS_BIN = "libdns";
    private Process dnsProcess;
    private File filedns;
    private Bundle bundle;

    public DNSTunnelThread(Context context, Bundle bundle) {
        mContext = context;
        this.bundle = bundle;
    }

    @Override
    public void run(){
        try {
            String mDns = bundle.getString(AppConstants.TWEAKS_DNS);
            String chave = bundle.getString(AppConstants.SERVER_PUBKEY);
            String nameserver = bundle.getString(AppConstants.SERVER_NS);

            StringBuilder cmd1 = new StringBuilder();
            filedns = CustomNativeLoader.loadNativeBinary(mContext, DNS_BIN, new File(mContext.getFilesDir(),DNS_BIN));

            if (filedns == null){
                throw new IOException("DNS bin not found");
            }

            cmd1.append(filedns.getCanonicalPath());
            cmd1.append(" -udp " + mDns + ":53   -pubkey " + chave + " " + nameserver + " " + "127.0.0.1:2222");

            dnsProcess = Runtime.getRuntime().exec(cmd1.toString());

            StreamGobbler.OnLineListener onLineListener = new StreamGobbler.OnLineListener(){
                @Override
                public void onLine(String log){
                    VpnStatus.logInfo("<b>DNS Client: </b>" + log);
                   //VpnStatus.logInfo(String.format("<b>DNS Info: </b> %s", (dt.isLock() ? OharaConstants.locked : cmd1.toString())));
                }
            };
            StreamGobbler stdoutGobbler = new StreamGobbler(dnsProcess.getInputStream(), onLineListener);
            StreamGobbler stderrGobbler = new StreamGobbler(dnsProcess.getErrorStream(), onLineListener);

            stdoutGobbler.start();
            stderrGobbler.start();

            dnsProcess.waitFor();
        } catch (IOException | InterruptedException e) {
            VpnStatus.logInfo("SlowDNS: " + e.getMessage());
        }

    }

    @Override
    public void interrupt(){
        if (dnsProcess != null)
            dnsProcess.destroy();
        try {
            if (filedns != null)
                VpnUtils.killProcess(filedns);
        } catch (Exception e) {}

        dnsProcess = null;
        filedns = null;
        super.interrupt();
    }

}

