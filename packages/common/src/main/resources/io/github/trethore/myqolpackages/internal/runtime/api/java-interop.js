function installJavaInterop(host, bootstrap) {
  const interop = host.interop;
  bootstrap.defineGlobal("importClass", (...args) => interop.importClass(...args));
  bootstrap.defineGlobal("wrap", (...args) => interop.wrap(...args));
  bootstrap.defineGlobal("packages", interop.packages);
  bootstrap.defineGlobal("net", interop.net);
}
