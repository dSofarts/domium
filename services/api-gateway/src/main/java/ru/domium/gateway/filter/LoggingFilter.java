package ru.domium.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().toString();
        String path = request.getURI().getPath();
        String userId = request.getHeaders().getFirst("X-User-Id");
        String targetService = extractTargetService(exchange);
        
        log.info("Gateway request: {} {} | UserId: {} | Target: {}", 
                method, path, userId != null ? userId : "anonymous", targetService);
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            int statusCode = exchange.getResponse().getStatusCode() != null 
                    ? exchange.getResponse().getStatusCode().value() 
                    : 0;
            log.debug("Gateway response: {} {} | Status: {}", method, path, statusCode);
        }));
    }

    private String extractTargetService(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        if (route != null && route.getUri() != null) {
            String uri = route.getUri().toString();
            if (uri.startsWith("lb://")) {
                return uri.substring(5);
            }
            int schemeEnd = uri.indexOf("://");
            if (schemeEnd > 0) {
                return uri.substring(schemeEnd + 3);
            }
            return uri;
        }

        String path = exchange.getRequest().getURI().getPath();
        if (path != null && path.length() > 1) {
            String[] segments = path.split("/");
            if (segments.length > 1 && !segments[1].isEmpty()) {
                return segments[1];
            }
        }
        
        return "unknown";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}

