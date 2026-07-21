# Configuration

The global MQP configuration is stored in the `myqolpackages` directory inside the Minecraft game directory:

```text
<minecraft-directory>/myqolpackages/config.json
```

MQP creates the file with the default configuration when it does not exist:

```json
{
  "additionalPackageRoots": []
}
```

## Additional Package Roots

`additionalPackageRoots` contains directories that MQP should also search for packages. Each configured directory is a
package root whose direct child directories are treated as packages.

```json
{
  "additionalPackageRoots": [
    "../../../../qolpackages",
    "/absolute/path/to/qolpackages"
  ]
}
```

Relative paths are resolved from the directory containing `config.json`. Absolute paths are used directly. Home
directory expansion, environment variables, and glob patterns are not supported.

The default `myqolpackages` root is searched first, followed by additional roots in configuration order. When multiple
packages have the same package identifier, none of the packages sharing that identifier are loaded and a diagnostic is
reported for each one.

Additional package roots must already exist. MQP does not create configured roots automatically.

The `/mqp packages refresh` command reloads `config.json` before scanning package roots, so configuration changes do not
require restarting Minecraft.
