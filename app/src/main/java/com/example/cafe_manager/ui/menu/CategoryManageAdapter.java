package com.example.cafe_manager.ui.menu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.entity.CategoryEntity;

public class CategoryManageAdapter extends ListAdapter<CategoryEntity, CategoryManageAdapter.ViewHolder> {

    public interface OnActionListener {
        void onEdit(CategoryEntity category);
        void onDelete(CategoryEntity category);
    }

    private final OnActionListener listener;

    public CategoryManageAdapter(OnActionListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<CategoryEntity> DIFF =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull CategoryEntity a, @NonNull CategoryEntity b) {
                    return a.getCategoryId() == b.getCategoryId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull CategoryEntity a, @NonNull CategoryEntity b) {
                    return a.getCategoryName().equals(b.getCategoryName())
                            && (a.getDescription() == null ? b.getDescription() == null : a.getDescription().equals(b.getDescription()))
                            && a.isActive() == b.isActive();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_manage, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        CategoryEntity c = getItem(position);
        h.tvName.setText(c.getCategoryName());

        String desc = c.getDescription();
        if (desc == null || desc.trim().isEmpty()) {
            h.tvDescription.setText("Không có mô tả");
        } else {
            h.tvDescription.setText(desc);
        }

        h.btnEdit.setOnClickListener(v -> listener.onEdit(c));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(c));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName, tvDescription;
        final TextView btnEdit, btnDelete;

        ViewHolder(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_name);
            tvDescription = v.findViewById(R.id.tv_description);
            btnEdit = v.findViewById(R.id.btn_edit);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }
}
