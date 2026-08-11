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
if (
  !Object.isFrozen(mqp) ||
  !Object.isFrozen(mqp.package) ||
  !Object.isFrozen(mqp.java) ||
  !Object.isFrozen(mqp.java.type)
) {
  throw new Error("mutable MQP API");
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
const originalJavaApi = mqp.java;
try { mqp.java = {}; } catch (error) {}
if (mqp.java !== originalJavaApi) throw new Error("mutable Java API");
const originalJavaTypes = mqp.java.type;
try { mqp.java.type = {}; } catch (error) {}
if (mqp.java.type !== originalJavaTypes) throw new Error("mutable Java type API");
const originalIntType = mqp.java.type.int;
try { mqp.java.type.int = null; } catch (error) {}
if (mqp.java.type.int !== originalIntType) throw new Error("mutable Java primitive type");

// Java class lookup

const HostString = Java.type("java.lang.String");
if (HostString.valueOf(42) !== "42") throw new Error("host lookup failed");
const imported = importClass("java.lang.Double");
const packaged = packages.java.lang.Double;
if (!imported || imported !== packaged) throw new Error("class proxies differ");

// Java primitive types

const primitiveTypes = {
  void: Java.type("java.lang.Void").TYPE,
  boolean: Java.type("java.lang.Boolean").TYPE,
  byte: Java.type("java.lang.Byte").TYPE,
  short: Java.type("java.lang.Short").TYPE,
  int: Java.type("java.lang.Integer").TYPE,
  long: Java.type("java.lang.Long").TYPE,
  float: Java.type("java.lang.Float").TYPE,
  double: Java.type("java.lang.Double").TYPE,
  char: Java.type("java.lang.Character").TYPE,
};
if (Object.keys(mqp.java.type).join(",") !== Object.keys(primitiveTypes).join(",")) {
  throw new Error("unexpected Java primitive type members");
}
for (const [name, type] of Object.entries(primitiveTypes)) {
  if (mqp.java.type[name] !== type) throw new Error(`invalid Java primitive type: ${name}`);
}
if (!importClass("java.lang.Integer").TYPE._equals(mqp.java.type.int)) {
  throw new Error("Java primitive type alias differs");
}
const valueOfInt = importClass("java.lang.String")._class.getMethod("valueOf", mqp.java.type.int);
if (valueOfInt.invoke(null, 42) !== "42") throw new Error("primitive reflection lookup failed");

export function onEnable() {}
export function onDisable() {}
