package com.example.cafe_manager.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.ShiftCashSessionEntity;
import com.example.cafe_manager.data.remote.ApiClient;
import com.example.cafe_manager.data.remote.CloseShiftRequest;
import com.example.cafe_manager.data.remote.ShiftApiService;
import com.example.cafe_manager.data.remote.ShiftReportResponse;
import com.example.cafe_manager.data.remote.DailyShiftReportResponse;
import com.example.cafe_manager.data.remote.ShiftResponse;
import com.example.cafe_manager.model.PaymentMethodStatsRow;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShiftReportRepository {

    private static volatile ShiftReportRepository instance;
    private final ShiftApiService apiService;
    private final AppExecutors appExecutors;

    private ShiftReportRepository(Context context) {
        this.apiService = ApiClient.getInstance(context).getService(ShiftApiService.class);
        this.appExecutors = AppExecutors.getInstance();
    }

    public static ShiftReportRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (ShiftReportRepository.class) {
                if (instance == null) {
                    instance = new ShiftReportRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // ── Cash Session ──

    /** Mở phiên tiền mặt khi mở ca. */
    public void openCashSession(int shiftId, double openingCash, int openedBy,
                                RepositoryCallback<Long> callback) {
        appExecutors.mainThread().execute(() -> callback.onSuccess(0L));
    }

    /** Đóng phiên tiền mặt khi đóng ca atomically. */
    public void closeCashSession(int shiftId, double actualCash, int closedBy,
                                 RepositoryCallback<ShiftCashSessionEntity> callback) {
        CloseShiftRequest request = new CloseShiftRequest();
        request.setClosingCash(actualCash);
        apiService.closeShift(shiftId, request).enqueue(new Callback<ShiftResponse>() {
            @Override
            public void onResponse(Call<ShiftResponse> call, Response<ShiftResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ShiftResponse shift = response.body();
                    ShiftCashSessionEntity session = new ShiftCashSessionEntity();
                    session.setShiftId(shiftId);
                    session.setOpeningCash(shift.getOpeningCash() != null ? shift.getOpeningCash() : 0.0);
                    session.setClosingCash(shift.getClosingCash() != null ? shift.getClosingCash() : 0.0);
                    session.setExpectedCash(shift.getClosingCash() != null ? shift.getClosingCash() : 0.0);
                    session.setActualCash(shift.getClosingCash() != null ? shift.getClosingCash() : 0.0);
                    session.setCashDifference(0.0);
                    session.setStatus(shift.getStatus());
                    session.setClosedBy(closedBy);
                    session.setClosedAt(System.currentTimeMillis());
                    callback.onSuccess(session);
                } else {
                    String errorMsg = "Lỗi khi đóng ca";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    callback.onError(new Exception(errorMsg));
                }
            }

            @Override
            public void onFailure(Call<ShiftResponse> call, Throwable t) {
                callback.onError(new Exception("Lỗi kết nối khi đóng ca: " + t.getMessage(), t));
            }
        });
    }

    // ── Report queries ──

    public LiveData<ShiftCashSessionEntity> getCashSessionLive(int shiftId) {
        MutableLiveData<ShiftCashSessionEntity> liveData = new MutableLiveData<>();
        apiService.getShiftReport(shiftId).enqueue(new Callback<ShiftReportResponse>() {
            @Override
            public void onResponse(Call<ShiftReportResponse> call, Response<ShiftReportResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ShiftReportResponse report = response.body();
                    ShiftCashSessionEntity session = new ShiftCashSessionEntity();
                    session.setShiftId(shiftId);
                    session.setOpeningCash(report.getOpeningCash() != null ? report.getOpeningCash() : 0.0);
                    session.setClosingCash(report.getClosingCash() != null ? report.getClosingCash() : 0.0);
                    session.setExpectedCash(report.getExpectedCash() != null ? report.getExpectedCash() : 0.0);
                    session.setActualCash(report.getClosingCash() != null ? report.getClosingCash() : 0.0);
                    session.setCashDifference(report.getCashDifference() != null ? report.getCashDifference() : 0.0);
                    session.setStatus(report.getStatus());
                    liveData.setValue(session);
                } else {
                    liveData.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<ShiftReportResponse> call, Throwable t) {
                liveData.setValue(null);
            }
        });
        return liveData;
    }

    public void getShiftReportDetails(int shiftId, RepositoryCallback<ShiftReportResponse> callback) {
        apiService.getShiftReport(shiftId).enqueue(new Callback<ShiftReportResponse>() {
            @Override
            public void onResponse(Call<ShiftReportResponse> call, Response<ShiftReportResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Lỗi khi tải chi tiết báo cáo ca: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ShiftReportResponse> call, Throwable t) {
                callback.onError(new Exception("Lỗi kết nối khi tải chi tiết báo cáo ca: " + t.getMessage(), t));
            }
        });
    }

    public void getDailyShiftReport(String date, RepositoryCallback<DailyShiftReportResponse> callback) {
        apiService.getDailyShiftReport(date).enqueue(new Callback<DailyShiftReportResponse>() {
            @Override
            public void onResponse(Call<DailyShiftReportResponse> call, Response<DailyShiftReportResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Lỗi khi tải báo cáo ngày: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<DailyShiftReportResponse> call, Throwable t) {
                callback.onError(new Exception("Lỗi kết nối khi tải báo cáo ngày: " + t.getMessage(), t));
            }
        });
    }

    /** Lấy tổng doanh thu theo ca (background, không LiveData). */
    public void getShiftRevenueSummary(int shiftId, RepositoryCallback<ShiftRevenueSummary> callback) {
        apiService.getShiftReport(shiftId).enqueue(new Callback<ShiftReportResponse>() {
            @Override
            public void onResponse(Call<ShiftReportResponse> call, Response<ShiftReportResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ShiftReportResponse report = response.body();

                    List<PaymentMethodStatsRow> methodStats = new ArrayList<>();
                    if (report.getPaymentMethodStats() != null) {
                        for (ShiftReportResponse.PaymentMethodStatsResponse stats : report.getPaymentMethodStats()) {
                            PaymentMethodStatsRow row = new PaymentMethodStatsRow();
                            row.paymentMethod = stats.getPaymentMethod();
                            row.orderCount = stats.getOrderCount() != null ? stats.getOrderCount() : 0;
                            row.totalRevenue = stats.getTotalRevenue() != null ? stats.getTotalRevenue() : 0.0;
                            methodStats.add(row);
                        }
                    }

                    int paidOrderCount = report.getPaymentCount() != null ? report.getPaymentCount() : 0;
                    int unpaidOrderCount = report.getUnpaidOrders() != null ? report.getUnpaidOrders() : 0;

                    ShiftRevenueSummary summary = new ShiftRevenueSummary(
                            report.getTotalRevenue(),
                            report.getCashRevenue() != null ? report.getCashRevenue() : 0.0,
                            report.getPaymentCount() != null ? report.getPaymentCount() : 0,
                            paidOrderCount,
                            unpaidOrderCount,
                            methodStats
                    );
                    callback.onSuccess(summary);
                } else {
                    callback.onError(new Exception("Lỗi khi lấy báo cáo doanh thu: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ShiftReportResponse> call, Throwable t) {
                callback.onError(new Exception("Lỗi kết nối khi lấy báo cáo doanh thu: " + t.getMessage(), t));
            }
        });
    }

    /** Lấy số đơn chưa thanh toán (để check trước khi đóng ca). */
    public void getUnpaidOrderCount(int shiftId, RepositoryCallback<Integer> callback) {
        apiService.getShiftReport(shiftId).enqueue(new Callback<ShiftReportResponse>() {
            @Override
            public void onResponse(Call<ShiftReportResponse> call, Response<ShiftReportResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int unpaidCount = response.body().getUnpaidOrders() != null ? response.body().getUnpaidOrders() : 0;
                    callback.onSuccess(unpaidCount);
                } else {
                    callback.onError(new Exception("Lỗi khi kiểm tra hóa đơn chưa thanh toán: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ShiftReportResponse> call, Throwable t) {
                callback.onError(new Exception("Lỗi kết nối khi kiểm tra hóa đơn chưa thanh toán: " + t.getMessage(), t));
            }
        });
    }

    // ── Inner class cho summary ──

    public static class ShiftRevenueSummary {
        public final double totalRevenue;
        public final double cashRevenue;
        public final int paymentCount;
        public final int paidOrderCount;
        public final int unpaidOrderCount;
        public final List<PaymentMethodStatsRow> methodStats;

        public ShiftRevenueSummary(double totalRevenue, double cashRevenue,
                                   int paymentCount, int paidOrderCount,
                                   int unpaidOrderCount,
                                   List<PaymentMethodStatsRow> methodStats) {
            this.totalRevenue = totalRevenue;
            this.cashRevenue = cashRevenue;
            this.paymentCount = paymentCount;
            this.paidOrderCount = paidOrderCount;
            this.unpaidOrderCount = unpaidOrderCount;
            this.methodStats = methodStats;
        }
    }
}
