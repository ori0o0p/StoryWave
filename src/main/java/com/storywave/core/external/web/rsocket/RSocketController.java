package com.storywave.core.external.web.rsocket;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Controller
public class RSocketController {

    /**
     * 기본 RSocket 연결 상태 확인용 엔드포인트
     */
    @MessageMapping("ping")
    public Mono<String> ping() {
        return Mono.just("pong");
    }

    /**
     * 사용자 정보 확인용 엔드포인트
     */
    @MessageMapping("register")
    public Mono<Map<String, Object>> register(@Payload Map<String, String> payload) {
        String userId = payload.get("userId");
        System.out.println("등록 요청 받음: " + userId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", userId != null && !userId.isEmpty());
        response.put("userId", userId);
        return Mono.just(response);
    }
}
