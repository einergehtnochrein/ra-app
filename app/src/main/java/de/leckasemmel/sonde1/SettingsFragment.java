package de.leckasemmel.sonde1;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import android.util.Log;
import android.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final static String TAG = SettingsFragment.class.getName();

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setDivider(null);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstance, String rootKey) {
        // Load preferences from XML file
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // Individual preference's preparation
        MultiSelectListPreference mapFilesPreference = findPreference(RaPreferences.KEY_PREF_MAP_FILES);
        if (mapFilesPreference != null) {
            EditTextPreference mapPathPref =
                    getPreferenceScreen().findPreference(RaPreferences.KEY_PREF_MAP_PATH);
            if (mapPathPref != null) {
                setListPrefMapFiles(mapFilesPreference, new File(Objects.requireNonNull(mapPathPref.getText())));
            }
        }

        ListPreference mapThemeFilePreference = findPreference(RaPreferences.KEY_PREF_MAP_CUSTOM_THEME_FILE);
        if (mapThemeFilePreference != null) {
            EditTextPreference mapThemePathPref =
                    getPreferenceScreen().findPreference(RaPreferences.KEY_PREF_MAP_THEME_PATH);
            if (mapThemePathPref != null) {
                setListPrefMapThemeFile(mapThemeFilePreference, new File(Objects.requireNonNull(mapThemePathPref.getText())));
            }
        }

        MultiSelectListPreference macPref = findPreference(RaPreferences.KEY_PREF_BLUETOOTH_MAC_ADDRESSES);
        if (macPref != null) {
            // Define list entries once (we don't have them in the XML file!)
            setListPrefMacAddresses(macPref);
        }

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


    private void setListPrefMapFiles(MultiSelectListPreference mapFilesPref, File directory) {
        List<String> entries = new ArrayList<>();
        List<String> entryValues = new ArrayList<>();

        // Copy existing entries
        Set<String> preset = mapFilesPref.getValues();
        if (preset != null) {
            for (String s : preset) {
                entries.add(s);
                entryValues.add(s);
            }
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".map")) {
                    // Add if not already in list
                    int index = entryValues.indexOf(f.getName());
                    if (index >= 0) {
                        entries.set(index, f.getName());
                    } else {
                        entries.add(f.getName());
                        entryValues.add(f.getName());
                    }
                }
            }
        }

        final CharSequence[] entriesCharArray = entries.toArray(new CharSequence[0]);
        final CharSequence[] entryValuesCharArray = entryValues.toArray(new CharSequence[0]);

        mapFilesPref.setEntries(entriesCharArray);
        mapFilesPref.setEntryValues(entryValuesCharArray);
    }


    private void setListPrefMapThemeFile(ListPreference mapThemeFilePref, File directory) {
        List<String> entries = new ArrayList<>();
        List<String> entryValues = new ArrayList<>();

        File[] files = directory.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    File[] sub_files = f.listFiles();
                    assert sub_files != null;
                    for (File sub : sub_files) {
                        if (sub.getName().endsWith(".xml")) {
                            entries.add(f.getName() + "/" + sub.getName());
                            entryValues.add(f.getName() + "/" + sub.getName());
                        }
                    }
                } else {
                    if (f.getName().endsWith(".xml")) {
                        entries.add(f.getName());
                        entryValues.add(f.getName());
                    }
                }
            }
        }

        final CharSequence[] entriesCharArray = entries.toArray(new CharSequence[0]);
        final CharSequence[] entryValuesCharArray = entryValues.toArray(new CharSequence[0]);

        mapThemeFilePref.setEntries(entriesCharArray);
        mapThemeFilePref.setEntryValues(entryValuesCharArray);
    }

    // Fill the MAC address selection list with the list of bonded BLE devices
    private void setListPrefMacAddresses(MultiSelectListPreference macPref) {
        List<String> entries = new ArrayList<>();
        List<String> entryValues = new ArrayList<>();

        // Copy existing entries
        Set<String> preset = macPref.getValues();
        if (preset != null) {
            for (String s : preset) {
                entries.add(s);
                entryValues.add(s);
            }
        }

        BluetoothManager man = (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = man.getAdapter();
        if (mBluetoothAdapter != null) {
            boolean havePermission = (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) ||
                    (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED);
            if (havePermission) {
                Set<BluetoothDevice> bondedDevicesReal = mBluetoothAdapter.getBondedDevices();
                Set<BluetoothDevice> bondedDevices = new HashSet<>(bondedDevicesReal);
                for (BluetoothDevice dev : bondedDevices) {
                    String name = dev.getName();
                    String address = dev.getAddress();

                    // If this is a BL652 with factory set name, try and identify known modules
                    if (name == null) {
                        name = address;
                    } else {
                        if (name.equals("LAIRD BL652")) {
                            if (address.equalsIgnoreCase("CA:B7:F9:3E:D6:82")) {
                                name = "Ra1 #1";
                            }
                            if (address.equalsIgnoreCase("C5:03:9F:4A:E1:16")) {
                                name = "Ra1 #2";
                            }
                            if (address.equalsIgnoreCase("F9:D6:A4:94:32:C2")) {
                                name = "Ra1 #3";
                            }
                            if (address.equalsIgnoreCase("FD:D8:50:37:9D:8B")) {
                                name = "Ra1 #4";
                            }
                            if (address.equalsIgnoreCase("D7:03:7A:25:DA:77")) {
                                name = "Ra1 #5";
                            }

                            if (address.equalsIgnoreCase("C5:47:D5:70:1B:F8")) {
                                name = "Ra2 #7";
                            }
                            if (address.equalsIgnoreCase("E5:8D:4F:81:44:3B")) {
                                name = "Ra2 #9";
                            }
                            if (address.equalsIgnoreCase("FC:BA:5D:42:75:C5")) {
                                name = "Ra2 #10";
                            }
                            if (address.equalsIgnoreCase("C2:2E:C5:69:97:49")) {
                                name = "Ra2 #13";
                            }
                            if (address.equalsIgnoreCase("F3:61:06:04:E4:EF")) {
                                name = "Ra2 #14";
                            }
                            if (address.equalsIgnoreCase("F3:FC:17:37:1D:29")) {
                                name = "Ra2 #15";
                            }
                        }
                    }

                    // If the address already exists, just update its name, but don't add a new entry
                    int index = entryValues.indexOf(address);
                    if (index >= 0) {
                        entries.set(index, name);
                    } else {
                        entries.add(name);
                        entryValues.add(address);
                    }
                }
            }
        }

        final CharSequence[] entriesCharArray = entries.toArray(new CharSequence[0]);
        final CharSequence[] entryValuesCharArray = entryValues.toArray(new CharSequence[0]);

        macPref.setEntries(entriesCharArray);
        macPref.setEntryValues(entryValuesCharArray);
    }
}
