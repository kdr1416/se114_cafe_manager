package com.example.cafe_manager.ui.dashboard;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.manager.CartManager;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.model.DailyRevenueRow;
import com.example.cafe_manager.model.PaymentMethodStatsRow;
import com.example.cafe_manager.ui.admin.AdminMenuActivity;
import com.example.cafe_manager.ui.menu.CategoryManagementActivity;
import com.example.cafe_manager.ui.promotion.PromotionManagementActivity;
import com.example.cafe_manager.ui.table.TableManagementActivity;
import com.example.cafe_manager.ui.user.UserManagementActivity;
import com.example.cafe_manager.ui.attendance.AttendanceReportActivity;
import com.example.cafe_manager.ui.availability.MyAvailabilityActivity;
import com.example.cafe_manager.ui.communication.MessengerActivity;
import com.example.cafe_manager.ui.communication.NewsFeedActivity;
import com.example.cafe_manager.ui.history.HistoryActivity;
import com.example.cafe_manager.ui.leave.LeaveApprovalActivity;
import com.example.cafe_manager.ui.leave.LeaveRequestActivity;
import com.example.cafe_manager.ui.auth.LoginActivity;
import com.example.cafe_manager.ui.orderslist.OrdersListActivity;
import com.example.cafe_manager.ui.profile.ProfileActivity;
import com.example.cafe_manager.ui.shift.MyShiftActivity;
import com.example.cafe_manager.ui.shift.ShiftReportActivity;
import com.example.cafe_manager.ui.shift.DailyShiftReportActivity;
import com.example.cafe_manager.ui.shift.ShiftScheduleActivity;
import com.example.cafe_manager.ui.shift.ShiftTemplateActivity;
import com.example.cafe_manager.ui.table.TableActivity;
import com.example.cafe_manager.data.remote.ApiClient;
import com.example.cafe_manager.data.remote.ChatApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.CurrencyUtils;
import com.example.cafe_manager.util.DateRange;
import com.example.cafe_manager.util.PermissionUtils;
import com.example.cafe_manager.viewmodel.DashboardViewModel;
import com.example.cafe_manager.viewmodel.NewsViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private DashboardViewModel viewModel;
    private TodayShiftAdapter todayShiftAdapter;
    private TopProductAdapter topAdapter;

    // View bindings for different layouts dynamically inflated
    private FrameLayout containerDashboard;
    
    // STAFF specific views
    private TextView tvStatShifts, tvStatHours, tvStatOrders, tvStatRevenue;

    // MANAGER / ADMIN specific views
    private TextView tvStatTodayShifts, tvStatActiveStaff, tvStatTodayRevenue;
    private TextView tvRevenue, tvOrderCount, tvAvgOrder;
    private TextView tvCashCount, tvCashRevenue;
    private TextView tvBankingCount, tvBankingRevenue;
    private TextView tvMomoCount, tvMomoRevenue;
    private TextView tvTopEmpty, tvChartEmpty;
    private BarChart chartRevenue;
    private PieChart chartPayment;
    private TextView chipToday, chipWeek, chipMonth;

    // ADMIN specific views
    private TextView tvStatSystemUsers, tvStatSystemTables, tvStatSystemProducts;
    private TextView tvCountAdmin, tvCountManager, tvCountStaff, tvUserActiveRatio;

    // Common views
    private RecyclerView rvTodayShifts;
    private TextView tvNoShifts;
    private RecyclerView rvTop;

    private int colorAccent;
    private int colorSuccess;
    private int colorWarning;
    private int colorInfo;
    
    private String currentRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        currentRole = SessionManager.getInstance(this).getRole();

        colorAccent = getColor(R.color.accent);
        colorSuccess = getColor(R.color.success);
        colorWarning = getColor(R.color.warning);
        colorInfo = getColor(R.color.info);

        setupTopBar();
        setupBottomNav();
        
        containerDashboard = findViewById(R.id.container_dashboard);
        inflateDashboardLayout();
        
        setupViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadDashboardData();
        }
        refreshUnreadBadge();
        com.example.cafe_manager.data.repository.NewsRepository.getInstance(this).refreshPosts();
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);
        
        title.setText("Tổng quan");
        btnBack.setVisibility(View.GONE); // Dashboard is entry screen

        // Bell button (btn_right_2)
        View containerBtnRight2 = topBar.findViewById(R.id.container_btn_right_2);
        ImageButton btnRight2 = topBar.findViewById(R.id.btn_right_2);
        TextView bellBadge = topBar.findViewById(R.id.badge_right_2);
        
        if (containerBtnRight2 != null) containerBtnRight2.setVisibility(View.VISIBLE);
        if (btnRight2 != null) {
            btnRight2.setImageResource(R.drawable.ic_bell);
            btnRight2.setOnClickListener(v -> startActivity(new Intent(this, NewsFeedActivity.class)));
        }
        
        // Bell Badge Count Observer
        if (bellBadge != null) {
            NewsViewModel newsViewModel = new ViewModelProvider(this).get(NewsViewModel.class);
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

        // Chat button (btn_right_3)
        View containerBtnRight3 = topBar.findViewById(R.id.container_btn_right_3);
        ImageButton btnRight3 = topBar.findViewById(R.id.btn_right_3);
        
        if (containerBtnRight3 != null) containerBtnRight3.setVisibility(View.VISIBLE);
        if (btnRight3 != null) {
            btnRight3.setImageResource(R.drawable.ic_chat);
            btnRight3.setOnClickListener(v -> startActivity(new Intent(this, MessengerActivity.class)));
        }

        // Options Menu button (btn_right)
        ImageButton btnRight = topBar.findViewById(R.id.btn_right);
        btnRight.setVisibility(View.VISIBLE);
        btnRight.setImageResource(R.drawable.ic_menu_book);
        btnRight.setOnClickListener(this::showOptionsMenu);
    }

    private void showOptionsMenu(View anchor) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_options_menu, null);
        dialog.setContentView(sheet);

        // Role-based visibility
        sheet.findViewById(R.id.row_history).setVisibility(
                PermissionUtils.canViewReports(currentRole) ? View.VISIBLE : View.GONE);
        sheet.findViewById(R.id.row_dashboard).setVisibility(
                PermissionUtils.canViewReports(currentRole) ? View.VISIBLE : View.GONE);
        sheet.findViewById(R.id.row_users).setVisibility(
                PermissionUtils.canManageUsers(currentRole) ? View.VISIBLE : View.GONE);
        sheet.findViewById(R.id.row_tables).setVisibility(
                PermissionUtils.canManageTables(currentRole) ? View.VISIBLE : View.GONE);
        sheet.findViewById(R.id.row_promotions).setVisibility(
                PermissionUtils.canManagePromotions(currentRole) ? View.VISIBLE : View.GONE);
        sheet.findViewById(R.id.row_categories).setVisibility(
                PermissionUtils.canManageCategories(currentRole) ? View.VISIBLE : View.GONE);
        
        boolean canManageShifts = PermissionUtils.canManageShifts(currentRole);
        sheet.findViewById(R.id.row_shift_report).setVisibility(canManageShifts ? View.VISIBLE : View.GONE);
        sheet.findViewById(R.id.row_shift_template).setVisibility(canManageShifts ? View.VISIBLE : View.GONE);
        sheet.findViewById(R.id.row_shift_schedule).setVisibility(canManageShifts ? View.VISIBLE : View.GONE);
        sheet.findViewById(R.id.row_leave_approval).setVisibility(
                (Constants.ROLE_ADMIN.equals(currentRole) || Constants.ROLE_MANAGER.equals(currentRole)) ? View.VISIBLE : View.GONE);
        sheet.findViewById(R.id.row_attendance_report).setVisibility(
                (Constants.ROLE_ADMIN.equals(currentRole) || Constants.ROLE_MANAGER.equals(currentRole)) ? View.VISIBLE : View.GONE);

        // Click handlers
        sheet.findViewById(R.id.row_profile).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, ProfileActivity.class));
        });
        sheet.findViewById(R.id.row_my_shift).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, MyShiftActivity.class));
        });
        sheet.findViewById(R.id.row_my_availability).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, MyAvailabilityActivity.class));
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
            startActivity(new Intent(this, HistoryActivity.class));
        });
        sheet.findViewById(R.id.row_news).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, NewsFeedActivity.class));
        });
        sheet.findViewById(R.id.row_chat).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, MessengerActivity.class));
        });
        sheet.findViewById(R.id.row_dashboard).setOnClickListener(v -> {
            dialog.dismiss();
            // Just refresh data
            if (viewModel != null) viewModel.loadDashboardData();
        });
        sheet.findViewById(R.id.row_users).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, UserManagementActivity.class));
        });
        sheet.findViewById(R.id.row_attendance_report).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, AttendanceReportActivity.class));
        });
        sheet.findViewById(R.id.row_tables).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, TableManagementActivity.class));
        });
        sheet.findViewById(R.id.row_promotions).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, PromotionManagementActivity.class));
        });
        sheet.findViewById(R.id.row_categories).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, CategoryManagementActivity.class));
        });
        sheet.findViewById(R.id.row_shift_report).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, DailyShiftReportActivity.class));
        });
        sheet.findViewById(R.id.row_shift_template).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, ShiftTemplateActivity.class));
        });
        sheet.findViewById(R.id.row_shift_schedule).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, ShiftScheduleActivity.class));
        });
        sheet.findViewById(R.id.row_logout).setOnClickListener(v -> {
            dialog.dismiss();
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

        navDashboard.setSelected(true);

        navTables.setOnClickListener(v -> {
            Intent intent = new Intent(this, TableActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        navOrders.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrdersListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        if (!PermissionUtils.canManageMenu(currentRole)) {
            navMenu.setVisibility(View.GONE);
        } else {
            navMenu.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminMenuActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }
    }

    private void inflateDashboardLayout() {
        containerDashboard.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        View view;

        if (Constants.ROLE_STAFF.equals(currentRole)) {
            view = inflater.inflate(R.layout.layout_dashboard_staff, containerDashboard, false);
            bindStaffViews(view);
        } else if (Constants.ROLE_MANAGER.equals(currentRole)) {
            view = inflater.inflate(R.layout.layout_dashboard_manager, containerDashboard, false);
            bindManagerViews(view);
        } else {
            // ADMIN
            view = inflater.inflate(R.layout.layout_dashboard_admin, containerDashboard, false);
            bindAdminViews(view);
        }
        containerDashboard.addView(view);
    }

    private void bindStaffViews(View root) {
        rvTodayShifts = root.findViewById(R.id.rv_today_shifts);
        tvNoShifts = root.findViewById(R.id.tv_no_shifts);
        
        tvStatShifts = root.findViewById(R.id.tv_stat_shifts);
        tvStatHours = root.findViewById(R.id.tv_stat_hours);
        tvStatOrders = root.findViewById(R.id.tv_stat_orders);
        tvStatRevenue = root.findViewById(R.id.tv_stat_revenue);
        
        setupShiftsRecyclerView(true);
    }

    private void bindManagerViews(View root) {
        rvTodayShifts = root.findViewById(R.id.rv_today_shifts);
        tvNoShifts = root.findViewById(R.id.tv_no_shifts);

        tvStatTodayShifts = root.findViewById(R.id.tv_stat_today_shifts);
        tvStatActiveStaff = root.findViewById(R.id.tv_stat_active_staff);
        tvStatTodayRevenue = root.findViewById(R.id.tv_stat_today_revenue);

        tvRevenue = root.findViewById(R.id.tv_revenue);
        tvOrderCount = root.findViewById(R.id.tv_order_count);
        tvAvgOrder = root.findViewById(R.id.tv_avg_order);

        tvCashCount = root.findViewById(R.id.tv_cash_count);
        tvCashRevenue = root.findViewById(R.id.tv_cash_revenue);
        tvBankingCount = root.findViewById(R.id.tv_banking_count);
        tvBankingRevenue = root.findViewById(R.id.tv_banking_revenue);
        tvMomoCount = root.findViewById(R.id.tv_momo_count);
        tvMomoRevenue = root.findViewById(R.id.tv_momo_revenue);

        tvTopEmpty = root.findViewById(R.id.tv_top_empty);
        tvChartEmpty = root.findViewById(R.id.tv_chart_empty);

        chipToday = root.findViewById(R.id.chip_today);
        chipWeek = root.findViewById(R.id.chip_week);
        chipMonth = root.findViewById(R.id.chip_month);

        chartRevenue = root.findViewById(R.id.chart_revenue);
        chartPayment = root.findViewById(R.id.chart_payment);
        
        rvTop = root.findViewById(R.id.rv_top_products);

        setupShiftsRecyclerView(false);
        setupChips();
        setupCharts();
        setupTopProductsRecyclerView();
    }

    private void bindAdminViews(View root) {
        // ADMIN shares almost all views with MANAGER + has system/staff statistics
        rvTodayShifts = root.findViewById(R.id.rv_today_shifts);
        tvNoShifts = root.findViewById(R.id.tv_no_shifts);

        tvStatSystemUsers = root.findViewById(R.id.tv_stat_system_users);
        tvStatSystemTables = root.findViewById(R.id.tv_stat_system_tables);
        tvStatSystemProducts = root.findViewById(R.id.tv_stat_system_products);

        tvCountAdmin = root.findViewById(R.id.tv_count_admin);
        tvCountManager = root.findViewById(R.id.tv_count_manager);
        tvCountStaff = root.findViewById(R.id.tv_count_staff);
        tvUserActiveRatio = root.findViewById(R.id.tv_user_active_ratio);

        tvRevenue = root.findViewById(R.id.tv_revenue);
        tvOrderCount = root.findViewById(R.id.tv_order_count);
        tvAvgOrder = root.findViewById(R.id.tv_avg_order);

        tvCashCount = root.findViewById(R.id.tv_cash_count);
        tvCashRevenue = root.findViewById(R.id.tv_cash_revenue);
        tvBankingCount = root.findViewById(R.id.tv_banking_count);
        tvBankingRevenue = root.findViewById(R.id.tv_banking_revenue);
        tvMomoCount = root.findViewById(R.id.tv_momo_count);
        tvMomoRevenue = root.findViewById(R.id.tv_momo_revenue);

        tvTopEmpty = root.findViewById(R.id.tv_top_empty);
        tvChartEmpty = root.findViewById(R.id.tv_chart_empty);

        chipToday = root.findViewById(R.id.chip_today);
        chipWeek = root.findViewById(R.id.chip_week);
        chipMonth = root.findViewById(R.id.chip_month);

        chartRevenue = root.findViewById(R.id.chart_revenue);
        chartPayment = root.findViewById(R.id.chart_payment);

        rvTop = root.findViewById(R.id.rv_top_products);

        setupShiftsRecyclerView(false);
        setupChips();
        setupCharts();
        setupTopProductsRecyclerView();
    }

    private void setupShiftsRecyclerView(boolean isStaff) {
        rvTodayShifts.setLayoutManager(new LinearLayoutManager(this));
        todayShiftAdapter = new TodayShiftAdapter(isStaff, new TodayShiftAdapter.OnTodayShiftActionListener() {
            @Override
            public void onCheckIn(int shiftId) {
                viewModel.checkIn(shiftId);
            }

            @Override
            public void onCheckOut(int shiftId) {
                viewModel.checkOut(shiftId);
            }
        });
        rvTodayShifts.setAdapter(todayShiftAdapter);
    }

    private void setupTopProductsRecyclerView() {
        rvTop.setLayoutManager(new LinearLayoutManager(this));
        topAdapter = new TopProductAdapter();
        rvTop.setAdapter(topAdapter);
    }

    private void setupChips() {
        chipToday.setOnClickListener(v -> viewModel.selectPeriod(DateRange.Period.TODAY));
        chipWeek.setOnClickListener(v -> viewModel.selectPeriod(DateRange.Period.WEEK));
        chipMonth.setOnClickListener(v -> viewModel.selectPeriod(DateRange.Period.MONTH));
    }

    private void setupCharts() {
        chartRevenue.getDescription().setEnabled(false);
        chartRevenue.setDrawGridBackground(false);
        chartRevenue.setDrawBarShadow(false);
        chartRevenue.setFitBars(true);
        chartRevenue.getLegend().setEnabled(false);
        chartRevenue.setTouchEnabled(false);
        chartRevenue.getAxisRight().setEnabled(false);
        chartRevenue.getAxisLeft().setTextColor(getColor(R.color.text_soft));
        chartRevenue.getAxisLeft().setTextSize(10f);
        chartRevenue.getAxisLeft().setAxisMinimum(0f);
        chartRevenue.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000);
                if (value >= 1_000) return String.format("%.0fK", value / 1_000);
                return String.valueOf((int) value);
            }
        });

        XAxis xAxis = chartRevenue.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(getColor(R.color.text_soft));
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1f);

        chartPayment.getDescription().setEnabled(false);
        chartPayment.setUsePercentValues(true);
        chartPayment.setDrawHoleEnabled(true);
        chartPayment.setHoleColor(Color.WHITE);
        chartPayment.setHoleRadius(45f);
        chartPayment.setTransparentCircleRadius(50f);
        chartPayment.setEntryLabelTextSize(11f);
        chartPayment.setEntryLabelColor(getColor(R.color.text_primary));
        chartPayment.getLegend().setEnabled(false);
        chartPayment.setTouchEnabled(false);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        // Observe greeting for TopBar caption
        View topBar = findViewById(R.id.top_bar);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        viewModel.getGreeting().observe(this, caption::setText);

        // Observe loading / error
        viewModel.getError().observe(this, err -> {
            if (err != null) {
                Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });

        // Observe today's shifts (All roles)
        viewModel.getTodayShifts().observe(this, shifts -> {
            todayShiftAdapter.setItems(shifts);
            boolean empty = shifts == null || shifts.isEmpty();
            rvTodayShifts.setVisibility(empty ? View.GONE : View.VISIBLE);
            tvNoShifts.setVisibility(empty ? View.VISIBLE : View.GONE);
        });

        if (Constants.ROLE_STAFF.equals(currentRole)) {
            // Observe Staff Monthly Stats
            viewModel.getStaffMonthlyShifts().observe(this, count -> 
                tvStatShifts.setText(count + " ca")
            );
            viewModel.getStaffMonthlyHours().observe(this, hours -> 
                tvStatHours.setText(String.format(Locale.getDefault(), "%.1fh", hours))
            );
            viewModel.getStaffMonthlyOrders().observe(this, count -> 
                tvStatOrders.setText(count + " đơn")
            );
            viewModel.getStaffMonthlyRevenue().observe(this, rev -> 
                tvStatRevenue.setText(formatShortVnd(rev))
            );
        } else {
            // Observe Manager / Admin KPI overview cards
            if (Constants.ROLE_MANAGER.equals(currentRole)) {
                viewModel.getManagerTodayShiftsCount().observe(this, count -> 
                    tvStatTodayShifts.setText(count + " ca")
                );
                viewModel.getManagerTodayActiveStaffCount().observe(this, count -> 
                    tvStatActiveStaff.setText(count + " NV")
                );
                viewModel.getManagerTodayRevenue().observe(this, rev -> 
                    tvStatTodayRevenue.setText(formatShortVnd(rev))
                );
            }

            // Observe Revenue report live data
            viewModel.getPeriod().observe(this, this::updateChipSelection);
            
            viewModel.getRevenue().observe(this, rev -> {
                tvRevenue.setText(CurrencyUtils.formatVnd(rev != null ? rev : 0));
                updateAvg();
            });

            viewModel.getOrderCount().observe(this, count -> {
                tvOrderCount.setText(String.valueOf(count != null ? count : 0));
                updateAvg();
            });

            viewModel.getTopProducts().observe(this, list -> {
                topAdapter.submitList(list);
                boolean empty = list == null || list.isEmpty();
                rvTop.setVisibility(empty ? View.GONE : View.VISIBLE);
                tvTopEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            });

            viewModel.getPaymentMethodStats().observe(this, this::updatePaymentStats);
            viewModel.getDailyRevenue().observe(this, this::updateRevenueChart);

            if (Constants.ROLE_ADMIN.equals(currentRole)) {
                // Observe Admin system stats
                viewModel.getAdminTotalUsers().observe(this, count -> 
                    tvStatSystemUsers.setText(count + " NV")
                );
                viewModel.getAdminTotalTables().observe(this, count -> 
                    tvStatSystemTables.setText(count + " bàn")
                );
                viewModel.getAdminTotalProducts().observe(this, count -> 
                    tvStatSystemProducts.setText(count + " SP")
                );

                // Observe Admin breakdown stats
                viewModel.getAdminCountAdmin().observe(this, count -> 
                    tvCountAdmin.setText(String.valueOf(count))
                );
                viewModel.getAdminCountManager().observe(this, count -> 
                    tvCountManager.setText(String.valueOf(count))
                );
                viewModel.getAdminCountStaff().observe(this, count -> 
                    tvCountStaff.setText(String.valueOf(count))
                );
                
                viewModel.getAdminActiveUsers().observe(this, active -> {
                    Integer total = viewModel.getAdminTotalUsers().getValue();
                    if (total == null) total = 0;
                    tvUserActiveRatio.setText("Đang hoạt động: " + active + "/" + total);
                });
            }
        }
    }

    private void updateChipSelection(DateRange.Period period) {
        chipToday.setSelected(period == DateRange.Period.TODAY);
        chipWeek.setSelected(period == DateRange.Period.WEEK);
        chipMonth.setSelected(period == DateRange.Period.MONTH);

        int onAccent = getColor(R.color.text_on_accent);
        int onSoft = getColor(R.color.text_soft);
        chipToday.setTextColor(period == DateRange.Period.TODAY ? onAccent : onSoft);
        chipWeek.setTextColor(period == DateRange.Period.WEEK ? onAccent : onSoft);
        chipMonth.setTextColor(period == DateRange.Period.MONTH ? onAccent : onSoft);
    }

    private void updateAvg() {
        Double rev = viewModel.getRevenue().getValue();
        Integer count = viewModel.getOrderCount().getValue();
        if (rev == null || count == null || count == 0) {
            tvAvgOrder.setText(CurrencyUtils.formatVnd(0));
        } else {
            tvAvgOrder.setText(CurrencyUtils.formatVnd(rev / count));
        }
    }

    private void updateRevenueChart(List<DailyRevenueRow> data) {
        if (data == null || data.isEmpty()) {
            chartRevenue.setVisibility(View.GONE);
            tvChartEmpty.setVisibility(View.VISIBLE);
            return;
        }

        chartRevenue.setVisibility(View.VISIBLE);
        tvChartEmpty.setVisibility(View.GONE);

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            entries.add(new BarEntry(i, (float) data.get(i).dailyRevenue));
            labels.add(data.get(i).dayLabel);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Doanh thu");
        dataSet.setColor(colorAccent);
        dataSet.setDrawValues(data.size() <= 7);
        dataSet.setValueTextSize(9f);
        dataSet.setValueTextColor(getColor(R.color.text_soft));
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000);
                if (value >= 1_000) return String.format("%.0fK", value / 1_000);
                return String.valueOf((int) value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        chartRevenue.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartRevenue.getXAxis().setLabelCount(Math.min(labels.size(), 7));
        chartRevenue.setData(barData);
        chartRevenue.invalidate();
    }

    private void updatePaymentStats(List<PaymentMethodStatsRow> list) {
        tvCashCount.setText("0 order");
        tvCashRevenue.setText(CurrencyUtils.formatVnd(0));
        tvBankingCount.setText("0 order");
        tvBankingRevenue.setText(CurrencyUtils.formatVnd(0));
        tvMomoCount.setText("0 order");
        tvMomoRevenue.setText(CurrencyUtils.formatVnd(0));

        if (list == null || list.isEmpty()) {
            chartPayment.setVisibility(View.GONE);
            return;
        }

        chartPayment.setVisibility(View.VISIBLE);
        List<PieEntry> pieEntries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        for (PaymentMethodStatsRow row : list) {
            if (Constants.PAYMENT_CASH.equals(row.paymentMethod)) {
                tvCashCount.setText(row.orderCount + " order");
                tvCashRevenue.setText(CurrencyUtils.formatVnd(row.totalRevenue));
                pieEntries.add(new PieEntry((float) row.totalRevenue, "Tiền mặt"));
                colors.add(colorSuccess);
            } else if (Constants.PAYMENT_BANKING.equals(row.paymentMethod)) {
                tvBankingCount.setText(row.orderCount + " order");
                tvBankingRevenue.setText(CurrencyUtils.formatVnd(row.totalRevenue));
                pieEntries.add(new PieEntry((float) row.totalRevenue, "Banking"));
                colors.add(colorInfo);
            } else if (Constants.PAYMENT_MOMO.equals(row.paymentMethod)) {
                tvMomoCount.setText(row.orderCount + " order");
                tvMomoRevenue.setText(CurrencyUtils.formatVnd(row.totalRevenue));
                pieEntries.add(new PieEntry((float) row.totalRevenue, "MoMo"));
                colors.add(colorWarning);
            }
        }

        if (pieEntries.isEmpty()) {
            chartPayment.setVisibility(View.GONE);
            return;
        }

        PieDataSet dataSet = new PieDataSet(pieEntries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.0f%%", value);
            }
        });

        PieData pieData = new PieData(dataSet);
        chartPayment.setData(pieData);
        chartPayment.invalidate();
    }

    private String formatShortVnd(double value) {
        if (value >= 1_000_000) {
            return String.format(Locale.getDefault(), "%.1fM", value / 1_000_000.0);
        } else if (value >= 1_000) {
            return String.format(Locale.getDefault(), "%.0fK", value / 1_000.0);
        } else {
            return String.format(Locale.getDefault(), "%.0f", value);
        }
    }

    private void refreshUnreadBadge() {
        View topBar = findViewById(R.id.top_bar);
        if (topBar == null) return;
        TextView chatBadge = topBar.findViewById(R.id.badge_right_3);
        if (chatBadge == null) return;

        ApiClient.getInstance(this)
            .getService(ChatApiService.class)
            .getUnreadCount()
            .enqueue(new Callback<Integer>() {
                @Override
                public void onResponse(Call<Integer> call, Response<Integer> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        int count = response.body();
                        if (count > 0) {
                            chatBadge.setVisibility(View.VISIBLE);
                            chatBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                        } else {
                            chatBadge.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onFailure(Call<Integer> call, Throwable t) {
                    // Fail silently
                }
            });
    }
}
