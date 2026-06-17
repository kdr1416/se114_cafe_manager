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
import com.example.cafe_manager.model.DailyRevenueRow;
import com.example.cafe_manager.model.PaymentMethodStatsRow;
import com.example.cafe_manager.model.TopProductRow;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.DateRange;

import java.util.List;

public class DashboardViewModel extends AndroidViewModel {

    private static final int TOP_LIMIT = 5;

    private final PaymentDao paymentDao;
    private final OrderItemDao orderItemDao;

    private final MutableLiveData<DateRange.Period> periodLive =
            new MutableLiveData<>(DateRange.Period.TODAY);

    private final LiveData<Double> revenueLive;
    private final LiveData<Integer> orderCountLive;
    private final LiveData<List<TopProductRow>> topProductsLive;
    private final LiveData<List<PaymentMethodStatsRow>> paymentMethodLive;
    private final LiveData<List<DailyRevenueRow>> dailyRevenueLive;

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        this.paymentDao = db.paymentDao();
        this.orderItemDao = db.orderItemDao();

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

    public LiveData<DateRange.Period> getPeriod() { return periodLive; }
    public LiveData<Double> getRevenue() { return revenueLive; }
    public LiveData<Integer> getOrderCount() { return orderCountLive; }
    public LiveData<List<TopProductRow>> getTopProducts() { return topProductsLive; }
    public LiveData<List<PaymentMethodStatsRow>> getPaymentMethodStats() {
        return paymentMethodLive;
    }
    public LiveData<List<DailyRevenueRow>> getDailyRevenue() { return dailyRevenueLive; }

    public void selectPeriod(DateRange.Period p) {
        DateRange.Period current = periodLive.getValue();
        if (current == p) return;
        periodLive.setValue(p);
    }
}
