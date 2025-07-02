package com.storywave.core.internal.core.domain.component.story;

import com.storywave.core.internal.core.domain.model.room.GameRoom;
import com.storywave.core.internal.core.domain.model.story.Story;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StoryManager {
    
    private static final String STORY_KEY_PREFIX = "story:";
    
    private final Map<String, Story> stories = new ConcurrentHashMap<>();
    
    private final Sinks.Many<Story> storySink = Sinks.many().multicast().onBackpressureBuffer();

    private final Map<String, List<RSocketRequester>> rSocketRequesters = new ConcurrentHashMap<>();
    
    private final ReactiveRedisOperations<String, Object> redisOperations;
    private final boolean useRedis;
    private final int defaultMaxRound;
    
    public StoryManager(
            final ReactiveRedisOperations<String, Object> redisOperations,
            @Value("${storywave.story.use-redis:true}") final boolean useRedis,
            @Value("${storywave.story.default-max-round:3}") final int defaultMaxRound) {
        this.redisOperations = redisOperations;
        this.useRedis = useRedis;
        this.defaultMaxRound = defaultMaxRound;
    }
    
    public Mono<Story> createStory(final GameRoom room) {
        return Mono.fromCallable(() -> {
            String roomId = room.getId();

            
            if (stories.containsKey(roomId)) {
                return stories.get(roomId);
            }
            
            
            String startingPrompt = "어느날 학교에서...";
            
            
            Story story = new Story(roomId, new ArrayList<>(room.getUserIds()), startingPrompt, defaultMaxRound);
            
            
            stories.put(roomId, story);
            
            
            if (useRedis) {
                saveStoryToRedis(story);
            }
            
            return story;
        });
    }
    
    public Mono<Story> getStoryByRoomId(final String roomId) {
        Story story = stories.get(roomId);
        return story != null ? Mono.just(story) : Mono.empty();
    }
    
    public Mono<Story> getStoryById(final String storyId) {
        return getStoryByRoomId(storyId); 
    }
    
    public Mono<Boolean> addStoryLine(final String storyId, final String userId, final String content) {
        return Mono.fromCallable(() -> {
            Story story = stories.get(storyId);

            if (story == null) {
                return false;
            }
            
            boolean success = story.addLine(userId, content);
            
            if (success) {
                
                if (useRedis) {
                    saveStoryToRedis(story);
                }

                List<RSocketRequester> requesters = rSocketRequesters.get(storyId);

                storySink.tryEmitNext(story);

                Flux.fromIterable(requesters)
                        .doOnNext(requester -> {
                            requester.route("story.update")
                                    .data(story)
                                    .send()
                                    .subscribe();
                        })
                        .subscribe();
            }
            
            return success;
        });
    }
    
    public Flux<Story> getStoryEvents() {
        return storySink.asFlux();
    }
    
    private void saveStoryToRedis(final Story story) {
        String key = STORY_KEY_PREFIX + story.getId();
        redisOperations.opsForValue().set(key, story).subscribe();
    }
} 