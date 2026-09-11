package com.gymmind.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class DocumentParserService {

    public String parseDocument(InputStream inputStream, String fileType) throws IOException {
        return switch (fileType.toUpperCase()) {
            case "PDF" -> parsePdf(inputStream);
            case "TXT" -> parseTxt(inputStream);
            default -> throw new IllegalArgumentException("Unsupported file type: " + fileType);
        };
    }

    private String parsePdf(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // Clean up text
            return cleanText(text);
        }
    }

    private String parseTxt(InputStream inputStream) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return cleanText(content.toString());
    }

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }

        // Remove excessive whitespace
        text = text.replaceAll("\\s+", " ");

        // Remove control characters
        text = text.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");

        // Normalize line breaks
        text = text.replaceAll("\\n{3,}", "\n\n");

        return text.trim();
    }
}
