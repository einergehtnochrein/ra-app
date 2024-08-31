package de.leckasemmel.sonde1;

import android.text.Spanned;


public class OnlineMapDescriptor {
    public OnlineTileSourceTMS tileSource;
    public Spanned credits;
    public int cacheIndex;

    public OnlineMapDescriptor(OnlineTileSourceTMS tileSource, Spanned credits, int cacheIndex) {
        this.tileSource = tileSource;
        this.credits = credits;
        this.cacheIndex = cacheIndex;
    }
}
