package com.storywave.core.external.web.rsocket;

import com.storywave.core.internal.core.domain.component.room.GameRoomManager;
import com.storywave.core.internal.core.domain.component.story.StoryManager;
import com.storywave.core.internal.core.domain.event.story.StoryEvent;
import com.storywave.core.internal.core.domain.model.story.Story;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class StoryRSocketController {
    
    private static final Logger logger = LoggerFactory.getLogger(StoryRSocketController.class);
    
    private final StoryManager storyManager;
    private final GameRoomManager gameRoomManager;
    private final RSocketDisconnectListener disconnectListener;

    public StoryRSocketController(final StoryManager storyManager, 
                               final GameRoomManager gameRoomManager,
                               final RSocketDisconnectListener disconnectListener) {
        this.storyManager = storyManager;
        this.gameRoomManager = gameRoomManager;
        this.disconnectListener = disconnectListener;
    }
    
    // 기본 연결 핸들러 추가
    @ConnectMapping
    public void defaultConnect(final RSocketRequester requester) {
        requester.rsocket()
                .onClose()
                .doFinally(signal -> {
                    logger.info("기본 연결 종료");
                })
                .subscribe(
                    null,
                    error -> logger.error("기본 RSocket 연결 오류: {}", error.getMessage())
                );

        logger.info("새로운 RSocket 연결 성립");
    }

    @ConnectMapping("user.{userId}")
    public void connect(@DestinationVariable final String userId, final RSocketRequester requester) {
        disconnectListener.registerRequester(userId, requester);
    }

    @MessageMapping("user.{userId}")
    public Mono<Map<String, Object>> registerUser(@DestinationVariable final String userId) {
        logger.info("사용자 등록 요청: {}", userId);
        boolean isRegistered = disconnectListener.isUserConnected(userId);
        if (!isRegistered) {
            logger.info("등록되지 않은 사용자: {}", userId);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("registered", isRegistered);
        response.put("userId", userId);
        return Mono.just(response);
    }
    
    @MessageMapping("story.room.{roomId}")
    public Flux<Map<String, Object>> subscribeStory(@DestinationVariable final String roomId) {
        logger.info("방 구독 요청 받음: {}", roomId);
        return gameRoomManager.getRoomById(roomId)
                .doOnNext(room -> logger.info("방 찾음: {}", room))
                .flatMap(room ->
                        storyManager.getStoryByRoomId(roomId)
                            .switchIfEmpty(Mono.defer(() -> {
                                logger.info("새 스토리 생성: {}", roomId);
                                return storyManager.createStory(room);
                            }))
                )
                .doOnNext(story -> logger.info("스토리 로드됨: {}", story.getId()))
                .map(story -> {
                    StoryEvent.EventType eventType = story.isCompleted() ? StoryEvent.EventType.STORY_COMPLETED : StoryEvent.EventType.STORY_CREATED;
                    StoryEvent initialEvent = new StoryEvent(eventType, story);
                    return storyEventToMap(initialEvent);
                })
                .flatMapMany(initialEvent ->
                        Flux.concat(
                                Flux.just(initialEvent),
                                storyManager.getStoryEvents()
                                        .filter(story -> story.getRoomId().equals(roomId))
                                        .map(story -> {
                                            StoryEvent.EventType eventType = story.isCompleted() ? StoryEvent.EventType.STORY_COMPLETED : StoryEvent.EventType.LINE_ADDED;
                                            StoryEvent event = new StoryEvent(eventType, story);
                                            return storyEventToMap(event);
                                        })
                        )
                )
                .doOnError(e -> logger.error("스토리 구독 오류: {}", e.getMessage()))
                .onErrorResume(e -> {
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("error", "스토리 구독 실패");
                    errorMap.put("message", e.getMessage());
                    return Flux.just(errorMap);
                });
    }

    private Map<String, Object> storyEventToMap(final StoryEvent event) {
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("type", event.getType().toString());
        eventMap.put("story", convertStoryToMap(event.getStory()));
        return eventMap;
    }

    @MessageMapping("story.line.{storyId}")
    public Mono<Map<String, Object>> addStoryLine(
            @DestinationVariable final String storyId,
            @Payload final Map<String, String> payload,
            final RSocketRequester requester
    ) {
        String userId = payload.get("userId");
        String content = payload.get("content");
        Map<String, Object> response = new HashMap<>();

        if (userId == null || content == null) {
            response.put("success", false);
            return Mono.just(response);
        }

        return storyManager.addStoryLine(storyId, userId, content)
                .map(success -> {
                    response.put("success", success);
                    response.put("storyId", storyId);
                    return response;
                });
    }
    
    @MessageMapping("story.info.{storyId}")
    public Mono<Map<String, Object>> getStoryInfo(@DestinationVariable final String storyId) {
        logger.info("스토리 정보 요청: {}", storyId);
        return storyManager.getStoryById(storyId)
                .map(this::convertStoryToMap)
                .doOnError(e -> logger.error("스토리 정보 조회 오류: {}", e.getMessage()))
                .onErrorResume(e -> {
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("error", "스토리 정보 조회 실패");
                    errorMap.put("message", e.getMessage());
                    return Mono.just(errorMap);
                });
    }

    private Map<String, Object> convertStoryToMap(final Story story) {
        Map<String, Object> storyMap = new HashMap<>();
        storyMap.put("id", story.getId());
        storyMap.put("roomId", story.getRoomId());
        storyMap.put("userIds", story.getPlayerIds());
        storyMap.put("lines", story.getLines());
        storyMap.put("completed", story.isCompleted());
        storyMap.put("maxRound", story.getMaxRound());
        storyMap.put("currentPlayerId", story.getCurrentPlayerId());
        storyMap.put("currentRound", story.getCurrentRound());
        storyMap.put("startingPrompt", story.getStartingPrompt());

        return storyMap;
    }
}