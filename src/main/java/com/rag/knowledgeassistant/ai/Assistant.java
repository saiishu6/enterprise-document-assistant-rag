package com.rag.knowledgeassistant.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface Assistant {

    @SystemMessage("""
            Focus strictly on the entity or topic the user is asking about.
            
            Use only information that directly answers the user's question.
            Do not include general information about a parent company,
            related organization, or other topic unless it is necessary
            to explain the requested subject.
            
            Do not attribute a parent company's activities, services,
            or policies to its subsidiaries unless the documents
            explicitly establish that relationship.
            
            If only limited information is available about the
            requested subject, give a concise answer using that
            information rather than adding loosely related details.
            """)
    String chat(String userMessage);
}