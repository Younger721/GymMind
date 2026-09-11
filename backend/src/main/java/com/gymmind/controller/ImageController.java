package com.gymmind.controller;

import com.gymmind.common.response.ApiResponse;
import com.gymmind.dto.knowledge.ImageUploadResponse;
import com.gymmind.entity.KnowledgeImage;
import com.gymmind.service.ImageProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/knowledge/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageProcessingService imageProcessingService;

    @PostMapping("/upload")
    public ApiResponse<ImageUploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "documentId", required = false) Long documentId) {

        log.info("Image upload request: filename={}, size={}", file.getOriginalFilename(), file.getSize());

        KnowledgeImage image = imageProcessingService.uploadImage(file, category, documentId);

        ImageUploadResponse response = ImageUploadResponse.builder()
                .imageId(image.getId())
                .imageName(image.getImageName())
                .status(image.getStatus())
                .message("图片上传成功，正在处理中...")
                .build();

        return ApiResponse.success(response);
    }

    @GetMapping
    public ApiResponse<List<KnowledgeImage>> getUserImages() {
        List<KnowledgeImage> images = imageProcessingService.getUserImages();
        return ApiResponse.success(images);
    }

    @GetMapping("/{imageId}")
    public ApiResponse<KnowledgeImage> getImageById(@PathVariable Long imageId) {
        KnowledgeImage image = imageProcessingService.getImageById(imageId);
        return ApiResponse.success(image);
    }

    @DeleteMapping("/{imageId}")
    public ApiResponse<Void> deleteImage(@PathVariable Long imageId) {
        imageProcessingService.deleteImage(imageId);
        return ApiResponse.success(null);
    }
}
