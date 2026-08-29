# Repository Guidelines

My QOL Packages (MQP) is a client-side Minecraft mod that provides an API and JavaScript engine for creating and
distributing packages that modify the game at runtime.

## Project Structure

Here is an overview of the project:

```text
.github/
build-logic/sonar-analysis/                    # Gradle plugin for running SonarQube analysis.
config/                                        # SonarQube and Qodana configuration files.
docs/
gradle/libs.versions.toml
packages/
  common/                                      # Shared mod logic with no Minecraft or Fabric dependencies.
    src/
      main/
        java/io/github/trethore/myqolpackages/
          api/                                 # Public entry points used by loader/version implementations.
          internal/                            # Private common implementation details.
      test/
        java/io/github/trethore/myqolpackages/
        resources/
    build.gradle.kts
  fabric-1.21.11/                              # Fabric implementation for Minecraft 1.21.11.
    src/main/
      java/io/github/trethore/myqolpackages/
        mixin/
        FabricBootstrap.java                   # Fabric ModInitializer that boots common code.
      resources/
        assets/myqolpackages/
        myqolpackages.mixins.json
        fabric.mod.json
    build.gradle.kts
scripts/release/                               # Release preparation and publishing scripts.
test-packages/                                 # Example packages for development and testing.
.gitignore
build.gradle.kts
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
- Do not write comments unless documentation is explicitly requested by the user.
- Assume contributors use IntelliJ IDEA, and keep code free of IDE warnings.

## Java Expectations

- Prefer explicit types over `var`, and use descriptive names instead of one-letter identifiers.
- Keep member order consistent in Java classes: static constants, static fields, instance fields, constructors,
  overridden methods, public methods, protected and private helper methods, then getters and setters at the bottom.
- Import types instead of using fully qualified names inside method bodies.

## Testing & Verification

- Run `./gradlew check --quiet` to catch Java compilation errors, formatting issues, and execute tests.
- Run `./gradlew spotlessApply --quiet` to format changes directly instead of running `./gradlew spotlessCheck` first and then fixing formatting issues.
- Do not run long-running Gradle tasks, such as game launches. Instead, provide the exact command for the user to run, for example:
  `./gradlew :packages:fabric-1.21.11:runClient`

## Dependencies and External Source Browsing

- Assume that JDK tools such as `javap`, `jdeps`, and `javadoc`, as well as `cfr`, are available.
- Read `gradle/libs.versions.toml` to identify the dependencies and versions used by the project.
- Look in `~/.gradle/caches/modules-2/files-2.1/` to locate the downloaded dependencies.

## Commits & Pull Requests

- Follow the Conventional Commits specification for commit messages.
- Pull request summaries should include the related issue(s), a brief description of the changes, and how the changes were tested.
