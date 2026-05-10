package com.example.cafe_manager.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.cafe_manager.data.local.dao.CategoryDao;
import com.example.cafe_manager.data.local.dao.OrderDao;
import com.example.cafe_manager.data.local.dao.OrderItemDao;
import com.example.cafe_manager.data.local.dao.PaymentDao;
import com.example.cafe_manager.data.local.dao.ProductDao;
import com.example.cafe_manager.data.local.dao.TableDao;
import com.example.cafe_manager.data.local.entity.CategoryEntity;
import com.example.cafe_manager.data.local.entity.OrderEntity;
import com.example.cafe_manager.data.local.entity.OrderItemEntity;
import com.example.cafe_manager.data.local.entity.PaymentEntity;
import com.example.cafe_manager.data.local.entity.ProductEntity;
import com.example.cafe_manager.data.local.entity.TableEntity;
import com.example.cafe_manager.util.AppExecutors;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.data.local.dao.OrderTransactionDao;
import com.example.cafe_manager.data.local.dao.PaymentTransactionDao;

import java.util.Arrays;
import java.util.List;

@Database(
        entities = {
                TableEntity.class,
                CategoryEntity.class,
                ProductEntity.class,
                OrderEntity.class,
                OrderItemEntity.class,
                PaymentEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TableDao tableDao();
    public abstract CategoryDao categoryDao();
    public abstract ProductDao productDao();
    public abstract OrderDao orderDao();
    public abstract OrderItemDao orderItemDao();
    public abstract PaymentDao paymentDao();
    public abstract OrderTransactionDao orderTransactionDao();
    public abstract PaymentTransactionDao paymentTransactionDao();


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

                seedTables(database);
                long[] categoryIds = seedCategories(database);
                seedProducts(database, categoryIds);
            });
        }
    };

    // ── Seed 10 bàn ──────────────────────────────────────────────
    private static void seedTables(AppDatabase db) {
        // capacity: B01-B04 → 2 người, B05-B08 → 4 người, B09-B10 → 6 người
        String[] names = {"B01","B02","B03","B04","B05","B06","B07","B08","B09","B10"};
        int[]   caps  = {  2,   2,   2,   2,   4,   4,   4,   4,   6,   6  };

        TableEntity[] tables = new TableEntity[names.length];
        for (int i = 0; i < names.length; i++) {
            TableEntity t = new TableEntity();
            t.setTableName(names[i]);
            t.setStatus(Constants.TABLE_EMPTY);
            t.setCapacity(caps[i]);
            tables[i] = t;
        }
        db.tableDao().insertAll(Arrays.asList(tables));
    }

    // ── Seed 4 danh mục, trả về mảng id ──────────────────────────
    private static long[] seedCategories(AppDatabase db) {
        String[][] cats = {
                {"Cà phê",  "Các loại cà phê"},
                {"Trà",     "Các loại trà"},
                {"Sinh tố", "Các loại sinh tố"},
                {"Bánh",    "Các loại bánh"}
        };

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

    // ── Seed 6 sản phẩm mẫu ─────────────────────────────────────
    private static void seedProducts(AppDatabase db, long[] categoryIds) {
        // categoryIds: [0]=Cà phê, [1]=Trà, [2]=Sinh tố, [3]=Bánh
        Object[][] products = {
                // {categoryIndex, name, price}
                {0, "Cà phê sữa đá",        35000.0},
                {0, "Bạc xỉu",              38000.0},
                {1, "Trà sữa trân châu",    45000.0},
                {1, "Trà đào cam sả",       45000.0},
                {2, "Sinh tố bơ",           50000.0},
                {3, "Bánh Tiramisu",        55000.0}
        };

        for (Object[] p : products) {
            int    catIdx = (int)    p[0];
            String name   = (String) p[1];
            double price  = (double) p[2];

            ProductEntity product = new ProductEntity();
            product.setCategoryId((int) categoryIds[catIdx]);
            product.setProductName(name);
            product.setPrice(price);
            product.setActive(true);
            db.productDao().insert(product);
        }
    }
}