package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.remote.SchedulingResponse;
import com.example.cafe_manager.data.repository.SchedulingRepository;
import com.example.cafe_manager.util.RepositoryCallback;

public class SchedulingViewModel extends AndroidViewModel {

    private final SchedulingRepository schedulingRepository;

    private final MutableLiveData<SchedulingResponse> previewResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();

    public SchedulingViewModel(@NonNull Application application) {
        super(application);
        schedulingRepository = SchedulingRepository.getInstance(application);
    }

    public LiveData<SchedulingResponse> getPreviewResult() {
        return previewResult;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public void clearError() {
        errorMessage.setValue(null);
    }

    public void clearMessage() {
        successMessage.setValue(null);
    }

    public void runPreview(long startDate, long endDate) {
        if (startDate == 0 || endDate == 0) {
            errorMessage.setValue("Vui lòng chọn ngày bắt đầu và ngày kết thúc");
            return;
        }
        if (endDate < startDate) {
            errorMessage.setValue("Ngày kết thúc phải sau ngày bắt đầu");
            return;
        }

        android.util.Log.d("SchedulingVM", "runPreview called: startDate=" + startDate);
        isLoading.setValue(true);
        schedulingRepository.runPreview(startDate, endDate, new RepositoryCallback<SchedulingResponse>() {
            @Override
            public void onSuccess(SchedulingResponse result) {
                android.util.Log.d("SchedulingVM", "onSuccess received, posting to LiveData");
                isLoading.setValue(false);
                previewResult.setValue(result);
            }

            @Override
            public void onError(Exception e) {
                android.util.Log.d("SchedulingVM", "onError: " + e.getMessage());
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
                previewResult.setValue(null);
            }
        });
    }

    public void applyPreview(long runId) {
        if (runId == 0) {
            errorMessage.setValue("Không tìm thấy runId để áp dụng");
            return;
        }

        isLoading.setValue(true);
        schedulingRepository.applyPreview(runId, new RepositoryCallback<SchedulingResponse>() {
            @Override
            public void onSuccess(SchedulingResponse result) {
                isLoading.setValue(false);
                successMessage.setValue("Đã áp dụng lịch ca thành công");
                previewResult.setValue(result);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }
}
