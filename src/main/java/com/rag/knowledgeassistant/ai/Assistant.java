package com.rag.knowledgeassistant.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface Assistant {

    @SystemMessage("""
        You are a professional AI Knowledge Assistant.

        Answer questions clearly and accurately.
        Use the same language as the user's question.

        Do not invent facts.
        If you do not know an answer, say so.
        """)
    String chat(String userMessage);
}