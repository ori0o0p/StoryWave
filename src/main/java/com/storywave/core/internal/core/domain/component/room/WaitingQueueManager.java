package com.storywave.core.internal.core.domain.component.room;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component 
public final class WaitingQueueManager {

    private final Queue<String> waitingQueue = new ConcurrentLinkedQueue<>();
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
        return Mono.fromCallable(() -> {

            if (waitingQueue.contains(userId)) {
                return false;
            }


            boolean added = waitingQueue.offer(userId);


            if (added && useRedis) {
                redisOperations.opsForList().rightPush(WAITING_QUEUE_KEY, userId)
                        .subscribe(
                                result -> {
                                },
                                error -> System.err.println("Redis 대기열 추가 오류: " + error.getMessage())
                        );
            }


            if (added) {
                matchUsers();
            }

            return added;
        });
    }

    public Mono<Boolean> removeUser(final String userId) {
        return Mono.fromCallable(() -> {
            boolean removed = waitingQueue.remove(userId);


            if (removed && useRedis) {
                redisOperations.opsForList().remove(WAITING_QUEUE_KEY, 0, userId)
                        .subscribe(
                                result -> {
                                },
                                error -> System.err.println("Redis 대기열 삭제 오류: " + error.getMessage())
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

    // WaitingQueueManager.java의 matchUsers() 메서드를 다음과 같이 수정하세요:

    private void matchUsers() {
        // 동시성 제어
        if (!reentrantLock.tryLock()) {
            return;
        }

        try {
            // 필요한 인원이 모이지 않았으면 리턴
            if (waitingQueue.size() < requiredUsersForMatching) {
                return;
            }

            // 필요한 인원만큼 대기열에서 추출
            List<String> matchedUsers = new ArrayList<>();
            for (int i = 0; i < requiredUsersForMatching; i++) {
                String userId = waitingQueue.poll();
                if (userId == null) {
                    break;
                }
                matchedUsers.add(userId);
            }

            // 인원이 부족하면 다시 대기열에 추가
            if (matchedUsers.size() < requiredUsersForMatching) {
                waitingQueue.addAll(matchedUsers);
                return;
            }

            // 방 생성 및 게임 시작
            final Set<String> userSet = new HashSet<>(matchedUsers);
            gameRoomManager.createRoom(userSet).subscribe(room -> {
                // 방을 활성화하고 게임 시작 상태로 변경
                room.setActive(true);
                room.startGame();

                System.out.println("방이 생성되고 게임이 시작되었습니다. ID: " + room.getId() + ", 참여자: " + userSet);

                // Redis에서 대기열 삭제
                if (useRedis) {
                    for (String userId : userSet) {
                        redisOperations.opsForList().remove(WAITING_QUEUE_KEY, 0, userId)
                                .subscribe(
                                        result -> {},
                                        error -> System.err.println("방 생성 후 Redis 대기열 삭제 오류: " + error.getMessage())
                                );
                    }
                }
            });
        } finally {
            reentrantLock.unlock();
        }
    }
}
