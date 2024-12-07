package de.leckasemmel.sonde1.views;

import android.graphics.drawable.Drawable;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.overlay.Marker;

public class MapsforgeTappableMarker extends Marker {
    MapsforgeMarkerAction tapAction;

    public MapsforgeTappableMarker(LatLong latLong,
                                   Drawable drawable,
                                   int horizontalOffset, int verticalOffset) {
        super(latLong,
              AndroidGraphicFactory.convertToBitmap(drawable),
              horizontalOffset, verticalOffset);
    }

    public void setTapAction (MapsforgeMarkerAction action) {
        this.tapAction = action;
    }

    @Override
    public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
        if (tapAction != null) {
            double centerX = layerXY.x + getHorizontalOffset();
            double centerY = layerXY.y + getVerticalOffset();

            double radiusX = (getBitmap().getWidth() / 2.0) * 1.1;
            double radiusY = (getBitmap().getHeight() / 2.0) * 1.1;

            double distX = Math.abs(centerX - tapXY.x);
            double distY = Math.abs(centerY - tapXY.y);

            if( distX < radiusX && distY < radiusY) {
                tapAction.setTapParameters(getLatLong());
                tapAction.run();
                return true;
            }
        }

        return false;
    }
}
