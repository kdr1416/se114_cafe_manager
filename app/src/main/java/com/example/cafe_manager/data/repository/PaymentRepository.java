package com.example.cafe_manager.data.repository;

import android.content.Context;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.PaymentEntity;
import com.example.cafe_manager.data.remote.ApiClient;
import com.example.cafe_manager.data.remote.PaymentApiService;
import com.example.cafe_manager.data.remote.PaymentRequest;
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
                if (response.isSuccessful()) {
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
        paymentApiService.getPaymentByOrderId(orderId).enqueue(new Callback<PaymentEntity>() {
            @Override
            public void onResponse(Call<PaymentEntity> call, Response<PaymentEntity> response) {
                if (response.isSuccessful() && response.body() != null) {
                    liveData.postValue(response.body());
                } else {
                    liveData.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<PaymentEntity> call, Throwable t) {
                liveData.postValue(null);
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
