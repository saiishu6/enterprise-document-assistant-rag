
package com.rag.knowledgeassistant.rag;

import com.rag.knowledgeassistant.Component.RagPromptLoader;
import com.rag.knowledgeassistant.ai.Assistant;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Service;
import java.util.List;
import com.rag.knowledgeassistant.dto.ChatResponse;

@Service
public class RagChatService {

    private final DocumentRetrievalService retrievalService;
    private final Assistant assistant;
    private final RagPromptLoader promptLoader;

    public RagChatService(
            DocumentRetrievalService retrievalService,
            Assistant assistant, RagPromptLoader promptLoader) {
        this.retrievalService = retrievalService;
        this.assistant = assistant;
        this.promptLoader = promptLoader;
    }


    public ChatResponse chat(String question) {

        List<EmbeddingMatch<TextSegment>> matches =
                retrievalService.search(question);

        if (matches.isEmpty()) {
            return new ChatResponse(
                    "I couldn't find relevant information in your uploaded knowledge base.",
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
        String systemPrompt = promptLoader.getSystemPrompt();
        String prompt = systemPrompt.formatted(context, question);

        String result = assistant.chat(prompt);

        System.out.println("RAW LLM RESPONSE:\n" + result);

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
        if (answer.toLowerCase(java.util.Locale.ROOT)
                .startsWith("i couldn't find any information about")) {
            return new ChatResponse(answer, List.of());
        }
        List<String> sources = java.util.Arrays.stream(sourceLine.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .map(id -> id.matches("[1-9][0-9]*") ? "SOURCE_" + id : id)
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
