package com.example.cafe_manager.ui.communication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;

import java.util.ArrayList;
import java.util.List;

public class NewsFilterAdapter extends RecyclerView.Adapter<NewsFilterAdapter.FilterVH> {

    public interface OnFilterClickListener {
        void onFilterClick(String filter);
    }

    private final List<String> filters = new ArrayList<>();
    private String selectedFilter = "Tất cả";
    private final OnFilterClickListener listener;

    public NewsFilterAdapter(OnFilterClickListener listener) {
        this.listener = listener;
        filters.add("Tất cả");
        filters.add("Chưa đọc");
        filters.add("Đã ghim");
        filters.add("Khẩn cấp");
    }

    public String getSelectedFilter() {
        return selectedFilter;
    }

    public void setSelectedFilter(String filter) {
        if (selectedFilter.equals(filter)) return;
        selectedFilter = filter;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FilterVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_chip, parent, false);
        return new FilterVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FilterVH holder, int position) {
        String filter = filters.get(position);
        holder.bind(filter, selectedFilter.equals(filter), listener);
    }

    @Override
    public int getItemCount() {
        return filters.size();
    }

    static class FilterVH extends RecyclerView.ViewHolder {
        private final TextView tvChip;

        FilterVH(View itemView) {
            super(itemView);
            tvChip = itemView.findViewById(R.id.tv_chip);
        }

        void bind(final String filter, boolean isSelected, final OnFilterClickListener listener) {
            tvChip.setText(filter);
            applyState(isSelected);
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onFilterClick(filter);
            });
        }

        private void applyState(boolean selected) {
            tvChip.setSelected(selected);
            tvChip.setTextColor(itemView.getContext().getColor(
                    selected ? R.color.text_on_accent : R.color.text_soft));
        }
    }
}
