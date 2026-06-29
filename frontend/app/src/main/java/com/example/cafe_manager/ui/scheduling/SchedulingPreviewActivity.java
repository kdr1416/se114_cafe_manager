package com.example.cafe_manager.ui.scheduling;

import android.content.Intent;
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
import com.example.cafe_manager.data.remote.SchedulingResponse;
import com.example.cafe_manager.data.remote.ShiftSuggestion;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.viewmodel.SchedulingViewModel;

import com.example.cafe_manager.util.WeekNavigationHelper;
import java.util.List;
import java.util.ArrayList;

public class SchedulingPreviewActivity extends AppCompatActivity {

    private SchedulingViewModel viewModel;
    private SchedulingPreviewAdapter adapter;

    private ImageButton btnPrevWeek;
    private ImageButton btnNextWeek;
    private TextView tvWeekRange;
    private Button btnRunPreview;
    private Button btnApplyAll;
    private ProgressBar progressLoading;
    private TextView tvEmpty;
    private TextView tvTotal;
    private TextView tvFulfilled;
    private TextView tvMissing;
    private RecyclerView rvSuggestions;

    private long currentWeekStart;
    private long currentRunId = 0;
    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Role guard: only ADMIN or MANAGER
        SessionManager session = SessionManager.getInstance(this);
        if (!session.isAdmin() && !session.isManager()) {
            Toast.makeText(this, "Bạn không có quyền truy cập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_scheduling_preview);

        currentWeekStart = WeekNavigationHelper.getCurrentWeekStart();

        setupTopBar();
        bindViews();
        tvWeekRange.setText(WeekNavigationHelper.formatWeekRange(currentWeekStart));
        setupRecyclerView();
        setupViewModel();
        setupClickListeners();
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);
        View btnRight = topBar.findViewById(R.id.btn_right);

        title.setText("Tự động xếp ca");
        caption.setText("Tạo lịch ca tự động theo mẫu");
        btnBack.setVisibility(View.VISIBLE);
        btnBack.setOnClickListener(v -> finish());
        btnRight.setVisibility(View.GONE);
    }

    private void bindViews() {
        btnPrevWeek = findViewById(R.id.btn_prev_week);
        btnNextWeek = findViewById(R.id.btn_next_week);
        tvWeekRange = findViewById(R.id.tv_week_range);
        btnRunPreview = findViewById(R.id.btn_run_preview);
        btnApplyAll = findViewById(R.id.btn_apply_all);
        progressLoading = findViewById(R.id.progressLoading);
        tvEmpty = findViewById(R.id.tv_empty);
        tvTotal = findViewById(R.id.tv_total);
        tvFulfilled = findViewById(R.id.tv_fulfilled);
        tvMissing = findViewById(R.id.tv_missing);
        rvSuggestions = findViewById(R.id.rv_suggestions);
    }

    private void setupRecyclerView() {
        rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SchedulingPreviewAdapter();
        rvSuggestions.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(SchedulingViewModel.class);

        viewModel.getIsLoading().observe(this, isLoading -> {
            android.util.Log.d("SchedulingActivity", "isLoading changed: " + isLoading);
            progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnRunPreview.setEnabled(!isLoading);
            if (isLoading) {
                rvSuggestions.setVisibility(View.GONE);
                btnApplyAll.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.GONE);
            }
        });

        viewModel.getPreviewResult().observe(this, response -> {
            android.util.Log.d("SchedulingActivity", "previewResult received: "
                + (response == null ? "null" : "runId=" + response.getRunId()));
            if (response != null && response.getSuggestions() != null) {
                List<ShiftSuggestion> suggestions = response.getSuggestions();
                adapter.setItems(suggestions);

                boolean hasSuggestions = !suggestions.isEmpty();
                rvSuggestions.setVisibility(hasSuggestions ? View.VISIBLE : View.GONE);
                tvEmpty.setVisibility(hasSuggestions ? View.GONE : View.VISIBLE);

                // Update summary counts
                tvTotal.setText(String.valueOf(response.getTotalShifts() != null ? response.getTotalShifts() : 0));
                tvFulfilled.setText(String.valueOf(response.getFulfilledShifts() != null ? response.getFulfilledShifts() : 0));
                tvMissing.setText(String.valueOf(response.getMissingShifts() != null ? response.getMissingShifts() : 0));

                // Show Apply button only if status is PREVIEW
                if ("PREVIEW".equalsIgnoreCase(response.getStatus())) {
                    currentRunId = response.getRunId();
                    btnApplyAll.setVisibility(View.VISIBLE);
                } else {
                    btnApplyAll.setVisibility(View.GONE);
                }
            }
        });

        viewModel.getErrorMessage().observe(this, err -> {
            android.util.Log.d("SchedulingActivity", "errorMessage: " + err);
            if (err != null) {
                Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });

        viewModel.getSuccessMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
                finish(); // Go back after successful apply
            }
        });
    }

    private void setupClickListeners() {
        btnPrevWeek.setOnClickListener(v -> {
            currentWeekStart = WeekNavigationHelper.addWeeks(currentWeekStart, -1);
            tvWeekRange.setText(WeekNavigationHelper.formatWeekRange(currentWeekStart));
        });
        btnNextWeek.setOnClickListener(v -> {
            currentWeekStart = WeekNavigationHelper.addWeeks(currentWeekStart, 1);
            tvWeekRange.setText(WeekNavigationHelper.formatWeekRange(currentWeekStart));
        });
        btnRunPreview.setOnClickListener(v -> {
            viewModel.runPreview(currentWeekStart, currentWeekStart + 6 * DAY_MS);
        });
        btnApplyAll.setOnClickListener(v -> {
            if (currentRunId != 0) {
                viewModel.applyPreview(currentRunId);
            }
        });
    }
}
