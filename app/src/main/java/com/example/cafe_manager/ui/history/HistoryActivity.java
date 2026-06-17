package com.example.cafe_manager.ui.history;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.model.OrderWithItems;
import com.example.cafe_manager.ui.payment.InvoiceActivity;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.PermissionUtils;
import com.example.cafe_manager.util.CurrencyUtils;
import com.example.cafe_manager.util.DateRange;
import com.example.cafe_manager.viewmodel.HistoryViewModel;

public class HistoryActivity extends AppCompatActivity {

    private HistoryViewModel viewModel;
    private HistoryAdapter adapter;

    private TextView tvRevenue;
    private TextView tvOrderCount;
    private View emptyState;
    private RecyclerView rvHistory;

    private TextView chipToday, chipWeek, chipMonth, chipAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PermissionUtils.requireRole(this, Constants.ROLE_ADMIN, Constants.ROLE_MANAGER)) return;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);

        setupTopBar();
        bindViews();
        setupChips();
        setupRecyclerView();
        setupViewModel();
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);
        View btnRight = topBar.findViewById(R.id.btn_right);

        title.setText("Lịch sử giao dịch");
        caption.setText("Các order đã thanh toán");
        btnBack.setOnClickListener(v -> finish());
        btnRight.setVisibility(View.GONE);
    }

    private void bindViews() {
        tvRevenue = findViewById(R.id.tv_revenue);
        tvOrderCount = findViewById(R.id.tv_order_count);
        emptyState = findViewById(R.id.empty_state);
        TextView msg = emptyState.findViewById(R.id.tv_empty_message);
        msg.setText("Không có giao dịch nào trong khoảng này.");

        chipToday = findViewById(R.id.chip_today);
        chipWeek = findViewById(R.id.chip_week);
        chipMonth = findViewById(R.id.chip_month);
        chipAll = findViewById(R.id.chip_all);
    }

    private void setupChips() {
        chipToday.setOnClickListener(v -> viewModel.selectPeriod(DateRange.Period.TODAY));
        chipWeek.setOnClickListener(v -> viewModel.selectPeriod(DateRange.Period.WEEK));
        chipMonth.setOnClickListener(v -> viewModel.selectPeriod(DateRange.Period.MONTH));
        chipAll.setOnClickListener(v -> viewModel.selectPeriod(DateRange.Period.ALL));
    }

    private void updateChipSelection(DateRange.Period period) {
        chipToday.setSelected(period == DateRange.Period.TODAY);
        chipWeek.setSelected(period == DateRange.Period.WEEK);
        chipMonth.setSelected(period == DateRange.Period.MONTH);
        chipAll.setSelected(period == DateRange.Period.ALL);

        int onAccent = getColor(R.color.text_on_accent);
        int onSoft = getColor(R.color.text_soft);
        chipToday.setTextColor(period == DateRange.Period.TODAY ? onAccent : onSoft);
        chipWeek.setTextColor(period == DateRange.Period.WEEK ? onAccent : onSoft);
        chipMonth.setTextColor(period == DateRange.Period.MONTH ? onAccent : onSoft);
        chipAll.setTextColor(period == DateRange.Period.ALL ? onAccent : onSoft);
    }

    private void setupRecyclerView() {
        rvHistory = findViewById(R.id.rv_history);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(this::onHistoryClicked);
        rvHistory.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        viewModel.getPeriod().observe(this, this::updateChipSelection);

        viewModel.getOrders().observe(this, list -> {
            adapter.submitList(list);
            boolean empty = list == null || list.isEmpty();
            rvHistory.setVisibility(empty ? View.GONE : View.VISIBLE);
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        });

        viewModel.getRevenue().observe(this, rev ->
                tvRevenue.setText(CurrencyUtils.formatVnd(rev != null ? rev : 0)));

        viewModel.getOrderCount().observe(this, count ->
                tvOrderCount.setText(String.valueOf(count != null ? count : 0)));
    }

    private void onHistoryClicked(OrderWithItems data) {
        Intent intent = new Intent(this, InvoiceActivity.class);
        intent.putExtra(InvoiceActivity.EXTRA_ORDER_ID, data.getOrder().getOrderId());
        intent.putExtra(InvoiceActivity.EXTRA_TABLE_NAME,
                "Bàn #" + data.getOrder().getTableId());
        startActivity(intent);
    }
}
