package de.leckasemmel.sonde1.viewmodels;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.RadioGroup;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Locale;

import de.leckasemmel.sonde1.R;
import de.leckasemmel.sonde1.RaPreferences;
import de.leckasemmel.sonde1.SondeListItem;


public class DashboardViewModel extends ViewModel {

    public MutableLiveData<Boolean> loaderMode = new MutableLiveData<>(false);
    public MutableLiveData<Integer> loaderVersion = new MutableLiveData<>();

    public MutableLiveData<SondeListItem.WayPoint> lastWayPoint = new MutableLiveData<>();
    public MutableLiveData<String> sondeSerial = new MutableLiveData<>();
    public MutableLiveData<Double> rssi = new MutableLiveData<>(Double.NaN);
    public MutableLiveData<Integer> smeter_style = new MutableLiveData<>();

    public MutableLiveData<Boolean> connected = new MutableLiveData<>();
    public MutableLiveData<Integer> mode = new MutableLiveData<>();
    public MutableLiveData<String> frequency_s = new MutableLiveData<>(" ");
    public MutableLiveData<Boolean> frequency_updated = new MutableLiveData<>(false);
    public MutableLiveData<Integer> decoder = new MutableLiveData<>(null);
    public MutableLiveData<Boolean> debugEnable = new MutableLiveData<>(false);
    public MutableLiveData<Integer> debugAudio = new MutableLiveData<>(null);
    public MutableLiveData<Double> batteryVoltage = new MutableLiveData<>();
    public MutableLiveData<Double> spectrumStartFrequency = new MutableLiveData<>();
    public MutableLiveData<Double> spectrumEndFrequency = new MutableLiveData<>();
    public MutableLiveData<Double> spectrumLevelsFrequency = new MutableLiveData<>();
    public MutableLiveData<Double> spectrumLevelsSpacing = new MutableLiveData<>();
    public MutableLiveData<Double[]> spectrumLevels = new MutableLiveData<>();
    public MutableLiveData<Boolean> monitor = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> monitorSupported = new MutableLiveData<>(false);
    private RaPreferences mRaPrefs;


    public void setPreferences(RaPreferences prefs) {
        mRaPrefs = prefs;
    }
    public void setLoaderMode (Boolean val) {
        loaderMode.setValue(val);
    }
    public void setLoaderVersion (Integer val) {
        loaderVersion.setValue(val);
    }
    public void setLastWayPoint (SondeListItem.WayPoint val) {
        lastWayPoint.setValue(val);
    }
    public void setSondeSerial (String val) {
        sondeSerial.setValue(val);
    }
    public void setRssi (Double val) {
        rssi.setValue(val);
    }
    public void setSmeterStyle (Integer val) {
        smeter_style.setValue(val);
    }
    public void setConnected (Boolean val) {
        connected.setValue(val);
    }
    public void setMode (Integer val) {
        mode.setValue(val);
    }
    public void setFrequency (Double val) {
        if (Double.isNaN(val)) {
            frequency_s.setValue(" ");
        }
        else {
            frequency_s.setValue(String.format(Locale.US, "%.3f", val));
        }
    }
    public void setFrequencyUpdated (Boolean val) { frequency_updated.setValue(val); }
    public void setDecoder (Integer val) { decoder.setValue(val); }
    public void setDebugEnable (Boolean val) { debugEnable.setValue(val); }
    public void setDebugAudio (Integer val) { debugAudio.setValue(val); }
    public void setBatteryVoltage (Double val) { batteryVoltage.setValue(val); }
    public void setSpectrumLevels (double frequency, double spacing, Double[] val) {
        spectrumLevelsFrequency.setValue(frequency);
        spectrumLevelsSpacing.setValue(spacing);
        spectrumLevels.setValue(val);
    }
    public void setMonitor (Boolean val) { monitor.setValue(val); }
    public void setMonitorSupported (Boolean val) { monitorSupported.setValue(val); }

    public boolean onSmeterLongClicked(View view) {
        Context context = view.getContext();

        String[] items = context.getResources().getStringArray(R.array.smeter_styles);
        int currentValue = Integer.parseInt(mRaPrefs.getLookSmeterStyle());

        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.fragment_dashboard_dialog_smeter_style_title))
                .setCancelable(true)
                .setSingleChoiceItems(items, currentValue, (dialog, which) -> {
                    SharedPreferences.Editor myEdit = mRaPrefs.getSharedPreferences().edit();
                    if (which != currentValue) {
                        String s = String.format(Locale.US, "%d", which);
                        myEdit.putString(RaPreferences.KEY_PREF_LOOK_SMETER_STYLE, s);
                        myEdit.apply();
                    }
                    dialog.cancel();
                })
                .setNegativeButton(context.getString(R.string.fragment_dashboard_dialog_smeter_style_cancel), (dialog, whichButton) -> dialog.cancel())
                .show()
        ;

        return true;
    }

    public void onModeSelected(RadioGroup radioGroup, int id) {
        Integer currentSelect = mode.getValue();
        if (currentSelect != null) {
            int select = 1;
            if (id == R.id.radio_mode_list) {
                select = 0;
            }
            if (id == R.id.radio_mode_spectrum) {
                select = 2;
            }
            if (select != currentSelect) {
                mode.setValue(select);
            }
        }
    }
}
