# My QOL Packages documentation

## Package permissions

Packages declare the permissions they need in `manifest.json`:

```json
{
  "id": "example-package",
  "name": "Example Package",
  "description": "An example package.",
  "version": "1.0.0",
  "entrypoint": "src/index.js",
  "permissions": {
    "hostAccess": "full",
    "hostClassLookup": "minecraft",
    "filesystem": {
      "read": "package",
      "write": "data"
    }
  }
}
```

Every declared permission is required. Permissions omitted from the manifest default to `none`.

Users grant maximum permissions globally or per package in `config.json`:

```json
{
  "additionalPackageRoots": [],
  "enabledPackages": ["example-package"],
  "permissions": {
    "defaults": {
      "hostAccess": "none",
      "hostClassLookup": "none",
      "filesystem": {
        "read": "none",
        "write": "none"
      }
    },
    "packages": {
      "example-package": {
        "hostAccess": "full",
        "hostClassLookup": "minecraft",
        "filesystem": {
          "read": "package",
          "write": "data"
        }
      }
    }
  }
}
```

The package receives only the permissions declared in its manifest, even when the user grant is broader. MQP refuses to
enable a package when a requested permission exceeds its grant.

### Permission values

- `hostAccess`: `none`, `full`
- `hostClassLookup`: `none`, `minecraft`, `all`
- `filesystem.read`: `none`, `package`, `mqp`, `all`
- `filesystem.write`: `none`, `data`, `mqp`, `all`

`minecraft` class lookup permits `net.minecraft.*` and `com.mojang.blaze3d.*`. `data` is private storage under
`.data/<package-id>`. Write access includes read access to the same scope. `mqp` covers the MQP directory and configured
additional package roots.

`hostAccess: full` exposes public members of accessible host objects. Combining it with `hostClassLookup: all` should only
be granted to fully trusted packages.

Permissions are checked before the package context is created on both direct enable and reload.

## Runtime metadata

Packages can inspect their effective permissions and MQP metadata:

```js
mqp.version;
mqp.package.id;
mqp.permissions.hostAccess;
mqp.permissions.hostClassLookup;
mqp.permissions.filesystem.read;
mqp.permissions.filesystem.write;
mqp.permissions.has("filesystem.write.data");
```

The `mqp` metadata object is immutable. MQP executes packages using ECMAScript 2026.
