package ssh2.matss.sshtunnel.tunnel.vpn;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.ParcelFileDescriptor;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;

import ssh2.matss.sshtunnel.logger.VpnStatus;
import ssh2.matss.sshtunnel.utils.CustomNativeLoader;
import ssh2.matss.sshtunnel.utils.StreamGobbler;
import ssh2.matss.sshtunnel.utils.VpnUtils;


public class Tun2Socks extends Thread implements StreamGobbler.OnLineListener {
	
    private static final String TAG = Tun2Socks.class.getSimpleName();
    private static final String TUN2SOCKS_BIN = "libtun2socks";
	
	private OnTun2SocksListener mListener;
	public interface OnTun2SocksListener {
		public void onStart();
		public void onStop();
	}
	
	private Process tun2SocksProcess;
    private ParcelFileDescriptor mVpnInterfaceFileDescriptor;
    private int mVpnInterfaceMTU;
    private String mVpnIpAddress;
    private String mVpnNetMask;
    private String mSocksServerAddress;
    private String mUdpgwServerAddress;
	private String mDnsResolverAddress;
    private boolean mUdpgwTransparentDNS;
	private Context mContext;
	
	private File fileTun2Socks;

	public Tun2Socks(Context context, ParcelFileDescriptor vpnInterfaceFileDescriptor, int vpnInterfaceMTU, String vpnIpAddress,
		String vpnNetMask, String socksServerAddress, String udpgwServerAddress, String dnsResolverAddress, boolean udpgwTransparentDNS) {
		mContext = context;
		
		mVpnInterfaceFileDescriptor = vpnInterfaceFileDescriptor;
        mVpnInterfaceMTU = vpnInterfaceMTU;
        mVpnIpAddress = vpnIpAddress;
        mVpnNetMask = vpnNetMask;
        mSocksServerAddress = socksServerAddress;
        mUdpgwServerAddress = udpgwServerAddress;
		mDnsResolverAddress = dnsResolverAddress;
        mUdpgwTransparentDNS = udpgwTransparentDNS;
	}

	@Override
	public void run() {
		
		if (mListener != null) {
			mListener.onStart();
		}
		
 		try {
			
			StringBuilder cmd = new StringBuilder();

			//File fileTun2Socks = CustomNativeLoader.loadExecutableBinary(mContext, "libtun2socks.so");
			fileTun2Socks = CustomNativeLoader.loadNativeBinary(mContext, TUN2SOCKS_BIN, new File(mContext.getFilesDir(),TUN2SOCKS_BIN));
			
			if (fileTun2Socks == null){
				throw new IOException("Bin Tun2Socks não encontrado");
			}

			if (mVpnInterfaceFileDescriptor != null){
				File file_path = new File(ContextCompat.getDataDir(mContext), "sock_path");
				
				try {
					if (!file_path.exists())
						file_path.createNewFile();
				} catch(IOException e){
					throw new IOException("Failed to create file: " + file_path.getCanonicalPath());
				}
				
				cmd.append(fileTun2Socks.getCanonicalPath());
				cmd.append(" --netif-ipaddr " + mVpnIpAddress);
				//cmd.append(" --netif-ip6addr " + mVpnIpAddress);
				cmd.append(" --netif-netmask " + mVpnNetMask);
				cmd.append(" --socks-server-addr " + mSocksServerAddress);
				cmd.append(" --tunmtu " + Integer.toString(mVpnInterfaceMTU));
				cmd.append(" --tunfd " + mVpnInterfaceFileDescriptor.getFd());
				cmd.append(" --sock " + file_path.getAbsolutePath()); 
				cmd.append(" --loglevel " + Integer.toString(3));

				if (mUdpgwServerAddress != null) {
					if (mUdpgwTransparentDNS) {
						cmd.append(" --udpgw-transparent-dns");
					}
					cmd.append(" --udpgw-remote-server-addr " + mUdpgwServerAddress);
				}
				
				if (mDnsResolverAddress != null) {
					cmd.append(" --dnsgw " + mDnsResolverAddress);
				}

				// executa comando
				tun2SocksProcess = Runtime.getRuntime().exec(cmd.toString());

				StreamGobbler stdoutGobbler = new StreamGobbler(tun2SocksProcess.getInputStream(), this);
				StreamGobbler stderrGobbler = new StreamGobbler(tun2SocksProcess.getErrorStream(), this);

				stdoutGobbler.start();
				stderrGobbler.start();
				
				// send Fd
				if (!sendFd(mVpnInterfaceFileDescriptor, file_path)) {
					throw new IOException("Failed to send Fd to sock, this may not be supported on your device. Please contact the developer.");
				}

				tun2SocksProcess.waitFor();
			}
		
		} catch (IOException e) {
			VpnStatus.logException("Tun2Socks Error", e);
		} catch (Exception e) {
			VpnStatus.logDebug("Tun2Socks Error: " + e.getMessage());
		}
		
		tun2SocksProcess = null;
		if (mListener != null) {
			mListener.onStop();
		}
	}

	@Override
	public synchronized void interrupt()
	{
		// TODO: Implement this method
		super.interrupt();
		
		//net.typeblog.socks.System.jniclose(mVpnInterfaceFileDescriptor.getFd());
		
		if (tun2SocksProcess != null)
        	tun2SocksProcess.destroy();
		
		try {
			if (fileTun2Socks != null)
				VpnUtils.killProcess(fileTun2Socks);
		} catch (Exception e) {}
		
		tun2SocksProcess = null;
		fileTun2Socks = null;
	}
	
	public void setOnTun2SocksListener(OnTun2SocksListener listener){
		this.mListener = listener;
	}
	
	
	/**
	* StreamGobbler OnLine Listener
	* implementação
	*/
	
	@Override
	public void onLine(String log){
		VpnStatus.logDebug("Tun2Socks: " + log);
	}
	
	
	//----------------------------------------------------------------------------
	// Utils
	//----------------------------------------------------------------------------

	private boolean sendFd(ParcelFileDescriptor fileDescriptor, File toFile) throws InterruptedException {

		VpnStatus.logDebug("Sending Fd to sock");

		for (int tries = 10; tries >= 0; tries--) {
			try {
				LocalSocket localSocket = new LocalSocket();
				localSocket.connect(new LocalSocketAddress(toFile.getAbsolutePath(), LocalSocketAddress.Namespace.FILESYSTEM)); 
				localSocket.setFileDescriptorsForSend(new FileDescriptor[]{
					fileDescriptor.getFileDescriptor()
				});
				localSocket.getOutputStream().write(42);
				localSocket.shutdownOutput();
				localSocket.close();
				return true;
			} catch(IOException unused) {
				Thread.sleep(500);
			}
			
			/*if (net.typeblog.socks.System.sendfd(fileDescriptor.getFd(), toFile.getAbsolutePath()) != -1) {
                return true;
            }

			try {
				Thread.sleep(500);
			} catch(InterruptedException e){}*/
		}

		return false;
	}
}
