package com.example.cafe_manager.ui.order;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.ui.table.TableActivity;
import com.example.cafe_manager.util.CurrencyUtils;
import com.example.cafe_manager.viewmodel.OrderViewModel;

public class OrderActivity extends AppCompatActivity {

    private OrderViewModel viewModel;
    private OrderItemAdapter adapter;

    private String tableName = "";

    private RecyclerView rvItems;
    private View emptyState;
    private TextView tvTotal;
    private Button btnConfirm;
    
    private EditText edtNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_order);

        parseIntent();
        setupTopBar();
        setupFooter();
        setupExtras();
        setupEmptyState();
        setupRecyclerView();
        setupViewModel();
    }

    private void parseIntent() {
        tableName = getIntent().getStringExtra(TableActivity.EXTRA_TABLE_NAME);
        if (tableName == null) tableName = "";
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);
        View btnRight = topBar.findViewById(R.id.btn_right);

        title.setText(getString(R.string.title_order_with_table, tableName));
        caption.setText(R.string.caption_order);
        btnBack.setOnClickListener(v -> finish());
        btnRight.setVisibility(View.GONE);
    }

    private void setupFooter() {
        View footer = findViewById(R.id.footer);
        TextView tvLabel = footer.findViewById(R.id.tv_total_label);
        tvTotal = footer.findViewById(R.id.tv_total_amount);
        btnConfirm = footer.findViewById(R.id.btn_primary);

        tvLabel.setText(R.string.label_total);

        // Button text + caption tuỳ mode
        if (com.example.cafe_manager.manager.CartManager.getInstance().isAddMode()) {
            btnConfirm.setText("Thêm vào order");
        } else {
            btnConfirm.setText(R.string.btn_confirm_order);
        }
        btnConfirm.setOnClickListener(v -> onConfirmClicked());
    }

    private void setupExtras() {
        edtNote = findViewById(R.id.edt_order_note);
    }

    private void setupEmptyState() {
        emptyState = findViewById(R.id.empty_state);
        TextView tvMsg = emptyState.findViewById(R.id.tv_empty_message);
        tvMsg.setText(R.string.empty_order);
    }

    private void setupRecyclerView() {
        rvItems = findViewById(R.id.rv_order_items);
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderItemAdapter(new OrderItemAdapter.OnQuantityChangeListener() {
            @Override
            public void onIncrease(int productId) {
                viewModel.increaseQuantity(productId);
            }
            @Override
            public void onDecrease(int productId) {
                viewModel.decreaseQuantity(productId);
            }
        });
        rvItems.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(OrderViewModel.class);

        viewModel.getCartItems().observe(this, items -> {
            adapter.submitList(items);

            boolean empty = items == null || items.isEmpty();
            rvItems.setVisibility(empty ? View.GONE : View.VISIBLE);
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            btnConfirm.setEnabled(!empty);
            
            View extras = findViewById(R.id.layout_extras);
            if (extras != null) {
                extras.setVisibility(empty ? View.GONE : View.VISIBLE);
            }
        });

        viewModel.getTotalAmount().observe(this, amount -> {
            tvTotal.setText(CurrencyUtils.formatVnd(amount != null ? amount : 0));
        });

        viewModel.getLoading().observe(this, loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            btnConfirm.setEnabled(!isLoading && !viewModel.isCartEmpty());
            if (isLoading) {
                btnConfirm.setText("Đang xử lý...");
            } else if (viewModel.isAddMode()) {
                btnConfirm.setText("Thêm vào order");
            } else {
                btnConfirm.setText(getString(R.string.btn_confirm_order));
            }
        });

        viewModel.getConfirmSuccess().observe(this, orderId -> {
            if (orderId != null) {
                Toast.makeText(this, R.string.msg_order_confirmed,
                        Toast.LENGTH_SHORT).show();
                viewModel.clearConfirmSuccess();
                navigateBackToTables();
            }
        });

        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                viewModel.clearErrorMessage();
            }
        });
    }

    private void onConfirmClicked() {
        if (viewModel.isCartEmpty()) {
            return;
        }
        if (viewModel.isAddMode()) {
            // Mode "gọi thêm món" — gắn vào order existing
            viewModel.addItemsToExistingOrder();
        } else {
            // Order mới
            String note = edtNote.getText().toString().trim();
            viewModel.confirmOrder(note);
        }
    }

    private void navigateBackToTables() {
        Intent intent = new Intent(this, TableActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
