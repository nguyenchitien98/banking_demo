package com.bankx.core.domain.payment.qr;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Component hỗ trợ Đóng gói (Generate) mã QR theo đúng chuẩn kỹ thuật VietQR EMVCo.
 * 
 * <p>Sử dụng annotation {@link Component} để Spring quản lý bean và hỗ trợ tiêm
 * dependencies khi sinh mã QR chuyển tiền/nhận tiền.</p>
 * 
 * <p>Quy trình đóng gói chuỗi VietQR EMVCo:</p>
 * <ol>
 *   <li>Tạo Tag 00: Payload Format Indicator ("000201")</li>
 *   <li>Tạo Tag 01: Point of Initiation Method ("010211" cho static, "010212" cho dynamic)</li>
 *   <li>Tạo Tag 38: Merchant Account Info (Napas GUID A000000727 + Beneficiary Sub-tags)</li>
 *   <li>Tạo Tag 53: Currency ("5303704" cho VND)</li>
 *   <li>Tạo Tag 54: Amount (Nếu số tiền > 0)</li>
 *   <li>Tạo Tag 58: Country Code ("5802VN")</li>
 *   <li>Tạo Tag 62: Additional Data Field (Sub-tag 08 cho Nội dung thanh toán)</li>
 *   <li>Định dạng Tag 6304: Append "6304" và tính checksum CRC-16/CCITT-FALSE</li>
 * </ol>
 */
@Component
public class VietQrGenerator {

    /**
     * Tạo chuỗi VietQR chuẩn EMVCo dựa trên thông tin ngân hàng và số tiền.
     * 
     * @param bankBin Mã BIN Ngân hàng (6 chữ số)
     * @param accountNumber Số tài khoản nhận tiền
     * @param amount Số tiền chuyển (nếu null hoặc 0 sẽ sinh mã Static QR)
     * @param description Nội dung chuyển tiền
     * @return Chuỗi mã VietQR EMVCo hoàn chỉnh có đính kèm mã CRC-16
     */
    public String generate(String bankBin, String accountNumber, BigDecimal amount, String description) {
        if (bankBin == null || bankBin.trim().isEmpty()) {
            bankBin = "970400"; // Default BankX BIN
        }
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Số tài khoản nhận tiền không được để trống");
        }

        boolean isDynamic = amount != null && amount.compareTo(BigDecimal.ZERO) > 0;

        StringBuilder sb = new StringBuilder();

        // Tag 00: Payload Format Indicator
        sb.append(buildTlv("00", "01"));

        // Tag 01: Point of Initiation Method (11: Static, 12: Dynamic)
        sb.append(buildTlv("01", isDynamic ? "12" : "11"));

        // Tag 38: VietQR Merchant Info
        // Sub-tag 00: GUID A000000727
        String sub00 = buildTlv("00", "A000000727");
        
        // Sub-tag 01: Beneficiary Info (Sub-sub-tag 00: BIN, Sub-sub-tag 01: Account Number)
        String subBen00 = buildTlv("00", bankBin.trim());
        String subBen01 = buildTlv("01", accountNumber.trim());
        String sub01 = buildTlv("01", subBen00 + subBen01);

        // Sub-tag 02: Service Code (QRIBFTTA: Quick Response Instant Bank Fund Transfer to Account)
        String sub02 = buildTlv("02", "QRIBFTTA");

        sb.append(buildTlv("38", sub00 + sub01 + sub02));

        // Tag 53: Transaction Currency (704 = VND)
        sb.append(buildTlv("53", "704"));

        // Tag 54: Transaction Amount
        if (isDynamic) {
            String amountStr = amount.stripTrailingZeros().toPlainString();
            sb.append(buildTlv("54", amountStr));
        }

        // Tag 58: Country Code (VN)
        sb.append(buildTlv("58", "VN"));

        // Tag 62: Additional Data Field (Sub-tag 08: Purpose of Transaction)
        if (description != null && !description.trim().isEmpty()) {
            String sub08 = buildTlv("08", description.trim());
            sb.append(buildTlv("62", sub08));
        }

        // Tag 63: CRC-16 Checksum
        sb.append("6304");

        String dataToCrc = sb.toString();
        String crcHex = VietQrParser.calculateCrc16(dataToCrc);

        return dataToCrc + crcHex;
    }

    /**
     * Helper đóng gói Tag-Length-Value (TLV).
     * 
     * @param tag Tag 2 chữ số
     * @param value Giá trị văn bản
     * @return Chuỗi định dạng TLV
     */
    private String buildTlv(String tag, String value) {
        if (value == null) {
            value = "";
        }
        int len = value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        String lenStr = String.format("%02d", len);
        return tag + lenStr + value;
    }
}
