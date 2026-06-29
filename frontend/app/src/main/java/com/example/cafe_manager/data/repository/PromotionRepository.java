package com.example.cafe_manager.data.repository;

import android.content.Context;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.PromotionEntity;
import com.example.cafe_manager.data.remote.ApiClient;
import com.example.cafe_manager.data.remote.PromotionApiService;
import com.example.cafe_manager.data.remote.NetworkException;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.RepositoryCallback;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PromotionRepository {

    private static PromotionRepository instance;

    private final Context context;
    private final PromotionApiService apiService;
    private final AppExecutors appExecutors;

    private final MutableLiveData<List<PromotionEntity>> allPromotions;
    private final MutableLiveData<Long> totalCountLive;
    private final MutableLiveData<Long> activeCountLive;

    public static synchronized PromotionRepository getInstance(Context context) {
        if (instance == null) {
            instance = new PromotionRepository(context);
        }
        return instance;
    }

    private PromotionRepository(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = ApiClient.getInstance(context).getService(PromotionApiService.class);
        this.appExecutors = AppExecutors.getInstance();

        this.allPromotions = new MutableLiveData<>();
        this.totalCountLive = new MutableLiveData<>(0L);
        this.activeCountLive = new MutableLiveData<>(0L);

        refreshAll();
        refreshCounts();
    }

    public LiveData<List<PromotionEntity>> getAll() {
        return allPromotions;
    }

    public LiveData<Long> getTotalCountLive() {
        return totalCountLive;
    }

    public LiveData<Long> getActiveCountLive() {
        return activeCountLive;
    }

    public void refreshAll() {
        apiService.getAll().enqueue(new Callback<List<PromotionEntity>>() {
            @Override
            public void onResponse(Call<List<PromotionEntity>> call, Response<List<PromotionEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allPromotions.postValue(response.body());
                } else {
                    showError(parseError(response));
                }
            }

            @Override
            public void onFailure(Call<List<PromotionEntity>> call, Throwable t) {
                showError(new NetworkException("Không có kết nối mạng", t));
            }
        });
    }

    public void refreshCounts() {
        apiService.getTotalCount().enqueue(new Callback<Long>() {
            @Override
            public void onResponse(Call<Long> call, Response<Long> response) {
                if (response.isSuccessful() && response.body() != null) {
                    totalCountLive.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<Long> call, Throwable t) {}
        });

        apiService.getActiveCount().enqueue(new Callback<Long>() {
            @Override
            public void onResponse(Call<Long> call, Response<Long> response) {
                if (response.isSuccessful() && response.body() != null) {
                    activeCountLive.postValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<Long> call, Throwable t) {}
        });
    }

    /**
     * Safety asynchronous lookup wrapper for checking code.
     */
    public void getByCode(String code, RepositoryCallback<PromotionEntity> callback) {
        if (code == null || code.trim().isEmpty()) {
            appExecutors.mainThread().execute(() -> callback.onError(new Exception("Mã không hợp lệ")));
            return;
        }
        appExecutors.diskIO().execute(() -> {
            try {
                Response<PromotionEntity> response = apiService.getByCode(code.trim().toUpperCase()).execute();
                if (response.isSuccessful() && response.body() != null) {
                    appExecutors.mainThread().execute(() -> callback.onSuccess(response.body()));
                } else if (response.code() == 404) {
                    appExecutors.mainThread().execute(() -> callback.onError(new Exception("Mã khuyến mãi không hợp lệ")));
                } else {
                    appExecutors.mainThread().execute(() -> callback.onError(parseError(response)));
                }
            } catch (IOException e) {
                appExecutors.mainThread().execute(() -> callback.onError(new NetworkException("Không có kết nối mạng", e)));
            }
        });
    }

    public void insert(PromotionEntity promotion, RepositoryCallback<PromotionEntity> callback) {
        apiService.insert(promotion).enqueue(new Callback<PromotionEntity>() {
            @Override
            public void onResponse(Call<PromotionEntity> call, Response<PromotionEntity> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PromotionEntity newPromo = response.body();
                    appExecutors.mainThread().execute(() -> {
                        List<PromotionEntity> currentList = allPromotions.getValue();
                        if (currentList != null) {
                            List<PromotionEntity> newList = new java.util.ArrayList<>(currentList);
                            newList.add(newPromo);
                            allPromotions.setValue(newList);
                        } else {
                            refreshAll();
                        }
                        refreshCounts();
                    });
                    if (callback != null) {
                        appExecutors.mainThread().execute(() -> callback.onSuccess(newPromo));
                    }
                } else {
                    Exception e = parseError(response);
                    showError(e);
                    if (callback != null) {
                        appExecutors.mainThread().execute(() -> callback.onError(e));
                    }
                }
            }

            @Override
            public void onFailure(Call<PromotionEntity> call, Throwable t) {
                Exception e = new NetworkException("Không có kết nối mạng", t);
                showError(e);
                if (callback != null) {
                    appExecutors.mainThread().execute(() -> callback.onError(e));
                }
            }
        });
    }

    @Deprecated
    public void insert(PromotionEntity promotion, Runnable onSuccess, Runnable onError) {
        insert(promotion, new RepositoryCallback<PromotionEntity>() {
            @Override
            public void onSuccess(PromotionEntity result) {
                if (onSuccess != null) {
                    appExecutors.mainThread().execute(onSuccess);
                }
            }

            @Override
            public void onError(Exception e) {
                if (onError != null) {
                    appExecutors.mainThread().execute(onError);
                }
            }
        });
    }

    public void update(PromotionEntity promotion, RepositoryCallback<PromotionEntity> callback) {
        apiService.update(promotion.getPromotionId(), promotion).enqueue(new Callback<PromotionEntity>() {
            @Override
            public void onResponse(Call<PromotionEntity> call, Response<PromotionEntity> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PromotionEntity updatedPromo = response.body();
                    appExecutors.mainThread().execute(() -> {
                        List<PromotionEntity> currentList = allPromotions.getValue();
                        if (currentList != null) {
                            List<PromotionEntity> newList = new java.util.ArrayList<>(currentList);
                            for (int i = 0; i < newList.size(); i++) {
                                if (newList.get(i).getPromotionId() == updatedPromo.getPromotionId()) {
                                    newList.set(i, updatedPromo);
                                    break;
                                }
                            }
                            allPromotions.setValue(newList);
                        } else {
                            refreshAll();
                        }
                        refreshCounts();
                    });
                    if (callback != null) {
                        appExecutors.mainThread().execute(() -> callback.onSuccess(updatedPromo));
                    }
                } else {
                    Exception e = parseError(response);
                    showError(e);
                    if (callback != null) {
                        appExecutors.mainThread().execute(() -> callback.onError(e));
                    }
                }
            }

            @Override
            public void onFailure(Call<PromotionEntity> call, Throwable t) {
                Exception e = new NetworkException("Không có kết nối mạng", t);
                showError(e);
                if (callback != null) {
                    appExecutors.mainThread().execute(() -> callback.onError(e));
                }
            }
        });
    }

    @Deprecated
    public void update(PromotionEntity promotion, Runnable onSuccess) {
        update(promotion, new RepositoryCallback<PromotionEntity>() {
            @Override
            public void onSuccess(PromotionEntity result) {
                if (onSuccess != null) {
                    appExecutors.mainThread().execute(onSuccess);
                }
            }

            @Override
            public void onError(Exception e) {}
        });
    }

    public void toggleActive(int id, boolean active) {
        // Optimistic UI update
        List<PromotionEntity> currentList = allPromotions.getValue();
        final List<PromotionEntity> originalList = currentList != null ? new java.util.ArrayList<>(currentList) : null;
        if (currentList != null) {
            List<PromotionEntity> newList = new java.util.ArrayList<>(currentList);
            for (int i = 0; i < newList.size(); i++) {
                if (newList.get(i).getPromotionId() == id) {
                    PromotionEntity promo = newList.get(i);
                    // Clone new
                    PromotionEntity updated = new PromotionEntity();
                    updated.setPromotionId(promo.getPromotionId());
                    updated.setCode(promo.getCode());
                    updated.setType(promo.getType());
                    updated.setValue(promo.getValue());
                    updated.setActive(active);
                    updated.setExpiresAt(promo.getExpiresAt());
                    updated.setCreatedAt(promo.getCreatedAt());
                    newList.set(i, updated);
                    break;
                }
            }
            allPromotions.setValue(newList);
        }

        apiService.toggleActive(id, active).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    appExecutors.mainThread().execute(() -> refreshCounts());
                } else {
                    if (originalList != null) {
                        appExecutors.mainThread().execute(() -> allPromotions.setValue(originalList));
                    }
                    showError(parseError(response));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (originalList != null) {
                    appExecutors.mainThread().execute(() -> allPromotions.setValue(originalList));
                }
                showError(new NetworkException("Không có kết nối mạng", t));
            }
        });
    }

    public void delete(int id) {
        // Optimistic UI update
        List<PromotionEntity> currentList = allPromotions.getValue();
        final List<PromotionEntity> originalList = currentList != null ? new java.util.ArrayList<>(currentList) : null;
        if (currentList != null) {
            List<PromotionEntity> newList = new java.util.ArrayList<>(currentList);
            for (int i = 0; i < newList.size(); i++) {
                if (newList.get(i).getPromotionId() == id) {
                    newList.remove(i);
                    break;
                }
            }
            allPromotions.setValue(newList);
        }

        apiService.delete(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    appExecutors.mainThread().execute(() -> refreshCounts());
                } else {
                    if (originalList != null) {
                        appExecutors.mainThread().execute(() -> allPromotions.setValue(originalList));
                    }
                    showError(parseError(response));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (originalList != null) {
                    appExecutors.mainThread().execute(() -> allPromotions.setValue(originalList));
                }
                showError(new NetworkException("Không có kết nối mạng", t));
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
