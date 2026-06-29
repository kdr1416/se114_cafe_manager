package com.example.cafe_manager.ui.scheduling;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.data.remote.ShiftSuggestion;
import com.example.cafe_manager.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class SchedulingPreviewAdapter extends RecyclerView.Adapter<SchedulingPreviewAdapter.ViewHolder> {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    {
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }
    private List<ShiftSuggestion> suggestions = new java.util.ArrayList<>();

    public void setItems(List<ShiftSuggestion> items) {
        this.suggestions = items != null ? items : new java.util.ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shift_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShiftSuggestion item = suggestions.get(position);

        // Template name (bold)
        holder.tvShiftName.setText(item.getTemplateName() != null ? item.getTemplateName() : "Ca làm việc");

        // Date + Time
        String dateTime = "";
        if (item.getShiftDate() != null) {
            dateTime = dateFormat.format(new Date(item.getShiftDate()));
        }
        if (item.getStartTime() != null && item.getEndTime() != null) {
            dateTime += " " + item.getStartTime() + "–" + item.getEndTime();
        }
        holder.tvShiftDateTime.setText(dateTime);

        // Suggested staff
        if (item.getSuggestedUserNames() != null && !item.getSuggestedUserNames().isEmpty()) {
            StringBuilder names = new StringBuilder();
            for (String name : item.getSuggestedUserNames()) {
                if (names.length() > 0) names.append(", ");
                names.append(name);
            }
            holder.tvSuggestedStaff.setText(names.toString());
        } else {
            holder.tvSuggestedStaff.setText("Chưa có nhân viên");
        }

        // Status badge
        Boolean fulfilled = item.getIsFulfilled();
        Integer missing = item.getMissingCount();
        if (fulfilled != null && fulfilled) {
            holder.tvStatusBadge.setText("✅ Đủ người");
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.success));
        } else {
            int count = missing != null ? missing : 0;
            holder.tvStatusBadge.setText("⚠️ Thiếu " + count + " người");
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.warning));
        }
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvShiftName;
        TextView tvShiftDateTime;
        TextView tvSuggestedStaff;
        TextView tvStatusBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvShiftName = itemView.findViewById(R.id.tv_shift_name);
            tvShiftDateTime = itemView.findViewById(R.id.tv_shift_date_time);
            tvSuggestedStaff = itemView.findViewById(R.id.tv_suggested_staff);
            tvStatusBadge = itemView.findViewById(R.id.tv_status_badge);
        }
    }
}
