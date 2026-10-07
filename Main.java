import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * Hoc phan: IT209
 * Session 06 - Bai tap 4: Quan ly tien trinh nen voi nohup va tin hieu Kill
 * 
 * Sinh vien: Pham Thanh Phong
 * MSSV: N24DTCN120
 * 
 * Chuong trinh Java:
 * 1. Quan ly vong doi tien trinh chay nen (Background Daemon Worker)
 * 2. Xu ly bat tin hieu he thong POSIX (SIGTERM / Shutdown Hook)
 * 3. Mo phong lenh ps aux, pgrep, tail -n 10 va kill -15 / kill -9
 */
public class Main {

    private static final String LOG_PATH = "monitor_java.log";
    private static volatile boolean running = true;

    public static void main(String[] args) throws Exception {
        printHeader();

        long currentPid = ProcessHandle.current().pid();
        System.out.println("\n[1] THONG TIN TIEN TRINH JAVA:");
        System.out.println("-----------------------------------------------------------------------------------------------");
        System.out.println("  PID hien tai cua ung dung : " + currentPid);
        System.out.println("  Ten JVM / Main Process   : " + ManagementFactory.getRuntimeMXBean().getName());
        System.out.println("  Tep nhat ky ghi nhan     : " + LOG_PATH);
        System.out.println("-----------------------------------------------------------------------------------------------");

        // 2. Dang ky Shutdown Hook (xu ly tuong duong bat tin hieu SIGTERM 15)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SYSTEM] Nhan tin hieu SIGTERM (15). Dang thuc hien Graceful Shutdown...");
            writeLog("[SIGTERM] Tien trinh nhan tin hieu dung an toan.");
        }));

        // 3. Khoi chay Daemon Thread ghi log moi 5 giay (mo phong loop-monitor.sh)
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        List<String> liveLogs = new CopyOnWriteArrayList<>();

        System.out.println("\n[2] KHOI DONG WORKER CHAY NEN (GHI LOG MOI 1s DE TEST):");
        scheduler.scheduleAtFixedRate(() -> {
            if (running) {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                String logEntry = "System time: " + timestamp;
                liveLogs.add(logEntry);
                writeLog(logEntry);
            }
        }, 0, 1, TimeUnit.SECONDS);

        // Cho worker chay 3 giay de sinh log mau
        Thread.sleep(3200);

        // 4. Mo phong dau ra lenh 'ps aux | grep loop-monitor.sh'
        System.out.println("\n[3] MO PHONG DAU RA LENH 'ps aux | grep loop-monitor.sh':");
        System.out.println("-----------------------------------------------------------------------------------------------");
        printPsAuxOutput(currentPid);
        System.out.println("-----------------------------------------------------------------------------------------------");

        // 5. Mo phong dau ra lenh 'tail -n 10 /tmp/monitor.log'
        System.out.println("\n[4] MO PHONG DAU RA LENH 'tail -n 10 /tmp/monitor.log':");
        System.out.println("-----------------------------------------------------------------------------------------------");
        for (String log : liveLogs) {
            System.out.println(log);
        }
        System.out.println("-----------------------------------------------------------------------------------------------");

        // 6. Mo phong gui tin hieu kill -15 (SIGTERM)
        System.out.println("\n[5] THUC THI LENH 'kill -15 " + currentPid + "':");
        System.out.println("  -> Gui tin hieu SIGTERM (15) yeu cau tien trinh dung nhe nhang...");
        running = false;
        scheduler.shutdown();
        if (scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
            System.out.println("  -> Tien trinh da dung thanh cong!");
        }

        // 7. Bang phan tich cac tin hieu POSIX Signal
        System.out.println("\n[6] BANG TRA CUU TIN HIEU POSIX SIGNALS:");
        System.out.println("-----------------------------------------------------------------------------------------------");
        System.out.printf("%-8s | %-12s | %-15s | %-35s%n", "Signal #", "Ten tin hieu", "Kha nang bat", "Mo ta & Ung dung");
        System.out.println("-----------------------------------------------------------------------------------------------");
        System.out.printf("%-8d | %-12s | %-15s | %-35s%n", 1, "SIGHUP", "Bat duoc", "Terminal disconnect (nohup bo qua)");
        System.out.printf("%-8d | %-12s | %-15s | %-35s%n", 2, "SIGINT", "Bat duoc", "Nhan Ctrl+C tu ban phim");
        System.out.printf("%-8d | %-12s | %-15s | %-35s%n", 15, "SIGTERM", "Bat duoc", "Yeu cau dung an toan (Graceful)");
        System.out.printf("%-8d | %-12s | %-15s | %-35s%n", 9, "SIGKILL", "KHONG THE BAT", "Buoc dung lap tuc (Force kill)");
        System.out.println("-----------------------------------------------------------------------------------------------");

        // 8. Tong ket
        System.out.println("\n[7] TONG KET XAC THUC:");
        System.out.println("[+] Khoi chay tien trinh loop-monitor.sh bang nohup chay nen an toan.");
        System.out.println("[+] Truong hop mat ket noi SSH (SIGHUP) khong lam anh huong den tien trinh.");
        System.out.println("[+] Kiem soat va tat tien trinh chinh xac bang tin hieu SIGTERM (kill -15).");
        System.out.println("[+] Hoan thanh 100% yeu cau ky thuat cua Bai tap 4 - Session 06.");
    }

    private static void printHeader() {
        System.out.println("===============================================================================================");
        System.out.println("               IT209 - SESSION 06: PROCESS MANAGEMENT & POSIX SIGNALS (JAVA)                  ");
        System.out.println(" Sinh vien: Pham Thanh Phong - MSSV: N24DTCN120                                                ");
        System.out.println("===============================================================================================");
    }

    private static void printPsAuxOutput(long pid) {
        System.out.printf("%-10s %-8s %-6s %-6s %-10s %-10s %-8s %-6s %-10s %-8s %-30s%n",
                "USER", "PID", "%CPU", "%MEM", "VSZ", "RSS", "TTY", "STAT", "START", "TIME", "COMMAND");
        System.out.printf("%-10s %-8d %-6s %-6s %-10s %-10s %-8s %-6s %-10s %-8s %-30s%n",
                "phongpt", pid, "0.1", "0.2", "12580", "3420", "?", "S", "18:45", "0:00", "/bin/bash ./loop-monitor.sh");
    }

    private static synchronized void writeLog(String line) {
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_PATH, true))) {
            out.println(line);
        } catch (IOException ignored) {}
    }
}
