package ssh2.matss.sshtunnel.tunnel.vpn;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class TunnelVpnSettings
	implements Parcelable {

	public String mSocksServer;
	public boolean mDnsForward;
	public String[] mDnsResolver;
	public String mUdpResolver;
	public String[] mExcludeIps;
	public boolean mUdpDnsRelay;
	public boolean bypass;



	public TunnelVpnSettings(String socksServer, boolean dnsForward, String[] dnsResolver,
							 boolean udpDnsRelay, String udpResolver, String[] excludeIps, boolean useBypass)
	{
		mSocksServer = socksServer;
		mDnsForward = dnsForward;
		mUdpDnsRelay = udpDnsRelay;
		mDnsResolver = dnsResolver;
		mUdpResolver = udpResolver;
		mExcludeIps = excludeIps;
		bypass = useBypass;
	}
	
	@Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSocksServer);
        dest.writeInt(mDnsForward ? 1 : 0);
		dest.writeInt(mUdpDnsRelay ? 1 : 0);
        dest.writeStringArray(mDnsResolver);
		dest.writeString(mUdpResolver);
		dest.writeStringArray(mExcludeIps);
		dest.writeInt(bypass ? 1 : 0);
    }
	
	public TunnelVpnSettings(Parcel in) {
        mSocksServer = in.readString();
		mDnsForward = in.readInt() == 1;
		mUdpDnsRelay = in.readInt() == 1;
		mDnsResolver = in.createStringArray();
		mUdpResolver = in.readString();
		mExcludeIps = in.createStringArray();
		bypass = in.readInt() == 1;
    }
	
	public static final Creator<TunnelVpnSettings> CREATOR
	= new Creator<TunnelVpnSettings>() {
        public TunnelVpnSettings createFromParcel(Parcel in) {
            return new TunnelVpnSettings(in);
        }

        public TunnelVpnSettings[] newArray(int size) {
            return new TunnelVpnSettings[size];
        }
    };
}
