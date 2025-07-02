package com.storywave.core.internal.core.domain.model.story;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Story {
    
    private final String id;                  
    private final String roomId;              
    private final List<String> playerIds;     
    private final String startingPrompt;      
    private final List<StoryLine> lines;      
    private final int maxRound;               
    private boolean completed;                
    private int currentPlayerIndex;           
    private int currentRound;                 
    private final LocalDateTime createdAt;    
    private LocalDateTime lastUpdatedAt;      
    
    public Story(String roomId, List<String> playerIds, String startingPrompt, int maxRound) {
        this.id = roomId;  
        this.roomId = roomId;
        this.playerIds = new ArrayList<>(playerIds);
        this.startingPrompt = startingPrompt;
        this.lines = new ArrayList<>();
        this.maxRound = maxRound;
        this.completed = false;
        this.currentPlayerIndex = 0;
        this.currentRound = 1;
        this.createdAt = LocalDateTime.now();
        this.lastUpdatedAt = this.createdAt;
    }
    
    public boolean addLine(String userId, String content) {
        if (completed) {
            return false;
        }
        
        if (!getCurrentPlayerId().equals(userId)) {
            return false;
        }
        
        if (content.length() > 15) {
            content = content.substring(0, 15);
        }
        
        StoryLine line = new StoryLine(userId, content, currentRound);
        lines.add(line);
        
        moveToNextPlayer();
        
        if (currentPlayerIndex == 0) {
            currentRound++;
            
            if (currentRound > maxRound) {
                completed = true;
            }
        }
        
        lastUpdatedAt = LocalDateTime.now();
        return true;
    }
    
    private void moveToNextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % playerIds.size();
    }
    
    public String getCurrentPlayerId() {
        if (playerIds.isEmpty() || currentPlayerIndex >= playerIds.size()) {
            return null;
        }
        return playerIds.get(currentPlayerIndex);
    }
    
    public String getCompletedStory() {
        StringBuilder story = new StringBuilder(startingPrompt);
        
        for (StoryLine line : lines) {
            story.append(" ").append(line.getContent());
        }
        
        return story.toString();
    }
    
    public List<String> getStoryByRounds() {
        List<String> roundStories = new ArrayList<>();
        
        roundStories.add(startingPrompt);
        
        for (int round = 1; round <= maxRound; round++) {
            final int currentRound = round;
            String roundStory = lines.stream()
                    .filter(line -> line.getRound() == currentRound)
                    .map(StoryLine::getContent)
                    .collect(Collectors.joining(" "));
            
            if (!roundStory.isEmpty()) {
                roundStories.add(roundStory);
            }
        }
        
        return roundStories;
    }
    
    public String getId() {
        return id;
    }
    
    public String getRoomId() {
        return roomId;
    }
    
    public List<String> getPlayerIds() {
        return new ArrayList<>(playerIds);
    }
    
    public String getStartingPrompt() {
        return startingPrompt;
    }
    
    public List<StoryLine> getLines() {
        return new ArrayList<>(lines);
    }
    
    public int getMaxRound() {
        return maxRound;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }
    
    public int getCurrentRound() {
        return currentRound;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }
    
    public static class StoryLine {
        private final String userId;
        private final String content;
        private final int round;
        private final LocalDateTime createdAt;
        
        public StoryLine(String userId, String content, int round) {
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
} 