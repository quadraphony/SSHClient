package ssh2.matss.sshtunnel.tunnel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.ProxyInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import ssh2.matss.library.AppConstants;
import ssh2.matss.sshtunnel.R;
import ssh2.matss.sshtunnel.config.PasswordCache;
import ssh2.matss.sshtunnel.logger.VpnStatus;
import ssh2.matss.sshtunnel.tunnel.vpn.TunnelState;
import ssh2.matss.sshtunnel.tunnel.vpn.TunnelVpnManager;
import ssh2.matss.sshtunnel.tunnel.vpn.TunnelVpnService;
import ssh2.matss.sshtunnel.tunnel.vpn.TunnelVpnSettings;
import ssh2.matss.sshtunnel.utils.VpnUtils;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.ConnectionMonitor;
import com.trilead.ssh2.DebugLogger;
import com.trilead.ssh2.DynamicPortForwarder;
import com.trilead.ssh2.InteractiveCallback;
import com.trilead.ssh2.KnownHosts;
import com.trilead.ssh2.ProxyData;
import com.trilead.ssh2.ServerHostKeyVerifier;
import com.trilead.ssh2.transport.TransportManager;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;


public class TunnelManagerThread
	implements Runnable, ConnectionMonitor, InteractiveCallback,
		ServerHostKeyVerifier, DebugLogger
{
	private static final String TAG = TunnelManagerThread.class.getSimpleName();

	private Context mContext;
	private Bundle mBundle;
	private Handler mHandler;
	private SharedPreferences defaultPref;

	private OnStopCliente mListener;
	public String[] m_dnsResolvers;
	private boolean mRunning = false, mStopping = false, mStarting = false;
	
	private CountDownLatch mTunnelThreadStopSignal;
	//private ConnectivityManager mCmgr;
	
	public interface OnStopCliente {
		void onStop();
	}
	
	public TunnelManagerThread(Handler handler, Context context, Bundle bundle) {
		mContext = context;
		mHandler = handler;
		mBundle = bundle;
		defaultPref = PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	public void setOnStopClienteListener(OnStopCliente listener) {
		mListener = listener;
	}

	@Override
	public void run()
	{
		mStarting = true;
		mTunnelThreadStopSignal = new CountDownLatch(1);
		
		VpnStatus.logInfo("<strong>" + mContext.getString(R.string.starting_service_ssh) + "</strong>");

		int tries = 0;
		while (!mStopping) {
			try {
				if (!TunnelUtils.isNetworkOnline(mContext)) {
					VpnStatus.updateStateString("WAIT", mContext.getString(R.string.state_nonetwork));
					VpnStatus.logInfo(R.string.state_nonetwork);
					
					try {
						Thread.sleep(5000);
					} catch(InterruptedException e2) {
						stopAll();
						break;
					}
				}
				else {
					if (tries > 0)
						VpnStatus.logInfo("<strong>" + mContext.getString(R.string.state_reconnecting) + "</strong>");

					try {
						Thread.sleep(500);
					} catch(InterruptedException e2) {
						stopAll();
						break;
					}

					startClienteSSH();
					break;
				}
			} catch(Exception e) {
				VpnStatus.logInfo("<strong>" + e.getMessage() + "</strong>");
				VpnStatus.logError("<strong>" + mContext.getString(R.string.state_disconnected) + "</strong>");
				closeSSH();
				
				try {
					Thread.sleep(500);
				} catch(InterruptedException e2) {
					stopAll();
					break;
				}
			}
			
			tries++;
		}
		
		mStarting = false;
		
		if (!mStopping) {
			try {
				mTunnelThreadStopSignal.await();
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		
		if (mListener != null) {
			mListener.onStop();
		}
	}
	
	public void stopAll() {
		if (mStopping) return;
		
		VpnStatus.updateStateString("", mContext.getString(R.string.stopping_service_ssh));
		VpnStatus.logInfo("<strong>" + mContext.getString(R.string.stopping_service_ssh) + "</strong>");
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				mStopping = true;

				if (mTunnelThreadStopSignal != null)
					mTunnelThreadStopSignal.countDown();

				closeSSH();
				
				try {
					Thread.sleep(1000);
				} catch(InterruptedException e){}

				VpnStatus.updateStateString("DISCONNECTED", mContext.getString(R.string.state_disconnected));

				mRunning = false;
				mStarting = false;
				mReconnecting = false;
			}
		}).start();
	}
	
	
	/**
	 * Forwarder
	*/

	protected void startForwarder(int portaLocal) throws Exception {
		if (!mConnected) {
			throw new Exception();
		}
		
		startForwarderSocks(portaLocal);
		
		startTunnelVpnService();
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					if (!mConnected) break;
					
					try {
						Thread.sleep(2000);
					} catch(InterruptedException e) {
						break;
					}
					
					if (lastPingLatency > 0) {
						VpnStatus.logInfo(String.format("Ping Latency: %d ms", lastPingLatency));
						break;
					}
				}
			}
		}).start();
	}

	protected void stopForwarder() {
		stopTunnelVpnService();
		
		stopForwarderSocks();
	}
	
	
	/**
	* Cliente SSH
	*/
	
	private final static int AUTH_TRIES = 1;
	private final static int RECONNECT_TRIES = 5;
	
	private Connection mConnection;
	
	private boolean mConnected = false;
	
	protected void startClienteSSH() throws Exception {
		mStopping = false;
		mRunning = true;
		
		String server = mBundle.getString(AppConstants.SERVER_IP);
		int port = mBundle.getInt(AppConstants.SERVER_PORT);
		String auth_user = mBundle.getString(AppConstants.AUTH_USERNAME);
		
		String _auth_pass = mBundle.getString(AppConstants.AUTH_PASSWORD);
		String auth_pass = _auth_pass.isEmpty() ? PasswordCache.getAuthPassword(null, false) : _auth_pass;

		String keyPath = defaultPref.getString(AppConstants.SSH_PATH, "");
		int localPort = mBundle.getInt(AppConstants.LOCAL_PORT); // matssbuild

		try {
			
			connectTo(server, port);

			for (int i = 0; i < AUTH_TRIES; i++) {
				if (mStopping) {
					return;
				}

				try {
					autenticar(auth_user, auth_pass, keyPath);

					break;
				} catch(IOException e) {
					if (i + 1 >= AUTH_TRIES) {
						throw new IOException("Autentication failed");
					}
					else {
						try {
							Thread.sleep(3000);
						} catch(InterruptedException e2) {
							return;
						}
					}
				}
			}

			VpnStatus.updateStateString("CONNECTED", "SSH Connected");
			VpnStatus.logInfo("<strong>" + mContext.getString(R.string.state_connected) + "</strong>");

			if (Integer.parseInt(defaultPref.getString(AppConstants.SSH_PINGER, "")) > 0) {
				startPinger(Integer.parseInt(defaultPref.getString(AppConstants.SSH_PINGER, "")));
			}

			startForwarder(localPort);

		} catch(Exception e) {
			mConnected = false;

			throw e;
		}
	}
	
	public synchronized void closeSSH() {
		stopForwarder();
		//stopPinger();

		if (mConnection != null) {
			VpnStatus.logDebug("Stopping SSH");
			mConnection.close();
		}
	}
	
	protected void connectTo(String servidor, int porta) throws Exception {
		if (!mStarting) {
			throw new Exception();
		}

		// aqui deve conectar
		try {

			mConnection = new Connection(servidor, porta);

			if (defaultPref.getBoolean(AppConstants.IS_DEBUG, false) && !mBundle.getBoolean(AppConstants.IS_LOCK)) {
				// Desativado, pois estava enchendo o Logger
				//mConnection.enableDebugging(true, this);
				mHandler.post(() -> Toast.makeText(mContext, "Debug mode enabled", Toast.LENGTH_SHORT).show());
			}

			// delay sleep
			if (defaultPref.getBoolean(AppConstants.SSH_DELAY, false)) {
				mConnection.setTCPNoDelay(true);
			}

			// dataCompress
			if (defaultPref.getBoolean(AppConstants.DATA_COMPRESS, false)) {
				mConnection.setCompression(true);
				VpnStatus.logInfo("Data Compression: ON");
			}

			// proxy
			addProxy(mBundle.getInt(AppConstants.CONNECTION_METHOD), mConnection);

			// monitora a conexão
			mConnection.addConnectionMonitor(this);
			
			if (Build.VERSION.SDK_INT >= 23) {
				ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
				ProxyInfo proxy = cm.getDefaultProxy();
				if (proxy != null) {
					VpnStatus.logInfo("<strong>Network Proxy:</strong> " + String.format("%s:%d", proxy.getHost(), proxy.getPort()));
				}
			}
			
			VpnStatus.updateStateString("CONNECTING", mContext.getString(R.string.state_connecting));
			VpnStatus.logInfo(R.string.state_connecting);
			
			mConnection.connect(this, 10*1000, 20*1000);

			mConnected = true;

		} catch(Exception e) {

			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));

			String cause = e.getCause().toString();
			if (useProxy && cause.contains("Key exchange was not finished")) {
				VpnStatus.logError("Proxy: Connection Lost");
			}
			else {
				VpnStatus.logError("SSH : " + cause);
			}
			VpnStatus.logError("SSH : " + e.getMessage());
			throw new Exception(e);
		}
	}


	/**
	 * Autenticação
	 */

	private static final String AUTH_PUBLICKEY = "publickey",
			AUTH_PASSWORD = "password", AUTH_KEYBOARDINTERACTIVE = "keyboard-interactive";


	protected void autenticar(String usuario, String senha, String keyPath) throws IOException {
		if (!mConnected) {
			throw new IOException();
		}

		VpnStatus.updateStateString("AUTH", mContext.getString(R.string.state_auth));

		try {
			if (mConnection.isAuthMethodAvailable(usuario,
					AUTH_PASSWORD)) {

				VpnStatus.logInfo("Authenticating with password");

				if (mConnection.authenticateWithPassword(usuario,
						senha)) {
					VpnStatus.logInfo(mContext.getString(R.string.state_auth_success));
				}
			}
		} catch (IllegalStateException e) {
			Log.e(TAG,
					"Connection went away while we were trying to authenticate",
					e);
		} catch (Exception e) {
			Log.e(TAG, "Problem during handleAuthentication()", e);
		}

		try {
			if (mConnection.isAuthMethodAvailable(usuario,
					AUTH_PUBLICKEY) && keyPath != null && !keyPath.isEmpty()) {
				File f = new File(keyPath);
				if (f.exists()) {
					if (senha.equals("")) senha = null;
					VpnStatus.logInfo("Authenticating with public key");
					if (mConnection.authenticateWithPublicKey(usuario, f,
							senha)) {
						VpnStatus.logInfo(mContext.getString(R.string.state_auth_success));
					}
				}
			}
		} catch (Exception e) {
			Log.d(TAG, "Host does not support 'Public key' authentication.");
		}

		if (!mConnection.isAuthenticationComplete()) {
			VpnStatus.logInfo("Failed to authenticate, expired username or password");
			throw new IOException("It was not possible to authenticate with the data provided");
		}
	}

	// XXX: Is it right?
	@Override
	public String[] replyToChallenge(String name, String instruction,
			int numPrompts, String[] prompt, boolean[] echo) throws Exception {
		String[] responses = new String[numPrompts];
		for (int i = 0; i < numPrompts; i++) {
			// request response from user for each prompt
			if (prompt[i].toLowerCase().contains("password"))
				responses[i] = mBundle.getString(AppConstants.AUTH_PASSWORD);
		}
		return responses;
	}


	/**
	 * ServerHostKeyVerifier
	 * Fingerprint
	 */

	@Override
	public boolean verifyServerHostKey(String hostname, int port,
		String serverHostKeyAlgorithm, byte[] serverHostKey)
	throws Exception {

		String fingerPrint = KnownHosts.createHexFingerprint(
			serverHostKeyAlgorithm, serverHostKey);
		//int fingerPrintStatus = SSHConstants.FINGER_PRINT_CHANGED;

		VpnStatus.logInfo("Finger Print: " + fingerPrint);

		//Log.d(TAG, "Finger Print Type: " + "");

		return true;
	}


	/**
	 * Proxy
	 */

	private boolean useProxy = false;

	protected void addProxy(int connectFrom, Connection conn) throws Exception {

		if (connectFrom != 0) {
			useProxy = true;
			boolean mts = mBundle.getBoolean(AppConstants.IS_LOCK);

			String server_address = mBundle.getString(AppConstants.SERVER_IP);
			int server_port = mBundle.getInt(AppConstants.SERVER_PORT);
			String mCustomPayload = mBundle.getString(AppConstants.TWEAKS_PAYLOAD);
			String mCustomSNI = mBundle.getString(AppConstants.TWEAKS_SNI);
			String proxy_ip = mBundle.getString(AppConstants.REMOTE_IP);
			int proxy_port = mBundle.getInt(AppConstants.REMOTE_PORT);

			final String rp = "Proxy Remote: " + (mts ? AppConstants.LOCKED : proxy_ip + ":" + proxy_port);
			switch(connectFrom) {
				case 1:
					if (mCustomPayload != null) {
						try {

							String base64Payload = Base64.encodeToString(mCustomPayload.getBytes(), Base64.NO_WRAP);
							ProxyData proxyData = new HttpProxyCustom(
									server_address,
									server_port,
									null,
									null,
									mCustomPayload,
									true,
									mContext);
							conn.setProxyData(proxyData);
							//VpnStatus.logInfo(String.format("Payload: %s", base64Payload));
						} catch(Exception e) {
							throw new Exception(mContext.getString(R.string.error_proxy_invalid));
						}
					}
					else {
						useProxy = false;
					}
					break;
				case 2:
					try {
						String base64Payload = Base64.encodeToString(mCustomPayload.getBytes(), Base64.NO_WRAP);
						ProxyData proxyData = new HttpProxyCustom(
								proxy_ip,
								proxy_port,
								null,
								null,
								mCustomPayload,
								false,
								mContext);
						conn.setProxyData(proxyData);
						//VpnStatus.logInfo(String.format("Payload: %s", base64Payload));
						//VpnStatus.logInfo(rp);
					} catch (Exception e) {
						VpnStatus.logError(R.string.error_proxy_invalid);
						throw new Exception(mContext.getString(R.string.error_proxy_invalid));
					}
					break;
				case 3:
					//VpnStatus.logInfo(String.format("<b>Connection From - %s", "SSH - SSL</b>"));
					try {
						String base64Payload = Base64.encodeToString(mCustomSNI.getBytes(), Base64.NO_WRAP);
						SSLTunnelProxy sslTun = new SSLTunnelProxy(
								server_address,
								server_port,
								mCustomSNI,
								false, mContext);
						conn.setProxyData(sslTun);

						//VpnStatus.logInfo(String.format("SNI Host: %s", base64Payload));
					} catch (Exception e) {
						VpnStatus.logError(R.string.error_proxy_invalid);

						throw new Exception(mContext.getString(R.string.error_proxy_invalid));
					}
					break;
				default:
					useProxy = false;
			}

			Log.d(TAG, mBundle.getString(AppConstants.SERVER_IP));
			Log.d(TAG, String.valueOf(mBundle.getInt(AppConstants.SERVER_PORT)));
			Log.d(TAG, mBundle.getString(AppConstants.AUTH_USERNAME));
			Log.d(TAG, mBundle.getString(AppConstants.AUTH_PASSWORD));
			Log.d(TAG, mBundle.getString(AppConstants.REMOTE_IP));
			Log.d(TAG, String.valueOf(mBundle.getInt(AppConstants.REMOTE_PORT)));
			Log.d(TAG, String.valueOf(mBundle.getInt(AppConstants.CONNECTION_METHOD)));
		}
	}


	/**
	 * Socks5 Forwarder
	 */

	private DynamicPortForwarder dpf;

	private synchronized void startForwarderSocks(int portaLocal) throws Exception {
		if (!mConnected) {
			throw new Exception();
		}

		VpnStatus.logInfo("starting socks local");
		VpnStatus.logDebug(String.format("socks local listen: %d", portaLocal));
		
		try {

			int nThreads = Integer.parseInt(defaultPref.getString(AppConstants.MAX_THREADS, ""));

			if (nThreads > 0) {
				dpf = mConnection.createDynamicPortForwarder(portaLocal, nThreads);

				VpnStatus.logDebug("socks local number threads: " + Integer.toString(nThreads));
			}
			else {
				dpf = mConnection.createDynamicPortForwarder(portaLocal);
			}

		} catch (Exception e) {
			VpnStatus.logError("Socks Local: " + e.getCause().toString());

			throw new Exception();
		}
	}

	private synchronized void stopForwarderSocks() {
		if (dpf != null) {
			try {
				dpf.close(); 
			} catch(IOException e){}
			dpf = null;
		}
	}


	/**
	 * Pinger
	 */

	private Thread thPing;
	private long lastPingLatency = -1;
	
	private void startPinger(final int timePing) throws Exception {
		if (!mConnected) {
			throw new Exception();
		}

		VpnStatus.logInfo("starting pinger");

		thPing = new Thread() {
			@Override
			public void run() {
				while (mConnected) {
					try {
						makePinger();
					} catch(InterruptedException e) {
						break;
					}
				}
				VpnStatus.logDebug("pinger stopped");
			}
			
			private synchronized void makePinger() throws InterruptedException {
				try {
					if (mConnection != null) {
						long ping = mConnection.ping();
						if (lastPingLatency < 0) {
							lastPingLatency = ping;
						}
					}
					else throw new InterruptedException();
				} catch(Exception e) {
					Log.e(TAG, "ping error", e);
				}
				
				if (timePing == 0)
					return;

				if (timePing > 0)
					sleep(timePing*1000);
				else {
					VpnStatus.logError("ping invalid");
					throw new InterruptedException();
				}
			}
		};

		// inicia
		thPing.start();
	}

	private synchronized void stopPinger() {
		if (thPing != null && thPing.isAlive()) {
			VpnStatus.logInfo("stopping pinger");
			thPing.interrupt();
			thPing = null;
		}
	}
	
	/**
	 * Connection Monitor
	 */

	@Override
	public void connectionLost(Throwable reason)
	{
		if (mStarting || mStopping || mReconnecting) {
			return;
		}
		
		VpnStatus.logError("<strong>" + mContext.getString(R.string.log_conection_lost) + "</strong>");

		VpnStatus.logInfo(reason.getMessage());

		if (reason != null) {
			if (reason.getMessage().contains(
					"There was a problem during connect")) {
				return;
			} else if (reason.getMessage().contains(
						   "Closed due to user request")) {
				return;
			} else if (reason.getMessage().contains(
						   "The connect timeout expired")) {
				stopAll();
				return;
			}
		} else {
			stopAll();
			return;
		}
		
		reconnectSSH();
	}
	
	public boolean mReconnecting = false;
	
	public void reconnectSSH() {
		if (mStarting || mStopping || mReconnecting) {
			return;
		}
		
		mReconnecting = true;
		
		closeSSH();
		
		VpnStatus.updateStateString("RECONNECTING", "Reconecting..");

		try {
			Thread.sleep(1000);
		} catch(InterruptedException e) {
			mReconnecting = false;
			return;
		}

		for (int i = 0; i < RECONNECT_TRIES; i++) {
			if (mStopping) {
				mReconnecting = false;
				return;
			}

			int sleepTime = 5;
			if (!TunnelUtils.isNetworkOnline(mContext)) {
				VpnStatus.updateStateString("WAIT", "Aguardando rede..");

				VpnStatus.logInfo(R.string.state_nonetwork);
			}
			else {
				sleepTime = 3;
				mStarting = true;
				VpnStatus.updateStateString("RECONNECTING", "Reconnecting..");

				VpnStatus.logInfo("<strong>" + mContext.getString(R.string.state_reconnecting) + "</strong>");

				try {
					startClienteSSH();
					
					mStarting = false;
					mReconnecting = false;
					//mConnected = true;

					return;
				} catch(Exception e) {
					VpnStatus.logInfo("<strong>" + e.getMessage() + "</strong>");
					VpnStatus.logInfo("<strong>" + mContext.getString(R.string.state_disconnected) + "</strong>");
				}
				
				mStarting = false;
			}

			try {
				Thread.sleep(sleepTime*1000);
				i--;
			} catch(InterruptedException e2){
				mReconnecting = false;
				return;
			}
		}
		
		mReconnecting = false;

		stopAll();
	}

	@Override
	public void onReceiveInfo(int id, String msg) {
		if (id == SERVER_BANNER) {
			//VpnStatus.logInfo("<strong>" + mContext.getString(R.string.log_server_banner) + "</strong> " + msg);
		}
	}


	/**
	 * Debug Logger
	 */

	@Override
	public void log(int level, String className, String message)
	{
		VpnStatus.logDebug(String.format("%s: %s", className, message));
	}
	

	/**
	 * Vpn Tunnel
	 */
	 
	String serverAddr;

	protected void startTunnelVpnService() throws IOException {
		if (!mConnected) {
			throw new IOException();
		}
		
		VpnStatus.logInfo("starting tunnel service");

		// Broadcast
		IntentFilter broadcastFilter =
			new IntentFilter(TunnelVpnService.TUNNEL_VPN_DISCONNECT_BROADCAST);
		broadcastFilter.addAction(TunnelVpnService.TUNNEL_VPN_START_BROADCAST);
		// Inicia Broadcast
		LocalBroadcastManager.getInstance(mContext)
			.registerReceiver(m_vpnTunnelBroadcastReceiver, broadcastFilter);

		String m_socksServerAddress = String.format("127.0.0.1:%s", mBundle.getInt(AppConstants.LOCAL_PORT));
		boolean m_dnsForward = defaultPref.getBoolean(AppConstants.DNS_FORWARD, false);
		String m_udpResolver = defaultPref.getBoolean(
				AppConstants.UDP_FORWARD, false) ?
				defaultPref.getString(AppConstants.UDP_RESOLVER, "") : null;

		String servidorIP = mBundle.getString(AppConstants.SERVER_IP);

		if (mBundle.getInt(AppConstants.CONNECTION_METHOD) == 2) {
			try {
				servidorIP = mBundle.getString(AppConstants.REMOTE_IP);
			} catch(Exception e) {
				VpnStatus.logError(R.string.error_proxy_invalid);
				throw new IOException(mContext.getString(R.string.error_proxy_invalid));
			}
		}

		try {
			InetAddress servidorAddr = TransportManager.createInetAddress(servidorIP);
			serverAddr = servidorIP = servidorAddr.getHostAddress();
		} catch(UnknownHostException e) {
			throw new IOException(mContext.getString(R.string.error_server_ip_invalid));
		}
		
		String[] m_excludeIps = {servidorIP};

		ArrayList arrayList = new ArrayList();
		if (m_dnsForward) {
			//m_dnsResolvers = new String[]{dt.getVpnDnsResolver()};
			arrayList.add(defaultPref.getString(AppConstants.DNS_PRIMARY, ""));
			arrayList.add(defaultPref.getString(AppConstants.DNS_SECONDARY, ""));
		}
		else {
			arrayList.addAll(VpnUtils.getNetworkDnsServer(mContext));
			/*List<String> lista = VpnUtils.getNetworkDnsServer(mContext);
			m_dnsResolvers = new String[]{lista.get(0)};
			 */
		}
		m_dnsResolvers = new String[arrayList.size()];
		arrayList.toArray(m_dnsResolvers);


		if (isServiceVpnRunning()) {
			Log.d(TAG, "already running service");

			TunnelVpnManager tunnelManager = TunnelState.getTunnelState().getTunnelManager();
			
			if (tunnelManager != null) {
				tunnelManager.restartTunnel(m_socksServerAddress);
			}

			return;
		}

		Intent startTunnelVpn = new Intent(mContext, TunnelVpnService.class);
		startTunnelVpn.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		TunnelVpnSettings settings = new TunnelVpnSettings(
				//mBundle,
				m_socksServerAddress,
				m_dnsForward,
				m_dnsResolvers,
				(m_dnsForward && m_udpResolver == null || !m_dnsForward && m_udpResolver != null),
				m_udpResolver,
				m_excludeIps,
				mBundle.getBoolean(AppConstants.IS_BYPASS));

		startTunnelVpn.putExtra(TunnelVpnManager.VPN_SETTINGS, settings);

		if (mContext.startService(startTunnelVpn) == null) {
			VpnStatus.logInfo("failed to start tunnel vpn service");

			throw new IOException("Failed to start VPN Service");
		}

		TunnelState.getTunnelState().setStartingTunnelManager();
	}

	public static boolean isServiceVpnRunning() {
		TunnelState tunnelState = TunnelState.getTunnelState();
		return tunnelState.getStartingTunnelManager() || tunnelState.getTunnelManager() != null;
	}

	protected synchronized void stopTunnelVpnService() {
		if (!isServiceVpnRunning()) {
			return;
		}
		
		// Use signalStopService to asynchronously stop the service.
		// 1. VpnService doesn't respond to stopService calls
		// 2. The UI will not block while waiting for stopService to return
		// This scheme assumes that the UI will monitor that the service is
		// running while the Activity is not bound to it. This is the state
		// while the tunnel is shutting down.
		VpnStatus.logInfo("stopping tunnel service");
		
		TunnelVpnManager currentTunnelManager = TunnelState.getTunnelState()
			.getTunnelManager();
		
		if (currentTunnelManager != null) {
			currentTunnelManager.signalStopService();
		}
		
		/*if (mThreadLocation != null && mThreadLocation.isAlive()) {
			mThreadLocation.interrupt();
		}
		mThreadLocation = null;*/

		// Parando Broadcast
		LocalBroadcastManager.getInstance(mContext)
			.unregisterReceiver(m_vpnTunnelBroadcastReceiver);
	}
	
	//private Thread mThreadLocation;

	// Local BroadcastReceiver
	private BroadcastReceiver m_vpnTunnelBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public synchronized void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

			if (TunnelVpnService.TUNNEL_VPN_START_BROADCAST.equals(action)) {
				boolean startSuccess = intent.getBooleanExtra(TunnelVpnService.TUNNEL_VPN_START_SUCCESS_EXTRA, true);

				if (!startSuccess) {
					stopAll();
				}
				
			} else if (TunnelVpnService.TUNNEL_VPN_DISCONNECT_BROADCAST.equals(action)) {
				stopAll();
			}
		}
	};
	
}
