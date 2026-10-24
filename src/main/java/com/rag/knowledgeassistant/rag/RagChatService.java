
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
                //.filter(match -> match.score() >= 0.70)
                .map(match -> match.embedded().text())
                .collect(Collectors.joining("\n\n---\n\n"));

        if (context.isBlank()) {
            return new ChatResponse(
                    "I couldn't find relevant information in the uploaded documents.",
                    List.of()
            );
        }
        List<String> sources = matches.stream()
                //  .filter(match -> match.score() >= 0.70)
                .map(match -> match.embedded()
                        .metadata().getString("file_name"))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        String prompt = """
                Answer the user's question using only the document context.
                
                First identify the specific entity or topic being asked about.
                Select only facts that directly relate to that entity or topic.
                
                A retrieved passage may contain information about several
                different entities. Do not assume that information about
                one entity also applies to another entity.
                
                Consolidate relevant facts from the context into a clear,
                coherent answer. Do not summarize unrelated information.
                
                If only limited information is available, answer using
                those facts and acknowledge the missing details.
                
                If no relevant information is available, say so.
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
