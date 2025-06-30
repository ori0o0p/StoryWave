package com.storywave.core.config;

import io.rsocket.core.Resume;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.web.util.pattern.PathPatternRouteMatcher;

import java.time.Duration;

@Configuration
public class RSocketConfig {

    @Bean
    public RSocketMessageHandler rsocketMessageHandler(RSocketStrategies strategies) {
        RSocketMessageHandler handler = new RSocketMessageHandler();
        handler.setRSocketStrategies(strategies);
        return handler;
    }

    @Bean
    public RSocketStrategies rSocketStrategies() {
        // Jackson 디코더/인코더에 버퍼 크기 제한 설정
        Jackson2JsonEncoder encoder = new Jackson2JsonEncoder();

        Jackson2JsonDecoder decoder = new Jackson2JsonDecoder();
        decoder.setMaxInMemorySize(1024 * 1024); // 1MB

        return RSocketStrategies.builder()
                .routeMatcher(new PathPatternRouteMatcher())
                .encoders(encoders -> encoders.add(encoder))
                .decoders(decoders -> decoders.add(decoder))
                .build();
    }

//    @Bean
//    public io.rsocket.transport.netty.server.TcpServerTransport tcpServerTransport() {
//        return io.rsocket.transport.netty.server.TcpServerTransport.create(7000);
//    }
//
//    @Bean
//    public io.rsocket.transport.netty.server.CloseableChannel rSocketServer(io.rsocket.transport.netty.server.TcpServerTransport transport) {
//        return io.rsocket.core.RSocketServer.create()
//                .fragment(1024 * 1024) // 1MB 프레임 크기 설정
//                .bind(transport)
//                .block();
//    }

    @Bean
    public Resume resume() {
        return new Resume()
                .sessionDuration(Duration.ofMinutes(5));
    }

    @Bean
    public io.rsocket.core.RSocketConnector rSocketConnector(Resume resume) {
        return io.rsocket.core.RSocketConnector.create()
                .resume(resume)
                .keepAlive(
                    Duration.ofSeconds(30),
                    Duration.ofMinutes(2));
    }
}
