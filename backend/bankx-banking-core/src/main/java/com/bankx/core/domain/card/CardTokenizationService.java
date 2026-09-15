package com.bankx.core.domain.card;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Component xử lý Mã hóa Tokenization Thẻ và Tạo Số Thẻ Ảo (Virtual Card Generator).
 * 
 * <p>Sử dụng annotation {@link Component} để Spring quản lý bean dịch vụ mã hóa.</p>
 * 
 * <p><b>Nguyên tắc Tokenization PCI-DSS:</b>
 * Chuyển đổi Số Thẻ Nguyên Bản (PAN) thành chuỗi Token an toàn và Mã che (Masked PAN).
 * Chuỗi Token không chứa thuật toán giải ngược về số thẻ thật.</p>
 */
@Component
public class CardTokenizationService {

    private final SecureRandom random = new SecureRandom();

    /**
     * Sinh số thẻ ngẫu nhiên 16 chữ số dựa trên thương hiệu Thẻ.
     * 
     * @param brand Thương hiệu thẻ (VISA, MASTERCARD, NAPAS)
     * @return Chuỗi số thẻ 16 chữ số
     */
    public String generateRawPan(CardBrand brand) {
        String bin = switch (brand) {
            case VISA -> "400012";
            case MASTERCARD -> "512345";
            case NAPAS -> "970400";
        };

        StringBuilder sb = new StringBuilder(bin);
        for (int i = 0; i < 10; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Tạo chuỗi Masked PAN để hiển thị an toàn trên giao diện UI (ví dụ: "4000 12** **** 8899").
     * 
     * @param rawPan Số thẻ nguyên bản 16 chữ số
     * @return Chuỗi Masked PAN đã ẩn 6 chữ số giữa
     */
    public String maskPan(String rawPan) {
        if (rawPan == null || rawPan.length() < 16) {
            return "**** **** **** ****";
        }
        String p1 = rawPan.substring(0, 4);
        String p2 = rawPan.substring(4, 6) + "**";
        String p3 = "****";
        String p4 = rawPan.substring(12, 16);

        return String.format("%s %s %s %s", p1, p2, p3, p4);
    }

    /**
     * Sinh token mã hóa duy nhất đại diện cho Thẻ (PAN Token).
     * 
     * @param brand Thương hiệu thẻ
     * @return Token mã hóa định dạng TOK-CARD-{BRAND}-{UUID}
     */
    public String generatePanToken(CardBrand brand) {
        return String.format("TOK-CARD-%s-%s", brand.name(), UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    /**
     * Sinh mã CVV ngẫu nhiên 3 chữ số (chỉ dùng cho hiển thị khởi tạo thẻ ảo).
     * 
     * @return Mã CVV 3 chữ số
     */
    public String generateCvv() {
        return String.format("%03d", random.nextInt(1000));
    }
}
