package com.example.cafe_manager.ui.order;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.model.CartItem;
import com.example.cafe_manager.util.CurrencyUtils;

public class OrderItemAdapter extends ListAdapter<CartItem, OrderItemAdapter.OrderItemVH> {

    public interface OnQuantityChangeListener {
        void onIncrease(int productId);
        void onDecrease(int productId);  // qty 1 → decrease = remove
    }

    private final OnQuantityChangeListener listener;

    public OrderItemAdapter(OnQuantityChangeListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderItemVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_item, parent, false);
        return new OrderItemVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemVH holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class OrderItemVH extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvUnitPrice;
        private final TextView tvQuantity;
        private final TextView tvSubtotal;
        private final ImageButton btnDecrease;
        private final ImageButton btnIncrease;

        OrderItemVH(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_product_name);
            tvUnitPrice = itemView.findViewById(R.id.tv_unit_price);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
            tvSubtotal = itemView.findViewById(R.id.tv_subtotal);
            btnDecrease = itemView.findViewById(R.id.btn_decrease);
            btnIncrease = itemView.findViewById(R.id.btn_increase);
        }

        void bind(final CartItem item, final OnQuantityChangeListener listener) {
            tvName.setText(item.getProductName());
            tvUnitPrice.setText(itemView.getContext().getString(
                    R.string.label_unit_price,
                    CurrencyUtils.formatVnd(item.getUnitPrice())));
            tvQuantity.setText(String.valueOf(item.getQuantity()));
            tvSubtotal.setText(CurrencyUtils.formatVnd(item.getSubtotal()));

            btnDecrease.setOnClickListener(v -> {
                if (listener != null) listener.onDecrease(item.getProductId());
            });
            btnIncrease.setOnClickListener(v -> {
                if (listener != null) listener.onIncrease(item.getProductId());
            });
        }
    }

    private static final DiffUtil.ItemCallback<CartItem> DIFF =
            new DiffUtil.ItemCallback<CartItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull CartItem o, @NonNull CartItem n) {
                    return o.getProductId() == n.getProductId();
                }
                @Override
                public boolean areContentsTheSame(@NonNull CartItem o, @NonNull CartItem n) {
                    return o.getQuantity() == n.getQuantity()
                            && o.getUnitPrice() == n.getUnitPrice()
                            && o.getProductName().equals(n.getProductName());
                }
            };
}