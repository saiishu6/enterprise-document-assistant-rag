package com.rag.knowledgeassistant.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.nio.file.Path;
import java.util.Locale;

@Component
public class DocumentLoader {

    public Document loadDocument() {

        Path path = Path.of(
                "src/main/resources/documents/company-handbook.txt"
        );

        Document document = FileSystemDocumentLoader.loadDocument(path);
        List<TextSegment> segments =
                DocumentSplitters.recursive(100, 20)
                        .split(document);

        System.out.println("Number of chunks: " + segments.size());

        for (int i = 0; i < segments.size(); i++) {
            System.out.println("\n--- CHUNK " + (i + 1) + " ---");
            System.out.println(segments.get(i).text());
        }

        //System.out.println(document.text());

        return document;
    }


    public Document loadUploadedDocument(MultipartFile file)
            throws IOException, TikaException {


        if (file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Uploaded file is empty"
            );
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException(
                    "Filename must not be null or empty"
            );
        }

        Metadata metadata = createMetadata(filename);

        Tika tika = new Tika();

        String content = tika.parseToString(file.getInputStream());

        return Document.from(content, metadata);
    }

    private Metadata createMetadata(String filename) {

        int dotIndex = filename.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            throw new IllegalArgumentException(
                    "File must have a valid extension"
            );
        }

        String extension = filename.substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);

        Metadata metadata = new Metadata();
        metadata.put("file_name", filename);
        metadata.put("file_type", extension);

        return metadata;
    }

}