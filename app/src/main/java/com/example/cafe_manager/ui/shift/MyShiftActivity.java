package com.example.cafe_manager.ui.shift;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.viewmodel.MyShiftViewModel;

public class MyShiftActivity extends AppCompatActivity {

    private MyShiftViewModel viewModel;
    private MyShiftAdapter adapter;
    private TextView tvCount, tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_shift);

        viewModel = new ViewModelProvider(this).get(MyShiftViewModel.class);

        // Top bar
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tv_title)).setText("Ca làm của tôi");

        tvCount = findViewById(R.id.tv_shift_count);
        tvEmpty = findViewById(R.id.tv_empty);

        // RecyclerView
        RecyclerView rv = findViewById(R.id.rv_my_shifts);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyShiftAdapter(assignmentId -> viewModel.confirmAssignment(assignmentId));
        rv.setAdapter(adapter);

        // Observe
        viewModel.getMyShifts().observe(this, items -> {
            adapter.setItems(items);
            int size = items != null ? items.size() : 0;
            tvCount.setText("Tổng: " + size + " ca");
            tvEmpty.setVisibility(size == 0 ? View.VISIBLE : View.GONE);
        });

        viewModel.getMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadMyShifts(); // Refresh khi quay lại
    }
}
