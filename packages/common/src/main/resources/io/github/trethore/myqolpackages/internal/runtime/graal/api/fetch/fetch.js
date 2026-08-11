const toTypeError = (error) => {
  if (error instanceof TypeError) return error;
  const message = error?.message === undefined ? error : error.message;
  return new TypeError(String(message));
};

class MqpHeaders {
  #bridge;

  constructor(bridge) {
    this.#bridge = bridge;
    Object.freeze(this);
  }

  get(name) {
    return this.#bridge.get(String(name));
  }

  has(name) {
    return this.#bridge.has(String(name));
  }

  entries() {
    return Array.from(this.#bridge.entries, (entry) => [String(entry[0]), String(entry[1])])[
      Symbol.iterator
    ]();
  }

  keys() {
    return Array.from(this.#bridge.keys, String)[Symbol.iterator]();
  }

  values() {
    return Array.from(this.#bridge.values, String)[Symbol.iterator]();
  }

  [Symbol.iterator]() {
    return this.entries();
  }
}

class MqpResponse {
  #bridge;

  constructor(bridge) {
    this.#bridge = bridge;
    this.status = Number(bridge.status);
    this.statusText = String(bridge.statusText);
    this.ok = Boolean(bridge.ok);
    this.url = String(bridge.url);
    this.redirected = Boolean(bridge.redirected);
    this.headers = new MqpHeaders(bridge.headers);
    Object.defineProperties(this, {
      status: { writable: false, configurable: false },
      statusText: { writable: false, configurable: false },
      ok: { writable: false, configurable: false },
      url: { writable: false, configurable: false },
      redirected: { writable: false, configurable: false },
      headers: { writable: false, configurable: false },
    });
  }

  get bodyUsed() {
    return Boolean(this.#bridge.bodyUsed);
  }

  #consume(read, convert) {
    let value;
    try {
      value = read();
    } catch (error) {
      return Promise.reject(toTypeError(error));
    }
    try {
      return Promise.resolve(convert(value));
    } catch (error) {
      return Promise.reject(error);
    }
  }

  text() {
    return this.#consume(() => this.#bridge.consumeText(), String);
  }

  json() {
    return this.#consume(() => this.#bridge.consumeText(), (text) => JSON.parse(String(text)));
  }

  arrayBuffer() {
    return this.#consume(
      () => this.#bridge.consumeBytes(),
      (bytes) => Uint8Array.from(bytes).buffer,
    );
  }
}

const resolveFetch = (resolve) => (response) => resolve(new MqpResponse(response));
const rejectFetch = (reject) => (message) => reject(new TypeError(String(message)));
const executeFetch = (fetchBridge, input, init, resolve, reject) => {
  try {
    fetchBridge(input, init, resolveFetch(resolve), rejectFetch(reject));
  } catch (error) {
    reject(toTypeError(error));
  }
};

export default function createFetch(fetchBridge) {
  return (input, init = {}) =>
    new Promise((resolve, reject) => executeFetch(fetchBridge, input, init, resolve, reject));
}
