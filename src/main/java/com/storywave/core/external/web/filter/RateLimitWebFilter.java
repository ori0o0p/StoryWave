package com.storywave.core.external.web.filter;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.storywave.core.external.web.rest.auth.rate.RequestRateLimiter;

import reactor.core.publisher.Mono;

@Component
public class RateLimitWebFilter implements WebFilter {

    private final RequestRateLimiter requestRateLimiter;

    public RateLimitWebFilter(final RequestRateLimiter requestRateLimiter) {
        this.requestRateLimiter = requestRateLimiter;
    }

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        if (isGuestLoginRequest(exchange)) {
            String ipAddress = getClientIpAddress(exchange);
            
            return requestRateLimiter.isAllowed(ipAddress)
                    .flatMap(allowed -> {
                        if (allowed) {
                            return chain.filter(exchange);
                        } else {
                            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                            return exchange.getResponse().setComplete();
                        }
                    });
        }
        
        return chain.filter(exchange);
    }

    private boolean isGuestLoginRequest(final ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();
        
        return "/api/auth/guest".equals(path) && "POST".equals(method);
    }

    private String getClientIpAddress(final ServerWebExchange exchange) {
        String ipAddress = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = exchange.getRequest().getHeaders().getFirst("Proxy-Client-IP");
        }
        
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = exchange.getRequest().getHeaders().getFirst("WL-Proxy-Client-IP");
        }
        
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        
        
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        
        return ipAddress;
    }
} 