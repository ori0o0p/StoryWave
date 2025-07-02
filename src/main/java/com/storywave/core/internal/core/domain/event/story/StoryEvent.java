package com.storywave.core.internal.core.domain.event.story;

import java.time.LocalDateTime;
import java.util.List;

import com.storywave.core.internal.core.domain.model.story.Story;

public class StoryEvent {
    
    public enum EventType {
        STORY_CREATED,   
        LINE_ADDED,      
        STORY_COMPLETED  
    }
    
    private final EventType type;         
    private final Story story;            
    private final LocalDateTime eventTime; 
    
    public StoryEvent(EventType type, Story story) {
        this.type = type;
        this.story = story;
        this.eventTime = LocalDateTime.now();
    }


    private StoryEvent(Story story) {
        this(
                story.isCompleted()
                        ? StoryEvent.EventType.STORY_COMPLETED
                        : StoryEvent.EventType.STORY_CREATED,
                story
        );
    }
    
    public EventType getType() {
        return type;
    }
    
    public Story getStory() {
        return story;
    }
    
    public LocalDateTime getEventTime() {
        return eventTime;
    }
    
    public String getCurrentTurnUserId() {
        return story.getCurrentPlayerId();
    }
    
    public boolean isStoryCompleted() {
        return story.isCompleted();
    }
    
    public List<Story.StoryLine> getStoryLines() {
        return story.getLines();
    }
    
    public String getStartingPrompt() {
        return story.getStartingPrompt();
    }
    
    public int getCurrentRound() {
        return story.getCurrentRound();
    }
    
    public String getFullStory() {
        return story.getCompletedStory();
    }
} 