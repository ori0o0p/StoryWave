package com.storywave.core.external.web.rsocket;

import com.storywave.core.internal.core.domain.component.story.StoryManager;
import com.storywave.core.internal.core.domain.model.story.Story;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class StoryLinesController {

    private final StoryManager storyManager;

    public StoryLinesController(final StoryManager storyManager) {
        this.storyManager = storyManager;
    }

    @MessageMapping("story.lines.{storyId}")
    public Mono<Map<String, Object>> getStoryLines(
            @DestinationVariable final String storyId,
            @Payload(required = false) final Map<String, Object> payload) {

        int offset = 0;
        int limit = 20;

        if (payload != null) {
            if (payload.containsKey("offset") && payload.get("offset") instanceof Number) {
                offset = ((Number) payload.get("offset")).intValue();
            }

            if (payload.containsKey("limit") && payload.get("limit") instanceof Number) {
                limit = ((Number) payload.get("limit")).intValue();
                // 너무 많은 데이터를 요청하지 않도록 제한
                if (limit > 50) limit = 50;
            }
        }

        final int finalOffset = offset;
        final int finalLimit = limit;

        return storyManager.getStoryById(storyId)
                .map(story -> {
                    Map<String, Object> response = new HashMap<>();
                    List<Story.StoryLine> allLines = story.getLines();

                    if (allLines != null && !allLines.isEmpty()) {
                        int fromIndex = Math.min(finalOffset, allLines.size());
                        int toIndex = Math.min(fromIndex + finalLimit, allLines.size());

                        List<Map<String, Object>> lines = new ArrayList<>();
                        for (int i = fromIndex; i < toIndex; i++) {
                            Story.StoryLine line = allLines.get(i);
                            Map<String, Object> lineMap = new HashMap<>();
                            lineMap.put("userId", line.getUserId());
                            lineMap.put("content", line.getContent());
                            lineMap.put("round", line.getRound());
                            lines.add(lineMap);
                        }

                        response.put("lines", lines);
                        response.put("totalCount", allLines.size());
                        response.put("hasMore", toIndex < allLines.size());
                    } else {
                        response.put("lines", new ArrayList<>());
                        response.put("totalCount", 0);
                        response.put("hasMore", false);
                    }

                    response.put("storyId", storyId);
                    return response;
                })
                .onErrorResume(e -> {
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("error", "스토리 라인 조회 실패");
                    errorMap.put("message", e.getMessage());
                    return Mono.just(errorMap);
                });
    }
}
