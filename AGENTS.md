# Repository Guidelines

My QOL Scripts (MQS) is a powerful, client-side scripting mod for Minecraft 1.21.11 that runs on the Fabric mod loader.
It provides a high-performance JavaScript engine and a rich set of APIs, enabling complex scripts that enhance gameplay.

## Project Overview & Architecture

My QOL Scripts (MQS) ships as a single Fabric client mod with `net.me.Main` as the entry point. During client
initialization it spins up the GraalJS engine, wires together the scripting, mapping, configuration, event, command,
console, keybind, and hook managers, then defers script activation until official Mojang mappings finish loading and work is
rescheduled onto the Minecraft client thread via `McUtils`.

Runtime modules are organized into focused packages:

- `net.me.scripting`: script discovery (`ScriptDiscoverer` scans the `Main.MOD_DIR/scripts` tree for `@module(...)`
  declarations), Graal context pooling, lifecycle management, and the `ScriptingService` façade used by in-game commands
  and the console.
- `net.me.event`: an MQS-specific event bus layered on Fabric hooks. `EventManager` tracks per-phase listeners, bridges
  Fabric events through `FabricEventAdapter`, and enforces ownership per `RunningScript`.
- `net.me.hooking`: ByteBuddy-driven interception. `HookManager` installs the agent, resolves official Mojang → runtime names via
  `MappingsManager`, and applies or removes hooks in response to script requests.
- `net.me.console`, `net.me.command`, `net.me.keybinds`, `net.me.config`: user-facing surfaces for the integrated
  console, `/mqs` command tree, script-defined keybinds, and persistent configuration (`mqs_config.json` plus per-script
  files).
- `net.me.utils` together with mixins under `net.me.mixin`: shared helpers and mixins declared in
  `myqolscripts.client.mixins.json` that expose Minecraft internals required by the scripting APIs.

Assets and metadata live under `src/client/resources/`; `fabric.mod.json` registers the client entry point and mixin
config.

## Design & Philosophy

- **Scripts are treated as first-class modules with explicit lifecycles**: Managers ensure that enabling a script
  registers its events, keybinds, hooks, and config, and that disabling it unwinds that state. `ScriptingService`
  exposes these lifecycle controls to commands, the console, and other surfaces without leaking implementation details.
- **Performance and stability drive host-side choices**: `ScriptContextManager` pools Graal contexts, hook
  retransformation only occurs while entries exist, and Minecraft-facing work is scheduled back onto the client thread
  through `McUtils`. Official Mojang mappings are loaded up front so JavaScript can rely on stable names.
- **Security defaults stay conservative**: `ScriptingClassResolver` restricts class access to whitelisted packages
  unless `GlobalConfigManager`'s `allowAllClasses` flag is explicitly enabled, and global toggles (log redirect, class
  access) are surfaced via console commands. Scripts are discovered from the user-controlled `myqolscripts/scripts`
  folder, keeping ownership with the player.
- **Contributor ergonomics matter**: APIs expand through explicit utilities (for example `MQSUtils`, rendering helpers,
  config accessors) instead of broad inheritance trees. Host code targets Java 25 and follows Fabric/Mojang abstractions,
  so new features should extend the existing managers or adapters rather than bypass them.

## General Coding Conventions

- Target Java 25, use 4-space indentation, and keep packages under `tytoo.grapheneui*`.
- Use PascalCase for classes, camelCase for methods and fields, and UPPER_SNAKE_CASE for constants.
- Use explicit types instead of `var`, and prefer descriptive names over one-letter identifiers.
- Keep member order consistent in Java classes: static constants, static fields, instance fields, constructors, overridden
  methods, public methods, protected and private helper methods, then getters and setters at the bottom.
- Import types instead of using fully qualified names inside method bodies.
- When adding shared utilities, express behavior through clear method names and arguments rather than abstract hierarchies.
- Avoid comments unless documentation is explicitly requested.
- Keep edits minimal and consistent with surrounding style; avoid unrelated refactors or formatting-only changes.
- Assume contributors use IntelliJ IDEA, and keep code free of IDE warnings.
- If requirements are unclear or infeasible, ask for clarification before proceeding.

## Java 25 Expectations

- Assume Java 25 at runtime; use only stable features and avoid preview or incubator APIs.
- Use modern Java 25 standard-library utilities (Streams, Optional, records) when they improve clarity.
- Prefer unnamed variables (`_`) for intentionally unused variables, parameters, and caught exceptions.
- When intentionally ignoring a caught exception, keep a short explanatory comment in the catch block.
- Maintain explicit, readable control flow; avoid clever constructs that harm comprehension.

## Minecraft Integration Rules

- The codebase targets Fabric for Minecraft 1.21.11 with official Mojang mappings; use APIs that exist in this combination.
- Prefer modern Fabric/Minecraft methods such as `Identifier.fromNamespaceAndPath(String string, String string2)` and
  up-to-date rendering APIs; avoid deprecated signatures.
- Place new assets, mixin configs, and JSON metadata within `src/client/resources/`, keeping identifiers in the `Main.MOD_ID` namespace.
- Integrate through established abstractions unless explicitly extending them.
- Never reference loaders, mappings, or game versions beyond the configured target without explicit user approval.

## Dependencies & External Sources

- Fabric Loader and Fabric API are versioned in `gradle.properties`; Fabric Loom integrates official Mojang mappings
  into the client source set and remaps game classes during packaging. Keep these aligned with Minecraft `1.21.11` before updating APIs.
- GraalVM JavaScript artifacts (`graal-sdk`, `truffle-api`, `js-language`, `js-scriptengine`) are bundled through
  Shadow, relocated to `net.me.libs.graalvm`, and used by the scripting engine.
- Byte Buddy (`byte-buddy`, `byte-buddy-agent`) is shaded to `net.me.libs.bytebuddy` and powers runtime interception in `HookManager`.
- Lombok ships as a dependency; prefer its annotations to reduce boilerplate.
- Library sources are fetched through the `sourceDeps` configuration (see `build.gradle.kts`) and unpacked per library using
  `./gradlew unpackSources` into `libs-src/<library>`. Use these sources to explore library source code.

## Testing & Verification

- Do not run Gradle commands yourself; instead provide the exact command for the user to execute and state tooling limitations clearly.
- Encourage running `./gradlew compileJava` after changes, `./gradlew build` for full validation, and `./gradlew runDebugClient` to test UI flows.
- Document manual validation steps and remaining risks before completing work.

## Pull Requests

- Keep PRs focused on a single concern and avoid unrelated cleanups.
- Provide clear summaries, rationale, and manual test steps; include visuals for UI changes when relevant.
- Use Conventional Commit conventions (e.g., `feat(ui): add slider snap support`) and flag breaking API changes early.
