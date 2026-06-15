package com.example.cafe_manager.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.dao.TableDao;
import com.example.cafe_manager.data.local.entity.TableEntity;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.Constants;

import java.util.List;

public class TableRepository {

    private final TableDao tableDao;
    private final AppExecutors appExecutors;

    public TableRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.tableDao = db.tableDao();
        this.appExecutors = AppExecutors.getInstance();
    }

    public LiveData<List<TableEntity>> getAllTables() {
        return tableDao.getAll();
    }

    public void updateTableStatus(int tableId, String status) {
        appExecutors.diskIO().execute(() ->
                tableDao.updateStatus(tableId, status)
        );
    }

    public void insert(TableEntity table, Runnable onSuccess) {
        appExecutors.diskIO().execute(() -> {
            tableDao.insert(table);
            appExecutors.mainThread().execute(onSuccess);
        });
    }

    public void update(TableEntity table, Runnable onSuccess) {
        appExecutors.diskIO().execute(() -> {
            tableDao.update(table);
            appExecutors.mainThread().execute(onSuccess);
        });
    }

    public void delete(int tableId, Runnable onSuccess, Runnable onError) {
        appExecutors.diskIO().execute(() -> {
            int activeOrders = tableDao.countOrdersByTableAndStatus(tableId, Constants.ORDER_CONFIRMED);
            if (activeOrders > 0) {
                appExecutors.mainThread().execute(onError);
                return;
            }
            tableDao.deleteById(tableId);
            appExecutors.mainThread().execute(onSuccess);
        });
    }
}
