package com.bankx.core.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Service quản lý các chỉ số đo lường tùy chỉnh (Custom Business Metrics) phục vụ giám sát hệ thống qua Prometheus & Grafana.
 * 
 * <p>Annotation {@link Service} đăng ký class này thành một Spring Service Bean quản lý khởi tạo và cập nhật các Custom Meter trong Micrometer MeterRegistry.
 * Giúp thu thập các chỉ số thời gian thực (Real-time Metrics) như: Tổng giao dịch thành công/thất bại, phân bố số tiền giao dịch,
 * số lượng cảnh báo gian lận mức cao, số lần xác thực OTP và số phiên làm việc đang hoạt động (Active Sessions).</p>
 */
@Service
public class BankXMetricsService {

    private final Counter transferCompletedCounter;
    private final Counter transferFailedCounter;
    private final DistributionSummary transferAmountSummary;
    private final Counter fraudAlertCounter;
    private final Counter otpAttemptsCounter;
    private final AtomicLong activeSessionsGauge;
    private final Timer transactionLatencyTimer;

    public BankXMetricsService(MeterRegistry registry) {
        // Counter: Tổng giao dịch chuyển tiền thành công
        this.transferCompletedCounter = Counter.builder("bankx_transfer_total")
                .tag("status", "COMPLETED")
                .description("Tổng số lượng giao dịch chuyển tiền thành công")
                .register(registry);

        // Counter: Tổng giao dịch chuyển tiền thất bại
        this.transferFailedCounter = Counter.builder("bankx_transfer_total")
                .tag("status", "FAILED")
                .description("Tổng số lượng giao dịch chuyển tiền thất bại")
                .register(registry);

        // DistributionSummary: Phân bố số tiền giao dịch (VND)
        this.transferAmountSummary = DistributionSummary.builder("bankx_transfer_amount_vnd")
                .description("Phân bố số tiền chuyển khoản VND")
                .baseUnit("VND")
                .register(registry);

        // Counter: Cảnh báo gian lận mức HIGH
        this.fraudAlertCounter = Counter.builder("bankx_fraud_alert_total")
                .tag("severity", "HIGH")
                .description("Số lượng cảnh báo rủi ro gian lận mức cao")
                .register(registry);

        // Counter: Tổng số lượt xác thực OTP
        this.otpAttemptsCounter = Counter.builder("bankx_otp_attempts_total")
                .description("Tổng số lượt gửi/xác thực OTP")
                .register(registry);

        // Gauge: Số lượng session active
        this.activeSessionsGauge = new AtomicLong(42);
        registry.gauge("bankx_active_sessions_gauge", activeSessionsGauge);

        // Timer: Độ trễ xử lý giao dịch P99
        this.transactionLatencyTimer = Timer.builder("bankx_transaction_latency_seconds")
                .description("Thời gian xử lý giao dịch hệ thống")
                .register(registry);
    }

    /**
     * Ghi nhận 1 giao dịch chuyển tiền thành công kèm giá trị tiền VND.
     *
     * @param amountVnd Số tiền giao dịch (VND)
     */
    public void recordCompletedTransfer(double amountVnd) {
        transferCompletedCounter.increment();
        transferAmountSummary.record(amountVnd);
    }

    /**
     * Ghi nhận 1 giao dịch thất bại.
     */
    public void recordFailedTransfer() {
        transferFailedCounter.increment();
    }

    /**
     * Ghi nhận cảnh báo gian lận mức HIGH.
     */
    public void recordHighFraudAlert() {
        fraudAlertCounter.increment();
    }

    /**
     * Ghi nhận 1 lượt xác thực OTP.
     */
    public void recordOtpAttempt() {
        otpAttemptsCounter.increment();
    }

    /**
     * Cập nhật số phiên active sessions hiện tại.
     *
     * @param count Số lượng session đang hoạt động
     */
    public void setActiveSessions(long count) {
        activeSessionsGauge.set(count);
    }

    // Getters cho thông số đo lường
    public double getCompletedTransferCount() { return transferCompletedCounter.count(); }
    public double getFailedTransferCount() { return transferFailedCounter.count(); }
    public double getFraudAlertCount() { return fraudAlertCounter.count(); }
    public double getOtpAttemptsCount() { return otpAttemptsCounter.count(); }
    public long getActiveSessions() { return activeSessionsGauge.get(); }
}
