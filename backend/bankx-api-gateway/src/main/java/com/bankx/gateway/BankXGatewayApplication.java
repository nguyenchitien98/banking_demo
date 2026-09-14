package com.bankx.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Điểm khởi động của BankX API Gateway.
 *
 * <p>Gateway là cổng vào duy nhất của toàn bộ hệ thống BankX, chịu trách nhiệm:
 * <ul>
 *   <li>Rate Limiting: Giới hạn số request theo IP và userId (Token Bucket via Redis)</li>
 *   <li>JWT Validation: Xác thực Access Token trước khi forward đến Banking Core</li>
 *   <li>Routing: Điều hướng request đến đúng service dựa trên path</li>
 *   <li>CORS: Cho phép Angular (localhost:4200) gọi API</li>
 *   <li>Correlation ID: Gán X-Trace-Id cho mỗi request để theo dõi distributed trace</li>
 * </ul>
 *
 * <p><b>Tại sao @SpringBootApplication?</b>
 * Annotation tổng hợp gồm @Configuration + @EnableAutoConfiguration + @ComponentScan.
 * Tự động cấu hình Spring Cloud Gateway, Redis, Actuator dựa trên dependencies trong classpath.
 *
 * <p><b>Tại sao không dùng @EnableWebMvc?</b>
 * Spring Cloud Gateway dựa trên WebFlux (reactive), không phải Spring MVC (servlet).
 * Không được add Spring MVC vào Gateway — sẽ xung đột.
 *
 * @since Sprint 00
 */
@SpringBootApplication
public class BankXGatewayApplication {

    /**
     * Entry point của BankX API Gateway.
     *
     * @param args tham số dòng lệnh (ví dụ: --spring.profiles.active=local)
     */
    public static void main(String[] args) {
        SpringApplication.run(BankXGatewayApplication.class, args);
    }
}
