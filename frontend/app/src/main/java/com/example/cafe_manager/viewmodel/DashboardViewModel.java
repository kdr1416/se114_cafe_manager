package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.dao.OrderItemDao;
import com.example.cafe_manager.data.local.dao.PaymentDao;
import com.example.cafe_manager.data.local.entity.AttendanceEntity;
import com.example.cafe_manager.data.local.entity.ShiftAssignmentEntity;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.data.repository.AttendanceRepository;
import com.example.cafe_manager.data.repository.ShiftRepository;
import com.example.cafe_manager.data.remote.UserAttendanceDetailResponse;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.model.DailyRevenueRow;
import com.example.cafe_manager.model.PaymentMethodStatsRow;
import com.example.cafe_manager.model.TopProductRow;
import com.example.cafe_manager.ui.dashboard.TodayShiftItem;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.DateRange;
import com.example.cafe_manager.util.RepositoryCallback;

import com.example.cafe_manager.data.repository.RevenueRepository;
import com.example.cafe_manager.data.remote.RevenueReportResponse;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.List;
import java.util.Map;

public class DashboardViewModel extends AndroidViewModel {

    private static final int TOP_LIMIT = 5;

    private final AppDatabase appDatabase;
    private final PaymentDao paymentDao;
    private final OrderItemDao orderItemDao;
    private final int currentUserId;
    private final String currentRole;

    private final MutableLiveData<DateRange.Period> periodLive =
            new MutableLiveData<>(DateRange.Period.TODAY);

    // Existing revenue report LiveData
    private final MutableLiveData<Double> revenueLive = new MutableLiveData<>(0.0);
    private final MutableLiveData<Integer> orderCountLive = new MutableLiveData<>(0);
    private final MutableLiveData<List<TopProductRow>> topProductsLive = new MutableLiveData<>(new java.util.ArrayList<>());
    private final MutableLiveData<List<PaymentMethodStatsRow>> paymentMethodLive = new MutableLiveData<>(new java.util.ArrayList<>());
    private final MutableLiveData<List<DailyRevenueRow>> dailyRevenueLive = new MutableLiveData<>(new java.util.ArrayList<>());
    private final MutableLiveData<Boolean> isRevenueLoading = new MutableLiveData<>(false);

    // Unified/New LiveData for Personalized Dashboard
    private final MutableLiveData<List<TodayShiftItem>> todayShiftsLive = new MutableLiveData<>();
    private final MutableLiveData<String> greetingLive = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLive = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLive = new MutableLiveData<>();

    // Staff-specific Monthly LiveData
    private final MutableLiveData<Integer> staffMonthlyShiftsLive = new MutableLiveData<>(0);
    private final MutableLiveData<Double> staffMonthlyHoursLive = new MutableLiveData<>(0.0);
    private final MutableLiveData<Integer> staffMonthlyOrdersLive = new MutableLiveData<>(0);
    private final MutableLiveData<Double> staffMonthlyRevenueLive = new MutableLiveData<>(0.0);

    // Manager-specific LiveData
    private final MutableLiveData<Integer> managerTodayShiftsCountLive = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> managerTodayActiveStaffCountLive = new MutableLiveData<>(0);
    private final MutableLiveData<Double> managerTodayRevenueLive = new MutableLiveData<>(0.0);

    // Admin-specific LiveData
    private final MutableLiveData<Integer> adminTotalUsersLive = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> adminTotalTablesLive = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> adminTotalProductsLive = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> adminCountAdminLive = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> adminCountManagerLive = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> adminCountStaffLive = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> adminActiveUsersLive = new MutableLiveData<>(0);

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        this.appDatabase = AppDatabase.getInstance(application);
        this.paymentDao = appDatabase.paymentDao();
        this.orderItemDao = appDatabase.orderItemDao();

        SessionManager sm = SessionManager.getInstance(application);
        this.currentUserId = sm.getUserId();
        this.currentRole = sm.getRole();

        // Greeting
        String fullName = sm.getFullName();
        if (fullName == null || fullName.isEmpty()) {
            fullName = sm.getUsername();
        }
        greetingLive.setValue("Xin chào, " + fullName);

    }

    // ── Getters ──
    public LiveData<DateRange.Period> getPeriod() { return periodLive; }
    public LiveData<Double> getRevenue() { return revenueLive; }
    public LiveData<Integer> getOrderCount() { return orderCountLive; }
    public LiveData<List<TopProductRow>> getTopProducts() { return topProductsLive; }
    public LiveData<List<PaymentMethodStatsRow>> getPaymentMethodStats() { return paymentMethodLive; }
    public LiveData<List<DailyRevenueRow>> getDailyRevenue() { return dailyRevenueLive; }

    public LiveData<Double> getRevenueLive() { return revenueLive; }
    public LiveData<Integer> getOrderCountLive() { return orderCountLive; }
    public LiveData<List<PaymentMethodStatsRow>> getPaymentMethodLive() { return paymentMethodLive; }
    public LiveData<List<DailyRevenueRow>> getDailyRevenueLive() { return dailyRevenueLive; }
    public LiveData<Boolean> getIsRevenueLoading() { return isRevenueLoading; }

    public LiveData<List<TodayShiftItem>> getTodayShifts() { return todayShiftsLive; }
    public LiveData<String> getGreeting() { return greetingLive; }
    public LiveData<Boolean> getLoading() { return loadingLive; }
    public LiveData<String> getError() { return errorLive; }

    // Staff getters
    public LiveData<Integer> getStaffMonthlyShifts() { return staffMonthlyShiftsLive; }
    public LiveData<Double> getStaffMonthlyHours() { return staffMonthlyHoursLive; }
    public LiveData<Integer> getStaffMonthlyOrders() { return staffMonthlyOrdersLive; }
    public LiveData<Double> getStaffMonthlyRevenue() { return staffMonthlyRevenueLive; }

    // Manager getters
    public LiveData<Integer> getManagerTodayShiftsCount() { return managerTodayShiftsCountLive; }
    public LiveData<Integer> getManagerTodayActiveStaffCount() { return managerTodayActiveStaffCountLive; }
    public LiveData<Double> getManagerTodayRevenue() { return managerTodayRevenueLive; }

    // Admin getters
    public LiveData<Integer> getAdminTotalUsers() { return adminTotalUsersLive; }
    public LiveData<Integer> getAdminTotalTables() { return adminTotalTablesLive; }
    public LiveData<Integer> getAdminTotalProducts() { return adminTotalProductsLive; }
    public LiveData<Integer> getAdminCountAdmin() { return adminCountAdminLive; }
    public LiveData<Integer> getAdminCountManager() { return adminCountManagerLive; }
    public LiveData<Integer> getAdminCountStaff() { return adminCountStaffLive; }
    public LiveData<Integer> getAdminActiveUsers() { return adminActiveUsersLive; }

    public void selectPeriod(DateRange.Period p) {
        DateRange.Period current = periodLive.getValue();
        if (current == p) return;
        periodLive.setValue(p);
        loadDashboardData();
    }

    public void clearError() {
        errorLive.setValue(null);
    }

    // ── Business Actions & Load ──

    private long lastSyncTime = 0;
    private static final long SYNC_DEBOUNCE_MS = 3000; // 3 seconds

    public void loadDashboardData() {
        loadingLive.setValue(true);
        
        // Step 1: Load local data IMMEDIATELY — show UI right away
        loadLocalData();

        // Step 2: Sync from API in background (non-blocking)
        if (!Constants.ROLE_STAFF.equalsIgnoreCase(currentRole)) {
            ShiftRepository.getInstance(getApplication()).syncMyShiftsAndAssignments(new RepositoryCallback<Void>() {
                @Override
                public void onSuccess(Void res) {
                    loadLocalData();
                    fetchRevenueReport();
                }

                @Override
                public void onError(Exception e) {
                    loadLocalData();
                    fetchRevenueReport();
                }
            });
        } else {
            // STAFF -> Sync my shifts + assignments
            ShiftRepository.getInstance(getApplication()).syncMyShiftsAndAssignments(new RepositoryCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    AttendanceRepository.getInstance(getApplication()).syncMyAttendances(new RepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(Void res) {
                            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
                            int year = cal.get(Calendar.YEAR);
                            int month = cal.get(Calendar.MONTH) + 1;
                            AttendanceRepository.getInstance(getApplication()).getUserDetails(
                                    currentUserId, year, month, new RepositoryCallback<UserAttendanceDetailResponse>() {
                                         @Override
                                         public void onSuccess(UserAttendanceDetailResponse details) {
                                             if (details != null) {
                                                 staffMonthlyOrdersLive.postValue(details.getOrdersCreated() != null ? details.getOrdersCreated() : 0);
                                                 staffMonthlyRevenueLive.postValue(details.getRevenueProcessed() != null ? details.getRevenueProcessed() : 0.0);
                                             }
                                             loadLocalData();
                                             loadingLive.postValue(false);
                                         }

                                         @Override
                                         public void onError(Exception e) {
                                             loadLocalData();
                                             loadingLive.postValue(false);
                                         }
                                    });
                        }

                        @Override
                        public void onError(Exception e) {
                            loadLocalData();
                            loadingLive.postValue(false);
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    errorLive.postValue("Lỗi đồng bộ ca làm: " + e.getMessage());
                    loadLocalData();
                    loadingLive.postValue(false);
                }
            });
        }
    }

    private void fetchRevenueReport() {
        isRevenueLoading.postValue(true);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH) + 1; // 1-based

        DateRange.Period period = periodLive.getValue();
        if (period == null) {
            period = DateRange.Period.TODAY;
        }

        if (period == DateRange.Period.ALL) {
            RevenueRepository.getInstance(getApplication())
                .getYearlySummary(currentYear, new RepositoryCallback<RevenueReportResponse>() {
                    @Override
                    public void onSuccess(RevenueReportResponse report) {
                        processYearlySummary(report);
                        isRevenueLoading.postValue(false);
                        loadingLive.postValue(false);
                    }

                    @Override
                    public void onError(Exception e) {
                        errorLive.postValue("Lỗi lấy báo cáo năm: " + e.getMessage());
                        isRevenueLoading.postValue(false);
                        loadingLive.postValue(false);
                    }
                });
        } else {
            RevenueRepository.getInstance(getApplication())
                .getMonthlyRevenue(currentYear, currentMonth, new RepositoryCallback<RevenueReportResponse>() {
                    @Override
                    public void onSuccess(RevenueReportResponse report) {
                        processMonthlyRevenue(report, periodLive.getValue());
                        isRevenueLoading.postValue(false);
                        loadingLive.postValue(false);
                    }

                    @Override
                    public void onError(Exception e) {
                        errorLive.postValue("Lỗi lấy báo cáo doanh thu: " + e.getMessage());
                        isRevenueLoading.postValue(false);
                        loadingLive.postValue(false);
                    }
                });
        }
    }

    private void processMonthlyRevenue(RevenueReportResponse report, DateRange.Period period) {
        if (report == null) return;

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        int todayDay = cal.get(Calendar.DAY_OF_MONTH);

        double todayRevenue = 0.0;
        if (report.getRevenueByDay() != null) {
            for (RevenueReportResponse.DailyRevenue r : report.getRevenueByDay()) {
                if (r.getDay() == todayDay) {
                    todayRevenue = r.getRevenue() != null ? r.getRevenue() : 0.0;
                    break;
                }
            }
        }
        managerTodayRevenueLive.postValue(todayRevenue);

        long now = System.currentTimeMillis();
        long startOfWeekMillis = 0L;

        Calendar calBound = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        calBound.setTimeInMillis(now);
        calBound.set(Calendar.HOUR_OF_DAY, 0);
        calBound.set(Calendar.MINUTE, 0);
        calBound.set(Calendar.SECOND, 0);
        calBound.set(Calendar.MILLISECOND, 0);

        calBound.set(Calendar.DAY_OF_WEEK, calBound.getFirstDayOfWeek());
        startOfWeekMillis = calBound.getTimeInMillis();

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        double totalRev = 0.0;
        int totalOrders = 0;
        List<DailyRevenueRow> chartRows = new ArrayList<>();

        if (report.getRevenueByDay() != null) {
            for (RevenueReportResponse.DailyRevenue r : report.getRevenueByDay()) {
                long dateMillis = 0;
                try {
                    java.util.Date d = sdf.parse(r.getDate());
                    if (d != null) {
                        dateMillis = d.getTime();
                    }
                } catch (Exception ignored) {}

                boolean include = false;
                if (period == DateRange.Period.TODAY) {
                    include = (r.getDay() == todayDay);
                } else if (period == DateRange.Period.WEEK) {
                    include = (dateMillis >= startOfWeekMillis && dateMillis <= now);
                } else if (period == DateRange.Period.MONTH) {
                    include = true;
                }

                if (include) {
                    double rev = r.getRevenue() != null ? r.getRevenue() : 0.0;
                    int orders = r.getOrderCount() != null ? r.getOrderCount() : 0;
                    totalRev += rev;
                    totalOrders += orders;

                    DailyRevenueRow row = new DailyRevenueRow();
                    row.dayLabel = String.valueOf(r.getDay());
                    row.dailyRevenue = rev;
                    chartRows.add(row);
                }
            }
        }

        revenueLive.postValue(totalRev);
        orderCountLive.postValue(totalOrders);
        dailyRevenueLive.postValue(chartRows);

        List<TopProductRow> topRows = new ArrayList<>();
        if (report.getItemsSold() != null) {
            for (RevenueReportResponse.ProductSoldResponse item : report.getItemsSold()) {
                TopProductRow row = new TopProductRow();
                row.productName = item.getProductName();
                row.totalQuantity = item.getQuantity() != null ? item.getQuantity() : 0;
                row.totalRevenue = item.getRevenue() != null ? item.getRevenue() : 0.0;
                topRows.add(row);
            }
        }
        topProductsLive.postValue(topRows);

        List<PaymentMethodStatsRow> methodRows = new ArrayList<>();
        if (report.getRevenueByMethod() != null) {
            for (Map.Entry<String, Double> entry : report.getRevenueByMethod().entrySet()) {
                PaymentMethodStatsRow row = new PaymentMethodStatsRow();
                row.paymentMethod = entry.getKey();
                row.totalRevenue = entry.getValue() != null ? entry.getValue() : 0.0;
                Integer count = report.getOrderCountByMethod() != null ? report.getOrderCountByMethod().get(entry.getKey()) : 0;
                row.orderCount = count != null ? count : 0;
                methodRows.add(row);
            }
        }
        paymentMethodLive.postValue(methodRows);
    }

    private void processYearlySummary(RevenueReportResponse report) {
        if (report == null) return;

        double totalRev = report.getTotalRevenue() != null ? report.getTotalRevenue() : 0.0;
        int totalOrders = report.getOrderCount() != null ? report.getOrderCount() : 0;

        revenueLive.postValue(totalRev);
        orderCountLive.postValue(totalOrders);

        List<DailyRevenueRow> chartRows = new ArrayList<>();
        if (report.getRevenueByMonth() != null) {
            for (RevenueReportResponse.MonthlyRevenue m : report.getRevenueByMonth()) {
                DailyRevenueRow row = new DailyRevenueRow();
                row.dayLabel = "Th " + m.getMonth();
                row.dailyRevenue = m.getRevenue() != null ? m.getRevenue() : 0.0;
                chartRows.add(row);
            }
        }
        dailyRevenueLive.postValue(chartRows);

        List<TopProductRow> topRows = new ArrayList<>();
        if (report.getItemsSold() != null) {
            for (RevenueReportResponse.ProductSoldResponse item : report.getItemsSold()) {
                TopProductRow row = new TopProductRow();
                row.productName = item.getProductName();
                row.totalQuantity = item.getQuantity() != null ? item.getQuantity() : 0;
                row.totalRevenue = item.getRevenue() != null ? item.getRevenue() : 0.0;
                topRows.add(row);
            }
        }
        topProductsLive.postValue(topRows);

        List<PaymentMethodStatsRow> methodRows = new ArrayList<>();
        if (report.getRevenueByMethod() != null) {
            for (Map.Entry<String, Double> entry : report.getRevenueByMethod().entrySet()) {
                PaymentMethodStatsRow row = new PaymentMethodStatsRow();
                row.paymentMethod = entry.getKey();
                row.totalRevenue = entry.getValue() != null ? entry.getValue() : 0.0;
                Integer count = report.getOrderCountByMethod() != null ? report.getOrderCountByMethod().get(entry.getKey()) : 0;
                row.orderCount = count != null ? count : 0;
                methodRows.add(row);
            }
        }
        paymentMethodLive.postValue(methodRows);
    }

    private void loadLocalData() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                long now = System.currentTimeMillis();
                long todayStart = getStartOfDayMillis(now);
                long todayEnd = getEndOfDayMillis(now);

                if (Constants.ROLE_STAFF.equals(currentRole)) {
                    // 1. Today's Shifts for STAFF
                    List<ShiftAssignmentEntity> assignments = appDatabase.shiftAssignmentDao()
                            .getAssignmentsInRange(currentUserId, todayStart, todayEnd);
                    List<TodayShiftItem> shiftsList = new ArrayList<>();
                    for (ShiftAssignmentEntity a : assignments) {
                        ShiftEntity shift = appDatabase.shiftDao().getById(a.getShiftId());
                        if (shift != null && (shift.getStatus().equals(Constants.SHIFT_PUBLISHED)
                                || shift.getStatus().equals(Constants.SHIFT_IN_PROGRESS)
                                || shift.getStatus().equals(Constants.SHIFT_CLOSED))) {
                            AttendanceEntity attendance = appDatabase.attendanceDao()
                                    .getByShiftAndUser(a.getShiftId(), currentUserId);
                            shiftsList.add(new TodayShiftItem(shift, 0, attendance, a.isConfirmed(), a.getAssignmentId()));
                        }
                    }
                    todayShiftsLive.postValue(shiftsList);

                    // 2. Monthly stats range
                    long[] monthRange = getMonthRange(now);
                    List<ShiftAssignmentEntity> monthAssigns = appDatabase.shiftAssignmentDao()
                            .getAssignmentsInRange(currentUserId, monthRange[0], monthRange[1]);
                    int totalShifts = 0;
                    double totalHours = 0.0;
                    for (ShiftAssignmentEntity a : monthAssigns) {
                        ShiftEntity shift = appDatabase.shiftDao().getById(a.getShiftId());
                        if (shift != null && (shift.getStatus().equals(Constants.SHIFT_PUBLISHED)
                                || shift.getStatus().equals(Constants.SHIFT_IN_PROGRESS)
                                || shift.getStatus().equals(Constants.SHIFT_CLOSED))) {
                            totalShifts++;
                            AttendanceEntity att = appDatabase.attendanceDao()
                                    .getByShiftAndUser(a.getShiftId(), currentUserId);
                            if (att != null && att.getCheckInAt() > 0 && att.getCheckOutAt() > 0) {
                                totalHours += (att.getCheckOutAt() - att.getCheckInAt()) / 3600000.0;
                            }
                        }
                    }
                    staffMonthlyShiftsLive.postValue(totalShifts);
                    staffMonthlyHoursLive.postValue(totalHours);

                    if (staffMonthlyOrdersLive.getValue() == null) {
                        staffMonthlyOrdersLive.postValue(0);
                    }
                    if (staffMonthlyRevenueLive.getValue() == null) {
                        staffMonthlyRevenueLive.postValue(0.0);
                    }

                } else {
                    // MANAGER / ADMIN Today's Shifts
                    List<ShiftEntity> shifts = appDatabase.shiftDao()
                            .getByDateRangeSync(todayStart, todayEnd);
                    List<TodayShiftItem> shiftsList = new ArrayList<>();
                    int activeStaff = 0;
                    for (ShiftEntity s : shifts) {
                        if (s.getStatus().equals(Constants.SHIFT_PUBLISHED)
                                || s.getStatus().equals(Constants.SHIFT_IN_PROGRESS)
                                || s.getStatus().equals(Constants.SHIFT_CLOSED)) {
                            int count = appDatabase.shiftAssignmentDao().countByShift(s.getShiftId());
                            shiftsList.add(new TodayShiftItem(s, count, null, false, 0));

                            // Count currently active staff checked-in but not checked-out
                            List<AttendanceEntity> attList = appDatabase.attendanceDao().getByShiftSync(s.getShiftId());
                            if (attList != null) {
                                for (AttendanceEntity att : attList) {
                                    if (att.getCheckInAt() > 0 && att.getCheckOutAt() == 0) {
                                        activeStaff++;
                                    }
                                }
                            }
                        }
                    }
                    todayShiftsLive.postValue(shiftsList);
                    managerTodayShiftsCountLive.postValue(shiftsList.size());
                    managerTodayActiveStaffCountLive.postValue(activeStaff);

                    double todayRev = appDatabase.paymentDao().getRevenueInRangeSync(todayStart, todayEnd);
                    managerTodayRevenueLive.postValue(todayRev);

                    if (Constants.ROLE_ADMIN.equals(currentRole)) {
                        // System Wide Statistics for ADMIN
                        int totalUsers = appDatabase.userDao().countAllUsers();
                        int activeUsers = appDatabase.userDao().countActiveUsers();
                        int countAdmin = appDatabase.userDao().countByRole(Constants.ROLE_ADMIN);
                        int countManager = appDatabase.userDao().countByRole(Constants.ROLE_MANAGER);
                        int countStaff = appDatabase.userDao().countByRole(Constants.ROLE_STAFF);

                        int totalTables = appDatabase.tableDao().countAll();
                        int totalProducts = appDatabase.productDao().countAllActive();

                        adminTotalUsersLive.postValue(totalUsers);
                        adminTotalTablesLive.postValue(totalTables);
                        adminTotalProductsLive.postValue(totalProducts);
                        adminCountAdminLive.postValue(countAdmin);
                        adminCountManagerLive.postValue(countManager);
                        adminCountStaffLive.postValue(countStaff);
                        adminActiveUsersLive.postValue(activeUsers);
                    }
                }
                AppExecutors.getInstance().mainThread().execute(() -> {
                    periodLive.setValue(periodLive.getValue());
                });
            } catch (Exception e) {
                errorLive.postValue("Lỗi truy vấn cơ sở dữ liệu: " + e.getMessage());
            } finally {
                loadingLive.postValue(false);
            }
        });
    }

    // ── Check-in / Check-out actions for STAFF ──

    public void checkIn(int shiftId) {
        loadingLive.setValue(true);
        AttendanceRepository.getInstance(getApplication()).checkIn(shiftId, currentUserId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                loadDashboardData();
            }

            @Override
            public void onError(Exception e) {
                loadingLive.postValue(false);
                errorLive.postValue("Lỗi check-in: " + e.getMessage());
            }
        });
    }

    public void checkOut(int shiftId) {
        loadingLive.setValue(true);
        AttendanceRepository.getInstance(getApplication()).checkOut(shiftId, currentUserId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                loadDashboardData();
            }

            @Override
            public void onError(Exception e) {
                loadingLive.postValue(false);
                errorLive.postValue("Lỗi check-out: " + e.getMessage());
            }
        });
    }

    // ── Helpers ──

    private long getStartOfDayMillis(long timeMs) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        cal.setTimeInMillis(timeMs);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private long getEndOfDayMillis(long timeMs) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        cal.setTimeInMillis(timeMs);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }

    private long[] getMonthRange(long timeMs) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        cal.setTimeInMillis(timeMs);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long end = cal.getTimeInMillis();

        return new long[]{start, end};
    }
}
