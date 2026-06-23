package com.example.cafe_manager.ui.shift;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.entity.ShiftTemplateEntity;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.PermissionUtils;
import com.example.cafe_manager.viewmodel.ShiftTemplateViewModel;

import java.util.Locale;

public class ShiftTemplateActivity extends AppCompatActivity {

    private ShiftTemplateViewModel viewModel;
    private ShiftTemplateAdapter adapter;
    private TextView tvCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift_template);

        if (!PermissionUtils.requireRole(this, Constants.ROLE_ADMIN, Constants.ROLE_MANAGER)) return;

        viewModel = new ViewModelProvider(this).get(ShiftTemplateViewModel.class);

        // Top bar - Access views from include
        View topBar = findViewById(R.id.top_bar);
        topBar.findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        ((TextView) topBar.findViewById(R.id.tv_title)).setText("Quản lý mẫu ca");
        
        ImageButton btnRight = topBar.findViewById(R.id.btn_right);
        btnRight.setVisibility(View.VISIBLE);
        btnRight.setImageResource(R.drawable.ic_plus);
        btnRight.setOnClickListener(v -> showTemplateDialog(null));

        tvCount = findViewById(R.id.tv_template_count);

        // RecyclerView
        RecyclerView rv = findViewById(R.id.rv_templates);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ShiftTemplateAdapter(new ShiftTemplateAdapter.OnTemplateActionListener() {
            @Override
            public void onEdit(ShiftTemplateEntity template) {
                showTemplateDialog(template);
            }

            @Override
            public void onDeactivate(ShiftTemplateEntity template) {
                new AlertDialog.Builder(ShiftTemplateActivity.this)
                        .setTitle("Xác nhận")
                        .setMessage("Ngưng hoạt động mẫu ca \"" + template.getTemplateName() + "\"?")
                        .setPositiveButton("Ngưng", (d, w) -> viewModel.deactivateTemplate(template.getTemplateId()))
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });
        rv.setAdapter(adapter);

        // Observe
        viewModel.getTemplates().observe(this, templates -> {
            adapter.setItems(templates);
            tvCount.setText("Tổng: " + (templates != null ? templates.size() : 0) + " mẫu ca");
        });

        viewModel.getMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });

        // Filter chips setup
        TextView chipAll = findViewById(R.id.chip_template_all);
        TextView chipActive = findViewById(R.id.chip_template_active);
        TextView chipInactive = findViewById(R.id.chip_template_inactive);

        View.OnClickListener filterClickListener = v -> {
            String filter = "ALL";
            if (v.getId() == R.id.chip_template_all) {
                filter = "ALL";
            } else if (v.getId() == R.id.chip_template_active) {
                filter = "ACTIVE";
            } else if (v.getId() == R.id.chip_template_inactive) {
                filter = "INACTIVE";
            }
            viewModel.setFilter(filter);
            updateTemplateChips(filter, chipAll, chipActive, chipInactive);
        };

        chipAll.setOnClickListener(filterClickListener);
        chipActive.setOnClickListener(filterClickListener);
        chipInactive.setOnClickListener(filterClickListener);
    }

    private void updateTemplateChips(String filter, TextView chipAll, TextView chipActive, TextView chipInactive) {
        int activeBg = R.drawable.bg_button_primary;
        int activeTextColor = getColor(R.color.text_on_accent);
        int inactiveBg = R.drawable.bg_button_secondary;
        int inactiveTextColor = getColor(R.color.text_soft);

        chipAll.setBackgroundResource("ALL".equals(filter) ? activeBg : inactiveBg);
        chipAll.setTextColor("ALL".equals(filter) ? activeTextColor : inactiveTextColor);

        chipActive.setBackgroundResource("ACTIVE".equals(filter) ? activeBg : inactiveBg);
        chipActive.setTextColor("ACTIVE".equals(filter) ? activeTextColor : inactiveTextColor);

        chipInactive.setBackgroundResource("INACTIVE".equals(filter) ? activeBg : inactiveBg);
        chipInactive.setTextColor("INACTIVE".equals(filter) ? activeTextColor : inactiveTextColor);
    }

    private void showTemplateDialog(ShiftTemplateEntity existing) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_shift_template_form, null);
        EditText etName = dialogView.findViewById(R.id.et_template_name);
        Button btnStart = dialogView.findViewById(R.id.btn_start_time);
        Button btnEnd = dialogView.findViewById(R.id.btn_end_time);
        EditText etMinStaff = dialogView.findViewById(R.id.et_min_staff);

        final String[] startTime = {"06:00"};
        final String[] endTime = {"14:00"};

        if (existing != null) {
            etName.setText(existing.getTemplateName());
            startTime[0] = existing.getStartTime();
            endTime[0] = existing.getEndTime();
            etMinStaff.setText(String.valueOf(existing.getMinStaff()));
        }
        btnStart.setText(startTime[0]);
        btnEnd.setText(endTime[0]);

        btnStart.setOnClickListener(v -> {
            String[] parts = startTime[0].split(":");
            new TimePickerDialog(this, (view, h, m) -> {
                startTime[0] = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                btnStart.setText(startTime[0]);
            }, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), true).show();
        });

        btnEnd.setOnClickListener(v -> {
            String[] parts = endTime[0].split(":");
            new TimePickerDialog(this, (view, h, m) -> {
                endTime[0] = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                btnEnd.setText(endTime[0]);
            }, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), true).show();
        });

        String title = existing == null ? "Thêm mẫu ca" : "Sửa mẫu ca";
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton("Lưu", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String minStr = etMinStaff.getText().toString().trim();
                    int minStaff = minStr.isEmpty() ? 0 : Integer.parseInt(minStr);

                    if (existing == null) {
                        viewModel.addTemplate(name, startTime[0], endTime[0], minStaff);
                    } else {
                        existing.setTemplateName(name);
                        existing.setStartTime(startTime[0]);
                        existing.setEndTime(endTime[0]);
                        existing.setMinStaff(minStaff);
                        viewModel.updateTemplate(existing);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
