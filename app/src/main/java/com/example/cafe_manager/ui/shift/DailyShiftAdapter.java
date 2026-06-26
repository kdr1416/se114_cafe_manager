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
import com.example.cafe_manager.data.remote.DailyShiftReportResponse;
import com.example.cafe_manager.util.CurrencyUtils;

public class DailyShiftAdapter extends ListAdapter<DailyShiftReportResponse.ShiftSummary, DailyShiftAdapter.ViewHolder> {

    public interface OnShiftClickListener {
        void onShiftClick(DailyShiftReportResponse.ShiftSummary shift);
    }

    private final OnShiftClickListener listener;

    public DailyShiftAdapter(OnShiftClickListener listener) {
        super(new DiffUtil.ItemCallback<DailyShiftReportResponse.ShiftSummary>() {
            @Override
            public boolean areItemsTheSame(@NonNull DailyShiftReportResponse.ShiftSummary oldItem, @NonNull DailyShiftReportResponse.ShiftSummary newItem) {
                return oldItem.getShiftId().equals(newItem.getShiftId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull DailyShiftReportResponse.ShiftSummary oldItem, @NonNull DailyShiftReportResponse.ShiftSummary newItem) {
                return oldItem.getStatus().equals(newItem.getStatus()) &&
                        oldItem.getRevenue().equals(newItem.getRevenue()) &&
                        oldItem.getOrderCount().equals(newItem.getOrderCount());
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_daily_shift_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvShiftName, tvShiftStatus, tvShiftTime, tvShiftRevenue, tvShiftOrders;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvShiftName = itemView.findViewById(R.id.tv_shift_name);
            tvShiftStatus = itemView.findViewById(R.id.tv_shift_status);
            tvShiftTime = itemView.findViewById(R.id.tv_shift_time);
            tvShiftRevenue = itemView.findViewById(R.id.tv_shift_revenue);
            tvShiftOrders = itemView.findViewById(R.id.tv_shift_orders);
        }

        public void bind(DailyShiftReportResponse.ShiftSummary item, OnShiftClickListener listener) {
            tvShiftName.setText(item.getShiftName());
            tvShiftTime.setText(item.getStartTime() + " - " + item.getEndTime());
            tvShiftStatus.setText(item.getStatus());
            tvShiftRevenue.setText(CurrencyUtils.formatVnd(item.getRevenue() != null ? item.getRevenue() : 0.0));
            tvShiftOrders.setText(item.getOrderCount() + " đơn hàng");

            String status = item.getStatus() != null ? item.getStatus().toUpperCase() : "";
            if ("CLOSED".equals(status)) {
                tvShiftStatus.setBackgroundResource(R.drawable.bg_badge_success);
                tvShiftStatus.setTextColor(itemView.getContext().getColor(R.color.success));
            } else if ("IN_PROGRESS".equals(status)) {
                tvShiftStatus.setBackgroundResource(R.drawable.bg_badge_warning);
                tvShiftStatus.setTextColor(itemView.getContext().getColor(R.color.warning));
            } else {
                tvShiftStatus.setBackgroundResource(R.drawable.bg_badge_accent);
                tvShiftStatus.setTextColor(itemView.getContext().getColor(R.color.text_soft));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onShiftClick(item);
                }
            });
        }
    }
}
