export default function installMqp(host, bootstrap) {
  bootstrap.defineGlobal("mqp", Object.freeze({
    version: String(host.metadata.version),
    package: Object.freeze({ id: String(host.metadata.packageId) })
  }));
}
