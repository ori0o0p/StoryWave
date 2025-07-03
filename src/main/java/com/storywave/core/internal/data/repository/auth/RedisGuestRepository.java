package com.storywave.core.internal.data.repository.auth;

import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Repository
@Primary
public class RedisGuestRepository {

    private static final String GUEST_KEY_PREFIX = "guest:";
    private static final Duration GUEST_EXPIRE_TIME = Duration.ofDays(7); 

    private final ReactiveRedisOperations<String, Object> redisOperations;

    public RedisGuestRepository(final ReactiveRedisOperations<String, Object> redisOperations) {
        this.redisOperations = redisOperations;
    }

    public Mono<String> saveGuest(final String guestId) {
        String key = GUEST_KEY_PREFIX + guestId;
        return redisOperations.opsForValue().set(key, guestId, GUEST_EXPIRE_TIME)
                .thenReturn(guestId);
    }

    public Mono<Boolean> isGuestExists(final String guestId) {
        String key = GUEST_KEY_PREFIX + guestId;
        return redisOperations.hasKey(key);
    }

    public Flux<String> getAllGuests() {
        return redisOperations.keys(GUEST_KEY_PREFIX + "*")
                .flatMap(key -> redisOperations.opsForValue().get(key)
                        .cast(String.class));
    }
} 