package ssh2.matss.library;

/**
 * Build by Codexs
 * 06/12/2022
 * https://t.me/icodexs
 */

public final class AppConstants {

    /**
     * Ads
     */

    public static final String BANNER_ID = "ca-app-pub-3940256099942544/6300978111";
    public static final String INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712";
    public static final String REWARD_ID = "ca-app-pub-3940256099942544/5224354917"; //reward video ad units
    public static String APP_OPEN_ID = "ca-app-pub-3940256099942544/3419835294";

    public static final String CIPHER_KEY = "matsscrypt";
    public static final String RESOURCES_URL = "https://raw.githubusercontent.com/quadraphony/cus/main/1";
    public static final String CHANNEL_URL = "http://telegram.me/customtweak";

    public static final String LOCKED = "Locked";
    public static final String PAYLOAD_BUGHOST = "CONNECT [host_port] [protocol][crlf]Host: www.bughost.com[crlf][crlf]";
    public static final String SNI_BUGHOST = "www.bughost.com";
    public static final String DNS_DEFAULT = "8.8.8.8";

    public static final String
            IS_FIRST = "keyFirstRun",
            SELECTED_SERVER = "keySelectedServer",
            SELECTED_TWEAKS = "keySelectedTweaks",
            SELECTED_CUSTOM = "keySelectedCustom",
            SELECTED_PORT = "keySelectedPort",
            CUSTOM_PAYLOAD = "keyCustomPayload",
            CUSTOM_SNI = "keyCustomSNI",
            CUSTOM_DNS = "keyCustomDNS",
            USE_CUSTOM = "keyUseCustom",
            USE_CUSTOM_SERVER = "keyUseCustomServer",
            COUNTDOWN = "keyCountdown",
            USER_TIMER = "keyTimerUser",
            TIMER_TICK = "keyTimerTick";

    //custom server
    public static final String
            CUSTOM_SERVER_IP = "keyCustomServerAddress",
            CUSTOM_DROPBEAR = "keyCustomDropbear",
            CUSTOM_OPENSSH = "keyCustomOpenSSH",
            CUSTOM_STUNNEL = "keyCustomStunnel",
            CUSTOM_PUB_KEY = "keyCustomPubKey",
            CUSTOM_NAME_SERVER = "keyCustomNameServer",
            CUSTOM_PROXY_IP = "keyCustomProxyAddress",
            CUSTOM_PROXY_PORT = "keyCustomProxyPort",
            CUSTOM_AUTH_USER = "keyCustomUsername",
            CUSTOM_AUTH_PASS = "keyCustomPassword";

    public static final String
            SERVER_IP = "keyServerIP",
            SERVER_PORT = "keyServerPort",
            SERVER_NS = "keyServeName",
            SERVER_PUBKEY = "keyPubKey",
            AUTH_USERNAME = "keyAuthUser",
            AUTH_PASSWORD = "keyAuthPass",
            CUSTOM_PROXY = "keyCustomProxy",
            REMOTE_IP = "keyRemoteIP",
            REMOTE_PORT = "keyRemotePort",
            CONNECTION_METHOD = "keyConnectionMethod",
            TWEAKS_PAYLOAD = "keyTweaksPayload",
            TWEAKS_SNI = "keyTweaksSNI",
            TWEAKS_DNS = "keyTweaksDNS",
            LOCAL_PORT = "keyLocalPort",
    // configs
            CONFIG_STORED = "keyConfigStored",
            AUTHOR_MSG = "keyAuthorMsg",
            SSH_PATH = "keySSHPath",
            IS_BYPASS = "keyBypass",
            IS_LOCK = "keyConfigLock",
    // Vpn
            HARDWARE_ID = "keyHardwareId",
            DNS_FORWARD = "keyDnsForward",
            DNS_PRIMARY = "keyDnsPrimary",
            DNS_SECONDARY = "keyDnsSecondary",
            UDP_FORWARD = "keyUDPForward",
            UDP_RESOLVER = "keyUDPResolver",
            SSH_PINGER = "keySSHPinger",
            MAX_THREADS = "keyMaxThreads",
            DATA_COMPRESS = "keyDataCompress",
            SSH_DELAY = "keySSHDelay",
            IS_DEBUG = "keyDebug",
            WAKELOCK = "keyWakelock",
            IS_TELE = "keyJoinChannel",
    // app
            APP_THEME = "keyAppTheme",
            APP_LANG = "keyLanguage";

    public static final int
            ONE_DAY = 86400,
            ONE_HOUR = 3600,
            TEN_MINUTES = 600,
            FIVE_MINUTES = 300,
            ONE_MINUTE = 60,
            FIFTEEN_SECS = 15,
            REWARDS = 7200, // 2hrs.
            REWARDS_FOR_INSTALL = ONE_HOUR; // 1hr.

    /*VPN State*/
    public static final String
            STATE_STARTING = "STARTING",
            STATE_CONNECTING = "CONNECTING",
            STATE_AUTHENTICATING = "AUTHENTICATING",
            STATE_CONNECTED = "CONNECTED",
            STATE_STOPPING = "STOPPING",
            STATE_DISCONNECTED = "DISCONNECTED",
            STATE_RECONNECTING = "RECONNECTING";
}
