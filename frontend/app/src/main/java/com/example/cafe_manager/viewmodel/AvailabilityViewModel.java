package com.example.cafe_manager.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cafe_manager.data.remote.AvailabilityResponse;
import com.example.cafe_manager.data.remote.SetAvailabilityRequest;
import com.example.cafe_manager.data.remote.ShiftTemplateResponse;
import com.example.cafe_manager.data.remote.ConflictException;
import com.example.cafe_manager.data.remote.PublishAvailabilityRequest;
import com.example.cafe_manager.data.remote.WeekLockResponse;
import com.example.cafe_manager.data.repository.AvailabilityRepository;
import com.example.cafe_manager.ui.availability.model.AvailabilityConflictUiModel;
import com.example.cafe_manager.ui.availability.model.AvailabilityDayUiModel;
import com.example.cafe_manager.ui.availability.model.AvailabilitySlotUiModel;
import com.example.cafe_manager.util.RepositoryCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AvailabilityViewModel extends AndroidViewModel {

    private final AvailabilityRepository repository;

    private final MutableLiveData<List<AvailabilityDayUiModel>> availabilityDays = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> selectedCount = new MutableLiveData<>(0);
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<AvailabilityConflictUiModel> conflict = new MutableLiveData<>();
    private final MutableLiveData<Boolean> weekLocked = new MutableLiveData<>(false);
    private long lastLoadedWeekStart = 0;

    public AvailabilityViewModel(@NonNull Application application) {
        super(application);
        this.repository = AvailabilityRepository.getInstance(application);
    }

    public LiveData<List<AvailabilityDayUiModel>> getAvailabilityDays() { return availabilityDays; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Boolean> getSaving() { return saving; }
    public LiveData<Integer> getSelectedCount() { return selectedCount; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<String> getError() { return error; }
    public LiveData<AvailabilityConflictUiModel> getConflict() { return conflict; }
    public LiveData<Boolean> getWeekLocked() { return weekLocked; }

    public void clearMessage() { message.setValue(null); }
    public void clearError() { error.setValue(null); }
    public void clearConflict() { conflict.setValue(null); }

    public void loadData() {
        if (lastLoadedWeekStart == 0) {
            lastLoadedWeekStart = com.example.cafe_manager.util.WeekNavigationHelper.getCurrentWeekStart();
        }
        loadAvailabilityForWeek(lastLoadedWeekStart);
    }

    public void loadAvailabilityForWeek(long weekStart) {
        this.lastLoadedWeekStart = weekStart;
        loading.setValue(true);
        repository.getActiveTemplates(new RepositoryCallback<List<ShiftTemplateResponse>>() {
            @Override
            public void onSuccess(List<ShiftTemplateResponse> activeTemplates) {
                repository.getMyAvailability(new RepositoryCallback<List<AvailabilityResponse>>() {
                    @Override
                    public void onSuccess(List<AvailabilityResponse> myAvailability) {
                        List<AvailabilityResponse> filtered = new ArrayList<>();
                        if (myAvailability != null) {
                            for (AvailabilityResponse res : myAvailability) {
                                if (res.getWeekStart() == null || res.getWeekStart() == weekStart) {
                                    filtered.add(res);
                                }
                            }
                        }
                        combineData(activeTemplates, filtered);
                        loading.setValue(false);
                    }

                    @Override
                    public void onError(Exception e) {
                        loading.setValue(false);
                        error.setValue("Không thể tải lịch rảnh cá nhân. Vui lòng thử lại.");
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                loading.setValue(false);
                error.setValue("Không thể tải danh sách ca mẫu. Vui lòng thử lại.");
            }
        });
    }

    private void combineData(List<ShiftTemplateResponse> activeTemplates, List<AvailabilityResponse> myAvailability) {
        List<AvailabilityDayUiModel> days = new ArrayList<>();
        int count = 0;

        for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
            String dayLabel = getDayLabel(dayOfWeek);
            List<AvailabilitySlotUiModel> slots = new ArrayList<>();

            for (ShiftTemplateResponse template : activeTemplates) {
                AvailabilityResponse matched = findMatch(myAvailability, template.getTemplateId(), dayOfWeek);

                boolean selected = matched != null && matched.isAvailable();
                Integer availabilityId = matched != null ? matched.getAvailabilityId() : null;

                if (selected) {
                    count++;
                }

                AvailabilitySlotUiModel slot = new AvailabilitySlotUiModel(
                        template.getTemplateId(),
                        dayOfWeek,
                        dayLabel,
                        template.getTemplateName(),
                        template.getStartTime(),
                        template.getEndTime(),
                        selected,
                        selected,
                        availabilityId,
                        template.getEffectiveFromDate(),
                        template.getEffectiveToDate()
                );
                slots.add(slot);
            }
            days.add(new AvailabilityDayUiModel(dayOfWeek, dayLabel, slots));
        }

        availabilityDays.setValue(days);
        selectedCount.setValue(count);
    }

    private AvailabilityResponse findMatch(List<AvailabilityResponse> list, int templateId, int dayOfWeek) {
        if (list == null) return null;
        for (AvailabilityResponse res : list) {
            if (res.getTemplateId() == templateId && res.getDayOfWeek() == dayOfWeek) {
                return res;
            }
        }
        return null;
    }

    private String getDayLabel(int dayOfWeek) {
        switch (dayOfWeek) {
            case 1: return "Thứ 2";
            case 2: return "Thứ 3";
            case 3: return "Thứ 4";
            case 4: return "Thứ 5";
            case 5: return "Thứ 6";
            case 6: return "Thứ 7";
            case 7: return "Chủ nhật";
            default: return "";
        }
    }

    public void onSlotChanged(int templateId, int dayOfWeek, boolean selected) {
        List<AvailabilityDayUiModel> days = availabilityDays.getValue();
        if (days == null) return;

        int count = 0;
        for (AvailabilityDayUiModel day : days) {
            for (AvailabilitySlotUiModel slot : day.getSlots()) {
                if (slot.getTemplateId() == templateId && slot.getDayOfWeek() == dayOfWeek) {
                    slot.setSelected(selected);
                }
                if (slot.isSelected()) {
                    count++;
                }
            }
        }
        selectedCount.setValue(count);
    }

    public void saveChanges() {
        List<AvailabilityDayUiModel> days = availabilityDays.getValue();
        if (days == null) return;

        List<AvailabilitySlotUiModel> changedSlots = new ArrayList<>();
        for (AvailabilityDayUiModel day : days) {
            for (AvailabilitySlotUiModel slot : day.getSlots()) {
                if (slot.isSelected() != slot.isOriginalSelected()) {
                    changedSlots.add(slot);
                }
            }
        }

        if (changedSlots.isEmpty()) {
            message.setValue("Không có thay đổi cần lưu");
            return;
        }

        saving.setValue(true);
        int totalTasks = changedSlots.size();
        int[] completedCount = {0};
        boolean[] hasError = {false};
        List<String> failedTemplates = new ArrayList<>();

        for (AvailabilitySlotUiModel slot : changedSlots) {
            SetAvailabilityRequest request = new SetAvailabilityRequest(
                    slot.getTemplateId(),
                    slot.getDayOfWeek(),
                    slot.isSelected()
            );

            repository.setAvailability(request, new RepositoryCallback<AvailabilityResponse>() {
                @Override
                public void onSuccess(AvailabilityResponse result) {
                    slot.setOriginalSelected(slot.isSelected());
                    slot.setAvailabilityId(result.getAvailabilityId());
                    checkCompletion(completedCount, totalTasks, hasError, failedTemplates);
                }

                @Override
                public void onError(Exception e) {
                    hasError[0] = true;
                    if (e instanceof ConflictException) {
                        AvailabilityConflictUiModel conflictUi = new AvailabilityConflictUiModel(
                                "Không thể cập nhật lịch rảnh",
                                "Bạn đã được xếp vào một hoặc nhiều ca trùng với thay đổi này.\nNếu bạn không thể làm ca đã được xếp, vui lòng tạo đơn xin nghỉ.\nHoặc chỉ thay đổi lịch rảnh từ khoảng thời gian chưa được sắp xếp ca.",
                                Collections.singletonList(slot.getShiftName() + " (" + slot.getDayLabel() + ")")
                        );
                        conflict.setValue(conflictUi);
                    } else {
                        failedTemplates.add(slot.getShiftName() + " (" + slot.getDayLabel() + ")");
                    }
                    checkCompletion(completedCount, totalTasks, hasError, failedTemplates);
                }
            });
        }
    }

    private void checkCompletion(int[] completedCount, int totalTasks, boolean[] hasError, List<String> failedTemplates) {
        completedCount[0]++;
        if (completedCount[0] == totalTasks) {
            saving.setValue(false);
            if (hasError[0]) {
                if (conflict.getValue() == null) {
                    if (failedTemplates.isEmpty()) {
                        error.setValue("Lưu lịch rảnh thất bại. Vui lòng thử lại.");
                    } else {
                        error.setValue("Lưu lịch rảnh thất bại: " + String.join(", ", failedTemplates));
                    }
                }
                loadData(); // Re-sync in case of errors
            } else {
                message.setValue("Đã lưu lịch rảnh");
                loadData(); // Re-sync to ensure clean states
            }
        }
    }

    public void checkWeekLock(long weekStart) {
        repository.getWeekLock(weekStart, new RepositoryCallback<WeekLockResponse>() {
            @Override
            public void onSuccess(WeekLockResponse response) {
                weekLocked.setValue(response.isLocked());
            }

            @Override
            public void onError(Exception e) {
                weekLocked.setValue(false);
            }
        });
    }

    public void publishAvailability(int templateId, int dayOfWeek, boolean isAvailable,
                                     String scope, Long untilDate) {
        saving.setValue(true);
        PublishAvailabilityRequest request = new PublishAvailabilityRequest(
                templateId, dayOfWeek, isAvailable, scope, untilDate);

        repository.publishAvailability(request, new RepositoryCallback<List<AvailabilityResponse>>() {
            @Override
            public void onSuccess(List<AvailabilityResponse> result) {
                saving.setValue(false);
                message.setValue("Đã phát hành lịch rảnh");
                loadData();
            }

            @Override
            public void onError(Exception e) {
                saving.setValue(false);
                error.setValue(e.getMessage());
            }
        });
    }
}
