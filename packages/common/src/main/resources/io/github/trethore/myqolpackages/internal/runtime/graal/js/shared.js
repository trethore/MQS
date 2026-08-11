export default function createJavaScriptRuntimeSupport() {
  return Object.freeze({
    createFrozenObject(members) {
      const object = {};
      for (const name of Object.keys(members)) {
        Object.defineProperty(object, name, {
          value: members[name],
          configurable: true,
          enumerable: true,
          writable: true,
        });
      }
      return Object.freeze(object);
    },

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
