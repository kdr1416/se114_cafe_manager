package com.example.cafe_manager.ui.leave;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.remote.LeaveRequestResponse;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.PermissionUtils;
import com.example.cafe_manager.viewmodel.LeaveApprovalViewModel;

public class LeaveApprovalActivity extends AppCompatActivity implements LeaveApprovalAdapter.OnLeaveApprovalActionListener {

    private LeaveApprovalViewModel viewModel;
    private LeaveApprovalAdapter adapter;

    private Button btnFilterPending;
    private Button btnFilterApproved;
    private Button btnFilterRejected;
    private ProgressBar progressLoading;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enforce role guard at startup
        if (!PermissionUtils.requireRole(this, Constants.ROLE_ADMIN, Constants.ROLE_MANAGER)) {
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_leave_approval);

        viewModel = new ViewModelProvider(this).get(LeaveApprovalViewModel.class);

        setupTopBar();
        bindViews();
        setupRecyclerView();
        observeViewModel();

        // Default to loading Pending requests
        String initialStatus = viewModel.getSelectedStatus().getValue();
        if (initialStatus == null) {
            initialStatus = "PENDING";
        }
        selectFilter(initialStatus);
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);
        View btnRight = topBar.findViewById(R.id.btn_right);

        title.setText("Duyệt đơn xin nghỉ");
        caption.setText("Quản lý và phê duyệt đơn xin nghỉ của nhân viên");
        btnBack.setOnClickListener(v -> finish());
        btnRight.setVisibility(View.GONE);
    }

    private void bindViews() {
        btnFilterPending = findViewById(R.id.btnFilterPending);
        btnFilterApproved = findViewById(R.id.btnFilterApproved);
        btnFilterRejected = findViewById(R.id.btnFilterRejected);
        progressLoading = findViewById(R.id.progressLoading);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        btnFilterPending.setOnClickListener(v -> selectFilter("PENDING"));
        btnFilterApproved.setOnClickListener(v -> selectFilter("APPROVED"));
        btnFilterRejected.setOnClickListener(v -> selectFilter("REJECTED"));
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerLeaveApproval);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaveApprovalAdapter(this);
        recyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getRequests().observe(this, list -> {
            adapter.setItems(list);
            int size = list != null ? list.size() : 0;
            tvEmptyState.setVisibility(size == 0 ? View.VISIBLE : View.GONE);
        });

        viewModel.getLoading().observe(this, isLoading -> {
            progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getSelectedStatus().observe(this, this::updateFilterButtons);

        viewModel.getMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });

        viewModel.getError().observe(this, err -> {
            if (err != null) {
                Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
    }

    private void selectFilter(String status) {
        viewModel.setSelectedStatus(status);
        viewModel.loadRequests(status);
    }

    private void updateFilterButtons(String selectedStatus) {
        if ("PENDING".equals(selectedStatus)) {
            btnFilterPending.setBackgroundResource(R.drawable.bg_button_primary);
            btnFilterPending.setTextColor(getColor(R.color.text_on_accent));

            btnFilterApproved.setBackgroundResource(R.drawable.bg_button_secondary);
            btnFilterApproved.setTextColor(getColor(R.color.text_primary));

            btnFilterRejected.setBackgroundResource(R.drawable.bg_button_secondary);
            btnFilterRejected.setTextColor(getColor(R.color.text_primary));

            tvEmptyState.setText("Không có đơn chờ duyệt");
        } else if ("APPROVED".equals(selectedStatus)) {
            btnFilterPending.setBackgroundResource(R.drawable.bg_button_secondary);
            btnFilterPending.setTextColor(getColor(R.color.text_primary));

            btnFilterApproved.setBackgroundResource(R.drawable.bg_button_primary);
            btnFilterApproved.setTextColor(getColor(R.color.text_on_accent));

            btnFilterRejected.setBackgroundResource(R.drawable.bg_button_secondary);
            btnFilterRejected.setTextColor(getColor(R.color.text_primary));

            tvEmptyState.setText("Không có đơn đã duyệt");
        } else if ("REJECTED".equals(selectedStatus)) {
            btnFilterPending.setBackgroundResource(R.drawable.bg_button_secondary);
            btnFilterPending.setTextColor(getColor(R.color.text_primary));

            btnFilterApproved.setBackgroundResource(R.drawable.bg_button_secondary);
            btnFilterApproved.setTextColor(getColor(R.color.text_primary));

            btnFilterRejected.setBackgroundResource(R.drawable.bg_button_primary);
            btnFilterRejected.setTextColor(getColor(R.color.text_on_accent));

            tvEmptyState.setText("Không có đơn bị từ chối");
        }
    }

    @Override
    public void onApprove(LeaveRequestResponse request) {
        // Step 1: Show warning dialog
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận duyệt đơn")
                .setMessage("Đơn nghỉ này sẽ gỡ nhân viên khỏi các ca trùng thời gian nghỉ. Các ca đang mở hoặc đã đóng sẽ không bị thay đổi. Bạn có chắc muốn duyệt đơn này?")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    // Step 2: Show note input dialog
                    showReviewDialog(request, true);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onReject(LeaveRequestResponse request) {
        // Directly show note input dialog for rejection
        showReviewDialog(request, false);
    }

    private void showReviewDialog(LeaveRequestResponse request, boolean isApprove) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isApprove ? "Duyệt đơn xin nghỉ" : "Từ chối đơn xin nghỉ");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_leave_review, null);
        builder.setView(dialogView);

        EditText etReviewNote = dialogView.findViewById(R.id.etReviewNote);
        etReviewNote.setHint(isApprove ? "Ghi chú duyệt đơn" : "Lý do từ chối");

        builder.setPositiveButton("Xác nhận", (dialog, which) -> {
            String note = etReviewNote.getText().toString().trim();
            if (isApprove) {
                viewModel.approveRequest(request.getLeaveRequestId(), note);
            } else {
                if (note.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập lý do từ chối", Toast.LENGTH_SHORT).show();
                    return;
                }
                viewModel.rejectRequest(request.getLeaveRequestId(), note);
            }
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }
}
