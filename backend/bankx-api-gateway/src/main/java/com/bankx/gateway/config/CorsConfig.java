package com.bankx.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Cấu hình chia sẻ tài nguyên giữa các nguồn (CORS - Cross-Origin Resource Sharing) cho API Gateway.
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Configuration}: Đánh dấu đây là lớp cấu hình của Spring Framework, nơi khai báo
 *       các Spring Bean để khởi tạo và nạp vào Spring ApplicationContext trong quá trình bootstrap.</li>
 * </ul>
 * </p>
 *
 * <p><b>Vai trò nghiệp vụ:</b>
 * Cho phép Angular Dev Server (chạy tại {@code http://localhost:4200}) và các ứng dụng frontend được phép
 * thực hiện yêu cầu HTTP (GET, POST, PUT, DELETE, OPTIONS...) tới API Gateway mà không bị trình duyệt chặn CORS policy.</p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Configuration
public class CorsConfig {

    /**
     * Khởi tạo {@link CorsWebFilter} tương thích với Spring WebFlux / Gateway.
     *
     * @return Đối tượng {@link CorsWebFilter} với các quy tắc cho phép CORS
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // Cho phép các nguồn gốc (Origins) kết nối (Angular Web Client)
        corsConfig.setAllowedOrigins(List.of("http://localhost:4200", "http://localhost:8080"));
        
        // Cho phép tất cả các HTTP Methods phổ biến
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        // Cho phép tất cả các HTTP Headers cần thiết (Bao gồm Authorization token & X-Trace-Id)
        corsConfig.setAllowedHeaders(List.of("*"));
        
        // Bật gửi Cookie / Authorization Credentials
        corsConfig.setAllowCredentials(true);
        
        // Expose headers để client đọc được X-Trace-Id từ Response Header
        corsConfig.setExposedHeaders(List.of("X-Trace-Id", "Authorization"));
        
        // Thời gian cache kết quả pre-flight request (1 giờ)
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
