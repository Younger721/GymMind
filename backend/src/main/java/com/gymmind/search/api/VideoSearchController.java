package com.gymmind.search.api;

import com.gymmind.exercise.application.*;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search/videos")
public class VideoSearchController {
    private final VideoSearchService service; private final CurrentActorProvider actors;
    public VideoSearchController(VideoSearchService service, CurrentActorProvider actors) { this.service = service; this.actors = actors; }
    @GetMapping public ApiResponse<java.util.List<ExerciseVideoView>> search(@RequestParam String q) {
        return ApiResponse.success(service.search(actors.requireCurrent(), q));
    }
}
