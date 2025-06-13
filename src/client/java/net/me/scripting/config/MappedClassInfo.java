package net.me.scripting.config;

import java.util.List;
import java.util.Map;

public record MappedClassInfo(
        String yarnName,
        Class<?> targetClass,
        Map<String, List<String>> methodMappings,
        Map<String, String> fieldMappings
) {}