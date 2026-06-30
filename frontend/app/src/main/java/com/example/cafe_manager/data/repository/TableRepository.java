package com.example.cafe_manager.data.repository;

import android.content.Context;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.local.entity.TableEntity;
import com.example.cafe_manager.data.remote.ApiClient;
import com.example.cafe_manager.data.remote.TableApiService;
import com.example.cafe_manager.data.remote.NetworkException;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository quản lý dữ liệu bàn ăn, kết nối trực tiếp đến Backend qua Retrofit APIs.
 */
public class TableRepository {

    private static TableRepository instance;

    private final Context context;
    private final TableApiService tableApiService;
    private final AppExecutors appExecutors;
    private final MutableLiveData<List<TableEntity>> allTables;
    private final MutableLiveData<Boolean> isLoading;

    public static synchronized TableRepository getInstance(Context context) {
        if (instance == null) {
            instance = new TableRepository(context);
        }
        return instance;
    }

    private TableRepository(Context context) {
        this.context = context.getApplicationContext();
        this.tableApiService = ApiClient.getInstance(context).getService(TableApiService.class);
        this.appExecutors = AppExecutors.getInstance();
        this.allTables = new MutableLiveData<>();
        this.isLoading = new MutableLiveData<>(false);
        refreshAllTables();
    }

    public LiveData<List<TableEntity>> getAllTables() {
        return allTables;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * Tải lại danh sách bàn ăn từ máy chủ và cập nhật vào LiveData.
     */
    public void refreshAllTables() {
        isLoading.postValue(true);
        tableApiService.getAllTables().enqueue(new Callback<List<TableEntity>>() {
            @Override
            public void onResponse(Call<List<TableEntity>> call, Response<List<TableEntity>> response) {
                isLoading.postValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    allTables.postValue(response.body());
                } else {
                    showError(parseError(response));
                }
            }

            @Override
            public void onFailure(Call<List<TableEntity>> call, Throwable t) {
                isLoading.postValue(false);
                showError(new NetworkException("Không có kết nối mạng", t));
            }
        });
    }

    /**
     * Cập nhật trạng thái bàn ăn (Ví dụ: trống, đang dùng) sử dụng Optimistic UI.
     */
    public void updateTableStatus(int tableId, String status) {
        List<TableEntity> currentList = allTables.getValue();
        if (currentList == null) {
            // Fallback if cache is empty
            refreshAllTables();
            return;
        }

        // Sao lưu danh sách cũ để khôi phục nếu lỗi
        final List<TableEntity> originalList = new java.util.ArrayList<>(currentList);

        TableEntity targetTable = null;
        int targetIndex = -1;
        for (int i = 0; i < currentList.size(); i++) {
            if (currentList.get(i).getTableId() == tableId) {
                targetTable = currentList.get(i);
                targetIndex = i;
                break;
            }
        }

        if (targetTable == null) {
            refreshAllTables();
            return;
        }

        // Optimistic update: cập nhật LiveData ngay lập tức trên Main Thread
        List<TableEntity> optimisticList = new java.util.ArrayList<>(currentList);
        TableEntity updatedTable = new TableEntity(
                targetTable.getTableName(),
                status,
                targetTable.getCapacity(),
                targetTable.getArea(),
                targetTable.getCreatedAt()
        );
        updatedTable.setTableId(targetTable.getTableId());
        optimisticList.set(targetIndex, updatedTable);
        allTables.setValue(optimisticList);

        // Gọi API ngầm dưới nền mà không dùng loading spinner cản trở người dùng
        tableApiService.updateTable(tableId, updatedTable).enqueue(new Callback<TableEntity>() {
            @Override
            public void onResponse(Call<TableEntity> call, Response<TableEntity> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TableEntity confirmedTable = response.body();
                    appExecutors.mainThread().execute(() -> {
                        List<TableEntity> list = allTables.getValue();
                        if (list != null) {
                            List<TableEntity> updatedList = new java.util.ArrayList<>(list);
                            for (int i = 0; i < updatedList.size(); i++) {
                                if (updatedList.get(i).getTableId() == confirmedTable.getTableId()) {
                                    updatedList.set(i, confirmedTable);
                                    break;
                                }
                            }
                            allTables.setValue(updatedList);
                        }
                    });
                } else {
                    // Revert if error
                    appExecutors.mainThread().execute(() -> allTables.setValue(originalList));
                    showError(parseError(response));
                }
            }

            @Override
            public void onFailure(Call<TableEntity> call, Throwable t) {
                // Revert if network failure
                appExecutors.mainThread().execute(() -> allTables.setValue(originalList));
                showError(new NetworkException("Không có kết nối mạng", t));
            }
        });
    }

    /**
     * Thêm bàn mới. Cập nhật trực tiếp cache LiveData.
     */
    public void insert(TableEntity table, RepositoryCallback<TableEntity> callback) {
        isLoading.postValue(true);
        tableApiService.createTable(table).enqueue(new Callback<TableEntity>() {
            @Override
            public void onResponse(Call<TableEntity> call, Response<TableEntity> response) {
                isLoading.postValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    TableEntity newTable = response.body();
                    appExecutors.mainThread().execute(() -> {
                        List<TableEntity> currentList = allTables.getValue();
                        if (currentList != null) {
                            List<TableEntity> newList = new java.util.ArrayList<>(currentList);
                            newList.add(newTable);
                            allTables.setValue(newList);
                        } else {
                            refreshAllTables();
                        }
                    });
                    if (callback != null) {
                        appExecutors.mainThread().execute(() -> callback.onSuccess(newTable));
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
            public void onFailure(Call<TableEntity> call, Throwable t) {
                isLoading.postValue(false);
                Exception e = new NetworkException("Không có kết nối mạng", t);
                showError(e);
                if (callback != null) {
                    appExecutors.mainThread().execute(() -> callback.onError(e));
                }
            }
        });
    }

    @Deprecated
    public void insert(TableEntity table, Runnable onSuccess) {
        insert(table, new RepositoryCallback<TableEntity>() {
            @Override
            public void onSuccess(TableEntity result) {
                if (onSuccess != null) {
                    appExecutors.mainThread().execute(onSuccess);
                }
            }
            @Override
            public void onError(Exception e) {
                // Lỗi hiển thị qua showError
            }
        });
    }

    /**
     * Cập nhật thông tin bàn. Cập nhật trực tiếp cache LiveData.
     */
    public void update(TableEntity table, RepositoryCallback<TableEntity> callback) {
        isLoading.postValue(true);
        tableApiService.updateTable(table.getTableId(), table).enqueue(new Callback<TableEntity>() {
            @Override
            public void onResponse(Call<TableEntity> call, Response<TableEntity> response) {
                isLoading.postValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    TableEntity updatedTable = response.body();
                    appExecutors.mainThread().execute(() -> {
                        List<TableEntity> currentList = allTables.getValue();
                        if (currentList != null) {
                            List<TableEntity> newList = new java.util.ArrayList<>(currentList);
                            for (int i = 0; i < newList.size(); i++) {
                                if (newList.get(i).getTableId() == updatedTable.getTableId()) {
                                    newList.set(i, updatedTable);
                                    break;
                                }
                            }
                            allTables.setValue(newList);
                        } else {
                            refreshAllTables();
                        }
                    });
                    if (callback != null) {
                        appExecutors.mainThread().execute(() -> callback.onSuccess(updatedTable));
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
            public void onFailure(Call<TableEntity> call, Throwable t) {
                isLoading.postValue(false);
                Exception e = new NetworkException("Không có kết nối mạng", t);
                showError(e);
                if (callback != null) {
                    appExecutors.mainThread().execute(() -> callback.onError(e));
                }
            }
        });
    }

    @Deprecated
    public void update(TableEntity table, Runnable onSuccess) {
        update(table, new RepositoryCallback<TableEntity>() {
            @Override
            public void onSuccess(TableEntity result) {
                if (onSuccess != null) {
                    appExecutors.mainThread().execute(onSuccess);
                }
            }
            @Override
            public void onError(Exception e) {
                // Lỗi hiển thị qua showError
            }
        });
    }

    /**
     * Xóa bàn ăn. Cập nhật trực tiếp cache LiveData.
     */
    public void delete(int tableId, RepositoryCallback<Void> callback) {
        isLoading.postValue(true);
        tableApiService.deleteTable(tableId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                isLoading.postValue(false);
                if (response.isSuccessful()) {
                    appExecutors.mainThread().execute(() -> {
                        List<TableEntity> currentList = allTables.getValue();
                        if (currentList != null) {
                            List<TableEntity> newList = new java.util.ArrayList<>(currentList);
                            for (int i = 0; i < newList.size(); i++) {
                                if (newList.get(i).getTableId() == tableId) {
                                    newList.remove(i);
                                    break;
                                }
                            }
                            allTables.setValue(newList);
                        } else {
                            refreshAllTables();
                        }
                    });
                    if (callback != null) {
                        appExecutors.mainThread().execute(() -> callback.onSuccess(null));
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
            public void onFailure(Call<Void> call, Throwable t) {
                isLoading.postValue(false);
                Exception e = new NetworkException("Không có kết nối mạng", t);
                showError(e);
                if (callback != null) {
                    appExecutors.mainThread().execute(() -> callback.onError(e));
                }
            }
        });
    }

    @Deprecated
    public void delete(int tableId, Runnable onSuccess, Runnable onError) {
        delete(tableId, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
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

    /**
     * Cập nhật tên khu vực cho toàn bộ bàn ăn thuộc khu vực cũ.
     */
    public void updateTableAreaName(String oldAreaName, String newAreaName, Runnable onSuccess) {
        if (allTables.getValue() == null) {
            if (onSuccess != null) {
                appExecutors.mainThread().execute(onSuccess);
            }
            return;
        }

        int count = 0;
        for (TableEntity t : allTables.getValue()) {
            if (oldAreaName.equals(t.getArea())) {
                count++;
            }
        }

        if (count == 0) {
            if (onSuccess != null) {
                appExecutors.mainThread().execute(onSuccess);
            }
            return;
        }

        isLoading.postValue(true);
        final int totalToUpdate = count;
        final int[] updatedCount = {0};

        for (TableEntity t : allTables.getValue()) {
            if (oldAreaName.equals(t.getArea())) {
                TableEntity updatedTable = new TableEntity(
                        t.getTableName(),
                        t.getStatus(),
                        t.getCapacity(),
                        newAreaName,
                        t.getCreatedAt()
                );
                updatedTable.setTableId(t.getTableId());

                tableApiService.updateTable(t.getTableId(), updatedTable).enqueue(new Callback<TableEntity>() {
                    @Override
                    public void onResponse(Call<TableEntity> call, Response<TableEntity> response) {
                        synchronized (updatedCount) {
                            updatedCount[0]++;
                            if (updatedCount[0] == totalToUpdate) {
                                refreshAllTables();
                                if (onSuccess != null) {
                                    appExecutors.mainThread().execute(onSuccess);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<TableEntity> call, Throwable t) {
                        synchronized (updatedCount) {
                            updatedCount[0]++;
                            if (updatedCount[0] == totalToUpdate) {
                                refreshAllTables();
                                if (onSuccess != null) {
                                    appExecutors.mainThread().execute(onSuccess);
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    private Exception parseError(Response<?> response) {
        if (response == null) return new NetworkException("Không có kết nối mạng");
        switch (response.code()) {
            case 400:
                try {
                    String errorBody = response.errorBody().string();
                    if (errorBody.contains("bàn đang có hóa đơn chưa thanh toán") || errorBody.contains("active orders")) {
                        return new Exception("Không thể thực hiện: Bàn đang có hóa đơn chưa thanh toán");
                    }
                } catch (Exception e) {
                    // ignore
                }
                return new Exception("Yêu cầu không hợp lệ (400)");
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
