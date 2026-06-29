package com.example.cafe_manager.ui.payment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.ui.table.TableActivity;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.CurrencyUtils;
import com.example.cafe_manager.viewmodel.OrderDetailViewModel;
import com.example.cafe_manager.viewmodel.PaymentViewModel;

public class PaymentActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "order_id";
    public static final String EXTRA_TABLE_ID = "table_id";
    public static final String EXTRA_TABLE_NAME = "table_name";
    public static final String EXTRA_SUBTOTAL = "subtotal";

    private PaymentViewModel paymentVm;
    private OrderDetailViewModel detailVm;
    private InvoiceItemAdapter itemsAdapter;

    private int orderId = -1;
    private int tableId = -1;
    private String tableName = "";
    private double subtotal = 0.0;

    // Views
    private TextView tvSubtotal, tvDiscount, tvFinalAmount;
    private EditText etDiscount;
    private Button btnApply, btnConfirm, btnSelectPromo;
    private LinearLayout rowCash, rowBanking, rowMomo;
    private ImageView radioCash, radioBanking, radioMomo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_payment);

        parseIntent();
        setupTopBar();
        bindViews();
        setupItemsAdapter();
        setupPaymentRowClicks();
        setupDiscount();
        setupViewModels();
    }

    private void parseIntent() {
        orderId = getIntent().getIntExtra(EXTRA_ORDER_ID, -1);
        tableId = getIntent().getIntExtra(EXTRA_TABLE_ID, -1);
        tableName = getIntent().getStringExtra(EXTRA_TABLE_NAME);
        if (tableName == null) tableName = "";
        subtotal = getIntent().getDoubleExtra(EXTRA_SUBTOTAL, 0.0);

        if (orderId == -1 || tableId == -1) {
            Toast.makeText(this, "Thiếu thông tin order. Quay lại.",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);
        View btnRight = topBar.findViewById(R.id.btn_right);

        title.setText(getString(R.string.title_payment_with_table, tableName));
        caption.setText(R.string.caption_payment);
        btnBack.setOnClickListener(v -> finish());
        btnRight.setVisibility(View.GONE);
    }

    private void bindViews() {
        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvDiscount = findViewById(R.id.tv_discount);
        tvFinalAmount = findViewById(R.id.tv_final_amount);

        etDiscount = findViewById(R.id.et_discount);
        btnSelectPromo = findViewById(R.id.btn_select_promo);
        btnApply = findViewById(R.id.btn_apply);
        btnConfirm = findViewById(R.id.btn_confirm_payment);

        rowCash = findViewById(R.id.row_cash);
        rowBanking = findViewById(R.id.row_banking);
        rowMomo = findViewById(R.id.row_momo);
        radioCash = findViewById(R.id.radio_cash);
        radioBanking = findViewById(R.id.radio_banking);
        radioMomo = findViewById(R.id.radio_momo);
    }

    private void setupItemsAdapter() {
        RecyclerView rv = findViewById(R.id.rv_items_summary);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setNestedScrollingEnabled(false);
        itemsAdapter = new InvoiceItemAdapter();
        rv.setAdapter(itemsAdapter);
    }

    private void setupPaymentRowClicks() {
        rowCash.setOnClickListener(v ->
                paymentVm.selectPaymentMethod(Constants.PAYMENT_CASH));
        rowBanking.setOnClickListener(v ->
                paymentVm.selectPaymentMethod(Constants.PAYMENT_BANKING));
        rowMomo.setOnClickListener(v ->
                paymentVm.selectPaymentMethod(Constants.PAYMENT_MOMO));
    }

    private void setupDiscount() {
        // Áp dụng mã giảm giá hoặc số tiền raw qua nút "Áp dụng".
        // ViewModel tự xử lý: nếu là số → raw VND, nếu là mã → query DB validate.
        btnApply.setOnClickListener(v -> {
            String text = etDiscount.getText().toString().trim();
            paymentVm.applyPromotionCode(text);
        });

        btnSelectPromo.setOnClickListener(v -> showSelectPromotionDialog());
    }

    private void showSelectPromotionDialog() {
        androidx.lifecycle.Observer<java.util.List<com.example.cafe_manager.data.local.entity.PromotionEntity>> observer = new androidx.lifecycle.Observer<>() {
            @Override
            public void onChanged(java.util.List<com.example.cafe_manager.data.local.entity.PromotionEntity> list) {
                paymentVm.getAllPromotions().removeObserver(this);

                // Lọc danh sách mã giảm giá khả dụng
                java.util.List<com.example.cafe_manager.data.local.entity.PromotionEntity> activePromos = new java.util.ArrayList<>();
                long now = System.currentTimeMillis();
                if (list != null) {
                    for (com.example.cafe_manager.data.local.entity.PromotionEntity p : list) {
                        if (p.isActive() && (p.getExpiresAt() == 0 || p.getExpiresAt() >= now)) {
                            activePromos.add(p);
                        }
                    }
                }

                View view = android.view.LayoutInflater.from(PaymentActivity.this)
                        .inflate(R.layout.dialog_select_promotion, null);

                androidx.recyclerview.widget.RecyclerView rv = view.findViewById(R.id.rv_promotions_select);
                TextView tvEmpty = view.findViewById(R.id.tv_empty);

                android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(PaymentActivity.this)
                        .setView(view)
                        .setNegativeButton("Huỷ", null)
                        .create();

                if (activePromos.isEmpty()) {
                    rv.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    rv.setVisibility(View.VISIBLE);
                    tvEmpty.setVisibility(View.GONE);
                    rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(PaymentActivity.this));
                    com.example.cafe_manager.ui.promotion.PromotionSelectAdapter selectAdapter =
                            new com.example.cafe_manager.ui.promotion.PromotionSelectAdapter(p -> {
                                etDiscount.setText(p.getCode());
                                paymentVm.applyPromotionCode(p.getCode());
                                dialog.dismiss();
                            });
                    rv.setAdapter(selectAdapter);
                    selectAdapter.submitList(activePromos);
                }

                dialog.show();
            }
        };
        paymentVm.getAllPromotions().observe(this, observer);
    }

    private void setupViewModels() {
        paymentVm = new ViewModelProvider(this).get(PaymentViewModel.class);
        detailVm = new ViewModelProvider(this).get(OrderDetailViewModel.class);

        paymentVm.setOrderInfo(orderId, tableId, subtotal);
        detailVm.setOrderId(orderId);

        // Items summary
        detailVm.getItems().observe(this, items -> itemsAdapter.submitList(items));

        // Amounts
        paymentVm.getSubtotal().observe(this, v ->
                tvSubtotal.setText(CurrencyUtils.formatVnd(v != null ? v : 0)));
        paymentVm.getDiscountAmount().observe(this, v -> {
            double d = v != null ? v : 0;
            tvDiscount.setText(d > 0
                    ? "−" + CurrencyUtils.formatVnd(d)
                    : CurrencyUtils.formatVnd(0));
        });
        paymentVm.getFinalAmount().observe(this, v ->
                tvFinalAmount.setText(CurrencyUtils.formatVnd(v != null ? v : 0)));

        // Payment method selection → update radio UI
        paymentVm.getSelectedPaymentMethod().observe(this, this::updateMethodRadios);

        // Loading
        paymentVm.getLoading().observe(this, loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            btnConfirm.setEnabled(!isLoading);
            btnConfirm.setText(isLoading
                    ? "Đang xử lý..."
                    : getString(R.string.btn_confirm_payment));
        });

        // Success → Hiện hộp thoại thành công & quay về màn hình bàn ăn
        paymentVm.getPaySuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                paymentVm.clearPaySuccess();
                showPaymentSuccessDialog();
            }
        });

        // Error
        paymentVm.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                paymentVm.clearErrorMessage();
            }
        });

        // Promo message (success or invalid)
        paymentVm.getPromoMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                paymentVm.clearPromoMessage();
            }
        });

        // Confirm button
        btnConfirm.setOnClickListener(v -> paymentVm.confirmPayment());
    }

    private void showPaymentSuccessDialog() {
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Thành công")
                .setMessage("Thanh toán thành công!")
                .setCancelable(false)
                .setPositiveButton("OK", (d, which) -> {
                    navigateToTableActivity();
                })
                .create();

        dialog.show();

        // Tự động chuyển hướng sau 3 giây (3000ms)
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing() && dialog.isShowing()) {
                dialog.dismiss();
                navigateToTableActivity();
            }
        }, 3000);
    }

    private void navigateToTableActivity() {
        Intent intent = new Intent(this, TableActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void updateMethodRadios(String selectedMethod) {
        applyRadioStyle(rowCash, radioCash,
                Constants.PAYMENT_CASH.equals(selectedMethod));
        applyRadioStyle(rowBanking, radioBanking,
                Constants.PAYMENT_BANKING.equals(selectedMethod));
        applyRadioStyle(rowMomo, radioMomo,
                Constants.PAYMENT_MOMO.equals(selectedMethod));
    }

    private void applyRadioStyle(View row, ImageView radio, boolean selected) {
        row.setBackgroundResource(selected
                ? R.drawable.bg_card_soft
                : R.drawable.bg_card);
        // Hiển thị dot khi selected. Đơn giản: dùng ic_check khi selected.
        radio.setImageResource(selected
                ? R.drawable.ic_check
                : 0);
        radio.setColorFilter(selected
                ? getColor(R.color.accent)
                : getColor(R.color.text_mute));
    }
}
