
package com.rag.knowledgeassistant.controller;

import com.rag.knowledgeassistant.dto.ChatRequest;
import com.rag.knowledgeassistant.dto.ChatResponse;
import com.rag.knowledgeassistant.rag.GreetingHandler;
import com.rag.knowledgeassistant.rag.RagChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final RagChatService ragChatService;
    private final GreetingHandler greetingHandler;

    public ChatController(RagChatService ragChatService, GreetingHandler greetingHandler) {
        this.ragChatService = ragChatService;
        this.greetingHandler = greetingHandler;
    }

    @GetMapping
    public ChatResponse chat(String message) {
        Optional<String> greeting = greetingHandler.handle(message);
        return greeting.map(s -> new ChatResponse(s, List.of())).orElseGet(() -> ragChatService.chat(message));
    }

    @PostMapping
    public ChatResponse chatPost(@Valid @RequestBody ChatRequest request) {
        return ragChatService.chat(request.message());
    }
}
