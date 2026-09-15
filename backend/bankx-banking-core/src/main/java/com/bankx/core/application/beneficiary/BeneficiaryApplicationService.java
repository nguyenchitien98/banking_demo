package com.bankx.core.application.beneficiary;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;
import com.bankx.core.application.beneficiary.dto.AccountLookupResponse;
import com.bankx.core.application.beneficiary.dto.AddBeneficiaryRequest;
import com.bankx.core.application.beneficiary.dto.UpdateBeneficiaryRequest;
import com.bankx.core.domain.beneficiary.BeneficiaryJpaEntity;
import com.bankx.core.domain.beneficiary.SpringDataBeneficiaryRepository;
import com.bankx.core.infrastructure.persistence.account.BankAccountJpaEntity;
import com.bankx.core.infrastructure.persistence.account.SpringDataBankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service tầng Application quản lý Danh bạ Thụ hưởng và Gợi ý chuyển tiền thông minh.
 * 
 * <p>Sử dụng các annotation {@link Service} để Spring quản lý bean dịch vụ, {@link Slf4j}
 * ghi log thao tác danh bạ, {@link RequiredArgsConstructor} tiêm tự động các repositories,
 * và {@link Transactional} duy trì CSDL transaction.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BeneficiaryApplicationService {

    private final SpringDataBeneficiaryRepository beneficiaryRepository;
    private final SpringDataBankAccountRepository bankAccountRepository;

    /**
     * Lấy danh sách người thụ hưởng của khách hàng (Sắp xếp theo tần suất chuyển tiền giảm dần).
     * 
     * @param customerId ID khách hàng
     * @return Danh sách entity thụ hưởng
     */
    @Transactional(readOnly = true)
    public List<BeneficiaryJpaEntity> getBeneficiaries(String customerId) {
        log.info("Lấy danh sách người thụ hưởng cho khách hàng [{}]", customerId);
        return beneficiaryRepository.findByCustomerIdOrderByTransferCountDescLastTransferAtDesc(customerId);
    }

    /**
     * Thêm thủ công một người thụ hưởng vào danh bạ.
     * 
     * @param request DTO thông tin thụ hưởng
     * @return Entity người thụ hưởng vừa lưu
     */
    @Transactional
    public BeneficiaryJpaEntity addBeneficiary(AddBeneficiaryRequest request) {
        String bankBin = (request.bankBin() != null && !request.bankBin().isBlank()) 
                ? request.bankBin() : "970400";
        String bankName = (request.bankName() != null && !request.bankName().isBlank()) 
                ? request.bankName() : ("970423".equals(bankBin) ? "TPBank" : "BankX Digital Bank");

        log.info("Thêm người thụ hưởng [{}] - STK [{}] cho khách hàng [{}]",
                request.accountHolderName(), request.accountNumber(), request.customerId());

        Optional<BeneficiaryJpaEntity> existing = beneficiaryRepository
                .findByCustomerIdAndAccountNumberAndBankBin(request.customerId(), request.accountNumber(), bankBin);

        if (existing.isPresent()) {
            BeneficiaryJpaEntity entity = existing.get();
            entity.setTransferCount(entity.getTransferCount() + 1);
            entity.setLastTransferAt(Instant.now());
            if (request.nickname() != null && !request.nickname().isBlank()) {
                entity.setNickname(request.nickname());
            }
            return beneficiaryRepository.save(entity);
        }

        String benId = "BEN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BeneficiaryJpaEntity entity = BeneficiaryJpaEntity.builder()
                .id(benId)
                .customerId(request.customerId())
                .accountNumber(request.accountNumber())
                .bankBin(bankBin)
                .bankName(bankName)
                .accountHolderName(request.accountHolderName().toUpperCase())
                .nickname(request.nickname())
                .transferCount(1)
                .lastTransferAt(Instant.now())
                .createdAt(Instant.now())
                .build();

        return beneficiaryRepository.save(entity);
    }

    /**
     * Đổi biệt danh (nickname) gợi nhớ cho người thụ hưởng.
     * 
     * @param id ID người thụ hưởng
     * @param request DTO biệt danh mới
     * @return Entity thụ hưởng sau cập nhật
     */
    @Transactional
    public BeneficiaryJpaEntity updateBeneficiary(String id, UpdateBeneficiaryRequest request) {
        log.info("Cập nhật biệt danh cho người thụ hưởng [{}]: Nickname mới [{}]", id, request.nickname());
        BeneficiaryJpaEntity entity = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new BankingException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy người thụ hưởng trong danh bạ"));

        entity.setNickname(request.nickname());
        return beneficiaryRepository.save(entity);
    }

    /**
     * Xóa một người thụ hưởng khỏi danh bạ.
     * 
     * @param id ID người thụ hưởng
     */
    @Transactional
    public void deleteBeneficiary(String id) {
        log.info("Xóa người thụ hưởng [{}] khỏi danh bạ", id);
        if (!beneficiaryRepository.existsById(id)) {
            throw new BankingException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy người thụ hưởng cần xóa");
        }
        beneficiaryRepository.deleteById(id);
    }

    /**
     * Tự động lưu hoặc tăng biến đếm tần suất `transfer_count` sau mỗi giao dịch chuyển tiền thành công.
     * 
     * @param customerId ID khách hàng thực hiện chuyển tiền
     * @param accountNumber Số tài khoản người nhận
     * @param bankBin Mã BIN ngân hàng
     * @param accountHolderName Tên người nhận
     */
    @Transactional
    public void recordSuccessfulTransfer(String customerId, String accountNumber, String bankBin, String accountHolderName) {
        if (customerId == null || accountNumber == null) return;
        String bin = (bankBin != null && !bankBin.isBlank()) ? bankBin : "970400";
        String bankName = "970423".equals(bin) ? "TPBank" : "BankX Digital Bank";

        Optional<BeneficiaryJpaEntity> existing = beneficiaryRepository
                .findByCustomerIdAndAccountNumberAndBankBin(customerId, accountNumber, bin);

        if (existing.isPresent()) {
            BeneficiaryJpaEntity ben = existing.get();
            ben.setTransferCount(ben.getTransferCount() + 1);
            ben.setLastTransferAt(Instant.now());
            beneficiaryRepository.save(ben);
            log.info("Đã tăng transferCount người thụ hưởng [{}] lên {}", accountNumber, ben.getTransferCount());
        } else {
            String benId = "BEN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            BeneficiaryJpaEntity ben = BeneficiaryJpaEntity.builder()
                    .id(benId)
                    .customerId(customerId)
                    .accountNumber(accountNumber)
                    .bankBin(bin)
                    .bankName(bankName)
                    .accountHolderName(accountHolderName != null ? accountHolderName.toUpperCase() : "TÀI KHOẢN THỤ HƯỞNG")
                    .transferCount(1)
                    .lastTransferAt(Instant.now())
                    .createdAt(Instant.now())
                    .build();
            beneficiaryRepository.save(ben);
            log.info("Tự động lưu người thụ hưởng mới [{}] vào danh bạ", accountNumber);
        }
    }

    /**
     * Tra cứu thông tin tên chủ tài khoản nhận tiền (Account Lookup API).
     * 
     * @param accountNumber Số tài khoản nhận
     * @param bankBin Mã BIN ngân hàng
     * @return DTO chứa thông tin tên người nhận
     */
    @Transactional(readOnly = true)
    public AccountLookupResponse lookupAccount(String accountNumber, String bankBin) {
        String bin = (bankBin != null && !bankBin.isBlank()) ? bankBin : "970400";
        log.info("Tra cứu tên chủ tài khoản [{}] ngân hàng BIN [{}]", accountNumber, bin);

        if ("970423".equals(bin)) {
            return new AccountLookupResponse(accountNumber, "NGUYEN CHITIEN (TPBANK MOCK)", bin, "TPBank");
        }

        Optional<BankAccountJpaEntity> accOpt = bankAccountRepository.findByAccountNumber(accountNumber);
        if (accOpt.isPresent()) {
            BankAccountJpaEntity acc = accOpt.get();
            String name = (acc.getAccountName() != null) ? acc.getAccountName().toUpperCase() : "TÀI KHOẢN BANKX " + accountNumber;
            return new AccountLookupResponse(accountNumber, name, bin, "BankX Digital Bank");
        }

        return new AccountLookupResponse(accountNumber, "TÀI KHOẢN THỤ HƯỞNG " + accountNumber, bin, "Ngân hàng liên kết (" + bin + ")");
    }
}
