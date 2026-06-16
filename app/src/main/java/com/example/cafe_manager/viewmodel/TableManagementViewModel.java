package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.cafe_manager.data.local.entity.TableEntity;
import com.example.cafe_manager.data.local.entity.AreaEntity;
import com.example.cafe_manager.data.repository.TableRepository;
import com.example.cafe_manager.data.repository.AreaRepository;
import com.example.cafe_manager.util.Constants;

import java.util.List;

public class TableManagementViewModel extends AndroidViewModel {

    private final TableRepository repository;
    private final AreaRepository areaRepository;
    private final LiveData<List<TableEntity>> tables;
    private final LiveData<List<AreaEntity>> areas;
    private final LiveData<Integer> totalCount;
    private final LiveData<Integer> emptyCount;
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public TableManagementViewModel(@NonNull Application application) {
        super(application);
        repository = new TableRepository(application);
        areaRepository = new AreaRepository(application);
        tables = repository.getAllTables();
        areas = areaRepository.getAllAreas();

        totalCount = Transformations.map(tables,
                list -> list == null ? 0 : list.size());

        emptyCount = Transformations.map(tables, list -> {
            if (list == null) return 0;
            int count = 0;
            for (TableEntity t : list) {
                if (Constants.TABLE_EMPTY.equals(t.getStatus())) count++;
            }
            return count;
        });
    }

    public LiveData<List<TableEntity>> getTables() { return tables; }
    public LiveData<List<AreaEntity>> getAreas() { return areas; }
    public LiveData<Integer> getTotalCount() { return totalCount; }
    public LiveData<Integer> getEmptyCount() { return emptyCount; }
    public LiveData<String> getMessage() { return message; }

    public void clearMessage() { message.setValue(null); }

    public void addTable(String name, int capacity, String area) {
        TableEntity table = new TableEntity();
        table.setTableName(name);
        table.setCapacity(capacity);
        table.setArea(area);
        table.setStatus(Constants.TABLE_EMPTY);
        table.setCreatedAt(System.currentTimeMillis());

        repository.insert(table,
                () -> message.setValue("Thêm bàn thành công"));
    }

    public void updateTable(TableEntity table) {
        repository.update(table,
                () -> message.setValue("Cập nhật bàn thành công"));
    }

    public void deleteTable(int tableId) {
        repository.delete(tableId,
                () -> message.setValue("Đã xoá bàn"),
                () -> message.setValue("Không thể xoá bàn đang có order"));
    }

    public void addArea(String name, String prefix) {
        AreaEntity area = new AreaEntity(name, prefix, System.currentTimeMillis());
        areaRepository.insert(area,
                () -> message.setValue("Thêm khu vực thành công"));
    }

    public void updateArea(AreaEntity area) {
        areaRepository.update(area,
                () -> message.setValue("Cập nhật khu vực thành công"));
    }

    public void deleteArea(int areaId, String areaName) {
        areaRepository.delete(areaId, areaName,
                () -> message.setValue("Đã xoá khu vực"),
                () -> message.setValue("Không thể xoá khu vực đang chứa bàn!"));
    }
}
