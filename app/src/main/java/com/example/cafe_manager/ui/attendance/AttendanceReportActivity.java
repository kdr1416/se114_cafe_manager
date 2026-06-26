package com.example.cafe_manager.ui.attendance;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cafe_manager.R;
import com.example.cafe_manager.data.remote.TeamAttendanceSummary;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.viewmodel.AttendanceReportViewModel;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AttendanceReportActivity extends AppCompatActivity {

    private AttendanceReportViewModel viewModel;
    private TeamAttendanceAdapter teamAdapter;
    private AttendanceRecordAdapter recordAdapter;

    private TextView tvMonthLabel;
    private TabLayout tabLayout;
    private View layoutTabTeam;
    private View layoutTabDetails;

    private Spinner spinnerEmployees;
    private TextView tvTotalShifts;
    private TextView tvTotalHours;
    private TextView tvTotalOrders;
    private TextView tvTotalRevenue;

    private List<TeamAttendanceSummary> teamSummaryList = new ArrayList<>();
    private boolean isSelfSpinnerTrigger = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_attendance_report);

            // 1. Role-based Access Guard
            String role = SessionManager.getInstance(this).getRole();
            if (!Constants.ROLE_ADMIN.equals(role) && !Constants.ROLE_MANAGER.equals(role)) {
                Toast.makeText(this, "Quyền truy cập bị từ chối.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // 2. Initialize Views
            setupTopBar();
            tvMonthLabel = findViewById(R.id.tv_month_label);
            tabLayout = findViewById(R.id.tab_layout);
            layoutTabTeam = findViewById(R.id.layout_tab_team);
            layoutTabDetails = findViewById(R.id.layout_tab_details);
            spinnerEmployees = findViewById(R.id.spinner_employees);
            tvTotalShifts = findViewById(R.id.tv_total_shifts);
            tvTotalHours = findViewById(R.id.tv_total_hours);
            tvTotalOrders = findViewById(R.id.tv_total_orders);
            tvTotalRevenue = findViewById(R.id.tv_total_revenue);

            // 3. Setup TabLayout
            tabLayout.addTab(tabLayout.newTab().setText("Tổng quan"));
            tabLayout.addTab(tabLayout.newTab().setText("Chi tiết"));
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    if (tab.getPosition() == 0) {
                        layoutTabTeam.setVisibility(View.VISIBLE);
                        layoutTabDetails.setVisibility(View.GONE);
                    } else {
                        layoutTabTeam.setVisibility(View.GONE);
                        layoutTabDetails.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}

                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });

            // 4. Setup RecyclerViews
            RecyclerView rvTeam = findViewById(R.id.rv_team_attendance);
            rvTeam.setLayoutManager(new LinearLayoutManager(this));
            teamAdapter = new TeamAttendanceAdapter(summary -> {
                // Click to switch to details of selected employee
                navigateToEmployeeDetails(summary.getUserId());
            });
            rvTeam.setAdapter(teamAdapter);

            RecyclerView rvRecords = findViewById(R.id.rv_attendance_records);
            rvRecords.setLayoutManager(new LinearLayoutManager(this));
            recordAdapter = new AttendanceRecordAdapter();
            rvRecords.setAdapter(recordAdapter);

            // 5. Month selector bindings
            findViewById(R.id.btn_prev_month).setOnClickListener(v -> viewModel.prevMonth());
            findViewById(R.id.btn_next_month).setOnClickListener(v -> viewModel.nextMonth());

            // 6. ViewModels & Observers
            viewModel = new ViewModelProvider(this).get(AttendanceReportViewModel.class);
            setupObservers();

            viewModel.loadReport();
        } catch (Throwable t) {
            android.util.Log.e("AttendanceReport", "Crash in onCreate", t);
            showCrashDialog(t);
        }
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);
        View btnRight = topBar.findViewById(R.id.btn_right);

        title.setText("Báo cáo chấm công");
        caption.setText("Chi tiết công & Điểm danh nhân viên");
        btnBack.setOnClickListener(v -> finish());
        btnRight.setVisibility(View.GONE);
    }

    private void setupObservers() {
        viewModel.getCurrentMonth().observe(this, m -> tvMonthLabel.setText(viewModel.getCurrentPeriodLabel()));
        viewModel.getCurrentYear().observe(this, y -> tvMonthLabel.setText(viewModel.getCurrentPeriodLabel()));

        viewModel.getTeamSummaries().observe(this, summaries -> {
            if (summaries != null) {
                teamSummaryList = summaries;
                teamAdapter.setItems(summaries);
                updateSpinner(summaries);
            }
        });

        viewModel.getSelectedUserDetail().observe(this, detail -> {
            if (detail != null) {
                int attended = detail.getAttendedShifts() != null ? detail.getAttendedShifts() : 0;
                double hours = detail.getTotalHoursWorked() != null ? detail.getTotalHoursWorked() : 0.0;
                int orders = detail.getOrdersCreated() != null ? detail.getOrdersCreated() : 0;
                double revenue = detail.getRevenueProcessed() != null ? detail.getRevenueProcessed() : 0.0;

                tvTotalShifts.setText(String.valueOf(attended));
                tvTotalHours.setText(String.format(Locale.getDefault(), "%.1fh", hours));
                tvTotalOrders.setText(String.valueOf(orders));
                tvTotalRevenue.setText(formatRevenue(revenue));
                recordAdapter.setItems(detail.getRecords());
            } else {
                tvTotalShifts.setText("0");
                tvTotalHours.setText("0.0h");
                tvTotalOrders.setText("0");
                tvTotalRevenue.setText("0đ");
                recordAdapter.setItems(new ArrayList<>());
            }
        });

        viewModel.getErrorMessage().observe(this, err -> {
            if (err != null) {
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
                viewModel.clearErrorMessage();
            }
        });
    }

    private void updateSpinner(List<TeamAttendanceSummary> summaries) {
        List<String> names = new ArrayList<>();
        for (TeamAttendanceSummary s : summaries) {
            String name = s.getFullName() != null ? s.getFullName() : "";
            String role = s.getRole() != null ? s.getRole() : "";
            names.add(name + " (" + role + ")");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEmployees.setAdapter(adapter);

        // Re-select currently selected employee in the spinner if applicable
        Integer selectedUserId = viewModel.getSelectedUserId();
        if (selectedUserId != null) {
            for (int i = 0; i < summaries.size(); i++) {
                if (summaries.get(i).getUserId().equals(selectedUserId)) {
                    isSelfSpinnerTrigger = true;
                    spinnerEmployees.setSelection(i);
                    break;
                }
            }
        }

        spinnerEmployees.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isSelfSpinnerTrigger) {
                    isSelfSpinnerTrigger = false;
                    return;
                }
                TeamAttendanceSummary selected = teamSummaryList.get(position);
                viewModel.loadUserDetails(selected.getUserId());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void navigateToEmployeeDetails(int userId) {
        viewModel.setSelectedUserId(userId);
        
        // Find spinner index
        int index = -1;
        for (int i = 0; i < teamSummaryList.size(); i++) {
            if (teamSummaryList.get(i).getUserId() == userId) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            isSelfSpinnerTrigger = true;
            spinnerEmployees.setSelection(index);
        }

        // Switch to details tab
        TabLayout.Tab detailTab = tabLayout.getTabAt(1);
        if (detailTab != null) {
            detailTab.select();
        }
        
        viewModel.loadUserDetails(userId);
    }

    private String formatRevenue(double amount) {
        if (amount >= 1_000_000) {
            return String.format(Locale.getDefault(), "%.1ftr", amount / 1_000_000.0).replace(".0", "");
        } else if (amount >= 1_000) {
            return String.format(Locale.getDefault(), "%.0fk", amount / 1_000.0);
        } else {
            return String.format(Locale.getDefault(), "%.0fđ", amount);
        }
    }

    private void showCrashDialog(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        t.printStackTrace(pw);
        String stackTrace = sw.toString();

        TextView textView = new TextView(this);
        textView.setText(stackTrace);
        textView.setPadding(32, 32, 32, 32);
        textView.setTextIsSelectable(true);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.addView(textView);

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Lỗi ứng dụng (Crash)")
            .setMessage("Vui lòng chụp màn hình lỗi này gửi cho lập trình viên:")
            .setView(scrollView)
            .setPositiveButton("Đóng", (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }
}
