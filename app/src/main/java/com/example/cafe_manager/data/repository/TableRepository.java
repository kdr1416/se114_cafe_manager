package com.example.cafe_manager.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.dao.TableDao;
import com.example.cafe_manager.data.local.entity.TableEntity;
import com.example.cafe_manager.util.AppExecutors;

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
}