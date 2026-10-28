package com.rag.knowledgeassistant.controller;

import com.rag.knowledgeassistant.dto.ChatRequest;
import com.rag.knowledgeassistant.dto.ChatResponse;
import com.rag.knowledgeassistant.rag.RagChatService;
import com.rag.knowledgeassistant.rag.GreetingHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private RagChatService ragChatService;

    @Mock
    private GreetingHandler greetingHandler;

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

        when(greetingHandler.handle(anyString()))
                .thenReturn(Optional.empty());

        // Act
        ChatResponse actualResponse = chatController.chat(message);

        // Assert
        assertEquals(expectedResponse, actualResponse);

        verify(ragChatService, times(1)).chat(message);
    }
    @Test
    void shouldReturnGreetingWithoutCallingRag() {
        when(greetingHandler.handle("Hello"))
                .thenReturn(Optional.of("Hello!"));

        ChatResponse response = chatController.chat("Hello");

        assertEquals("Hello!", response.answer());
        assertTrue(response.sources().isEmpty());

        verifyNoInteractions(ragChatService);
    }

    @Test
    void shouldCallRagForDocumentQuestion() {
        String message = "What is the refund policy?";
        ChatResponse expected =
                new ChatResponse("Refund within 30 days", List.of("policy.pdf"));

        when(greetingHandler.handle(message))
                .thenReturn(Optional.empty());

        when(ragChatService.chat(message))
                .thenReturn(expected);

        ChatResponse actual = chatController.chat(message);

        assertEquals(expected, actual);
        verify(ragChatService).chat(message);
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