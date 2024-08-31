package de.leckasemmel.sonde1.fragments;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import de.leckasemmel.sonde1.R;
import de.leckasemmel.sonde1.SondeListItem;
import de.leckasemmel.sonde1.databinding.FragmentHeardBinding;
import de.leckasemmel.sonde1.viewmodels.HeardListViewModel;
import de.leckasemmel.sonde1.views.HeardListAdapter;


public class FragmentHeard extends Fragment implements HeardListAdapter.HeardListActions {
    HeardListViewModel mViewModel;
    HeardListAdapter mAdapter;
    SondeActions mListener;

    @Override
    public void onAttach(@NonNull Context context) {
        mListener = (SondeActions) context;
        super.onAttach(context);
    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = ViewModelProviders.of(requireActivity()).get(HeardListViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentHeardBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_heard, container, false);
        View group = binding.getRoot();
        binding.setViewModel(mViewModel);
        binding.setLifecycleOwner(this);

        binding.heardList.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new HeardListAdapter(this);
        mAdapter.updateData();
        binding.heardList.setAdapter(mAdapter);

        mViewModel.setEmptyHeardListImageResId(R.drawable.schade_guy);

        return group;
    }

    @BindingAdapter("heardListUpdated")
    public static void handleHeardListUpdated(RecyclerView view, Long value) {
        HeardListAdapter adapter = (HeardListAdapter) view.getAdapter();
        if (adapter != null) {
            adapter.updateData();
        }
    }

    @Override
    public void onMenuItemClick(MenuItem menuItem, SondeListItem item) {
        if (menuItem.getGroupId() == R.id.menu_group_heard_list) {
            if (menuItem.getItemId() == R.id.action_heard_list_item_delete) {
                mListener.onRemoveSondeFromList(item);
            } else if (menuItem.getItemId() == R.id.action_heard_list_enable_raw_log) {
                mListener.onEnableSondeLogging(item);
            }
        }
    }

    @Override
    public void onHeardListItemClick(int position, SondeListItem item) {
        mListener.onSetFocusSonde(item);
    }

    public interface SondeActions {
        void onRemoveSondeFromList(SondeListItem item);
        void onEnableSondeLogging(SondeListItem item);
        void onSetFocusSonde(SondeListItem item);
    }
}
