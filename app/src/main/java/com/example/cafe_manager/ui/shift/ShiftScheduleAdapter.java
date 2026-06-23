package com.example.cafe_manager.ui.shift;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.viewmodel.ShiftScheduleViewModel;

import java.util.ArrayList;
import java.util.List;

public class ShiftScheduleAdapter extends RecyclerView.Adapter<ShiftScheduleAdapter.ViewHolder> {

    public interface OnShiftActionListener {
        void onPublish(ShiftEntity shift);
        void onOpenShift(ShiftEntity shift);
        void onAssignStaff(ShiftEntity shift);
        void onCloseShift(ShiftEntity shift);
        void onViewReport(ShiftEntity shift);
        void onCancel(ShiftEntity shift);
        void onChatShift(ShiftEntity shift);
    }

    private List<ShiftScheduleViewModel.ShiftDisplayItem> items = new ArrayList<>();
    private OnShiftActionListener listener;

    public ShiftScheduleAdapter(OnShiftActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ShiftScheduleViewModel.ShiftDisplayItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shift_schedule, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        ShiftScheduleViewModel.ShiftDisplayItem item = items.get(position);
        ShiftEntity shift = item.shift;

        h.tvName.setText(shift.getShiftName());
        h.tvTime.setText(shift.getStartTime() + " — " + shift.getEndTime());
        h.tvStaffCount.setText(item.assignedCount + " nhân viên");

        // Warning understaffed
        if (item.understaffed) {
            h.tvUnderstaffedWarning.setText("⚠️ Thiếu nhân viên (" + item.assignedCount + "/" + item.minStaff + ")");
            h.tvUnderstaffedWarning.setVisibility(View.VISIBLE);
        } else {
            h.tvUnderstaffedWarning.setVisibility(View.GONE);
        }

        // Status badge
        String status = shift.getStatus();
        h.tvStatus.setText(getStatusLabel(status));
        h.tvStatus.setTextColor(getStatusColor(h.itemView, status));

        // PopupMenu dựa trên status
        h.btnAction.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            switch (status) {
                case Constants.SHIFT_DRAFT:
                    popup.getMenu().add(0, 1, 0, "Phát hành");
                    popup.getMenu().add(0, 2, 0, "Phân công NV");
                    popup.getMenu().add(0, 3, 0, "Hủy ca");
                    break;
                case Constants.SHIFT_PUBLISHED:
                    popup.getMenu().add(0, 4, 0, "Mở ca");
                    popup.getMenu().add(0, 2, 0, "Phân công NV");
                    popup.getMenu().add(0, 7, 0, "Chat nhóm ca");
                    popup.getMenu().add(0, 3, 0, "Hủy ca");
                    break;
                case Constants.SHIFT_IN_PROGRESS:
                    popup.getMenu().add(0, 5, 0, "Đóng ca");
                    popup.getMenu().add(0, 6, 0, "Xem báo cáo");
                    popup.getMenu().add(0, 7, 0, "Chat nhóm ca");
                    break;
                case Constants.SHIFT_CLOSED:
                    popup.getMenu().add(0, 6, 0, "Xem báo cáo");
                    popup.getMenu().add(0, 7, 0, "Chat nhóm ca");
                    break;
                default:
                    return; // CANCELLED → no actions
            }
            popup.setOnMenuItemClickListener(menuItem -> {
                switch (menuItem.getItemId()) {
                    case 1: listener.onPublish(shift); return true;
                    case 2: listener.onAssignStaff(shift); return true;
                    case 3: listener.onCancel(shift); return true;
                    case 4: listener.onOpenShift(shift); return true;
                    case 5: listener.onCloseShift(shift); return true;
                    case 6: listener.onViewReport(shift); return true;
                    case 7: listener.onChatShift(shift); return true;
                    default: return false;
                }
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    private String getStatusLabel(String status) {
        switch (status) {
            case Constants.SHIFT_DRAFT:       return "Nháp";
            case Constants.SHIFT_PUBLISHED:   return "Đã phát hành";
            case Constants.SHIFT_IN_PROGRESS: return "Đang mở";
            case Constants.SHIFT_CLOSED:      return "Đã đóng";
            case Constants.SHIFT_CANCELLED:   return "Đã hủy";
            default: return status;
        }
    }

    private int getStatusColor(View view, String status) {
        int colorRes;
        switch (status) {
            case Constants.SHIFT_IN_PROGRESS: colorRes = R.color.success; break;
            case Constants.SHIFT_PUBLISHED:   colorRes = R.color.info; break;
            case Constants.SHIFT_CANCELLED:   colorRes = R.color.error; break;
            case Constants.SHIFT_CLOSED:      colorRes = R.color.text_soft; break;
            default:                          colorRes = R.color.text_mute; break;
        }
        return view.getContext().getResources().getColor(colorRes);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTime, tvStatus, tvStaffCount, tvUnderstaffedWarning;
        Button btnAction;

        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_shift_name);
            tvTime = v.findViewById(R.id.tv_time);
            tvStatus = v.findViewById(R.id.tv_status);
            tvStaffCount = v.findViewById(R.id.tv_staff_count);
            tvUnderstaffedWarning = v.findViewById(R.id.tv_understaffed_warning);
            btnAction = v.findViewById(R.id.btn_action);
        }
    }
}