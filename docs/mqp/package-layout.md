# Package Layout

An MQP package is a directory containing:

- A `manifest.json` file describing the package.
- A JavaScript entrypoint executed with GraalJS.

## Package Directory

By default, packages are loaded from the `myqolpackages` directory inside the Minecraft game directory:

```text
<minecraft-directory>/
  myqolpackages/
    config.json
    <package-directory>/
      manifest.json
      src/
        index.js
```

Each direct child of `myqolpackages` is treated as a package directory. For example:

```text
<minecraft-directory>/myqolpackages/example-package/
```

The directory name does not determine the package identifier. It only identifies the package on the filesystem.
The package identifier comes from the manifest's optional `id` field or is derived from its display name.

Additional package roots can be configured in [`config.json`](configuration.md).

## Manifest

The package manifest is named `manifest.json` and contains:

- `id`: The optional package identifier.
- `name`: The package name.
- `description`: A short description of the package.
- `version`: The package version.
- `entrypoint`: The path to the JavaScript entrypoint, relative to the package directory.

Example:

```json
{
  "id": "example-package",
  "name": "Example Package",
  "description": "An example MQP package.",
  "version": "1.0.0",
  "entrypoint": "src/index.js"
}
```

Package identifiers must contain lowercase ASCII letters, numbers, and single hyphens between words. When `id` is
omitted, it is derived from `name` by trimming it, converting it to lowercase, replacing runs of non-alphanumeric
characters with hyphens, and removing leading or trailing hyphens. For example, `My Cool Package!` becomes
`my-cool-package`.

If multiple discovered packages have the same identifier, none of the packages sharing that identifier are loaded.

The resulting package layout is:

```text
example-package/
  manifest.json
  src/
    index.js
```

## Entrypoint

The entrypoint is a JavaScript file executed by the MQP GraalJS runtime when the package is loaded.

For now, no additional files, lifecycle functions, dependencies, permissions, or hook declarations are required by the
package format. The hook API implemented with Byte Buddy is outside the scope of this initial package layout.
