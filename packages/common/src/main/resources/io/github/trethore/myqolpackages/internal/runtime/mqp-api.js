{
  const hostAccess = globalThis.__mqpHostAccess;
  const hostClassLookup = globalThis.__mqpHostClassLookup;
  const filesystemRead = globalThis.__mqpFilesystemRead;
  const filesystemWrite = globalThis.__mqpFilesystemWrite;
  const internetAccess = globalThis.__mqpInternetAccess;
  const internetDomains = Object.freeze(Array.from(globalThis.__mqpInternetDomains));
  const fetchBridge = globalThis.__mqpFetch;
  const importClassBridge = globalThis.__mqpImportClass;
  const wrapBridge = globalThis.__mqpWrap;
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
  defineGlobal("wrap", (...args) => wrapBridge(...args));
  defineGlobal("packages", packagesBridge);
  defineGlobal("net", netBridge);
  if (Object.prototype.hasOwnProperty.call(globalThis, "fetch")) {
    throw new Error("MQP cannot install fetch because globalThis.fetch already exists");
  }
  const normalizeHeaders = (headers) => {
    if (headers === undefined || headers === null) return [];
    if (Array.isArray(headers)) {
      return headers.map((entry) => {
        if (!Array.isArray(entry) || entry.length !== 2) throw new TypeError("Invalid header entry");
        return [String(entry[0]), String(entry[1])];
      });
    }
    if (typeof headers === "object") {
      return Object.entries(headers).map(([name, value]) => [name, String(value)]);
    }
    throw new TypeError("Headers must be an object or an array");
  };
  const decodeBase64 = (value) => {
    const alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    const clean = value.replace(/=+$/, "");
    const bytes = new Uint8Array(Math.floor(clean.length * 6 / 8));
    let accumulator = 0;
    let bits = 0;
    let outputIndex = 0;
    for (const character of clean) {
      const index = alphabet.indexOf(character);
      if (index < 0) throw new TypeError("Invalid response body encoding");
      accumulator = (accumulator << 6) | index;
      bits += 6;
      if (bits >= 8) {
        bits -= 8;
        bytes[outputIndex++] = (accumulator >> bits) & 0xff;
        accumulator &= (1 << bits) - 1;
      }
    }
    return bytes.buffer;
  };
  class MqpHeaders {
    #entries;
    constructor(entries) {
      this.#entries = entries.map(([name, value]) => [String(name).toLowerCase(), String(value)]);
      Object.freeze(this);
    }
    get(name) {
      const normalizedName = String(name).toLowerCase();
      const values = this.#entries.filter(([entryName]) => entryName === normalizedName).map(([, value]) => value);
      return values.length === 0 ? null : values.join(", ");
    }
    has(name) {
      const normalizedName = String(name).toLowerCase();
      return this.#entries.some(([entryName]) => entryName === normalizedName);
    }
    entries() { return this.#entries.map((entry) => [...entry])[Symbol.iterator](); }
    keys() { return this.#entries.map(([name]) => name)[Symbol.iterator](); }
    values() { return this.#entries.map(([, value]) => value)[Symbol.iterator](); }
    [Symbol.iterator]() { return this.entries(); }
  }
  const statusText = (status) => ({
    200: "OK", 201: "Created", 202: "Accepted", 204: "No Content",
    301: "Moved Permanently", 302: "Found", 303: "See Other",
    307: "Temporary Redirect", 308: "Permanent Redirect",
    400: "Bad Request", 401: "Unauthorized", 403: "Forbidden", 404: "Not Found",
    429: "Too Many Requests", 500: "Internal Server Error", 502: "Bad Gateway",
    503: "Service Unavailable", 504: "Gateway Timeout"
  })[status] || "";
  class MqpResponse {
    #body;
    #base64;
    #bodyUsed = false;
    constructor(response) {
      this.status = Number(response.status);
      this.statusText = statusText(this.status);
      this.ok = this.status >= 200 && this.status <= 299;
      this.url = String(response.url);
      this.redirected = Boolean(response.redirected);
      this.headers = new MqpHeaders(Array.from(response.headers, (entry) => Array.from(entry)));
      this.#body = String(response.text);
      this.#base64 = String(response.base64);
      Object.defineProperties(this, {
        status: { writable: false, configurable: false },
        statusText: { writable: false, configurable: false },
        ok: { writable: false, configurable: false },
        url: { writable: false, configurable: false },
        redirected: { writable: false, configurable: false },
        headers: { writable: false, configurable: false }
      });
    }
    get bodyUsed() { return this.#bodyUsed; }
    #consume(value) {
      if (this.#bodyUsed) return Promise.reject(new TypeError("Response body has already been used"));
      this.#bodyUsed = true;
      return Promise.resolve(value);
    }
    text() { return this.#consume(this.#body); }
    json() {
      if (this.#bodyUsed) return Promise.reject(new TypeError("Response body has already been used"));
      this.#bodyUsed = true;
      try { return Promise.resolve(JSON.parse(this.#body)); }
      catch (error) { return Promise.reject(error); }
    }
    arrayBuffer() { return this.#consume(decodeBase64(this.#base64)); }
  }
  defineGlobal("fetch", (input, init = {}) => {
    if (typeof input !== "string") return Promise.reject(new TypeError("fetch URL must be a string"));
    if (init === null || typeof init !== "object") return Promise.reject(new TypeError("fetch options must be an object"));
    for (const option of Object.keys(init)) {
      if (option !== "method" && option !== "headers" && option !== "body") {
        return Promise.reject(new TypeError(`Unsupported fetch option: ${option}`));
      }
    }
    const method = init.method === undefined ? "GET" : String(init.method).toUpperCase();
    const body = init.body === undefined || init.body === null ? null : String(init.body);
    let headers;
    try { headers = normalizeHeaders(init.headers); }
    catch (error) { return Promise.reject(error); }
    return new Promise((resolve, reject) => fetchBridge(
      { url: input, method, headers, body },
      (response) => resolve(new MqpResponse(response)),
      (message) => reject(new TypeError(String(message)))
    ));
  });
  delete globalThis.__mqpVersion;
  delete globalThis.__mqpPackageId;
  delete globalThis.__mqpHostAccess;
  delete globalThis.__mqpHostClassLookup;
  delete globalThis.__mqpFilesystemRead;
  delete globalThis.__mqpFilesystemWrite;
  delete globalThis.__mqpInternetAccess;
  delete globalThis.__mqpInternetDomains;
  delete globalThis.__mqpFetch;
  delete globalThis.__mqpImportClass;
  delete globalThis.__mqpWrap;
  delete globalThis.__mqpPackages;
  delete globalThis.__mqpNet;
}
