function installMqp(host, bootstrap, permissions) {
  bootstrap.defineGlobal("mqp", Object.freeze({
    version: String(host.metadata.version),
    package: Object.freeze({ id: String(host.metadata.packageId) }),
    permissions
  }));
}
