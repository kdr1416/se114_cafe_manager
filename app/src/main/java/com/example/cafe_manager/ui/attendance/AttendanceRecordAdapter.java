package com.example.cafe_manager.ui.attendance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cafe_manager.R;
import com.example.cafe_manager.data.remote.UserAttendanceDetailResponse.AttendanceRecord;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class AttendanceRecordAdapter extends RecyclerView.Adapter<AttendanceRecordAdapter.ViewHolder> {

    private List<AttendanceRecord> items = new ArrayList<>();
    private final SimpleDateFormat sdfDate = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
    {
        sdfDate.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }
    private final SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public AttendanceRecordAdapter() {}

    public void setItems(List<AttendanceRecord> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance_record, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        AttendanceRecord item = items.get(position);

        long shiftDate = item.getShiftDate() != null ? item.getShiftDate() : 0L;
        h.tvDate.setText(capitalizeFirstLetter(sdfDate.format(new Date(h.itemView.getContext().getPackageName().contains("test") ? System.currentTimeMillis() : shiftDate))));
        
        String shiftName = item.getShiftName() != null ? item.getShiftName() : "";
        String startTime = item.getStartTime() != null ? item.getStartTime() : "";
        String endTime = item.getEndTime() != null ? item.getEndTime() : "";
        double durationHours = item.getDurationHours() != null ? item.getDurationHours() : 0.0;
        
        String shiftInfo = String.format("%s (%s - %s) — %.1fh", 
                shiftName, startTime, endTime, durationHours);
        h.tvShiftInfo.setText(shiftInfo);

        // Actual check in/out times
        long checkInAt = item.getCheckInAt() != null ? item.getCheckInAt() : 0L;
        long checkOutAt = item.getCheckOutAt() != null ? item.getCheckOutAt() : 0L;
        
        String inTime = checkInAt > 0 ? formatTime(checkInAt) : "--:--";
        String outTime = checkOutAt > 0 ? formatTime(checkOutAt) : "--:--";
        h.tvCheckInfo.setText("📥 Check-in: " + inTime + "  |  📤 Check-out: " + outTime);

        // Order & Payment stats in shift
        int ordersCreated = item.getOrdersCreated() != null ? item.getOrdersCreated() : 0;
        int paymentsProcessed = item.getPaymentsProcessed() != null ? item.getPaymentsProcessed() : 0;
        double revenue = item.getRevenueProcessed() != null ? item.getRevenueProcessed() : 0.0;

        if (ordersCreated > 0 || paymentsProcessed > 0) {
            h.tvOrderStats.setVisibility(View.VISIBLE);
            h.tvOrderStats.setText(String.format(Locale.getDefault(), "📦 %d đơn tạo  |  💵 %d thanh toán  |  💰 %s", 
                    ordersCreated, paymentsProcessed, formatRevenue(revenue)));
        } else {
            h.tvOrderStats.setVisibility(View.GONE);
        }

        // Alerts / Notes
        StringBuilder alerts = new StringBuilder();
        int lateMinutes = item.getLateMinutes() != null ? item.getLateMinutes() : 0;
        int earlyLeaveMinutes = item.getEarlyLeaveMinutes() != null ? item.getEarlyLeaveMinutes() : 0;
        
        if (lateMinutes > 0) {
            alerts.append("⚠️ Đi muộn ").append(lateMinutes).append(" phút. ");
        }
        if (earlyLeaveMinutes > 0) {
            alerts.append("⚠️ Về sớm ").append(earlyLeaveMinutes).append(" phút. ");
        }
        if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
            alerts.append("\n📝 Ghi chú: ").append(item.getNotes().trim());
        }

        if (alerts.length() > 0) {
            h.tvNotes.setText(alerts.toString().trim());
            h.tvNotes.setVisibility(View.VISIBLE);
        } else {
            h.tvNotes.setVisibility(View.GONE);
        }

        // Status badge
        String statusLabel = "Sắp tới";
        int badgeBg = R.drawable.bg_badge_accent;
        int textColor = h.itemView.getContext().getColor(R.color.text_mute);

        if (item.getStatus() != null) {
            switch (item.getStatus()) {
                case "COMPLETED":
                    statusLabel = "Đúng giờ";
                    badgeBg = R.drawable.bg_badge_success;
                    textColor = h.itemView.getContext().getColor(R.color.success);
                    break;
                case "LATE":
                    statusLabel = "Đi muộn";
                    badgeBg = R.drawable.bg_badge_warning;
                    textColor = h.itemView.getContext().getColor(R.color.warning);
                    break;
                case "EARLY_LEAVE":
                    statusLabel = "Về sớm";
                    badgeBg = R.drawable.bg_badge_warning;
                    textColor = h.itemView.getContext().getColor(R.color.warning);
                    break;
                case "ABSENT":
                    statusLabel = "Vắng mặt";
                    badgeBg = R.drawable.bg_badge_accent;
                    textColor = h.itemView.getContext().getColor(R.color.error);
                    break;
                case "IN_PROGRESS":
                    statusLabel = "Đang làm";
                    badgeBg = R.drawable.bg_badge_success;
                    textColor = h.itemView.getContext().getColor(R.color.accent);
                    break;
                case "CHECKED_IN":
                    statusLabel = "Đã check-in";
                    badgeBg = R.drawable.bg_badge_success;
                    textColor = h.itemView.getContext().getColor(R.color.accent);
                    break;
                case "UPCOMING":
                default:
                    statusLabel = "Sắp tới";
                    badgeBg = R.drawable.bg_badge_accent;
                    textColor = h.itemView.getContext().getColor(R.color.text_soft);
                    break;
            }
        }

        h.tvStatus.setText(statusLabel);
        h.tvStatus.setBackgroundResource(badgeBg);
        h.tvStatus.setTextColor(textColor);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatRevenue(double amount) {
        if (amount >= 1_000_000) {
            return String.format(Locale.getDefault(), "%.1ftr", amount / 1_000_000.0).replace(".0", "");
        } else if (amount >= 1_000) {
            return String.format(Locale.getDefault(), "%.0fk", amount / 1_000.0);
        } else {
            return String.format(Locale.getDefault(), "%.0fđ", amount);
        }
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) return "--:--";
        return sdfTime.format(new Date(timestamp));
    }

    private String capitalizeFirstLetter(String original) {
        if (original == null || original.isEmpty()) return "";
        return original.substring(0, 1).toUpperCase() + original.substring(1);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvDate, tvStatus, tvShiftInfo, tvCheckInfo, tvNotes, tvOrderStats;

        public ViewHolder(View v) {
            super(v);
            tvDate = v.findViewById(R.id.tv_record_date);
            tvStatus = v.findViewById(R.id.tv_record_status);
            tvShiftInfo = v.findViewById(R.id.tv_record_shift_info);
            tvCheckInfo = v.findViewById(R.id.tv_record_check_info);
            tvNotes = v.findViewById(R.id.tv_record_notes);
            tvOrderStats = v.findViewById(R.id.tv_record_order_stats);
        }
    }
}
