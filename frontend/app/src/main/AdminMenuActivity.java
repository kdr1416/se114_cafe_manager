package com.example.cafe_manager.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.entity.ProductEntity;
import com.example.cafe_manager.viewmodel.AdminMenuViewModel;

public class AdminMenuActivity extends AppCompatActivity {

    private AdminMenuViewModel viewModel;
    private AdminProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_menu);

        RecyclerView rvProducts =
                findViewById(R.id.rv_products);

        Button btnAdd =
                findViewById(R.id.btn_add);

        rvProducts.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter = new AdminProductAdapter(
                new AdminProductAdapter.OnActionListener() {
                    @Override
                    public void onToggleVisibility(ProductEntity product) {

                        product.setActive(!product.isActive());

                        viewModel.updateProduct(product);
                    }

                    @Override
                    public void onEdit(ProductEntity product) {
                        showDialog(product);
                    }
                }
        );

        rvProducts.setAdapter(adapter);

        viewModel = new ViewModelProvider(this)
                .get(AdminMenuViewModel.class);

        viewModel.getProducts().observe(this, products -> {
            adapter.submitList(products);
        });

        btnAdd.setOnClickListener(v -> showDialog(null));
    }

    private void showDialog(ProductEntity product) {

        android.view.View view = getLayoutInflater()
                .inflate(R.layout.dialog_product_form, null);

        EditText etName = view.findViewById(R.id.et_name);
        EditText etPrice = view.findViewById(R.id.et_price);

        boolean isEdit = product != null;

        if (isEdit) {
            etName.setText(product.getProductName());
            etPrice.setText(String.valueOf(product.getPrice()));
        }

        new AlertDialog.Builder(this)
                .setTitle(isEdit ? "Sửa món" : "Thêm món")
                .setView(view)
                .setPositiveButton("Lưu", (d, w) -> {

                    String name =
                            etName.getText().toString().trim();

                    String priceText =
                            etPrice.getText().toString().trim();

                    double price =
                            Double.parseDouble(priceText);

                    if (isEdit) {

                        product.setProductName(name);
                        product.setPrice(price);

                        viewModel.updateProduct(product);

                    } else {

                        viewModel.addProduct(
                                1,
                                name,
                                price
                        );
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
