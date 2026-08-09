import { value } from "./value.js";

// Package modules and metadata

if (value !== 42) throw new Error("module was not loaded");
if (mqp.version !== "0.0.1") throw new Error("unexpected MQP version");
if (typeof mqp.dataDirectory !== "string") throw new Error("invalid data directory");
if (mqp.package.id !== "example-package") throw new Error("unexpected package ID");

// Package data directory access

const Files = Java.type("java.nio.file.Files");
const Path = Java.type("java.nio.file.Path");
if (!Files.isDirectory(Path.of(mqp.dataDirectory))) {
  throw new Error("missing data directory");
}
Files.writeString(
  Path.of(mqp.dataDirectory).resolve("created-during-load.txt"),
  "data"
);

// Installed API globals

if (typeof fetch !== "function") throw new Error("missing fetch");
for (const name of ["mqp", "importClass", "wrap", "packages", "net", "fetch"]) {
  const descriptor = Object.getOwnPropertyDescriptor(globalThis, name);
  if (!descriptor || descriptor.configurable || !descriptor.enumerable || descriptor.writable) {
    throw new Error(`invalid global descriptor: ${name}`);
  }
}
if (!Object.isFrozen(mqp) || !Object.isFrozen(mqp.package)) {
  throw new Error("mutable MQP metadata");
}

// Internal implementation details must not leak into the package

for (const name of Object.getOwnPropertyNames(globalThis)) {
  if (name.startsWith("__mqp")) throw new Error(`leaked host bridge: ${name}`);
}
for (const name of [
  "createMqpBootstrap",
  "createRuntimeAdapter",
  "installMqp",
  "installJavaInterop",
  "installFetch"
]) {
  if (Object.hasOwn(globalThis, name)) throw new Error(`leaked API installer: ${name}`);
}

// Immutable metadata

try { mqp.version = "changed"; } catch (error) {}
if (mqp.version !== "0.0.1") throw new Error("mutable MQP API");
const originalDataDirectory = mqp.dataDirectory;
try { mqp.dataDirectory = "changed"; } catch (error) {}
if (mqp.dataDirectory !== originalDataDirectory) throw new Error("mutable data directory");

// Java class lookup

const HostString = Java.type("java.lang.String");
if (HostString.valueOf(42) !== "42") throw new Error("host lookup failed");
const imported = importClass("java.lang.Double");
const packaged = packages.java.lang.Double;
if (!imported || imported !== packaged) throw new Error("class proxies differ");

export function onEnable() {}
export function onDisable() {}
