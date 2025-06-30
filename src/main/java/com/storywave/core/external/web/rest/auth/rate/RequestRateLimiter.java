package com.storywave.core.external.web.rest.auth.rate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RequestRateLimiter {

    private final int maxRequestsPerIp;
    private final Duration windowSize;
    
    
    private final Map<String, RequestCount> requestCountMap = new ConcurrentHashMap<>();
    
    private final ReactiveRedisOperations<String, Object> redisOperations;
    private final boolean useRedis;
    
    public RequestRateLimiter(
            final ReactiveRedisOperations<String, Object> redisOperations,
            @Value("${storywave.auth.use-redis:true}") final boolean useRedis,
            @Value("${storywave.auth.rate-limit.max-requests:10}") final int maxRequestsPerIp,
            @Value("${storywave.auth.rate-limit.window-hours:1}") final int windowHours) {
        this.redisOperations = redisOperations;
        this.useRedis = useRedis;
        this.maxRequestsPerIp = maxRequestsPerIp;
        this.windowSize = Duration.ofHours(windowHours);
    }
    
    public Mono<Boolean> isAllowed(final String ipAddress) {
        if (useRedis) {
            return checkRedisRateLimit(ipAddress);
        } else {
            return checkLocalRateLimit(ipAddress);
        }
    }
    
    private Mono<Boolean> checkRedisRateLimit(final String ipAddress) {
        String key = "rate:limit:" + ipAddress;
        
        return redisOperations.opsForValue().increment(key, 1)
            .flatMap(count -> {
                if (count == 1) {
                    
                    return redisOperations.expire(key, windowSize)
                        .thenReturn(true);
                } else {
                    return Mono.just(count <= maxRequestsPerIp);
                }
            });
    }
    
    private Mono<Boolean> checkLocalRateLimit(final String ipAddress) {
        return Mono.fromCallable(() -> {
            RequestCount count = requestCountMap.computeIfAbsent(ipAddress, ip -> new RequestCount(maxRequestsPerIp, windowSize));
            
            
            if (count.isExpired()) {
                count.reset();
            }
            
            
            return count.incrementAndCheck();
        });
    }
    
    
    private static class RequestCount {
        private final AtomicInteger count = new AtomicInteger(0);
        private long expiryTimeMillis;
        private final int maxRequests;
        private final Duration windowSize;
        
        public RequestCount(int maxRequests, Duration windowSize) {
            this.maxRequests = maxRequests;
            this.windowSize = windowSize;
            reset();
        }
        
        public void reset() {
            count.set(0);
            expiryTimeMillis = System.currentTimeMillis() + windowSize.toMillis();
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTimeMillis;
        }
        
        public boolean incrementAndCheck() {
            int currentCount = count.incrementAndGet();
            return currentCount <= maxRequests;
        }
    }
} 