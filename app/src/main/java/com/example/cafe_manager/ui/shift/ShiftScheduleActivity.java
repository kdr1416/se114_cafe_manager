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

        // Set initial date label
        tvSelectedDate.setText(sdf.format(new Date(viewModel.getSelectedDate())));
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
        // Sử dụng currentTemplates đã được observe sẵn thay vì getValue()
        if (currentTemplates.isEmpty()) {
            Toast.makeText(this, "Chưa có mẫu ca nào. Hãy tạo mẫu ca trước.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_shift, null);
        Spinner spinner = dialogView.findViewById(R.id.spinner_template);

        String[] names = new String[currentTemplates.size()];
        for (int i = 0; i < currentTemplates.size(); i++) {
            ShiftTemplateEntity t = currentTemplates.get(i);
            names[i] = t.getTemplateName() + " (" + t.getStartTime() + " - " + t.getEndTime() + ")";
        }
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, names));

        new AlertDialog.Builder(this)
                .setTitle("Tạo ca cho " + sdf.format(new Date(viewModel.getSelectedDate())))
                .setView(dialogView)
                .setPositiveButton("Tạo", (d, w) -> {
                    int pos = spinner.getSelectedItemPosition();
                    if (pos >= 0 && pos < currentTemplates.size()) {
                        viewModel.createShiftFromTemplate(currentTemplates.get(pos));
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

        String[] names = new String[users.size()];
        boolean[] checked = new boolean[users.size()];
        boolean[] originalChecked = new boolean[users.size()];

        for (int i = 0; i < users.size(); i++) {
            UserEntity u = users.get(i);
            names[i] = u.getFullName() + " (" + u.getRole() + ")";
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
                    // Refresh list
                    viewModel.setDate(viewModel.getSelectedDate());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
