package com.example.cafe_manager.ui.menu;

import android.view.LayoutInflater;

import android.view.View;
import com.example.cafe_manager.util.ImageLoader;
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

    public interface OnAddToCartListener {

        void onAddToCart(ProductEntity product);

    }

    private final OnAddToCartListener listener;

    public ProductAdapter(OnAddToCartListener listener) {

        super(DIFF);

        this.listener = listener;

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

        holder.bind(getItem(position), listener);

    }

    static class ProductVH extends RecyclerView.ViewHolder {

        private final ImageView ivMedia;

        private final TextView tvName;

        private final TextView tvPrice;

        private final TextView tvCategoryTag;

        private final ImageButton btnAdd;

        ProductVH(View itemView) {

            super(itemView);

            ivMedia = itemView.findViewById(R.id.iv_media);

            tvName = itemView.findViewById(R.id.tv_product_name);

            tvPrice = itemView.findViewById(R.id.tv_product_price);

            tvCategoryTag = itemView.findViewById(R.id.tv_category_tag);

            btnAdd = itemView.findViewById(R.id.btn_add);

        }

        void bind(final ProductEntity product, final OnAddToCartListener listener) {

            tvName.setText(product.getProductName());

            tvPrice.setText(CurrencyUtils.formatVnd(product.getPrice()));

            // MVP: tạm dùng icon mặc định + tag rỗng.

            // Tối ưu sau: pass thêm Map<categoryId, CategoryEntity> để show tên + đổi icon.

            tvCategoryTag.setVisibility(View.GONE);

            ImageLoader.loadProductImage(
                    itemView.getContext(),
                    ivMedia,
                    product.getImageUrl()   // null/empty → fallback ic_coffee
            );

            btnAdd.setOnClickListener(v -> {

                if (listener != null) listener.onAddToCart(product);

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
                            && o.isActive() == n.isActive()
                            && o.getCategoryId() == n.getCategoryId();

                }

            };

}
