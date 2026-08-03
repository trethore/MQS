function createMqpBootstrap() {
  return Object.freeze({
    defineGlobal(name, value) {
      Object.defineProperty(globalThis, name, {
        value,
        configurable: false,
        enumerable: true,
        writable: false
      });
    }
  });
}
