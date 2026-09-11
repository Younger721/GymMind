package com.gymmind.controller;

import com.gymmind.common.response.ApiResponse;
import com.gymmind.service.SystemTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 系统测试和监控接口
 * 注意：生产环境应该限制访问权限
 */
@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemTestService systemTestService;

    /**
     * 系统健康检查
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> healthCheck() {
        Map<String, Object> health = systemTestService.healthCheck();
        return ApiResponse.success(health);
    }

    /**
     * 数据隔离测试
     */
    @GetMapping("/test/isolation")
    public ApiResponse<Map<String, Object>> testIsolation() {
        Map<String, Object> result = systemTestService.testUserDataIsolation();
        return ApiResponse.success(result);
    }

    /**
     * 系统统计信息
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        Map<String, Object> stats = systemTestService.getSystemStats();
        return ApiResponse.success(stats);
    }
}
