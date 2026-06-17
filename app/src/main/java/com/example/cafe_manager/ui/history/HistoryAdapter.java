package com.example.cafe_manager.ui.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.model.OrderWithItems;
import com.example.cafe_manager.util.CurrencyUtils;
import com.example.cafe_manager.util.DateTimeUtils;
import com.example.cafe_manager.viewmodel.OrdersListViewModel;

public class HistoryAdapter
        extends ListAdapter<OrderWithItems, HistoryAdapter.HistoryVH> {

    public interface OnHistoryClickListener {
        void onClick(OrderWithItems data);
    }

    private final OnHistoryClickListener listener;

    public HistoryAdapter(OnHistoryClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryVH holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class HistoryVH extends RecyclerView.ViewHolder {
        private final TextView tvInvoiceCode;
        private final TextView tvMeta;
        private final TextView tvItemsSummary;
        private final TextView tvTotal;

        HistoryVH(View itemView) {
            super(itemView);
            tvInvoiceCode = itemView.findViewById(R.id.tv_invoice_code);
            tvMeta = itemView.findViewById(R.id.tv_meta);
            tvItemsSummary = itemView.findViewById(R.id.tv_items_summary);
            tvTotal = itemView.findViewById(R.id.tv_total);
        }

        void bind(final OrderWithItems data, final OnHistoryClickListener listener) {
            tvInvoiceCode.setText("#" + data.getOrder().getOrderCode());

            String time = DateTimeUtils.formatDateTime(data.getOrder().getPaidAt());
            tvMeta.setText("Bàn #" + data.getOrder().getTableId() + " · " + time);

            tvItemsSummary.setText(OrdersListViewModel.buildItemsSummary(data.getItems()));

            // Trong History, total chuẩn xác hơn là dùng order.totalAmount
            tvTotal.setText(CurrencyUtils.formatVnd(data.getOrder().getTotalAmount()));

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClick(data);
            });
        }
    }

    private static final DiffUtil.ItemCallback<OrderWithItems> DIFF =
            new DiffUtil.ItemCallback<OrderWithItems>() {
                @Override
                public boolean areItemsTheSame(@NonNull OrderWithItems o, @NonNull OrderWithItems n) {
                    return o.getOrder().getOrderId() == n.getOrder().getOrderId();
                }
                @Override
                public boolean areContentsTheSame(@NonNull OrderWithItems o, @NonNull OrderWithItems n) {
                    return o.getOrder().getPaidAt() == n.getOrder().getPaidAt()
                            && o.getItems().size() == n.getItems().size();
                }
            };
}
