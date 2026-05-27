package com.example.cafe_manager.ui.admin;

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
import com.example.cafe_manager.data.local.entity.CategoryEntity;
import com.example.cafe_manager.data.local.entity.ProductEntity;
import com.example.cafe_manager.util.CurrencyUtils;
import com.example.cafe_manager.util.ImageLoader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter cho danh sách món ăn trong Admin.
 * Sử dụng ListAdapter để tối ưu hiệu năng render.
 */
public class AdminProductAdapter
        extends ListAdapter<ProductEntity, AdminProductAdapter.ProductRowVH> {

    public interface OnActionListener {
        void onToggleVisibility(ProductEntity product);
        void onEdit(ProductEntity product);
    }

    private final OnActionListener listener;
    private final Map<Integer, String> categoryNameMap = new HashMap<>();

    public AdminProductAdapter(OnActionListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    /** Cập nhật mapping categoryId → categoryName, gọi mỗi khi categories load xong. */
    public void setCategoryMap(List<CategoryEntity> categories) {
        categoryNameMap.clear();
        if (categories != null) {
            for (CategoryEntity c : categories) {
                categoryNameMap.put(c.getCategoryId(), c.getCategoryName());
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductRowVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_product, parent, false);
        return new ProductRowVH(view, categoryNameMap);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductRowVH holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ProductRowVH extends RecyclerView.ViewHolder {
        private final ImageView ivThumb;
        private final TextView tvName;
        private final TextView tvMeta;
        private final TextView tvStatusBadge;
        private final ImageButton btnToggle;
        private final ImageButton btnEdit;
        private final Map<Integer, String> categoryNameMap;

        ProductRowVH(View itemView, Map<Integer, String> categoryNameMap) {
            super(itemView);
            this.categoryNameMap = categoryNameMap;
            ivThumb = itemView.findViewById(R.id.iv_thumb);
            tvName = itemView.findViewById(R.id.tv_product_name);
            tvMeta = itemView.findViewById(R.id.tv_product_meta);
            tvStatusBadge = itemView.findViewById(R.id.tv_status_badge);
            btnToggle = itemView.findViewById(R.id.btn_toggle);
            btnEdit = itemView.findViewById(R.id.btn_edit);
        }

        void bind(final ProductEntity product, final OnActionListener listener) {
            tvName.setText(product.getProductName());

            String categoryName = categoryNameMap.get(product.getCategoryId());
            if (categoryName == null) categoryName = "—";

            tvMeta.setText(categoryName + " · "
                    + CurrencyUtils.formatVnd(product.getPrice()));

            // Load ảnh dùng ImageLoader (theo yêu cầu 3)
            ImageLoader.loadProductImage(
                    itemView.getContext(),
                    ivThumb,
                    product.getImageUrl()
            );

            boolean active = product.isActive();
            tvStatusBadge.setText(active
                    ? R.string.status_active
                    : R.string.status_inactive);

            tvStatusBadge.setBackgroundResource(active
                    ? R.drawable.bg_badge_success
                    : R.drawable.bg_badge_accent);

            tvStatusBadge.setTextColor(itemView.getContext().getColor(
                    active ? R.color.success : R.color.accent));

            btnToggle.setImageResource(active
                    ? R.drawable.ic_eye
                    : R.drawable.ic_eye_off);

            itemView.setAlpha(active ? 1.0f : 0.7f);

            btnToggle.setOnClickListener(v -> {
                if (listener != null) listener.onToggleVisibility(product);
            });

            btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(product);
            });
        }
    }

    private static final DiffUtil.ItemCallback<ProductEntity> DIFF =
            new DiffUtil.ItemCallback<ProductEntity>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull ProductEntity o, @NonNull ProductEntity n) {
                    return o.getProductId() == n.getProductId();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull ProductEntity o, @NonNull ProductEntity n) {
                    return o.getProductName().equals(n.getProductName())
                            && o.getPrice() == n.getPrice()
                            && o.isActive() == n.isActive()
                            && o.getCategoryId() == n.getCategoryId()
                            && (o.getImageUrl() == null ? n.getImageUrl() == null : o.getImageUrl().equals(n.getImageUrl()));
                }
            };
}
