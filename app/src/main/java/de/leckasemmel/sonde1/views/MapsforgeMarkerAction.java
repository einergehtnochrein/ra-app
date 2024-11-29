package de.leckasemmel.sonde1.views;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;

public interface MapsforgeMarkerAction extends Runnable {
    void setTapParameters(LatLong markerLatLong);
}
