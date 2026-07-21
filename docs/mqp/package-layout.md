# Package Layout

An MQP package is a directory containing:

- A `manifest.json` file describing the package.
- A JavaScript entrypoint executed with GraalJS.

## Package Directory

By default, packages are loaded from the `myqolpackages` directory inside the Minecraft game directory:

```text
<minecraft-directory>/
  myqolpackages/
    <package-name>/
      manifest.json
      src/
        index.js
```

Each direct child of `myqolpackages` is treated as a package directory. For example:

```text
<minecraft-directory>/myqolpackages/example-package/
```

The directory name is the package identifier. In this example, the package identifier is `example-package`, while the
manifest `name` is its display name.

## Manifest

The package manifest is named `manifest.json` and contains:

- `name`: The package name.
- `description`: A short description of the package.
- `version`: The package version.
- `entrypoint`: The path to the JavaScript entrypoint, relative to the package directory.

Example:

```json
{
  "name": "Example Package",
  "description": "An example MQP package.",
  "version": "1.0.0",
  "entrypoint": "src/index.js"
}
```

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
