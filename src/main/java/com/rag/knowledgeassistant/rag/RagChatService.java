
package com.rag.knowledgeassistant.rag;

import com.rag.knowledgeassistant.ai.Assistant;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import com.rag.knowledgeassistant.dto.ChatResponse;

import java.util.Objects;

@Service
public class RagChatService {

    private final DocumentRetrievalService retrievalService;
    private final Assistant assistant;

    public RagChatService(
            DocumentRetrievalService retrievalService,
            Assistant assistant) {
        this.retrievalService = retrievalService;
        this.assistant = assistant;
    }

    public ChatResponse chat(String question) {

        List<EmbeddingMatch<TextSegment>> matches =
                retrievalService.search(question);

        String context = matches.stream()
                .filter(match -> match.score() >= 0.70)
                .map(match -> match.embedded().text())
                .collect(Collectors.joining("\n\n"));

        if (context.isBlank()) {
            return new ChatResponse(
                    "I couldn't find relevant information in the uploaded documents.",
                    List.of()
            );
        }
        List<String> sources = matches.stream()
                .filter(match -> match.score() >= 0.70)
                .map(match -> match.embedded()
                        .metadata().getString("file_name"))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        String prompt = """
                Answer the question using only the context below.
                If the context does not contain the answer,
                say that the information is not available.
                Do not use outside knowledge.
                
                Context:
                %s
                
                Question:
                %s
                """.formatted(context, question);

        String answer = assistant.chat(prompt);

        return new ChatResponse(answer, sources);
    }
}
