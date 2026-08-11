if (!Object.isFrozen(mqp.java) || !Object.isFrozen(mqp.java.type)) {
  throw new Error("mutable Java API");
}

const originalJavaApi = mqp.java;
try {
  mqp.java = {};
} catch (error) {}
if (mqp.java !== originalJavaApi) throw new Error("mutable Java API member");
const originalJavaTypes = mqp.java.type;
try {
  mqp.java.type = {};
} catch (error) {}
if (mqp.java.type !== originalJavaTypes) throw new Error("mutable Java type API");
const originalIntType = mqp.java.type.int;
try {
  mqp.java.type.int = null;
} catch (error) {}
if (mqp.java.type.int !== originalIntType) throw new Error("mutable Java primitive type");

const HostString = Java.type("java.lang.String");
if (HostString.valueOf(42) !== "42") throw new Error("host lookup failed");
const imported = importClass("java.lang.Double");
const packaged = packages.java.lang.Double;
if (!imported || imported !== packaged) throw new Error("class proxies differ");

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
