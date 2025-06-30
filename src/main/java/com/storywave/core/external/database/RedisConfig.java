package com.storywave.core.external.database;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import jakarta.annotation.PostConstruct;

@Configuration
class RedisConfig {

    private final ReactiveRedisConnectionFactory factory;
    
    public RedisConfig(ReactiveRedisConnectionFactory factory) {
        this.factory = factory;
    }
    
    @PostConstruct
    public void init() {
        try {
            // Redis 연결 테스트
            factory.getReactiveConnection().ping().block();
            System.out.println("Redis 서버에 성공적으로 연결되었습니다.");
        } catch (Exception e) {
            System.err.println("Redis 서버 연결 실패: " + e.getMessage());
            System.err.println("Redis 기능이 제한될 수 있습니다. 로컬 메모리 모드로 작동합니다.");
        }
    }

    @Bean
    @Primary
    ReactiveRedisOperations<String, Object> reactiveRedisOperations(final ReactiveRedisConnectionFactory factory) {
        final var keySerializer = new StringRedisSerializer();
        final var valueSerializer = new Jackson2JsonRedisSerializer<>(Object.class);

        final var redisSerializationContext = RedisSerializationContext
                .<String, Object>newSerializationContext(keySerializer)
                .value(valueSerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, redisSerializationContext);
    }

}