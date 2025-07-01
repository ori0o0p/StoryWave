package com.storywave.core.external.web.rsocket;

import org.springframework.context.ApplicationListener;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RSocket 연결 해제를 감지하여 처리하는 컴포넌트
 */
@Component
public class RSocketDisconnectListener {

    private final Map<String, RSocketRequester> requesterMap = new ConcurrentHashMap<>();

    /**
     * 새로운 RSocket 연결 및 해제 리스너 등록
     */
    public void registerRequester(String userId, RSocketRequester requester) {
        requester.rsocket()
                .onClose()
                .doFinally(signal -> {
                    requesterMap.remove(userId);
                    System.out.println("사용자 연결 종료: " + userId);
                })
                .subscribe();

        requesterMap.put(userId, requester);
        System.out.println("사용자 연결 등록됨: " + userId);
    }

    /**
     * 사용자 ID로 등록된 RSocketRequester 조회
     */
    public RSocketRequester getRequester(String userId) {
        return requesterMap.get(userId);
    }

    /**
     * 모든 등록된 사용자 ID 목록 반환
     */
    public List<String> getConnectedUsers() {
        return new ArrayList<>(requesterMap.keySet());
    }

    /**
     * 사용자가 연결되어 있는지 확인
     */
    public boolean isUserConnected(String userId) {
        return requesterMap.containsKey(userId);
    }
}
