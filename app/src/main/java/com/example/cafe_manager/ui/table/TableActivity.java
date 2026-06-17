package com.example.cafe_manager.ui.table;

import android.app.AlertDialog;
import android.content.Intent;

import android.os.Bundle;

import android.view.View;

import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;

import androidx.appcompat.app.AppCompatActivity;

import androidx.lifecycle.ViewModelProvider;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.entity.TableEntity;
import com.example.cafe_manager.manager.CartManager;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.ui.admin.AdminMenuActivity;
import com.example.cafe_manager.ui.auth.LoginActivity;
import com.example.cafe_manager.ui.dashboard.DashboardActivity;
import com.example.cafe_manager.ui.history.HistoryActivity;
import com.example.cafe_manager.ui.menu.MenuActivity;
import com.example.cafe_manager.ui.orderslist.OrdersListActivity;
import com.example.cafe_manager.ui.profile.ProfileActivity;
import com.example.cafe_manager.ui.promotion.PromotionManagementActivity;
import com.example.cafe_manager.ui.user.UserManagementActivity;
import com.example.cafe_manager.ui.menu.CategoryManagementActivity;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.PermissionUtils;
import com.example.cafe_manager.viewmodel.TableViewModel;

public class TableActivity extends AppCompatActivity {

    public static final String EXTRA_TABLE_ID = "table_id";

    public static final String EXTRA_TABLE_NAME = "table_name";

    private TableViewModel viewModel;

    private TableAdapter adapter;

    private AreaAdapter areaAdapter;

    private TextView tvActiveCount;

    private TextView tvEmptyCount;

    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_table);

        setupTopBar();

        setupBottomNav();

        tvActiveCount = findViewById(R.id.tv_active_count);

        tvEmptyCount = findViewById(R.id.tv_empty_count);

        setupRecyclerView();

        setupViewModel();

    }

    private void setupTopBar() {

        View topBar = findViewById(R.id.top_bar);

        TextView title = topBar.findViewById(R.id.tv_title);

        TextView caption = topBar.findViewById(R.id.tv_caption);

        View btnBack = topBar.findViewById(R.id.btn_back);

        ImageButton btnRight = topBar.findViewById(R.id.btn_right);

        title.setText(R.string.title_tables);

        // Greeting với tên user (nếu có session)
        String fullName = SessionManager.getInstance(this).getFullName();
        if (fullName == null || fullName.isEmpty()) {
            caption.setText(R.string.caption_tables);
        } else {
            caption.setText("Xin chào, " + fullName);
        }

        btnBack.setVisibility(View.GONE);   // entry screen, không back

        // Right icon → popup menu: Lịch sử / Báo cáo / Đăng xuất
        btnRight.setVisibility(View.VISIBLE);
        btnRight.setImageResource(R.drawable.ic_menu_book);
        btnRight.setOnClickListener(this::showOptionsMenu);

    }

    private void showOptionsMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        String role = SessionManager.getInstance(this).getRole();

        popup.getMenu().add(0, 4, 0, "Hồ sơ cá nhân");
        if (PermissionUtils.canViewReports(role)) {
            popup.getMenu().add(0, 1, 0, "Lịch sử giao dịch");
            popup.getMenu().add(0, 2, 0, "Báo cáo doanh thu");
        }
        if (PermissionUtils.canManageUsers(role)) {
            popup.getMenu().add(0, 5, 0, "Quản lý nhân viên");
        }
        if (PermissionUtils.canManageTables(role)) {
            popup.getMenu().add(0, 6, 0, "Quản lý bàn");
        }
        if (PermissionUtils.canManagePromotions(role)) {
            popup.getMenu().add(0, 7, 0, "Quản lý khuyến mãi");
        }
        if (PermissionUtils.canManageCategories(role)) {
            popup.getMenu().add(0, 8, 0, "Quản lý danh mục");
        }
        popup.getMenu().add(0, 3, 0, "Đăng xuất");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 4:
                    startActivity(new Intent(this, ProfileActivity.class));
                    return true;
                case 1:
                    startActivity(new Intent(this, HistoryActivity.class));
                    return true;
                case 2:
                    startActivity(new Intent(this, DashboardActivity.class));
                    return true;
                case 5:
                    startActivity(new Intent(this, UserManagementActivity.class));
                    return true;
                case 6:
                    startActivity(new Intent(this, TableManagementActivity.class));
                    return true;
                case 7:
                    startActivity(new Intent(this, PromotionManagementActivity.class));
                    return true;
                case 8:
                    startActivity(new Intent(this, CategoryManagementActivity.class));
                    return true;
                case 3:
                    showLogoutDialog();
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất?")
                .setMessage("Bạn sẽ phải đăng nhập lại để tiếp tục.")
                .setPositiveButton("Đăng xuất", (d, w) -> performLogout())
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void performLogout() {
        SessionManager.getInstance(this).logout();
        CartManager.getInstance().clearCart();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBottomNav() {
        View bottomNav = findViewById(R.id.bottom_nav);
        View navTables = bottomNav.findViewById(R.id.nav_tables);
        View navOrders = bottomNav.findViewById(R.id.nav_orders);
        View navMenu = bottomNav.findViewById(R.id.nav_menu);
        navTables.setSelected(true);
        navOrders.setOnClickListener(v -> startActivity(new Intent(this, OrdersListActivity.class)));

        String role = SessionManager.getInstance(this).getRole();
        if (!PermissionUtils.canManageMenu(role)) {
            navMenu.setVisibility(View.GONE);
        } else {
            navMenu.setOnClickListener(v -> startActivity(new Intent(this, AdminMenuActivity.class)));
        }
    }


    private void setupRecyclerView() {
        RecyclerView rvAreas = findViewById(R.id.rv_areas);
        rvAreas.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        areaAdapter = new AreaAdapter(area -> {
            viewModel.selectArea(area);
        });
        rvAreas.setAdapter(areaAdapter);

        RecyclerView rv = findViewById(R.id.rv_tables);
        rv.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new TableAdapter(this::onTableClicked);
        rv.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TableViewModel.class);

        viewModel.getTables().observe(this, list -> {
            if (list != null) adapter.submitList(list);
        });

        viewModel.getAreas().observe(this, areas -> {
            if (areas != null) areaAdapter.submitList(areas);
        });

        viewModel.getSelectedArea().observe(this, area -> {
            if (area != null) areaAdapter.setSelectedArea(area);
        });

        viewModel.getOccupiedCount().observe(this, c -> updateActiveStat());
        viewModel.getTotalCount().observe(this, c -> updateActiveStat());
        viewModel.getEmptyCount().observe(this, empty -> {
            if (empty != null) tvEmptyCount.setText(String.valueOf(empty));
        });
    }

    private void updateActiveStat() {

        Integer total = viewModel.getTotalCount().getValue();

        Integer occupied = viewModel.getOccupiedCount().getValue();

        if (total != null && occupied != null) {

            tvActiveCount.setText(getString(R.string.format_total_tables, occupied, total));

        }

    }

    private void onTableClicked(TableEntity table) {

        // Reset cart trước mọi flow mới — đảm bảo không kế thừa state cũ
        CartManager.getInstance().clearCart();

        if (Constants.TABLE_EMPTY.equals(table.getStatus())) {

            Intent intent = new Intent(this, MenuActivity.class);

            intent.putExtra(EXTRA_TABLE_ID, table.getTableId());

            intent.putExtra(EXTRA_TABLE_NAME, table.getTableName());

            startActivity(intent);

        } else {

            // OCCUPIED: mở OrdersList để chọn order cần thu tiền (hoặc gọi thêm)

            Intent intent = new Intent(this, OrdersListActivity.class);

            startActivity(intent);

        }

    }
}
