package com.gymmind.ai.infrastructure.bailian;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

/** 将百炼 HTTP/网络异常转为管理员可读说明（不含密钥）。 */
public final class BailianFailureMessages {

    private BailianFailureMessages() {
    }

    public static String dependencyUnavailableMessage(Throwable failure) {
        if (failure == null) {
            return missingOrUnreachable();
        }
        String mapped = mapKnownFailure(failure);
        if (mapped != null) {
            return mapped;
        }
        Throwable cause = failure.getCause();
        if (cause != null && cause != failure) {
            mapped = mapKnownFailure(cause);
            if (mapped != null) {
                return mapped;
            }
        }
        return missingOrUnreachable();
    }

    private static String mapKnownFailure(Throwable failure) {
        if (failure instanceof HttpClientErrorException.Unauthorized) {
            return invalidApiKey();
        }
        if (failure instanceof HttpClientErrorException httpError) {
            String body = httpError.getResponseBodyAsString();
            if (body != null && body.contains("invalid_api_key")) {
                return invalidApiKey();
            }
            if (httpError.getStatusCode().value() == 401) {
                return invalidApiKey();
            }
        }
        if (failure instanceof ResourceAccessException) {
            return "无法连接阿里百炼服务，请检查本机网络、代理或防火墙（docs/CONFIGURATION.md）";
        }
        String message = failure.getMessage();
        if (message != null) {
            if (message.contains("invalid_api_key") || message.contains("Incorrect API key")) {
                return invalidApiKey();
            }
            if (message.contains("401") && message.contains("Unauthorized")) {
                return invalidApiKey();
            }
        }
        return null;
    }

    private static String invalidApiKey() {
        return "DASHSCOPE_API_KEY 无效或已失效，请在阿里云百炼控制台重新创建 API Key，"
                + "更新仓库根 .env 后重启后端（见 docs/CONFIGURATION.md）";
    }

    private static String missingOrUnreachable() {
        return "未配置或无法访问阿里百炼 AI 服务（请在 IDEA 环境变量或根目录 .env 设置 DASHSCOPE_API_KEY，"
                + "见 docs/CONFIGURATION.md）";
    }
}
