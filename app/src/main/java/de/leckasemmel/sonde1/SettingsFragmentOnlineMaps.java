package de.leckasemmel.sonde1;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Set;

public class SettingsFragmentOnlineMaps extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final static String TAG = SettingsFragmentOnlineMaps.class.getName();

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setDivider(null);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstance, String rootKey) {
        // Load preferences from XML file
        setPreferencesFromResource(R.xml.preferences_online_maps, rootKey);

        handleDependencies();

        // Set summaries
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            updateSummary(getPreferenceScreen().getPreference(i));
        }
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");

        super.onResume();
        final SharedPreferences sh = getPreferenceManager().getSharedPreferences();
        if (sh != null) {
            sh.registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");

        super.onPause();
        final SharedPreferences sh = getPreferenceManager().getSharedPreferences();
        if (sh != null) {
            sh.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        handleDependencies();

        Preference pref = findPreference(key);
        updateSummary(pref);
    }

    // Take care of dependencies between preferences
    private void handleDependencies() {
    }

    private void updateSummary(Preference p) {
        if (p == null) {
            return;
        }

        if (p instanceof PreferenceCategory) {
            for (int i = 0; i < ((PreferenceCategory) p).getPreferenceCount(); i++) {
                updateSummary(((PreferenceCategory) p).getPreference(i));
            }
        }

        if (p instanceof EditTextPreference) {
            p.setSummary(((EditTextPreference) p).getText());
        }

        if (p instanceof ListPreference) {
            p.setSummary(((ListPreference) p).getEntry());
        }

        if (p instanceof MultiSelectListPreference) {
            Set<String> valueSet = ((MultiSelectListPreference) p).getValues();
            StringBuilder summary = new StringBuilder();
            for (String entry : valueSet) {
                summary.append(entry).append("\n");
            }
            if (summary.toString().endsWith("\n")) {
                summary = new StringBuilder(summary.substring(0, summary.length() - 1));
            }
            p.setSummary(summary.toString());
        }
    }
}
