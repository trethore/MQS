# Repository Guidelines

My QOL Scripts (MQS) is a powerful, client-side scripting mod for Minecraft 1.21.11 that runs on the Fabric mod loader.
It provides a high-performance JavaScript engine and a rich set of APIs, enabling complex scripts that enhance gameplay.

## Project Overview & Architecture

Here is the structure of the repository:

```text
/
├── libs-src/                                  # Unpacked dependency sources for browsing/reference.
│   ├── fabric/
│   ├── minecraft/
│   └── <lib-name>/
├── src/
│   └── client/
│       ├── java/net/me/
│       │   ├── scripting/                     # Discovery (`ScriptDiscoverer`), Graal context pooling, lifecycle, `ScriptingService`.
│       │   ├── event/                         # MQS event bus + Fabric bridge (`EventManager`, `FabricEventAdapter`).
│       │   ├── hooking/                       # ByteBuddy interception and hook lifecycle (`HookManager`).
│       │   ├── console/                       # Integrated console and console command surface.
│       │   ├── command/                       # `/mqs` command tree and command integration.
│       │   ├── keybinds/                      # Script-defined keybind registration and dispatch.
│       │   ├── config/                        # Global host config (`mqs_config.json`) and config keys.
│       │   ├── utils/                         # Shared host-side utilities.
│       │   └── mixin/                         # Mixins exposing Minecraft internals required by scripting APIs.
│       └── resources/
│           ├── fabric.mod.json                # Registers the client entry point and mixin config.
│           └── myqolscripts.client.mixins.json
├── scripts/                                   # User-controlled scripts loaded by MQS.
├── docs/
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

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
  config accessors) instead of broad inheritance trees. Host code targets Java 21 and follows Fabric/Mojang abstractions,
  so new features should extend the existing managers or adapters rather than bypass them.

## General Coding Conventions

- Target Java 21, use 4-space indentation, and keep packages under `tytoo.grapheneui*`.
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

## Java 21 Expectations

- Assume Java 21 at runtime; use only stable features and avoid preview or incubator APIs.
- Use modern Java 21 standard-library utilities (Streams, Optional, records) when they improve clarity.
- Use descriptive names like `ignored` for intentionally unused variables, parameters, and caught exceptions.
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
