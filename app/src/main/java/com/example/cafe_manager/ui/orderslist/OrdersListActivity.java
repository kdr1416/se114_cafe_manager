package com.example.cafe_manager.ui.orderslist;

import android.content.Intent;
import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.model.OrderWithItems;
import com.example.cafe_manager.ui.common.BaseActivity;
import com.example.cafe_manager.ui.payment.PaymentActivity;
import com.example.cafe_manager.viewmodel.OrdersListViewModel;

public class OrdersListActivity extends BaseActivity {

    private OrdersListViewModel viewModel;
    private OrdersListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders_list);

        RecyclerView rvOrders = findViewById(R.id.rv_orders);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));

        adapter = new OrdersListAdapter(this::openPayment);
        rvOrders.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(OrdersListViewModel.class);
        viewModel.getOrders().observe(this, orders -> {
            adapter.submitList(orders);
        });
    }

    private void openPayment(OrderWithItems order) {
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("order_id", order.getOrder().getOrderId());
        startActivity(intent);
    }
}
