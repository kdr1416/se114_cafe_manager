package com.example.cafe_manager.ui.shift;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.viewmodel.ShiftScheduleViewModel;
import com.example.cafe_manager.viewmodel.ShiftScheduleViewModel.DisplayItem;

import java.util.Objects;
import java.util.List;
import android.widget.CheckBox;
import java.util.HashSet;
import java.util.Set;

public class ShiftScheduleAdapter extends ListAdapter<DisplayItem, RecyclerView.ViewHolder> {

    private boolean selectionMode = false;
    private final Set<ShiftScheduleViewModel.ShiftDisplayItem> selectedShifts = new HashSet<>();
    private OnSelectionChangedListener selectionListener;

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }

    public void setSelectionMode(boolean enabled) {
        this.selectionMode = enabled;
        if (!enabled) {
            selectedShifts.clear();
        }
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public Set<ShiftScheduleViewModel.ShiftDisplayItem> getSelectedShifts() {
        return new HashSet<>(selectedShifts);
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionListener = listener;
    }

    public interface OnShiftActionListener {
        void onPublish(ShiftEntity shift);
        void onOpenShift(ShiftEntity shift);
        void onAssignStaff(ShiftEntity shift);
        void onCloseShift(ShiftEntity shift);
        void onViewReport(ShiftEntity shift);
        void onCancel(ShiftEntity shift);
        void onChatShift(ShiftEntity shift);
    }

    private OnShiftActionListener listener;

    private static final DiffUtil.ItemCallback<DisplayItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<DisplayItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull DisplayItem oldItem, @NonNull DisplayItem newItem) {
                    if (oldItem.getViewType() != newItem.getViewType()) return false;
                    if (oldItem instanceof ShiftScheduleViewModel.DayHeaderItem) {
                        return ((ShiftScheduleViewModel.DayHeaderItem) oldItem).date ==
                               ((ShiftScheduleViewModel.DayHeaderItem) newItem).date;
                    }
                    if (oldItem instanceof ShiftScheduleViewModel.ShiftDisplayItem) {
                        return ((ShiftScheduleViewModel.ShiftDisplayItem) oldItem).shift.getShiftId() ==
                               ((ShiftScheduleViewModel.ShiftDisplayItem) newItem).shift.getShiftId();
                    }
                    return false;
                }

                @Override
                public boolean areContentsTheSame(@NonNull DisplayItem oldItem, @NonNull DisplayItem newItem) {
                    if (oldItem instanceof ShiftScheduleViewModel.DayHeaderItem) {
                        return true;
                    }
                    if (oldItem instanceof ShiftScheduleViewModel.ShiftDisplayItem) {
                        ShiftScheduleViewModel.ShiftDisplayItem o =
                                (ShiftScheduleViewModel.ShiftDisplayItem) oldItem;
                        ShiftScheduleViewModel.ShiftDisplayItem n =
                                (ShiftScheduleViewModel.ShiftDisplayItem) newItem;
                        return o.assignedCount == n.assignedCount
                                && o.understaffed == n.understaffed
                                && Objects.equals(o.assignedStaffNames, n.assignedStaffNames)
                                && Objects.equals(o.shift.getStatus(), n.shift.getStatus());
                    }
                    return false;
                }
            };

    public ShiftScheduleAdapter(OnShiftActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == DisplayItem.TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_week_day_header, parent, false);
            return new DayHeaderViewHolder(v);
        }
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shift_schedule, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DisplayItem displayItem = getItem(position);
        if (holder instanceof DayHeaderViewHolder) {
            ShiftScheduleViewModel.DayHeaderItem header = (ShiftScheduleViewModel.DayHeaderItem) displayItem;
            ((DayHeaderViewHolder) holder).tvDayLabel.setText(header.dayLabel);
            return;
        }

        ViewHolder h = (ViewHolder) holder;
        ShiftScheduleViewModel.ShiftDisplayItem item = (ShiftScheduleViewModel.ShiftDisplayItem) displayItem;
        ShiftEntity shift = item.shift;

        h.tvName.setText(shift.getShiftName());
        h.tvTime.setText(shift.getStartTime() + " — " + shift.getEndTime());
        h.tvStaffCount.setText(item.assignedCount + " nhân viên");

        List<String> names = item.assignedStaffNames;
        if (names != null && !names.isEmpty()) {
            h.tvStaffNames.setText(android.text.TextUtils.join(", ", names));
            h.tvStaffNames.setVisibility(View.VISIBLE);
        } else {
            h.tvStaffNames.setVisibility(View.GONE);
        }

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

        // Selection mode checkbox binding
        if (selectionMode && Constants.SHIFT_DRAFT.equals(status)) {
            h.cbSelectShift.setVisibility(View.VISIBLE);
            h.cbSelectShift.setChecked(selectedShifts.contains(item));
            h.btnAction.setVisibility(View.GONE);

            View.OnClickListener clickListener = v -> {
                if (selectedShifts.contains(item)) {
                    selectedShifts.remove(item);
                } else {
                    selectedShifts.add(item);
                }
                h.cbSelectShift.setChecked(selectedShifts.contains(item));
                if (selectionListener != null) {
                    selectionListener.onSelectionChanged(selectedShifts.size());
                }
            };
            h.itemView.setOnClickListener(clickListener);
            h.cbSelectShift.setOnClickListener(clickListener);
        } else {
            h.cbSelectShift.setVisibility(View.GONE);
            h.btnAction.setVisibility(View.VISIBLE);
            h.itemView.setOnClickListener(null);
            h.itemView.setClickable(false);

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
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getViewType();
    }

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
        return view.getContext().getColor(colorRes);
    }

    static class DayHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayLabel;
        DayHeaderViewHolder(View v) {
            super(v);
            tvDayLabel = v.findViewById(R.id.tv_day_label);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTime, tvStatus, tvStaffCount, tvUnderstaffedWarning, tvStaffNames;
        CheckBox cbSelectShift;
        Button btnAction;

        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_shift_name);
            tvTime = v.findViewById(R.id.tv_time);
            tvStatus = v.findViewById(R.id.tv_status);
            tvStaffCount = v.findViewById(R.id.tv_staff_count);
            tvUnderstaffedWarning = v.findViewById(R.id.tv_understaffed_warning);
            tvStaffNames = v.findViewById(R.id.tv_staff_names);
            cbSelectShift = v.findViewById(R.id.cb_select_shift);
            btnAction = v.findViewById(R.id.btn_action);
        }
    }
}