package de.leckasemmel.sonde1.viewmodels;

import static java.lang.Double.max;
import static java.lang.Double.min;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.header.MapFileException;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import de.leckasemmel.sonde1.OnlineMapDescriptor;
import de.leckasemmel.sonde1.OnlineMapEnableDialog;
import de.leckasemmel.sonde1.OnlineTileSourceTMS;
import de.leckasemmel.sonde1.PredictorTawhiri;
import de.leckasemmel.sonde1.R;
import de.leckasemmel.sonde1.RaPreferences;
import de.leckasemmel.sonde1.SondeListItem;
import de.leckasemmel.sonde1.model.SondeListModel;


public class MapViewModel extends ViewModel
    implements PredictorTawhiri.OnPredictionResultListener {

    public MutableLiveData<SondeListItem> mFocusSonde = new MutableLiveData<>();
    public MutableLiveData<Date> currentDate = new MutableLiveData<>();
    public MutableLiveData<Double> distance = new MutableLiveData<>();
    public MutableLiveData<Integer> landingTimeStyle = new MutableLiveData<>();

    public MutableLiveData<String> sondeSerial = new MutableLiveData<>();
    public MutableLiveData<Double> rssi = new MutableLiveData<>();
    public MutableLiveData<Double> frequency = new MutableLiveData<>();

    public MutableLiveData<LatLong> centerPosition = new MutableLiveData<>(new LatLong(48.13069,11.54594));
    public MutableLiveData<LatLong> areaNorthWest = new MutableLiveData<>();
    public MutableLiveData<LatLong> areaSouthEast = new MutableLiveData<>();

    public MutableLiveData<LatLong> myPosition = new MutableLiveData<>();
    public MutableLiveData<Double> myMslAltitude = new MutableLiveData<>();

    public MutableLiveData<MultiMapDataStore> multiMapDataStore = new MutableLiveData<>();
    public MutableLiveData<Boolean> useHillShading = new MutableLiveData<>();
    public MutableLiveData<String> hillShadingPath = new MutableLiveData<>("");
    public MutableLiveData<Boolean> useExternalTheme = new MutableLiveData<>(false);
    public MutableLiveData<String> externalThemePath = new MutableLiveData<>("");
    public MutableLiveData<Double> predictBurstAltitude = new MutableLiveData<>(30000.0);

    public OnlineMapDescriptor onlineMapDescriptor;
    public MutableLiveData<OnlineTileSourceTMS> onlineTileSource = new MutableLiveData<>();
    public MutableLiveData<Integer> onlineCacheIndex = new MutableLiveData<>();
    public MutableLiveData<Spanned> mapCredits = new MutableLiveData<>();
    private String mapPath;
    public Drawable drawableSonde;
    public Drawable drawablePredictionPos;
    public MutableLiveData<Integer> mapMode = new MutableLiveData<>(0);
    public MutableLiveData<ArrayList<Layer>> sondeLayers = new MutableLiveData<>();
    public MutableLiveData<Boolean> debugGrid = new MutableLiveData<>(false);
    private int onlineMapSelector = -1;
    private RaPreferences mRaPrefs;

    public void setRssi (Double val) { rssi.setValue(val); }
    public void setFrequency (Double val) { frequency.setValue(val); }
    public void setCenterPosition(LatLong pos) { centerPosition.setValue(pos); }
    public void setMyPosition (LatLong pos) {
        myPosition.setValue(pos);

        // Distance to focus sonde
        double d = Double.NaN;
        SondeListItem item = mFocusSonde.getValue();
        if (item != null) {
            SondeListItem.WayPoint point = item.position;
            double lat = point.getLatitude();
            double lon = point.getLongitude();
            if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
                LatLong sondePosition = new LatLong(lat, lon);
                d = sondePosition.sphericalDistance(myPosition.getValue());
            }
        }
        distance.setValue(d);
    }
    public void setMyMslAltitude (Double value) { myMslAltitude.setValue(value); }
    public void setUseHillShading (Boolean enable) { useHillShading.setValue(enable); }
    public void setHillShadingPath (String path) { hillShadingPath.setValue(path); }
    public void setUseExternalTheme (Boolean enable) { useExternalTheme.setValue(enable); }
    public void setExternalThemePath (String path) { externalThemePath.setValue(path); }
    public void setPredictBurstAltitude (Double value) { predictBurstAltitude.setValue(value); }
    public void setDebugGrid (Boolean enable) {
        debugGrid.setValue(enable);
    }
    public void setMapMode (int mode) {
        mapMode.setValue(mode);
    }

    private Boolean getSafeBoolean(MutableLiveData<Boolean> var, Boolean defaultValue) {
        if (var == null) {
            return defaultValue;
        }
        Boolean value = var.getValue();
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public MapViewModel() {
    }

    public void setPreferences(RaPreferences prefs) {
        mRaPrefs = prefs;
    }

    public void updateFromHeardList() {
        SondeListModel heardListModel = SondeListModel.getInstance();

        // Check if focus sonde is set and still in heard list
        SondeListItem focusItem = mFocusSonde.getValue();
        boolean focusSondeInvalid = focusItem == null;
        if (!focusSondeInvalid) {
            focusSondeInvalid = heardListModel.heardListFind(focusItem.id) == null;
        }

        if (focusSondeInvalid) {
            // If only one sonde left in heard list, set focus sonde automatically to this sonde.
            if (heardListModel.getItems().size() == 1) {
                SondeListItem item = heardListModel.getItems().get(0);
                double latitude = item.getLatitude();
                double longitude = item.getLongitude();
                if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
                    setFocusSonde(item);
                    setCenterPosition(new LatLong(latitude, longitude));
                }
            }
        }
        else {
            // Update focus sonde with latest data
            mFocusSonde.setValue(heardListModel.heardListFind(mFocusSonde.getValue().id));
        }

        buildSondeLayers();
    }

    public void setFocusSonde(SondeListItem item) { mFocusSonde.setValue(item); }
    public void setMapPath (String path) {
        mapPath = path;
    }
    public void setLandingTimeStyle (Integer val) {
        landingTimeStyle.setValue(val);
    }

    public void setDrawableSonde (Drawable drawable) {
        drawableSonde = drawable;
    }

    public void setDrawablePredictionPos (Drawable drawable) {
        drawablePredictionPos = drawable;
    }

    public void setMapFiles (Set<String> mapFiles) {
        MultiMapDataStore store = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);
        if (mapFiles != null) {
            for (String mapFile : mapFiles) {
                try {
                    MapDataStore mapDataStore = new MapFile(new File(mapPath, mapFile));
                    store.addMapDataStore(mapDataStore, false, false);
                } catch (MapFileException e) {
                    Log.w(this.getClass().getSimpleName(), String.format(Locale.US, "Cannot find map file %s", mapFile));
                }
            }
        }
        multiMapDataStore.setValue(store);
    }

    public void setOnlineMapDescriptor (@NonNull OnlineMapDescriptor onlineMapDescriptor) {
        this.onlineMapDescriptor = onlineMapDescriptor;
        this.onlineTileSource.setValue(onlineMapDescriptor.tileSource);
        this.mapCredits.setValue(onlineMapDescriptor.credits);
        this.onlineCacheIndex.setValue(onlineMapDescriptor.cacheIndex);
    }


    @Override
    public void onPredictionResult(
            long id,
            LinkedList<SondeListItem.WayPoint> ascentWay,
            LinkedList<SondeListItem.WayPoint> descentWay,
            String eta) {

        // Write ascent and descent ways to the sonde list
        SondeListModel sondeListModel = SondeListModel.getInstance();
        SondeListItem item = sondeListModel.heardListFind(id);
        if (item != null) {
            item.ascentWay = ascentWay;
            item.descentWay = descentWay;
            sondeListModel.postUpdated(true);

            // Convert date/time string
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            try {
                // Time zone should be parsed from string, but sondehub.org uses a variable number
                // of digits for the millisecond field which is difficult to parse with
                // SimpleDateFormat (even crashes on older Android versions).
                // Assume UTC, which is probably safe...
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                item.timeEta = format.parse(eta);
            } catch (ParseException e) {
                Log.w(this.getClass().getSimpleName(), e.toString());
            }
        }
    }

    public boolean onPredictClicked(View view) {
        if (mFocusSonde != null) {
            SondeListItem item = mFocusSonde.getValue();
            if (item != null) {
                SondeListItem.WayPoint point = item.way.peekLast();
                if (point != null) {
                    double descentRate = point.getClimbRate();
                    int N = item.way.size();
                    int numDescentFrames = 0;
                    for (int i = N - 1; i >= 0; i--) {
                        SondeListItem.WayPoint p = item.way.get(i);
                        if (p.getClimbRate() >= 0) {
                            break;
                        }
                        ++numDescentFrames;

                        // Limit to the last x frames
                        if (numDescentFrames >= 10) {
                            break;
                        }
                    }

                    if (numDescentFrames >= 1) {
                        double[] h = new double[numDescentFrames];
                        double[] t = new double[numDescentFrames];

                        for (int i = 0; i < numDescentFrames; i++) {
                            SondeListItem.WayPoint p = item.way.get(N - 1 - i);
                            h[numDescentFrames - 1 - i] = p.getAltitude();
                            t[numDescentFrames - 1 - i] = p.getTimeStamp() / 1e3;
                        }

                        descentRate = (h[0] - h[numDescentFrames - 1])
                                    / (t[0] - t[numDescentFrames - 1]);
                    }

                    PredictorTawhiri predictorTawhiri = new PredictorTawhiri("https://api.v2.sondehub.org/tawhiri", this);
                    predictorTawhiri.doPrediction(
                            item.id,
                            point.getLatitude(),
                            point.getLongitude(),
                            point.getAltitude(),
                            descentRate,
                            predictBurstAltitude.getValue(),
                            item.getTemperature());
                }
            }

            return true;
        }

        return false;
    }

    public boolean onBurstAltitudeLongClicked(View view) {
        Context context = view.getContext();

        final EditText editField = new EditText(context);
        editField.setText(String.format(Locale.US, "%.0f", predictBurstAltitude.getValue()));
        editField.setInputType(InputType.TYPE_CLASS_NUMBER);
        editField.setSelection(editField.getText().length());
        LinearLayout editLayout = new LinearLayout(context);
        editLayout.setOrientation(LinearLayout.VERTICAL);
        editLayout.addView(editField);

        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.fragment_map_dialog_burst_altitude_title))
                .setCancelable(true)
                .setView(editLayout)
                .setPositiveButton(context.getString(R.string.dialog_ok), (dialog, whichButton) -> {
                    predictBurstAltitude.setValue(Double.parseDouble(editField.getText().toString()));
                })
                .setNegativeButton(context.getString(R.string.dialog_cancel), (dialog, whichButton) -> dialog.cancel())
                .show()
                ;

        return true;
    }

    public boolean onPredictLandingTimeLongClicked(View view) {
        Context context = view.getContext();

        String[] items = context.getResources().getStringArray(R.array.map_predict_landing_time_styles);
        int currentValue = Integer.parseInt(mRaPrefs.getMapPredictLandingTimeStyle());

        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.fragment_map_dialog_landing_time_style_title))
                .setCancelable(true)
                .setSingleChoiceItems(items, currentValue, (dialog, which) -> {
                    SharedPreferences.Editor myEdit = mRaPrefs.getSharedPreferences().edit();
                    if (which != currentValue) {
                        String s = String.format(Locale.US, "%d", which);
                        myEdit.putString(RaPreferences.KEY_PREF_MAP_PREDICT_LANDING_TIME_STYLE, s);
                        myEdit.apply();
                    }
                    dialog.cancel();
                })
                .setNegativeButton(context.getString(R.string.dialog_cancel), (dialog, whichButton) -> dialog.cancel())
                .show()
        ;

        return true;
    }

    public boolean onFabNextMapClicked(View view) {
        setNextMap();
        return true;
    }

    public boolean onFabNextMapLongClicked(View view) {
        Context context = view.getContext();

        DialogOnlineMapEnableViewModel viewModel = new DialogOnlineMapEnableViewModel();
        viewModel.nameMap1.setValue(mRaPrefs.getMapOnline1Name());
        viewModel.nameMap2.setValue(mRaPrefs.getMapOnline2Name());
        viewModel.nameMap3.setValue(mRaPrefs.getMapOnline3Name());
        viewModel.enableMap1.setValue(mRaPrefs.getMapOnline1Enable());
        viewModel.enableMap2.setValue(mRaPrefs.getMapOnline2Enable());
        viewModel.enableMap3.setValue(mRaPrefs.getMapOnline3Enable());

        OnlineMapEnableDialog mapEnableDialog = new OnlineMapEnableDialog(context, viewModel, v -> {
            SharedPreferences.Editor myEdit = mRaPrefs.getSharedPreferences().edit();
            myEdit.putBoolean(RaPreferences.KEY_PREF_MAP_ONLINE1_ENABLE, getSafeBoolean(viewModel.enableMap1, false));
            myEdit.putBoolean(RaPreferences.KEY_PREF_MAP_ONLINE2_ENABLE, getSafeBoolean(viewModel.enableMap2, false));
            myEdit.putBoolean(RaPreferences.KEY_PREF_MAP_ONLINE3_ENABLE, getSafeBoolean(viewModel.enableMap3, false));
            myEdit.apply();
        });
        mapEnableDialog.show();

        return true;
    }

    public boolean onFabGotoMyPositionClicked(View view) {
        if (myPosition != null) {
            setCenterPosition(myPosition.getValue());
        }

        return true;
    }

    public boolean onFabGotoSondeClicked(View view) {
        if (mFocusSonde != null) {
            SondeListItem item = mFocusSonde.getValue();
            if (item != null) {
                setCenterPosition(new LatLong(item.getLatitude(), item.getLongitude()));
            }
        }

        return true;
    }

    public boolean onFabGotoSondeLongClicked(View view) {
        // Zoom to area containing all of sonde, own position, and predicted landing position.

        // Need a valid sonde
        if (mFocusSonde != null) {
            SondeListItem item = mFocusSonde.getValue();
            if (item != null) {
                // Determine sonde position
                LatLong sondePos = new LatLong(item.getLatitude(), item.getLongitude());

                // Determine own position (or null)
                LatLong me = null;
                if (myPosition != null) {
                    me = myPosition.getValue();
                }

                // Determine predicted landing position (or null)
                LatLong target = null;
                if (item.descentWay != null) {
                    SondeListItem.WayPoint point = item.descentWay.peekLast();
                    if (point != null) {
                        target = new LatLong(point.getLatitude(), point.getLongitude());
                    }
                }

                // Only sonde position known? Just jump to that position at current zoom level.
                if ((me == null) && (target == null)) {
                    setCenterPosition(sondePos);
                }
                else {
                    // Determine bounding box for all relevant positions
                    double minLat = sondePos.latitude;
                    double maxLat = minLat;
                    double minLon = sondePos.longitude;
                    double maxLon = minLon;
                    if (me != null) {
                        minLat = min(minLat, me.latitude);
                        maxLat = max(maxLat, me.latitude);
                        minLon = min(minLon, me.longitude);
                        maxLon = max(maxLon, me.longitude);
                    }
                    if (target != null) {
                        minLat = min(minLat, target.latitude);
                        maxLat = max(maxLat, target.latitude);
                        minLon = min(minLon, target.longitude);
                        maxLon = max(maxLon, target.longitude);
                    }

                    areaNorthWest.setValue(new LatLong(maxLat, minLon));
                    areaSouthEast.setValue(new LatLong(minLat, maxLon));
                }
            }
        }

        return true;
    }

    public void buildSondeLayers() {
        SondeListModel model = SondeListModel.getInstance();
        List<SondeListItem> items = model.getItems();

        ArrayList<Layer> layers = new ArrayList<>();
        for (SondeListItem item : items) {
            // Balloon symbol at current sonde position
            SondeListItem.WayPoint point = item.way.peekLast();
            if (point != null) {
                double latitude = point.getLatitude();
                double longitude = point.getLongitude();
                if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
                    LatLong position = new LatLong(latitude, longitude);
                    layers.add(new Marker(position, AndroidGraphicFactory.convertToBitmap(drawableSonde), 0, -16));
                }
            }

            // Balloon flight path
            Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
            paint.setStyle(Style.STROKE);
            paint.setColor(Color.BLUE);
            paint.setStrokeWidth(15f);
            Polyline way = new Polyline(paint, AndroidGraphicFactory.INSTANCE, true);
            List<LatLong> coordinates = way.getLatLongs();
            for (SondeListItem.WayPoint p : item.way) {
                if (p.getLatitude() < 90) {
                    coordinates.add(new LatLong(p.getLatitude(), p.getLongitude()));
                }
            }
            layers.add(way);

            // Create a polygon to represent the ascent track and add it to the map
            Paint paintAscent = AndroidGraphicFactory.INSTANCE.createPaint();
            paintAscent.setStyle(Style.STROKE);
            paintAscent.setColor(Color.GREEN);
            paintAscent.setStrokeWidth(10f);
            paintAscent.setDashPathEffect(new float[] {3,20});
            Polyline ascent = new Polyline(paintAscent, AndroidGraphicFactory.INSTANCE, true);
            List<LatLong> coordinatesAscent = ascent.getLatLongs();
            if (item.ascentWay != null) {
                for (SondeListItem.WayPoint p : item.ascentWay) {
                    if (p.getLatitude() < 90) {
                        coordinatesAscent.add(new LatLong(
                                p.getLatitude(), p.getLongitude()));
                    }
                }
            }
            layers.add(ascent);

            // Create a polygon to represent the descent track and add it to the map
            Paint paintDescent = AndroidGraphicFactory.INSTANCE.createPaint();
            paintDescent.setStyle(Style.STROKE);
            paintDescent.setColor(Color.GREEN);
            paintDescent.setStrokeWidth(10f);
            Polyline descent = new Polyline(paintDescent, AndroidGraphicFactory.INSTANCE, true);
            List<LatLong> coordinatesDescent = descent.getLatLongs();
            if (item.descentWay != null) {
                for (SondeListItem.WayPoint p : item.descentWay) {
                    if (p.getLatitude() < 90) {
                        coordinatesDescent.add(new LatLong(
                                p.getLatitude(), p.getLongitude()));
                    }
                }

                // Symbol at predicted landing position
                point = item.descentWay.peekLast();
                if (point != null) {
                    double latitude = point.getLatitude();
                    double longitude = point.getLongitude();
                    if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
                        LatLong position = new LatLong(latitude, longitude);
                        layers.add(new Marker(position, AndroidGraphicFactory.convertToBitmap(drawablePredictionPos), 0, -48));
                    }
                }
            }
            layers.add(descent);
        }

        sondeLayers.setValue(layers);
    }

    // Select the next available map. Cycle through layers:
    // Render -> Online 1 -> Online 2 -> ... -> Render -> ...
    public void setNextMap() {
        Integer mode = mapMode.getValue();
        if (mode != null) {
            if (mode == 0) {
                if (mRaPrefs.getMapOnline1Enable()) {
                    onlineMapSelector = 0;
                }
                else if (mRaPrefs.getMapOnline2Enable()) {
                    onlineMapSelector = 1;
                }
                else if (mRaPrefs.getMapOnline3Enable()) {
                    onlineMapSelector = 2;
                }
            } else {
                // Next online map
                if (onlineMapSelector == 0) {
                    if (mRaPrefs.getMapOnline2Enable()) {
                        onlineMapSelector = 1;
                    }
                    else if (mRaPrefs.getMapOnline3Enable()) {
                        onlineMapSelector = 2;
                    }
                    else {
                        onlineMapSelector = -1;
                        setMapMode(0);
                    }
                }
                else if (onlineMapSelector == 1) {
                    if (mRaPrefs.getMapOnline3Enable()) {
                        onlineMapSelector = 2;
                    }
                    else {
                        onlineMapSelector = -1;
                        setMapMode(0);
                    }
                }
                else {
                    onlineMapSelector = -1;
                    setMapMode(0);
                }
            }

            // Set online map?
            if (onlineMapSelector >= 0) {
                if (onlineMapSelector == 0) {
                    OnlineTileSourceTMS onlineTileSource = new OnlineTileSourceTMS(
                            new String[]{
                                    mRaPrefs.getMapOnline1ServerA(),
                                    mRaPrefs.getMapOnline1ServerB(),
                                    mRaPrefs.getMapOnline1ServerC()},
                            mRaPrefs.getMapOnline1ServerPort())
                            .setName(mRaPrefs.getMapOnline1Name())
                            .setAlpha(false)
                            .setTileSize(mRaPrefs.getMapOnline1TileSize())
                            .setProtocol(mRaPrefs.getMapOnline1ServerProtocol())
                            .setBaseUrl(mRaPrefs.getMapOnline1ServerBaseUrl())
                            .setParallelRequestsLimit(8)
                            .setZoomLevelMax((byte) mRaPrefs.getMapOnline1MaxZoom())
                            .setZoomLevelMin((byte) mRaPrefs.getMapOnline1MinZoom())
                            .setFormat(mRaPrefs.getMapOnline1Format())
                            ;
                    onlineTileSource.setUserAgent(mRaPrefs.getMapOnline1UserAgent());
                    OnlineMapDescriptor onlineMapDescriptor = new OnlineMapDescriptor(
                            onlineTileSource,
                            Html.fromHtml(mRaPrefs.getMapOnline1Credits(), Html.FROM_HTML_MODE_LEGACY),
                            1);
                    setOnlineMapDescriptor(onlineMapDescriptor);
                } else if (onlineMapSelector == 1) {
                    OnlineTileSourceTMS onlineTileSource = new OnlineTileSourceTMS(
                            new String[]{
                                    mRaPrefs.getMapOnline2ServerA(),
                                    mRaPrefs.getMapOnline2ServerB(),
                                    mRaPrefs.getMapOnline2ServerC()},
                            mRaPrefs.getMapOnline2ServerPort())
                            .setName(mRaPrefs.getMapOnline2Name())
                            .setAlpha(false)
                            .setTileSize(mRaPrefs.getMapOnline2TileSize())
                            .setProtocol(mRaPrefs.getMapOnline2ServerProtocol())
                            .setBaseUrl(mRaPrefs.getMapOnline2ServerBaseUrl())
                            .setParallelRequestsLimit(8)
                            .setZoomLevelMax((byte) mRaPrefs.getMapOnline2MaxZoom())
                            .setZoomLevelMin((byte) mRaPrefs.getMapOnline2MinZoom())
                            .setFormat(mRaPrefs.getMapOnline2Format())
                            ;
                    onlineTileSource.setUserAgent(mRaPrefs.getMapOnline2UserAgent());
                    OnlineMapDescriptor onlineMapDescriptor = new OnlineMapDescriptor(
                            onlineTileSource,
                            Html.fromHtml(mRaPrefs.getMapOnline2Credits(), Html.FROM_HTML_MODE_LEGACY),
                            2);
                    setOnlineMapDescriptor(onlineMapDescriptor);
                } else if (onlineMapSelector == 2) {
                    OnlineTileSourceTMS onlineTileSource = new OnlineTileSourceTMS(
                            new String[]{
                                    mRaPrefs.getMapOnline3ServerA(),
                                    mRaPrefs.getMapOnline3ServerB(),
                                    mRaPrefs.getMapOnline3ServerC()},
                            mRaPrefs.getMapOnline3ServerPort())
                            .setName(mRaPrefs.getMapOnline3Name())
                            .setAlpha(false)
                            .setTileSize(mRaPrefs.getMapOnline3TileSize())
                            .setProtocol(mRaPrefs.getMapOnline3ServerProtocol())
                            .setBaseUrl(mRaPrefs.getMapOnline3ServerBaseUrl())
                            .setParallelRequestsLimit(8)
                            .setZoomLevelMax((byte) mRaPrefs.getMapOnline3MaxZoom())
                            .setZoomLevelMin((byte) mRaPrefs.getMapOnline3MinZoom())
                            .setFormat(mRaPrefs.getMapOnline3Format())
                            ;
                    onlineTileSource.setUserAgent(mRaPrefs.getMapOnline3UserAgent());
                    OnlineMapDescriptor onlineMapDescriptor = new OnlineMapDescriptor(
                            onlineTileSource,
                            Html.fromHtml(mRaPrefs.getMapOnline3Credits(), Html.FROM_HTML_MODE_LEGACY),
                            3);
                    setOnlineMapDescriptor(onlineMapDescriptor);
                }

                setMapMode(1);
            }
        }
    }
}
