#!/usr/bin/env bash
# ==============================================================================
# Học phần: IT209
# Session: 06 - Bài tập 4: Quản lý tiến trình nền với nohup và tín hiệu Kill
# Sinh viên thực hiện: Phạm Thanh Phong - MSSV: N24DTCN120
# ==============================================================================

set -euo pipefail

SCRIPT_PATH="./loop-monitor.sh"

echo "======================================================================"
echo " [IT209 - Session 06 - BT4] KHỞI CHẠY TIẾN TRÌNH NỀN VỚI NOHUP & &   "
echo "======================================================================"

# 1. Gán quyền thực thi cho kịch bản
echo "[1/3] Cấp quyền thực thi cho ${SCRIPT_PATH}..."
chmod +x "${SCRIPT_PATH}"

# 2. Khởi chạy tiến trình dưới nền độc lập với nohup
echo "[2/3] Khởi chạy tiến trình chạy nền độc lập với nohup..."
nohup "${SCRIPT_PATH}" > /dev/null 2>&1 &
PID=$!

echo "  -> Tiến trình đã khởi chạy thành công dưới nền!"
echo "  -> PID của tiến trình: ${PID}"

# 3. Chờ 6 giây để tiến trình sinh log ban đầu
sleep 6

# 4. Kiểm tra trạng thái
echo "[3/3] KIỂM TRA TRẠNG THÁI TIẾN TRÌNH VÀ NHẬT KÝ:"
echo "----------------------------------------------------------------------"
ps aux | grep "[l]oop-monitor.sh"
echo "----------------------------------------------------------------------"
tail -n 5 /tmp/monitor.log
echo "======================================================================"
