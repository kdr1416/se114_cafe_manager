# Báo cáo triển khai — OrderActivity, PaymentActivity & InvoiceActivity

> **Đối tượng:** Member 3  
> **Phụ thuộc:** Phase 0 xong + Member 2 đang làm song song (Table/Menu)  
> **Output cuối:** Luồng đầy đủ từ "Xác nhận order" → "Thanh toán" → "Hóa đơn" → quay về Sơ đồ bàn.

---

## Mục lục

1. Tổng quan + flow update
2. OrderDetailViewModel (tạo mới)
3. OrderActivity
4. PaymentActivity
5. InvoiceActivity
6. Cập nhật AndroidManifest
7. Cập nhật cần đồng bộ với báo cáo Member 2
8. Test checklist
9. Cạm bẫy hay gặp

---

## 1. Tổng quan

### Luồng nghiệp vụ Member 3 phụ trách

```
MenuActivity (Member 2)
   │ click "Xem order"
   ↓
OrderActivity (Mode 1: cart review)
   │ click "Xác nhận order"
   │   → OrderRepository.confirmOrder()
   │     - insert orders + order_items + update tables=OCCUPIED (atomic)
   │   → cartManager.clearCart()
   │ navigate (CLEAR_TOP)
   ↓
TableActivity (bàn vừa chọn = OCCUPIED)


TableActivity click OCCUPIED → OrdersListActivity (Member 4)
   │ click order row "Thu tiền"
   ↓
PaymentActivity
   │ load order items, hiển thị summary
   │ user chọn method, nhập discount
   │ click "Xác nhận thanh toán"
   │   → PaymentRepository.payOrder()
   │     - insert payments + update orders=PAID + update tables=EMPTY (atomic)
   ↓
InvoiceActivity (hiển thị hóa đơn)
   │ click "Quay về sơ đồ bàn"
   ↓
TableActivity (CLEAR_TOP)
```

### Phạm vi báo cáo này

| File | Mục đích | Trạng thái hiện tại |
|---|---|---|
| `viewmodel/InvoiceViewModel.java` *(đổi tên gọi: `OrderDetailViewModel`)* | Load order + items + payment cho 1 orderId | **Tạo mới** |
| `ui/order/OrderActivity.java` | Review cart, confirm order | Replace boilerplate |
| `ui/order/OrderItemAdapter.java` | Hiển thị CartItem editable (qty +/-, remove) | Replace skeleton |
| `res/layout/activity_order.xml` | Layout OrderActivity | Replace boilerplate |
| `res/layout/item_order_item.xml` | Row trong RecyclerView Order | Mới |
| `ui/payment/PaymentActivity.java` | Form thanh toán | Replace boilerplate |
| `res/layout/activity_payment.xml` | Layout PaymentActivity | Replace boilerplate |
| `ui/payment/InvoiceActivity.java` | Display hóa đơn read-only | Replace boilerplate |
| `ui/payment/InvoiceItemAdapter.java` | Hiển thị OrderItemEntity read-only | Mới |
| `res/layout/activity_invoice.xml` | Layout InvoiceActivity | Mới |
| `res/layout/item_invoice_item.xml` | Row trong RecyclerView Invoice | Mới |
| `AndroidManifest.xml` | Khai báo InvoiceActivity, đổi LAUNCHER | Cập nhật |

### Lưu ý thiết kế

1. **OrderActivity chỉ làm Mode 1 (cart review).** Việc xem lại order đang phục vụ của bàn OCCUPIED thuộc về OrdersListActivity (Member 4).

2. **PaymentActivity dùng 2 ViewModel:**
   - `PaymentViewModel` (đã có): quản lý state thanh toán (discount, method, confirm)
   - `OrderDetailViewModel` (mới): load order + items để hiển thị summary
   - Không phải sửa `PaymentViewModel` hiện tại.

3. **InvoiceActivity dùng `OrderDetailViewModel`** giống PaymentActivity — chia sẻ ViewModel này để tránh code trùng lặp.

4. **Sau confirm order / pay:** navigate về TableActivity bằng `FLAG_ACTIVITY_CLEAR_TOP | SINGLE_TOP` để clear stack, không bị back vào Menu hay Order cũ.

---

## 2. OrderDetailViewModel (tạo mới)

Đặt tại `viewmodel/OrderDetailViewModel.java`. Dùng cho cả PaymentActivity và InvoiceActivity.

```java
package com.example.cafe_manager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.cafe_manager.data.local.entity.OrderEntity;
import com.example.cafe_manager.data.local.entity.OrderItemEntity;
import com.example.cafe_manager.data.local.entity.PaymentEntity;
import com.example.cafe_manager.data.repository.OrderRepository;
import com.example.cafe_manager.data.repository.PaymentRepository;

import java.util.List;

/**
 * Read-only ViewModel: load chi tiết một order (order + items + payment).
 * Dùng bởi PaymentActivity (chưa có payment) và InvoiceActivity (đã có payment).
 */
public class OrderDetailViewModel extends AndroidViewModel {

    private final MutableLiveData<Integer> orderIdLive = new MutableLiveData<>();

    private final LiveData<OrderEntity> orderLive;
    private final LiveData<List<OrderItemEntity>> itemsLive;
    private final LiveData<PaymentEntity> paymentLive;

    public OrderDetailViewModel(@NonNull Application application) {
        super(application);

        OrderRepository orderRepository = new OrderRepository(application);
        PaymentRepository paymentRepository = new PaymentRepository(application);

        this.orderLive = Transformations.switchMap(
                orderIdLive,
                id -> (id == null || id <= 0)
                        ? new MutableLiveData<>()
                        : orderRepository.getOrderLive(id)
        );

        this.itemsLive = Transformations.switchMap(
                orderIdLive,
                id -> (id == null || id <= 0)
                        ? new MutableLiveData<>()
                        : orderRepository.getItemsByOrderId(id)
        );

        this.paymentLive = Transformations.switchMap(
                orderIdLive,
                id -> (id == null || id <= 0)
                        ? new MutableLiveData<>()
                        : paymentRepository.getPaymentByOrder(id)
        );
    }

    public void setOrderId(int orderId) {
        Integer current = orderIdLive.getValue();
        if (current != null && current == orderId) {
            return;
        }
        orderIdLive.setValue(orderId);
    }

    public LiveData<OrderEntity> getOrder() {
        return orderLive;
    }

    public LiveData<List<OrderItemEntity>> getItems() {
        return itemsLive;
    }

    public LiveData<PaymentEntity> getPayment() {
        return paymentLive;
    }
}
```

**Tại sao dùng `Transformations.switchMap`?**  
Khi `orderIdLive.setValue(id)` được gọi, các LiveData con tự động chuyển sang nguồn mới (DAO query với id mới). Activity chỉ cần observe 1 lần — không phải remove/add observer thủ công.

---

## 3. OrderActivity

### 3.1. Mục tiêu

Hiển thị các món trong giỏ hàng (từ `CartManager` qua `OrderViewModel`). Cho phép tăng/giảm số lượng, xoá món. Bấm "Xác nhận order" → atomic transaction insert order + items + đổi bàn = OCCUPIED → quay về TableActivity.

### 3.2. File `activity_order.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/bg_primary">

    <include
        android:id="@+id/top_bar"
        layout="@layout/component_top_bar" />

    <!-- Container chính: stack RecyclerView + empty state -->
    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/rv_order_items"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:padding="@dimen/spacing_lg"
            android:clipToPadding="false"
            tools:listitem="@layout/item_order_item" />

        <include
            android:id="@+id/empty_state"
            layout="@layout/component_empty_state"
            android:visibility="gone" />
    </FrameLayout>

    <include
        android:id="@+id/footer"
        layout="@layout/component_bottom_total_panel" />
</LinearLayout>
```

### 3.3. File `item_order_item.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@drawable/bg_card"
    android:padding="@dimen/spacing_lg"
    android:layout_marginBottom="@dimen/spacing_md">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical">

        <ImageView
            android:layout_width="@dimen/thumb_size"
            android:layout_height="@dimen/thumb_size"
            android:src="@drawable/ic_coffee"
            android:background="@drawable/bg_media"
            android:padding="@dimen/spacing_md"
            android:contentDescription="@null"
            app:tint="@color/accent" />

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical"
            android:layout_marginStart="@dimen/spacing_md">

            <TextView
                android:id="@+id/tv_product_name"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textAppearance="@style/TextAppearance.Cafe.BodyBold"
                tools:text="Cà phê sữa đá" />

            <TextView
                android:id="@+id/tv_unit_price"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textAppearance="@style/TextAppearance.Cafe.Caption"
                android:layout_marginTop="@dimen/spacing_xs"
                tools:text="35.000đ / món" />
        </LinearLayout>

        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <ImageButton
                android:id="@+id/btn_decrease"
                android:layout_width="@dimen/height_qty_btn"
                android:layout_height="@dimen/height_qty_btn"
                android:src="@drawable/ic_minus"
                android:background="@drawable/bg_quantity_btn"
                android:scaleType="centerInside"
                android:contentDescription="@string/content_desc_decrease" />

            <TextView
                android:id="@+id/tv_quantity"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:minWidth="24dp"
                android:gravity="center"
                android:textAppearance="@style/TextAppearance.Cafe.BodyBold"
                android:layout_marginHorizontal="@dimen/spacing_sm"
                tools:text="1" />

            <ImageButton
                android:id="@+id/btn_increase"
                android:layout_width="@dimen/height_qty_btn"
                android:layout_height="@dimen/height_qty_btn"
                android:src="@drawable/ic_plus"
                android:background="@drawable/bg_quantity_btn_plus"
                android:scaleType="centerInside"
                android:contentDescription="@string/content_desc_increase"
                app:tint="@color/text_on_accent" />
        </LinearLayout>
    </LinearLayout>

    <View
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:background="@color/border"
        android:layout_marginTop="@dimen/spacing_md" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:layout_marginTop="@dimen/spacing_md">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/label_amount"
            android:textAppearance="@style/TextAppearance.Cafe.Caption" />

        <TextView
            android:id="@+id/tv_subtotal"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textAppearance="@style/TextAppearance.Cafe.BodyBold"
            tools:text="35.000đ" />
    </LinearLayout>
</LinearLayout>
```

### 3.4. File `OrderItemAdapter.java`

Đặt tại `ui/order/OrderItemAdapter.java`.

```java
package com.example.cafe_manager.ui.order;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.model.CartItem;
import com.example.cafe_manager.util.CurrencyUtils;

public class OrderItemAdapter extends ListAdapter<CartItem, OrderItemAdapter.OrderItemVH> {

    public interface OnQuantityChangeListener {
        void onIncrease(int productId);
        void onDecrease(int productId);  // qty 1 → decrease = remove
    }

    private final OnQuantityChangeListener listener;

    public OrderItemAdapter(OnQuantityChangeListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderItemVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_item, parent, false);
        return new OrderItemVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemVH holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class OrderItemVH extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvUnitPrice;
        private final TextView tvQuantity;
        private final TextView tvSubtotal;
        private final ImageButton btnDecrease;
        private final ImageButton btnIncrease;

        OrderItemVH(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_product_name);
            tvUnitPrice = itemView.findViewById(R.id.tv_unit_price);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
            tvSubtotal = itemView.findViewById(R.id.tv_subtotal);
            btnDecrease = itemView.findViewById(R.id.btn_decrease);
            btnIncrease = itemView.findViewById(R.id.btn_increase);
        }

        void bind(final CartItem item, final OnQuantityChangeListener listener) {
            tvName.setText(item.getProductName());
            tvUnitPrice.setText(itemView.getContext().getString(
                    R.string.label_unit_price,
                    CurrencyUtils.formatVnd(item.getUnitPrice())));
            tvQuantity.setText(String.valueOf(item.getQuantity()));
            tvSubtotal.setText(CurrencyUtils.formatVnd(item.getSubtotal()));

            btnDecrease.setOnClickListener(v -> {
                if (listener != null) listener.onDecrease(item.getProductId());
            });
            btnIncrease.setOnClickListener(v -> {
                if (listener != null) listener.onIncrease(item.getProductId());
            });
        }
    }

    private static final DiffUtil.ItemCallback<CartItem> DIFF =
            new DiffUtil.ItemCallback<CartItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull CartItem o, @NonNull CartItem n) {
                    return o.getProductId() == n.getProductId();
                }
                @Override
                public boolean areContentsTheSame(@NonNull CartItem o, @NonNull CartItem n) {
                    return o.getQuantity() == n.getQuantity()
                            && o.getUnitPrice() == n.getUnitPrice()
                            && o.getProductName().equals(n.getProductName());
                }
            };
}
```

### 3.5. File `OrderActivity.java`

```java
package com.example.cafe_manager.ui.order;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.ui.table.TableActivity;
import com.example.cafe_manager.util.CurrencyUtils;
import com.example.cafe_manager.viewmodel.OrderViewModel;

public class OrderActivity extends AppCompatActivity {

    private OrderViewModel viewModel;
    private OrderItemAdapter adapter;

    private String tableName = "";

    private RecyclerView rvItems;
    private View emptyState;
    private TextView tvTotal;
    private Button btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_order);

        parseIntent();
        setupTopBar();
        setupFooter();
        setupEmptyState();
        setupRecyclerView();
        setupViewModel();
    }

    private void parseIntent() {
        tableName = getIntent().getStringExtra(TableActivity.EXTRA_TABLE_NAME);
        if (tableName == null) tableName = "";
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);
        View btnRight = topBar.findViewById(R.id.btn_right);

        title.setText(getString(R.string.title_order_with_table, tableName));
        caption.setText(R.string.caption_order);
        btnBack.setOnClickListener(v -> finish());
        btnRight.setVisibility(View.GONE);
    }

    private void setupFooter() {
        View footer = findViewById(R.id.footer);
        TextView tvLabel = footer.findViewById(R.id.tv_total_label);
        tvTotal = footer.findViewById(R.id.tv_total_amount);
        btnConfirm = footer.findViewById(R.id.btn_primary);

        tvLabel.setText(R.string.label_total);
        btnConfirm.setText(R.string.btn_confirm_order);
        btnConfirm.setOnClickListener(v -> onConfirmClicked());
    }

    private void setupEmptyState() {
        emptyState = findViewById(R.id.empty_state);
        TextView tvMsg = emptyState.findViewById(R.id.tv_empty_message);
        tvMsg.setText(R.string.empty_order);
    }

    private void setupRecyclerView() {
        rvItems = findViewById(R.id.rv_order_items);
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderItemAdapter(new OrderItemAdapter.OnQuantityChangeListener() {
            @Override
            public void onIncrease(int productId) {
                viewModel.increaseQuantity(productId);
            }
            @Override
            public void onDecrease(int productId) {
                viewModel.decreaseQuantity(productId);
            }
        });
        rvItems.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(OrderViewModel.class);

        viewModel.getCartItems().observe(this, items -> {
            adapter.submitList(items);

            boolean empty = items == null || items.isEmpty();
            rvItems.setVisibility(empty ? View.GONE : View.VISIBLE);
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            btnConfirm.setEnabled(!empty);
        });

        viewModel.getTotalAmount().observe(this, amount -> {
            tvTotal.setText(CurrencyUtils.formatVnd(amount != null ? amount : 0));
        });

        viewModel.getLoading().observe(this, loading -> {
            btnConfirm.setEnabled(loading != null && !loading
                    && !viewModel.isCartEmpty());
            btnConfirm.setText(Boolean.TRUE.equals(loading)
                    ? "Đang xử lý..."
                    : getString(R.string.btn_confirm_order));
        });

        viewModel.getConfirmSuccess().observe(this, orderId -> {
            if (orderId != null) {
                Toast.makeText(this, R.string.msg_order_confirmed,
                        Toast.LENGTH_SHORT).show();
                viewModel.clearConfirmSuccess();
                navigateBackToTables();
            }
        });

        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                viewModel.clearErrorMessage();
            }
        });
    }

    private void onConfirmClicked() {
        if (viewModel.isCartEmpty()) {
            return;
        }
        // Note: chưa có UI nhập note. MVP truyền rỗng. Khi nào có dialog
        // ghi chú thì lấy text rồi truyền vào.
        viewModel.confirmOrder("");
    }

    private void navigateBackToTables() {
        Intent intent = new Intent(this, TableActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
```

---

## 4. PaymentActivity

### 4.1. Mục tiêu

Hiển thị summary order + ô nhập discount + 3 dòng phương thức (CASH/BANKING/MOMO). Nút "Xác nhận thanh toán" gọi atomic payment → mở InvoiceActivity.

### 4.2. File `activity_payment.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/bg_primary">

    <include
        android:id="@+id/top_bar"
        layout="@layout/component_top_bar" />

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="@dimen/spacing_lg">

            <!-- Order summary card -->
            <LinearLayout
                android:id="@+id/card_summary"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="@drawable/bg_card"
                android:padding="@dimen/spacing_lg">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/label_order_summary"
                    android:textAppearance="@style/TextAppearance.Cafe.SectionLabel" />

                <!-- Item list dynamically inflated via Adapter -->
                <androidx.recyclerview.widget.RecyclerView
                    android:id="@+id/rv_items_summary"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="@dimen/spacing_sm"
                    android:nestedScrollingEnabled="false"
                    tools:listitem="@layout/item_invoice_item" />

                <View
                    android:layout_width="match_parent"
                    android:layout_height="1dp"
                    android:background="@color/border"
                    android:layout_marginTop="@dimen/spacing_md" />

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:layout_marginTop="@dimen/spacing_md">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="@string/label_subtotal"
                        android:textColor="@color/text_soft"
                        android:textSize="@dimen/text_body_sm" />

                    <TextView
                        android:id="@+id/tv_subtotal"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:textAppearance="@style/TextAppearance.Cafe.Body"
                        tools:text="119.000đ" />
                </LinearLayout>

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:layout_marginTop="@dimen/spacing_sm">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="@string/label_discount"
                        android:textColor="@color/text_soft"
                        android:textSize="@dimen/text_body_sm" />

                    <TextView
                        android:id="@+id/tv_discount"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:textColor="@color/success"
                        android:textSize="@dimen/text_body"
                        tools:text="−10.000đ" />
                </LinearLayout>

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:layout_marginTop="@dimen/spacing_md">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="@string/label_final_amount"
                        android:textAppearance="@style/TextAppearance.Cafe.BodyBold" />

                    <TextView
                        android:id="@+id/tv_final_amount"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:textAppearance="@style/TextAppearance.Cafe.Subtitle"
                        tools:text="109.000đ" />
                </LinearLayout>
            </LinearLayout>

            <!-- Discount input -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="@drawable/bg_card"
                android:padding="@dimen/spacing_lg"
                android:layout_marginTop="@dimen/spacing_md">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/label_discount"
                    android:textAppearance="@style/TextAppearance.Cafe.SectionLabel" />

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:layout_marginTop="@dimen/spacing_sm">

                    <EditText
                        android:id="@+id/et_discount"
                        android:layout_width="0dp"
                        android:layout_height="@dimen/height_input"
                        android:layout_weight="1"
                        android:background="@drawable/bg_input"
                        android:hint="@string/hint_discount_code"
                        android:inputType="number"
                        android:paddingHorizontal="@dimen/spacing_md"
                        android:autofillHints="off" />

                    <Button
                        android:id="@+id/btn_apply"
                        style="@style/Widget.Cafe.Button.Secondary"
                        android:layout_width="wrap_content"
                        android:layout_height="@dimen/height_input"
                        android:text="@string/btn_apply"
                        android:layout_marginStart="@dimen/spacing_sm" />
                </LinearLayout>
            </LinearLayout>

            <!-- Payment methods -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/label_payment_method"
                android:textAppearance="@style/TextAppearance.Cafe.SectionLabel"
                android:layout_marginTop="@dimen/spacing_lg" />

            <LinearLayout
                android:id="@+id/row_cash"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:background="@drawable/bg_card"
                android:padding="@dimen/spacing_lg"
                android:layout_marginTop="@dimen/spacing_sm"
                android:clickable="true"
                android:focusable="true">

                <ImageView
                    android:layout_width="42dp"
                    android:layout_height="42dp"
                    android:src="@drawable/ic_cash"
                    android:background="@drawable/bg_media"
                    android:padding="@dimen/spacing_sm"
                    android:contentDescription="@null"
                    app:tint="@color/accent" />

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:orientation="vertical"
                    android:layout_marginStart="@dimen/spacing_md">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/payment_cash"
                        android:textAppearance="@style/TextAppearance.Cafe.BodyBold" />

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/payment_cash_desc"
                        android:textAppearance="@style/TextAppearance.Cafe.Caption"
                        android:layout_marginTop="2dp" />
                </LinearLayout>

                <ImageView
                    android:id="@+id/radio_cash"
                    android:layout_width="20dp"
                    android:layout_height="20dp"
                    android:contentDescription="@null" />
            </LinearLayout>

            <LinearLayout
                android:id="@+id/row_banking"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:background="@drawable/bg_card"
                android:padding="@dimen/spacing_lg"
                android:layout_marginTop="@dimen/spacing_sm"
                android:clickable="true"
                android:focusable="true">

                <ImageView
                    android:layout_width="42dp"
                    android:layout_height="42dp"
                    android:src="@drawable/ic_bank_transfer"
                    android:background="@drawable/bg_media"
                    android:padding="@dimen/spacing_sm"
                    android:contentDescription="@null"
                    app:tint="@color/accent" />

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:orientation="vertical"
                    android:layout_marginStart="@dimen/spacing_md">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/payment_banking"
                        android:textAppearance="@style/TextAppearance.Cafe.BodyBold" />

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/payment_banking_desc"
                        android:textAppearance="@style/TextAppearance.Cafe.Caption"
                        android:layout_marginTop="2dp" />
                </LinearLayout>

                <ImageView
                    android:id="@+id/radio_banking"
                    android:layout_width="20dp"
                    android:layout_height="20dp"
                    android:contentDescription="@null" />
            </LinearLayout>

            <LinearLayout
                android:id="@+id/row_momo"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:background="@drawable/bg_card"
                android:padding="@dimen/spacing_lg"
                android:layout_marginTop="@dimen/spacing_sm"
                android:clickable="true"
                android:focusable="true">

                <ImageView
                    android:layout_width="42dp"
                    android:layout_height="42dp"
                    android:src="@drawable/ic_momo"
                    android:background="@drawable/bg_media"
                    android:padding="@dimen/spacing_sm"
                    android:contentDescription="@null"
                    app:tint="@color/accent" />

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:orientation="vertical"
                    android:layout_marginStart="@dimen/spacing_md">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/payment_momo"
                        android:textAppearance="@style/TextAppearance.Cafe.BodyBold" />

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/payment_momo_desc"
                        android:textAppearance="@style/TextAppearance.Cafe.Caption"
                        android:layout_marginTop="2dp" />
                </LinearLayout>

                <ImageView
                    android:id="@+id/radio_momo"
                    android:layout_width="20dp"
                    android:layout_height="20dp"
                    android:contentDescription="@null" />
            </LinearLayout>
        </LinearLayout>
    </androidx.core.widget.NestedScrollView>

    <!-- Footer button -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="@dimen/spacing_lg"
        android:background="@color/surface">

        <Button
            android:id="@+id/btn_confirm_payment"
            style="@style/Widget.Cafe.Button.Primary"
            android:layout_width="match_parent"
            android:layout_height="@dimen/height_button"
            android:text="@string/btn_confirm_payment" />
    </LinearLayout>
</LinearLayout>
```

### 4.3. File `PaymentActivity.java`

```java
package com.example.cafe_manager.ui.payment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.ui.table.TableActivity;
import com.example.cafe_manager.util.Constants;
import com.example.cafe_manager.util.CurrencyUtils;
import com.example.cafe_manager.viewmodel.OrderDetailViewModel;
import com.example.cafe_manager.viewmodel.PaymentViewModel;

public class PaymentActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "order_id";
    public static final String EXTRA_TABLE_ID = "table_id";
    public static final String EXTRA_TABLE_NAME = "table_name";
    public static final String EXTRA_SUBTOTAL = "subtotal";

    private PaymentViewModel paymentVm;
    private OrderDetailViewModel detailVm;
    private InvoiceItemAdapter itemsAdapter;

    private int orderId = -1;
    private int tableId = -1;
    private String tableName = "";
    private double subtotal = 0.0;

    // Views
    private TextView tvSubtotal, tvDiscount, tvFinalAmount;
    private EditText etDiscount;
    private Button btnApply, btnConfirm;
    private LinearLayout rowCash, rowBanking, rowMomo;
    private ImageView radioCash, radioBanking, radioMomo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_payment);

        parseIntent();
        setupTopBar();
        bindViews();
        setupItemsAdapter();
        setupPaymentRowClicks();
        setupDiscount();
        setupViewModels();
    }

    private void parseIntent() {
        orderId = getIntent().getIntExtra(EXTRA_ORDER_ID, -1);
        tableId = getIntent().getIntExtra(EXTRA_TABLE_ID, -1);
        tableName = getIntent().getStringExtra(EXTRA_TABLE_NAME);
        if (tableName == null) tableName = "";
        subtotal = getIntent().getDoubleExtra(EXTRA_SUBTOTAL, 0.0);

        if (orderId == -1 || tableId == -1) {
            Toast.makeText(this, "Thiếu thông tin order. Quay lại.",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);
        View btnRight = topBar.findViewById(R.id.btn_right);

        title.setText(getString(R.string.title_payment_with_table, tableName));
        caption.setText(R.string.caption_payment);
        btnBack.setOnClickListener(v -> finish());
        btnRight.setVisibility(View.GONE);
    }

    private void bindViews() {
        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvDiscount = findViewById(R.id.tv_discount);
        tvFinalAmount = findViewById(R.id.tv_final_amount);

        etDiscount = findViewById(R.id.et_discount);
        btnApply = findViewById(R.id.btn_apply);
        btnConfirm = findViewById(R.id.btn_confirm_payment);

        rowCash = findViewById(R.id.row_cash);
        rowBanking = findViewById(R.id.row_banking);
        rowMomo = findViewById(R.id.row_momo);
        radioCash = findViewById(R.id.radio_cash);
        radioBanking = findViewById(R.id.radio_banking);
        radioMomo = findViewById(R.id.radio_momo);
    }

    private void setupItemsAdapter() {
        RecyclerView rv = findViewById(R.id.rv_items_summary);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setNestedScrollingEnabled(false);
        itemsAdapter = new InvoiceItemAdapter();
        rv.setAdapter(itemsAdapter);
    }

    private void setupPaymentRowClicks() {
        rowCash.setOnClickListener(v ->
                paymentVm.selectPaymentMethod(Constants.PAYMENT_CASH));
        rowBanking.setOnClickListener(v ->
                paymentVm.selectPaymentMethod(Constants.PAYMENT_BANKING));
        rowMomo.setOnClickListener(v ->
                paymentVm.selectPaymentMethod(Constants.PAYMENT_MOMO));
    }

    private void setupDiscount() {
        // Áp dụng qua nút "Áp dụng" — đơn giản, không cần TextWatcher
        btnApply.setOnClickListener(v -> {
            String text = etDiscount.getText().toString().trim();
            double value = 0;
            if (!text.isEmpty()) {
                try {
                    value = Double.parseDouble(text);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Số tiền giảm không hợp lệ",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            paymentVm.setDiscount(value);
        });
    }

    private void setupViewModels() {
        paymentVm = new ViewModelProvider(this).get(PaymentViewModel.class);
        detailVm = new ViewModelProvider(this).get(OrderDetailViewModel.class);

        paymentVm.setOrderInfo(orderId, tableId, subtotal);
        detailVm.setOrderId(orderId);

        // Items summary
        detailVm.getItems().observe(this, items -> itemsAdapter.submitList(items));

        // Amounts
        paymentVm.getSubtotal().observe(this, v ->
                tvSubtotal.setText(CurrencyUtils.formatVnd(v != null ? v : 0)));
        paymentVm.getDiscountAmount().observe(this, v -> {
            double d = v != null ? v : 0;
            tvDiscount.setText(d > 0
                    ? "−" + CurrencyUtils.formatVnd(d)
                    : CurrencyUtils.formatVnd(0));
        });
        paymentVm.getFinalAmount().observe(this, v ->
                tvFinalAmount.setText(CurrencyUtils.formatVnd(v != null ? v : 0)));

        // Payment method selection → update radio UI
        paymentVm.getSelectedPaymentMethod().observe(this, this::updateMethodRadios);

        // Loading
        paymentVm.getLoading().observe(this, loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            btnConfirm.setEnabled(!isLoading);
            btnConfirm.setText(isLoading
                    ? "Đang xử lý..."
                    : getString(R.string.btn_confirm_payment));
        });

        // Success → mở Invoice
        paymentVm.getPaySuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                paymentVm.clearPaySuccess();
                Intent intent = new Intent(this, InvoiceActivity.class);
                intent.putExtra(InvoiceActivity.EXTRA_ORDER_ID, orderId);
                intent.putExtra(InvoiceActivity.EXTRA_TABLE_NAME, tableName);
                startActivity(intent);
                finish();
            }
        });

        // Error
        paymentVm.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                paymentVm.clearErrorMessage();
            }
        });

        // Confirm button
        btnConfirm.setOnClickListener(v -> paymentVm.confirmPayment());
    }

    private void updateMethodRadios(String selectedMethod) {
        applyRadioStyle(rowCash, radioCash,
                Constants.PAYMENT_CASH.equals(selectedMethod));
        applyRadioStyle(rowBanking, radioBanking,
                Constants.PAYMENT_BANKING.equals(selectedMethod));
        applyRadioStyle(rowMomo, radioMomo,
                Constants.PAYMENT_MOMO.equals(selectedMethod));
    }

    private void applyRadioStyle(View row, ImageView radio, boolean selected) {
        row.setBackgroundResource(selected
                ? R.drawable.bg_card_soft
                : R.drawable.bg_card);
        // Hiển thị dot khi selected. Đơn giản: dùng ic_check khi selected.
        radio.setImageResource(selected
                ? R.drawable.ic_check
                : 0);
        radio.setColorFilter(selected
                ? getColor(R.color.accent)
                : getColor(R.color.text_mute));
    }
}
```

> **Note**: Cách hiển thị radio dot ở trên dùng `ic_check` cho đơn giản. Nếu muốn dot tròn đúng prototype, tạo thêm 2 drawable `radio_selected.xml` (vòng tròn outline + chấm bên trong) và `radio_unselected.xml` (chỉ outline) rồi thay vào `setImageResource()`.

---

## 5. InvoiceActivity

### 5.1. Mục tiêu

Hiển thị hóa đơn read-only sau khi thanh toán thành công. Có nút "Quay về sơ đồ bàn" để clear stack về TableActivity.

### 5.2. File `activity_invoice.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/bg_primary">

    <include
        android:id="@+id/top_bar"
        layout="@layout/component_top_bar" />

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="@dimen/spacing_lg">

            <!-- Brand card -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:gravity="center_horizontal"
                android:background="@drawable/bg_card"
                android:padding="@dimen/spacing_xl">

                <ImageView
                    android:layout_width="52dp"
                    android:layout_height="52dp"
                    android:src="@drawable/ic_coffee"
                    android:background="@drawable/bg_media"
                    android:padding="@dimen/spacing_md"
                    android:contentDescription="@null"
                    app:tint="@color/accent" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/app_name"
                    android:textAppearance="@style/TextAppearance.Cafe.Subtitle"
                    android:layout_marginTop="@dimen/spacing_sm" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/invoice_thanks"
                    android:textAppearance="@style/TextAppearance.Cafe.Caption" />

                <View
                    android:layout_width="match_parent"
                    android:layout_height="1dp"
                    android:background="@color/border"
                    android:layout_marginVertical="@dimen/spacing_md" />

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal">

                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:orientation="vertical">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="@string/invoice_code"
                            android:textAppearance="@style/TextAppearance.Cafe.Caption" />

                        <TextView
                            android:id="@+id/tv_invoice_code"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:textAppearance="@style/TextAppearance.Cafe.BodyBold"
                            tools:text="#ORD-12345678" />
                    </LinearLayout>

                    <LinearLayout
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:gravity="end">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="@string/invoice_table"
                            android:textAppearance="@style/TextAppearance.Cafe.Caption" />

                        <TextView
                            android:id="@+id/tv_invoice_table"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:textAppearance="@style/TextAppearance.Cafe.BodyBold"
                            tools:text="B02" />
                    </LinearLayout>
                </LinearLayout>
            </LinearLayout>

            <!-- Items -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="@drawable/bg_card"
                android:padding="@dimen/spacing_lg"
                android:layout_marginTop="@dimen/spacing_md">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/invoice_items"
                    android:textAppearance="@style/TextAppearance.Cafe.SectionLabel" />

                <androidx.recyclerview.widget.RecyclerView
                    android:id="@+id/rv_items"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:nestedScrollingEnabled="false"
                    android:layout_marginTop="@dimen/spacing_sm"
                    tools:listitem="@layout/item_invoice_item" />

                <View
                    android:layout_width="match_parent"
                    android:layout_height="1dp"
                    android:background="@color/border"
                    android:layout_marginVertical="@dimen/spacing_md" />

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="@string/label_subtotal"
                        android:textColor="@color/text_soft"
                        android:textSize="@dimen/text_body_sm" />

                    <TextView
                        android:id="@+id/tv_subtotal"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:textAppearance="@style/TextAppearance.Cafe.Body"
                        tools:text="119.000đ" />
                </LinearLayout>

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:layout_marginTop="@dimen/spacing_sm">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="@string/label_discount"
                        android:textColor="@color/text_soft"
                        android:textSize="@dimen/text_body_sm" />

                    <TextView
                        android:id="@+id/tv_discount"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:textColor="@color/success"
                        android:textSize="@dimen/text_body"
                        tools:text="−10.000đ" />
                </LinearLayout>

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:layout_marginTop="@dimen/spacing_md">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="@string/label_final_amount"
                        android:textAppearance="@style/TextAppearance.Cafe.BodyBold" />

                    <TextView
                        android:id="@+id/tv_final_amount"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:textAppearance="@style/TextAppearance.Cafe.Subtitle"
                        tools:text="109.000đ" />
                </LinearLayout>
            </LinearLayout>

            <!-- Payment info -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="@drawable/bg_card"
                android:padding="@dimen/spacing_lg"
                android:layout_marginTop="@dimen/spacing_md">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/invoice_payment_info"
                    android:textAppearance="@style/TextAppearance.Cafe.SectionLabel" />

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:layout_marginTop="@dimen/spacing_sm">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="@string/invoice_method"
                        android:textColor="@color/text_soft"
                        android:textSize="@dimen/text_body_sm" />

                    <TextView
                        android:id="@+id/tv_method"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:textAppearance="@style/TextAppearance.Cafe.Body"
                        tools:text="Tiền mặt" />
                </LinearLayout>

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:layout_marginTop="@dimen/spacing_sm">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="@string/invoice_time"
                        android:textColor="@color/text_soft"
                        android:textSize="@dimen/text_body_sm" />

                    <TextView
                        android:id="@+id/tv_paid_at"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:textAppearance="@style/TextAppearance.Cafe.Body"
                        tools:text="09:48 · 14/05/2025" />
                </LinearLayout>

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:gravity="center_vertical"
                    android:layout_marginTop="@dimen/spacing_sm">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="Trạng thái"
                        android:textColor="@color/text_soft"
                        android:textSize="@dimen/text_body_sm" />

                    <TextView
                        style="@style/Widget.Cafe.Badge"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/status_paid"
                        android:textColor="@color/success"
                        android:background="@drawable/bg_badge_success" />
                </LinearLayout>
            </LinearLayout>
        </LinearLayout>
    </androidx.core.widget.NestedScrollView>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="@dimen/spacing_lg"
        android:background="@color/surface">

        <Button
            android:id="@+id/btn_back_to_tables"
            style="@style/Widget.Cafe.Button.Primary"
            android:layout_width="match_parent"
            android:layout_height="@dimen/height_button"
            android:text="@string/btn_back_to_tables" />
    </LinearLayout>
</LinearLayout>
```

### 5.3. File `item_invoice_item.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:paddingVertical="@dimen/spacing_sm">

    <TextView
        android:id="@+id/tv_item_summary"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:textAppearance="@style/TextAppearance.Cafe.Body"
        tools:text="Cà phê sữa × 1" />

    <TextView
        android:id="@+id/tv_item_total"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textAppearance="@style/TextAppearance.Cafe.BodyBold"
        tools:text="35.000đ" />
</LinearLayout>
```

### 5.4. File `InvoiceItemAdapter.java`

Đặt tại `ui/payment/InvoiceItemAdapter.java`. **Dùng chung cho PaymentActivity và InvoiceActivity** (cả 2 đều hiển thị OrderItemEntity read-only).

```java
package com.example.cafe_manager.ui.payment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.data.local.entity.OrderItemEntity;
import com.example.cafe_manager.util.CurrencyUtils;

public class InvoiceItemAdapter extends ListAdapter<OrderItemEntity, InvoiceItemAdapter.ItemVH> {

    public InvoiceItemAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public ItemVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invoice_item, parent, false);
        return new ItemVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemVH holder, int position) {
        holder.bind(getItem(position));
    }

    static class ItemVH extends RecyclerView.ViewHolder {
        private final TextView tvSummary;
        private final TextView tvTotal;

        ItemVH(View itemView) {
            super(itemView);
            tvSummary = itemView.findViewById(R.id.tv_item_summary);
            tvTotal = itemView.findViewById(R.id.tv_item_total);
        }

        void bind(OrderItemEntity item) {
            tvSummary.setText(item.getProductNameSnapshot()
                    + " × " + item.getQuantity());
            tvTotal.setText(CurrencyUtils.formatVnd(item.getSubtotal()));
        }
    }

    private static final DiffUtil.ItemCallback<OrderItemEntity> DIFF =
            new DiffUtil.ItemCallback<OrderItemEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull OrderItemEntity o, @NonNull OrderItemEntity n) {
                    return o.getOrderItemId() == n.getOrderItemId();
                }
                @Override
                public boolean areContentsTheSame(@NonNull OrderItemEntity o, @NonNull OrderItemEntity n) {
                    return o.getQuantity() == n.getQuantity()
                            && o.getSubtotal() == n.getSubtotal();
                }
            };
}
```

### 5.5. File `InvoiceActivity.java`

```java
package com.example.cafe_manager.ui.payment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cafe_manager.R;
import com.example.cafe_manager.ui.table.TableActivity;
import com.example.cafe_manager.util.CurrencyUtils;
import com.example.cafe_manager.util.DateTimeUtils;
import com.example.cafe_manager.util.StatusUtils;
import com.example.cafe_manager.viewmodel.OrderDetailViewModel;

public class InvoiceActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "order_id";
    public static final String EXTRA_TABLE_NAME = "table_name";

    private OrderDetailViewModel detailVm;
    private InvoiceItemAdapter itemsAdapter;

    private int orderId = -1;
    private String tableName = "";

    private TextView tvInvoiceCode, tvInvoiceTable;
    private TextView tvSubtotal, tvDiscount, tvFinalAmount;
    private TextView tvMethod, tvPaidAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_invoice);

        parseIntent();
        setupTopBar();
        bindViews();
        setupItemsAdapter();
        setupViewModel();
        setupBackButton();
    }

    private void parseIntent() {
        orderId = getIntent().getIntExtra(EXTRA_ORDER_ID, -1);
        tableName = getIntent().getStringExtra(EXTRA_TABLE_NAME);
        if (tableName == null) tableName = "";
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        TextView title = topBar.findViewById(R.id.tv_title);
        TextView caption = topBar.findViewById(R.id.tv_caption);
        View btnBack = topBar.findViewById(R.id.btn_back);
        View btnRight = topBar.findViewById(R.id.btn_right);

        title.setText(R.string.title_invoice);
        caption.setText(R.string.caption_invoice);
        // Invoice là điểm cuối — back = quay về Tables luôn
        btnBack.setOnClickListener(v -> navigateBackToTables());
        btnRight.setVisibility(View.GONE);
    }

    private void bindViews() {
        tvInvoiceCode = findViewById(R.id.tv_invoice_code);
        tvInvoiceTable = findViewById(R.id.tv_invoice_table);
        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvDiscount = findViewById(R.id.tv_discount);
        tvFinalAmount = findViewById(R.id.tv_final_amount);
        tvMethod = findViewById(R.id.tv_method);
        tvPaidAt = findViewById(R.id.tv_paid_at);

        tvInvoiceTable.setText(tableName);
    }

    private void setupItemsAdapter() {
        RecyclerView rv = findViewById(R.id.rv_items);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setNestedScrollingEnabled(false);
        itemsAdapter = new InvoiceItemAdapter();
        rv.setAdapter(itemsAdapter);
    }

    private void setupViewModel() {
        detailVm = new ViewModelProvider(this).get(OrderDetailViewModel.class);
        detailVm.setOrderId(orderId);

        detailVm.getOrder().observe(this, order -> {
            if (order != null) {
                tvInvoiceCode.setText("#" + order.getOrderCode());
            }
        });

        detailVm.getItems().observe(this, items -> itemsAdapter.submitList(items));

        detailVm.getPayment().observe(this, payment -> {
            if (payment == null) return;

            tvSubtotal.setText(CurrencyUtils.formatVnd(payment.getSubtotal()));
            double discount = payment.getDiscountAmount();
            tvDiscount.setText(discount > 0
                    ? "−" + CurrencyUtils.formatVnd(discount)
                    : CurrencyUtils.formatVnd(0));
            tvFinalAmount.setText(CurrencyUtils.formatVnd(payment.getFinalAmount()));

            tvMethod.setText(StatusUtils.getPaymentMethodDisplayName(
                    payment.getPaymentMethod()));
            tvPaidAt.setText(DateTimeUtils.formatDateTime(payment.getPaidAt()));
        });
    }

    private void setupBackButton() {
        Button btn = findViewById(R.id.btn_back_to_tables);
        btn.setOnClickListener(v -> navigateBackToTables());
    }

    @Override
    public void onBackPressed() {
        // Override back hardware → cũng về Tables, không quay lại Payment
        navigateBackToTables();
    }

    private void navigateBackToTables() {
        Intent intent = new Intent(this, TableActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
```

> **Note**: `DateTimeUtils.formatDateTime(long)` — kiểm tra đã có trong util chưa. Nếu chưa, format đơn giản: `new SimpleDateFormat("HH:mm · dd/MM/yyyy", new Locale("vi", "VN")).format(new Date(timestamp))`.

---

## 6. Cập nhật `AndroidManifest.xml`

Trong `<application>`, thêm `InvoiceActivity`. Đồng thời chuyển LAUNCHER từ MainActivity sang TableActivity (nếu báo cáo Member 2 chưa làm).

```xml
<activity
    android:name=".ui.table.TableActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<activity
    android:name=".ui.menu.MenuActivity"
    android:exported="false" />

<activity
    android:name=".ui.order.OrderActivity"
    android:exported="false" />

<activity
    android:name=".ui.payment.PaymentActivity"
    android:exported="false" />

<activity
    android:name=".ui.payment.InvoiceActivity"
    android:exported="false" />

<!-- MainActivity có thể xoá hoặc giữ không LAUNCHER -->
```

---

## 7. Cập nhật cần đồng bộ với báo cáo Member 2

**Vấn đề:** Báo cáo Member 2 viết:  
```
Click bàn OCCUPIED → OrderActivity
```

**Đúng:** OrderActivity hiện chỉ làm Mode 1 (cart). Cho OCCUPIED, không có cart → màn rỗng. Cần đổi sang `OrdersListActivity` (Member 4) để hiển thị danh sách order đang phục vụ, từ đó user chọn order → PaymentActivity.

**Fix tạm thời (trước khi OrdersList xong):**

Trong `TableActivity.java` của Member 2, sửa `onTableClicked()`:

```java
private void onTableClicked(TableEntity table) {
    if (Constants.TABLE_EMPTY.equals(table.getStatus())) {
        Intent intent = new Intent(this, MenuActivity.class);
        intent.putExtra(EXTRA_TABLE_ID, table.getTableId());
        intent.putExtra(EXTRA_TABLE_NAME, table.getTableName());
        startActivity(intent);
    } else {
        // OCCUPIED — tạm thời show toast, đợi OrdersListActivity của Member 4
        Toast.makeText(this,
                "Bàn đang có order. Vào tab Order để thu tiền.",
                Toast.LENGTH_SHORT).show();
    }
}
```

Khi `OrdersListActivity` xong, thay bằng:
```java
} else {
    Intent intent = new Intent(this, OrdersListActivity.class);
    intent.putExtra(EXTRA_TABLE_ID, table.getTableId()); // optional: filter
    startActivity(intent);
}
```

---

## 8. Test checklist

### OrderActivity

```text
[ ] Mở từ MenuActivity có 1+ món trong giỏ → hiển thị list, total đúng
[ ] Mở từ MenuActivity không có món (vd vào thẳng OrderActivity) → 
    empty state hiện, nút "Xác nhận order" disable
[ ] Click +/- → quantity + total cập nhật ngay
[ ] Decrease món có qty=1 → món bị xoá khỏi list
[ ] Format tiền đúng VND
[ ] Click "Xác nhận order" lúc loading → button đổi text "Đang xử lý..."
[ ] Sau confirm thành công → Toast + back về TableActivity (clear stack)
[ ] Sau confirm: bàn vừa chọn ở TableActivity hiện status OCCUPIED
[ ] Sau confirm: cart bar ở MenuActivity (nếu mở lại) ẩn (cart cleared)
[ ] Confirm gặp lỗi → Toast error, không navigate
```

### PaymentActivity

```text
[ ] Mở từ OrdersList với extras đầy đủ → hiển thị table name ở title
[ ] Item summary list đúng (tên · qty + thành tiền)
[ ] Subtotal/Discount/Final hiển thị đúng theo subtotal truyền vào
[ ] Mặc định method = Tiền mặt (radio CASH checked)
[ ] Click row CASH/BANKING/MOMO → radio đổi, row đổi background
[ ] Nhập discount "10000" → click Áp dụng → Discount hiện "−10.000đ", Final giảm
[ ] Nhập discount > subtotal → bị clamp về = subtotal (Final = 0)
[ ] Nhập text không phải số → Toast lỗi
[ ] Click "Xác nhận thanh toán" → button đổi "Đang xử lý..."
[ ] Sau pay thành công → navigate sang InvoiceActivity
[ ] Sau pay: bàn ở TableActivity hiện EMPTY (đã update)
[ ] Sau pay: order ở DB có status = PAID
```

### InvoiceActivity

```text
[ ] Hiển thị mã hóa đơn #ORD-... đúng (từ OrderEntity.orderCode)
[ ] Hiển thị tên bàn ở góc phải
[ ] List item hiển thị "Tên × qty" + giá thành tiền đúng
[ ] Subtotal/Discount/Final khớp với payment lúc thanh toán
[ ] Method tiếng Việt (Tiền mặt / Chuyển khoản / Ví MoMo)
[ ] Thời gian định dạng "HH:mm · dd/MM/yyyy"
[ ] Badge "Đã thanh toán" hiện màu xanh
[ ] Click "Quay về sơ đồ bàn" → về Table, stack đã clear
[ ] Bấm back hardware → cũng về Table, không quay lại Payment
```

---

## 9. Cạm bẫy hay gặp

**1. `OrderDetailViewModel` không trigger khi quay lại Activity sau rotation**  
Khi xoay màn hình, ViewModel còn nhưng `orderIdLive` value có thể bị mất nếu chưa gọi `setOrderId()` lại. Activity nên gọi `setOrderId()` mỗi lần `onCreate()` — `switchMap` sẽ no-op nếu id không đổi.

**2. `paymentVm.getPaySuccess()` trigger lại sau rotation**  
`MutableLiveData` giữ value cũ. Đã có `clearPaySuccess()` để set null, nhưng nếu Activity bị destroy + recreate sau khi navigate, có thể trigger Invoice 2 lần. Để an toàn, gọi `clearPaySuccess()` NGAY TRƯỚC khi `startActivity(InvoiceActivity)`.

**3. EditText discount không reset khi mở lại Payment cho order khác**  
ViewModel scope = Activity. Mỗi PaymentActivity instance có ViewModel riêng. Discount mặc định = 0. OK.

**4. `subtotal` truyền qua Intent có thể sai số do double**  
Trong MVP không sao. Nếu chính xác hơn, dùng `OrderEntity.totalAmount` từ DB thay vì truyền qua Intent — load qua `OrderDetailViewModel.getOrder().observe()`, sau đó mới `paymentVm.setOrderInfo(orderId, tableId, order.getTotalAmount())`.

**5. Atomic transaction fail giữa chừng**  
`OrderTransactionDao` và `PaymentTransactionDao` đảm bảo atomic. Nếu DB lỗi giữa chừng, sẽ rollback toàn bộ. Activity nhận error qua `RepositoryCallback.onError()` → hiển thị Toast.

**6. ListAdapter `submitList(null)` an toàn không?**  
ListAdapter handle `null` OK. Adapter sẽ render empty. Không cần guard `if (list != null)`.

**7. RecyclerView lồng trong NestedScrollView không scroll riêng**  
Set `android:nestedScrollingEnabled="false"` cho RecyclerView con. Đã có sẵn.

**8. `PaymentEntity.discount_amount` lưu 0.0 nếu user không nhập discount**  
OK. InvoiceActivity hiển thị "0 ₫" hoặc bỏ row đi nếu 0. MVP: cứ hiện 0 cho đơn giản.

**9. Format tiền `−10.000đ`**  
Dấu `−` (U+2212 minus sign) hoặc `-` (hyphen)? Báo cáo dùng `−` (proper minus). Copy đúng ký tự vào Java string.

**10. `ViewModelProvider(this)` vs `ViewModelProvider(this).Factory`**  
ViewModel của project đều `extends AndroidViewModel(Application)` → không cần Factory custom. `ViewModelProvider(this).get(XxxViewModel.class)` đủ dùng.

---

## Tóm tắt deliverable Member 3

```text
viewmodel/
    OrderDetailViewModel.java       ✅ (mới)

res/layout/
    activity_order.xml              ✅ (replace boilerplate)
    item_order_item.xml             ✅ (mới)
    activity_payment.xml            ✅ (replace boilerplate)
    activity_invoice.xml            ✅ (mới)
    item_invoice_item.xml           ✅ (mới)

ui/order/
    OrderActivity.java              ✅ (replace boilerplate)
    OrderItemAdapter.java           ✅ (replace skeleton)

ui/payment/
    PaymentActivity.java            ✅ (replace boilerplate)
    InvoiceActivity.java            ✅ (replace boilerplate)
    InvoiceItemAdapter.java         ✅ (mới — dùng chung cho Payment + Invoice)

AndroidManifest.xml                 ✅ (thêm InvoiceActivity, đổi LAUNCHER)
```

Tổng: **11 file XML/Java cần code**. Estimate **2.5–3.5 ngày** cho 1 người.

---

## Phụ lục — Flow giữa các Activity

```
                        TableActivity (LAUNCHER)
                          ▲           │
                          │           ├── click EMPTY ────► MenuActivity
                          │           │                        │
        (back hardware    │           │                        │ "Xem order"
         hoặc nút         │           │                        ▼
         "Quay về")       │           │                     OrderActivity
                          │           │                        │
                          │ CLEAR_TOP │                        │ "Xác nhận order"
                          │           │                        │ (atomic save)
                          │           │                        ▼
                          ◄───────────┴───────────────────── (back to Tables)
                          │
                          └── click OCCUPIED ────► OrdersListActivity (Member 4)
                                                       │ click "Thu tiền"
                                                       ▼
                                                  PaymentActivity
                                                       │ "Xác nhận thanh toán"
                                                       │ (atomic pay)
                                                       ▼
                                                  InvoiceActivity
                                                       │ "Quay về sơ đồ bàn"
                                                       ▼ (CLEAR_TOP)
                                                  TableActivity
```
