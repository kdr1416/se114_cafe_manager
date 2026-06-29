package com.example.cafe_manager.ui.payment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.ui.table.TableActivity;
import com.example.cafe_manager.util.CurrencyUtils;
import com.example.cafe_manager.util.DateTimeUtils;
import com.example.cafe_manager.util.StatusUtils;
import com.example.cafe_manager.viewmodel.OrderDetailViewModel;

public class InvoiceActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "order_id";
    public static final String EXTRA_TABLE_NAME = "table_name";

    private OrderDetailViewModel detailVm;
    private InvoiceItemAdapter itemsAdapter;

    private int orderId = -1;
    private String tableName = "";

    private TextView tvInvoiceCode, tvInvoiceTable;
    private TextView tvSubtotal, tvDiscount, tvFinalAmount;
    private TextView tvMethod, tvPaidAt;
    private TextView tvCreatedBy, tvCashier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_invoice);

        parseIntent();
        setupTopBar();
        bindViews();
        setupItemsAdapter();
        setupViewModel();
        setupBackButton();
    }

    private void parseIntent() {
        orderId = getIntent().getIntExtra(EXTRA_ORDER_ID, -1);
        tableName = getIntent().getStringExtra(EXTRA_TABLE_NAME);
        if (tableName == null) tableName = "";
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);
        View btnRight = topBar.findViewById(R.id.btn_right);

        title.setText(R.string.title_invoice);
        caption.setText(R.string.caption_invoice);
        // Invoice là điểm cuối — back = quay về Tables luôn
        btnBack.setOnClickListener(v -> navigateBackToTables());
        btnRight.setVisibility(View.GONE);
    }

    private void bindViews() {
        tvInvoiceCode = findViewById(R.id.tv_invoice_code);
        tvInvoiceTable = findViewById(R.id.tv_invoice_table);
        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvDiscount = findViewById(R.id.tv_discount);
        tvFinalAmount = findViewById(R.id.tv_final_amount);
        tvMethod = findViewById(R.id.tv_method);
        tvPaidAt = findViewById(R.id.tv_paid_at);
        tvCreatedBy = findViewById(R.id.tv_created_by);
        tvCashier = findViewById(R.id.tv_cashier);

        tvInvoiceTable.setText(tableName);
    }

    private void setupItemsAdapter() {
        RecyclerView rv = findViewById(R.id.rv_items);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setNestedScrollingEnabled(false);
        itemsAdapter = new InvoiceItemAdapter();
        rv.setAdapter(itemsAdapter);
    }

    private void setupViewModel() {
        detailVm = new ViewModelProvider(this).get(OrderDetailViewModel.class);
        detailVm.setOrderId(orderId);

        detailVm.getOrder().observe(this, order -> {
            if (order != null) {
                tvInvoiceCode.setText("#" + order.getOrderCode());
                if (order.getCreatedByFullName() != null && !order.getCreatedByFullName().isEmpty()) {
                    tvCreatedBy.setText(order.getCreatedByFullName());
                } else {
                    tvCreatedBy.setText("Không rõ");
                }
            }
        });

        detailVm.getItems().observe(this, items -> itemsAdapter.submitList(items));

        detailVm.getPayment().observe(this, payment -> {
            if (payment == null) return;

            tvSubtotal.setText(CurrencyUtils.formatVnd(payment.getSubtotal()));
            double discount = payment.getDiscountAmount();
            tvDiscount.setText(discount > 0
                    ? "−" + CurrencyUtils.formatVnd(discount)
                    : CurrencyUtils.formatVnd(0));
            tvFinalAmount.setText(CurrencyUtils.formatVnd(payment.getFinalAmount()));

            tvMethod.setText(StatusUtils.getPaymentMethodDisplayName(
                    payment.getPaymentMethod()));
            tvPaidAt.setText(DateTimeUtils.formatDateTime(payment.getPaidAt()));

            if (payment.getCashierFullName() != null && !payment.getCashierFullName().isEmpty()) {
                tvCashier.setText(payment.getCashierFullName());
            } else {
                tvCashier.setText("Không rõ");
            }
        });
    }

    private void setupBackButton() {
        Button btn = findViewById(R.id.btn_back_to_tables);
        btn.setOnClickListener(v -> navigateBackToTables());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateBackToTables();
            }
        });
    }



    private void navigateBackToTables() {
        if (getIntent().getBooleanExtra("finish_only", false)) {
            finish();
            return;
        }
        Intent intent = new Intent(this, TableActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}