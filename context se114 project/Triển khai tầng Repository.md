# Triển khai tầng Repository

Sau khi hoàn thành Entity, DAO, `AppDatabase` và dữ liệu mẫu, nhóm triển khai tầng Repository để làm lớp trung gian giữa ViewModel và Room Database. Repository có nhiệm vụ che giấu chi tiết truy xuất dữ liệu khỏi tầng giao diện, đồng thời tập trung các thao tác nghiệp vụ như lấy danh sách bàn, lấy menu, xác nhận order và thanh toán. Cách tổ chức này giúp Activity/ViewModel không gọi trực tiếp DAO, từ đó mã nguồn dễ bảo trì và phù hợp với kiến trúc MVVM đã chọn cho dự án.

Repository được đặt trong package:

```text
com.example.cafe_manager.data.repository
```

Các Repository chính gồm:

```text
TableRepository
MenuRepository
OrderRepository
PaymentRepository
```

Luồng xử lý tổng quát:

```text
Activity / UI
→ ViewModel
→ Repository
→ DAO
→ Room Database
```

Luồng trả dữ liệu:

```text
Room Database
→ DAO
→ Repository
→ ViewModel
→ Activity / UI
```

---

## 1. Vai trò của Repository trong hệ thống

Trong ứng dụng quản lý quán cafe, dữ liệu được lưu bằng Room Database. Nếu Activity hoặc Fragment gọi trực tiếp DAO, giao diện sẽ bị phụ thuộc mạnh vào cơ sở dữ liệu. Điều này làm code khó mở rộng, khó kiểm thử và dễ bị rối khi nghiệp vụ phức tạp hơn.

Vì vậy, nhóm sử dụng Repository Pattern để tách biệt trách nhiệm:

| Thành phần    | Vai trò                                      |
| ------------- | -------------------------------------------- |
| Activity / UI | Hiển thị dữ liệu và nhận thao tác người dùng |
| ViewModel     | Giữ trạng thái màn hình và gọi Repository    |
| Repository    | Xử lý truy xuất dữ liệu và nghiệp vụ         |
| DAO           | Thực hiện truy vấn Room Database             |
| Entity        | Đại diện cho bảng dữ liệu                    |

Repository không trực tiếp hiển thị giao diện. Nó chỉ nhận yêu cầu từ ViewModel, gọi DAO phù hợp, xử lý logic cần thiết và trả kết quả về ViewModel.

Ví dụ, khi người dùng nhấn **Xác nhận Order**, ViewModel không cần biết phải insert bảng nào trước. ViewModel chỉ gọi:

```java
orderRepository.confirmOrder(tableId, cartItems, note, callback);
```

Bên trong Repository mới xử lý:

```text
Tính tổng tiền
→ Tạo OrderEntity
→ Tạo danh sách OrderItemEntity
→ Gọi TransactionDao để lưu dữ liệu
→ Trả kết quả về ViewModel
```

---

## 2. Xử lý bất đồng bộ trong Repository

Các thao tác với database không nên chạy trên Main Thread vì có thể làm treo giao diện. Do đó, Repository sử dụng `AppExecutors` để chạy các thao tác ghi dữ liệu trên background thread.

Ví dụ:

```java
appExecutors.diskIO().execute(() -> {
    // database task
});
```

Trong đó:

```text
diskIO()     → dùng cho thao tác database
mainThread() → dùng để trả kết quả về UI/ViewModel
```

Với các thao tác ghi dữ liệu như xác nhận order hoặc thanh toán, Repository chạy logic trong `diskIO()`. Sau khi hoàn tất, kết quả được gửi về `mainThread()` thông qua `RepositoryCallback`.

Cách này giúp hệ thống tránh lỗi:

```text
Cannot access database on the main thread
```

Đồng thời giữ cho giao diện vẫn phản hồi mượt khi người dùng thao tác.

---

## 3. Cơ chế trả kết quả bằng `RepositoryCallback`

Vì các thao tác ghi dữ liệu chạy bất đồng bộ, Repository không thể trả kết quả trực tiếp bằng `return` như hàm thông thường. Do đó, nhóm tạo interface `RepositoryCallback<T>` để nhận kết quả sau khi xử lý xong.

```java
public interface RepositoryCallback<T> {
    void onSuccess(T result);
    void onError(Exception e);
}
```

Interface này gồm hai nhánh:

| Method                 | Ý nghĩa                          |
| ---------------------- | -------------------------------- |
| `onSuccess(T result)`  | Được gọi khi thao tác thành công |
| `onError(Exception e)` | Được gọi khi thao tác thất bại   |

Ví dụ với xác nhận order:

```java
RepositoryCallback<Long>
```

`Long` là `orderId` được tạo sau khi lưu order thành công.

Ví dụ với thanh toán:

```java
RepositoryCallback<Boolean>
```

`Boolean` cho biết thao tác thanh toán đã hoàn tất hay chưa.

Cách làm này giúp Repository không phụ thuộc vào Activity hoặc Fragment. Repository chỉ báo kết quả, còn ViewModel/UI quyết định sẽ hiển thị thông báo, chuyển màn hình hay xử lý lỗi.

---

## 4. `TableRepository`

`TableRepository` phục vụ màn hình **Sơ đồ bàn**. Repository này quản lý các thao tác liên quan đến bảng `tables`.

Các chức năng chính:

```text
Lấy danh sách bàn
Lọc bàn theo trạng thái nếu cần
Cập nhật trạng thái bàn
```

Ví dụ các phương thức chính:

```java
public LiveData<List<TableEntity>> getAllTables() {
    return tableDao.getAll();
}

public void updateTableStatus(int tableId, String status) {
    appExecutors.diskIO().execute(() ->
            tableDao.updateStatus(tableId, status)
    );
}
```

Trong màn hình `TableActivity`, danh sách bàn được quan sát thông qua ViewModel. Khi trạng thái bàn thay đổi, ví dụ từ `EMPTY` sang `OCCUPIED`, dữ liệu trong Room thay đổi và UI có thể tự cập nhật lại thông qua `LiveData`.

Ví dụ nghiệp vụ:

```text
Bàn đang trống
→ Nhân viên gọi món và xác nhận order
→ TableRepository / TransactionDao cập nhật bàn thành OCCUPIED
→ Màn Sơ đồ bàn hiển thị bàn đó là Có khách
```

Việc cập nhật trạng thái bàn không nên làm thủ công từ UI. Trạng thái bàn nên thay đổi theo nghiệp vụ:

```text
Xác nhận order → bàn có khách
Thanh toán xong → bàn trống
```

---

## 5. `MenuRepository`

`MenuRepository` phục vụ hai màn hình:

```text
MenuActivity       → Gọi món
AdminMenuActivity  → Quản lý menu
```

Repository này làm việc với hai DAO:

```text
CategoryDao
ProductDao
```

Các chức năng chính:

```text
Lấy danh sách danh mục đang hoạt động
Lấy danh sách sản phẩm đang bán
Lọc sản phẩm theo danh mục
Thêm sản phẩm
Cập nhật sản phẩm
Ẩn / hiện sản phẩm
```

Ví dụ:

```java
public LiveData<List<CategoryEntity>> getActiveCategories() {
    return categoryDao.getAllActive();
}

public LiveData<List<ProductEntity>> getActiveProducts() {
    return productDao.getAllActive();
}

public LiveData<List<ProductEntity>> getProductsByCategory(int categoryId) {
    return productDao.getByCategoryId(categoryId);
}
```

Đối với màn hình gọi món, app chỉ hiển thị các sản phẩm đang hoạt động:

```text
isActive = true
```

Đối với màn hình quản lý menu, quản lý có thể ẩn hoặc hiện món:

```java
public void updateProductActiveStatus(int productId, boolean isActive) {
    appExecutors.diskIO().execute(() ->
            productDao.setActive(productId, isActive)
    );
}
```

Nhóm không xóa cứng sản phẩm khỏi database. Khi món ngừng bán, hệ thống chỉ cập nhật:

```text
isActive = false
```

Lý do là một sản phẩm có thể đã từng xuất hiện trong order hoặc hóa đơn cũ. Nếu xóa cứng khỏi database, dữ liệu lịch sử có thể bị mất liên kết. Cách dùng `isActive` giúp hệ thống vừa ẩn món khỏi menu bán hàng, vừa giữ được lịch sử giao dịch.

---

## 6. `OrderRepository`

`OrderRepository` là Repository quan trọng nhất trong nghiệp vụ gọi món. Nó xử lý thao tác **Xác nhận Order**.

Khi nhân viên chọn món ở `MenuActivity`, dữ liệu ban đầu được lưu tạm dưới dạng danh sách `CartItem`. Khi nhân viên nhấn **Xác nhận Order**, Repository sẽ chuyển danh sách này thành dữ liệu chính thức trong database.

Luồng xử lý:

```text
List<CartItem>
→ Tính tổng tiền
→ Tạo OrderEntity
→ Tạo List<OrderItemEntity>
→ Gọi OrderTransactionDao
→ Cập nhật bàn thành OCCUPIED
→ Trả orderId về ViewModel
```

Ví dụ xử lý tổng tiền:

```java
double totalAmount = 0;

for (CartItem cartItem : cartItems) {
    totalAmount += cartItem.getSubtotal();
}
```

Tạo `OrderEntity`:

```java
OrderEntity order = new OrderEntity();
order.setTableId(tableId);
order.setOrderCode("ORD" + System.currentTimeMillis());
order.setStatus(Constants.ORDER_CONFIRMED);
order.setTotalAmount(totalAmount);
order.setNote(note);
order.setCreatedAt(System.currentTimeMillis());
order.setPaidAt(0L);
```

Chuyển `CartItem` thành `OrderItemEntity`:

```java
OrderItemEntity item = new OrderItemEntity();
item.setProductId(cartItem.getProductId());
item.setProductNameSnapshot(cartItem.getProductName());
item.setQuantity(cartItem.getQuantity());
item.setUnitPrice(cartItem.getUnitPrice());
item.setSubtotal(cartItem.getSubtotal());
item.setNote(cartItem.getNote());
```

Điểm quan trọng là `OrderItemEntity` lưu:

```text
productNameSnapshot
unitPrice
```

Hai giá trị này giúp hóa đơn cũ không bị thay đổi nếu sau này quản lý sửa tên món hoặc giá món trong bảng `products`.

---

## 6.1. Vì sao cần transaction khi xác nhận order?

Thao tác xác nhận order không chỉ ghi một bảng. Nó gồm ba bước:

```text
1. Insert vào bảng orders
2. Insert nhiều dòng vào bảng order_items
3. Update bảng tables thành OCCUPIED
```

Ba bước này phải được xử lý như một giao dịch thống nhất. Nếu bước 1 thành công nhưng bước 2 lỗi, hệ thống sẽ có order nhưng không có món. Nếu bước 1 và bước 2 thành công nhưng bước 3 lỗi, bàn vẫn hiển thị là trống dù đã có order.

Các lỗi dữ liệu có thể xảy ra nếu không có transaction:

```text
Có Order nhưng không có OrderItem
Có OrderItem nhưng bàn chưa đổi trạng thái
Bàn hiển thị Trống dù đang có khách
Tổng tiền và danh sách món không khớp
```

Vì vậy, nhóm tách phần thao tác nhiều bảng vào `OrderTransactionDao` và đánh dấu bằng `@Transaction`.

Repository chỉ chuẩn bị dữ liệu, còn DAO transaction đảm bảo tính atomic khi ghi database.

```text
OrderRepository
→ OrderTransactionDao.confirmOrderAtomic()
→ insert Order
→ insert OrderItems
→ update Table
```

Cách này giúp Repository gọn hơn so với việc gọi `db.runInTransaction()` trực tiếp trong Repository, nhưng vẫn giữ được an toàn dữ liệu.

---

## 6.2. Kết quả trả về của `OrderRepository`

Sau khi xác nhận order thành công, Repository trả về `orderId` thông qua callback:

```java
RepositoryCallback<Long>
```

Ví dụ:

```java
callback.onSuccess(orderId);
```

Nếu có lỗi trong quá trình lưu dữ liệu, Repository gọi:

```java
callback.onError(e);
```

ViewModel có thể dựa vào kết quả này để cập nhật UI:

```text
Thành công → thông báo xác nhận order thành công, quay về sơ đồ bàn
Thất bại → hiển thị thông báo lỗi
```

---

## 7. `PaymentRepository`

`PaymentRepository` xử lý nghiệp vụ thanh toán.

Khi một bàn đã có order, thu ngân có thể vào màn hình thanh toán. Sau khi chọn phương thức thanh toán và nhấn **Xác nhận thanh toán**, Repository sẽ xử lý dữ liệu thanh toán.

Luồng xử lý:

```text
Nhận orderId, tableId, paymentMethod, subtotal, discount, finalAmount
→ Tạo PaymentEntity
→ Gọi PaymentTransactionDao
→ Insert Payment
→ Update Order = PAID
→ Update Table = EMPTY
→ Trả kết quả về ViewModel
```

Tạo `PaymentEntity`:

```java
long paidAt = System.currentTimeMillis();

PaymentEntity payment = new PaymentEntity();
payment.setOrderId(orderId);
payment.setPaymentMethod(paymentMethod);
payment.setSubtotal(subtotal);
payment.setDiscountAmount(discount);
payment.setFinalAmount(finalAmount);
payment.setPaidAt(paidAt);
payment.setStatus(Constants.PAYMENT_SUCCESS);
```

Sau đó gọi transaction DAO:

```java
paymentTransactionDao.payOrderAtomic(
        payment,
        orderId,
        tableId,
        Constants.ORDER_PAID,
        Constants.TABLE_EMPTY,
        paidAt
);
```

---

## 7.1. Vì sao thanh toán cũng cần transaction?

Thanh toán cũng gồm nhiều bước ghi dữ liệu:

```text
1. Insert vào bảng payments
2. Update order thành PAID
3. Update table thành EMPTY
```

Nếu chỉ insert payment nhưng chưa cập nhật order, hệ thống sẽ có hóa đơn thanh toán nhưng order vẫn chưa được đánh dấu đã thanh toán. Nếu order đã chuyển PAID nhưng bàn chưa chuyển về EMPTY, màn hình sơ đồ bàn sẽ hiển thị sai trạng thái.

Các lỗi dữ liệu có thể xảy ra nếu không dùng transaction:

```text
Có Payment nhưng Order chưa PAID
Order đã PAID nhưng bàn vẫn OCCUPIED
Bàn đã EMPTY nhưng chưa có Payment
Hóa đơn và trạng thái bàn không thống nhất
```

Vì vậy, thanh toán cũng được xử lý trong `PaymentTransactionDao` với `@Transaction`.

```text
PaymentRepository
→ PaymentTransactionDao.payOrderAtomic()
→ insert Payment
→ update Order
→ update Table
```

Sau khi thanh toán thành công, ViewModel có thể điều hướng sang màn hình hóa đơn (`InvoiceActivity`) hoặc quay về sơ đồ bàn.

---

## 8. Sử dụng `long` cho thời gian

Trong hệ thống, các trường thời gian như:

```text
createdAt
paidAt
```

được lưu bằng kiểu `long`.

Khi tạo order:

```java
order.setCreatedAt(System.currentTimeMillis());
```

Khi thanh toán:

```java
payment.setPaidAt(System.currentTimeMillis());
```

Lý do chọn `long` thay vì `String`:

| Kiểu     | Nhận xét                                           |
| -------- | -------------------------------------------------- |
| `String` | Dễ hiển thị nhưng khó sort, khó lọc theo thời gian |
| `long`   | Dễ sort, dễ so sánh, dễ lọc theo khoảng thời gian  |

Khi cần hiển thị lên UI, hệ thống dùng `DateTimeUtils` để chuyển timestamp sang chuỗi dễ đọc:

```java
DateTimeUtils.formatDateTime(payment.getPaidAt());
```

Nhờ đó, database lưu dữ liệu ở dạng phù hợp cho xử lý, còn UI vẫn hiển thị ngày giờ theo định dạng thân thiện.

---

## 9. Vì sao Repository không gọi trực tiếp UI?

Repository không được gọi `Toast`, không được chuyển màn hình và không được tham chiếu Activity. Điều này giúp tầng dữ liệu độc lập với tầng giao diện.

Không nên làm:

```java
Toast.makeText(context, "Thanh toán thành công", Toast.LENGTH_SHORT).show();
```

trong Repository.

Thay vào đó, Repository chỉ trả kết quả:

```java
callback.onSuccess(true);
```

Sau đó ViewModel hoặc Activity xử lý:

```text
Hiển thị thông báo
Cập nhật trạng thái loading
Chuyển sang InvoiceActivity
```

Cách tách trách nhiệm này giúp code dễ kiểm thử và dễ thay đổi UI mà không ảnh hưởng tới tầng dữ liệu.

---

## 10. Kiểm soát lỗi trong Repository

Các thao tác ghi dữ liệu được bọc trong `try/catch` ở Repository để bắt lỗi phát sinh từ Room hoặc logic xử lý.

Ví dụ:

```java
try {
    // database operation
    callback.onSuccess(result);
} catch (Exception e) {
    callback.onError(e);
}
```

Một số lỗi có thể xảy ra:

```text
DAO query sai
Dữ liệu truyền vào thiếu
Cart rỗng
Payment method không hợp lệ
Database chưa khởi tạo
Transaction thất bại
```

Trong các trường hợp đó, Repository không làm app crash trực tiếp mà trả lỗi về ViewModel thông qua `onError()`.

Sau này ViewModel có thể chuyển lỗi này thành thông báo người dùng:

```text
Không thể xác nhận order
Không thể thanh toán
Dữ liệu không hợp lệ
```

---

## 11. Kết quả triển khai Repository

Sau khi hoàn thành tầng Repository, hệ thống đạt được các kết quả sau:

```text
TableRepository lấy và cập nhật trạng thái bàn
MenuRepository lấy danh mục, sản phẩm và hỗ trợ ẩn/hiện món
OrderRepository xử lý xác nhận order từ giỏ hàng
PaymentRepository xử lý thanh toán và cập nhật trạng thái order/bàn
Các thao tác ghi dữ liệu được chạy trên background thread
Các kết quả bất đồng bộ được trả về bằng RepositoryCallback
Các thao tác nhiều bảng được bảo vệ bằng @Transaction
Activity không cần gọi trực tiếp DAO
```

Repository trở thành lớp trung gian ổn định để ViewModel sử dụng ở bước tiếp theo.

---

## 12. Tóm tắt luồng Repository theo nghiệp vụ

### Luồng xác nhận order

```text
MenuActivity
→ OrderViewModel
→ OrderRepository.confirmOrder()
→ OrderTransactionDao.confirmOrderAtomic()
→ orders + order_items + tables
→ callback.onSuccess(orderId)
→ UI cập nhật trạng thái
```

### Luồng thanh toán

```text
PaymentActivity
→ PaymentViewModel
→ PaymentRepository.payOrder()
→ PaymentTransactionDao.payOrderAtomic()
→ payments + orders + tables
→ callback.onSuccess(true)
→ UI chuyển sang hóa đơn
```

### Luồng quản lý menu

```text
AdminMenuActivity
→ AdminMenuViewModel
→ MenuRepository
→ ProductDao
→ products.isActive thay đổi
→ MenuActivity chỉ hiển thị sản phẩm active
```

### Luồng sơ đồ bàn

```text
TableActivity
→ TableViewModel
→ TableRepository
→ TableDao
→ LiveData<List<TableEntity>>
→ RecyclerView cập nhật danh sách bàn
```

---

## 13. Kết luận

Tầng Repository là phần kết nối giữa dữ liệu và nghiệp vụ của ứng dụng. Thay vì để giao diện gọi trực tiếp DAO, Repository gom các thao tác dữ liệu vào một nơi thống nhất. Điều này giúp hệ thống dễ mở rộng hơn khi bổ sung các chức năng như quản lý kho, báo cáo doanh thu, phân quyền nhân viên hoặc đồng bộ dữ liệu nhiều thiết bị.

Trong MVP hiện tại, Repository đã đảm bảo các nghiệp vụ cốt lõi:

```text
Xem bàn
Gọi món
Xác nhận order
Thanh toán
Quản lý menu
```

được xử lý đúng luồng, đúng tầng trách nhiệm và đảm bảo tính nhất quán dữ liệu thông qua transaction.
