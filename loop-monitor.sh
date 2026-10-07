#!/bin/bash
# ==============================================================================
# Kịch bản giám sát thời gian hệ thống chạy nền
# Học phần: IT209 - Session 06 - Bài tập 4
# Sinh viên: Phạm Thanh Phong - MSSV: N24DTCN120
# ==============================================================================

LOG_FILE="/tmp/monitor.log"

# Bắt tín hiệu SIGTERM (15) để dừng an toàn
trap 'echo "[$(date)] [MONITOR]: Nhan tin hieu SIGTERM (15). Tien trinh dung an toan." >> "${LOG_FILE}"; exit 0' SIGTERM

echo "[$(date)] [MONITOR]: Tien trinh bat dau khoi chay voi PID $$" >> "${LOG_FILE}"

while true; do
    echo "System time: $(date)" >> "${LOG_FILE}"
    sleep 5
done
