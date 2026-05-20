package com.example.cafe_manager.ui.menu;

import android.content.Intent;

import android.os.Bundle;

import android.view.View;

import android.widget.TextView;

import androidx.activity.EdgeToEdge;

import androidx.appcompat.app.AppCompatActivity;

import androidx.lifecycle.LiveData;

import androidx.lifecycle.Observer;

import androidx.lifecycle.ViewModelProvider;

import androidx.recyclerview.widget.GridLayoutManager;

import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;

import com.example.cafe_manager.data.local.entity.ProductEntity;

import com.example.cafe_manager.ui.order.OrderActivity;

import com.example.cafe_manager.ui.table.TableActivity;

import com.example.cafe_manager.util.CurrencyUtils;

import com.example.cafe_manager.viewmodel.MenuViewModel;

import java.util.List;

public class MenuActivity extends AppCompatActivity {

    private MenuViewModel viewModel;

    private CategoryAdapter categoryAdapter;

    private ProductAdapter productAdapter;

    private int tableId = -1;

    private String tableName = "";

    private View cartBar;

    private TextView tvCartCount;

    private TextView tvCartTotal;

    private int currentCategoryId = 0;

    private LiveData<List<ProductEntity>> currentProductsLiveData;

    private final Observer<List<ProductEntity>> productsObserver =

            list -> productAdapter.submitList(list);

    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_menu);

        parseIntent();

        setupTopBar();

        setupCartBar();

        setupRecyclerViews();

        setupViewModel();

    }

    private void parseIntent() {

        tableId = getIntent().getIntExtra(TableActivity.EXTRA_TABLE_ID, -1);

        tableName = getIntent().getStringExtra(TableActivity.EXTRA_TABLE_NAME);

        if (tableName == null) tableName = "";

    }

    private void setupTopBar() {

        View topBar = findViewById(R.id.top_bar);

        TextView title = topBar.findViewById(R.id.tv_title);

        TextView caption = topBar.findViewById(R.id.tv_caption);

        View btnBack = topBar.findViewById(R.id.btn_back);

        View btnRight = topBar.findViewById(R.id.btn_right);

        title.setText(getString(R.string.title_menu_with_table, tableName));

        caption.setText(R.string.caption_menu);

        btnBack.setOnClickListener(v -> finish());

        btnRight.setVisibility(View.VISIBLE);  // search icon (MVP chưa wire)

    }

    private void setupCartBar() {

        cartBar = findViewById(R.id.cart_bar);

        tvCartCount = cartBar.findViewById(R.id.tv_cart_count);

        tvCartTotal = cartBar.findViewById(R.id.tv_cart_total);

        View btnViewOrder = cartBar.findViewById(R.id.btn_view_order);

        btnViewOrder.setOnClickListener(v -> {

            Intent intent = new Intent(this, OrderActivity.class);

            intent.putExtra(TableActivity.EXTRA_TABLE_ID, tableId);

            intent.putExtra(TableActivity.EXTRA_TABLE_NAME, tableName);

            startActivity(intent);

        });

        cartBar.setVisibility(View.GONE);  // ẩn cho đến khi cart có món

    }

    private void setupRecyclerViews() {

        RecyclerView rvCategories = findViewById(R.id.rv_categories);

        rvCategories.setLayoutManager(

                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        categoryAdapter = new CategoryAdapter(this::onCategorySelected);

        rvCategories.setAdapter(categoryAdapter);

        RecyclerView rvProducts = findViewById(R.id.rv_products);

        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));

        productAdapter = new ProductAdapter(this::onAddToCart);

        rvProducts.setAdapter(productAdapter);

    }

    private void setupViewModel() {

        viewModel = new ViewModelProvider(this).get(MenuViewModel.class);

        viewModel.setCurrentTable(tableId);

        viewModel.getCategories().observe(this, list -> categoryAdapter.submitList(list));

        switchProductsSource(0);  // default = Tất cả

        viewModel.getCartTotalQuantityLiveData().observe(this,

                q -> updateCartBar(q, viewModel.getCartTotalAmountLiveData().getValue()));

        viewModel.getCartTotalAmountLiveData().observe(this,

                a -> updateCartBar(viewModel.getCartTotalQuantityLiveData().getValue(), a));

    }

    private void onCategorySelected(int categoryId) {

        if (currentCategoryId == categoryId) return;

        currentCategoryId = categoryId;

        categoryAdapter.setSelectedCategoryId(categoryId);

        switchProductsSource(categoryId);

    }

    private void switchProductsSource(int categoryId) {

        if (currentProductsLiveData != null) {

            currentProductsLiveData.removeObserver(productsObserver);

        }

        currentProductsLiveData = (categoryId == 0)

                ? viewModel.getAllProducts()

                : viewModel.getProductsByCategory(categoryId);

        currentProductsLiveData.observe(this, productsObserver);

    }

    private void onAddToCart(ProductEntity product) {

        viewModel.addToCart(product);

    }

    private void updateCartBar(Integer quantity, Double amount) {

        int q = quantity != null ? quantity : 0;

        double a = amount != null ? amount : 0;

        if (q <= 0) {

            cartBar.setVisibility(View.GONE);

            return;

        }

        cartBar.setVisibility(View.VISIBLE);

        tvCartCount.setText(getString(R.string.format_items_selected, q));

        tvCartTotal.setText(CurrencyUtils.formatVnd(a));

    }

    @Override

    protected void onResume() {

        super.onResume();

        // Cart là singleton; có thể đã đổi khi quay lại từ OrderActivity (xoá món, confirm)

        viewModel.refreshCartBar();

    }

}
