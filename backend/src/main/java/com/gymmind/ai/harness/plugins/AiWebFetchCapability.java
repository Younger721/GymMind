package com.gymmind.ai.harness.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gymmind.ai.harness.capability.AiCapability;
import com.gymmind.ai.harness.capability.CapabilityDescriptor;
import com.gymmind.ai.harness.capability.ToolExecutionContext;
import com.gymmind.ai.harness.capability.ToolResult;
import com.gymmind.ai.infrastructure.websearch.WebSearchProperties;
import com.gymmind.shared.security.CurrentActor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.URI;
import java.util.Set;

/** 读取网页正文 function call 工具：用户消息包含 URL 时，模型可抓取该网页内容辅助回答 */
@Component
public class AiWebFetchCapability implements AiCapability {

    /** 工具注册 ID */
    public static final String ID = "web_fetch";

    /** 重定向最大跳数，防止重定向循环并保证每跳都过内网校验 */
    private static final int MAX_REDIRECTS = 3;

    private static final CapabilityDescriptor DESCRIPTOR = new CapabilityDescriptor(
            ID,
            "网页读取",
            "读取指定 URL 网页的正文内容。当用户消息中出现链接（http/https）或要求查看某个网页时优先使用。"
                    + "参数 url 为完整网页地址。",
            "READ",
            "",
            false,
            Set.of(),
            schema());

    /** 页面抓取函数抽象，单元测试可替换为固定返回，避免真实网络请求 */
    @FunctionalInterface
    interface PageFetcher {
        String fetch(String url, int timeoutMs, long maxBytes) throws Exception;
    }

    private static final PageFetcher DEFAULT_FETCHER = AiWebFetchCapability::fetchPage;

    private final PageFetcher fetcher;
    private final WebSearchProperties properties;

    public AiWebFetchCapability(WebSearchProperties properties) {
        this(DEFAULT_FETCHER, properties);
    }

    /** 测试用构造：注入抓取函数与配置 */
    AiWebFetchCapability(PageFetcher fetcher, WebSearchProperties properties) {
        this.fetcher = fetcher;
        this.properties = properties;
    }

    @Override
    public CapabilityDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public ToolResult execute(CurrentActor actor, JsonNode arguments, ToolExecutionContext context) {
        String url = arguments == null ? "" : arguments.path("url").asText("").trim();
        if (url.isEmpty()) {
            return ToolResult.error("缺少网页地址 url，请提供要读取的链接");
        }
        if (isBlockedUrl(url)) {
            return ToolResult.error("仅支持公网 http/https 链接，已阻止内网或非法地址");
        }
        try {
            // 统一在出口做正文提取：生产 fetcher 返回原始 HTML，测试注入的 stub 同样走该逻辑
            String text = extractText(fetcher.fetch(url,
                    (int) Math.min(properties.getTimeoutMs(), Integer.MAX_VALUE),
                    properties.getMaxDownloadBytes()));
            if (text == null || text.isBlank()) {
                return ToolResult.error("网页内容为空或无法提取正文");
            }
            String host = URI.create(url).getHost();
            String trimmed = text.length() > properties.getMaxContentChars()
                    ? text.substring(0, properties.getMaxContentChars()) + "…（正文过长已截断）"
                    : text;
            return ToolResult.context("web-fetch:" + (host == null ? "unknown" : host),
                    "【网页正文】" + url + "\n" + trimmed);
        } catch (Exception failure) {
            String message = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
            return ToolResult.error("网页读取失败：" + message);
        }
    }

    /**
     * 生产抓取实现：jsoup 抓取页面（自动处理编码），手动逐跳校验重定向防止绕过内网防护。
     */
    static String fetchPage(String url, int timeoutMs, long maxBytes) throws Exception {
        String current = url;
        for (int redirects = 0; ; redirects++) {
            // 每一跳都重新做内网地址校验，防止重定向跳到内网
            if (isBlockedUrl(current)) {
                throw new IllegalStateException("不允许访问的地址（内网或非 http/https）");
            }
            Connection.Response response = Jsoup.connect(current)
                    .userAgent("Mozilla/5.0 (compatible; GymMind-AI/1.0)")
                    .timeout(timeoutMs)
                    .maxBodySize((int) Math.min(maxBytes, Integer.MAX_VALUE))
                    .followRedirects(false)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .execute();
            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                String location = response.header("Location");
                if (location == null || location.isBlank()) {
                    throw new IllegalStateException("网页重定向缺少目标地址");
                }
                if (redirects >= MAX_REDIRECTS) {
                    throw new IllegalStateException("网页重定向次数过多，请直接提供最终链接");
                }
                current = URI.create(current).resolve(location).toString();
                continue;
            }
            if (status >= 400) {
                throw new IllegalStateException("网页返回 HTTP " + status);
            }
            if (!isSupportedContentType(response.contentType())) {
                throw new IllegalStateException("暂不支持的内容类型：" + response.contentType());
            }
            // 原始 HTML 交由 execute 出口统一提取正文
            return response.body();
        }
    }

    /** 提取 HTML 正文：去掉脚本样式等噪音标签，jsoup text() 自动处理实体与空白 */
    static String extractText(String html) {
        if (html == null || html.isBlank()) return "";
        Document doc = Jsoup.parse(html);
        doc.select("script,style,noscript,iframe,svg").remove();
        return doc.body() == null ? doc.text() : doc.body().text();
    }

    /** 常见网页文本类型白名单 */
    private static boolean isSupportedContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) return true;
        String value = contentType.toLowerCase();
        return value.startsWith("text/")
                || value.contains("html")
                || value.contains("json")
                || value.contains("xml");
    }

    /**
     * SSRF 防护：仅放行解析结果全部为公网 IP 的 http/https 地址，
     * 拦截 localhost、内网段（10/172.16-31/192.168/100.64）、链路本地、IPv6 本地段等。
     */
    static boolean isBlockedUrl(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            if (!"http".equals(scheme) && !"https".equals(scheme)) return true;
            String host = uri.getHost();
            if (host == null || host.isBlank()) return true;
            String lower = host.toLowerCase();
            if ("localhost".equals(lower) || lower.endsWith(".localhost") || lower.endsWith(".local")
                    || lower.endsWith(".internal") || lower.endsWith(".lan") || "0.0.0.0".equals(lower)) {
                return true;
            }
            // 解析出的所有地址（域名会做 DNS）必须全部为公网，任一内网地址即拒绝
            for (InetAddress address : InetAddress.getAllByName(lower)) {
                if (isPrivateAddress(address)) return true;
            }
            return false;
        } catch (Exception failure) {
            return true;
        }
    }

    /** 判断是否为内网/保留地址 */
    private static boolean isPrivateAddress(InetAddress address) {
        if (address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isAnyLocalAddress()
                || address.isMulticastAddress() || address.isSiteLocalAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        // IPv4 运营商级 NAT 100.64.0.0/10
        if (bytes.length == 4 && bytes[0] == 100 && (bytes[1] & 0xC0) == 64) return true;
        // IPv6 unique local fc00::/7
        if (address instanceof Inet6Address && bytes.length == 16) {
            return (bytes[0] & 0xFE) == 0xFC;
        }
        return false;
    }

    private static JsonNode schema() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("url").put("type", "string")
                .put("description", "要读取的网页完整地址，须为公网 http/https 链接");
        schema.putArray("required").add("url");
        return schema;
    }
}
