/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.leckasemmel.sonde1;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.Locale;
import java.util.zip.CRC32;


/**
 * Service for transferring ephemerides to the device.
 */
public class FirmwareUpdateService extends Service {
    private final static String TAG = FirmwareUpdateService.class.getName();

    private int mStartId;
    private BLEService mBLEService;
    private File mFirmwareFile;

    public final static String ACTION_FIRMWARE_UPDATE_NOTIFY =
            "de.leckasemmel.sonde1.ACTION_FIRMWARE_UPDATE_NOTIFY";
    public final static String EXTRA_FIRMWARE_UPDATE_PROGRESS_TITLE =
            "de.leckasemmel.sonde1.EXTRA_FIRMWARE_UPDATE_PROGRESS_TITLE";
    public final static String EXTRA_FIRMWARE_UPDATE_PROGRESS =
            "de.leckasemmel.sonde1.EXTRA_FIRMWARE_UPDATE_PROGRESS";
    public final static String EXTRA_FIRMWARE_UPDATE_PROGRESS_INFO =
            "de.leckasemmel.sonde1.EXTRA_FIRMWARE_UPDATE_PROGRESS_INFO";
    public final static String EXTRA_FIRMWARE_UPDATE_PROGRESS_SHOW =
            "de.leckasemmel.sonde1.EXTRA_FIRMWARE_UPDATE_PROGRESS_SHOW";

    // Constructor that tells "super" the name of the worker thread
    public FirmwareUpdateService()
    {
        super();
    }


    public class LocalBinder extends Binder {
        FirmwareUpdateService getService() {
            return FirmwareUpdateService.this;
        }
    }


    private String myReadLine (BufferedReader br) {
        String s;

        try {
            s = br.readLine();
        } catch (IOException e) {
            return null;
        }

        return s;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final ServiceConnection mBLEServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBLEService = ((BLEService.LocalBinder) service).getService();

            FileInputStream fis;
            try {
                fis = new FileInputStream(mFirmwareFile);

            } catch (FileNotFoundException e) {
                // File not there? Strange.
                Log.w(TAG, "Firmware file " + mFirmwareFile.getAbsolutePath() + " has magically disappeared");
                return;
            }

            FileChannel fc;
            try {
                fc = fis.getChannel();
            } catch(Exception e) {
                return;
            }

            long bytesAvailable;
            try {
                bytesAvailable = fc.size();
            } catch (IOException e) {
                bytesAvailable = -1;
            }
            Log.d(TAG, "Firmware file contains " + bytesAvailable + " bytes");

            // Attach a reader
            InputStreamReader isr = new InputStreamReader(fis);
            // Then a buffered version to read line by line
            BufferedReader br = new BufferedReader(isr);

            // Begin with the upload
            Uploader mUp = new Uploader(br, (int)bytesAvailable);
            mUp.run();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName){
            mBLEService = null;
            stopSelf(mStartId);
        }
    };

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        // In case the system treats the service as sticky (it shouldn't...),
        // we might get called with an empty intent even after we have stopped.
        // Since we do not store intents, an empty intent means we can stop immediately.
        if (intent == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        // Does the file name point to an existing file?
        Uri uri = Uri.parse(intent.getStringExtra("filename"));
        mFirmwareFile = new File(uri.getPath());
        if (!mFirmwareFile.exists()) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        updateProgress(
                true,
                getString(R.string.firmware_update_notification_title),
                getString(R.string.firmware_update_notification_read_hex),
                -1);

        mStartId = startId;

        // Bind to the Bluetooth service
        getApplicationContext().bindService(
                new Intent(this, BLEService.class),
                mBLEServiceConnection,
                BIND_AUTO_CREATE);

        return START_NOT_STICKY;
    }


    private void updateProgress(boolean show, CharSequence title, CharSequence text, double progress) {
        Intent intent = new Intent(ACTION_FIRMWARE_UPDATE_NOTIFY);
        intent.putExtra(EXTRA_FIRMWARE_UPDATE_PROGRESS_SHOW, show);
        intent.putExtra(EXTRA_FIRMWARE_UPDATE_PROGRESS_TITLE, title);
        intent.putExtra(EXTRA_FIRMWARE_UPDATE_PROGRESS_INFO, text);
        intent.putExtra(EXTRA_FIRMWARE_UPDATE_PROGRESS, progress);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();



    private void terminate(boolean success) {
        String successMessage = getString(R.string.firmware_update_notification_result_success);
        if (!success) {
            successMessage = getString(R.string.firmware_update_notification_result_failure);
        }
        updateProgress(
                true,
                getString(R.string.firmware_update_notification_title),
                successMessage,
                -1);

        getApplicationContext().unbindService(mBLEServiceConnection);
    }

    // Manage the upload to Ra
    private class Uploader {
        CRC32 mCrc;
        int state;
        int currentPos;
        int totalLength;
        BufferedReader mReader;
        long page;

        private Uploader (BufferedReader reader, int size) {
            state = 0;
            page = 0;
            mReader = reader;
            totalLength = size;
            currentPos = 0;

            // Calculate CRC of binary image
            mCrc = new CRC32();
        }

        private void run() {
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mBLEReceiver, new IntentFilter(BLEService.ACTION_DATA_AVAILABLE));

            updateProgress(
                    true,
                    getString(R.string.firmware_update_notification_title),
                    getString(R.string.firmware_update_notification_action_connect_device),
                    -1);

            // Start by sending poll command
            state = 1;
            String command = "9,1";
            mBLEService.send(command);
        }

        private void stop(boolean success) {
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mBLEReceiver);

            terminate(success);

            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> updateProgress(false, "", "", -1), 2000);
        }

        // Listen to events from Bluetooth service (data from device)
        private final BroadcastReceiver mBLEReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (BLEService.ACTION_DATA_AVAILABLE.equals(action)) {
                    String s = intent.getStringExtra(BLEService.EXTRA_RX_PAYLOAD);
                    assert s != null;
                    int n = s.length();
                    if (n > 0) {
                        // Split comma separated string into fields
                        String[] fields = s.split(",");

                        // There must be a minimum of three fields, starting with channel number 4
                        if (fields[0].equals("#9") && (fields.length >= 3)) {
                            boolean shouldTerminate = true;
                            boolean success = false;

                            if (fields[1].equals("1") && (state == 1)) {
                                if (Integer.parseInt(fields[2]) == 0) {
                                    updateProgress(
                                            true,
                                            getString(R.string.firmware_update_notification_title),
                                            getString(R.string.firmware_update_notification_action_upload),
                                            -1);

                                    shouldTerminate = false;
                                    currentPos = 0;
                                    state = 2;
                                    sendNextHex();
                                }
                            }

                            if (fields[1].equals("2") && (state == 2)) {
                                if (Integer.parseInt(fields[2]) == 0) {
                                    shouldTerminate = false;

                                    if (!sendNextHex()) {
                                        updateProgress(
                                                true,
                                                getString(R.string.firmware_update_notification_title),
                                                getString(R.string.firmware_update_notification_action_activate),
                                                -1);

                                        state = 3;
                                        String command = String.format(Locale.US, "9,3,%d", mCrc.getValue());
                                        mBLEService.send(command);
                                    }
                                }
                            }

                            if (fields[1].equals("3") && (state == 3)) {
                                if (Long.parseLong(fields[2]) == 0) {
                                    success = true;
                                }
                            }

                            if (shouldTerminate) {
                                stop(success);
                            }
                        }
                    }
                }
            }
        };

        private boolean sendNextHex() {
            // Simple approach:
            // A zero-length line indicates end of file
            // Any non-hex line (not starting with ':', invalid in any other way) indicates
            // end of file
            boolean haveData = false;
            String line;
            do {
                line = myReadLine(mReader);
                if (line != null) {
                    currentPos += line.length() + 1;
                    if (line.length() >= 11) {
                        if (line.charAt(0) == ':') {
                            String type = line.substring(7,9);
                            if (type.equals("00")) {
                                // Data. Update CRC before sending
                                int length = Integer.parseInt(line.substring(1,3), 16);
                                String data = line.substring(9,9+2*length);
                                byte[] dataBytes = new byte[length];
                                for (int i = 0; i < length; i++) {
                                    dataBytes[i] = (byte)
                                            ((Character.digit(data.charAt(2*i),   16) << 4)
                                            + Character.digit(data.charAt(2*i+1), 16));
                                }
                                mCrc.update(dataBytes);

                                haveData = true;
                                break;
                            }
                            if (type.equals("01")) {
                                //end record
                                break;
                            }
                            if (type.equals("02")) {
                                // Page
                                haveData = true;
                                page = Integer.parseInt(line.substring(9,13));
                            }
                        }
                    }
                }
            } while (!haveData);

            if (haveData) {
                String command = String.format(Locale.US, "9,2,%s", line);
                mBLEService.send(command);

                double d = 100.0 * (double)currentPos / (double)totalLength;
                updateProgress(
                        true,
                        getString(R.string.firmware_update_notification_title),
                        String.format(Locale.US, "%.1f %%", d), d);

                return true;
            }

            return false;
        }
    }
}
