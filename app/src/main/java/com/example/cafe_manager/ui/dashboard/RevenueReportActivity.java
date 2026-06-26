package com.example.cafe_manager.ui.dashboard;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.remote.RevenueReportResponse;
import com.example.cafe_manager.util.CurrencyUtils;
import com.example.cafe_manager.viewmodel.RevenueReportViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RevenueReportActivity extends AppCompatActivity {

    private RevenueReportViewModel viewModel;

    private TextView tvPeriodLabel;
    private TextView tvGrowth;
    private TextView tvTotalRevenue;
    private TextView tvOrderCount;
    private TextView tvAvgOrder;
    private TextView tvTotalItemsSold;
    private TextView tvChartTitle;

    private TextView tvMethodCashCount;
    private TextView tvMethodCashRevenue;
    private TextView tvMethodTransferCount;
    private TextView tvMethodTransferRevenue;
    private TextView tvMethodMomoCount;
    private TextView tvMethodMomoRevenue;

    private ImageButton btnPrevPeriod;
    private ImageButton btnNextPeriod;
    private Button btnViewYear;
    private BarChart chartRevenue;
    private ProgressBar progressLoading;

    private RecyclerView rvProductsSold;
    private TextView tvProductsEmpty;
    private ProductSoldAdapter productAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue_report);

        bindViews();
        setupTopBar();
        setupChart();
        setupRecyclerView();
        setupListeners();
        setupViewModel();
    }

    private void bindViews() {
        tvPeriodLabel = findViewById(R.id.tv_period_label);
        tvGrowth = findViewById(R.id.tv_growth);
        tvTotalRevenue = findViewById(R.id.tv_total_revenue);
        tvOrderCount = findViewById(R.id.tv_order_count);
        tvAvgOrder = findViewById(R.id.tv_avg_order);
        tvTotalItemsSold = findViewById(R.id.tv_total_items_sold);
        tvChartTitle = findViewById(R.id.tv_chart_title);

        tvMethodCashCount = findViewById(R.id.tv_method_cash_count);
        tvMethodCashRevenue = findViewById(R.id.tv_method_cash_revenue);
        tvMethodTransferCount = findViewById(R.id.tv_method_transfer_count);
        tvMethodTransferRevenue = findViewById(R.id.tv_method_transfer_revenue);
        tvMethodMomoCount = findViewById(R.id.tv_method_momo_count);
        tvMethodMomoRevenue = findViewById(R.id.tv_method_momo_revenue);

        btnPrevPeriod = findViewById(R.id.btn_prev_period);
        btnNextPeriod = findViewById(R.id.btn_next_period);
        btnViewYear = findViewById(R.id.btn_view_year);
        chartRevenue = findViewById(R.id.chart_daily_revenue);
        progressLoading = findViewById(R.id.progressLoading);

        rvProductsSold = findViewById(R.id.rv_products_sold);
        tvProductsEmpty = findViewById(R.id.tv_products_empty);
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        if (topBar != null) {
            TextView tvTitle = topBar.findViewById(R.id.tv_title);
            if (tvTitle != null) {
                tvTitle.setText("Báo cáo doanh thu");
            }
            View btnBack = topBar.findViewById(R.id.btn_back);
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> finish());
            }
            View btnRight = topBar.findViewById(R.id.btn_right);
            if (btnRight != null) {
                btnRight.setVisibility(View.GONE);
            }
        }
    }

    private void setupChart() {
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
    }

    private void setupRecyclerView() {
        rvProductsSold.setLayoutManager(new LinearLayoutManager(this));
        productAdapter = new ProductSoldAdapter();
        rvProductsSold.setAdapter(productAdapter);
    }

    private void setupListeners() {
        btnPrevPeriod.setOnClickListener(v -> viewModel.prevPeriod());
        btnNextPeriod.setOnClickListener(v -> viewModel.nextPeriod());
        btnViewYear.setOnClickListener(v -> viewModel.toggleMode());
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(RevenueReportViewModel.class);

        viewModel.getIsLoading().observe(this, loading -> {
            progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorMessage().observe(this, err -> {
            if (err != null) {
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
                viewModel.clearErrorMessage();
            }
        });

        viewModel.getReportData().observe(this, this::updateUi);

        viewModel.loadCurrentMonth();
    }

    private void updateUi(RevenueReportResponse data) {
        if (data == null) return;

        tvPeriodLabel.setText(viewModel.getCurrentPeriodLabel());

        boolean canNext = viewModel.canGoNext();
        btnNextPeriod.setEnabled(canNext);
        btnNextPeriod.setAlpha(canNext ? 1.0f : 0.4f);

        if (viewModel.isYearlyMode()) {
            btnViewYear.setText("Theo tháng");
            tvChartTitle.setText("Doanh thu theo tháng");
            tvGrowth.setVisibility(View.GONE);
        } else {
            btnViewYear.setText("Cả năm");
            tvChartTitle.setText("Doanh thu theo ngày");
            tvGrowth.setVisibility(View.VISIBLE);

            Double growth = data.getGrowthPercent();
            Double prevRev = data.getPreviousMonthRevenue();
            if (prevRev == null || prevRev == 0.0 || growth == null) {
                tvGrowth.setText("── Chưa có dữ liệu tháng trước");
                tvGrowth.setTextColor(getColor(R.color.text_soft));
            } else if (growth > 0.0) {
                tvGrowth.setText(String.format("▲ %.1f%% so với tháng trước", growth));
                tvGrowth.setTextColor(getColor(R.color.success));
            } else if (growth < 0.0) {
                tvGrowth.setText(String.format("▼ %.1f%% so với tháng trước", Math.abs(growth)));
                tvGrowth.setTextColor(getColor(R.color.warning));
            } else {
                tvGrowth.setText("── 0.0% so với tháng trước");
                tvGrowth.setTextColor(getColor(R.color.text_soft));
            }
        }

        tvTotalRevenue.setText(CurrencyUtils.formatVnd(data.getTotalRevenue() != null ? data.getTotalRevenue() : 0.0));
        tvOrderCount.setText((data.getOrderCount() != null ? data.getOrderCount() : 0) + " đơn");
        tvAvgOrder.setText(CurrencyUtils.formatVnd(data.getAvgOrderValue() != null ? data.getAvgOrderValue() : 0.0));
        tvTotalItemsSold.setText((data.getTotalItemsSold() != null ? data.getTotalItemsSold() : 0) + " món");

        // Payment method stats
        Map<String, Double> revByMethod = data.getRevenueByMethod();
        Map<String, Integer> countByMethod = data.getOrderCountByMethod();

        if (revByMethod != null && countByMethod != null) {
            double cashRev = revByMethod.getOrDefault("CASH", 0.0);
            int cashCount = countByMethod.getOrDefault("CASH", 0);
            tvMethodCashCount.setText(cashCount + " đơn");
            tvMethodCashRevenue.setText(CurrencyUtils.formatVnd(cashRev));

            double transRev = revByMethod.getOrDefault("TRANSFER", 0.0);
            int transCount = countByMethod.getOrDefault("TRANSFER", 0);
            tvMethodTransferCount.setText(transCount + " đơn");
            tvMethodTransferRevenue.setText(CurrencyUtils.formatVnd(transRev));

            double momoRev = revByMethod.getOrDefault("MOMO", 0.0);
            int momoCount = countByMethod.getOrDefault("MOMO", 0);
            tvMethodMomoCount.setText(momoCount + " đơn");
            tvMethodMomoRevenue.setText(CurrencyUtils.formatVnd(momoRev));
        }

        // Render products sold
        List<RevenueReportResponse.ProductSoldResponse> productsSold = data.getItemsSold();
        boolean emptyProducts = productsSold == null || productsSold.isEmpty();
        rvProductsSold.setVisibility(emptyProducts ? View.GONE : View.VISIBLE);
        tvProductsEmpty.setVisibility(emptyProducts ? View.VISIBLE : View.GONE);
        productAdapter.submitList(productsSold);

        // Render chart
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        if (viewModel.isYearlyMode()) {
            List<RevenueReportResponse.MonthlyRevenue> monthlyData = data.getRevenueByMonth();
            if (monthlyData != null) {
                for (int i = 0; i < monthlyData.size(); i++) {
                    RevenueReportResponse.MonthlyRevenue m = monthlyData.get(i);
                    double rev = m.getRevenue() != null ? m.getRevenue() : 0.0;
                    entries.add(new BarEntry(i, (float) rev));
                    labels.add("T" + m.getMonth());
                }
            }
        } else {
            List<RevenueReportResponse.DailyRevenue> dailyData = data.getRevenueByDay();
            if (dailyData != null) {
                for (int i = 0; i < dailyData.size(); i++) {
                    RevenueReportResponse.DailyRevenue d = dailyData.get(i);
                    double rev = d.getRevenue() != null ? d.getRevenue() : 0.0;
                    entries.add(new BarEntry(i, (float) rev));
                    labels.add(String.valueOf(d.getDay()));
                }
            }
        }

        BarDataSet dataSet = new BarDataSet(entries, "Doanh thu");
        dataSet.setColor(getColor(R.color.accent));
        dataSet.setDrawValues(entries.size() <= 12);
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
        barData.setBarWidth(0.5f);

        chartRevenue.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartRevenue.getXAxis().setLabelCount(Math.min(labels.size(), 12));
        chartRevenue.setData(barData);
        chartRevenue.invalidate();
    }

    private static class ProductSoldAdapter extends RecyclerView.Adapter<ProductSoldAdapter.ProductVH> {
        private final List<RevenueReportResponse.ProductSoldResponse> items = new ArrayList<>();

        public void submitList(List<RevenueReportResponse.ProductSoldResponse> list) {
            items.clear();
            if (list != null) items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ProductVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_top_product, parent, false);
            return new ProductVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProductVH holder, int position) {
            holder.bind(position + 1, items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ProductVH extends RecyclerView.ViewHolder {
            private final TextView tvRank;
            private final TextView tvName;
            private final TextView tvQty;
            private final TextView tvRevenue;

            ProductVH(View itemView) {
                super(itemView);
                tvRank = itemView.findViewById(R.id.tv_rank);
                tvName = itemView.findViewById(R.id.tv_product_name);
                tvQty = itemView.findViewById(R.id.tv_qty);
                tvRevenue = itemView.findViewById(R.id.tv_revenue);
            }

            void bind(int rank, RevenueReportResponse.ProductSoldResponse row) {
                tvRank.setText(String.valueOf(rank));
                tvName.setText(row.getProductName() != null ? row.getProductName() : "—");
                tvQty.setText("Bán: " + row.getQuantity());
                tvRevenue.setText(CurrencyUtils.formatVnd(row.getRevenue() != null ? row.getRevenue() : 0.0));
            }
        }
    }
}
