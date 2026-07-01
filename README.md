# ☕ Cafe Manager - Hệ Thống Quản Lý Quán Cà Phê

Hệ thống quản lý quán cà phê toàn diện bao gồm: **Backend Server (Spring Boot)**, **Mobile App dành cho Nhân viên (Android Native)** và **Web Admin Dashboard dành cho Quản lý (React + Vite)**.

Hệ thống hỗ trợ quản lý khu vực bàn ăn, đặt món, gọi thanh toán, quản lý sơ đồ ca làm việc thông minh (Smart Scheduling), điểm danh, đăng ký nghỉ phép, và nhắn tin nội bộ thời gian thực qua WebSocket.

---

## 🏗️ Kiến Trúc Hệ Thống (Architecture)

Dự án được cấu trúc theo dạng **Monorepo** gồm 3 thành phần chính:

```
se114-cafe-management/
├── backend/             # Spring Boot 3.x REST API & WebSocket Server (Java 17)
├── frontend/            # Android Mobile App (Java, MVVM Architecture)
└── cafe-dashboard/      # Web Admin Dashboard (React, Vite, Tailwind CSS)
```

| Thành phần | Công nghệ sử dụng | Vai trò |
|---|---|---|
| **Backend** | Spring Boot 3.2, Spring Data JPA, PostgreSQL (Supabase), Spring Security (JWT), WebSocket (STOMP), Java Mail Sender | Cung cấp RESTful API, quản lý cơ sở dữ liệu, xử lý xác thực OTP, phân quyền và giao tiếp thời gian thực. |
| **Android App** | Java, MVVM, Retrofit 2, OkHttp, WebSocket client, SharedPreferences | Ứng dụng di động dành cho nhân viên phục vụ tại bàn, thực hiện đặt món, kiểm tra trạng thái bàn, điểm danh nhận ca và gửi đơn nghỉ phép. |
| **Web Dashboard** | React 18, Vite, Tailwind CSS, Context API, Axios | Giao diện web dành cho Quản trị viên/Quản lý để quản lý nhân sự, thiết lập sơ đồ ca, phê duyệt nghỉ phép và xem báo cáo doanh thu. |

---

## ✨ Các Tính Năng Chính

### 1. Quản Lý Bàn & Đặt Món (POS Flow)
* Quản lý danh sách khu vực (Areas) và sơ đồ bàn (Tables) động.
* Tạo đơn hàng trực tiếp tại bàn, gọi món và chuyển thông tin xuống bếp.
* Quy trình thanh toán khép kín: Đặt món ➔ Thanh toán ➔ Xuất hóa đơn (Invoice).

### 2. Quản Lý Nhân Sự & Ca Làm Việc (Scheduling & Shift Management)
* Quản lý danh sách nhân viên, thông tin chi tiết và phân quyền (Admin, Manager, Staff).
* **Smart Scheduling (Sơ đồ ca thông minh):** Tạo lịch làm việc tự động hoặc thủ công dựa trên các Shift Template.
* Quản lý tính khả dụng của nhân viên (Employee Availability).

### 3. Điểm Danh & Nghỉ Phép (Attendance & Leave Request)
* Nhân viên thực hiện Check-in / Check-out trên thiết bị Android khi vào ca làm việc để chấm công.
* Nhân viên gửi đơn xin nghỉ phép (Leave Request) kèm lý do thông qua ứng dụng Android.
* Quản lý phê duyệt/từ chối đơn nghỉ phép trên Web Dashboard. Khi đơn được duyệt, hệ thống sẽ tự động cập nhật và huỷ các ca làm việc bị trùng lặp của nhân viên đó.

### 4. Giao Tiếp Thời Gian Thực (Real-time Chat & News)
* Kênh chat nội bộ tích hợp trực tiếp trên Web Dashboard và Android App sử dụng WebSocket + STOMP protocol.
* Bản tin thông báo (News/Announcements) giúp quản lý truyền đạt thông tin quan trọng đến toàn bộ nhân viên tức thì.

### 5. Thống Kê & Báo Cáo Doanh Thu (Revenue & Report)
* Biểu đồ doanh thu chi tiết theo ngày, tuần, tháng.
* Thống kê các sản phẩm bán chạy nhất để tối ưu hóa menu.
* Nhật ký hoạt động hệ thống (Audit Log) lưu lại lịch sử thao tác của người dùng.

---

## 🚀 Hướng Dẫn Cài Đặt & Chạy Dự Án

### Yêu Cầu Hệ Thống (Prerequisites)
* **Java Development Kit (JDK):** Version 17
* **Node.js:** Phiên bản `>= 18.x` cùng `npm` hoặc `yarn`
* **Android Studio:** Phiên bản Flamingo trở lên (để chạy project Android)
* **Database:** PostgreSQL (Khuyến nghị dùng cơ sở dữ liệu lưu trữ trực tiếp trên Supabase)

---

### 1. Cấu Hình & Chạy Backend Server

1. Di chuyển vào thư mục `backend`:
   ```bash
   cd backend
   ```
2. Tạo file `.env` từ file `backend/.env.example` và cấu hình các thông số:
   ```properties
   # Kết nối Database Supabase (PostgreSQL)
   SUPABASE_URL=jdbc:postgresql://<your-supabase-db-host>:<port>/postgres
   SUPABASE_PASSWORD=<your-database-password>
   SUPABASE_ANON_KEY=<your-supabase-anon-key>

   # Cấu hình gửi mail OTP đăng nhập bằng Gmail SMTP
   SPRING_MAIL_USERNAME=<your-gmail-address>
   SPRING_MAIL_PASSWORD=<your-gmail-app-password>
   ```

   * **Lưu ý quan trọng:** `SPRING_MAIL_PASSWORD` không phải là mật khẩu đăng nhập Gmail thông thường. Đây phải là **Mật khẩu ứng dụng (App Password)** được tạo từ trang quản lý tài khoản Google (Yêu cầu tài khoản Gmail đã bật Xác minh 2 bước).

3. Khởi chạy ứng dụng bằng Maven:
   * **Trên Windows:**
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```
   * **Trên macOS/Linux:**
     ```bash
     chmod +x mvnw
     ./mvnw spring-boot:run
     ```
4. Server sẽ hoạt động tại địa chỉ mặc định: `http://localhost:8080`.

---

### 2. Cấu Hình & Chạy Web Admin Dashboard

1. Di chuyển vào thư mục `cafe-dashboard`:
   ```bash
   cd cafe-dashboard
   ```
2. Tạo file `.env` để cấu hình đường dẫn API trỏ đến Backend:
   ```env
   VITE_API_URL=http://localhost:8080
   ```
3. Cài đặt các gói phụ thuộc:
   ```bash
   npm install
   ```
4. Chạy dự án ở môi trường phát triển (Development Mode):
   ```bash
   npm run dev
   ```
5. Mở trình duyệt truy cập địa chỉ được hiển thị ở terminal (thường là `http://localhost:5173`).

---

### 3. Cấu Hình & Chạy Android App (Frontend)

1. Mở phần mềm **Android Studio**.
2. Chọn **Open an Existing Project** và dẫn đường dẫn đến thư mục `frontend` trong dự án.
3. Chờ Gradle đồng bộ (Sync Gradle) xong.
4. Cấu hình địa chỉ IP máy chủ backend tại file `app/build.gradle.kts`:
   * Chỉnh sửa dòng `buildConfigField` trong block `debug`:
     ```kotlin
     debug {
         buildConfigField("String", "BASE_URL", "\"http://<IP_MÁY_TÍNH_CỦA_BẠN>:8080/\"")
     }
     ```
     * **Nếu sử dụng Máy ảo Android Studio (Emulator):** Bạn có thể thiết lập `"http://10.0.2.2:8080/"`.
     * **Nếu chạy trên Thiết bị thật:** Máy tính chạy backend và điện thoại phải kết nối chung một mạng Wi-Fi. Hãy sử dụng địa chỉ IPv4 của máy tính (ví dụ: `"http://192.168.1.46:8080/"`).
5. Nếu chạy trên thiết bị Android thật, bạn cũng có thể sử dụng cổng kết nối ngược (ADB Reverse) thông qua cổng USB:
   ```bash
   adb reverse tcp:8080 tcp:8080
   ```
6. Bấm nút **Run** (phím tắt `Shift + F10`) để cài đặt và chạy ứng dụng trên thiết bị/máy ảo.

---

## 📂 Chi Tiết Cấu Trúc Dự Án

### 💻 Backend API (`backend/`)
* `com.example.cafe_manager_api.controller`: Tiếp nhận và định tuyến các HTTP Requests từ Client.
* `com.example.cafe_manager_api.service`: Xử lý toàn bộ logic nghiệp vụ (Business Logic).
* `com.example.cafe_manager_api.repository`: Giao tiếp trực tiếp với cơ sở dữ liệu qua Spring Data JPA.
* `com.example.cafe_manager_api.entity`: Định nghĩa các đối tượng ORM map với bảng cơ sở dữ liệu.
* `com.example.cafe_manager_api.security`: Quản lý cơ chế bảo mật JWT, xác thực OTP qua Email, và phân quyền truy cập.

### 🌐 Web Dashboard (`cafe-dashboard/`)
* `src/pages/`: Chứa các màn hình chức năng chính (Dashboard, Attendance, LeaveRequest, Shifts, Menu, Revenue...).
* `src/components/`: Các thành phần UI dùng chung (Sidebar, Navbar, Modal, Card, Button...).
* `src/api/`: Chứa cấu hình gọi API qua Axios.

### 📱 Android Application (`frontend/`)
* `app/src/main/java/com/example/cafe_manager/ui`: Các lớp Activity, Fragment điều phối giao diện hiển thị.
* `app/src/main/java/com/example/cafe_manager/viewmodel`: Quản lý trạng thái giao diện và giao tiếp giữa UI và Data layer.
* `app/src/main/java/com/example/cafe_manager/data`: Chứa logic lấy dữ liệu qua Retrofit và quản lý phiên đăng nhập (SessionManager, WebSockets).
