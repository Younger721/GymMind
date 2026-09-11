package com.gymmind.controller;

import com.gymmind.common.response.ApiResponse;
import com.gymmind.dto.rag.ChatRequest;
import com.gymmind.dto.rag.ChatResponse;
import com.gymmind.service.RagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final RagService ragService;

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = ragService.chat(request);
        return ApiResponse.success(response);
    }
}
