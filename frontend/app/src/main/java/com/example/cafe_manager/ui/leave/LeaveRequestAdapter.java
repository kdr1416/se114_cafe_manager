package com.example.cafe_manager.ui.leave;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.remote.LeaveRequestResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LeaveRequestAdapter extends RecyclerView.Adapter<LeaveRequestAdapter.ViewHolder> {

    private List<LeaveRequestResponse> items = new ArrayList<>();
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public void setItems(List<LeaveRequestResponse> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leave_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaveRequestResponse item = items.get(position);

        // Format times
        String startStr = dateTimeFormat.format(new Date(item.getStartAt()));
        String endStr = dateTimeFormat.format(new Date(item.getEndAt()));
        holder.tvTimeRange.setText(startStr + " - " + endStr);

        holder.tvReason.setText("Lý do: " + item.getReason());
        holder.tvCreatedAt.setText("Ngày gửi: " + dateTimeFormat.format(new Date(item.getCreatedAt())));

        // Status badge configuration
        String status = item.getStatus() != null ? item.getStatus().toUpperCase() : "PENDING";
        int badgeBg;
        int badgeTextColor;
        String statusLabel;

        switch (status) {
            case "APPROVED":
                statusLabel = "Đã duyệt";
                badgeBg = R.drawable.bg_badge_success;
                badgeTextColor = R.color.success;
                break;
            case "REJECTED":
                statusLabel = "Từ chối";
                badgeBg = R.drawable.bg_badge_accent;
                badgeTextColor = R.color.accent;
                break;
            case "CANCELLED":
                statusLabel = "Đã hủy";
                badgeBg = R.drawable.bg_badge_accent;
                badgeTextColor = R.color.text_soft;
                break;
            case "PENDING":
            default:
                statusLabel = "Chờ duyệt";
                badgeBg = R.drawable.bg_badge_warning;
                badgeTextColor = R.color.warning;
                break;
        }

        holder.tvStatusBadge.setText(statusLabel);
        holder.tvStatusBadge.setBackgroundResource(badgeBg);
        holder.tvStatusBadge.setTextColor(holder.itemView.getContext().getColor(badgeTextColor));

        // Review notes & information
        if (item.getReviewedAt() != null && item.getReviewedAt() > 0) {
            String note = item.getReviewNote() != null ? item.getReviewNote().trim() : "";
            if (note.isEmpty()) {
                note = (status.equals("APPROVED")) ? "Đã được phê duyệt." : "Yêu cầu bị từ chối.";
            }
            holder.tvReviewNote.setText("Phản hồi: " + note);
            holder.tvReviewNote.setVisibility(View.VISIBLE);

            String reviewerName = item.getReviewedByName() != null ? item.getReviewedByName() : "Quản lý";
            String reviewedDateStr = dateTimeFormat.format(new Date(item.getReviewedAt()));
            holder.tvReviewedAt.setText("Duyệt bởi " + reviewerName + " lúc " + reviewedDateStr);
            holder.tvReviewedAt.setVisibility(View.VISIBLE);
        } else {
            holder.tvReviewNote.setVisibility(View.GONE);
            holder.tvReviewedAt.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvCreatedAt, tvStatusBadge, tvTimeRange, tvReason, tvReviewNote, tvReviewedAt;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvTimeRange = itemView.findViewById(R.id.tvTimeRange);
            tvReason = itemView.findViewById(R.id.tvReason);
            tvReviewNote = itemView.findViewById(R.id.tvReviewNote);
            tvReviewedAt = itemView.findViewById(R.id.tvReviewedAt);
        }
    }
}
