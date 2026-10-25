
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
@RequestMapping("/documents")
public class DocumentIngestionController {

    private final DocumentIngestionService ingestionService;
    private final DocumentRetrievalService retrievalService;
    @Value("${qdrant.host:localhost}")
    private String qdrantHost;

    @Value("${qdrant.rest-port:6333}")
    private int qdrantRestPort;

    public DocumentIngestionController(
            DocumentIngestionService ingestionService, DocumentRetrievalService retrievalService) {
        this.ingestionService = ingestionService;
        this.retrievalService = retrievalService;
    }


    @GetMapping
    public Map<String, List<String>> getDocuments() {

        RestClient client = RestClient.create();

        String url = "http://" + qdrantHost + ":"
                + qdrantRestPort
                + "/collections/knowledge_documents/points/scroll";

        Set<String> filenames = new TreeSet<>();
        Object nextOffset = null;

        do {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("limit", 100);
            requestBody.put("with_payload", List.of("file_name"));
            requestBody.put("with_vector", false);

            if (nextOffset != null) {
                requestBody.put("offset", nextOffset);
            }

            Map<String, Object> response = client.post()
                    .uri(url)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new IllegalStateException(
                        "Empty response from Qdrant");
            }

            Map<String, Object> result =
                    (Map<String, Object>) response.get("result");

            if (result == null) {
                throw new IllegalStateException(
                        "Missing Qdrant result");
            }

            List<Map<String, Object>> points =
                    (List<Map<String, Object>>) result.get("points");

            if (points != null) {
                for (Map<String, Object> point : points) {
                    Map<String, Object> payload =
                            (Map<String, Object>) point.get("payload");

                    if (payload != null
                            && payload.get("file_name") instanceof String filename
                            && !filename.isBlank()) {
                        filenames.add(filename);
                    }
                }
            }

            nextOffset = result.get("next_page_offset");

        } while (nextOffset != null);

        return Map.of("documents", new ArrayList<>(filenames));
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


    @DeleteMapping("/{filename}")
    public ResponseEntity<String> deleteDocument(
            @PathVariable String filename) {

        if (filename == null || filename.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Filename is required.");
        }

        String qdrantUrl = "http://" + qdrantHost + ":"
                + qdrantRestPort
                + "/collections/knowledge_documents/points/delete?wait=true";

        Map<String, Object> requestBody = Map.of(
                "filter", Map.of(
                        "must", List.of(
                                Map.of(
                                        "key", "file_name",
                                        "match", Map.of("value", filename)
                                )
                        )
                )
        );

        RestClient client = RestClient.create();

        client.post()
                .uri(qdrantUrl)
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();

        return ResponseEntity.ok(
                "Document deleted successfully: " + filename
        );
    }

}
