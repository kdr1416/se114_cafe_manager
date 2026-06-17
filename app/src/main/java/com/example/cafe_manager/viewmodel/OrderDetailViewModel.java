package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.cafe_manager.data.local.entity.OrderEntity;
import com.example.cafe_manager.data.local.entity.OrderItemEntity;
import com.example.cafe_manager.data.local.entity.PaymentEntity;
import com.example.cafe_manager.data.repository.OrderRepository;
import com.example.cafe_manager.data.repository.PaymentRepository;

import java.util.List;

/**
 * Read-only ViewModel: load chi tiết một order (order + items + payment).
 * Dùng bởi PaymentActivity (chưa có payment) và InvoiceActivity (đã có payment).
 */
public class OrderDetailViewModel extends AndroidViewModel {

    private final MutableLiveData<Integer> orderIdLive = new MutableLiveData<>();

    private final LiveData<OrderEntity> orderLive;
    private final LiveData<List<OrderItemEntity>> itemsLive;
    private final LiveData<PaymentEntity> paymentLive;

    public OrderDetailViewModel(@NonNull Application application) {
        super(application);

        OrderRepository orderRepository = new OrderRepository(application);
        PaymentRepository paymentRepository = new PaymentRepository(application);

        this.orderLive = Transformations.switchMap(
                orderIdLive,
                id -> (id == null || id <= 0)
                        ? new MutableLiveData<>()
                        : orderRepository.getOrderLive(id)
        );

        this.itemsLive = Transformations.switchMap(
                orderIdLive,
                id -> (id == null || id <= 0)
                        ? new MutableLiveData<>()
                        : orderRepository.getItemsByOrderId(id)
        );

        this.paymentLive = Transformations.switchMap(
                orderIdLive,
                id -> (id == null || id <= 0)
                        ? new MutableLiveData<>()
                        : paymentRepository.getPaymentByOrder(id)
        );
    }

    public void setOrderId(int orderId) {
        Integer current = orderIdLive.getValue();
        if (current != null && current == orderId) {
            return;
        }
        orderIdLive.setValue(orderId);
    }

    public LiveData<OrderEntity> getOrder() {
        return orderLive;
    }

    public LiveData<List<OrderItemEntity>> getItems() {
        return itemsLive;
    }

    public LiveData<PaymentEntity> getPayment() {
        return paymentLive;
    }
}
