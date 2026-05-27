package com.example.cafe_manager.ui.common;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cafe_manager.R;

/**
 * Base class cho tất cả Activity — tự apply slide animation
 * khi startActivity hoặc finish. Activity con extends class này
 * thay vì AppCompatActivity trực tiếp.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
