package net.me.scripting.config;

import org.graalvm.polyglot.Context;
import java.util.List;

public record ExtensionConfig(
        MappedClassInfo extendsClass,
        List<MappedClassInfo> implementsClasses,
        Context context
) {}