import { value } from "./value.js";

if (value !== 42) throw new Error("module was not loaded");

if (typeof fetch !== "function") throw new Error("missing fetch");
for (const name of ["mqp", "importClass", "wrap", "packages", "net", "fetch"]) {
  const descriptor = Object.getOwnPropertyDescriptor(globalThis, name);
  if (!descriptor || descriptor.configurable || !descriptor.enumerable || descriptor.writable) {
    throw new Error(`invalid global descriptor: ${name}`);
  }
}
if (!Object.isFrozen(mqp)) throw new Error("mutable MQP API");

for (const name of Object.getOwnPropertyNames(globalThis)) {
  if (name.startsWith("__mqp")) throw new Error(`leaked host bridge: ${name}`);
}
for (const name of [
  "createJavaScriptRuntimeSupport",
  "createFrozenObject",
  "createJavaApi",
  "createFetch"
]) {
  if (Object.hasOwn(globalThis, name)) throw new Error(`leaked API installer: ${name}`);
}

export function onEnable() {}
export function onDisable() {}
