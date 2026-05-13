# Context Handoff — Cafe Manager Android Java

> Paste toàn bộ file này vào đầu chat mới. File `claude_prompt_transfer_cafe_manager.md` (file gốc 24 phần) chứa toàn bộ scope/ERD/UI/team plan — chỉ cần upload kèm khi nào Claude cần đào sâu thiết kế.

**Cập nhật gần nhất:** Đã viết xong 6 DAO interfaces. Đang chuẩn bị qua bước AppDatabase + Repository.

---

## 1. Prompt khởi động cho Claude

```text
Bạn là Senior Software Engineer kiêm Project Lead cho đồ án Android Java
"Cafe Manager - Ứng dụng quản lý quán cafe".

Stack đã chốt:
- Native Android, Java, XML layout
- Room Database
- MVVM + Repository
- ExecutorService/AppExecutors (KHÔNG dùng Coroutine)
- RecyclerView, LiveData

Quy tắc (TUÂN THỦ TUYỆT ĐỐI):
1. Activity KHÔNG gọi DAO trực tiếp. Luồng: Activity → ViewModel → Repository → DAO → Room.
2. KHÔNG hard-code status/định dạng tiền — dùng Constants/CurrencyUtils/StatusUtils.
3. Soft delete cho Product (isActive), không hard delete.
4. Confirm order và Confirm payment phải atomic (transaction).
5. Code đơn giản, sạch, hợp sinh viên — không over-engineer (không DI framework, không generic adapter, không multi-module).
6. Ưu tiên thứ tự code: Entity → DAO → AppDatabase → Repository → ViewModel → Activity/Adapter/XML.
7. Trả lời tiếng Việt.

Đọc kỹ phần "Tình trạng hiện tại" và "Bước tiếp theo" bên dưới rồi giúp tôi
viết code cho bước tiếp theo.
```

---

## 2. Tình trạng hiện tại của dự án

Project path: `D:\Nam2\SE114\` — package gốc `com.example.cafe_manager`.

### ✅ Đã hoàn chỉnh (có code thật):

**6 Entity** ở `data/local/entity/`:
- `TableEntity` (table_id, table_name, status, capacity, area, created_at)
- `CategoryEntity` (category_id, category_name, is_active, description)
- `ProductEntity` (product_id, category_id FK, product_name, price, image_url, is_active, created_at)
- `OrderEntity` (order_id, table_id FK CASCADE, order_code, status, total_amount, note, created_at, paid_at)
- `OrderItemEntity` (order_item_id, order_id FK, product_id FK, product_name_snapshot, quantity, unit_price, subtotal, note)
- `PaymentEntity` (payment_id, order_id FK, payment_method, subtotal, discount_amount, final_amount, paid_at, status)

Tất cả đã có `@Entity`, `@PrimaryKey(autoGenerate = true)`, `@ColumnInfo`, `@ForeignKey`, `@Index`, constructors, getters/setters.

**6 DAO** ở `data/local/dao/` — đã viết xong, đúng pattern `@Dao public interface`:

- **`TableDao`**: `getAll()` LiveData, `getById()`, `insert()`, `insertAll()`, `updateStatus()`, `deleteAll()`
- **`CategoryDao`**: `getAllActive()` LiveData, `getAll()` LiveData, `getById()`, `insert()`, `update()`, `setActive()`
- **`ProductDao`**: `getAllActive()` LiveData, `getByCategoryId()` LiveData, `getById()`, `insert()`, `update()`, `setActive()` ⚠️ có bug, xem mục 3
- **`OrderDao`**: `insert()` (return long), `getLatestOrderByStatus()` ⚠️ có bug, `getActiveByTableId()`, `getById()`, `getByIdLive()`, `updateStatus()`, `updateStatusWithPaidAt()`, `updateTotal()`
- **`OrderItemDao`**: `insertAll()`, `getByOrderId()` LiveData, `getByOrderIdSync()`, `deleteByOrderId()`
- **`PaymentDao`**: `insert()`, `getByOrderId()`, `getByOrderIdLive()`

**Util** ở `util/`:
- `Constants.java`: TABLE_EMPTY/OCCUPIED, ORDER_OPEN/CONFIRMED/PAID/CANCELLED, PAYMENT_CASH/BANKING/MOMO, PAYMENT_SUCCESS/FAILED, ICON_COFFEE/TEA/FOOD/DESSERT/OTHER.
- `AppExecutors.java`: singleton, có `diskIO()` (single-thread executor) + `mainThread()` (Handler).
- `CurrencyUtils.java`, `StatusUtils.java`, `OrderCalculator.java`, `DateTimeUtils.java`: đã có file (chưa kiểm tra nội dung).

### ⚠️ File tồn tại nhưng RỖNG (skeleton, cần viết tiếp):

| File | Tình trạng |
|---|---|
| `data/local/AppDatabase.java` | `public class AppDatabase {}` — RỖNG |
| `data/repository/TableRepository.java` | RỖNG |
| `data/repository/MenuRepository.java` | RỖNG |
| `data/repository/OrderRepository.java` | RỖNG |
| `data/repository/PaymentRepository.java` | RỖNG |
| Các ViewModel (4 file) | Skeleton |
| Các Activity/Adapter | Skeleton/boilerplate (chỉ có code auto-generated) |

### Cần kiểm tra:
- `app/build.gradle.kts`: chưa xác nhận đã thêm Room dependencies (`androidx.room:room-runtime`, `room-compiler` annotation processor, `lifecycle-livedata`).

---

## 3. ⚠️ Bug đã phát hiện trong DAO (cần sửa khi tiện)

### Bug 1 — `ProductDao.getByCategoryId()`
File: `data/local/dao/ProductDao.java`
```java
// SAI — đang lọc theo product_id
@Query("SELECT * FROM products WHERE product_id = :categoryId AND is_active = 1 ORDER BY product_name ASC")
LiveData<List<ProductEntity>> getByCategoryId(int categoryId);
```
**Sửa thành `category_id`:**
```java
@Query("SELECT * FROM products WHERE category_id = :categoryId AND is_active = 1 ORDER BY product_name ASC")
LiveData<List<ProductEntity>> getByCategoryId(int categoryId);
```

### Bug 2 — `OrderDao.getLatestOrderByStatus()`
File: `data/local/dao/OrderDao.java`
```java
// Vô nghĩa — order_id là PK unique, ORDER BY DESC LIMIT 1 không có tác dụng
@Query("SELECT * FROM orders WHERE order_id = :orderId AND status = :status ORDER BY order_id DESC LIMIT 1")
LiveData<OrderEntity> getLatestOrderByStatus(int orderId, String status);
```
**Đề xuất:** Hàm này gần như trùng chức năng với `getActiveByTableId(tableId, status)` nếu đổi `order_id` thành `table_id`. Có thể **xoá luôn** để giữ DAO gọn, dùng `getActiveByTableId` là đủ. Nếu muốn giữ phiên bản LiveData cho UI observe, đổi lại như sau:
```java
@Query("SELECT * FROM orders WHERE table_id = :tableId AND status = :status ORDER BY order_id DESC LIMIT 1")
LiveData<OrderEntity> getActiveByTableIdLive(int tableId, String status);
```

### Lưu ý chung về "Active Order"
Trong scope MVP, chỉ có **`CONFIRMED`** mới được coi là active (đang phục vụ, chưa thanh toán). `OPEN` được khai báo trong Constants nhưng không thực dùng (giỏ hàng giữ trong RAM/ViewModel, chỉ insert vào DB khi nhấn "Xác nhận Order" với status = `CONFIRMED`). Khi gọi `getActiveByTableId()`, truyền `Constants.ORDER_CONFIRMED`.

---

## 4. Bước tiếp theo (đang ở đây)

**Mục tiêu:** AppDatabase → Verify Room dependencies → 4 Repository → seed data.

### Bước 4.1 — `AppDatabase.java`
Đổi từ `public class` thành **abstract class extends RoomDatabase**:
```java
@Database(
    entities = {
        TableEntity.class, CategoryEntity.class, ProductEntity.class,
        OrderEntity.class, OrderItemEntity.class, PaymentEntity.class
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
                // seed 10 bàn (B01-B10), 4 categories, 6 products mẫu
            });
        }
    };
}
```

### Bước 4.2 — Verify `app/build.gradle.kts`
Cần có (hoặc thêm vào):
```kotlin
dependencies {
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
}
```

### Bước 4.3 — 4 Repository
Mỗi repository:
- Có constructor nhận `Context` (lấy DAO từ `AppDatabase.getInstance(context)`)
- Expose method cho ViewModel
- Method ghi (`insert/update/delete`) bọc trong `AppExecutors.getInstance().diskIO().execute(...)`
- Method đọc `LiveData` thì trả thẳng từ DAO (Room đã chạy ngầm)

**Method bắt buộc theo team plan:**

`TableRepository`:
```text
LiveData<List<TableEntity>> getAllTables()
void updateTableStatus(int tableId, String status)
```

`MenuRepository`:
```text
LiveData<List<CategoryEntity>> getActiveCategories()
LiveData<List<ProductEntity>> getActiveProducts()
LiveData<List<ProductEntity>> getProductsByCategory(int categoryId)
void updateProductActiveStatus(int productId, boolean isActive)
void insertProduct(ProductEntity p)
void updateProduct(ProductEntity p)
```

`OrderRepository`:
```text
void confirmOrder(int tableId, List<CartItem> cartItems, String note, Callback<Long> onResult)
   // ATOMIC trong database.runInTransaction(...):
   //   insert OrderEntity (status=CONFIRMED) → lấy orderId
   //   build OrderItemEntity[] với productNameSnapshot, unitPrice tại thời điểm này
   //   insertAll order_items
   //   updateStatus table = OCCUPIED
OrderEntity getActiveOrderByTable(int tableId)  // sync, dùng trong transaction
LiveData<List<OrderItemEntity>> getItemsByOrderId(int orderId)
```

`PaymentRepository`:
```text
void payOrder(int orderId, int tableId, String paymentMethod,
              double subtotal, double discount, double finalAmount,
              Callback<Boolean> onResult)
   // ATOMIC:
   //   insert PaymentEntity (status=SUCCESS, paidAt=now)
   //   updateStatusWithPaidAt(orderId, PAID, now)
   //   updateTableStatus(tableId, EMPTY)
LiveData<PaymentEntity> getPaymentByOrder(int orderId)
```

### Bước 4.4 — Seed data (trong `onCreate` callback của AppDatabase)
- 10 bàn: B01–B10, status = `EMPTY`, capacity 2/4/6
- 4 categories: Cà phê, Trà, Sinh tố, Bánh
- 6 products: Cà phê sữa đá 35.000đ, Bạc xỉu 38.000đ, Trà sữa trân châu 45.000đ, Sinh tố bơ 50.000đ, Bánh Tiramisu 55.000đ, Trà đào cam sả 45.000đ

---

## 5. Quy ước đặt tên đã thống nhất

- Status table: `EMPTY` / `OCCUPIED`
- Status order: `OPEN` / `CONFIRMED` / `PAID` / `CANCELLED` (thực dùng: chỉ CONFIRMED/PAID/CANCELLED)
- Payment method: `CASH` / `BANKING` / `MOMO`
- Payment status: `SUCCESS` / `FAILED`
- Soft delete product: `isActive = false`
- Money format: `CurrencyUtils.formatVnd()` → `35.000đ`
- `OrderItemEntity` luôn lưu `productNameSnapshot` và `unitPrice` tại thời điểm tạo order (giữ nguyên giá trị lịch sử khi product đổi tên/giá sau này).

---

## 6. MVP scope nhắc lại (tránh over-scope)

6 màn hình: TableActivity, MenuActivity, OrderActivity, PaymentActivity, InvoiceActivity, AdminMenuActivity.

KHÔNG làm trong MVP: login/role, inventory, kitchen display, real-time sync, loyalty, VNPay thật, sales dashboard.

---

## 7. File tham khảo

- `D:\Nam2\SE114\CLAUDE.md` — quy tắc behavioral cho Claude (think before coding, simplicity first, surgical changes, goal-driven).
- `claude_prompt_transfer_cafe_manager.md` (file đã upload trong chat cũ) — full context 24 phần (ERD, sequence diagram, team plan, day plan, UI prototype). Upload lại khi cần thiết kế chi tiết.
