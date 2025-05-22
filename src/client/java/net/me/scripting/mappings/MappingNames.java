package net.me.scripting.mappings;

public enum MappingNames {
    NAMED("named"),
    INTERMEDIARY("intermediary"),
    OFFICIAL("official");

    private final String name;

    MappingNames(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
