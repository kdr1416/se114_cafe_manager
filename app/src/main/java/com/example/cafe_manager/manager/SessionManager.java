package com.example.cafe_manager.manager;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.cafe_manager.data.local.entity.UserEntity;
import com.example.cafe_manager.util.Constants;

/**
 * Session persistence qua SharedPreferences.
 * Singleton — gọi getInstance(context) ở mọi nơi.
 */
public class SessionManager {

    private static final String PREFS_NAME = "cafe_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_ROLE = "role";
    private static final String KEY_LOGGED_IN = "is_logged_in";

    private static volatile SessionManager instance;
    private final SharedPreferences prefs;

    private SessionManager(Context appContext) {
        this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static SessionManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /** Lưu toàn bộ thông tin session sau khi đăng nhập thành công. */
    public void saveLoginSession(UserEntity user) {
        prefs.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putInt(KEY_USER_ID, user.getUserId())
                .putString(KEY_USERNAME, user.getUsername())
                .putString(KEY_FULL_NAME, user.getFullName())
                .putString(KEY_ROLE, user.getRole())
                .apply();
    }

    /**
     * @deprecated Dùng {@link #saveLoginSession(UserEntity)} thay thế.
     */
    @Deprecated
    public void saveSession(UserEntity user) {
        saveLoginSession(user);
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public String getFullName() {
        return prefs.getString(KEY_FULL_NAME, "");
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, "");
    }

    public boolean isAdmin() {
        return Constants.ROLE_ADMIN.equals(getRole());
    }

    public boolean isManager() {
        return Constants.ROLE_MANAGER.equals(getRole());
    }

    public boolean isStaff() {
        return Constants.ROLE_STAFF.equals(getRole());
    }

    public void logout() {
        prefs.edit().clear().apply();
    }
}
