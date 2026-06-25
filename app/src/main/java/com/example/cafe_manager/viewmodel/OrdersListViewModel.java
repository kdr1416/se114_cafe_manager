package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.cafe_manager.data.local.entity.OrderItemEntity;
import com.example.cafe_manager.data.repository.OrderRepository;
import com.example.cafe_manager.model.OrderWithItems;
import com.example.cafe_manager.util.Constants;

import java.util.List;

public class OrdersListViewModel extends AndroidViewModel {

    private final OrderRepository orderRepository;
    private final MutableLiveData<Boolean> refreshTrigger = new MutableLiveData<>(true);
    private final LiveData<List<OrderWithItems>> ordersLive;
    private final LiveData<Integer> activeCountLive;

    public OrdersListViewModel(@NonNull Application application) {
        super(application);

        this.orderRepository = OrderRepository.getInstance(application);

        this.ordersLive = Transformations.switchMap(refreshTrigger, trigger ->
                orderRepository.getActiveOrdersWithItems(Constants.ORDER_CONFIRMED)
        );

        this.activeCountLive = Transformations.map(
                ordersLive,
                list -> list == null ? 0 : list.size()
        );
    }

    public LiveData<List<OrderWithItems>> getOrders() {
        return ordersLive;
    }

    public LiveData<Integer> getActiveCount() {
        return activeCountLive;
    }

    /**
     * Build chuỗi tóm tắt items: "1 Cà phê · 2 Trà".
     * Giới hạn 3 món đầu, các món sau gộp "+ N món khác".
     */
    public static String buildItemsSummary(List<OrderItemEntity> items) {
        if (items == null || items.isEmpty()) {
            return "Chưa có món";
        }

        StringBuilder sb = new StringBuilder();
        int show = Math.min(3, items.size());

        for (int i = 0; i < show; i++) {
            if (i > 0) sb.append(" · ");
            OrderItemEntity it = items.get(i);
            sb.append(it.getQuantity())
                    .append(" ")
                    .append(it.getProductNameSnapshot());
        }

        int remaining = items.size() - show;
        if (remaining > 0) {
            sb.append(" + ").append(remaining).append(" món khác");
        }

        return sb.toString();
    }

    /**
     * Tính tổng tiền từ items (đảm bảo chính xác kể cả khi order.totalAmount chưa cập nhật).
     */
    public static double calculateTotal(List<OrderItemEntity> items) {
        if (items == null) return 0;
        double total = 0;
        for (OrderItemEntity it : items) {
            total += it.getSubtotal();
        }
        return total;
    }

    public void refreshOrders() {
        refreshTrigger.setValue(true);
    }
}
