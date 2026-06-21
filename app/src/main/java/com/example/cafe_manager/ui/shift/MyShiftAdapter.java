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
        h.tvShiftStatus.setTextColor(h.itemView.getContext().getResources().getColor(statusColor));

        // Assignment status
        if (item.confirmed) {
            h.tvConfirmStatus.setText("✅ Đã xác nhận");
            h.tvConfirmStatus.setTextColor(h.itemView.getContext().getResources().getColor(R.color.success));
            h.btnConfirm.setVisibility(View.GONE);
        } else {
            h.tvConfirmStatus.setText("⏳ Chờ xác nhận");
            h.tvConfirmStatus.setTextColor(h.itemView.getContext().getResources().getColor(R.color.warning));
            h.btnConfirm.setVisibility(View.VISIBLE);
            h.btnConfirm.setOnClickListener(v -> listener.onConfirm(item.assignmentId));
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvNameDate, tvTime, tvShiftStatus, tvConfirmStatus;
        public Button btnConfirm;

        public ViewHolder(View v) {
            super(v);
            tvNameDate = v.findViewById(R.id.tv_shift_name_date);
            tvTime = v.findViewById(R.id.tv_time);
            tvShiftStatus = v.findViewById(R.id.tv_shift_status);
            tvConfirmStatus = v.findViewById(R.id.tv_confirm_status);
            btnConfirm = v.findViewById(R.id.btn_confirm);
        }
    }
}
