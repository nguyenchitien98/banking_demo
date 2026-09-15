package com.bankx.core.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Lớp cấu hình OpenAPI 3 / Swagger UI cho hệ thống BankX Digital Banking Core.
 * 
 * <p>Annotation {@link Configuration} đánh dấu class này chứa các Spring Bean định nghĩa tài liệu REST API interactive.
 * Cấu hình tích hợp Bearer Token (JWT) Security Scheme để cho phép thực thi thử nghiệm các API bảo mật trực tiếp trên giao diện Swagger UI.</p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * Khởi tạo và cấu hình Bean OpenAPI tổng hợp toàn bộ tài liệu REST API của hệ thống.
     *
     * @return Đối tượng {@link OpenAPI} cấu hình thông tin dịch vụ, Server URLs và phương thức xác thực JWT Bearer
     */
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "BearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Titan BankX Core API Documentation")
                        .description("Tài liệu tương tác REST API cho Nền tảng Ngân hàng Số Titan BankX (TPBank Simulation). " +
                                "Hỗ trợ xác thực JWT, Idempotency, Double-Entry Ledger, Saga Orchestration & Chaos Engineering.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Titan Engineering Team")
                                .email("engineering@bankx.com")
                                .url("https://bankx.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Core Direct Service (Local Development)"),
                        new Server().url("http://localhost:8080").description("Spring Cloud API Gateway")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Nhập chuỗi Access Token JWT (không cần chữ Bearer tiền tố) để thực thi API bảo mật.")));
    }
}
