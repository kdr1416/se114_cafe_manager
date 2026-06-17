package com.example.cafe_manager.ui.dashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.model.DailyRevenueRow;
import com.example.cafe_manager.model.PaymentMethodStatsRow;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.CurrencyUtils;
import com.example.cafe_manager.util.DateRange;
import com.example.cafe_manager.util.PermissionUtils;
import com.example.cafe_manager.viewmodel.DashboardViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private DashboardViewModel viewModel;
    private TopProductAdapter topAdapter;

    private TextView tvRevenue, tvOrderCount, tvAvgOrder;
    private TextView tvCashCount, tvCashRevenue;
    private TextView tvBankingCount, tvBankingRevenue;
    private TextView tvMomoCount, tvMomoRevenue;
    private TextView tvTopEmpty, tvChartEmpty;
    private RecyclerView rvTop;
    private BarChart chartRevenue;
    private PieChart chartPayment;

    private TextView chipToday, chipWeek, chipMonth;

    private int colorAccent;
    private int colorSuccess;
    private int colorWarning;
    private int colorInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PermissionUtils.requireRole(this, Constants.ROLE_ADMIN, Constants.ROLE_MANAGER)) return;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        colorAccent = getColor(R.color.accent);
        colorSuccess = getColor(R.color.success);
        colorWarning = getColor(R.color.warning);
        colorInfo = getColor(R.color.info);

        setupTopBar();
        bindViews();
        setupChips();
        setupCharts();
        setupRecyclerView();
        setupViewModel();
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);
        View btnRight = topBar.findViewById(R.id.btn_right);

        title.setText("Báo cáo doanh thu");
        caption.setText("Thống kê theo khoảng thời gian");
        btnBack.setOnClickListener(v -> finish());
        btnRight.setVisibility(View.GONE);
    }

    private void bindViews() {
        tvRevenue = findViewById(R.id.tv_revenue);
        tvOrderCount = findViewById(R.id.tv_order_count);
        tvAvgOrder = findViewById(R.id.tv_avg_order);

        tvCashCount = findViewById(R.id.tv_cash_count);
        tvCashRevenue = findViewById(R.id.tv_cash_revenue);
        tvBankingCount = findViewById(R.id.tv_banking_count);
        tvBankingRevenue = findViewById(R.id.tv_banking_revenue);
        tvMomoCount = findViewById(R.id.tv_momo_count);
        tvMomoRevenue = findViewById(R.id.tv_momo_revenue);

        tvTopEmpty = findViewById(R.id.tv_top_empty);
        tvChartEmpty = findViewById(R.id.tv_chart_empty);

        chipToday = findViewById(R.id.chip_today);
        chipWeek = findViewById(R.id.chip_week);
        chipMonth = findViewById(R.id.chip_month);

        chartRevenue = findViewById(R.id.chart_revenue);
        chartPayment = findViewById(R.id.chart_payment);
    }

    private void setupChips() {
        chipToday.setOnClickListener(v -> viewModel.selectPeriod(DateRange.Period.TODAY));
        chipWeek.setOnClickListener(v -> viewModel.selectPeriod(DateRange.Period.WEEK));
        chipMonth.setOnClickListener(v -> viewModel.selectPeriod(DateRange.Period.MONTH));
    }

    private void setupCharts() {
        chartRevenue.getDescription().setEnabled(false);
        chartRevenue.setDrawGridBackground(false);
        chartRevenue.setDrawBarShadow(false);
        chartRevenue.setFitBars(true);
        chartRevenue.getLegend().setEnabled(false);
        chartRevenue.setTouchEnabled(false);
        chartRevenue.getAxisRight().setEnabled(false);
        chartRevenue.getAxisLeft().setTextColor(getColor(R.color.text_soft));
        chartRevenue.getAxisLeft().setTextSize(10f);
        chartRevenue.getAxisLeft().setAxisMinimum(0f);
        chartRevenue.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000);
                if (value >= 1_000) return String.format("%.0fK", value / 1_000);
                return String.valueOf((int) value);
            }
        });

        XAxis xAxis = chartRevenue.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(getColor(R.color.text_soft));
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1f);

        chartPayment.getDescription().setEnabled(false);
        chartPayment.setUsePercentValues(true);
        chartPayment.setDrawHoleEnabled(true);
        chartPayment.setHoleColor(Color.WHITE);
        chartPayment.setHoleRadius(45f);
        chartPayment.setTransparentCircleRadius(50f);
        chartPayment.setEntryLabelTextSize(11f);
        chartPayment.setEntryLabelColor(getColor(R.color.text_primary));
        chartPayment.getLegend().setEnabled(false);
        chartPayment.setTouchEnabled(false);
    }

    private void setupRecyclerView() {
        rvTop = findViewById(R.id.rv_top_products);
        rvTop.setLayoutManager(new LinearLayoutManager(this));
        topAdapter = new TopProductAdapter();
        rvTop.setAdapter(topAdapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        viewModel.getPeriod().observe(this, this::updateChipSelection);

        viewModel.getRevenue().observe(this, rev -> {
            tvRevenue.setText(CurrencyUtils.formatVnd(rev != null ? rev : 0));
            updateAvg();
        });

        viewModel.getOrderCount().observe(this, count -> {
            tvOrderCount.setText(String.valueOf(count != null ? count : 0));
            updateAvg();
        });

        viewModel.getTopProducts().observe(this, list -> {
            topAdapter.submitList(list);
            boolean empty = list == null || list.isEmpty();
            rvTop.setVisibility(empty ? View.GONE : View.VISIBLE);
            tvTopEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        });

        viewModel.getPaymentMethodStats().observe(this, this::updatePaymentStats);
        viewModel.getDailyRevenue().observe(this, this::updateRevenueChart);
    }

    private void updateChipSelection(DateRange.Period period) {
        chipToday.setSelected(period == DateRange.Period.TODAY);
        chipWeek.setSelected(period == DateRange.Period.WEEK);
        chipMonth.setSelected(period == DateRange.Period.MONTH);

        int onAccent = getColor(R.color.text_on_accent);
        int onSoft = getColor(R.color.text_soft);
        chipToday.setTextColor(period == DateRange.Period.TODAY ? onAccent : onSoft);
        chipWeek.setTextColor(period == DateRange.Period.WEEK ? onAccent : onSoft);
        chipMonth.setTextColor(period == DateRange.Period.MONTH ? onAccent : onSoft);
    }

    private void updateAvg() {
        Double rev = viewModel.getRevenue().getValue();
        Integer count = viewModel.getOrderCount().getValue();
        if (rev == null || count == null || count == 0) {
            tvAvgOrder.setText(CurrencyUtils.formatVnd(0));
        } else {
            tvAvgOrder.setText(CurrencyUtils.formatVnd(rev / count));
        }
    }

    private void updateRevenueChart(List<DailyRevenueRow> data) {
        if (data == null || data.isEmpty()) {
            chartRevenue.setVisibility(View.GONE);
            tvChartEmpty.setVisibility(View.VISIBLE);
            return;
        }

        chartRevenue.setVisibility(View.VISIBLE);
        tvChartEmpty.setVisibility(View.GONE);

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            entries.add(new BarEntry(i, (float) data.get(i).dailyRevenue));
            labels.add(data.get(i).dayLabel);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Doanh thu");
        dataSet.setColor(colorAccent);
        dataSet.setDrawValues(data.size() <= 7);
        dataSet.setValueTextSize(9f);
        dataSet.setValueTextColor(getColor(R.color.text_soft));
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000);
                if (value >= 1_000) return String.format("%.0fK", value / 1_000);
                return String.valueOf((int) value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        chartRevenue.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartRevenue.getXAxis().setLabelCount(Math.min(labels.size(), 7));
        chartRevenue.setData(barData);
        chartRevenue.invalidate();
    }

    private void updatePaymentStats(List<PaymentMethodStatsRow> list) {
        tvCashCount.setText("0 order");
        tvCashRevenue.setText(CurrencyUtils.formatVnd(0));
        tvBankingCount.setText("0 order");
        tvBankingRevenue.setText(CurrencyUtils.formatVnd(0));
        tvMomoCount.setText("0 order");
        tvMomoRevenue.setText(CurrencyUtils.formatVnd(0));

        if (list == null || list.isEmpty()) {
            chartPayment.setVisibility(View.GONE);
            return;
        }

        chartPayment.setVisibility(View.VISIBLE);
        List<PieEntry> pieEntries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        for (PaymentMethodStatsRow row : list) {
            if (Constants.PAYMENT_CASH.equals(row.paymentMethod)) {
                tvCashCount.setText(row.orderCount + " order");
                tvCashRevenue.setText(CurrencyUtils.formatVnd(row.totalRevenue));
                pieEntries.add(new PieEntry((float) row.totalRevenue, "Tiền mặt"));
                colors.add(colorSuccess);
            } else if (Constants.PAYMENT_BANKING.equals(row.paymentMethod)) {
                tvBankingCount.setText(row.orderCount + " order");
                tvBankingRevenue.setText(CurrencyUtils.formatVnd(row.totalRevenue));
                pieEntries.add(new PieEntry((float) row.totalRevenue, "Banking"));
                colors.add(colorInfo);
            } else if (Constants.PAYMENT_MOMO.equals(row.paymentMethod)) {
                tvMomoCount.setText(row.orderCount + " order");
                tvMomoRevenue.setText(CurrencyUtils.formatVnd(row.totalRevenue));
                pieEntries.add(new PieEntry((float) row.totalRevenue, "MoMo"));
                colors.add(colorWarning);
            }
        }

        if (pieEntries.isEmpty()) {
            chartPayment.setVisibility(View.GONE);
            return;
        }

        PieDataSet dataSet = new PieDataSet(pieEntries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.0f%%", value);
            }
        });

        PieData pieData = new PieData(dataSet);
        chartPayment.setData(pieData);
        chartPayment.invalidate();
    }
}
