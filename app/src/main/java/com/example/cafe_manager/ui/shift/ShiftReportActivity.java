package com.example.cafe_manager.ui.shift;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.entity.ShiftCashSessionEntity;
import com.example.cafe_manager.data.repository.ShiftReportRepository;
import com.example.cafe_manager.util.CurrencyUtils;
import com.example.cafe_manager.viewmodel.ShiftReportViewModel;

public class ShiftReportActivity extends AppCompatActivity {

    public static final String EXTRA_SHIFT_ID = "extra_shift_id";

    private ShiftReportViewModel viewModel;

    private TextView tvShiftName, tvShiftTime, tvShiftStatus;
    private TextView tvTotalRevenue, tvPaidCount, tvPaymentCount;
    private TextView tvOpeningCash, tvExpectedCash, tvActualCash, tvCashDifference;
    private TextView tvUnpaidCount;
    private LinearLayout layoutUnpaid;
    private RecyclerView rvOrderHistory;
    private ShiftOrderHistoryAdapter orderHistoryAdapter;
    private TextView tvNoOrders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift_report);

        viewModel = new ViewModelProvider(this).get(ShiftReportViewModel.class);

        // Top bar
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tv_title)).setText("Báo cáo ca");

        // Bind views
        tvShiftName = findViewById(R.id.tv_shift_name);
        tvShiftTime = findViewById(R.id.tv_shift_time);
        tvShiftStatus = findViewById(R.id.tv_shift_status);
        tvTotalRevenue = findViewById(R.id.tv_total_revenue);
        tvPaidCount = findViewById(R.id.tv_paid_count);
        tvPaymentCount = findViewById(R.id.tv_payment_count);
        tvOpeningCash = findViewById(R.id.tv_opening_cash);
        tvExpectedCash = findViewById(R.id.tv_expected_cash);
        tvActualCash = findViewById(R.id.tv_actual_cash);
        tvCashDifference = findViewById(R.id.tv_cash_difference);
        tvUnpaidCount = findViewById(R.id.tv_unpaid_count);
        layoutUnpaid = findViewById(R.id.layout_unpaid);
        rvOrderHistory = findViewById(R.id.rv_order_history);
        tvNoOrders = findViewById(R.id.tv_no_orders);

        // Setup order history recyclerview
        rvOrderHistory.setLayoutManager(new LinearLayoutManager(this));
        orderHistoryAdapter = new ShiftOrderHistoryAdapter(order -> {
            Intent intent = new Intent(ShiftReportActivity.this, com.example.cafe_manager.ui.payment.InvoiceActivity.class);
            intent.putExtra(com.example.cafe_manager.ui.payment.InvoiceActivity.EXTRA_ORDER_ID, order.getOrderId());
            intent.putExtra(com.example.cafe_manager.ui.payment.InvoiceActivity.EXTRA_TABLE_NAME, order.getTableName());
            intent.putExtra("finish_only", true);
            startActivity(intent);
        });
        rvOrderHistory.setAdapter(orderHistoryAdapter);

        // Load
        int shiftId = getIntent().getIntExtra(EXTRA_SHIFT_ID, -1);
        if (shiftId == -1) {
            Toast.makeText(this, "Không xác định được ca.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        viewModel.loadReport(shiftId);

        // Observe shift info
        viewModel.getShift().observe(this, shift -> {
            if (shift != null) {
                tvShiftName.setText(shift.getShiftName());
                tvShiftTime.setText(shift.getStartTime() + " - " + shift.getEndTime());
                tvShiftStatus.setText(shift.getStatus());
            }
        });

        // Observe revenue summary
        viewModel.getSummary().observe(this, summary -> {
            if (summary != null) {
                tvTotalRevenue.setText(CurrencyUtils.formatVnd(summary.totalRevenue));
                tvPaidCount.setText(String.valueOf(summary.paidOrderCount));
                tvPaymentCount.setText(String.valueOf(summary.paymentCount));

                if (summary.unpaidOrderCount > 0) {
                    layoutUnpaid.setVisibility(View.VISIBLE);
                    tvUnpaidCount.setText("Còn " + summary.unpaidOrderCount + " đơn chưa thanh toán");
                } else {
                    layoutUnpaid.setVisibility(View.GONE);
                }
            }
        });

        // Observe cash session
        viewModel.getCashSession().observe(this, session -> {
            if (session != null) {
                tvOpeningCash.setText(CurrencyUtils.formatVnd(session.getOpeningCash()));
                tvExpectedCash.setText(CurrencyUtils.formatVnd(session.getExpectedCash()));
                tvActualCash.setText(CurrencyUtils.formatVnd(session.getActualCash()));

                double diff = session.getCashDifference();
                tvCashDifference.setText(CurrencyUtils.formatVnd(diff));

                // Đổi màu: xanh nếu dương/0, đỏ nếu âm
                if (diff < 0) {
                    tvCashDifference.setTextColor(getColor(R.color.warning));
                } else {
                    tvCashDifference.setTextColor(getColor(R.color.success));
                }
            } else {
                tvOpeningCash.setText("—");
                tvExpectedCash.setText("—");
                tvActualCash.setText("—");
                tvCashDifference.setText("—");
            }
        });

        // Observe detailed report (for order history)
        viewModel.getReportDetails().observe(this, report -> {
            if (report != null && report.getOrderHistory() != null && !report.getOrderHistory().isEmpty()) {
                orderHistoryAdapter.submitList(report.getOrderHistory());
                rvOrderHistory.setVisibility(View.VISIBLE);
                tvNoOrders.setVisibility(View.GONE);
            } else {
                rvOrderHistory.setVisibility(View.GONE);
                tvNoOrders.setVisibility(View.VISIBLE);
            }
        });

        // Observe errors
        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
    }
}
