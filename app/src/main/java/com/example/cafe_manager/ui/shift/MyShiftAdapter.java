package com.example.cafe_manager.ui.shift;

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
import com.example.cafe_manager.viewmodel.MyShiftViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyShiftAdapter extends RecyclerView.Adapter<MyShiftAdapter.ViewHolder> {

    public interface OnMyShiftActionListener {
        void onConfirm(int assignmentId);
        void onCheckIn(int shiftId);
        void onCheckOut(int shiftId);
        void onChat(int shiftId);
    }

    private List<MyShiftViewModel.MyShiftItem> items = new ArrayList<>();
    private OnMyShiftActionListener listener;
    private final SimpleDateFormat sdf =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public MyShiftAdapter(OnMyShiftActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<MyShiftViewModel.MyShiftItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_shift, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        MyShiftViewModel.MyShiftItem item = items.get(position);
        ShiftEntity shift = item.shift;

        h.tvNameDate.setText(shift.getShiftName() + " — " + sdf.format(new Date(shift.getShiftDate())));
        h.tvTime.setText(shift.getStartTime() + " — " + shift.getEndTime());

        // Shift status
        String statusLabel;
        int statusColor;
        switch (shift.getStatus()) {
            case Constants.SHIFT_IN_PROGRESS:
                statusLabel = "● Đang mở";
                statusColor = R.color.success;
                break;
            case Constants.SHIFT_CLOSED:
                statusLabel = "● Đã đóng";
                statusColor = R.color.text_soft;
                break;
            case Constants.SHIFT_PUBLISHED:
                statusLabel = "● Sắp tới";
                statusColor = R.color.info;
                break;
            default:
                statusLabel = "● " + shift.getStatus();
                statusColor = R.color.text_mute;
                break;
        }
        h.tvShiftStatus.setText(statusLabel);
        h.tvShiftStatus.setTextColor(h.itemView.getContext().getColor(statusColor));

        // Assignment status text
        if (item.confirmed) {
            h.tvConfirmStatus.setText("✅ Đã xác nhận");
            h.tvConfirmStatus.setTextColor(h.itemView.getContext().getColor(R.color.success));
        } else {
            h.tvConfirmStatus.setText("⏳ Chờ xác nhận");
            h.tvConfirmStatus.setTextColor(h.itemView.getContext().getColor(R.color.warning));
        }

        // Attendance details text
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
            h.tvAttendanceStatus.setText(badgeText);
            h.tvAttendanceStatus.setBackgroundResource(badgeBg);
            h.tvAttendanceStatus.setVisibility(View.VISIBLE);

            StringBuilder details = new StringBuilder();
            details.append("📥 Check-in: ").append(formatTime(item.attendance.getCheckInAt()));
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
            h.tvAttendanceStatus.setVisibility(View.GONE);
            h.tvAttendanceDetails.setVisibility(View.GONE);
        }

        // Dynamic action button based on state
        if (!item.confirmed) {
            h.btnAction.setVisibility(View.VISIBLE);
            h.btnAction.setText("Xác nhận");
            h.btnAction.setOnClickListener(v -> listener.onConfirm(item.assignmentId));
        } else if (item.attendance == null || item.attendance.getCheckInAt() == 0) {
            // Đã xác nhận ca, chưa check-in
            if (Constants.SHIFT_PUBLISHED.equals(shift.getStatus()) || Constants.SHIFT_IN_PROGRESS.equals(shift.getStatus())) {
                h.btnAction.setVisibility(View.VISIBLE);
                h.btnAction.setText("Check-in");
                h.btnAction.setOnClickListener(v -> listener.onCheckIn(shift.getShiftId()));
            } else {
                h.btnAction.setVisibility(View.GONE);
            }
        } else if (item.attendance.getCheckOutAt() == 0) {
            // Đã check-in, chưa check-out
            h.btnAction.setVisibility(View.VISIBLE);
            h.btnAction.setText("Check-out");
            h.btnAction.setOnClickListener(v -> listener.onCheckOut(shift.getShiftId()));
        } else {
            // Đã hoàn tất check-out
            h.btnAction.setVisibility(View.GONE);
        }

        // Chat action button
        boolean canChat = Constants.SHIFT_PUBLISHED.equals(shift.getStatus()) || 
                          Constants.SHIFT_IN_PROGRESS.equals(shift.getStatus()) || 
                          Constants.SHIFT_CLOSED.equals(shift.getStatus());
        h.btnChat.setVisibility(canChat ? View.VISIBLE : View.GONE);
        h.btnChat.setOnClickListener(v -> listener.onChat(shift.getShiftId()));
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) return "--:--";
        return new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(new Date(timestamp));
    }

    @Override
    public int getItemCount() { return items.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvNameDate, tvTime, tvShiftStatus, tvConfirmStatus, tvAttendanceStatus, tvAttendanceDetails;
        public Button btnAction, btnChat;

        public ViewHolder(View v) {
            super(v);
            tvNameDate = v.findViewById(R.id.tv_shift_name_date);
            tvTime = v.findViewById(R.id.tv_time);
            tvShiftStatus = v.findViewById(R.id.tv_shift_status);
            tvConfirmStatus = v.findViewById(R.id.tv_confirm_status);
            tvAttendanceStatus = v.findViewById(R.id.tv_attendance_status);
            tvAttendanceDetails = v.findViewById(R.id.tv_attendance_details);
            btnAction = v.findViewById(R.id.btn_action);
            btnChat = v.findViewById(R.id.btn_chat);
        }
    }
}
