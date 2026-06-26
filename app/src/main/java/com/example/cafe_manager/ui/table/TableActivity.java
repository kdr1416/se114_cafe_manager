package com.example.cafe_manager.ui.table;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.entity.TableEntity;
import com.example.cafe_manager.manager.CartManager;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.ui.admin.AdminMenuActivity;
import com.example.cafe_manager.ui.auth.LoginActivity;
import com.example.cafe_manager.ui.dashboard.DashboardActivity;
import com.example.cafe_manager.ui.dashboard.RevenueReportActivity;
import com.example.cafe_manager.ui.history.HistoryActivity;
import com.example.cafe_manager.ui.menu.MenuActivity;
import com.example.cafe_manager.ui.orderslist.OrdersListActivity;
import com.example.cafe_manager.ui.profile.ProfileActivity;
import com.example.cafe_manager.ui.promotion.PromotionManagementActivity;
import com.example.cafe_manager.ui.user.UserManagementActivity;
import com.example.cafe_manager.ui.menu.CategoryManagementActivity;
import com.example.cafe_manager.ui.shift.ShiftCloseActivity;
import com.example.cafe_manager.ui.shift.ShiftReportActivity;
import com.example.cafe_manager.ui.shift.DailyShiftReportActivity;
import com.example.cafe_manager.ui.shift.ShiftTemplateActivity;
import com.example.cafe_manager.ui.shift.ShiftScheduleActivity;
import com.example.cafe_manager.ui.shift.MyShiftActivity;
import com.example.cafe_manager.ui.leave.LeaveRequestActivity;
import com.example.cafe_manager.ui.leave.LeaveApprovalActivity;
import com.example.cafe_manager.ui.attendance.AttendanceReportActivity;
import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.PermissionUtils;
import com.example.cafe_manager.viewmodel.TableViewModel;

import android.widget.Toast;

public class TableActivity extends AppCompatActivity {

    public static final String EXTRA_TABLE_ID = "table_id";
    public static final String EXTRA_TABLE_NAME = "table_name";

    private TableViewModel viewModel;
    private TableAdapter adapter;
    private AreaAdapter areaAdapter;
    private TextView tvActiveCount;
    private TextView tvEmptyCount;

    private Handler pollingHandler;
    private Runnable pollingRunnable;
    private static final long POLLING_INTERVAL_MS = 5000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_table);

        setupTopBar();
        setupBottomNav();

        tvActiveCount = findViewById(R.id.tv_active_count);
        tvEmptyCount = findViewById(R.id.tv_empty_count);

        setupRecyclerView();
        setupViewModel();
        setupPolling();
    }

    private void setupPolling() {
        pollingHandler = new Handler(Looper.getMainLooper());
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                refreshData();
                pollingHandler.postDelayed(this, POLLING_INTERVAL_MS);
            }
        };
    }

    private void refreshData() {
        if (viewModel != null) {
            viewModel.refreshTables();
            viewModel.refreshAreas();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.post(pollingRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);
        ImageButton btnRight = topBar.findViewById(R.id.btn_right);
        ImageButton btnRight2 = topBar.findViewById(R.id.btn_right_2);

        // Greeting với tên user (nếu có session)
        title.setText(R.string.title_tables);

        String fullName = SessionManager.getInstance(this).getFullName();
        if (fullName == null || fullName.isEmpty()) {
            caption.setText(R.string.caption_tables);
        } else {
            caption.setText("Xin chào, " + fullName);
        }

        btnBack.setVisibility(View.GONE);

        // Right icon 2 → Bell
        View containerBtnRight2 = topBar.findViewById(R.id.container_btn_right_2);
        if (containerBtnRight2 != null) {
            containerBtnRight2.setVisibility(View.VISIBLE);
        }
        if (btnRight2 != null) {
            btnRight2.setImageResource(R.drawable.ic_bell);
            btnRight2.setOnClickListener(v -> startActivity(new Intent(this, com.example.cafe_manager.ui.communication.NotificationCenterActivity.class)));
        }
        TextView bellBadge = topBar.findViewById(R.id.badge_right_2);
        if (bellBadge != null) {
            com.example.cafe_manager.viewmodel.NewsViewModel newsViewModel = new ViewModelProvider(this).get(com.example.cafe_manager.viewmodel.NewsViewModel.class);
            int currentUserId = SessionManager.getInstance(this).getUserId();
            newsViewModel.getUnreadCount(currentUserId).observe(this, count -> {
                if (count != null && count > 0) {
                    bellBadge.setVisibility(View.VISIBLE);
                    bellBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                } else {
                    bellBadge.setVisibility(View.GONE);
                }
            });
        }

        // Right icon → popup menu: Lịch sử / Báo cáo / Đăng xuất
        btnRight.setVisibility(View.VISIBLE);
        btnRight.setImageResource(R.drawable.ic_menu_book);
        btnRight.setOnClickListener(this::showOptionsMenu);
    }

    private void showOptionsMenu(View anchor) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_options_menu, null);
        dialog.setContentView(sheet);

        String role = SessionManager.getInstance(this).getRole();

        // ----- Role-based visibility -----
        sheet.findViewById(R.id.row_history).setVisibility(
                PermissionUtils.canViewReports(role) ? View.VISIBLE : View.GONE);
        sheet.findViewById(R.id.row_dashboard).setVisibility(
                PermissionUtils.canViewReports(role) ? View.VISIBLE : View.GONE);
        sheet.findViewById(R.id.row_users).setVisibility(
                PermissionUtils.canManageUsers(role) ? View.VISIBLE : View.GONE);
        sheet.findViewById(R.id.row_tables).setVisibility(
                PermissionUtils.canManageTables(role) ? View.VISIBLE : View.GONE);
        sheet.findViewById(R.id.row_promotions).setVisibility(
                PermissionUtils.canManagePromotions(role) ? View.VISIBLE : View.GONE);
        sheet.findViewById(R.id.row_categories).setVisibility(
                PermissionUtils.canManageCategories(role) ? View.VISIBLE : View.GONE);
        boolean canManageShifts = PermissionUtils.canManageShifts(role);
        sheet.findViewById(R.id.row_shift_report).setVisibility(canManageShifts ? View.VISIBLE : View.GONE);
        sheet.findViewById(R.id.row_shift_template).setVisibility(canManageShifts ? View.VISIBLE : View.GONE);
        sheet.findViewById(R.id.row_shift_schedule).setVisibility(canManageShifts ? View.VISIBLE : View.GONE);
        sheet.findViewById(R.id.row_leave_approval).setVisibility(
                (Constants.ROLE_ADMIN.equals(role) || Constants.ROLE_MANAGER.equals(role)) ? View.VISIBLE : View.GONE);
        sheet.findViewById(R.id.row_attendance_report).setVisibility(
                (Constants.ROLE_ADMIN.equals(role) || Constants.ROLE_MANAGER.equals(role)) ? View.VISIBLE : View.GONE);

        // ----- Click handlers (IDs preserved, same navigation code) -----
        sheet.findViewById(R.id.row_profile).setOnClickListener(v -> {
            dialog.dismiss();
            // case 4
            startActivity(new Intent(this, ProfileActivity.class));
        });
        sheet.findViewById(R.id.row_my_shift).setOnClickListener(v -> {
            dialog.dismiss();
            // case 13
            startActivity(new Intent(this, MyShiftActivity.class));
        });
        sheet.findViewById(R.id.row_my_availability).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, com.example.cafe_manager.ui.availability.MyAvailabilityActivity.class));
        });
        sheet.findViewById(R.id.row_leave_request).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, LeaveRequestActivity.class));
        });
        sheet.findViewById(R.id.row_leave_approval).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, LeaveApprovalActivity.class));
        });
        sheet.findViewById(R.id.row_history).setOnClickListener(v -> {
            dialog.dismiss();
            // case 1
            startActivity(new Intent(this, HistoryActivity.class));
        });
        sheet.findViewById(R.id.row_news).setOnClickListener(v -> {
            dialog.dismiss();
            // case 14
            startActivity(new Intent(this, com.example.cafe_manager.ui.communication.NewsFeedActivity.class));
        });
        sheet.findViewById(R.id.row_chat).setOnClickListener(v -> {
            dialog.dismiss();
            // case 15
            startActivity(new Intent(this, com.example.cafe_manager.ui.communication.ChatRoomListActivity.class));
        });
        sheet.findViewById(R.id.row_dashboard).setOnClickListener(v -> {
            dialog.dismiss();
            // case 2
            startActivity(new Intent(this, RevenueReportActivity.class));
        });
        sheet.findViewById(R.id.row_users).setOnClickListener(v -> {
            dialog.dismiss();
            // case 5
            startActivity(new Intent(this, UserManagementActivity.class));
        });
        sheet.findViewById(R.id.row_attendance_report).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, AttendanceReportActivity.class));
        });
        sheet.findViewById(R.id.row_tables).setOnClickListener(v -> {
            dialog.dismiss();
            // case 6
            startActivity(new Intent(this, TableManagementActivity.class));
        });
        sheet.findViewById(R.id.row_promotions).setOnClickListener(v -> {
            dialog.dismiss();
            // case 7
            startActivity(new Intent(this, PromotionManagementActivity.class));
        });
        sheet.findViewById(R.id.row_categories).setOnClickListener(v -> {
            dialog.dismiss();
            // case 8
            startActivity(new Intent(this, CategoryManagementActivity.class));
        });
        sheet.findViewById(R.id.row_shift_report).setOnClickListener(v -> {
            dialog.dismiss();
            // case 10
            startActivity(new Intent(this, DailyShiftReportActivity.class));
        });
        sheet.findViewById(R.id.row_shift_template).setOnClickListener(v -> {
            dialog.dismiss();
            // case 11
            startActivity(new Intent(this, ShiftTemplateActivity.class));
        });
        sheet.findViewById(R.id.row_shift_schedule).setOnClickListener(v -> {
            dialog.dismiss();
            // case 12
            startActivity(new Intent(this, ShiftScheduleActivity.class));
        });
        sheet.findViewById(R.id.row_logout).setOnClickListener(v -> {
            dialog.dismiss();
            // case 3
            showLogoutDialog();
        });

        dialog.show();
    }

    private void launchShiftActivity(Class<?> targetActivityClass) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            ShiftEntity openShift = AppDatabase.getInstance(this).shiftDao().getCurrentlyOpen();
            AppExecutors.getInstance().mainThread().execute(() -> {
                if (openShift != null) {
                    Intent intent = new Intent(this, targetActivityClass);
                    intent.putExtra("extra_shift_id", openShift.getShiftId());
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Không có ca làm việc nào đang mở.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất?")
                .setMessage("Bạn sẽ phải đăng nhập lại để tiếp tục.")
                .setPositiveButton("Đăng xuất", (d, w) -> performLogout())
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void performLogout() {
        SessionManager.getInstance(this).logout();
        CartManager.getInstance().clearCart();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBottomNav() {
        View bottomNav = findViewById(R.id.bottom_nav);
        View navTables = bottomNav.findViewById(R.id.nav_tables);
        View navOrders = bottomNav.findViewById(R.id.nav_orders);
        View navMenu = bottomNav.findViewById(R.id.nav_menu);
        View navDashboard = bottomNav.findViewById(R.id.nav_dashboard);
        navTables.setSelected(true);
        navDashboard.setSelected(false);
        navOrders.setOnClickListener(v -> startActivity(new Intent(this, OrdersListActivity.class)));

        String role = SessionManager.getInstance(this).getRole();
        if (!PermissionUtils.canManageMenu(role)) {
            navMenu.setVisibility(View.GONE);
        } else {
            navMenu.setOnClickListener(v -> startActivity(new Intent(this, AdminMenuActivity.class)));
        }

        navDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        RecyclerView rvAreas = findViewById(R.id.rv_areas);
        rvAreas.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        areaAdapter = new AreaAdapter(area -> {
            viewModel.selectArea(area);
        });
        rvAreas.setAdapter(areaAdapter);

        RecyclerView rv = findViewById(R.id.rv_tables);
        rv.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new TableAdapter(this::onTableClicked);
        rv.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TableViewModel.class);

        viewModel.getTables().observe(this, list -> {
            if (list != null) adapter.submitList(list);
        });

        viewModel.getAreas().observe(this, areas -> {
            if (areas != null) areaAdapter.submitList(areas);
        });

        viewModel.getSelectedArea().observe(this, area -> {
            if (area != null) areaAdapter.setSelectedArea(area);
        });

        viewModel.getOccupiedCount().observe(this, c -> updateActiveStat());
        viewModel.getTotalCount().observe(this, c -> updateActiveStat());
        viewModel.getEmptyCount().observe(this, empty -> {
            if (empty != null) tvEmptyCount.setText(String.valueOf(empty));
        });
    }

    private void updateActiveStat() {
        Integer total = viewModel.getTotalCount().getValue();
        Integer occupied = viewModel.getOccupiedCount().getValue();
        if (total != null && occupied != null) {
            tvActiveCount.setText(getString(R.string.format_total_tables, occupied, total));
        }
    }

    private void onTableClicked(TableEntity table) {
        CartManager.getInstance().clearCart();
        if (Constants.TABLE_EMPTY.equals(table.getStatus())) {
            Intent intent = new Intent(this, MenuActivity.class);
            intent.putExtra(EXTRA_TABLE_ID, table.getTableId());
            intent.putExtra(EXTRA_TABLE_NAME, table.getTableName());
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, OrdersListActivity.class);
            startActivity(intent);
        }
    }
}
