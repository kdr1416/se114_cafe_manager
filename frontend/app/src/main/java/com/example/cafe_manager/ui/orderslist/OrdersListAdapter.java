package com.example.cafe_manager.ui.orderslist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.model.OrderWithItems;
import com.example.cafe_manager.util.CurrencyUtils;
import com.example.cafe_manager.util.DateTimeUtils;
import com.example.cafe_manager.util.StatusUtils;
import com.example.cafe_manager.viewmodel.OrdersListViewModel;

public class OrdersListAdapter
        extends ListAdapter<OrderWithItems, OrdersListAdapter.OrderRowVH> {

    public interface OnCollectListener {
        void onCollect(OrderWithItems order);
    }

    public interface OnCancelListener {
        void onCancel(OrderWithItems order);
    }

    public interface OnAddMoreListener {
        void onAddMore(OrderWithItems order);
    }

    public interface OnServeListener {
        void onServe(OrderWithItems order);
    }

    private final OnCollectListener collectListener;
    private final OnCancelListener cancelListener;
    private final OnAddMoreListener addMoreListener;
    private final OnServeListener serveListener;

    public OrdersListAdapter(OnCollectListener collectListener,
                             OnCancelListener cancelListener,
                             OnAddMoreListener addMoreListener,
                             OnServeListener serveListener) {
        super(DIFF);
        this.collectListener = collectListener;
        this.cancelListener = cancelListener;
        this.addMoreListener = addMoreListener;
        this.serveListener = serveListener;
    }

    @NonNull
    @Override
    public OrderRowVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_orders_list, parent, false);
        return new OrderRowVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderRowVH holder, int position) {
        holder.bind(getItem(position), collectListener, cancelListener, addMoreListener, serveListener);
    }

    static class OrderRowVH extends RecyclerView.ViewHolder {
        private final TextView tvTableName;
        private final TextView tvOrderCode;
        private final TextView tvStatusBadge;
        private final TextView tvItems;
        private final TextView tvTotal;
        private final Button btnPayment;
        private final Button btnCancel;
        private final Button btnServe;
        private final View viewStatusIndicator;

        OrderRowVH(View itemView) {
            super(itemView);
            tvTableName = itemView.findViewById(R.id.tv_table_name);
            tvOrderCode = itemView.findViewById(R.id.tv_order_code);
            tvStatusBadge = itemView.findViewById(R.id.tv_status_badge);
            tvItems = itemView.findViewById(R.id.tv_items);
            tvTotal = itemView.findViewById(R.id.tv_total);
            btnPayment = itemView.findViewById(R.id.btn_payment);
            btnCancel = itemView.findViewById(R.id.btn_cancel);
            btnServe = itemView.findViewById(R.id.btn_serve);
            viewStatusIndicator = itemView.findViewById(R.id.view_status_indicator);
        }

        void bind(final OrderWithItems data,
                  final OnCollectListener collectListener,
                  final OnCancelListener cancelListener,
                  final OnAddMoreListener addMoreListener,
                  final OnServeListener serveListener) {
            String tableName = data.getTableName();
            if (tableName != null && !tableName.isEmpty()) {
                tvTableName.setText(tableName);
            } else {
                tvTableName.setText("Bàn #" + data.getOrder().getTableId());
            }

            String time = DateTimeUtils.formatDateTime(data.getOrder().getCreatedAt());
            tvOrderCode.setText(time + " · #" + data.getOrder().getOrderCode());

            String status = data.getOrder().getStatus();
            tvStatusBadge.setText(StatusUtils.getDisplayName(status));

            if (com.example.cafe_manager.util.Constants.ORDER_SERVED.equals(status)) {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_success);
                tvStatusBadge.setTextColor(itemView.getContext().getColor(R.color.success));
                viewStatusIndicator.setBackgroundColor(itemView.getContext().getColor(R.color.success));
                btnServe.setVisibility(View.GONE);
                // Make payment button Primary
                btnPayment.setBackgroundResource(R.drawable.bg_button_primary);
                btnPayment.setTextColor(itemView.getContext().getColor(R.color.text_on_accent));
            } else {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_warning);
                tvStatusBadge.setTextColor(itemView.getContext().getColor(R.color.warning));
                viewStatusIndicator.setBackgroundColor(itemView.getContext().getColor(R.color.warning));
                btnServe.setVisibility(View.VISIBLE);
                // Make payment button Secondary
                btnPayment.setBackgroundResource(R.drawable.bg_button_secondary);
                btnPayment.setTextColor(itemView.getContext().getColor(R.color.text_primary));
            }

            tvItems.setText(OrdersListViewModel.buildItemsSummary(data.getItems()));

            double total = OrdersListViewModel.calculateTotal(data.getItems());
            tvTotal.setText(CurrencyUtils.formatVnd(total));

            // Click vào row (không phải buttons) → "gọi thêm món"
            itemView.setOnClickListener(v -> {
                if (addMoreListener != null) addMoreListener.onAddMore(data);
            });

            btnPayment.setOnClickListener(v -> {
                if (collectListener != null) collectListener.onCollect(data);
            });

            btnCancel.setOnClickListener(v -> {
                if (cancelListener != null) cancelListener.onCancel(data);
            });

            btnServe.setOnClickListener(v -> {
                if (serveListener != null) serveListener.onServe(data);
            });
        }
    }

    private static final DiffUtil.ItemCallback<OrderWithItems> DIFF =
            new DiffUtil.ItemCallback<OrderWithItems>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull OrderWithItems o, @NonNull OrderWithItems n) {
                    return o.getOrder().getOrderId() == n.getOrder().getOrderId();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull OrderWithItems o, @NonNull OrderWithItems n) {
                    return o.getOrder().getStatus().equals(n.getOrder().getStatus())
                            && o.getItems().size() == n.getItems().size();
                }
            };
}
