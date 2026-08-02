{
  const hostAccess = globalThis.__mqpHostAccess;
  const hostClassLookup = globalThis.__mqpHostClassLookup;
  const filesystemRead = globalThis.__mqpFilesystemRead;
  const filesystemWrite = globalThis.__mqpFilesystemWrite;
  const importClassBridge = globalThis.__mqpImportClass;
  const packagesBridge = globalThis.__mqpPackages;
  const netBridge = globalThis.__mqpNet;
  const defineGlobal = (name, value) => Object.defineProperty(globalThis, name, {
    value,
    configurable: false,
    enumerable: true,
    writable: false
  });
  const permissions = Object.freeze({
    hostAccess,
    hostClassLookup,
    filesystem: Object.freeze({ read: filesystemRead, write: filesystemWrite }),
    has(permission) {
      if (permission === "hostAccess.full") return hostAccess === "full";
      if (permission === "hostClassLookup.minecraft") {
        return hostClassLookup === "minecraft" || hostClassLookup === "all";
      }
      if (permission === "hostClassLookup.all") return hostClassLookup === "all";
      if (permission === "filesystem.read.package") {
        return filesystemRead === "package" || filesystemRead === "mqp" || filesystemRead === "all";
      }
      if (permission === "filesystem.read.data") {
        return filesystemWrite === "data" || filesystemWrite === "mqp" || filesystemWrite === "all";
      }
      if (permission === "filesystem.read.mqp") {
        return filesystemRead === "mqp" || filesystemRead === "all" || filesystemWrite === "mqp" || filesystemWrite === "all";
      }
      if (permission === "filesystem.read.all") {
        return filesystemRead === "all" || filesystemWrite === "all";
      }
      if (permission === "filesystem.write.data") {
        return filesystemWrite === "data" || filesystemWrite === "mqp" || filesystemWrite === "all";
      }
      if (permission === "filesystem.write.mqp") {
        return filesystemWrite === "mqp" || filesystemWrite === "all";
      }
      if (permission === "filesystem.write.all") return filesystemWrite === "all";
      return false;
    }
  });
  Object.defineProperty(globalThis, "mqp", {
    value: Object.freeze({
      version: globalThis.__mqpVersion,
      package: Object.freeze({ id: globalThis.__mqpPackageId }),
      permissions
    }),
    configurable: false,
    enumerable: true,
    writable: false
  });
  defineGlobal("importClass", (...args) => importClassBridge(...args));
  defineGlobal("packages", packagesBridge);
  defineGlobal("net", netBridge);
  delete globalThis.__mqpVersion;
  delete globalThis.__mqpPackageId;
  delete globalThis.__mqpHostAccess;
  delete globalThis.__mqpHostClassLookup;
  delete globalThis.__mqpFilesystemRead;
  delete globalThis.__mqpFilesystemWrite;
  delete globalThis.__mqpImportClass;
  delete globalThis.__mqpPackages;
  delete globalThis.__mqpNet;
}
