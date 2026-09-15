package com.bankx.core.application.saga;

import com.bankx.core.application.saga.dto.SagaAuditStepResponse;
import com.bankx.core.application.saga.dto.SagaExecutionRequest;
import com.bankx.core.application.saga.dto.SagaInstanceResponse;
import com.bankx.core.domain.saga.SagaAuditStepJpaEntity;
import com.bankx.core.domain.saga.SagaInstanceJpaEntity;
import com.bankx.core.domain.saga.SpringDataSagaAuditStepRepository;
import com.bankx.core.domain.saga.SpringDataSagaInstanceRepository;
import com.bankx.core.infrastructure.persistence.account.BankAccountJpaEntity;
import com.bankx.core.infrastructure.persistence.account.SpringDataBankAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service Saga Orchestrator quản lý luồng giao dịch chuyển tiền phân tán qua State Machine và Compensating Transactions.
 * 
 * <p>Annotation {@link Service} đánh dấu class này là một Spring Service Bean đảm nhiệm vai trò Saga Orchestrator.
 * Quản lý chuỗi trạng thái:
 * {@code STARTED -> DEBIT_COMPLETED -> CREDIT_COMPLETED -> LEDGER_RECORDED -> COMPLETED}.</p>
 * 
 * <p><b>Cơ chế Bút toán Đảo (Compensating Transactions):</b>
 * Nếu bước Cộng tiền (Credit) hoặc Ghi sổ (Ledger) gặp sự cố, Orchestrator sẽ tự động phát lệnh hoàn tiền
 * {@code REVERSE_DEBIT} để khôi phục số dư tài khoản nguồn về trạng thái ban đầu, đảm bảo tính Nhất quán Đạt Sau (Eventual Consistency).</p>
 */
@Service
public class TransferSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TransferSagaOrchestrator.class);

    private final SpringDataSagaInstanceRepository sagaRepository;
    private final SpringDataSagaAuditStepRepository auditRepository;
    private final SpringDataBankAccountRepository bankAccountRepository;

    public TransferSagaOrchestrator(SpringDataSagaInstanceRepository sagaRepository,
                                    SpringDataSagaAuditStepRepository auditRepository,
                                    SpringDataBankAccountRepository bankAccountRepository) {
        this.sagaRepository = sagaRepository;
        this.auditRepository = auditRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    /**
     * Khởi chạy và điều phối luồng Saga Chuyển tiền phân tán.
     *
     * @param request DTO chứa chi tiết tài khoản nguồn, đích, số tiền và cờ giả lập lỗi
     * @return DTO {@link SagaInstanceResponse} chứa trạng thái cuối cùng và lịch sử các bước
     */
    @Transactional
    public SagaInstanceResponse executeSaga(SagaExecutionRequest request) {
        String sagaId = "SAGA-" + UUID.randomUUID().toString().substring(0, 8);
        String transferCode = "SAGA-TRF-" + System.currentTimeMillis();

        log.info("▶ [SAGA START] Khởi chạy Saga Orchestrator [{}] cho giao dịch [{}]", sagaId, transferCode);

        // 1. Khởi tạo Saga Instance ở trạng thái STARTED
        SagaInstanceJpaEntity instance = SagaInstanceJpaEntity.builder()
                .sagaId(sagaId)
                .transferCode(transferCode)
                .currentState("STARTED")
                .sourceAccountNumber(request.getSourceAccountNumber())
                .targetAccountNumber(request.getTargetAccountNumber())
                .amount(request.getAmount())
                .payloadJson(request.getDescription())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        sagaRepository.save(instance);
        recordStep(sagaId, "INITIATE_SAGA", "NOT_STARTED", "STARTED", false, "Khởi tạo Saga Instance");

        // 2. Bước 1: Trừ tiền tài khoản nguồn (DEBIT)
        try {
            executeDebit(instance);
            transitionState(instance, "DEBIT_COMPLETED");
            recordStep(sagaId, "EXECUTE_DEBIT", "STARTED", "DEBIT_COMPLETED", false, "Trừ tiền tài khoản nguồn " + request.getSourceAccountNumber());
        } catch (Exception ex) {
            transitionState(instance, "FAILED");
            instance.setFailureReason(ex.getMessage());
            sagaRepository.save(instance);
            recordStep(sagaId, "EXECUTE_DEBIT_FAILED", "STARTED", "FAILED", false, ex.getMessage());
            return getSagaDetails(sagaId);
        }

        // 3. Bước 2: Cộng tiền tài khoản đích (CREDIT)
        if ("CREDIT_FAILED".equalsIgnoreCase(request.getForceFailureStep())) {
            log.warn("⚠️ [SAGA SIMULATION] Giả lập lỗi tại bước CREDIT cho Saga [{}]", sagaId);
            recordStep(sagaId, "EXECUTE_CREDIT_FAILED", "DEBIT_COMPLETED", "CREDIT_FAILED", false, "Giả lập lỗi kết nối cổng InterBank Credit");
            
            // Trigger Compensating Transaction: REVERSE_DEBIT
            compensateDebit(instance, "Credit thất bại -> Hoàn tiền về tài khoản nguồn");
            transitionState(instance, "FAILED_COMPENSATED");
            instance.setFailureReason("Lỗi cổng InterBank Credit -> Đã thực hiện Bút toán Đảo Hoàn Tiền");
            sagaRepository.save(instance);
            return getSagaDetails(sagaId);
        }

        executeCredit(instance);
        transitionState(instance, "CREDIT_COMPLETED");
        recordStep(sagaId, "EXECUTE_CREDIT", "DEBIT_COMPLETED", "CREDIT_COMPLETED", false, "Cộng tiền tài khoản đích " + request.getTargetAccountNumber());

        // 4. Bước 3: Hạch toán Bút toán Ghi sổ kép (LEDGER)
        if ("LEDGER_FAILED".equalsIgnoreCase(request.getForceFailureStep())) {
            log.warn("⚠️ [SAGA SIMULATION] Giả lập lỗi tại bước LEDGER cho Saga [{}]", sagaId);
            recordStep(sagaId, "RECORD_LEDGER_FAILED", "CREDIT_COMPLETED", "LEDGER_FAILED", false, "Giả lập lỗi hạch toán Ledger");

            // Trigger Compensation: REVERSE_CREDIT & REVERSE_DEBIT
            compensateCredit(instance, "Ledger thất bại -> Thu hồi tiền tài khoản đích");
            compensateDebit(instance, "Ledger thất bại -> Hoàn tiền tài khoản nguồn");
            transitionState(instance, "FAILED_COMPENSATED");
            instance.setFailureReason("Lỗi hạch toán sổ kép -> Đã hoàn tất Bút toán Đảo 2 chiều");
            sagaRepository.save(instance);
            return getSagaDetails(sagaId);
        }

        // 5. Hoàn tất thành công toàn bộ Saga
        transitionState(instance, "COMPLETED");
        recordStep(sagaId, "RECORD_LEDGER", "CREDIT_COMPLETED", "COMPLETED", false, "Hạch toán bút toán ghi sổ kép thành công");
        log.info("✅ [SAGA COMPLETED] Saga [{}] đã hoàn tất thành công 100%", sagaId);

        return getSagaDetails(sagaId);
    }

    /**
     * Lấy chi tiết thông tin và lịch sử các bước của Saga.
     *
     * @param sagaId Mã Saga ID
     * @return DTO {@link SagaInstanceResponse}
     */
    @Transactional(readOnly = true)
    public SagaInstanceResponse getSagaDetails(String sagaId) {
        SagaInstanceJpaEntity instance = sagaRepository.findById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Saga Instance: " + sagaId));

        List<SagaAuditStepResponse> steps = auditRepository.findBySagaIdOrderByCreatedAtAsc(sagaId).stream()
                .map(s -> SagaAuditStepResponse.builder()
                        .id(s.getId())
                        .sagaId(s.getSagaId())
                        .stepName(s.getStepName())
                        .stateBefore(s.getStateBefore())
                        .stateAfter(s.getStateAfter())
                        .compensating(s.isCompensating())
                        .details(s.getDetails())
                        .createdAt(s.getCreatedAt())
                        .build())
                .toList();

        return SagaInstanceResponse.builder()
                .sagaId(instance.getSagaId())
                .transferCode(instance.getTransferCode())
                .currentState(instance.getCurrentState())
                .sourceAccountNumber(instance.getSourceAccountNumber())
                .targetAccountNumber(instance.getTargetAccountNumber())
                .amount(instance.getAmount())
                .failureReason(instance.getFailureReason())
                .createdAt(instance.getCreatedAt())
                .updatedAt(instance.getUpdatedAt())
                .steps(steps)
                .build();
    }

    /**
     * Lấy danh sách tất cả các Saga Instances.
     *
     * @return Danh sách DTO {@link SagaInstanceResponse}
     */
    @Transactional(readOnly = true)
    public List<SagaInstanceResponse> listSagas() {
        return sagaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(s -> getSagaDetails(s.getSagaId()))
                .toList();
    }

    private void executeDebit(SagaInstanceJpaEntity instance) {
        BankAccountJpaEntity source = bankAccountRepository.findAll().stream()
                .filter(a -> a.getAccountNumber().equalsIgnoreCase(instance.getSourceAccountNumber()))
                .findFirst()
                .orElse(null);

        if (source != null) {
            source.setBalance(source.getBalance().subtract(instance.getAmount()));
            bankAccountRepository.save(source);
        }
    }

    private void executeCredit(SagaInstanceJpaEntity instance) {
        BankAccountJpaEntity target = bankAccountRepository.findAll().stream()
                .filter(a -> a.getAccountNumber().equalsIgnoreCase(instance.getTargetAccountNumber()))
                .findFirst()
                .orElse(null);

        if (target != null) {
            target.setBalance(target.getBalance().add(instance.getAmount()));
            bankAccountRepository.save(target);
        }
    }

    /** Bút toán Đảo Hoàn Tiền (Compensating Transaction: Reverse Debit) */
    private void compensateDebit(SagaInstanceJpaEntity instance, String reason) {
        log.info("🔄 [SAGA COMPENSATION] Bút toán Đảo: Hoàn tiền %,.0f VND về STK nguồn [{}]", instance.getAmount(), instance.getSourceAccountNumber());
        BankAccountJpaEntity source = bankAccountRepository.findAll().stream()
                .filter(a -> a.getAccountNumber().equalsIgnoreCase(instance.getSourceAccountNumber()))
                .findFirst()
                .orElse(null);

        if (source != null) {
            source.setBalance(source.getBalance().add(instance.getAmount()));
            bankAccountRepository.save(source);
        }
        recordStep(instance.getSagaId(), "REVERSE_DEBIT", instance.getCurrentState(), "REVERSED_DEBIT", true, reason);
    }

    /** Bút toán Đảo Thu Hồi (Compensating Transaction: Reverse Credit) */
    private void compensateCredit(SagaInstanceJpaEntity instance, String reason) {
        log.info("🔄 [SAGA COMPENSATION] Bút toán Đảo: Thu hồi %,.0f VND từ STK đích [{}]", instance.getAmount(), instance.getTargetAccountNumber());
        BankAccountJpaEntity target = bankAccountRepository.findAll().stream()
                .filter(a -> a.getAccountNumber().equalsIgnoreCase(instance.getTargetAccountNumber()))
                .findFirst()
                .orElse(null);

        if (target != null) {
            target.setBalance(target.getBalance().subtract(instance.getAmount()));
            bankAccountRepository.save(target);
        }
        recordStep(instance.getSagaId(), "REVERSE_CREDIT", instance.getCurrentState(), "REVERSED_CREDIT", true, reason);
    }

    private void transitionState(SagaInstanceJpaEntity instance, String newState) {
        instance.setCurrentState(newState);
        instance.setUpdatedAt(Instant.now());
        sagaRepository.save(instance);
    }

    private void recordStep(String sagaId, String stepName, String before, String after, boolean isCompensating, String details) {
        SagaAuditStepJpaEntity audit = SagaAuditStepJpaEntity.builder()
                .id("STEP-" + UUID.randomUUID().toString().substring(0, 8))
                .sagaId(sagaId)
                .stepName(stepName)
                .stateBefore(before)
                .stateAfter(after)
                .compensating(isCompensating)
                .details(details)
                .createdAt(Instant.now())
                .build();
        auditRepository.save(audit);
    }
}
