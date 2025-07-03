package com.storywave.core.internal.core.domain.service.auth;

import com.storywave.core.internal.core.usecase.auth.LoginUseCase;
import com.storywave.core.internal.data.repository.auth.GuestRepository;
import com.storywave.core.internal.data.repository.auth.RedisGuestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
class GuestLoginService implements LoginUseCase {

    private final GuestRepository memoryGuestRepository;
    private final RedisGuestRepository redisGuestRepository;
    private final boolean useRedis;

    GuestLoginService(
            final GuestRepository memoryGuestRepository,
            final RedisGuestRepository redisGuestRepository,
            @Value("${storywave.auth.use-redis:true}") final boolean useRedis) {
        this.memoryGuestRepository = memoryGuestRepository;
        this.redisGuestRepository = redisGuestRepository;
        this.useRedis = useRedis;
    }

    public Mono<String> login() {
        String guestId = UUID.randomUUID().toString();
        
        if (useRedis) {
            return redisGuestRepository.saveGuest(guestId);
        } else {
            return memoryGuestRepository.saveGuest(guestId);
        }
    }
}
