package com.example.cafe_manager.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.ui.availability.model.AvailabilitySlotUiModel;

import java.util.ArrayList;
import java.util.List;

public class WeeklyCalendarAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnSlotToggleListener {
        void onSlotToggled(AvailabilitySlotUiModel slot, boolean isAvailable);
    }

    private final List<WeeklyCalendarItem> items = new ArrayList<>();
    private final OnSlotToggleListener listener;

    public WeeklyCalendarAdapter(OnSlotToggleListener listener) {
        this.listener = listener;
    }

    public void setItems(List<WeeklyCalendarItem> newItems) {
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
        if (viewType == WeeklyCalendarItem.TYPE_DAY_HEADER) {
            View view = inflater.inflate(R.layout.item_week_day_header, parent, false);
            return new DayHeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_availability_slot, parent, false);
            return new AvailabilitySlotViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        WeeklyCalendarItem item = items.get(position);
        if (holder instanceof DayHeaderViewHolder) {
            WeeklyCalendarItem.DayHeader header = (WeeklyCalendarItem.DayHeader) item;
            DayHeaderViewHolder h = (DayHeaderViewHolder) holder;
            h.tvDayLabel.setText(header.getDayLabel());
            h.ivLockIcon.setVisibility(header.isLocked() ? View.VISIBLE : View.GONE);
        } else if (holder instanceof AvailabilitySlotViewHolder) {
            WeeklyCalendarItem.AvailabilitySlot slotItem = (WeeklyCalendarItem.AvailabilitySlot) item;
            AvailabilitySlotUiModel slot = slotItem.getSlot();
            boolean isLocked = slotItem.isLocked();
            AvailabilitySlotViewHolder h = (AvailabilitySlotViewHolder) holder;

            h.tvShiftName.setText(slot.getShiftName());
            h.tvShiftTime.setText(slot.getStartTime() + " - " + slot.getEndTime());

            // Remove listener before setChecked to prevent recursive callbacks
            h.switchAvailable.setOnCheckedChangeListener(null);
            h.switchAvailable.setChecked(slot.isSelected());

            if (isLocked) {
                h.switchAvailable.setEnabled(false);
                h.switchAvailable.setAlpha(0.5f);
            } else {
                h.switchAvailable.setEnabled(true);
                h.switchAvailable.setAlpha(1.0f);
                h.switchAvailable.setOnCheckedChangeListener((btn, checked) -> {
                    slot.setSelected(checked);
                    if (listener != null) {
                        listener.onSlotToggled(slot, checked);
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class DayHeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDayLabel;
        final ImageView ivLockIcon;

        DayHeaderViewHolder(View view) {
            super(view);
            tvDayLabel = view.findViewById(R.id.tv_day_label);
            ivLockIcon = view.findViewById(R.id.iv_lock_icon);
        }
    }

    static class AvailabilitySlotViewHolder extends RecyclerView.ViewHolder {
        final TextView tvShiftName;
        final TextView tvShiftTime;
        final SwitchCompat switchAvailable;

        AvailabilitySlotViewHolder(View view) {
            super(view);
            tvShiftName = view.findViewById(R.id.tvShiftName);
            tvShiftTime = view.findViewById(R.id.tvShiftTime);
            switchAvailable = view.findViewById(R.id.switchAvailable);
        }
    }
}
