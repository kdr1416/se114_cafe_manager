package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.cafe_manager.data.local.entity.TableEntity;
import com.example.cafe_manager.data.local.entity.AreaEntity;
import com.example.cafe_manager.data.repository.TableRepository;
import com.example.cafe_manager.data.repository.AreaRepository;
import com.example.cafe_manager.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class TableViewModel extends AndroidViewModel {

    private final TableRepository tableRepository;
    private final AreaRepository areaRepository;

    private final MutableLiveData<String> selectedAreaLiveData = new MutableLiveData<>("Tất cả");
    private final MediatorLiveData<List<TableEntity>> tablesLiveData = new MediatorLiveData<>();
    private final LiveData<List<String>> areasLiveData;
    private final LiveData<Integer> totalCountLiveData;
    private final LiveData<Integer> emptyCountLiveData;
    private final LiveData<Integer> occupiedCountLiveData;

    public TableViewModel(@NonNull Application application) {
        super(application);

        this.tableRepository = TableRepository.getInstance(application);
        this.areaRepository = AreaRepository.getInstance(application);

        LiveData<List<TableEntity>> allTables = tableRepository.getAllTables();

        this.areasLiveData = Transformations.map(areaRepository.getAllAreas(), areas -> {
            List<String> list = new ArrayList<>();
            list.add("Tất cả");
            if (areas != null) {
                for (AreaEntity a : areas) {
                    String area = a.getAreaName();
                    if (area != null && !area.trim().isEmpty() && !list.contains(area)) {
                        list.add(area);
                    }
                }
            }
            return list;
        });

        tablesLiveData.addSource(allTables, tables -> filterTables(tables, selectedAreaLiveData.getValue()));
        tablesLiveData.addSource(selectedAreaLiveData, area -> filterTables(allTables.getValue(), area));

        this.totalCountLiveData = Transformations.map(
                tablesLiveData,
                tables -> tables == null ? 0 : tables.size()
        );

        this.emptyCountLiveData = Transformations.map(
                tablesLiveData,
                tables -> countByStatus(tables, Constants.TABLE_EMPTY)
        );

        this.occupiedCountLiveData = Transformations.map(
                tablesLiveData,
                tables -> countByStatus(tables, Constants.TABLE_OCCUPIED)
        );
    }

    private void filterTables(List<TableEntity> tables, String area) {
        if (tables == null) {
            tablesLiveData.setValue(null);
            return;
        }
        if (area == null || "Tất cả".equals(area)) {
            tablesLiveData.setValue(tables);
            return;
        }
        List<TableEntity> filtered = new ArrayList<>();
        for (TableEntity t : tables) {
            if (area.equals(t.getArea())) {
                filtered.add(t);
            }
        }
        tablesLiveData.setValue(filtered);
    }

    public LiveData<List<TableEntity>> getTables() {
        return tablesLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return tableRepository.getIsLoading();
    }

    public LiveData<List<String>> getAreas() {
        return areasLiveData;
    }

    public LiveData<String> getSelectedArea() {
        return selectedAreaLiveData;
    }

    public void selectArea(String area) {
        selectedAreaLiveData.setValue(area);
    }

    public LiveData<Integer> getTotalCount() {
        return totalCountLiveData;
    }

    public LiveData<Integer> getEmptyCount() {
        return emptyCountLiveData;
    }

    public LiveData<Integer> getOccupiedCount() {
        return occupiedCountLiveData;
    }

    private static int countByStatus(List<TableEntity> tables, String status) {
        if (tables == null) {
            return 0;
        }

        int count = 0;

        for (TableEntity table : tables) {
            if (status.equals(table.getStatus())) {
                count++;
            }
        }

        return count;
    }
}
