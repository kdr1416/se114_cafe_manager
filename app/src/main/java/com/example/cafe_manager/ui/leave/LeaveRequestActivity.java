package com.example.cafe_manager.ui.leave;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
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
import com.example.cafe_manager.viewmodel.LeaveRequestViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class LeaveRequestActivity extends AppCompatActivity {

    private LeaveRequestViewModel viewModel;
    private LeaveRequestAdapter adapter;

    private TextView tvPrefilledShiftInfo;
    private TextView tvStartDateTime;
    private TextView tvEndDateTime;
    private EditText etReason;
    private Button btnSubmitLeave;
    private ProgressBar progressLoading;
    private TextView tvEmptyState;

    private Long selectedStartAt;
    private Long selectedEndAt;
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_leave_request);

        viewModel = new ViewModelProvider(this).get(LeaveRequestViewModel.class);

        setupTopBar();
        bindViews();
        setupRecyclerView();
        observeViewModel();
        parseIntentExtras();
        
        viewModel.loadMyRequests();
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);
        View btnRight = topBar.findViewById(R.id.btn_right);

        title.setText("Đơn xin nghỉ");
        caption.setText("Gửi yêu cầu nghỉ và theo dõi trạng thái");
        btnBack.setOnClickListener(v -> finish());
        btnRight.setVisibility(View.GONE);
    }

    private void bindViews() {
        tvPrefilledShiftInfo = findViewById(R.id.tvPrefilledShiftInfo);
        tvStartDateTime = findViewById(R.id.tvStartDateTime);
        tvEndDateTime = findViewById(R.id.tvEndDateTime);
        etReason = findViewById(R.id.etReason);
        btnSubmitLeave = findViewById(R.id.btnSubmitLeave);
        progressLoading = findViewById(R.id.progressLoading);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        tvStartDateTime.setOnClickListener(v -> showDateTimePicker(true));
        tvEndDateTime.setOnClickListener(v -> showDateTimePicker(false));
        btnSubmitLeave.setOnClickListener(v -> {
            String reason = etReason.getText().toString();
            viewModel.submitLeaveRequest(selectedStartAt, selectedEndAt, reason);
        });
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerLeaveRequests);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaveRequestAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getMyRequests().observe(this, list -> {
            adapter.setItems(list);
            int size = list != null ? list.size() : 0;
            tvEmptyState.setVisibility(size == 0 ? View.VISIBLE : View.GONE);
        });

        viewModel.getLoading().observe(this, isLoading -> {
            progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getSubmitting().observe(this, isSubmitting -> {
            btnSubmitLeave.setEnabled(!isSubmitting);
            if (isSubmitting) {
                btnSubmitLeave.setText("Đang gửi...");
            } else {
                btnSubmitLeave.setText("Gửi đơn xin nghỉ");
            }
        });

        viewModel.getMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
                
                // Clear form on success
                etReason.setText("");
                tvPrefilledShiftInfo.setVisibility(View.GONE);
                selectedStartAt = null;
                selectedEndAt = null;
                tvStartDateTime.setText("Chọn thời gian bắt đầu...");
                tvStartDateTime.setTextColor(getColor(R.color.text_soft));
                tvEndDateTime.setText("Chọn thời gian kết thúc...");
                tvEndDateTime.setTextColor(getColor(R.color.text_soft));
            }
        });

        viewModel.getError().observe(this, err -> {
            if (err != null) {
                Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
    }

    private void parseIntentExtras() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("EXTRA_START_AT")) {
                long start = intent.getLongExtra("EXTRA_START_AT", 0);
                if (start > 0) {
                    selectedStartAt = start;
                    tvStartDateTime.setText(dateTimeFormat.format(new Date(selectedStartAt)));
                    tvStartDateTime.setTextColor(getColor(R.color.text_primary));
                }
            }
            if (intent.hasExtra("EXTRA_END_AT")) {
                long end = intent.getLongExtra("EXTRA_END_AT", 0);
                if (end > 0) {
                    selectedEndAt = end;
                    tvEndDateTime.setText(dateTimeFormat.format(new Date(selectedEndAt)));
                    tvEndDateTime.setTextColor(getColor(R.color.text_primary));
                }
            }
            if (intent.hasExtra("EXTRA_SHIFT_NAME")) {
                String shiftName = intent.getStringExtra("EXTRA_SHIFT_NAME");
                if (shiftName != null && !shiftName.trim().isEmpty()) {
                    tvPrefilledShiftInfo.setText("Xin nghỉ cho ca: " + shiftName.trim());
                    tvPrefilledShiftInfo.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void showDateTimePicker(boolean isStart) {
        Calendar current = Calendar.getInstance();
        if (isStart && selectedStartAt != null) {
            current.setTimeInMillis(selectedStartAt);
        } else if (!isStart && selectedEndAt != null) {
            current.setTimeInMillis(selectedEndAt);
        }

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth, hourOfDay, minute, 0);
                selected.set(Calendar.MILLISECOND, 0);
                
                long epoch = selected.getTimeInMillis();
                if (isStart) {
                    selectedStartAt = epoch;
                    tvStartDateTime.setText(dateTimeFormat.format(new Date(epoch)));
                    tvStartDateTime.setTextColor(getColor(R.color.text_primary));
                } else {
                    selectedEndAt = epoch;
                    tvEndDateTime.setText(dateTimeFormat.format(new Date(epoch)));
                    tvEndDateTime.setTextColor(getColor(R.color.text_primary));
                }
            }, current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE), true).show();
        }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH)).show();
    }
}
