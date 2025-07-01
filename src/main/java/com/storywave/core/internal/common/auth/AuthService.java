package com.storywave.core.internal.common.auth;

import com.storywave.core.internal.data.repository.auth.RedisGuestRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthService {
    
    private final RedisGuestRepository redisGuestRepository;
    
    public AuthService(final RedisGuestRepository redisGuestRepository) {
        this.redisGuestRepository = redisGuestRepository;
    }
    
    public Mono<Boolean> isValidGuestId(final String guestId) {
        if (guestId == null || guestId.isBlank()) {
            return Mono.just(false);
        }
        
        return redisGuestRepository.isGuestExists(guestId);
    }
} 