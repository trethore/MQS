import { BRIDGE_UNAVAILABLE_MESSAGE, BridgeError } from "@/bridge/core/bridgeError";

export interface GrapheneBridgeApi {
  request: (channel: string, payload?: unknown) => Promise<unknown>;
  on: (channel: string, listener: (payload: unknown) => void) => () => void;
}

declare global {
  interface Window {
    grapheneBridge?: GrapheneBridgeApi;
  }
}

export function getGrapheneBridge(): GrapheneBridgeApi | null {
  if (typeof window === "undefined") {
    return null;
  }

  const bridge = window.grapheneBridge;
  if (!bridge) {
    return null;
  }

  if (typeof bridge.request !== "function" || typeof bridge.on !== "function") {
    return null;
  }

  return bridge;
}

export function requireGrapheneBridge(): GrapheneBridgeApi {
  const bridge = getGrapheneBridge();
  if (!bridge) {
    throw new BridgeError("BRIDGE_UNAVAILABLE", BRIDGE_UNAVAILABLE_MESSAGE);
  }

  return bridge;
}
