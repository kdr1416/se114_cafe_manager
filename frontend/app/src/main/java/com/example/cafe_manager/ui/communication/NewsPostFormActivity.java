package com.example.cafe_manager.ui.communication;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.AppDatabase;
import com.example.cafe_manager.data.local.entity.NewsPostEntity;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.manager.SessionManager;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.RepositoryCallback;
import com.example.cafe_manager.viewmodel.NewsViewModel;
import com.example.cafe_manager.data.remote.UploadApiService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class NewsPostFormActivity extends AppCompatActivity {

    private int postId = -1;
    private NewsViewModel viewModel;
    private AppDatabase db;
    private final SimpleDateFormat shiftDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    {
        shiftDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    private EditText etTitle;
    private EditText etContent;
    private Spinner spinnerType;
    private Spinner spinnerPriority;
    private Spinner spinnerTargetType;
    private Spinner spinnerTargetRole;
    private Spinner spinnerTargetShift;
    private SwitchCompat switchPinned;
    private Button btnSave;

    private View layoutTargetRole;
    private View layoutTargetShift;

    private Button btnSelectImage;
    private TextView tvImagePath;
    private android.widget.ImageView ivImagePreview;

    private androidx.activity.result.ActivityResultLauncher<String> imagePickerLauncher;
    private android.net.Uri selectedImageUri = null;
    private String serverImageUrl = "";

    // Keys mapping
    private final String[] typeKeys = {"GENERAL", "MEETING", "SHIFT", "RULE", "URGENT", "PROMOTION", "STOCK"};
    private final String[] typeNames = {"Thông báo chung", "Họp nhân viên", "Ca làm việc", "Nội quy/Quy định", "Thông tin khẩn cấp", "Chương trình khuyến mãi", "Quản lý kho hàng"};

    private final String[] priorityKeys = {"NORMAL", "IMPORTANT", "URGENT"};
    private final String[] priorityNames = {"Thường (Normal)", "Quan trọng (Important)", "Khẩn cấp (Urgent)"};

    private final String[] targetTypeKeys = {"ALL", "ROLE", "SHIFT"};
    private final String[] targetTypeNames = {"Tất cả nhân viên", "Theo vai trò (Role)", "Theo ca làm việc (Shift)"};

    private final String[] targetRoleKeys = {Constants.ROLE_STAFF, Constants.ROLE_MANAGER};
    private final String[] targetRoleNames = {"Nhân viên (Staff)", "Quản lý (Manager)"};

    private final List<ShiftEntity> shiftsList = new ArrayList<>();
    private final List<String> shiftsDisplayList = new ArrayList<>();
    private ArrayAdapter<String> shiftsAdapter;

    private NewsPostEntity existingPost;
    private int currentUserId;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_news_post_form);

        db = AppDatabase.getInstance(this);
        currentUserId = SessionManager.getInstance(this).getUserId();

        // Check permissions - only ADMIN and MANAGER can create/edit posts
        if (!com.example.cafe_manager.util.PermissionUtils.requireRole(this,
                com.example.cafe_manager.util.Constants.ROLE_ADMIN,
                com.example.cafe_manager.util.Constants.ROLE_MANAGER)) {
            return;
        }

        postId = getIntent().getIntExtra("post_id", -1);
        isEditMode = (postId != -1);

        imagePickerLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        tvImagePath.setText("Đã chọn: " + uri.getLastPathSegment());
                        ivImagePreview.setVisibility(View.VISIBLE);
                        com.bumptech.glide.Glide.with(this).load(uri).into(ivImagePreview);
                    }
                }
        );

        initViews();
        setupTopBar();
        setupSpinners();
        setupViewModel();
        loadShifts();

        if (isEditMode) {
            loadExistingPost();
        }
    }

    private void initViews() {
        etTitle = findViewById(R.id.et_form_title);
        etContent = findViewById(R.id.et_form_content);
        spinnerType = findViewById(R.id.spinner_form_type);
        spinnerPriority = findViewById(R.id.spinner_form_priority);
        spinnerTargetType = findViewById(R.id.spinner_form_target_type);
        spinnerTargetRole = findViewById(R.id.spinner_form_target_role);
        spinnerTargetShift = findViewById(R.id.spinner_form_target_shift);
        switchPinned = findViewById(R.id.switch_form_pinned);
        btnSave = findViewById(R.id.btn_form_save);

        layoutTargetRole = findViewById(R.id.layout_target_role);
        layoutTargetShift = findViewById(R.id.layout_target_shift);

        btnSelectImage = findViewById(R.id.btn_form_select_image);
        tvImagePath = findViewById(R.id.tv_image_path);
        ivImagePreview = findViewById(R.id.iv_form_image_preview);

        btnSelectImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);

        title.setText(isEditMode ? "Sửa thông báo" : "Tạo thông báo");
        caption.setText(isEditMode ? "Chỉnh sửa thông báo đã đăng" : "Tạo thông báo mới trên bảng tin");
        btnBack.setOnClickListener(v -> finish());

        btnSave.setText(isEditMode ? "Cập nhật thông báo" : "Đăng thông báo");
        btnSave.setOnClickListener(v -> uploadAndSave());
    }

    private void setupSpinners() {
        // 1. Type
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, typeNames);
        spinnerType.setAdapter(typeAdapter);

        // 2. Priority
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, priorityNames);
        spinnerPriority.setAdapter(priorityAdapter);

        // 3. Target Type
        ArrayAdapter<String> targetTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, targetTypeNames);
        spinnerTargetType.setAdapter(targetTypeAdapter);
        spinnerTargetType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String targetType = targetTypeKeys[position];
                layoutTargetRole.setVisibility("ROLE".equals(targetType) ? View.VISIBLE : View.GONE);
                layoutTargetShift.setVisibility("SHIFT".equals(targetType) ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 4. Target Role
        ArrayAdapter<String> targetRoleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, targetRoleNames);
        spinnerTargetRole.setAdapter(targetRoleAdapter);

        // 5. Target Shift
        shiftsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, shiftsDisplayList);
        spinnerTargetShift.setAdapter(shiftsAdapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(NewsViewModel.class);

        viewModel.getMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                if (msg.contains("thành công") || msg.contains("Đã đăng") || msg.contains("Đã cập nhật")) {
                    viewModel.clearMessage();
                    finish();
                }
            }
        });
    }

    private void loadShifts() {
        db.shiftDao().getAll().observe(this, shifts -> {
            shiftsList.clear();
            shiftsDisplayList.clear();
            if (shifts != null) {
                shiftsList.addAll(shifts);
                for (ShiftEntity s : shifts) {
                    String dateStr = shiftDateFormat.format(new Date(s.getShiftDate()));
                    shiftsDisplayList.add(s.getShiftName() + " (" + dateStr + " " + s.getStartTime() + " - " + s.getEndTime() + ")");
                }
            }
            shiftsAdapter.notifyDataSetChanged();

            // Select active shift if we were in edit mode and just loaded the list
            if (existingPost != null && existingPost.getTargetShiftId() != null) {
                selectShiftInSpinner(existingPost.getTargetShiftId());
            }
        });
    }

    private void selectShiftInSpinner(int shiftId) {
        for (int i = 0; i < shiftsList.size(); i++) {
            if (shiftsList.get(i).getShiftId() == shiftId) {
                spinnerTargetShift.setSelection(i);
                break;
            }
        }
    }

    private void loadExistingPost() {
        viewModel.getPostById(postId, new RepositoryCallback<NewsPostEntity>() {
            @Override
            public void onSuccess(NewsPostEntity post) {
                existingPost = post;
                prefillForm(post);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(NewsPostFormActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void prefillForm(NewsPostEntity post) {
        etTitle.setText(post.getTitle());
        etContent.setText(post.getContent());
        switchPinned.setChecked(post.getIsPinned());

        // Type
        int typeIdx = findIndex(typeKeys, post.getType());
        if (typeIdx >= 0) spinnerType.setSelection(typeIdx);

        // Priority
        int priorityIdx = findIndex(priorityKeys, post.getPriority());
        if (priorityIdx >= 0) spinnerPriority.setSelection(priorityIdx);

        // Target Type
        int targetTypeIdx = findIndex(targetTypeKeys, post.getTargetType());
        if (targetTypeIdx >= 0) spinnerTargetType.setSelection(targetTypeIdx);

        // Target Role
        if ("ROLE".equals(post.getTargetType())) {
            int roleIdx = findIndex(targetRoleKeys, post.getTargetRole());
            if (roleIdx >= 0) spinnerTargetRole.setSelection(roleIdx);
        }

        // Target Shift
        if ("SHIFT".equals(post.getTargetType()) && post.getTargetShiftId() != null) {
            selectShiftInSpinner(post.getTargetShiftId());
        }

        if (post.getImageUrl() != null && !post.getImageUrl().trim().isEmpty()) {
            serverImageUrl = post.getImageUrl();
            tvImagePath.setText("Đã có ảnh banner");
            ivImagePreview.setVisibility(View.VISIBLE);
            com.bumptech.glide.Glide.with(this)
                    .load(post.getImageUrl().trim())
                    .into(ivImagePreview);
        }
    }

    private int findIndex(String[] array, String value) {
        if (value == null) return -1;
        for (int i = 0; i < array.length; i++) {
            if (value.equals(array[i])) return i;
        }
        return -1;
    }

    private void uploadAndSave() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Tiêu đề không được rỗng", Toast.LENGTH_SHORT).show();
            return;
        }
        if (content.isEmpty()) {
            Toast.makeText(this, "Nội dung không được rỗng", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri != null) {
            uploadSelectedImage();
        } else {
            savePostData();
        }
    }

    private void uploadSelectedImage() {
        btnSave.setEnabled(false);
        btnSave.setText("Đang tải ảnh lên...");

        try {
            android.content.ContentResolver resolver = getContentResolver();
            String mimeType = resolver.getType(selectedImageUri);
            if (mimeType == null) {
                mimeType = "image/jpeg";
            }

            java.io.InputStream inputStream = resolver.openInputStream(selectedImageUri);
            if (inputStream == null) {
                throw new java.io.IOException("Không thể đọc file ảnh");
            }

            byte[] bytes = readAllBytes(inputStream);
            okhttp3.RequestBody requestFile = okhttp3.RequestBody.create(
                    okhttp3.MediaType.parse(mimeType),
                    bytes
            );

            okhttp3.MultipartBody.Part body = okhttp3.MultipartBody.Part.createFormData(
                    "file",
                    "upload_" + System.currentTimeMillis() + ".jpg",
                    requestFile
            );

            com.example.cafe_manager.data.remote.ApiClient apiClient = com.example.cafe_manager.data.remote.ApiClient.getInstance(this);
            UploadApiService uploadApi = apiClient.getService(UploadApiService.class);
            
            uploadApi.uploadFile(body, "news").enqueue(new retrofit2.Callback<java.util.Map<String, String>>() {
                @Override
                public void onResponse(retrofit2.Call<java.util.Map<String, String>> call, retrofit2.Response<java.util.Map<String, String>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        serverImageUrl = response.body().get("url");
                        savePostData();
                    } else {
                        btnSave.setEnabled(true);
                        btnSave.setText(isEditMode ? "Cập nhật thông báo" : "Đăng thông báo");
                        Toast.makeText(NewsPostFormActivity.this, "Tải ảnh thất bại: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<java.util.Map<String, String>> call, Throwable t) {
                    btnSave.setEnabled(true);
                    btnSave.setText(isEditMode ? "Cập nhật thông báo" : "Đăng thông báo");
                    Toast.makeText(NewsPostFormActivity.this, "Lỗi mạng khi tải ảnh: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            btnSave.setEnabled(true);
            btnSave.setText(isEditMode ? "Cập nhật thông báo" : "Đăng thông báo");
            Toast.makeText(this, "Lỗi chuẩn bị file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] readAllBytes(java.io.InputStream inputStream) throws java.io.IOException {
        java.io.ByteArrayOutputStream byteBuffer = new java.io.ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private void savePostData() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        String typeKey = typeKeys[spinnerType.getSelectedItemPosition()];
        String priorityKey = priorityKeys[spinnerPriority.getSelectedItemPosition()];
        String targetTypeKey = targetTypeKeys[spinnerTargetType.getSelectedItemPosition()];

        String targetRoleKey = null;
        if ("ROLE".equals(targetTypeKey)) {
            targetRoleKey = targetRoleKeys[spinnerTargetRole.getSelectedItemPosition()];
        }

        int targetShiftId = -1;
        if ("SHIFT".equals(targetTypeKey)) {
            int selectedPos = spinnerTargetShift.getSelectedItemPosition();
            if (selectedPos >= 0 && selectedPos < shiftsList.size()) {
                targetShiftId = shiftsList.get(selectedPos).getShiftId();
            }
        }

        if (isEditMode && existingPost != null) {
            existingPost.setTitle(title);
            existingPost.setContent(content);
            existingPost.setType(typeKey);
            existingPost.setPriority(priorityKey);
            existingPost.setTargetType(targetTypeKey);
            existingPost.setIsPinned(switchPinned.isChecked());
            existingPost.setUpdatedAt(System.currentTimeMillis());
            existingPost.setImageUrl(serverImageUrl);

            if ("ROLE".equals(targetTypeKey)) {
                existingPost.setTargetRole(targetRoleKey);
                existingPost.setTargetShiftId(null);
            } else if ("SHIFT".equals(targetTypeKey)) {
                existingPost.setTargetRole(null);
                existingPost.setTargetShiftId(targetShiftId);
            } else {
                existingPost.setTargetRole(null);
                existingPost.setTargetShiftId(null);
            }

            viewModel.updatePost(existingPost, () -> {
                Toast.makeText(this, "Đã cập nhật thông báo thành công", Toast.LENGTH_SHORT).show();
                finish();
            });
        } else {
            viewModel.createPost(title, content, typeKey, priorityKey, targetTypeKey, targetRoleKey, targetShiftId, currentUserId, serverImageUrl);
        }
    }
}
