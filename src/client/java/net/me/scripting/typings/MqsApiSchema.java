/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2026 Titouan Réthoré
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package net.me.scripting.typings;

import net.me.event.Events;
import net.me.keybinds.Keys;
import net.me.scripting.commands.ScriptArgumentType;
import net.me.scripting.typings.schema.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class MqsApiSchema {
    private static final String PHASE = "phase";
    private static final String MQS_EVENT_OPTIONS_BUILDER = "MQSEventOptionsBuilder";
    private static final String MQS_DISPOSER = "MQSDisposer";
    private static final String CALLBACK = "callback";
    private static final String MQS_EVENT_CALLBACK = "MQSEventCallback";
    private static final String OPTIONS = "options";
    private static final String TARGET = "target";
    private static final String UNKNOWN = "unknown";
    private static final String STRING = "string";
    private static final String DEFAULT_VALUE = "defaultValue";
    private static final String BOOLEAN = "boolean";
    private static final String NUMBER = "number";
    private static final String REPEATABLE = "repeatable";
    private static final String MQS_KEYBIND_OPTIONS_BUILDER = "MQSKeybindOptionsBuilder";
    private static final String MQS_KEY_CODE_MAP = "MQSKeyCodeMap";
    private static final String HANDLER = "handler";
    private static final String MQS_ANY_FUNCTION = "MQSAnyFunction";
    private static final String MQS_COMMAND_BUILDER = "MQSCommandBuilder";
    private static final String MQS_HOOK_OPTIONS = "MQSHookOptions";
    private static final String MQS_HOOK_HANDLER = "MQSHookHandler";
    private static final String TARGET_CLASS = "targetClass";
    private static final String METHOD_NAME = "methodName";

    private MqsApiSchema() {
    }

    public static List<TsObject> objects() {
        return List.of(
                eventPhaseEnum(),
                eventsEnum(),
                eventSubscriptionOptions(),
                eventOptionsBuilder(),
                fabricEventsApi(),
                eventsApi(),
                configApi(),
                keybindOptions(),
                keybindOptionsBuilder(),
                keyCodeMap(),
                keybindsApi(),
                commandContext(),
                commandBuilder(),
                commandTypes(),
                commandApi(),
                hookOptions(),
                hooksApi(),
                utilsSchedulerApi(),
                utilsApi(),
                mqsApi()
        );
    }

    private static TsObject eventPhaseEnum() {
        return new TsObject(
                "MQSEventPhaseValues",
                List.of(
                        ro("PRE", "\"PRE\""),
                        ro("POST", "\"POST\"")
                )
        );
    }

    private static TsObject eventsEnum() {
        List<TsMember> members = new ArrayList<>();
        for (Events event : Events.values()) {
            String eventName = event.name();
            members.add(ro(eventName, "\"" + eventName + "\""));
        }
        return new TsObject("MQSEventsEnum", List.copyOf(members));
    }

    private static TsObject eventSubscriptionOptions() {
        return new TsObject(
                "MQSEventSubscriptionOptions",
                List.of(prop(PHASE, "MQSEventPhase"))
        );
    }

    private static TsObject eventOptionsBuilder() {
        return new TsObject(
                MQS_EVENT_OPTIONS_BUILDER,
                List.of(
                        method(PHASE, fn(MQS_EVENT_OPTIONS_BUILDER, p(PHASE, "MQSEventPhase | string"))),
                        method("build", fn("MQSEventSubscriptionOptions"))
                )
        );
    }

    private static TsObject fabricEventsApi() {
        return new TsObject(
                "MQSFabricEventsApi",
                List.of(
                        method("clientTickStart", fn(MQS_DISPOSER, p(CALLBACK, MQS_EVENT_CALLBACK))),
                        method("clientTickEnd", fn(MQS_DISPOSER, p(CALLBACK, MQS_EVENT_CALLBACK)))
                )
        );
    }

    private static TsObject eventsApi() {
        List<TsMember> members = new ArrayList<>(List.of(
                ro("fabric", "MQSFabricEventsApi"),
                ro("Events", "MQSEventsEnum"),
                ro("Phase", "MQSEventPhaseValues"),
                method(OPTIONS, fn(MQS_EVENT_OPTIONS_BUILDER)),
                method("off", fn("void", opt(TARGET, MQS_DISPOSER + " | " + MQS_EVENT_CALLBACK))),
                method(
                        "register",
                        fn(MQS_DISPOSER, p("eventType", UNKNOWN), p(CALLBACK, MQS_EVENT_CALLBACK)),
                        fn(MQS_DISPOSER, p("eventType", UNKNOWN), p(PHASE, "MQSEventPhase | string"), p(CALLBACK, MQS_EVENT_CALLBACK))
                ),
                method("unregister", fn("void", p("eventTarget", UNKNOWN), opt("phaseOrCallback", UNKNOWN), opt(CALLBACK, MQS_EVENT_CALLBACK))),
                method("unregisterAll", fn("void"))
        ));

        for (Events event : Events.values()) {
            members.add(method(
                    buildEventHandlerName(event),
                    fn(MQS_DISPOSER, p(CALLBACK, MQS_EVENT_CALLBACK), opt(OPTIONS, "MQSEventOptionsLike"))
            ));
        }

        return new TsObject("MQSEventsApi", List.copyOf(members));
    }

    private static TsObject configApi() {
        return new TsObject(
                "MQSConfigApi",
                List.of(
                        method("get", fn(UNKNOWN, p("key", STRING), opt(DEFAULT_VALUE, UNKNOWN))),
                        method("set", fn("void", p("key", STRING), p("value", UNKNOWN))),
                        method("has", fn(BOOLEAN, p("key", STRING))),
                        method("save", fn("void")),
                        method("load", fn("void")),
                        method("getAll", fn("Record<string, unknown>")),
                        method("getBool", fn(BOOLEAN, p("key", STRING), opt(DEFAULT_VALUE, BOOLEAN))),
                        method("getNumber", fn(NUMBER, p("key", STRING), opt(DEFAULT_VALUE, NUMBER))),
                        method("getString", fn("string | null", p("key", STRING), opt(DEFAULT_VALUE, STRING)))
                )
        );
    }

    private static TsObject keybindOptions() {
        return new TsObject(
                "MQSKeybindOptions",
                List.of(
                        prop(REPEATABLE, BOOLEAN),
                        prop("debounce", NUMBER)
                )
        );
    }

    private static TsObject keybindOptionsBuilder() {
        return new TsObject(
                MQS_KEYBIND_OPTIONS_BUILDER,
                List.of(
                        method(REPEATABLE, fn(MQS_KEYBIND_OPTIONS_BUILDER), fn(MQS_KEYBIND_OPTIONS_BUILDER, p(REPEATABLE, BOOLEAN))),
                        method("debounce", fn(MQS_KEYBIND_OPTIONS_BUILDER, p("debounceMillis", NUMBER))),
                        method("build", fn("MQSKeybindOptions"))
                )
        );
    }

    private static TsObject keyCodeMap() {
        List<TsMember> members = new ArrayList<>();
        for (Keys key : Keys.values()) {
            members.add(ro(key.name(), NUMBER));
        }
        members.add(ro("[key: string]", NUMBER + " | undefined"));
        return new TsObject(MQS_KEY_CODE_MAP, List.copyOf(members));
    }

    private static TsObject keybindsApi() {
        return new TsObject(
                "MQSKeybindsApi",
                List.of(
                        ro("keys", MQS_KEY_CODE_MAP),
                        ro("Keys", MQS_KEY_CODE_MAP),
                        method(OPTIONS, fn(MQS_KEYBIND_OPTIONS_BUILDER)),
                        method(
                                "bind",
                                fn(
                                        MQS_DISPOSER,
                                        p("name", STRING),
                                        p("key", NUMBER + " | " + STRING),
                                        p(HANDLER, MQS_ANY_FUNCTION),
                                        opt(OPTIONS, "MQSKeybindOptions | MQSKeybindOptionsBuilder | Record<string, unknown>")
                                )
                        ),
                        method(
                                "bindToggle",
                                fn(
                                        MQS_DISPOSER,
                                        p("name", STRING),
                                        p("key", NUMBER + " | " + STRING),
                                        p(HANDLER, "(enabled: boolean) => unknown"),
                                        opt(OPTIONS, "MQSKeybindOptions | MQSKeybindOptionsBuilder | Record<string, unknown>")
                                )
                        ),
                        method("unbind", fn(BOOLEAN, p("name", STRING))),
                        method("unbindAll", fn("void"))
                )
        );
    }

    private static TsObject commandContext() {
        return new TsObject(
                "MQSCommandContext",
                List.of(
                        method("source", fn("any")),
                        method("arg", fn(UNKNOWN, p("name", STRING))),
                        method("str", fn(STRING, p("name", STRING)))
                )
        );
    }

    private static TsObject commandBuilder() {
        return new TsObject(
                MQS_COMMAND_BUILDER,
                List.of(
                        method("then", fn(MQS_COMMAND_BUILDER, p("child", MQS_COMMAND_BUILDER))),
                        method("lit", fn(MQS_COMMAND_BUILDER, p("name", STRING))),
                        method("arg", fn(MQS_COMMAND_BUILDER, p("name", STRING), p("typeName", STRING))),
                        method("run", fn(MQS_COMMAND_BUILDER, p(HANDLER, "(context: MQSCommandContext) => unknown"))),
                        method("suggest", fn(MQS_COMMAND_BUILDER, p("suggestions", "MQSCommandSuggestionProvider")))
                )
        );
    }

    private static TsObject commandTypes() {
        List<TsMember> members = new ArrayList<>();
        for (ScriptArgumentType argumentType : ScriptArgumentType.values()) {
            String typeName = argumentType.toString();
            members.add(ro(typeName, "\"" + typeName + "\""));
        }
        return new TsObject("MQSCommandTypes", List.copyOf(members));
    }

    private static TsObject commandApi() {
        return new TsObject(
                "MQSCommandApi",
                List.of(
                        ro("types", "MQSCommandTypes"),
                        method("lit", fn(MQS_COMMAND_BUILDER, p("name", STRING), opt("configure", "(builder: MQSCommandBuilder) => unknown"))),
                        method("arg", fn(MQS_COMMAND_BUILDER, p("name", STRING), p("type", STRING))),
                        method("reg", fn(MQS_DISPOSER, p("builder", MQS_COMMAND_BUILDER))),
                        method("unreg", fn("void", p("commandName", STRING))),
                        method("clear", fn("void")),
                        method("quick", fn(MQS_DISPOSER, p("name", STRING), p(HANDLER, "(context: MQSCommandContext) => unknown")))
                )
        );
    }

    private static TsObject hookOptions() {
        return new TsObject(
                MQS_HOOK_OPTIONS,
                List.of(
                        optProp("args", NUMBER),
                        optProp("mode", "MQSHookMode")
                )
        );
    }

    private static TsObject hooksApi() {
        TsFunction descriptorOverload = fn(
                MQS_DISPOSER,
                p(TARGET, STRING),
                p(HANDLER, MQS_HOOK_HANDLER),
                opt(OPTIONS, MQS_HOOK_OPTIONS)
        );
        TsFunction classOverload = fn(
                MQS_DISPOSER,
                p(TARGET_CLASS, UNKNOWN),
                p(METHOD_NAME, STRING),
                p(HANDLER, MQS_HOOK_HANDLER),
                opt(OPTIONS, MQS_HOOK_OPTIONS)
        );

        return new TsObject(
                "MQSHooksApi",
                List.of(
                        method("before", descriptorOverload, classOverload),
                        method("after", descriptorOverload, classOverload),
                        method("instead", descriptorOverload, classOverload),
                        method("hook", descriptorOverload, classOverload),
                        method(
                                "unhook",
                                fn("void", p(TARGET, STRING), opt(OPTIONS, MQS_HOOK_OPTIONS)),
                                fn("void", p(TARGET_CLASS, UNKNOWN), p(METHOD_NAME, STRING), opt(OPTIONS, MQS_HOOK_OPTIONS))
                        ),
                        method("unhookAll", fn("void"))
                )
        );
    }

    private static TsObject utilsSchedulerApi() {
        return new TsObject(
                "MQSUtilsSchedulerApi",
                List.of(
                        method("timeout", fn(MQS_DISPOSER, p(CALLBACK, MQS_ANY_FUNCTION), opt("delayTicks", NUMBER))),
                        method("interval", fn(MQS_DISPOSER, p(CALLBACK, MQS_ANY_FUNCTION), p("intervalTicks", NUMBER)))
                )
        );
    }

    private static TsObject utilsApi() {
        return new TsObject(
                "MQSUtilsApi",
                List.of(
                        method("mc", fn("JavaInstance | any")),
                        method("runOnClientThread", fn("void", p(CALLBACK, MQS_ANY_FUNCTION))),
                        method("world", fn("JavaInstance | any | null")),
                        method("player", fn("JavaInstance | any | null")),
                        ro("scheduler", "MQSUtilsSchedulerApi"),
                        ro("chat", "JavaClass<any>"),
                        ro("math", "JavaClass<any>")
                )
        );
    }

    private static TsObject mqsApi() {
        return new TsObject(
                "MQSApi",
                List.of(
                        ro("events", "MQSEventsApi"),
                        ro("config", "MQSConfigApi"),
                        ro("keybinds", "MQSKeybindsApi"),
                        ro("cmd", "MQSCommandApi"),
                        ro("hooks", "MQSHooksApi"),
                        ro("utils", "MQSUtilsApi")
                )
        );
    }

    private static TsProperty ro(String name, String type) {
        return new TsProperty(name, type, true, false);
    }

    private static TsProperty prop(String name, String type) {
        return new TsProperty(name, type, false, false);
    }

    private static TsProperty optProp(String name, String type) {
        return new TsProperty(name, type, false, true);
    }

    private static TsMethod method(String name, TsFunction... overloads) {
        return new TsMethod(name, List.of(overloads));
    }

    private static TsFunction fn(String returnType, TsParam... params) {
        return new TsFunction(List.of(params), returnType);
    }

    private static TsParam p(String name, String type) {
        return new TsParam(name, type, false, false);
    }

    private static TsParam opt(String name, String type) {
        return new TsParam(name, type, true, false);
    }

    private static String buildEventHandlerName(Events event) {
        String[] parts = event.name().split("_");
        StringBuilder nameBuilder = new StringBuilder("on");

        for (String part : parts) {
            if (part.isEmpty() || "EVENT".equals(part)) {
                continue;
            }
            String normalizedPart = part.toLowerCase(Locale.ROOT);
            nameBuilder.append(Character.toUpperCase(normalizedPart.charAt(0)));
            if (normalizedPart.length() > 1) {
                nameBuilder.append(normalizedPart, 1, normalizedPart.length());
            }
        }

        return nameBuilder.toString();
    }
}
