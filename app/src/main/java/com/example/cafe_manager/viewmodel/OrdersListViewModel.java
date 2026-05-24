package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.cafe_manager.data.repository.OrderRepository;
import com.example.cafe_manager.model.OrderWithItems;
import com.example.cafe_manager.util.Constants;

import java.util.List;

public class OrdersListViewModel extends AndroidViewModel {

    private final OrderRepository orderRepository;
    private final LiveData<List<OrderWithItems>> ordersLive;

    public OrdersListViewModel(@NonNull Application application) {
        super(application);

        orderRepository = new OrderRepository(application);

        ordersLive = orderRepository
                .getActiveOrdersWithItems(Constants.ORDER_CONFIRMED);
    }

    public LiveData<List<OrderWithItems>> getOrders() {
        return ordersLive;
    }
}
