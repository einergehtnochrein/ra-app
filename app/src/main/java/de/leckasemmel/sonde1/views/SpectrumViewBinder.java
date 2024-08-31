package de.leckasemmel.sonde1.views;

import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;

public class SpectrumViewBinder {

    @BindingAdapter(value = "rangeStartAttrChanged")
    public static void setRangeStartListener(SpectrumView spectrumView, final InverseBindingListener listener) {
        if (listener != null) {
            spectrumView.setRangeStartEventListener(new SpectrumView.RangeEventListener() {
                @Override
                public void onRangeChange(double value) {
                    listener.onChange();
                }
            });
        }
    }

    @BindingAdapter("rangeStart")
    public static void setRangeStart(SpectrumView view, Double value) {
        if (value != null) {
            view.setRangeStart(value);
        }
    }

    @InverseBindingAdapter(attribute = "rangeStart")
    public static double getRangeStart(SpectrumView view) {
        return view.getRangeStart();
    }

    @BindingAdapter(value = "rangeEndAttrChanged")
    public static void setRangeEndListener(SpectrumView spectrumView, final InverseBindingListener listener) {
        if (listener != null) {
            spectrumView.setRangeEndEventListener(new SpectrumView.RangeEventListener() {
                @Override
                public void onRangeChange(double value) {
                    listener.onChange();
                }
            });
        }
    }

    @BindingAdapter("rangeEnd")
    public static void setRangeEnd(SpectrumView view, Double value) {
        if (value != null) {
            view.setRangeEnd(value);
        }
    }

    @InverseBindingAdapter(attribute = "rangeEnd")
    public static double getRangeEnd(SpectrumView view) {
        return view.getRangeEnd();
    }

    @BindingAdapter("spectrumLevelsFrequency")
    public static void setSpectrumLevelsFrequency (SpectrumView view, Double frequency) {
        if (frequency != null) {
            view.setLevelsFrequency(frequency);
        }
    }

    @BindingAdapter("spectrumLevelsSpacing")
    public static void setSpectrumLevelsSpacing (SpectrumView view, Double spacing) {
        if (spacing != null) {
            view.setLevelsSpacing(spacing);
        }
    }

    @BindingAdapter("spectrumLevels")
    public static void setSpectrumLevels (SpectrumView view, Double[] levels) {
        if (levels != null) {
            view.setLevels(levels);
        }
    }
}
