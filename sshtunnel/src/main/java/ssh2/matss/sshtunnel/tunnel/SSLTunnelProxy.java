package ssh2.matss.sshtunnel.tunnel;

import android.content.Context;

import ssh2.matss.sshtunnel.logger.VpnStatus;

import com.trilead.ssh2.ProxyData;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;

public class SSLTunnelProxy implements ProxyData
{
	class HandshakeTunnelCompletedListener implements HandshakeCompletedListener {
        private final String val$host;
        private final int val$port;
        private final SSLSocket val$sslSocket;

        HandshakeTunnelCompletedListener( String str, int i, SSLSocket sSLSocket) {
            this.val$host = str;
            this.val$port = i;
            this.val$sslSocket = sSLSocket;
        }

        public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
			// addLog(new StringBuffer().append("<b>Established ").append(handshakeCompletedEvent.getSession().getProtocol()).append(" connection with ").append(val$host).append(":").append(this.val$port).append(" using ").append(handshakeCompletedEvent.getCipherSuite()).append("</b>").toString());
			//addLog(new StringBuffer().append("<b>Established ").append(handshakeCompletedEvent.getSession().getProtocol()).append(" connection ").append("using ").append(handshakeCompletedEvent.getCipherSuite()).append("</b>").toString());
		    //addLog(new StringBuffer().append("Supported cipher suites: ").append(Arrays.toString(this.val$sslSocket.getSupportedCipherSuites())).toString());
            //addLog(new StringBuffer().append("Enabled cipher suites: ").append(Arrays.toString(this.val$sslSocket.getEnabledCipherSuites())).toString());
            //VpnStatus.logInfo(new StringBuffer().append("SSL: Supported protocols: <br>").append(Arrays.toString(val$sslSocket.getSupportedProtocols())).toString().replace("[", "").replace("]", "").replace(",", "<br>"));
			//VpnStatus.logInfo(new StringBuffer().append("SSL: Enabled protocols: <br>").append(Arrays.toString(val$sslSocket.getEnabledProtocols())).toString().replace("[", "").replace("]", "").replace(",", "<br>"));
			VpnStatus.logInfo("SSL: Using cipher " + handshakeCompletedEvent.getSession().getCipherSuite());
			VpnStatus.logInfo("SSL: Using protocol " + handshakeCompletedEvent.getSession().getProtocol());
			VpnStatus.logInfo("SSL: Handshake finished");
        }
    }

    private Context context;
	private String stunnelServer;
	private int stunnelPort = 443;
	private String stunnelHostSNI;
	private Socket mSocket;
	private boolean isProxy;
	
	public SSLTunnelProxy(String server, int port, String hostSni, boolean proxy, Context context) {
		this.stunnelServer = server;
		this.stunnelPort = port;
		this.stunnelHostSNI = hostSni;
		this.isProxy = proxy;
		this.context = context;
	}

	@Override
	public Socket openConnection(String hostname, int port, int connectTimeout, int readTimeout) throws IOException
	{
		mSocket = SocketChannel.open().socket();
		mSocket.connect(new InetSocketAddress(stunnelServer, stunnelPort));
		if (mSocket.isConnected()) {
			mSocket = doSSLHandshake(hostname, stunnelHostSNI, port);
		}
		return mSocket;
	}


	private SSLSocket doSSLHandshake(String host, String sni, int port) throws IOException {
        try {
			TLSSocketFactory tsf = new TLSSocketFactory();
            SSLSocket socket = (SSLSocket) tsf.createSocket(host, port);
			try {
				socket.getClass().getMethod("setHostname", String.class).invoke(socket, sni);
				//VpnStatus.logInfo("Setting up SNI: "+sni);
			} catch (Throwable e) {
				// ignore any error, we just can't set the hostname...
			}

			socket.setEnabledProtocols(socket.getSupportedProtocols());
            socket.addHandshakeCompletedListener(new HandshakeTunnelCompletedListener(host, port, socket));
            VpnStatus.logInfo("Starting SSL Handshake...");
			socket.startHandshake();
			return socket;
        } catch (Exception e) {
            IOException iOException = new IOException(new StringBuffer().append("Could not do SSL handshake: ").append(e).toString());
            throw iOException;
        }
    }

	@Override
	public void close()
	{
		try {
			if (mSocket != null) {
				mSocket.close();
			}
		} catch(IOException e) {}
	}

}
