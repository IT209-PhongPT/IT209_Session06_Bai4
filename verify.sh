#!/usr/bin/env bash
# ==============================================================================
# Script kiểm tra, theo dõi log và dừng tiến trình bằng kill -15
# Học phần: IT209 - Session 06 - BT4
# Sinh viên: Phạm Thanh Phong - MSSV: N24DTCN120
# ==============================================================================

echo "=== [1. KIỂM TRA TIẾN TRÌNH ĐANG HOẠT ĐỘNG (ps aux | grep loop-monitor.sh)] ==="
ps aux | grep "[l]oop-monitor.sh" || echo "[INFO] Khong tim thay tien trinh."
echo ""

echo "=== [2. XEM 10 DÒNG NHẬT KÝ MỚI NHẤT (tail -n 10 /tmp/monitor.log)] ==="
if [ -f /tmp/monitor.log ]; then
    tail -n 10 /tmp/monitor.log
else
    echo "[INFO] Tep log chua duoc tao."
fi
echo ""

echo "=== [3. DỪNG TIẾN TRÌNH BẰNG TÍN HIỆU SIGTERM (kill -15)] ==="
PID=$(pgrep -f loop-monitor.sh || true)

if [ -n "${PID}" ]; then
    echo "  -> Tim thay PID: ${PID}. Dang gui tin hieu kill -15 ${PID}..."
    kill -15 "${PID}"
    sleep 1
    echo "  -> Kiem tra lai danh sach tien trinh sau khi kill:"
    ps aux | grep "[l]oop-monitor.sh" || echo "[SUCCESS] Tien trinh da duoc tat hoan toan khoi he thong!"
else
    echo "  [INFO] Khong co tien trinh loop-monitor.sh nao dang chay."
fi
