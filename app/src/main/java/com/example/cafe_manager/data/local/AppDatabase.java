package com.example.cafe_manager.data.local;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.cafe_manager.data.local.dao.AuditLogDao;
import com.example.cafe_manager.data.local.dao.CategoryDao;
import com.example.cafe_manager.data.local.dao.OrderDao;
import com.example.cafe_manager.data.local.dao.OrderItemDao;
import com.example.cafe_manager.data.local.dao.PaymentDao;
import com.example.cafe_manager.data.local.dao.ProductDao;
import com.example.cafe_manager.data.local.dao.PromotionDao;
import com.example.cafe_manager.data.local.dao.TableDao;
import com.example.cafe_manager.data.local.dao.UserDao;
import com.example.cafe_manager.data.local.entity.AuditLogEntity;
import com.example.cafe_manager.data.local.entity.CategoryEntity;
import com.example.cafe_manager.data.local.entity.OrderEntity;
import com.example.cafe_manager.data.local.entity.OrderItemEntity;
import com.example.cafe_manager.data.local.entity.PaymentEntity;
import com.example.cafe_manager.data.local.entity.ProductEntity;
import com.example.cafe_manager.data.local.entity.PromotionEntity;
import com.example.cafe_manager.data.local.entity.TableEntity;
import com.example.cafe_manager.data.local.entity.UserEntity;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.PasswordUtils;
import com.example.cafe_manager.data.local.dao.OrderTransactionDao;
import com.example.cafe_manager.data.local.dao.PaymentTransactionDao;
import com.example.cafe_manager.data.local.dao.AreaDao;
import com.example.cafe_manager.data.local.entity.AreaEntity;
import com.example.cafe_manager.data.local.dao.AttendanceDao;
import com.example.cafe_manager.data.local.dao.ShiftAssignmentDao;
import com.example.cafe_manager.data.local.dao.ShiftDao;
import com.example.cafe_manager.data.local.dao.ShiftTemplateDao;
import com.example.cafe_manager.data.local.entity.AttendanceEntity;
import com.example.cafe_manager.data.local.entity.ShiftAssignmentEntity;
import com.example.cafe_manager.data.local.entity.ShiftEntity;
import com.example.cafe_manager.data.local.entity.ShiftTemplateEntity;
import com.example.cafe_manager.data.local.entity.ShiftCashSessionEntity;
import com.example.cafe_manager.data.local.dao.ShiftCashSessionDao;
import com.example.cafe_manager.data.local.dao.ShiftTransactionDao;
import com.example.cafe_manager.data.local.dao.NewsPostDao;
import com.example.cafe_manager.data.local.dao.NewsReadDao;
import com.example.cafe_manager.data.local.entity.NewsPostEntity;
import com.example.cafe_manager.data.local.entity.NewsReadEntity;
import com.example.cafe_manager.data.local.dao.ChatRoomDao;
import com.example.cafe_manager.data.local.dao.ChatParticipantDao;
import com.example.cafe_manager.data.local.dao.ChatMessageDao;
import com.example.cafe_manager.data.local.dao.ChatReadDao;
import com.example.cafe_manager.data.local.entity.ChatRoomEntity;
import com.example.cafe_manager.data.local.entity.ChatParticipantEntity;
import com.example.cafe_manager.data.local.entity.ChatMessageEntity;
import com.example.cafe_manager.data.local.entity.ChatReadEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Database(
        entities = {
                TableEntity.class, AreaEntity.class, CategoryEntity.class, ProductEntity.class,
                OrderEntity.class, OrderItemEntity.class, PaymentEntity.class, UserEntity.class,
                PromotionEntity.class, AuditLogEntity.class,
                ShiftTemplateEntity.class, ShiftEntity.class, ShiftAssignmentEntity.class, AttendanceEntity.class,
                ShiftCashSessionEntity.class, NewsPostEntity.class, NewsReadEntity.class,
                ChatRoomEntity.class, ChatParticipantEntity.class, ChatMessageEntity.class, ChatReadEntity.class
        },
        version = 13,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TableDao tableDao();
    public abstract AreaDao areaDao();
    public abstract CategoryDao categoryDao();
    public abstract ProductDao productDao();
    public abstract OrderDao orderDao();
    public abstract OrderItemDao orderItemDao();
    public abstract PaymentDao paymentDao();
    public abstract ShiftTemplateDao shiftTemplateDao();
    public abstract ShiftDao shiftDao();
    public abstract ShiftAssignmentDao shiftAssignmentDao();
    public abstract AttendanceDao attendanceDao();
    public abstract PromotionDao promotionDao();
    public abstract UserDao userDao();
    public abstract AuditLogDao auditLogDao();
    public abstract OrderTransactionDao orderTransactionDao();
    public abstract PaymentTransactionDao paymentTransactionDao();
    public abstract ShiftCashSessionDao shiftCashSessionDao();
    public abstract ShiftTransactionDao shiftTransactionDao();
    public abstract NewsPostDao newsPostDao();
    public abstract NewsReadDao newsReadDao();
    public abstract ChatRoomDao chatRoomDao();
    public abstract ChatParticipantDao chatParticipantDao();
    public abstract ChatMessageDao chatMessageDao();
    public abstract ChatReadDao chatReadDao();

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
                            .fallbackToDestructiveMigration(true)
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
                if (database != null) {
                    seedDatabase(database);
                }
            });
        }

        @Override
        public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db) {
            super.onDestructiveMigration(db);
            AppExecutors.getInstance().diskIO().execute(() -> {
                AppDatabase database = instance;
                if (database != null) {
                    seedDatabase(database);
                }
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

    private static void seedDatabase(AppDatabase db) {
         seedAreas(db);
         seedTables(db);
         long[] categoryIds = seedCategories(db);
         seedProducts(db, categoryIds);
         seedPromotions(db);
         seedUsers(db);
         seedShiftTemplates(db);
         seedNews(db);
         seedChatRooms(db);
         seedShiftChatRooms(db);
         // Đã loại bỏ seedActiveShift để không làm kẹt hệ thống
    }

    private static void seedTables(AppDatabase db) {
        String[] names = {"B01","B02","B03","B04","B05","B06","B07","B08","B09","B10"};
        int[] caps = {2, 2, 2, 2, 4, 4, 4, 4, 6, 6};
        String[] areas = {"Tầng 1", "Tầng 1", "Tầng 1", "Tầng 1", "Tầng 2", "Tầng 2", "Tầng 2", "Ngoài trời", "Ngoài trời", "VIP"};

        TableEntity[] tables = new TableEntity[names.length];
        for (int i = 0; i < names.length; i++) {
            TableEntity t = new TableEntity();
            t.setTableName(names[i]);
            t.setStatus(Constants.TABLE_EMPTY);
            t.setCapacity(caps[i]);
            t.setArea(areas[i]);
            tables[i] = t;
        }
        db.tableDao().insertAll(Arrays.asList(tables));
    }

    private static long[] seedCategories(AppDatabase db) {
        String[][] cats = { {"Cà phê", "Các loại cà phê"}, {"Trà", "Các loại trà"}, {"Sinh tố", "Các loại sinh tố"}, {"Bánh", "Các loại bánh"} };
        long[] ids = new long[cats.length];
        for (int i = 0; i < cats.length; i++) {
            CategoryEntity c = new CategoryEntity();
            c.setCategoryName(cats[i][0]);
            c.setDescription(cats[i][1]);
            c.setActive(true);
            ids[i] = db.categoryDao().insert(c);
        }
        return ids;
    }

    private static void seedProducts(AppDatabase db, long[] categoryIds) {
        Object[][] products = { {0, "Cà phê sữa đá", 35000.0}, {0, "Bạc xỉu", 38000.0}, {1, "Trà sữa trân châu", 45000.0}, {1, "Trà đào cam sả", 45000.0}, {2, "Sinh tố bơ", 50000.0}, {3, "Bánh Tiramisu", 55000.0} };
        for (Object[] p : products) {
            ProductEntity product = new ProductEntity();
            product.setCategoryId((int) categoryIds[(int)p[0]]);
            product.setProductName((String)p[1]);
            product.setPrice((double)p[2]);
            product.setActive(true);
            db.productDao().insert(product);
        }
    }

    private static void seedPromotions(AppDatabase db) {
        Object[][] promos = { {"CAFE10K", Constants.PROMO_CASH, 10000.0}, {"WELCOME50", Constants.PROMO_CASH, 50000.0}, {"MEMBER20", Constants.PROMO_PERCENT, 20.0} };
        long now = System.currentTimeMillis();
        for (Object[] row : promos) {
            PromotionEntity p = new PromotionEntity();
            p.setCode((String) row[0]);
            p.setType((String) row[1]);
            p.setValue((double) row[2]);
            p.setActive(true);
            p.setExpiresAt(0);
            p.setCreatedAt(now);
            db.promotionDao().insert(p);
        }
    }

    private static void seedAreas(AppDatabase db) {
        long now = System.currentTimeMillis();
        String[][] areas = { {"Tầng 1", "A"}, {"Tầng 2", "B"}, {"Ngoài trời", "C"}, {"VIP", "VIP"} };
        List<AreaEntity> list = new ArrayList<>();
        for (String[] row : areas) {
            list.add(new AreaEntity(row[0], row[1], now));
        }
        db.areaDao().insertAll(list);
    }

    private static void seedUsers(AppDatabase db) {
        long now = System.currentTimeMillis();
        Object[][] users = { {"admin", "admin123", "Quản trị viên", Constants.ROLE_ADMIN}, {"manager", "manager123", "Quản lý Demo", Constants.ROLE_MANAGER}, {"staff", "123456", "Nhân viên Demo", Constants.ROLE_STAFF} };
        for (Object[] row : users) {
            UserEntity u = new UserEntity();
            u.setUsername((String) row[0]);
            u.setPasswordHash(PasswordUtils.hashPassword((String) row[1]));
            u.setFullName((String) row[2]);
            u.setRole((String) row[3]);
            u.setActive(true);
            u.setCreatedAt(now);
            u.setUpdatedAt(now);
            db.userDao().insert(u);
        }
    }

    private static void seedNews(AppDatabase db) {
        long now = System.currentTimeMillis();
        NewsPostEntity welcome = new NewsPostEntity();
        welcome.setTitle("Chào mừng đến với Cafe Manager");
        welcome.setContent("Đây là thông báo mẫu. Các nhân viên có thể xem thông báo từ quản lý tại đây.");
        welcome.setType("GENERAL");
        welcome.setPriority("NORMAL");
        welcome.setTargetType("ALL");
        welcome.setCreatedByUserId(1); // admin
        welcome.setCreatedAt(now);
        welcome.setIsPinned(true);
        welcome.setIsDeleted(false);
        db.newsPostDao().insert(welcome);
    }

    private static void seedChatRooms(AppDatabase db) {
        long now = System.currentTimeMillis();
        
        // 1. Get users by role
        List<UserEntity> admins = db.userDao().getByRoleSync(Constants.ROLE_ADMIN);
        List<UserEntity> managers = db.userDao().getByRoleSync(Constants.ROLE_MANAGER);
        List<UserEntity> staffs = db.userDao().getByRoleSync(Constants.ROLE_STAFF);

        int creatorId = 1; // Fallback to user_id = 1 (admin)
        if (!admins.isEmpty()) {
            creatorId = admins.get(0).getUserId();
        } else if (!managers.isEmpty()) {
            creatorId = managers.get(0).getUserId();
        }

        // 2. Create "Tất cả quản lý" (Role: MANAGER)
        ChatRoomEntity managersRoom = new ChatRoomEntity();
        managersRoom.setRoomName("Tất cả quản lý");
        managersRoom.setRoomType(Constants.CHAT_TYPE_ROLE);
        managersRoom.setTargetRole(Constants.ROLE_MANAGER);
        managersRoom.setCreatedBy(creatorId);
        managersRoom.setCreatedAt(now);
        managersRoom.setUpdatedAt(now);
        managersRoom.setIsActive(true);
        int managersRoomId = (int) db.chatRoomDao().insert(managersRoom);

        // Add all admins and managers as participants to managersRoom
        for (UserEntity u : admins) {
            ChatParticipantEntity p = new ChatParticipantEntity();
            p.setRoomId(managersRoomId);
            p.setUserId(u.getUserId());
            p.setJoinedAt(now);
            p.setRoleInRoom(Constants.CHAT_ROLE_MEMBER);
            db.chatParticipantDao().insert(p);
        }
        for (UserEntity u : managers) {
            ChatParticipantEntity p = new ChatParticipantEntity();
            p.setRoomId(managersRoomId);
            p.setUserId(u.getUserId());
            p.setJoinedAt(now);
            p.setRoleInRoom(Constants.CHAT_ROLE_MEMBER);
            db.chatParticipantDao().insert(p);
        }

        // 3. Create "Tất cả nhân viên" (Role: STAFF)
        ChatRoomEntity staffsRoom = new ChatRoomEntity();
        staffsRoom.setRoomName("Tất cả nhân viên");
        staffsRoom.setRoomType(Constants.CHAT_TYPE_ROLE);
        staffsRoom.setTargetRole(Constants.ROLE_STAFF);
        staffsRoom.setCreatedBy(creatorId);
        staffsRoom.setCreatedAt(now);
        staffsRoom.setUpdatedAt(now);
        staffsRoom.setIsActive(true);
        int staffsRoomId = (int) db.chatRoomDao().insert(staffsRoom);

        // Add all staffs as participants
        for (UserEntity u : staffs) {
            ChatParticipantEntity p = new ChatParticipantEntity();
            p.setRoomId(staffsRoomId);
            p.setUserId(u.getUserId());
            p.setJoinedAt(now);
            p.setRoleInRoom(Constants.CHAT_ROLE_MEMBER);
            db.chatParticipantDao().insert(p);
        }
    }

    private static void seedShiftChatRooms(AppDatabase db) {
        try {
            List<com.example.cafe_manager.data.local.entity.ShiftEntity> shifts = db.shiftDao().getAllSync();
            if (shifts != null) {
                for (com.example.cafe_manager.data.local.entity.ShiftEntity shift : shifts) {
                    com.example.cafe_manager.data.repository.ChatRepository.syncShiftChatRoomSync(db, shift.getShiftId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
