package de.leckasemmel.sonde1;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class PredictorTawhiri {
    private final static String TAG = PredictorTawhiri.class.getName();

    private long mSondeID;
    private final String predictorUrl;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final OnPredictionResultListener mListener;

    public PredictorTawhiri (final String predictorUrl, @NonNull OnPredictionResultListener listener) {
        this.predictorUrl = predictorUrl;
        this.mListener = listener;
    }

    // Interface to send prediction result to a listener
    public interface OnPredictionResultListener {
        public void onPredictionResult(
                long id,
                LinkedList<SondeListItem.WayPoint> ascent,
                LinkedList<SondeListItem.WayPoint> descent,
                String eta);
    }

    // Trigger a prediction for the given 3D position
    public void doPrediction (long id, double latitude, double longitude,
                              double altitude, double climbRate,
                              double estimatedBurstAltitude,
                              double temperature) {
        // Remember ID of sonde for later
        mSondeID = id;

        // Start time (= current time)
        Date date = new Date();
        SimpleDateFormat dateTimeFormat =
                new SimpleDateFormat("yyyy-MM-dd'T'HH'%3A'mm'%3A'ss", Locale.US);

        // Time zone needs some extra work to replace +/- by %2B/%2D and to add ':'
        SimpleDateFormat timezoneFormat =
                new SimpleDateFormat("ZZZ", Locale.US);
        String timezoneString = timezoneFormat.format(date);

        double burstAltitude = altitude + 1.1;
        if ((climbRate >= 0) && (estimatedBurstAltitude > burstAltitude)) {
            burstAltitude = estimatedBurstAltitude;
        }

        // Expected descent rate at sea level
        if (climbRate >= 0) {
            // Still in ascent. Use a typical descent rate
            //climbRate = -4.0 * Math.exp(8.19e-5 * altitude);
            climbRate = Double.NaN;
        }
        else {
            if (climbRate > -0.2) {
                // Replace a possibly invalid descent rate by the default value
                climbRate = -5.0;
            }
        }

        // Form the request URL
        final String url = predictorUrl
                + String.format(Locale.US,
                    "?launch_latitude=%.5f&launch_longitude=%.5f",
                    latitude,
                    longitude)
                + "&launch_datetime=" + dateTimeFormat.format(date)
                + ((timezoneString.charAt(0) == '+') ? "%2B" : "%2D")
                + timezoneString.substring(1,3) + ":" +  timezoneString.substring(3,5)
                + "&ascent_rate=5.5"    // Use a fixed ascent rate
                + String.format(Locale.US,
                    "&launch_altitude=%.0f&burst_altitude=%.0f",
                    altitude,
                    burstAltitude)
                + String.format(Locale.US,
                    "&descent_rate=%.1f",
                    computeSeaLevelDescentRate(altitude, Math.abs(climbRate), temperature, Double.NaN))
                ;

        Runnable backgroundRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    String jsonString = downloadUrl(url);
                    processJson(jsonString);
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }
            }
        };

        mExecutor.execute(backgroundRunnable);

        Log.d(TAG, "Start download of predictor JSON file @ " + url);
    }

    // Process the received JSON file
    private void processJson (String jsonStr) {
        JSONObject tawhiri;
        try {
            tawhiri = new JSONObject(jsonStr);
        }
        catch (JSONException|NullPointerException e) {
            Log.w(TAG, "JSON parse failed: " + e.getMessage());
            return;
        }

        // Get the prediction array (two elements for ascent and descent)
        JSONArray prediction;
        try {
            prediction = tawhiri.getJSONArray("prediction");
        }
        catch (JSONException e) {
            Log.w(TAG, "JSON parse failed (when getting array 'prediction'): " + e.getMessage());
            return;
        }

        // The descent is the second object in the prediction
        JSONObject ascent;
        JSONObject descent;
        try {
            ascent = prediction.getJSONObject(0);
            descent = prediction.getJSONObject(1);
        }
        catch (JSONException e) {
            Log.w(TAG, "JSON parse failed (getting ascent/descent objects from prediction): " + e.getMessage());
            return;
        }

        // There should be trajectories
        JSONArray ascentTrajectory;
        JSONArray descentTrajectory;
        try {
            ascentTrajectory = ascent.getJSONArray("trajectory");
            descentTrajectory = descent.getJSONArray("trajectory");
        }
        catch (JSONException e) {
            Log.w(TAG, "JSON parse failed (getting trajectory from ascent/descent): " + e.getMessage());
            return;
        }

        // Build a way from the ascent trajectory
        int ascentNumWayPoints = ascentTrajectory.length();
        LinkedList<SondeListItem.WayPoint> ascentWay = new LinkedList<>();
        for (int i = 0; i < ascentNumWayPoints; i++) {
            try {
                SondeListItem.WayPoint point = new SondeListItem.WayPoint();
                point.latitude = ascentTrajectory.getJSONObject(i).getDouble("latitude");
                point.longitude = ascentTrajectory.getJSONObject(i).getDouble("longitude");
                point.altitude = ascentTrajectory.getJSONObject(i).getDouble("altitude");
                ascentWay.add(point);
            }
            catch (JSONException e) {
                Log.w(TAG, String.format(Locale.US, "JSON parse failed (reading ascent trajectory element #%d: ", i) + e.getMessage());
                return;
            }
        }

        // Build a way from the descent trajectory
        int descentNumWayPoints = descentTrajectory.length();
        LinkedList<SondeListItem.WayPoint> descentWay = new LinkedList<>();
        String eta = "";
        for (int i = 0; i < descentNumWayPoints; i++) {
            try {
                SondeListItem.WayPoint point = new SondeListItem.WayPoint();
                point.latitude = descentTrajectory.getJSONObject(i).getDouble("latitude");
                point.longitude = descentTrajectory.getJSONObject(i).getDouble("longitude");
                point.altitude = descentTrajectory.getJSONObject(i).getDouble("altitude");
                descentWay.add(point);
                if (i == descentNumWayPoints - 1) {
                    eta = descentTrajectory.getJSONObject(i).getString("datetime");
                }
            }
            catch (JSONException e) {
                Log.w(TAG, String.format(Locale.US, "JSON parse failed (reading descent trajectory element #%d: ", i) + e.getMessage());
                return;
            }
        }

        // Inform about the completed prediction
        mListener.onPredictionResult(mSondeID, ascentWay, descentWay, eta);
    }

    // From the given descent rate at altitude, compute the expected descent rate at sea level
    private double computeSeaLevelDescentRate (double altitude,
                                               double descentRateAtAltitude,
                                               double temperatureAtAltitude,
                                               double pressureAtAltitude) {

        boolean temperatureFromModel = false;
        if (Double.isNaN(temperatureAtAltitude)) {
            temperatureFromModel = true;
        }
        else {
            if (temperatureAtAltitude < -90.0) {
                temperatureFromModel = true;
            }
        }

        // If temperature value is bad, estimate it for the given altitude using an atmosphere
        // model and a sea level temperature of +15°C
        if (temperatureFromModel) {
            if (altitude <= 11000.0) {
                temperatureAtAltitude = 15.04 - 0.00649 * altitude;
            }
            else if (altitude <= 25000.0) {
                temperatureAtAltitude = -56.46;
            }
            else {
                temperatureAtAltitude = -131.21 + 0.00299 * altitude;
            }
        }

        // If pressure unknown, estimate it for the given altitude using an atmosphere model
        if (Double.isNaN(pressureAtAltitude)) {
            if (altitude <= 11000.0) {
                pressureAtAltitude = 1012.9 * Math.pow((temperatureAtAltitude + 273.1) / 288.08, 5.256);
            }
            else if (altitude <= 25000.0) {
                pressureAtAltitude = 226.5 * Math.exp(1.73 - 0.000157 * altitude);
            }
            else {
                pressureAtAltitude = 24.88 * Math.pow((temperatureAtAltitude + 273.1) / 216.6, -11.388);
            }
        }

        // Air density at altitude
        double density = pressureAtAltitude / (2.869 * (temperatureAtAltitude + 273.1));
        Log.d(TAG, String.format("a=%f, t=%f, p=%f, d=%f, dr=%f", altitude, temperatureAtAltitude, pressureAtAltitude, density, descentRateAtAltitude));

        if (Double.isNaN(descentRateAtAltitude)) {
            return 5.0;
        }

        return descentRateAtAltitude * 0.9054 * Math.sqrt(density);
    }

    // A method to download JSON data from URL
    private String downloadUrl (String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuilder sb  = new StringBuilder();

            String line;
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch(Exception e){
            Log.w("Tawhiri Download", e.toString());
        } finally{
            if (iStream != null) {
                iStream.close();
            }
        }

        return data;
    }
}
