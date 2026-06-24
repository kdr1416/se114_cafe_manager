package com.example.cafe_manager.ui.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.entity.CategoryEntity;
import com.example.cafe_manager.data.local.entity.ProductEntity;
import com.example.cafe_manager.ui.menu.CategoryAdapter;
import com.example.cafe_manager.ui.orderslist.OrdersListActivity;
import com.example.cafe_manager.ui.dashboard.DashboardActivity;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.ui.table.TableActivity;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.PermissionUtils;
import com.example.cafe_manager.viewmodel.AdminMenuViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminMenuActivity extends AppCompatActivity {

    private AdminMenuViewModel viewModel;
    private CategoryAdapter categoryAdapter;
    private AdminProductAdapter productAdapter;

    private TextView tvTotalCount;
    private TextView tvVisibleCount;
    private TextView tvHiddenHint;
    private EditText etSearch;
    private RecyclerView rvProducts;
    private View emptyState;

    private List<CategoryEntity> currentCategories = new ArrayList<>();
    private List<ProductEntity> currentProducts = new ArrayList<>();
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PermissionUtils.requireRole(this, Constants.ROLE_ADMIN, Constants.ROLE_MANAGER, Constants.ROLE_STAFF)) return;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_menu);

        setupTopBar();
        setupBottomNav();
        bindViews();
        setupSearch();
        setupEmptyState();
        setupRecyclerViews();
        setupViewModel();
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);
        ImageButton btnRight = topBar.findViewById(R.id.btn_right);

        title.setText(R.string.title_admin_menu);
        caption.setText(R.string.caption_admin_menu);
        btnBack.setVisibility(View.GONE);

        boolean isStaff = SessionManager.getInstance(this).isStaff();
        if (isStaff) {
            btnRight.setVisibility(View.GONE);
        } else {
            btnRight.setVisibility(View.VISIBLE);
            btnRight.setImageResource(R.drawable.ic_plus);
            btnRight.setOnClickListener(v -> showProductDialog(null));
        }
    }

    private void setupBottomNav() {
        View bottomNav = findViewById(R.id.bottom_nav);
        View navTables = bottomNav.findViewById(R.id.nav_tables);
        View navOrders = bottomNav.findViewById(R.id.nav_orders);
        View navMenu = bottomNav.findViewById(R.id.nav_menu);
        View navDashboard = bottomNav.findViewById(R.id.nav_dashboard);

        navMenu.setSelected(true);
        navDashboard.setSelected(false);

        navTables.setOnClickListener(v -> {
            Intent intent = new Intent(this, TableActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        navOrders.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrdersListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        navDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void bindViews() {
        tvTotalCount = findViewById(R.id.tv_total_count);
        tvVisibleCount = findViewById(R.id.tv_visible_count);
        tvHiddenHint = findViewById(R.id.tv_hidden_hint);
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

    private void setupRecyclerViews() {
        RecyclerView rvCategories = findViewById(R.id.rv_categories);
        rvCategories.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        categoryAdapter = new CategoryAdapter(this::onCategorySelected);
        rvCategories.setAdapter(categoryAdapter);

        rvProducts = findViewById(R.id.rv_products);
        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        productAdapter = new AdminProductAdapter(new AdminProductAdapter.OnActionListener() {
            @Override
            public void onToggleVisibility(ProductEntity product) {
                viewModel.toggleActive(product.getProductId(), !product.isActive());
            }
            @Override
            public void onEdit(ProductEntity product) {
                showProductDialog(product);
            }
        });
        rvProducts.setAdapter(productAdapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(AdminMenuViewModel.class);

        viewModel.getCategories().observe(this, list -> {
            currentCategories = list != null ? list : new ArrayList<>();
            categoryAdapter.submitList(currentCategories);
            productAdapter.setCategoryMap(currentCategories);
        });

        viewModel.getProducts().observe(this, list -> {
            currentProducts = list != null ? list : new ArrayList<>();
            applyFilter();
        });

        viewModel.getTotalCount().observe(this, count -> {
            tvTotalCount.setText(String.valueOf(count != null ? count : 0));
            updateHiddenHint();
        });

        viewModel.getVisibleCount().observe(this, count -> {
            tvVisibleCount.setText(String.valueOf(count != null ? count : 0));
            updateHiddenHint();
        });
    }

    private void updateHiddenHint() {
        Integer total = viewModel.getTotalCount().getValue();
        Integer visible = viewModel.getVisibleCount().getValue();
        if (total == null || visible == null) return;

        int hidden = total - visible;
        if (hidden <= 0) {
            tvHiddenHint.setText("Tất cả đang hiển thị");
        } else {
            tvHiddenHint.setText(hidden + " món đang ẩn");
        }
    }

    private void onCategorySelected(int categoryId) {
        categoryAdapter.setSelectedCategoryId(categoryId);
        viewModel.selectCategory(categoryId);
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

    private void showProductDialog(@Nullable ProductEntity existing) {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_product_form, null);

        EditText etName = view.findViewById(R.id.et_name);
        EditText etPrice = view.findViewById(R.id.et_price);
        Spinner spinner = view.findViewById(R.id.spinner_category);

        if (currentCategories.isEmpty()) {
            Toast.makeText(this,
                    "Chưa có danh mục. Tạo danh mục trước.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        List<String> categoryNames = new ArrayList<>();
        for (CategoryEntity c : currentCategories) {
            categoryNames.add(c.getCategoryName());
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                categoryNames
        );
        spinner.setAdapter(spinnerAdapter);

        if (existing != null) {
            etName.setText(existing.getProductName());
            etPrice.setText(String.valueOf((long) existing.getPrice()));

            for (int i = 0; i < currentCategories.size(); i++) {
                if (currentCategories.get(i).getCategoryId() == existing.getCategoryId()) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }

        String title = existing == null
                ? getString(R.string.btn_add_product)
                : getString(R.string.btn_edit_product);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(view)
                .setPositiveButton(R.string.btn_save, (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String priceStr = etPrice.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(this, "Tên món không được rỗng",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double price;
                    try {
                        price = Double.parseDouble(priceStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Giá không hợp lệ",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (price <= 0) {
                        Toast.makeText(this, "Giá phải lớn hơn 0",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int spinnerPos = spinner.getSelectedItemPosition();
                    if (spinnerPos < 0 || spinnerPos >= currentCategories.size()) {
                        return;
                    }
                    int categoryId = currentCategories.get(spinnerPos).getCategoryId();

                    if (existing == null) {
                        viewModel.addProduct(categoryId, name, price);
                    } else {
                        ProductEntity updatedProduct = new ProductEntity(
                                categoryId,
                                name,
                                price,
                                existing.getImageUrl(),
                                existing.isActive(),
                                existing.getCreatedAt()
                        );
                        updatedProduct.setProductId(existing.getProductId());
                        viewModel.updateProduct(updatedProduct);
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }
}
