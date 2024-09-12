package de.leckasemmel.sonde1;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import androidx.core.content.res.ResourcesCompat;
import androidx.core.location.LocationListenerCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import de.leckasemmel.sonde1.databinding.ActivityMainBinding;
import de.leckasemmel.sonde1.fragments.Dashboard;
import de.leckasemmel.sonde1.fragments.FragmentHeard;
import de.leckasemmel.sonde1.fragments.FragmentMap;
import de.leckasemmel.sonde1.model.SondeListModel;
import de.leckasemmel.sonde1.viewmodels.DashboardViewModel;
import de.leckasemmel.sonde1.viewmodels.HeardListViewModel;
import de.leckasemmel.sonde1.viewmodels.MainViewModel;
import de.leckasemmel.sonde1.viewmodels.MapViewModel;


public class MainActivity extends AppCompatActivity
    implements
        View.OnCreateContextMenuListener,
        SharedPreferences.OnSharedPreferenceChangeListener,
        FragmentHeard.SondeActions {
    private final static String TAG = MainActivity.class.getName();

    private static final int PERMISSIONS_REQUEST_ALL = 18;

    private RaApp mApplication;
    private RaPreferences mRaPrefs;
    int mThemeResId;

    MainViewModel mainViewModel;
    StateAdapter mStateAdapter;
    ActivityMainBinding mBinding;
    DashboardViewModel dashboardViewModel;
    MapViewModel mapViewModel;
    SondeListModel heardModel;
    HeardListViewModel heardListViewModel;

    LocationManager locationManager;
    MyLocationListener mLocationListener;

    private int mScanMode;
    private Double mFrequencyRequested;
    private int mDecoder;
    private int mDebugAudio;
    private boolean mMonitorEnabled;
    private MonitorService mMonitorService;
    private BLEService mBleService;
    private RaComm raComm;
    private Timer mKeepAliveTimer;

    private RaComm.TargetInfo mTargetInfo;
    private HashMap<Integer, SondeListItem.Xdata> mXdataCollection;
    ActivityResultLauncher<Intent> mSettingsChangedActivityResultLauncher;

    // Safe conversion from String to double/integer
    private double safeDoubleFromString(String s, double defaultValue) {
        double value;
        try {
            value = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            value = defaultValue;
        }

        return value;
    }

    // Handles local broadcasts
    private final BroadcastReceiver mLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (RaApp.ACTION_APP_BLE_SERVICE_READY.equals(action)) {
                mBleService = mApplication.getBleService();
                if (mBleService != null) {
                    Set<String> macs = mRaPrefs.getMacSet();
                    String[] addresses = macs.toArray(new String[0]);
                    mBleService.setMacAddresses(addresses);
                }
            } else if (BLEService.ACTION_BLE_CONNECTION_STATUS_CHANGE.equals(action)) {
                int status = intent.getIntExtra(BLEService.EXTRA_BLE_CONNECTION_STATUS, BLEService.BLE_CONNECTION_STATUS_NO_LINK);
                switch (status) {
                    case BLEService.BLE_CONNECTION_STATUS_NO_LINK:
                        Log.d(TAG, "BLE link: no link");
                        break;
                    case BLEService.BLE_CONNECTION_STATUS_SCANNING:
                        Log.d(TAG, "BLE link: scanning");
                        break;
                    case BLEService.BLE_CONNECTION_STATUS_GATT_DISCONNECTED:
                        Log.d(TAG, "BLE link: disconnected from GATT");

                        mTargetInfo = new RaComm.TargetInfo();
                        dashboardViewModel.setMonitorSupported(false);
                        dashboardViewModel.setRssi(Double.NaN);
                        dashboardViewModel.setConnected(false);
                        dashboardViewModel.setLastWayPoint(null);
                        dashboardViewModel.setSondeSerial("");
                        mapViewModel.setRssi(Double.NaN);
                        mainViewModel.setRaName(null);
                        break;
                    case BLEService.BLE_CONNECTION_STATUS_GATT_CONNECTED:
                        Log.d(TAG, "BLE link: connected to GATT");
                        break;
                    case BLEService.BLE_CONNECTION_STATUS_VSP_CONNECTED:
                        Log.d(TAG, "BLE link: serial port ready");

                        dashboardViewModel.setConnected(true);

                        mainViewModel.setRaName(intent.getStringExtra(BLEService.EXTRA_BLE_DEVICE_NAME));

                        // Send initial ping with some delay
                        final Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(() -> {
                            // First characters might get lost on Ra side,
                            // so send some dummy characters
                            mBleService.sendRaw("\r\r\r");

                            // Ping the device to get current status information
                            Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
                            long currentTime = calendar.getTimeInMillis() / 1000L;
                            mBleService.send(raComm.makePingResponse(currentTime));
                        }, 1000);
                        break;
                    default:
                        Log.d(TAG, "BLE link: unknown (" + status + ")");
                        break;
                }
            } else if (EphemerisUpdateService.ACTION_EPHEMUPDATE_NOTIFY.equals(action)) {
                mainViewModel.setBleTransferVisible(
                        intent.getBooleanExtra(EphemerisUpdateService.EXTRA_EPHEMUPDATE_PROGRESS_SHOW, false));
                mainViewModel.setBleTransferTitle(
                        intent.getStringExtra(EphemerisUpdateService.EXTRA_EPHEMUPDATE_PROGRESS_TITLE));
                mainViewModel.setBleTransferProgressInfo(
                        intent.getStringExtra(EphemerisUpdateService.EXTRA_EPHEMUPDATE_PROGRESS_INFO));

                double d =
                        intent.getDoubleExtra(EphemerisUpdateService.EXTRA_EPHEMUPDATE_PROGRESS, 0);
                if (d >= 0) {
                    mainViewModel.setBleTransferProgress((int)Math.round(d));
                    mainViewModel.setBleTransferProgressVisible(true);
                } else {
                    mainViewModel.setBleTransferProgressVisible(false);
                }
            } else if (FirmwareUpdateService.ACTION_FIRMWARE_UPDATE_NOTIFY.equals(action)) {
                mainViewModel.setBleTransferVisible(
                        intent.getBooleanExtra(FirmwareUpdateService.EXTRA_FIRMWARE_UPDATE_PROGRESS_SHOW, false));
                mainViewModel.setBleTransferTitle(
                        intent.getStringExtra(FirmwareUpdateService.EXTRA_FIRMWARE_UPDATE_PROGRESS_TITLE));
                mainViewModel.setBleTransferProgressInfo(
                        intent.getStringExtra(FirmwareUpdateService.EXTRA_FIRMWARE_UPDATE_PROGRESS_INFO));

                double d =
                        intent.getDoubleExtra(FirmwareUpdateService.EXTRA_FIRMWARE_UPDATE_PROGRESS, 0);
                if (d >= 0) {
                    mainViewModel.setBleTransferProgress((int)Math.round(d));
                    mainViewModel.setBleTransferProgressVisible(true);
                } else {
                    mainViewModel.setBleTransferProgressVisible(false);
                }
            } else if (BLEService.ACTION_DATA_AVAILABLE.equals(action)) {
                String line = intent.getStringExtra(BLEService.EXTRA_RX_PAYLOAD);
                if (line != null) {
                    Object parsed = raComm.processRxLine(line);

                    if (parsed != null) {
                        if (parsed.getClass() == RaComm.TargetInfo.class) {
                            mTargetInfo = (RaComm.TargetInfo)parsed;
                            dashboardViewModel.setMonitorSupported(mTargetInfo.firmwareMajor >= 54);
                            dashboardViewModel.setLoaderMode(mTargetInfo.isLoader);
                            dashboardViewModel.setLoaderVersion(mTargetInfo.loaderVersion);
                        } else if (parsed.getClass() == SondeListItem.class) {
                            //opcode (1+)2
                            SondeListItem item = (SondeListItem)parsed;
                            SondeListItem existingItem = heardModel.heardListFind(item.id);
                            if (existingItem != null) {
                                if (item.way.isEmpty()) {
                                    item.way = existingItem.way;
                                } else {
                                    existingItem.way.add(item.way.peekLast());
                                    item.way = existingItem.way;
                                }
                                item.ascentWay = existingItem.ascentWay;
                                item.descentWay = existingItem.descentWay;
                                item.timeEta = existingItem.timeEta;

                                if (mXdataCollection == null) {
                                    item.xdata = null;
                                }
                                else {
                                    item.xdata = new HashMap<>(mXdataCollection);
                                }
                                mXdataCollection = null;
                            }
                            item.findDescentRate();
                            heardModel.heardListUpdate(item);
                            heardModel.setUpdated(true);

                            dashboardViewModel.setLastWayPoint(item.way.peekLast());
                            dashboardViewModel.setSondeSerial(item.scalars.name);

                            mapViewModel.updateFromHeardList();
                        } else if (parsed.getClass() == RaComm.RxFrequency.class) {
                            double frequency = ((RaComm.RxFrequency) parsed).frequency;
                            dashboardViewModel.setFrequency(frequency);
                        } else if (parsed.getClass() == RaComm.ScanMode.class) {
                            int mode = ((RaComm.ScanMode) parsed).mode;
                            mScanMode = mode;
                            dashboardViewModel.setMode(mode);
                        } else if (parsed.getClass() == RaComm.Rssi.class) {
                            double rssi = ((RaComm.Rssi) parsed).rssi;
                            dashboardViewModel.setRssi(rssi);
                            mapViewModel.setRssi(rssi);
                        } else if (parsed.getClass() == RaComm.RaBatteryVoltage.class) {
                            double vbat = ((RaComm.RaBatteryVoltage) parsed).vbat;
                            dashboardViewModel.setBatteryVoltage(vbat);
                        } else if (parsed.getClass() == RaComm.DecoderCode.class) {
                            int code = ((RaComm.DecoderCode) parsed).code;
                            mDecoder = code;
                            dashboardViewModel.setDecoder(code);
                        } else if (parsed.getClass() == RaComm.DebugAudioCode.class) {
                            int code = ((RaComm.DebugAudioCode) parsed).code;
                            mDebugAudio = code;
                            dashboardViewModel.setDebugAudio(code);
                        } else if (parsed.getClass() == RaComm.EphemerisAge.class) {
                            String age = ((RaComm.EphemerisAge) parsed).age;
                            SimpleDateFormat dateTimeFormat =
                                    new SimpleDateFormat("yyyy-MM-dd'T'HH':'mm':'ssZ", Locale.US);
                            Date t_oldsat = dateTimeFormat.parse(age, new ParsePosition(0));

                            if (t_oldsat != null) {
                                // Difference to current time
                                Date t_now = new Date();
                                mTargetInfo.ephemerisAge = Math.round((t_now.getTime() - t_oldsat.getTime()) / 1000.0);
                            }
                        } else if (parsed.getClass() == RaComm.AudioMonitorState.class) {
                            if (mMonitorEnabled != ((RaComm.AudioMonitorState) parsed).enable) {
                                mMonitorEnabled = ((RaComm.AudioMonitorState) parsed).enable;
                                dashboardViewModel.setMonitor(mMonitorEnabled);
                                if (!mMonitorEnabled && (mMonitorService != null)) {
                                    mMonitorService.requestStop();
                                }
                            }
                        } else if (parsed.getClass() == RaComm.Spectrum.class) {
                            RaComm.Spectrum spectrum = (RaComm.Spectrum) parsed;
                            dashboardViewModel.setSpectrumLevels(
                                    spectrum.frequency, spectrum.spacing, spectrum.levels
                            );
                        } else if (parsed.getClass() == RaComm.Xdata.class) {
                            if (mXdataCollection == null) {
                                mXdataCollection = new HashMap<>();
                            }
                            RaComm.Xdata xdata = (RaComm.Xdata) parsed;
                            mXdataCollection.put(
                                    xdata.chainPosition,
                                    new SondeListItem.Xdata(xdata.instrument, xdata.message));
                        } else if (parsed.getClass() == RaComm.RawFrameData.class) {
                            RaComm.RawFrameData rawFrameData = (RaComm.RawFrameData) parsed;
                            SondeListItem item = heardModel.heardListFind(rawFrameData.id);
                            if (item != null) {
                                String name = item.getName();
                                if (!name.isEmpty()) {
                                    saveLogLine(
                                            item.getSondeDecoder(),
                                            name,
                                            item.getFrequency(),
                                            rawFrameData.logLine
                                    );
                                }
                            }
                        } else if (parsed.getClass() == RaComm.FirmwareUpdateResponse.class) {
                            RaComm.FirmwareUpdateResponse response = (RaComm.FirmwareUpdateResponse) parsed;
                            if ((response.phase == 0) && (response.param1 == 0)) {
                                mBleService.send("9,99");
                            }
                            if ((response.phase == 0) && (response.param1 == 1)) {
                                selectFirmware();
                            }
                            if (response.phase == 99) {
                                showFirmwareUpdateConfirmationDialog(
                                        response.param1,
                                        response.param2
                                );
                            }
                        } else if (parsed.getClass() == RaComm.AudioSamples.class) {
                            if (mMonitorService != null) {
                                mMonitorService.supplySamples(((RaComm.AudioSamples) parsed).uuEncodedSamples);
                            }
                        }
                    }
                }
            }
        }
    };

    private void saveLogLine(
            SondeListItem.SondeDecoder decoder,
            String name,
            double frequency,
            String logLine) {

        if (!mRaPrefs.getSystemLogRawFrames()) {
            return;
        }

        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
        String fileName = switch (decoder) {
            case SONDE_DECODER_BEACON -> "beacon_" + name + ".txt";
            case SONDE_DECODER_C34_C50 -> "srsc_" + name + ".txt";
            case SONDE_DECODER_GRAW -> "dfm_" + name + ".txt";
            case SONDE_DECODER_IMET ->
                    "imet_" + dateFormat.format(date) + "_" + String.format(Locale.US, "%6.0f", frequency / 1000.0) + ".txt";
            case SONDE_DECODER_IMET54 -> "imet54_" + name + ".txt";
            case SONDE_DECODER_JINYANG -> "jinyang_" + dateFormat.format(date) + ".txt";
            case SONDE_DECODER_M10 ->
                    "m10_" + name.replace("-", "") + "_" + dateFormat.format(date) + ".txt";
            case SONDE_DECODER_M20 ->
                    "m20_" + name.replace("-", "") + "_" + dateFormat.format(date) + ".txt";
            case SONDE_DECODER_MEISEI -> "meisei_" + dateFormat.format(date) + "_" + name + ".txt";
            case SONDE_DECODER_MRZ -> "mrz_" + dateFormat.format(date) + "_" + name + ".txt";
            case SONDE_DECODER_PILOT -> "pilot_" + dateFormat.format(date) + ".txt";
            case SONDE_DECODER_RS41 -> "rs41_" + name + ".txt";
            case SONDE_DECODER_RS92 -> "rs92_" + name + ".txt";
            default -> "";
        };

        // NOTE: No need to support old devices for this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!fileName.isEmpty()) {
                try {
                    Files.write(
                            Paths.get(
                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath(),
                                    "ralog",
                                    fileName),
                            logLine.getBytes(),
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch(IOException e) {
                    Log.d(TAG, "Error writing raw log (" + decoder + ")");
                }
            }
        }
    }
/*
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    mApplication.launchBleService();
                }
            });
*/
    private void selectFirmware() {
        final String firmwarePath = mRaPrefs.getSystemFirmwarePath();
        File directory = new File(firmwarePath);

        final List<String> entries = new ArrayList<>();

        File[] files = directory.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".hex")) {
                    entries.add(f.getName());
                }
            }
        }

        if (entries.isEmpty()) {
            String message = getString(R.string.firmware_update_dialog_no_images) + " " + firmwarePath;
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        } else {
            final CharSequence[] entriesCharArray = entries.toArray(new CharSequence[0]);

            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.firmware_update_dialog_confirmation_title))
                    .setCancelable(true)
                    .setItems(entriesCharArray, (dialogInterface, i) -> {
                        mTargetInfo.firmwareName = firmwarePath + "/" + entriesCharArray[i].toString();
                        Intent intent = new Intent(getApplicationContext(), FirmwareUpdateService.class);
                        intent.putExtra("filename", mTargetInfo.firmwareName);
                        startService(intent);
                    })
                    .setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> {
                    })
                    .create().show();
        }
    }

    public void showFirmwareUpdateConfirmationDialog(final int x1, final int x2) {
        // Valid firmware running. Need to go through challenge-response first
        final View dialogView = this.getLayoutInflater().inflate(R.layout.firmwareupdate, null);
        final EditText responseView = dialogView.findViewById(R.id.firmware_update_response);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setTitle(getString(R.string.firmware_update_dialog_confirmation_title))
                .setMessage(String.format(Locale.US, "%s\n%d + %d = ?", getString(R.string.firmware_update_dialog_confirmation_message), x1, x2))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.firmware_update_dialog_confirmation_button_positive), (dialog, which) -> {
                    String response = responseView.getText().toString();
                    int responseInt;
                    try {
                        responseInt = Integer.parseInt(response);
                    } catch (NumberFormatException e) {
                        responseInt = 0;
                    }
                    if ((responseInt == (x1 + x2)) || (responseInt == 42)) {
                        mBleService.send(String.format(Locale.US, "9,42,%d", x1 + x2));
                        dashboardViewModel.setMonitorSupported(false);
                        dashboardViewModel.setRssi(Double.NaN);
                        dashboardViewModel.setConnected(false);
                    }
                })
                .setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> {
                })
                .create().show();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate(), savedInstanceState = " + savedInstanceState);

        super.onCreate(savedInstanceState);

        mApplication = (RaApp)getApplication();
        mRaPrefs = mApplication.getPreferences();
        mRaPrefs.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        // Set theme selected by preferences
        mThemeResId = R.style.AppTheme;
        if (mRaPrefs.getThemeSelect().equals("1")) {
            mThemeResId = R.style.AppTheme_Dark;
        }
        setTheme(mThemeResId);

        mainViewModel = ViewModelProviders.of(this).get(MainViewModel.class);

        dashboardViewModel = ViewModelProviders.of(this).get(DashboardViewModel.class);
        dashboardViewModel.setPreferences(mRaPrefs);
        dashboardViewModel.frequency_s.observe(this, value -> {
            double d = safeDoubleFromString(value, Double.NaN);
            mFrequencyRequested = safeDoubleFromString(value, Double.NaN);
            if (mFrequencyRequested == 145.25) {
                dashboardViewModel.setDebugEnable(true);
            }
            else {
                mFrequencyRequested = d;
            }
        });
        dashboardViewModel.frequency_updated.observe(this, value -> {
            if (value) {
                dashboardViewModel.setFrequencyUpdated(false);
                if (!Double.isNaN(mFrequencyRequested) && (mBleService != null)) {
                    mBleService.send(raComm.setFrequency(mFrequencyRequested));
                }
            }
        });
        dashboardViewModel.decoder.observe(this, value -> {
            if (value != null) {
                if ((value != mDecoder) && (mBleService != null)) {
                    mDecoder = value;
                    mBleService.send("7,4," + mDecoder);
                }
            }
        });
        dashboardViewModel.debugAudio.observe(this, value -> {
            if (value != null) {
                if ((value != mDebugAudio) && (mBleService != null)) {
                    mDebugAudio = value;
                    mBleService.send("77,13," + mDebugAudio);
                }
            }
        });
        dashboardViewModel.mode.observe(this, value -> {
            if ((value != mScanMode) && (mBleService != null)) {
                mScanMode = value;
                mBleService.send("7,3," + mScanMode);
            }
        });
        dashboardViewModel.monitor.observe(this, value -> {
            if (mBleService != null) {
                mBleService.send(value ? "7,8,1" : "7,8,0");

                Intent intent = new Intent(this, MonitorService.class);
                if (value) {
                    bindService(intent, mMonServiceConnection, Context.BIND_AUTO_CREATE);
                    startService(intent);
                } else {
                    try {
                        unbindService(mMonServiceConnection);
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, e.toString());
                    }
                    stopService(intent);
                }
            }
        });
        dashboardViewModel.spectrumEndFrequency.observe(this, value -> {
            if (mBleService != null) {
                String text = String.format(Locale.US, "7,7,%f,%f",
                        dashboardViewModel.spectrumStartFrequency.getValue(),
                        value);
                mBleService.send(text);
            }
        });

        mapViewModel = ViewModelProviders.of(this).get(MapViewModel.class);
        mapViewModel.setPreferences(mRaPrefs);
        mapViewModel.setDrawableSonde(ResourcesCompat.getDrawable(getResources(), R.drawable.ballon_32, null));
        mapViewModel.setDrawablePredictionPos(ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_location_pin_48, null));
        mapViewModel.predictBurstAltitude.observe(this, value -> {
            SharedPreferences.Editor myEdit = mRaPrefs.getSharedPreferences().edit();
            String newVal = String.format(Locale.US, "%.0f", value);
            if (!newVal.equals(mRaPrefs.getMapPredictBurstAltitude())) {
                myEdit.putString(RaPreferences.KEY_PREF_MAP_PREDICT_BURST_ALTITUDE, String.format(Locale.US, "%.0f", value));
                myEdit.apply();
            }
        });

        heardModel = SondeListModel.getInstance();

        heardListViewModel = ViewModelProviders.of(this).get(HeardListViewModel.class);

        SondeListModel.getInstance().updated.observe(this, aBoolean -> {
            if (aBoolean) {
                SondeListModel.getInstance().updated.setValue(false);
                heardListViewModel.setHeardListUpdated();
                heardListViewModel.heardListEmpty.setValue(heardModel.getNumItems() == 0);
                mapViewModel.updateFromHeardList();
            }
        });

        // Use preferences to configure UI
        processPreference(RaPreferences.KEY_PREF_LOOK_SMETER_STYLE);
        processPreference(RaPreferences.KEY_PREF_MAP_PATH);
        processPreference(RaPreferences.KEY_PREF_MAP_FILES);
        processPreference(RaPreferences.KEY_PREF_MAP_USE_CUSTOM_THEME);
        processPreference(RaPreferences.KEY_PREF_MAP_THEME_PATH);
        processPreference(RaPreferences.KEY_PREF_MAP_CUSTOM_THEME_FILE);
        processPreference(RaPreferences.KEY_PREF_MAP_USE_HILL_SHADING);
        processPreference(RaPreferences.KEY_PREF_MAP_HGT_PATH);
        processPreference(RaPreferences.KEY_PREF_MAP_PREDICT_BURST_ALTITUDE);
        processPreference(RaPreferences.KEY_PREF_MAP_PREDICT_LANDING_TIME_STYLE);
        processPreference(RaPreferences.KEY_PREF_BLUETOOTH_MAC_ADDRESSES);

        raComm = new RaComm();

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBinding.setViewModel(mainViewModel);
        mBinding.setLifecycleOwner(this);

        mStateAdapter = new StateAdapter(this);
        mBinding.viewPager.setAdapter(mStateAdapter);
        mBinding.viewPager.setUserInputEnabled(false);
        mBinding.bottomNavigation.setOnItemSelectedListener(
                item -> {
                    int id = item.getItemId();
                    if (id == R.id.action_overview) {
                        mBinding.viewPager.setCurrentItem(0, true);
                    }
                    if (id == R.id.action_map) {
                        mBinding.viewPager.setCurrentItem(1, true);
                    }
                    if (id == R.id.action_heard_list) {
                        mBinding.viewPager.setCurrentItem(2, true);
                    }
                    return true;
                }
        );

        mBinding.progressBleGroup.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeLeft() {
                mainViewModel.setBleTransferVisible(false);
            }
        });

        setSupportActionBar(mBinding.toolbar);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        AndroidGraphicFactory.createInstance(getApplication());

        mTargetInfo = new RaComm.TargetInfo();

        // Check for required permission(s)
        List<String> missingPermissions = mApplication.checkApplicationPermissions();
        if (missingPermissions.isEmpty()) {
            mApplication.launchBleService();
            enableGps();
        }
        else {
            String[] list = new String[missingPermissions.size()];
            for (int i = 0; i < missingPermissions.size(); i++) {
                list[i] = missingPermissions.get(i);
            }
            requestPermissions(list, PERMISSIONS_REQUEST_ALL);
        }

        final long keepAlivePeriod = 10000;
        if (mKeepAliveTimer == null) {
            mKeepAliveTimer = new Timer();
            mKeepAliveTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (mBleService != null) {
                        mBleService.send("99");
                    }
                }
            }, 0, keepAlivePeriod);
        }

        // Callback for "settings changed"
        mSettingsChangedActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    //TODO
//                    if (result.getResultCode() == Activity.RESULT_OK) {
//                        Intent data = result.getData();
//                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");

        mKeepAliveTimer.cancel();
        mKeepAliveTimer = null;

        disableGps();

        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Intent intent;
        int id = item.getItemId();
        boolean returnValue = true;

        if (id == R.id.action_settings) {
            intent = new Intent(this, SettingsActivity.class);
            intent.putExtra(SettingsActivity.EXTRA_THEME_RESOURCE_ID, mThemeResId);
            mSettingsChangedActivityResultLauncher.launch(intent);
        } else if (id == R.id.action_ephemupdate) {
            // All the nice work of getting RINEX data, converting to the format used by Ra,
            // and the download to Ra are done by a background service.
            intent = new Intent(this, EphemerisUpdateService.class);
            intent.putExtra(EphemerisUpdateService.EXTRA_MAX_LINE, mTargetInfo.firmwareMaxLineLength);
            startService(intent);
        } else if (id == R.id.action_firmware_update) {
            // Send a ping to Ra to determine whether we are already in loader mode
            mBleService.send("9,0");
        } else if (id == R.id.action_about) {
            new AboutDialog(this, mTargetInfo).show();
        } else if (id == R.id.action_fragment_map_debug_grid) {
            // Allow fragments to handle their own stuff
            returnValue = false;
        } else {
            returnValue = super.onOptionsItemSelected(item);
        }

        return returnValue;
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalReceiver);

        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_ALL) {
            boolean result = true;
            for (int thisResult : grantResults) {
                result = result && (thisResult == PackageManager.PERMISSION_GRANTED);
            }

            if (result) {
                if (mApplication.checkApplicationPermissions().isEmpty()) {
                    mApplication.launchBleService();
                    enableGps();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume()");

        mBleService = mApplication.getBleService();

        IntentFilter filter = new IntentFilter();
        filter.addAction(RaApp.ACTION_APP_BLE_SERVICE_READY);
        filter.addAction(BLEService.ACTION_BLE_CONNECTION_STATUS_CHANGE);
        filter.addAction(BLEService.ACTION_DATA_AVAILABLE);
        filter.addAction(EphemerisUpdateService.ACTION_EPHEMUPDATE_NOTIFY);
        filter.addAction(FirmwareUpdateService.ACTION_FIRMWARE_UPDATE_NOTIFY);
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocalReceiver, filter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Uri uri = Uri.parse("package:" + Application.getProcessName());
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
                startActivity(intent);
            }
        }
    }

    private void disableGps() {
        if (locationManager != null) {
            if (mLocationListener != null) {
                locationManager.removeUpdates(mLocationListener);
                mLocationListener = null;
                locationManager = null;
            }
        }
    }

    private void enableGps() {
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            Toast.makeText(getApplicationContext(), (R.string.notify_location_service_not_found), Toast.LENGTH_SHORT).show();
        } else {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(getApplicationContext(), (R.string.notify_gps_service_disabled), Toast.LENGTH_SHORT).show();
            } else {
                try {
                    mLocationListener = new MyLocationListener();
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 2, mLocationListener);
                } catch (SecurityException ex) {
                    Toast.makeText(getApplicationContext(), getString(R.string.notify_gps_service_denied), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onRemoveSondeFromList(SondeListItem item) {
        SondeListItem sondeListItem = heardModel.heardListFind(item.id);
        if (sondeListItem != null) {
            mBleService.send(raComm.ScannerRemoveSonde(sondeListItem));
            heardModel.heardListRemove(item.id);
            heardModel.setUpdated(true);
        }
    }

    @Override
    public void onEnableSondeLogging(SondeListItem item) {
        mBleService.send(String.format(Locale.US, "7,5,1,%d", item.id));
    }

    @Override
    public void onRestart() {
        Log.d(TAG, "onRestart()");

        super.onRestart();
    }

    @Override
    public void onSetFocusSonde(SondeListItem item) {
        mBleService.send(raComm.setDetector(item.getSondeDecoder()));
        mBleService.send(raComm.setFrequency(item.getFrequency() / 1e6));
        mBinding.bottomNavigation.setSelectedItemId(R.id.action_map);
        mapViewModel.setFocusSonde(item);
        mapViewModel.updateFromHeardList();
        mapViewModel.setCenterPosition(new LatLong(item.getLatitude(), item.getLongitude()));
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart()");

        // Make sure sonde layers are rebuilt.
        // If this activity start is because of a configuration change, the existing layers would
        // be presented unchanged to the map view. However, they would already have a redraw engine
        // assigned, which would cause the mapsforge library to throw an exception.
        mapViewModel.updateFromHeardList();

        super.onStart();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop()");

        super.onStop();
    }

    private class MyLocationListener implements LocationListenerCompat {
        @Override
        public void onLocationChanged(Location loc) {
            LatLong position = new LatLong(loc.getLatitude(), loc.getLongitude());
            mapViewModel.setMyPosition(position);

            if (loc.hasAltitude()) {
                mapViewModel.setMyMslAltitude(loc.getAltitude());
            }

            Log.d(TAG, "GPS: onLocationChanged, " + position);
        }
    }

    public class StateAdapter extends FragmentStateAdapter {
        final int PAGE_COUNT = 3;

        public StateAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        @NonNull
        public Fragment createFragment(int position) {
            return switch (position) {
                default -> new Dashboard();
                case 1 -> new FragmentMap();
                case 2 -> new FragmentHeard();
            };
        }

        @Override
        public int getItemCount() {
            return PAGE_COUNT;
        }
    }

    private final ServiceConnection mMonServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mMonitorService = ((MonitorService.MonitorBinder) iBinder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mMonitorService = null;
        }
    };

    // Shared preferences have changed
    @Override
    public void onSharedPreferenceChanged (SharedPreferences sharedPref, String key) {
        processPreference(key);
    }

    // Trigger action for shared preference "key"
    private void processPreference (String key) {
        if (RaPreferences.KEY_PREF_LOOK_SMETER_STYLE.equals(key)) {
            dashboardViewModel.setSmeterStyle(Integer.parseInt(mRaPrefs.getLookSmeterStyle()));
        }
        else if (RaPreferences.KEY_PREF_MAP_USE_HILL_SHADING.equals(key)) {
            mapViewModel.setUseHillShading(mRaPrefs.getMapUseHillShading());
        }
        else if (RaPreferences.KEY_PREF_MAP_HGT_PATH.equals(key)) {
            mapViewModel.setHillShadingPath(mRaPrefs.getMapHgtPath());
        }
        else if (RaPreferences.KEY_PREF_MAP_PATH.equals(key)) {
            mapViewModel.setMapPath(mRaPrefs.getMapPath());
        }
        else if (RaPreferences.KEY_PREF_MAP_FILES.equals(key)) {
            mapViewModel.setMapFiles(mRaPrefs.getMapFiles());
        }
        else if (RaPreferences.KEY_PREF_MAP_USE_CUSTOM_THEME.equals(key)) {
            mapViewModel.setUseExternalTheme(mRaPrefs.getMapUseCustomTheme());
        }
        else if (RaPreferences.KEY_PREF_MAP_THEME_PATH.equals(key) ||
                 RaPreferences.KEY_PREF_MAP_CUSTOM_THEME_FILE.equals(key)) {
            String customThemeName =
                    mRaPrefs.getMapThemePath()
                            + "/"
                            + mRaPrefs.getMapCustomThemeFile();
            mapViewModel.setExternalThemePath(customThemeName);
        }
        else if (RaPreferences.KEY_PREF_MAP_PREDICT_BURST_ALTITUDE.equals(key)) {
            Double d = safeDoubleFromString(mRaPrefs.getMapPredictBurstAltitude(), 30000.0);
            mapViewModel.setPredictBurstAltitude(d);
        }
        else if (RaPreferences.KEY_PREF_MAP_PREDICT_LANDING_TIME_STYLE.equals(key)) {
            mapViewModel.setLandingTimeStyle(Integer.parseInt(mRaPrefs.getMapPredictLandingTimeStyle()));
        }
        else if (RaPreferences.KEY_PREF_BLUETOOTH_MAC_ADDRESSES.equals(key)) {
            if (mBleService != null) {
                mBleService.disconnect();
                mTargetInfo = new RaComm.TargetInfo();
                dashboardViewModel.setMonitorSupported(false);
                dashboardViewModel.setRssi(Double.NaN);
                dashboardViewModel.setConnected(false);
                dashboardViewModel.setLastWayPoint(null);
                dashboardViewModel.setSondeSerial("");
                dashboardViewModel.setFrequency(Double.NaN);
                mapViewModel.setRssi(Double.NaN);
                mainViewModel.setRaName(null);

                Set<String> macs = mRaPrefs.getMacSet();
                String[] addresses = macs.toArray(new String[0]);
                mBleService.setMacAddresses(addresses);
            }
        }
    }
}
