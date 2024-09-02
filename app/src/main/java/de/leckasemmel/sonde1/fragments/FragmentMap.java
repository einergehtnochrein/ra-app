package de.leckasemmel.sonde1.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProviders;

import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.layer.cache.TileCache;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import de.leckasemmel.sonde1.R;
import de.leckasemmel.sonde1.databinding.FragmentMapBinding;
import de.leckasemmel.sonde1.viewmodels.MapViewModel;


public class FragmentMap extends Fragment
{
    private FragmentMapBinding binding;
    public MapViewModel viewModel;
    private final ArrayList<TileCache> tileCaches = new ArrayList<>();
    private Timer clockUpdateTimer;

    /*---------- Mapsforge functions. Follows MapViewerTemplate. ----------*/

    private void createTileCaches() {
        // First cache used for rendered layer
        tileCaches.add(AndroidUtil.createTileCache(
                requireContext(),
                "offline_tiles",
                binding.mapView.getModel().displayModel.getTileSize(),
                getScreenRatio(),
                binding.mapView.getModel().frameBufferModel.getOverdrawFactor(),
                true
        ));

        tileCaches.add(AndroidUtil.createTileCache(
                requireContext(),
                "online_tiles1",
                binding.mapView.getModel().displayModel.getTileSize(),
                getScreenRatio(),
                binding.mapView.getModel().frameBufferModel.getOverdrawFactor(),
                true
        ));

        tileCaches.add(AndroidUtil.createTileCache(
                requireContext(),
                "online_tiles2",
                binding.mapView.getModel().displayModel.getTileSize(),
                getScreenRatio(),
                binding.mapView.getModel().frameBufferModel.getOverdrawFactor(),
                true
        ));

        tileCaches.add(AndroidUtil.createTileCache(
                requireContext(),
                "online_tiles3",
                binding.mapView.getModel().displayModel.getTileSize(),
                getScreenRatio(),
                binding.mapView.getModel().frameBufferModel.getOverdrawFactor(),
                true
        ));
    }

    private float getScreenRatio() {
        return 1.0f;
    }

    /*----------  ----------*/

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = ViewModelProviders.of(requireActivity()).get(MapViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        org.mapsforge.core.util.Parameters.NUMBER_OF_THREADS = 8;

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_map, container, false);
        View group = binding.getRoot();
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(this);

        binding.mapView.setMyPositionMarkerDrawable(R.drawable.mypos_16);
        binding.mapView.setClickable(true);
        binding.mapView.getMapScaleBar().setVisible(true);
        binding.mapView.setBuiltInZoomControls(true);
        binding.mapView.setZoomLevelMin((byte) 1);
        binding.mapView.setZoomLevelMax((byte) 19);
        binding.mapView.getModel().displayModel.setFixedTileSize(768);
        createTileCaches();
        binding.mapView.setTileCaches(tileCaches);

        binding.credits.setMovementMethod(LinkMovementMethod.getInstance());

        clockUpdateTimer = new Timer();
        TimerTask updateTask = new TimerTask() {
            @Override
            public void run() {
                viewModel.currentDate.postValue(new Date());
            }
        };
        clockUpdateTimer.schedule(updateTask, 0, 1000);

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_fragment_map, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_fragment_map_debug_grid) {
                    boolean gridEnabled = Boolean.TRUE.equals(viewModel.debugGrid.getValue());
                    viewModel.setDebugGrid(!gridEnabled);
                }

                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        return group;
    }

    @Override
    public void onDestroyView() {
        if (clockUpdateTimer != null) {
            clockUpdateTimer.cancel();
            clockUpdateTimer = null;
        }

        super.onDestroyView();
    }

    @BindingAdapter("sondeDistance")
    public static void handleSondeDistance(TextView view, Double distance) {
        String s = "";
        if (distance != null) {
            if (!Double.isNaN(distance)) {
                if (distance < 10000.0) {
                    s = String.format(Locale.US, "%.0f m", distance);
                }
                else {
                    s = String.format(Locale.US, "%.1f km", distance / 1e3);

                }
            }
        }

        if (!s.contentEquals(view.getText())) {
            view.setText(s);
        }
    }

    @BindingAdapter({"sondeEta", "currentDate", "landingTimeStyle"})
    public static synchronized void handleSondeEta(TextView view, Date eta, Date current, Integer style) {
        String s = "";
        if ((eta != null) && (current != null) && (style != null)) {

            long difference = eta.getTime() - current.getTime();
            if (difference <= 0) {
                s = view.getResources().getString(R.string.fragment_map_landed);
            }
            else {
                if (style == 0) {   // Absolute clock time
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(eta);
                    s = String.format(Locale.US, "%02d:%02dh",
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE));
                }

                if (style == 1) {   // Time remaining
                    if (difference / 1000 > 600) {
                        s = String.format(Locale.US, "%d min", (difference / 1000) / 60);
                    }
                    else {
                        s = String.format(Locale.US, "%d s", difference / 1000);
                    }
                }
            }
        }

        if (!s.contentEquals(view.getText())) {
            view.setText(s);
        }
    }
}
