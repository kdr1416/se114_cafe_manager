package com.example.cafe_manager.ui.shift;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cafe_manager.R;
import com.example.cafe_manager.data.remote.ShiftReportResponse;
import com.example.cafe_manager.util.CurrencyUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ShiftOrderHistoryAdapter extends ListAdapter<ShiftReportResponse.ShiftOrderResponse, ShiftOrderHistoryAdapter.ViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(ShiftReportResponse.ShiftOrderResponse order);
    }

    private final OnOrderClickListener listener;

    public ShiftOrderHistoryAdapter(OnOrderClickListener listener) {
        super(new DiffUtil.ItemCallback<ShiftReportResponse.ShiftOrderResponse>() {
            @Override
            public boolean areItemsTheSame(@NonNull ShiftReportResponse.ShiftOrderResponse oldItem, @NonNull ShiftReportResponse.ShiftOrderResponse newItem) {
                return oldItem.getOrderId().equals(newItem.getOrderId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull ShiftReportResponse.ShiftOrderResponse oldItem, @NonNull ShiftReportResponse.ShiftOrderResponse newItem) {
                return oldItem.getStatus().equals(newItem.getStatus()) &&
                        oldItem.getTotalAmount().equals(newItem.getTotalAmount()) &&
                        oldItem.getPaymentMethod().equals(newItem.getPaymentMethod());
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shift_order_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvOrderCode, tvOrderAmount, tvTableName, tvOrderStatus, tvCashierName, tvPaymentMethod;
        private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderCode = itemView.findViewById(R.id.tv_order_code);
            tvOrderAmount = itemView.findViewById(R.id.tv_order_amount);
            tvTableName = itemView.findViewById(R.id.tv_table_name);
            tvOrderStatus = itemView.findViewById(R.id.tv_order_status);
            tvCashierName = itemView.findViewById(R.id.tv_cashier_name);
            tvPaymentMethod = itemView.findViewById(R.id.tv_payment_method);
        }

        public void bind(ShiftReportResponse.ShiftOrderResponse item, OnOrderClickListener listener) {
            tvOrderCode.setText(item.getOrderCode());
            tvOrderAmount.setText(CurrencyUtils.formatVnd(item.getTotalAmount() != null ? item.getTotalAmount() : 0.0));
            tvTableName.setText(item.getTableName());
            
            String status = item.getStatus() != null ? item.getStatus().toUpperCase() : "";
            
            if ("PAID".equals(status)) {
                tvOrderStatus.setTextColor(itemView.getContext().getColor(R.color.success));
                tvOrderStatus.setText("Đã thanh toán");
            } else if ("CANCELLED".equals(status)) {
                tvOrderStatus.setTextColor(itemView.getContext().getColor(R.color.warning));
                tvOrderStatus.setText("Đã hủy");
            } else {
                tvOrderStatus.setTextColor(itemView.getContext().getColor(R.color.text_soft));
                tvOrderStatus.setText(item.getStatus());
            }

            String timeStr = "";
            if (item.getPaidAt() != null && item.getPaidAt() > 0) {
                timeStr = " [" + timeFormat.format(new Date(item.getPaidAt())) + "]";
            }
            tvCashierName.setText("Thu ngân: " + item.getCashierName() + timeStr);

            String pMethod = item.getPaymentMethod() != null ? item.getPaymentMethod().toUpperCase() : "—";
            tvPaymentMethod.setText(pMethod);
            if ("CASH".equals(pMethod)) {
                tvPaymentMethod.setBackgroundResource(R.drawable.bg_badge_accent);
                tvPaymentMethod.setTextColor(itemView.getContext().getColor(R.color.text_soft));
                tvPaymentMethod.setVisibility(View.VISIBLE);
            } else if ("MOMO".equals(pMethod) || "TRANSFER".equals(pMethod)) {
                tvPaymentMethod.setBackgroundResource(R.drawable.bg_badge_success);
                tvPaymentMethod.setTextColor(itemView.getContext().getColor(R.color.success));
                tvPaymentMethod.setVisibility(View.VISIBLE);
            } else {
                tvPaymentMethod.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOrderClick(item);
                }
            });
        }
    }
}
