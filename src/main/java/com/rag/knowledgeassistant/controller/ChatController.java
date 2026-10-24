
package com.rag.knowledgeassistant.controller;

import com.rag.knowledgeassistant.dto.ChatRequest;
import com.rag.knowledgeassistant.dto.ChatResponse;
import com.rag.knowledgeassistant.rag.RagChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final RagChatService ragChatService;

    public ChatController(RagChatService ragChatService) {
        this.ragChatService = ragChatService;
    }

    @GetMapping
    public ChatResponse chat(@RequestParam String message) {
        return ragChatService.chat(message);
    }

    @PostMapping
    public ChatResponse chatPost(@Valid @RequestBody ChatRequest request) {
        return ragChatService.chat(request.message());
    }
}
