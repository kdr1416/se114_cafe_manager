package com.example.cafe_manager.ui.orderslist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.model.OrderWithItems;

import java.util.ArrayList;
import java.util.List;

public class OrdersListAdapter
        extends RecyclerView.Adapter<OrdersListAdapter.OrderVH> {

    public interface OnPaymentClickListener {
        void onPayment(OrderWithItems order);
    }

    private final List<OrderWithItems> list = new ArrayList<>();
    private final OnPaymentClickListener listener;

    public OrdersListAdapter(OnPaymentClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<OrderWithItems> data) {
        list.clear();

        if (data != null) {
            list.addAll(data);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_orders_list, parent, false);

        return new OrderVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderVH holder, int position) {
        holder.bind(list.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class OrderVH extends RecyclerView.ViewHolder {

        private final TextView tvTable;
        private final TextView tvCode;
        private final TextView tvItems;
        private final TextView tvTotal;
        private final Button btnPayment;

        public OrderVH(@NonNull View itemView) {
            super(itemView);

            tvTable = itemView.findViewById(R.id.tv_table_name);
            tvCode = itemView.findViewById(R.id.tv_order_code);
            tvItems = itemView.findViewById(R.id.tv_items);
            tvTotal = itemView.findViewById(R.id.tv_total);
            btnPayment = itemView.findViewById(R.id.btn_payment);
        }

        void bind(
                OrderWithItems order,
                OnPaymentClickListener listener
        ) {

            tvTable.setText("Bàn");

            tvCode.setText(
                    "Order #" + order.getOrder().getOrderId()
            );

            tvItems.setText(
                    order.getItems().size() + " món"
            );

            tvTotal.setText(
                    String.valueOf(order.getOrder().getTotalAmount())
            );

            btnPayment.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPayment(order);
                }
            });
        }
    }
}
