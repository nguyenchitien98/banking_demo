package com.bankx.core.domain.payment.provider;

import com.bankx.common.exception.BankingException;
import com.bankx.common.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory quản lý và tra cứu các chiến lược Thanh toán Hóa đơn (Payment Provider Factory — Strategy Pattern).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Component}: Đăng ký Factory làm Spring Bean điều phối các Strategy.</li>
 *   <li>{@code @PostConstruct}: Tự động quét và xây dựng {@code Map<String, PaymentProvider>} ngay khi Spring IoC Container khởi tạo xong.</li>
 * </ul>
 * </p>
 *
 * <p><b>Tuân thủ Nguyên lý SOLID (Open-Closed Principle - OCP):</b>
 * Khi cần tích hợp Nhà cung cấp mới (ví dụ: MoMo, VNPay), lập trình viên <b>KHÔNG</b> cần sửa code tại Factory này.
 * Chỉ cần tạo 1 Class mới triển khai Interface {@link PaymentProvider} và đính kèm annotation {@code @Component("MOMO")},
 * Spring IoC sẽ tự động đưa Provider mới vào danh sách injection!
 * </p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Component
public class PaymentProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(PaymentProviderFactory.class);

    private final List<PaymentProvider> providerList;
    private final Map<String, PaymentProvider> providerMap = new HashMap<>();

    public PaymentProviderFactory(List<PaymentProvider> providerList) {
        this.providerList = providerList;
    }

    /**
     * Khởi tạo Map tra cứu Provider theo Provider Code.
     */
    @PostConstruct
    public void initProviderMap() {
        for (PaymentProvider provider : providerList) {
            providerMap.put(provider.getProviderCode().toUpperCase(), provider);
            log.info("Sprint 13 Registered Payment Provider Strategy: [{}] category [{}]",
                    provider.getProviderCode(), provider.getCategory());
        }
    }

    /**
     * Lấy chiến lược Thanh toán Hóa đơn theo Mã Nhà cung cấp.
     *
     * @param providerCode Mã nhà cung cấp (ví dụ: "EVN_HN", "WATER_HCM")
     * @return Chiến lược {@link PaymentProvider} tương ứng
     */
    public PaymentProvider getProvider(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            throw new BankingException(ErrorCode.INVALID_REQUEST_PARAMETER, "Mã nhà cung cấp dịch vụ không được để trống!");
        }

        PaymentProvider provider = providerMap.get(providerCode.toUpperCase());
        if (provider == null) {
            throw new BankingException(ErrorCode.RESOURCE_NOT_FOUND, "Hệ thống chưa hỗ trợ nhà cung cấp dịch vụ có mã: " + providerCode);
        }

        return provider;
    }
}
