package com.example.cafe_manager.ui.table;

import android.content.Intent;

import android.os.Bundle;

import android.view.View;

import android.widget.TextView;

import androidx.activity.EdgeToEdge;

import androidx.appcompat.app.AppCompatActivity;

import androidx.lifecycle.ViewModelProvider;

import androidx.recyclerview.widget.GridLayoutManager;

import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;

import com.example.cafe_manager.data.local.entity.TableEntity;

import com.example.cafe_manager.ui.admin.AdminMenuActivity;

import com.example.cafe_manager.ui.menu.MenuActivity;

import com.example.cafe_manager.ui.orderslist.OrdersListActivity;

import com.example.cafe_manager.util.Constants;

import com.example.cafe_manager.viewmodel.TableViewModel;

public class TableActivity extends AppCompatActivity {

    public static final String EXTRA_TABLE_ID = "table_id";

    public static final String EXTRA_TABLE_NAME = "table_name";

    private TableViewModel viewModel;

    private TableAdapter adapter;

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

        View btnRight = topBar.findViewById(R.id.btn_right);

        title.setText(R.string.title_tables);

        caption.setText(R.string.caption_tables);

        btnBack.setVisibility(View.GONE);   // entry screen, không back

        btnRight.setVisibility(View.VISIBLE); // search icon (MVP: chưa wire)

    }

    private void setupBottomNav() {
        View bottomNav = findViewById(R.id.bottom_nav);
        View navTables = bottomNav.findViewById(R.id.nav_tables);
        View navOrders = bottomNav.findViewById(R.id.nav_orders);
        View navMenu = bottomNav.findViewById(R.id.nav_menu);
        navTables.setSelected(true);
        navOrders.setOnClickListener(v -> startActivity(new Intent(this, OrdersListActivity.class)));

        navMenu.setOnClickListener(v -> startActivity(new Intent(this, AdminMenuActivity.class)));
    }


    private void setupRecyclerView() {

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

        if (Constants.TABLE_EMPTY.equals(table.getStatus())) {

            Intent intent = new Intent(this, MenuActivity.class);

            intent.putExtra(EXTRA_TABLE_ID, table.getTableId());

            intent.putExtra(EXTRA_TABLE_NAME, table.getTableName());

            startActivity(intent);

        } else {

            // OCCUPIED: mở OrdersList để chọn order cần thu tiền

            Intent intent = new Intent(this, OrdersListActivity.class);

            startActivity(intent);

        }

    }
}
