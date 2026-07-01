package com.example.cafe_manager.ui.shift;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.Set;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.util.WeekNavigationHelper;
import com.example.cafe_manager.data.local.entity.ShiftAssignmentEntity;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.data.local.entity.ShiftTemplateEntity;
import com.example.cafe_manager.data.local.entity.UserEntity;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.PermissionUtils;
import com.example.cafe_manager.util.RepositoryCallback;
import com.example.cafe_manager.viewmodel.ShiftScheduleViewModel;
import com.example.cafe_manager.ui.communication.ChatMessageActivity;
import com.example.cafe_manager.ui.scheduling.SchedulingPreviewActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ShiftScheduleActivity extends AppCompatActivity {

    private ShiftScheduleViewModel viewModel;
    private ShiftScheduleAdapter adapter;
    private TextView tvSelectedDate, tvEmpty;
    private TextView tvWeekRange;
    private ImageButton btnPrevWeek, btnNextWeek;
    private long currentWeekStart;
    private Button btnTogglePublishMode, btnConfirmPublish, btnCancelSelection;
    private LinearLayout layoutPublishBar, layoutNormalMode;
    private TextView tvSelectionCount;

    private final java.util.Locale localeVi = new java.util.Locale("vi", "VN");
    private final SimpleDateFormat sdf =
            new SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));
    
    // Thêm list này để tránh lỗi getValue() bị null do LiveData chưa load
    private List<ShiftTemplateEntity> currentTemplates = new ArrayList<>();

    {
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift_schedule);

        if (!PermissionUtils.requireRole(this, Constants.ROLE_ADMIN, Constants.ROLE_MANAGER)) return;

        viewModel = new ViewModelProvider(this).get(ShiftScheduleViewModel.class);
        currentWeekStart = WeekNavigationHelper.getCurrentWeekStart();

        // Top bar
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tv_title)).setText("Lịch ca làm việc");
        ImageButton btnRight = findViewById(R.id.btn_right);
        btnRight.setVisibility(View.VISIBLE);
        btnRight.setImageResource(R.drawable.ic_plus);
        btnRight.setOnClickListener(v -> showActionDialog());

        tvSelectedDate = findViewById(R.id.tv_selected_date);
        tvEmpty = findViewById(R.id.tv_empty);

        // Week navigation setup
        tvWeekRange = findViewById(R.id.tv_week_range);
        btnPrevWeek = findViewById(R.id.btn_prev_week);
        btnNextWeek = findViewById(R.id.btn_next_week);

        tvWeekRange.setText(WeekNavigationHelper.formatWeekRange(currentWeekStart));
        btnPrevWeek.setOnClickListener(v -> {
            currentWeekStart = WeekNavigationHelper.addWeeks(currentWeekStart, -1);
            tvWeekRange.setText(WeekNavigationHelper.formatWeekRange(currentWeekStart));
            viewModel.setDate(currentWeekStart);
            viewModel.loadShiftsForWeek(currentWeekStart);
        });
        btnNextWeek.setOnClickListener(v -> {
            currentWeekStart = WeekNavigationHelper.addWeeks(currentWeekStart, 1);
            tvWeekRange.setText(WeekNavigationHelper.formatWeekRange(currentWeekStart));
            viewModel.setDate(currentWeekStart);
            viewModel.loadShiftsForWeek(currentWeekStart);
        });

        // Date selector click
        findViewById(R.id.layout_date_selector).setOnClickListener(v -> showDatePicker());

        // RecyclerView
        RecyclerView rv = findViewById(R.id.rv_shifts);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ShiftScheduleAdapter(new ShiftScheduleAdapter.OnShiftActionListener() {
            @Override
            public void onPublish(ShiftEntity shift) {
                viewModel.optimisticallyUpdateShiftStatus(shift.getShiftId(), "PUBLISHED");
                viewModel.publishShift(shift.getShiftId());
            }

            @Override
            public void onOpenShift(ShiftEntity shift) {
                showOpenShiftDialog(shift);
            }

            @Override
            public void onAssignStaff(ShiftEntity shift) {
                showAssignStaffDialog(shift);
            }

            @Override
            public void onCloseShift(ShiftEntity shift) {
                Intent intent = new Intent(ShiftScheduleActivity.this, ShiftCloseActivity.class);
                intent.putExtra("extra_shift_id", shift.getShiftId());
                startActivity(intent);
            }

            @Override
            public void onViewReport(ShiftEntity shift) {
                Intent intent = new Intent(ShiftScheduleActivity.this, ShiftReportActivity.class);
                intent.putExtra("extra_shift_id", shift.getShiftId());
                startActivity(intent);
            }

            @Override
            public void onCancel(ShiftEntity shift) {
                new AlertDialog.Builder(ShiftScheduleActivity.this)
                        .setTitle("Hủy ca?")
                        .setMessage("Bạn có chắc muốn hủy ca \"" + shift.getShiftName() + "\"?")
                        .setPositiveButton("Hủy ca", (d, w) -> {
                            viewModel.optimisticallyUpdateShiftStatus(shift.getShiftId(), "CANCELLED");
                            viewModel.cancelShift(shift.getShiftId());
                        })
                        .setNegativeButton("Quay lại", null)
                        .show();
            }

            @Override
            public void onChatShift(ShiftEntity shift) {
                viewModel.getOrCreateShiftChatRoom(shift.getShiftId(), new RepositoryCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer roomId) {
                        Intent intent = new Intent(ShiftScheduleActivity.this, ChatMessageActivity.class);
                        intent.putExtra("room_id", roomId);
                        
                        SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        sdfDate.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
                        String dateStr = sdfDate.format(new Date(shift.getShiftDate()));
                        String roomName = shift.getShiftName() + " - " + dateStr + " (" + shift.getStartTime() + "-" + shift.getEndTime() + ")";
                        intent.putExtra("room_name", roomName);
                        startActivity(intent);
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(ShiftScheduleActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        rv.setAdapter(adapter);

        // Observe week shifts instead of daily shifts
        viewModel.getWeekShiftsLive().observe(this, items -> {
            List<ShiftScheduleViewModel.DisplayItem> withHeaders = insertDayHeaders(items);
            adapter.submitList(withHeaders);
            boolean empty = items == null || items.isEmpty();
            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        });

        viewModel.loadShiftsForWeek(currentWeekStart);

        // QUAN TRỌNG: Observe templates để kích hoạt load dữ liệu từ DB (Sửa lỗi Spinner trống)
        viewModel.getTemplates().observe(this, templates -> {
            if (templates != null) {
                currentTemplates = templates;
            }
        });

        viewModel.getMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });

        // Filter chips setup
        TextView chipAll = findViewById(R.id.chip_shift_all);
        TextView chipDraft = findViewById(R.id.chip_shift_draft);
        TextView chipPublished = findViewById(R.id.chip_shift_published);
        TextView chipInProgress = findViewById(R.id.chip_shift_in_progress);
        TextView chipClosed = findViewById(R.id.chip_shift_closed);

        View.OnClickListener filterClickListener = v -> {
            String filter = "ALL";
            if (v.getId() == R.id.chip_shift_all) {
                filter = "ALL";
            } else if (v.getId() == R.id.chip_shift_draft) {
                filter = Constants.SHIFT_DRAFT;
            } else if (v.getId() == R.id.chip_shift_published) {
                filter = Constants.SHIFT_PUBLISHED;
            } else if (v.getId() == R.id.chip_shift_in_progress) {
                filter = Constants.SHIFT_IN_PROGRESS;
            } else if (v.getId() == R.id.chip_shift_closed) {
                filter = Constants.SHIFT_CLOSED;
            }
            viewModel.setStatusFilter(filter);
            viewModel.loadShiftsForWeek(currentWeekStart);
            updateShiftChips(filter, chipAll, chipDraft, chipPublished, chipInProgress, chipClosed);
        };

        chipAll.setOnClickListener(filterClickListener);
        chipDraft.setOnClickListener(filterClickListener);
        chipPublished.setOnClickListener(filterClickListener);
        chipInProgress.setOnClickListener(filterClickListener);
        chipClosed.setOnClickListener(filterClickListener);

        // Copy schedule click
        findViewById(R.id.btn_copy_schedule).setOnClickListener(v -> showCopyScheduleDialog());

        // Set initial date label
        tvSelectedDate.setText(sdf.format(new Date(viewModel.getSelectedDate())));

        // Wire batch publish controls
        layoutNormalMode = findViewById(R.id.layout_normal_mode);
        layoutPublishBar = findViewById(R.id.layout_publish_bar);
        btnTogglePublishMode = findViewById(R.id.btn_toggle_publish_mode);
        btnCancelSelection = findViewById(R.id.btn_cancel_selection);
        btnConfirmPublish = findViewById(R.id.btn_confirm_publish);
        tvSelectionCount = findViewById(R.id.tv_selection_count);

        btnTogglePublishMode.setOnClickListener(v -> {
            adapter.setSelectionMode(true);
            layoutNormalMode.setVisibility(View.GONE);
            layoutPublishBar.setVisibility(View.VISIBLE);
            btnConfirmPublish.setEnabled(false);
            tvSelectionCount.setText("Đã chọn 0 ca");
        });

        btnCancelSelection.setOnClickListener(v -> {
            adapter.setSelectionMode(false);
            layoutNormalMode.setVisibility(View.VISIBLE);
            layoutPublishBar.setVisibility(View.GONE);
            tvSelectionCount.setText("Đã chọn 0 ca");
        });

        adapter.setOnSelectionChangedListener(count -> {
            tvSelectionCount.setText("Đã chọn " + count + " ca");
            btnConfirmPublish.setEnabled(count > 0);
        });

        btnConfirmPublish.setOnClickListener(v -> {
            Set<ShiftScheduleViewModel.ShiftDisplayItem> selected = adapter.getSelectedShifts();
            if (selected.isEmpty()) return;

            // 1. Client-side validation: minStaff check
            for (ShiftScheduleViewModel.ShiftDisplayItem item : selected) {
                if (item.assignedCount < item.minStaff) {
                    new AlertDialog.Builder(this)
                            .setTitle("Chưa đủ nhân sự")
                            .setMessage("Ca \"" + item.shift.getShiftName() + "\" chưa đủ số lượng nhân sự tối thiểu (" + 
                                    item.assignedCount + "/" + item.minStaff + "). Vui lòng phân công thêm trước khi phát hành.")
                            .setPositiveButton("Đồng ý", null)
                            .show();
                    return;
                }
            }

            // 2. Confirm publication
            new AlertDialog.Builder(this)
                    .setTitle("Phát hành ca")
                    .setMessage("Phát hành " + selected.size() + " ca đã chọn?")
                    .setPositiveButton("Phát hành", (d, w) -> {
                        publishSelectedShifts(selected);
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    private void publishSelectedShifts(Set<ShiftScheduleViewModel.ShiftDisplayItem> selectedItems) {
        int total = selectedItems.size();
        Toast.makeText(this, "Đang xử lý phát hành " + total + " ca...", Toast.LENGTH_SHORT).show();
        
        for (ShiftScheduleViewModel.ShiftDisplayItem item : selectedItems) {
            viewModel.publishShift(item.shift.getShiftId());
        }

        // Exit selection mode and refresh
        adapter.setSelectionMode(false);
        layoutNormalMode.setVisibility(View.VISIBLE);
        layoutPublishBar.setVisibility(View.GONE);
        viewModel.refresh();
    }

    private void showCopyScheduleDialog() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(viewModel.getSelectedDate());
        cal.add(Calendar.DATE, 1);

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar c = Calendar.getInstance();
            c.set(year, month, dayOfMonth, 0, 0, 0);
            c.set(Calendar.MILLISECOND, 0);
            long targetDate = c.getTimeInMillis();

            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận sao chép")
                    .setMessage("Bạn có chắc muốn sao chép toàn bộ khung ca từ ngày " +
                            sdf.format(new Date(viewModel.getSelectedDate())) +
                            " sang ngày " + sdf.format(new Date(targetDate)) + "?\n(Không copy phân công nhân sự)")
                    .setPositiveButton("Sao chép", (dialog, which) -> {
                        viewModel.copySchedule(viewModel.getSelectedDate(), targetDate);
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void updateShiftChips(String filter, TextView chipAll, TextView chipDraft, TextView chipPublished, TextView chipInProgress, TextView chipClosed) {
        int activeBg = R.drawable.bg_button_primary;
        int activeTextColor = getColor(R.color.text_on_accent);
        int inactiveBg = R.drawable.bg_button_secondary;
        int inactiveTextColor = getColor(R.color.text_soft);

        chipAll.setBackgroundResource("ALL".equals(filter) ? activeBg : inactiveBg);
        chipAll.setTextColor("ALL".equals(filter) ? activeTextColor : inactiveTextColor);

        chipDraft.setBackgroundResource(Constants.SHIFT_DRAFT.equals(filter) ? activeBg : inactiveBg);
        chipDraft.setTextColor(Constants.SHIFT_DRAFT.equals(filter) ? activeTextColor : inactiveTextColor);

        chipPublished.setBackgroundResource(Constants.SHIFT_PUBLISHED.equals(filter) ? activeBg : inactiveBg);
        chipPublished.setTextColor(Constants.SHIFT_PUBLISHED.equals(filter) ? activeTextColor : inactiveTextColor);

        chipInProgress.setBackgroundResource(Constants.SHIFT_IN_PROGRESS.equals(filter) ? activeBg : inactiveBg);
        chipInProgress.setTextColor(Constants.SHIFT_IN_PROGRESS.equals(filter) ? activeTextColor : inactiveTextColor);

        chipClosed.setBackgroundResource(Constants.SHIFT_CLOSED.equals(filter) ? activeBg : inactiveBg);
        chipClosed.setTextColor(Constants.SHIFT_CLOSED.equals(filter) ? activeTextColor : inactiveTextColor);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh khi quay lại từ ShiftCloseActivity
        viewModel.loadShiftsForWeek(currentWeekStart);
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(viewModel.getSelectedDate());

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar c = Calendar.getInstance();
            c.set(year, month, dayOfMonth, 0, 0, 0);
            c.set(Calendar.MILLISECOND, 0);
            long date = c.getTimeInMillis();
            viewModel.setDate(date);
            tvSelectedDate.setText(sdf.format(new Date(date)));

            // Đồng bộ thanh chuyển tuần
            currentWeekStart = WeekNavigationHelper.getWeekStart(date);
            tvWeekRange.setText(WeekNavigationHelper.formatWeekRange(currentWeekStart));
            viewModel.loadShiftsForWeek(currentWeekStart);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void showCreateShiftDialog() {
        if (currentTemplates.isEmpty()) {
            Toast.makeText(this, "Chưa có mẫu ca nào. Hãy tạo mẫu ca trước.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[currentTemplates.size()];
        boolean[] checked = new boolean[currentTemplates.size()];
        for (int i = 0; i < currentTemplates.size(); i++) {
            ShiftTemplateEntity t = currentTemplates.get(i);
            names[i] = t.getTemplateName() + " (" + t.getStartTime() + " - " + t.getEndTime() + ")";
            checked[i] = false;
        }

        new AlertDialog.Builder(this)
                .setTitle("Tạo ca cho " + sdf.format(new Date(viewModel.getSelectedDate())))
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> {
                    checked[which] = isChecked;
                })
                .setPositiveButton("Tạo", (dialog, which) -> {
                    List<ShiftTemplateEntity> selectedTemplates = new ArrayList<>();
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) {
                            selectedTemplates.add(currentTemplates.get(i));
                        }
                    }
                    if (!selectedTemplates.isEmpty()) {
                        viewModel.bulkCreateShifts(selectedTemplates);
                    } else {
                        Toast.makeText(this, "Vui lòng chọn ít nhất một mẫu ca.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showOpenShiftDialog(ShiftEntity shift) {
        EditText etCash = new EditText(this);
        etCash.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etCash.setHint("Nhập số tiền (VNĐ)");

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        int margin = (int) (24 * getResources().getDisplayMetrics().density);
        params.setMargins(margin, margin / 3, margin, 0);
        container.addView(etCash, params);

        new AlertDialog.Builder(this)
                .setTitle("Mở ca — " + shift.getShiftName())
                .setMessage("Nhập số tiền mặt ban đầu trong két:")
                .setView(container)
                .setPositiveButton("Mở ca", (d, w) -> {
                    String text = etCash.getText().toString().trim();
                    if (text.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập số tiền.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double cash = Double.parseDouble(text);
                        viewModel.optimisticallyUpdateShiftStatus(shift.getShiftId(), "IN_PROGRESS");
                        viewModel.openShiftWithCash(shift.getShiftId(), cash);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số tiền không hợp lệ.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showAssignStaffDialog(ShiftEntity shift) {
        viewModel.loadStaffForAssignment(shift.getShiftId(),
                new RepositoryCallback<ShiftScheduleViewModel.StaffAssignmentData>() {
                    @Override
                    public void onSuccess(ShiftScheduleViewModel.StaffAssignmentData data) {
                        buildAssignDialog(shift, data);
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(ShiftScheduleActivity.this,
                                "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void buildAssignDialog(ShiftEntity shift,
                                   ShiftScheduleViewModel.StaffAssignmentData data) {
        List<UserEntity> users = data.allUsers;
        List<ShiftAssignmentEntity> assigned = data.currentAssignments;

        if (users == null || users.isEmpty()) {
            Toast.makeText(this, "Không có nhân viên nào.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Sắp xếp theo số ca trong tuần (ít nhất trước)
        java.util.Collections.sort(users, (u1, u2) -> {
            Integer count1 = data.weeklyShiftCounts.get(u1.getUserId());
            Integer count2 = data.weeklyShiftCounts.get(u2.getUserId());
            int c1 = count1 != null ? count1 : 0;
            int c2 = count2 != null ? count2 : 0;
            return Integer.compare(c1, c2);
        });

        String[] names = new String[users.size()];
        boolean[] checked = new boolean[users.size()];
        boolean[] originalChecked = new boolean[users.size()];

        for (int i = 0; i < users.size(); i++) {
            UserEntity u = users.get(i);
            Integer count = data.weeklyShiftCounts.get(u.getUserId());
            int cnt = count != null ? count : 0;
            names[i] = u.getFullName() + " (" + u.getRole() + ") — " + cnt + " ca/tuần";
            
            boolean isAssigned = false;
            for (ShiftAssignmentEntity a : assigned) {
                if (a.getUserId() == u.getUserId()) {
                    isAssigned = true;
                    break;
                }
            }
            checked[i] = isAssigned;
            originalChecked[i] = isAssigned;
        }

        new AlertDialog.Builder(this)
                .setTitle("Phân công — " + shift.getShiftName())
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) ->
                        checked[which] = isChecked)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    for (int i = 0; i < users.size(); i++) {
                        UserEntity u = users.get(i);
                        if (!originalChecked[i] && checked[i]) {
                            // Thêm mới
                            viewModel.optimisticallyAddStaff(shift.getShiftId(), u.getFullName());
                            viewModel.assignStaff(shift.getShiftId(), u.getUserId());
                        } else if (originalChecked[i] && !checked[i]) {
                            // Xóa
                            for (ShiftAssignmentEntity a : assigned) {
                                if (a.getUserId() == u.getUserId()) {
                                    viewModel.optimisticallyRemoveStaff(shift.getShiftId(), u.getFullName());
                                    viewModel.removeAssignment(a.getAssignmentId());
                                    break;
                                }
                            }
                        }
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showActionDialog() {
        String[] options = {"Tạo ca thủ công", "Tự động sắp xếp ca"};
        new AlertDialog.Builder(this)
                .setTitle("Tùy chọn lập lịch")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showCreateShiftDialog();
                    } else if (which == 1) {
                        Intent intent = new Intent(ShiftScheduleActivity.this, SchedulingPreviewActivity.class);
                        startActivity(intent);
                    }
                })
                .show();
    }

    private List<ShiftScheduleViewModel.DisplayItem> insertDayHeaders(List<ShiftScheduleViewModel.ShiftDisplayItem> sortedShifts) {
        List<ShiftScheduleViewModel.DisplayItem> result = new ArrayList<>();
        if (sortedShifts == null) return result;
        long lastDate = -1;
        SimpleDateFormat daySdf = new SimpleDateFormat("EEE, dd/MM", localeVi);
        daySdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        for (ShiftScheduleViewModel.ShiftDisplayItem item : sortedShifts) {
            long shiftDate = item.shift.getShiftDate();
            if (shiftDate != lastDate) {
                lastDate = shiftDate;
                String label = daySdf.format(new Date(shiftDate));
                result.add(new ShiftScheduleViewModel.DayHeaderItem(label, shiftDate));
            }
            result.add(item);
        }
        return result;
    }
}
