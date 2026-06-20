package com.example.cafe_manager.ui.shift;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.cafe_manager.R;
import com.example.cafe_manager.viewmodel.ShiftCloseViewModel;

public class ShiftCloseActivity extends AppCompatActivity {

    public static final String EXTRA_SHIFT_ID = "extra_shift_id";

    private ShiftCloseViewModel viewModel;

    private TextView tvShiftName, tvShiftTime, tvUnpaidWarning;
    private LinearLayout layoutUnpaidWarning;
    private EditText etActualCash;
    private Button btnCloseShift;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift_close);

        viewModel = new ViewModelProvider(this).get(ShiftCloseViewModel.class);

        // Top bar
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tv_title)).setText("Đóng ca");

        // Bind views
        tvShiftName = findViewById(R.id.tv_shift_name);
        tvShiftTime = findViewById(R.id.tv_shift_time);
        tvUnpaidWarning = findViewById(R.id.tv_unpaid_warning);
        layoutUnpaidWarning = findViewById(R.id.layout_unpaid_warning);
        etActualCash = findViewById(R.id.et_actual_cash);
        btnCloseShift = findViewById(R.id.btn_close_shift);

        // Load shift
        int shiftId = getIntent().getIntExtra(EXTRA_SHIFT_ID, -1);
        if (shiftId == -1) {
            Toast.makeText(this, "Không xác định được ca.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        viewModel.setShiftId(shiftId);

        // Observe
        viewModel.getShift().observe(this, shift -> {
            if (shift != null) {
                tvShiftName.setText(shift.getShiftName());
                tvShiftTime.setText(shift.getStartTime() + " - " + shift.getEndTime());
            }
        });

        viewModel.getUnpaidCount().observe(this, count -> {
            if (count != null && count > 0) {
                layoutUnpaidWarning.setVisibility(View.VISIBLE);
                tvUnpaidWarning.setText("⚠ Còn " + count + " đơn chưa thanh toán!");
            } else {
                layoutUnpaidWarning.setVisibility(View.GONE);
            }
        });

        viewModel.getLoading().observe(this, loading -> {
            btnCloseShift.setEnabled(loading == null || !loading);
        });

        viewModel.getCloseSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "Đã đóng ca thành công!", Toast.LENGTH_SHORT).show();
                viewModel.clearCloseSuccess();
                setResult(RESULT_OK);
                finish();
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });

        // Button
        btnCloseShift.setOnClickListener(v -> {
            String cashText = etActualCash.getText().toString().trim();
            if (cashText.isEmpty()) {
                etActualCash.setError("Vui lòng nhập số tiền thực tế");
                return;
            }
            try {
                double actualCash = Double.parseDouble(cashText);
                viewModel.closeShift(actualCash);
            } catch (NumberFormatException e) {
                etActualCash.setError("Số tiền không hợp lệ");
            }
        });
    }
}
