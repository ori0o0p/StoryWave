package com.storywave.core.internal.core.domain.event.room;

import java.time.LocalDateTime;
import java.util.Set;

public class RoomMatchingEvent {
    
    private final String eventType;
    private final String roomId;
    private final Set<String> userIds;
    private final LocalDateTime matchedAt;
    
    public RoomMatchingEvent(final String eventType, final String roomId, final Set<String> userIds) {
        this.eventType = eventType;
        this.roomId = roomId;
        this.userIds = userIds;
        this.matchedAt = LocalDateTime.now();
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public String getRoomId() {
        return roomId;
    }
    
    public Set<String> getUserIds() {
        return userIds;
    }
    
    public LocalDateTime getMatchedAt() {
        return matchedAt;
    }
} 