package com.rag.knowledgeassistant.dto;

import java.util.List;

public record ChatResponse(
        String answer,
        List<String> sources
) {
}