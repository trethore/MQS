# Repository Guidelines

My QOL Scripts (MQS) is a powerful, client-side scripting mod for Minecraft 1.21.11 that runs on the Fabric mod loader.
It provides a high-performance JavaScript engine and a rich set of APIs, enabling complex scripts that enhance gameplay.

## Project Structure

Here is the structure of the repository:

```text
/
├── references/                                 # Unpacked dependency sources for browsing/reference.
│   ├── fabric/
│   ├── minecraft/
│   └── <lib-name>/
├── src/
│   └── client/
│       ├── java/net/me/
│       │   ├── command/                        # `/mqs` command tree and command integration.
│       │   ├── config/                         # Global host config (`mqs_config.json`) and config keys.
│       │   ├── console/                        # Integrated console and console command surface.
│       │   ├── event/                          # MQS event bus + Fabric bridge (`EventManager`, `FabricEventAdapter`).
│       │   ├── hooking/                        # ByteBuddy interception and hook lifecycle (`HookManager`).
│       │   ├── keybinds/                       # Script-defined keybind registration and dispatch.
│       │   ├── mixin/                          # Mixins exposing Minecraft internals required by scripting APIs.
│       │   ├── scripting/                      # Discovery (`ScriptDiscoverer`), Graal context pooling, lifecycle, `ScriptingService`.
│       │   ├── ui/                             # Graphene-based web UI.
│       │   └── utils/                          # Shared host-side utilities.
│       └── resources/
│           ├── fabric.mod.json                 # Registers the client entry point and mixin config.
│           └── myqolscripts.client.mixins.json
├── scripts/                                    # User-controlled scripts loaded by MQS.
├── web/                                        # Graphene powered web UI (see `web/AGENTS.md` for details).
├── docs/
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

## General Coding Conventions

- Target Java 21, use 4-space indentation, and keep packages under `net.me*`.
- Use explicit types instead of `var`, and prefer descriptive names over one-letter identifiers.
- Keep member order consistent in Java classes: static constants, static fields, instance fields, constructors, overridden
  methods, public methods, protected and private helper methods, then getters and setters at the bottom.
- Import types instead of using fully qualified names inside method bodies.
- When adding shared utilities, express behavior through clear method names and arguments rather than abstract hierarchies.
- Avoid adding comments unless it is explicitly requested.
- Assume contributors use IntelliJ IDEA, and keep code free of IDE warnings.

## Java 21 Expectations

- Assume Java 21 at runtime; use only stable features and avoid preview or incubator APIs.
- Use modern Java 21 standard-library utilities (Streams, Optional, records) when they improve clarity.
- Use descriptive names like `ignored` for intentionally unused variables, parameters, and caught exceptions.
- Maintain explicit, readable control flow; avoid clever constructs that harm comprehension.

## Minecraft Integration Rules

- The codebase targets Fabric for Minecraft 1.21.11 with official Mojang mappings; use APIs that exist in this combination.
- Prefer modern Fabric/Minecraft methods such as `Identifier.fromNamespaceAndPath(String string, String string2)` and
  up-to-date rendering APIs; avoid deprecated signatures.
- Place new assets, mixin configs, and JSON metadata within `src/client/resources/`, keeping identifiers in the `Main.MOD_ID` namespace.
- Integrate through established abstractions unless explicitly extending them.
- Never reference loaders, mappings, or game versions beyond the configured target without explicit user approval.

## Dependencies & External Sources

- Fabric Loader, Fabric API, and official Mojang mappings are the core dependencies; keep their versions aligned with Minecraft `1.21.11`.
- GraalJS is the embedded JavaScript engine and is relocated to `net.me.libs.graalvm` during the build.
- Byte Buddy powers the `HookManager` and is shaded to `net.me.libs.bytebuddy`.
- Graphene is a modern, Chromium-based UI library for Minecraft.
- Lombok is included as a dependency; prefer its annotations to reduce boilerplate.

The source code for all these dependencies is included in `references/<library>`. Use these sources to explore the library APIs.

## Testing & Verification

- Do not run Gradle commands yourself; instead provide the exact command for the user to execute and state tooling limitations clearly.
- Encourage running `./gradlew compileJava` after changes, `./gradlew build` for full validation, and `./gradlew runDebugClient` to test UI flows.
- Document manual validation steps and remaining risks before completing work.

## Pull Requests & Commits

- Keep PRs focused on a single concern and avoid unrelated cleanups.
- Provide clear summaries, rationale, and manual test steps; include visuals for UI changes when relevant.
- Use Conventional Commit conventions (e.g., `feat(ui): add slider snap support`) and flag breaking API changes early.
