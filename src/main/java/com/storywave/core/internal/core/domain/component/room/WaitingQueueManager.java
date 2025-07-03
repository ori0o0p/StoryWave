package com.storywave.core.internal.core.domain.component.room;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentHashMap;

@Component 
public final class WaitingQueueManager {

    private static final Logger logger = LoggerFactory.getLogger(WaitingQueueManager.class);

    private final Queue<String> waitingQueue = new ConcurrentLinkedQueue<>();
    private final Set<String> waitingSet = ConcurrentHashMap.newKeySet();
    private final Lock reentrantLock = new ReentrantLock();
    private final GameRoomManager gameRoomManager;
    private final ReactiveRedisOperations<String, Object> redisOperations;
    private final int requiredUsersForMatching;
    private final boolean useRedis;

    private static final String WAITING_QUEUE_KEY = "waiting:queue";

    public WaitingQueueManager(
            final GameRoomManager gameRoomManager,
            final ReactiveRedisOperations<String, Object> redisOperations,
            @Value("${storywave.room.required-users:4}") final int requiredUsersForMatching,
            @Value("${storywave.room.use-redis:true}") final boolean useRedis) {
        this.gameRoomManager = gameRoomManager;
        this.redisOperations = redisOperations;
        this.requiredUsersForMatching = requiredUsersForMatching;
        this.useRedis = useRedis;
    }

    public Mono<Boolean> addUser(final String userId) {
        // simulation-user는 무시
        if ("simulation-user".equals(userId)) {
            return Mono.just(false);
        }
        return Mono.fromCallable(() -> {
            // O(1) 중복 체크
            if (!waitingSet.add(userId)) {
                return false;
            }
            boolean added = waitingQueue.offer(userId);
            if (!added) {
                waitingSet.remove(userId); // offer 실패 시 Set 롤백
                return false;
            }
            if (added && useRedis) {
                redisOperations.opsForList().rightPush(WAITING_QUEUE_KEY, userId)
                        .subscribe(
                                result -> {},
                                error -> logger.error("Redis 대기열 추가 오류: {}", error.getMessage())
                        );
            }
            if (added) {
                matchUsers();
            }
            return true;
        });
    }

    public Mono<Boolean> removeUser(final String userId) {
        return Mono.fromCallable(() -> {
            boolean removed = waitingQueue.remove(userId);
            waitingSet.remove(userId); // Set에서도 항상 제거
            if (removed && useRedis) {
                redisOperations.opsForList().remove(WAITING_QUEUE_KEY, 0, userId)
                        .subscribe(
                                result -> {},
                                error -> logger.error("Redis 대기열 삭제 오류: {}", error.getMessage())
                        );
            }
            return removed;
        });
    }

    public int getWaitingCount() {
        return waitingQueue.size();
    }

    public int getRequiredUsersForMatching() {
        return requiredUsersForMatching;
    }

    private void matchUsers() {
        // 동시성 제어: 대기열 조작에만 Lock 최소화
        if (!reentrantLock.tryLock()) {
            return;
        }
        List<String> matchedUsers = new ArrayList<>();
        try {
            // 필요한 인원이 모이지 않았으면 리턴
            if (waitingQueue.size() < requiredUsersForMatching) {
                return;
            }
            // 필요한 인원만큼 대기열에서 추출
            for (int i = 0; i < requiredUsersForMatching; i++) {
                String userId = waitingQueue.poll();
                if (userId == null) {
                    break;
                }
                // simulation-user는 매칭에서 제외
                if ("simulation-user".equals(userId)) {
                    continue;
                }
                matchedUsers.add(userId);
                waitingSet.remove(userId); // Set에서도 제거
            }
            // 인원이 부족하면 다시 대기열에 추가
            if (matchedUsers.size() < requiredUsersForMatching) {
                waitingQueue.addAll(matchedUsers);
                waitingSet.addAll(matchedUsers);
                return;
            }
        } finally {
            reentrantLock.unlock();
        }
        // 방 생성 및 게임 시작(이 부분은 Lock 밖에서 처리)
        final Set<String> userSet = new HashSet<>(matchedUsers);
        gameRoomManager.createRoom(userSet).subscribe(room -> {
            room.setActive(true);
            room.startGame();
            logger.info("방이 생성되고 게임이 시작되었습니다. ID: {}, 참여자: {}", room.getId(), userSet);
            // Redis에서 대기열 삭제
            if (useRedis) {
                for (String userId : userSet) {
                    redisOperations.opsForList().remove(WAITING_QUEUE_KEY, 0, userId)
                            .subscribe(
                                    result -> {},
                                    error -> logger.error("방 생성 후 Redis 대기열 삭제 오류: {}", error.getMessage())
                            );
                }
            }
        });
    }
}
