
package com.rag.knowledgeassistant.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import org.springframework.web.client.RestClient;
import java.util.Map;
import java.util.List;

@Service
public class DocumentIngestionService {

    private final DocumentLoader documentLoader;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public DocumentIngestionService(
            DocumentLoader documentLoader,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore) {

        this.documentLoader = documentLoader;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    public void ingestDocument() {

        Document document = documentLoader.loadDocument();

        List<TextSegment> segments =
                DocumentSplitters.recursive(100, 20)
                        .split(document);

        for (TextSegment segment : segments) {

            var embeddingResponse =
                    embeddingModel.embed(segment);

            embeddingStore.add(
                    embeddingResponse.content(),
                    segment
            );
        }

        System.out.println(
                "Stored " + segments.size()
                        + " chunks in Qdrant"
        );
    }


    public void ingestUploadedDocument(MultipartFile file)
            throws IOException, TikaException {

        // 1. Read the uploaded TXT file
        Document document =
                documentLoader.loadUploadedDocument(file);

        // 2. Split the document into chunks
        List<TextSegment> segments =
                DocumentSplitters.recursive(500, 50)
                        .split(document);

        // 3. Generate embeddings and store them in Qdrant
        for (TextSegment segment : segments) {

            var embedding =
                    embeddingModel.embed(segment).content();

            embeddingStore.add(embedding, segment);
        }

        System.out.println(
                "Uploaded document ingested: "
                        + file.getOriginalFilename()
                        + " | Chunks: " + segments.size()
        );
    }


    public boolean documentExists(String filename) {

        Map<String, Object> request = Map.of(
                "filter", Map.of(
                        "must", List.of(
                                Map.of(
                                        "key", "file_name",
                                        "match", Map.of("value", filename)
                                )
                        )
                ),
                "limit", 1,
                "with_payload", false,
                "with_vector", false
        );

        Map response = RestClient.create()
                .post()
                .uri("http://localhost:6333/collections/knowledge_documents/points/scroll")
                .body(request)
                .retrieve()
                .body(Map.class);

        if (response == null || !(response.get("result") instanceof Map result)) {
            throw new IllegalStateException("Invalid response from Qdrant");
        }

        Object points = result.get("points");

        return points instanceof List<?> list && !list.isEmpty();
    }


}
