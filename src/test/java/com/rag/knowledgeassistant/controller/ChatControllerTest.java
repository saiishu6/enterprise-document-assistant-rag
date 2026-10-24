package com.rag.knowledgeassistant.controller;

import com.rag.knowledgeassistant.dto.ChatRequest;
import com.rag.knowledgeassistant.dto.ChatResponse;
import com.rag.knowledgeassistant.rag.RagChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private RagChatService ragChatService;

    @InjectMocks
    private ChatController chatController;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    @Test
    void shouldReturnChatResponseForGetRequest() {

        // Arrange
        String message = "When was REWE Group founded?";

        ChatResponse expectedResponse = new ChatResponse(
                "REWE Group was founded in 1927.",
                List.of("REWE_RAG_Knowledge_Base.pdf")
        );

        when(ragChatService.chat(message))
                .thenReturn(expectedResponse);

        // Act
        ChatResponse actualResponse = chatController.chat(message);

        // Assert
        assertEquals(expectedResponse, actualResponse);

        verify(ragChatService, times(1)).chat(message);
    }

    @Test
    void shouldReturnChatResponseForPostRequest() {

        // Arrange
        ChatRequest request =
                new ChatRequest("When was REWE Group founded?");

        ChatResponse expectedResponse = new ChatResponse(
                "REWE Group was founded in 1927.",
                List.of("REWE_RAG_Knowledge_Base.pdf")
        );

        when(ragChatService.chat(request.message()))
                .thenReturn(expectedResponse);

        // Act
        ChatResponse actualResponse =
                chatController.chatPost(request);

        // Assert
        assertEquals(expectedResponse, actualResponse);

        verify(ragChatService, times(1))
                .chat(request.message());
    }

    @Test
    void shouldCaptureMessageFromPostRequest() {

        // Arrange
        String question = "When was REWE founded?";
        ChatRequest request = new ChatRequest(question);

        ChatResponse mockResponse = new ChatResponse(
                "REWE was founded in 1927.",
                List.of("REWE_RAG_Knowledge_Base.pdf")
        );

        when(ragChatService.chat(anyString()))
                .thenReturn(mockResponse);

        // Act
        ChatResponse actualResponse = chatController.chatPost(request);

        // Assert
        verify(ragChatService).chat(messageCaptor.capture());

        assertEquals(question, messageCaptor.getValue());
        assertEquals(mockResponse, actualResponse);
    }
}