package de.leckasemmel.sonde1.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.databinding.BindingAdapter;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.debug.TileCoordinatesLayer;
import org.mapsforge.map.layer.debug.TileGridLayer;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.hills.DemFolderFS;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.layer.hills.MemoryCachingHgtReaderTileSource;
import org.mapsforge.map.layer.hills.SimpleShadingAlgorithm;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Objects;

import de.leckasemmel.sonde1.OnlineTileSourceTMS;

/*
 *  Layers provided by the view:
 *    0: Render layer. A background map can be implemented by providing a properly
 *       organized multi-map data store.
 *    1: Online tile layer
 *    2: Debug layer: tile grid
 *    3: Debug layer: tile numbers
 *    4: MyPosition marker
 *    5+: Sondes
 */

public class RaMapView extends MapView {
    private MultiMapDataStore multiMapDataStore;
    private OnlineTileSourceTMS onlineTileSource;
    private boolean useHillShading;
    private String hillShadingPath;
    private boolean useExternalTheme;
    private String externalThemePath;
    private ArrayList<TileCache>tileCaches;

    private TileRendererLayer tileRendererLayer;
    private TileDownloadLayer tileDownloadLayer;
    private TileGridLayer mGridLayer;
    private TileCoordinatesLayer mTileNumbersLayer;
    private Marker myPositionMarker;
    private int mNumBaseLayers;
    private ArrayList<Layer> sondeLayersPending;
    private int mapMode;    // 0=render layer, 1=online tile layer


    public RaMapView(Context context) {
        super(context);
    }

    public RaMapView(Context context, AttributeSet set)
    {
        super(context, set);
    }

    public void setMyPositionMarkerDrawable(int resourceId) {
        setZoomLevel((byte) 12);

        // Fixed tile size needed for TMS type tile store
        getModel().displayModel.setFixedTileSize(256);
        getModel().displayModel.setUserScaleFactor(1.3f);

        myPositionMarker = new Marker(
                new LatLong(0,0),
                AndroidGraphicFactory.convertToBitmap(
                        Objects.requireNonNull(AppCompatResources.getDrawable(getContext(), resourceId))),
                16, -16);
        myPositionMarker.setVisible(false);
        getLayerManager().getLayers().add(myPositionMarker);
    }

    public void setTileCaches(ArrayList<TileCache>tileCaches) {
        this.tileCaches = tileCaches;
    }

    private void buildRendererLayer() {
        //TODO support hill shading

        // Remove existing layer
        int index = 0;
        if (tileRendererLayer != null) {
            index = getLayerManager().getLayers().indexOf(tileRendererLayer);
            if (index != -1) {
                getLayerManager().getLayers().remove(tileRendererLayer);
            } else {
                index = 0;
            }
            tileRendererLayer = null;
        }

        //tileCaches.get(0).purge();

        // Check prerequisites for renderer layer
        if ((multiMapDataStore != null) && (tileCaches.get(0) != null)) {
            // Prepare hill shading configuration
            HillsRenderConfig hillShadingConfig = null;
            if (useHillShading) {
                File dem = new File(hillShadingPath, "");
                DemFolderFS df = new DemFolderFS(dem);
                MemoryCachingHgtReaderTileSource hillTileSource = new MemoryCachingHgtReaderTileSource(
                        df,
                        new SimpleShadingAlgorithm(),
                        AndroidGraphicFactory.INSTANCE);
                hillTileSource.setEnableInterpolationOverlap(true);
                hillShadingConfig = new HillsRenderConfig(hillTileSource);

                tileRendererLayer = new TileRendererLayer(
                        tileCaches.get(0),
                        multiMapDataStore,
                        getModel().mapViewPosition,
                        false, true, false,
                        AndroidGraphicFactory.INSTANCE,
                        hillShadingConfig);
            } else {
                tileRendererLayer = new TileRendererLayer(
                        tileCaches.get(0),
                        multiMapDataStore,
                        getModel().mapViewPosition,
                        AndroidGraphicFactory.INSTANCE
                );
            }

            // TODO custom theme
            tileRendererLayer.setVisible(mapMode == 0);

            if (useExternalTheme) {
                try {
                    tileRendererLayer.setXmlRenderTheme(new ExternalRenderTheme(externalThemePath));
                } catch (FileNotFoundException e) {
                    tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
                }
            } else {
                tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
            }

            getLayerManager().getLayers().add(index, tileRendererLayer);
        }
    }

    private void buildOnlineTileLayer(int cacheIndex) {
        // Remove existing layer
        int index = 1;
        if (tileDownloadLayer != null) {
            index = getLayerManager().getLayers().indexOf(tileDownloadLayer);
            if (index != -1) {
                getLayerManager().getLayers().remove(tileDownloadLayer);
            } else {
                index = 1;
            }
            tileDownloadLayer.onDestroy();
            tileDownloadLayer = null;
        }

        // Check prerequisites for download layer
        if ((onlineTileSource != null) && (tileCaches.get(cacheIndex) != null)) {
            //tileCaches.get(cacheIndex).purge();

            // Insert new layer at same position in layer stack
            tileDownloadLayer = new TileDownloadLayer(
                    tileCaches.get(cacheIndex),
                    getModel().mapViewPosition,
                    onlineTileSource,
                    AndroidGraphicFactory.INSTANCE
            );

            tileDownloadLayer.setVisible(mapMode == 1);

            // TODO custom theme
            getLayerManager().getLayers().add(index, tileDownloadLayer);
            tileDownloadLayer.start();
        }
    }

    private void buildDebugLayers() {
        mGridLayer = new TileGridLayer(AndroidGraphicFactory.INSTANCE, getModel().displayModel);
        mGridLayer.setVisible(false);
        mTileNumbersLayer = new TileCoordinatesLayer(AndroidGraphicFactory.INSTANCE, getModel().displayModel);
        mTileNumbersLayer.setDrawSimple(true);
        mTileNumbersLayer.setVisible(false);
        getLayerManager().getLayers().add(mGridLayer);
        getLayerManager().getLayers().add(mTileNumbersLayer);
    }

    @BindingAdapter("centerPosition")
    static public void setCenterPosition(RaMapView view, LatLong position) {
        if (position != null) {
            if (!Double.isNaN(position.latitude) && !Double.isNaN(position.longitude)) {
                view.setCenter(position);
            }
        }
    }

    @BindingAdapter("myPosition")
    static public void setMyPosition(RaMapView view, LatLong position) {
        if ((view.myPositionMarker != null) && (position != null)) {
            if (!Double.isNaN(position.latitude) && !Double.isNaN(position.longitude)) {
                view.myPositionMarker.setLatLong(position);
                view.myPositionMarker.setVisible(true, true);
            } else {
                view.myPositionMarker.setVisible(false);
            }
        }
    }

    @BindingAdapter({"multiMapDataStore", "useHillShading", "hillShadingPath", "useExternalTheme", "externalThemePath"})
    static public void setMultiMapDataStore(RaMapView view,
                                            MultiMapDataStore multiMapDataStore,
                                            boolean enableHillShading,
                                            String hillShadingPath,
                                            boolean enableExternalTheme,
                                            String externalThemePath) {
        view.multiMapDataStore = multiMapDataStore;
        view.useHillShading = enableHillShading;
        view.hillShadingPath = hillShadingPath;
        view.useExternalTheme = enableExternalTheme;
        view.externalThemePath = externalThemePath;

        if (view.tileCaches != null) {
            TileCache cache = view.tileCaches.get(0);
            if (cache != null) {
                cache.purge();
            }
        }
        view.buildRendererLayer();
        view.buildDebugLayers();
        view.mNumBaseLayers = view.getLayerManager().getLayers().size();

        if (view.sondeLayersPending != null) {
            view.addSondeLayers(view.sondeLayersPending);
            view.sondeLayersPending = null;
        }
    }

    public void addSondeLayers(ArrayList<Layer> layers) {
        if (layers != null) {
            // Ignore if base layers have not yet been built up
            if (mNumBaseLayers > 0) {
                int numLayers = getLayerManager().getLayers().size();
                for (int i = numLayers - 1; i >= mNumBaseLayers; i--) {
                    getLayerManager().getLayers().remove(i);
                }
                getLayerManager().getLayers().addAll(layers);
            }
        }
    }

    @BindingAdapter({"onlineTileSource", "cacheIndex"})
    static public void setOnlineTileSource(RaMapView view, OnlineTileSourceTMS onlineTileSource, int cacheIndex) {
        view.onlineTileSource = onlineTileSource;
        view.buildOnlineTileLayer(cacheIndex);
    }

    @BindingAdapter("mapMode")
    static public void setMapMode(RaMapView view, int mapMode) {
        view.mapMode = mapMode;
        view.buildRendererLayer();
        view.buildOnlineTileLayer(1);
    }

    @BindingAdapter("sondeLayers")
    static public void setSondeLayers(RaMapView view, ArrayList<Layer> layers) {
        if (view.mNumBaseLayers == 0) {
            view.sondeLayersPending = layers;
        }
        else {
            view.addSondeLayers(layers);
        }
    }

    @BindingAdapter("showDebugLayers")
    static public void setShowDebugLayers(RaMapView view, boolean enable) {
        if (view.mGridLayer != null) {
            view.mGridLayer.setVisible(enable);
        }
        if (view.mTileNumbersLayer != null) {
            view.mTileNumbersLayer.setVisible(enable);
        }
    }
}
