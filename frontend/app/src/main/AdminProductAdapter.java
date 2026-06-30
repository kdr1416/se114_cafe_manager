package com.example.cafe_manager.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.entity.ProductEntity;

import java.util.ArrayList;
import java.util.List;

public class AdminProductAdapter
        extends RecyclerView.Adapter<AdminProductAdapter.ProductVH> {

    public interface OnActionListener {
        void onToggleVisibility(ProductEntity product);
        void onEdit(ProductEntity product);
    }

    private final List<ProductEntity> list = new ArrayList<>();
    private final OnActionListener listener;

    public AdminProductAdapter(OnActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ProductEntity> data) {
        list.clear();

        if (data != null) {
            list.addAll(data);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductVH onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(
                        R.layout.item_admin_product,
                        parent,
                        false
                );

        return new ProductVH(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ProductVH holder,
            int position
    ) {

        holder.bind(list.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ProductVH extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvPrice;
        private final android.widget.ImageView ivThumb;

        private final ImageButton btnEdit;
        private final ImageButton btnVisible;

        public ProductVH(@NonNull View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tv_name);
            tvPrice = itemView.findViewById(R.id.tv_price);
            ivThumb = itemView.findViewById(R.id.iv_thumb);

            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnVisible = itemView.findViewById(R.id.btn_visible);
        }

        void bind(
                ProductEntity product,
                OnActionListener listener
        ) {

            tvName.setText(product.getProductName());

            tvPrice.setText(
                    String.valueOf(product.getPrice())
            );

            if (product.getImageUrl() != null && !product.getImageUrl().trim().isEmpty()) {
                ivThumb.setImageTintList(null);
                com.bumptech.glide.Glide.with(itemView.getContext())
                        .load(product.getImageUrl().trim())
                        .placeholder(R.drawable.ic_coffee)
                        .error(R.drawable.ic_coffee)
                        .into(ivThumb);
            } else {
                ivThumb.setImageTintList(android.content.res.ColorStateList.valueOf(
                        itemView.getContext().getColor(R.color.accent)
                ));
                ivThumb.setImageResource(R.drawable.ic_coffee);
            }

            btnVisible.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onToggleVisibility(product);
                }
            });

            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEdit(product);
                }
            });
        }
    }
}
