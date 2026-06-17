package com.example.cafe_manager.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.dao.AreaDao;
import com.example.cafe_manager.data.local.dao.TableDao;
import com.example.cafe_manager.data.local.entity.AreaEntity;
import com.example.cafe_manager.util.AppExecutors;

import java.util.List;

public class AreaRepository {

    private final AreaDao areaDao;
    private final TableDao tableDao;
    private final AppExecutors appExecutors;

    public AreaRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.areaDao = db.areaDao();
        this.tableDao = db.tableDao();
        this.appExecutors = AppExecutors.getInstance();
    }

    public LiveData<List<AreaEntity>> getAllAreas() {
        return areaDao.getAll();
    }

    public void insert(AreaEntity area, Runnable onSuccess) {
        appExecutors.diskIO().execute(() -> {
            areaDao.insert(area);
            appExecutors.mainThread().execute(onSuccess);
        });
    }

    public void update(AreaEntity area, Runnable onSuccess) {
        appExecutors.diskIO().execute(() -> {
            AreaEntity oldArea = areaDao.getById(area.getAreaId());
            String oldName = oldArea != null ? oldArea.getAreaName() : null;
            areaDao.update(area);
            if (oldName != null && !oldName.equals(area.getAreaName())) {
                tableDao.updateAreaNameInTables(oldName, area.getAreaName());
            }
            appExecutors.mainThread().execute(onSuccess);
        });
    }

    public void delete(int areaId, String areaName, Runnable onSuccess, Runnable onError) {
        appExecutors.diskIO().execute(() -> {
            int tableCount = areaDao.countTablesByArea(areaName);
            if (tableCount > 0) {
                appExecutors.mainThread().execute(onError);
                return;
            }
            areaDao.deleteById(areaId);
            appExecutors.mainThread().execute(onSuccess);
        });
    }
}
