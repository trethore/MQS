# Repository Guidelines

My QOL Scripts (MQS) is a powerful, client-side scripting mod for Minecraft 1.21.4 that runs on the Fabric mod loader.
It provides a high-performance JavaScript engine and a rich set of APIs, enabling complex scripts that enhance gameplay.

## Project Overview & Architecture

My QOL Scripts (MQS) ships as a single Fabric client mod with `net.me.Main` as the entry point. During client initialization it spins up the GraalJS engine, wires together the scripting, mapping, configuration, event, command, console, keybind, and hook managers, then defers script activation until Yarn mappings finish loading and work is rescheduled onto the Minecraft client thread via `McUtils`.

Runtime modules are organized into focused packages:
- `net.me.scripting`: script discovery (`ScriptDiscoverer` scans the `Main.MOD_DIR/scripts` tree for `@module(...)` declarations), Graal context pooling, lifecycle management, and the `ScriptingService` façade used by in-game commands and the console.
- `net.me.event`: an MQS-specific event bus layered on Fabric hooks. `EventManager` tracks per-phase listeners, bridges Fabric events through `FabricEventAdapter`, and enforces ownership per `RunningScript`.
- `net.me.hooking`: ByteBuddy-driven interception. `HookManager` installs the agent, resolves Yarn → runtime names via `MappingsManager`, and applies or removes hooks in response to script requests.
- `net.me.console`, `net.me.command`, `net.me.keybinds`, `net.me.config`: user-facing surfaces for the integrated console, `/mqs` command tree, script-defined keybinds, and persistent configuration (`mqs_config.json` plus per-script files).
- `net.me.utils` together with mixins under `net.me.mixin`: shared helpers and mixins declared in `myqolscripts.client.mixins.json` that expose Minecraft internals required by the scripting APIs.

Assets and metadata live under `src/client/resources/`; `fabric.mod.json` registers the client entry point and mixin config.

## Design & Philosophy

- **Scripts are treated as first-class modules with explicit lifecycles**: Managers ensure that enabling a script registers its events, keybinds, hooks, and config, and that disabling it unwinds that state. `ScriptingService` exposes these lifecycle controls to commands, the console, and other surfaces without leaking implementation details.
- **Performance and stability drive host-side choices**: `ScriptContextManager` pools Graal contexts, hook retransformation only occurs while entries exist, and Minecraft-facing work is scheduled back onto the client thread through `McUtils`. Yarn mappings are loaded up front so JavaScript can rely on stable names.
- **Security defaults stay conservative**: `ScriptingClassResolver` restricts class access to whitelisted packages unless `GlobalConfigManager`'s `allowAllClasses` flag is explicitly enabled, and global toggles (log redirect, class access) are surfaced via console commands. Scripts are discovered from the user-controlled `myqolscripts/scripts` folder, keeping ownership with the player.
- **Contributor ergonomics matter**: APIs expand through explicit utilities (for example `MQSUtils`, rendering helpers, config accessors) instead of broad inheritance trees. Host code targets Java 21 and follows Fabric/Yarn abstractions, so new features should extend the existing managers or adapters rather than bypass them.

## General Coding Conventions
- Target Java 21 with 4-space indentation and packages under `net.me.*`.
- Use PascalCase for classes, camelCase for methods and fields, and UPPER_SNAKE_CASE for constants.
- Declare explicit types and avoid `var`; prefer descriptive names over one-letter identifiers.
- Import types rather than using fully qualified names inside method bodies.
- When adding shared utilities, document behavior through clear method names and arguments rather than abstract component hierarchies.
- Assume contributors are working in IntelliJ IDEA; keep code free of IDE warnings.
- Avoid code comments unless documentation is explicitly requested.
- Keep edits minimal and stylistically consistent with surrounding code; do not introduce unrelated refactors or new formatting tools.
- If requirements are unclear or infeasible, request clarification before proceeding.
- Order members in Java classes consistently: static constants, static fields, instance fields, constructors, overridden methods, public methods, protected/private helpers, then getters and setters at the bottom.

## Java 21 Expectations
- Assume Java 21 at runtime; use only stable features and avoid preview or incubator APIs.
- Use modern Java 21 standard-library utilities (Streams, Optional, records) when they improve clarity.
- Maintain explicit, readable control flow; avoid clever constructs that harm comprehension.

## Minecraft Integration Rules
- The codebase targets Fabric for Minecraft 1.21.4 with Yarn mappings `1.21.4+build.8`; use APIs that exist in this combination.
- Prefer modern Fabric/Minecraft methods such as `Identifier.of(String namespace, String path)` and up-to-date rendering APIs; avoid deprecated signatures.
- Place new assets, mixin configs, and JSON metadata within `src/client/resources/`, keeping identifiers in the `Main.MOD_ID` namespace.
- Integrate through established abstractions unless explicitly extending them.
- Never reference loaders, mappings, or game versions beyond the configured target without explicit user approval.

## Dependencies & External Sources
- Fabric Loader, Fabric API, and Yarn mappings are versioned in `gradle.properties`; Fabric Loom integrates them into the client source set and remaps game classes during packaging. Keep these aligned with Minecraft `1.21.4` before updating APIs.
- GraalVM JavaScript artifacts (`graal-sdk`, `truffle-api`, `js-language`, `js-scriptengine`) are bundled through Shadow, relocated to `net.me.libs.graalvm`, and used by the scripting engine.
- Byte Buddy (`byte-buddy`, `byte-buddy-agent`) is shaded to `net.me.libs.bytebuddy` and powers runtime interception in `HookManager`.
- Lombok ships as a dependency; prefer its annotations to reduce boilerplate.
- Library sources are fetched through the `sourceDeps` configuration (see `build.gradle`) and unpacked per-library with `./gradlew unpackSources` into `libs-src/<library>`. The task `./gradlew cleanSources` prune those directories if you need a fresh extraction.

## Testing & Verification
- Do not run Gradle commands yourself; instead provide the exact command for the user to execute and state tooling limitations clearly.
- Encourage running `./gradlew compileJava` after changes, `./gradlew build` for full validation, and `./gradlew runClient` to test UI flows.
- Document manual validation steps and remaining risks before completing work.

## Pull Requests
- Keep PRs focused on a single concern and avoid unrelated cleanups.
- Provide clear summaries, rationale, and manual test steps; include visuals for UI changes when relevant.
- Use Conventional Commit conventions (e.g., `feat(ui): add slider snap support`) and flag breaking API changes early.
