package de.leckasemmel.sonde1;

import android.Manifest.permission;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.color.DynamicColors;

import java.util.ArrayList;
import java.util.List;

public class RaApp extends Application {
    public final static String ACTION_APP_BLE_SERVICE_READY =
            "de.leckasemmel.sonde1.ACTION_APP_BLE_SERVICE_READY";

    private BLEService mBleService;
    private RaPreferences mRaPrefs;

    // Give others access to the BLE service
    public BLEService getBleService() {
        return mBleService;
    }

    public RaPreferences getPreferences() {
        return mRaPrefs;
    }

    // Representation of bound BLE service connection
    private final ServiceConnection mBleServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBleService = ((BLEService.LocalBinder) service).getService();
            mBleService.setLoggingActive(true);

            Intent intent = new Intent(ACTION_APP_BLE_SERVICE_READY);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBleService = null;
        }
    };

    // Check permissions required to launch all features of this app.
    // Return empty list if all permissions are granted, return a list of strings representing the
    // missing permissions.
    List<String> checkApplicationPermissions() {
        List<String> missing = new ArrayList<>();

        if (checkSelfPermission(permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            missing.add(permission.ACCESS_FINE_LOCATION);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (checkSelfPermission(permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission.BLUETOOTH_CONNECT);
            }
            if (checkSelfPermission(permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission.BLUETOOTH_SCAN);
            }
        }

        return missing;
    }

    // Start the BLE service. Make sure permissions are ok!
    void launchBleService() {
        Intent gattServiceIntent = new Intent(this, BLEService.class);
        bindService(gattServiceIntent, mBleServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mRaPrefs = new RaPreferences(this);
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
