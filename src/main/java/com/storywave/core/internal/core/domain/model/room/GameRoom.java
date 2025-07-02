package com.storywave.core.internal.core.domain.model.room;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GameRoom {
    
    private final String id;
    private final Set<String> userIds;
    private final LocalDateTime createdAt;
    private boolean isActive;
    private boolean gameStarted = false;
    private LocalDateTime gameStartedAt;

    // 생성자에서 초기화
    public GameRoom(Set<String> userIds) {
        this.id = UUID.randomUUID().toString();
        this.userIds = new HashSet<>(userIds);
        this.createdAt = LocalDateTime.now();
        this.gameStarted = false; // 초기에는 게임이 시작되지 않음
    }

    // 게임 시작 메서드
    public void startGame() {
        this.gameStarted = true;
        this.gameStartedAt = LocalDateTime.now();
    }

    // 게임 시작 여부 확인
    public boolean isGameStarted() {
        return gameStarted;
    }

    public LocalDateTime getGameStartedAt() {
        return gameStartedAt;
    }
    
    public String getId() {
        return id;
    }
    
    public Set<String> getUserIds() {
        return Collections.unmodifiableSet(userIds);
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(final boolean active) {
        isActive = active;
    }
} 