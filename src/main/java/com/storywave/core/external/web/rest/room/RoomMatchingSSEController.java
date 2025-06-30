package com.storywave.core.external.web.rest.room;

import com.storywave.core.internal.common.auth.AuthService;
import com.storywave.core.internal.core.domain.component.room.GameRoomManager;
import com.storywave.core.internal.core.domain.component.room.WaitingQueueManager;
import com.storywave.core.internal.core.domain.event.room.RoomMatchingEvent;
import com.storywave.core.internal.core.domain.model.room.GameRoom;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/room")
class RoomMatchingSSEController {

    private final WaitingQueueManager waitingQueueManager;
    private final GameRoomManager gameRoomManager;
    private final AuthService authService;

    RoomMatchingSSEController(
            final WaitingQueueManager waitingQueueManager,
            final GameRoomManager gameRoomManager,
            final AuthService authService) {
        this.waitingQueueManager = waitingQueueManager;
        this.gameRoomManager = gameRoomManager;
        this.authService = authService;
    }

    @GetMapping(path = "/subscribe/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<ServerSentEvent<Object>> subscribe(@PathVariable final String userId) {
        return authService.isValidGuestId(userId)
                .flatMapMany(isValid -> {
                    if (!isValid) {
                        return Flux.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
                    }

                    // 게임이 진행 중인 방에 있는지 확인 (이미 매칭이 완료되어 게임이 시작된 경우)
                    return gameRoomManager.getRoomByUserId(userId)
                            .filter(room -> room.isGameStarted()) // 게임이 시작된 방만 필터링
                            .flatMapMany(existingRoom -> {
                                // 이미 게임이 진행 중인 방에 있으면 해당 방 이벤트만 전송
                                ServerSentEvent<Object> matchedEvent = createMatchedEvent(existingRoom, userId);

                                Flux<ServerSentEvent<Object>> keepAlive = Flux.interval(Duration.ofSeconds(15))
                                        .map(i -> ServerSentEvent.builder()
                                                .id(String.valueOf(i + 1))
                                                .event("KEEP_ALIVE")
                                                .data("ping")
                                                .build());

                                return Flux.concat(
                                        Flux.just(matchedEvent),
                                        keepAlive
                                );
                            })
                            .switchIfEmpty(
                                    // 게임이 진행 중인 방이 없으면 대기열 처리
                                    handleWaitingQueue(userId)
                            );
                });
    }

    private Flux<ServerSentEvent<Object>> handleWaitingQueue(String userId) {
        return waitingQueueManager.addUser(userId)
                .flatMapMany(added -> {
                    if (!added) {
                        // 이미 대기열에 있는 경우 현재 상태만 반환
                        return createWaitingEventStream(userId);
                    }

                    // 새로 대기열에 추가된 경우
                    return createWaitingEventStream(userId);
                });
    }

    private Flux<ServerSentEvent<Object>> createWaitingEventStream(String userId) {
        // 초기 대기 이벤트
        ServerSentEvent<Object> initialEvent = ServerSentEvent.builder()
                .id("0")
                .event("WAITING")
                .data(new RoomMatchingEvent("WAITING", null, Collections.singleton(userId)))
                .build();

        // 대기열 상태 이벤트 (현재 대기 인원 정보 포함)
        ServerSentEvent<Object> queueStatusEvent = ServerSentEvent.builder()
                .id("queue-status")
                .event("QUEUE_STATUS")
                .data(Map.of(
                        "queueSize", waitingQueueManager.getWaitingCount(),
                        "userId", userId,
                        "status", "WAITING",
                        "requiredUsers", waitingQueueManager.getRequiredUsersForMatching(),
                        "estimatedWaitTime", calculateEstimatedWaitTime(waitingQueueManager.getWaitingCount())
                ))
                .build();

        // Keep-Alive 이벤트
        Flux<ServerSentEvent<Object>> keepAlive = Flux.interval(Duration.ofSeconds(15))
                .map(i -> ServerSentEvent.builder()
                        .id(String.valueOf(i + 1))
                        .event("KEEP_ALIVE")
                        .data("ping")
                        .build());

        // 방 생성 이벤트 (매칭 완료)
        Flux<ServerSentEvent<Object>> roomEvents = gameRoomManager.getRoomEvents()
                .filter(room -> room.getUserIds().contains(userId))
                .map(room -> createMatchedEvent(room, userId));

        return Flux.concat(
                Flux.just(initialEvent),
                Flux.just(queueStatusEvent),
                roomEvents,
                keepAlive
        );
    }

    @PostMapping("/subscribe")
    Mono<Boolean> addToWaitingQueue(@RequestParam final String userId) {
        return authService.isValidGuestId(userId)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
                    }

                    if (userId == null || userId.isBlank()) {
                        return Mono.just(false);
                    }

                    // 게임이 진행 중인 방에 있는지 확인
                    return gameRoomManager.getRoomByUserId(userId)
                            .filter(room -> room.isGameStarted())
                            .map(room -> false) // 이미 게임 중이면 대기열에 추가하지 않음
                            .switchIfEmpty(waitingQueueManager.addUser(userId));
                });
    }

    private ServerSentEvent<Object> createMatchedEvent(final GameRoom room, final String userId) {
        RoomMatchingEvent event = new RoomMatchingEvent("MATCHED", room.getId(), room.getUserIds());

        return ServerSentEvent.builder()
                .id(room.getId())
                .event("MATCHED")
                .data(event)
                .build();
    }

    private ServerSentEvent<Object> createQueueStatusEvent(final String userId, final int position) {
        Map<String, Object> statusData = new HashMap<>();
        statusData.put("userId", userId);
        statusData.put("queuePosition", position);
        statusData.put("queueSize", waitingQueueManager.getWaitingCount());
        statusData.put("requiredUsers", waitingQueueManager.getRequiredUsersForMatching());
        statusData.put("estimatedWaitTime", calculateEstimatedWaitTime(position));

        return ServerSentEvent.builder()
                .id("queue-" + position)
                .event("QUEUE_STATUS")
                .data(statusData)
                .build();
    }

    private int calculateEstimatedWaitTime(final int position) {
        // 대기열 위치에 따른 예상 대기 시간(초) 계산 로직
        // 매칭 필요 인원과 현재 대기열 크기를 고려한 간단한 계산
        int requiredUsers = waitingQueueManager.getRequiredUsersForMatching();
        int waitingCount = waitingQueueManager.getWaitingCount();

        // 대기 위치가 높을수록 대기 시간이 길어짐
        int baseWaitTime = 30; // 기본 30초
        int positionFactor = position / requiredUsers * 20; // 위치별 가중치

        // 대기 인원이 적을수록 대기 시간이 길어짐 (인원이 모이길 기다려야 함)
        int queueFactor = Math.max(0, requiredUsers - waitingCount) * 15;

        return baseWaitTime + positionFactor + queueFactor;
    }
}