package com.storywave.core.internal.core.domain.component.room;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Component;

import com.storywave.core.internal.core.domain.model.room.GameRoom;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameRoomManager {
    
    private static final String ROOM_KEY_PREFIX = "room:";
    
    
    private final Map<String, GameRoom> gameRooms = new ConcurrentHashMap<>();
    
    
    private final Map<String, String> userRoomMap = new ConcurrentHashMap<>();
    
    
    private final Sinks.Many<GameRoom> roomSink = Sinks.many().multicast().onBackpressureBuffer();
    
    private final ReactiveRedisOperations<String, Object> redisOperations;
    private final boolean useRedis;
    
    public GameRoomManager(
            final ReactiveRedisOperations<String, Object> redisOperations,
            @Value("${storywave.room.use-redis:true}") final boolean useRedis) {
        this.redisOperations = redisOperations;
        this.useRedis = useRedis;
    }
    
    public Mono<GameRoom> createRoom(final Set<String> userIds) {
        return Mono.fromCallable(() -> {
            
            GameRoom room = new GameRoom(userIds);
            
            
            gameRooms.put(room.getId(), room);
            
            
            for (String userId : userIds) {
                userRoomMap.put(userId, room.getId());
            }
            
            
            if (useRedis) {
                saveRoomToRedis(room);
            }
            
            
            roomSink.tryEmitNext(room);
            
            return room;
        });
    }
    
    public Mono<GameRoom> getRoomByUserId(final String userId) {
        
        String roomId = userRoomMap.get(userId);
        if (roomId == null) {
            return Mono.empty();
        }
        
        
        GameRoom room = gameRooms.get(roomId);
        return room != null ? Mono.just(room) : Mono.empty();
    }
    
    public Mono<GameRoom> getRoomById(final String roomId) {
        GameRoom room = gameRooms.get(roomId);
        return room != null ? Mono.just(room) : Mono.empty();
    }
    
    public Flux<GameRoom> getAllRooms() {
        return Flux.fromIterable(gameRooms.values());
    }
    
    public Flux<GameRoom> getRoomEvents() {
        return roomSink.asFlux();
    }
    
    private void saveRoomToRedis(final GameRoom room) {
        String key = ROOM_KEY_PREFIX + room.getId();
        
        
        Set<String> userIds = room.getUserIds();
        for (String userId : userIds) {
            redisOperations.opsForSet().add(key, userId)
                .subscribe(
                    result -> {},
                    error -> System.err.println("Redis 방 멤버 추가 오류: " + error.getMessage())
                );
            
            
            redisOperations.opsForValue().set("user:" + userId + ":room", room.getId())
                .subscribe(
                    result -> {},
                    error -> System.err.println("Redis 사용자-방 매핑 오류: " + error.getMessage())
                );
        }
    }
}
