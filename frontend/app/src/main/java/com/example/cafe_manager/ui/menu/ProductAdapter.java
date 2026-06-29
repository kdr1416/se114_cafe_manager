package com.example.cafe_manager.ui.menu;

import android.view.LayoutInflater;

import android.view.View;

import android.view.ViewGroup;

import android.widget.ImageButton;

import android.widget.ImageView;

import android.widget.TextView;

import androidx.annotation.NonNull;

import androidx.recyclerview.widget.DiffUtil;

import androidx.recyclerview.widget.ListAdapter;

import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;

import com.example.cafe_manager.data.local.entity.ProductEntity;

import com.example.cafe_manager.util.CurrencyUtils;

public class ProductAdapter extends ListAdapter<ProductEntity, ProductAdapter.ProductVH> {

    public interface OnProductQtyChangeListener {
        void onIncrease(ProductEntity product);
        void onDecrease(ProductEntity product);
    }

    private final OnProductQtyChangeListener listener;
    private final java.util.Map<Integer, String> categoryNameMap = new java.util.HashMap<>();

    public ProductAdapter(OnProductQtyChangeListener listener) {

        super(DIFF);

        this.listener = listener;

    }

    public void setCategoryMap(java.util.List<com.example.cafe_manager.data.local.entity.CategoryEntity> categories) {
        categoryNameMap.clear();
        if (categories != null) {
            for (com.example.cafe_manager.data.local.entity.CategoryEntity c : categories) {
                categoryNameMap.put(c.getCategoryId(), c.getCategoryName());
            }
        }
        notifyDataSetChanged();
    }

    @NonNull

    @Override

    public ProductVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())

                .inflate(R.layout.item_product, parent, false);

        return new ProductVH(view);

    }

    @Override

    public void onBindViewHolder(@NonNull ProductVH holder, int position) {

        holder.bind(getItem(position), listener, categoryNameMap);

    }

    static class ProductVH extends RecyclerView.ViewHolder {

        private final ImageView ivMedia;

        private final TextView tvName;

        private final TextView tvPrice;

        private final TextView tvCategoryTag;

        private final ImageButton btnAdd;
        private final View layoutQuantity;
        private final ImageButton btnDecrease;
        private final TextView tvQuantity;
        private final ImageButton btnIncrease;

        ProductVH(View itemView) {

            super(itemView);

            ivMedia = itemView.findViewById(R.id.iv_media);

            tvName = itemView.findViewById(R.id.tv_product_name);

            tvPrice = itemView.findViewById(R.id.tv_product_price);

            tvCategoryTag = itemView.findViewById(R.id.tv_category_tag);

            btnAdd = itemView.findViewById(R.id.btn_add);
            layoutQuantity = itemView.findViewById(R.id.layout_quantity);
            btnDecrease = itemView.findViewById(R.id.btn_decrease);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
            btnIncrease = itemView.findViewById(R.id.btn_increase);

        }

        void bind(final ProductEntity product, final OnProductQtyChangeListener listener, final java.util.Map<Integer, String> categoryNameMap) {

            tvName.setText(product.getProductName());

            tvPrice.setText(CurrencyUtils.formatVnd(product.getPrice()));

            String categoryName = categoryNameMap.get(product.getCategoryId());
            if (categoryName != null && !categoryName.isEmpty()) {
                tvCategoryTag.setText(categoryName);
                tvCategoryTag.setVisibility(View.VISIBLE);
            } else {
                tvCategoryTag.setVisibility(View.GONE);
            }

            int defaultIcon = R.drawable.ic_coffee;
            if (categoryName != null) {
                String nameLower = categoryName.toLowerCase();
                if (nameLower.contains("trà") || nameLower.contains("tea")) {
                    defaultIcon = R.drawable.ic_tea;
                } else if (nameLower.contains("bánh") || nameLower.contains("cake") || nameLower.contains("ngọt")) {
                    defaultIcon = R.drawable.ic_cake;
                } else if (nameLower.contains("sinh tố") || nameLower.contains("smoothie")) {
                    defaultIcon = R.drawable.ic_smoothie;
                }
            }

            if (product.getImageUrl() != null && !product.getImageUrl().trim().isEmpty()) {
                ivMedia.setImageTintList(null);
                ivMedia.clearColorFilter();
                ivMedia.setPadding(0, 0, 0, 0);
                com.bumptech.glide.Glide.with(itemView.getContext())
                        .load(product.getImageUrl().trim())
                        .placeholder(defaultIcon)
                        .error(defaultIcon)
                        .into(ivMedia);
            } else {
                ivMedia.setImageTintList(android.content.res.ColorStateList.valueOf(
                        itemView.getContext().getColor(R.color.accent)
                ));
                int padding = itemView.getContext().getResources().getDimensionPixelSize(R.dimen.spacing_lg);
                ivMedia.setPadding(padding, padding, padding, padding);
                ivMedia.setImageResource(defaultIcon);
            }

            int quantity = com.example.cafe_manager.manager.CartManager.getInstance().getProductQuantity(product.getProductId());
            if (quantity > 0) {
                btnAdd.setVisibility(View.GONE);
                layoutQuantity.setVisibility(View.VISIBLE);
                tvQuantity.setText(String.valueOf(quantity));
            } else {
                btnAdd.setVisibility(View.VISIBLE);
                layoutQuantity.setVisibility(View.GONE);
            }

            btnAdd.setOnClickListener(v -> {
                if (listener != null) listener.onIncrease(product);
            });
            btnDecrease.setOnClickListener(v -> {
                if (listener != null) listener.onDecrease(product);
            });
            btnIncrease.setOnClickListener(v -> {
                if (listener != null) listener.onIncrease(product);
            });

        }

    }

    private static final DiffUtil.ItemCallback<ProductEntity> DIFF =

            new DiffUtil.ItemCallback<ProductEntity>() {

                @Override

                public boolean areItemsTheSame(@NonNull ProductEntity o, @NonNull ProductEntity n) {

                    return o.getProductId() == n.getProductId();

                }

                @Override

                public boolean areContentsTheSame(@NonNull ProductEntity o, @NonNull ProductEntity n) {

                    return o.getProductName().equals(n.getProductName())

                            && o.getPrice() == n.getPrice()

                            && o.isActive() == n.isActive();

                }

            };

}
