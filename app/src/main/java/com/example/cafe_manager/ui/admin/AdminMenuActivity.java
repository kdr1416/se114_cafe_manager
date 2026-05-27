package com.example.cafe_manager.ui.admin;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.entity.ProductEntity;
import com.example.cafe_manager.ui.common.BaseActivity;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.ImageLoader;
import com.example.cafe_manager.util.ImageStorageUtils;
import com.example.cafe_manager.viewmodel.AdminMenuViewModel;

public class AdminMenuActivity extends BaseActivity {

    private AdminMenuViewModel viewModel;
    private AdminProductAdapter adapter;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ImageView currentDialogPreview;
    private String pickedImageFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_menu);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::onImagePicked
        );

        RecyclerView rvProducts = findViewById(R.id.rv_products);
        Button btnAdd = findViewById(R.id.btn_add);

        rvProducts.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AdminProductAdapter(new AdminProductAdapter.OnActionListener() {
            @Override
            public void onToggleVisibility(ProductEntity product) {
                product.setActive(!product.isActive());
                viewModel.updateProduct(product);
            }

            @Override
            public void onEdit(ProductEntity product) {
                showDialog(product);
            }
        });

        rvProducts.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(AdminMenuViewModel.class);
        viewModel.getProducts().observe(this, products -> {
            adapter.submitList(products);
        });

        btnAdd.setOnClickListener(v -> showDialog(null));
    }

    private void showDialog(ProductEntity product) {
        View view = getLayoutInflater().inflate(R.layout.dialog_product_form, null);

        EditText etName = view.findViewById(R.id.et_name);
        EditText etPrice = view.findViewById(R.id.et_price);
        currentDialogPreview = view.findViewById(R.id.iv_product_preview);
        Button btnPickImage = view.findViewById(R.id.btn_pick_image);

        boolean isEdit = product != null;
        pickedImageFileName = isEdit ? product.getImageUrl() : null;

        if (isEdit) {
            etName.setText(product.getProductName());
            etPrice.setText(String.valueOf(product.getPrice()));
            ImageLoader.loadProductImage(this, currentDialogPreview, product.getImageUrl());
        }

        btnPickImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        new AlertDialog.Builder(this)
                .setTitle(isEdit ? "Sửa món" : "Thêm món")
                .setView(view)
                .setPositiveButton("Lưu", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String priceText = etPrice.getText().toString().trim();
                    if (name.isEmpty() || priceText.isEmpty()) return;

                    double price = Double.parseDouble(priceText);

                    if (isEdit) {
                        product.setProductName(name);
                        product.setPrice(price);
                        product.setImageUrl(pickedImageFileName);
                        viewModel.updateProduct(product);
                    } else {
                        // Using hardcoded category 1 for now, or you can add a category selector
                        ProductEntity newProduct = new ProductEntity(1, name, price);
                        newProduct.setImageUrl(pickedImageFileName);
                        newProduct.setActive(true);
                        viewModel.addProduct(newProduct);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void onImagePicked(Uri uri) {
        if (uri == null) return;

        AppExecutors.getInstance().diskIO().execute(() -> {
            String fileName = ImageStorageUtils.saveImageFromUri(this, uri);
            AppExecutors.getInstance().mainThread().execute(() -> {
                if (fileName == null) {
                    Toast.makeText(this, "Không thể lưu ảnh.", Toast.LENGTH_SHORT).show();
                    return;
                }
                pickedImageFileName = fileName;
                if (currentDialogPreview != null) {
                    ImageLoader.loadProductImage(this, currentDialogPreview, fileName);
                }
            });
        });
    }
}
