package com.example.cafe_manager.ui.payment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.entity.OrderItemEntity;
import com.example.cafe_manager.util.CurrencyUtils;

public class InvoiceItemAdapter extends ListAdapter<OrderItemEntity, InvoiceItemAdapter.ItemVH> {

    public InvoiceItemAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public ItemVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invoice_item, parent, false);
        return new ItemVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemVH holder, int position) {
        holder.bind(getItem(position));
    }

    static class ItemVH extends RecyclerView.ViewHolder {
        private final TextView tvSummary;
        private final TextView tvTotal;

        ItemVH(View itemView) {
            super(itemView);
            tvSummary = itemView.findViewById(R.id.tv_item_summary);
            tvTotal = itemView.findViewById(R.id.tv_item_total);
        }

        void bind(OrderItemEntity item) {
            tvSummary.setText(item.getProductNameSnapshot()
                    + " × " + item.getQuantity());
            tvTotal.setText(CurrencyUtils.formatVnd(item.getSubtotal()));
        }
    }

    private static final DiffUtil.ItemCallback<OrderItemEntity> DIFF =
            new DiffUtil.ItemCallback<OrderItemEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull OrderItemEntity o, @NonNull OrderItemEntity n) {
                    return o.getOrderItemId() == n.getOrderItemId();
                }
                @Override
                public boolean areContentsTheSame(@NonNull OrderItemEntity o, @NonNull OrderItemEntity n) {
                    return o.getQuantity() == n.getQuantity()
                            && o.getSubtotal() == n.getSubtotal();
                }
            };
}