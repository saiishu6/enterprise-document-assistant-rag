
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

        if (matches.isEmpty()) {
            return new ChatResponse(
                    "I couldn't find relevant information in the uploaded documents.",
                    List.of()
            );
        }

        // Give every retrieved chunk a source identifier.
        StringBuilder context = new StringBuilder();
        List<String> filenames = new java.util.ArrayList<>();

        for (int i = 0; i < matches.size(); i++) {
            TextSegment segment = matches.get(i).embedded();
            String filename = segment.metadata().getString("file_name");

            if (filename == null) {
                filename = "Unknown";
            }

            filenames.add(filename);

            context.append("[SOURCE_").append(i + 1).append("]\n")
                    .append("Filename: ").append(filename).append("\n")
                    .append("Content:\n").append(segment.text())
                    .append("\n\n---\n\n");
        }

        String prompt = """
            Answer the question using only the provided document context.

            Identify the specific entity or topic being asked about.
            Use only facts directly relevant to the question.
            Do not attribute information about one entity to another.
            Do not use outside knowledge.

            If the context does not support an answer, say so.

            After your answer, output a separate line in this exact format:
            SUPPORTING_SOURCES: SOURCE_1,SOURCE_2

            Include only source IDs containing evidence that directly
            supports your answer. Do not include unrelated sources.
            If no source supports the answer, output:
            SUPPORTING_SOURCES: NONE

            Context:
            %s

            Question:
            %s
            """.formatted(context, question);

        String result = assistant.chat(prompt);

        // Extract the supporting source IDs from the LLM response.
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?m)^SUPPORTING_SOURCES:\\s*(.*)$")
                .matcher(result);

        if (!matcher.find()) {
            // Fail closed: don't claim citations without attribution.
            return new ChatResponse(result, List.of());
        }

        String sourceLine = matcher.group(1).trim();
        String answer = result.substring(0, matcher.start()).trim();

        List<String> sources = java.util.Arrays.stream(sourceLine.split(","))
                .map(String::trim)
                .filter(id -> id.matches("SOURCE_[1-9][0-9]*"))
                .map(id -> Integer.parseInt(id.substring(7)) - 1)
                .filter(index -> index >= 0 && index < filenames.size())
                .map(filenames::get)
                .filter(filename -> !filename.equals("Unknown"))
                .distinct()
                .toList();

        return new ChatResponse(answer, sources);
    }

}
