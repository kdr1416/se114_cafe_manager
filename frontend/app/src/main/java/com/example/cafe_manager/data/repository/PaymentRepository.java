package com.example.cafe_manager.data.repository;

import android.content.Context;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.List;

import com.example.cafe_manager.data.local.entity.PaymentEntity;
import com.example.cafe_manager.data.remote.ApiClient;
import com.example.cafe_manager.data.remote.PaymentApiService;
import com.example.cafe_manager.data.remote.PaymentRequest;
import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.remote.PaymentResponse;
import com.example.cafe_manager.data.remote.NetworkException;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.RepositoryCallback;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository quản lý thông tin hóa đơn thanh toán (payments),
 * kết nối trực tiếp đến Backend qua Retrofit APIs.
 */
public class PaymentRepository {

    private static PaymentRepository instance;

    private final Context context;
    private final PaymentApiService paymentApiService;
    private final AppExecutors appExecutors;

    public static synchronized PaymentRepository getInstance(Context context) {
        if (instance == null) {
            instance = new PaymentRepository(context);
        }
        return instance;
    }

    private PaymentRepository(Context context) {
        this.context = context.getApplicationContext();
        this.paymentApiService = ApiClient.getInstance(context).getService(PaymentApiService.class);
        this.appExecutors = AppExecutors.getInstance();
    }

    /**
     * Thực hiện thanh toán hóa đơn.
     */
    public void payOrder(
            int orderId,
            int tableId,
            String paymentMethod,
            double subtotal,
            double discount,
            double finalAmount,
            int cashierUserId,
            int paidShiftId,
            RepositoryCallback<Boolean> callback
    ) {
        if (paidShiftId <= 0) {
            appExecutors.mainThread().execute(() ->
                    callback.onError(new Exception("Chưa có ca bán hàng đang mở. Không thể thanh toán."))
            );
            return;
        }

        appExecutors.diskIO().execute(() -> {
            try {
                // Tạo PaymentRequest gửi lên backend.
                // Chúng ta truyền amountReceived bằng finalAmount, truyền discountAmount bằng discount.
                PaymentRequest request = new PaymentRequest(
                        orderId,
                        paymentMethod,
                        "",
                        finalAmount,
                        discount
                );

                Response<PaymentResponse> response = paymentApiService.processPayment(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    PaymentResponse res = response.body();
                    PaymentEntity localPayment = new PaymentEntity(
                            res.getOrderId() != null ? res.getOrderId() : orderId,
                            res.getPaymentMethod(),
                            res.getSubtotal(),
                            res.getDiscountAmount(),
                            res.getFinalAmount(),
                            res.getPaidAt() != null ? res.getPaidAt() : System.currentTimeMillis(),
                            "PAID"
                    );
                    localPayment.setPaymentId(res.getPaymentId());
                    localPayment.setCashierUserId(res.getCashierUserId() != null ? res.getCashierUserId() : cashierUserId);
                    localPayment.setPaidShiftId(paidShiftId);
                    AppDatabase.getInstance(context).paymentDao().insert(localPayment);

                    appExecutors.mainThread().execute(() -> {
                        TableRepository.getInstance(context).refreshAllTables();
                        callback.onSuccess(true);
                    });
                } else {
                    appExecutors.mainThread().execute(() ->
                            callback.onError(parseError(response))
                    );
                }
            } catch (Exception e) {
                appExecutors.mainThread().execute(() ->
                        callback.onError(e)
                );
            }
        });
    }

    public LiveData<PaymentEntity> getPaymentByOrder(int orderId) {
        MutableLiveData<PaymentEntity> liveData = new MutableLiveData<>();
        paymentApiService.getPaymentByOrderId(orderId).enqueue(new Callback<PaymentResponse>() {
            @Override
            public void onResponse(Call<PaymentResponse> call, Response<PaymentResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PaymentResponse res = response.body();
                    PaymentEntity entity = new PaymentEntity(
                            res.getOrderId() != null ? res.getOrderId() : orderId,
                            res.getPaymentMethod(),
                            res.getSubtotal(),
                            res.getDiscountAmount(),
                            res.getFinalAmount(),
                            res.getPaidAt() != null ? res.getPaidAt() : 0L,
                            "PAID"
                    );
                    entity.setPaymentId(res.getPaymentId());
                    entity.setCashierUserId(res.getCashierUserId() != null ? res.getCashierUserId() : 0);
                    entity.setPaidShiftId(res.getPaidShiftId() != null ? res.getPaidShiftId() : 0);
                    entity.setCashierFullName(res.getCashierFullName());
                    liveData.postValue(entity);
                } else {
                    liveData.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<PaymentResponse> call, Throwable t) {
                liveData.postValue(null);
            }
        });
        return liveData;
    }

    public void getRevenueInRange(long start, long end, RepositoryCallback<Double> callback) {
        paymentApiService.getRevenueInRange(start, end).enqueue(new Callback<Double>() {
            @Override
            public void onResponse(Call<Double> call, Response<Double> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) {
                        callback.onSuccess(response.body());
                    }
                } else {
                    Exception e = parseError(response);
                    showError(e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
            }

            @Override
            public void onFailure(Call<Double> call, Throwable t) {
                Exception e = new NetworkException("Không có kết nối mạng", t);
                showError(e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    public void countPaymentsInRange(long start, long end, RepositoryCallback<Long> callback) {
        paymentApiService.countPaymentsInRange(start, end).enqueue(new Callback<Long>() {
            @Override
            public void onResponse(Call<Long> call, Response<Long> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (callback != null) {
                        callback.onSuccess(response.body());
                    }
                } else {
                    Exception e = parseError(response);
                    showError(e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
            }

            @Override
            public void onFailure(Call<Long> call, Throwable t) {
                Exception e = new NetworkException("Không có kết nối mạng", t);
                showError(e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    public LiveData<Double> getRevenueInRangeLive(long start, long end) {
        MutableLiveData<Double> liveData = new MutableLiveData<>(0.0);
        getRevenueInRange(start, end, new RepositoryCallback<Double>() {
            @Override
            public void onSuccess(Double result) {
                liveData.postValue(result);
            }

            @Override
            public void onError(Exception e) {
                liveData.postValue(0.0);
            }
        });
        return liveData;
    }

    public LiveData<Long> countPaymentsInRangeLive(long start, long end) {
        MutableLiveData<Long> liveData = new MutableLiveData<>(0L);
        countPaymentsInRange(start, end, new RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long result) {
                liveData.postValue(result);
            }

            @Override
            public void onError(Exception e) {
                liveData.postValue(0L);
            }
        });
        return liveData;
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
