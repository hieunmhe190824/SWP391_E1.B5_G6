

 FEATURE 1: AUTHENTICATION & USER MANAGEMENT

 UC01: Register Account - done
- Roles: Guest
- Mô tả: Đăng ký tài khoản mới, submit thông tin cá nhân và giấy tờ

 UC02: Login - done
- Roles: Customer, Staff, Admin
- Mô tả: Đăng nhập vào hệ thống

 UC03: Manage Profile - done
- Roles: Customer, Staff, Admin
- Mô tả: Quản lý thông tin cá nhân, cập nhật giấy tờ

---

 FEATURE 2: VEHICLE CATALOG

 UC04: Browse Vehicles - done
- Roles: Customer, Guest
- Mô tả: Xem, tìm kiếm, lọc danh sách xe có sẵn, xem chi tiết xe
- Gộp: View list + Search/Filter + View details

 UC05: Manage Vehicles - done
- Roles: Admin, Staff
- Mô tả: CRUD thông tin xe, cập nhật trạng thái, upload hình ảnh
- Gộp: Add/Edit/Delete vehicles + Update status + Manage images

---

 FEATURE 3: BOOKING MANAGEMENT

 UC06: Create Booking - done
- Roles: Customer
- Mô tả: Tạo đặt xe mới, chọn thời gian, địa điểm, submit giấy tờ
- Gộp: Schedule rental + Submit documents

 UC07: Approve/Reject Booking - donedone
- Roles: Staff
- Mô tả: Xem yêu cầu đặt xe, kiểm tra giấy tờ, phê duyệt hoặc từ chối
- Gộp: Check documents + 

 UC08: Cancel Booking - done
- Roles: Customer, Staff
- Mô tả: Hủy đặt xe (trước khi tạo hợp đồng)

 UC09: View Booking History - done
- Roles: Customer
- Mô tả: Xem lịch sử đặt xe và trạng thái

---

 FEATURE 4: CONTRACT & PAYMENT

 UC10: Create Contract - done
- Roles: Staff
- Mô tả: Tạo hợp đồng thuê xe từ booking đã được duyệt
- Gộp: Create contract + Collect deposit (online payment)

 UC11: View Contract - done
- Roles: Customer, Staff
- Mô tả: Xem chi tiết hợp đồng, điều khoản, in/download hợp đồng

---

 FEATURE 5: RENTAL TRACKING

 UC12: Check-in Vehicle (Pickup)
- Roles: Staff
- Mô tả: Ghi nhận customer đã nhận xe, upload ảnh tình trạng xe, cập nhật odometer
- Gộp: Inspect + Confirm acceptance + Update status to "Rented"
- Lưu ý: Nếu có vấn đề → Cancel contract + Refund 

 UC13: Track Active Rentals
- Roles: Customer, Staff, Admin
- Mô tả: Xem danh sách và trạng thái các xe đang được thuê

---

 FEATURE 6: VEHICLE RETURN

 UC14: Check-in Return
- Roles: Staff
- Mô tả: Ghi nhận xe trả về, kiểm tra xe, tính toán các khoản phí
- Bao gồm:
  - Ghi thời gian trả xe
  - Kiểm tra trễ hạn → Tính phí overdue
  - Kiểm tra hư hỏng → Tính phí damage
  - Kiểm tra one-way return → Tính phí one-way
  - Upload ảnh tình trạng xe
  - Cập nhật odometer

 UC15: Process Rental Payment
- Roles: Staff, System
- Mô tả: Tạo hóa đơn tiền thuê, customer thanh toán online/offline, trừ các khoản phí từ cọc
- Gộp: Generate invoice + Pay rental fee + Process payment

 UC16: Hold Deposit
- Roles: System
- Mô tả: Giữ tiền cọc 14 ngày, tracking trạng thái
- Note: Tự động, không có interaction

---

 FEATURE 7: DEPOSIT REFUND

 UC17: Check Traffic Violations
- Roles: Staff, System
- Mô tả: Sau 14 ngày, staff nhập vi phạm giao thông (nếu có) vào hệ thống

 UC18: Process Deposit Refund
- Roles: Staff, System
- Mô tả: Tính toán và thực hiện hoàn tiền cọc, gửi thông báo cho customer
- Gộp: Calculate refund + Generate invoice + Process refund + Notify customer

---

 FEATURE 8: CUSTOMER SUPPORT

 UC19: Submit Support Request
- Roles: Customer, Guest
- Mô tả: Tạo yêu cầu hỗ trợ mới, chọn danh mục, mô tả vấn đề

 UC20: Manage Support Tickets
- Roles: Staff, Admin
- Mô tả: Xem, phân loại, trả lời, cập nhật trạng thái yêu cầu hỗ trợ
- Gộp: Review + Reply + Update status

 UC21: Rate Support
- Roles: Customer
- Mô tả: Đánh giá chất lượng hỗ trợ sau khi giải quyết
- Gộp: Provide feedback + Store feedback

---

 FEATURE 9: REPORTS & ANALYTICS - done

 UC22: View Reports
- Roles: Admin, Staff
- Mô tả: Xem các báo cáo (doanh thu, xe sử dụng, khách hàng, v.v.)
- Gộp: Revenue report + Vehicle usage + Customer stats + Dashboard

---

 FEATURE 10: NOTIFICATIONS

 UC23: Manage Notifications
- Roles: Customer, Staff, Admin
- Mô tả: Xem, đánh dấu đã đọc thông báo trong hệ thống
- Note: Email/SMS tự động gửi

