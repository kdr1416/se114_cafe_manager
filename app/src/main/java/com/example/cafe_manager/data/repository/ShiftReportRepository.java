package com.example.cafe_manager.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.dao.OrderDao;
import com.example.cafe_manager.data.local.dao.PaymentDao;
import com.example.cafe_manager.data.local.dao.ShiftCashSessionDao;
import com.example.cafe_manager.data.local.dao.ShiftDao;
import com.example.cafe_manager.data.local.entity.ShiftCashSessionEntity;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.model.PaymentMethodStatsRow;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.List;

public class ShiftReportRepository {

    private final ShiftDao shiftDao;
    private final ShiftCashSessionDao cashSessionDao;
    private final PaymentDao paymentDao;
    private final OrderDao orderDao;
    private final AppExecutors appExecutors;

    public ShiftReportRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.shiftDao = db.shiftDao();
        this.cashSessionDao = db.shiftCashSessionDao();
        this.paymentDao = db.paymentDao();
        this.orderDao = db.orderDao();
        this.appExecutors = AppExecutors.getInstance();
    }

    // ── Cash Session ──

    /** Mở phiên tiền mặt khi mở ca. */
    public void openCashSession(int shiftId, double openingCash, int openedBy,
                                RepositoryCallback<Long> callback) {
        appExecutors.diskIO().execute(() -> {
            try {
                ShiftCashSessionEntity session = new ShiftCashSessionEntity();
                session.setShiftId(shiftId);
                session.setOpeningCash(openingCash);
                session.setOpenedBy(openedBy);
                session.setOpenedAt(System.currentTimeMillis());
                session.setStatus("OPEN");

                long id = cashSessionDao.insert(session);
                appExecutors.mainThread().execute(() -> callback.onSuccess(id));
            } catch (Exception e) {
                appExecutors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    /** Đóng phiên tiền mặt khi đóng ca. */
    public void closeCashSession(int shiftId, double actualCash, int closedBy,
                                 RepositoryCallback<ShiftCashSessionEntity> callback) {
        appExecutors.diskIO().execute(() -> {
            try {
                ShiftCashSessionEntity session = cashSessionDao.getByShift(shiftId);
                if (session == null) {
                    appExecutors.mainThread().execute(() ->
                            callback.onError(new Exception("Không tìm thấy phiên tiền mặt cho ca này.")));
                    return;
                }

                // Tính expectedCash = openingCash + tổng thanh toán tiền mặt trong ca
                double cashPaymentsTotal = paymentDao.getCashTotalByPaidShiftId(shiftId);
                double expectedCash = session.getOpeningCash() + cashPaymentsTotal;
                double difference = actualCash - expectedCash;
                long now = System.currentTimeMillis();

                cashSessionDao.closeSession(
                        session.getSessionId(),
                        actualCash, // closingCash = actualCash
                        actualCash,
                        expectedCash,
                        difference,
                        closedBy,
                        now
                );

                // Đồng thời đóng ca
                shiftDao.closeShift(shiftId, closedBy, now);

                // Trả về session đã cập nhật
                ShiftCashSessionEntity closed = cashSessionDao.getByShift(shiftId);
                appExecutors.mainThread().execute(() -> callback.onSuccess(closed));
            } catch (Exception e) {
                appExecutors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    // ── Report queries ──

    public LiveData<ShiftCashSessionEntity> getCashSessionLive(int shiftId) {
        return cashSessionDao.getByShiftLive(shiftId);
    }

    /** Lấy tổng doanh thu theo ca (background, không LiveData). */
    public void getShiftRevenueSummary(int shiftId, RepositoryCallback<ShiftRevenueSummary> callback) {
        appExecutors.diskIO().execute(() -> {
            try {
                double totalRevenue = paymentDao.getTotalRevenueByPaidShiftId(shiftId);
                double cashRevenue = paymentDao.getCashTotalByPaidShiftId(shiftId);
                int paymentCount = paymentDao.countByPaidShiftId(shiftId);
                int paidOrderCount = orderDao.countPaidByShift(shiftId);
                int unpaidOrderCount = orderDao.countUnpaidByShift(shiftId);
                List<PaymentMethodStatsRow> methodStats =
                        paymentDao.getPaymentMethodStatsByShift(shiftId);

                ShiftRevenueSummary summary = new ShiftRevenueSummary(
                        totalRevenue, cashRevenue, paymentCount,
                        paidOrderCount, unpaidOrderCount, methodStats
                );

                appExecutors.mainThread().execute(() -> callback.onSuccess(summary));
            } catch (Exception e) {
                appExecutors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    /** Lấy số đơn chưa thanh toán (để check trước khi đóng ca). */
    public void getUnpaidOrderCount(int shiftId, RepositoryCallback<Integer> callback) {
        appExecutors.diskIO().execute(() -> {
            try {
                int count = orderDao.countUnpaidByShift(shiftId);
                appExecutors.mainThread().execute(() -> callback.onSuccess(count));
            } catch (Exception e) {
                appExecutors.mainThread().execute(() -> callback.onError(e));
            }
        });
    }

    // ── Inner class cho summary ──

    public static class ShiftRevenueSummary {
        public final double totalRevenue;
        public final double cashRevenue;
        public final int paymentCount;
        public final int paidOrderCount;
        public final int unpaidOrderCount;
        public final List<PaymentMethodStatsRow> methodStats;

        public ShiftRevenueSummary(double totalRevenue, double cashRevenue,
                                   int paymentCount, int paidOrderCount,
                                   int unpaidOrderCount,
                                   List<PaymentMethodStatsRow> methodStats) {
            this.totalRevenue = totalRevenue;
            this.cashRevenue = cashRevenue;
            this.paymentCount = paymentCount;
            this.paidOrderCount = paidOrderCount;
            this.unpaidOrderCount = unpaidOrderCount;
            this.methodStats = methodStats;
        }
    }
}
