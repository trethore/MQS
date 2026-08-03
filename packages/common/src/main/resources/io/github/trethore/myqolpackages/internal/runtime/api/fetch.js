export default function installFetch(host, bootstrap) {
  if (Object.hasOwn(globalThis, "fetch")) {
    throw new Error("MQP cannot install fetch because globalThis.fetch already exists");
  }
  const fetchBridge = host.fetch;
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
    let end = value.length;
    while (end > 0 && value[end - 1] === "=") end--;
    const clean = value.slice(0, end);
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
      this.headers = new MqpHeaders(Array.from(response.headers, (entry) => [entry[0], entry[1]]));
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
  bootstrap.defineGlobal("fetch", (input, init = {}) => {
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
}
