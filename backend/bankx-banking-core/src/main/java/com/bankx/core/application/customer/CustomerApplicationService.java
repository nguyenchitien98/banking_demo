package com.bankx.core.application.customer;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;
import com.bankx.common.util.MaskingUtils;
import com.bankx.core.domain.auth.model.User;
import com.bankx.core.domain.auth.repository.UserRepository;
import com.bankx.core.domain.customer.model.Customer;
import com.bankx.core.domain.customer.repository.CustomerRepository;
import com.bankx.core.presentation.customer.dto.CustomerResponse;
import com.bankx.core.presentation.customer.dto.UpdateProfileRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application Service quản lý thông tin hồ sơ và định danh khách hàng (Customer Application Service).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Service}: Đánh dấu Spring Service Bean quản lý các use cases khách hàng.</li>
 *   <li>{@code @Transactional}: Đảm bảo tính nhất quán giao dịch CSDL khi truy vấn hoặc chỉnh sửa hồ sơ.</li>
 * </ul>
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Service
public class CustomerApplicationService {

    private static final Logger log = LoggerFactory.getLogger(CustomerApplicationService.class);

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public CustomerApplicationService(CustomerRepository customerRepository, UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    /**
     * Lấy thông tin hồ sơ cá nhân của khách hàng đăng nhập.
     *
     * @param userId ID người dùng
     * @return {@link CustomerResponse} DTO an toàn
     */
    @Transactional(readOnly = true)
    public CustomerResponse getProfileByUserId(UUID userId) {
        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new BankingException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy hồ sơ khách hàng"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BankingException(ErrorCode.USER_NOT_FOUND));

        return buildResponse(customer, user);
    }

    /**
     * Cập nhật thông tin hồ sơ cá nhân khách hàng.
     *
     * @param userId ID người dùng
     * @param request Payload {@link UpdateProfileRequest}
     * @return {@link CustomerResponse} DTO đã cập nhật
     */
    @Transactional
    public CustomerResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new BankingException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy hồ sơ khách hàng"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BankingException(ErrorCode.USER_NOT_FOUND));

        customer.updateProfile(request.fullName(), request.address(), request.dateOfBirth());
        Customer updated = customerRepository.save(customer);

        log.info("Cập nhật hồ sơ khách hàng thành công cho CIF: [{}]", updated.getCifNumber());
        return buildResponse(updated, user);
    }

    private CustomerResponse buildResponse(Customer customer, User user) {
        return new CustomerResponse(
                customer.getId(),
                customer.getCifNumber(),
                customer.getFullName(),
                MaskingUtils.maskCardNumber(customer.getIdentityNumber()), // Masking identity number
                MaskingUtils.maskPhone(user.getPhone()),
                MaskingUtils.maskEmail(user.getEmail()),
                customer.getDateOfBirth(),
                customer.getAddress(),
                customer.getStatus()
        );
    }
}
