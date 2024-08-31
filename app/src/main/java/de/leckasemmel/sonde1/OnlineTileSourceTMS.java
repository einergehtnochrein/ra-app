package de.leckasemmel.sonde1;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource;

import java.net.MalformedURLException;
import java.net.URL;

/* This class replaces OnlineTileSource in the Mapsforge package.
 * It would be cleaner to just derive a new class from the existing one,
 * but OnlineTileSource is not well suited for deriving from it.
 */


public class OnlineTileSourceTMS extends AbstractTileSource {
    protected boolean alpha = false;
    protected float alphaValue = 1.0f;
    protected String baseUrl = "/";
    protected String extension = "png";
    protected String name;
    protected int parallelRequestsLimit = 8;
    protected String protocol = "http";
    protected int tileSize = 256;
    protected byte zoomLevelMax = 18;
    protected byte zoomLevelMin = 0;
    protected int format = 0;

    public OnlineTileSourceTMS(String[] hostNames, int port) {
        super(hostNames, port);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof OnlineTileSourceTMS)) {
            return false;
        }
        OnlineTileSourceTMS other = (OnlineTileSourceTMS) obj;
        if (!this.baseUrl.equals(other.baseUrl)) {
            return false;
        }
        return true;
    }

    public float getAlphaValue() {
        return alphaValue;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getExtension() {
        return extension;
    }

    public String getName() {
        return name;
    }

    @Override
    public int getParallelRequestsLimit() {
        return parallelRequestsLimit;
    }

    public String getProtocol() {
        return protocol;
    }

    public int getTileSize() {
        return tileSize;
    }

    @Override
    public URL getTileUrl(Tile tile) throws MalformedURLException {
        StringBuilder stringBuilder = new StringBuilder(32);

        stringBuilder.append(baseUrl);
        stringBuilder.append(tile.zoomLevel);
        stringBuilder.append('/');
        if (format == 0) {
            stringBuilder.append(tile.tileX);
            stringBuilder.append('/');
            stringBuilder.append(tile.tileY);
        }
        else if (format == 1) {
            stringBuilder.append(tile.tileY);
            stringBuilder.append('/');
            stringBuilder.append(tile.tileX);
        }
        else if (format == 2) {
            stringBuilder.append(tile.tileX);
            stringBuilder.append('/');
            stringBuilder.append((1 << tile.zoomLevel) - 1 - tile.tileY);
        }
        else {
            stringBuilder.append((1 << tile.zoomLevel) - 1 - tile.tileY);
            stringBuilder.append('/');
            stringBuilder.append(tile.tileX);
        }
        stringBuilder.append('.').append(extension);
        if (apiKey != null) {
            stringBuilder.append('?').append(keyName).append("=").append(apiKey);
        }

        return new URL(this.protocol, getHostName(), this.port, stringBuilder.toString());
    }

    @Override
    public byte getZoomLevelMax() {
        return zoomLevelMax;
    }

    @Override
    public byte getZoomLevelMin() {
        return zoomLevelMin;
    }

    public int getFormat() {
        return format;
    }

    @Override
    public boolean hasAlpha() {
        return alpha;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + this.baseUrl.hashCode();
        return result;
    }

    public OnlineTileSourceTMS setAlpha(boolean alpha) {
        this.alpha = alpha;
        return this;
    }

    public OnlineTileSourceTMS setAlphaValue(float alphaValue) {
        this.alphaValue = Math.max(0, Math.min(1, alphaValue));
        return this;
    }

    public OnlineTileSourceTMS setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public OnlineTileSourceTMS setExtension(String extension) {
        this.extension = extension;
        return this;
    }

    public OnlineTileSourceTMS setName(String name) {
        this.name = name;
        return this;
    }

    public OnlineTileSourceTMS setParallelRequestsLimit(int parallelRequestsLimit) {
        this.parallelRequestsLimit = parallelRequestsLimit;
        return this;
    }

    public OnlineTileSourceTMS setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public OnlineTileSourceTMS setTileSize(int tileSize) {
        this.tileSize = tileSize;
        return this;
    }

    public OnlineTileSourceTMS setZoomLevelMax(byte zoomLevelMax) {
        this.zoomLevelMax = zoomLevelMax;
        return this;
    }

    public OnlineTileSourceTMS setZoomLevelMin(byte zoomLevelMin) {
        this.zoomLevelMin = zoomLevelMin;
        return this;
    }

    public OnlineTileSourceTMS setFormat(int format) {
        this.format = format;
        return this;
    }
}
