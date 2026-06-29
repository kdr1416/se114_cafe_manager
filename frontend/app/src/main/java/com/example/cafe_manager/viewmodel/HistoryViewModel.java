package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.cafe_manager.data.repository.OrderRepository;
import com.example.cafe_manager.data.repository.PaymentRepository;
import com.example.cafe_manager.model.OrderWithItems;
import com.example.cafe_manager.util.DateRange;

import java.util.List;

public class HistoryViewModel extends AndroidViewModel {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    private final MutableLiveData<DateRange.Period> periodLive =
            new MutableLiveData<>(DateRange.Period.TODAY);

    private final LiveData<List<OrderWithItems>> ordersLive;
    private final LiveData<Double> revenueLive;
    private final LiveData<Long> orderCountLive;

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        this.orderRepository = OrderRepository.getInstance(application);
        this.paymentRepository = PaymentRepository.getInstance(application);

        this.ordersLive = Transformations.switchMap(periodLive, period -> {
            long[] range = DateRange.compute(period);
            return orderRepository.getPaidOrdersInRange(range[0], range[1]);
        });

        this.revenueLive = Transformations.switchMap(periodLive, period -> {
            long[] range = DateRange.compute(period);
            return paymentRepository.getRevenueInRangeLive(range[0], range[1]);
        });

        this.orderCountLive = Transformations.switchMap(periodLive, period -> {
            long[] range = DateRange.compute(period);
            return paymentRepository.countPaymentsInRangeLive(range[0], range[1]);
        });
    }

    public LiveData<List<OrderWithItems>> getOrders() { return ordersLive; }
    public LiveData<Double> getRevenue() { return revenueLive; }
    public LiveData<Long> getOrderCount() { return orderCountLive; }
    public LiveData<DateRange.Period> getPeriod() { return periodLive; }

    public void selectPeriod(DateRange.Period p) {
        DateRange.Period current = periodLive.getValue();
        if (current == p) return;
        periodLive.setValue(p);
    }
}
