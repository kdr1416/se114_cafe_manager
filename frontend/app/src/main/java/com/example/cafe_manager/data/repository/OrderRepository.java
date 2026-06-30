package com.example.cafe_manager.data.repository;

import android.content.Context;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.OrderEntity;
import com.example.cafe_manager.data.local.entity.OrderItemEntity;
import com.example.cafe_manager.data.remote.ApiClient;
import com.example.cafe_manager.data.remote.OrderApiService;
import com.example.cafe_manager.data.remote.OrderDetailResponse;
import com.example.cafe_manager.data.remote.OrderItemRequest;
import com.example.cafe_manager.data.remote.OrderRequest;
import com.example.cafe_manager.data.remote.NetworkException;
import com.example.cafe_manager.model.CartItem;
import com.example.cafe_manager.model.OrderWithItems;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository quản lý hóa đơn (orders) và chi tiết hóa đơn (order items),
 * kết nối trực tiếp đến Backend qua Retrofit APIs.
 */
public class OrderRepository {

    private static OrderRepository instance;

    private final Context context;
    private final OrderApiService orderApiService;
    private final AppExecutors appExecutors;

    public static synchronized OrderRepository getInstance(Context context) {
        if (instance == null) {
            instance = new OrderRepository(context);
        }
        return instance;
    }

    private OrderRepository(Context context) {
        this.context = context.getApplicationContext();
        this.orderApiService = ApiClient.getInstance(context).getService(OrderApiService.class);
        this.appExecutors = AppExecutors.getInstance();
    }

    public void confirmOrder(
            int tableId,
            List<CartItem> cartItems,
            String note,
            int createdByUserId,
            int createdShiftId,
            RepositoryCallback<Long> callback
    ) {
        if (createdShiftId <= 0) {
            appExecutors.mainThread().execute(() ->
                    callback.onError(new Exception("Chưa có ca bán hàng đang mở. Vui lòng yêu cầu quản lý mở ca."))
            );
            return;
        }

        appExecutors.diskIO().execute(() -> {
            try {
                // 1. Convert CartItems to OrderItemRequests
                List<OrderItemRequest> itemRequests = new ArrayList<>();
                for (CartItem cartItem : cartItems) {
                    itemRequests.add(new OrderItemRequest(
                            cartItem.getProductId(),
                            cartItem.getQuantity(),
                            cartItem.getNote()
                    ));
                }

                // 2. Send single bulk OrderRequest to backend
                OrderRequest request = new OrderRequest(tableId, note, itemRequests);
                Response<OrderEntity> createResp = orderApiService.createOrder(request).execute();
                if (!createResp.isSuccessful() || createResp.body() == null) {
                    throw parseError(createResp);
                }
                OrderEntity order = createResp.body();
                int orderId = order.getOrderId();

                appExecutors.mainThread().execute(() -> {
                    TableRepository.getInstance(context).refreshAllTables();
                    callback.onSuccess((long) orderId);
                });

            } catch (Exception e) {
                appExecutors.mainThread().execute(() ->
                        callback.onError(e)
                );
            }
        });
    }

    public OrderEntity getActiveOrderByTable(int tableId) {
        try {
            // Lấy tất cả hóa đơn đang phục vụ (CONFIRMED) từ backend và lọc theo tableId
            Response<List<OrderEntity>> resp = orderApiService.getAllOrders(Constants.ORDER_CONFIRMED).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                for (OrderEntity o : resp.body()) {
                    if (o.getTableId() == tableId) {
                        return o;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public LiveData<OrderEntity> getActiveOrderByTableLive(int tableId) {
        MutableLiveData<OrderEntity> liveData = new MutableLiveData<>();
        appExecutors.diskIO().execute(() -> {
            try {
                Response<List<OrderEntity>> resp = orderApiService.getAllOrders(Constants.ORDER_CONFIRMED).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    for (OrderEntity o : resp.body()) {
                        if (o.getTableId() == tableId) {
                            liveData.postValue(o);
                            return;
                        }
                    }
                }
                liveData.postValue(null);
            } catch (Exception e) {
                liveData.postValue(null);
            }
        });
        return liveData;
    }

    public LiveData<OrderEntity> getOrderLive(int orderId) {
        MutableLiveData<OrderEntity> liveData = new MutableLiveData<>();
        orderApiService.getOrderDetail(orderId).enqueue(new Callback<OrderDetailResponse>() {
            @Override
            public void onResponse(Call<OrderDetailResponse> call, Response<OrderDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    OrderEntity order = response.body().getOrder();
                    liveData.postValue(order);
                } else {
                    liveData.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<OrderDetailResponse> call, Throwable t) {
                liveData.postValue(null);
            }
        });
        return liveData;
    }

    public LiveData<List<OrderEntity>> getActiveOrders() {
        MutableLiveData<List<OrderEntity>> liveData = new MutableLiveData<>();
        orderApiService.getAllOrders(Constants.ORDER_CONFIRMED).enqueue(new Callback<List<OrderEntity>>() {
            @Override
            public void onResponse(Call<List<OrderEntity>> call, Response<List<OrderEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    liveData.postValue(response.body());
                } else {
                    liveData.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<OrderEntity>> call, Throwable t) {
                liveData.postValue(null);
            }
        });
        return liveData;
    }

    public LiveData<List<OrderEntity>> getOrdersByStatus(String status) {
        MutableLiveData<List<OrderEntity>> liveData = new MutableLiveData<>();
        orderApiService.getAllOrders(status).enqueue(new Callback<List<OrderEntity>>() {
            @Override
            public void onResponse(Call<List<OrderEntity>> call, Response<List<OrderEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    liveData.postValue(response.body());
                } else {
                    liveData.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<OrderEntity>> call, Throwable t) {
                liveData.postValue(null);
            }
        });
        return liveData;
    }

    public LiveData<List<OrderWithItems>> getActiveOrdersWithItems(String status) {
        MutableLiveData<List<OrderWithItems>> liveData = new MutableLiveData<>();
        appExecutors.diskIO().execute(() -> {
            try {
                Response<List<OrderEntity>> resp = orderApiService.getAllOrders(status).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    List<OrderWithItems> result = new ArrayList<>();
                    for (OrderEntity order : resp.body()) {
                        Response<OrderDetailResponse> detailResp = orderApiService.getOrderDetail(order.getOrderId()).execute();
                        if (detailResp.isSuccessful() && detailResp.body() != null) {
                            OrderWithItems owi = new OrderWithItems();
                            owi.setOrder(order);
                            owi.setItems(detailResp.body().getItems());
                            owi.setTableName("Bàn " + order.getTableId());
                            result.add(owi);
                        }
                    }
                    liveData.postValue(result);
                } else {
                    liveData.postValue(new ArrayList<>());
                }
            } catch (Exception e) {
                liveData.postValue(new ArrayList<>());
            }
        });
        return liveData;
    }

    public LiveData<List<OrderWithItems>> getPaidOrdersInRange(long fromMs, long toMs) {
        MutableLiveData<List<OrderWithItems>> liveData = new MutableLiveData<>();
        appExecutors.diskIO().execute(() -> {
            try {
                Response<List<OrderDetailResponse>> resp = orderApiService.getPaidOrdersHistory(fromMs, toMs).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    List<OrderWithItems> result = new ArrayList<>();
                    for (OrderDetailResponse detail : resp.body()) {
                        if (detail.getOrder() != null) {
                            OrderWithItems owi = new OrderWithItems();
                            owi.setOrder(detail.getOrder());
                            owi.setItems(detail.getItems());
                            owi.setTableName("Bàn " + detail.getOrder().getTableId());
                            result.add(owi);
                        }
                    }
                    liveData.postValue(result);
                } else {
                    liveData.postValue(new ArrayList<>());
                }
            } catch (Exception e) {
                e.printStackTrace();
                liveData.postValue(new ArrayList<>());
            }
        });
        return liveData;
    }

    public LiveData<Integer> countPaidInRange(long fromMs, long toMs) {
        MutableLiveData<Integer> liveData = new MutableLiveData<>();
        appExecutors.diskIO().execute(() -> {
            try {
                Response<List<OrderEntity>> resp = orderApiService.getAllOrders(Constants.ORDER_PAID).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    int count = 0;
                    for (OrderEntity order : resp.body()) {
                        long paidAt = order.getPaidAt();
                        if (paidAt >= fromMs && paidAt <= toMs) {
                            count++;
                        }
                    }
                    liveData.postValue(count);
                } else {
                    liveData.postValue(0);
                }
            } catch (Exception e) {
                liveData.postValue(0);
            }
        });
        return liveData;
    }

    public LiveData<List<OrderItemEntity>> getItemsByOrderId(int orderId) {
        MutableLiveData<List<OrderItemEntity>> liveData = new MutableLiveData<>();
        orderApiService.getOrderDetail(orderId).enqueue(new Callback<OrderDetailResponse>() {
            @Override
            public void onResponse(Call<OrderDetailResponse> call, Response<OrderDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    liveData.postValue(response.body().getItems());
                } else {
                    liveData.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<OrderDetailResponse> call, Throwable t) {
                liveData.postValue(null);
            }
        });
        return liveData;
    }

    public void addItemsToOrder(
            int orderId,
            List<CartItem> cartItems,
            RepositoryCallback<Long> callback
    ) {
        appExecutors.diskIO().execute(() -> {
            try {
                List<OrderItemRequest> itemRequests = new ArrayList<>();
                for (CartItem cartItem : cartItems) {
                    itemRequests.add(new OrderItemRequest(
                            cartItem.getProductId(),
                            cartItem.getQuantity(),
                            cartItem.getNote()
                    ));
                }
                Response<OrderDetailResponse> addResp = orderApiService.addItemsBulk(orderId, itemRequests).execute();
                if (!addResp.isSuccessful()) {
                    throw parseError(addResp);
                }
                appExecutors.mainThread().execute(() ->
                        callback.onSuccess((long) orderId)
                );
            } catch (Exception e) {
                appExecutors.mainThread().execute(() ->
                        callback.onError(e)
                );
            }
        });
    }

    public void cancelOrder(
            int orderId,
            int tableId,
            RepositoryCallback<Boolean> callback
    ) {
        orderApiService.cancelOrder(orderId).enqueue(new Callback<OrderEntity>() {
            @Override
            public void onResponse(Call<OrderEntity> call, Response<OrderEntity> response) {
                if (response.isSuccessful()) {
                    appExecutors.mainThread().execute(() -> {
                        TableRepository.getInstance(context).refreshAllTables();
                        callback.onSuccess(true);
                    });
                } else {
                    appExecutors.mainThread().execute(() -> callback.onError(parseError(response)));
                }
            }

            @Override
            public void onFailure(Call<OrderEntity> call, Throwable t) {
                appExecutors.mainThread().execute(() ->
                        callback.onError(new NetworkException("Không có kết nối mạng", t))
                );
            }
        });
    }

    public void serveOrder(
            int orderId,
            RepositoryCallback<Boolean> callback
    ) {
        orderApiService.serveOrder(orderId).enqueue(new Callback<OrderEntity>() {
            @Override
            public void onResponse(Call<OrderEntity> call, Response<OrderEntity> response) {
                if (response.isSuccessful()) {
                    appExecutors.mainThread().execute(() -> {
                        callback.onSuccess(true);
                    });
                } else {
                    appExecutors.mainThread().execute(() -> callback.onError(parseError(response)));
                }
            }

            @Override
            public void onFailure(Call<OrderEntity> call, Throwable t) {
                appExecutors.mainThread().execute(() ->
                        callback.onError(new NetworkException("Không có kết nối mạng", t))
                );
            }
        });
    }

    private Exception parseError(Response<?> response) {
        if (response == null) return new NetworkException("Không có kết nối mạng");
        switch (response.code()) {
            case 401: return new Exception("Phiên đăng nhập hết hạn (401)");
            case 403: return new Exception("Không có quyền thực hiện (403)");
            case 404: return new Exception("Không tìm thấy dữ liệu (404)");
            case 500: return new Exception("Lỗi máy chủ (500)");
            default:  return new Exception("Lỗi hệ thống: " + response.code());
        }
    }

    private void showError(final Exception e) {
        appExecutors.mainThread().execute(() ->
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }
}
