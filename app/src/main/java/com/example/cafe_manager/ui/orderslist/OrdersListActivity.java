package com.example.cafe_manager.ui.orderslist;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.repository.OrderRepository;
import com.example.cafe_manager.manager.CartManager;
import com.example.cafe_manager.model.OrderWithItems;
import com.example.cafe_manager.ui.admin.AdminMenuActivity;
import com.example.cafe_manager.ui.menu.MenuActivity;
import com.example.cafe_manager.ui.payment.PaymentActivity;
import com.example.cafe_manager.ui.table.TableActivity;
import com.example.cafe_manager.util.RepositoryCallback;
import com.example.cafe_manager.viewmodel.OrdersListViewModel;

public class OrdersListActivity extends AppCompatActivity {

    private OrdersListViewModel viewModel;
    private OrdersListAdapter adapter;
    private OrderRepository orderRepository;

    private TextView tvActiveCount;
    private TextView tvAwaitingCount;
    private View emptyState;
    private RecyclerView rvOrders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_orders_list);

        setupTopBar();
        setupBottomNav();
        bindViews();
        setupRecyclerView();
        setupViewModel();
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);
        View btnRight = topBar.findViewById(R.id.btn_right);

        title.setText(R.string.title_orders_list);
        caption.setText(R.string.caption_orders_list);
        btnBack.setVisibility(View.GONE);
        btnRight.setVisibility(View.VISIBLE);
    }

    private void setupBottomNav() {
        View bottomNav = findViewById(R.id.bottom_nav);
        View navTables = bottomNav.findViewById(R.id.nav_tables);
        View navOrders = bottomNav.findViewById(R.id.nav_orders);
        View navMenu = bottomNav.findViewById(R.id.nav_menu);

        navOrders.setSelected(true);

        navTables.setOnClickListener(v -> {
            Intent intent = new Intent(this, TableActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        navMenu.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminMenuActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void bindViews() {
        tvActiveCount = findViewById(R.id.tv_active_count);
        tvAwaitingCount = findViewById(R.id.tv_awaiting_count);
        emptyState = findViewById(R.id.empty_state);
        TextView msg = emptyState.findViewById(R.id.tv_empty_message);
        msg.setText("Chưa có order nào đang phục vụ.");
    }

    private void setupRecyclerView() {
        rvOrders = findViewById(R.id.rv_orders);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrdersListAdapter(
                this::onCollectClicked,
                this::onCancelClicked,
                this::onAddMoreClicked
        );
        rvOrders.setAdapter(adapter);
    }

    private void setupViewModel() {
        orderRepository = new OrderRepository(this);
        viewModel = new ViewModelProvider(this).get(OrdersListViewModel.class);

        viewModel.getOrders().observe(this, orders -> {
            adapter.submitList(orders);

            boolean empty = orders == null || orders.isEmpty();
            rvOrders.setVisibility(empty ? View.GONE : View.VISIBLE);
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        });

        viewModel.getActiveCount().observe(this, count -> {
            String c = String.valueOf(count != null ? count : 0);
            tvActiveCount.setText(c);
            // MVP: cùng status CONFIRMED → 2 stat trùng. Nếu muốn phân biệt, lọc theo thời gian.
            tvAwaitingCount.setText(c);
        });
    }

    private void onCollectClicked(OrderWithItems data) {
        double subtotal = OrdersListViewModel.calculateTotal(data.getItems());

        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra(PaymentActivity.EXTRA_ORDER_ID, data.getOrder().getOrderId());
        intent.putExtra(PaymentActivity.EXTRA_TABLE_ID, data.getOrder().getTableId());
        intent.putExtra(PaymentActivity.EXTRA_TABLE_NAME,
                "Bàn #" + data.getOrder().getTableId());
        intent.putExtra(PaymentActivity.EXTRA_SUBTOTAL, subtotal);
        startActivity(intent);
    }

    private void onAddMoreClicked(OrderWithItems data) {
        // Setup CartManager để MenuActivity hiểu là đang "thêm món" vào order existing.
        CartManager cm = CartManager.getInstance();
        cm.clearCart();
        cm.setCurrentTableId(data.getOrder().getTableId());
        cm.setPendingOrderId(data.getOrder().getOrderId());

        Intent intent = new Intent(this, MenuActivity.class);
        intent.putExtra(TableActivity.EXTRA_TABLE_ID, data.getOrder().getTableId());
        intent.putExtra(TableActivity.EXTRA_TABLE_NAME,
                "Bàn #" + data.getOrder().getTableId());
        startActivity(intent);
    }

    private void onCancelClicked(OrderWithItems data) {
        new AlertDialog.Builder(this)
                .setTitle("Hủy order?")
                .setMessage("Order #" + data.getOrder().getOrderCode()
                        + " sẽ bị hủy và bàn quay về trống. Không thể hoàn tác.")
                .setPositiveButton("Hủy order", (d, w) ->
                        confirmCancelOrder(data))
                .setNegativeButton("Thoát", null)
                .show();
    }

    private void confirmCancelOrder(OrderWithItems data) {
        orderRepository.cancelOrder(
                data.getOrder().getOrderId(),
                data.getOrder().getTableId(),
                new RepositoryCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        Toast.makeText(OrdersListActivity.this,
                                "Đã hủy order", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(OrdersListActivity.this,
                                "Hủy order thất bại: "
                                        + (e != null ? e.getMessage() : ""),
                                Toast.LENGTH_LONG).show();
                    }
                }
        );
    }
}
