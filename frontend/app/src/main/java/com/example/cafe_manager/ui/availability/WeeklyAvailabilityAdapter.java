package com.example.cafe_manager.ui.availability;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.ui.availability.model.AvailabilitySlotUiModel;

import java.util.ArrayList;
import java.util.List;

public class WeeklyAvailabilityAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnAvailabilityChangedListener {
        void onAvailabilityChanged(AvailabilitySlotUiModel slot, boolean selected);
    }

    public static abstract class ListItem {
        public static final int TYPE_HEADER = 0;
        public static final int TYPE_SLOT = 1;
        public abstract int getType();
    }

    public static class HeaderItem extends ListItem {
        private final String dayLabel;
        public HeaderItem(String dayLabel) { this.dayLabel = dayLabel; }
        public String getDayLabel() { return dayLabel; }
        @Override public int getType() { return TYPE_HEADER; }
    }

    public static class SlotItem extends ListItem {
        private final AvailabilitySlotUiModel slot;
        public SlotItem(AvailabilitySlotUiModel slot) { this.slot = slot; }
        public AvailabilitySlotUiModel getSlot() { return slot; }
        @Override public int getType() { return TYPE_SLOT; }
    }

    private final List<ListItem> items = new ArrayList<>();
    private final OnAvailabilityChangedListener listener;

    public WeeklyAvailabilityAdapter(OnAvailabilityChangedListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ListItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == ListItem.TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_availability_day_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_availability_shift, parent, false);
            return new SlotViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListItem item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            HeaderItem header = (HeaderItem) item;
            ((HeaderViewHolder) holder).tvDayLabel.setText(header.getDayLabel());
        } else if (holder instanceof SlotViewHolder) {
            SlotItem slotItem = (SlotItem) item;
            AvailabilitySlotUiModel slot = slotItem.getSlot();
            SlotViewHolder slotHolder = (SlotViewHolder) holder;

            slotHolder.tvShiftName.setText(slot.getShiftName());
            slotHolder.tvShiftTime.setText(slot.getStartTime() + " - " + slot.getEndTime());

            // Disable listener temporarily to avoid side-effects during binding
            slotHolder.switchAvailable.setOnCheckedChangeListener(null);
            slotHolder.switchAvailable.setChecked(slot.isSelected());

            slotHolder.switchAvailable.setOnCheckedChangeListener((buttonView, isChecked) -> {
                slot.setSelected(isChecked);
                if (listener != null) {
                    listener.onAvailabilityChanged(slot, isChecked);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDayLabel;
        HeaderViewHolder(View view) {
            super(view);
            tvDayLabel = view.findViewById(R.id.tvDayLabel);
        }
    }

    static class SlotViewHolder extends RecyclerView.ViewHolder {
        final TextView tvShiftName;
        final TextView tvShiftTime;
        final SwitchCompat switchAvailable;

        SlotViewHolder(View view) {
            super(view);
            tvShiftName = view.findViewById(R.id.tvShiftName);
            tvShiftTime = view.findViewById(R.id.tvShiftTime);
            switchAvailable = view.findViewById(R.id.switchAvailable);
        }
    }
}
