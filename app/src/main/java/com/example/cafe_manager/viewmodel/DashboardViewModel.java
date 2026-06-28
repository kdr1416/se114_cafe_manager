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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.List;

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
    private final LiveData<Double> revenueLive;
    private final LiveData<Integer> orderCountLive;
    private final LiveData<List<TopProductRow>> topProductsLive;
    private final LiveData<List<PaymentMethodStatsRow>> paymentMethodLive;
    private final LiveData<List<DailyRevenueRow>> dailyRevenueLive;

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

        // Switch maps for Period selection (original report logic)
        this.revenueLive = Transformations.switchMap(periodLive, period -> {
            long[] r = DateRange.compute(period);
            return paymentDao.getRevenueInRange(r[0], r[1]);
        });

        this.orderCountLive = Transformations.switchMap(periodLive, period -> {
            long[] r = DateRange.compute(period);
            return paymentDao.countPaymentsInRange(r[0], r[1]);
        });

        this.topProductsLive = Transformations.switchMap(periodLive, period -> {
            long[] r = DateRange.compute(period);
            return orderItemDao.getTopProducts(
                    Constants.ORDER_PAID, r[0], r[1], TOP_LIMIT);
        });

        this.paymentMethodLive = Transformations.switchMap(periodLive, period -> {
            long[] r = DateRange.compute(period);
            return paymentDao.getPaymentMethodStats(r[0], r[1]);
        });

        this.dailyRevenueLive = Transformations.switchMap(periodLive, period -> {
            long[] r = DateRange.compute(period);
            return paymentDao.getDailyRevenue(r[0], r[1]);
        });
    }

    // ── Getters ──
    public LiveData<DateRange.Period> getPeriod() { return periodLive; }
    public LiveData<Double> getRevenue() { return revenueLive; }
    public LiveData<Integer> getOrderCount() { return orderCountLive; }
    public LiveData<List<TopProductRow>> getTopProducts() { return topProductsLive; }
    public LiveData<List<PaymentMethodStatsRow>> getPaymentMethodStats() { return paymentMethodLive; }
    public LiveData<List<DailyRevenueRow>> getDailyRevenue() { return dailyRevenueLive; }

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
            long now = System.currentTimeMillis();
            if (now - lastSyncTime < SYNC_DEBOUNCE_MS) {
                loadingLive.setValue(false);
                return; // skip if synced recently
            }
            lastSyncTime = now;

            long[] range = DateRange.compute(periodLive.getValue());
            com.example.cafe_manager.data.repository.PaymentRepository.getInstance(getApplication())
                .syncPaymentsInRange(range[0], range[1], new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        ShiftRepository.getInstance(getApplication()).syncMyShiftsAndAssignments(new RepositoryCallback<Void>() {
                            @Override
                            public void onSuccess(Void res) {
                                loadLocalData();
                            }

                            @Override
                            public void onError(Exception e) {
                                loadLocalData();
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        android.util.Log.w("Dashboard", "Sync failed, showing cached data: " + e.getMessage());
                        ShiftRepository.getInstance(getApplication()).syncMyShiftsAndAssignments(new RepositoryCallback<Void>() {
                            @Override
                            public void onSuccess(Void res) {
                                loadLocalData();
                            }

                            @Override
                            public void onError(Exception ex) {
                                loadLocalData();
                            }
                        });
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
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            loadLocalData();
                                        }
                                    });
                        }

                        @Override
                        public void onError(Exception e) {
                            loadLocalData();
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    errorLive.postValue("Lỗi đồng bộ ca làm: " + e.getMessage());
                    loadLocalData();
                }
            });
        }
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
                        if (shift != null && !shift.getStatus().equals("CANCELLED")) {
                            AttendanceEntity attendance = appDatabase.attendanceDao()
                                    .getByShiftAndUser(a.getShiftId(), currentUserId);
                            shiftsList.add(new TodayShiftItem(shift, 0, attendance, a.isConfirmed(), a.getAssignmentId()));
                        }
                    }
                    todayShiftsLive.postValue(shiftsList);

                    // 2. Monthly stats range
                    long[] monthRange = getMonthRange(now);
                    int totalShifts = appDatabase.shiftAssignmentDao()
                            .countAssignmentsInRange(currentUserId, monthRange[0], monthRange[1]);
                    staffMonthlyShiftsLive.postValue(totalShifts);

                    List<ShiftAssignmentEntity> monthAssigns = appDatabase.shiftAssignmentDao()
                            .getAssignmentsInRange(currentUserId, monthRange[0], monthRange[1]);
                    double totalHours = 0.0;
                    for (ShiftAssignmentEntity a : monthAssigns) {
                        AttendanceEntity att = appDatabase.attendanceDao()
                                .getByShiftAndUser(a.getShiftId(), currentUserId);
                        if (att != null && att.getCheckInAt() > 0 && att.getCheckOutAt() > 0) {
                            totalHours += (att.getCheckOutAt() - att.getCheckInAt()) / 3600000.0;
                        }
                    }
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
                        if (!s.getStatus().equals("CANCELLED")) {
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
