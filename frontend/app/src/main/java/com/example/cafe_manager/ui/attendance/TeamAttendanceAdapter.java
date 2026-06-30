package com.example.cafe_manager.ui.attendance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cafe_manager.R;
import com.example.cafe_manager.data.remote.TeamAttendanceSummary;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TeamAttendanceAdapter extends RecyclerView.Adapter<TeamAttendanceAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(TeamAttendanceSummary summary);
    }

    private List<TeamAttendanceSummary> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public TeamAttendanceAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<TeamAttendanceSummary> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_team_attendance, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        TeamAttendanceSummary item = items.get(position);

        String fullName = item.getFullName() != null ? item.getFullName() : "";
        String role = item.getRole() != null ? item.getRole() : "";
        
        h.tvName.setText(fullName);
        h.tvRole.setText(role);
        h.tvInitials.setText(getInitials(fullName));

        int attendedShifts = item.getAttendedShifts() != null ? item.getAttendedShifts() : 0;
        double totalHoursWorked = item.getTotalHoursWorked() != null ? item.getTotalHoursWorked() : 0.0;
        double attendanceRate = item.getAttendanceRate() != null ? item.getAttendanceRate() : 0.0;

        String shiftsHours = String.format(Locale.getDefault(), "%d ca — %.1fh", attendedShifts, totalHoursWorked);
        h.tvShiftsHours.setText(shiftsHours);

        int orders = item.getOrdersCreated() != null ? item.getOrdersCreated() : 0;
        double revenue = item.getRevenueProcessed() != null ? item.getRevenueProcessed() : 0.0;
        h.tvOrderStats.setText(String.format(Locale.getDefault(), "📦 %d đơn · 💰 %s", orders, formatRevenue(revenue)));

        String rateStr = String.format(Locale.getDefault(), "%.1f%%", attendanceRate);
        h.tvRate.setText(rateStr);

        // Dynamic rate badge styling
        int badgeBg;
        int textColor;
        if (attendanceRate >= 90.0) {
            badgeBg = R.drawable.bg_badge_success;
            textColor = h.itemView.getContext().getColor(R.color.success);
        } else if (attendanceRate >= 75.0) {
            badgeBg = R.drawable.bg_badge_warning;
            textColor = h.itemView.getContext().getColor(R.color.warning);
        } else {
            badgeBg = R.drawable.bg_badge_accent;
            textColor = h.itemView.getContext().getColor(R.color.error);
        }
        h.tvRate.setBackgroundResource(badgeBg);
        h.tvRate.setTextColor(textColor);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
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

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "NV";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        String first = parts[0].substring(0, 1);
        String last = parts[parts.length - 1].substring(0, 1);
        return (first + last).toUpperCase();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvInitials, tvName, tvRole, tvShiftsHours, tvRate, tvOrderStats;

        public ViewHolder(View v) {
            super(v);
            tvInitials = v.findViewById(R.id.tv_initials);
            tvName = v.findViewById(R.id.tv_employee_name);
            tvRole = v.findViewById(R.id.tv_role);
            tvShiftsHours = v.findViewById(R.id.tv_shifts_hours);
            tvRate = v.findViewById(R.id.tv_attendance_rate);
            tvOrderStats = v.findViewById(R.id.tv_order_stats);
        }
    }
}
