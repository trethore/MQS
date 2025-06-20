package net.me.scripting.module;

import java.nio.file.Path;
import java.util.Objects;

public record ScriptDescriptor(
        Path path,
        String moduleName
) {

    public String getId() {
        return path.getFileName().toString() + ":" + moduleName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScriptDescriptor that = (ScriptDescriptor) o;
        return Objects.equals(path, that.path) && Objects.equals(moduleName, that.moduleName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, moduleName);
    }
}