export default function createMqpPermissions(host) {
  const hostAccess = String(host.permissions.hostAccess);
  const hostClassLookup = String(host.permissions.hostClassLookup);
  const filesystemRead = String(host.permissions.filesystemRead);
  const filesystemWrite = String(host.permissions.filesystemWrite);
  const internetAccess = String(host.permissions.internetAccess);
  const internetDomains = Object.freeze(Array.from(host.permissions.internetDomains));
  return Object.freeze({
    hostAccess,
    hostClassLookup,
    filesystem: Object.freeze({ read: filesystemRead, write: filesystemWrite }),
    internet: Object.freeze({ access: internetAccess, domains: internetDomains }),
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
      if (permission === "internet.domains") {
        return internetAccess === "domains" || internetAccess === "full";
      }
      if (permission === "internet.full") return internetAccess === "full";
      return false;
    }
  });
}
