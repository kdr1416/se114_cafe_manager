package com.example.cafe_manager.ui.shift;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cafe_manager.R;
import com.example.cafe_manager.data.remote.DailyShiftReportResponse;
import com.example.cafe_manager.util.CurrencyUtils;
import com.example.cafe_manager.viewmodel.DailyShiftReportViewModel;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DailyShiftReportActivity extends AppCompatActivity {

    private DailyShiftReportViewModel viewModel;
    
    private ImageButton btnPrevDate, btnNextDate;
    private TextView tvDateLabel;
    private NestedScrollView scrollContent;
    private LinearLayout layoutEmpty;
    private ProgressBar progressLoading;
    
    private TextView tvDailyRevenue, tvDailyOrders, tvDailyPayments, tvDailyOpeningCash, tvDailyExpectedCash;
    
    private RecyclerView rvShifts;
    private DailyShiftAdapter adapter;
    
    private final DateTimeFormatter viFormatter = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_shift_report);

        viewModel = new ViewModelProvider(this).get(DailyShiftReportViewModel.class);

        // Setup top bar
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tv_title)).setText("Báo cáo ca");

        // Bind views
        btnPrevDate = findViewById(R.id.btn_prev_date);
        btnNextDate = findViewById(R.id.btn_next_date);
        tvDateLabel = findViewById(R.id.tv_date_label);
        scrollContent = findViewById(R.id.scroll_content);
        layoutEmpty = findViewById(R.id.layout_empty);
        progressLoading = findViewById(R.id.progress_loading);
        
        tvDailyRevenue = findViewById(R.id.tv_daily_revenue);
        tvDailyOrders = findViewById(R.id.tv_daily_orders);
        tvDailyPayments = findViewById(R.id.tv_daily_payments);
        tvDailyOpeningCash = findViewById(R.id.tv_daily_opening_cash);
        tvDailyExpectedCash = findViewById(R.id.tv_daily_expected_cash);
        
        rvShifts = findViewById(R.id.rv_shifts);

        // RecyclerView setup
        rvShifts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DailyShiftAdapter(shift -> {
            Intent intent = new Intent(DailyShiftReportActivity.this, ShiftReportActivity.class);
            intent.putExtra("extra_shift_id", shift.getShiftId());
            startActivity(intent);
        });
        rvShifts.setAdapter(adapter);

        // Date selection events
        btnPrevDate.setOnClickListener(v -> viewModel.prevDay());
        btnNextDate.setOnClickListener(v -> viewModel.nextDay());
        tvDateLabel.setOnClickListener(v -> showDatePicker());

        // Observe LiveData
        viewModel.getSelectedDate().observe(this, this::updateDateLabel);
        
        viewModel.getLoading().observe(this, loading -> {
            if (Boolean.TRUE.equals(loading)) {
                progressLoading.setVisibility(View.VISIBLE);
                scrollContent.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.GONE);
            } else {
                progressLoading.setVisibility(View.GONE);
            }
        });

        viewModel.getDailyReport().observe(this, report -> {
            if (report != null && report.getShifts() != null && !report.getShifts().isEmpty()) {
                bindSummaryData(report);
                adapter.submitList(report.getShifts());
                scrollContent.setVisibility(View.VISIBLE);
                layoutEmpty.setVisibility(View.GONE);
            } else {
                scrollContent.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                viewModel.clearError();
            }
        });

        // Initial load
        viewModel.loadDailyReport();
    }

    private void updateDateLabel(LocalDate date) {
        if (date != null) {
            String formatted = date.format(viFormatter);
            if (!formatted.isEmpty()) {
                formatted = Character.toUpperCase(formatted.charAt(0)) + formatted.substring(1);
            }
            tvDateLabel.setText(formatted);
        }
    }

    private void bindSummaryData(DailyShiftReportResponse report) {
        tvDailyRevenue.setText(CurrencyUtils.formatVnd(report.getTotalRevenue() != null ? report.getTotalRevenue() : 0.0));
        tvDailyOrders.setText(report.getTotalOrders() + " đơn");
        tvDailyPayments.setText(report.getPaymentCount() + " lượt");
        tvDailyOpeningCash.setText(CurrencyUtils.formatVnd(report.getTotalOpeningCash() != null ? report.getTotalOpeningCash() : 0.0));
        tvDailyExpectedCash.setText(CurrencyUtils.formatVnd(report.getTotalExpectedCash() != null ? report.getTotalExpectedCash() : 0.0));
    }

    private void showDatePicker() {
        LocalDate current = viewModel.getSelectedDate().getValue();
        if (current == null) {
            current = LocalDate.now();
        }
        
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    LocalDate selected = LocalDate.of(year, month + 1, dayOfMonth);
                    viewModel.selectDate(selected);
                },
                current.getYear(),
                current.getMonthValue() - 1,
                current.getDayOfMonth()
        );
        dialog.show();
    }
}
