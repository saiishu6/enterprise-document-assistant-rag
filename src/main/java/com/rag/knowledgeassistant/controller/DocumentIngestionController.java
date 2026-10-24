
package com.rag.knowledgeassistant.controller;

import com.rag.knowledgeassistant.rag.DocumentIngestionService;
import org.apache.tika.exception.TikaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import com.rag.knowledgeassistant.rag.DocumentRetrievalService;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import java.util.Map;

@RestController
@RequestMapping("/documents")
public class DocumentIngestionController {

    private final DocumentIngestionService ingestionService;
    private final DocumentRetrievalService retrievalService;

    public DocumentIngestionController(
            DocumentIngestionService ingestionService, DocumentRetrievalService retrievalService) {
        this.ingestionService = ingestionService;
        this.retrievalService = retrievalService;
    }

    @GetMapping
    public Map<String, Object> getDocuments() {

        RestClient client = RestClient.create();

        return client.post()
                .uri("http://localhost:6333/collections/knowledge_documents/points/scroll")
                .body(Map.of(
                        "limit", 100,
                        "with_payload", true,
                        "with_vector", false
                ))
                .retrieve()
                .body(Map.class);
    }

    @GetMapping("/search")
    public List<Map<String, Object>> searchDocuments(
            @RequestParam String question) {

        return retrievalService.search(question)
                .stream()
                .map(match -> Map.<String, Object>of(
                        "score", match.score(),
                        "text", match.embedded().text()
                ))
                .toList();
    }

    @PostMapping("/ingest")
    public ResponseEntity<String> ingestDocument() {
        ingestionService.ingestDocument();
        return ResponseEntity.ok("Document ingested successfully");
    }


    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(
            @RequestParam("file") MultipartFile file)
            throws IOException, TikaException {
        String filename = file.getOriginalFilename();

        if (filename == null || filename.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Filename is missing.");
        }

        // Check Qdrant before ingesting
        if (ingestionService.documentExists(filename)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Document already exists: " + filename);
        }

        ingestionService.ingestUploadedDocument(file);

        return ResponseEntity.ok(
                "Document uploaded and ingested successfully: "
                        + filename
        );
    }

}
