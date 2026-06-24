package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.cafe_manager.data.local.dao.PaymentDao;
import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.repository.OrderRepository;
import com.example.cafe_manager.model.OrderWithItems;
import com.example.cafe_manager.util.DateRange;

import java.util.List;

public class HistoryViewModel extends AndroidViewModel {

    private final OrderRepository orderRepository;
    private final PaymentDao paymentDao;

    private final MutableLiveData<DateRange.Period> periodLive =
            new MutableLiveData<>(DateRange.Period.TODAY);

    private final LiveData<List<OrderWithItems>> ordersLive;
    private final LiveData<Double> revenueLive;
    private final LiveData<Integer> orderCountLive;

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        this.orderRepository = OrderRepository.getInstance(application);
        this.paymentDao = AppDatabase.getInstance(application).paymentDao();

        this.ordersLive = Transformations.switchMap(periodLive, period -> {
            long[] range = DateRange.compute(period);
            return orderRepository.getPaidOrdersInRange(range[0], range[1]);
        });

        this.revenueLive = Transformations.switchMap(periodLive, period -> {
            long[] range = DateRange.compute(period);
            return paymentDao.getRevenueInRange(range[0], range[1]);
        });

        this.orderCountLive = Transformations.switchMap(periodLive, period -> {
            long[] range = DateRange.compute(period);
            return paymentDao.countPaymentsInRange(range[0], range[1]);
        });
    }

    public LiveData<List<OrderWithItems>> getOrders() { return ordersLive; }
    public LiveData<Double> getRevenue() { return revenueLive; }
    public LiveData<Integer> getOrderCount() { return orderCountLive; }
    public LiveData<DateRange.Period> getPeriod() { return periodLive; }

    public void selectPeriod(DateRange.Period p) {
        DateRange.Period current = periodLive.getValue();
        if (current == p) return;
        periodLive.setValue(p);
    }
}
