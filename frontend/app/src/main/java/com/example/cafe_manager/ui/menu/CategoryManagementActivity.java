package com.example.cafe_manager.ui.menu;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.PermissionUtils;
import com.example.cafe_manager.viewmodel.CategoryManagementViewModel;

public class CategoryManagementActivity extends AppCompatActivity {

    private CategoryManagementViewModel viewModel;
    private CategoryManageAdapter adapter;
    private RecyclerView rvCategories;
    private View emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PermissionUtils.requireRole(this, Constants.ROLE_ADMIN, Constants.ROLE_MANAGER)) return;

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_category_management);

        setupTopBar();
        setupRecyclerView();
        setupViewModel();
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);
        ImageButton btnRight = topBar.findViewById(R.id.btn_right);

        title.setText("Quản lý danh mục");
        caption.setText("Thêm, sửa, xoá danh mục món ăn");
        btnBack.setOnClickListener(v -> finish());

        btnRight.setVisibility(View.VISIBLE);
        btnRight.setImageResource(R.drawable.ic_plus);
        btnRight.setOnClickListener(v -> showCategoryDialog(null));
    }

    private void setupRecyclerView() {
        rvCategories = findViewById(R.id.rv_categories);
        emptyState = findViewById(R.id.empty_state);
        TextView msg = emptyState.findViewById(R.id.tv_empty_message);
        msg.setText("Chưa có danh mục nào.");

        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryManageAdapter(new CategoryManageAdapter.OnActionListener() {
            @Override
            public void onEdit(CategoryEntity category) {
                showCategoryDialog(category);
            }

            @Override
            public void onDelete(CategoryEntity category) {
                new AlertDialog.Builder(CategoryManagementActivity.this)
                        .setTitle("Xoá danh mục " + category.getCategoryName() + "?")
                        .setMessage("Thao tác này không thể hoàn tác.")
                        .setPositiveButton("Xoá", (d, w) -> viewModel.deleteCategory(category.getCategoryId()))
                        .setNegativeButton("Huỷ", null)
                        .show();
            }
        });
        rvCategories.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(CategoryManagementViewModel.class);

        viewModel.getCategories().observe(this, list -> {
            adapter.submitList(list);
            boolean empty = list == null || list.isEmpty();
            rvCategories.setVisibility(empty ? View.GONE : View.VISIBLE);
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        });

        TextView tvTotal = findViewById(R.id.tv_total_count);
        viewModel.getCategories().observe(this, list -> {
            tvTotal.setText(String.valueOf(list != null ? list.size() : 0));
        });

        viewModel.getMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });
    }

    private void showCategoryDialog(@Nullable CategoryEntity existing) {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_category_form, null);

        EditText etName = view.findViewById(R.id.et_category_name);
        EditText etDescription = view.findViewById(R.id.et_description);

        if (existing != null) {
            etName.setText(existing.getCategoryName());
            etDescription.setText(existing.getDescription());
        }

        String title = existing == null ? "Thêm danh mục" : "Sửa danh mục";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(view)
                .setPositiveButton("Lưu", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String desc = etDescription.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(this, "Tên danh mục không được rỗng", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (existing == null) {
                        viewModel.addCategory(name, desc);
                    } else {
                        CategoryEntity updatedCategory = new CategoryEntity(
                                name,
                                existing.isActive(),
                                desc
                        );
                        updatedCategory.setCategoryId(existing.getCategoryId());
                        viewModel.updateCategory(updatedCategory);
                    }
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }
}
