package com.example.cafe_manager.util;

import android.app.Activity;
import android.widget.Toast;

import com.example.cafe_manager.manager.SessionManager;

/**
 * Helper tập trung kiểm tra quyền theo vai trò.
 * Tránh rải logic phân quyền khắp nơi.
 */
public final class PermissionUtils {

    private PermissionUtils() {}

    // ── Kiểm tra quyền từng chức năng ────────────────────────────

    public static boolean canViewReports(String role) {
        return Constants.ROLE_ADMIN.equals(role) || Constants.ROLE_MANAGER.equals(role);
    }

    public static boolean canManageMenu(String role) {
        return Constants.ROLE_ADMIN.equals(role) || Constants.ROLE_MANAGER.equals(role) || Constants.ROLE_STAFF.equals(role);
    }

    public static boolean canManageUsers(String role) {
        return Constants.ROLE_ADMIN.equals(role) || Constants.ROLE_MANAGER.equals(role);
    }

    public static boolean canManagePromotions(String role) {
        return Constants.ROLE_ADMIN.equals(role) || Constants.ROLE_MANAGER.equals(role);
    }

    public static boolean canManageTables(String role) {
        return Constants.ROLE_ADMIN.equals(role) || Constants.ROLE_MANAGER.equals(role);
    }

    public static boolean canManageCategories(String role) {
        return Constants.ROLE_ADMIN.equals(role) || Constants.ROLE_MANAGER.equals(role);
    }

    /**
     * Kiểm tra currentRole có được thay đổi role của targetRole không.
     * ADMIN: được thay đổi bất kỳ.
     * MANAGER: chỉ được tác động STAFF.
     * STAFF: không được thay đổi ai.
     */
    public static boolean canChangeRole(String currentRole, String targetRole) {
        if (Constants.ROLE_ADMIN.equals(currentRole)) {
            return true;
        }
        if (Constants.ROLE_MANAGER.equals(currentRole)) {
            return Constants.ROLE_STAFF.equals(targetRole);
        }
        return false;
    }

    /**
     * Kiểm tra role hiện tại có nằm trong danh sách allowedRoles không.
     * Nếu không → Toast + finish() activity.
     *
     * @return true nếu role hợp lệ (được phép truy cập), false nếu đã chặn.
     */
    public static boolean requireRole(Activity activity, String... allowedRoles) {
        String currentRole = SessionManager.getInstance(activity).getRole();
        for (String allowed : allowedRoles) {
            if (allowed.equals(currentRole)) {
                return true;
            }
        }
        Toast.makeText(activity,
                "Bạn không có quyền truy cập chức năng này.",
                Toast.LENGTH_SHORT).show();
        activity.finish();
        return false;
    }
}
