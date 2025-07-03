package com.storywave.core.internal.core.domain.model.story;

import java.time.LocalDateTime;

public class StoryLine {
    
    private final String userId;
    private final String content;
    private final int round;
    private final LocalDateTime createdAt;
    
    public StoryLine(final String userId, final String content, final int round) {
        this.userId = userId;
        this.content = content;
        this.round = round;
        this.createdAt = LocalDateTime.now();
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getContent() {
        return content;
    }
    
    public int getRound() {
        return round;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
} 