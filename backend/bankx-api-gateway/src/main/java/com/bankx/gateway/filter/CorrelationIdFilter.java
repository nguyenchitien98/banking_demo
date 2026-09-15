package com.bankx.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Filter toàn cục (Global Filter) sinh và truyền Correlation ID (Header {@code X-Trace-Id}).
 *
 * <p><b>Lý do sử dụng các Annotation:</b>
 * <ul>
 *   <li>{@code @Component}: Đăng ký lớp này thành một Spring Bean quản lý tự động,
 *       cho phép Spring Cloud Gateway phát hiện và đưa filter vào chuỗi xử lý (Filter Chain)
 *       của mọi request đi qua API Gateway.</li>
 * </ul>
 * </p>
 *
 * <p><b>Vai trò nghiệp vụ:</b>
 * Đảm bảo mọi HTTP Request từ phía client (Angular/Mobile) đi qua Gateway đều sở hữu một
 * mã {@code X-Trace-Id} duy nhất. Nếu client không truyền, Filter sẽ tự sinh mã UUID mới
 * và gắn thêm vào Request Header trước khi routing sang các downstream banking microservices.</p>
 *
 * @author BankX Engineering Team
 * @version 1.0
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
    public static final String CORRELATION_ID_HEADER = "X-Trace-Id";

    /**
     * Thực hiện kiểm tra, bổ sung và truyền header {@code X-Trace-Id} qua Filter Chain.
     *
     * @param exchange Đối tượng chứa thông tin ServerHttpRequest và ServerHttpResponse (WebFlux)
     * @param chain Chuỗi filter tiếp theo cần thực thi
     * @return {@link Mono<Void>} biểu thị hoàn thành xử lý reactive
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String traceId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);

        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
            log.debug("Đã sinh Correlation ID mới cho request [URI={}]: {}", request.getURI(), traceId);
        } else {
            log.debug("Sử dụng Correlation ID từ client [URI={}]: {}", request.getURI(), traceId);
        }

        // Mutation HTTP Request gắn thêm Header X-Trace-Id cho downstream service
        ServerHttpRequest modifiedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, traceId)
                .build();

        // Gắn thêm X-Trace-Id vào Response Header trả về cho Client
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, traceId);

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    /**
     * Quy định thứ tự ưu tiên chạy của Filter này trong Spring Cloud Gateway.
     *
     * @return {@code Ordered.HIGHEST_PRECEDENCE} để Filter này chạy đầu tiên trước các filter khác
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
