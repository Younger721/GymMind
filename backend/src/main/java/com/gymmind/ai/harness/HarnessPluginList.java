package com.gymmind.ai.harness;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/** 插件 ID 列表序列化工具 */
public final class HarnessPluginList {

    private HarnessPluginList() {
    }

    public static String join(List<String> pluginIds) {
        if (pluginIds == null || pluginIds.isEmpty()) {
            return "basic_chat";
        }
        return pluginIds.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.joining(","));
    }

    public static List<String> parse(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of("basic_chat");
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    public static List<String> intersect(List<String> requested, List<String> allowed) {
        var allowedSet = new LinkedHashSet<>(allowed);
        return requested.stream().filter(allowedSet::contains).toList();
    }
}
