package com.example.cafe_manager.ui.promotion;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
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
import com.example.cafe_manager.data.local.entity.PromotionEntity;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.PermissionUtils;
import com.example.cafe_manager.viewmodel.PromotionViewModel;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PromotionManagementActivity extends AppCompatActivity {

    private PromotionViewModel viewModel;
    private PromotionAdapter adapter;
    private RecyclerView rvPromotions;
    private View emptyState;

    private static final List<String> TYPE_LABELS = Arrays.asList(
            "Giảm tiền (VND)", "Giảm phần trăm (%)");
    private static final List<String> TYPE_VALUES = Arrays.asList(
            Constants.PROMO_CASH, Constants.PROMO_PERCENT);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PermissionUtils.requireRole(this, Constants.ROLE_ADMIN, Constants.ROLE_MANAGER)) return;

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_promotion_management);

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

        title.setText("Quản lý khuyến mãi");
        caption.setText("Thêm, sửa, bật/tắt mã giảm giá");
        btnBack.setOnClickListener(v -> finish());

        btnRight.setVisibility(View.VISIBLE);
        btnRight.setImageResource(R.drawable.ic_plus);
        btnRight.setOnClickListener(v -> showPromotionDialog(null));
    }

    private void setupRecyclerView() {
        rvPromotions = findViewById(R.id.rv_promotions);
        emptyState = findViewById(R.id.empty_state);
        TextView msg = emptyState.findViewById(R.id.tv_empty_message);
        msg.setText("Chưa có mã giảm giá nào.");

        rvPromotions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PromotionAdapter(new PromotionAdapter.OnActionListener() {
            @Override
            public void onEdit(PromotionEntity p) {
                showPromotionDialog(p);
            }

            @Override
            public void onToggleActive(PromotionEntity p) {
                viewModel.toggleActive(p.getPromotionId(), !p.isActive());
            }

            @Override
            public void onDelete(PromotionEntity p) {
                new AlertDialog.Builder(PromotionManagementActivity.this)
                        .setTitle("Xoá mã " + p.getCode() + "?")
                        .setMessage("Thao tác này không thể hoàn tác.")
                        .setPositiveButton("Xoá", (d, w) -> viewModel.delete(p.getPromotionId()))
                        .setNegativeButton("Huỷ", null)
                        .show();
            }
        });
        rvPromotions.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(PromotionViewModel.class);

        viewModel.getPromotions().observe(this, list -> {
            adapter.submitList(list);
            boolean empty = list == null || list.isEmpty();
            rvPromotions.setVisibility(empty ? View.GONE : View.VISIBLE);
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        });

        TextView tvTotal = findViewById(R.id.tv_total_count);
        TextView tvActive = findViewById(R.id.tv_active_count);

        viewModel.getTotalCount().observe(this, c ->
                tvTotal.setText(String.valueOf(c != null ? c : 0)));
        viewModel.getActiveCount().observe(this, c ->
                tvActive.setText(String.valueOf(c != null ? c : 0)));

        viewModel.getMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });
    }

    private void showPromotionDialog(@Nullable PromotionEntity existing) {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_promotion_form, null);

        EditText etCode = view.findViewById(R.id.et_code);
        Spinner spinnerType = view.findViewById(R.id.spinner_type);
        TextView labelValue = view.findViewById(R.id.label_value);
        EditText etValue = view.findViewById(R.id.et_value);
        EditText etExpires = view.findViewById(R.id.et_expires);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, TYPE_LABELS);
        spinnerType.setAdapter(typeAdapter);

        final long[] selectedExpires = {0};

        etExpires.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (dp, y, m, d) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(y, m, d, 23, 59, 59);
                selectedExpires[0] = selected.getTimeInMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                etExpires.setText(sdf.format(new Date(selectedExpires[0])));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        if (existing != null) {
            etCode.setText(existing.getCode());
            etCode.setEnabled(false);

            int typeIdx = TYPE_VALUES.indexOf(existing.getType());
            if (typeIdx >= 0) spinnerType.setSelection(typeIdx);

            etValue.setText(String.valueOf(
                    existing.getType().equals(Constants.PROMO_PERCENT)
                            ? (int) existing.getValue()
                            : (long) existing.getValue()));

            if (existing.getExpiresAt() > 0) {
                selectedExpires[0] = existing.getExpiresAt();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                etExpires.setText(sdf.format(new Date(existing.getExpiresAt())));
            }
        }

        String title = existing == null ? "Thêm mã giảm giá" : "Sửa mã giảm giá";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(view)
                .setPositiveButton("Lưu", (d, w) -> {
                    String code = etCode.getText().toString().trim();
                    String valueStr = etValue.getText().toString().trim();

                    if (code.isEmpty()) {
                        Toast.makeText(this, "Mã không được rỗng", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double value;
                    try {
                        value = Double.parseDouble(valueStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Giá trị không hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value <= 0) {
                        Toast.makeText(this, "Giá trị phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int typePos = spinnerType.getSelectedItemPosition();
                    String type = TYPE_VALUES.get(typePos);

                    if (Constants.PROMO_PERCENT.equals(type) && value > 100) {
                        Toast.makeText(this, "Phần trăm tối đa là 100", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (existing == null) {
                        viewModel.addPromotion(code, type, value, selectedExpires[0]);
                    } else {
                        existing.setType(type);
                        existing.setValue(value);
                        existing.setExpiresAt(selectedExpires[0]);
                        viewModel.updatePromotion(existing);
                    }
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }
}
