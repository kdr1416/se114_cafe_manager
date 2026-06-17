package com.example.cafe_manager.data.local;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.cafe_manager.data.local.dao.*;
import com.example.cafe_manager.data.local.entity.*;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.Constants;

import java.util.Arrays;

@Database(
        entities = {
                TableEntity.class, AreaEntity.class, CategoryEntity.class, ProductEntity.class,
                OrderEntity.class, OrderItemEntity.class, PaymentEntity.class, UserEntity.class,
                PromotionEntity.class, AuditLogEntity.class,
                ShiftTemplateEntity.class, ShiftEntity.class, ShiftAssignmentEntity.class, AttendanceEntity.class
        },
        version = 8,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    // Existing DAOs
    public abstract TableDao tableDao();
    public abstract CategoryDao categoryDao();
    public abstract ProductDao productDao();
    public abstract OrderDao orderDao();
    public abstract OrderItemDao orderItemDao();
    public abstract PaymentDao paymentDao();
    // Assuming other DAOs like UserDao, etc. exist
    
    // New DAOs
    public abstract ShiftTemplateDao shiftTemplateDao();
    public abstract ShiftDao shiftDao();
    public abstract ShiftAssignmentDao shiftAssignmentDao();
    public abstract AttendanceDao attendanceDao();

    private static volatile AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "cafe_manager.db"
                            )
                            .fallbackToDestructiveMigration()
                            .addCallback(seedCallback)
                            .build();
                }
            }
        }
        return instance;
    }

    private static final Callback seedCallback = new Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            AppExecutors.getInstance().diskIO().execute(() -> {
                AppDatabase database = instance;
                if (database == null) return;

                // Các logic seed cũ...
                seedTables(database);
                long[] categoryIds = seedCategories(database);
                seedProducts(database, categoryIds);
                
                // Seed thêm dữ liệu cho Shift
                seedShiftTemplates(database);
            });
        }
    };

    private static void seedShiftTemplates(AppDatabase db) {
        long now = System.currentTimeMillis();

        ShiftTemplateEntity caSang = new ShiftTemplateEntity();
        caSang.setTemplateName("Ca Sáng");
        caSang.setStartTime("06:00");
        caSang.setEndTime("14:00");
        caSang.setMinStaff(2);
        caSang.setActive(true);
        caSang.setCreatedAt(now);
        db.shiftTemplateDao().insert(caSang);

        ShiftTemplateEntity caChieu = new ShiftTemplateEntity();
        caChieu.setTemplateName("Ca Chiều");
        caChieu.setStartTime("14:00");
        caChieu.setEndTime("22:00");
        caChieu.setMinStaff(2);
        caChieu.setActive(true);
        caChieu.setCreatedAt(now);
        db.shiftTemplateDao().insert(caChieu);

        ShiftTemplateEntity caDem = new ShiftTemplateEntity();
        caDem.setTemplateName("Ca Tối/Đêm");
        caDem.setStartTime("22:00");
        caDem.setEndTime("06:00");
        caDem.setMinStaff(1);
        caDem.setActive(true);
        caDem.setCreatedAt(now);
        db.shiftTemplateDao().insert(caDem);
    }

    // Các hàm seed cũ giữ nguyên...
    private static void seedTables(AppDatabase db) { /* ... */ }
    private static long[] seedCategories(AppDatabase db) { /* ... */ return new long[]{}; }
    private static void seedProducts(AppDatabase db, long[] categoryIds) { /* ... */ }
}