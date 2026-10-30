package com.gymmind.ocr.api;

import com.gymmind.ocr.application.OcrResultView;
import com.gymmind.ocr.application.OcrService;
import com.gymmind.ocr.application.OcrUploadView;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * OCR 接口（M1 同步链路）：
 * <ul>
 *   <li>POST /api/v1/ocr/uploads —— 上传图片，返回 uploadId（先上传后引用）</li>
 *   <li>POST /api/v1/ocr/recognize/{uploadId} —— 同步识别，返回 Markdown 与用量</li>
 * </ul>
 * 权限：租户内持有 {@code ocr:use}（知识库菜单展开项）。
 */
@RestController
@RequestMapping("/api/v1/ocr")
@SecurityRequirement(name = "bearerAuth")
public class OcrController {

    private final OcrService ocr;
    private final CurrentActorProvider actors;

    public OcrController(OcrService ocr, CurrentActorProvider actors) {
        this.ocr = ocr;
        this.actors = actors;
    }

    /** 上传图片：校验魔数/大小 → 落对象存储 → 登记 ocr_job(PENDING) */
    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<OcrUploadView> upload(@RequestPart("file") MultipartFile file) throws IOException {
        return ApiResponse.success(ocr.upload(actors.requireCurrent(),
                file.getOriginalFilename(), file.getContentType(), file.getBytes()));
    }

    /** 同步识别：成功返回 Markdown；已识别过的任务返回缓存结果不重复计费 */
    @PostMapping("/recognize/{uploadId}")
    public ApiResponse<OcrResultView> recognize(@PathVariable Long uploadId) {
        return ApiResponse.success(ocr.recognize(actors.requireCurrent(), uploadId));
    }
}
