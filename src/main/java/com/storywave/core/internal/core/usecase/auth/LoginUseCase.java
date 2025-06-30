package com.storywave.core.internal.core.usecase.auth;

import reactor.core.publisher.Mono;

public interface LoginUseCase {

    Mono<String> login();

}
