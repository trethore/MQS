export default function createJavaScriptRuntimeSupport() {
  return Object.freeze({
    defineGlobal(name, value) {
      Object.defineProperty(globalThis, name, {
        value,
        configurable: false,
        enumerable: true,
        writable: false,
      });
    },

    hasOwnGlobal(name) {
      return Object.hasOwn(globalThis, String(name));
    },

    isArray(value) {
      return Array.isArray(value);
    },

    isObject(value) {
      return value !== null && typeof value === "object";
    },

    isUndefined(value) {
      return value === undefined;
    },

    ownKeys(value) {
      return Object.keys(value);
    },

    stringify: String,
  });
}
