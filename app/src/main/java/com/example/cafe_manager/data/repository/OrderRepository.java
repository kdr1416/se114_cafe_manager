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
import com.example.cafe_manager.model.OrderWithItems;

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

    public LiveData<OrderEntity> getActiveOrderByTableLive(int tableId) {
        return orderDao.getActiveByTableIdLive(tableId, Constants.ORDER_CONFIRMED);
    }

    public LiveData<OrderEntity> getOrderLive(int orderId) {
        return orderDao.getByIdLive(orderId);
    }

    public LiveData<List<OrderEntity>> getActiveOrders() {
        return orderDao.getAllByStatus(Constants.ORDER_CONFIRMED);
    }

    public LiveData<List<OrderEntity>> getOrdersByStatus(String status) {
        return orderDao.getAllByStatus(status);
    }

    public LiveData<List<OrderWithItems>> getActiveOrdersWithItems(String status) {
        return orderDao.getOrdersWithItemsByStatus(status);
    }

    public LiveData<List<OrderWithItems>> getPaidOrdersInRange(long fromMs, long toMs) {
        return orderDao.getPaidOrdersInRange(Constants.ORDER_PAID, fromMs, toMs);
    }

    public LiveData<Integer> countPaidInRange(long fromMs, long toMs) {
        return orderDao.countPaidInRange(Constants.ORDER_PAID, fromMs, toMs);
    }

    public LiveData<List<OrderItemEntity>> getItemsByOrderId(int orderId) {
        return orderItemDao.getByOrderId(orderId);
    }

    /**
     * Atomic: thêm items vào order existing + cộng dồn total.
     * Dùng khi nhân viên gọi thêm món cho bàn đang OCCUPIED.
     */
    public void addItemsToOrder(
            int orderId,
            List<CartItem> cartItems,
            RepositoryCallback<Long> callback
    ) {
        appExecutors.diskIO().execute(() -> {
            try {
                double deltaAmount = 0;
                List<OrderItemEntity> newItems = new ArrayList<>();

                for (CartItem c : cartItems) {
                    deltaAmount += c.getSubtotal();

                    OrderItemEntity item = new OrderItemEntity();
                    item.setProductId(c.getProductId());
                    item.setProductNameSnapshot(c.getProductName());
                    item.setQuantity(c.getQuantity());
                    item.setUnitPrice(c.getUnitPrice());
                    item.setSubtotal(c.getSubtotal());
                    item.setNote(c.getNote());

                    newItems.add(item);
                }

                orderTransactionDao.addItemsToOrderAtomic(orderId, newItems, deltaAmount);

                appExecutors.mainThread().execute(() ->
                        callback.onSuccess((long) orderId));
            } catch (Exception e) {
                appExecutors.mainThread().execute(() ->
                        callback.onError(e));
            }
        });
    }

    /**
     * Atomic: cancel order → status=CANCELLED + bàn=EMPTY.
     */
    public void cancelOrder(
            int orderId,
            int tableId,
            RepositoryCallback<Boolean> callback
    ) {
        appExecutors.diskIO().execute(() -> {
            try {
                orderTransactionDao.cancelOrderAtomic(
                        orderId,
                        tableId,
                        Constants.ORDER_CANCELLED,
                        Constants.TABLE_EMPTY
                );
                appExecutors.mainThread().execute(() ->
                        callback.onSuccess(true));
            } catch (Exception e) {
                appExecutors.mainThread().execute(() ->
                        callback.onError(e));
            }
        });
    }

}
