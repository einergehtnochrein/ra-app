package de.leckasemmel.sonde1;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;


public class BLEService extends Service {
    private final static String TAG = BLEService.class.getName();
    private boolean loggingActive = false;

    private String rxCurrent = "";
    private String txCurrent;
    private final LinkedList<String> txFIFO = new LinkedList<>();

    private String[] mPreferredDeviceAddresses; // Array with allowed device addresses
    private String mDeviceAddress;
    private String mDeviceName;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private BluetoothGattCharacteristic mVspCharTx;
    private BluetoothGattCharacteristic mVspCharCts;
    private boolean clearToSend = true;
    private int mMtuSize = 20;
    private int bleRssiThrottle;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String UUID_VSP_SERVICE = "569a1101-b87f-490c-92cb-11ba5ea5167c";
    public final static String UUID_VSP_CHAR_RX = "569a2000-b87f-490c-92cb-11ba5ea5167c";
    public final static String UUID_VSP_CHAR_TX = "569a2001-b87f-490c-92cb-11ba5ea5167c";
    public final static String UUID_VSP_CHAR_CTS = "569a2002-b87f-490c-92cb-11ba5ea5167c";
    //public final static String UUID_VSP_CHAR_RTS = "569a2003-b87f-490c-92cb-11ba5ea5167c";

    public final static String EXTRA_START_MAC =
            "de.leckasemmel.sonde1.INTENT_START_MAC";

    public final static String ACTION_BLE_CONNECTION_STATUS_CHANGE =
            "de.leckasemmel.sonde1.ACTION_BLE_CONNECTION_STATUS_CHANGE";
    public final static String EXTRA_BLE_CONNECTION_STATUS =
            "de.leckasemmel.sonde1.EXTRA_BLE_CONNECTION_STATUS";
    public final static String EXTRA_BLE_RSSI =
            "de.leckasemmel.sonde1.EXTRA_BLE_RSSI";
    public final static String ACTION_DATA_AVAILABLE =
            "de.leckasemmel.sonde1.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_RX_PAYLOAD =
            "de.leckasemmel.sonde1.EXTRA_RX_PAYLOAD";
    public final static String EXTRA_BLE_DEVICE_NAME =
            "de.leckasemmel.sonde1.EXTRA_BLE_DEVICE_NAME";

    public final static int BLE_CONNECTION_STATUS_NO_LINK = 0;
    public final static int BLE_CONNECTION_STATUS_SCANNING = 1;
    //public final static int BLE_CONNECTION_STATUS_DEVICE_CONNECTED = 2;
    public final static int BLE_CONNECTION_STATUS_GATT_CONNECTED = 3;
    public final static int BLE_CONNECTION_STATUS_GATT_DISCONNECTED = 4;
    public final static int BLE_CONNECTION_STATUS_VSP_CONNECTED = 5;
    public final static int BLE_CONNECTION_STATUS_RSSI_UPDATE = 6;


    public BLEService() {
        super();
    }

    // Callback for BLE scan events
    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.w(TAG, "BLE scan callback: " + result.toString());

            // No action if already connected
            if (mDeviceAddress != null) {
                return;
            }

            for (String s : mPreferredDeviceAddresses) {
                if (s.equalsIgnoreCase(result.getDevice().getAddress())) {
                    mDeviceAddress = s;
                    mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
                    Log.w(TAG, "Connecting to Ra device " + mDeviceAddress);

                    if (!connect(mDeviceAddress)) {
                        mDeviceAddress = null;
                    }
                }
            }
        }
    };

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Intent intent = new Intent(ACTION_BLE_CONNECTION_STATUS_CHANGE);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server.");

                mDeviceName = gatt.getDevice().getName();

                mConnectionState = STATE_CONNECTED;
                intent.putExtra(EXTRA_BLE_CONNECTION_STATUS, BLE_CONNECTION_STATUS_GATT_CONNECTED);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

                // Attempts to discover services after successful connection.
                //mBluetoothGatt.requestMtu(247);
                mBluetoothGatt.discoverServices();

                txCurrent = null;
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");

                mDeviceAddress = null;
                mConnectionState = STATE_DISCONNECTED;
                intent.putExtra(EXTRA_BLE_CONNECTION_STATUS, BLE_CONNECTION_STATUS_GATT_DISCONNECTED);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

                disconnect();

                startScan();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "GATT services discovered");

                // Loops through available GATT Services.
                for (BluetoothGattService gattService : gatt.getServices()) {
                    String uuid = gattService.getUuid().toString();
                    if (uuid.equals(UUID_VSP_SERVICE)) {
                        // Loops through available Characteristics.
                        List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                            uuid = gattCharacteristic.getUuid().toString();
                            if (uuid.equals(UUID_VSP_CHAR_RX)) {
                                setCharacteristicNotification(gattCharacteristic, true);
                            }
                            if (uuid.equals(UUID_VSP_CHAR_TX)) {
                                mVspCharTx = gattCharacteristic;
                            }
                            if (uuid.equals(UUID_VSP_CHAR_CTS)) {
                                mVspCharCts = gattCharacteristic;
                                setCharacteristicNotification(gattCharacteristic, true);
                            }
                        }

                        Intent txIntent = new Intent(ACTION_BLE_CONNECTION_STATUS_CHANGE);
                        mConnectionState = STATE_CONNECTED;
                        txIntent.putExtra(EXTRA_BLE_CONNECTION_STATUS, BLE_CONNECTION_STATUS_VSP_CONNECTED);
                        txIntent.putExtra(EXTRA_BLE_DEVICE_NAME, mDeviceName);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(txIntent);
                    }
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            handleTx();
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            mMtuSize = mtu;
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                final Intent intent = new Intent(ACTION_BLE_CONNECTION_STATUS_CHANGE);
                intent.putExtra(EXTRA_BLE_CONNECTION_STATUS, BLE_CONNECTION_STATUS_RSSI_UPDATE);
                intent.putExtra(EXTRA_BLE_RSSI, rssi);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
        }
    };

    private void broadcastUpdate(final BluetoothGattCharacteristic characteristic) {
        String uuid = characteristic.getUuid().toString();
        if (uuid.equals(UUID_VSP_CHAR_RX)) {
            final byte[] data = characteristic.getValue();
            if (data != null) {
                for (byte b : data) {
                    char c = (char) (b & 0xFF);
                    if (c == '\r') {
                        if (!rxCurrent.isEmpty()) {
                            /* Separate checksum part from payload */
                            int checkStart = rxCurrent.lastIndexOf(',');
                            if (checkStart >= 1) {
                                int receivedChecksum;
                                try {
                                    receivedChecksum = Integer.parseInt(rxCurrent.substring(checkStart + 1));
                                } catch (NumberFormatException e) {
                                    receivedChecksum = -1;
                                }
                                /* Remove received checksum from string */
                                rxCurrent = rxCurrent.substring(0, checkStart);

                                /* Calculate checksum of payload */
                                int checksum = ',';
                                for (int i = 0; i < rxCurrent.length(); i++) {
                                    checksum += rxCurrent.charAt(i);
                                }
                                checksum %= 100;

                                if (checksum == receivedChecksum) {
                                    final Intent intent = new Intent(ACTION_DATA_AVAILABLE);
                                    intent.putExtra(EXTRA_RX_PAYLOAD, rxCurrent);
                                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

                                    if (loggingActive && !rxCurrent.startsWith("#3,3,") && !rxCurrent.startsWith("#5,"))
                                        Log.d(TAG, "RX message = " + rxCurrent + " (" + checksum + ")");

                                    if (++bleRssiThrottle > 10) {
                                        bleRssiThrottle = 0;
                                        mBluetoothGatt.readRemoteRssi();
                                    }
                                }
                            }
                            rxCurrent = "";
                        }
                    } else if (rxCurrent.length() < 1500) {
                        rxCurrent += c;
                    }
                }
            }
        }
    }

    public class LocalBinder extends Binder {
        BLEService getService() {
            return BLEService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Initialize Bluetooth environment. Die young if not ready
        BluetoothManager blueManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (blueManager == null) {
            Log.e(TAG, "Unable to initialize BluetoothManager.");
        } else {
            mBluetoothAdapter = blueManager.getAdapter();
            if (mBluetoothAdapter == null) {
                Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            }
        }

        return mBinder;
    }

    protected void setMacAddresses(String[] addresses) {
        if (mBluetoothAdapter != null) {
            mPreferredDeviceAddresses = addresses;
            startScan();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();


    // Start scanning for BLE devices
    private void startScan() {
        if (mPreferredDeviceAddresses.length == 1) {
            connect(mPreferredDeviceAddresses[0]);
            return;
        }

        BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (scanner == null) {
            // Bluetooth not enabled
            Toast.makeText(getApplicationContext(), (R.string.notify_ble_service_disabled), Toast.LENGTH_LONG).show();
            return;
        }

        // Stop any ongoing scan activity
        scanner.stopScan(mScanCallback);

        // Prepare filter and settings


        // Start scan
        ArrayList<ScanFilter> scanFilter = new ArrayList<>();
        scanFilter.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(UUID.fromString(UUID_VSP_SERVICE))).build());
        scanner.startScan(
                scanFilter,
                new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build(),
                mScanCallback
        );
    }


    public boolean connect(final String address) {
        Log.d(TAG, "connect(" + address + ")");


        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
        if (mBluetoothDevice == null) {
            Log.w(TAG, "Invalid device address " + address);
            return false;
        }

        mBluetoothGatt = mBluetoothDevice.connectGatt(this, false, mGattCallback);
        mConnectionState = STATE_CONNECTING;

        return true;
    }


    public void disconnect() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt.disconnect();
        }

        mBluetoothGatt = null;
        mBluetoothDevice = null;

        mDeviceAddress = null;
        mConnectionState = STATE_DISCONNECTED;
    }


    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }


    // Transmit next fragment if possible
    private void handleTx() {
        if ((mBluetoothGatt == null) || (mVspCharTx == null)) { //TODO
            return;
        }

        if (clearToSend) {
            // Check if existing transmission is over now
            if (txCurrent != null) {                // Remainder from previous transmission
                if (txCurrent.length() == 0) {      // Last fragment has just been sent
                    txCurrent = null;               // Done
                }
            }

            // Do we need the next string?
            if (txCurrent == null) {
                txCurrent = txFIFO.poll();          // Pop next string from TX FIFO
                // Throw away if zero length
                if (txCurrent != null) {
                    if (txCurrent.isEmpty()) {
                        txCurrent = null;
                    }
                }
            }

            // Something to send?
            if (txCurrent != null) {
                // Extract first 20 bytes (one VSP fragment)
                int thisN = 20;
                if (txCurrent.length() < thisN) {
                    thisN = txCurrent.length();
                }

                String s = txCurrent.substring(0, thisN);
                txCurrent = txCurrent.substring(thisN);
                if(true){//if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        mVspCharTx.setValue(s);
                        mBluetoothGatt.writeCharacteristic(mVspCharTx);
                    } else {
                        mBluetoothGatt.writeCharacteristic(mVspCharTx, s.getBytes(StandardCharsets.UTF_8), BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    }
                }
            }
        }
    }


    public synchronized void send(String message) {
        // Put message into TX queue if it has enough room left. Otherwise ignore it.
        String txString = "#" + message;
        int checksum = ',';
        for (int i = 0; i < txString.length(); i++) {
            checksum += txString.charAt(i);
        }
        txString += String.format(Locale.US, ",%d\r", checksum % 100);
        if (loggingActive) {
            Log.d(TAG, "SEND: " + txString);
        }
        if (txFIFO.offer(txString)) {
            if (txCurrent == null) {
                handleTx();
            }
        }
    }


    public synchronized void sendRaw(String message) {
        // Put raw characters into TX queue.
        // Used to send dummy characters to be ignored by the receiver
        if (loggingActive) {
            Log.d(TAG, "SEND RAW: " + message);
        }
        if (txFIFO.offer(message)) {
            if (txCurrent == null) {
                handleTx();
            }
        }
    }


    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return;
        }

        if(true){//if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

            if (enabled) {
                List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
                if (!descriptors.isEmpty()) {
                    BluetoothGattDescriptor desc = descriptors.get(0);

                    if(true){//if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            mBluetoothGatt.writeDescriptor(desc);
                        } else {
                            mBluetoothGatt.writeDescriptor(desc, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        }
                    }
                }
            }
        }
    }

    public void setLoggingActive (boolean enable) {
        loggingActive = enable;
    }
}
