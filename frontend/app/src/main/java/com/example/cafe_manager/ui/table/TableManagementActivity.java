package com.example.cafe_manager.ui.table;

import android.app.AlertDialog;
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
import com.example.cafe_manager.data.local.entity.TableEntity;
import com.example.cafe_manager.data.local.entity.AreaEntity;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.PermissionUtils;
import com.example.cafe_manager.viewmodel.TableManagementViewModel;

import android.widget.PopupMenu;
import java.util.ArrayList;
import java.util.List;

public class TableManagementActivity extends AppCompatActivity {

    private TableManagementViewModel viewModel;
    private TableManageAdapter adapter;
    private RecyclerView rvTables;
    private View emptyState;
    private List<AreaEntity> currentAreas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PermissionUtils.requireRole(this, Constants.ROLE_ADMIN, Constants.ROLE_MANAGER)) return;

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_table_management);

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

        title.setText("Quản lý bàn");
        caption.setText("Thêm, sửa, xoá bàn trong quán");
        btnBack.setOnClickListener(v -> finish());

        btnRight.setVisibility(View.VISIBLE);
        btnRight.setImageResource(R.drawable.ic_menu_book);
        btnRight.setOnClickListener(this::showManagementMenu);
    }

    private void showManagementMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Thêm bàn mới");
        popup.getMenu().add(0, 2, 0, "Quản lý khu vực");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                showTableDialog(null);
                return true;
            } else if (item.getItemId() == 2) {
                showAreaManagementDialog();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void setupRecyclerView() {
        rvTables = findViewById(R.id.rv_tables);
        emptyState = findViewById(R.id.empty_state);
        TextView msg = emptyState.findViewById(R.id.tv_empty_message);
        msg.setText("Chưa có bàn nào.");

        rvTables.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TableManageAdapter(new TableManageAdapter.OnActionListener() {
            @Override
            public void onEdit(TableEntity table) {
                showTableDialog(table);
            }

            @Override
            public void onDelete(TableEntity table) {
                if (Constants.TABLE_OCCUPIED.equals(table.getStatus())) {
                    Toast.makeText(TableManagementActivity.this,
                            "Không thể xoá bàn đang có khách",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                new AlertDialog.Builder(TableManagementActivity.this)
                        .setTitle("Xoá bàn " + table.getTableName() + "?")
                        .setMessage("Thao tác này không thể hoàn tác.")
                        .setPositiveButton("Xoá", (d, w) ->
                                viewModel.deleteTable(table.getTableId()))
                        .setNegativeButton("Huỷ", null)
                        .show();
            }
        });
        rvTables.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TableManagementViewModel.class);

        viewModel.getTables().observe(this, list -> {
            adapter.submitList(list);
            boolean empty = list == null || list.isEmpty();
            rvTables.setVisibility(empty ? View.GONE : View.VISIBLE);
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        });

        viewModel.getAreas().observe(this, list -> {
            if (list != null) {
                currentAreas = list;
            }
        });

        TextView tvTotal = findViewById(R.id.tv_total_count);
        TextView tvEmpty = findViewById(R.id.tv_empty_count);

        viewModel.getTotalCount().observe(this, c ->
                tvTotal.setText(String.valueOf(c != null ? c : 0)));
        viewModel.getEmptyCount().observe(this, c ->
                tvEmpty.setText(String.valueOf(c != null ? c : 0)));

        viewModel.getMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });
    }

    private void showTableDialog(@Nullable TableEntity existing) {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_table_form, null);

        EditText etName = view.findViewById(R.id.et_table_name);
        EditText etCapacity = view.findViewById(R.id.et_capacity);
        Spinner spinnerArea = view.findViewById(R.id.spinner_area);

        List<String> areaNames = new ArrayList<>();
        for (AreaEntity a : currentAreas) {
            areaNames.add(a.getAreaName());
        }

        ArrayAdapter<String> areaAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, areaNames);
        spinnerArea.setAdapter(areaAdapter);

        if (existing != null) {
            etName.setText(existing.getTableName());
            etCapacity.setText(String.valueOf(existing.getCapacity()));

            if (existing.getArea() != null) {
                int areaIdx = areaNames.indexOf(existing.getArea());
                if (areaIdx >= 0) spinnerArea.setSelection(areaIdx);
            }
        }

        spinnerArea.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (existing != null) return; // Không thay đổi tên bàn cũ
                if (position >= 0 && position < currentAreas.size()) {
                    AreaEntity selectedArea = currentAreas.get(position);
                    String prefix = selectedArea.getPrefix();
                    int count = 0;
                    List<TableEntity> allTables = viewModel.getTables().getValue();
                    if (allTables != null) {
                        for (TableEntity t : allTables) {
                            if (selectedArea.getAreaName().equals(t.getArea())) {
                                count++;
                            }
                        }
                    }
                    String suggestedName = prefix + String.format("%02d", count + 1);
                    etName.setText(suggestedName);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        String title = existing == null ? "Thêm bàn" : "Sửa bàn";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(view)
                .setPositiveButton("Lưu", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String capStr = etCapacity.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(this, "Tên bàn không được rỗng",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int capacity;
                    try {
                        capacity = Integer.parseInt(capStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Sức chứa không hợp lệ",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (capacity <= 0) {
                        Toast.makeText(this, "Sức chứa phải lớn hơn 0",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (spinnerArea.getSelectedItemPosition() < 0) {
                        Toast.makeText(this, "Vui lòng chọn khu vực", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String area = areaNames.get(spinnerArea.getSelectedItemPosition());

                    if (existing == null) {
                        viewModel.addTable(name, capacity, area);
                    } else {
                        TableEntity updatedTable = new TableEntity(
                                name,
                                existing.getStatus(),
                                capacity,
                                area,
                                existing.getCreatedAt()
                        );
                        updatedTable.setTableId(existing.getTableId());
                        viewModel.updateTable(updatedTable);
                    }
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void showAreaManagementDialog() {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_area_management, null);

        RecyclerView rv = view.findViewById(R.id.rv_areas_list);
        rv.setLayoutManager(new LinearLayoutManager(this));

        AreaManageAdapter areaAdapter = new AreaManageAdapter(new AreaManageAdapter.OnActionListener() {
            @Override
            public void onEdit(AreaEntity area) {
                showAreaFormDialog(area);
            }

            @Override
            public void onDelete(AreaEntity area) {
                new AlertDialog.Builder(TableManagementActivity.this)
                        .setTitle("Xoá khu vực " + area.getAreaName() + "?")
                        .setMessage("Hệ thống sẽ chặn xoá nếu có bàn thuộc khu vực này.")
                        .setPositiveButton("Xoá", (d, w) ->
                                viewModel.deleteArea(area.getAreaId(), area.getAreaName()))
                        .setNegativeButton("Huỷ", null)
                        .show();
            }
        });
        rv.setAdapter(areaAdapter);

        viewModel.getAreas().observe(this, list -> {
            if (list != null) {
                areaAdapter.submitList(list);
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Quản lý khu vực")
                .setView(view)
                .setNegativeButton("Đóng", null)
                .create();

        view.findViewById(R.id.btn_add_area).setOnClickListener(v -> {
            showAreaFormDialog(null);
        });

        dialog.show();
    }

    private void showAreaFormDialog(@Nullable AreaEntity existing) {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_area_form, null);

        EditText etName = view.findViewById(R.id.et_area_name);
        EditText etPrefix = view.findViewById(R.id.et_prefix);

        if (existing != null) {
            etName.setText(existing.getAreaName());
            etPrefix.setText(existing.getPrefix());
        }

        String title = existing == null ? "Thêm khu vực" : "Sửa khu vực";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(view)
                .setPositiveButton("Lưu", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String prefix = etPrefix.getText().toString().trim().toUpperCase();

                    if (name.isEmpty()) {
                        Toast.makeText(this, "Tên khu vực không được rỗng", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (prefix.isEmpty()) {
                        Toast.makeText(this, "Tiền tố không được rỗng", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (existing == null) {
                        for (AreaEntity a : currentAreas) {
                            if (a.getAreaName().equalsIgnoreCase(name)) {
                                Toast.makeText(this, "Tên khu vực đã tồn tại", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (a.getPrefix().equalsIgnoreCase(prefix)) {
                                Toast.makeText(this, "Tiền tố đã tồn tại", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        viewModel.addArea(name, prefix);
                    } else {
                        for (AreaEntity a : currentAreas) {
                            if (a.getAreaId() != existing.getAreaId()) {
                                if (a.getAreaName().equalsIgnoreCase(name)) {
                                    Toast.makeText(this, "Tên khu vực đã tồn tại", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                if (a.getPrefix().equalsIgnoreCase(prefix)) {
                                    Toast.makeText(this, "Tiền tố đã tồn tại", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }
                        }
                        existing.setAreaName(name);
                        existing.setPrefix(prefix);
                        viewModel.updateArea(existing);
                    }
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }
}
