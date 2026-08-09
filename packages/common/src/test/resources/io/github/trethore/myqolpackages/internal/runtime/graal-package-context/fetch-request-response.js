let complete = false;
let failure;

const assert = (condition, message) => {
  if (!condition) throw new Error(message);
};

export function onEnable() {
  // Request options and response metadata

  // Non-string values verify the runtime's RequestInit string coercion.
  /** @type {*} */
  const requestInit = {
    method: "post",
    headers: { "X-Request": 42 },
    body: 123
  };
  fetch("http://127.0.0.1:__PORT__/json", requestInit)
    .then((response) => {
      assert(response.status === 201, "invalid status");
      assert(response.statusText === "Created", "invalid status text");
      assert(response.ok, "invalid ok value");
      assert(!response.redirected, "invalid redirected value");
      assert(response.url.endsWith("/json"), "invalid URL");

      // Header access and iteration

      assert(Object.isFrozen(response.headers), "mutable headers");
      const header = response.headers.get("X-Result");
      assert(header.includes("first") && header.includes("second"), "invalid header");
      assert(response.headers.has("x-result"), "missing header");
      assert([...response.headers].some(([name]) => name === "x-result"), "invalid entries");
      assert([...response.headers.keys()].includes("x-result"), "invalid keys");
      assert([...response.headers.values()].includes("first"), "invalid values");

      // JSON body consumption

      const result = response.json();
      assert(result instanceof Promise, "json did not return a Promise");
      assert(response.bodyUsed, "body was not consumed synchronously");
      return result.then((value) => {
        assert(value.value === 42, "invalid JSON body");
        return response.text().then(
          () => { throw new Error("body was consumed twice"); },
          (error) => assert(error instanceof TypeError, "invalid body error")
        );
      });
    })

    // Binary response bodies

    .then(() => fetch("http://127.0.0.1:__PORT__/binary"))
    .then((response) => response.arrayBuffer())
    .then((buffer) => {
      assert(buffer instanceof ArrayBuffer, "invalid array buffer");
      assert(
        Array.from(new Uint8Array(buffer)).join(",") === "0,1,127,128,255",
        "invalid binary body"
      );
      complete = true;
    })
    .catch((error) => {
      failure = String(error?.stack ?? error);
      complete = true;
    });
}

export function onDisable() {
  if (!complete) throw new Error("fetch did not complete");
  if (failure !== undefined) throw new Error(failure);
}
