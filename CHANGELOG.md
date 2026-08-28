# Changelog

## [Unreleased]

### Added

- An asynchronous JavaScript `fetch()` API.
- Optional package ID argument for reloading a single enabled package without restarting others.

### Changed

- Updated Fabric API to 0.141.6+1.21.11.
- Updated Gradle to 9.7.1.
- Removed granular package permissions and `mqp.permissions`; executing packages have unrestricted
  Java and Minecraft access until a separate trust system is introduced.

## [0.0.1]

### Added

- Initial common runtime and Fabric 1.21.11 implementation.
- JavaScript package discovery, configuration, and runtime APIs.
- Build, SonarQube analysis, and dependency source tooling.
