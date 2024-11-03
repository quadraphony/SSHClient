package ssh2.matss.ph.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import ssh2.matss.library.AppConstants;
import ssh2.matss.ph.cipher.AESCrypt;
import ssh2.matss.ph.preferences.PrefsUtil;

public class Profiling {

    private final String TAG = "Resources";
    private final Context con;

    public Profiling(Context context){
        this.con = context;
    }

    public JSONArray network_ja(){
        try {
            JSONObject resources = extractResources();
            if(resources != null){
                return resources.getJSONArray("networks");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public JSONArray server_ja(){
        try {
            JSONObject resources = extractResources();
            if(resources != null){
                return resources.getJSONArray("servers");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    private JSONObject extractResources(){
        try {
            File file = new File(con.getFilesDir(), "configs.json");
            if (file.exists()) {
                String json = readStream(new FileInputStream(file));
                return new JSONObject(AESCrypt.decrypt(AppConstants.CIPHER_KEY, json));
            } else {
                InputStream inputStream = con.getAssets().open("resources/configs.json");
                String json = readStream(inputStream);
                return new JSONObject(AESCrypt.decrypt(AppConstants.CIPHER_KEY, json));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "MTS Error Config: " + e.getMessage());
        }
        return null;
    }


    public JSONArray extractCustoms(){
        try {
            InputStream inputStream = con.getAssets().open("resources/customs.json");
            String json = readStream(inputStream);
            return new JSONObject(json).getJSONArray("customs");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "MTS Error Config: " + e.getMessage());
        }
        return null;
    }

    private boolean compareVersion(Pair<String, String> version) {
        String[] bago = version.first.split("\\.");
        String[] luma = version.second.split("\\.");
        int i = 0;

        // set index to first non-equal ordinal or length of shortest version string
        while (i < bago.length && i < luma.length && bago[i].equals(luma[i])) {
            i++;
        }
        // compare first non-equal ordinal number
        if (i < bago.length && i < luma.length) {
            int diff = Integer.valueOf(bago[i]).compareTo(Integer.valueOf(luma[i]));
            return Integer.signum(diff) > 0;
        }

        // the strings are equal or one string is a substring of the other
        // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
        return Integer.signum(bago.length - luma.length) > 0;
    }

    private String getVersion() {
        JSONObject resources = extractResources();
        if(resources != null){
            try {
                return resources.getString("version");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public boolean check_version(String updated) {
        String version = getVersion();
        if(version != null){
            return compareVersion(new Pair<>(updated, version));
        }
        return false;
    }


    public String readStream(InputStream in) {
        StringBuilder sb = new StringBuilder();
        try {
            Reader reader = new BufferedReader(new InputStreamReader(in));
            char[] buff = new char[1024];
            while (true) {
                int read = reader.read(buff, 0, buff.length);
                if (read <= 0) {
                    break;
                }
                sb.append(buff, 0, read);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public Bundle generateConfig(boolean custom, 
                                 int serverPos,
                                 int networkPos){

        SharedPreferences sp = new PrefsUtil(con).sharedPreferences();
        SharedPreferences defaultPref = PreferenceManager.getDefaultSharedPreferences(con);
        boolean custom_server = sp.getBoolean(AppConstants.USE_CUSTOM_SERVER, false);
        Bundle bundle = new Bundle();
        try {
            JSONObject server = server_ja().getJSONObject(serverPos);
            JSONObject auth = server.getJSONObject("authentication");
            JSONObject network = network_ja().getJSONObject(networkPos);
            JSONObject proxy = network.getJSONObject("custom_proxy");

            int connection = custom ? networkPos : network.getInt("connection");

            bundle.putString(AppConstants.SERVER_IP, (
                    custom_server ? sp.getString(AppConstants.CUSTOM_SERVER_IP, "") : server.getString("server_ip")
                    ));
            bundle.putString(AppConstants.SERVER_PUBKEY, (
                    custom_server ? sp.getString(AppConstants.SERVER_PUBKEY, "") : server.getString("public_key")
                    ));
            bundle.putString(AppConstants.SERVER_NS, (
                    custom_server ? sp.getString(AppConstants.CUSTOM_NAME_SERVER, "") : server.getString("name_server")
                    ));
            bundle.putString(AppConstants.AUTH_USERNAME, (
                    custom_server ? sp.getString(AppConstants.CUSTOM_AUTH_USER, "") : auth.getString("username")
                    ));
            bundle.putString(AppConstants.AUTH_PASSWORD, (
                    custom_server ? sp.getString(AppConstants.CUSTOM_AUTH_PASS, "") : auth.getString("password")
                    ));
            bundle.putInt(AppConstants.LOCAL_PORT, 1080);
            bundle.putBoolean(AppConstants.IS_LOCK, false);

            if(custom && networkPos == 5) {
                String stored = sp.getString(AppConstants.CONFIG_STORED, "");
                JSONObject conf = new JSONObject(stored);
                connection = conf.getInt("connection");
                bundle.putInt(AppConstants.CONNECTION_METHOD, connection);
                bundle.putString(AppConstants.TWEAKS_PAYLOAD, conf.getString("payload"));
                bundle.putString(AppConstants.TWEAKS_SNI, conf.getString("sni"));
                bundle.putString(AppConstants.TWEAKS_DNS, conf.getString("dns"));
            } else {
                bundle.putInt(AppConstants.CONNECTION_METHOD, connection);
                bundle.putString(AppConstants.TWEAKS_PAYLOAD,
                        (custom ? sp.getString(AppConstants.CUSTOM_PAYLOAD, "") : network.getString("payload")));
                bundle.putString(AppConstants.TWEAKS_SNI,
                        (custom ? sp.getString(AppConstants.CUSTOM_SNI, "") : network.getString("sni")));
                bundle.putString(AppConstants.TWEAKS_DNS,
                        (custom ? sp.getString(AppConstants.CUSTOM_DNS, "") : network.getString("dns")));
            }

            if(connection == 0 || connection == 1 || connection == 4){
                bundle.putInt(AppConstants.SERVER_PORT, (
                        custom_server ? sp.getInt(AppConstants.CUSTOM_DROPBEAR, 0) : server.getInt("dropbear")
                        ));
            } else if(connection == 2){
                bundle.putInt(AppConstants.SERVER_PORT, (
                        custom_server ? sp.getInt(AppConstants.CUSTOM_OPENSSH, 0) : server.getInt("openssh")
                        ));
            } else if(connection == 3){
                bundle.putInt(AppConstants.SERVER_PORT, (
                        custom_server ? sp.getInt(AppConstants.CUSTOM_STUNNEL, 0) : server.getInt("stunnel")
                        ));
            }

            if(proxy.getBoolean("is")){
                bundle.putString(AppConstants.REMOTE_IP, proxy.getString("proxy_host"));
                bundle.putInt(AppConstants.REMOTE_PORT, proxy.getInt("proxy_port"));
            } else {
                bundle.putString(AppConstants.REMOTE_IP, (
                        custom_server ? sp.getString(AppConstants.CUSTOM_PROXY_IP, "") : server.getString("server_ip")
                        ));
                bundle.putInt(AppConstants.REMOTE_PORT, (
                        custom_server ? sp.getInt(AppConstants.CUSTOM_PROXY_PORT, 0) : 8080
                        ));
            }

            //preferences
            bundle.putInt(AppConstants.SSH_PINGER, Integer.parseInt(defaultPref.getString(AppConstants.SSH_PINGER, "")));

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return bundle;
    }

}
