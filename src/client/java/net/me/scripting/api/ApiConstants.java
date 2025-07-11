package net.me.scripting.api;

public final class ApiConstants {

    public static final String REGISTER = "register";
    public static final String UNREGISTER = "unregister";
    public static final String UNREGISTER_ALL = "unregisterAll";
    public static final String BUILDER = "builder";
    // Commands API
    public static final String LITERAL = "literal";
    public static final String ARGUMENT = "argument";
    public static final String ARG_TYPE = "ArgType";
    // Config API
    public static final String GET = "get";
    public static final String SET = "set";
    public static final String HAS = "has";
    public static final String SAVE = "save";
    public static final String LOAD = "load";
    public static final String GET_ALL = "getAll";
    // Event API
    public static final String EVENTS = "Events";
    public static final String PHASE = "Phase";
    // Hook API
    public static final String HOOK = "hook";
    public static final String UNHOOK = "unhook";
    public static final String UNHOOK_ALL = "unhookAll";
    // Keybind API
    public static final String KEYS = "Keys";
    private ApiConstants() {
    }
}