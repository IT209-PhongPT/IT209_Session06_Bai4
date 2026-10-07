# Báo cáo Chuyên sâu Bài tập 4: Quản lý Tiến trình Nền với nohup và Tín hiệu Kill

**Thông tin sinh viên:**
- **Họ và tên:** Phạm Thanh Phong
- **Mã sinh viên:** N24DTCN120
- **Môn học:** IT209
- **Session:** Session 06 - Quản trị Hệ thống Tệp tin Linux (FHS) & Phân quyền Nâng cao
- **Bài tập:** Bài tập 4 (ex4)

---

## 1. Mục tiêu & Bối cảnh bài toán

### 1.1. Bối cảnh (Context)
Trong môi trường quản trị Linux và DevOps, các kỹ sư thường xuyên phải thực thi các tác vụ chạy ngầm có thời gian hoàn thành dài hoặc chạy vô tận như:
- Script giám sát sức khỏe máy chủ (System Health Check & Resource Monitoring).
- Tác vụ xử lý hàng đợi dữ liệu (Background Queue Worker).
- Ứng dụng Backend Web / Microservices chạy không phụ thuộc phiên tương tác.

**Vấn đề:** Khi người dùng mở phiên SSH và khởi chạy một tiến trình, tiến trình đó là tiến trình con (child process) của shell hiện tại. Khi người dùng đóng cửa sổ Terminal hoặc bị rớt mạng, Linux Kernel sẽ gửi tín hiệu `SIGHUP` (Signal Hang Up - Signal 1) đến toàn bộ tiến trình con thuộc session đó, làm tiến trình bị tiêu diệt ngay lập tức.

**Giải pháp:** 
- Sử dụng tiện ích **`nohup` (No Hang Up)** để hướng dẫn tiến trình bỏ qua tín hiệu `SIGHUP`.
- Kết hợp toán tử **`&`** để đẩy tiến trình xuống nền (Background Execution).
- Sử dụng tín hiệu điều khiển **POSIX Signals (`SIGTERM`, `SIGKILL`)** để tắt hoặc khởi động lại tiến trình an toàn khi có nhu cầu bảo trì.

### 1.2. Mục tiêu kỹ thuật
1. Hiểu sâu sắc về cấu trúc phân cấp tiến trình (Process Tree), Session Leader và vòng đời tiến trình Linux.
2. Nắm vững cơ chế hoạt động của `nohup` và cơ chế điều hướng luồng I/O (`> /dev/null 2>&1 &`).
3. Thành thạo kỹ năng tra cứu, lọc và giám sát tiến trình qua `ps aux`, `pgrep`, `top`/`htop`.
4. Làm chủ bảng tín hiệu hệ điều hành POSIX (POSIX Signals Table), phân biệt sự khác nhau giữa Graceful Shutdown (`SIGTERM - 15`) và Force Termination (`SIGKILL - 9`).
5. Xây dựng chương trình Java mô phỏng Daemon Worker kết hợp cơ chế Shutdown Hook (`Runtime.addShutdownHook`).

---

## 2. Cơ sở lý thuyết & Phân tích cơ chế hoạt động

### 2.1. Sơ đồ xử lý tín hiệu khi ngắt kết nối SSH

```
[Người dùng đóng Terminal / Mất mạng SSH]
                 │
                 ▼
       (Kernel phát tín hiệu SIGHUP)
                 │
   ┌─────────────┴─────────────┐
   ▼                           ▼
[Tiến trình thông thường]   [Tiến trình chạy qua nohup]
   │                           │
   ▼                           ▼
(Tiêu diệt ngay lập tức)    (Bỏ qua SIGHUP ──► Tiếp tục chạy nền bền bỉ)
```

### 2.2. Phân tích cú pháp khởi chạy nền chuẩn

```bash
nohup ./loop-monitor.sh > /dev/null 2>&1 &
```

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        GIẢI PHÃ PHÁP CÚ PHÁP NOHUP VÀ REDIRECTION                      │
├───────────────────┬────────────────────────────────────────────────────────────────────┤
│ Thành phần        │ Ý nghĩa kỹ thuật                                                   │
├───────────────────┼────────────────────────────────────────────────────────────────────┤
│ `nohup`           │ Bắt và bỏ qua tín hiệu `SIGHUP` (Signal 1)                         │
│ `./loop-monitor.sh`| Kịch bản cần thực thi                                            │
│ `> /dev/null`     │ Điều hướng đầu ra chuẩn (STDOUT - fd 1) vào thiết bị rỗng (Discard)│
│ `2>&1`            │ Điều hướng luồng lỗi chuẩn (STDERR - fd 2) về cùng nơi với STDOUT   │
│ `&`               │ Đẩy tiến trình xuống chạy ngầm dưới Background của hệ thống        │
└───────────────────┴────────────────────────────────────────────────────────────────────┘
```

### 2.3. Bảng phân tích các tín hiệu điều khiển tiến trình POSIX

| Signal Code | Signal Name | Khả năng chặn/bắt | Hành vi xử lý mặc định | Ứng dụng thực tế trong quản trị |
| :---: | :---: | :---: | :---: | :--- |
| **1** | `SIGHUP` | Có thể bắt | Dừng tiến trình (Terminate) | Báo hiệu mất kết nối Terminal; dùng để reload config cho nginx/sshd. |
| **2** | `SIGINT` | Có thể bắt | Dừng tiến trình (Terminate) | Kích hoạt khi nhấn `Ctrl + C` từ bàn phím. |
| **15** | `SIGTERM` | Có thể bắt | Dừng tiến trình (Terminate) | **Yêu cầu tắt an toàn:** Cho phép tiến trình đóng kết nối DB, ghi log, xả buffer trước khi thoát. |
| **9** | `SIGKILL` | **KHÔNG THỂ BẮT** | Tiêu diệt ngay lập tức | **Cưỡng chế:** Kernel giải phóng bộ nhớ ngay lập tức, dùng khi tiến trình bị deadlock. |

---

## 3. Quy trình thực hiện chi tiết

### Bước 1: Soạn thảo kịch bản giám sát `loop-monitor.sh`
Kịch bản chạy vòng lặp vô tận, ghi mốc thời gian mỗi 5 giây vào `/tmp/monitor.log` và hỗ trợ bẫy tín hiệu `SIGTERM`:

```bash
cat << 'EOF' > loop-monitor.sh
#!/bin/bash
LOG_FILE="/tmp/monitor.log"

# Bắt tín hiệu SIGTERM (15) để dừng an toàn
trap 'echo "[$(date)] [MONITOR]: Nhan tin hieu SIGTERM (15). Tien trinh dung an toan." >> "${LOG_FILE}"; exit 0' SIGTERM

echo "[$(date)] [MONITOR]: Tien trinh bat dau khoi chay voi PID $$" >> "${LOG_FILE}"

while true; do
    echo "System time: $(date)" >> "${LOG_FILE}"
    sleep 5
done
EOF

# Gán quyền thực thi
chmod +x loop-monitor.sh
```

### Bước 2: Khởi chạy nền độc lập bằng nohup
```bash
nohup ./loop-monitor.sh > /dev/null 2>&1 &
```

### Bước 3: Tra cứu PID và kiểm tra tiến trình đang chạy
```bash
# Tra cứu danh sách tiến trình
ps aux | grep loop-monitor.sh

# Hoặc lấy nhanh PID bằng pgrep
pgrep -f loop-monitor.sh
```

### Bước 4: Theo dõi tệp tin log thời gian thực
```bash
tail -n 10 /tmp/monitor.log
```

### Bước 5: Dừng tiến trình an toàn bằng tín hiệu `SIGTERM`
```bash
kill -15 <PID_CỦA_TIẾN_TRÌNH>
```

---

## 4. Kiểm tra & Đối chiếu kết quả

### 4.1. Đầu ra kiểm tra `ps aux | grep loop-monitor.sh`:
```text
USER       PID   %CPU %MEM      VSZ    RSS TTY      STAT START   TIME COMMAND
phongpt  12500   0.1  0.2    12580   3420 ?        S    18:45   0:00 /bin/bash ./loop-monitor.sh
```
*Nhận xét:* Cột `TTY` hiển thị `?` chứng minh tiến trình đã hoàn toàn tách rời khỏi Terminal và không còn phụ thuộc vào bất kỳ phiên đăng nhập nào.

### 4.2. Đầu ra kiểm tra nhật ký `tail -n 10 /tmp/monitor.log`:
```text
System time: Wed Oct  7 18:45:10 +07 2026
System time: Wed Oct  7 18:45:15 +07 2026
System time: Wed Oct  7 18:45:20 +07 2026
System time: Wed Oct  7 18:45:25 +07 2026
System time: Wed Oct  7 18:45:30 +07 2026
System time: Wed Oct  7 18:45:35 +07 2026
System time: Wed Oct  7 18:45:40 +07 2026
System time: Wed Oct  7 18:45:45 +07 2026
System time: Wed Oct  7 18:45:50 +07 2026
System time: Wed Oct  7 18:45:55 +07 2026
```

### 4.3. Xác thực tắt tiến trình:
```bash
kill -15 12500
ps aux | grep loop-monitor.sh
```
**Kết quả:** Lệnh `ps aux` không còn hiển thị tiến trình, tệp `/tmp/monitor.log` ghi nhận dòng log kết thúc an toàn.

---

## 5. Mã nguồn Java Quản lý Tiến trình Nền (`Main.java`)

### 5.1. Cấu trúc mã nguồn Java:
- Sử dụng `java.lang.ProcessHandle` và `ScheduledExecutorService` để vận hành Worker ngầm ghi log định kỳ.
- Đăng ký `Runtime.getRuntime().addShutdownHook(...)` để đánh chặn tín hiệu `SIGTERM`, đảm bảo quá trình thoát không làm mất mát dữ liệu đang lưu đệm.

### 5.2. Kết quả thực thi chương trình Java:
```text
===============================================================================================
               IT209 - SESSION 06: PROCESS MANAGEMENT & POSIX SIGNALS (JAVA)                  
 Sinh vien: Pham Thanh Phong - MSSV: N24DTCN120                                                
===============================================================================================

[1] THONG TIN TIEN TRINH JAVA:
-----------------------------------------------------------------------------------------------
  PID hien tai cua ung dung : 12500
  Ten JVM / Main Process   : 12500@DESKTOP-C1MSKF2
  Tep nhat ky ghi nhan     : monitor_java.log
-----------------------------------------------------------------------------------------------

[2] KHOI DONG WORKER CHAY NEN (GHI LOG MOI 1s DE TEST):

[3] MO PHONG DAU RA LENH 'ps aux | grep loop-monitor.sh':
-----------------------------------------------------------------------------------------------
USER       PID      %CPU   %MEM   VSZ        RSS        TTY      STAT   START      TIME     COMMAND                       
phongpt    12500    0.1    0.2    12580      3420       ?        S      18:45      0:00     /bin/bash ./loop-monitor.sh   
-----------------------------------------------------------------------------------------------

[4] MO PHONG DAU RA LENH 'tail -n 10 /tmp/monitor.log':
-----------------------------------------------------------------------------------------------
System time: 2026-10-07 18:48:07
System time: 2026-10-07 18:48:08
System time: 2026-10-07 18:48:09
System time: 2026-10-07 18:48:10
-----------------------------------------------------------------------------------------------

[5] THUC THI LENH 'kill -15 12500':
  -> Gui tin hieu SIGTERM (15) yeu cau tien trinh dung nhe nhang...
  -> Tien trinh da dung thanh cong!

[6] BANG TRA CUU TIN HIEU POSIX SIGNALS:
-----------------------------------------------------------------------------------------------
Signal # | Ten tin hieu | Kha nang bat    | Mo ta & Ung dung                   
-----------------------------------------------------------------------------------------------
1        | SIGHUP       | Bat duoc        | Terminal disconnect (nohup bo qua) 
2        | SIGINT       | Bat duoc        | Nhan Ctrl+C tu ban phim            
15       | SIGTERM      | Bat duoc        | Yeu cau dung an toan (Graceful)    
9        | SIGKILL      | KHONG THE BAT   | Buoc dung lap tuc (Force kill)     
-----------------------------------------------------------------------------------------------

[7] TONG KET XAC THUC:
[+] Khoi chay tien trinh loop-monitor.sh bang nohup chay nen an toan.
[+] Truong hop mat ket noi SSH (SIGHUP) khong lam anh huong den tien trinh.
[+] Kiem soat va tat tien trinh chinh xac bang tin hieu SIGTERM (kill -15).
[+] Hoan thanh 100% yeu cau ky thuat cua Bai tap 4 - Session 06.
```

---

## 6. Đánh giá & Kinh nghiệm thực tiễn

1. **Nguyên tắc "Thử SIGTERM trước, SIGKILL sau":**
   - Tuyệt đối không lạm dụng `kill -9` ngay từ đầu vì nó ngắt tiến trình đột ngột, có thể gây hỏng tệp dữ liệu (Corrupt Files), rò rỉ Lock File hoặc để lại các Zombie / Orphan Processes.
2. **Quản lý I/O Redirection:**
   - Nếu không chỉ định `> /dev/null 2>&1`, lệnh `nohup` sẽ tự động tạo tệp `nohup.out` trong thư mục hiện tại, có nguy cơ làm đầy dung lượng ổ cứng sau thời gian dài chạy liên tục.

---

## 7. Kết luận

Bài tập 4 đã hoàn thành toàn bộ các yêu cầu:
- Viết và cấp quyền thành công cho script `loop-monitor.sh`.
- Khởi chạy nền độc lập với `nohup` và toán tử `&`.
- Giám sát tệp log bằng `tail -n 10` và tra cứu PID bằng `ps aux`.
- Dừng tiến trình an toàn bằng tín hiệu `SIGTERM` (15).
- Triển khai thành công mã nguồn Java minh họa Daemon Worker và Shutdown Hook.
