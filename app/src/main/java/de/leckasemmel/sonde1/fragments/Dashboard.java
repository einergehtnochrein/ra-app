package de.leckasemmel.sonde1.fragments;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Locale;

import de.leckasemmel.sonde1.R;
import de.leckasemmel.sonde1.RaPreferences;
import de.leckasemmel.sonde1.SondeListItem;
import de.leckasemmel.sonde1.databinding.FragmentDashboardBinding;
import de.leckasemmel.sonde1.viewmodels.DashboardViewModel;


public class Dashboard extends Fragment {
    private final static String TAG = Dashboard.class.getName();
    public DashboardViewModel viewModel;

    public Dashboard() {
        // Required empty public constructor
    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate(), savedInstanceState = " + savedInstanceState);

        viewModel = ViewModelProviders.of(requireActivity()).get(DashboardViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");

        super.onCreateView(inflater, container, savedInstanceState);

        FragmentDashboardBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_dashboard, container, false);
        View group = binding.getRoot();
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(this);

        Typeface font7seg = Typeface.createFromAsset(requireActivity().getAssets(), "fonts/DSEG14Modern-BoldItalic.ttf");
        binding.editFrequency.setTypeface(font7seg);
        binding.staticFrequency.setTypeface(font7seg);
        binding.callSetup1.setTypeface(font7seg);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        String smeterStyle = sharedPref.getString(RaPreferences.KEY_PREF_LOOK_SMETER_STYLE, "");
        int smeter_code;
        try {
            smeter_code = Integer.parseInt(smeterStyle);
        } catch (NumberFormatException e) {
            smeter_code = 1;
        }
        binding.smeter.setStyle(smeter_code);

        ArrayAdapter<CharSequence> debugAudioChannelAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.debug_audio_channel, android.R.layout.simple_spinner_item);
        debugAudioChannelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerDebugAudioChannel.setAdapter(debugAudioChannelAdapter);

        ArrayAdapter<CharSequence> sondeTypeAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.sonde_decoder, android.R.layout.simple_spinner_item);
        sondeTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerManualSondeDecoder.setAdapter(sondeTypeAdapter);

        binding.editFrequency.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewModel.setFrequencyUpdated(true);
                return true;
            }
            return false;
        });

        return group;
    }

    @BindingAdapter("lastWayPointAltitude")
    public static void handleLastWayPointAltitude(TextView view, SondeListItem.WayPoint point) {
        String s = "";
        if (point != null) {
            s = String.format(Locale.US, "%.0f m", point.getAltitude());
        }

        if (!s.contentEquals(view.getText())) {
            view.setText(s);
        }
    }
}

