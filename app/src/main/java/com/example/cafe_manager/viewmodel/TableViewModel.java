package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.cafe_manager.data.local.entity.TableEntity;
import com.example.cafe_manager.data.repository.TableRepository;
import com.example.cafe_manager.util.Constants;

import java.util.List;

public class TableViewModel extends AndroidViewModel {

    private final TableRepository tableRepository;

    private final LiveData<List<TableEntity>> tablesLiveData;
    private final LiveData<Integer> totalCountLiveData;
    private final LiveData<Integer> emptyCountLiveData;
    private final LiveData<Integer> occupiedCountLiveData;

    public TableViewModel(@NonNull Application application) {
        super(application);

        this.tableRepository = new TableRepository(application);

        // Lấy danh sách tất cả bàn
        this.tablesLiveData = tableRepository.getAllTables();

        // Tổng số bàn
        this.totalCountLiveData = Transformations.map(
                tablesLiveData,
                tables -> tables == null ? 0 : tables.size()
        );

        // Số bàn trống
        this.emptyCountLiveData = Transformations.map(
                tablesLiveData,
                tables -> countByStatus(tables, Constants.TABLE_EMPTY)
        );

        // Số bàn đang sử dụng
        this.occupiedCountLiveData = Transformations.map(
                tablesLiveData,
                tables -> countByStatus(tables, Constants.TABLE_OCCUPIED)
        );
    }

    public LiveData<List<TableEntity>> getTables() {
        return tablesLiveData;
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

    /**
     * Đếm số bàn theo trạng thái
     */
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
