package de.leckasemmel.sonde1;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import androidx.preference.PreferenceManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;

import static java.lang.Math.round;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

/**
 * Service for transferring ephemerides to the device.
 */
public class EphemerisUpdateService extends Service {
    private final static String TAG = EphemerisUpdateService.class.getName();
    public final static String EXTRA_MAX_LINE = "EXTRA_MAX_LINE";

    private int mStartId;
    private File mRinexFullPath;
    private BLEService mBLEService;
    private int mMaxLineLength;
    private long mDownloadId;

    public final static String ACTION_EPHEMUPDATE_NOTIFY =
            "de.leckasemmel.sonde1.ACTION_EPHEMUPDATE_NOTIFY";
    public final static String EXTRA_EPHEMUPDATE_PROGRESS =
            "de.leckasemmel.sonde1.EXTRA_EPHEMUPDATE_PROGRESS";
    public final static String EXTRA_EPHEMUPDATE_PROGRESS_TITLE =
            "de.leckasemmel.sonde1.EXTRA_EPHEMUPDATE_PROGRESS_TITLE";
    public final static String EXTRA_EPHEMUPDATE_PROGRESS_INFO =
            "de.leckasemmel.sonde1.EXTRA_EPHEMUPDATE_PROGRESS_INFO";
    public final static String EXTRA_EPHEMUPDATE_PROGRESS_SHOW =
            "de.leckasemmel.sonde1.EXTRA_EPHEMUPDATE_PROGRESS_SHOW";

    // Constructor that tells "super" the name of the worker thread
    public EphemerisUpdateService() {
        super();
    }


    public class LocalBinder extends Binder {
        EphemerisUpdateService getService() {
            return EphemerisUpdateService.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final ServiceConnection mBLEServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBLEService = ((BLEService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBLEService = null;
            stopSelf(mStartId);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // In case the system treats the service as sticky (it shouldn't...),
        // we might get called with an empty intent even after we have stopped.
        // Since we do not store intents, an empty intent means we can stop immediately.
        if (intent == null) {
            stopSelf(startId);
        } else {
            mMaxLineLength = intent.getIntExtra(EXTRA_MAX_LINE, 80);
        }

        updateProgress(
                true,
                getString(R.string.ephemupdate_notification_title),
                getString(R.string.ephemupdate_notification_action_fetch_rinex),
                -1);

        mStartId = startId;

        // Bind to the Bluetooth service
        getApplicationContext().bindService(
                new Intent(this, BLEService.class),
                mBLEServiceConnection,
                BIND_AUTO_CREATE);

        // Get current date and time
        Date date = new Date();

        // Go back to UTC minus two hours
        // NOTE: Not really clean. Use TimeZone to set the offset
        Calendar calendar = Calendar.getInstance();
        int offsetMillisecondsToUTC = -(calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET));
        date.setTime(date.getTime() + offsetMillisecondsToUTC - 7200 * 1000);
        Log.d(TAG, String.format(Locale.US, "current time (including offset) = %f", date.getTime() / 86400.0f));

        // Use year and day-of-year to format the URL of the RINEX file
        // (NOTE: DownloadManager doesn't support FTP for the time being)
        SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyy/DDD", Locale.US);
        SimpleDateFormat dateFormat2 = new SimpleDateFormat("'brdc'DDD0.yy'n'", Locale.US);
        String fileName = dateFormat2.format(date);
        String s = dateFormat1.format(date);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String rinexBaseUrl = sharedPref.getString(RaPreferences.KEY_PREF_SYSTEM_RINEX_URL, "https://ra.leckasemmel.de/rinex");
        // Add trailing "/"
        if (!rinexBaseUrl.endsWith("/")) {
            rinexBaseUrl += "/";
        }

        String url = rinexBaseUrl + s + "/" + fileName + ".gz";
        Log.d(TAG, "url = " + url);

        // Check if target file already exists. If so, delete it before starting the download
        // (NOTE: DownloadManager doesn't overwrite existing files, but rather uses a fantasy
        // file name to store the download...)
        File dir;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            dir = getApplicationContext().getExternalFilesDir(null);
        } else {
            dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        }
        mRinexFullPath = new File(dir.getAbsolutePath() + "/" + fileName);
        if (mRinexFullPath.exists()) {
            if (!mRinexFullPath.delete()) {
                Log.w(TAG, "Error deleting " + mRinexFullPath);
            }
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
//            .setDescription("RINEX data file")
//            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
//            .setTitle("RINEX")
            ;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            request.setDestinationInExternalFilesDir(
                    getApplicationContext(),
                    null,
                    fileName);
        } else {
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        }

        // get download service and enqueue file
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(onRinexDownloadComplete, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(onRinexDownloadComplete, filter);
        }
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        mDownloadId = manager.enqueue(request);

        return START_NOT_STICKY;
    }


    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();


    // Handle RINEX download complete
    private final BroadcastReceiver onRinexDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "RINEX download complete");

            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id != mDownloadId) {
                Log.w(TAG, "Wrong DOWNLOAD_ID " + id + ", expected " + mDownloadId);
                return;
            }

            FileInputStream fis;
            InputStream gz_fis;
            try {
                fis = new FileInputStream(mRinexFullPath);
                gz_fis = new GZIPInputStream(fis);
            } catch (FileNotFoundException | SecurityException e) {
                // File not there? Strange.
                Log.w(TAG, "RINEX file " + mRinexFullPath.getAbsolutePath() + " has magically disappeared");
                return;
            } catch (IOException e) {
                // File not there? Strange.
                Log.w(TAG, "RINEX file " + mRinexFullPath.getAbsolutePath() + " not a well-formed GZIP file");
                return;
            }
            int bytesAvailable;
            try {
                bytesAvailable = fis.available();
            } catch (IOException e) {
                bytesAvailable = -1;
            }
            Log.d(TAG, "RINEX file contains " + bytesAvailable + " bytes");

            updateProgress(
                    true,
                    getString(R.string.ephemupdate_notification_title),
                    getString(R.string.ephemupdate_notification_action_process_rinex),
                    -1);

            // Attach a reader
            InputStreamReader isr = null;
            try {
                isr = new InputStreamReader(gz_fis);
            } catch (Exception e) {
                Log.w(TAG, "isr is null");
            }
            // Then a buffered version to read line by line
            BufferedReader br = new BufferedReader(isr);

            Rinex rinex = new Rinex();

            // Read RINEX file line by line.
            // Start with processing the header, then, once the end-of-header marker is found,
            // process the satellites.
            String line;
            int lineNumber = 1;
            while ((line = myReadLine(br)) != null) {
                rinex.processLine(line);
                lineNumber = lineNumber + 1;
            }

/*
            // Debug output: All parsed Rinex info as JSON file
            JSONObject json = new JSONObject();
            try {
                json.put("iono_A0", String.format(Locale.US, "%e", rinex.iono_A0));
                json.put("iono_A1", String.format(Locale.US, "%e", rinex.iono_A1));
                json.put("iono_A2", String.format(Locale.US, "%e", rinex.iono_A2));
                json.put("iono_A3", String.format(Locale.US, "%e", rinex.iono_A3));
                json.put("iono_B0", String.format(Locale.US, "%e", rinex.iono_B0));
                json.put("iono_B1", String.format(Locale.US, "%e", rinex.iono_B1));
                json.put("iono_B2", String.format(Locale.US, "%e", rinex.iono_B2));
                json.put("iono_B3", String.format(Locale.US, "%e", rinex.iono_B3));
                json.put("deltaUTC_A0", String.format(Locale.US, "%e", rinex.deltaUTC_A0));
                json.put("deltaUTC_A1", String.format(Locale.US, "%e", rinex.deltaUTC_A1));
                json.put("deltaUTC_T", String.format(Locale.US, "%d", rinex.deltaUTC_T));
                json.put("deltaUTC_W", String.format(Locale.US, "%d", rinex.deltaUTC_W));
                json.put("leapSeconds", String.format(Locale.US, "%d", rinex.leapSeconds));

                JSONArray satArray = new JSONArray();
                int i;
                for (i = 0; i < rinex.sats.length; i++) {
                    JSONObject sat = new JSONObject();
                    sat.put("year", String.format(Locale.US, "%d", rinex.sats[i].year));
                    sat.put("month", String.format(Locale.US, "%d", rinex.sats[i].month));
                    sat.put("day", String.format(Locale.US, "%d", rinex.sats[i].day));
                    sat.put("hour", String.format(Locale.US, "%d", rinex.sats[i].hour));
                    sat.put("minute", String.format(Locale.US, "%d", rinex.sats[i].minute));
                    sat.put("second", String.format(Locale.US, "%d", rinex.sats[i].second));
                    sat.put("SV_clock_bias", String.format(Locale.US, "%e", rinex.sats[i].SV_clock_bias));
                    sat.put("SV_clock_drift", String.format(Locale.US, "%e", rinex.sats[i].SV_clock_drift));
                    sat.put("SV_clock_drift_rate", String.format(Locale.US, "%e", rinex.sats[i].SV_clock_drift_rate));

                    sat.put("IODE", String.format(Locale.US, "%e", rinex.sats[i].IODE));
                    sat.put("Crs", String.format(Locale.US, "%e", rinex.sats[i].Crs));
                    sat.put("Delta_n", String.format(Locale.US, "%e", rinex.sats[i].Delta_n));
                    sat.put("M0", String.format(Locale.US, "%e", rinex.sats[i].M0));
                    sat.put("Cuc", String.format(Locale.US, "%e", rinex.sats[i].Cuc));
                    sat.put("e", String.format(Locale.US, "%e", rinex.sats[i].e));
                    sat.put("Cus", String.format(Locale.US, "%e", rinex.sats[i].Cus));
                    sat.put("sqrt_A", String.format(Locale.US, "%e", rinex.sats[i].sqrt_A));
                    sat.put("Toe", String.format(Locale.US, "%e", rinex.sats[i].Toe));
                    sat.put("Cic", String.format(Locale.US, "%e", rinex.sats[i].Cic));
                    sat.put("OMEGA", String.format(Locale.US, "%e", rinex.sats[i].OMEGA));
                    sat.put("Cis", String.format(Locale.US, "%e", rinex.sats[i].Cis));
                    sat.put("i0", String.format(Locale.US, "%e", rinex.sats[i].i0));
                    sat.put("Crc", String.format(Locale.US, "%e", rinex.sats[i].Crc));
                    sat.put("omega", String.format(Locale.US, "%e", rinex.sats[i].omega));
                    sat.put("OMEGA_DOT", String.format(Locale.US, "%e", rinex.sats[i].OMEGA_DOT));
                    sat.put("IDOT", String.format(Locale.US, "%e", rinex.sats[i].IDOT));
                    sat.put("CodesL2", String.format(Locale.US, "%e", rinex.sats[i].CodesL2));
                    sat.put("GPSWeek", String.format(Locale.US, "%e", rinex.sats[i].GPSWeek));
                    sat.put("L2P_data_flag", String.format(Locale.US, "%e", rinex.sats[i].L2P_data_flag));
                    sat.put("SV_accuracy", String.format(Locale.US, "%e", rinex.sats[i].SV_accuracy));
                    sat.put("SV_health", String.format(Locale.US, "%e", rinex.sats[i].SV_health));
                    sat.put("TGD", String.format(Locale.US, "%e", rinex.sats[i].TGD));
                    sat.put("IODC", String.format(Locale.US, "%e", rinex.sats[i].IODC));
                    sat.put("TTOM", String.format(Locale.US, "%e", rinex.sats[i].TTOM));
                    sat.put("spare7_2", String.format(Locale.US, "%e", rinex.sats[i].spare7_2));

                    satArray.put(sat);
                }
                json.put("sats", satArray);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                String path = "/storage/emulated/0/ra";
                OutputStreamWriter outputStreamWriter =
                        new OutputStreamWriter(new FileOutputStream(new File(path, "rinex.json")));
                outputStreamWriter.write(json.toString(4));
                outputStreamWriter.close();
            }
            catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
            catch (JSONException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
*/

            // Create a binary image for the target, and calculate the CRC32 checksum of it
            ByteBuffer bb = rinex.makeBinaryImage();

            // Begin with the upload
            Uploader up = new Uploader(bb);
            up.run();

            try {
                fis.close();
            } catch (IOException e) {
                Log.d(TAG, "I/O error when closing RINEX file");
            } finally {
                if (!mRinexFullPath.delete()) {
                    Log.w(TAG, "Error deleting " + mRinexFullPath);
                }
            }
        }

        private String myReadLine(BufferedReader br) {
            String s;

            try {
                s = br.readLine();
            } catch (IOException e) {
                return null;
            }

            return s;
        }
    };

    private void terminate(boolean success) {
        updateProgress(
                true,
                getString(R.string.ephemupdate_notification_title),
                success ? getString(R.string.ephemupdate_notification_result_success) : getString(R.string.ephemupdate_notification_result_failure),
                -1);

        try {
            unregisterReceiver(onRinexDownloadComplete);
            getApplicationContext().unbindService(mBLEServiceConnection);
        }
        catch (IllegalArgumentException e) {
            Log.d(TAG, e.getLocalizedMessage() + " (unregisterReceiver)");
        }
    }

    private void updateProgress(boolean show, CharSequence title, CharSequence text, double progress) {
        Intent intent = new Intent(ACTION_EPHEMUPDATE_NOTIFY);
        intent.putExtra(EXTRA_EPHEMUPDATE_PROGRESS_SHOW, show);
        intent.putExtra(EXTRA_EPHEMUPDATE_PROGRESS_TITLE, title);
        intent.putExtra(EXTRA_EPHEMUPDATE_PROGRESS_INFO, text);
        intent.putExtra(EXTRA_EPHEMUPDATE_PROGRESS, progress);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private static class Rinex {
        private double iono_A0, iono_A1, iono_A2, iono_A3;
        private double iono_B0, iono_B1, iono_B2, iono_B3;
        private double deltaUTC_A0, deltaUTC_A1;
        private int deltaUTC_T, deltaUTC_W;
        private int leapSeconds;

        private static class SatEphem {
            byte year, month, day;
            byte hour, minute, second;
            double SV_clock_bias, SV_clock_drift, SV_clock_drift_rate;

            double IODE, Crs, Delta_n, M0;
            double Cuc, e, Cus, sqrt_A;
            double Toe, Cic, OMEGA, Cis;
            double i0, Crc, omega, OMEGA_DOT;
            double IDOT, CodesL2, GPSWeek, L2P_data_flag;
            double SV_accuracy, SV_health, TGD, IODC;
            double TTOM, spare7_2;

            boolean isNewerThan (SatEphem s) {
                // Calculate hash values from the date/time entries
                double myTime = 0
                        + 86400.0 * (day + 31 * (month + 12 * year))
                        + 3600.0 * hour
                        + 60.0 * minute
                        + second;
                double otherTime = 0
                        + 86400.0 * (s.day + 31 * (s.month + 12 * s.year))
                        + 3600.0 * s.hour
                        + 60.0 * s.minute
                        + s.second;

                return myTime > otherTime;
            }
        }
        private final SatEphem[] sats;
        private SatEphem sat;
        private int PRN;

        private boolean inHeader;
        private int lineCounter;

        private Rinex() {
            sats = new SatEphem[33];
            for (int i = 0; i < sats.length; i++) {
                sats[i] = new SatEphem();
            }

            iono_A0 = iono_A1 = iono_A2 = iono_A3 = 0;
            iono_B0 = iono_B1 = iono_B2 = iono_B3 = 0;
            deltaUTC_A0 = deltaUTC_A1 = 0;
            deltaUTC_T = deltaUTC_W = 0;
            leapSeconds = 0;

            start();
        }

        private void start() {
            inHeader = true;
        }

        private void processLine(String line) {
            Log.d(TAG, "processLine: " + line);

            if (inHeader) {
                if (line.contains("ION ALPHA")) {
                    line = line.replaceAll("D", "e");
                    iono_A0 = Double.parseDouble(line.substring(2, 14).trim());
                    iono_A1 = Double.parseDouble(line.substring(15, 26).trim());
                    iono_A2 = Double.parseDouble(line.substring(27, 38).trim());
                    iono_A3 = Double.parseDouble(line.substring(39, 50).trim());
                }

                if (line.contains("ION BETA")) {
                    line = line.replaceAll("D", "e");
                    iono_B0 = Double.parseDouble(line.substring(2, 14).trim());
                    iono_B1 = Double.parseDouble(line.substring(15, 26).trim());
                    iono_B2 = Double.parseDouble(line.substring(27, 38).trim());
                    iono_B3 = Double.parseDouble(line.substring(39, 50).trim());
                }

                if (line.contains("DELTA-UTC")) {
                    line = line.replaceAll("D", "e");
                    deltaUTC_A0 = Double.parseDouble(line.substring(3, 22).trim());
                    deltaUTC_A1 = Double.parseDouble(line.substring(22, 41).trim());
                    deltaUTC_T = Integer.parseInt(line.substring(41, 50).trim());
                    deltaUTC_W = Integer.parseInt(line.substring(50, 59).trim());
                }

                if (line.contains("LEAP SECONDS")) {
                    line = line.replaceAll("D", "e");
                    leapSeconds = Integer.parseInt(line.substring(1, 7).trim());
                }

                if (line.contains("END OF HEADER")) {
                    inHeader = false;
                    lineCounter = 0;
                    PRN = 0;
                }
            }
            else {
                line = line.replaceAll("D", "e");
                if (line.isEmpty()) {
                    return;
                }
                switch (lineCounter) {
                    case 0:
                        sat = new SatEphem();
                        PRN = Integer.parseInt(line.substring(0,2).trim());
                        if ((PRN < 1) || (PRN > 32)) {
                            PRN = 0;
                        }
                        if (PRN > 0) {
                            sat.year = Byte.parseByte(line.substring(3, 5).trim());
                            sat.month = Byte.parseByte(line.substring(6, 8).trim());
                            sat.day = Byte.parseByte(line.substring(9, 11).trim());
                            sat.hour = Byte.parseByte(line.substring(12, 14).trim());
                            sat.minute = Byte.parseByte(line.substring(15, 17).trim());
                            sat.second = (byte)round(Double.parseDouble(line.substring(18, 20).trim()));
                            sat.SV_clock_bias = Double.parseDouble(line.substring(22, 41).trim());
                            sat.SV_clock_drift = Double.parseDouble(line.substring(41, 60).trim());
                            sat.SV_clock_drift_rate = Double.parseDouble(line.substring(60, 79).trim());
                        }
                        break;

                    case 1:
                        if (PRN > 0) {
                            sat.IODE = Double.parseDouble(line.substring(3, 22).trim());
                            sat.Crs = Double.parseDouble(line.substring(22, 41).trim());
                            sat.Delta_n = Double.parseDouble(line.substring(41, 60).trim());
                            sat.M0 = Double.parseDouble(line.substring(60, 79).trim());
                        }
                        break;

                    case 2:
                        if (PRN > 0) {
                            sat.Cuc = Double.parseDouble(line.substring(3, 22).trim());
                            sat.e = Double.parseDouble(line.substring(22, 41).trim());
                            sat.Cus = Double.parseDouble(line.substring(41, 60).trim());
                            sat.sqrt_A = Double.parseDouble(line.substring(60, 79).trim());
                        }
                        break;

                    case 3:
                        if (PRN > 0) {
                            sat.Toe = Double.parseDouble(line.substring(3, 22).trim());
                            sat.Cic = Double.parseDouble(line.substring(22, 41).trim());
                            sat.OMEGA = Double.parseDouble(line.substring(41, 60).trim());
                            sat.Cis = Double.parseDouble(line.substring(60, 79).trim());
                        }
                        break;

                    case 4:
                        if (PRN > 0) {
                            sat.i0 = Double.parseDouble(line.substring(3, 22).trim());
                            sat.Crc = Double.parseDouble(line.substring(22, 41).trim());
                            sat.omega = Double.parseDouble(line.substring(41, 60).trim());
                            sat.OMEGA_DOT = Double.parseDouble(line.substring(60, 79).trim());
                        }
                        break;

                    case 5:
                        if (PRN > 0) {
                            sat.IDOT = Double.parseDouble(line.substring(3, 22).trim());
                            sat.CodesL2 = Double.parseDouble(line.substring(22, 41).trim());
                            sat.GPSWeek = Double.parseDouble(line.substring(41, 60).trim());
                            sat.L2P_data_flag = Double.parseDouble(line.substring(60, 79).trim());
                        }
                        break;

                    case 6:
                        if (PRN > 0) {
                            sat.SV_accuracy = Double.parseDouble(line.substring(3, 22).trim());
                            sat.SV_health = Double.parseDouble(line.substring(22, 41).trim());
                            sat.TGD = Double.parseDouble(line.substring(41, 60).trim());
                            sat.IODC = Double.parseDouble(line.substring(60, 79).trim());
                        }
                        break;

                    case 7:
                        if (PRN > 0) {
                            sat.TTOM = Double.parseDouble(line.substring(3, 22).trim());
                            sat.spare7_2 = 0;
                            // This field isn't available in all broadcast navigation files!
                            if (line.length() >= 41) {
                                sat.spare7_2 = Double.parseDouble(line.substring(22, 41).trim());
                            }

                            // If this is a newer entry than the one we have stored for the
                            // current satellite, replace the entry
                            if (sat.isNewerThan(sats[PRN])) {
                                sats[PRN] = sat;
                            }
                        }
                        break;
                }

                ++lineCounter;
                if (lineCounter >= 8) {
                    lineCounter = 0;
                }
            }
        }

        private ByteBuffer makeBinaryImage () {
            // Create a new image buffer that holds exactly the amount of bytes
            // in a "GPS_Ephemeris" structure on the target.
            ByteBuffer image = ByteBuffer.allocate(33 * (8 + 29 * 8) + (10 * 8 + 4 * 4));
            image.order(LITTLE_ENDIAN);

            for (int i = 0; i < 33; i++) {
                image.put(sats[i].year);
                image.put(sats[i].month);
                image.put(sats[i].day);
                image.put(sats[i].hour);
                image.put(sats[i].minute);
                image.put(sats[i].second);
                image.put((byte)0);
                image.put((byte)0);

                image.putDouble(sats[i].SV_clock_bias);
                image.putDouble(sats[i].SV_clock_drift);
                image.putDouble(sats[i].SV_clock_drift_rate);

                image.putDouble(sats[i].IODE);
                image.putDouble(sats[i].Crs);
                image.putDouble(sats[i].Delta_n);
                image.putDouble(sats[i].M0);

                image.putDouble(sats[i].Cuc);
                image.putDouble(sats[i].e);
                image.putDouble(sats[i].Cus);
                image.putDouble(sats[i].sqrt_A);

                image.putDouble(sats[i].Toe);
                image.putDouble(sats[i].Cic);
                image.putDouble(sats[i].OMEGA);
                image.putDouble(sats[i].Cis);

                image.putDouble(sats[i].i0);
                image.putDouble(sats[i].Crc);
                image.putDouble(sats[i].omega);
                image.putDouble(sats[i].OMEGA_DOT);

                image.putDouble(sats[i].IDOT);
                image.putDouble(sats[i].CodesL2);
                image.putDouble(sats[i].GPSWeek);
                image.putDouble(sats[i].L2P_data_flag);

                image.putDouble(sats[i].SV_accuracy);
                image.putDouble(sats[i].SV_health);
                image.putDouble(sats[i].TGD);
                image.putDouble(sats[i].IODC);

                image.putDouble(sats[i].TTOM);
                image.putDouble(sats[i].spare7_2);
            }

            image.putDouble(iono_A0);
            image.putDouble(iono_A1);
            image.putDouble(iono_A2);
            image.putDouble(iono_A3);
            image.putDouble(iono_B0);
            image.putDouble(iono_B1);
            image.putDouble(iono_B2);
            image.putDouble(iono_B3);
            image.putDouble(deltaUTC_A0);
            image.putDouble(deltaUTC_A1);
            image.putInt(deltaUTC_T);
            image.putInt(deltaUTC_W);
            image.putInt(leapSeconds);
            image.putInt(0);

            return image;
        }
    }

    // Manage the upload to Ra
    private class Uploader {
        private final ByteBuffer mImage;
        CRC32 mCrc;
        int state;
        int currentPos;
        int totalLength;

        private Uploader (ByteBuffer image) {
            mImage = image;
            state = 0;
            totalLength = mImage.position();

            // Connect to Bluetooth service

            // Calculate CRC of binary image
            mCrc = new CRC32();  //TODO seed?
            mCrc.update(mImage.array());
            Log.w(TAG, "CRC32 of ephem image (" + totalLength + " bytes) is " + mCrc.getValue());
        }

        private void run() {
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mBLEReceiver, new IntentFilter(BLEService.ACTION_DATA_AVAILABLE));

            updateProgress(
                    true,
                    getString(R.string.ephemupdate_notification_title),
                    getString(R.string.ephemupdate_notification_action_connect_device),
                    -1);

            // Start by sending poll command
            mBLEService.send("4,9");
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
                    int n = s.length();
                    if (n > 0) {
if(s.charAt(0)=='#'){
    --n;
    s = s.substring(1);
}
                        // Split comma separated string into fields
                        String[] fields = s.split("[,]");

                        // There must be a minimum of three fields, starting with channel number 4
                        if (fields[0].equals("4") && (fields.length >= 3)) {
                            boolean shouldTerminate = true;
                            boolean success = false;

                            if (fields[1].equals("0") && (state == 1)) {
                                if (Integer.parseInt(fields[2]) == 0) {
                                    updateProgress(true, "", getString(R.string.ephemupdate_notification_action_upload), -1);

                                    shouldTerminate = false;
                                    currentPos = 0;
                                    state = 2;
                                    sendNextHex();
                                }
                            }

                            if (fields[1].equals("1") && (state == 2)) {
                                if (Integer.parseInt(fields[2]) == 0) {
                                    shouldTerminate = false;

                                    if (currentPos < totalLength) {
                                        sendNextHex();
                                    }
                                    else {
                                        updateProgress(
                                                true,
                                                getString(R.string.ephemupdate_notification_title),
                                                getString(R.string.ephemupdate_notification_action_activate),
                                                -1);

                                        state = 3;
                                        String command = "4,2,999";
                                        mBLEService.send(command);
                                    }
                                }
                            }

                            if (fields[1].equals("2") && (state == 3)) {
                                if (Integer.parseInt(fields[2]) == 0) {
                                    success = true;
                                }
                            }

                            if (fields[1].equals("9") && (state == 0)) {
                                long targetCrc = Long.parseLong(fields[2]);
                                if (targetCrc == mCrc.getValue()) {
                                    // Nothing to do
                                    success = true;
                                }
                                else {
                                    state = 1;
                                    String command = "4,0," + totalLength;
                                    mBLEService.send(command);
                                    shouldTerminate = false;
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

        private void sendNextHex() {
            int thisN = totalLength - currentPos;
            int maxN = 16;
            if (mMaxLineLength > 80) {
                maxN = (mMaxLineLength - 40) / 2;
            }
            if (thisN > maxN) {
                thisN = maxN;
            }

            StringBuilder command = new StringBuilder(64);
            command.append(String.format(Locale.US, "4,1,:%02X%04X00", thisN, currentPos));
            int checksum = thisN + (currentPos % 256) + (currentPos / 256);
            for (int i = 0; i < thisN; i++) {
                byte b = mImage.get(currentPos + i);
                checksum += b;
                command.append(String.format(Locale.US, "%02X", b));
            }
            checksum = checksum % 256;
            command.append(String.format(Locale.US, "%02X", (0x100 - checksum) % 256));
            currentPos += thisN;
            mBLEService.send(command.toString());

            updateProgress(
                    true,
                    getString(R.string.ephemupdate_notification_title),
                    getString(R.string.ephemupdate_notification_action_upload),
                    100.0 * (double)currentPos / (double)totalLength);
        }
    }
}
