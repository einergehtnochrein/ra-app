package de.leckasemmel.sonde1.views;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import de.leckasemmel.sonde1.BR;
import de.leckasemmel.sonde1.SondeListItem;
import de.leckasemmel.sonde1.R;
import de.leckasemmel.sonde1.databinding.HeardListItemBinding;
import de.leckasemmel.sonde1.model.SondeListModel;


// Adapter to fill the list view
public class HeardListAdapter extends RecyclerView.Adapter<HeardListAdapter.HeardListViewHolder> {
    private List<SondeListItem> mItemList;
    private final HeardListActions mListener;

    public HeardListAdapter(HeardListActions listener) {
        this.mListener = listener;
    }

    public void updateData() {
        SondeListModel model = SondeListModel.getInstance();
        mItemList = model.getItems();
        notifyDataSetChanged();
    }

    @Override
    @NonNull
    public HeardListAdapter.HeardListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        HeardListItemBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()),
                R.layout.heard_list_item, parent, false);
        return new HeardListViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(HeardListViewHolder viewHolder, final int position) {
        SondeListItem item = mItemList.get(position);
        viewHolder.bind(item);

        viewHolder.itemView.setOnClickListener(v ->
                mListener.onHeardListItemClick(position, item));
    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }

    public void onHeardListAction(int position, MenuItem menuItem) {
        if (position < mItemList.size()) {
            SondeListItem item = mItemList.get(position);
            if (mListener != null) {
                mListener.onMenuItemClick(menuItem, item);
            }
        }
    }

    public static class HeardListViewHolder extends RecyclerView.ViewHolder
        implements View.OnCreateContextMenuListener, PopupMenu.OnMenuItemClickListener {

        private final HeardListItemBinding itemBinding;

        public HeardListViewHolder(HeardListItemBinding binding) {
            super(binding.getRoot());
            itemBinding = binding;
            binding.getRoot().setOnCreateContextMenuListener(this);
        }

        public void bind(SondeListItem item) {
            itemBinding.setVariable(BR.item, item);
            itemBinding.executePendingBindings();
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenuInflater().inflate(R.menu.menu_heard_list_item, popup.getMenu());
            popup.setOnMenuItemClickListener(this);
            popup.show();
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            int position = this.getLayoutPosition();
            HeardListAdapter adapter = (HeardListAdapter)this.getBindingAdapter();
            if (adapter != null) {
                adapter.onHeardListAction(position, menuItem);
            }
            return true;
        }
    }

    public interface HeardListActions {
        void onMenuItemClick (MenuItem menuItem, SondeListItem item);
        void onHeardListItemClick(int position, SondeListItem item);
    }
}
