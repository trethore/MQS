if (mqp.version !== "0.0.1") throw new Error("unexpected MQP version");
if (typeof mqp.dataDirectory !== "string") throw new Error("invalid data directory");
if (mqp.package.id !== "example-package") throw new Error("unexpected package ID");
if (!Object.isFrozen(mqp) || !Object.isFrozen(mqp.package)) {
  throw new Error("mutable MQP API");
}

const Files = Java.type("java.nio.file.Files");
const Path = Java.type("java.nio.file.Path");
if (!Files.isDirectory(Path.of(mqp.dataDirectory))) {
  throw new Error("missing data directory");
}
Files.writeString(Path.of(mqp.dataDirectory).resolve("created-during-load.txt"), "data");

try {
  mqp.version = "changed";
} catch (error) {}
if (mqp.version !== "0.0.1") throw new Error("mutable MQP version");
const originalDataDirectory = mqp.dataDirectory;
try {
  mqp.dataDirectory = "changed";
} catch (error) {}
if (mqp.dataDirectory !== originalDataDirectory) throw new Error("mutable data directory");

export function onEnable() {}
export function onDisable() {}
