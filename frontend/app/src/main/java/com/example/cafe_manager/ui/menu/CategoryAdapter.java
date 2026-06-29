package com.example.cafe_manager.ui.menu;

import android.view.LayoutInflater;

import android.view.View;

import android.view.ViewGroup;

import android.widget.TextView;

import androidx.annotation.NonNull;

import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;

import com.example.cafe_manager.data.local.entity.CategoryEntity;

import java.util.ArrayList;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ChipVH> {

    public interface OnCategoryClickListener {

        /** categoryId = 0 nghĩa là "Tất cả" */

        void onCategoryClick(int categoryId);

    }

    private final List<CategoryEntity> categories = new ArrayList<>();

    private int selectedCategoryId = 0;

    private final OnCategoryClickListener listener;

    public CategoryAdapter(OnCategoryClickListener listener) {

        this.listener = listener;

    }

    public void submitList(List<CategoryEntity> list) {

        categories.clear();

        if (list != null) categories.addAll(list);

        notifyDataSetChanged();

    }

    public void setSelectedCategoryId(int categoryId) {

        if (selectedCategoryId == categoryId) return;

        selectedCategoryId = categoryId;

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

        if (position == 0) {

            holder.bindAll(selectedCategoryId == 0, listener);

        } else {

            CategoryEntity category = categories.get(position - 1);

            holder.bind(category,

                    selectedCategoryId == category.getCategoryId(),

                    listener);

        }

    }

    @Override

    public int getItemCount() {

        return categories.size() + 1;  // +1 cho chip "Tất cả"

    }

    static class ChipVH extends RecyclerView.ViewHolder {

        private final TextView tvChip;

        ChipVH(View itemView) {

            super(itemView);

            tvChip = itemView.findViewById(R.id.tv_chip);

        }

        void bindAll(boolean isSelected, OnCategoryClickListener listener) {

            tvChip.setText(R.string.category_all);

            applyState(isSelected);

            itemView.setOnClickListener(v -> {

                if (listener != null) listener.onCategoryClick(0);

            });

        }

        void bind(final CategoryEntity category,

                  boolean isSelected,

                  final OnCategoryClickListener listener) {

            tvChip.setText(category.getCategoryName());

            applyState(isSelected);

            itemView.setOnClickListener(v -> {

                if (listener != null) listener.onCategoryClick(category.getCategoryId());

            });

        }

        private void applyState(boolean selected) {

            tvChip.setSelected(selected);

            tvChip.setTextColor(itemView.getContext().getColor(

                    selected ? R.color.text_on_accent : R.color.text_soft));

        }

    }

}
