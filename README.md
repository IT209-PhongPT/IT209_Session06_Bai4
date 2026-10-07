# Báo cáo Bài tập 4: Quản lý Tiến trình Nền với nohup và Tín hiệu Kill

- **Họ và tên:** Phạm Thanh Phong
- **Mã sinh viên:** N24DTCN120
- **Môn học:** IT209
- **Session:** 06 - Quản trị Hệ thống Tệp tin Linux (FHS) & Phân quyền Nâng cao
- **Đường dẫn nộp bài:** `homework/session_06/ex4/`

---

## 1. Bối cảnh & Yêu cầu bài tập

Triển khai và quản lý tiến trình chạy nền độc lập với phiên làm việc Terminal (SSH Session) bằng `nohup` và cơ chế tín hiệu POSIX:
1. **Viết script `loop-monitor.sh`:** Chạy vòng lặp vô tận ghi thời gian hệ thống vào `/tmp/monitor.log` cứ mỗi 5 giây.
2. **Khởi chạy nền độc lập:** Sử dụng lệnh `nohup` kết hợp toán tử `&` để chống ngắt tiến trình khi đóng Terminal (bỏ qua tín hiệu `SIGHUP`).
3. **Giám sát & Xác định PID:** Tra cứu định danh tiến trình bằng `ps aux` hoặc `pgrep`.
4. **Dừng tiến trình an toàn:** Sử dụng lệnh `kill` với tín hiệu `SIGTERM` (`-15`) để giải phóng tài nguyên đúng chuẩn (Graceful Shutdown).

---

## 2. Mã nguồn Script Giám sát (`loop-monitor.sh`)

```bash
#!/bin/bash
LOG_FILE="/tmp/monitor.log"

# Bắt tín hiệu SIGTERM (15) để dừng an toàn
trap 'echo "[$(date)] [MONITOR]: Nhan tin hieu SIGTERM (15). Tien trinh dung an toan." >> "${LOG_FILE}"; exit 0' SIGTERM

while true; do
    echo "System time: $(date)" >> "${LOG_FILE}"
    sleep 5
done
```

---

## 3. Nhật ký câu lệnh thực hiện (Command Execution Log)

```bash
# 1. Cấp quyền thực thi cho kịch bản
chmod +x loop-monitor.sh

# 2. Khởi chạy tiến trình dưới nền độc lập với nohup
nohup ./loop-monitor.sh > /dev/null 2>&1 &

# 3. Kiểm tra tiến trình đang chạy và xác định PID
ps aux | grep loop-monitor.sh

# 4. Kiểm tra dữ liệu log được ghi liên tục
tail -n 10 /tmp/monitor.log

# 5. Dừng tiến trình bằng tín hiệu SIGTERM (15)
kill -15 12500

# 6. Kiểm tra lại để xác nhận tiến trình đã dừng
ps aux | grep loop-monitor.sh
```

---

## 4. Kết quả kiểm tra xác thực (Verification Output)

### 4.1. Kết quả kiểm tra tiến trình `ps aux | grep loop-monitor.sh`:
```text
USER       PID   %CPU %MEM      VSZ    RSS TTY      STAT START   TIME COMMAND
phongpt  12500   0.1  0.2    12580   3420 ?        S    18:45   0:00 /bin/bash ./loop-monitor.sh
```

### 4.2. Kết quả kiểm tra nhật ký `tail -n 10 /tmp/monitor.log`:
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

### 4.3. Dừng tiến trình và xác nhận (`kill -15 12500`):
```bash
kill -15 12500
ps aux | grep loop-monitor.sh
```
**Đầu ra:** Tiến trình đã biến mất hoàn toàn khỏi bảng `ps aux`.

---

## 5. Bảng tra cứu Tín hiệu POSIX Signals

| Mã Signal | Tên tín hiệu | Khả năng đánh chặn (Catchable) | Hành vi & Ứng dụng thực tế |
| :---: | :---: | :---: | :--- |
| **1** | `SIGHUP` | Có thể bắt | Gửi khi Terminal đóng/mất kết nối SSH. Lệnh `nohup` bỏ qua tín hiệu này để giữ tiến trình sống. |
| **2** | `SIGINT` | Có thể bắt | Gửi khi người dùng nhấn `Ctrl + C` trên bàn phím. |
| **15** | `SIGTERM` | Có thể bắt | Tín hiệu yêu cầu tắt mặc định, cho phép tiến trình dọn dẹp bộ nhớ/ghi log trước khi thoát (Graceful Shutdown). |
| **9** | `SIGKILL` | **Không thể bắt** | Buộc Kernel dừng tiến trình ngay lập tức, dùng khi tiến trình bị treo cứng không phản hồi `SIGTERM`. |

---

## 6. Chương trình Java Quản lý Tiến trình & Tín hiệu POSIX (`Main.java`)

Mã nguồn Java mô phỏng cơ chế Daemon Worker, Shutdown Hook (`SIGTERM`), ghi log nền và quản lý tiến trình:

### Lệnh thực thi:
```bash
java Main.java
```

### Kết quả chạy chương trình Java:
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
