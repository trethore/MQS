package net.me.hooking.context;

import org.graalvm.polyglot.HostAccess;

public class CallerInfo {
    private final StackTraceElement element;

    public CallerInfo(StackTraceElement element) {
        this.element = element;
    }

    @HostAccess.Export
    public String getClassName() {
        return element.getClassName();
    }

    @HostAccess.Export
    public String getMethodName() {
        return element.getMethodName();
    }

    @HostAccess.Export
    public int getLineNumber() {
        return element.getLineNumber();
    }

    @Override
    public String toString() {
        return String.format("%s.%s(%s:%d)", getClassName(), getMethodName(), element.getFileName(), getLineNumber());
    }
}