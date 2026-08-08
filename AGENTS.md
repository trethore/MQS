# Repository Guidelines

My QOL Packages (MQP) is a client-side Minecraft mod that provides an API and JavaScript engine for creating and
distributing packages that modify the game at runtime.

## Project Structure

Here is an overview of the project:

```text
myqolpackages/
  .github/
  build-logic/                                  # Included Gradle build for custom build logic.
    sonar-analysis/                             # Gradle plugin for running SonarQube analysis.
    unpack-sources/                             # Gradle plugin that unpacks dependency and Git reference sources.
  docs/
  gradle/libs.versions.toml
  packages/
    common/                                     # Shared mod logic with no Minecraft or Fabric dependencies.
      src/
        main/
          java/io/github/trethore/myqolpackages/
            api/                                # Public entry points used by loader/version implementations.
            internal/                           # Private common implementation details.
        test/
          java/io/github/trethore/myqolpackages/
          resources/
      build.gradle.kts
    fabric-1.21.11/                             # Fabric implementation for Minecraft 1.21.11.
      src/main/
        java/io/github/trethore/myqolpackages/
          mixin/
          FabricBootstrap.java                  # Fabric ModInitializer that boots common code.
        resources/
          assets/myqolpackages/
          myqolpackages.mixins.json
          fabric.mod.json
      build.gradle.kts
  references/                                   # Dependency source code for browsing and reference.
    net.fabricmc.fabric-api-fabric-api-0.141.4-1.21.11/
      nested/                                   # Source code of the nested jars.
    com.mojang-minecraft-1.21.11/
    <group>-<lib-name>-<version>/
  .gitignore
  build.gradle.kts                              # Root Gradle configuration.
  CHANGELOG.md
  gradle.properties
  HEADER
  LICENSE
  README.md
  settings.gradle.kts
```

## General Coding Conventions

- `packages/common` should contain only the version-independent logic that is shared across all Minecraft implementations.
- `packages/<loader>-<version>` should contain version-dependent code, like the mod entry point, integration logic, mixins, and Minecraft/loader dependencies.
- Write comments and documentation only when it is explicitly requested by the user.
- Assume contributors use IntelliJ IDEA, and keep code free of IDE warnings.

## Java Expectations

- Prefer explicit types over `var`, and use descriptive names instead of one-letter identifiers.
- Keep member order consistent in Java classes: static constants, static fields, instance fields, constructors,
  overridden methods, public methods, protected and private helper methods, then getters and setters at the bottom.
- Import types instead of using fully qualified names inside method bodies.

## Testing & Verification

- Run `./gradlew check` to catch Java compilation errors, formatting issues, and execute tests.
- Run `./gradlew spotlessApply` to format changes directly instead of running `./gradlew spotlessCheck` first and then fixing formatting issues.
- Do not run long-running Gradle tasks, such as game launches. Instead, provide the exact command for the user to run, for example:
  `./gradlew :packages:fabric-1.21.11:runClient`

## Dependencies & External Sources

- Library source code is available in the `references` directory for browsing and reference only. Do not edit it.
- The `references` directory is generated via the `./gradlew unpackSources` command.
- You can clean the generated references by running `./gradlew cleanUnpackedSources`.

## Commits & Pull Requests

- Follow the Conventional Commits specification for commit messages.
- Pull request summaries should include the related issue(s), a brief description of the changes, and how the changes were tested.
