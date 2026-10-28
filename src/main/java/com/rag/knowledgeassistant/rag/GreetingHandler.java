package com.rag.knowledgeassistant.rag;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
public class GreetingHandler {

    private static final Set<String> ENGLISH_GREETING = Set.of(
            "hi", "hello", "hey", "good morning",
            "good afternoon", "good evening"
    );

    private static final Set<String> GERMAN_GREETING = Set.of(
            "hallo", "hi", "guten morgen",
            "guten tag", "guten abend", "servus"
    );

    private static final Set<String> ENGLISH_SMALL_TALK = Set.of(
            "how are you", "how are you doing",
            "how are you doing today", "how's it going"
    );

    private static final Set<String> GERMAN_SMALL_TALK = Set.of(
            "wie geht es dir", "wie geht's dir",
            "wie gehts dir", "wie geht es ihnen",
            "wie geht's"
    );

    public Optional<String> handle(String message) {
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }

        String normalized = message.strip()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[.!?]+$", "")
                .strip();

        if (GERMAN_SMALL_TALK.contains(normalized)) {
            return Optional.of(
                    "Mir geht es gut, danke! 😊 Wie kann ich dir " +
                            "mit deinen Dokumenten helfen?"
            );
        }

        if (ENGLISH_SMALL_TALK.contains(normalized)) {
            return Optional.of(
                    "I'm doing well, thank you! 😊 " +
                            "How can I help you with your documents?"
            );
        }

        if (GERMAN_GREETING.contains(normalized)) {
            return Optional.of(
                    "Hallo! 👋 Wie kann ich dir mit deinen Dokumenten helfen?"
            );
        }

        if (ENGLISH_GREETING.contains(normalized)) {
            return Optional.of(
                    "Hello! 👋 How can I help you with your documents?"
            );
        }

        return Optional.empty();
    }
}
