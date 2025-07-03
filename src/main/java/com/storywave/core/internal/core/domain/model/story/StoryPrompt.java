package com.storywave.core.internal.core.domain.model.story;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class StoryPrompt {
    
    private static final List<String> DEFAULT_PROMPTS = Arrays.asList(
            "어느날 학교에서...",
            "비가 오는 오후에...",
            "친구와 함께 여행을 갔는데...",
            "오랜만에 만난 친구가...",
            "집에 돌아왔더니...",
            "어느 날 우연히 발견한 편지에는...",
            "처음으로 혼자 해외여행을 갔다가...",
            "오래된 상자를 열어보니...",
            "길을 걷다가 지갑을 주웠는데...",
            "축제에 갔더니 이상한 일이..."
    );
    
    public static String getRandomPrompt() {
        Random random = new Random();
        return DEFAULT_PROMPTS.get(random.nextInt(DEFAULT_PROMPTS.size()));
    }
    
    public static String getPrompt(int index) {
        if (index < 0 || index >= DEFAULT_PROMPTS.size()) {
            return getRandomPrompt();
        }
        return DEFAULT_PROMPTS.get(index);
    }
    
    public static List<String> getAllPrompts() {
        return DEFAULT_PROMPTS;
    }
} 