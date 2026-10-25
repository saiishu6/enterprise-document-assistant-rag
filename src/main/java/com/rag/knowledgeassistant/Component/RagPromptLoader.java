package com.rag.knowledgeassistant.Component;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Getter
@Component
public class RagPromptLoader {

    private final String systemPrompt;

    public RagPromptLoader(
            @Value("classpath:prompts/rag-system-prompt.txt")
            Resource resource) throws IOException {

        try (InputStream inputStream = resource.getInputStream()) {
            this.systemPrompt = new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }
    }

}