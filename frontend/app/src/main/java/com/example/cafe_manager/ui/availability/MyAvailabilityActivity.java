package com.example.cafe_manager.ui.availability;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.ui.availability.model.AvailabilityConflictUiModel;
import com.example.cafe_manager.ui.availability.model.AvailabilityDayUiModel;
import com.example.cafe_manager.ui.availability.model.AvailabilitySlotUiModel;
import com.example.cafe_manager.ui.common.WeeklyCalendarAdapter;
import com.example.cafe_manager.ui.common.WeeklyCalendarItem;
import com.example.cafe_manager.util.WeekNavigationHelper;
import com.example.cafe_manager.viewmodel.AvailabilityViewModel;

import java.util.ArrayList;
import java.util.List;

public class MyAvailabilityActivity extends AppCompatActivity {

    private AvailabilityViewModel viewModel;
    private WeeklyCalendarAdapter adapter;

    private TextView tvSelectedCount;
    private ProgressBar progressLoading;
    private RecyclerView recyclerAvailability;
    private TextView tvEmptyState;
    private Button btnSaveAvailability;

    // Week navigation
    private TextView tvWeekRange;
    private ImageButton btnPrevWeek;
    private ImageButton btnNextWeek;
    private TextView tvLockBanner;
    private long currentWeekStart;

    private boolean isWeekLocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_availability);

        currentWeekStart = WeekNavigationHelper.getCurrentWeekStart();

        setupTopBar();
        bindViews();
        setupWeekNavigation();
        setupRecyclerView();
        setupViewModel();

        viewModel.loadAvailabilityForWeek(currentWeekStart);
        viewModel.checkWeekLock(currentWeekStart);
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView tvTitle = topBar.findViewById(R.id.tv_title);
        TextView tvCaption = topBar.findViewById(R.id.tv_caption);
        ImageButton btnBack = topBar.findViewById(R.id.btn_back);

        tvTitle.setText("Lịch rảnh của tôi");
        tvCaption.setText("Chọn các ca bạn có thể làm hằng tuần");

        btnBack.setVisibility(View.VISIBLE);
        btnBack.setOnClickListener(v -> finish());
    }

    private void bindViews() {
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        progressLoading = findViewById(R.id.progressLoading);
        recyclerAvailability = findViewById(R.id.recyclerAvailability);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnSaveAvailability = findViewById(R.id.btnSaveAvailability);
        tvWeekRange = findViewById(R.id.tv_week_range);
        btnPrevWeek = findViewById(R.id.btn_prev_week);
        btnNextWeek = findViewById(R.id.btn_next_week);
        tvLockBanner = findViewById(R.id.tv_lock_banner);

        btnSaveAvailability.setOnClickListener(v -> onSaveClicked());
    }

    private void setupWeekNavigation() {
        tvWeekRange.setText(WeekNavigationHelper.formatWeekRange(currentWeekStart));

        btnPrevWeek.setOnClickListener(v -> {
            currentWeekStart = WeekNavigationHelper.addWeeks(currentWeekStart, -1);
            onWeekChanged();
        });

        btnNextWeek.setOnClickListener(v -> {
            currentWeekStart = WeekNavigationHelper.addWeeks(currentWeekStart, 1);
            onWeekChanged();
        });
    }

    private void onWeekChanged() {
        tvWeekRange.setText(WeekNavigationHelper.formatWeekRange(currentWeekStart));
        viewModel.checkWeekLock(currentWeekStart);
        viewModel.loadAvailabilityForWeek(currentWeekStart);
    }

    private void setupRecyclerView() {
        recyclerAvailability.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WeeklyCalendarAdapter((slot, isAvailable) -> {
            viewModel.onSlotChanged(slot.getTemplateId(), slot.getDayOfWeek(), isAvailable);
        });
        recyclerAvailability.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(AvailabilityViewModel.class);

        // Loading state observer
        viewModel.getLoading().observe(this, isLoading -> {
            progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            recyclerAvailability.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            btnSaveAvailability.setEnabled(!isLoading && !isWeekLocked);
        });

        // Saving state observer
        viewModel.getSaving().observe(this, isSaving -> {
            if (isSaving) {
                progressLoading.setVisibility(View.VISIBLE);
                btnSaveAvailability.setEnabled(false);
                btnSaveAvailability.setText("Đang lưu...");
            } else {
                progressLoading.setVisibility(View.GONE);
                btnSaveAvailability.setEnabled(!isWeekLocked);
                btnSaveAvailability.setText("Lưu lịch rảnh");
            }
        });

        // Selected count observer
        viewModel.getSelectedCount().observe(this, count -> {
            tvSelectedCount.setText("Đã chọn " + count + " ca rảnh");
        });

        // Error message observer
        viewModel.getError().observe(this, err -> {
            if (err != null) {
                Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });

        // Success message observer
        viewModel.getMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });

        // Conflict observer
        viewModel.getConflict().observe(this, conflict -> {
            if (conflict != null) {
                showConflictDialog(conflict);
            }
        });

        // Week lock observer
        viewModel.getWeekLocked().observe(this, locked -> {
            isWeekLocked = locked != null && locked;
            if (isWeekLocked) {
                tvLockBanner.setVisibility(View.VISIBLE);
                btnSaveAvailability.setEnabled(false);
            } else {
                tvLockBanner.setVisibility(View.GONE);
                btnSaveAvailability.setEnabled(true);
            }
        });

        // Availability Days observer — build WeeklyCalendarItem list
        viewModel.getAvailabilityDays().observe(this, days -> {
            if (days == null || days.isEmpty()) {
                tvEmptyState.setVisibility(View.VISIBLE);
                recyclerAvailability.setVisibility(View.GONE);
            } else {
                tvEmptyState.setVisibility(View.GONE);
                recyclerAvailability.setVisibility(View.VISIBLE);

                List<WeeklyCalendarItem> listItems = new ArrayList<>();
                boolean hasTemplates = false;
                for (AvailabilityDayUiModel day : days) {
                    if (day.getSlots() != null && !day.getSlots().isEmpty()) {
                        hasTemplates = true;
                        // Use formatted day label with date from week navigation
                        long dayEpoch = currentWeekStart + (long)(day.getDayOfWeek() - 1) * 24 * 60 * 60 * 1000;
                        String label = WeekNavigationHelper.formatDayLabel(dayEpoch);
                        listItems.add(new WeeklyCalendarItem.DayHeader(label, day.getDayOfWeek(), isWeekLocked));
                        for (AvailabilitySlotUiModel slot : day.getSlots()) {
                            listItems.add(new WeeklyCalendarItem.AvailabilitySlot(slot, isWeekLocked));
                        }
                    }
                }

                if (!hasTemplates) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    recyclerAvailability.setVisibility(View.GONE);
                } else {
                    adapter.setItems(listItems);
                }
            }
        });
    }

    private void onSaveClicked() {
        // Collect dirty slots first
        List<AvailabilityDayUiModel> days = viewModel.getAvailabilityDays().getValue();
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
            Toast.makeText(this, "Không có thay đổi cần lưu", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show publish scope dialog
        PublishScopeDialog.show(this, (scope, untilDate) -> {
            for (AvailabilitySlotUiModel slot : changedSlots) {
                viewModel.publishAvailability(
                        slot.getTemplateId(),
                        slot.getDayOfWeek(),
                        slot.isSelected(),
                        scope,
                        untilDate
                );
            }
        });
    }

    private void showConflictDialog(AvailabilityConflictUiModel conflict) {
        StringBuilder sb = new StringBuilder();
        sb.append(conflict.getMessage());
        if (conflict.getConflictItems() != null && !conflict.getConflictItems().isEmpty()) {
            sb.append("\n\nChi tiết xung đột:\n");
            for (String item : conflict.getConflictItems()) {
                sb.append("• ").append(item).append("\n");
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(conflict.getTitle())
                .setMessage(sb.toString())
                .setPositiveButton("Đóng", (dialog, which) -> {
                    dialog.dismiss();
                    viewModel.clearConflict();
                })
                .setCancelable(false)
                .show();
    }
}
