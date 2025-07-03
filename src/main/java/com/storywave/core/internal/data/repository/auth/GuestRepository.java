package com.storywave.core.internal.data.repository.auth;

import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class GuestRepository {

    private final Map<String, Boolean> guestIds = new ConcurrentHashMap<>();

    public Mono<String> saveGuest(final String guestId) {
        return Mono.fromCallable(() -> {
            guestIds.put(guestId, true);
            return guestId;
        });
    }
    
    public Mono<Boolean> isGuestExists(final String guestId) {
        return Mono.fromCallable(() -> guestIds.containsKey(guestId));
    }
    
    public Flux<String> getAllGuests() {
        return Flux.fromIterable(guestIds.keySet());
    }
    
}
