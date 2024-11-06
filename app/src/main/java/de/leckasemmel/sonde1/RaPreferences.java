package de.leckasemmel.sonde1;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.io.File;
import java.util.Set;

public class RaPreferences {
    private final static String TAG = RaPreferences.class.getName();

    public final static String KEY_PREF_BLUETOOTH_MAC_ADDRESSES = "prefBluetoothMac";
    public final static String KEY_PREF_MAP_PATH = "prefMapPath";
    public final static String KEY_PREF_MAP_FILES = "prefMapFiles";
    public final static String KEY_PREF_MAP_THEME_PATH = "prefMapThemePath";
    public final static String KEY_PREF_MAP_CUSTOM_THEME_FILE = "prefMapCustomThemeFile";
    public final static String KEY_PREF_MAP_USE_CUSTOM_THEME = "prefMapUseCustomTheme";
    public final static String KEY_PREF_MAP_USE_HILL_SHADING = "prefMapUseHillShading";
    public final static String KEY_PREF_MAP_HGT_PATH = "prefMapHgtPath";
    public final static String KEY_PREF_MAP_PREDICT_BURST_ALTITUDE = "prefMapPredictBurstAltitude";
    public final static String KEY_PREF_MAP_PREDICT_LANDING_TIME_STYLE = "prefMapPredictLandingTimeStyle";
    public final static String KEY_PREF_LOOK_SMETER_STYLE = "prefLookSmeterStyle";
    public final static String KEY_PREF_LOOK_THEME = "prefLookTheme";
    public final static String KEY_PREF_SYSTEM_FIRMWARE_PATH = "prefSystemFirmwarePath";
    public final static String KEY_PREF_SYSTEM_RINEX_URL = "prefSystemRinexUrl";
    public final static String KEY_PREF_SYSTEM_PREDICT_URL = "prefSystemPredictUrl";
    public final static String KEY_PREF_SYSTEM_LOG_RAW_FRAMES = "prefSystemLogRawFrames";
    public final static String KEY_PREF_SYSTEM_SHOW_RSSI_IN_MAP = "prefSystemShowRssiInMap";
    public final static String KEY_PREF_SYSTEM_SHOW_BLE_RSSI = "prefSystemBleRssi";

    public final static String KEY_PREF_MAP_ONLINE1_ENABLE = "prefMapOnline1Enable";
    public final static String KEY_PREF_MAP_ONLINE1_NAME = "prefMapOnline1Name";
    public final static String KEY_PREF_MAP_ONLINE1_FORMAT = "prefMapOnline1Format";
    public final static String KEY_PREF_MAP_ONLINE1_SERVER_A = "prefMapOnline1ServerA";
    public final static String KEY_PREF_MAP_ONLINE1_SERVER_B = "prefMapOnline1ServerB";
    public final static String KEY_PREF_MAP_ONLINE1_SERVER_C = "prefMapOnline1ServerC";
    public final static String KEY_PREF_MAP_ONLINE1_SERVER_PROTOCOL = "prefMapOnline1ServerProtocol";
    public final static String KEY_PREF_MAP_ONLINE1_SERVER_PORT = "prefMapOnline1ServerPort";
    public final static String KEY_PREF_MAP_ONLINE1_SERVER_BASE_URL = "prefMapOnline1ServerBaseUrl";
    public final static String KEY_PREF_MAP_ONLINE1_USER_AGENT = "prefMapOnline1UserAgent";
    public final static String KEY_PREF_MAP_ONLINE1_TILE_SIZE = "prefMapOnline1TileSize";
    public final static String KEY_PREF_MAP_ONLINE1_MIN_ZOOM = "prefMapOnline1MinZoom";
    public final static String KEY_PREF_MAP_ONLINE1_MAX_ZOOM = "prefMapOnline1MaxZoom";
    public final static String KEY_PREF_MAP_ONLINE1_CREDITS = "prefMapOnline1Credits";

    public final static String KEY_PREF_MAP_ONLINE2_ENABLE = "prefMapOnline2Enable";
    public final static String KEY_PREF_MAP_ONLINE2_NAME = "prefMapOnline2Name";
    public final static String KEY_PREF_MAP_ONLINE2_FORMAT = "prefMapOnline2Format";
    public final static String KEY_PREF_MAP_ONLINE2_SERVER_A = "prefMapOnline2ServerA";
    public final static String KEY_PREF_MAP_ONLINE2_SERVER_B = "prefMapOnline2ServerB";
    public final static String KEY_PREF_MAP_ONLINE2_SERVER_C = "prefMapOnline2ServerC";
    public final static String KEY_PREF_MAP_ONLINE2_SERVER_PROTOCOL = "prefMapOnline2ServerProtocol";
    public final static String KEY_PREF_MAP_ONLINE2_SERVER_PORT = "prefMapOnline2ServerPort";
    public final static String KEY_PREF_MAP_ONLINE2_SERVER_BASE_URL = "prefMapOnline2ServerBaseUrl";
    public final static String KEY_PREF_MAP_ONLINE2_USER_AGENT = "prefMapOnline2UserAgent";
    public final static String KEY_PREF_MAP_ONLINE2_TILE_SIZE = "prefMapOnline2TileSize";
    public final static String KEY_PREF_MAP_ONLINE2_MIN_ZOOM = "prefMapOnline2MinZoom";
    public final static String KEY_PREF_MAP_ONLINE2_MAX_ZOOM = "prefMapOnline2MaxZoom";
    public final static String KEY_PREF_MAP_ONLINE2_CREDITS = "prefMapOnline2Credits";

    public final static String KEY_PREF_MAP_ONLINE3_ENABLE = "prefMapOnline3Enable";
    public final static String KEY_PREF_MAP_ONLINE3_NAME = "prefMapOnline3Name";
    public final static String KEY_PREF_MAP_ONLINE3_FORMAT = "prefMapOnline3Format";
    public final static String KEY_PREF_MAP_ONLINE3_SERVER_A = "prefMapOnline3ServerA";
    public final static String KEY_PREF_MAP_ONLINE3_SERVER_B = "prefMapOnline3ServerB";
    public final static String KEY_PREF_MAP_ONLINE3_SERVER_C = "prefMapOnline3ServerC";
    public final static String KEY_PREF_MAP_ONLINE3_SERVER_PROTOCOL = "prefMapOnline3ServerProtocol";
    public final static String KEY_PREF_MAP_ONLINE3_SERVER_PORT = "prefMapOnline3ServerPort";
    public final static String KEY_PREF_MAP_ONLINE3_SERVER_BASE_URL = "prefMapOnline3ServerBaseUrl";
    public final static String KEY_PREF_MAP_ONLINE3_USER_AGENT = "prefMapOnline3UserAgent";
    public final static String KEY_PREF_MAP_ONLINE3_TILE_SIZE = "prefMapOnline3TileSize";
    public final static String KEY_PREF_MAP_ONLINE3_MIN_ZOOM = "prefMapOnline3MinZoom";
    public final static String KEY_PREF_MAP_ONLINE3_MAX_ZOOM = "prefMapOnline3MaxZoom";
    public final static String KEY_PREF_MAP_ONLINE3_CREDITS = "prefMapOnline3Credits";

    Context mContext;
    SharedPreferences mSharedPref;

    public RaPreferences(Context context) {
        this.mContext = context;
        this.mSharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);

        removeDeprecatedPreferences();
        setDefaults();
    }

    public SharedPreferences getSharedPreferences() {
        return mSharedPref;
    }

    private void setStringDefault(String prefKey, String defaultValue) {
        String prefValue = mSharedPref.getString(prefKey, "");
        if (prefValue.isEmpty()) {
            mSharedPref.edit().putString(prefKey, defaultValue).apply();
        }
    }

    private void setDefaults() {
        // Initialize preferences that don't exist yet
        PreferenceManager.setDefaultValues(mContext, R.xml.preferences, false);

        // Determine base path of Ra data directory

        // Fallback: standard external storage
        String raBase = Environment.getExternalStorageDirectory().getPath();

        // Check all "external" data file directories of this application.
        // Choose the (first) one that's not emulated.
        File[] ext_file_directories = mContext.getExternalFilesDirs(null);
        for (File f : ext_file_directories) {
            if (!Environment.isExternalStorageEmulated(f)) {
                raBase = f.getAbsolutePath().split("/Android/data")[0];
                break;
            }
        }

        // All files should be in "ra" directory
        raBase += "/ra";

        // (Large) read-only data goes to SD card

        setStringDefault(KEY_PREF_MAP_PATH, raBase + "/map");
        setStringDefault(KEY_PREF_MAP_THEME_PATH, raBase + "/map/theme");
        setStringDefault(KEY_PREF_MAP_CUSTOM_THEME_FILE, "Elevate4/Elevate.xml");
        setStringDefault(KEY_PREF_MAP_HGT_PATH, raBase + "/hgt");
        setStringDefault(KEY_PREF_MAP_PREDICT_BURST_ALTITUDE, "30000");
        setStringDefault(KEY_PREF_MAP_PREDICT_LANDING_TIME_STYLE, "0");

        setStringDefault(KEY_PREF_SYSTEM_FIRMWARE_PATH, raBase + "/firmware");
        setStringDefault(KEY_PREF_SYSTEM_RINEX_URL, "https://ra.leckasemmel.de/rinex");
        setStringDefault(KEY_PREF_SYSTEM_PREDICT_URL, "https://api.v2.sondehub.org/tawhiri");

        setStringDefault(KEY_PREF_MAP_ONLINE1_NAME, "OpenStreetMap");
        setStringDefault(KEY_PREF_MAP_ONLINE1_FORMAT, "0");
        setStringDefault(KEY_PREF_MAP_ONLINE1_SERVER_A, "a.tile.openstreetmap.org");
        setStringDefault(KEY_PREF_MAP_ONLINE1_SERVER_B, "b.tile.openstreetmap.org");
        setStringDefault(KEY_PREF_MAP_ONLINE1_SERVER_C, "c.tile.openstreetmap.org");
        setStringDefault(KEY_PREF_MAP_ONLINE1_SERVER_PROTOCOL, "https");
        setStringDefault(KEY_PREF_MAP_ONLINE1_SERVER_PORT, "443");
        setStringDefault(KEY_PREF_MAP_ONLINE1_SERVER_BASE_URL, "/");
        setStringDefault(KEY_PREF_MAP_ONLINE1_USER_AGENT, "ra-radiosonde-receiver");
        setStringDefault(KEY_PREF_MAP_ONLINE1_TILE_SIZE, "256");
        setStringDefault(KEY_PREF_MAP_ONLINE1_MIN_ZOOM, "1");
        setStringDefault(KEY_PREF_MAP_ONLINE1_MAX_ZOOM, "19");
        setStringDefault(KEY_PREF_MAP_ONLINE1_CREDITS,
                " &#169; <a href=\"https://openstreetmap.org/copyright\">OpenStreetMap</a> contributors ");

        setStringDefault(KEY_PREF_MAP_ONLINE2_NAME, "OpenTopoMap");
        setStringDefault(KEY_PREF_MAP_ONLINE2_FORMAT, "0");
        setStringDefault(KEY_PREF_MAP_ONLINE2_SERVER_A, "a.tile.opentopomap.org");
        setStringDefault(KEY_PREF_MAP_ONLINE2_SERVER_B, "b.tile.opentopomap.org");
        setStringDefault(KEY_PREF_MAP_ONLINE2_SERVER_C, "c.tile.opentopomap.org");
        setStringDefault(KEY_PREF_MAP_ONLINE2_SERVER_PROTOCOL, "https");
        setStringDefault(KEY_PREF_MAP_ONLINE2_SERVER_PORT, "443");
        setStringDefault(KEY_PREF_MAP_ONLINE2_SERVER_BASE_URL, "/");
        setStringDefault(KEY_PREF_MAP_ONLINE2_USER_AGENT, "ra-radiosonde-receiver");
        setStringDefault(KEY_PREF_MAP_ONLINE2_TILE_SIZE, "256");
        setStringDefault(KEY_PREF_MAP_ONLINE2_MIN_ZOOM, "1");
        setStringDefault(KEY_PREF_MAP_ONLINE2_MAX_ZOOM, "19");
        setStringDefault(KEY_PREF_MAP_ONLINE2_CREDITS,
                " &#169; <a href=\"https://opentopomap.org/credits\">OpenTopoMap</a> | <a href=\"https://openstreetmap.org/copyright\">OpenStreetMap</a> contributors ");

        setStringDefault(KEY_PREF_MAP_ONLINE3_NAME, "Esri World Imagery");
        setStringDefault(KEY_PREF_MAP_ONLINE3_FORMAT, "1");
        setStringDefault(KEY_PREF_MAP_ONLINE3_SERVER_A, "services.arcgisonline.com");
        setStringDefault(KEY_PREF_MAP_ONLINE3_SERVER_B, "services.arcgisonline.com");
        setStringDefault(KEY_PREF_MAP_ONLINE3_SERVER_C, "services.arcgisonline.com");
        setStringDefault(KEY_PREF_MAP_ONLINE3_SERVER_PROTOCOL, "https");
        setStringDefault(KEY_PREF_MAP_ONLINE3_SERVER_PORT, "443");
        setStringDefault(KEY_PREF_MAP_ONLINE3_SERVER_BASE_URL, "/ArcGis/rest/services/World_Imagery/MapServer/tile/");
        setStringDefault(KEY_PREF_MAP_ONLINE3_USER_AGENT, "ra-radiosonde-receiver");
        setStringDefault(KEY_PREF_MAP_ONLINE3_TILE_SIZE, "256");
        setStringDefault(KEY_PREF_MAP_ONLINE3_MIN_ZOOM, "1");
        setStringDefault(KEY_PREF_MAP_ONLINE3_MAX_ZOOM, "20");
        setStringDefault(KEY_PREF_MAP_ONLINE3_CREDITS,
                " &#169; <a href=\"https://www.arcgis.com/home/item.html?id=10df2279f9684e4a9f6a7f08febac2a9\">Esri</a>, Maxar, Earthstar Geographics, and the GIS User Community ");
    }

    private void removeDeprecatedPreferences() {
        String[] deprecatedPrefs = mContext.getResources().getStringArray(R.array.pref_deprecated);
        for (String prefKey : deprecatedPrefs) {
            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.remove(prefKey).apply();
        }
    }

    public Set<String> getMacSet() {
        return mSharedPref.getStringSet(KEY_PREF_BLUETOOTH_MAC_ADDRESSES, null);
    }

    public String getThemeSelect() {
        return mSharedPref.getString(KEY_PREF_LOOK_THEME, "0");
    }

    public String getLookSmeterStyle() {
        return mSharedPref.getString(KEY_PREF_LOOK_SMETER_STYLE, "0");
    }

    public boolean getMapUseHillShading() {
        return mSharedPref.getBoolean(KEY_PREF_MAP_USE_HILL_SHADING, false);
    }

    public String getMapHgtPath() {
        return mSharedPref.getString(KEY_PREF_MAP_HGT_PATH, "");
    }

    public String getMapPath() {
        return mSharedPref.getString(KEY_PREF_MAP_PATH, "");
    }

    public boolean getMapUseCustomTheme() {
        return mSharedPref.getBoolean(KEY_PREF_MAP_USE_CUSTOM_THEME, false);
    }

    public Set<String> getMapFiles() {
        return mSharedPref.getStringSet(KEY_PREF_MAP_FILES, null);
    }

    public String getMapThemePath() {
        return mSharedPref.getString(KEY_PREF_MAP_THEME_PATH, "");
    }

    public String getMapCustomThemeFile() {
        return mSharedPref.getString(KEY_PREF_MAP_CUSTOM_THEME_FILE, "");
    }

    public String getSystemFirmwarePath() {
        return mSharedPref.getString(KEY_PREF_SYSTEM_FIRMWARE_PATH, "");
    }
    public boolean getSystemLogRawFrames() {
        return mSharedPref.getBoolean(KEY_PREF_SYSTEM_LOG_RAW_FRAMES, false);
    }
    public boolean getSystemShowRssiInMap() {
        return mSharedPref.getBoolean(KEY_PREF_SYSTEM_SHOW_RSSI_IN_MAP, false);
    }
    public boolean getSystemShowBleRssi() {
        return mSharedPref.getBoolean(KEY_PREF_SYSTEM_SHOW_BLE_RSSI, false);
    }

    public String getMapPredictBurstAltitude() {
        return mSharedPref.getString(KEY_PREF_MAP_PREDICT_BURST_ALTITUDE, "30000");
    }

    public String getMapPredictLandingTimeStyle() {
        return mSharedPref.getString(KEY_PREF_MAP_PREDICT_LANDING_TIME_STYLE, "0");
    }

    public boolean getMapOnline1Enable() {
        return mSharedPref.getBoolean(KEY_PREF_MAP_ONLINE1_ENABLE, false);
    }
    public String getMapOnline1Name() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE1_NAME, "");
    }
    public int getMapOnline1Format() {
        String s = mSharedPref.getString(KEY_PREF_MAP_ONLINE1_FORMAT, "");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            Log.w(TAG, e.toString());
        }
        return 0;
    }
    public String getMapOnline1ServerA() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE1_SERVER_A, "");
    }
    public String getMapOnline1ServerB() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE1_SERVER_B, "");
    }
    public String getMapOnline1ServerC() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE1_SERVER_C, "");
    }
    public String getMapOnline1ServerProtocol() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE1_SERVER_PROTOCOL, "https");
    }
    public int getMapOnline1ServerPort() {
        String s = mSharedPref.getString(KEY_PREF_MAP_ONLINE1_SERVER_PORT, "");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            Log.w(TAG, e.toString());
        }
        return 443;
    }
    public String getMapOnline1UserAgent() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE1_USER_AGENT, "");
    }
    public String getMapOnline1ServerBaseUrl() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE1_SERVER_BASE_URL, "/");
    }
    public int getMapOnline1TileSize() {
        String s = mSharedPref.getString(KEY_PREF_MAP_ONLINE1_TILE_SIZE, "");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            Log.w(TAG, e.toString());
        }
        return 256;
    }
    public int getMapOnline1MinZoom() {
        String s = mSharedPref.getString(KEY_PREF_MAP_ONLINE1_MIN_ZOOM, "");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            Log.w(TAG, e.toString());
        }
        return 1;
    }
    public int getMapOnline1MaxZoom() {
        String s = mSharedPref.getString(KEY_PREF_MAP_ONLINE1_MAX_ZOOM, "");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            Log.w(TAG, e.toString());
        }
        return 19;
    }
    public String getMapOnline1Credits() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE1_CREDITS, "");
    }

    public boolean getMapOnline2Enable() {
        return mSharedPref.getBoolean(KEY_PREF_MAP_ONLINE2_ENABLE, false);
    }
    public String getMapOnline2Name() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE2_NAME, "");
    }
    public int getMapOnline2Format() {
        String s = mSharedPref.getString(KEY_PREF_MAP_ONLINE2_FORMAT, "");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            Log.w(TAG, e.toString());
        }
        return 0;
    }
    public String getMapOnline2ServerA() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE2_SERVER_A, "");
    }
    public String getMapOnline2ServerB() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE2_SERVER_B, "");
    }
    public String getMapOnline2ServerC() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE2_SERVER_C, "");
    }
    public String getMapOnline2ServerProtocol() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE2_SERVER_PROTOCOL, "https");
    }
    public int getMapOnline2ServerPort() {
        String s = mSharedPref.getString(KEY_PREF_MAP_ONLINE2_SERVER_PORT, "");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            Log.w(TAG, e.toString());
        }
        return 443;
    }
    public String getMapOnline2UserAgent() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE2_USER_AGENT, "");
    }
    public String getMapOnline2ServerBaseUrl() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE2_SERVER_BASE_URL, "/");
    }
    public int getMapOnline2TileSize() {
        String s = mSharedPref.getString(KEY_PREF_MAP_ONLINE2_TILE_SIZE, "");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            Log.w(TAG, e.toString());
        }
        return 256;
    }
    public int getMapOnline2MinZoom() {
        String s = mSharedPref.getString(KEY_PREF_MAP_ONLINE2_MIN_ZOOM, "");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            Log.w(TAG, e.toString());
        }
        return 1;
    }
    public int getMapOnline2MaxZoom() {
        String s = mSharedPref.getString(KEY_PREF_MAP_ONLINE2_MAX_ZOOM, "");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            Log.w(TAG, e.toString());
        }
        return 19;
    }
    public String getMapOnline2Credits() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE2_CREDITS, "");
    }

    public boolean getMapOnline3Enable() {
        return mSharedPref.getBoolean(KEY_PREF_MAP_ONLINE3_ENABLE, false);
    }
    public String getMapOnline3Name() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE3_NAME, "");
    }
    public int getMapOnline3Format() {
        String s = mSharedPref.getString(KEY_PREF_MAP_ONLINE3_FORMAT, "");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            Log.w(TAG, e.toString());
        }
        return 0;
    }
    public String getMapOnline3ServerA() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE3_SERVER_A, "");
    }
    public String getMapOnline3ServerB() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE3_SERVER_B, "");
    }
    public String getMapOnline3ServerC() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE3_SERVER_C, "");
    }
    public String getMapOnline3ServerProtocol() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE3_SERVER_PROTOCOL, "https");
    }
    public int getMapOnline3ServerPort() {
        String s = mSharedPref.getString(KEY_PREF_MAP_ONLINE3_SERVER_PORT, "");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            Log.w(TAG, e.toString());
        }
        return 443;
    }
    public String getMapOnline3UserAgent() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE3_USER_AGENT, "");
    }
    public String getMapOnline3ServerBaseUrl() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE3_SERVER_BASE_URL, "/");
    }
    public int getMapOnline3TileSize() {
        String s = mSharedPref.getString(KEY_PREF_MAP_ONLINE3_TILE_SIZE, "");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            Log.w(TAG, e.toString());
        }
        return 256;
    }
    public int getMapOnline3MinZoom() {
        String s = mSharedPref.getString(KEY_PREF_MAP_ONLINE3_MIN_ZOOM, "");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            Log.w(TAG, e.toString());
        }
        return 1;
    }
    public int getMapOnline3MaxZoom() {
        String s = mSharedPref.getString(KEY_PREF_MAP_ONLINE3_MAX_ZOOM, "");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            Log.w(TAG, e.toString());
        }
        return 19;
    }
    public String getMapOnline3Credits() {
        return mSharedPref.getString(KEY_PREF_MAP_ONLINE3_CREDITS, "");
    }
}
