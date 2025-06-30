package com.storywave.core.external.web.rest.auth;

import com.storywave.core.internal.core.usecase.auth.LoginUseCase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
class GuestLoginRestController {

    private final LoginUseCase loginUseCase;

    GuestLoginRestController(final LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    @PostMapping("/guest")
    Mono<Map<String, String>> guestLogin() {
        return loginUseCase.login()
                .map(userId -> Map.of(
                        "userId", userId,
                        "status", "success",
                        "message", "게스트 로그인 성공"
                ));
    }

}
