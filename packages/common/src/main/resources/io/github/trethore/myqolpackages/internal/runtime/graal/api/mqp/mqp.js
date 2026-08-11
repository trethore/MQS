export default function createMqp(version, dataDirectory, packageId, members) {
  const mqp = {
    version: String(version),
    dataDirectory: String(dataDirectory),
    package: Object.freeze({ id: String(packageId) }),
  };
  for (const name of Object.keys(members)) {
    Object.defineProperty(mqp, name, {
      value: members[name],
      configurable: true,
      enumerable: true,
      writable: true,
    });
  }
  return Object.freeze(mqp);
}
