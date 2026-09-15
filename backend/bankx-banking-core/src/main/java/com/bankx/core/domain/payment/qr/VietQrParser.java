package com.bankx.core.domain.payment.qr;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Component hỗ trợ Giải mã (Parse) chuỗi mã QR theo chuẩn VietQR EMVCo.
 * 
 * <p>Sử dụng annotation {@link Component} để Spring quản lý bean và tiêm (inject) vào
 * các Application Services cần xử lý quét mã QR.</p>
 * 
 * <p>Cấu trúc EMVCo tuân theo định dạng Tag-Length-Value (TLV):</p>
 * <ul>
 *   <li>Tag 00: Payload Format Indicator (01)</li>
 *   <li>Tag 01: Point of Initiation Method (11: Tĩnh, 12: Động)</li>
 *   <li>Tag 38: Thông tin tài khoản nhận VietQR (GUID A000000727 + BIN Ngân hàng + Số TK)</li>
 *   <li>Tag 53: Loại tiền tệ (704 = VND)</li>
 *   <li>Tag 54: Số tiền thanh toán (chỉ có ở QR động)</li>
 *   <li>Tag 58: Mã quốc gia (VN)</li>
 *   <li>Tag 62: Thông tin bổ sung (Sub-tag 08: Nội dung chuyển tiền)</li>
 *   <li>Tag 63: Mã kiểm tra toàn vẹn CRC-16 (4 ký tự Hex)</li>
 * </ul>
 */
@Component
public class VietQrParser {

    /**
     * Phân tích chuỗi mã QR VietQR EMVCo thành đối tượng {@link VietQrPayload}.
     * 
     * @param qrData Chuỗi dữ liệu QR thu thập từ camera hoặc file ảnh
     * @return Đối tượng {@link VietQrPayload} chứa đầy đủ các thuộc tính giao dịch
     */
    public VietQrPayload parse(String qrData) {
        if (qrData == null || qrData.trim().isEmpty()) {
            throw new IllegalArgumentException("Chuỗi dữ liệu mã QR không được để trống");
        }

        String trimmed = qrData.trim();
        Map<String, String> tags = parseTlv(trimmed);

        // Verify CRC-16
        boolean crcValid = verifyCrc(trimmed, tags.get("63"));

        // Extract Initiation Method (11: Static, 12: Dynamic)
        String initMethod = tags.getOrDefault("01", "11");
        boolean isDynamic = "12".equals(initMethod);

        // Extract Tag 38 (Merchant Account Info)
        String tag38Str = tags.get("38");
        String bankBin = "970400"; // Mặc định BankX
        String accountNumber = "";
        String serviceCode = "QRIBFTTA";

        if (tag38Str != null && !tag38Str.isEmpty()) {
            Map<String, String> sub38 = parseTlv(tag38Str);
            String beneficiaryStr = sub38.get("01");
            if (beneficiaryStr != null && !beneficiaryStr.isEmpty()) {
                Map<String, String> subBen = parseTlv(beneficiaryStr);
                bankBin = subBen.getOrDefault("00", "970400");
                accountNumber = subBen.getOrDefault("01", "");
            }
            if (sub38.containsKey("02")) {
                serviceCode = sub38.get("02");
            }
        }

        // Extract Amount (Tag 54)
        BigDecimal amount = null;
        if (tags.containsKey("54")) {
            try {
                amount = new BigDecimal(tags.get("54"));
            } catch (Exception ignored) {
            }
        }

        // Extract Purpose/Description (Tag 62 -> Sub-tag 08)
        String description = "";
        String tag62Str = tags.get("62");
        if (tag62Str != null && !tag62Str.isEmpty()) {
            Map<String, String> sub62 = parseTlv(tag62Str);
            description = sub62.getOrDefault("08", "");
        }

        String bankName = resolveBankName(bankBin);

        return new VietQrPayload(
                bankBin,
                bankName,
                accountNumber,
                "", // Tên chủ tài khoản sẽ được tra cứu từ hệ thống
                amount,
                description,
                isDynamic || amount != null,
                crcValid,
                trimmed
        );
    }

    /**
     * Giải mã chuỗi TLV (Tag-Length-Value) theo độ dài được định nghĩa ở 2 ký tự Length.
     * 
     * @param tlvStream Chuỗi TLV nguyên bản
     * @return Map lưu trữ các cặp [Tag, Value]
     */
    public Map<String, String> parseTlv(String tlvStream) {
        Map<String, String> result = new HashMap<>();
        int index = 0;
        int len = tlvStream.length();

        while (index + 4 <= len) {
            String tag = tlvStream.substring(index, index + 2);
            int length;
            try {
                length = Integer.parseInt(tlvStream.substring(index + 2, index + 4));
            } catch (NumberFormatException e) {
                break;
            }

            index += 4;
            if (index + length > len) {
                break;
            }

            String value = tlvStream.substring(index, index + length);
            result.put(tag, value);
            index += length;
        }

        return result;
    }

    /**
     * Kiểm tra tính đúng đắn của mã checksum CRC-16/CCITT-FALSE.
     * 
     * @param fullQrData Chuỗi QR đầy đủ
     * @param expectedCrc Mã CRC-16 kỳ vọng khai báo ở Tag 63
     * @return True nếu tính toán khớp với CRC kỳ vọng
     */
    public boolean verifyCrc(String fullQrData, String expectedCrc) {
        if (expectedCrc == null || expectedCrc.length() != 4) {
            return false;
        }

        int crcTagIndex = fullQrData.lastIndexOf("6304");
        if (crcTagIndex == -1) {
            return false;
        }

        String dataToCrc = fullQrData.substring(0, crcTagIndex + 4);
        String calculatedCrc = calculateCrc16(dataToCrc);
        return calculatedCrc.equalsIgnoreCase(expectedCrc);
    }

    /**
     * Tính toán checksum CRC-16/CCITT-FALSE (Polynomial 0x1021, Initial Value 0xFFFF).
     * 
     * @param input Chuỗi cần tính CRC
     * @return Chuỗi Hex 4 ký tự in hoa
     */
    public static String calculateCrc16(String input) {
        int crc = 0xFFFF;
        int polynomial = 0x1021;

        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i)) & 1) == 1;
                boolean c15 = ((crc >> 15) & 1) == 1;
                crc <<= 1;
                if (c15 ^ bit) {
                    crc ^= polynomial;
                }
            }
        }
        crc &= 0xFFFF;
        return String.format("%04X", crc);
    }

    /**
     * Tra cứu tên Ngân hàng từ mã BIN (Napas).
     * 
     * @param bankBin Mã BIN ngân hàng (6 chữ số)
     * @return Tên thương hiệu Ngân hàng
     */
    private String resolveBankName(String bankBin) {
        return switch (bankBin) {
            case "970423" -> "TPBank - NHA TRANG (MOCK)";
            case "970400", "970415" -> "BankX Digital Bank";
            case "970436" -> "Vietcombank";
            case "970418" -> "BIDV";
            case "970405" -> "Agribank";
            case "970416" -> "ACB";
            case "970422" -> "MBBank";
            case "970407" -> "Techcombank";
            default -> "Ngân hàng liên kết (" + bankBin + ")";
        };
    }
}
