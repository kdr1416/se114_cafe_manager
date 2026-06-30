package com.example.cafe_manager.ui.menu;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
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
import com.example.cafe_manager.manager.CartManager;
import com.example.cafe_manager.ui.order.OrderActivity;
import com.example.cafe_manager.ui.table.TableActivity;
import com.example.cafe_manager.util.CurrencyUtils;
import com.example.cafe_manager.viewmodel.MenuViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MenuActivity extends AppCompatActivity {

    private MenuViewModel viewModel;
    private CategoryAdapter categoryAdapter;
    private ProductAdapter productAdapter;

    private int tableId = -1;
    private String tableName = "";

    private View cartBar;
    private TextView tvCartCount;
    private TextView tvCartTotal;
    private EditText etSearch;
    private View emptyState;
    private RecyclerView rvProducts;

    private int currentCategoryId = 0;
    private String currentQuery = "";
    private List<ProductEntity> currentProducts = new ArrayList<>();

    private LiveData<List<ProductEntity>> currentProductsLiveData;
    private final Observer<List<ProductEntity>> productsObserver = list -> {
        currentProducts = list != null ? list : new ArrayList<>();
        applyFilter();
    };

    private Handler pollingHandler;
    private Runnable pollingRunnable;
    private static final long POLLING_INTERVAL_MS = 5000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_menu);

        parseIntent();
        setupTopBar();
        setupSearch();
        setupEmptyState();
        setupCartBar();
        setupRecyclerViews();
        setupViewModel();
        setupPolling();
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

        if (CartManager.getInstance().isAddMode()) {
            title.setText("Gọi thêm món · " + tableName);
            caption.setText("Thêm vào order đang phục vụ");
        } else {
            title.setText(getString(R.string.title_menu_with_table, tableName));
            caption.setText(R.string.caption_menu);
        }
        btnBack.setOnClickListener(v -> finish());
        btnRight.setVisibility(View.GONE);
    }

    private void setupSearch() {
        etSearch = findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s == null ? "" : s.toString().trim();
                applyFilter();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupEmptyState() {
        emptyState = findViewById(R.id.empty_state);
        TextView msg = emptyState.findViewById(R.id.tv_empty_message);
        msg.setText("Không có món nào phù hợp.");
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

        cartBar.setVisibility(View.GONE);
    }

    private void setupRecyclerViews() {
        RecyclerView rvCategories = findViewById(R.id.rv_categories);
        rvCategories.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        categoryAdapter = new CategoryAdapter(this::onCategorySelected);
        rvCategories.setAdapter(categoryAdapter);

        rvProducts = findViewById(R.id.rv_products);
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        productAdapter = new ProductAdapter(new ProductAdapter.OnProductQtyChangeListener() {
            @Override
            public void onIncrease(ProductEntity product) {
                viewModel.increaseQuantity(product);
            }

            @Override
            public void onDecrease(ProductEntity product) {
                viewModel.decreaseQuantity(product);
            }
        });
        rvProducts.setAdapter(productAdapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MenuViewModel.class);
        viewModel.setCurrentTable(tableId);

        viewModel.getCategories().observe(this, list -> {
            categoryAdapter.submitList(list);
            productAdapter.setCategoryMap(list);
        });

        switchProductsSource(0);

        viewModel.getCartTotalQuantityLiveData().observe(this, q -> {
            updateCartBar(q, viewModel.getCartTotalAmountLiveData().getValue());
            productAdapter.notifyDataSetChanged();
        });
        viewModel.getCartTotalAmountLiveData().observe(this, a -> {
            updateCartBar(viewModel.getCartTotalQuantityLiveData().getValue(), a);
            productAdapter.notifyDataSetChanged();
        });
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

    /** Apply search filter trên top của category-filtered list. */
    private void applyFilter() {
        List<ProductEntity> filtered;

        if (currentQuery.isEmpty()) {
            filtered = currentProducts;
        } else {
            filtered = new ArrayList<>();
            String q = currentQuery.toLowerCase(Locale.ROOT);
            for (ProductEntity p : currentProducts) {
                if (p.getProductName() != null
                        && p.getProductName().toLowerCase(Locale.ROOT).contains(q)) {
                    filtered.add(p);
                }
            }
        }

        productAdapter.submitList(filtered);

        boolean empty = filtered.isEmpty();
        rvProducts.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
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

    private void setupPolling() {
        pollingHandler = new Handler(Looper.getMainLooper());
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                refreshData();
                pollingHandler.postDelayed(this, POLLING_INTERVAL_MS);
            }
        };
    }

    private void refreshData() {
        if (viewModel != null) {
            viewModel.refreshCategories();
            viewModel.refreshProducts();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refreshCartBar();
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.post(pollingRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
    }
}
