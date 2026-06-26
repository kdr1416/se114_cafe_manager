package com.example.cafe_manager.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.util.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TodayShiftAdapter extends RecyclerView.Adapter<TodayShiftAdapter.ViewHolder> {

    public interface OnTodayShiftActionListener {
        void onCheckIn(int shiftId);
        void onCheckOut(int shiftId);
    }

    private List<TodayShiftItem> items = new ArrayList<>();
    private final OnTodayShiftActionListener listener;
    private final boolean isStaff;

    public TodayShiftAdapter(boolean isStaff, OnTodayShiftActionListener listener) {
        this.isStaff = isStaff;
        this.listener = listener;
    }

    public void setItems(List<TodayShiftItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_today_shift, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        TodayShiftItem item = items.get(position);
        ShiftEntity shift = item.shift;

        h.tvShiftName.setText(shift.getShiftName());
        h.tvShiftTime.setText(shift.getStartTime() + " — " + shift.getEndTime());

        // Shift Status
        String statusLabel;
        int statusColor;
        switch (shift.getStatus()) {
            case Constants.SHIFT_IN_PROGRESS:
                statusLabel = "● Đang mở";
                statusColor = R.color.success;
                h.tvShiftStatus.setBackgroundResource(R.drawable.bg_badge_success);
                break;
            case Constants.SHIFT_CLOSED:
                statusLabel = "● Đã đóng";
                statusColor = R.color.text_soft;
                h.tvShiftStatus.setBackgroundResource(R.drawable.bg_media);
                break;
            case Constants.SHIFT_PUBLISHED:
                statusLabel = "● Sắp tới";
                statusColor = R.color.info;
                h.tvShiftStatus.setBackgroundResource(R.drawable.bg_badge_accent);
                break;
            default:
                statusLabel = "● " + shift.getStatus();
                statusColor = R.color.text_mute;
                h.tvShiftStatus.setBackgroundResource(R.drawable.bg_media);
                break;
        }
        h.tvShiftStatus.setText(statusLabel);
        h.tvShiftStatus.setTextColor(h.itemView.getContext().getColor(statusColor));

        if (isStaff) {
            // STAFF Role specific view
            h.tvStaffCount.setVisibility(View.GONE);

            // Attendance Status & Details
            if (item.attendance != null && item.attendance.getCheckInAt() > 0) {
                String attStatus = item.attendance.getStatus();
                String badgeText = attStatus;
                int badgeBg = R.drawable.bg_badge_success;

                switch (attStatus) {
                    case Constants.ATTENDANCE_CHECKED_IN:
                        badgeText = "✅ Đã check-in";
                        badgeBg = R.drawable.bg_badge_success;
                        break;
                    case Constants.ATTENDANCE_LATE:
                        badgeText = "⏰ Đi muộn";
                        badgeBg = R.drawable.bg_badge_warning;
                        break;
                    case Constants.ATTENDANCE_EARLY_LEAVE:
                        badgeText = "⚡ Về sớm";
                        badgeBg = R.drawable.bg_badge_warning;
                        break;
                    case Constants.ATTENDANCE_COMPLETED:
                        badgeText = "✔️ Hoàn thành";
                        badgeBg = R.drawable.bg_badge_success;
                        break;
                    case Constants.ATTENDANCE_ABSENT:
                        badgeText = "❌ Vắng mặt";
                        badgeBg = R.drawable.bg_badge_warning;
                        break;
                }
                
                // Reuse tvShiftStatus for attendance state badge if checked in, or just prepend to attendance details
                StringBuilder details = new StringBuilder();
                details.append("Trạng thái: ").append(badgeText);
                details.append("\n📥 Check-in: ").append(formatTime(item.attendance.getCheckInAt()));
                if (item.attendance.getLateMinutes() > 0) {
                    details.append(" (muộn ").append(item.attendance.getLateMinutes()).append(" phút)");
                }
                if (item.attendance.getCheckOutAt() > 0) {
                    details.append("\n📤 Check-out: ").append(formatTime(item.attendance.getCheckOutAt()));
                    if (item.attendance.getEarlyLeaveMinutes() > 0) {
                        details.append(" (sớm ").append(item.attendance.getEarlyLeaveMinutes()).append(" phút)");
                    }
                }
                h.tvAttendanceDetails.setText(details.toString());
                h.tvAttendanceDetails.setVisibility(View.VISIBLE);
            } else {
                h.tvAttendanceDetails.setVisibility(View.GONE);
            }

            // Dynamic Action Button (Check-in / Check-out)
            if (item.attendance == null || item.attendance.getCheckInAt() == 0) {
                // Chưa check-in
                if (Constants.SHIFT_PUBLISHED.equals(shift.getStatus()) || Constants.SHIFT_IN_PROGRESS.equals(shift.getStatus())) {
                    h.btnAction.setVisibility(View.VISIBLE);
                    h.btnAction.setText("Check-in");
                    h.btnAction.setOnClickListener(v -> {
                        if (listener != null) listener.onCheckIn(shift.getShiftId());
                    });
                } else {
                    h.btnAction.setVisibility(View.GONE);
                }
            } else if (item.attendance.getCheckOutAt() == 0) {
                // Đã check-in, chưa check-out
                h.btnAction.setVisibility(View.VISIBLE);
                h.btnAction.setText("Check-out");
                h.btnAction.setOnClickListener(v -> {
                    if (listener != null) listener.onCheckOut(shift.getShiftId());
                });
            } else {
                // Đã hoàn tất check-out
                h.btnAction.setVisibility(View.GONE);
            }
        } else {
            // MANAGER / ADMIN specific view
            h.tvStaffCount.setVisibility(View.VISIBLE);
            h.tvStaffCount.setText("👥 " + item.staffCount + " nhân sự");
            h.tvAttendanceDetails.setVisibility(View.GONE);
            h.btnAction.setVisibility(View.GONE);
        }
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) return "--:--";
        return new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(new Date(timestamp));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvShiftName;
        public final TextView tvShiftStatus;
        public final TextView tvShiftTime;
        public final TextView tvStaffCount;
        public final TextView tvAttendanceDetails;
        public final Button btnAction;

        public ViewHolder(@NonNull View v) {
            super(v);
            tvShiftName = v.findViewById(R.id.tv_shift_name);
            tvShiftStatus = v.findViewById(R.id.tv_shift_status);
            tvShiftTime = v.findViewById(R.id.tv_shift_time);
            tvStaffCount = v.findViewById(R.id.tv_staff_count);
            tvAttendanceDetails = v.findViewById(R.id.tv_attendance_details);
            btnAction = v.findViewById(R.id.btn_action);
        }
    }
}
