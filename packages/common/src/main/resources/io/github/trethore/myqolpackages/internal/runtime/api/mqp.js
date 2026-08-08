export default function installMqp(host, bootstrap) {
  bootstrap.defineGlobal("mqp", Object.freeze({
    version: String(host.metadata.version),
    dataDirectory: String(host.metadata.dataDirectory),
    package: Object.freeze({ id: String(host.metadata.packageId) })
  }));
}
