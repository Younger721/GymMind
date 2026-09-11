package com.gymmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageEmbeddingService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.openai.api-key}")
    private String apiKey;

    @Value("${ai.openai.base-url}")
    private String baseUrl;

    /**
     * Generate embedding for image using CLIP model
     * Using OpenAI's vision embedding API or compatible endpoint
     */
    public float[] generateImageEmbedding(MultipartFile imageFile) {
        try {
            // Convert image to base64
            byte[] imageBytes = imageFile.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // Call embedding API
            String url = baseUrl + "/embeddings";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "clip-vit-base-patch32"); // or your multimodal model
            requestBody.put("input", "data:image/jpeg;base64," + base64Image);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");

                if (data != null && !data.isEmpty()) {
                    List<Double> embeddingList = (List<Double>) data.get(0).get("embedding");

                    float[] embedding = new float[embeddingList.size()];
                    for (int i = 0; i < embeddingList.size(); i++) {
                        embedding[i] = embeddingList.get(i).floatValue();
                    }

                    log.info("Generated image embedding: dimension={}", embedding.length);
                    return embedding;
                }
            }

            throw new RuntimeException("Failed to generate image embedding");

        } catch (IOException e) {
            log.error("Failed to read image file", e);
            throw new RuntimeException("Failed to read image file", e);
        } catch (Exception e) {
            log.error("Failed to generate image embedding", e);
            throw new RuntimeException("Failed to generate image embedding", e);
        }
    }

    /**
     * Analyze image content using Vision API
     */
    public String analyzeImageContent(MultipartFile imageFile) {
        try {
            byte[] imageBytes = imageFile.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            String url = baseUrl + "/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4-vision-preview");

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");

            List<Map<String, Object>> content = List.of(
                Map.of("type", "text", "text", "请详细描述这张健身图片的内容，包括：动作名称、姿势要点、肌肉群、难度级别。"),
                Map.of("type", "image_url", "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image))
            );
            message.put("content", content);

            requestBody.put("messages", List.of(message));
            requestBody.put("max_tokens", 500);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");

                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> msg = (Map<String, Object>) choice.get("message");
                    String description = (String) msg.get("content");

                    log.info("Image analysis result: {}", description);
                    return description;
                }
            }

            return "无法识别图片内容";

        } catch (IOException e) {
            log.error("Failed to read image file", e);
            throw new RuntimeException("Failed to read image file", e);
        } catch (Exception e) {
            log.error("Failed to analyze image", e);
            return "图片分析失败";
        }
    }

    /**
     * Validate if file is a valid image
     */
    public boolean isValidImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        return contentType != null && (
                contentType.equals("image/jpeg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/webp")
        );
    }
}
