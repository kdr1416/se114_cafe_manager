package com.example.cafe_manager.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.dao.OrderDao;
import com.example.cafe_manager.data.local.dao.OrderItemDao;
import com.example.cafe_manager.data.local.dao.OrderTransactionDao;
import com.example.cafe_manager.data.local.entity.OrderEntity;
import com.example.cafe_manager.data.local.entity.OrderItemEntity;
import com.example.cafe_manager.model.CartItem;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.RepositoryCallback;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderRepository {

    private final OrderDao orderDao;
    private final OrderItemDao orderItemDao;
    private final OrderTransactionDao orderTransactionDao;
    private final AppExecutors appExecutors;

    public OrderRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.orderDao = db.orderDao();
        this.orderItemDao = db.orderItemDao();
        this.orderTransactionDao = db.orderTransactionDao();
        this.appExecutors = AppExecutors.getInstance();
    }

    public void confirmOrder(
            int tableId,
            List<CartItem> cartItems,
            String note,
            RepositoryCallback<Long> callback
    ) {
        appExecutors.diskIO().execute(() -> {
            try {
                double totalAmount = 0;
                List<OrderItemEntity> orderItems = new ArrayList<>();

                for (CartItem cartItem : cartItems) {
                    totalAmount += cartItem.getSubtotal();

                    OrderItemEntity item = new OrderItemEntity();
                    item.setProductId(cartItem.getProductId());
                    item.setProductNameSnapshot(cartItem.getProductName());
                    item.setQuantity(cartItem.getQuantity());
                    item.setUnitPrice(cartItem.getUnitPrice());
                    item.setSubtotal(cartItem.getSubtotal());
                    item.setNote(cartItem.getNote());

                    orderItems.add(item);
                }

                OrderEntity order = new OrderEntity();
                order.setTableId(tableId);
                order.setOrderCode("ORD" + System.currentTimeMillis());
                order.setStatus(Constants.ORDER_CONFIRMED);
                order.setTotalAmount(totalAmount);
                order.setNote(note);
                order.setCreatedAt(System.currentTimeMillis());

                long orderId = orderTransactionDao.confirmOrderAtomic(
                        order,
                        orderItems,
                        tableId,
                        Constants.TABLE_OCCUPIED
                );

                appExecutors.mainThread().execute(() ->
                        callback.onSuccess(orderId)
                );

            } catch (Exception e) {
                appExecutors.mainThread().execute(() ->
                        callback.onError(e)
                );
            }
        });
    }

    public OrderEntity getActiveOrderByTable(int tableId) {
        return orderDao.getActiveByTableId(tableId, Constants.ORDER_CONFIRMED);
    }

    public LiveData<List<OrderItemEntity>> getItemsByOrderId(int orderId) {
        return orderItemDao.getByOrderId(orderId);
    }

}