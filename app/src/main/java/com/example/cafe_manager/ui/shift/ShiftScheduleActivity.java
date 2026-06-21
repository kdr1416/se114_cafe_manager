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

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.entity.ShiftAssignmentEntity;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.data.local.entity.ShiftTemplateEntity;
import com.example.cafe_manager.data.local.entity.UserEntity;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.PermissionUtils;
import com.example.cafe_manager.util.RepositoryCallback;
import com.example.cafe_manager.viewmodel.ShiftScheduleViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ShiftScheduleActivity extends AppCompatActivity {

    private ShiftScheduleViewModel viewModel;
    private ShiftScheduleAdapter adapter;
    private TextView tvSelectedDate, tvEmpty;
    private final SimpleDateFormat sdf =
            new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
    
    // Thêm list này để tránh lỗi getValue() bị null do LiveData chưa load
    private List<ShiftTemplateEntity> currentTemplates = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift_schedule);

        if (!PermissionUtils.requireRole(this, Constants.ROLE_ADMIN, Constants.ROLE_MANAGER)) return;

        viewModel = new ViewModelProvider(this).get(ShiftScheduleViewModel.class);

        // Top bar
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tv_title)).setText("Lịch ca làm việc");
        ImageButton btnRight = findViewById(R.id.btn_right);
        btnRight.setVisibility(View.VISIBLE);
        btnRight.setImageResource(R.drawable.ic_plus);
        btnRight.setOnClickListener(v -> showCreateShiftDialog());

        tvSelectedDate = findViewById(R.id.tv_selected_date);
        tvEmpty = findViewById(R.id.tv_empty);

        // Date selector click
        findViewById(R.id.layout_date_selector).setOnClickListener(v -> showDatePicker());

        // RecyclerView
        RecyclerView rv = findViewById(R.id.rv_shifts);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ShiftScheduleAdapter(new ShiftScheduleAdapter.OnShiftActionListener() {
            @Override
            public void onPublish(ShiftEntity shift) {
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
                        .setPositiveButton("Hủy ca", (d, w) -> viewModel.cancelShift(shift.getShiftId()))
                        .setNegativeButton("Quay lại", null)
                        .show();
            }
        });
        rv.setAdapter(adapter);

        // Observe shifts
        viewModel.getShifts().observe(this, items -> {
            adapter.setItems(items);
            boolean empty = items == null || items.isEmpty();
            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        });

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
        int activeTextColor = getResources().getColor(R.color.text_on_accent);
        int inactiveBg = R.drawable.bg_button_secondary;
        int inactiveTextColor = getResources().getColor(R.color.text_soft);

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
        viewModel.setDate(viewModel.getSelectedDate());
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
                            viewModel.assignStaff(shift.getShiftId(), u.getUserId());
                        } else if (originalChecked[i] && !checked[i]) {
                            // Xóa
                            for (ShiftAssignmentEntity a : assigned) {
                                if (a.getUserId() == u.getUserId()) {
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
}
