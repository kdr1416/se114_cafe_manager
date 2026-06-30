package com.example.cafe_manager.ui.table;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;

import java.util.ArrayList;
import java.util.List;

public class AreaAdapter extends RecyclerView.Adapter<AreaAdapter.ChipVH> {

    public interface OnAreaClickListener {
        void onAreaClick(String area);
    }

    private final List<String> areas = new ArrayList<>();
    private String selectedArea = "Tất cả";
    private final OnAreaClickListener listener;

    public AreaAdapter(OnAreaClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<String> list) {
        areas.clear();
        if (list != null) {
            areas.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void setSelectedArea(String area) {
        if (selectedArea.equals(area)) return;
        selectedArea = area;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChipVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_chip, parent, false);
        return new ChipVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChipVH holder, int position) {
        String area = areas.get(position);
        holder.bind(area, selectedArea.equals(area), listener);
    }

    @Override
    public int getItemCount() {
        return areas.size();
    }

    static class ChipVH extends RecyclerView.ViewHolder {
        private final TextView tvChip;

        ChipVH(View itemView) {
            super(itemView);
            tvChip = itemView.findViewById(R.id.tv_chip);
        }

        void bind(final String area, boolean isSelected, final OnAreaClickListener listener) {
            tvChip.setText(area);
            applyState(isSelected);
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onAreaClick(area);
            });
        }

        private void applyState(boolean selected) {
            tvChip.setSelected(selected);
            tvChip.setTextColor(itemView.getContext().getColor(
                    selected ? R.color.text_on_accent : R.color.text_soft));
        }
    }
}
