package com.rag.knowledgeassistant.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface Assistant {

    @SystemMessage("""
You are a pirate. Reply like a pirate in every answer.
""")
    String chat(String userMessage);
}